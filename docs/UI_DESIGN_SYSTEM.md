# UTEEXPRESS — UI/UX DESIGN SYSTEM SPECIFICATION
## Quy chuẩn Thiết kế Giao diện & Trải nghiệm Người dùng Chuẩn Logistics UTEExpress
**Dự án:** UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics  
**Nhận diện Thương hiệu:** Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE) Logistics Enterprise  
**Nền tảng Giao diện:** Spring Boot 4.x + JSP/JSTL 3.0 Jakarta + Bootstrap 5.3 + Bootstrap Icons  
**Mục đích:** Xóa bỏ triệt để giao diện AI generic tím/navy, chuẩn hóa bảng màu Deep Cobalt Blue & Safety Orange, bảo đảm độ tương phản cao cho nghiệp vụ kho và tài xế/shipper ngoài hiện trường.

---

## 1. NGUYÊN TẮC CỐT LÕI (CORE DESIGN PRINCIPLES)

1. **Chuẩn Nhận diện HCMUTE Logistics (Authentic Brand Identity):**
   - Màu chủ đạo là **Deep Cobalt Blue (`#0F3D78`)** — màu sắc biểu trưng cho uy tín, kỷ luật kỹ thuật và bản sắc HCMUTE.
   - Màu hành động / chuyển phát là **Express Safety Orange (`#F26522`)** — kích thích thị giác, độ nhận diện cao chuyên biệt cho các nút thao tác giao hàng, quét mã, thu tiền COD.
   - Màu nền giao diện là **Industrial Gray (`#F1F5F9`)** — chống lóa mắt khi nhân viên kho và shipper thao tác liên tục trong ca làm việc.
2. **Độ tương phản cao cho Hiện trường & Kho vận (High-Contrast Ergonomics):**
   - Màn hình tác nghiệp bưu cục (Hub Inbound/Outbound) và màn hình di động của Shipper ngoài trời bắt buộc phải có độ tương phản đạt chuẩn WCAG AA/AAA (tỷ lệ tương phản tối thiểu 4.5:1 cho text thông thường, 3:1 cho badge lớn).
   - 13 Trạng thái vận đơn (`ShipmentStatus`) được mã hóa màu sắc tách biệt hoàn toàn, cấm dùng các gam màu pastel nhạt gây nhầm lẫn khi quét kiện hàng.
3. **Bootstrap 5 Native & Zero Inline CSS:**
   - Tận dụng 100% hệ thống utility classes của Bootstrap 5.3 kết hợp với bộ biến CSS thương hiệu chuẩn tại `src/main/webapp/assets/css/ute-express.css`.
   - **CẤM TUYỆT ĐỐI** viết CSS inline trực tiếp vào thẻ HTML (`style="..."`).
4. **Không Scriptlet & Tuân thủ JSTL 3.0 Jakarta:**
   - 100% View JSP sử dụng cú pháp EL `${}` và Jakarta JSTL (`c:out`, `c:if`, `c:choose`, `c:forEach`, `fmt:formatNumber`, `fn:escapeXml`).
   - Thoát chuỗi an toàn chống XSS: `<c:out value="${shipment.trackingNumber}" />`.
5. **Dual Validation Feedback & Form Ergonomics:**
   - Mọi form khi submit có lỗi Server-side phải add class `is-invalid` vào thẻ input và hiển thị lỗi ngay bên dưới: `<div class="invalid-feedback d-block">${errors.field}</div>`.
   - Giữ lại giá trị người dùng đã nhập: `value="${fn:escapeXml(formData.field)}"`. Tuyệt đối không echo lại password.

---

## 2. BẢNG MÀU THƯƠNG HIỆU & HỆ THỐNG PHÂN CẤP (COLOR SYSTEM)

### 2.1. Brand Palette (Bảng màu Cốt lõi)

