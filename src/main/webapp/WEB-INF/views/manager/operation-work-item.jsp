<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi"><head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><meta charset="UTF-8"><title>Việc cần xử lý | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" /><main class="container py-4">
<h1 class="h3"><c:out value="${item.summary}"/></h1>
<p>Tham chiếu: <c:out value="${item.kind}"/> #<c:out value="${item.id}"/></p>
<c:choose><c:when test="${item.kind == 'MISSING'}"><a class="btn btn-primary" href="${pageContext.request.contextPath}/manager/incidents/missing/${item.id}">Điều tra kiện thất lạc</a></c:when>
<c:otherwise><c:if test="${not empty item.shipmentId}"><a class="btn btn-primary" href="${pageContext.request.contextPath}/manager/audit/shipments/${item.shipmentId}">Xem lịch sử đơn</a></c:if></c:otherwise></c:choose>
<a class="btn btn-link" href="${pageContext.request.contextPath}/manager/operations">Quay lại bảng điều hành</a>
</main><jsp:include page="/WEB-INF/views/layout/footer.jsp" />
