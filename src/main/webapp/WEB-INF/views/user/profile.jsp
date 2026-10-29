<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="displayName" value="${not empty profileUser.fullName ? profileUser.fullName : profileUser.username}" />
<c:set var="pendingOtp" value="${not empty phoneChallengeId ? 'phoneOtp' : (not empty emailChallengeId ? 'emailOtp' : '')}" />

<main class="container ute-profile flex-grow-1">
  <h1>Hồ sơ cá nhân</h1>

  <c:if test="${empty pendingOtp && not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
  <c:if test="${empty pendingOtp && not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>
  <c:if test="${not empty passwordError}"><div class="alert alert-danger" role="alert"><c:out value="${passwordError}"/></div></c:if>

  <section class="pf-card pf-id" aria-label="Tài khoản">
    <form id="avatarForm" action="${ctx}/user/profile/avatar" method="post" enctype="multipart/form-data">
      <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
      <label class="pf-avatar" for="avatarInput" title="Đổi ảnh đại diện (JPG, PNG, WEBP, tối đa 2MB)">
        <c:choose>
          <c:when test="${not empty avatarUrl}"><img src="${ctx}${avatarUrl}" alt="Ảnh đại diện" width="80" height="80"></c:when>
          <c:otherwise><span aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(displayName, 0, 2))}"/></span></c:otherwise>
        </c:choose>
        <span class="pf-avatar-edit"><i class="bi bi-camera" aria-hidden="true"></i><span class="visually-hidden">Đổi ảnh đại diện</span></span>
      </label>
      <input id="avatarInput" class="visually-hidden" type="file" name="avatar" accept="image/jpeg,image/png,image/webp">
    </form>
    <div class="min-w-0">
      <h2 class="pf-name"><c:out value="${displayName}"/></h2>
      <p class="pf-sub">@<c:out value="${profileUser.username}"/> · Tham gia <c:out value="${fn:substring(profileUser.createdAt, 0, 10)}"/>
        <c:if test="${not empty profileUser.googleSubject}"> · <i class="bi bi-google" aria-hidden="true"></i> Google</c:if></p>
    </div>
  </section>

  <section class="pf-card" aria-label="Thông tin đăng nhập">
    <div class="pf-row">
      <span class="pf-label">Họ và tên</span>
      <span class="pf-value"><c:out value="${displayName}"/></span>
      <button type="button" class="btn btn-outline-secondary btn-sm" data-open="name">Sửa</button>
    </div>

    <div class="pf-row">
      <span class="pf-label">Số điện thoại</span>
      <span class="pf-value">
        <c:choose>
          <c:when test="${empty profileUser.phone}"><span class="muted">Chưa liên kết</span></c:when>
          <c:otherwise>
            <c:out value="${profileUser.phone}"/>
            <span class="pf-pill ${profileUser.phoneVerified ? 'pf-ok' : 'pf-warn'}">${profileUser.phoneVerified ? 'Đã xác thực' : 'Chưa xác thực'}</span>
          </c:otherwise>
        </c:choose>
      </span>
      <span class="d-flex gap-2">
        <c:if test="${not empty profileUser.phone && not profileUser.phoneVerified}">
          <form action="${ctx}/user/profile/phone/send-otp" method="post">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="phone" value="${profileUser.phone}" />
            <button type="submit" class="btn btn-primary btn-sm">Xác thực</button>
          </form>
        </c:if>
        <button type="button" class="btn btn-outline-secondary btn-sm" data-open="phone" data-secure>${empty profileUser.phone ? 'Thêm' : 'Đổi'}</button>
      </span>
    </div>

    <div class="pf-row">
      <span class="pf-label">Email</span>
      <span class="pf-value">
        <c:out value="${profileUser.email}"/>
        <span class="pf-pill ${profileUser.emailVerified ? 'pf-ok' : 'pf-warn'}">${profileUser.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'}</span>
      </span>
      <span class="d-flex gap-2">
        <c:if test="${not profileUser.emailVerified}">
          <form action="${ctx}/user/profile/email/send-otp" method="post">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <input type="hidden" name="email" value="${profileUser.email}" />
            <button type="submit" class="btn btn-primary btn-sm">Xác thực</button>
          </form>
        </c:if>
        <button type="button" class="btn btn-outline-secondary btn-sm" data-open="email" data-secure>Đổi</button>
      </span>
    </div>

    <div class="pf-row">
      <span class="pf-label">Mật khẩu</span>
      <span class="pf-value">${empty profileUser.googleSubject ? '••••••••' : '<span class="muted">Chưa đặt</span>'}</span>
      <button type="button" class="btn btn-outline-secondary btn-sm" data-open="password" data-secure>${empty profileUser.googleSubject ? 'Đổi' : 'Đặt'}</button>
    </div>
  </section>
</main>

<p id="pfFlash" hidden><c:out value="${errorMessage}"/></p>
<!-- One dialog for every step. It cannot be dismissed from outside: the user finishes or cancels. -->
<div class="modal fade" id="pfModal" tabindex="-1" data-bs-backdrop="static" data-bs-keyboard="false" aria-labelledby="pfTitle" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content pf-modal">
      <div class="modal-header"><h2 class="modal-title" id="pfTitle"></h2></div>
      <div class="modal-body">
        <p id="pfError" class="text-danger" role="alert" hidden></p>

        <div data-pane="pass" data-title="Nhập mật khẩu hiện tại" hidden>
          <label class="visually-hidden" for="pfPassword">Mật khẩu hiện tại</label>
          <input id="pfPassword" type="password" class="form-control" autocomplete="current-password">
          <div class="pf-foot"><button type="button" class="btn btn-cancel" data-cancel>Hủy</button><button type="button" class="btn btn-primary" id="pfPassGo">Tiếp tục</button></div>
        </div>

        <div data-pane="otp" data-title="Nhập mã xác minh" hidden>
          <p class="pf-to">Mã đã gửi tới <strong id="pfMail"></strong></p>
          <div class="pf-boxes" data-boxes="step" aria-label="Mã 6 số"></div>
          <button type="button" class="btn btn-primary w-100" id="pfOtpGo">Xác minh</button>
          <div class="pf-foot"><button type="button" class="btn btn-link" id="pfResend">Gửi lại mã</button><button type="button" class="btn btn-cancel" data-cancel>Hủy</button></div>
        </div>

        <form data-pane="name" data-title="Sửa họ và tên" action="${ctx}/user/profile/update-info" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <label class="visually-hidden" for="pfName">Họ và tên</label>
          <input id="pfName" type="text" name="fullName" class="form-control" value="${fn:escapeXml(profileUser.fullName)}" required minlength="2" maxlength="100">
          <div class="pf-foot"><button type="button" class="btn btn-cancel" data-cancel>Hủy</button><button type="submit" class="btn btn-accent">Lưu</button></div>
        </form>

        <form data-pane="phone" data-title="Nhập số điện thoại mới" action="${ctx}/user/profile/phone/send-otp" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <label class="visually-hidden" for="pfPhone">Số điện thoại mới</label>
          <input id="pfPhone" type="tel" name="phone" class="form-control" placeholder="09xx xxx xxx" required pattern="^(0|\+84)[35789][0-9]{8}$" inputmode="tel" autocomplete="tel">
          <div class="pf-foot"><button type="button" class="btn btn-cancel" data-cancel>Hủy</button><button type="submit" class="btn btn-accent">Gửi mã</button></div>
        </form>

        <form data-pane="email" data-title="Nhập email mới" action="${ctx}/user/profile/email/send-otp" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <label class="visually-hidden" for="pfEmail">Email mới</label>
          <input id="pfEmail" type="email" name="email" class="form-control" placeholder="ten@gmail.com" maxlength="100" required autocomplete="email">
          <div class="pf-foot"><button type="button" class="btn btn-cancel" data-cancel>Hủy</button><button type="submit" class="btn btn-accent">Gửi mã</button></div>
        </form>

        <form data-pane="password" data-title="Mật khẩu mới" action="${ctx}/user/profile/change-password" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <input type="hidden" name="currentPassword" id="pfCurrent">
          <label class="form-label" for="pfNew">Mật khẩu mới</label>
          <input id="pfNew" type="password" name="newPassword" class="form-control mb-3" minlength="8" maxlength="72" required autocomplete="new-password">
          <label class="form-label" for="pfConfirm">Nhập lại</label>
          <input id="pfConfirm" type="password" name="confirmPassword" class="form-control" minlength="8" maxlength="72" required autocomplete="new-password">
          <div class="pf-foot"><button type="button" class="btn btn-cancel" data-cancel>Hủy</button><button type="submit" class="btn btn-accent">Cập nhật</button></div>
        </form>

        <form data-pane="phoneOtp" data-title="Nhập mã OTP" action="${ctx}/user/profile/phone/verify-otp" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <input type="hidden" name="code" data-code>
          <p class="pf-to">Mã đã gửi tới <strong><c:out value="${phoneTarget}"/></strong></p>
          <div class="pf-boxes" data-boxes="submit" aria-label="Mã 6 số"></div>
          <button type="submit" class="btn btn-accent w-100">Xác nhận</button>
          <div class="pf-foot">
            <button type="submit" class="btn btn-link" formaction="${ctx}/user/profile/phone/resend-otp" formnovalidate data-resend>Gửi lại mã</button>
            <button type="button" class="btn btn-cancel" data-cancel>Hủy</button>
          </div>
        </form>

        <form data-pane="emailOtp" data-title="Nhập mã OTP" action="${ctx}/user/profile/email/verify-otp" method="post" hidden>
          <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
          <input type="hidden" name="code" data-code>
          <p class="pf-to">Mã đã gửi tới <strong><c:out value="${emailTarget}"/></strong></p>
          <div class="pf-boxes" data-boxes="submit" aria-label="Mã 6 số"></div>
          <button type="submit" class="btn btn-accent w-100">Xác nhận</button>
          <div class="pf-foot">
            <button type="submit" class="btn btn-link" formaction="${ctx}/user/profile/email/resend-otp" formnovalidate data-resend>Gửi lại mã</button>
            <button type="button" class="btn btn-cancel" data-cancel>Hủy</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<script>
// Bootstrap's script loads in the footer, after this block.
document.addEventListener('DOMContentLoaded', function () {
  var ctx = '${ctx}', csrf = {n: '${_csrf.parameterName}', v: '${_csrf.token}'};
  var needPass = ${stepUpPassword}, unlocked = ${stepUpUnlocked}, pending = '${pendingOtp}';
  var flashError = document.getElementById('pfFlash').textContent;
  var modalEl = document.getElementById('pfModal'), errEl = document.getElementById('pfError'), titleEl = document.getElementById('pfTitle');
  var modal = new bootstrap.Modal(modalEl);
  var action = null, password = '', current = '', stepBoxes;

  function fail(msg) { errEl.textContent = msg || ''; errEl.hidden = !msg; }
  function pane(name) {
    current = name; fail('');
    modalEl.querySelectorAll('[data-pane]').forEach(function (p) { p.hidden = p.dataset.pane !== name; });
    var p = modalEl.querySelector('[data-pane="' + name + '"]');
    titleEl.textContent = p.dataset.title;
    var first = p.querySelector('.pf-boxes input, input:not([type=hidden])');
    setTimeout(function () { if (first) first.focus(); }, 200);
  }
  function post(path, data) {
    var body = new URLSearchParams(data); body.set(csrf.n, csrf.v);
    return fetch(ctx + path, {method: 'POST', headers: {'Accept': 'application/json'}, body: body}).then(function (r) { return r.json(); });
  }

  // Six one-digit boxes that feed one value; typing, pasting and backspace move between them.
  function boxes(el, onValue, onFull) {
    var inputs = [];
    for (var i = 0; i < 6; i++) {
      var b = document.createElement('input');
      b.type = 'text'; b.inputMode = 'numeric'; b.maxLength = 1; b.autocomplete = i === 0 ? 'one-time-code' : 'off';
      b.setAttribute('aria-label', 'Chữ số ' + (i + 1));
      el.appendChild(b); inputs.push(b);
    }
    function value() { return inputs.map(function (x) { return x.value; }).join(''); }
    function sync() { var v = value(); onValue(v); if (v.length === 6 && onFull) onFull(v); }
    inputs.forEach(function (b, i) {
      b.addEventListener('input', function () {
        b.value = b.value.replace(/\D/g, '').slice(-1);
        if (b.value && inputs[i + 1]) inputs[i + 1].focus();
        sync();
      });
      b.addEventListener('keydown', function (e) {
        if (e.key === 'Backspace' && !b.value && inputs[i - 1]) { inputs[i - 1].focus(); inputs[i - 1].value = ''; sync(); }
      });
      b.addEventListener('paste', function (e) {
        var t = (e.clipboardData.getData('text') || '').replace(/\D/g, '').slice(0, 6);
        if (!t) return;
        e.preventDefault();
        inputs.forEach(function (x, j) { x.value = t[j] || ''; });
        inputs[Math.min(t.length, 5)].focus(); sync();
      });
    });
    return {value: value, clear: function () { inputs.forEach(function (x) { x.value = ''; }); inputs[0].focus(); }};
  }

  function cooldown(btn, seconds) {
    var label = btn.textContent, left = seconds; btn.disabled = true;
    var t = setInterval(function () {
      left--; btn.textContent = left > 0 ? label + ' (' + left + 's)' : label;
      if (left <= 0) { clearInterval(t); btn.disabled = false; }
    }, 1000);
    btn.textContent = label + ' (' + left + 's)';
  }

  // Step-up code (checked by AJAX)
  stepBoxes = boxes(modalEl.querySelector('[data-boxes="step"]'), function () {}, function () { document.getElementById('pfOtpGo').click(); });
  // Phone / email codes (plain form posts)
  modalEl.querySelectorAll('form[data-pane$="Otp"]').forEach(function (f) {
    var hidden = f.querySelector('[data-code]');
    boxes(f.querySelector('[data-boxes]'), function (v) { hidden.value = v; }, function () { f.requestSubmit(); });
    cooldown(f.querySelector('[data-resend]'), 60);
  });

  function ready() {
    unlocked = true;
    if (action === 'password') document.getElementById('pfCurrent').value = password;
    pane(action);
  }
  function begin(res) {
    if (!res.ok) { fail(res.message); return; }
    if (res.otpSent) { document.getElementById('pfMail').textContent = res.maskedEmail; pane('otp'); cooldown(document.getElementById('pfResend'), 60); }
    else { ready(); }
  }

  document.querySelectorAll('[data-open]').forEach(function (btn) {
    btn.addEventListener('click', function () {
      action = btn.dataset.open; password = '';
      modal.show();
      if (!btn.hasAttribute('data-secure')) { pane(action); return; }
      // Changing the password needs the typed password itself, so it always asks.
      if (unlocked && !(action === 'password' && needPass)) { ready(); }
      else if (needPass) { pane('pass'); }
      else { pane('otp'); post('/user/profile/step-up/start', {}).then(begin); }
    });
  });
  document.getElementById('pfPassGo').addEventListener('click', function () {
    password = document.getElementById('pfPassword').value;
    post('/user/profile/step-up/start', {password: password}).then(begin);
  });
  document.getElementById('pfPassword').addEventListener('keydown', function (e) { if (e.key === 'Enter') document.getElementById('pfPassGo').click(); });
  document.getElementById('pfOtpGo').addEventListener('click', function () {
    post('/user/profile/step-up/verify', {code: stepBoxes.value()}).then(function (res) {
      if (res.ok) ready(); else { fail(res.message); stepBoxes.clear(); }
    });
  });
  document.getElementById('pfResend').addEventListener('click', function () {
    var b = this; post('/user/profile/step-up/resend', {}).then(function (res) { if (res.ok) cooldown(b, 60); else fail(res.message); });
  });
  modalEl.querySelectorAll('[data-cancel]').forEach(function (b) {
    b.addEventListener('click', function () {
      if (current === 'phoneOtp' || current === 'emailOtp') {
        post('/user/profile/cancel', {}).finally(function () { modal.hide(); });
      } else { modal.hide(); }
    });
  });

  var avatarInput = document.getElementById('avatarInput');
  if (avatarInput) avatarInput.addEventListener('change', function () { if (avatarInput.files.length) document.getElementById('avatarForm').submit(); });

  // A code is waiting (after sending, resending or a wrong code): reopen straight on its step.
  if (pending) { modal.show(); pane(pending); fail(flashError); }
});
</script>

<jsp:include page="../layout/footer.jsp" />
