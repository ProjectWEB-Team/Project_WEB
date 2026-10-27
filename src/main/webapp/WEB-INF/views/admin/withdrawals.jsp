<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">

    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3 mb-0 text-ute-dark">Duyệt lệnh rút tiền</h1>
        <span class="badge bg-ute-primary">
            <i class="bi bi-shield-lock me-1"></i>ROLE_ADMIN
        </span>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            <c:out value="${successMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
        </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <c:out value="${errorMessage}"/>
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
        </div>
    </c:if>

    <div class="card-ute p-4">
        <h2 class="h5 mb-3 text-ute-dark">Lệnh rút tiền đang chờ duyệt</h2>

        <c:choose>
            <c:when test="${empty pendingWithdrawals}">
                <p class="text-muted mb-0">Không có lệnh rút tiền nào đang chờ duyệt.</p>
            </c:when>
            <c:otherwise>
                <div class="table-responsive">
                    <table class="table table-ute align-middle mb-0">
                        <thead>
                            <tr>
                                <th>Mã</th>
                                <th>Người gửi</th>
                                <th>Ngân hàng</th>
                                <th>Chủ tài khoản</th>
                                <th class="text-end">Số tiền</th>
                                <th>Thời điểm</th>
                                <th class="text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${pendingWithdrawals}">
                                <tr>
                                    <td class="fw-bold text-ute-primary">#<c:out value="${item.id}"/></td>
                                    <td><c:out value="${item.username}"/></td>
                                    <td>
                                        <c:out value="${item.bankName}"/>
                                        <small class="d-block text-ute-muted font-monospace">
                                            <c:out value="${item.accountNumber}"/>
                                        </small>
                                    </td>
                                    <td><c:out value="${item.accountHolderName}"/></td>
                                    <td class="text-end fw-bold text-danger">
                                        <fmt:formatNumber value="${item.amount}" type="currency" currencySymbol="₫"/>
                                    </td>
                                    <td class="small text-ute-muted">
                                        <c:out value="${item.requestedAt}"/>
                                    </td>
                                    <td>
                                        <div class="d-flex flex-column gap-2">
                                            <form action="${pageContext.request.contextPath}/admin/withdrawals/${item.id}/approve" method="post">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                <button type="submit" class="btn btn-ute-accent btn-sm w-100">
                                                    <i class="bi bi-check2-circle me-1"></i>Duyệt
                                                </button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/admin/withdrawals/${item.id}/reject" method="post">
                                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                <input type="hidden" name="requestId" value="${item.id}"/>
                                                <input type="text" class="form-control form-control-sm mb-1"
                                                       name="rejectionReason" maxlength="255"
                                                       placeholder="Lý do từ chối" required/>
                                                <button type="submit" class="btn btn-outline-danger btn-sm w-100">
                                                    <i class="bi bi-x-circle me-1"></i>Từ chối
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<jsp:include page="../layout/footer.jsp" />
