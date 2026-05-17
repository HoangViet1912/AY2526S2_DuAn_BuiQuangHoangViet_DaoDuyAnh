package com.project;

import com.project.algorithm.MiningAlgorithm;
import com.project.algorithm.charm.IncUncertainCharmMiner;
import com.project.algorithm.dcharm.IncDCharmMiner;
import com.project.algorithm.fpclose.IncFPCloseMiner;
import com.project.manager.ResultWriter;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.DatasetManager;
import com.project.util.MemoryLogger;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * HỆ THỐNG BENCHMARK DỮ LIỆU ĐỘNG TÍCH LŨY (BẢN TỐI ƯU CÙNG SMART CACHE)
 */
public class ExBMS1 {

    // File gốc (Bảo tồn vĩnh viễn, không bao giờ bị ghi đè)
    private static final String SOURCE_BMS1 = "datasets/dataset-BMS1.txt";
    // File Workspace (Dùng để hứng dữ liệu phình to trong lúc test)
    private static final String DYNAMIC_BMS1 = "datasets/dataset-BMS1_dynamic.txt";

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" 🚀 BẮT ĐẦU THỰC NGHIỆM TÍCH LŨY ĐỘNG (SMART CACHING ENABLED)");
        System.out.println("=========================================================================\n");

        try {
            // 1. Tự động thiết lập Workspace an toàn (Không cần xóa Cache nữa!)
            prepareWorkspace();

            // 2. Chạy Thực nghiệm trên file Dynamic
            runBMS1Benchmark();
            
            System.out.println("\n=========================================================================");
            System.out.println(" ✨ HOÀN TẤT! Kết quả đã được lưu tại: 'experiment.csv'");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Copy file gốc ra file nháp để chạy thực nghiệm.
     * Khi dung lượng file nháp bị reset về trạng thái gốc, Smart Cache của DatasetManager 
     * sẽ tự động nhận diện và cập nhật bộ đệm!
     */
    private static void prepareWorkspace() throws Exception {
        System.out.println("[⚙️] Đang kiểm tra môi trường làm việc...");
        File sourceFile = new File(SOURCE_BMS1);
        File dynamicFile = new File(DYNAMIC_BMS1);

        if (!sourceFile.exists()) {
            throw new Exception("Không tìm thấy file gốc: " + SOURCE_BMS1);
        }

        // ĐIỂM QUYẾT ĐỊNH: Chỉ copy từ file gốc nếu file Dynamic CHƯA TỒN TẠI
        if (!dynamicFile.exists()) {
            Files.copy(Paths.get(SOURCE_BMS1), Paths.get(DYNAMIC_BMS1));
            System.out.println("   -> Đã khởi tạo Workspace lần đầu từ: " + SOURCE_BMS1);
        } else {
            // NẾU ĐÃ CÓ FILE RỒI -> GIỮ NGUYÊN ĐỂ TÍCH LŨY TIẾP
            System.out.println("   -> 🟢 Đã tìm thấy Workspace cũ! Hệ thống sẽ TIẾP TỤC TÍCH LŨY nối tiếp vào file này.");
        }
    }

    private static void runBMS1Benchmark() throws Exception {
        // Chỉ trỏ thuật toán vào file nháp (Dynamic)
        String[] datasets = { DYNAMIC_BMS1 };
        
        double[] bms1MinSups = { 0.015, 0.012, 0.010, 0.008, 0.006, 0.005, 0.004, 0.003, 0.002, 0.001 };
        int[] genCounts = { 0 ,500,1000,2000,5000}; 

        for (String currentDataset : datasets) {
            System.out.println("\n=========================================================================");
            System.out.println(" 📂 TARGET DATASET: " + currentDataset);
            System.out.println("=========================================================================");

            Map<Integer, String> idToItem = new HashMap<>();
            Map<String, Integer> itemToId = new HashMap<>();

            System.out.println("[*] Đang nạp CSDL...");
            // Nơi phép màu Smart Cache diễn ra
            List<Transaction> baseDB = DatasetManager.loadDataset(currentDataset, idToItem, itemToId);

            boolean isFirstMinSup = true;

            for (double minSup : bms1MinSups) {
                System.out.println("\n-------------------------------------------------------------------------");
                System.out.println(" 🎯 NGƯỠNG MINSUP = " + minSup);
                System.out.println("-------------------------------------------------------------------------");

                IncUncertainCharmMiner incCharm = new IncUncertainCharmMiner();
                IncDCharmMiner incDCharm = new IncDCharmMiner();
                IncFPCloseMiner incFP = new IncFPCloseMiner();

                int currentTotalDBSize = 0;
                int maxTid = baseDB.isEmpty() ? 0 : baseDB.get(baseDB.size() - 1).getTid();
                Random rand = new Random(42); 

                for (int gen : genCounts) {
                    List<Transaction> chunkToMine;
                    String modeName;

                    if (gen == 0) {
                        modeName = "BASE DB (Khởi tạo Cấu trúc)";
                        chunkToMine = baseDB;
                        currentTotalDBSize = baseDB.size();
                    } else {
                        modeName = "INCREMENTAL (Nạp thêm " + gen + " dòng mới)";
                        chunkToMine = new ArrayList<>();
                        for (int i = 0; i < gen; i++) {
                            Transaction randomTx = baseDB.get(rand.nextInt(baseDB.size()));
                            Transaction newTx = new Transaction(maxTid + i + 1); 
                            
                            double rowProb = 0.5 + (rand.nextDouble() * 0.5);
                            for (Integer itemId : randomTx.getProbs().keySet()) {
                                newTx.getProbs().put(itemId, rowProb);
                            }
                            chunkToMine.add(newTx);
                        }
                        maxTid += gen;
                        currentTotalDBSize += gen; 
                        
                        // Ghi vật lý dòng mới vào file nháp (Chỉ làm ở vòng MinSup đầu tiên)
                        if (isFirstMinSup) {
                            ResultWriter.appendDeltaToDataset(chunkToMine, idToItem, currentDataset, gen);
                        }
                    }

                    System.out.println("\n>>> " + modeName + " | Tổng Data: " + currentTotalDBSize);

                    runMinerAndLog(incFP, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
                    runMinerAndLog(incCharm, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
                    runMinerAndLog(incDCharm, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
                }
                
                isFirstMinSup = false; 
            }
        }
    }

    private static void runMinerAndLog(MiningAlgorithm miner, List<Transaction> dbChunk, 
                                       double minSup, String datasetPath, int totalSize, 
                                       int genCount, Map<Integer, String> idToItem) {
        
        System.out.print("   + " + miner.getName() + " ... ");
        
        System.gc();
        try { Thread.sleep(50); } catch (Exception ignored) {}
        MemoryLogger.getInstance().reset();
        
        long t1 = System.currentTimeMillis();
        List<Itemset> res = miner.mineIncremental(dbChunk, minSup);
        long duration = System.currentTimeMillis() - t1;
        double memUsage = MemoryLogger.getInstance().getMaxMemory();
        
        System.out.printf("Xong! (%,d tập đóng | %,d ms | %.2f MB)\n", res.size(), duration, memUsage);
        
        ResultWriter.exportLogCSV(miner.getName(), datasetPath, totalSize, genCount, 
                                minSup, res.size(), duration, memUsage);

        if (!res.isEmpty()) {
            ResultWriter.exportFormatResult(miner.getName(), datasetPath, genCount, 
                                        totalSize, minSup, res, idToItem, duration, memUsage);
        }
    }
}