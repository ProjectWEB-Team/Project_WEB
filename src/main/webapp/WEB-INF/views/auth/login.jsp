<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="ute-auth">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container ute-auth-main">
    <div class="ute-auth-layout">
        <section class="ute-auth-brand" aria-label="UTEexpress"><jsp:include page="/WEB-INF/views/layout/logo.jsp" /><p>Giao nhanh. Trọn niềm tin.</p></section>
        <div class="ute-auth-form">
            <section class="card ute-auth-card">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h3 mb-2">Đăng nhập</h1>
                    <p class="text-muted mb-4">Gửi hàng và theo dõi hành trình tại một nơi.</p>
                    <c:if test="${not empty param.error}"><div class="alert alert-danger" role="alert">Tên đăng nhập hoặc mật khẩu không đúng.</div></c:if>
                    <c:if test="${not empty param.googleError}"><div class="alert alert-danger" role="alert">Không thể đăng nhập bằng Google. Vui lòng thử lại hoặc chọn cách đăng nhập khác.</div></c:if>
                    <c:if test="${not empty param.phoneError}"><div class="alert alert-danger" role="alert">Không thể đăng nhập bằng điện thoại. Vui lòng thử lại hoặc chọn cách đăng nhập khác.</div></c:if>
                    <c:if test="${not empty param.logout}"><div class="alert alert-success" role="status">Bạn đã đăng xuất.</div></c:if>
                    <c:if test="${not empty param.reset}"><div class="alert alert-success" role="status">Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.</div></c:if>
                    <c:if test="${not empty param.registered}"><div class="alert alert-success" role="status">Tài khoản đã được kích hoạt. Bạn có thể đăng nhập.</div></c:if>
                    <a class="btn btn-ute-accent w-100 mb-3" href="${pageContext.request.contextPath}/auth/phone-login">Đăng nhập bằng mã OTP điện thoại</a>
                    <c:if test="${googleLoginEnabled}">
                        <a class="btn btn-outline-secondary w-100 mb-3" href="${pageContext.request.contextPath}/oauth2/authorization/google">Đăng nhập bằng Google</a>
                    </c:if>
                    <section class="ute-password-login" aria-labelledby="pw-title">
                        <h2 id="pw-title" class="h6 text-muted mb-0">Hoặc đăng nhập bằng mật khẩu</h2>
                        <form method="post" action="${pageContext.request.contextPath}/login" class="mt-3 ute-login-form">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <div class="mb-3"><label class="form-label" for="username">Tên đăng nhập</label><input class="form-control" id="username" name="username" type="text" autocomplete="username" required></div>
                            <div class="mb-4"><label class="form-label" for="password">Mật khẩu</label><input class="form-control" id="password" name="password" type="password" autocomplete="current-password" required></div>
                            <button class="btn btn-primary w-100" type="submit">Đăng nhập</button>
                        </form>
                        <p class="small mt-2 mb-0"><a href="${pageContext.request.contextPath}/auth/forgot-password">Quên mật khẩu?</a></p>
                    </section>
                    <p class="text-center small mb-0 mt-4">Chưa có tài khoản? <a href="${pageContext.request.contextPath}/register">Đăng ký</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
