<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en"><head><title>Driver trips</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-express.css"></head>
<body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" /><main class="container py-4"><h1>Assigned linehaul trips</h1>
<c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
<p>Actions are permitted only for your assigned, accepted custody. Arrival does not complete destination receipt.</p>
<form method="post" action="${pageContext.request.contextPath}/driver/handovers/scan"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Handover id <input name="handoverId" type="number" required></label><label>Container id <input name="containerId" type="number" required></label><button type="submit">Scan container</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/handovers/accept"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Handover id <input name="handoverId" type="number" required></label>
<label>Available vehicle <select name="vehicleId" required><option value="">Choose a vehicle</option><c:forEach items="${vehicles}" var="vehicle"><option value="${vehicle.id}"><c:out value="${vehicle.plateNumber}"/> — <c:out value="${vehicle.vehicleType}"/></option></c:forEach></select></label><button type="submit">Accept handover</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/start"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Trip id <input name="tripId" type="number" required></label><button type="submit">Start trip</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/arrive"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Trip id <input name="tripId" type="number" required></label><button type="submit">Record arrival</button></form>
<form method="post" action="${pageContext.request.contextPath}/driver/trips/delay"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/><label>Trip id <input name="tripId" type="number" required></label><label>Delay note <textarea name="note" maxlength="500" required></textarea></label><label>Evidence reference <input name="evidenceReference" maxlength="120"></label><button type="submit">Report trip delay</button></form>
</main></body></html>
