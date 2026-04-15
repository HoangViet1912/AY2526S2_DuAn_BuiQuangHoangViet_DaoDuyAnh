package com.project;

import com.project.algorithm.fpclose.IncFPCloseMiner;
import com.project.algorithm.charm.IncUncertainCharmMiner;
import com.project.algorithm.dcharm.IncDCharmMiner;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MemoryLogger;
import com.project.manager.ResultWriter;
import com.project.util.DatasetManager; // ĐÃ THÊM IMPORT

import java.io.*;
import java.util.*;

public class Main {

    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("======================================================");
        System.out.println("🛠️ HỆ THỐNG KIỂM THỬ KHAI PHÁ TÍCH LŨY ĐỘNG (INTERACTIVE)");
        System.out.println("======================================================");

        try {
            // [1] NHẬP ĐƯỜNG DẪN FILE
            // ==========================================================
            System.out.println("\n[1] NHẬP ĐƯỜNG DẪN TẬP DỮ LIỆU CẦN PHÂN TÍCH:");
            System.out.print("=> Đường dẫn file: ");
            String targetFilePath = scanner.nextLine().trim();

            File checkFile = new File(targetFilePath);
            if (!checkFile.exists()) {
                System.out.println("❌ Lỗi: File không tồn tại tại: " + checkFile.getAbsolutePath());
                
                File dsFolder = new File("datasets");
                if (dsFolder.exists() && dsFolder.isDirectory()) {
                    System.out.println("📂 Gợi ý: Các file đang có trong thư mục datasets là:");
                    for (File f : dsFolder.listFiles()) {
                        if (f.isFile()) System.out.println("   - datasets/" + f.getName());
                    }
                }
                return;
            }

            // ==========================================================
            // ĐÃ SỬA: SỬ DỤNG DATASET MANAGER ĐỂ ĐỌC FILE VÀ TẠO CACHE
            // ==========================================================
            Map<Integer, String> idToItem = new HashMap<>();
            Map<String, Integer> itemToId = new HashMap<>();
            
            // Gọi hàm từ DatasetManager
            List<Transaction> dataset = DatasetManager.loadDataset(targetFilePath, idToItem, itemToId);
            
            if (dataset.isEmpty()) {
                System.out.println("❌ Lỗi: File tồn tại nhưng không có dữ liệu hoặc sai định dạng (T1: 1(0.5)).");
                return;
            }
            int currentMaxTid = dataset.get(dataset.size() - 1).getTid();
            System.out.println("✅ Đã nạp " + dataset.size() + " giao dịch hiện có từ: " + targetFilePath);

            // Phân tích dữ liệu
            int maxItemFound = 0;
            int totalItemsCount = 0;
            for (Transaction t : dataset) {
                totalItemsCount += t.getProbs().size();
                for (int item : t.getProbs().keySet()) {
                    if (item > maxItemFound) maxItemFound = item;
                }
            }
            int avgLength = Math.max(1, totalItemsCount / dataset.size());

            // [2] CHỌN THUẬT TOÁN
            System.out.println("\n[2] CHỌN THUẬT TOÁN TEST (1: FP-Close, 2: Charm, 3: dCharm, 4: Cả 3):");
            int algoChoice = Integer.parseInt(scanner.nextLine().trim());

            // [3] CHỌN NGƯỠNG
            System.out.print("\n[3] NHẬP CÁC NGƯỠNG (VD: 0.5, 0.2): ");
            String[] supStrings = scanner.nextLine().split(",");
            List<Double> thresholds = new ArrayList<>();
            for (String s : supStrings) thresholds.add(Double.parseDouble(s.trim()));

            // [4] SINH DỮ LIỆU ĐỘNG
            System.out.print("\n[4] Bạn muốn sinh thêm bao nhiêu giao dịch? (0 để bỏ qua): ");
            int genCount = Integer.parseInt(scanner.nextLine().trim());

            if (genCount > 0) {
                List<Transaction> newData = generateAndAppendData(targetFilePath, currentMaxTid + 1, genCount, maxItemFound, avgLength);
                dataset.addAll(newData); 
                System.out.println("✅ Ghi nối thành công! Tổng dataset: " + dataset.size());
            }

            // [5] CHẠY BENCHMARK
            // ==========================================================
            List<Transaction> sharedDB = dataset; 
            int totalTransactions = sharedDB.size();

            System.out.println("\n🚀 KẾT QUẢ BENCHMARK: " + targetFilePath.toUpperCase());
            System.out.println("🧹 Đang dọn dẹp trạng thái cũ để tránh nhiễm chéo dữ liệu...");
            new File("accumulated_db.bin").delete(); 
            new File("state.dat").delete();
            
            for (double minSup : thresholds) {
                System.out.printf("🔥 Ngưỡng MinSup = %.1f%% (%.3f)\n", (minSup * 100), minSup);

                // --- TEST FP-CLOSE ---
                if (algoChoice == 1 || algoChoice == 4) {
                    IncFPCloseMiner fp = new IncFPCloseMiner();
                    MemoryLogger.getInstance().reset();
                    long t1 = System.currentTimeMillis();
                    List<Itemset> res = fp.mineIncremental(sharedDB, minSup);
                    long time = System.currentTimeMillis() - t1;
                    double mem = MemoryLogger.getInstance().getMaxMemory();
                    String algoName = "Inc-FP-Close (HeaderTable + Stateful CFI-Tree)";

                    System.out.printf("[FP-Close]   CFI: %-8d | Time: %-6d ms | RAM: %.2f MB\n", res.size(), time, mem);
                    ResultWriter.exportLogCSV(algoName, targetFilePath, totalTransactions, genCount, minSup, res.size(), time, mem);
                    // ĐÃ SỬA: Đổi null thành idToItem
                    ResultWriter.exportFormatResult(algoName, targetFilePath, genCount, totalTransactions, minSup, res, idToItem, time, mem);
                }

                // --- TEST CHARM ---
                if (algoChoice == 2 || algoChoice == 4) {
                    IncUncertainCharmMiner charm = new IncUncertainCharmMiner();
                    MemoryLogger.getInstance().reset();
                    long t1 = System.currentTimeMillis();
                    List<Itemset> res = charm.mineIncremental(sharedDB, minSup);
                    long time = System.currentTimeMillis() - t1;
                    double mem = MemoryLogger.getInstance().getMaxMemory();
                    String algoName = "Inc-CHARM (Vertical Incremental)";

                    System.out.printf("[Charm]      CFI: %-8d | Time: %-6d ms | RAM: %.2f MB\n", res.size(), time, mem);
                    ResultWriter.exportLogCSV(algoName, targetFilePath, totalTransactions, genCount, minSup, res.size(), time, mem);
                    // ĐÃ SỬA: Đổi null thành idToItem
                    ResultWriter.exportFormatResult(algoName, targetFilePath, genCount, totalTransactions, minSup, res, idToItem, time, mem);
                }

                // --- TEST DCHARM ---
                if (algoChoice == 3 || algoChoice == 4) {
                    IncDCharmMiner dcharm = new IncDCharmMiner();
                    MemoryLogger.getInstance().reset();
                    long t1 = System.currentTimeMillis();
                    List<Itemset> res = dcharm.mineIncremental(sharedDB, minSup);
                    long time = System.currentTimeMillis() - t1;
                    double mem = MemoryLogger.getInstance().getMaxMemory();
                    String algoName = "Inc-dCharm (Exact Probabilistic Diffset)";

                    System.out.printf("[dCharm]     CFI: %-8d | Time: %-6d ms | RAM: %.2f MB\n", res.size(), time, mem);
                    ResultWriter.exportLogCSV(algoName, targetFilePath, totalTransactions, genCount, minSup, res.size(), time, mem);
                    // ĐÃ SỬA: Đổi null thành idToItem
                    ResultWriter.exportFormatResult(algoName, targetFilePath, genCount, totalTransactions, minSup, res, idToItem, time, mem);
                }
            }
            System.out.println("\n🎉 HOÀN TẤT! Đã cập nhật vào file performance_log.csv");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }

