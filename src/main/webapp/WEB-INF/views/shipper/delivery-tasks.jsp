<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="u" tagdir="/WEB-INF/tags" %>
<c:set var="pageTitle" value="Nhiệm vụ giao hàng | UTEExpress" scope="request" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />
<main class="container ute-page-medium">
    <div class="ute-task-bar">
        <div>
            <h1>Giao hàng hôm nay</h1>
            <p>Cập nhật trạng thái giao nhận và đối soát tiền thu hộ COD trực tiếp.</p>
        </div>
        <a class="btn ute-btn-ghost" href="${pageContext.request.contextPath}/shipper/return-tasks">Giao hàng hoàn</a>
    </div>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <c:choose>
        <c:when test="${empty tasks}">
            <div class="ute-empty"><h2 class="h4">Chưa có đơn nào để giao</h2><p class="text-muted mb-0">Khi bưu cục phân công, đơn sẽ hiện ở đây. Nếu bạn vừa nhận hàng tại bưu cục, hãy quét đơn ở mục "Bắt đầu giao" bên dưới.</p></div>
        </c:when>
        <c:otherwise>
            <h2 class="ute-staff-h3 ute-task-count">2. Danh sách đơn đang giao (<c:out value="${tasks.size()}"/> đơn)</h2>
            <ul class="ute-task-list">
                <c:forEach items="${tasks}" var="task">
                    <li class="ute-task">
                        <div class="ute-task-head">
                            <strong class="ute-task-code"><c:out value="${task.trackingNumber}"/></strong>
                            <u:statusbadge status="${task.currentStatus}"/>
                        </div>
                        <p class="ute-task-addr"><c:out value="${task.receiverAddress}"/></p>
                        <p class="ute-task-who"><c:out value="${task.receiverName}"/> · <a href="tel:${task.receiverPhone}"><c:out value="${task.receiverPhone}"/></a></p>

                        <c:choose>
                            <c:when test="${!task.codCollected || !task.feeCollected}">
                                <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/deliver" method="post" class="ute-task-form">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                    <div class="ute-task-money">
                                        <label>Thu hộ COD (đ)
                                            <input type="number" name="codAmountCollected" min="0" step="1" inputmode="numeric" required value="${task.codAmount}"/></label>
                                        <label>Thu cước từ người nhận (đ)
                                            <input type="number" name="shippingFeeCollected" min="0" step="1" inputmode="numeric" required value="${task.feePayer == 'RECEIVER' ? task.shippingFee : 0}"/></label>
                                        <label>Hình thức thu COD
                                            <select name="codPaymentMethod"><option value="CASH">Tiền mặt</option><option value="VNPAY">VNPAY</option><option value="TRANSFER">Chuyển khoản</option></select></label>
                                        <label>Hình thức thu cước
                                            <select name="shippingFeePaymentMethod"><option value="CASH">Tiền mặt</option><option value="VNPAY">VNPAY</option></select></label>
                                    </div>
                                    <button type="submit" class="btn btn-ute-accent">Thu tiền &amp; Xác nhận đã giao</button>
                                </form>
                            </c:when>
                            <c:when test="${task.eligibleForDelivery}">
                                <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/confirm" method="post" class="ute-task-form">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                    <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                    <p class="text-muted mb-2">Đã thu đủ tiền.</p>
                                    <button type="submit" class="btn btn-ute-accent">Xác nhận đã giao</button>
                                </form>
                            </c:when>
                        </c:choose>
                        <details class="ute-task-fail">
                            <summary>Không giao được</summary>
                            <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/failure" method="post" enctype="multipart/form-data" class="ute-task-form">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <input type="hidden" name="shipmentId" value="${task.shipmentId}"/>
                                <label>Lý do
                                    <select name="reason" required>
                                        <option value="NO_ANSWER">Không nghe máy</option>
                                        <option value="PAYMENT_NOT_AVAILABLE">Chưa có tiền mặt</option>
                                        <option value="RESCHEDULED">Khách hẹn ngày khác</option>
                                        <option value="OTHER">Lý do khác</option>
                                        <option value="RECEIVER_REFUSED">Khách từ chối nhận (báo thất bại ngay)</option>
                                        <option value="WRONG_ADDRESS">Sai địa chỉ (báo thất bại ngay)</option>
                                    </select></label>
                                <label>Ghi chú <input type="text" maxlength="255" name="note"></label>
                                <label>Ngày giao lại (nếu hẹn lại) <input type="date" name="rescheduledDate"></label>
                                <label>Ảnh bằng chứng <input type="file" name="evidence" accept="image/*"></label>
                                <button type="submit" class="btn ute-btn-ghost">Ghi nhận không giao được</button>
                            </form>
                        </details>
                    </li>
                </c:forEach>
            </ul>
        </c:otherwise>
    </c:choose>

    <section class="ute-scan" aria-labelledby="scan-title">
        <h2 id="scan-title" class="ute-staff-h3">1. Bắt đầu giao (Quét nhận đơn tại bưu cục)</h2>
        <form action="${pageContext.request.contextPath}/shipper/delivery-tasks/scan" method="post" class="ute-task-form">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <label>Bưu cục của bạn
                <select name="hubId" required>
                    <option value="">Chọn bưu cục</option>
                    <c:forEach items="${hubs}" var="h"><option value="${h.id}"><c:out value="${h.code}"/> – <c:out value="${h.name}"/></option></c:forEach>
                </select></label>
            <label>Mã đơn
                <input type="number" name="shipmentId" required inputmode="numeric" placeholder="Số thứ tự đơn hiển thị trên phiếu"></label>
            <button type="submit" class="btn btn-ink">Quét xuất kho</button>
        </form>
    </section>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
