<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Đã có lỗi xảy ra | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-error flex-grow-1">
    <p class="ute-error-code">500</p>
    <h1>Lỗi hệ thống</h1>
    <p class="ute-error-desc">Hệ thống gặp sự cố. Vui lòng thử lại sau.</p>
    <div class="ute-error-actions"><a class="ute-error-btn is-ink" href="${pageContext.request.contextPath}/">Về trang chủ</a><a class="ute-error-btn is-ghost" href="${pageContext.request.contextPath}/tracking">Tra cứu vận đơn</a></div>
</main><jsp:include page="/WEB-INF/views/layout/footer.jsp" />