| Tên màu | Mã HEX | Vai trò kỹ thuật & Ngữ cảnh sử dụng | Class CSS Utility |
|---|---|---|---|
| **HCMUTE Cobalt Blue** | `#0F3D78` | **Primary Brand Color:** Dùng cho Topbar, Sidebar active item, tiêu đề bảng, liên kết chính, logo thương hiệu. | `.bg-ute-primary`, `.text-ute-primary` |
| **Express Safety Orange** | `#F26522` | **Accent / Primary CTA:** Dùng cho nút tạo đơn mới, nút xác nhận giao hàng, nút thu tiền COD, icon cảnh báo chú ý cao. | `.bg-ute-accent`, `.btn-ute-accent` |
| **Industrial Gray Canvas** | `#F1F5F9` | **App Background:** Màu nền tổng thể chống chói mắt, tạo độ sâu tách biệt với các thẻ Card màu trắng `#FFFFFF`. | `.bg-ute-canvas` |
| **Pure White** | `#FFFFFF` | **Surface / Card Background:** Nền card, container dữ liệu, bảng danh sách. | `.bg-white` |
| **Dark Slate (Main Text)** | `#1E293B` | **Text chính:** Màu chữ tiêu đề, dữ liệu quan trọng, đảm bảo độ tương phản tối đa trên nền trắng/xám. | `.text-ute-dark` |
| **Muted Slate (Subtext)** | `#64748B` | **Text phụ:** Nhãn mô tả, timestamp, thông tin metadata. | `.text-ute-muted` |
| **Border Gray** | `#CBD5E1` | **Đường phân cách:** Viền card, phân tách dòng bảng, input border. | `.border-ute` |

### 2.2. CSS Custom Properties (`src/main/webapp/assets/css/ute-express.css`)
```css
:root {
    --ute-primary: #0F3D78;
    --ute-primary-dark: #0A2852;
    --ute-primary-light: #EBF2FA;
    --ute-accent: #F26522;
    --ute-accent-hover: #D85213;
    --ute-accent-light: #FFF0E8;
    --ute-canvas: #F1F5F9;
    --ute-surface: #FFFFFF;
    --ute-text-main: #1E293B;
    --ute-text-muted: #64748B;
    --ute-border: #CBD5E1;
}

body {
    background-color: var(--ute-canvas);
    color: var(--ute-text-main);
    font-family: system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.bg-ute-primary { background-color: var(--ute-primary) !important; color: #FFFFFF !important; }
.text-ute-primary { color: var(--ute-primary) !important; }
.bg-ute-accent { background-color: var(--ute-accent) !important; color: #FFFFFF !important; }
.text-ute-accent { color: var(--ute-accent) !important; }

/* Nút bấm CTA chính mang màu Express Safety Orange */
.btn-ute-accent {
    background-color: var(--ute-accent);
    border-color: var(--ute-accent);
    color: #FFFFFF;
    font-weight: 600;
}
.btn-ute-accent:hover, .btn-ute-accent:focus {
    background-color: var(--ute-accent-hover);
    border-color: var(--ute-accent-hover);
    color: #FFFFFF;
}

/* Nút bấm thứ cấp phong cách Deep Cobalt */
.btn-ute-primary {
    background-color: var(--ute-primary);
    border-color: var(--ute-primary);
    color: #FFFFFF;
    font-weight: 600;
}
.btn-ute-primary:hover, .btn-ute-primary:focus {
    background-color: var(--ute-primary-dark);
    border-color: var(--ute-primary-dark);
    color: #FFFFFF;
}
```

---

## 3. QUY CHUẨN ĐỘ TƯƠNG PHẢN CAO CHO 13 TRẠNG THÁI VẬN ĐƠN (13 STATUS BADGES)

Trong môi trường kho vận ánh sáng mạnh và shipper di chuyển ngoài trời, màu sắc badge phải có độ tương phản vượt trội (chữ trắng trên nền đậm, hoặc chữ đen trên nền vàng rực rỡ):

