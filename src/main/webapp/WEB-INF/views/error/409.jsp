<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Dữ liệu vừa thay đổi | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="ute-error flex-grow-1">
    <p class="ute-error-code">409</p>
    <h1>Dữ liệu đã thay đổi</h1>
    <p class="ute-error-desc">Dữ liệu bạn đang thao tác đã được cập nhật ở nơi khác. Vui lòng tải lại trang để xem phiên bản mới nhất rồi thử lại.</p>
    <div class="ute-error-actions"><a class="ute-error-btn is-ghost" href="javascript:location.reload()">Tải lại trang</a><a class="ute-error-btn is-ink" href="${pageContext.request.contextPath}/">Về trang chủ</a></div>
</main><jsp:include page="/WEB-INF/views/layout/footer.jsp" />
