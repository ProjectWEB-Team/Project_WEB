<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Chuyến xe của tài xế | UTEExpress</title>
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-express.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-modern.css">
</head>
<body class="d-flex flex-column min-vh-100">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />

<main class="container py-4 flex-grow-1">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
        <div>
            <h1 class="h3 fw-bold mb-1 text-ink">Chuyến xe trung chuyển được giao</h1>
            <p class="text-muted small mb-0">Theo dõi hành trình, báo xe tới nơi và nhận bàn giao bao hàng theo ca.</p>
        </div>
        <div class="d-flex gap-2">
            <a href="#handover-section" class="btn btn-outline-secondary btn-sm rounded-pill px-3">
                <i class="bi bi-box-seam me-1"></i> Nhận bàn giao
            </a>
            <button class="btn btn-ute-danger-soft btn-sm rounded-pill px-3" data-bs-toggle="modal" data-bs-target="#delayModal">
                <i class="bi bi-exclamation-triangle me-1"></i> Báo trễ chuyến
            </button>
        </div>
    </div>

    <c:if test="${not empty operationMessage}">
        <div class="alert alert-info alert-dismissible fade show rounded-3 shadow-sm mb-4" role="alert">
            <i class="bi bi-info-circle-fill me-2"></i>
            <c:out value="${operationMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
        </div>
    </c:if>

    <!-- CARD 1: DANH SÁCH CHUYẾN XE ĐƯỢC GIAO (Figma Master Component 118:92) -->
    <div class="card shadow-sm border-0 rounded-4 mb-4">
        <div class="card-body p-4">
            <div class="mb-3">
                <h2 class="h5 fw-bold mb-1 text-ink">Danh sách chuyến xe được giao</h2>
                <div class="text-muted small">
                    <c:choose>
                        <c:when test="${not empty tripPicks}">
                            ${fn:length(tripPicks)} chuyến xe đang vận hành trong ca
                        </c:when>
                        <c:otherwise>0 chuyến xe đang vận hành trong ca</c:otherwise>
                    </c:choose>
                </div>
            </div>

            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="py-3 px-3 text-start fw-semibold text-muted small">Mã chuyến</th>
                            <th class="py-3 px-3 text-start fw-semibold text-muted small">Phiếu bàn giao &amp; Lộ trình</th>
                            <th class="py-3 px-3 text-start fw-semibold text-muted small">Trạng thái</th>
                            <th class="py-3 px-3 text-start fw-semibold text-muted small th-action">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty tripPicks}">
                                <tr>
                                    <td colspan="4" class="text-center py-5 text-muted">
                                        <i class="bi bi-truck fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                        Chưa có chuyến xe nào được bàn giao hoặc đang chạy trong ca hiện tại.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach items="${tripPicks}" var="p">
                                    <tr>
                                        <td class="py-3 px-3 fw-bold text-ink">
                                            <c:out value="${p[1]}"/>
                                            <span class="badge bg-light text-secondary border ms-1">#<c:out value="${p[0]}"/></span>
                                        </td>
                                        <td class="py-3 px-3">
                                            <div class="fw-semibold text-ink">
                                                Hub đích: <span class="badge bg-secondary-subtle text-secondary-emphasis"><c:out value="${p[3]}"/></span>
                                            </div>
                                            <div class="text-muted small">Lộ trình vận chuyển liên hub / kho bãi</div>
                                        </td>
                                        <td class="py-3 px-3">
                                            <c:choose>
                                                <c:when test="${p[2] == 'IN_TRANSIT'}">
                                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill px-3 py-2">
                                                        <i class="bi bi-circle-half me-1"></i> Đang trung chuyển
                                                    </span>
                                                </c:when>
                                                <c:when test="${p[2] == 'SCHEDULED'}">
                                                    <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle rounded-pill px-3 py-2">
                                                        <i class="bi bi-clock me-1"></i> Đã lên lịch
                                                    </span>
                                                </c:when>
                                                <c:when test="${p[2] == 'ARRIVED'}">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-3 py-2">
                                                        <i class="bi bi-check-circle me-1"></i> Đã tới nơi
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-light text-dark rounded-pill px-3 py-2">
                                                        <c:out value="${p[2]}"/>
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="py-3 px-3 td-action">
                                            <div class="d-flex align-items-center gap-2 justify-content-start flex-wrap">
                                                <c:if test="${p[2] == 'SCHEDULED'}">
                                                    <form method="post" action="${pageContext.request.contextPath}/driver/trips/start" class="m-0">
                                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                        <input type="hidden" name="tripId" value="${p[0]}"/>
                                                        <button type="submit" class="btn btn-ute-primary btn-sm px-3 py-1 rounded-pill">
                                                            <i class="bi bi-play-circle me-1"></i> Bắt đầu chuyến
                                                        </button>
                                                    </form>
                                                </c:if>
                                                <c:if test="${p[2] == 'IN_TRANSIT'}">
                                                    <form method="post" action="${pageContext.request.contextPath}/driver/trips/arrive" class="m-0">
                                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                        <input type="hidden" name="tripId" value="${p[0]}"/>
                                                        <button type="submit" class="btn btn-ute-primary btn-sm px-3 py-1 rounded-pill">
                                                            <i class="bi bi-geo-alt-fill me-1"></i> Báo tới nơi
                                                        </button>
                                                    </form>
                                                </c:if>
                                                <button type="button" class="btn btn-ute-danger-soft btn-sm px-3 py-1 rounded-pill" onclick="openDelayModal('${p[0]}', '${p[1]}')">
                                                    Báo trễ
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <div class="pt-3 border-top mt-3 text-muted small d-flex justify-content-between align-items-center">
                <span>Hiển thị ${fn:length(tripPicks)} / ${fn:length(tripPicks)} chuyến</span>
                <span class="fst-italic">* Báo tới nơi chưa phải là hub đích đã hoàn tất nhận hàng; hub đích cần kiểm đếm bàn giao.</span>
            </div>
        </div>
    </div>

    <!-- CARD 2: GIAO NHẬN BÀN GIAO & PHƯƠNG TIỆN -->
    <div class="row g-4" id="handover-section">
        <!-- 2A: Nhận bàn giao & Xe -->
        <div class="col-lg-6">
            <div class="card shadow-sm border-0 rounded-4 h-100">
                <div class="card-body p-4">
                    <h2 class="h5 fw-bold mb-2 text-ink">
                        <i class="bi bi-truck me-2 text-warning"></i>Nhận bàn giao &amp; Gán phương tiện
                    </h2>
                    <p class="text-muted small mb-3">Tài xế nhận phiếu bàn giao từ kho xuất phát và chọn phương tiện vận chuyển trước khi xuất phát.</p>

                    <form method="post" action="${pageContext.request.contextPath}/driver/handovers/accept">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <div class="mb-3">
                            <label class="form-label fw-semibold small">Phiếu bàn giao</label>
                            <select name="handoverId" aria-label="Phiếu bàn giao" class="form-select rounded-3" required>
                                <option value="">-- Chọn phiếu bàn giao chờ nhận --</option>
                                <c:forEach items="${handoverPicks}" var="p">
                                    <option value="${p[0]}">Phiếu #${p[0]}: Hub ${p[1]} → Hub ${p[2]}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold small">Phương tiện sẵn sàng</label>
                            <select name="vehicleId" aria-label="Phương tiện" class="form-select rounded-3" required>
                                <option value="">-- Chọn phương tiện di chuyển --</option>
                                <c:forEach items="${vehicles}" var="vehicle">
                                    <option value="${vehicle.id}">
                                        <c:out value="${vehicle.plateNumber}"/> — <c:out value="${vehicle.vehicleType}"/>
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <button type="submit" class="btn btn-ute-accent rounded-pill px-4">
                            <i class="bi bi-check-circle me-1"></i> Xác nhận nhận bàn giao
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <!-- 2B: Quét bao hàng -->
        <div class="col-lg-6">
            <div class="card shadow-sm border-0 rounded-4 h-100">
                <div class="card-body p-4">
                    <h2 class="h5 fw-bold mb-2 text-ink">
                        <i class="bi bi-upc-scan me-2 text-primary"></i>Quét bao hàng kiểm đếm
                    </h2>
                    <p class="text-muted small mb-3">Xác thực quét từng bao hàng đã bàn giao lên xe để đảm bảo không thất lạc kiện hàng.</p>

                    <form method="post" action="${pageContext.request.contextPath}/driver/handovers/scan">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <div class="mb-3">
                            <label class="form-label fw-semibold small">Phiếu bàn giao</label>
                            <select name="handoverId" aria-label="Phiếu bàn giao" class="form-select rounded-3" required>
                                <option value="">-- Chọn phiếu bàn giao --</option>
                                <c:forEach items="${handoverPicks}" var="p">
                                    <option value="${p[0]}">Phiếu #${p[0]}: Hub ${p[1]} → Hub ${p[2]}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold small">Bao hàng cần quét</label>
                            <select name="containerId" aria-label="Bao hàng" class="form-select rounded-3" required>
                                <option value="">-- Chọn bao hàng --</option>
                                <c:forEach items="${containerPicks}" var="p">
                                    <option value="${p[1]}">Phiếu #${p[0]} – Bao: <c:out value="${p[2]}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <button type="submit" class="btn btn-outline-primary rounded-pill px-4">
                            <i class="bi bi-qr-code-scan me-1"></i> Quét xác nhận bao
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- MODAL BÁO CHUYẾN BỊ TRỄ -->
<div class="modal fade" id="delayModal" tabindex="-1" aria-labelledby="delayModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4 border-0 shadow">
            <form method="post" action="${pageContext.request.contextPath}/driver/trips/delay">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="modal-header border-0 pb-0">
                    <h5 class="modal-title fw-bold" id="delayModalLabel">
                        <i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>Báo chuyến xe bị trễ
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Chọn chuyến xe</label>
                        <select id="modalTripSelect" name="tripId" class="form-select rounded-3" required>
                            <option value="">-- Chọn chuyến xe --</option>
                            <c:forEach items="${tripPicks}" var="p">
                                <option value="${p[0]}"><c:out value="${p[1]}"/> → Hub <c:out value="${p[3]}"/></option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Lý do / Ghi chú trễ</label>
                        <textarea class="form-control rounded-3" name="note" rows="3" maxlength="500" placeholder="Nguyên nhân (kẹt xe, thời tiết, sự cố kỹ thuật...)" required></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Tham chiếu bằng chứng (nếu có)</label>
                        <input class="form-control rounded-3" name="evidenceReference" maxlength="120" placeholder="Mã biên bản, link hình ảnh hoặc số điện thoại điều phối">
                    </div>
                </div>
                <div class="modal-footer border-0 pt-0">
                    <button type="button" class="btn btn-light rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-danger rounded-pill px-4">Xác nhận báo trễ</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function openDelayModal(tripId, tripCode) {
        const select = document.getElementById("modalTripSelect");
        if (select) {
            select.value = tripId;
        }
        const modal = new bootstrap.Modal(document.getElementById("delayModal"));
        modal.show();
    }
</script>

<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
</body>
</html>
