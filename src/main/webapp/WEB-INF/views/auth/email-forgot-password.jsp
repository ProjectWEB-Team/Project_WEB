<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Khôi phục mật khẩu qua email | UTEExpress</title>
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5"><div class="row justify-content-center"><div class="col-12 col-md-7 col-lg-5">
    <section class="card border-0 shadow-sm"><div class="card-body p-4 p-md-5">
        <h1 class="h3 mb-2">Quên mật khẩu</h1>
        <p class="text-muted">Nếu email đã xác thực thuộc tài khoản khách hàng đang hoạt động, mã đặt lại mật khẩu sẽ được gửi tới.</p>
        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
        <form method="post" action="${pageContext.request.contextPath}/auth/email/forgot-password">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <label for="email" class="form-label">Email đã xác thực</label>
            <input class="form-control mb-3" id="email" name="email" type="email" maxlength="100" autocomplete="email" required>
            <button class="btn btn-ute-accent w-100" type="submit">Nhận mã qua email</button>
        </form>
        <p class="small text-center mt-3 mb-0"><a href="${pageContext.request.contextPath}/auth/forgot-password">Dùng số điện thoại</a> · <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
    </div></section>
</div></div></main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
