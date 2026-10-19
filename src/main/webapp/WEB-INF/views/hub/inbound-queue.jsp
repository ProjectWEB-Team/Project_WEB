<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html><head><title>Inbound receipt</title></head><body>
<h1>Destination inbound receipt</h1>
<c:if test="${not empty operationMessage}"><p><c:out value="${operationMessage}"/></p></c:if>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/receive">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Handover <input name="handoverId" required /></label>
  <label>Container <input name="containerId" required /></label>
  <label>Shipment <input name="shipmentId" required /></label>
  <button type="submit">Record physical receipt</button>
</form>
</body></html>
