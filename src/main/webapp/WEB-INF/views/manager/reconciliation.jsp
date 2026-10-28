<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<jsp:include page="../layout/header.jsp" />

<main class="container py-4">
  <div class="mb-4">
    <h1 class="h3 fw-bold text-ink mb-1">Đối soát và quyết toán</h1>
    <p class="text-muted small mb-0">Quản lý kiểm đếm dòng tiền COD, hoàn phí vận chuyển và bồi thường sự cố hàng hoá.</p>
  </div>

  <c:if test="${not empty successMessage}">
    <div class="alert alert-success alert-dismissible fade show rounded-3 shadow-sm mb-4" role="alert">
      <i class="bi bi-check-circle-fill me-2"></i><c:out value="${successMessage}"/>
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
    </div>
  </c:if>
  <c:if test="${not empty errorMessage}">
    <div class="alert alert-danger alert-dismissible fade show rounded-3 shadow-sm mb-4" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2"></i><c:out value="${errorMessage}"/>
      <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
    </div>
  </c:if>

  <!-- BỘ LỌC ĐỐI SOÁT -->
  <div class="card border-0 shadow-sm rounded-4 mb-4">
    <div class="card-body p-4">
      <h2 class="h5 fw-bold text-ink mb-3"><i class="bi bi-funnel me-2 text-warning"></i>Bộ lọc đối soát ca</h2>
      <form method="get" action="${pageContext.request.contextPath}/manager/reconciliation" class="row g-3">
        <div class="col-md-4">
          <label class="form-label fw-semibold small">Shipper thu tiền</label>
          <select class="form-select rounded-3" name="shipperId" aria-label="Shipper thu tiền" required>
            <option value="">-- Chọn shipper phụ trách --</option>
            <c:forEach items="${shippers}" var="sh">
              <option value="${sh.id}" ${sh.id == shipperId ? 'selected' : ''}><c:out value="${sh.fullName}"/> (<c:out value="${sh.phone}"/>)</option>
            </c:forEach>
          </select>
        </div>
        <div class="col-md-4">
          <label class="form-label fw-semibold small">Ngày thu tiền</label>
          <input class="form-control rounded-3" name="date" aria-label="Ngày thu tiền" type="date" value="${selectedDate}">
        </div>
        <div class="col-md-4">
          <label class="form-label fw-semibold small">Phương thức thu</label>
          <select class="form-select rounded-3" name="method" aria-label="Phương thức thu">
            <option value="CASH" ${selectedMethod == 'CASH' ? 'selected' : ''}>Tiền mặt (Cash)</option>
            <option value="VNPAY" ${selectedMethod == 'VNPAY' ? 'selected' : ''}>VNPAY</option>
            <option value="TRANSFER" ${selectedMethod == 'TRANSFER' ? 'selected' : ''}>Chuyển khoản</option>
          </select>
        </div>
        <div class="col-12 d-flex justify-content-start">
          <button type="submit" class="btn btn-ute-primary rounded-pill px-4">
            <i class="bi bi-cloud-arrow-down me-1"></i> Tải khoản thu đã ghi nhận
          </button>
        </div>
      </form>
    </div>
  </div>

  <!-- KHOẢN THU CẦN ĐỐI SOÁT -->
  <div class="card border-0 shadow-sm rounded-4 mb-4">
    <div class="card-body p-4">
      <div class="mb-3">
        <h2 class="h5 fw-bold text-ink mb-1">
          <i class="bi bi-wallet2 me-2 text-primary"></i>Khoản thu cần đối soát — <span class="badge bg-secondary-subtle text-secondary-emphasis"><c:out value="${selectedMethod}"/></span>
        </h2>
        <p class="text-muted small mb-0">Mức kỳ vọng lấy từ khoản thu đã ghi nhận. Nhập số tiền thực tế đã bàn giao hoặc xác minh cho từng khoản.</p>
      </div>

      <c:if test="${empty expectedItems}">
        <div class="text-center py-5 text-muted">
          <i class="bi bi-receipt fs-1 d-block mb-2 text-secondary opacity-50"></i>
          Không có khoản thu nào trong lựa chọn hiện tại. Vui lòng chọn Shipper và Ngày để tải danh sách.
        </div>
      </c:if>

      <c:if test="${not empty expectedItems}">
        <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/submit">
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
          <input type="hidden" name="shipperId" value="${shipperId}">
          <input type="hidden" name="reconciliationDate" value="${selectedDate}">
          <input type="hidden" name="collectionMethod" value="${fn:escapeXml(selectedMethod)}">

          <c:if test="${selectedMethod != 'CASH'}">
            <div class="p-3 bg-light rounded-3 mb-3 border">
              <label class="form-label fw-semibold small">Bằng chứng xác minh khoản tiền đã nhận</label>
              <input class="form-control rounded-3" name="settlementEvidence" maxlength="255" placeholder="Mã tham chiếu ngân hàng, mã giao dịch sao kê..." required>
              <p class="text-muted small mb-0 mt-1">Ghi mã tham chiếu và căn cứ đã kiểm tra. Chỉ xác nhận sau khi đối chiếu chứng từ nhận tiền.</p>
            </div>
          </c:if>

          <div class="table-responsive">
            <table class="table align-middle mb-0">
              <thead class="table-light">
                <tr>
                  <th class="py-3 px-3 text-start fw-semibold small text-muted">Vận đơn</th>
                  <th class="py-3 px-3 text-start fw-semibold small text-muted">Khoản thu</th>
                  <th class="py-3 px-3 text-start fw-semibold small text-muted">Kỳ vọng (đ)</th>
                  <th class="py-3 px-3 text-start fw-semibold small text-muted" style="min-width: 160px;">Thực tế (đ)</th>
                  <th class="py-3 px-3 text-start fw-semibold small text-muted">Ghi chú</th>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${expectedItems}" var="item" varStatus="row">
                  <tr>
                    <td class="py-3 px-3 fw-bold text-ink">
                      <c:out value="${item.trackingNumber}"/>
                      <input type="hidden" name="items[${row.index}].shipmentId" value="${item.shipmentId}">
                    </td>
                    <td class="py-3 px-3">
                      <c:out value="${item.description}"/>
                      <input type="hidden" name="items[${row.index}].sourceType" value="${item.sourceType}">
                      <input type="hidden" name="items[${row.index}].sourceId" value="${item.sourceId}">
                    </td>
                    <td class="py-3 px-3 fw-semibold text-secondary">
                      <c:out value="${item.expectedAmount}"/>
                    </td>
                    <td class="py-3 px-3">
                      <input class="form-control rounded-3 form-control-sm" aria-label="Số tiền thực tế" type="number" min="0" step="1" required name="items[${row.index}].actualAmount">
                    </td>
                    <td class="py-3 px-3">
                      <input class="form-control rounded-3 form-control-sm" aria-label="Ghi chú" maxlength="255" placeholder="Ghi chú nếu có..." name="items[${row.index}].note">
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </div>

          <!-- THAO TÁC CĂN TRÁI (FIGMA MASTER 118:646) -->
          <div class="d-flex justify-content-start align-items-center gap-3 mt-4 flex-wrap">
            <button class="btn btn-ute-accent rounded-pill px-4" type="submit">
              Xác nhận đối soát
            </button>
            <button type="button" class="btn btn-outline-secondary rounded-pill px-4" id="fill-expected">
              Điền đủ số tiền kỳ vọng
            </button>
          </div>

          <script>
            document.getElementById('fill-expected').addEventListener('click', function () {
              document.querySelectorAll('input[name$=".actualAmount"]').forEach(function (input) {
                var expected = input.closest('tr').children[2].textContent.trim();
                var n = Math.round(parseFloat(expected));
                if (!isNaN(n)) input.value = n;
              });
            });
          </script>
        </form>
      </c:if>
    </div>
  </div>

  <!-- COD ĐÃ ĐỐI SOÁT & CHỜ TRẢ NGƯỜI GỬI -->
  <div class="card border-0 shadow-sm rounded-4 mb-4">
    <div class="card-body p-4">
      <h2 class="h5 fw-bold text-ink mb-3"><i class="bi bi-cash-stack me-2 text-success"></i>COD đã đối soát, chờ trả người gửi</h2>
      <c:if test="${empty readyForPayout}">
        <p class="text-muted small mb-0">Không có COD nào đang chờ hoàn trả vào ví người gửi.</p>
      </c:if>
      <c:if test="${not empty readyForPayout}">
        <div class="row g-3">
          <c:forEach items="${readyForPayout}" var="item">
            <div class="col-md-6 col-lg-4">
              <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/payout" class="card border rounded-3 p-3 h-100">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <input type="hidden" name="codTransactionId" value="${item.sourceId}">
                <div class="fw-bold text-ink mb-1"><c:out value="${item.trackingNumber}"/></div>
                <div class="text-success fw-bold fs-5 mb-3">${item.expectedAmount} đ</div>
                <div class="mt-auto d-flex justify-content-start">
                  <button class="btn btn-ute-primary rounded-pill btn-sm px-3" type="submit">
                    <i class="bi bi-wallet-fill me-1"></i> Trả COD vào ví người gửi
                  </button>
                </div>
              </form>
            </div>
          </c:forEach>
        </div>
      </c:if>
    </div>
  </div>

  <!-- LỊCH SỬ ĐỐI SOÁT -->
  <div class="card border-0 shadow-sm rounded-4 mb-4">
    <div class="card-body p-4">
      <h2 class="h5 fw-bold text-ink mb-3"><i class="bi bi-clock-history me-2 text-secondary"></i>Lịch sử đối soát</h2>
      <c:if test="${empty history}">
        <p class="text-muted small mb-0">Chưa có lịch sử đối soát nào được lưu.</p>
      </c:if>
      <c:if test="${not empty history}">
        <div class="d-flex flex-wrap gap-2 mb-3">
          <c:forEach items="${history}" var="rec">
            <a href="${pageContext.request.contextPath}/manager/reconciliation/detail?reconciliationId=${rec.id()}" class="btn btn-light btn-sm border rounded-pill px-3">
              #${rec.id()} · <c:out value="${rec.date()}"/> · <span class="badge bg-secondary-subtle text-secondary-emphasis"><c:out value="${rec.collectionMethod()}"/></span>
            </a>
          </c:forEach>
        </div>
      </c:if>

      <c:if test="${not empty reconciliationItems}">
        <div class="table-responsive mt-3">
          <table class="table align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th class="py-2 px-3 text-start small text-muted">Vận đơn</th>
                <th class="py-2 px-3 text-start small text-muted">Kỳ vọng</th>
                <th class="py-2 px-3 text-start small text-muted">Thực tế</th>
                <th class="py-2 px-3 text-start small text-muted">Chênh lệch</th>
                <th class="py-2 px-3 text-start small text-muted">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${reconciliationItems}" var="item">
                <tr>
                  <td class="py-2 px-3 fw-semibold"><c:out value="${item.trackingNumber()}"/></td>
                  <td class="py-2 px-3">${item.expectedAmount()}</td>
                  <td class="py-2 px-3">${item.actualAmount()}</td>
                  <td class="py-2 px-3 ${item.differenceAmount() != 0 ? 'text-danger fw-bold' : 'text-muted'}">${item.differenceAmount()}</td>
                  <td class="py-2 px-3"><span class="badge bg-secondary-subtle text-secondary-emphasis rounded-pill"><c:out value="${item.status()}"/></span></td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:if>
    </div>
  </div>

  <!-- HOÀN PHÍ & BỒI THƯỜNG -->
  <div class="row g-4">
    <!-- Hoàn phí đơn đã hủy -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100">
        <div class="card-body p-4">
          <h2 class="h5 fw-bold text-ink mb-3"><i class="bi bi-arrow-counterclockwise me-2 text-warning"></i>Hoàn phí đơn đã hủy</h2>
          <c:if test="${empty pendingRefunds}">
            <p class="text-muted small mb-0">Không có khoản phí nào cần hoàn.</p>
          </c:if>
          <c:forEach items="${pendingRefunds}" var="item">
            <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/refund" class="border rounded-3 p-3 mb-2">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
              <input type="hidden" name="paymentId" value="${item.paymentId()}">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="fw-bold"><c:out value="${item.trackingNumber()}"/></span>
                <span class="text-danger fw-bold">${item.amount()} đ</span>
              </div>
              <div class="d-flex justify-content-start">
                <button class="btn btn-ute-primary btn-sm rounded-pill px-3" type="submit">Hoàn phí vào ví người gửi</button>
              </div>
            </form>
          </c:forEach>
        </div>
      </div>
    </div>

    <!-- Bồi thường kiện mất -->
    <div class="col-lg-6">
      <div class="card border-0 shadow-sm rounded-4 h-100">
        <div class="card-body p-4">
          <h2 class="h5 fw-bold text-ink mb-3"><i class="bi bi-shield-exclamation me-2 text-danger"></i>Bồi thường kiện đã xác nhận mất</h2>
          <c:if test="${empty confirmedLosses}">
            <p class="text-muted small mb-0">Không có kiện hàng nào chờ bồi thường.</p>
          </c:if>
          <c:forEach items="${confirmedLosses}" var="item">
            <form method="post" action="${pageContext.request.contextPath}/manager/reconciliation/compensate" class="border rounded-3 p-3 mb-2">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
              <input type="hidden" name="shipmentId" value="${item.shipmentId()}">
              <input type="hidden" name="incidentId" value="${item.incidentId()}">
              <div class="fw-bold mb-1"><c:out value="${item.trackingNumber()}"/> — sự cố #${item.incidentId()}</div>
              <div class="mb-2">
                <input class="form-control form-control-sm rounded-3" name="reason" maxlength="255" placeholder="Lý do quyết định bồi thường..." required>
              </div>
              <div class="d-flex justify-content-start">
                <button class="btn btn-danger btn-sm rounded-pill px-3" type="submit">Xác nhận bồi thường</button>
              </div>
            </form>
          </c:forEach>
        </div>
      </div>
    </div>
  </div>
</main>

<jsp:include page="../layout/footer.jsp" />
