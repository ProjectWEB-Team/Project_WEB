<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Hoàn tất tài khoản | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5">
    <div class="row justify-content-center">
        <div class="col-12 col-md-7 col-lg-5">

            <section class="card border-0 shadow-sm">
                <div class="card-body p-4 p-md-5">
                    <h1 class="h3 mb-2">Hoàn tất tài khoản</h1>
                    <p class="text-muted mb-2">Đã xác thực Google: <strong><c:out value="${googleEmail}"/></strong></p>
                    <p class="text-muted mb-4">Bổ sung thông tin liên hệ để bắt đầu gửi hàng. Số điện thoại cần được xác thực riêng trước khi liên kết đơn đang đến.</p>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <form:form method="post" action="${pageContext.request.contextPath}/auth/google/complete" modelAttribute="form">
                        <div class="mb-3">
                            <label class="form-label" for="fullName">Họ và tên</label>
                            <form:input path="fullName" id="fullName" type="text" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="name" maxlength="100" required="required"/>
                            <form:errors path="fullName" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <div class="mb-3">
                            <label class="form-label" for="phone">Số điện thoại liên hệ</label>
                            <form:input path="phone" id="phone" type="tel" cssClass="form-control" cssErrorClass="form-control is-invalid" htmlEscape="true" autocomplete="tel" maxlength="20" required="required"/>
                            <form:errors path="phone" cssClass="invalid-feedback d-block" htmlEscape="true"/>
                        </div>
                        <button class="btn btn-ute-accent w-100" type="submit">Hoàn tất và tiếp tục</button>
                    </form:form>
                    <p class="text-center small mb-0 mt-4"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></p>
                </div>
            </section>
        </div>
    </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
