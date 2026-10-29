<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Việc cần xử lý | UTEExpress" scope="request"/>
<jsp:include page="../layout/header.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="container flex-grow-1 py-4">
    <h1 class="h3 mb-1"><c:out value="${item.summary}"/></h1>
    <p class="text-muted">
        <c:choose>
            <c:when test="${item.kind == 'MISSING'}">Sự cố</c:when>
            <c:when test="${item.kind == 'ROUTE'}">Tuyến</c:when>
            <c:otherwise>Bàn giao</c:otherwise>
        </c:choose>
        #<c:out value="${item.id}"/>
        <c:if test="${not empty item.shipmentId}"> · Đơn #<c:out value="${item.shipmentId}"/></c:if>
    </p>
    <div class="d-flex flex-wrap gap-2">
        <c:choose>
            <c:when test="${item.kind == 'MISSING'}"><a class="btn btn-primary" href="${ctx}/manager/incidents/missing/${item.id}">Điều tra</a></c:when>
            <c:when test="${not empty item.shipmentId}"><a class="btn btn-primary" href="${ctx}/manager/audit/shipments/${item.shipmentId}">Lịch sử đơn</a></c:when>
        </c:choose>
        <a class="btn btn-outline-secondary" href="${ctx}/manager/operations">Quay lại</a>
    </div>
</main>
<jsp:include page="../layout/footer.jsp" />
