package com.project.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Transaction implements Serializable {
    private final int tid;
    private final Map<Integer, Double> probs;
    private static final long serialVersionUID = 1L;

    public Transaction(int tid) {
        this.tid = tid;
        this.probs = new HashMap<>();
    }

    public int getTid() { return tid; }
    public Map<Integer, Double> getProbs() { return probs; }

    // ==========================================
    // OVERRIDE METHODS
    // ==========================================
    
    @Override
    public String toString() {
        return "Transaction{tid=" + tid + ", items=" + probs.size() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Trỏ cùng địa chỉ bộ nhớ -> giống nhau
        if (o == null || getClass() != o.getClass()) return false; // Khác kiểu -> khác nhau
        Transaction that = (Transaction) o;
        return tid == that.tid; // Hai giao dịch giống nhau nếu trùng TID
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(tid); // Băm theo TID để tối ưu khi đưa vào HashMap
    }
}