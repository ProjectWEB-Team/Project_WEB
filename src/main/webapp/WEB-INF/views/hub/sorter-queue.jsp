<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hub sorter workspace | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4">
    <h1 class="h3">Hub sorter workspace</h1>
    <p class="text-muted">Receive at your assigned hub, then request the server-derived static next hop.</p>
    <c:if test="${not empty operationMessage}"><div class="alert alert-info"><c:out value="${operationMessage}"/></div></c:if>
    <c:if test="${not empty sortingDecision}">
        <div class="alert alert-success">
            Next hub: <strong><c:out value="${sortingDecision.nextHubCode}"/></strong>
            <span class="ms-2"><c:out value="${sortingDecision.destinationGrouping}"/></span>
            <span class="ms-2">Container readiness: <c:out value="${sortingDecision.containerReady}"/></span>
        </div>
    </c:if>
    <div class="row g-3">
        <section class="col-md-6"><div class="card"><div class="card-body">
            <h2 class="h5">Inbound receipt</h2>
            <form method="post" action="${pageContext.request.contextPath}/hub/inbound">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <label class="form-label" for="inboundShipmentId">Shipment ID</label>
                <input id="inboundShipmentId" name="shipmentId" type="number" min="1" class="form-control" required>
                <p class="text-muted mt-2">For self-dropoff, scan the parcel and enter the measured values. Cash includes the unpaid base fee and any accepted surcharge.</p>
                <label class="form-label" for="trackingNumber">Parcel tracking number</label>
                <input id="trackingNumber" name="trackingNumber" class="form-control">
                <label class="form-label" for="actualWeightGrams">Actual weight (g)</label>
                <input id="actualWeightGrams" name="actualWeightGrams" type="number" min="1" class="form-control">
                <label class="form-label" for="actualLengthCm">Length (cm)</label>
                <input id="actualLengthCm" name="actualLengthCm" type="number" min="1" class="form-control">
                <label class="form-label" for="actualWidthCm">Width (cm)</label>
                <input id="actualWidthCm" name="actualWidthCm" type="number" min="1" class="form-control">
                <label class="form-label" for="actualHeightCm">Height (cm)</label>
                <input id="actualHeightCm" name="actualHeightCm" type="number" min="1" class="form-control">
                <label class="form-label" for="cashReceived">Cash received (VND)</label>
                <input id="cashReceived" name="cashReceived" type="number" min="0" step="1" class="form-control">
                <label class="form-label" for="surchargeDecision">Sender decision if a surcharge is due</label>
                <select id="surchargeDecision" name="surchargeDecision" class="form-select">
                    <option value="">No decision yet</option><option value="ACCEPT">Accept and pay cash</option>
                    <option value="PAY_ONLINE">Yêu cầu thanh toán phụ thu bằng ví / VNPay demo</option><option value="REJECT">Refuse surcharge and cancel</option>
                </select>
                <button class="btn btn-primary mt-3" type="submit">Receive at hub</button>
            </form>
        </div></div></section>
        <section class="col-md-6"><div class="card"><div class="card-body">
            <h2 class="h5">Static route sorting</h2>
            <form method="post" action="${pageContext.request.contextPath}/hub/sorter">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <label class="form-label" for="sortShipmentId">Shipment ID</label>
                <input id="sortShipmentId" name="shipmentId" type="number" min="1" class="form-control" required>
                <button class="btn btn-success mt-3" type="submit">Resolve static next hop</button>
            </form>
        </div></div></section>
    </div>
</main>
</body>
</html>
