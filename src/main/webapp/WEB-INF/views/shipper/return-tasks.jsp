<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Nhiệm vụ giao hàng hoàn | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4 ute-page-medium">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3">Nhiệm vụ giao hàng hoàn</h1>
        <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/shipper/delivery-tasks">Quay lại giao hàng</a>
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

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3">
            <h2 class="h5 mb-0">Giao trả hàng cho người gửi</h2>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Mã vận đơn</th>
                        <th>Người gửi</th>
                        <th>Trạng thái</th>
                        <th>Cước chiều đi + chiều hoàn còn lại</th>
                        <th>Thao tác</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${empty tasks}">
                            <tr><td colspan="5" class="text-center py-4 text-muted">Chưa có nhiệm vụ giao hàng hoàn</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${tasks}" var="task">
                                <tr>
                                    <td><strong><c:out value="${task.trackingNumber}"/></strong></td>
                                    <td><c:out value="${task.senderName}"/><br><small class="text-muted"><c:out value="${task.senderPhone}"/></small></td>
                                    <td><u:statusbadge status="${task.currentStatus}"/></td>
                                    <td><c:out value="${task.outstandingFee}"/> (<c:out value="${task.feeSettled ? 'Settled' : 'Unpaid'}"/>)</td>
                                    <td>
                                        <form method="post" action="${pageContext.request.contextPath}/shipper/return-tasks/scan"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input name="shipmentId" type="hidden" value="${task.shipmentId}"><button>Quét xuất kho</button></form>
                                        <form method="post" action="${pageContext.request.contextPath}/shipper/return-tasks/cash"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input name="shipmentId" type="hidden" value="${task.shipmentId}"><button>Xác nhận thu đủ phí tiền mặt</button></form>
                                        <form action="${pageContext.request.contextPath}/shipper/return-tasks/confirm" method="post">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                            <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                            <button type="submit" class="btn btn-warning btn-sm">Xác nhận đã trả hàng</button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

