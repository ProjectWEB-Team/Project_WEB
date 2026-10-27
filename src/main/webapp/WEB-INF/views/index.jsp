<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="uteTopBar" value="true" scope="request" />

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
                        <div class="d-flex flex-wrap gap-2"><a class="btn btn-ute-accent" href="${ctx}/user/shipments/create">Tạo đơn gửi hàng</a><a class="btn btn-outline-secondary" href="${ctx}/pricing">Xem bảng giá</a></div>
                        <p>Báo giá hiện ngay trước khi bạn xác nhận. Cần đăng nhập để tạo đơn; xem bảng giá thì không cần.</p>
                    </div>
                </div>
            </div>
            <div class="ute-example" aria-label="Ví dụ trang theo dõi">
                <p class="ute-example-tag">Ví dụ minh họa: trang theo dõi một đơn</p>
                <div class="ute-example-top">
                    <p class="ute-example-code">UTX-20261027-0001</p>
                    <span class="badge badge-in-transit">Đang trung chuyển</span>
                </div>
                <p class="mb-3 text-muted">Dự kiến giao trước 31/10/2026</p>
                <ol class="ute-timeline">
                    <li><strong>Đã lấy hàng</strong><span>27/10 09:12 · Bưu cục Thủ Đức</span></li>
                    <li><strong>Tại bưu cục gốc</strong><span>27/10 11:40 · Hub TP. Hồ Chí Minh</span></li>
                    <li class="is-now"><strong>Đang trung chuyển</strong><span>27/10 14:40 · Chạy liên tỉnh</span></li>
                    <li><strong>Giao tận tay</strong><span>Dự kiến trước 31/10</span></li>
                </ol>
                <dl class="ute-example-facts">
                    <div><dt>Gửi từ</dt><dd>Thủ Đức, TP. Hồ Chí Minh</dd></div>
                    <div><dt>Giao đến</dt><dd>Hà Đông, Hà Nội</dd></div>
                    <div><dt>Dịch vụ</dt><dd>Tiêu chuẩn</dd></div>
                    <div><dt>Thu hộ COD</dt><dd>250.000 đ</dd></div>
                </dl>
            </div>
        </div>
    </section>

    <section class="ute-home-sec" aria-labelledby="services-title">
        <div class="ute-wrap">
            <h2 id="services-title">Chọn dịch vụ phù hợp</h2>
            <p class="ute-sec-lead">Bạn chọn dịch vụ khi tạo đơn và thấy cước phí ngay. <a href="${ctx}/pricing">Xem bảng giá và ước tính cước</a> không cần đăng nhập.</p>
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
            <p class="ute-sec-lead">Bấm từng bước để xem bạn làm gì và hệ thống làm gì.</p>
            <div class="ute-tabs" data-tabs>
                <div class="ute-tablist" role="tablist" aria-label="Các bước gửi hàng">
                    <button type="button" role="tab" id="tab-1" aria-controls="panel-1" aria-selected="true"><span>1</span> Tạo đơn</button>
                    <button type="button" role="tab" id="tab-2" aria-controls="panel-2" aria-selected="false"><span>2</span> Bàn giao</button>
                    <button type="button" role="tab" id="tab-3" aria-controls="panel-3" aria-selected="false"><span>3</span> Vận chuyển</button>
                    <button type="button" role="tab" id="tab-4" aria-controls="panel-4" aria-selected="false"><span>4</span> Giao hàng</button>
                </div>

                <div class="ute-tabpanel" role="tabpanel" id="panel-1" aria-labelledby="tab-1">
                    <div>
                        <h3>Điền thông tin và xem giá ngay</h3>
                        <ul class="ute-ticks">
                            <li>Người gửi và người nhận: chọn tỉnh, phường/xã từ danh sách có sẵn.</li>
                            <li>Kiện hàng: khối lượng, kích thước, giá trị khai báo, tiền thu hộ COD nếu có.</li>
                            <li>Chọn dịch vụ Tiêu chuẩn, Nhanh hoặc Hỏa tốc.</li>
                            <li>Bấm "Xem báo giá": cước hiện ngay trước khi bạn xác nhận.</li>
                        </ul>
                        <a class="btn btn-ute-accent mt-3" href="${ctx}/user/shipments/create">Tạo đơn gửi hàng</a>
                    </div>
                    <div class="ute-mock" aria-label="Ví dụ báo giá">
                        <p class="ute-example-tag">Ví dụ minh họa: màn báo giá</p>
                        <div class="ute-mock-row"><span>Cước vận chuyển</span><b>34.000 đ</b></div>
                        <div class="ute-mock-row"><span>Phụ phí khối lượng</span><b>0 đ</b></div>
                        <div class="ute-mock-row"><span>Phí thu hộ COD</span><b>4.000 đ</b></div>
                        <div class="ute-mock-total"><span>Tổng cước</span><b>38.000 đ</b></div>
                    </div>
                </div>

                <div class="ute-tabpanel" role="tabpanel" id="panel-2" aria-labelledby="tab-2">
                    <div>
                        <h3>Chọn cách đưa hàng cho bưu cục</h3>
                        <ul class="ute-ticks">
                            <li>Lấy tại nhà: bưu cục phân công shipper đến địa chỉ của bạn.</li>
                            <li>Tự mang ra bưu cục: mang kiện hàng và mã vận đơn đến bưu cục gần nhất.</li>
                            <li>Khi nhận hàng, trạng thái đơn đổi thành "Đã lấy hàng".</li>
                        </ul>
                    </div>
                    <div class="ute-mock" aria-label="Ví dụ hai cách bàn giao">
                        <p class="ute-example-tag">Ví dụ minh họa: hai cách bàn giao</p>
                        <div class="ute-mock-choice"><b>Lấy tại nhà</b><span>Shipper đến địa chỉ người gửi</span></div>
                        <div class="ute-mock-choice"><b>Tự mang ra bưu cục</b><span>Bạn đến bưu cục, đưa mã vận đơn</span></div>
                        <p class="mb-0 mt-2"><span class="badge badge-picked-up">Đã lấy hàng</span></p>
                    </div>
                </div>

                <div class="ute-tabpanel" role="tabpanel" id="panel-3" aria-labelledby="tab-3">
                    <div>
                        <h3>Hàng đi qua mạng lưới bưu cục</h3>
                        <ul class="ute-ticks">
                            <li>Bưu cục gốc nhận, kiểm tra và phân loại theo tuyến.</li>
                            <li>Hàng được đóng bao, bàn giao cho tài xế xe trung chuyển.</li>
                            <li>Bưu cục nhận đối chiếu từng bao, báo ngay nếu thiếu kiện.</li>
                            <li>Mỗi mốc đều hiện trên trang theo dõi của bạn.</li>
                        </ul>
                    </div>
                    <div class="ute-mock" aria-label="Ví dụ hành trình qua mạng lưới">
                        <p class="ute-example-tag">Ví dụ minh họa: mạng lưới 3 cấp</p>
                        <div class="ute-route"><b>Bưu cục gốc</b><span>nhận và phân loại</span></div>
                        <div class="ute-route-arrow" aria-hidden="true">↓</div>
                        <div class="ute-route"><b>Hub vùng</b><span>đóng bao, chờ xe</span></div>
                        <div class="ute-route-arrow" aria-hidden="true">↓</div>
                        <div class="ute-route"><b>Bưu cục phát</b><span>chuẩn bị giao</span></div>
                        <p class="mb-0 mt-2"><span class="badge badge-in-transit">Đang trung chuyển</span></p>
                    </div>
                </div>

                <div class="ute-tabpanel" role="tabpanel" id="panel-4" aria-labelledby="tab-4">
                    <div>
                        <h3>Shipper giao tận tay</h3>
                        <ul class="ute-ticks">
                            <li>Shipper nhận kiện tại bưu cục phát và đi giao.</li>
                            <li>Có thu hộ COD thì shipper thu tiền khi giao, quản lý đối soát theo ca.</li>
                            <li>Không giao được: đơn có thể giao lại, nếu vẫn không được thì hoàn về người gửi.</li>
                        </ul>
                    </div>
                    <div class="ute-mock" aria-label="Ví dụ các kết quả giao hàng">
                        <p class="ute-example-tag">Ví dụ minh họa: kết quả giao hàng</p>
                        <p><span class="badge badge-out-delivery">Đang phát hàng</span></p>
                        <p><span class="badge badge-delivered">Giao thành công</span></p>
                        <p><span class="badge badge-failed">Giao thất bại</span></p>
                        <p class="mb-0"><span class="badge badge-return">Đang chuyển hoàn</span></p>
                    </div>
                </div>
            </div>
            <script>
                (function () {
                    var root = document.querySelector('[data-tabs]'); if (!root) return;
                    var tabs = root.querySelectorAll('[role=tab]'), panels = root.querySelectorAll('[role=tabpanel]');
                    function show(i) {
                        tabs.forEach(function (t, k) { t.setAttribute('aria-selected', k === i ? 'true' : 'false'); t.tabIndex = k === i ? 0 : -1; });
                        panels.forEach(function (p, k) { p.hidden = k !== i; });
                    }
                    tabs.forEach(function (t, i) {
                        t.addEventListener('click', function () { show(i); });
                        t.addEventListener('keydown', function (e) {
                            var n = e.key === 'ArrowRight' ? (i + 1) % tabs.length : e.key === 'ArrowLeft' ? (i + tabs.length - 1) % tabs.length : -1;
                            if (n >= 0) { show(n); tabs[n].focus(); e.preventDefault(); }
                        });
                    });
                    show(0);
                })();
            </script>
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
