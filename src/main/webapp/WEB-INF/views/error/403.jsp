<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Không có quyền truy cập | UTEExpress</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet"></head>
<body class="bg-light">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5"><h1 class="h3">Không có quyền truy cập</h1><p>Tài khoản của bạn không được phép mở trang này.</p>
<a class="btn btn-primary" href="${pageContext.request.contextPath}/">Về trang chủ</a></main></body></html>
