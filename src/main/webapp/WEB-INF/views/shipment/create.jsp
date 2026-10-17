<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Create shipment | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-5" style="max-width: 860px">
    <h1 class="h3 mb-2">Create a shipment</h1>
    <p class="text-muted mb-4">Phase-01 tracer: sender and receiver snapshot only.</p>
    <form:form method="post" action="${pageContext.request.contextPath}/user/shipments" modelAttribute="createShipmentRequest" class="card shadow-sm">
        <div class="card-body">
            <h2 class="h5">Sender</h2>
            <div class="row g-3 mb-4">
                <div class="col-md-6"><label class="form-label" for="senderName">Name</label><form:input id="senderName" path="senderName" cssClass="form-control" htmlEscape="true"/><form:errors path="senderName" cssClass="text-danger small"/></div>
                <div class="col-md-6"><label class="form-label" for="senderPhone">Phone</label><form:input id="senderPhone" path="senderPhone" cssClass="form-control" htmlEscape="true"/><form:errors path="senderPhone" cssClass="text-danger small"/></div>
                <div class="col-12"><label class="form-label" for="senderAddress">Address</label><form:input id="senderAddress" path="senderAddress" cssClass="form-control" htmlEscape="true"/><form:errors path="senderAddress" cssClass="text-danger small"/></div>
            </div>
            <h2 class="h5">Receiver</h2>
            <div class="row g-3">
                <div class="col-md-6"><label class="form-label" for="receiverName">Name</label><form:input id="receiverName" path="receiverName" cssClass="form-control" htmlEscape="true"/><form:errors path="receiverName" cssClass="text-danger small"/></div>
                <div class="col-md-6"><label class="form-label" for="receiverPhone">Phone</label><form:input id="receiverPhone" path="receiverPhone" cssClass="form-control" htmlEscape="true"/><form:errors path="receiverPhone" cssClass="text-danger small"/></div>
                <div class="col-12"><label class="form-label" for="receiverAddress">Address</label><form:input id="receiverAddress" path="receiverAddress" cssClass="form-control" htmlEscape="true"/><form:errors path="receiverAddress" cssClass="text-danger small"/></div>
            </div>
        </div>
        <div class="card-footer bg-white d-flex justify-content-end"><button type="submit" class="btn btn-primary">Create shipment</button></div>
    </form:form>
</main>
</body>
</html>
