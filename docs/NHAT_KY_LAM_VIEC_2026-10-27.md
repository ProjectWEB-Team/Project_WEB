# Nhật ký làm việc ngày 27/10/2026 (UTEExpress)

Nhánh: `feature/role-completeness` (local, **chưa push**, **chưa đụng AWS**). Commit cuối ở thời điểm viết: xem `git log -1`.
Cập nhật lần cuối: cuối ngày 27/10/2026. Phần "Việc còn lại" ở cuối là những gì bạn cần tự làm tiếp.

---

## 1. Tóm tắt một dòng

Trong ngày: hoàn thiện chức năng theo vai trò, danh sách chọn thay ô gõ ID, tiếng Việt cho thông báo lỗi, ETA, khóa lạc quan (@Version), thiết kế lại toàn bộ giao diện trên Figma rồi triển khai vào JSP/CSS, làm lại trang chủ, sửa các điểm trong báo cáo kiểm thử của bạn bạn. **1932 test đạt, 0 lỗi, 0 bỏ qua** (có SQL Server thật).

---

## 2. Đã sửa gì, làm gì (theo nhóm, kèm commit)

### 2.1 Chức năng và dữ liệu
| Việc | Chi tiết | Commit |
|---|---|---|
| Danh sách chọn thay ô gõ ID | Trang đóng bao (hub đích, tài xế); trang tài xế (phiếu bàn giao, bao, chuyến); hub nhận hàng (phiếu, bao, đơn); đối soát (shipper); shipper quét đơn (chọn bưu cục) | `9f2b695`, `9f0dd20`, `79f8e4f`, `9e74959` |
| ETA (ngày dự kiến giao) | Trang chi tiết đơn: cùng miền +2 ngày, khác miền +4 ngày; không hiện khi đơn đã giao, hủy, hoàn | `4962ba0` |
| Việt hóa lỗi nghiệp vụ | ~190 câu lỗi tiếng Anh hiện bằng tiếng Việt qua bảng `src/main/resources/error-messages-vi.tsv` (cột 1: câu gốc, cột 2: câu Việt; câu có dấu `: ` ở cuối được ghép với phần chi tiết). Test cũ không phải sửa | `a923c5b` |
| Khóa lạc quan `@Version` | Ví, yêu cầu rút tiền, thanh toán, COD + migration `V19__optimistic_locking_money_records.sql` | `af3f9ee` |
| Shipper giao hàng một lần bấm | Endpoint mới `POST /shipper/delivery-tasks/deliver` (thu tiền rồi xác nhận giao); đã thêm vào bảng ma trận quyền | `9e74959` |
| Hub nhận hàng | Bảng "Phiếu bàn giao đang chờ nhận" | `9e74959` |
| Đối soát | Nút "Điền đủ số tiền kỳ vọng" | `9e74959` |
| Tạo đơn | Bước 4 có nút "Xem báo giá rồi tạo đơn" khi chưa xem báo giá | `9e74959` |
| Đăng nhập | Ô mật khẩu hiện sẵn (không còn giấu trong `<details>`) | `9e74959` |
| Lỗi ô tra cứu trang chủ | Trang chủ gửi tham số `trackingNumber` nhưng `/tracking` đọc `code`, nay đã khớp | `bcbca00` |

### 2.2 Giao diện
1. **Figma (đã duyệt):** https://www.figma.com/design/8rQ2SfsjqeSFDcGb7I3brh (tài khoản Hoang Phuc Khang). Gồm các trang: Cover, Foundations (màu kèm độ tương phản tính thật, chữ, khoảng cách), Components (Button, Input, Search, Status Badge 13 trạng thái, Nav, Sidebar, Table, Pagination, Filter, Modal, Toast, Skeleton, Empty state, Timeline), Landing & Tracking, Customer, Operations, Admin. Desktop 1440 và Mobile 390. Không có Tablet 768.
2. **Triển khai vào code** (`bcbca00`, `9e74959`):
   - `src/main/webapp/assets/css/ute-modern.css`: toàn bộ token và kiểu dáng (nền, nút bo tròn, thẻ, bảng, form, nhãn trạng thái có ký hiệu, vòng focus, giảm chuyển động). Tải sau Bootstrap và `ute-express.css`.
   - `layout/navbar.jsp`: thanh trắng đầy đủ; nhân viên hub, quản lý, admin có menu dọc bên trái (màn ≥992px); màn hẹp có nút "Menu"; có liên kết "Bỏ qua điều hướng"; trang có `<head>` riêng vẫn nhận kiểu mới qua navbar.
   - `layout/footer.jsp`, `index.jsp` (trang chủ), `tracking/search.jsp` (trang tra cứu).
   - Thẻ JSP dùng chung: `WEB-INF/tags/navlink.tag` (link menu, tự đánh dấu trang hiện tại), `WEB-INF/tags/statusbadge.tag` (nhãn trạng thái tiếng Việt kèm ký hiệu).
   - 4 trang hub/quản lý trước đây là HTML thô nay có khung và `viewport`: `hub/inbound-queue`, `hub/delivery-assignments`, `manager/missing-parcels`, `manager/overdue-returns`.
   - Trang giao hàng của shipper viết lại dạng thẻ (`shipper/delivery-tasks.jsp`).
   - Xóa `layout/sidebar.jsp` (không còn dùng).