| Mã Enum `ShipmentStatus` | Nhãn Tiếng Việt | Mã Màu Nền HEX | Màu Chữ | Class Bootstrap & Custom | Độ tương phản & Ý nghĩa nghiệp vụ |
|---|---|---|---|---|---|
| `CREATED` | Mới tạo | `#64748B` (Slate) | Trắng | `<span class="badge bg-secondary">Mới tạo</span>` | Đơn mới tạo trên Web, chưa vào bưu cục. |
| `WAITING_FOR_PAYMENT` | Chờ thanh toán | `#D97706` (Amber) | Trắng | `<span class="badge bg-warning text-dark fw-bold">Chờ thanh toán</span>` | Chờ người gửi thanh toán cước trực tuyến VNPay. |
| `WAITING_FOR_HANDOVER` | Chờ bàn giao | `#0284C7` (Sky Blue) | Trắng | `<span class="badge bg-info text-white fw-bold">Chờ bàn giao</span>` | Đã duyệt cước, chờ tiếp nhận vật lý. |
| `PICKED_UP` | Đã lấy hàng | `#4F46E5` (Indigo) | Trắng | `<span class="badge text-white fw-bold" style="background-color: #4F46E5;">Đã lấy hàng</span>` hoặc `<span class="badge badge-picked-up">Đã lấy hàng</span>` | Shipper đã nhận kiện từ người gửi. |
| `AT_HUB` | Tại bưu cục | `#0F3D78` (Cobalt Blue) | Trắng | `<span class="badge bg-ute-primary fw-bold">Tại bưu cục</span>` | Kiện đã quét Inbound vào bưu cục, chờ phân loại. |
| `IN_TRANSIT` | Đang trung chuyển | `#7C3AED` (Purple) | Trắng | `<span class="badge text-white fw-bold" style="background-color: #7C3AED;">Đang trung chuyển</span>` hoặc `<span class="badge badge-in-transit">Đang trung chuyển</span>` | Kiện đã đóng bao và xe tải Linehaul đang chạy. |
| `OUT_FOR_DELIVERY` | Đang phát hàng | `#F26522` (Safety Orange) | Trắng | `<span class="badge bg-ute-accent fw-bold text-uppercase">Đang phát hàng</span>` | Shipper bưu cục phát đang giao kiện tận tay người nhận. |
| `FAILED` | Giao thất bại | `#DC2626` (Crimson Red) | Trắng | `<span class="badge bg-danger fw-bold">Giao thất bại</span>` | Không phát được (Hẹn lại hoặc Khách từ chối). |
| `DELIVERED` | Giao thành công | `#16A34A` (Emerald Green) | Trắng | `<span class="badge bg-success fw-bold">Giao thành công</span>` | Khách đã nhận hàng và hoàn tất tiền COD. |
| `RETURN_TO_SENDER` | Đang chuyển hoàn | `#C2410C` (Rust Orange) | Trắng | `<span class="badge text-white fw-bold" style="background-color: #C2410C;">Đang chuyển hoàn</span>` hoặc `<span class="badge badge-return">Đang chuyển hoàn</span>` | Đang quay đầu về lại bưu cục gốc. |
| `RETURNED_TO_SENDER` | Đã hoàn trả | `#475569` (Dark Slate) | Trắng | `<span class="badge bg-secondary fw-bold">Đã hoàn trả</span>` | Người gửi đã nhận lại kiện và thanh toán phí hoàn. |
| `CANCELLED` | Đã hủy | `#991B1B` (Dark Red) | Trắng | `<span class="badge bg-danger text-white">Đã hủy</span>` | Hủy đơn trước khi kiện vào mạng lưới. |
| `CLOSED_EXCEPTION` | Đóng ngoại lệ | `#0F172A` (Obsidian Dark) | Trắng | `<span class="badge bg-dark text-white fw-bold">Đóng ngoại lệ</span>` | Sự cố mất hàng đền bù hoặc tồn kho tiêu hủy. |

