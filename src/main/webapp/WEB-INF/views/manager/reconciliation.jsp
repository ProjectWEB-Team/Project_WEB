<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="../layout/header.jsp" />
<main class="container py-4">
  <h1>Đối soát và quyết toán</h1>
  <c:if test="${not empty successMessage}"><p class="alert alert-success"><c:out value="${successMessage}"/></p></c:if>
  <c:if test="${not empty errorMessage}"><p class="alert alert-danger"><c:out value="${errorMessage}"/></p></c:if>
  <form method="get" action="${pageContext.request.contextPath}/manager/reconciliation" class="card p-3 mb-4">
    <label>Mã tài khoản shipper <input class="form-control" name="shipperId" type="number" min="1" required value="${shipperId}"></label>
    <label>Ngày thu tiền <input class="form-control" name="date" type="date" value="${selectedDate}"></label>
    <label>Phương thức thu <select class="form-select" name="method"><option value="CASH" ${selectedMethod == 'CASH' ? 'selected' : ''}>Tiền mặt</option><option value="VNPAY" ${selectedMethod == 'VNPAY' ? 'selected' : ''}>VNPAY</option><option value="TRANSFER" ${selectedMethod == 'TRANSFER' ? 'selected' : ''}>Chuyển khoản</option></select></label>
    <button class="btn btn-primary mt-2">Tải khoản thu đã ghi nhận</button>
  </form>
  <h2 class="h4">Khoản thu cần đối soát — <c:out value="${selectedMethod}"/></h2>
  <p>Mức kỳ vọng lấy từ khoản thu đã ghi nhận. Nhập số tiền thực tế đã bàn giao hoặc đã xác minh cho từng khoản. Mỗi lần đối soát chỉ gồm một phương thức.</p>
  <c:if test="${empty expectedItems}"><p>Không có khoản thu trong lựa chọn hiện tại.</p></c:if>
  <c:if test="${not empty expectedItems}">
  <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/submit">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
    <input type="hidden" name="shipperId" value="${shipperId}">
    <input type="hidden" name="reconciliationDate" value="${selectedDate}">
    <input type="hidden" name="collectionMethod" value="${fn:escapeXml(selectedMethod)}">
    <c:if test="${selectedMethod != 'CASH'}"><label>Bằng chứng xác minh khoản tiền đã nhận <input class="form-control" name="settlementEvidence" maxlength="255" required></label><p class="text-muted">Ghi mã tham chiếu và căn cứ đã kiểm tra. Chỉ xác nhận sau khi đối chiếu chứng từ nhận tiền; trạng thái do shipper ghi nhận chưa đủ để trả COD.</p></c:if>
    <div class="table-responsive"><table class="table"><thead><tr><th>Vận đơn</th><th>Khoản thu</th><th>Kỳ vọng (đ)</th><th>Thực tế (đ)</th><th>Ghi chú</th></tr></thead><tbody>
    <c:forEach items="${expectedItems}" var="item" varStatus="row"><tr>
      <td><c:out value="${item.trackingNumber}"/><input type="hidden" name="items[${row.index}].shipmentId" value="${item.shipmentId}"></td>
      <td><c:out value="${item.description}"/><input type="hidden" name="items[${row.index}].sourceType" value="${item.sourceType}"><input type="hidden" name="items[${row.index}].sourceId" value="${item.sourceId}"></td>
      <td><c:out value="${item.expectedAmount}"/></td>
      <td><input class="form-control" aria-label="Số tiền thực tế" type="number" min="0" step="1" required name="items[${row.index}].actualAmount"></td>
      <td><input class="form-control" aria-label="Ghi chú" maxlength="255" name="items[${row.index}].note"></td>
    </tr></c:forEach></tbody></table></div>
    <button class="btn btn-primary">Xác nhận đối soát</button>
  </form></c:if>
  <h2 class="h4 mt-4">COD đã đối soát, chờ trả người gửi</h2>
  <c:forEach items="${readyForPayout}" var="item">
    <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/payout" class="card p-3 mb-2">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="codTransactionId" value="${item.sourceId}">
      <p><c:out value="${item.trackingNumber}"/> — <c:out value="${item.expectedAmount}"/> đ</p><button class="btn btn-primary">Trả COD vào ví người gửi</button>
    </form>
  </c:forEach>
  <h2 class="h4 mt-4">Lịch sử đối soát</h2>
  <c:forEach items="${history}" var="rec"><p><a href="${pageContext.request.contextPath}/manager/reconciliation/detail?reconciliationId=${rec.id()}">#${rec.id()}</a> — <c:out value="${rec.date()}"/> — <c:out value="${rec.collectionMethod()}"/> — <c:out value="${rec.status()}"/></p></c:forEach>
  <c:if test="${not empty reconciliationItems}"><div class="table-responsive"><table class="table"><thead><tr><th>Vận đơn</th><th>Kỳ vọng</th><th>Thực tế</th><th>Chênh lệch</th><th>Trạng thái</th></tr></thead><tbody>
    <c:forEach items="${reconciliationItems}" var="item"><tr><td><c:out value="${item.trackingNumber()}"/></td><td>${item.expectedAmount()}</td><td>${item.actualAmount()}</td><td>${item.differenceAmount()}</td><td><c:out value="${item.status()}"/></td></tr></c:forEach>
  </tbody></table></div></c:if>
  <h2 class="h4 mt-4">Hoàn phí đơn đã hủy</h2>
  <c:forEach items="${pendingRefunds}" var="item">
    <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/refund" class="card p-3 mb-2">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="paymentId" value="${item.paymentId()}">
      <p><c:out value="${item.trackingNumber()}"/> — ${item.amount()} đ</p><button class="btn btn-primary">Hoàn phí vào ví người gửi</button>
    </form>
  </c:forEach>
  <h2 class="h4 mt-4">Bồi thường kiện đã xác nhận mất</h2>
  <c:forEach items="${confirmedLosses}" var="item">
    <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/compensate" class="card p-3 mb-2">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="shipmentId" value="${item.shipmentId()}"><input type="hidden" name="incidentId" value="${item.incidentId()}">
      <p><c:out value="${item.trackingNumber()}"/> — sự cố #${item.incidentId()}</p>
      <label>Lý do quyết định <input class="form-control" name="reason" maxlength="255" required></label>
      <button class="btn btn-primary mt-2">Xác nhận bồi thường</button>
    </form>
  </c:forEach>
</main>
<jsp:include page="../layout/footer.jsp" />
