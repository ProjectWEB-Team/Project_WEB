<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
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
<main class="ute-app flex-grow-1">
  <div class="ute-wrap">
    <c:if test="${not empty message}"><div class="alert alert-success mb-0" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger mb-0" role="alert"><c:out value="${error}"/></div></c:if>
    <div class="ute-app-head">
        <div>
            <h1>Nhiệm vụ lấy hàng của tôi</h1>
            <p>Đến địa chỉ người gửi được phân công, kiểm tra kiện hàng, thu tiền mặt cước (nếu có) và quét mã vận đơn xác nhận đã lấy.</p>
        </div>
    </div>
    <c:choose>
        <c:when test="${empty tasks}"><p class="ute-staff-card text-ute-muted mb-0">Chưa có nhiệm vụ.</p></c:when>
        <c:otherwise>
            <c:forEach items="${tasks}" var="t">
                <section class="ute-staff-card">
                    <div class="ute-staff-card-head">
                        <div>
                            <h2><c:out value="${t.trackingNumber}"/> &middot; Người gửi: <c:out value="${t.senderName}"/> &middot; <c:out value="${t.senderPhone}"/></h2>
                            <p>Địa chỉ lấy hàng: <c:out value="${t.senderAddress}"/></p>
                            <p class="ute-staff-meta">Phân công lúc <c:out value="${t.assignedAt}"/> &middot; đã liên hệ <c:out value="${t.attempts}"/> lần</p>
                        </div>
                        <c:if test="${not empty t.cashToCollect}">
                            <span class="ute-chip-warn">Thu tiền mặt tại nơi lấy: <fmt:formatNumber value="${t.cashToCollect}" pattern="#,##0"/> VND</span>
                        </c:if>
                    </div>
                    <div class="ute-staff-row">
                        <form class="ute-staff-form" method="post" action="${pageContext.request.contextPath}/shipper/tasks/${t.assignmentId}/complete">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <div>
                                <label for="scan-${t.assignmentId}">Quét / Nhập mã vận đơn xác nhận đã lấy</label>
                                <input class="form-control" id="scan-${t.assignmentId}" name="trackingNumber" required maxlength="50" placeholder="Quét/nhập mã vận đơn" autocomplete="off">
                            </div>
                            <button class="btn btn-ute-orange" type="submit">Đã lấy hàng</button>
                        </form>
                        <form class="ute-staff-form" method="post" action="${pageContext.request.contextPath}/shipper/tasks/${t.assignmentId}/attempt">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <div>
                                <label for="result-${t.assignmentId}">Kết quả nếu chưa lấy được</label>
                                <select class="form-select" id="result-${t.assignmentId}" name="result">
                                    <option value="FAILED_NO_ANSWER">Không liên lạc được</option>
                                    <option value="FAILED_SENDER_REFUSED">Người gửi từ chối</option>
                                    <option value="FAILED_OTHER">Lý do khác</option>
                                </select>
                            </div>
                            <div>
                                <label for="anote-${t.assignmentId}">Ghi chú lần liên hệ</label>
                                <input class="form-control" id="anote-${t.assignmentId}" name="note" maxlength="255" placeholder="Ghi chú">
                            </div>
                            <button class="btn btn-outline-secondary" type="submit">Ghi lần liên hệ</button>
                        </form>
                    </div>
                </section>
            </c:forEach>
        </c:otherwise>
    </c:choose>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
