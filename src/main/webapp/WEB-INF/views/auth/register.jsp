<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký | UTEExpress</title>
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
                    <h1 class="h3 mb-2">Tạo tài khoản</h1>
                    <p class="text-muted mb-4">${emailRegistration ? 'Xác thực email để bắt đầu gửi hàng. Số điện thoại được dùng để liên hệ và cần xác thực riêng.' : 'Xác thực số điện thoại để bắt đầu gửi hàng và theo dõi đơn của bạn.'}</p>
                    <c:if test="${not emailRegistration and not empty otpClosedNotice}"><div class="alert alert-warning" role="status"><c:out value="${otpClosedNotice}"/></div></c:if>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <form:form method="post" action="${pageContext.request.contextPath}${emailRegistration ? '/auth/email/register' : '/register'}" modelAttribute="registerUserRequest">
                        <div class="mb-3">
                            <label class="form-label" for="fullName">Họ và tên</label>
                            <form:input path="fullName" id="fullName" type="text" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="name" maxlength="100" required="required"/>
                            <form:errors path="fullName" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="phone">Số điện thoại</label>
                            <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="tel" maxlength="20" required="required" aria-describedby="phoneHelp"/>
                            <div id="phoneHelp" class="form-text">Số di động 10 chữ số, đầu 03, 05, 07, 08, 09; hoặc dạng +84 tương ứng.</div>
                            <form:errors path="phone" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="email">Email liên hệ</label>
                            <form:input path="email" id="email" type="email" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="email" maxlength="100" required="required"/>
                            <form:errors path="email" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="username">Tên đăng nhập</label>
                            <form:input path="username" id="username" type="text" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="username" maxlength="50" required="required"/>
                            <form:errors path="username" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <div class="mb-4">
                            <label class="form-label" for="password">Mật khẩu</label>
                            <form:password path="password" id="password" showPassword="false" cssClass="form-control" cssErrorClass="form-control is-invalid" autocomplete="new-password" minlength="8" maxlength="72" required="required"/>
                            <form:errors path="password" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <button class="btn btn-ute-accent w-100" type="submit">Đăng ký và nhận mã xác thực</button>
                    </form:form>
                    <c:if test="${emailLoginEnabled}">
                        <p class="text-center mt-3"><a href="${pageContext.request.contextPath}${emailRegistration ? '/register' : '/auth/email/register'}">${emailRegistration ? 'Đăng ký bằng OTP điện thoại' : 'Đăng ký bằng mã gửi qua email'}</a></p>
                    </c:if>
                    <c:if test="${googleLoginEnabled}">
                        <p class="text-muted text-center my-3">Hoặc</p>
                        <a class="btn btn-outline-secondary w-100" href="${pageContext.request.contextPath}/oauth2/authorization/google">Tiếp tục với Google</a>
                    </c:if>
                    <p class="text-center small mb-0 mt-4">Đã có tài khoản? <a href="${pageContext.request.contextPath}/login">Đăng nhập</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
