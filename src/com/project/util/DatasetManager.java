package com.project.util;

import com.project.model.Transaction;
import java.io.*;
import java.util.*;

public class DatasetManager {

    // Tạo tên file cache riêng
    private static String getCacheFileName(String targetFilePath) {
        String name = new File(targetFilePath).getName();
        return "cache" + File.separator + name.replace(".txt", "") + "_cache.dat";
    }

    /**
     * HÀM CHÍNH (Đã nâng cấp: Caching Thông minh dựa trên Dung lượng File)
     */
    public static List<Transaction> loadDataset(String txtPath, Map<Integer, String> idToItem, Map<String, Integer> itemToId) {
        String cachePath = getCacheFileName(txtPath);
        File txtFile = new File(txtPath);
        File cacheFile = new File(cachePath);

        // Lấy dung lượng file Text hiện tại (Dấu vân tay)
        long currentFileSize = txtFile.length();

        // 1. Kiểm tra Cache cũ
        if (cacheFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile)))) {
                // Đọc "Dấu vân tay" đã lưu trong Cache
                long cachedFileSize = ois.readLong();

                if (currentFileSize == cachedFileSize) {
                    System.out.println("⚡ [Cache Hit] Dữ liệu không đổi. Đang nạp siêu tốc từ Cache...");
                    
                    @SuppressWarnings("unchecked")
                    List<Transaction> dataset = (List<Transaction>) ois.readObject();
                    
                    @SuppressWarnings("unchecked")
                    Map<Integer, String> cachedIdToItem = (Map<Integer, String>) ois.readObject();
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> cachedItemToId = (Map<String, Integer>) ois.readObject();

                    idToItem.putAll(cachedIdToItem);
                    itemToId.putAll(cachedItemToId);
                    
                    return dataset;
                } else {
                    System.out.println("🔄 [Cache Stale] Phát hiện Dataset vừa phình to! Bỏ qua Cache cũ...");
                }
            } catch (Exception e) {
                System.err.println("⚠️ Cache bị lỗi định dạng, tiến hành nạp lại từ Text...");
            }
        }

        // 2. Nạp từ Text (Nếu chưa có Cache hoặc Cache đã cũ)
        System.out.println("⏳ [DatasetManager] Đang nạp và phân tích file Text...");
        List<Transaction> dataset = readFromText(txtPath, idToItem, itemToId);

        // 3. Tạo Cache mới toanh (Nhớ lưu kèm Dấu vân tay dung lượng)
        if (!dataset.isEmpty()) {
            buildAndCacheDataset(dataset, txtPath, idToItem, itemToId, currentFileSize);
        }

        return dataset;
    }

    // =========================================================
    // LOGIC ĐỌC TEXT VỚI BẢN VÁ TUPLE-LEVEL (Bảo vệ thuật toán)
    // =========================================================
    private static List<Transaction> readFromText(String path, Map<Integer, String> idToItem, Map<String, Integer> itemToId) {
        List<Transaction> db = new ArrayList<>();
        int tid = 1;
        int itemCounter = 1; 
        Random rand = new Random(42); 

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("@")) continue;

                Transaction t = new Transaction(tid++);
                
                // BẢN VÁ: 1 xác suất dùng chung cho cả dòng để Pruning hoạt động được
                double rowProb = 0.5 + (rand.nextDouble() * 0.5); 
                
                String[] tokens = line.split("\\s+");
                int startIndex = tokens[0].matches("T?\\d+:?") ? 1 : 0;

                for (int i = startIndex; i < tokens.length; i++) {
                    String token = tokens[i].replace(",", ""); 
                    String itemName = token;

                    // Lọc bỏ xác suất rác dính kèm
                    if (token.contains(":")) {
                        itemName = token.split(":")[0].trim();
                    } else if (token.contains("(")) {
                        itemName = token.substring(0, token.indexOf('(')).trim();
                    }

                    if (!itemName.isEmpty()) {
                        int itemId;
                        if (!itemToId.containsKey(itemName)) {
                            itemId = itemCounter++;
                            itemToId.put(itemName, itemId);
                            idToItem.put(itemId, itemName);
                        } else {
                            itemId = itemToId.get(itemName);
                        }
                        t.getProbs().put(itemId, rowProb);
                    }
                }

                if (!t.getProbs().isEmpty()) {
                    db.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi đọc file text: " + e.getMessage());
        }
        return db;
    }

    // =========================================================
    // CƠ CHẾ GHI CACHE KÈM DẤU VÂN TAY (SMART CACHING)
    // =========================================================
    private static void buildAndCacheDataset(List<Transaction> currentBatch, String targetFilePath, 
                                             Map<Integer, String> idToItem, Map<String, Integer> itemToId, 
                                             long currentFileSize) {
        String cacheFile = getCacheFileName(targetFilePath);
        File file = new File(cacheFile);
        
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); 
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            // LƯU DẤU VÂN TAY FILE VÀO ĐẦU CACHE
            oos.writeLong(currentFileSize); 
            
            // LƯU DATA & TỪ ĐIỂN
            oos.writeObject(new ArrayList<>(currentBatch));
            oos.writeObject(idToItem);
            oos.writeObject(itemToId);
            
            System.out.println("✨ [DatasetManager] Đã tạo Cache thông minh (Size: " + currentBatch.size() + " dòng)");
        } catch (Exception e) {
            System.err.println("❌ Lỗi ghi Cache: " + e.getMessage());
        }
    }
}
