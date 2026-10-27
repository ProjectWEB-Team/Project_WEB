<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Phân công giao hàng</title></head><body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container">
<h1>Phân công giao / hoàn hàng</h1>
<c:forEach items="${candidates}" var="item"><form method="post" action="${pageContext.request.contextPath}/hub/delivery-assignments">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
<input type="hidden" name="shipmentId" value="${item.shipmentId()}">
<span><c:out value="${item.trackingNumber()}"/> — <c:out value="${item.direction()}"/></span>
<select name="shipperId" required><c:forEach items="${shippers}" var="shipper"><option value="${shipper.userId()}"><c:out value="${shipper.fullName()}"/></option></c:forEach></select>
<button type="submit">Phân công</button></form></c:forEach>
<c:if test="${empty candidates}"><p>Không có kiện chờ phân công.</p></c:if>
<a href="${pageContext.request.contextPath}/hub/return-collection">Bàn giao kiện hoàn tại hub</a></main></body></html>
