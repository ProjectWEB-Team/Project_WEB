<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />

<jsp:include page="layout/header.jsp" />

<main class="ute-home flex-grow-1">

    <section class="ute-home-hero" aria-labelledby="hero-title">
        <div class="ute-wrap">
            <div>
                <h1 id="hero-title">Gửi hàng đi mọi tỉnh thành, theo dõi từng bước</h1>
                <p class="ute-lead">UTEExpress là dịch vụ giao nhận hàng. Bạn tạo đơn trên web, shipper đến lấy hoặc bạn mang ra bưu cục, rồi theo dõi hàng cho đến khi tới tay người nhận.</p>
                <div class="ute-home-actions">
                    <div class="ute-action-card">
                        <h2>Tra cứu vận đơn</h2>
                        <form class="ute-search" action="${ctx}/tracking" method="get" role="search">
                            <label class="visually-hidden" for="hero-code">Mã vận đơn</label>
                            <input id="hero-code" type="text" name="code" placeholder="Nhập mã, ví dụ UTX-20261027-0001" maxlength="50" autocomplete="off" required aria-describedby="hero-help">
                            <button type="submit" class="btn btn-ute-accent">Tra cứu</button>
                        </form>
                        <p id="hero-help">Mã nằm trên biên nhận hoặc tin nhắn xác nhận. Không cần đăng nhập.</p>
                    </div>
                    <div class="ute-action-card">
                        <h2>Muốn gửi hàng?</h2>
                        <a class="btn btn-ute-accent" href="${ctx}/user/shipments/create">Tạo đơn gửi hàng</a>
                        <p>Báo giá hiện ngay trước khi bạn xác nhận. Cần đăng nhập để tạo đơn.</p>
                    </div>
                </div>
            </div>
            <div class="ute-example" aria-label="Ví dụ trang theo dõi">
                <p class="ute-example-tag">Ví dụ minh họa: trang theo dõi một đơn</p>
                <p class="ute-example-code">UTX-20261027-0001</p>
                <p class="mb-3 text-muted">Dự kiến giao trước 31/10/2026</p>
                <ol class="ute-timeline">
                    <li><strong>Đã lấy hàng</strong><span>27/10 09:12 · Bưu cục Thủ Đức</span></li>
                    <li><strong>Tại bưu cục gốc</strong><span>27/10 11:40 · Hub TP. Hồ Chí Minh</span></li>
                    <li class="is-now"><strong>Đang trung chuyển</strong><span>27/10 14:40 · Chạy liên tỉnh</span></li>
                    <li><strong>Giao tận tay</strong><span>Dự kiến trước 31/10</span></li>
                </ol>
            </div>
        </div>
    </section>

    <section class="ute-home-sec" aria-labelledby="services-title">
        <div class="ute-wrap">
            <h2 id="services-title">Chọn dịch vụ phù hợp</h2>
            <p class="ute-sec-lead">Bạn chọn dịch vụ khi tạo đơn và thấy cước phí ngay, không phải chờ báo giá.</p>
            <div class="ute-grid-3">
                <div class="ute-card-plain"><i class="bi bi-box-seam" aria-hidden="true"></i><h3>Tiêu chuẩn</h3><p>Phù hợp phần lớn đơn hàng, cước tính theo vùng, khối lượng và kích thước.</p></div>
                <div class="ute-card-plain"><i class="bi bi-lightning-charge" aria-hidden="true"></i><h3>Nhanh</h3><p>Dành cho đơn cần đến sớm hơn, chọn ngay khi tạo đơn.</p></div>
                <div class="ute-card-plain"><i class="bi bi-rocket-takeoff" aria-hidden="true"></i><h3>Hỏa tốc</h3><p>Dành cho đơn gấp. Giá hiển thị trước khi bạn xác nhận.</p></div>
            </div>
        </div>
    </section>

    <section class="ute-home-sec alt" aria-labelledby="how-title">
        <div class="ute-wrap">
            <h2 id="how-title">Gửi hàng gồm 4 bước</h2>
            <p class="ute-sec-lead">Mỗi bước đều có mốc thời gian trên trang theo dõi.</p>
            <ol class="ute-steps">
                <li><h3>Tạo đơn</h3><p>Điền thông tin người gửi, người nhận, kiện hàng và xem báo giá.</p></li>
                <li><h3>Bàn giao</h3><p>Shipper đến lấy tại nhà, hoặc bạn mang hàng ra bưu cục gần nhất.</p></li>
                <li><h3>Vận chuyển</h3><p>Hàng đi qua mạng lưới bưu cục và xe trung chuyển tới nơi nhận.</p></li>
                <li><h3>Giao hàng</h3><p>Shipper giao tận tay, thu hộ tiền COD nếu có.</p></li>
            </ol>
        </div>
    </section>

    <section class="ute-home-sec" aria-labelledby="features-title">
        <div class="ute-wrap">
            <h2 id="features-title">Những gì bạn làm được</h2>
            <p class="ute-sec-lead">&nbsp;</p>
            <div class="ute-grid-3">
                <div class="ute-card-plain"><i class="bi bi-search" aria-hidden="true"></i><h3>Tra cứu không cần tài khoản</h3><p>Nhập mã vận đơn là xem được hành trình của đơn.</p></div>
                <div class="ute-card-plain"><i class="bi bi-calculator" aria-hidden="true"></i><h3>Biết giá trước khi gửi</h3><p>Cước hiện ngay khi bạn điền xong thông tin, trước khi xác nhận tạo đơn.</p></div>
                <div class="ute-card-plain"><i class="bi bi-cash-coin" aria-hidden="true"></i><h3>Thu hộ COD</h3><p>Shipper thu tiền khi giao, quản lý đối soát từng ca rồi tiền về ví.</p></div>
                <div class="ute-card-plain"><i class="bi bi-wallet2" aria-hidden="true"></i><h3>Ví điện tử</h3><p>Nhận hoàn tiền, theo dõi số dư và rút về ngân hàng.</p></div>
                <div class="ute-card-plain"><i class="bi bi-house-door" aria-hidden="true"></i><h3>Lấy hàng tại nhà</h3><p>Hẹn shipper đến lấy, hoặc tự mang ra bưu cục nếu tiện hơn.</p></div>
                <div class="ute-card-plain"><i class="bi bi-bell" aria-hidden="true"></i><h3>Cập nhật theo thời gian thực</h3><p>Trạng thái đơn đổi là bạn thấy ngay, không cần tải lại trang.</p></div>
            </div>
        </div>
    </section>

    <section id="faq" class="ute-home-sec alt" aria-labelledby="faq-title">
        <div class="ute-wrap">
            <h2 id="faq-title">Câu hỏi thường gặp</h2>
            <p class="ute-sec-lead">&nbsp;</p>
            <div class="ute-faq-list">
                <details><summary>Tôi tra cứu đơn bằng cách nào?</summary><p>Nhập mã vận đơn có dạng UTX-… vào ô tra cứu. Bạn không cần đăng nhập và sẽ thấy hành trình của đơn.</p></details>
                <details><summary>Cước phí tính thế nào?</summary><p>Hệ thống báo giá theo dịch vụ, vùng, khối lượng và kích thước. Bạn thấy giá trước khi xác nhận.</p></details>
                <details><summary>Đơn chờ thanh toán có hạn không?</summary><p>Có. Đơn thanh toán trực tuyến quá hạn sẽ tự hủy, và trang chi tiết đơn báo rõ lý do.</p></details>
                <details><summary>Thu hộ COD hoạt động ra sao?</summary><p>Shipper thu tiền khi giao. Quản lý đối soát theo ca rồi tiền về ví của bạn.</p></details>
                <details><summary>Hàng không giao được thì sao?</summary><p>Đơn có thể giao lại. Nếu vẫn không giao được, hàng được chuyển hoàn về người gửi.</p></details>
            </div>
        </div>
    </section>

    <section class="ute-home-cta" aria-labelledby="cta-title">
        <div class="ute-wrap">
            <div>
                <h2 id="cta-title">Sẵn sàng gửi đơn đầu tiên?</h2>
                <p>Đăng ký, xem báo giá và tạo đơn ngay trên web.</p>
            </div>
            <a class="btn btn-ute-accent" href="${ctx}/user/shipments/create">Tạo đơn gửi hàng</a>
        </div>
    </section>

</main>

<jsp:include page="layout/footer.jsp" />
