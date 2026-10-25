<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Origin Hub Return Collection | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-4 ute-page-narrow">
    <h1 class="h3 mb-4">Origin Hub Return Self-Pickup</h1>

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
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/user/return-collection" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <input type="hidden" name="handoverType" value="SELF_PICKUP"/>
                <div class="mb-3">
                    <label for="shipmentId" class="form-label">Shipment ID</label>
                    <input type="number" class="form-control" id="shipmentId" name="shipmentId" required>
                </div>
                <div class="mb-3">
                    <label for="identityProof" class="form-label">Identity Proof / OTP</label>
                    <input type="text" class="form-control" id="identityProof" name="identityProof" placeholder="Citizen ID or Pickup OTP" required>
                </div>
                <div class="mb-3">
                    <label for="settledReturnFee" class="form-label">Return Fee Paid (VND)</label>
                    <input type="number" step="0.01" class="form-control" id="settledReturnFee" name="settledReturnFee" placeholder="0.00">
                </div>
                <div class="mb-3">
                    <label for="paymentMethod" class="form-label">Phương thức thanh toán phí hoàn</label>
                    <select class="form-select" id="paymentMethod" name="paymentMethod">
                        <option value="CASH">Tiền mặt</option>
                        <option value="E_WALLET">Ví điện tử</option>
                        <option value="VNPAY">VNPay</option>
                    </select>
                </div>
                <button type="submit" class="btn btn-primary w-100">Collect Returned Package</button>
            </form>
        </div>
    </div>
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

