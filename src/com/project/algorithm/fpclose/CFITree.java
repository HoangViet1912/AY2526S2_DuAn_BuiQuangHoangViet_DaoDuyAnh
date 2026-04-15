package com.project.algorithm.fpclose;

import com.project.util.MathUtils;
import java.util.HashMap;
import java.util.Map;

public class CFITree {
    private final CFINode root = new CFINode(-1, null);
    private final Map<Integer, CFINode> headerTable = new HashMap<>();

    /**
     * Chèn một tập đóng vừa tìm thấy vào cây.
     */
    public void insert(int[] sortedItemset, double esup) {
        CFINode curr = root;
        for (int item : sortedItemset) {
            CFINode next = curr.getChild(item);
            if (next == null) {
                next = new CFINode(item, curr);
                curr.addChild(item, next);
                
                // Cập nhật Header Table (Nối vào đầu danh sách liên kết)
                next.setNodeLink(headerTable.get(item));
                headerTable.put(item, next);
            }
            curr = next;
        }
        curr.setEsup(esup);
    }

    /**
     * Kiểm tra tính đóng (Subsumption Check).
     * @return true nếu tập sortedX đã bị bao hàm bởi một tập đóng có cùng support.
     */
    public boolean isSubsumed(int[] sortedX, double targetSup) 
    {
        if (sortedX.length == 0) return true;
        
        // Lấy danh sách các nút chứa vật phẩm cuối cùng của X
        CFINode node = headerTable.get(sortedX[sortedX.length - 1]);
        
        while (node != null) {
            // Chỉ kiểm tra quan hệ tập con nếu support bằng nhau khít (có sai số epsilon)
            if (MathUtils.equals(node.getEsup(), targetSup)) {
                if (isSubsetUpward(node, sortedX)) {
                    return true; // Đã bị bao hàm, không cần tìm tiếp
                }
            }
            node = node.getNodeLink();
            }
            return false;
    }

    /**
     * Duyệt ngược từ nút lá lên gốc để kiểm tra quan hệ cha-con.
     */
    private boolean isSubsetUpward(CFINode node, int[] sortedX) 
    {
    // Vì node đang ở mức sâu nhất (vật phẩm cuối cùng của X), 
    // ta sẽ duyệt ngược từ cuối mảng sortedX về đầu.
    int xIdx = sortedX.length - 1;
    CFINode curr = node;
    
    // Duyệt cây ngược lên đến gốc (root có item = -1)
        while (curr != null && curr.getItem() != -1 && xIdx >= 0) {
            if (curr.getItem() == sortedX[xIdx]) {
                xIdx--; // Tìm thấy phần tử, chuyển sang phần tử tiếp theo trong X
            } else if (curr.getItem() < sortedX[xIdx]) {
                // TỐI ƯU: Vì mảng và đường đi cây đều có xu hướng giảm dần khi lên gốc,
                // nếu item trên cây đã nhỏ hơn phần tử đang tìm trong X, 
                // nghĩa là phần tử đó không tồn tại trên đường đi này.
                return false; 
            }
            curr = curr.getParent();
        }
    // Nếu xIdx == -1 nghĩa là toàn bộ phần tử trong X đã được tìm thấy trên nhánh cây
        return xIdx == -1;
    }
    public CFINode getRoot() 
    {
        return root;
    }

    public Map<Integer, CFINode> getHeaderTable() 
    {
        return headerTable;
    }
}