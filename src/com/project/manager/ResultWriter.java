package com.project.manager; // ĐÃ ĐỔI THÀNH MANAGER

import com.project.model.Itemset;
import com.project.model.Transaction;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResultWriter {

    // Ghi log hiệu năng vào CSV
    public static void exportLogCSV(String algoName, String dataset, int totalSize, int genCount, double sup, int resultCount, long time, double maxMemory) {
        String csvFile = "performance_log.csv";
        File file = new File(csvFile);
        boolean isNew = !file.exists();

        try (PrintWriter pw = new PrintWriter(new FileWriter(csvFile, true))) {
            if (isNew) {
                pw.println("Algorithm,Dataset,TotalTransactions,GEN,MinSup,FoundItems,Runtime(ms),PeakMemory(MB)");
            }
            pw.printf(Locale.US, "%s,%s,%d,%d,%.4f,%d,%d,%.2f\n", 
                      algoName, dataset, totalSize, genCount, sup, resultCount, time, maxMemory);
            System.out.println("   [CSV] Đã cập nhật log: performance_log.csv");
        } catch (IOException e) {
            System.err.println("❌ Lỗi ghi file CSV: " + e.getMessage());
        }
    }

    // Ghi chi tiết CFI ra file TXT
    public static void exportFormatResult(String algoName, String datasetName, int genCount, int totalDbSize, 
                                          double rawSup, List<Itemset> res, Map<Integer, String> idToItem, 
                                          long time, double maxMemory) {
        
            String algoFolder = algoName.toLowerCase().split(" ")[0].replace("-", "_"); 
            File dir = new File("results/" + algoFolder);
            if (!dir.exists()) dir.mkdirs(); // Tự động tạo thư mục results/ và thư mục con
            
            // Tên file: result-chess-sup0.2.txt
            String fileName = "results/" + algoFolder + "/result-" + new File(datasetName).getName().replace(".txt", "") + "-sup" + rawSup + ".txt";
            
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                pw.println("Algorithm: " + algoName);
                pw.println("Dataset: " + datasetName);
                pw.println("Total Transactions: " + totalDbSize + " (Added: " + genCount + ")");
                pw.println("Found CFIs: " + res.size());
                pw.println("---------------------------------------------------");
                
                for (Itemset is : res) {
                    pw.println(Arrays.toString(is.getItems()) + " - Support: " + is.getEsup());
                }
                System.out.println("   [+] Đã xuất CFI chi tiết ra: " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    // Ghi đè tích lũy vào chính file dataset đang dùng
    /**
     * Hàm 2: Cập nhật trực tiếp dữ liệu vào file vật lý.
     * Đã sửa: Thêm tham số genCount để khớp với lời gọi từ BenchmarkPipeline
     */
    public static void exportAccumulatedDB(List<Transaction> db, Map<Integer, String> idToItem, String targetFilePath, int genCount) {
        // Nếu genCount <= 0 (không sinh thêm gì) thì có thể bỏ qua không cần ghi đè file cho đỡ tốn I/O
        if (db == null || db.isEmpty() || genCount < 0) return;

        try (PrintWriter pw = new PrintWriter(new FileWriter(targetFilePath))) {
            for (Transaction t : db) {
                StringBuilder sb = new StringBuilder("T" + t.getTid() + ": ");
                boolean first = true;
                for (Map.Entry<Integer, Double> entry : t.getProbs().entrySet()) {
                    if (!first) sb.append(", ");
                    
                    // Lấy tên item từ Map, nếu không có thì lấy ID
                    String itemName = idToItem.getOrDefault(entry.getKey(), String.valueOf(entry.getKey()));
                    sb.append(itemName).append("(").append(String.format(Locale.US, "%.2f", entry.getValue())).append(")");
                    first = false;
                }
                pw.println(sb.toString());
            }
            System.out.println("   [💾] Đã tích lũy " + genCount + " dòng mới vào: " + targetFilePath);
        } catch (IOException e) {
            System.err.println("❌ Lỗi cập nhật file dữ liệu: " + e.getMessage());
        }
    }
}