<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${emailPasswordReset ? 'Đặt lại mật khẩu' : 'Xác thực email'} | UTEExpress</title>
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-modern.css" rel="stylesheet">
</head>
<body class="ute-auth">
<a class="ute-auth-home" href="${pageContext.request.contextPath}/"><svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true"><path fill="currentColor" fill-rule="evenodd" d="M15 8a.5.5 0 0 0-.5-.5H2.707l3.147-3.146a.5.5 0 1 0-.708-.708l-4 4a.5.5 0 0 0 0 .708l4 4a.5.5 0 0 0 .708-.708L2.707 8.5H14.5A.5.5 0 0 0 15 8"/></svg>Trang chủ</a>
<main class="container ute-auth-main">
    <div class="ute-auth-layout">
        <section class="ute-auth-brand" aria-label="UTEexpress"><jsp:include page="/WEB-INF/views/layout/logo.jsp" /><p>Giao nhanh. Trọn niềm tin.</p></section>
        <div class="ute-auth-form">
            <section class="card ute-auth-card ute-auth-card-sm">
                <div class="card-body p-4 p-md-5">
                    <form id="verifyEmailForm" method="post" action="${pageContext.request.contextPath}/auth/email/verify">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" id="fullOtpCode" name="code" value="">
                        <div id="step-otp-section">
                            <h1 class="h3 mb-2">${emailPasswordReset ? 'Xác minh mã OTP' : 'Nhập mã xác thực'}</h1>
                            <p class="text-muted">Mã 6 chữ số đã gửi tới <strong><c:out value="${maskedEmail}"/></strong>. Kiểm tra cả thư rác.</p>
                            <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                            <c:if test="${not empty notice}"><div class="alert alert-info" role="status"><c:out value="${notice}"/></div></c:if>
                            <div id="otpErrorAlert" class="alert alert-danger" role="alert" hidden></div>
                            <div class="ute-otp-inputs" role="group" aria-label="Nhập 6 chữ số mã OTP">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" autocomplete="one-time-code" aria-label="Chữ số 1" autofocus>
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" aria-label="Chữ số 2">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" aria-label="Chữ số 3">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" aria-label="Chữ số 4">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" aria-label="Chữ số 5">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" aria-label="Chữ số 6">
                            </div>
                            <noscript><label class="form-label" for="plainCode">Mã xác thực</label><input class="form-control" id="plainCode" name="code" inputmode="numeric" pattern="[0-9]{6}" maxlength="6"></noscript>
                            <button class="btn btn-ute-accent w-100" type="submit" id="otpSubmit">${emailPasswordReset ? 'Xác thực mã OTP & Tiếp tục →' : 'Xác thực email'}</button>
                        </div>
                        <c:if test="${emailPasswordReset}">
                            <div id="step-password-section">
                                <h1 class="h3 mb-2">Đặt lại mật khẩu mới</h1>
                                <div class="ute-verified-banner"><i class="bi bi-check-circle-fill"></i><span>Mã xác thực hợp lệ. Hãy nhập mật khẩu mới.</span></div>
                                <div><label class="form-label" for="newPassword">Mật khẩu mới</label><input class="form-control" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Nhập mật khẩu mới"><div class="form-text">• Tối thiểu 8 ký tự</div></div>
                                <div><label class="form-label" for="confirmPassword">Nhập lại mật khẩu mới</label><input class="form-control" id="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Nhập lại mật khẩu mới"><div id="passwordMismatchError" class="text-danger small" hidden>Mật khẩu xác nhận không khớp.</div></div>
                                <button class="btn btn-ute-accent w-100" type="submit" id="resetSubmit">Lưu mật khẩu mới &amp; Đăng nhập</button>
                            </div>
                        </c:if>
                    </form>
                    <form method="post" action="${pageContext.request.contextPath}/auth/email/resend" class="text-center">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="btn btn-outline-secondary w-100" type="submit">Gửi lại mã</button>
                    </form>
                    <p class="text-center small mb-0 ute-auth-back"><a href="${pageContext.request.contextPath}/login">← Quay lại đăng nhập</a></p>
                    <script>
                        (function () {
                            var form = document.getElementById('verifyEmailForm'), boxes = form.querySelectorAll('.ute-otp-box'),
                                hidden = document.getElementById('fullOtpCode'), alertBox = document.getElementById('otpErrorAlert'),
                                otpSubmit = document.getElementById('otpSubmit'), stepPassword = document.getElementById('step-password-section'),
                                reset = !!stepPassword, accepted = false;
                            function collect() { var s = ''; boxes.forEach(function (b) { s += b.value; }); hidden.value = s; return s; }
                            function say(text) { alertBox.textContent = text; alertBox.hidden = !text; }
                            boxes.forEach(function (box, i) {
                                box.addEventListener('input', function () { box.value = box.value.replace(/[^0-9]/g, ''); if (box.value && i < boxes.length - 1) boxes[i + 1].focus(); collect(); });
                                box.addEventListener('keydown', function (e) { if (e.key === 'Backspace' && !box.value && i > 0) boxes[i - 1].focus(); });
                                box.addEventListener('paste', function (e) {
                                    e.preventDefault();
                                    var text = (e.clipboardData || window.clipboardData).getData('text').trim();
                                    if (/^\d{6}$/.test(text)) { text.split('').forEach(function (ch, k) { boxes[k].value = ch; }); collect(); boxes[5].focus(); }
                                });
                            });
                            if (reset) { stepPassword.hidden = true; } // without script both steps stay visible and the single form still works
                            form.addEventListener('submit', function (event) {
                                if (accepted) {
                                    var pw = document.getElementById('newPassword'), again = document.getElementById('confirmPassword');
                                    if (pw.value.length < 8 || pw.value !== again.value) { event.preventDefault(); document.getElementById('passwordMismatchError').hidden = pw.value === again.value; (pw.value.length < 8 ? pw : again).focus(); }
                                    return;
                                }
                                if (collect().length < 6) { event.preventDefault(); say('Vui lòng nhập đủ 6 chữ số mã OTP.'); boxes[0].focus(); return; }
                                if (!reset) return;
                                event.preventDefault(); say(''); otpSubmit.disabled = true;
                                fetch('${pageContext.request.contextPath}/auth/email/check-code', {
                                    method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                    body: new URLSearchParams({ code: hidden.value, '${_csrf.parameterName}': '${_csrf.token}' })
                                }).then(function (r) { return r.json(); }).then(function (result) {
                                    otpSubmit.disabled = false;
                                    if (!result.valid) { say(result.message); boxes.forEach(function (b) { b.classList.add('is-invalid'); }); return; }
                                    accepted = true; boxes.forEach(function (b) { b.readOnly = true; });
                                    document.getElementById('step-otp-section').hidden = true; stepPassword.hidden = false;
                                    document.getElementById('newPassword').required = document.getElementById('confirmPassword').required = true;
                                    document.getElementById('newPassword').focus();
                                }).catch(function () { otpSubmit.disabled = false; say('Không kiểm tra được mã. Vui lòng thử lại.'); });
                            });
                        })();
                    </script>
                </div>
            </section>
        </div>
    </div>
</main>
</body>
</html>