    // ĐÃ XÓA: Hàm readDataset() cũ kỹ để tránh xung đột

    // Hàm sinh dữ liệu tự động giữ nguyên
    private static List<Transaction> generateAndAppendData(String targetFilePath, int startTid, int count, int maxItem, int avgLength) {
        List<Transaction> generatedData = new ArrayList<>();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(targetFilePath, true))) {
            for (int i = 0; i < count; i++) {
                int tid = startTid + i;
                Transaction t = new Transaction(tid);
                StringBuilder lineBuilder = new StringBuilder("T" + tid + ":");
                int numItems = Math.max(1, avgLength + (RANDOM.nextInt(10) - 5)); 
                List<Integer> pool = new ArrayList<>();
                for (int j = 1; j <= maxItem; j++) pool.add(j);
                Collections.shuffle(pool);
                List<Integer> selected = pool.subList(0, Math.min(numItems, pool.size()));
                Collections.sort(selected);
                for (int j = 0; j < selected.size(); j++) {
                    double p = Math.round((0.1 + RANDOM.nextDouble() * 0.8) * 100.0) / 100.0;
                    t.getProbs().put(selected.get(j), p);
                    lineBuilder.append(" ").append(selected.get(j)).append("(").append(String.format(Locale.US, "%.2f", p)).append(")").append(j == selected.size() - 1 ? "" : ",");
                }
                generatedData.add(t);
                bw.write(lineBuilder.toString());
                bw.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); }
        return generatedData;
    }
}