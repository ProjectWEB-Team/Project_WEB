<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Xác thực số điện thoại | UTEExpress</title>
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-modern.css" rel="stylesheet">
    <style>
        .ute-otp-inputs { display: flex; gap: 8px; justify-content: center; margin: 16px 0; }
        .ute-otp-box { width: 48px; height: 56px; font-size: 24px; font-weight: 700; text-align: center; border: 1.5px solid var(--line-strong); border-radius: 10px; background: var(--surface); color: var(--ink); }
        .ute-otp-box:focus { border-color: var(--accent); outline: none; box-shadow: 0 0 0 3px var(--accent-tint); }
        .ute-otp-box.is-invalid { border-color: #DC2626; background: #FEF2F2; }
    </style>
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
                    <h1 class="h3 mb-2">Nhập mã xác thực</h1>
                    <p class="text-muted mb-3">Mã 6 chữ số đã được gửi tới <strong><c:out value="${maskedPhone}"/></strong>. Mã có hiệu lực trong 5 phút.</p>
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <c:if test="${not empty notice}"><div class="alert alert-success" role="status"><c:out value="${notice}"/></div></c:if>

                    <form:form id="verifyOtpForm" method="post" action="${pageContext.request.contextPath}/auth/verify-phone" modelAttribute="otpForm">
                        <input type="hidden" id="fullOtpCode" name="code" value="">
                        <form:errors path="code" cssClass="alert alert-danger" htmlEscape="true"/>

                        <div class="ute-otp-inputs" role="group" aria-label="Nhập 6 chữ số mã OTP">
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" autocomplete="one-time-code" autofocus>
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                            <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                        </div>

                        <button class="btn btn-ute-accent w-100" type="submit">
                            <c:choose><c:when test="${phoneLogin}">Xác nhận và đăng nhập</c:when><c:otherwise>Kích hoạt tài khoản</c:otherwise></c:choose>
                        </button>
                    </form:form>
                    <form method="post" action="${pageContext.request.contextPath}/auth/verify-phone/resend">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="btn btn-outline-secondary w-100" type="submit" id="resendBtn">Gửi lại mã</button>
                    </form>
                    <p class="text-center small mb-0 ute-auth-back"><a href="${pageContext.request.contextPath}/login">← Đổi số điện thoại khác</a></p>

                    <script>
                        document.addEventListener("DOMContentLoaded", function() {
                            const boxes = document.querySelectorAll(".ute-otp-box");
                            const hiddenInput = document.getElementById("fullOtpCode");
                            const form = document.getElementById("verifyOtpForm");

                            boxes.forEach((box, idx) => {
                                box.addEventListener("input", (e) => {
                                    box.value = box.value.replace(/[^0-9]/g, "");
                                    if (box.value && idx < boxes.length - 1) {
                                        boxes[idx + 1].focus();
                                    }
                                    collectOtp();
                                });
                                box.addEventListener("keydown", (e) => {
                                    if (e.key === "Backspace" && !box.value && idx > 0) {
                                        boxes[idx - 1].focus();
                                    }
                                });
                                box.addEventListener("paste", (e) => {
                                    e.preventDefault();
                                    const paste = (e.clipboardData || window.clipboardData).getData("text").trim();
                                    if (/^\d{6}$/.test(paste)) {
                                        paste.split("").forEach((ch, i) => { if (boxes[i]) boxes[i].value = ch; });
                                        collectOtp();
                                        boxes[5].focus();
                                    }
                                });
                            });

                            function collectOtp() {
                                let code = "";
                                boxes.forEach(b => code += b.value);
                                hiddenInput.value = code;
                                return code;
                            }

                            form.addEventListener("submit", (e) => {
                                const code = collectOtp();
                                if (code.length < 6) {
                                    e.preventDefault();
                                    boxes.forEach(b => { if (!b.value) b.classList.add("is-invalid"); });
                                    alert("Vui lòng nhập đủ 6 chữ số mã OTP.");
                                }
                            });
                        });
                    </script>
                </div>
            </section>
        </div>
    </div>
</main>
</body>
</html>
