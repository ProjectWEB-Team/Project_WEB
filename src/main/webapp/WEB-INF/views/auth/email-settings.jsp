<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Email của tôi | UTEExpress</title>
</head>
<body class="bg-ute-canvas">
<jsp:include page="/WEB-INF/views/layout/navbar.jsp" />
<main class="container py-5"><div class="row justify-content-center"><div class="col-12 col-md-7 col-lg-5">
    <section class="card border-0 shadow-sm"><div class="card-body p-4 p-md-5">
        <h1 class="h3 mb-2">Email của tôi</h1>
        <p><c:out value="${maskedEmail}"/></p>
        <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
        <c:choose>
            <c:when test="${emailVerified}"><p class="alert alert-success" role="status">Email đã xác thực. Bạn có thể nhận thông báo và khôi phục mật khẩu qua email.</p></c:when>
            <c:otherwise>
                <p class="text-muted">Xác thực email để nhận thông báo đơn hàng và khôi phục mật khẩu khi cần.</p>
                <form method="post" action="${pageContext.request.contextPath}/user/email">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                    <button class="btn btn-ute-accent w-100" type="submit">Gửi mã xác thực email</button>
                </form>
            </c:otherwise>
        </c:choose>
    </div></section>
</div></div></main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
