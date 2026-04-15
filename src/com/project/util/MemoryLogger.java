package com.project.util;

public class MemoryLogger {
    // ĐÃ THÊM FINAL: Đảm bảo instance này là duy nhất và bất biến
    private static final MemoryLogger instance = new MemoryLogger();
    
    private double maxMemory = 0;

    public static MemoryLogger getInstance() {
        return instance;
    }

    public void reset() {
        maxMemory = 0;
    }

    public void checkMemory() {
        Runtime runtime = Runtime.getRuntime();
        double currentMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024.0 / 1024.0;
        if (currentMemory > maxMemory) {
            maxMemory = currentMemory;
        }
    }

    public double getMaxMemory() {
        return maxMemory;
    }
}