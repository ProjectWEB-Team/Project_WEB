<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Tra cứu vận đơn | UTEExpress" scope="request" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />
<main class="container ute-track">
    <c:if test="${not empty message}"><div class="alert alert-success" role="status"><c:out value="${message}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>
    <h1 class="ute-track-title">Tra cứu vận đơn</h1>
    <form class="ute-search ute-search-flat" method="get" action="${pageContext.request.contextPath}/tracking" role="search">
        <label class="visually-hidden" for="code">Mã vận đơn</label>
        <input id="code" name="code" value="<c:out value='${code}'/>" required maxlength="50" placeholder="UTX-20261027-0001" autocomplete="off" aria-describedby="code-help"<c:if test="${notFound}"> aria-invalid="true"</c:if>>
        <button class="btn btn-ute-accent" type="submit">Tra cứu</button>
    </form>
    <p id="code-help" class="ute-track-help">Nhập mã trên biên nhận hoặc tin nhắn xác nhận. Không cần đăng nhập.</p>

    <c:if test="${notFound}">
        <section class="ute-track-result" aria-live="polite">
            <h2 class="ute-track-error">Không tìm thấy mã <c:out value="${code}"/></h2>
            <p>Kiểm tra lại từng ký tự trong mã. Mã có dạng UTX, ngày tạo và số thứ tự, ví dụ UTX-20261027-0001. Nếu vẫn không thấy, đơn có thể chưa được tạo thành công.</p>
        </section>
    </c:if>

    <c:if test="${not empty tracking}">
        <section class="ute-track-result" aria-live="polite">
            <div class="ute-track-grid">
                <div>
                    <p class="ute-track-code"><c:out value="${tracking.trackingNumber}"/></p>
                    <p class="ute-track-status"><u:statusbadge status="${tracking.currentStatus}"/>
                        <span><c:out value="${tracking.currentHubName}" default="Chưa đến bưu cục"/></span></p>
                    <p class="text-muted">Thời gian giao dự kiến: chưa có ước tính.</p>
                    <p class="text-muted small">Trang công khai chỉ hiện thông tin vận chuyển, không hiện tên hay số điện thoại.</p>
                </div>
                <ol class="ute-timeline">
                    <c:forEach items="${tracking.events}" var="event" varStatus="st">
                        <li<c:if test="${st.last}"> class="is-now"</c:if>>
                            <strong><c:out value="${event.label}"/></strong>
                            <span><c:out value="${event.occurredAt}"/></span>
                        </li>
                    </c:forEach>
                </ol>
            </div>
        </section>
    </c:if>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
