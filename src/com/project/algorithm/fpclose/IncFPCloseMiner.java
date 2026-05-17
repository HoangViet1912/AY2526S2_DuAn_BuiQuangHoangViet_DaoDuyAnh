package com.project.algorithm.fpclose;

import com.project.algorithm.MiningAlgorithm;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MathUtils; // Import bộ công cụ toán học
import com.project.util.MemoryLogger;
import java.util.*;

/**
 * INC-FP-CLOSE (CLEAN OOP VERSION)
 * Giải pháp khai phá tập đóng tích lũy trên dữ liệu bất định.
 */
public class IncFPCloseMiner extends MiningAlgorithm {

    // --- BỘ NHỚ TRẠNG THÁI (Mảng tích lũy và Cửa sổ trượt) ---
    private final Map<Integer, Transaction> globalDBMap = new LinkedHashMap<>(); 
    private final int MAX_WINDOW_SIZE = 150000; 
    private double[] globalItemCounts = new double[10000]; 
    private int totalTransactions = 0;
    private int maxItemId = 0;

    private CFITree cfiTree; // Sử dụng lớp CFITree đã tách riêng

    @Override
    public String getName() {
        return "Inc-FP-Close ";
    }

    @Override
    public List<Itemset> mine(List<Transaction> db, double minSup) {
        // Reset trạng thái để chạy mới hoàn toàn
        globalDBMap.clear();
        globalItemCounts = new double[10000];
        totalTransactions = 0;
        return mineIncremental(db, minSup);
    }

    @Override
    public List<Itemset> mineIncremental(List<Transaction> deltaDB, double minSupRatio) {
        closedFrequent.clear();
        cfiTree = new CFITree(); // Khởi tạo cây Closure mới cho đợt mining này
        
        // 1. CẬP NHẬT CỬA SỔ TRƯỢT
        for (Transaction t : deltaDB) {
            if (globalDBMap.size() >= MAX_WINDOW_SIZE) {
                Iterator<Integer> it = globalDBMap.keySet().iterator();
                if (it.hasNext()) {
                    int oldestTid = it.next();
                    Transaction oldTx = globalDBMap.remove(oldestTid);
                    
                    // Trừ đi Support kỳ vọng của giao dịch bị đào thải
                    for (Map.Entry<Integer, Double> e : oldTx.getProbs().entrySet()) {
                        int item = e.getKey();
                        globalItemCounts[item] = Math.max(0.0, globalItemCounts[item] - e.getValue());
                    }
                    totalTransactions--; 
                }
            }
            
            globalDBMap.put(t.getTid(), t);
            for (Map.Entry<Integer, Double> e : t.getProbs().entrySet()) {
                int item = e.getKey();
                if (item >= globalItemCounts.length) {
                    globalItemCounts = Arrays.copyOf(globalItemCounts, item + 5000);
                }
                globalItemCounts[item] += e.getValue();
                if (item > maxItemId) maxItemId = item;
            }
        }
        
        totalTransactions += deltaDB.size();
        double minSupAbs = minSupRatio * globalDBMap.size(); // minSup dựa trên số lượng thực tế trong Window

        // 2. CHUẨN BỊ DATABASE CHIẾU BAN ĐẦU
        List<PrefixTx> currentDB = new ArrayList<>();
        for (Integer tid : globalDBMap.keySet()) {
            currentDB.add(new PrefixTx(tid, 1.0));
        }

        // 3. THỰC THI KHAI PHÁ (PATTERN GROWTH)
        minePatternGrowth(currentDB, new int[0], minSupAbs);

        return closedFrequent;
    }

    private void minePatternGrowth(List<PrefixTx> conditionalDB, int[] prefix, double minSupAbs) {
        MemoryLogger.getInstance().checkMemory();
        int lastItemInPrefix = prefix.length > 0 ? prefix[prefix.length - 1] : -1;
        
        // Tính Expected Support cục bộ
        Map<Integer, Double> localItemCounts = new HashMap<>();
        for (PrefixTx pt : conditionalDB) {
            Transaction original = globalDBMap.get(pt.getTid()); // Dùng Getter của PrefixTx
            if (original == null) continue;
            for (Map.Entry<Integer, Double> e : original.getProbs().entrySet()) {
                int item = e.getKey();
                if (item > lastItemInPrefix) {
                    localItemCounts.put(item, localItemCounts.getOrDefault(item, 0.0) + pt.getPathProb() * e.getValue());
                }
            }
        }

        // Lọc vật phẩm phổ biến dùng MathUtils
        List<Integer> validItems = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : localItemCounts.entrySet()) {
            double sup = (prefix.length == 0) ? globalItemCounts[entry.getKey()] : entry.getValue();
            if (MathUtils.greaterThanOrEqual(sup, minSupAbs)) {
                validItems.add(entry.getKey());
            }
        }

        // Sắp xếp theo tần suất giảm dần
        validItems.sort((a, b) -> {
            double sA = (prefix.length == 0) ? globalItemCounts[a] : localItemCounts.get(a);
            double sB = (prefix.length == 0) ? globalItemCounts[b] : localItemCounts.get(b);
            return Double.compare(sB, sA);
        });

        for (int item : validItems) {
            double support = (prefix.length == 0) ? globalItemCounts[item] : localItemCounts.get(item);
            int[] newPrefix = combine(prefix, item);

            // KIỂM TRA TÍNH ĐÓNG (Sử dụng CFITree đã tách file)
            if (!cfiTree.isSubsumed(newPrefix, support)) {
                cfiTree.insert(newPrefix, support);
                closedFrequent.add(new Itemset(newPrefix, support));

                // Tạo Projected Database cho đệ quy
                List<PrefixTx> projectedDB = new ArrayList<>();
                for (PrefixTx pt : conditionalDB) {
                    Transaction tx = globalDBMap.get(pt.getTid());
                    if (tx != null && tx.getProbs().containsKey(item)) {
                        double newProb = pt.getPathProb() * tx.getProbs().get(item);
                        // Lọc nhiễu bằng MathUtils
                        if (newProb > MathUtils.MIN_PROBABILITY) {
                            projectedDB.add(new PrefixTx(pt.getTid(), newProb));
                        }
                    }
                }
                
                if (!projectedDB.isEmpty()) {
                    minePatternGrowth(projectedDB, newPrefix, minSupAbs);
                }
            }
        }
    }

    // Hàm tiện ích hỗ trợ tạo prefix mới
    private int[] combine(int[] prefix, int item) {
        int[] res = Arrays.copyOf(prefix, prefix.length + 1);
        res[prefix.length] = item;
        Arrays.sort(res);
        return res;
    }
}