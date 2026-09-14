# UTEEXPRESS — UI/UX DESIGN SYSTEM SPECIFICATION
## Quy chuẩn Thiết kế Giao diện & Trải nghiệm Người dùng Dùng chung
**Dự án:** UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics  
**Nền tảng Giao diện:** Spring Boot 4.x + JSP/JSTL 3.0 Jakarta + Bootstrap 5.3 + Bootstrap Icons  
**Mục đích:** Ngăn chặn phân mảnh UI/UX giữa 3 thành viên, đồng bộ hóa thẩm mỹ chuyên nghiệp chuẩn Logistics Enterprise.

---

## 1. NGUYÊN TẮC CỐT LÕI (CORE DESIGN PRINCIPLES)

1. **Bootstrap 5 Native & Zero Inline CSS:**
   - Sử dụng 100% hệ thống utility classes của Bootstrap 5.3 (`d-flex`, `gap-3`, `p-3`, `shadow-sm`, `rounded-3`...).
   - **CẤM TUYỆT ĐỐI** viết style inline `<div style="color: red; margin-top: 10px;">`.
   - Các tùy biến nhận diện thương hiệu chỉ được định nghĩa tập trung trong file `src/main/webapp/assets/css/ute-express.css`.
2. **Không Scriptlet & Tuân thủ JSTL 3.0 Jakarta:**
   - 100% View JSP sử dụng cú pháp EL `${}` và Jakarta JSTL (`c:out`, `c:if`, `c:choose`, `c:forEach`, `fmt:formatNumber`, `fn:escapeXml`).
   - Thoát chuỗi an toàn chống XSS: `<c:out value="${shipment.trackingNumber}" />`.
3. **Dual Validation Feedback:**
   - Mọi form input khi có lỗi server-side phải add class `is-invalid` vào thẻ input và hiển thị lỗi ngay bên dưới bằng `<div class="invalid-feedback d-block">${errors.field}</div>`.
   - Giữ lại giá trị người dùng đã nhập: `value="${fn:escapeXml(formData.field)}"`. Cấm echo lại password.
4. **Mobile-First & Responsive Breakpoints:**
   - Màn hình Shipper và Mobile Tracking tối ưu trên màn hình nhỏ (`col-12 col-md-6 col-lg-4`).
   - Bảng biểu danh sách bắt buộc bọc trong `<div class="table-responsive">`.

---

## 2. BẢNG MÀU THƯƠNG HIỆU & TYPOGRAPHY (COLOR PALETTE & TYPE)

### 2.1. Brand Colors (Màu Nhận diện Logistics)
* **Primary (Express Navy / Royal Blue):** `#0a2540` (Navy đậm) và `#0d6efd` (Royal Blue) — Đại diện cho sự tin cậy, tốc độ và công nghệ giao vận.
* **Secondary (Steel Gray):** `#6c757d` — Dùng cho subtext, viền bảng, trạng thái chờ.
* **Background Canvas:** `#f8f9fa` (Light Gray) cho toàn bộ background ứng dụng; `#ffffff` cho card/panel container.

### 2.2. Semantic Feedback Colors (Màu Ngữ nghĩa)
* **Success (Xanh lục):** `#198754` (`bg-success`, `text-success`) — Dùng cho: Giao thành công (`DELIVERED`), Đã thanh toán (`PAID`), Hoàn thành ca (`COMPLETED`).
* **Warning (Vàng cam):** `#ffc107` (`bg-warning text-dark`) — Dùng cho: Chờ bàn giao (`WAITING_FOR_HANDOVER`), Đang lấy hàng (`PICKED_UP`), Chuyển hoàn (`RETURN_TO_SENDER`).
* **Danger (Đỏ cảnh báo):** `#dc3545` (`bg-danger`) — Dùng cho: Giao thất bại (`FAILED`), Hủy đơn (`CANCELLED`), Biên bản sự cố (`INCIDENT`), Lệch tiền thiếu (`SHORTAGE`).
* **Info (Xanh lơ Cyan):** `#0dcaf0` (`bg-info text-dark`) — Dùng cho: Đang trung chuyển (`IN_TRANSIT`), Chuyến xe xe tải (`Trips`).
* **Dark (Đen xám):** `#212529` (`bg-dark`) — Dùng cho: Đóng ngoại lệ bồi thường/tiêu hủy (`CLOSED_EXCEPTION`).