> [!IMPORTANT]
> **Cấu hình lớp CSS Badge tiện ích trong `ute-express.css`:**
> Nhóm phát triển định nghĩa sẵn các class để gọi tiện lợi trong thẻ JSP:
> ```css
> .badge-picked-up { background-color: #4F46E5 !important; color: #FFFFFF !important; }
> .badge-in-transit { background-color: #7C3AED !important; color: #FFFFFF !important; }
> .badge-return { background-color: #C2410C !important; color: #FFFFFF !important; }
> .badge-at-hub { background-color: #0F3D78 !important; color: #FFFFFF !important; }
> .badge-out-delivery { background-color: #F26522 !important; color: #FFFFFF !important; }
> ```

---

## 4. MẪU COMPONENT CHUẨN HÓA (REUSABLE UI COMPONENTS)

### 4.1. Cấu trúc Thẻ Kiện hàng (Shipment Card)
```jsp
<div class="card border-0 shadow-sm mb-3 rounded-3 bg-white">
    <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
        <div>
            <span class="text-muted small">Mã vận đơn:</span>
            <span class="fw-bold text-ute-primary font-monospace ms-1 fs-6">
                <c:out value="${shipment.trackingNumber}" />
            </span>
        </div>
        <div>
            <%-- Render Badge theo đúng quy chuẩn Mục 3 --%>
            <c:choose>
                <c:when test="${shipment.currentStatus == 'DELIVERED'}">
                    <span class="badge bg-success fw-bold">Giao thành công</span>
                </c:when>
                <c:when test="${shipment.currentStatus == 'OUT_FOR_DELIVERY'}">
                    <span class="badge bg-ute-accent fw-bold text-uppercase">Đang phát hàng</span>
                </c:when>
                <c:when test="${shipment.currentStatus == 'AT_HUB'}">
                    <span class="badge bg-ute-primary fw-bold">Tại bưu cục</span>
                </c:when>
                <c:when test="${shipment.currentStatus == 'FAILED'}">
                    <span class="badge bg-danger fw-bold">Giao thất bại</span>
                </c:when>
                <c:otherwise>
                    <span class="badge bg-secondary"><c:out value="${shipment.currentStatus}" /></span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="card-body">
        <div class="row g-2">
            <div class="col-md-6 border-end-md">
                <p class="mb-1 small text-muted"><i class="bi bi-geo-alt text-ute-primary me-1"></i>Người gửi:</p>
                <p class="mb-0 fw-semibold text-ute-dark"><c:out value="${shipment.senderName}" /> - <c:out value="${shipment.senderPhone}" /></p>
                <p class="small text-muted mb-0 text-truncate"><c:out value="${shipment.senderAddress}" /></p>
            </div>
            <div class="col-md-6">
                <p class="mb-1 small text-muted"><i class="bi bi-pin-map text-ute-accent me-1"></i>Người nhận:</p>
                <p class="mb-0 fw-semibold text-ute-dark"><c:out value="${shipment.receiverName}" /> - <c:out value="${shipment.receiverPhone}" /></p>
                <p class="small text-muted mb-0 text-truncate"><c:out value="${shipment.receiverAddress}" /></p>
            </div>
        </div>
    </div>
    <div class="card-footer bg-white border-top py-2 d-flex justify-content-between align-items-center">
        <span class="small text-muted">
            Tiền thu hộ COD: <strong class="text-danger fs-6"><fmt:formatNumber value="${shipment.codAmount}" type="currency" currencySymbol="₫" /></strong>
        </span>
        <a href="${pageContext.request.contextPath}/shipments/detail?id=${shipment.id}" class="btn btn-sm btn-outline-secondary rounded-pill px-3">
            Chi tiết <i class="bi bi-arrow-right ms-1"></i>
        </a>
    </div>
</div>
```

