<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Assigned Delivery Tasks | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-4 ute-page-medium">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h1 class="h3">Assigned Delivery Tasks</h1>
        <a class="btn btn-outline-primary btn-sm" href="${pageContext.request.contextPath}/shipper/return-tasks">View Return Tasks</a>
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

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white py-3">
            <h2 class="h5 mb-0">Hub Scan (Start Delivery)</h2>
        </div>
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/scan" method="post" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="col-md-6">
                    <label for="shipmentId" class="form-label">Shipment ID</label>
                    <input type="number" class="form-control" id="shipmentId" name="shipmentId" required>
                </div>
                <div class="col-md-4">
                    <label for="hubId" class="form-label">Current Hub ID</label>
                    <input type="number" class="form-control" id="hubId" name="hubId" required>
                </div>
                <div class="col-md-2 d-flex align-items-end">
                    <button type="submit" class="btn btn-primary w-100">Scan Out</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-header bg-white py-3">
            <h2 class="h5 mb-0">Ghi nhận giao thất bại</h2>
        </div>
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/failure" method="post"
                  enctype="multipart/form-data" class="row g-3">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="col-md-4">
                    <label for="failureShipmentId" class="form-label">Shipment ID</label>
                    <input type="number" class="form-control" id="failureShipmentId" name="shipmentId" required>
                </div>
                <div class="col-md-8">
                    <label for="failureReason" class="form-label">Lý do thất bại</label>
                    <select class="form-select" id="failureReason" name="reason" required>
                        <option value="NO_ANSWER">Không nghe máy</option>
                        <option value="PAYMENT_NOT_AVAILABLE">Chưa có tiền mặt</option>
                        <option value="RESCHEDULED">Khách hẹn ngày khác</option>
                        <option value="OTHER">Lý do khác</option>
                        <option value="RECEIVER_REFUSED">Khách từ chối nhận (fast-fail)</option>
                        <option value="WRONG_ADDRESS">Sai địa chỉ (fast-fail)</option>
                    </select>
                </div>
                <div class="col-md-6">
                    <label for="failureNote" class="form-label">Ghi chú</label>
                    <input type="text" maxlength="255" class="form-control" id="failureNote" name="note">
                </div>
                <div class="col-md-6">
                    <label for="failureEvidence" class="form-label">Ảnh bằng chứng</label>
                    <input type="file" class="form-control" id="failureEvidence" name="evidence" accept="image/*">
                </div>
                <div class="col-md-6">
                    <label for="failureRescheduleDate" class="form-label">Ngày giao lại (nếu hẹn lại)</label>
                    <input type="date" class="form-control" id="failureRescheduleDate" name="rescheduledDate">
                </div>
                <div class="col-12">
                    <button type="submit" class="btn btn-outline-danger">Ghi nhận thất bại</button>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="card-header bg-white py-3">
            <h2 class="h5 mb-0">Active Tasks</h2>
        </div>
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0">
                    <thead class="table-light">
                    <tr>
                        <th>Tracking #</th>
                        <th>Status</th>
                        <th>Receiver</th>
                        <th>Address</th>
                        <th>COD / Fee</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:choose>
                        <c:when test="${empty tasks}">
                            <tr><td colspan="6" class="text-center py-4 text-muted">No delivery tasks assigned</td></tr>
                        </c:when>
                        <c:otherwise>
                            <c:forEach items="${tasks}" var="task">
                                <tr>
                                    <td><strong><c:out value="${task.trackingNumber}"/></strong></td>
                                    <td><span class="badge text-bg-secondary"><c:out value="${task.currentStatus}"/></span></td>
                                    <td><c:out value="${task.receiverName}"/><br><small class="text-muted"><c:out value="${task.receiverPhone}"/></small></td>
                                    <td><c:out value="${task.receiverAddress}"/></td>
                                    <td>
                                        <small>
                                            COD: <c:out value="${task.codAmount}"/> (<c:out value="${task.codCollected ? 'Collected' : 'Pending'}"/>)<br>
                                            Fee: <c:out value="${task.shippingFee}"/> (<c:out value="${task.feePayer}"/> - <c:out value="${task.feeCollected ? 'Paid' : 'Pending'}"/>)
                                        </small>
                                    </td>
                                    <td>
                                        <div class="btn-group btn-group-sm">
                                            <c:if test="${!task.codCollected || !task.feeCollected}">
                                                <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/collect" method="post" class="d-inline">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                    <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                                    <label>COD amount received <input class="form-control form-control-sm" type="number" name="codAmountCollected" min="0" step="1" required value="${task.codAmount}"/></label>
                                                    <label>COD method <select class="form-select form-select-sm" name="codPaymentMethod"><option value="CASH">Cash</option><option value="VNPAY">VNPAY</option><option value="TRANSFER">Transfer</option></select></label>
                                                    <label>Receiver fee received <input class="form-control form-control-sm" type="number" name="shippingFeeCollected" min="0" step="1" required value="${task.feePayer == 'RECEIVER' ? task.shippingFee : 0}"/></label>
                                                    <label>Receiver fee method <select class="form-select form-select-sm" name="shippingFeePaymentMethod"><option value="CASH">Cash</option><option value="VNPAY">VNPAY</option></select></label>
                                                    <button type="submit" class="btn btn-outline-success btn-sm">Collect</button>
                                                </form>
                                            </c:if>
                                            <c:if test="${task.eligibleForDelivery}">
                                                <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/confirm" method="post" class="d-inline">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                                    <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                                    <button type="submit" class="btn btn-success btn-sm">Confirm Delivered</button>
                                                </form>
                                            </c:if>
                                        </div>
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

