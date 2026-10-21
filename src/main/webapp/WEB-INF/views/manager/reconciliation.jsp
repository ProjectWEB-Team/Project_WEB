<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="https://jakarta.ee/tags/jstl/core" %>
<%@ taglib prefix="fmt" uri="https://jakarta.ee/tags/jstl/fmt" %>
<%@ taglib prefix="fn" uri="https://jakarta.ee/tags/jstl/functions" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">

    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3 mb-0 text-ute-dark">Đối soát ca Shipper</h1>
        <a href="${pageContext.request.contextPath}/user/wallet" class="btn btn-outline-secondary btn-sm rounded-pill">
            <i class="bi bi-wallet2 me-1"></i>Ví điện tử
        </a>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    </c:if>

    <div class="row g-4">
        <div class="col-lg-8">
            <div class="card-ute p-4">
                <h2 class="h5 mb-3 text-ute-dark">Đối soát chi tiết từng đơn trong ca</h2>

                <form action="${pageContext.request.contextPath}/manager/reconciliation/submit" method="post" id="reconciliationForm">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label for="shipperId" class="form-label fw-semibold text-ute-dark">
                                Shipper User ID <span class="text-danger">*</span>
                            </label>
                            <input type="number" class="form-control" id="shipperId" name="shipperId" required/>
                        </div>
                        <div class="col-md-6">
                            <label for="reconciliationDate" class="form-label fw-semibold text-ute-dark">Ngày</label>
                            <input type="date" class="form-control" id="reconciliationDate" name="reconciliationDate"/>
                        </div>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h3 class="h6 mb-0 text-ute-dark">Các khoản tiền kỳ vọng</h3>
                        <button type="button" class="btn btn-outline-primary btn-sm" id="addItemButton">
                            <i class="bi bi-plus-lg me-1"></i>Thêm dòng
                        </button>
                    </div>

                    <div class="table-responsive mb-3">
                        <table class="table table-ute align-middle mb-0" id="itemTable">
                            <thead>
                                <tr>
                                    <th>Mã vận đơn</th>
                                    <th>Loại nguồn</th>
                                    <th>Mã nguồn</th>
                                    <th class="text-end">Kỳ vọng</th>
                                    <th class="text-end">Thực tế</th>
                                    <th>Ghi chú</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody id="itemTableBody">
                                <c:if test="${not empty expectedItems}">
                                    <c:forEach var="expected" items="${expectedItems}" varStatus="loop">
                                        <tr>
                                            <td>
                                                <input type="hidden" name="items[${loop.index}].shipmentId" value="${expected.shipmentId}"/>
                                                <c:out value="${expected.trackingNumber}"/>
                                            </td>
                                            <td>
                                                <input type="hidden" name="items[${loop.index}].sourceType" value="${expected.sourceType}"/>
                                                <code class="small"><c:out value="${expected.sourceType}"/></code>
                                            </td>
                                            <td>
                                                <input type="hidden" name="items[${loop.index}].sourceId" value="${expected.sourceId}"/>
                                                <c:out value="${expected.sourceId}"/>
                                            </td>
                                            <td class="text-end">
                                                <input type="hidden" name="items[${loop.index}].expectedAmount" value="${expected.expectedAmount}"/>
                                                <fmt:formatNumber value="${expected.expectedAmount}" type="currency" currencySymbol="₫"/>
                                            </td>
                                            <td>
                                                <input type="number" step="0.01" class="form-control form-control-sm"
                                                       name="items[${loop.index}].actualAmount" required/>
                                            </td>
                                            <td>
                                                <input type="text" maxlength="255" class="form-control form-control-sm"
                                                       name="items[${loop.index}].note"/>
                                            </td>
                                            <td></td>
                                        </tr>
                                    </c:forEach>
                                </c:if>
                            </tbody>
                        </table>
                    </div>

                    <button type="submit" class="btn btn-ute-primary">
                        <i class="bi bi-clipboard-check me-1"></i>Lưu và đối soát
                    </button>
                </form>
            </div>
        </div>

        <div class="col-lg-4">
            <div class="card-ute p-4 mb-4">
                <h2 class="h5 mb-3 text-ute-dark">Các đơn cần giải ngân</h2>
                <c:choose>
                    <c:when test="${empty readyForPayout}">
                        <p class="text-muted mb-0">Chưa có khoản COD nào ở trạng thái sẵn giải ngân.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="item" items="${readyForPayout}">
                            <div class="border rounded p-3 mb-2">
                                <p class="mb-1 fw-bold text-ute-primary">
                                    <c:out value="${item.shipment.trackingNumber}"/>
                                </p>
                                <p class="small text-ute-muted mb-2">
                                    COD <fmt:formatNumber value="${item.actualAmount}" type="currency" currencySymbol="₫"/>
                                </p>
                                <form action="${pageContext.request.contextPath}/manager/reconciliation/payout" method="post">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="codTransactionId" value="${item.sourceId}"/>
                                    <button type="submit" class="btn btn-ute-accent btn-sm w-100">
                                        <i class="bi bi-cash-coin me-1"></i>Giải ngân về ví người gửi
                                    </button>
                                </form>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="card-ute p-4">
                <h2 class="h5 mb-3 text-ute-dark">Lịch sử đối soát</h2>
                <c:choose>
                    <c:when test="${empty history}">
                        <p class="text-muted mb-0">Chưa có phiếu đối soát nào.</p>
                    </c:when>
                    <c:otherwise>
                        <ul class="list-unstyled mb-0">
                            <c:forEach var="rec" items="${history}">
                                <li class="border-bottom pb-2 mb-2">
                                    <p class="mb-1 fw-semibold">
                                        #<c:out value="${rec.id}"/>
                                        <span class="badge ${rec.status == 'COMPLETED' ? 'bg-success' : 'bg-warning text-dark'}">
                                            <c:out value="${rec.status}"/>
                                        </span>
                                    </p>
                                    <p class="small text-ute-muted mb-0">
                                        <fmt:formatDate value="${rec.reconciliationDate}" pattern="dd/MM/yyyy"/>
                                        &mdash; Chênh lệch
                                        <fmt:formatNumber value="${rec.differenceAmount}" type="currency" currencySymbol="₫"/>
                                    </p>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    (function () {
        const tableBody = document.getElementById('itemTableBody');
        const addButton = document.getElementById('addItemButton');

        addButton.addEventListener('click', function () {
            const index = tableBody.querySelectorAll('tr').length;
            const row = document.createElement('tr');
            row.innerHTML = [
                '<td><input type="number" class="form-control form-control-sm" name="items[' + index + '].shipmentId" required/></td>',
                '<td><select class="form-select form-select-sm" name="items[' + index + '].sourceType" required>' +
                    '<option value="COD_TRANSACTION">COD Transaction</option>' +
                    '<option value="RECEIVER_CASH_SHIPPING_FEE_PAYMENT">Receiver Cash Shipping Fee</option>' +
                '</select></td>',
                '<td><input type="number" class="form-control form-control-sm" name="items[' + index + '].sourceId" required/></td>',
                '<td><input type="number" step="0.01" class="form-control form-control-sm" name="items[' + index + '].expectedAmount" required/></td>',
                '<td><input type="number" step="0.01" class="form-control form-control-sm" name="items[' + index + '].actualAmount" required/></td>',
                '<td><input type="text" maxlength="255" class="form-control form-control-sm" name="items[' + index + '].note"/></td>',
                '<td><button type="button" class="btn btn-outline-danger btn-sm" data-remove-row>&times;</button></td>'
            ].join('');
            tableBody.appendChild(row);
        });

        tableBody.addEventListener('click', function (event) {
            if (event.target.matches('[data-remove-row]')) {
                event.target.closest('tr').remove();
            }
        });
    })();
</script>

<jsp:include page="../layout/footer.jsp" />
