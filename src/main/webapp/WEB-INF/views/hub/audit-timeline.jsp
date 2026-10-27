<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Custody Audit Timeline</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4">
    <h1 class="h3">Custody Audit Timeline</h1>
    <p class="text-muted">Shipment #<c:out value="${timeline.shipmentId}"/></p>
    <div class="table-responsive">
        <table class="table table-striped align-middle">
            <thead><tr><th>Event</th><th>Time</th><th>Hub</th><th>Actor role</th><th>Custody reference</th></tr></thead>
            <tbody>
            <c:forEach items="${timeline.entries}" var="entry">
                <tr>
                    <td><c:out value="${entry.eventType}"/></td>
                    <td><c:out value="${entry.occurredAt}"/></td>
                    <td><c:out value="${entry.hubCode}"/></td>
                    <td><c:out value="${entry.actorRole}"/></td>
                    <td>Container <c:out value="${entry.containerId}"/> / Handover <c:out value="${entry.handoverId}"/> / Trip <c:out value="${entry.tripId}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty timeline.entries}"><tr><td colspan="5" class="text-muted">No authorized operational events are available.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</main>
</body>
</html>
