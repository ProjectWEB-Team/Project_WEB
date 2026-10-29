<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn đã gửi | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-app flex-grow-1">
  <div class="ute-wrap">
    <c:if test="${not empty message}"><div class="alert alert-success mb-0" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger mb-0" role="alert"><c:out value="${error}"/></div></c:if>
    <div class="ute-app-head">
        <div>
            <h1>Đơn đã gửi của tôi</h1>
            <p>Theo dõi trạng thái toàn trình, đối soát tiền thu hộ COD và quản lý tất cả vận đơn bạn đã tạo.</p>
        </div>
        <div class="ute-app-head-actions">
            <form method="get" class="m-0">
                <label class="visually-hidden" for="status">Trạng thái</label>
                <select class="form-select form-select-sm" id="status" name="status" onchange="this.form.submit()">
                    <option value="">Tất cả trạng thái</option>
                    <c:forEach items="${statuses}" var="s">
                        <option value="${s}" <c:if test="${s == status}">selected</c:if>><c:out value="${s}"/></option>
                    </c:forEach>
                </select>
                <noscript><button class="btn btn-outline-secondary btn-sm" type="submit">Lọc</button></noscript>
            </form>
            <a class="btn btn-ute-accent ute-btn-create" href="${pageContext.request.contextPath}/user/shipments/create">+ Tạo đơn mới</a>
        </div>
    </div>
    <section class="ute-app-card">
        <div class="ute-app-card-head">
            <h2>Danh sách vận đơn đã gửi (${shipments.totalElements} đơn)</h2>
            <p>Nhấp vào mã vận đơn hoặc nút Xem chi tiết để xem lộ trình bàn giao, hủy đơn hoặc thanh toán phụ thu.</p>
        </div>
        <c:choose>
            <c:when test="${shipments.totalElements == 0}"><p class="mb-0 text-ute-muted">Chưa có đơn nào.</p></c:when>
            <c:otherwise>
                <div class="table-responsive">
                <table class="table ute-list-table align-middle">
                    <thead><tr><th>Mã vận đơn</th><th>Người nhận</th><th>Trạng thái</th><th>Cước phí</th><th>Tạo lúc</th><th>Thao tác</th></tr></thead>
                    <tbody>
                    <c:forEach items="${shipments.content}" var="s">
                        <tr>
                            <td><a class="ute-code" href="${pageContext.request.contextPath}/user/shipments/${s.id}"><c:out value="${s.trackingNumber}"/></a></td>
                            <td><c:out value="${s.receiverName}"/></td>
                            <td><u:statusbadge status="${s.currentStatus}"/></td>
                            <td><fmt:formatNumber value="${s.shippingFee}" pattern="#,##0" var="feeText"/>${fn:replace(feeText, ',', '.')} đ</td>
                            <td><c:set var="at" value="${s.createdAt}"/>${fn:substring(at, 8, 10)}/${fn:substring(at, 5, 7)}/${fn:substring(at, 0, 4)} ${fn:substring(at, 11, 16)}</td>
                            <td><a class="btn ute-btn-view" href="${pageContext.request.contextPath}/user/shipments/${s.id}">Xem chi tiết</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>
        <nav class="d-flex justify-content-between" aria-label="Phân trang">
            <c:if test="${shipments.hasPrevious()}"><a class="btn btn-outline-secondary btn-sm" href="?status=${status}&page=${shipments.number - 1}">Trang trước</a></c:if>
            <span class="small text-ute-muted">Trang ${shipments.number + 1} / ${shipments.totalPages == 0 ? 1 : shipments.totalPages}</span>
            <c:if test="${shipments.hasNext()}"><a class="btn btn-outline-secondary btn-sm" href="?status=${status}&page=${shipments.number + 1}">Trang sau</a></c:if>
        </nav>
    </section>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