---

### 4.2. Cấu trúc Bảng Danh sách Nghiệp vụ (Data Table)
```jsp
<div class="table-responsive bg-white rounded-3 shadow-sm p-3">
    <table class="table table-hover align-middle mb-0">
        <thead class="table-light border-bottom">
            <tr>
                <th scope="col" class="text-center text-muted" style="width: 50px;">#</th>
                <th scope="col" class="text-ute-primary fw-bold">Mã vận đơn</th>
                <th scope="col">Người gửi</th>
                <th scope="col">Người nhận</th>
                <th scope="col" class="text-end">Tiền COD</th>
                <th scope="col" class="text-center">Trạng thái</th>
                <th scope="col" class="text-center" style="width: 130px;">Thao tác</th>
            </tr>
        </thead>
        <tbody>
            <c:forEach var="item" items="${shipments}" varStatus="loop">
                <tr>
                    <td class="text-center text-muted"><c:out value="${loop.index + 1}" /></td>
                    <td>
                        <span class="font-monospace fw-bold text-ute-primary">
                            <c:out value="${item.trackingNumber}" />
                        </span>
                    </td>
                    <td><c:out value="${item.senderName}" /></td>
                    <td><c:out value="${item.receiverName}" /></td>
                    <td class="text-end fw-bold text-danger">
                        <fmt:formatNumber value="${item.codAmount}" type="currency" currencySymbol="₫" />
                    </td>
                    <td class="text-center">
                        <%-- Badge theo quy chuẩn Mục 3 --%>
                    </td>
                    <td class="text-center">
                        <a href="${pageContext.request.contextPath}/shipments/detail?id=${item.id}" class="btn btn-sm btn-light border me-1" title="Chi tiết">
                            <i class="bi bi-eye text-ute-primary"></i>
                        </a>
                        <c:if test="${item.currentStatus == 'OUT_FOR_DELIVERY'}">
                            <a href="${pageContext.request.contextPath}/shipper/deliver?id=${item.id}" class="btn btn-sm btn-ute-accent" title="Giao hàng">
                                <i class="bi bi-check2-circle"></i>
                            </a>
                        </c:if>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty shipments}">
                <tr>
                    <td colspan="7" class="text-center py-5 text-muted">
                        <i class="bi bi-inbox fs-1 d-block mb-2 text-secondary opacity-50"></i>
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
Nút Submit kích hoạt hành động vận đơn áp dụng **Express Safety Orange (`btn-ute-accent`)**:
```jsp
<form action="${pageContext.request.contextPath}/user/shipments/create" method="post" class="needs-validation bg-white p-4 rounded-3 shadow-sm">
    <!-- CSRF Token -->
    <div class="mb-3">
        <label for="receiverPhone" class="form-label fw-semibold text-ute-dark">Số điện thoại người nhận <span class="text-danger">*</span></label>
        <input type="tel" 
               class="form-control ${not empty errors.receiverPhone ? 'is-invalid' : ''}" 
               id="receiverPhone" 
               name="receiverPhone" 
               value="${fn:escapeXml(formData.receiverPhone)}" 
               placeholder="Ví dụ: 0981234567" 
               required>
        <c:if test="${not empty errors.receiverPhone}">
            <div class="invalid-feedback d-block fw-semibold">
                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.receiverPhone}" />
            </div>
        </c:if>
    </div>

    <div class="mb-3">
        <label for="weight" class="form-label fw-semibold text-ute-dark">Khối lượng kiện hàng (gram) <span class="text-danger">*</span></label>
        <input type="number" 
               class="form-control ${not empty errors.weight ? 'is-invalid' : ''}" 
               id="weight" 
               name="weight" 
               value="${fn:escapeXml(formData.weight)}" 
               min="100" 
               max="50000" 
               required>
        <c:if test="${not empty errors.weight}">
            <div class="invalid-feedback d-block fw-semibold">
                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.weight}" />
            </div>
        </c:if>
    </div>

    <div class="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
        <a href="${pageContext.request.contextPath}/user/shipments" class="btn btn-light px-4 border">Hủy bỏ</a>
        <button type="submit" class="btn btn-ute-accent px-4 py-2 shadow-sm">
            <i class="bi bi-box-seam me-1"></i> Tạo Vận Đơn Ngay
        </button>
    </div>
