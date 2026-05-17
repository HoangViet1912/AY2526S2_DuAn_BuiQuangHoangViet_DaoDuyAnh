package com.project.util;

/**
 * Lớp tiện ích lõi (Core Utility Class) xử lý các phép toán và so sánh sai số.
 * Đảm bảo tính đồng nhất toán học cho toàn bộ hệ thống khai phá dữ liệu bất định.
 */
public final class MathUtils {

    /** * Ngưỡng sai số (Epsilon) dùng để SO SÁNH hai số thực.
     * Giúp khắc phục lỗi làm tròn của kiểu dữ liệu double.
     */
    public static final double EPSILON = 1e-8; 

    /** * Ngưỡng cắt tỉa xác suất nhiễu (Noise Filter).
     * Loại bỏ các giá trị xác suất quá nhỏ (sát số 0) để tiết kiệm RAM và tránh Underflow.
     */
    public static final double MIN_PROBABILITY = 1e-9; 

    /**
     * Chặn khởi tạo đối tượng (Utility Class Pattern).
     */
    private MathUtils() {
        throw new AssertionError("Đây là lớp tiện ích, không được khởi tạo!");
    }

    // ==========================================================
    // 1. CÁC PHÉP SO SÁNH SAI SỐ (EPSILON-AWARE)
    // ==========================================================

    public static boolean equals(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    public static boolean greaterThan(double a, double b) {
        return a > (b + EPSILON);
    }
    
    public static boolean greaterThanOrEqual(double a, double b) {
        return a >= (b - EPSILON); 
    }

    public static boolean lessThan(double a, double b) {
        return a < (b - EPSILON);
    }
    
    public static boolean lessThanOrEqual(double a, double b) {
        return a <= (b + EPSILON);
    }

    // ==========================================================
    // 2. TỐI ƯU HÓA CẤU TRÚC DỮ LIỆU (ARRAY OPERATIONS)
    // ==========================================================

    /**
     * Kiểm tra mảng con siêu tốc (O(N)) cho mảng đã sắp xếp.
     * Ứng dụng: Kiểm tra tính đóng (Subsumption Check) trong mọi thuật toán.
     * * @param sub Mảng tập con tiềm năng.
     * @param superSet Mảng tập cha tiềm năng.
     * @return true nếu 'sub' là tập con của 'superSet'.
     */
    public static boolean isSubsetSorted(int[] sub, int[] superSet) {
        if (sub.length > superSet.length) return false;
        if (sub.length == 0) return true;

        // Early Pruning: Nếu phần tử nhỏ nhất/lớn nhất nằm ngoài biên, thoát luôn.
        if (sub[0] < superSet[0] || sub[sub.length - 1] > superSet[superSet.length - 1]) {
            return false;
        }

        int i = 0; // Pointer cho sub
        int j = 0; // Pointer cho superSet
        
        while (i < sub.length && j < superSet.length) {
            if (sub[i] < superSet[j]) {
                return false; 
            } else if (sub[i] == superSet[j]) {
                i++;
                j++;
            } else {
                j++;
            }
        }
        
        return i == sub.length;
    }
}