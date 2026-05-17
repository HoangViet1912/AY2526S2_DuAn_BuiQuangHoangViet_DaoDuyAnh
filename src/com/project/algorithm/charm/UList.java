package com.project.algorithm.charm;

import java.util.Map;

class UList {
    private int[] items;
    private Map<Integer, Double> tidProb;
    private double eSup;
    private long tidSum;

    // Constructor
    public UList(int[] items, Map<Integer, Double> tidProb, double eSup) {
        this.items = items;
        this.tidProb = tidProb;
        this.eSup = eSup;
        this.tidSum = 0;
        for (Integer tid : tidProb.keySet()) {
            this.tidSum += tid;
        }
    }

    // --- CÁC HÀM GETTER (Để lấy dữ liệu) ---
    public int[] getItems() {
        return items;
    }

    public Map<Integer, Double> getTidProb() {
        return tidProb;
    }

    public double getEsup() {
        return eSup;
    }
    public long getTidSum() { 
        return tidSum; 
    } // Getter mới

    // --- CÁC HÀM SETTER (Để cập nhật dữ liệu cho hàm replaceNode) ---
    public void setItems(int[] items) {
        this.items = items;
    }

    public void setTidProb(Map<Integer, Double> tidProb) {
        this.tidProb = tidProb;
        this.tidSum = 0;
        for (Integer tid : tidProb.keySet()) 
            this.tidSum += tid;
    }

    public void setEsup(double eSup) {
        this.eSup = eSup;
    }
}