<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

    <footer class="ute-footer" id="ute-contact">
        <div class="ute-footer-inner">
            <div>
                <a href="${pageContext.request.contextPath}/" aria-label="UTEexpress — Trang chủ"><jsp:include page="/WEB-INF/views/layout/logo.jsp" /></a>
                <p class="mt-2 mb-3 small text-white">Giao nhận và theo dõi vận đơn từ bưu cục gốc đến tay người nhận.</p>
                <div class="ute-footer-contact">
                    <div class="ute-contact-row">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="m5 3 4 5-2 3a16 16 0 0 0 6 6l3-2 5 4-2 3C9 22 2 15 2 5Z"/></svg>
                        <span>Hotline: <a class="ute-hotline" href="tel:+84911195500">0911 195 500</a></span>
                    </div>
                    <div class="ute-contact-row">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 7 9 6 9-6"/></svg>
                        <span class="ute-contact-label">Email hỗ trợ</span>
                    </div>
                    <ul class="ute-contact-emails">
                        <li><a href="mailto:khangfithcmute@gmail.com">khangfithcmute@gmail.com</a></li>
                        <li><a href="mailto:24162091@student.hcmute.edu.vn">24162091@student.hcmute.edu.vn</a></li>
                        <li><a href="mailto:24162144@student.hcmute.edu.vn">24162144@student.hcmute.edu.vn</a></li>
                    </ul>
                </div>
            </div>
            <div>
                <h6 class="fw-bold mb-0">Dịch vụ</h6>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/tracking">Tra cứu vận đơn</a></li>
                    <li><a href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn</a></li>
                    <li><a href="${pageContext.request.contextPath}/user/wallet">Ví</a></li>
                </ul>
                <a class="btn btn-ute-accent ute-footer-cta mt-3" href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn ngay</a>
            </div>
            <div>
                <h6 class="fw-bold mb-0">Tài khoản</h6>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/login">Đăng nhập</a></li>
                    <li><a href="${pageContext.request.contextPath}/register">Đăng ký</a></li>
                    <li><a href="${pageContext.request.contextPath}/auth/forgot-password">Quên mật khẩu</a></li>
                </ul>
            </div>
            <div>
                <h6 class="fw-bold mb-0">Hỗ trợ</h6>
                <ul>
                    <li><a href="${pageContext.request.contextPath}/#faq">Câu hỏi thường gặp</a></li>
                    <li><a href="mailto:khangfithcmute@gmail.com">Gửi email hỗ trợ</a></li>
                </ul>
            </div>
            <p class="ute-legal text-white">&copy; 2026 UTEExpress. Đồ án Lập trình Web, Trường Đại học Sư phạm Kỹ thuật TP. Hồ Chí Minh.</p>
        </div>
    </footer>

    <!-- Bootstrap 5.3.3 Bundle JS with Popper -->
    <c:if test="${uteAssetsLoaded}"><script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script></c:if>
</body>
</html>
