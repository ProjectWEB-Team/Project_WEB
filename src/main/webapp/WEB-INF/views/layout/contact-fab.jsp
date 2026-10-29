<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%-- Floating contact button: one round button that opens Facebook, Zalo and Telegram. Styles live in ute-modern.css. --%>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
<div class="ute-fab" id="uteFab">
    <ul class="ute-fab-list" id="uteFabList">
        <li><a class="ute-fab-item" href="https://www.facebook.com/khangnemoinguoi" target="_blank" rel="noopener noreferrer" aria-label="Facebook">
            <span class="ute-fab-label">Facebook</span>
            <span class="ute-fab-icon" style="color:#1877F2"><i class="bi bi-facebook" aria-hidden="true"></i></span></a></li>
        <li><a class="ute-fab-item" href="https://zalo.me/84911195500" target="_blank" rel="noopener noreferrer" aria-label="Zalo">
            <span class="ute-fab-label">Zalo</span>
            <span class="ute-fab-icon"><svg viewBox="0 0 48 48" width="26" height="26" aria-hidden="true"><rect width="48" height="48" rx="11" fill="#0068FF"/><text x="24" y="31" text-anchor="middle" font-family="Arial, sans-serif" font-size="17" font-weight="700" fill="#fff">Zalo</text></svg></span></a></li>
        <li><a class="ute-fab-item" href="https://t.me/+84911195500" target="_blank" rel="noopener noreferrer" aria-label="Telegram">
            <span class="ute-fab-label">Telegram</span>
            <span class="ute-fab-icon" style="color:#26A5E4"><i class="bi bi-telegram" aria-hidden="true"></i></span></a></li>
    </ul>
    <button type="button" class="ute-fab-toggle" aria-expanded="false" aria-controls="uteFabList" aria-label="Liên hệ">
        <i class="bi bi-headset ute-fab-open" aria-hidden="true"></i><i class="bi bi-x-lg ute-fab-close" aria-hidden="true"></i>
    </button>
</div>
<script>
(function () {
  var fab = document.getElementById('uteFab'), btn = fab.querySelector('.ute-fab-toggle');
  function set(open) { fab.classList.toggle('is-open', open); btn.setAttribute('aria-expanded', open); }
  btn.addEventListener('click', function () { set(!fab.classList.contains('is-open')); });
  document.addEventListener('click', function (e) { if (!fab.contains(e.target)) set(false); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') set(false); });
})();
</script>
