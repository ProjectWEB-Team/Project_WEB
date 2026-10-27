<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Điều tra hàng thất lạc</title></head><body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container">
<h1>Điều tra hàng thất lạc</h1>
<c:if test="${not empty operationMessage}"><p><c:out value="${operationMessage}"/></p></c:if>
<c:forEach items="${cases}" var="item"><p><a href="${pageContext.request.contextPath}/manager/incidents/missing/${item.incidentId}">Sự cố #<c:out value="${item.incidentId}"/></a> — Shipment #<c:out value="${item.shipmentId}"/></p></c:forEach>
<c:if test="${not empty selectedCase}">
<p>Bằng chứng bàn giao: <c:out value="${selectedCase.custodyReference}"/></p>
<p>Kết quả đã ghi: <c:out value="${selectedCase.resolutionOutcome}"/></p>
<c:if test="${empty selectedCase.resolutionOutcome}">
<form method="post" action="${pageContext.request.contextPath}/manager/incidents/missing/resolve">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
<input type="hidden" name="incidentId" value="${selectedCase.incidentId}"/>
<input type="hidden" name="shipmentId" value="${selectedCase.shipmentId}"/>
<input type="hidden" name="foundHubId" value="${selectedCase.hubId}"/>
<label>Phiếu bàn giao liên quan <input name="handoverId" type="number" min="1" required/></label>
<label>Bao hàng liên quan <input name="containerId" type="number" min="1" required/></label>
<label>Kết quả <select name="outcome"><option value="FOUND">Đã tìm thấy và nhận tại hub này</option><option value="CONFIRMED_LOST">Xác nhận thất lạc</option></select></label>
<label>Bằng chứng điều tra <textarea name="evidence" maxlength="500" required></textarea></label>
<button type="submit">Ghi nhận kết quả điều tra</button>
</form></c:if></c:if>
<a href="${pageContext.request.contextPath}/manager/operations">Quay lại bảng điều hành</a>
</main></body></html>
