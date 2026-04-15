package com.project.algorithm.charm;

import com.project.algorithm.MiningAlgorithm;
import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MathUtils;
import com.project.util.MemoryLogger; // ĐÃ BỔ SUNG: Import bộ đo RAM

import java.util.*;

public class IncUncertainCharmMiner extends MiningAlgorithm {

    // BỘ NHỚ TRẠNG THÁI: Lưu giữ Tidset dọc để không phải đọc lại CSDL
    private final Map<Integer, Map<Integer, Double>> globalL1Tidset = new HashMap<>();
    private int totalTransactions = 0;

    @Override
    public String getName() { 
        return "Inc-CHARM (Vertical Incremental)"; 
    }

    @Override
    public List<Itemset> mine(List<Transaction> db, double minSup) {
        throw new UnsupportedOperationException("Lỗi: Hãy sử dụng hàm mineIncremental()");
    }
    @Override
    public List<Itemset> mineIncremental(List<Transaction> deltaDB, double minSupRatio) {
        closedFrequent.clear();
        totalTransactions += deltaDB.size();
        double minSupAbs = minSupRatio * totalTransactions;

        // 1. CẬP NHẬT TĂNG TIẾN: Chỉ duyệt qua Delta DB và nối vào Tidset gốc
        for (Transaction t : deltaDB) {
            for (Map.Entry<Integer, Double> e : t.getProbs().entrySet()) {
                globalL1Tidset.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                             .put(t.getTid(), e.getValue());
            }
        }

        // 2. KHỞI TẠO SINGLETONS
        List<UList> singletons = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, Double>> e : globalL1Tidset.entrySet()) {
            double esup = 0;
            for (double p : e.getValue().values()) esup += p;
            
            if (esup >= minSupAbs - 1e-7) {
                singletons.add(new UList(new int[]{e.getKey()}, e.getValue(), esup));
            }
        }
        
        // Zaki Heuristic: Ascending Support Ordering (Rất chuẩn bài báo)
        singletons.sort(Comparator.comparingDouble(a -> a.getEsup()));
        
        mineDFS(singletons, minSupAbs);
        
        return filterClosedFast(closedFrequent);
    }

    private void mineDFS(List<UList> nodes, double minSupAbs) {
        // ĐÃ BỔ SUNG: Bắt đỉnh Peak Memory tại mỗi tầng đệ quy
        MemoryLogger.getInstance().checkMemory(); 

        for (int i = 0; i < nodes.size(); i++) {
            UList xi = nodes.get(i);
            List<UList> nextLevelNodes = new ArrayList<>();

            for (int j = i + 1; j < nodes.size(); j++) {
                UList xj = nodes.get(j);
                UList xij = joinNodes(xi, xj); 

                if (xij.getEsup() >= minSupAbs - 1e-7) {
                    boolean xiEqual = Math.abs(xi.getEsup() - xij.getEsup()) < 1e-7;
                    boolean xjEqual = Math.abs(xj.getEsup() - xij.getEsup()) < 1e-7;

                    // 4 Thuộc tính cắt tỉa của CHARM gốc (Áp dụng hoàn hảo cho Uncertain Data)
                    if (xiEqual && xjEqual) {
                        replaceNode(xi, xij); 
                    } else if (xiEqual) {
                        replaceNode(xi, xij); 
                    } else if (xjEqual) {
                        nodes.remove(j); j--;
                        nextLevelNodes.add(xij); 
                    } else {
                        nextLevelNodes.add(xij); 
                    }
                }
            }

            if (!isSubsumed(xi)) {
                if (!nextLevelNodes.isEmpty()) {
                    mineDFS(nextLevelNodes, minSupAbs);
                }
                closedFrequent.add(new Itemset(xi.getItems(), xi.getEsup())); 
            }
        }
    }

    private UList joinNodes(UList a, UList b) {
        int[] aItems = a.getItems();
        int[] bItems = b.getItems();
        int[] newItems = Arrays.copyOf(aItems, aItems.length + 1);
        int lastItem = bItems[bItems.length - 1];
        newItems[aItems.length] = lastItem;

        Map<Integer, Double> newTidProb = new HashMap<>();
        double newEsup = 0.0;
        Map<Integer, Double> probY = globalL1Tidset.get(lastItem);

        if (probY != null) {
            for (Map.Entry<Integer, Double> entry : a.getTidProb().entrySet()) {
                Double py = probY.get(entry.getKey());
                if (py != null) {
                    double pxy = entry.getValue() * py; 
                    if (pxy > 1e-9) { 
                        newTidProb.put(entry.getKey(), pxy);
                        newEsup += pxy;
                    }
                }
            }
        }
        return new UList(newItems, newTidProb, newEsup);
    }

    private void replaceNode(UList oldNode, UList newNode) {
        oldNode.setItems(newNode.getItems());
        oldNode.setTidProb(newNode.getTidProb());
        oldNode.setEsup(newNode.getEsup());
    }

    private boolean isSubsumed(UList node) {
        // Fast Subsumption Checking kết hợp Expected Support Tolerance
        for (Itemset c : closedFrequent) {
            if (c.getItems().length >= node.getItems().length) {
                if (Math.abs(c.getEsup() - node.getEsup()) < 1e-7) {
                    if (MathUtils.isSubsetSorted(node.getItems(), c.getItems())) return true;
                }
            }
        }
        return false;
    }
}