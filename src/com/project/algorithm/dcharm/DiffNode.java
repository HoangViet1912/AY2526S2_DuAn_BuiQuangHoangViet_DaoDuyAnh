package com.project.algorithm.dcharm;

import java.util.Map;

public class DiffNode implements ITidNode {
    private final int[] items;
    private final double eSup;
    private final Map<Integer, Double> diffs;
    private final ITidNode parent;

    public DiffNode(int[] items, ITidNode parent, Map<Integer, Double> diffs, double eSup) {
        this.items = items;
        this.parent = parent;
        this.diffs = diffs;
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

    // TỐI ƯU: Thay vì đệ quy, ta tính toán xác suất dựa trên cha và độ lệch ngay lập tức
    @Override
    public double getProb(int tid) {
        return parent.getProb(tid) - diffs.getOrDefault(tid, 0.0);
    }

    @Override public Map<Integer, Double> getDiffMap() 
    {
         return diffs; 
    }
    public ITidNode getParent() 
    { 
        return parent; 
    }
}
