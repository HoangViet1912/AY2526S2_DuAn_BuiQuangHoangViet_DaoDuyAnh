===============================================================================
DỰ ÁN: INC-CFI-UNCERTAIN-MINER
Tên đầy đủ: Giải pháp Hướng đối tượng cho bài toán Khai phá Tập mục thường xuyên đóng
từ Cơ sở dữ liệu Không chắc chắn Động Tích lũy (Object-Oriented Solutions
to the Problem of Mining Closed Frequent Itemsets from Accumulated Dynamic
Uncertain Databases).
Ngôn ngữ:   Java (SE 25) 
Cập nhật:   Tháng 5/2026
================================================================================

1. TỔNG QUAN
------------
Dự án này nghiên cứu, xây dựng kiến trúc phần mềm và so sánh hiệu năng của 3 thuật toán
trong việc khai phá các tập mục thường xuyên đóng (Closed Frequent Itemsets - CFI) trên
môi trường dữ liệu không chắc chắn và biến đổi liên tục (Accumulated Dynamic Uncertain Data).

Khái niệm cốt lõi:

Dữ liệu không chắc chắn: Mỗi mặt hàng trong giao dịch đi kèm xác suất xuất hiện (0 < p <= 1).

Độ hỗ trợ kỳ vọng (Expected Support - eSup): Tổng xác suất xuất hiện của tập vật phẩm trên toàn bộ cơ sở dữ liệu.

Dữ liệu động tích lũy (Incremental): Xử lý dữ liệu luồng liên tục bằng cơ chế Cửa sổ trượt (Sliding Window), cho phép cộng dồn xác suất mà không cần quét lại dữ liệu cũ.

Closed Itemsets: Chỉ giữ lại các tập mục "đóng" (không bị bao hàm bởi tập cha nào có cùng độ hỗ trợ kỳ vọng, xét qua dung sai epsilon 10^-8) để loại bỏ kết quả dư thừa.

2. CÁC THUẬT TOÁN ĐƯỢC CÀI ĐẶT
------------------------------
Dựa trên mã nguồn trong thư mục `src/com/project/algorithms/`:

A. Inc-CHARM (Incremental Uncertain CHARM)

Kỹ thuật: Khai phá theo chiều dọc (Vertical Mining), sử dụng cấu trúc Uncertain Tidset (UList).
Cơ chế: Giao cắt không gian giao dịch (Intersection) kết hợp 4 tính chất cắt tỉa CHARM.
Ưu điểm: Tốc độ thực thi nhanh nhất trên các tập dữ liệu thưa và trung bình.

Nhược điểm: Tiêu tốn nhiều bộ nhớ RAM khi dữ liệu cực kỳ dày đặc.

B. Inc-dCharm (Incremental Diffset CHARM)

Kỹ thuật: Khai phá theo chiều dọc, sử dụng cấu trúc Độ lệch xác suất (DiffNode).
Cơ chế: Tính toán và lưu trữ phần xác suất bị hao hụt (Diffset) thay vì lưu toàn bộ giao dịch.
Ưu điểm: Tối ưu không gian lưu trữ mảng so với Tidset nguyên thủy.

C. Inc-FP-Close (Incremental FP-Close)
Kỹ thuật: Khai phá theo chiều ngang (Horizontal Mining), sử dụng cây nén CFITree và PrefixTx.
Cơ chế: Phát triển mẫu đệ quy (Pattern Growth) kết hợp kiểm tra tính đóng tức thời (On-the-fly Subsumption Check).
Ưu điểm: Tiết kiệm bộ nhớ nhất (Peak Memory thấp nhất) trên dữ liệu cực kỳ dày đặc nhờ khả năng nén đường dẫn.
Nhược điểm: Thời gian chạy chậm do chi phí đệ quy và khởi tạo cơ sở dữ liệu chiếu..

3. HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY
----------------------------
Yêu cầu hệ thống:
- Java SE Development Kit (JDK) phiên bản 25 trở lên.

Bước 1: Biên dịch mã nguồn
Mở terminal/cmd tại thư mục gốc của dự án và chạy maintest:

    javac -d bin -sourcepath src src/com/project/Main.java

Bước 2: Chạy chương trình
Sau khi biên dịch thành công, chạy lệnh:

    java -cp bin com.project.Main

