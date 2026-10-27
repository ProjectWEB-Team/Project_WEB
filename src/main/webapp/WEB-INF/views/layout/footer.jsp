<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

    <!-- Footer -->
    <footer class="bg-ute-primary text-white mt-auto pt-4 pb-3 border-top border-secondary">
        <div class="container-fluid px-lg-4">
            <div class="row g-4 mb-3">
                <div class="col-lg-4 col-md-6">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <div class="bg-ute-accent text-white rounded-2 d-flex align-items-center justify-content-center ute-brand-mark-sm">
                            <i class="bi bi-box-seam-fill fs-6"></i>
                        </div>
                        <h6 class="mb-0 fw-bold tracking-wide">UTE<span class="text-ute-accent">EXPRESS</span> LOGISTICS</h6>
                    </div>
                    <p class="small text-light opacity-75 mb-2">
                        Hệ thống Quản lý Chuỗi Giao nhận Logistics thông minh 3 cấp độ (SOC - Regional Hub - Delivery Hub).
                    </p>
                    <p class="small text-light opacity-75 mb-0">
                        <i class="bi bi-geo-alt me-1 text-ute-accent"></i>Trường Đại học Sư phạm Kỹ thuật TP. Hồ Chí Minh (HCMUTE)
                    </p>
                </div>

                <div class="col-lg-2 col-md-6 col-6">
                    <h6 class="fw-bold mb-2 text-white">Tra cứu nhanh</h6>
                    <ul class="list-unstyled small text-light opacity-75">
                        <li class="mb-1"><a href="${pageContext.request.contextPath}/tracking" class="text-light text-decoration-none">Theo dõi đơn hàng</a></li>
                    </ul>
                </div>


                <div class="col-lg-3 col-md-6">
                    <h6 class="fw-bold mb-2 text-white">Tổng đài Hỗ trợ 24/7</h6>
                    <p class="mb-1 fs-5 fw-bold text-ute-accent"><i class="bi bi-telephone-fill me-1"></i>1900-UTE-EXPRESS</p>
                    <p class="small text-light opacity-75 mb-0">Email: <span class="text-white">support@uteexpress.edu.vn</span></p>
                </div>
            </div>

            <hr class="border-secondary my-3 opacity-25">

            <div class="d-flex flex-column flex-md-row justify-content-between align-items-center small text-light opacity-75">
                <p class="mb-1 mb-md-0">&copy; 2026 UTEExpress. Phát triển trên nền tảng Spring Boot & Java 21. Đồ án Lập trình Web - ThS. Nguyễn Hữu Trung.</p>
                <div>
                    <span class="badge bg-secondary me-1">Spring Boot 3.3</span>
                    <span class="badge bg-secondary me-1">Java 21 (JDK 21)</span>
                    <span class="badge bg-secondary">Bootstrap 5.3</span>
                </div>
            </div>
        </div>
    </footer>

    <!-- Bootstrap 5.3.3 Bundle JS with Popper -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
