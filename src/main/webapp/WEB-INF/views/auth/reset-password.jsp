<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đặt lại mật khẩu | UTEExpress</title>
    <link href="https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-express.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/assets/css/ute-modern.css" rel="stylesheet">
    <style>
        .ute-otp-inputs { display: flex; gap: 8px; justify-content: center; margin: 16px 0; }
        .ute-otp-box { width: 48px; height: 56px; font-size: 24px; font-weight: 700; text-align: center; border: 1.5px solid var(--line-strong); border-radius: 10px; background: var(--surface); color: var(--ink); }
        .ute-otp-box:focus { border-color: var(--accent); outline: none; box-shadow: 0 0 0 3px var(--accent-tint); }
        .ute-otp-box.is-invalid { border-color: #DC2626; background: #FEF2F2; }
        .ute-verified-banner { background: #ECFDF5; border: 1px solid #A7F3D0; border-radius: 12px; padding: 12px 16px; color: #065F46; font-size: 14px; font-weight: 500; display: flex; align-items: center; gap: 8px; margin-bottom: 20px; }
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
                    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
                    <c:if test="${not empty notice}"><div class="alert alert-success" role="status"><c:out value="${notice}"/></div></c:if>

                    <form id="resetPasswordForm" method="post" action="${pageContext.request.contextPath}/auth/reset-password">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <input type="hidden" id="fullOtpCode" name="code" value="">

                        <!-- BƯỚC 2: XÁC MINH MÃ OTP -->
                        <div id="step-otp-section">
                            <h1 class="h3 mb-2">Xác minh mã OTP</h1>
                            <p class="text-muted mb-3">Nhập mã 6 chữ số đã gửi tới số điện thoại <strong><c:out value="${maskedPhone}"/></strong>.</p>
                            
                            <div id="otpErrorAlert" class="alert alert-danger py-2 small mb-3" style="display: none;" role="alert"></div>
                            <div class="ute-otp-inputs" role="group" aria-label="Nhập 6 chữ số mã OTP">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]" autocomplete="one-time-code" autofocus>
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                                <input type="text" class="ute-otp-box" maxlength="1" inputmode="numeric" pattern="[0-9]">
                            </div>
                            
                            <div class="d-flex justify-content-between align-items-center mb-4 small text-muted">
                                <span>Thời gian còn lại: <strong id="resendTimer">45s</strong></span>
                                <a href="javascript:void(0)" id="resendOtpBtn" class="text-muted text-decoration-none">Gửi lại mã</a>
                            </div>

                            <button id="btnVerifyOtpNext" class="btn btn-ute-accent w-100" type="button">Xác thực mã OTP & Tiếp tục →</button>
                            <p class="text-center small mb-0 mt-4"><a href="${pageContext.request.contextPath}/auth/forgot-password">← Nhập lại số điện thoại</a></p>
                        </div>

                        <!-- BƯỚC 3: ĐẶT MẬT KHẨU MỚI (chỉ hiện khi OTP đã được nhập/xác nhận) -->
                        <div id="step-password-section" style="display: none;">
                            <h1 class="h3 mb-2">Đặt lại mật khẩu mới</h1>
                            <div class="ute-verified-banner">
                                <i class="bi bi-check-circle-fill text-success fs-5"></i>
                                <span>Số điện thoại <c:out value="${maskedPhone}"/> đã được xác thực OTP thành công.</span>
                            </div>
                            <p class="text-muted">Vui lòng thiết lập mật khẩu mới có độ bảo mật cao cho tài khoản của bạn.</p>

                            <div class="mb-3">
                                <label class="form-label" for="newPassword">Mật khẩu mới</label>
                                <input class="form-control" id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Nhập mật khẩu mới">
                                <div class="form-text">• Tối thiểu 8 ký tự  • Bao gồm ít nhất một chữ cái và một chữ số</div>
                            </div>

                            <div class="mb-4">
                                <label class="form-label" for="confirmPassword">Nhập lại mật khẩu mới</label>
                                <input class="form-control" id="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" placeholder="Nhập lại mật khẩu mới">
                                <div id="passwordMismatchError" class="text-danger small mt-1" style="display: none;">Mật khẩu xác nhận không khớp.</div>
                            </div>

                            <button class="btn btn-ute-accent w-100" type="submit">Lưu mật khẩu mới & Đăng nhập</button>
                            <p class="text-center small mb-0 mt-3"><a href="javascript:void(0)" id="backToOtpBtn">← Quay lại kiểm tra mã OTP</a></p>
                        </div>
                    </form>

                    <script>
                        document.addEventListener("DOMContentLoaded", function() {
                            const boxes = document.querySelectorAll(".ute-otp-box");
                            const hiddenInput = document.getElementById("fullOtpCode");
                            const btnNext = document.getElementById("btnVerifyOtpNext");
                            const secOtp = document.getElementById("step-otp-section");
                            const secPass = document.getElementById("step-password-section");
                            const backToOtp = document.getElementById("backToOtpBtn");
                            const form = document.getElementById("resetPasswordForm");

                            // OTP Box auto-advance
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

                            // 45s countdown timer
                            let timeLeft = 45;
                            const timerEl = document.getElementById("resendTimer");
                            const resendBtn = document.getElementById("resendOtpBtn");
                            const timerInterval = setInterval(() => {
                                timeLeft--;
                                if (timeLeft > 0) {
                                    timerEl.textContent = timeLeft + "s";
                                } else {
                                    clearInterval(timerInterval);
                                    timerEl.textContent = "Hết hạn";
                                    resendBtn.classList.remove("text-muted");
                                    resendBtn.classList.add("text-primary", "fw-bold");
                                }
                            }, 1000);

                            const errorAlert = document.getElementById("otpErrorAlert");

                            btnNext.addEventListener("click", async () => {
                                const code = collectOtp();
                                if (code.length < 6) {
                                    boxes.forEach(b => { if (!b.value) b.classList.add("is-invalid"); });
                                    errorAlert.textContent = "Vui lòng nhập đủ 6 chữ số mã OTP.";
                                    errorAlert.style.display = "block";
                                    return;
                                }

                                errorAlert.style.display = "none";
                                btnNext.disabled = true;
                                const originalText = btnNext.textContent;
                                btnNext.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xác thực OTP...';

                                try {
                                    const formData = new URLSearchParams();
                                    formData.append("code", code);
                                    formData.append("${_csrf.parameterName}", "${_csrf.token}");

                                    const resp = await fetch("${pageContext.request.contextPath}/auth/check-reset-otp", {
                                        method: "POST",
                                        headers: { "Content-Type": "application/x-www-form-urlencoded" },
                                        body: formData.toString()
                                    });

                                    const result = await resp.json();
                                    if (result.valid) {
                                        boxes.forEach(b => {
                                            b.classList.remove("is-invalid");
                                            b.style.borderColor = "#10B981";
                                            b.style.backgroundColor = "#ECFDF5";
                                        });
                                        setTimeout(() => {
                                            secOtp.style.display = "none";
                                            secPass.style.display = "block";
                                            document.getElementById("newPassword").focus();
                                        }, 250);
                                    } else {
                                        boxes.forEach(b => {
                                            b.classList.add("is-invalid");
                                            b.style.borderColor = "#DC2626";
                                            b.style.backgroundColor = "#FEF2F2";
                                        });
                                        errorAlert.textContent = result.message || "Mã OTP không chính xác hoặc đã hết hạn. Vui lòng kiểm tra lại.";
                                        errorAlert.style.display = "block";
                                    }
                                } catch (err) {
                                    errorAlert.textContent = "Không thể kết nối đến máy chủ để xác thực OTP. Vui lòng thử lại.";
                                    errorAlert.style.display = "block";
                                } finally {
                                    btnNext.disabled = false;
                                    btnNext.textContent = originalText;
                                }
                            });

                            backToOtp.addEventListener("click", () => {
                                secPass.style.display = "none";
                                secOtp.style.display = "block";
                            });

                            form.addEventListener("submit", (e) => {
                                const p1 = document.getElementById("newPassword").value;
                                const p2 = document.getElementById("confirmPassword").value;
                                const err = document.getElementById("passwordMismatchError");
                                if (p1 !== p2) {
                                    e.preventDefault();
                                    err.style.display = "block";
                                } else {
                                    err.style.display = "none";
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
