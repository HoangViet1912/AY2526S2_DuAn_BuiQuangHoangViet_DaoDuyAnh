package com.project.algorithm.dcharm;

import java.util.Map;

/**
 * Interface định nghĩa cấu trúc của một nút trong Vertical Mining.
 */
public interface ITidNode {
    int[] getItems();
    double getEsup();
    double getProb(int tid);
    Map<Integer, Double> getDiffMap();
}

/**
 * Lớp triển khai cho nút Tầng 1 (Lưu trữ Tidset gốc).
 * Để phạm vi là package-private (không có chữ public) để dùng chung trong folder dcharm.
 */
class TidNode implements ITidNode {
    private final int[] items;
    private final double eSup;
    private final Map<Integer, Double> probs;

    TidNode(int[] items, Map<Integer, Double> probs, double eSup) {
        this.items = items;
        this.probs = probs;
        this.eSup = eSup;
    }

    @Override public int[] getItems() 
    { 
        return items; 
    }

    @Override public double getEsup() 
    { 
        return eSup; 
    }
    
    @Override 
    public double getProb(int tid) { 
        return probs.getOrDefault(tid, 0.0); 
    }
    
    @Override public Map<Integer, Double> getDiffMap() 
    { 
        return probs; 
    }
}