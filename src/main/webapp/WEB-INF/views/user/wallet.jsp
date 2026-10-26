<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">

    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3 mb-0 text-ute-dark">Ví điện tử của tôi</h1>
        <a href="${pageContext.request.contextPath}/user/shipments" class="btn btn-outline-secondary btn-sm rounded-pill">
            <i class="bi bi-boxes me-1"></i>Vận đơn của tôi
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
        <div class="col-lg-4">
            <div class="card-ute p-4 text-white mb-4">
                <h2 class="h6 text-uppercase small opacity-75 mb-2">Số dư khả dụng</h2>
                <p class="display-6 fw-bold mb-1">
                    <fmt:formatNumber value="${wallet.balance}" type="currency" currencySymbol="₫"/>
                </p>
                <hr class="border-light opacity-25">
                <p class="small mb-1 opacity-75">Số dư đang bị khóa chờ duyệt rút tiền</p>
                <p class="h5 fw-bold mb-0">
                    <fmt:formatNumber value="${wallet.lockedBalance}" type="currency" currencySymbol="₫"/>
                </p>
            </div>

            <div class="card-ute p-4">
                <h2 class="h5 mb-3 text-ute-dark">Yêu cầu rút tiền</h2>
                <form action="${pageContext.request.contextPath}/user/wallet/withdraw" method="post" novalidate>
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                    <div class="mb-3">
                        <label for="amount" class="form-label fw-semibold text-ute-dark">
                            Số tiền rút (VNĐ) <span class="text-danger">*</span>
                        </label>
                        <input type="number" step="0.01" min="10000" required
                               class="form-control ${not empty errors.amount ? 'is-invalid' : ''}"
                               id="amount" name="amount" value="${fn:escapeXml(formData.amount)}"/>
                        <c:if test="${not empty errors.amount}">
                            <div class="invalid-feedback d-block fw-semibold">
                                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.amount}"/>
                            </div>
                        </c:if>
                    </div>

                    <div class="mb-3">
                        <label for="bankName" class="form-label fw-semibold text-ute-dark">
                            Ngân hàng <span class="text-danger">*</span>
                        </label>
                        <input type="text" required maxlength="100"
                               class="form-control ${not empty errors.bankName ? 'is-invalid' : ''}"
                               id="bankName" name="bankName" value="${fn:escapeXml(formData.bankName)}"/>
                        <c:if test="${not empty errors.bankName}">
                            <div class="invalid-feedback d-block fw-semibold">
                                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.bankName}"/>
                            </div>
                        </c:if>
                    </div>

                    <div class="mb-3">
                        <label for="accountNumber" class="form-label fw-semibold text-ute-dark">
                            Số tài khoản <span class="text-danger">*</span>
                        </label>
                        <input type="text" required maxlength="50"
                               class="form-control ${not empty errors.accountNumber ? 'is-invalid' : ''}"
                               id="accountNumber" name="accountNumber" value="${fn:escapeXml(formData.accountNumber)}"/>
                        <c:if test="${not empty errors.accountNumber}">
                            <div class="invalid-feedback d-block fw-semibold">
                                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.accountNumber}"/>
                            </div>
                        </c:if>
                    </div>

                    <div class="mb-3">
                        <label for="accountHolderName" class="form-label fw-semibold text-ute-dark">
                            Chủ tài khoản <span class="text-danger">*</span>
                        </label>
                        <input type="text" required maxlength="100"
                               class="form-control ${not empty errors.accountHolderName ? 'is-invalid' : ''}"
                               id="accountHolderName" name="accountHolderName" value="${fn:escapeXml(formData.accountHolderName)}"/>
                        <c:if test="${not empty errors.accountHolderName}">
                            <div class="invalid-feedback d-block fw-semibold">
                                <i class="bi bi-exclamation-circle me-1"></i><c:out value="${errors.accountHolderName}"/>
                            </div>
                        </c:if>
                    </div>

                    <div class="mb-3">
                        <label for="requestKey" class="form-label fw-semibold text-ute-dark">Mã idempotency</label>
                        <input type="text" maxlength="100" class="form-control"
                               id="requestKey" name="requestKey" value="${fn:escapeXml(formData.requestKey)}"/>
                        <div class="form-text">Gửi lại cùng mã sẽ không tạo yêu cầu trùng.</div>
                    </div>

                    <button type="submit" class="btn btn-ute-accent w-100">
                        <i class="bi bi-bank me-1"></i>Gửi yêu cầu rút tiền
                    </button>
                </form>
            </div>
        </div>

        <div class="col-lg-8">
            <div class="card-ute p-4 mb-4">
                <h2 class="h5 mb-3 text-ute-dark">Lịch sử biến động số dư</h2>
                <c:choose>
                    <c:when test="${empty transactions}">
                        <p class="text-muted mb-0">Chưa có giao dịch nào trên ví.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-ute align-middle mb-0">
                                <thead>
                                    <tr>
                                        <th>Thời điểm</th>
                                        <th>Loại giao dịch</th>
                                        <th class="text-end">Số tiền</th>
                                        <th class="text-end">Số dư sau</th>
                                        <th>Mô tả</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="tx" items="${transactions}">
                                        <tr>
                                            <td class="small text-ute-muted">
                                                <c:out value="${tx.createdAt}"/>
                                            </td>
                                            <td><code class="small"><c:out value="${tx.transactionType}"/></code></td>
                                            <td class="text-end fw-bold">
                                                <fmt:formatNumber value="${tx.amount}" type="currency" currencySymbol="₫"/>
                                            </td>
                                            <td class="text-end">
                                                <fmt:formatNumber value="${tx.balanceAfter}" type="currency" currencySymbol="₫"/>
                                            </td>
                                            <td class="small text-ute-muted"><c:out value="${tx.description}"/></td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <div class="card-ute p-4">
                <h2 class="h5 mb-3 text-ute-dark">Lệnh rút tiền của tôi</h2>
                <c:choose>
                    <c:when test="${empty withdrawals}">
                        <p class="text-muted mb-0">Chưa có lệnh rút tiền nào.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="table-responsive">
                            <table class="table table-ute align-middle mb-0">
                                <thead>
                                    <tr>
                                        <th>Mã</th>
                                        <th>Ngân hàng</th>
                                        <th class="text-end">Số tiền</th>
                                        <th>Trạng thái</th>
                                        <th>Thời điểm</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="request" items="${withdrawals}">
                                        <tr>
                                            <td class="fw-bold text-ute-primary">#<c:out value="${request.id}"/></td>
                                            <td>
                                                <c:out value="${request.bankName}"/>
                                                <small class="d-block text-ute-muted">
                                                    <c:out value="${request.accountNumber}"/>
                                                </small>
                                            </td>
                                            <td class="text-end fw-bold">
                                                <fmt:formatNumber value="${request.amount}" type="currency" currencySymbol="₫"/>
                                            </td>
                                            <td>
                                                <span class="badge ${request.status == 'COMPLETED' ? 'bg-success' : (request.status == 'REJECTED' ? 'bg-danger' : 'bg-warning text-dark')}">
                                                    <c:out value="${request.status}"/>
                                                </span>
                                                <c:if test="${not empty request.rejectionReason}">
                                                    <small class="d-block text-danger">
                                                        <c:out value="${request.rejectionReason}"/>
                                                    </small>
                                                </c:if>
                                            </td>
                                            <td class="small text-ute-muted">
                                                <c:out value="${request.requestedAt}"/>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</main>

<jsp:include page="../layout/footer.jsp" />
