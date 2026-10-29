<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đơn đang đến | UTEExpress</title>
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
            <h1>Đơn hàng đang giao đến bạn</h1>
            <p>Mã vận đơn và người gửi được che một phần. Nhập đúng mã để mở khóa.</p>
        </div>
    </div>
    <section class="ute-app-card">
        <div class="ute-app-card-head">
            <h2>Danh sách đơn đang đến số điện thoại của bạn (${shipments.totalElements} đơn)</h2>
            <p>Chọn một đơn rồi nhập mã vận đơn để mở khóa.</p>
        </div>
        <c:choose>
            <c:when test="${shipments.totalElements == 0}"><p class="mb-0 text-ute-muted">Chưa có đơn nào gửi đến bạn.</p></c:when>
            <c:otherwise>
                <div class="table-responsive">
                <table class="table ute-list-table align-middle">
                    <thead><tr><th>Mã vận đơn (đã che)</th><th>Người gửi (đã che)</th><th>Trạng thái hiện tại</th><th>Tạo lúc</th><th>Thao tác</th></tr></thead>
                    <tbody>
                    <c:forEach items="${shipments.content}" var="s">
                        <tr>
                            <td><a class="ute-code" href="${pageContext.request.contextPath}/user/incoming/${s.id}"><c:out value="${s.maskedTrackingNumber}"/></a></td>
                            <td><c:out value="${s.maskedSenderName}"/></td>
                            <td><u:statusbadge status="${s.currentStatus}"/></td>
                            <td><c:set var="at" value="${s.createdAt}"/>${fn:substring(at, 8, 10)}/${fn:substring(at, 5, 7)}/${fn:substring(at, 0, 4)} ${fn:substring(at, 11, 16)}</td>
                            <td><a class="btn ute-btn-view" href="${pageContext.request.contextPath}/user/incoming/${s.id}">Mở chi tiết</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                </div>
            </c:otherwise>
        </c:choose>
        <nav class="d-flex justify-content-between" aria-label="Phân trang">
            <c:if test="${shipments.hasPrevious()}"><a class="btn btn-outline-secondary btn-sm" href="?page=${shipments.number - 1}">Trang trước</a></c:if>
            <c:if test="${shipments.hasNext()}"><a class="btn btn-outline-secondary btn-sm" href="?page=${shipments.number + 1}">Trang sau</a></c:if>
        </nav>
    </section>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
