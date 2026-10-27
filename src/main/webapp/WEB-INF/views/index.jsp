<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<jsp:include page="layout/header.jsp" />

<main class="container px-3 flex-grow-1">

    <section class="ute-landing-hero">
        <h1>Gửi hàng dễ dàng.<br>Theo dõi từng bước.</h1>
        <p>Từ bưu cục gốc đến tận tay người nhận, bạn luôn biết hàng đang ở đâu và khi nào sẽ giao tới.</p>
        <form action="${ctx}/tracking" method="get" class="ute-search-pill">
            <input type="text" name="trackingNumber" placeholder="Nhập mã vận đơn để tra cứu" aria-label="Mã vận đơn" required>
            <button type="submit" class="btn btn-ute-accent"><i class="bi bi-search me-1"></i> Tra cứu</button>
        </form>
    </section>

    <section class="ute-tiles" aria-label="Dịch vụ">
        <a class="ute-tile" href="${ctx}/user/shipments/create">
            <i class="bi bi-box-seam"></i>
            <h3>Tạo đơn hàng</h3>
            <p>Nhập thông tin, xem ngay cước phí và chọn lấy hàng tại nhà hoặc mang ra bưu cục.</p>
        </a>
        <a class="ute-tile" href="${ctx}/tracking">
            <i class="bi bi-geo-alt"></i>
            <h3>Theo dõi vận đơn</h3>
            <p>Hành trình chi tiết và ngày dự kiến giao, không cần đăng nhập.</p>
        </a>
        <a class="ute-tile" href="${ctx}/user/wallet">
            <i class="bi bi-wallet2"></i>
            <h3>Ví điện tử</h3>
            <p>Nạp tiền, thanh toán cước và rút tiền nhanh chóng, minh bạch.</p>
        </a>
        <a class="ute-tile" href="${ctx}/login">
            <i class="bi bi-person-circle"></i>
            <h3>Tài khoản</h3>
            <p>Đăng nhập hoặc đăng ký để quản lý đơn hàng và nhận thông báo.</p>
        </a>
    </section>

    <section class="ute-steps" aria-label="Quy trình">
        <div><span class="n">1</span><h3 class="h6 fw-bold">Tạo đơn</h3><p>Điền thông tin người gửi, người nhận và kiện hàng.</p></div>
        <div><span class="n">2</span><h3 class="h6 fw-bold">Bàn giao</h3><p>Shipper đến lấy hoặc bạn mang ra bưu cục gần nhất.</p></div>
        <div><span class="n">3</span><h3 class="h6 fw-bold">Vận chuyển</h3><p>Hàng đi qua mạng lưới bưu cục, theo dõi theo thời gian thực.</p></div>
        <div><span class="n">4</span><h3 class="h6 fw-bold">Giao hàng</h3><p>Shipper giao tận nơi, thu hộ COD nếu có.</p></div>
    </section>

</main>

<jsp:include page="layout/footer.jsp" />
