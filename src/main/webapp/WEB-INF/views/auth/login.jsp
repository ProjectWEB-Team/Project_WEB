<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in | UTEExpress</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-light">
<main class="container py-5 ute-page-login">
    <section class="card shadow-sm">
        <div class="card-body p-4">
            <h1 class="h3 mb-4">Sign in</h1>

            <c:if test="${not empty param.error}">
                <div class="alert alert-danger" role="alert">Invalid username or password.</div>
            </c:if>
            <c:if test="${not empty param.logout}">
                <div class="alert alert-success" role="status">You have signed out successfully.</div>
            </c:if>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                <div class="mb-3">
                    <label class="form-label" for="username">Username</label>
                    <input class="form-control" id="username" name="username" type="text" autocomplete="username" required>
                </div>
                <div class="mb-4">
                    <label class="form-label" for="password">Password</label>
                    <input class="form-control" id="password" name="password" type="password" autocomplete="current-password" required>
                </div>
                <button class="btn btn-primary w-100" type="submit">Sign in</button>
            </form>
        </div>
    </section>
</main>
</body>
</html>
