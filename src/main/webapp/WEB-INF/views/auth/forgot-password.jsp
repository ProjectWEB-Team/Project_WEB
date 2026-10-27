<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quên mật khẩu | UTEExpress</title>
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
                    <h1 class="h3 mb-2">Quên mật khẩu</h1>
                    <p class="text-muted mb-4">Nhập số điện thoại đã xác thực của tài khoản. Nếu số này có tài khoản, mã đặt lại mật khẩu sẽ được gửi tới.</p>
                    <c:if test="${not empty otpClosedNotice}"><div class="alert alert-warning" role="status"><c:out value="${otpClosedNotice}"/></div></c:if>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <form:form method="post" action="${pageContext.request.contextPath}/auth/forgot-password" modelAttribute="phoneLoginRequest">
                        <div class="mb-3">
                            <label class="form-label" for="phone">Số điện thoại</label>
                            <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="tel" maxlength="20" required="required"/>
                            <form:errors path="phone" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <button class="btn btn-ute-accent w-100" type="submit">Nhận mã đặt lại mật khẩu</button>
                    </form:form>
                    <p class="text-center small mb-0 mt-4"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
</body>
</html>
