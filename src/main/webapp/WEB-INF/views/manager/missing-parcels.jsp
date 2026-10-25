<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="en"><head><title>Missing parcel investigation</title></head><body>
<h1>Missing parcel investigation</h1>
<c:if test="${not empty operationMessage}"><p><c:out value="${operationMessage}"/></p></c:if>
<c:forEach items="${cases}" var="item"><p><a href="${pageContext.request.contextPath}/manager/incidents/missing/${item.incidentId}">Incident #<c:out value="${item.incidentId}"/></a> — Shipment #<c:out value="${item.shipmentId}"/></p></c:forEach>
<c:if test="${not empty selectedCase}">
<p>Custody evidence: <c:out value="${selectedCase.custodyReference}"/></p>
<p>Recorded outcome: <c:out value="${selectedCase.resolutionOutcome}"/></p>
<c:if test="${empty selectedCase.resolutionOutcome}">
<form method="post" action="${pageContext.request.contextPath}/manager/incidents/missing/resolve">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
<input type="hidden" name="incidentId" value="${selectedCase.incidentId}"/>
<input type="hidden" name="shipmentId" value="${selectedCase.shipmentId}"/>
<input type="hidden" name="foundHubId" value="${selectedCase.hubId}"/>
<label>Handover in custody evidence <input name="handoverId" type="number" min="1" required/></label>
<label>Container in custody evidence <input name="containerId" type="number" min="1" required/></label>
<label>Outcome <select name="outcome"><option value="FOUND">Found and physically received at this hub</option><option value="CONFIRMED_LOST">Confirmed lost</option></select></label>
<label>Investigation evidence <textarea name="evidence" maxlength="500" required></textarea></label>
<button type="submit">Record investigation</button>
</form></c:if></c:if>
<a href="${pageContext.request.contextPath}/manager/operations">Back to operations</a>
</body></html>
