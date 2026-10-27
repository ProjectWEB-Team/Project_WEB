<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Operations Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-4">
    <header class="mb-4">
        <h1 class="h3 mb-1">Operations Dashboard</h1>
        <p class="text-muted mb-0">Scope: <c:out value="${dashboard.scopeLabel}"/></p>
    </header>
    <div class="row row-cols-1 row-cols-md-3 g-3" aria-label="Scoped operational KPIs">
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Inbound at hub</h2><p class="display-6 mb-0"><c:out value="${dashboard.inboundCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Outbound containers</h2><p class="display-6 mb-0"><c:out value="${dashboard.outboundCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Sorting backlog</h2><p class="display-6 mb-0"><c:out value="${dashboard.sortingBacklogCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Active handovers</h2><p class="display-6 mb-0"><c:out value="${dashboard.activeHandoverCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Active trips</h2><p class="display-6 mb-0"><c:out value="${dashboard.activeTripCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Blocked routes</h2><p class="display-6 mb-0"><c:out value="${dashboard.blockedRouteCount}"/></p></div></div></div>
        <div class="col"><div class="card h-100"><div class="card-body"><h2 class="h6">Actionable incidents</h2><p class="display-6 mb-0"><c:out value="${dashboard.actionableIncidentCount}"/></p></div></div></div>
    </div>
    <section class="mt-4">
        <h2 class="h5">Action required</h2>
        <p class="text-muted">Up to 25 open items per category in your current scope.</p>
        <ul class="list-group">
            <c:forEach items="${workItems}" var="item">
                <li class="list-group-item"><a href="${pageContext.request.contextPath}/manager/operations/work/${item.kind}/${item.id}"><c:out value="${item.summary}"/> #<c:out value="${item.id}"/></a></li>
            </c:forEach>
            <c:if test="${empty workItems}"><li class="list-group-item text-muted">No open work items in this scope.</li></c:if>
        </ul>
    </section>
</main>
</body>
</html>
