package com.project.util;

import com.project.model.Transaction;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatasetManager {

    // Tạo tên file cache riêng: ví dụ dataset-chess_cache.dat
    private static String getCacheFileName(String targetFilePath) {
        String name = new File(targetFilePath).getName();
        return "cache" + File.separator + name.replace(".txt", "") + "_cache.dat";
    }

    /**
     * HÀM CHÍNH (Được gọi từ BenchmarkPipeline)
     * Tự động quyết định việc nạp từ Cache hay đọc từ Text.
     */
    public static List<Transaction> loadDataset(String txtPath, Map<Integer, String> idToItem, Map<String, Integer> itemToId) {
        String cacheFile = getCacheFileName(txtPath);
        File file = new File(cacheFile);

        // 1. Đã có Cache -> Bỏ qua đọc Text, nạp siêu tốc từ Cache
        if (file.exists()) {
            System.out.println("⚡ [DatasetManager] Đang nạp siêu tốc từ file Cache...");
            return loadCachedDataset(cacheFile); 
        }

        // 2. Chưa có Cache -> Đọc file Text bằng logic Regex
        System.out.println("⏳ [DatasetManager] Đọc file Text lần đầu và tạo Cache...");
        List<Transaction> dataset = readFromText(txtPath, idToItem, itemToId);

        // 3. Lưu xuống Cache ngay lập tức để những lần chạy sau không bị chậm
        if (!dataset.isEmpty()) {
            buildAndCacheDataset(dataset, txtPath);
        }

        return dataset;
    }

    // =========================================================
    // LOGIC CỦA DATALOADER CŨ (Giờ là hàm private ẩn bên trong)
    // =========================================================
    private static List<Transaction> readFromText(String path, Map<Integer, String> idToItem, Map<String, Integer> itemToId) {
        List<Transaction> db = new ArrayList<>();
        int tid = 1;
        int itemCounter = 1; 
        
        Pattern pattern = Pattern.compile("([^\\s,()]+)\\(([0-9.]+)\\)");

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) continue;

                String[] parts = line.split(":");
                String itemsPart = parts.length > 1 ? parts[1].trim() : parts[0].trim();

                Transaction t = new Transaction(tid++);
                Matcher matcher = pattern.matcher(itemsPart);

                while (matcher.find()) {
                    String itemName = matcher.group(1).trim();
                    double prob = Double.parseDouble(matcher.group(2));

                    int itemId;
                    if (!itemToId.containsKey(itemName)) {
                        itemId = itemCounter++;
                        itemToId.put(itemName, itemId);
                        idToItem.put(itemId, itemName);
                    } else {
                        itemId = itemToId.get(itemName);
                    }

                    t.getProbs().put(itemId, prob);
                }
                
                if (!t.getProbs().isEmpty()) {
                    db.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi đọc file Text: " + e.getMessage());
        }
        return db;
    }

    // =========================================================
    // CƠ CHẾ GHI/ĐỌC CACHE BẰNG SERIALIZATION CỦA BẠN (Giữ nguyên)
    // =========================================================
    private static void buildAndCacheDataset(List<Transaction> currentBatch, String targetFilePath) {
        String cacheFile = getCacheFileName(targetFilePath);
        File file = new File(cacheFile);
        
        // THÊM ĐOẠN NÀY: Kiểm tra và tạo thư mục "cache" nếu nó chưa tồn tại
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Tự động tạo folder
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            oos.writeObject(new ArrayList<>(currentBatch));
            System.out.println("✨ [DatasetManager] Đã tạo cache sạch tại: " + file.getAbsolutePath());
            System.out.println("✨ [DatasetManager] Tổng số giao dịch thực tế: " + currentBatch.size());
        } catch (Exception e) {
            System.err.println("❌ Lỗi ghi Cache: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Transaction> loadCachedDataset(String cacheFile) {
        File file = new File(cacheFile);
        if (!file.exists()) return new ArrayList<>();

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return (List<Transaction>) ois.readObject();
        } catch (Exception e) {
            System.err.println("❌ Lỗi nạp Cache: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}