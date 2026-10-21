<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Shipment ${shipment.trackingNumber} | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-5 ute-page-medium">
    <a class="btn btn-outline-secondary btn-sm mb-3" href="${pageContext.request.contextPath}/user/shipments/create">Create another</a>
    <section class="card shadow-sm mb-4">
        <div class="card-body">
            <h1 class="h3">Shipment <c:out value="${shipment.trackingNumber}"/></h1>
            <p class="mb-1">Status: <span class="badge text-bg-primary"><c:out value="${shipment.currentStatus}"/></span></p>
            <p class="text-muted mb-0">Created: <c:out value="${shipment.createdAt}"/></p>
        </div>
    </section>
    <section class="card shadow-sm">
        <div class="card-body">
            <h2 class="h5">Tracking history</h2>
            <ul class="list-group list-group-flush">
                <c:forEach items="${shipment.events}" var="event">
                    <li class="list-group-item px-0"><strong><c:out value="${event.eventType}"/></strong><br><c:out value="${event.description}"/> <span class="text-muted small"><c:out value="${event.occurredAt}"/></span></li>
                </c:forEach>
            </ul>
        </div>
    </section>
</main>
</body>
</html>
