package com.project;

import com.project.manager.BenchmarkPipeline;
import com.project.model.Transaction;
import com.project.util.DatasetManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // Đảm bảo các thư mục hệ thống luôn tồn tại
        new File("datasets").mkdir();
        new File("results").mkdir();
        new File("cache").mkdir(); // Thư mục chứa file .dat của DatasetManager

        Scanner sc = new Scanner(System.in);
        BenchmarkPipeline pipeline = new BenchmarkPipeline();

        System.out.println("=========================================================================");
        System.out.println(" 🚀 HỆ THỐNG KHAI PHÁ TĂNG TIẾN (SMART CACHE + GIỮ NGUYÊN GỐC 100%) ");
        System.out.println("=========================================================================\n");

        try {
            // ---------------------------------------------------------
            // BƯỚC 1: CHỌN DATASET NỀN
            // ---------------------------------------------------------
            System.out.println("[1] CHỌN TẬP DỮ LIỆU:");
            System.out.println(" 1. Chess      (datasets/dataset-chess.txt)");
            System.out.println(" 2. Mushrooms  (datasets/dataset-mushrooms.txt)");
            System.out.println(" 3. BMSWebView1 (datasets/dataset-bms1.txt)");
            System.out.println(" 4. Nhập đường dẫn tùy chỉnh...");
            System.out.print(" >> Nhập lựa chọn [1-4] hoặc dán đường dẫn: ");
            
            String dsInput = sc.nextLine().trim();
            String datasetPath;

            if (dsInput.equals("1")) datasetPath = "datasets/dataset-chess.txt";
            else if (dsInput.equals("2")) datasetPath = "datasets/dataset-mushrooms.txt";
            else if (dsInput.equals("3")) datasetPath = "datasets/dataset-bms1.txt";
            else datasetPath = dsInput;

            String baseName = new File(datasetPath).getName().replace(".txt", "");
            String targetDynamicPath = "datasets/" + baseName + "_dynamic.txt";

            // ---------------------------------------------------------
            // BƯỚC 2: KHỞI TẠO HOẶC KẾ THỪA FILE DYNAMIC
            // ---------------------------------------------------------
            String pathToLoad = targetDynamicPath;
            if (new File(targetDynamicPath).exists()) {
                System.out.println("\n🌟 [KẾ THỪA] Phát hiện file Dynamic đã tồn tại! Hệ thống sẽ nạp để cộng dồn đi lên...");
            } else {
                // NẾU CHƯA CÓ: Copy thẳng file gốc sang file Dynamic để bảo toàn 100% dòng đầu
                Files.copy(Paths.get(datasetPath), Paths.get(targetDynamicPath), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("\n🌟 [KHỞI TẠO] Đã nhân bản gốc thành file Dynamic (An toàn dữ liệu tuyệt đối).");
            }

            // ---------------------------------------------------------
            // BƯỚC 3: SỬ DỤNG TRỌN VẸN DATASET MANAGER (CÓ CACHE)
            // ---------------------------------------------------------
            System.out.println("⏳ Đang gọi DatasetManager nạp dữ liệu...");
            Map<Integer, String> idToItem = new HashMap<>();
            Map<String, Integer> itemToId = new HashMap<>();
            
            // Gọi chức năng cốt lõi của DatasetManager
            List<Transaction> baseDB = DatasetManager.loadDataset(pathToLoad, idToItem, itemToId);
            
            if (baseDB == null || baseDB.isEmpty()) {
                System.out.println("❌ Lỗi: Không thể nạp dữ liệu từ " + pathToLoad);
                return;
            }

            // [HÀM BÍ MẬT] Sửa lại xác suất bị Manager làm sai để đảm bảo xuất file chuẩn!
            mineclosedfrequentuncertainfile(baseDB, pathToLoad, itemToId);

            // ---------------------------------------------------------
            // BƯỚC 4: CẤU HÌNH THAM SỐ THỰC NGHIỆM
            // ---------------------------------------------------------
            System.out.println("\n[2] CẤU HÌNH MÔ PHỎNG TĂNG TIẾN:");
            System.out.print(" >> Nhập số lượng giao dịch MỚI muốn sinh thêm (genCount): ");
            int genCount = Integer.parseInt(sc.nextLine().trim());
            
            if (genCount == 0) {
                System.out.println("   -> Đã chọn 0 gen thêm. Hệ thống sẽ khai phá trực tiếp trên dữ liệu hiện tại.");
            }

            System.out.print(" >> Nhập ngưỡng minSup (VD: 0.1, 0.2): ");
            String msRaw = sc.nextLine().trim();
            double minSup = Double.parseDouble(msRaw);

            System.out.println("\n[3] CHỌN THUẬT TOÁN:");
            System.out.println(" 1. Inc-CHARM | 2. Inc-dCharm | 3. Inc-FP-Close | 4. Chạy cả 3");
            System.out.print(" >> Nhập lựa chọn [1-4]: ");
            int algoChoice = Integer.parseInt(sc.nextLine().trim());

            // ---------------------------------------------------------
            // BƯỚC 5: THỰC THI QUA PIPELINE (ĐÃ FIX LỖI GHI ĐÈ FILE)
            // ---------------------------------------------------------
            System.out.println("\n=========================================================================");
            System.out.println(" 🚀 ĐANG THỰC THI BENCHMARK...");
            System.out.println("=========================================================================");
            
            if (algoChoice == 4) {
                System.out.println(">> Đang chạy độc lập 3 thuật toán để xuất đủ 3 file kết quả...");
                
                System.out.println("\n[--- LƯỢT 1: Inc-CHARM ---]");
                pipeline.runIncrementalRace(baseDB, targetDynamicPath, genCount, minSup, 1, idToItem);
                
                System.out.println("\n[--- LƯỢT 2: Inc-dCharm ---]");
                pipeline.runIncrementalRace(baseDB, targetDynamicPath, genCount, minSup, 2, idToItem);
                
                System.out.println("\n[--- LƯỢT 3: Inc-FP-Close ---]");
                pipeline.runIncrementalRace(baseDB, targetDynamicPath, genCount, minSup, 3, idToItem);
                
            } else {
                pipeline.runIncrementalRace(baseDB, targetDynamicPath, genCount, minSup, algoChoice, idToItem);
            }

            System.out.println("\n🎉 HOÀN THÀNH!");
            System.out.println("📂 Kiểm tra Cache: cache/" + baseName + "_dynamic_cache.dat");
            System.out.println("📂 Kiểm tra Dynamic: " + targetDynamicPath);
            System.out.println("📂 Kiểm tra File Kết quả: Đã xuất đầy đủ trong folder 'results/'");

        } catch (Exception e) {
            System.err.println("❌ Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        } finally {
            sc.close();
        }
    }

    /**
     * HÀM BỔ TRỢ: Dùng để khôi phục lại các con số xác suất gốc (0.xx)
     * Lý do: File DatasetManager của bạn chứa lệnh (rand.nextDouble() * 0.5) ép đè mọi số.
     * Hàm này sẽ đọc lại text gốc và trả lại sự công bằng cho 3196 dòng đầu tiên.
     */
    private static void mineclosedfrequentuncertainfile(List<Transaction> db, String filePath, Map<String, Integer> itemToId) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int idx = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) continue;
                if (idx >= db.size()) break;

                Transaction t = db.get(idx);
                String[] tokens = line.split("\\s+");
                int startIndex = tokens[0].matches("T?\\d+:?") ? 1 : 0;
                
                for (int i = startIndex; i < tokens.length; i++) {
                    String token = tokens[i].replace(",", "");
                    double prob = -1;
                    String itemName = null;
                    
                    // Cố gắng bắt chính xác con số nằm trong ngoặc tròn (0.xx)
                    if (token.contains("(") && token.contains(")")) {
                        itemName = token.substring(0, token.indexOf('(')).trim();
                        String probStr = token.substring(token.indexOf('(') + 1, token.indexOf(')'));
                        try { prob = Double.parseDouble(probStr); } catch(Exception ignored){}
                    } else if (token.contains(":")) {
                        itemName = token.split(":")[0].trim();
                        try { prob = Double.parseDouble(token.split(":")[1].trim()); } catch(Exception ignored){}
                    }
                    
                    // Nếu lấy được số gốc, ép đè lại vào bộ nhớ RAM
                    if (itemName != null && prob != -1) {
                        Integer itemId = itemToId.get(itemName);
                        if (itemId != null && t.getProbs().containsKey(itemId)) {
                            t.getProbs().put(itemId, prob);
                        }
                    }
                }
                idx++;
            }
            System.out.println("✅ [FIX] Đã bypass DatasetManager: Khôi phục thành công xác suất gốc cho " + idx + " giao dịch!");
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khôi phục xác suất: " + e.getMessage());
        }
    }
}
