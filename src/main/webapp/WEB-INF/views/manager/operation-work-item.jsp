<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>Operational action | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" /><main class="container py-4">
<h1 class="h3"><c:out value="${item.summary}"/></h1>
<p>Reference: <c:out value="${item.kind}"/> #<c:out value="${item.id}"/></p>
<c:choose><c:when test="${item.kind == 'MISSING'}"><a class="btn btn-primary" href="${pageContext.request.contextPath}/manager/incidents/missing/${item.id}">Investigate scoped missing parcel</a></c:when>
<c:otherwise><c:if test="${not empty item.shipmentId}"><a class="btn btn-primary" href="${pageContext.request.contextPath}/manager/audit/shipments/${item.shipmentId}">View scoped shipment history</a></c:if></c:otherwise></c:choose>
<a class="btn btn-link" href="${pageContext.request.contextPath}/manager/operations">Back to operations</a>
</main></body></html>
