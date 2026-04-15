package com.project.algorithm;

import com.project.model.Itemset;
import com.project.model.Transaction;
import com.project.util.MathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp trừu tượng cốt lõi định nghĩa mọi thuật toán khai phá.
 * Đã gộp AbstractMiner vào đây để tối ưu hóa kiến trúc OOP.
 */
public abstract class MiningAlgorithm {

    // ==========================================
    // 1. LINH HỒN CỦA ABSTRACT MINER (STATE & UTILS)
    // ==========================================
    
    // Biến dùng chung cho tất cả các thuật toán con (Protected để con gọi được)
    protected List<Itemset> closedFrequent = new ArrayList<>();

    // Hàm lọc tập đóng (Post-pruning) - Viết 1 lần, các thuật toán con xài chung
    protected List<Itemset> filterClosedFast(List<Itemset> frequentItemsets) {
        List<Itemset> closed = new ArrayList<>();
        // Sắp xếp giảm dần theo độ dài để lọc nhanh hơn
        frequentItemsets.sort((a, b) -> Integer.compare(b.getItems().length, a.getItems().length));

        for (Itemset X : frequentItemsets) {
            boolean isClosed = true;
            for (Itemset Y : closed) {
                // Nếu Y dài hơn X, có Support bằng X, và X là tập con của Y -> X không phải tập đóng
                if (Y.getItems().length > X.getItems().length && Math.abs(X.getEsup() - Y.getEsup()) < 1e-7) {
                    if (MathUtils.isSubsetSorted(X.getItems(), Y.getItems())) {
                        isClosed = false;
                        break;
                    }
                }
            }
            if (isClosed) {
                closed.add(X);
            }
        }
        return closed;
    }

    // ==========================================
    // 2. LINH HỒN CỦA INTERFACE (CONTRACTS)
    // ==========================================
    
    // Bắt buộc các thuật toán con phải tự khai báo tên
    public abstract String getName();

    // Bắt buộc thuật toán con phải cài đặt hàm chạy Tĩnh (Static)
    public abstract List<Itemset> mine(List<Transaction> db, double minSup);

    // Cung cấp hàm chạy Tăng tiến (Incremental). 
    // Mặc định ném lỗi, trừ khi thuật toán con (như IncFPClose) ghi đè (Override) nó.
    public List<Itemset> mineIncremental(List<Transaction> deltaDB, double minSupRatio) {
        throw new UnsupportedOperationException("Lỗi: Thuật toán [" + getName() + "] chưa hỗ trợ chạy Tăng tiến!");
    }
}