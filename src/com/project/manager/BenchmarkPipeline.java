package com.project.manager; // Cùng package

import com.project.algorithm.charm.IncUncertainCharmMiner;
import com.project.algorithm.dcharm.IncDCharmMiner;
import com.project.algorithm.fpclose.IncFPCloseMiner;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MemoryLogger;
import java.util.*;

public class BenchmarkPipeline {
    
    public void runIncrementalRace(List<Transaction> baseDB, String targetFilePath, int genCount, 
                                   double minSupRelative, int algoChoice, Map<Integer, String> idToItem) throws Exception {
        
        System.out.println("\n--- PHASE 1: KHỞI TẠO NỀN MÓNG (BASE DB: " + baseDB.size() + " Giao dịch) ---");
        
        // Khởi tạo các đối tượng TRƯỚC các khối IF để giữ trạng thái tích lũy
        IncUncertainCharmMiner incCharm = new IncUncertainCharmMiner();
        IncDCharmMiner incDCharm = new IncDCharmMiner();
        IncFPCloseMiner incFP = new IncFPCloseMiner();
        
        // Nạp CSDL gốc vào bộ nhớ của từng thuật toán
        if (algoChoice == 1 || algoChoice == 4) {
            System.out.println(">> Đang nạp CSDL gốc cho Inc-CHARM...");
            incCharm.mineIncremental(baseDB, minSupRelative);
        }
        if (algoChoice == 2 || algoChoice == 4) {
            System.out.println(">> Đang nạp CSDL gốc cho Inc-dCharm...");
            incDCharm.mineIncremental(baseDB, minSupRelative);
        }
        if (algoChoice == 3 || algoChoice == 4) {
            System.out.println(">> Đang nạp CSDL gốc cho Inc-FP-Close...");
            incFP.mineIncremental(baseDB, minSupRelative);
        }

        // Tạo dữ liệu mới (Mô phỏng Luồng giao thông)
        System.out.println("\n[Mô phỏng] Hệ thống đang nhận luồng dữ liệu mới gồm " + genCount + " giao dịch...");
        List<Transaction> deltaDB = new ArrayList<>();
        Random rand = new Random(); // Sử dụng random thực tế thay vì fix seed để test độ nhạy
        
        // Lấy TID lớn nhất hiện tại để đảm bảo tính duy nhất khi sinh dữ liệu mới
        int maxTid = baseDB.isEmpty() ? 0 : baseDB.get(baseDB.size() - 1).getTid();

        for (int i = 0; i < genCount; i++) {
            Transaction randomTx = baseDB.get(rand.nextInt(baseDB.size()));
            Transaction newTx = new Transaction(maxTid + i + 1); // Đảm bảo TID tăng dần
            newTx.getProbs().putAll(randomTx.getProbs());
            deltaDB.add(newTx);
        }

        // Tạo bản copy của CSDL đã tích lũy để xuất file báo cáo cuối cùng
        List<Transaction> fullDB = new ArrayList<>(baseDB);
        fullDB.addAll(deltaDB);

        // ====================================================================
        // ĐUA PHASE 2: XỬ LÝ DỮ LIỆU ĐỘNG (CHỈ NẠP PHẦN DELTA)
        // ====================================================================
        System.out.println("\n--- PHASE 2: CUỘC ĐUA TỐC ĐỘ TĂNG TIẾN (Tổng DB: " + fullDB.size() + ") ---");
        
        List<Itemset> finalResultToExport = null;
        double finalMem = 0;
        long finalTime = 0;
        String finalAlgoName = "";

        // Chạy Inc-CHARM
        if (algoChoice == 1 || algoChoice == 4) {
            System.out.print(">> " + incCharm.getName() + " ... ");
            System.gc(); // Ép dọn rác để đo RAM chính xác
            MemoryLogger.getInstance().reset();
            long startTime = System.currentTimeMillis();
            
            // QUAN TRỌNG: Chỉ truyền deltaDB vì thuật toán đã nhớ baseDB
            List<Itemset> res1 = incCharm.mineIncremental(deltaDB, minSupRelative);
            
            long duration = System.currentTimeMillis() - startTime;
            double memUsage = MemoryLogger.getInstance().getMaxMemory();
            System.out.printf("Xong! (%,d tập đóng | %,d ms | %.2f MB)\n", res1.size(), duration, memUsage);
            
            ResultWriter.exportLogCSV(incCharm.getName(), targetFilePath, fullDB.size(), genCount, minSupRelative, res1.size(), duration, memUsage);
            finalResultToExport = res1; finalMem = memUsage; finalTime = duration; finalAlgoName = incCharm.getName();
        }

        // Chạy Inc-dCharm
        if (algoChoice == 2 || algoChoice == 4) {
            System.out.print(">> " + incDCharm.getName() + " ... ");
            System.gc(); 
            MemoryLogger.getInstance().reset(); 
            long startTime = System.currentTimeMillis();
            
            List<Itemset> res2 = incDCharm.mineIncremental(deltaDB, minSupRelative);
            
            long duration = System.currentTimeMillis() - startTime;
            double memUsage = MemoryLogger.getInstance().getMaxMemory();
            System.out.printf("Xong! (%,d tập đóng | %,d ms | %.2f MB)\n", res2.size(), duration, memUsage);
            
            ResultWriter.exportLogCSV(incDCharm.getName(), targetFilePath, fullDB.size(), genCount, minSupRelative, res2.size(), duration, memUsage);
            finalResultToExport = res2; finalMem = memUsage; finalTime = duration; finalAlgoName = incDCharm.getName();
        }

        // Chạy Inc-FP-Close
        if (algoChoice == 3 || algoChoice == 4) {
            System.out.print(">> " + incFP.getName() + " ... ");
            System.gc(); 
            MemoryLogger.getInstance().reset(); 
            long startTime = System.currentTimeMillis();
            
            List<Itemset> res3 = incFP.mineIncremental(deltaDB, minSupRelative);
            
            long duration = System.currentTimeMillis() - startTime;
            double memUsage = MemoryLogger.getInstance().getMaxMemory();
            System.out.printf("Xong! (%,d tập đóng | %,d ms | %.2f MB)\n", res3.size(), duration, memUsage);
            
            ResultWriter.exportLogCSV(incFP.getName(), targetFilePath, fullDB.size(), genCount, minSupRelative, res3.size(), duration, memUsage);
            finalResultToExport = res3; finalMem = memUsage; finalTime = duration; finalAlgoName = incFP.getName();
        }
        
        // XUẤT FILE BÁO CÁO CUỐI CÙNG
        System.out.println("\n--- XUẤT BÁO CÁO THỰC NGHIỆM ---");
        if (finalResultToExport != null) {
            // Xuất danh sách CFI tìm được
            ResultWriter.exportFormatResult(finalAlgoName, targetFilePath, genCount, fullDB.size(), minSupRelative, finalResultToExport, idToItem, finalTime, finalMem);
        }
        
        // Cập nhật lại file dataset vật lý (ghi nối dữ liệu vừa sinh vào file gốc hoặc file tích lũy)
        ResultWriter.exportAccumulatedDB(fullDB, idToItem, targetFilePath, genCount);
        
        System.out.println("✅ HOÀN TẤT KỊCH BẢN THỰC NGHIỆM!");
    }
}