3. **Trang chủ làm lại hai lần** theo góp ý của bạn: lần 1 theo Taste Skill (chữ khổng lồ, nền cam), lần 2 dễ hiểu hơn (tiêu đề vừa phải, 2 ô hành động, ví dụ trang theo dõi, 3 dịch vụ, 4 bước, 6 tiện ích, FAQ, footer).

---

## 3. Cấu hình và cách chạy (để bạn tự làm tiếp)

### 3.1 Chạy web trên máy
Dùng profile `local,demo,otp-local` (DB local `ute_express_v2` đã có các migration demo, nếu chỉ dùng `local` Flyway sẽ báo "applied migration not resolved locally: demo ..."):

```powershell
cd E:\Project_WEB
$env:JAVA_HOME='C:\Program Files\Java\jdk-22'
$env:UTEEXPRESS_JWT_SECRET=[IO.File]::ReadAllText('E:\Project_WEB\.local\jwt-secret.txt').Trim()
# Maven trong ~\.m2\wrapper\dists\apache-maven-3.9.11\...\bin\mvn.cmd
mvn -B spring-boot:run "-Dspring-boot.run.profiles=local,demo,otp-local" "-Dspring-boot.run.arguments=--server.port=8081"
```
Mở http://localhost:8081. Tài khoản demo: `demo_admin`, `demo_manager`, `demo_hub_hcm`, `demo_shipper_hcm`, `demo_driver_hcm`... mật khẩu demo ghi ở đầu file `src/main/resources/db/demo/R__demo_b_accounts.sql`. **Mật khẩu demo nằm công khai trong repo, không bật demo trên Internet mà không đổi** (xem `docs/DEPLOY_AWS.md` mục 8b).

### 3.2 Chạy test
- Test thường: `mvn test` (SQL Server thật sẽ bị **bỏ qua** nếu thiếu biến môi trường).
- Test đầy đủ có SQL Server thật (1932 test): đặt các biến rồi chạy `mvn test`:
  - `UTEEXPRESS_SQLSERVER_URL` = `jdbc:sqlserver://localhost:1433;databaseName=uteexpress_phase05_test_tv2;encrypt=true;trustServerCertificate=true` (database dùng một lần, tên bắt buộc dạng `uteexpress_phase05_test_<hậu tố>`)
  - `UTEEXPRESS_SQLSERVER_USERNAME` = `uteexpress_app`, `UTEEXPRESS_SQLSERVER_PASSWORD` = mật khẩu trong `application-local.properties`
  - `UTEEXPRESS_JWT_SECRET` = nội dung `.local\jwt-secret.txt`
  - **Lưu ý PowerShell:** dùng đường dẫn tuyệt đối khi đọc `jwt-secret.txt`; nếu thiếu, toàn bộ test SQL bị bỏ qua mà vẫn báo "BUILD SUCCESS" (đã gặp 2 lần hôm nay, kết quả 1915 test, 31 bỏ qua là dấu hiệu này).
- Database test `uteexpress_phase05_test_tv2` đã được chạy `V19` thủ công bằng `sqlcmd -S localhost -E -C -d uteexpress_phase05_test_tv2 -i ...V19...sql`. DB local `ute_express_v2` được Flyway tự chạy V19 khi app khởi động.

### 3.3 File WAR (trong `E:\Project_WEB\.local\deploy\`)
- `project-web-1.0-role-completeness-final.war`: bản sau trang hub/định tuyến (cũ).
- `-v2.war`: sau danh sách chọn, ETA, Việt hóa, V19.
- `-v3.war`: sau lớp CSS tạm đầu tiên.
- `-v4.war`: sau khi triển khai thiết kế Figma.
- `-v5.war`: **bản mới nhất** (trang chủ làm lại, shipper một lần bấm...). Cần build lại nếu có thay đổi tiếp theo: `mvn -q -DskipTests package`, rồi copy `target\project-web-1.0.war`.

### 3.4 Công cụ đã dùng
- **CodeGraph** (`codegraph_explore`): truy vấn mã nguồn và phạm vi ảnh hưởng.
- **Figma MCP** (ghi vào file thật), **Taste Skill** (`design-taste-frontend`; thư mục nháp `.taste-skill/` ở gốc dự án, **không commit**, có thể xóa), **browser-automation** (chụp màn hình thật, script nháp nằm trong thư mục scratchpad của Claude, không thuộc repo).
- Quy tắc đã giữ: commit nhỏ trên nhánh local, **không push GitHub, không đưa lên AWS** khi chưa được bạn cho phép.

---

## 4. Thiếu sót và giới hạn (nói thẳng)

