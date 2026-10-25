<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Assigned Return Tasks | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-4 ute-page-medium">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3">Assigned Return Tasks</h1>
        <a class="btn btn-outline-secondary btn-sm" href="${pageContext.request.contextPath}/shipper/delivery-tasks">Back to Deliveries</a>
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

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3">
            <h2 class="h5 mb-0">Return to Sender Deliveries</h2>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Tracking #</th>
                        <th>Sender</th>
                        <th>Status</th>
                        <th>Return Fee</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${empty tasks}">
                            <tr><td colspan="5" class="text-center py-4 text-muted">No return delivery tasks assigned</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${tasks}" var="task">
                                <tr>
                                    <td><strong><c:out value="${task.trackingNumber}"/></strong></td>
                                    <td><c:out value="${task.senderName}"/><br><small class="text-muted"><c:out value="${task.senderPhone}"/></small></td>
                                    <td><span class="badge text-bg-warning"><c:out value="${task.currentStatus}"/></span></td>
                                    <td><c:out value="${task.returnFee}"/> (<c:out value="${task.feeSettled ? 'Settled' : 'Unpaid'}"/>)</td>
                                    <td>
                                        <form action="${pageContext.request.contextPath}/shipper/return-tasks/confirm" method="post">
                                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                            <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                            <button type="submit" class="btn btn-warning btn-sm">Confirm Return Handover</button>
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