### 2.3. Typography
* **Font Family:** `system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif`.
* **Font Size & Weight:**
  - Page Title: `h3 fw-bold text-dark mb-3`
  - Section Title: `h5 fw-semibold text-secondary mb-2`
  - Body Text: `fs-6 text-dark` (14px - 16px)
  - Small / Meta info: `small text-muted` (12px)

---

## 3. QUY CHUẨN BADGES CHO 13 TRẠNG THÁI VẬN ĐƠN (13 SHIPMENT STATUSES)

Cả 3 thành viên bắt buộc sử dụng đúng mã class Bootstrap 5 dưới đây khi hiển thị trạng thái vận đơn, không tự chế màu sắc:

| Mã Enum `ShipmentStatus` | Class Badge Bootstrap 5 Chuẩn | Nhãn hiển thị Tiếng Việt |
|---|---|---|
| `CREATED` | `<span class="badge bg-secondary">Mới tạo</span>` | Mới tạo |
| `WAITING_FOR_PAYMENT` | `<span class="badge bg-warning text-dark">Chờ thanh toán</span>` | Chờ thanh toán |
| `WAITING_FOR_HANDOVER` | `<span class="badge bg-info text-dark">Chờ bàn giao</span>` | Chờ bàn giao |
| `PICKED_UP` | `<span class="badge bg-primary">Đã lấy hàng</span>` | Đã lấy hàng |
| `AT_HUB` | `<span class="badge bg-primary">Tại bưu cục</span>` | Tại bưu cục |
| `IN_TRANSIT` | `<span class="badge bg-info text-white">Đang trung chuyển</span>` | Đang trung chuyển |
| `OUT_FOR_DELIVERY` | `<span class="badge bg-primary fw-bold">Đang phát hàng</span>` | Đang phát hàng |
| `FAILED` | `<span class="badge bg-danger">Giao thất bại</span>` | Giao thất bại |
| `DELIVERED` | `<span class="badge bg-success">Giao thành công</span>` | Giao thành công |
| `RETURN_TO_SENDER` | `<span class="badge bg-warning text-dark">Đang chuyển hoàn</span>` | Đang chuyển hoàn |
| `RETURNED_TO_SENDER` | `<span class="badge bg-secondary">Đã hoàn trả</span>` | Đã hoàn trả |
| `CANCELLED` | `<span class="badge bg-danger">Đã hủy</span>` | Đã hủy |
| `CLOSED_EXCEPTION` | `<span class="badge bg-dark text-white">Đóng ngoại lệ</span>` | Đóng ngoại lệ |

---

## 4. MẪU COMPONENT CHUẨN HÓA (REUSABLE UI COMPONENTS)

### 4.1. Cấu trúc Card Kiện hàng (Shipment Card)
```jsp
<div class="card border-0 shadow-sm mb-3 rounded-3">
    <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
        <div>
            <span class="text-muted small">Mã vận đơn:</span>
            <span class="fw-bold text-primary font-monospace ms-1"><c:out value="${shipment.trackingNumber}" /></span>
        </div>
        <div>
            <%-- Render Badge theo đúng bảng quy chuẩn Mục 3 --%>
            <c:choose>
                <c:when test="${shipment.currentStatus == 'DELIVERED'}">
                    <span class="badge bg-success">Giao thành công</span>
                </c:when>
                <c:when test="${shipment.currentStatus == 'OUT_FOR_DELIVERY'}">
                    <span class="badge bg-primary fw-bold">Đang phát hàng</span>
                </c:when>
                <c:otherwise>
                    <span class="badge bg-secondary"><c:out value="${shipment.currentStatus}" /></span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="card-body">
        <div class="row g-2">
            <div class="col-md-6">
                <p class="mb-1 small text-muted">Người gửi:</p>
                <p class="mb-0 fw-semibold"><c:out value="${shipment.senderName}" /> - <c:out value="${shipment.senderPhone}" /></p>
                <p class="small text-secondary mb-0"><c:out value="${shipment.senderAddress}" /></p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 small text-muted">Người nhận:</p>
                <p class="mb-0 fw-semibold"><c:out value="${shipment.receiverName}" /> - <c:out value="${shipment.receiverPhone}" /></p>
                <p class="small text-secondary mb-0"><c:out value="${shipment.receiverAddress}" /></p>
            </div>
        </div>
    </div>
    <div class="card-footer bg-light py-2 d-flex justify-content-between align-items-center">
        <span class="small text-muted">Tiền COD: <strong class="text-danger"><fmt:formatNumber value="${shipment.codAmount}" type="currency" currencySymbol="₫" /></strong></span>
        <a href="${pageContext.request.contextPath}/shipments/detail?id=${shipment.id}" class="btn btn-sm btn-outline-primary rounded-pill px-3">Chi tiết</a>
    </div>
</div>
```