4. ĐỊNH DẠNG DỮ LIỆU ĐẦU VÀO
----------------------------
File dữ liệu (trong thư mục `datasets/`) tuân thủ cấu trúc dữ liệu bất định, trong đó mỗi dòng chứa ID giao dịch và các item kèm xác suất trong ngoặc đơn:

    T[ID]: Item1(Prob1), Item2(Prob2), ...

Ví dụ một dòng giao dịch:
    T1: 10307(0.86), 10311(0.78), 12487(0.64)

Giải thích:
- Giao dịch T1 chứa 3 vật phẩm.
- Item 10307 có xác suất 86%, Item 10311 có xác suất 78%, Item 12487 có xác suất 64%.

B. Dữ liệu Đầu ra (Output)
Kết quả các tập đóng phổ biến (CFIs) được xuất ra terminal và file log với các nhãn định danh tuần tự t1, t2, t3...:
t1 12759 - Support: 796.82
t2 12339 - Support: 813.27
t3 32205 - Support: 823.23
...

5. CẤU TRÚC THƯ MỤC
-------------------
INC-CFI-UNCERTAIN-MINER/
|-- src/com/project/
|   |-- model/            # Thực thể dữ liệu (Transaction.java, Itemset.java)
|   |-- algorithm/        # Tầng Lõi Thuật toán
|   |   |-- MiningAlgorithm.java (Lớp trừu tượng gốc)
|   |   |-- fpclose/      # Mã nguồn Inc-FP-Close (IncFPCloseMiner, CFITree, CFINode, PrefixTx)
|   |   |-- charm/        # Mã nguồn Inc-CHARM (IncUncertainCharmMiner, UList)
|   |   |-- dcharm/       # Mã nguồn Inc-dCharm (IncDCharmMiner, DiffNode, ITidNode)
|   |-- utils/            # Tiện ích hệ thống (MathUtils.java, MemoryLogger.java,DatasetManagement)
|   |-- manager/        # Trình điều khiển thực nghiệm (BenchmarkPipeline.java, đọc ghi dữ liệu Resultwriter.java)
|
|-- datasets/             # Chứa các file dữ liệu thử nghiệm (BMS1, Mushrooms, Chess)
|-- results/          # Kết quả chạy (thời gian, bộ nhớ, số lượng CFI,Số lượng support) sẽ lưu tại đây


6. KẾT QUẢ THỰC NGHIỆM (THAM KHẢO)
----------------------------------
A. Dữ liệu thưa (dataset-BMS1.txt) 
| Thuật toán     | Thời gian (ms) | Bộ nhớ đỉnh (MB)| Đánh giá tổng quan                        |
|----------------|----------------|-----------------|-------------------------------------------|
| Inc-CHARM      | ~62 ms         | ~44.18 MB       | Tối ưu tuyệt đối (Nhanh nhất, RAM ít nhất)|
| Inc-dCharm     | ~73 ms         | ~92.65 MB       | Bám sát thuật toán gốc                    |
| Inc-FP-Close   | ~173 ms        | ~103.54 MB      | Chậm nhất do cây không phát huy khả năng nén trên dữ liệu thưa |

B. Dữ liệu dày đặc (dataset-mushrooms.txt)
| Thuật toán     | Thời gian (ms) | Bộ nhớ đỉnh (MB)| Đánh giá tổng quan                        |
|----------------|----------------|-----------------|-------------------------------------------|
| Inc-CHARM      | ~1,529 ms      | ~385.05 MB      | Tốc độ thực thi dẫn đầu                   |
| Inc-FP-Close   | ~6,129 ms      | ~317.43 MB      | Thể hiện rõ ưu thế tiết kiệm bộ nhớ       |
| Inc-dCharm     | ~2,646 ms      | ~1092.32 MB     | Chi phí khởi tạo object gây bùng nổ RAM   |

C. Dữ liệu cực kỳ dày đặc (dataset-chess.txt)
| Thuật toán     | Thời gian (ms) | Bộ nhớ đỉnh (MB)| Đánh giá tổng quan                        |
|----------------|----------------|-----------------|-------------------------------------------|
| Inc-CHARM      | ~11,955 ms     | ~543.0 MB       | Giữ vững phong độ tốc độ xử lý            |
| Inc-FP-Close   | ~38,764 ms     | ~494.7 MB       | Cây CFITree đạt đỉnh tối ưu nén tài nguyên|
| Inc-dCharm     | ~29,766 ms     | ~756.4 MB       | Tốc độ và bộ nhớ nằm ở mức trung bình     |
