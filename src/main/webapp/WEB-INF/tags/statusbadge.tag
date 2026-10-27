<%@ tag body-content="empty" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ attribute name="status" required="true" rtexprvalue="true" type="java.lang.Object" %>
<%-- A shipment status as a Vietnamese pill. The shape in front (see ute-modern.css) means colour is never the only signal. --%>
<c:choose>
    <c:when test="${status == 'CREATED'}"><c:set var="cls" value="created" /><c:set var="label" value="Mới tạo" /></c:when>
    <c:when test="${status == 'WAITING_FOR_PAYMENT'}"><c:set var="cls" value="waiting-payment" /><c:set var="label" value="Chờ thanh toán" /></c:when>
    <c:when test="${status == 'WAITING_FOR_HANDOVER'}"><c:set var="cls" value="waiting-handover" /><c:set var="label" value="Chờ bàn giao" /></c:when>
    <c:when test="${status == 'PICKED_UP'}"><c:set var="cls" value="picked-up" /><c:set var="label" value="Đã lấy hàng" /></c:when>
    <c:when test="${status == 'AT_HUB'}"><c:set var="cls" value="at-hub" /><c:set var="label" value="Tại bưu cục" /></c:when>
    <c:when test="${status == 'IN_TRANSIT'}"><c:set var="cls" value="in-transit" /><c:set var="label" value="Đang trung chuyển" /></c:when>
    <c:when test="${status == 'OUT_FOR_DELIVERY'}"><c:set var="cls" value="out-delivery" /><c:set var="label" value="Đang phát hàng" /></c:when>
    <c:when test="${status == 'FAILED'}"><c:set var="cls" value="failed" /><c:set var="label" value="Giao thất bại" /></c:when>
    <c:when test="${status == 'DELIVERED'}"><c:set var="cls" value="delivered" /><c:set var="label" value="Giao thành công" /></c:when>
    <c:when test="${status == 'RETURN_TO_SENDER'}"><c:set var="cls" value="return" /><c:set var="label" value="Đang chuyển hoàn" /></c:when>
    <c:when test="${status == 'RETURNED_TO_SENDER'}"><c:set var="cls" value="returned" /><c:set var="label" value="Đã hoàn trả" /></c:when>
    <c:when test="${status == 'CANCELLED'}"><c:set var="cls" value="cancelled" /><c:set var="label" value="Đã hủy" /></c:when>
    <c:when test="${status == 'CLOSED_EXCEPTION'}"><c:set var="cls" value="closed-exception" /><c:set var="label" value="Đóng ngoại lệ" /></c:when>
    <c:otherwise><c:set var="cls" value="created" /><c:set var="label" value="${status}" /></c:otherwise>
</c:choose>
<span class="badge badge-${cls}" data-status="${status}"><c:out value="${label}" /></span>
