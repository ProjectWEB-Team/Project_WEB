<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

    <c:if test="${not empty pageContext.request.userPrincipal}">
    <footer class="ute-footer ute-footer-compact" id="ute-contact">
        <div class="ute-footer-accent-line"></div>
        <div class="ute-footer-compact-body">
            <a href="${pageContext.request.contextPath}/" class="ute-footer-compact-brand" aria-label="UTEexpress — Trang chủ">
                <jsp:include page="/WEB-INF/views/layout/logo.jsp" />
                <span>Giao nhận nội địa — theo dõi từng bước.</span>
            </a>
            <div class="ute-footer-compact-contacts">
                <span class="ute-footer-compact-phone"><span class="ute-contact-badge-circle" aria-hidden="true"><svg width="12" height="12" viewBox="0 0 24 24" fill="#FFFFFF"><path d="M6.62 10.79a15.053 15.053 0 006.59 6.59l2.2-2.2a1 1 0 011.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.57 3.57a1 1 0 01-.25 1.02l-2.2 2.2z"/></svg></span>Hotline: <a href="tel:+84911195500">0911 195 500</a></span>
                <span>Email: khangfithcmute@gmail.com &nbsp;·&nbsp; 24162091@student.hcmute.edu.vn &nbsp;·&nbsp; 24162144@student.hcmute.edu.vn</span>
            </div>
            <p class="ute-footer-compact-legal">&copy; 2026 UTEExpress &middot; Trường Đại học Công nghệ Kỹ thuật TP. Hồ Chí Minh.</p>
        </div>
    </footer>
    </c:if>
    <c:if test="${empty pageContext.request.userPrincipal}">
    <footer class="ute-footer" id="ute-contact">
        <!-- ĐƯỜNG KẺ CAM 3PX (FIGMA footer-accent-line: 1440x3, #F26522) -->
        <div class="ute-footer-accent-line"></div>

        <!-- 4 CỘT CHÂN TRANG (FIGMA Footer / Four columns: 1440x305, pad: 48px 80px) -->
        <div class="ute-footer-columns-wrap">
            <div class="ute-footer-four-columns">
                <!-- CỘT 1: LOGO & GIỚI THIỆU (w: 340px) -->
                <div class="ute-footer-brand-col">
                    <a href="${pageContext.request.contextPath}/" class="ute-footer-logo-link" aria-label="UTEexpress — Trang chủ">
                        <jsp:include page="/WEB-INF/views/layout/logo.jsp" />
                    </a>
                    <p class="ute-footer-brand-desc">Dịch vụ giao nhận hàng hóa nội địa — tạo đơn nhanh, cước phí minh bạch, theo dõi hành trình từng bước.</p>
                </div>

                <!-- CỘT 2: DỊCH VỤ (w: 190px, all-caps "DỊCH VỤ") -->
                <div class="ute-footer-nav-col">
                    <h6 class="ute-footer-col-title">DỊCH VỤ</h6>
                    <ul class="ute-footer-links-list">
                        <li><a href="${pageContext.request.contextPath}/tracking">Tra cứu vận đơn</a></li>
                        <li><a href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn</a></li>
                        <li><a href="${pageContext.request.contextPath}/pricing">Bảng giá</a></li>
                        <li><a href="${pageContext.request.contextPath}/user/wallet">Ví điện tử</a></li>
                    </ul>
                </div>

                <!-- CỘT 3: TÀI KHOẢN (w: 190px, all-caps "TÀI KHOẢN") -->
                <div class="ute-footer-nav-col">
                    <h6 class="ute-footer-col-title">TÀI KHOẢN</h6>
                    <ul class="ute-footer-links-list">
                        <li><a href="${pageContext.request.contextPath}/login">Đăng nhập</a></li>
                        <li><a href="${pageContext.request.contextPath}/register">Đăng ký</a></li>
                        <li><a href="${pageContext.request.contextPath}/auth/forgot-password">Quên mật khẩu</a></li>
                        <li><a href="${pageContext.request.contextPath}/#faq">Câu hỏi thường gặp</a></li>
                    </ul>
                </div>

                <!-- CỘT 4: LIÊN HỆ HỖ TRỢ (w: 340px, all-caps "LIÊN HỆ HỖ TRỢ") -->
                <div class="ute-footer-support-col">
                    <h6 class="ute-footer-col-title-support">LIÊN HỆ HỖ TRỢ</h6>
                    <ul class="ute-footer-contact-list">
                        <!-- Hotline -->
                        <li class="ute-footer-contact-row">
                            <span class="ute-contact-badge-circle" aria-hidden="true">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="#FFFFFF"><path d="M6.62 10.79a15.053 15.053 0 006.59 6.59l2.2-2.2a1 1 0 011.02-.24c1.12.37 2.33.57 3.57.57a1 1 0 011 1V20a1 1 0 01-1 1A17 17 0 013 4a1 1 0 011-1h3.5a1 1 0 011 1c0 1.25.2 2.45.57 3.57a1 1 0 01-.25 1.02l-2.2 2.2z"/></svg>
                            </span>
                            <span class="ute-hotline-label">Hotline: <a href="tel:+84911195500" class="ute-hotline-val">0911 195 500</a></span>
                        </li>
                        <!-- Email 1 -->
                        <li class="ute-footer-contact-row">
                            <span class="ute-contact-badge-circle" aria-hidden="true">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="#FFFFFF"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                            </span>
                            <a href="mailto:khangfithcmute@gmail.com" class="ute-contact-mail-val">khangfithcmute@gmail.com</a>
                        </li>
                        <!-- Email 2 -->
                        <li class="ute-footer-contact-row">
                            <span class="ute-contact-badge-circle" aria-hidden="true">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="#FFFFFF"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                            </span>
                            <a href="mailto:24162091@student.hcmute.edu.vn" class="ute-contact-mail-val">24162091@student.hcmute.edu.vn</a>
                        </li>
                        <!-- Email 3 -->
                        <li class="ute-footer-contact-row">
                            <span class="ute-contact-badge-circle" aria-hidden="true">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="#FFFFFF"><path d="M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
                            </span>
                            <a href="mailto:24162144@student.hcmute.edu.vn" class="ute-contact-mail-val">24162144@student.hcmute.edu.vn</a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>

        <!-- DẢI BẢN QUYỀN (FIGMA footer-legal-strip: pad: 16px 80px 20px 80px, stroke: #556275) -->
        <div class="ute-footer-legal-strip">
            <div class="ute-footer-legal-inner">
                <p class="ute-footer-legal-text">
                    &copy; 2026 UTEExpress. Đồ án Lập trình Web &middot; Trường Đại học Công nghệ Kỹ thuật TP. Hồ Chí Minh.
                </p>
            </div>
        </div>
    </footer>
    </c:if>

    <!-- Bootstrap 5.3.3 Bundle JS with Popper -->
    <c:if test="${uteAssetsLoaded}"><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script></c:if>
</body>
</html>
