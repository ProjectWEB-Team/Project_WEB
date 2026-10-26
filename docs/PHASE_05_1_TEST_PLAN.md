# Phase 5.1 — Hướng dẫn kiểm thử cho TV3

**Một plan duy nhất:** [05.1-01-PLAN.md](../.planning/phases/05.1-full-functional-acceptance/05.1-01-PLAN.md).

TV3 chạy ứng dụng bằng **Spring Tools Suite**, thao tác các chức năng trên trình duyệt, tự đánh giá kết quả và tổng hợp những điểm bất hợp lý. Plan cung cấp bước làm và kết quả mong đợi; không tự kết luận nghiệm thu thay TV3 và không yêu cầu sửa source code, thêm test/harness, migration hoặc script.

Phạm vi hướng dẫn gồm tài khoản/OTP, địa chỉ và tạo đơn/thanh toán, nhận hàng/tracking, kho/trung chuyển/sự cố, giao/hoàn hàng, phí/ví/đối soát/rút tiền/quản trị và thông báo realtime. Thiếu điều kiện nào thì ghi chưa test được cùng lý do; không reset dữ liệu người dùng hoặc ép trạng thái để làm test đạt.

**TV2 làm Phase 6 song song.** Báo cáo rà soát của TV3 và kết quả Phase 6 của TV2 được đưa về để người dùng duyệt cuối cùng. Phase 5.1 chưa được thực hiện hoặc tự đánh giá bởi Codex.

Mốc [516 test, gồm 50 ca SQL thật](PHASE_05_1_PREPLAN_REGRESSION.md) là kết quả hồi quy trước khi lập hướng dẫn, tách riêng khỏi kết quả TV3. Các sửa chữa đăng ký/địa chỉ/trang phí đã có trước đó vẫn nằm trong công việc publish được yêu cầu trước khi chốt phân công này.