- **Chưa làm khung Tablet 768** trên Figma; mới có Desktop 1440 và Mobile 390.
- **Chưa xem thật trên trình duyệt** các trang khách: tạo đơn, danh sách đơn, chi tiết đơn, ví (chỉ áp kiểu qua CSS dùng chung). Đã xem thật: trang chủ, tra cứu, đăng nhập, hub (nhận hàng), quản lý (vận hành), admin (người dùng, mạng lưới), shipper (giao hàng).
- **Ô ảnh tư liệu** trên trang chủ chưa có (không dùng ảnh giả). Nếu lấy ảnh mở thì phải ghi nguồn ở chân trang.
- **Trang lỗi 4xx/5xx** chưa chỉnh kiểu riêng.
- **Bảng nhiều cột** (mạng lưới admin, người dùng) còn chật.
- **Nạp tiền vào ví** chưa có (cần cổng thanh toán; quyết định riêng).
- **Duyệt hàng loạt "Trả COD"** cho quản lý chưa có.
- **Trang hàng thất lạc** (quản lý) vẫn gõ ID phiếu bàn giao và bao.
- **Trang đơn đang đến** vẫn bắt nhập lại mã vận đơn để xem địa chỉ và COD. Đây là thiết kế che thông tin có chủ đích; nếu muốn bỏ, bạn quyết rồi mới sửa.
- **Chờ bạn chốt:** hạn thanh toán online hiện tạm 24 giờ (demo là 2 phút).
- **Nhánh chưa push:** mọi thứ chỉ có trên máy này. Nếu máy hỏng là mất.

---

## 5. Việc còn lại để bạn tự làm tiếp (theo thứ tự gợi ý)

1. **Xem thử toàn bộ giao diện** ở http://localhost:8081 (chạy như mục 3.1). Mở lần lượt từng vai trò (user, hub, shipper, tài xế, quản lý, admin), ở cả máy tính và điện thoại (thu hẹp cửa sổ). Ghi lại trang nào lệch, chữ nào khó đọc.
2. **Quyết định các điểm đang chờ:** hạn thanh toán online, có bỏ bước "nhập lại mã" ở đơn đang đến không, có làm nạp tiền ví không.
3. **Push nhánh lên GitHub khi hài lòng:**
   ```powershell
   git push -u origin feature/role-completeness
   ```
   (Chỉ làm khi bạn đã xem xong; tạo Pull Request vào `main` sau đó.)
4. **Đưa lên AWS** theo `docs/DEPLOY_AWS.md`: copy file `-v5.war` (hoặc bản build mới) lên EC2, đặt `SPRING_PROFILES_ACTIVE=prod,demo` nếu còn giai đoạn test, **đặt `UTEEXPRESS_DEMO_PASSWORD_HASH` khác mật khẩu mặc định**, khởi động lại dịch vụ. Flyway sẽ tự chạy `V18` (OTP đặt lại mật khẩu) và `V19` (@Version) trên RDS: **sao lưu RDS trước khi chạy**.
5. **Báo bạn trong nhóm** (Vạn) rằng web có trang quản lý hub và tuyến (phần định tuyến Phase 3 của Vạn đọc hai bảng này).
6. **Việc nên làm thêm nếu còn thời gian:** khung Tablet 768, ô ảnh trang chủ (lấy ảnh mở, ghi nguồn), trang lỗi đẹp hơn, chỉnh bảng nhiều cột, gõ ID ở trang hàng thất lạc đổi thành danh sách chọn.
7. **Dọn nháp (tùy chọn):** xóa thư mục `.taste-skill/` và `.codegraph/` nếu không cần (không nằm trong git).

---

## 6. Phần làm dở và việc cuối ngày

**Đã xong (commit cuối ngày):** trang chủ bớt trống theo góp ý của bạn:
- Thanh thông tin trên header (tổng đài 24/7, email, link tra cứu nhanh và câu hỏi thường gặp), chỉ hiện ở trang chủ (cờ `uteTopBar` trong `index.jsp`, xử lý ở `layout/navbar.jsp`).
- Mục "Gửi hàng gồm 4 bước" thành 4 thẻ bấm được (tab, dùng được bằng phím mũi tên trái/phải): mỗi bước hiện danh sách việc cần làm và một hình minh họa (báo giá, hai cách bàn giao, mạng lưới 3 cấp, các kết quả giao hàng). Nội dung minh họa luôn ghi rõ "Ví dụ minh họa".
- Thẻ ví dụ trang theo dõi (bên phải đầu trang) có thêm nhãn trạng thái, nơi gửi, nơi nhận, dịch vụ, tiền thu hộ COD.
- Tham khảo spx.vn và các trang giao nhận khác về bố cục (thanh thông tin, ô tra cứu nổi bật, dịch vụ, các bước, câu hỏi thường gặp); mình **không** sao chép nhận diện hay nội dung của họ. Mình chưa tải trang spx.vn để đối chiếu từng chi tiết.

**Chưa làm (bạn có thể giao tiếp cho phiên sau):** xem mục 4 và mục 5 ở trên. Nếu quota hết giữa chừng, tình trạng nhánh luôn nằm ở `git log -1` và `git status` (không có file dở khi báo cáo này được cập nhật).
