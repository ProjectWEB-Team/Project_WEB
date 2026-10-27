<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đặt lại mật khẩu | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-12 col-md-7 col-lg-5">
            <a class="d-inline-block text-ute-primary fw-bold text-decoration-none mb-4" href="${pageContext.request.contextPath}/">UTEExpress</a>
            <section class="card border-0 shadow-sm">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h3 mb-2">Đặt lại mật khẩu</h1>
                    <p class="text-muted mb-4">Nhập mã 6 chữ số đã gửi tới <strong><c:out value="${maskedPhone}"/></strong>. Không chia sẻ mã này với người khác.</p>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <c:if test="${not empty notice}"><div class="alert alert-success" role="status"><c:out value="${notice}"/></div></c:if>
                    <form method="post" action="${pageContext.request.contextPath}/auth/reset-password">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <div class="mb-3">
                            <label class="form-label" for="code">Mã xác thực</label>
                            <input class="form-control" id="code" name="code" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required>
                        </div>
                        <div class="mb-4">
                            <label class="form-label" for="newPassword">Mật khẩu mới</label>
                            <input class="form-control" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required>
                        </div>
                        <button class="btn btn-ute-accent w-100" type="submit">Đặt lại mật khẩu</button>
                    </form>
                    <p class="text-center small mb-0 mt-3"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
</body>
</html>
