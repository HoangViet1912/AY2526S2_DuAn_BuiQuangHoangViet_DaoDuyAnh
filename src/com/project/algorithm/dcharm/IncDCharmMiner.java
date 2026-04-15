package com.project.algorithm.dcharm;

import com.project.algorithm.MiningAlgorithm;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MathUtils;
import com.project.util.MemoryLogger;
import java.util.*;

public class IncDCharmMiner extends MiningAlgorithm {

    private final Map<Integer, Map<Integer, Double>> globalL1Tidset = new HashMap<>();
    private int totalTransactions = 0;

    @Override
    public String getName() { return "Inc-dCharm (True OOP & Math-Optimized)"; }

    @Override
    public List<Itemset> mine(List<Transaction> db, double minSup) {
    // 1. Reset lại các biến trạng thái để đảm bảo khai phá mới hoàn toàn
        globalL1Tidset.clear();
        totalTransactions = 0;
        
        // 2. Chuyển đổi minSup (số nguyên) thành ratio (tỷ lệ) nếu cần, 
        // hoặc gọi thẳng mineIncremental nếu logic của bạn khớp.
        // Ở đây ta giả định mineIncremental nhận vào tỷ lệ (0.0 -> 1.0)
        double minSupRatio = minSup / (double) db.size();
        

        return mineIncremental(db, minSupRatio);
    }
   

    @Override
    public List<Itemset> mineIncremental(List<Transaction> deltaDB, double minSupRatio) {
        closedFrequent.clear();
        totalTransactions += deltaDB.size();
        double minSupAbs = minSupRatio * totalTransactions;

        // 1. CẬP NHẬT TĂNG TIẾN (Giữ nguyên vì đã tối ưu I/O)
        for (Transaction t : deltaDB) {
            for (Map.Entry<Integer, Double> e : t.getProbs().entrySet()) {
                globalL1Tidset.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                             .put(t.getTid(), e.getValue());
            }
        }

        // 2. KHỞI TẠO SINGLETONS (Tầng 1)
        List<ITidNode> singletons = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Double>> e : globalL1Tidset.entrySet()) {
            double esup = 0;
            for (double p : e.getValue().values()) esup += p;
            
            if (MathUtils.greaterThanOrEqual(esup, minSupAbs)) {
                singletons.add(new TidNode(new int[]{e.getKey()}, e.getValue(), esup));
            }
        }
        
        singletons.sort(Comparator.comparingDouble(ITidNode::getEsup));
        mineDFS(singletons, null, minSupAbs);
        
        return closedFrequent; 
    }

    private void mineDFS(List<ITidNode> nodes, ITidNode parent, double minSupAbs) {
        MemoryLogger.getInstance().checkMemory(); 
        
        for (int i = 0; i < nodes.size(); i++) {
            ITidNode xi = nodes.get(i);
            List<ITidNode> nextNodes = new ArrayList<>();
            
            for (int j = i + 1; j < nodes.size(); j++) {
                ITidNode xj = nodes.get(j);
                
                Map<Integer, Double> diffXY = new HashMap<>();
                double diffEsup = 0.0;
                
                // --- PHÉP TOÁN JOIN DIFFSET TỐI ƯU ---
                if (parent == null) {
                    // TẦNG 1: Duyệt Tidset của xi, so khớp với xj
                    for (Map.Entry<Integer, Double> e : xi.getDiffMap().entrySet()) {
                        int tid = e.getKey();
                        double px = e.getValue();
                        double py = xj.getProb(tid); 
                        
                        // Hao hụt khi kết hợp X và Y: P(X) * (1 - P(Y))
                        double diff = px * (1.0 - py); 
                        if (diff > MathUtils.MIN_PROBABILITY) { 
                            diffXY.put(tid, diff);
                            diffEsup += diff; 
                        }
                    }
                } else {
                    // TẦNG SÂU: Chỉ duyệt trên mảng Diff của xj (Cực kỳ nhỏ -> TỐC ĐỘ GẤP 10 LẦN)
                    for (Map.Entry<Integer, Double> e : xj.getDiffMap().entrySet()) {
                        int tid = e.getKey();
                        double diffPy = e.getValue(); 
                        double pParent = parent.getProb(tid);
                        
                        if (pParent > MathUtils.MIN_PROBABILITY) {
                            // Công thức dCharm Uncertain: Diff_New = Diff_Y * (P(X) / P(Parent))
                            double diffNew = diffPy * (xi.getProb(tid) / pParent);
                            if (diffNew > MathUtils.MIN_PROBABILITY) {
                                diffXY.put(tid, diffNew);
                                diffEsup += diffNew;
                            }
                        }
                    }
                }
                
                double exactEsupXY = xi.getEsup() - diffEsup;

                if (MathUtils.greaterThanOrEqual(exactEsupXY, minSupAbs)) {
                    int[] newItems = combine(xi.getItems(), xj.getItems());
                    DiffNode xij = new DiffNode(newItems, xi, diffXY, exactEsupXY);

                    // --- 4 THUỘC TÍNH CẮT TỈA CHARM ---
                    if (MathUtils.equals(diffEsup, 0) && MathUtils.equals(xj.getEsup(), exactEsupXY)) {
                        nodes.remove(j); j--; 
                        nodes.set(i, xij); xi = xij;
                    } else if (MathUtils.equals(diffEsup, 0)) {
                        nodes.set(i, xij); xi = xij;
                    } else if (MathUtils.equals(xj.getEsup(), exactEsupXY)) {
                        nodes.remove(j); j--; 
                        nextNodes.add(xij);
                    } else {
                        nextNodes.add(xij);
                    }
                }
            }

            if (!isSubsumed(xi)) {
                if (!nextNodes.isEmpty()) mineDFS(nextNodes, xi, minSupAbs);
                closedFrequent.add(new Itemset(xi.getItems(), xi.getEsup()));
            }
        }
    }

    private int[] combine(int[] a, int[] b) {
        int[] res = Arrays.copyOf(a, a.length + 1);
        res[a.length] = b[b.length - 1];
        return res;
    }

    private boolean isSubsumed(ITidNode node) {
        for (Itemset c : closedFrequent) {
            if (c.getItems().length >= node.getItems().length) {
                if (MathUtils.equals(c.getEsup(), node.getEsup())) {
                    if (MathUtils.isSubsetSorted(node.getItems(), c.getItems())) return true;
                }
            }
        }
        return false;
    }
}