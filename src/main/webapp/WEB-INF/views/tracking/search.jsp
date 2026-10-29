<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Tra cứu vận đơn | UTEExpress" scope="request" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />
<main class="ute-home ute-track flex-grow-1">
  <div class="ute-wrap">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <div class="ute-track-head">
        <h1 class="ute-track-title">Tra cứu vận đơn</h1>
        <p>Nhập mã vận đơn để xem hành trình đơn hàng.</p>
    </div>
    <div class="ute-track-card">
        <form class="ute-search ute-search-flat" method="get" action="${pageContext.request.contextPath}/tracking" role="search">
            <label class="visually-hidden" for="code">Mã vận đơn</label>
            <input id="code" name="code" value="<c:out value='${code}'/>" required maxlength="50" placeholder="UTX-20261027-0001" autocomplete="off" aria-describedby="code-help"<c:if test="${notFound}"> aria-invalid="true"</c:if>>
            <button class="btn btn-ute-accent" type="submit">Tra cứu</button>
        </form>
        <p id="code-help" class="ute-track-help">Nhập mã trên biên nhận hoặc tin nhắn xác nhận. Không cần đăng nhập.</p>
    </div>

    <c:if test="${notFound}">
        <section class="ute-track-result" aria-live="polite">
            <div>
                <h2 class="ute-track-error">Không tìm thấy mã <c:out value="${code}"/></h2>
                <p class="mb-0">Kiểm tra lại mã. Mã có dạng UTX-20261027-0001.</p>
            </div>
        </section>
    </c:if>

    <c:if test="${not empty tracking}">
        <section class="ute-track-result" aria-live="polite">
            <div class="ute-track-grid">
                <div class="ute-track-summary">
                    <span class="ute-track-tag">Kết quả tra cứu vận đơn</span>
                    <p class="ute-track-code"><c:out value="${tracking.trackingNumber}"/></p>
                    <p class="ute-track-status"><u:statusbadge status="${tracking.currentStatus}"/></p>
                    <div class="ute-track-info">
                        <strong>Bưu cục hiện tại: <c:out value="${tracking.currentHubName}" default="Chưa đến bưu cục"/></strong>
                        <span>Thời gian giao dự kiến: chưa có ước tính.</span>
                    </div>
                    <p class="ute-track-note">Trang công khai ẩn tên và số điện thoại người gửi, người nhận.</p>
                </div>
                <div>
                    <h2 class="ute-track-events-title">Chi tiết hành trình vận đơn (${fn:length(tracking.events)} mốc cập nhật)</h2>
                    <ol class="ute-track-events">
                        <c:forEach items="${tracking.events}" var="event" varStatus="st">
                            <li<c:if test="${st.last}"> class="is-now"</c:if>>
                                <span class="ute-track-ev-tag"><c:out value="${st.last ? 'Hiện tại' : 'Đã qua'}"/></span>
                                <strong><c:out value="${event.label}"/></strong>
                                <time><c:set var="at" value="${event.occurredAt}"/>${fn:substring(at, 8, 10)}/${fn:substring(at, 5, 7)}/${fn:substring(at, 0, 4)} · ${fn:substring(at, 11, 16)}</time>
                            </li>
                        </c:forEach>
                    </ol>
                </div>
            </div>
        </section>
    </c:if>
  </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
