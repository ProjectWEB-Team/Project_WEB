<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác thực email | UTEExpress</title>
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5"><div class="row justify-content-center"><div class="col-12 col-md-7 col-lg-5">
    <section class="card border-0 shadow-sm"><div class="card-body p-4 p-md-5">
        <h1 class="h3 mb-2">${emailPasswordReset ? 'Đặt lại mật khẩu' : 'Xác thực email'}</h1>
        <p class="text-muted">Nếu yêu cầu hợp lệ, mã gồm 6 chữ số đã được gửi tới <strong><c:out value="${maskedEmail}"/></strong>. Kiểm tra cả thư rác và không chia sẻ mã.</p>
        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
        <c:if test="${not empty notice}"><div class="alert alert-info" role="status"><c:out value="${notice}"/></div></c:if>
        <form method="post" action="${pageContext.request.contextPath}/auth/email/verify">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <label for="code" class="form-label">Mã xác thực</label>
            <input class="form-control mb-3" id="code" name="code" type="text" inputmode="numeric" autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6" required>
            <c:if test="${emailPasswordReset}">
                <label for="newPassword" class="form-label">Mật khẩu mới</label>
                <input class="form-control mb-3" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required>
            </c:if>
            <button class="btn btn-ute-accent w-100" type="submit">${emailPasswordReset ? 'Đặt lại mật khẩu' : 'Xác thực email'}</button>
        </form>
        <form method="post" action="${pageContext.request.contextPath}/auth/email/resend" class="text-center mt-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <button class="btn btn-link" type="submit">Gửi lại mã</button>
        </form>
        <p class="small text-center mt-3 mb-0"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
    </div></section>
</div></div></main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
