<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Không có quyền truy cập | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-error flex-grow-1">
    <p class="ute-error-code">403</p>
    <h1>Không có quyền truy cập</h1>
    <p class="ute-error-desc">Tài khoản của bạn chưa được cấp quyền cho trang này. Hãy đăng nhập bằng tài khoản phù hợp hoặc quay về trang chủ.</p>
    <div class="ute-error-actions"><a class="ute-error-btn is-ink" href="${pageContext.request.contextPath}/">Về trang chủ</a><a class="ute-error-btn is-ghost" href="${pageContext.request.contextPath}/login">Đăng nhập tài khoản khác</a></div>
</main><jsp:include page="/WEB-INF/views/layout/footer.jsp" />
