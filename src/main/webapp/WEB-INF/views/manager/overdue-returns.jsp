<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %><%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Hàng hoàn quá hạn</title></head><body>
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container"><h1>Hàng hoàn quá hạn chờ quyết định</h1>
<c:forEach items="${returns}" var="item"><form method="post">
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="shipmentId" value="${item.shipmentId()}">
<c:out value="${item.trackingNumber()}"/> — nhận tại gốc <c:out value="${item.arrivedAt()}"/>
<label>Lý do xử lý<input name="reason" required maxlength="255"></label><button>Phê duyệt xử lý tồn kho</button></form></c:forEach>
</main></body></html>
