<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head><title>Outbound custody</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-express.css"></head>
<body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" /><main class="container py-4"><h1>Outbound container custody</h1>
<c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
<p>All hub, route, driver and custody checks are completed server-side.</p>
<form method="post" action="${pageContext.request.contextPath}/hub/outbound/containers"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Container code <input name="containerCode" required></label><label>Destination hub id <input name="destinationHubId" type="number" required></label><button type="submit">Create container</button></form>
<form method="post" action="${pageContext.request.contextPath}/hub/outbound/load"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Container id <input name="containerId" type="number" required></label><label>Shipment id <input name="shipmentId" type="number" required></label><button type="submit">Load shipment</button></form>
<form method="post" action="${pageContext.request.contextPath}/hub/outbound/handovers"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Destination hub id <input name="destinationHubId" type="number" required></label><label>Assigned driver id <input name="receiverDriverId" type="number" required></label><label>Container ids <input name="containerIds" type="number" required></label><button type="submit">Prepare handover</button></form>
<form method="post" action="${pageContext.request.contextPath}/hub/outbound/handovers/correct"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Handover id <input name="handoverId" type="number" required></label><label>Replacement container ids <input name="replacementContainerIds" type="number" required></label><button type="submit">Cancel and recreate custody</button></form>
</main></body></html>
