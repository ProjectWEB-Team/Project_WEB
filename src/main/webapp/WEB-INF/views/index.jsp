<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<jsp:include page="layout/header.jsp" />

<main class="flex-grow-1">

    <section class="ute-hero" aria-labelledby="hero-title">
        <div class="ute-hero-inner">
            <h1 id="hero-title"><span>Gửi hàng.</span><span>Thấy từng bước.</span></h1>
            <div class="ute-hero-row">
                <div>
                    <form class="ute-search" action="${ctx}/tracking" method="get" role="search">
                        <label class="visually-hidden" for="hero-code">Mã vận đơn</label>
                        <input id="hero-code" type="text" name="code" placeholder="UTX-20261027-0001" maxlength="50" autocomplete="off" required aria-describedby="hero-help">
                        <button type="submit" class="btn btn-ute-accent">Tra cứu</button>
                    </form>
                    <p id="hero-help" class="ute-hero-help">Mã vận đơn có dạng UTX-ngày-số, không cần đăng nhập.</p>
                </div>
                <p class="ute-hero-lead">Tra mã, tạo đơn có báo giá ngay và theo dõi hành trình từ bưu cục gốc đến tay người nhận.</p>
            </div>
        </div>
    </section>

    <section class="ute-band-ink" aria-labelledby="how-title">
        <div class="ute-section ute-how">
            <div>
                <h2 id="how-title">Một đơn, bốn chặng</h2>
                <p class="ute-how-lead">Mỗi chặng có người chịu trách nhiệm và một mốc thời gian trên trang theo dõi. Bạn luôn biết đơn đang ở đâu.</p>
            </div>
            <div>
                <div class="ute-slip">
                    <h3>Bạn</h3>
                    <ul>
                        <li>Tạo đơn và xem báo giá <span class="dots"></span> <b>Trên web</b></li>
                        <li>Mang ra bưu cục hoặc hẹn shipper <span class="dots"></span> <b>Bạn chọn</b></li>
                    </ul>
                    <h3>Mạng lưới</h3>
                    <ul>
                        <li>Bưu cục gốc nhận và phân loại <span class="dots"></span> <b>Hub</b></li>
                        <li>Đóng bao, bàn giao cho tài xế <span class="dots"></span> <b>Hub</b></li>
                        <li>Chạy liên tỉnh tới bưu cục phát <span class="dots"></span> <b>Tài xế</b></li>
                    </ul>
                    <h3>Giao tận tay</h3>
                    <ul>
                        <li>Shipper nhận kiện và đi giao <span class="dots"></span> <b>Shipper</b></li>
                        <li>Thu hộ COD nếu có <span class="dots"></span> <b>Shipper</b></li>
                    </ul>
                    <div class="ute-slip-result">Bạn nhận: mã UTX-… và mọi chặng trên trang theo dõi</div>
                </div>
                <div class="ute-slip-edge" aria-hidden="true"></div>
            </div>
        </div>
    </section>

    <section class="ute-section" aria-labelledby="caps-title">
        <ul class="ute-caps">
            <li class="ute-caps-head"><strong id="caps-title">Những gì bạn làm được</strong><span></span></li>
            <li><strong>Tra cứu</strong><span>Nhập mã, xem hành trình ngay. Không cần tài khoản.</span></li>
            <li><strong>Báo giá</strong><span>Cước hiện trước khi bạn xác nhận tạo đơn.</span></li>
            <li><strong>Thu hộ COD</strong><span>Shipper thu khi giao, quản lý đối soát từng ca.</span></li>
            <li><strong>Ví</strong><span>Trả cước, nhận hoàn tiền và rút về ngân hàng.</span></li>
            <li><strong>Lấy tại nhà</strong><span>Hẹn shipper đến lấy, hoặc mang ra bưu cục gần nhất.</span></li>
            <li><strong>Thời gian thực</strong><span>Trạng thái đổi là bạn thấy ngay, không cần tải lại.</span></li>
        </ul>
    </section>

    <section id="faq" class="ute-faq" aria-labelledby="faq-title">
        <div class="ute-section">
            <h2 id="faq-title" class="ute-faq-head">Câu hỏi thường gặp</h2>
            <dl>
                <div class="row-qa"><dt>Tôi tra cứu đơn bằng cách nào?</dt><dd>Nhập mã vận đơn có dạng UTX-… vào ô tra cứu. Bạn không cần đăng nhập và sẽ thấy hành trình của đơn.</dd></div>
                <div class="row-qa"><dt>Cước phí tính thế nào?</dt><dd>Hệ thống báo giá theo dịch vụ, vùng, khối lượng và kích thước. Bạn thấy giá trước khi xác nhận.</dd></div>
                <div class="row-qa"><dt>Đơn chờ thanh toán có hạn không?</dt><dd>Có. Đơn thanh toán trực tuyến quá hạn sẽ tự hủy, và trang chi tiết đơn báo rõ lý do.</dd></div>
                <div class="row-qa"><dt>Thu hộ COD hoạt động ra sao?</dt><dd>Shipper thu tiền khi giao. Quản lý đối soát theo ca rồi tiền về ví của bạn.</dd></div>
                <div class="row-qa"><dt>Hàng không giao được thì sao?</dt><dd>Đơn có thể giao lại. Nếu vẫn không giao được, hàng được chuyển hoàn về người gửi.</dd></div>
            </dl>
        </div>
    </section>

    <a class="ute-cta" href="${ctx}/user/shipments/create">
        <p>Báo giá hiện ngay trước khi bạn xác nhận đơn.</p>
        <span class="ute-cta-arrow" aria-hidden="true">↗</span>
        <span class="ute-cta-word">Tạo đơn</span>
    </a>

</main>

<jsp:include page="layout/footer.jsp" />
