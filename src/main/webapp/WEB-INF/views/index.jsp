<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="https://jakarta.ee/tags/jstl/core" %>
<%@ taglib prefix="fmt" uri="https://jakarta.ee/tags/jstl/fmt" %>
<%@ taglib prefix="fn" uri="https://jakarta.ee/tags/jstl/functions" %>

<jsp:include page="layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">

    <!-- Hero Section: Tra cứu nhanh & Giới thiệu -->
    <div class="row g-4 mb-4">
        <div class="col-lg-8">
            <div class="card-ute p-4 p-lg-5 text-white" style="background: linear-gradient(135deg, var(--ute-primary) 0%, var(--ute-primary-dark) 100%);">
                <div class="d-inline-block px-3 py-1 rounded-pill bg-white bg-opacity-20 small fw-semibold mb-3">
                    <i class="bi bi-shield-check text-ute-accent me-1"></i> Logistics Chuẩn Doanh Nghiệp HCMUTE
                </div>
                <h1 class="display-6 fw-bold mb-3">Giao Hàng Nhanh Chóng, <br>Theo Dõi Vận Đơn Thời Gian Thực</h1>
                <p class="lead opacity-90 mb-4" style="max-width: 620px;">
                    Hệ thống quản lý chuỗi logistics 3 phân hệ dọc: Trung chuyển Liên Hub & Linehaul xe tải, Cổng Khách hàng & Shipper hiện trường, Quản trị Mạng lưới & Đối soát tài chính.
                </p>

                <!-- Form tra cứu nhanh -->
                <form action="${pageContext.request.contextPath}/tracking" method="get" class="row g-2 align-items-center bg-white p-2 rounded-3 shadow" style="max-width: 580px;">
                    <div class="col-sm-8">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-0 text-muted ps-2"><i class="bi bi-search"></i></span>
                            <input type="text" name="trackingNumber" class="form-control border-0 font-monospace text-ute-primary fw-bold" placeholder="Nhập mã vận đơn (VD: UTE889900)..." required>
                        </div>
                    </div>
                    <div class="col-sm-4">
                        <button type="submit" class="btn btn-ute-accent w-100 py-2">
                            <i class="bi bi-geo-alt-fill me-1"></i> Tra cứu ngay
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <div class="col-lg-4">
            <jsp:include page="layout/sidebar.jsp" />
        </div>
    </div>

    <!-- 3 Thống kê Phân hệ Dọc -->
    <div class="row g-3 mb-4">
        <div class="col-md-4">
            <div class="card-ute p-3 border-start border-4 border-ute-primary">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="text-ute-muted small fw-semibold">PHÂN HỆ 1 (TEAM LEAD)</span>
                        <h5 class="fw-bold text-ute-dark mt-1 mb-0">Vận hành Bưu cục & Hub</h5>
                        <small class="text-secondary">9 Thực thể | Quét Inbound/Outbound</small>
                    </div>
                    <div class="bg-ute-primary bg-opacity-10 text-ute-primary p-3 rounded-circle">
                        <i class="bi bi-buildings-fill fs-4"></i>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-4">
            <div class="card-ute p-3 border-start border-4" style="border-left-color: var(--ute-accent) !important;">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="text-ute-muted small fw-semibold">PHÂN HỆ 2 (THÀNH VIÊN 2)</span>
                        <h5 class="fw-bold text-ute-dark mt-1 mb-0">Khách hàng & Shipper</h5>
                        <small class="text-secondary">7 Thực thể | Realtime STOMP & OTP</small>
                    </div>
                    <div class="bg-ute-accent bg-opacity-10 text-ute-accent p-3 rounded-circle">
                        <i class="bi bi-person-fill-gear fs-4"></i>
                    </div>
                </div>
            </div>
        </div>

        <div class="col-md-4">
            <div class="card-ute p-3 border-start border-4 border-secondary">
                <div class="d-flex justify-content-between align-items-center">
                    <div>
                        <span class="text-ute-muted small fw-semibold">PHÂN HỆ 3 (THÀNH VIÊN 3)</span>
                        <h5 class="fw-bold text-ute-dark mt-1 mb-0">Quản trị & Tài chính</h5>
                        <small class="text-secondary">9 Thực thể | Đối soát ca & Ví tiền</small>
                    </div>
                    <div class="bg-secondary bg-opacity-10 text-secondary p-3 rounded-circle">
                        <i class="bi bi-wallet-fill fs-4"></i>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Thẩm định Trực quan: 13 Status Badges Tương phản Cao (Theo docs/UI_DESIGN_SYSTEM.md) -->
    <div class="card-ute p-4 mb-4">
        <div class="d-flex justify-content-between align-items-center mb-3 pb-2 border-bottom">
            <div>
                <h5 class="fw-bold text-ute-dark mb-1">
                    <i class="bi bi-palette2 text-ute-primary me-2"></i>Quy chuẩn 13 Trạng thái Vận đơn (ShipmentStatus Badges)
                </h5>
                <p class="text-ute-muted small mb-0">Bảng màu tương phản cao chuyên biệt cho môi trường kho vận và shipper ngoài hiện trường.</p>
            </div>
            <span class="badge bg-success"><i class="bi bi-check-circle me-1"></i>Đạt chuẩn UI/UX HCMUTE</span>
        </div>

        <div class="table-responsive">
            <table class="table table-hover table-ute align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 50px;">STT</th>
                        <th>Mã Enum ShipmentStatus</th>
                        <th>Nhãn Tiếng Việt</th>
                        <th class="text-center">Mẫu Badge Trực quan</th>
                        <th>Ý nghĩa Nghiệp vụ Logistics</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>1</td>
                        <td><code>CREATED</code></td>
                        <td>Mới tạo</td>
                        <td class="text-center"><span class="badge badge-created px-3 py-2">Mới tạo</span></td>
                        <td class="small text-muted">Người gửi tạo đơn thành công trên web, chờ thanh toán hoặc mang ra bưu cục.</td>
                    </tr>
                    <tr>
                        <td>2</td>
                        <td><code>WAITING_FOR_PAYMENT</code></td>
                        <td>Chờ thanh toán</td>
                        <td class="text-center"><span class="badge badge-waiting-payment px-3 py-2">Chờ thanh toán</span></td>
                        <td class="small text-muted">Người gửi chọn thanh toán online qua VNPay, cước chưa được thanh toán.</td>
                    </tr>
                    <tr>
                        <td>3</td>
                        <td><code>WAITING_FOR_HANDOVER</code></td>
                        <td>Chờ bàn giao</td>
                        <td class="text-center"><span class="badge badge-waiting-handover px-3 py-2">Chờ bàn giao</span></td>
                        <td class="small text-muted">Đã thanh toán (hoặc trả sau), chờ Shipper đến lấy hoặc Sender mang ra Hub.</td>
                    </tr>
                    <tr>
                        <td>4</td>
                        <td><code>PICKED_UP</code></td>
                        <td>Đã lấy hàng</td>
                        <td class="text-center"><span class="badge badge-picked-up px-3 py-2">Đã lấy hàng</span></td>
                        <td class="small text-muted">Shipper quét mã nhận kiện tận nhà từ tay người gửi.</td>
                    </tr>
                    <tr>
                        <td>5</td>
                        <td><code>AT_HUB</code></td>
                        <td>Tại bưu cục</td>
                        <td class="text-center"><span class="badge badge-at-hub px-3 py-2">Tại bưu cục</span></td>
                        <td class="small text-muted">Kiện đã quét mã Inbound tại bưu cục gốc hoặc bưu cục trung chuyển.</td>
                    </tr>
                    <tr>
                        <td>6</td>
                        <td><code>IN_TRANSIT</code></td>
                        <td>Đang trung chuyển</td>
                        <td class="text-center"><span class="badge badge-in-transit px-3 py-2">Đang trung chuyển</span></td>
                        <td class="small text-muted">Kiện đã đóng bao niêm phong và xe tải Linehaul đang vận chuyển trên đường.</td>
                    </tr>
                    <tr>
                        <td>7</td>
                        <td><code>OUT_FOR_DELIVERY</code></td>
                        <td>Đang phát hàng</td>
                        <td class="text-center"><span class="badge badge-out-delivery px-3 py-2">Đang phát hàng</span></td>
                        <td class="small text-muted">Shipper bưu cục phát đã nhận kiện và bắt đầu ca giao cho người nhận.</td>
                    </tr>
                    <tr>
                        <td>8</td>
                        <td><code>FAILED</code></td>
                        <td>Giao thất bại</td>
                        <td class="text-center"><span class="badge badge-failed px-3 py-2">Giao thất bại</span></td>
                        <td class="small text-muted">Hết 3 lần giao (Nhóm 1) hoặc kích hoạt Fast-Fail ngay lần đầu (Nhóm 2: Bom hàng).</td>
                    </tr>
                    <tr>
                        <td>9</td>
                        <td><code>DELIVERED</code></td>
                        <td>Giao thành công</td>
                        <td class="text-center"><span class="badge badge-delivered px-3 py-2">Giao thành công</span></td>
                        <td class="small text-muted">Người nhận ký nhận và nộp đủ tiền thu hộ COD.</td>
                    </tr>
                    <tr>
                        <td>10</td>
                        <td><code>RETURN_TO_SENDER</code></td>
                        <td>Đang chuyển hoàn</td>
                        <td class="text-center"><span class="badge badge-return px-3 py-2">Đang chuyển hoàn</span></td>
                        <td class="small text-muted">Hàng quay đầu trên đường về lại bưu cục gốc hoặc chờ Sender nhận.</td>
                    </tr>
                    <tr>
                        <td>11</td>
                        <td><code>RETURNED_TO_SENDER</code></td>
                        <td>Đã hoàn trả</td>
                        <td class="text-center"><span class="badge badge-returned px-3 py-2">Đã hoàn trả</span></td>
                        <td class="small text-muted">Người gửi đã nộp phí hoàn và nhận lại kiện hàng.</td>
                    </tr>
                    <tr>
                        <td>12</td>
                        <td><code>CANCELLED</code></td>
                        <td>Đã hủy</td>
                        <td class="text-center"><span class="badge badge-cancelled px-3 py-2">Đã hủy</span></td>
                        <td class="small text-muted">Đơn hàng bị hủy trước khi kiện vào mạng lưới vận chuyển.</td>
                    </tr>
                    <tr>
                        <td>13</td>
                        <td><code>CLOSED_EXCEPTION</code></td>
                        <td>Đóng ngoại lệ</td>
                        <td class="text-center"><span class="badge badge-closed-exception px-3 py-2">Đóng ngoại lệ</span></td>
                        <td class="small text-muted">Đóng kết thúc do sự cố: Mất hàng bồi thường (LOST) hoặc hàng quá hạn tiêu hủy (DISPOSED).</td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>

</main>

<jsp:include page="layout/footer.jsp" />