---

### 4.2. Cấu trúc Bảng Danh sách (Data Table)
Mọi bảng quản trị trong hệ thống phải tuân thủ markup:
```jsp
<div class="table-responsive bg-white rounded-3 shadow-sm p-3">
    <table class="table table-hover table-striped align-middle mb-0">
        <thead class="table-light">
            <tr>
                <th scope="col" class="text-center" style="width: 50px;">#</th>
                <th scope="col">Mã vận đơn</th>
                <th scope="col">Người gửi</th>
                <th scope="col">Người nhận</th>
                <th scope="col" class="text-end">Tiền COD</th>
                <th scope="col" class="text-center">Trạng thái</th>
                <th scope="col" class="text-center" style="width: 120px;">Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="item" items="${shipments}" varStatus="loop">
                <tr>
                    <td class="text-center text-muted"><c:out value="${loop.index + 1}" /></td>
                    <td><span class="font-monospace fw-bold text-primary"><c:out value="${item.trackingNumber}" /></span></td>
                    <td><c:out value="${item.senderName}" /></td>
                    <td><c:out value="${item.receiverName}" /></td>
                    <td class="text-end fw-semibold text-danger">
                        <fmt:formatNumber value="${item.codAmount}" type="currency" currencySymbol="₫" />
                    </td>
                    <td class="text-center">
                        <%-- Badge theo chuẩn Mục 3 --%>
                    </td>
                    <td class="text-center">
                        <a href="..." class="btn btn-sm btn-outline-secondary me-1" title="Xem"><i class="bi bi-eye"></i></a>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty shipments}">
                <tr>
                    <td colspan="7" class="text-center py-4 text-muted">
                        <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                        Không có dữ liệu vận đơn phù hợp.
                    </td>
                </tr>
            </c:if>
        </tbody>
    </table>
</div>
```

---

### 4.3. Cấu trúc Form Input Chuẩn & Dual Validation Feedback
Áp dụng mẫu form đồng bộ giữ dữ liệu `${formData}` và xuất thông báo lỗi `${errors}`:
```jsp
<form action="${pageContext.request.contextPath}/user/shipments/create" method="post" class="needs-validation">
    <!-- CSRF Token (nếu có) -->
    <div class="mb-3">
        <label for="receiverPhone" class="form-label fw-semibold">Số điện thoại người nhận <span class="text-danger">*</span></label>
        <input type="tel" 
               class="form-control ${not empty errors.receiverPhone ? 'is-invalid' : ''}" 
               id="receiverPhone" 
               name="receiverPhone" 
               value="${fn:escapeXml(formData.receiverPhone)}" 
               placeholder="Ví dụ: 0981234567" 
               required>
        <c:if test="${not empty errors.receiverPhone}">
            <div class="invalid-feedback d-block">
                <c:out value="${errors.receiverPhone}" />
            </div>
        </c:if>
    </div>

    <div class="mb-3">
        <label for="weight" class="form-label fw-semibold">Khối lượng kiện (gram) <span class="text-danger">*</span></label>
        <input type="number" 
               class="form-control ${not empty errors.weight ? 'is-invalid' : ''}" 
               id="weight" 
               name="weight" 
               value="${fn:escapeXml(formData.weight)}" 
               min="100" 
               max="50000" 
               required>
        <c:if test="${not empty errors.weight}">
            <div class="invalid-feedback d-block">
                <c:out value="${errors.weight}" />
            </div>
        </c:if>
    </div>

    <div class="d-flex justify-content-end gap-2 mt-4">
        <a href="${pageContext.request.contextPath}/user/shipments" class="btn btn-light px-4">Hủy</a>
        <button type="submit" class="btn btn-primary px-4 fw-semibold">Tạo vận đơn</button>
    </div>
</form>
```

