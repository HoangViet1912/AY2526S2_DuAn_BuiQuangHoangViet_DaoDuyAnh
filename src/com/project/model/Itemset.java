package com.project.model;

import java.io.Serializable;
import java.util.Arrays;

// Bổ sung implements Serializable để không bị lỗi khi lưu file .dat
public class Itemset implements Serializable {
    
    private static final long serialVersionUID = 1L; // Cấp mã định danh an toàn cho class

    private final int[] items;
    private final double eSup;

    public Itemset(int[] items, double eSup) {
        this.items = items;
        this.eSup = eSup;
    }

    public int[] getItems() { return items; }
    
    public double getEsup() { return eSup; }

    // ==========================================
    // OVERRIDE METHODS
    // ==========================================

    @Override
    public String toString() {
        return "Itemset{items=" + Arrays.toString(items) + ", eSup=" + String.format("%.2f", eSup) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Itemset itemset = (Itemset) o;
        // Hai tập hợp được coi là giống nhau TẤT CẢ item bên trong phải giống hệt nhau
        return Arrays.equals(items, itemset.items); 
    }

    @Override
    public int hashCode() {
        // Thuật toán băm mảng cực nhanh có sẵn của Java
        return Arrays.hashCode(items); 
    }
}