</form>
```

---

### 4.4. Cấu trúc Dòng thời gian Hành trình (Vertical Stepper Timeline)
```html
<div class="tracking-timeline position-relative ps-4 my-4">
    <!-- Step 1: Đã giao thành công -->
    <div class="timeline-item position-relative pb-4">
        <div class="timeline-marker bg-success text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 30px; height: 30px; margin-left: -15px;">
            <i class="bi bi-check-lg fw-bold"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-bold mb-0 text-success">Giao hàng thành công</h6>
            <p class="small text-muted mb-1">14:30 - 15/09/2026 | Người nhận đã ký nhận</p>
            <p class="small text-ute-dark mb-0">Shipper Nguyễn Văn B (Mã: SHP01) đã thu hộ COD 500.000₫.</p>
        </div>
    </div>

    <!-- Step 2: Đang phát hàng (Active State mang màu Express Safety Orange) -->
    <div class="timeline-item position-relative pb-4">
        <div class="timeline-marker bg-ute-accent text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 30px; height: 30px; margin-left: -15px;">
            <i class="bi bi-truck small"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-bold mb-0 text-ute-accent">Đang trên đường giao hàng</h6>
            <p class="small text-muted mb-1">08:15 - 15/09/2026 | Bưu cục phát: Cần Thơ Delivery Hub</p>
        </div>
    </div>

    <!-- Step 3: Nhập bưu cục (Màu Deep Cobalt Blue) -->
    <div class="timeline-item position-relative">
        <div class="timeline-marker bg-ute-primary text-white rounded-circle position-absolute start-0 top-0 d-flex align-items-center justify-content-center" style="width: 30px; height: 30px; margin-left: -15px;">
            <i class="bi bi-building small"></i>
        </div>
        <div class="timeline-content ms-3">
            <h6 class="fw-semibold mb-0 text-ute-dark">Kiện đã nhập bưu cục gốc</h6>
            <p class="small text-muted mb-0">19:00 - 14/09/2026 | Bưu cục: Dĩ An Delivery Hub</p>
        </div>
    </div>
</div>
```

---

### 4.5. Cơ chế Che mờ Dữ liệu Nhạy cảm (Data Masking)
Bảo đảm tính riêng tư trên màn hình Public Tracking:
* **Số điện thoại:** Chỉ hiển thị 3 số đầu và 3 số cuối: `098****456`.
* **Địa chỉ:** Ẩn số nhà/tên đường cụ thể: `***, Phường Linh Trung, Thành phố Thủ Đức, TP. Hồ Chí Minh`.
* **Họ tên:** Ẩn ký tự giữa: `Ng*** V** A**`.
* **Markup chuẩn:**
```jsp
<div class="alert alert-warning border-0 bg-opacity-25 py-2 d-flex align-items-center gap-2 mb-3" style="background-color: #FFF0E8; color: #C2410C;">
    <i class="bi bi-shield-lock-fill fs-5"></i>
    <span class="small">Thông tin cá nhân được che mờ tự động để bảo vệ quyền riêng tư. Nhập mã OTP hoặc đăng nhập để xem đầy đủ.</span>
</div>
<p class="mb-1"><strong>SĐT Người nhận:</strong> <span class="font-monospace text-ute-primary fw-bold"><c:out value="${maskedReceiverPhone}" /></span></p>
<p class="mb-0"><strong>Địa chỉ giao:</strong> <c:out value="${maskedReceiverAddress}" /></p>
```