---

### 4.4. Cấu trúc Stepper Timeline Tra cứu Hành trình (Vertical Stepper)
Sử dụng cấu trúc dòng thời gian chuẩn cho màn hình Tra cứu Public Tracking:
```html
<div class="tracking-timeline position-relative ps-4 my-4">
    <!-- Line nối giữa các node: định nghĩa trong ute-express.css -->
    
    <!-- Step 1: Đã giao thành công -->
    <div class="timeline-item position-relative pb-4">
        <div class="timeline-marker bg-success text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; margin-left: -14px;">
            <i class="bi bi-check-lg small"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-bold mb-0 text-success">Giao hàng thành công</h6>
            <p class="small text-muted mb-1">14:30 - 15/09/2026 | Người nhận: Đã ký nhận</p>
            <p class="small text-secondary mb-0">Shipper Nguyễn Văn B (Mã: SHP01) đã thu hộ COD 500.000₫.</p>
        </div>
    </div>

    <!-- Step 2: Đang giao hàng -->
    <div class="timeline-item position-relative pb-4">
        <div class="timeline-marker bg-primary text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; margin-left: -14px;">
            <i class="bi bi-truck small"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-bold mb-0 text-primary">Đang trên đường giao hàng</h6>
            <p class="small text-muted mb-1">08:15 - 15/09/2026 | Bưu cục phát: Cần Thơ Delivery Hub</p>
        </div>
    </div>

    <!-- Step 3: Nhập bưu cục -->
    <div class="timeline-item position-relative">
        <div class="timeline-marker bg-secondary text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 28px; height: 28px; margin-left: -14px;">
            <i class="bi bi-box-seam small"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-semibold mb-0 text-dark">Kiện đã nhập bưu cục gốc</h6>
            <p class="small text-muted mb-0">19:00 - 14/09/2026 | Bưu cục: Dĩ An Delivery Hub</p>
        </div>
    </div>
</div>
```

---

### 4.5. Cơ chế Che mờ Dữ liệu Nhạy cảm (Data Masking)
Khi hiển thị thông tin người nhận trên Public Tracking hoặc danh sách "Đơn đang đến" mà người dùng chưa xác thực mã OTP/mã đầy đủ:
* **Số điện thoại:** Chỉ hiện 3 số đầu và 3 số cuối -> `098****456`
* **Địa chỉ chi tiết:** Ẩn số nhà/tên đường, chỉ giữ Phường/Xã, Quận/Huyện, Tỉnh/Thành -> `***, Phường Linh Trung, Thành phố Thủ Đức, TP. Hồ Chí Minh`
* **Họ tên người gửi:** `Ng*** V** A**`
* **Markup mẫu:**
```jsp
<div class="alert alert-info py-2 d-flex align-items-center gap-2 mb-3">
    <i class="bi bi-shield-lock-fill fs-5"></i>
    <span class="small">Thông tin cá nhân đã được che mờ tự động để bảo vệ quyền riêng tư. Nhập mã OTP để xem đầy đủ.</span>
</div>
<p class="mb-1"><strong>SĐT Người nhận:</strong> <span class="font-monospace"><c:out value="${maskedReceiverPhone}" /></span></p>
<p class="mb-0"><strong>Địa chỉ:</strong> <c:out value="${maskedReceiverAddress}" /></p>
```
