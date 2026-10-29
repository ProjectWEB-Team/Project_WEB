<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập bằng điện thoại | UTEExpress</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/favicon.png">
    <link rel="apple-touch-icon" href="${pageContext.request.contextPath}/assets/images/uteexpress-square-logo.png">
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-modern.css" rel="stylesheet">
</head>
<body class="ute-auth">
<main class="container ute-auth-main">
    <div class="ute-auth-layout">
        <section class="ute-auth-brand" aria-label="UTEexpress">
            <jsp:include page="/WEB-INF/views/layout/logo.jsp" />
            <p>Giao nhanh. Trọn niềm tin.</p>
        </section>
        <div class="ute-auth-form">
            <section class="card ute-auth-card">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h3 mb-2">Đăng nhập bằng OTP</h1>
                    <p class="text-muted mb-4">Nhập số điện thoại đã đăng ký. Mã xác thực 6 chữ số sẽ được gửi về số này.</p>
                    <c:if test="${not empty otpClosedNotice}"><div class="alert alert-warning" role="status"><c:out value="${otpClosedNotice}"/></div></c:if>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <form:form method="post" action="${pageContext.request.contextPath}/auth/phone-login" modelAttribute="phoneLoginRequest">
                        <div class="mb-3">
                            <label class="form-label" for="phone">Số điện thoại</label>
                            <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="tel" maxlength="20" required="required"/>
                            <form:errors path="phone" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <button class="btn btn-ute-accent w-100" type="submit">Gửi mã xác thực</button>
                    </form:form>
                    <p class="text-center small mb-0 ute-auth-back"><a href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập bằng mật khẩu</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
</body>
</html>
