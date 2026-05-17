package com.project.algorithm.fpclose;

/**
 * Lớp đại diện cho một giao dịch đã được chiếu (Projected Transaction).
 * Lưu trữ tham chiếu đến giao dịch gốc và xác suất lũy kế của tiền tố.
 */
public class PrefixTx {
    private final int tid;        // ID của giao dịch trong bộ nhớ toàn cục
    private final double pathProb; // Xác suất tích lũy (Path Probability)

    public PrefixTx(int tid, double pathProb) {
        this.tid = tid;
        this.pathProb = pathProb;
    }

    public int getTid() 
    {
        return tid; 
    }
    public double getPathProb() 
    { 
        return pathProb; 
    }
}
