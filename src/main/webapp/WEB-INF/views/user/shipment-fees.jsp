<%@ page contentType="text/html;charset=UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Thanh toán phí vận đơn</title></head><body>
<h1>Phí vận đơn <c:out value="${fees.trackingNumber()}"/></h1>
<p>Cước chiều đi còn phải trả: <c:out value="${fees.forwardDue()}"/>; phí hoàn: <c:out value="${fees.returnDue()}"/>; phụ thu cân nặng: <c:out value="${fees.surchargeDue()}"/></p>
<c:forEach items="${['RETURN','WEIGHT_SURCHARGE']}" var="kind">
<c:if test="${kind == 'RETURN' ? fees.forwardDue() + fees.returnDue() > 0 : fees.surchargeDue() > 0}">
<form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/wallet">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="obligation" value="${kind}">
<button>Trả ${kind == 'RETURN' ? 'phí hoàn và cước còn thiếu' : 'phụ thu cân nặng'} bằng ví</button></form>
<form method="post" action="${pageContext.request.contextPath}/user/shipment-fees/${fees.shipmentId()}/demo-vnpay">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="obligation" value="${kind}">
<label><input type="checkbox" name="confirmDemo" value="true" required>Xác nhận thanh toán VNPay mô phỏng, không giao dịch ngân hàng thật</label>
<button>VNPay demo — ${kind == 'RETURN' ? 'phí hoàn và cước còn thiếu' : 'phụ thu cân nặng'}</button></form></c:if></c:forEach>
</body></html>
