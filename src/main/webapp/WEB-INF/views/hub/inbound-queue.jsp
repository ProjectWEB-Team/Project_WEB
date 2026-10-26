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
<h2>Finish unpacking and reconcile the manifest</h2>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Handover <input name="handoverId" type="number" min="1" required /></label>
  <label>Container <input name="containerId" type="number" min="1" required /></label>
  <label>Received shipment IDs, separated by commas <input name="receivedShipmentIds" required /></label>
  <button type="submit">Record received and missing parcels</button>
</form>
<form method="post" action="${pageContext.request.contextPath}/hub/inbound/reconcile-empty">
  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
  <label>Handover <input name="handoverId" type="number" min="1" required /></label>
  <label>Container <input name="containerId" type="number" min="1" required /></label>
  <label><input type="checkbox" name="confirmAllMissing" value="true" required /> No expected parcel was physically received</label>
  <button type="submit">Report all contents missing</button>
</form>
<h2>Nhận lại kiện giao thất bại từ shipper</h2><form method="post" action="${pageContext.request.contextPath}/hub/delivery-returns/receive">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><label>Mã ID vận đơn<input name="shipmentId" type="number" required></label>
<label>Quét mã vận đơn<input name="trackingNumber" required></label><button>Xác nhận nhận kiện vật lý</button></form>
</body></html>
