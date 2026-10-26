<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tra cứu vận đơn | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<main class="container py-4" style="max-width: 960px">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1 class="h3 mb-3">Tra cứu vận đơn</h1>
    <form class="card-ute p-4 mb-3 row g-2 align-items-end" method="get" action="${pageContext.request.contextPath}/tracking">
        <div class="col-sm-9">
            <label class="form-label" for="code">Mã vận đơn</label>
            <input class="form-control font-monospace" id="code" name="code" value="<c:out value='${code}'/>" required maxlength="50" placeholder="UTX-...">
        </div>
        <div class="col-sm-3"><button class="btn btn-ute-accent w-100" type="submit">Tra cứu</button></div>
    </form>
    <c:if test="${notFound}"><div class="alert alert-warning" role="status">Không tìm thấy vận đơn với mã này.</div></c:if>
    <c:if test="${not empty tracking}">
        <section class="card-ute p-4">
            <h2 class="h5">Vận đơn <span class="font-monospace"><c:out value="${tracking.trackingNumber}"/></span></h2>
            <p class="mb-1">Trạng thái: <span class="badge text-bg-primary"><c:out value="${tracking.currentStatus}"/></span></p>
            <p class="mb-1">Bưu cục hiện tại: <c:out value="${tracking.currentHubName}" default="Chưa đến bưu cục"/></p>
            <p class="text-ute-muted small">Thời gian giao dự kiến: chưa có ước tính.</p>
            <ul class="list-group list-group-flush">
                <c:forEach items="${tracking.events}" var="event">
                    <li class="list-group-item px-0"><c:out value="${event.label}"/> <span class="text-ute-muted small"><c:out value="${event.occurredAt}"/></span></li>
                </c:forEach>
            </ul>
        </section>
    </c:if>
</main>
</body>
</html>
