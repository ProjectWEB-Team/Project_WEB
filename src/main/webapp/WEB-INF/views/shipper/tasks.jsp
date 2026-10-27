<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhiệm vụ lấy hàng | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4" style="max-width: 960px">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1 class="h3 mb-3">Nhiệm vụ lấy hàng của tôi</h1>
    <c:choose>
        <c:when test="${empty tasks}"><p class="card-ute p-4 text-ute-muted">Chưa có nhiệm vụ.</p></c:when>
        <c:otherwise>
            <c:forEach items="${tasks}" var="t">
                <section class="card-ute p-4 mb-3">
                    <h2 class="h6 font-monospace"><c:out value="${t.trackingNumber}"/></h2>
                    <p class="mb-1"><c:out value="${t.senderName}"/> · <c:out value="${t.senderPhone}"/></p>
                    <p class="mb-1 text-ute-muted"><c:out value="${t.senderAddress}"/></p>
                    <p class="mb-2 small">Phân công lúc <c:out value="${t.assignedAt}"/> · đã liên hệ <c:out value="${t.attempts}"/> lần</p>
                    <c:if test="${not empty t.cashToCollect}">
                        <p class="mb-2"><strong>Thu tiền mặt: <fmt:formatNumber value="${t.cashToCollect}" pattern="#,##0"/> VND</strong></p>
                    </c:if>
                    <form class="row g-2 mb-2" method="post" action="${pageContext.request.contextPath}/shipper/tasks/${t.assignmentId}/complete">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="col-sm-8">
                            <label class="visually-hidden" for="scan-${t.assignmentId}">Quét mã vận đơn</label>
                            <input class="form-control font-monospace" id="scan-${t.assignmentId}" name="trackingNumber" required maxlength="50" placeholder="Quét/nhập mã vận đơn" autocomplete="off">
                        </div>
                        <div class="col-sm-4"><button class="btn btn-ute-accent w-100" type="submit">Đã lấy hàng</button></div>
                    </form>
                    <form class="row g-2" method="post" action="${pageContext.request.contextPath}/shipper/tasks/${t.assignmentId}/attempt">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="col-sm-4">
                            <label class="visually-hidden" for="result-${t.assignmentId}">Kết quả</label>
                            <select class="form-select" id="result-${t.assignmentId}" name="result">
                                <option value="FAILED_NO_ANSWER">Không liên lạc được</option>
                                <option value="FAILED_SENDER_REFUSED">Người gửi từ chối</option>
                                <option value="FAILED_OTHER">Lý do khác</option>
                            </select>
                        </div>
                        <div class="col-sm-4">
                            <label class="visually-hidden" for="anote-${t.assignmentId}">Ghi chú</label>
                            <input class="form-control" id="anote-${t.assignmentId}" name="note" maxlength="255" placeholder="Ghi chú">
                        </div>
                        <div class="col-sm-4"><button class="btn btn-outline-secondary w-100" type="submit">Ghi lần liên hệ</button></div>
                    </form>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
