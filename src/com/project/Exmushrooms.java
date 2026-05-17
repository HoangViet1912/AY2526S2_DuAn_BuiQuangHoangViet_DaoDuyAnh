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
 * HỆ THỐNG BENCHMARK TẬP TRUNG MUSHROOMS - NGƯỠNG THẤP (0.1 - 0.3)
 */
public class Exmushrooms {

    // Trỏ vào dataset Mushrooms
    private static final String SOURCE_MUSHROOMS = "datasets/dataset-mushrooms.txt";
    private static final String DYNAMIC_MUSHROOMS = "datasets/dataset-mushrooms_dynamic.txt";

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println(" 🚀 START MUSHROOMS LOW-THRESHOLD BENCHMARK (0.1 -> 0.3)");
        System.out.println("=========================================================================\n");

        try {
            prepareWorkspace();
            runMushroomsBenchmark();
            
            System.out.println("\n=========================================================================");
            System.out.println(" ✨ HOÀN TẤT! Kết quả lưu tại: 'experiment.csv'");
            System.out.println("=========================================================================");

        } catch (Exception e) {
            System.err.println("❌ Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void prepareWorkspace() throws Exception {
        System.out.println("[⚙️] Thiết lập Workspace cho Mushrooms...");
        File sourceFile = new File(SOURCE_MUSHROOMS);
        File dynamicFile = new File(DYNAMIC_MUSHROOMS);

        if (!sourceFile.exists()) {
            throw new Exception("Không tìm thấy file gốc: " + SOURCE_MUSHROOMS);
        }

        // Tạo file dynamic nếu chưa có, hoặc giữ lại để tích lũy
        if (!dynamicFile.exists()) {
            Files.copy(Paths.get(SOURCE_MUSHROOMS), Paths.get(DYNAMIC_MUSHROOMS));
            System.out.println("   -> Khởi tạo file dynamic mới.");
        } else {
            System.out.println("   -> Tiếp tục sử dụng file dynamic hiện có.");
        }
    }

    private static void runMushroomsBenchmark() throws Exception {
        String currentDataset = DYNAMIC_MUSHROOMS;
        
        // Cập nhật dải ngưỡng theo yêu cầu: 0.1 đến 0.3
        double[] mushroomsMinSups = { 0.1, 0.2, 0.3, 0.4, 0.5 };
        // Giữ nguyên chu kỳ GEN cũ
        int[] genCounts = { 0, 500, 1000, 2000, 5000 };

        Map<Integer, String> idToItem = new HashMap<>();
        Map<String, Integer> itemToId = new HashMap<>();

        System.out.println("[*] Đang nạp CSDL Mushrooms...");
        List<Transaction> baseDB = DatasetManager.loadDataset(currentDataset, idToItem, itemToId);

        boolean isFirstMinSup = true;

        for (double minSup : mushroomsMinSups) {
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
                    modeName = "BASE DB";
                    chunkToMine = baseDB;
                    currentTotalDBSize = baseDB.size();
                } else {
                    modeName = "INCREMENTAL (+" + gen + ")";
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
                    
                    if (isFirstMinSup) {
                        ResultWriter.appendDeltaToDataset(chunkToMine, idToItem, currentDataset, gen);
                    }
                }

                System.out.println("\n>>> " + modeName + " | Tổng: " + currentTotalDBSize);

                // Thứ tự chạy: FP-Close -> Charm -> dCharm
                runMinerAndLog(incFP, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
                runMinerAndLog(incCharm, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
                runMinerAndLog(incDCharm, chunkToMine, minSup, currentDataset, currentTotalDBSize, gen, idToItem);
            }
            isFirstMinSup = false; 
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