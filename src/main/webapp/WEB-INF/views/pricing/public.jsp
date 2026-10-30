<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Bảng giá cước | UTEExpress" scope="request" />
<jsp:include page="/WEB-INF/views/layout/header.jsp" />
<main class="ute-home ute-pricing flex-grow-1">
    <div class="ute-page-hero">
        <div class="ute-wrap">
            <p class="ute-hero-tag"><span aria-hidden="true"></span>UTE<b>Express</b></p>
            <h1>Bảng giá cước</h1>
            <p class="ute-lead">Cước theo dịch vụ, tuyến và khối lượng. Giá chính thức hiện khi tạo đơn.</p>
        </div>
    </div>

    <div class="ute-wrap">
    <section class="ute-estimate" aria-labelledby="est-title">
        <h2 id="est-title">Ước tính cước nhanh</h2>
        <p class="ute-estimate-sub">Điền thông tin để xem cước tham khảo trước khi tạo đơn.</p>
        <form method="get" action="${pageContext.request.contextPath}/pricing" class="ute-estimate-form">
            <label>Dịch vụ
                <select name="serviceCode">
                    <option value="STANDARD" ${param.serviceCode == 'STANDARD' ? 'selected' : ''}>Tiêu chuẩn</option>
                    <option value="FAST" ${param.serviceCode == 'FAST' ? 'selected' : ''}>Nhanh</option>
                    <option value="EXPRESS" ${param.serviceCode == 'EXPRESS' ? 'selected' : ''}>Hỏa tốc</option>
                </select></label>
            <label>Gửi từ miền
                <select name="fromRegion">
                    <option value="NORTH" ${param.fromRegion == 'NORTH' ? 'selected' : ''}>Bắc</option>
                    <option value="CENTRAL" ${param.fromRegion == 'CENTRAL' ? 'selected' : ''}>Trung</option>
                    <option value="SOUTH" ${param.fromRegion == 'SOUTH' ? 'selected' : ''}>Nam</option>
                </select></label>
            <label>Đến miền
                <select name="toRegion">
                    <option value="NORTH" ${param.toRegion == 'NORTH' ? 'selected' : ''}>Bắc</option>
                    <option value="CENTRAL" ${param.toRegion == 'CENTRAL' ? 'selected' : ''}>Trung</option>
                    <option value="SOUTH" ${param.toRegion == 'SOUTH' ? 'selected' : ''}>Nam</option>
                </select></label>
            <label>Khối lượng (gram)
                <input type="number" name="weightGrams" min="1" max="1000000" inputmode="numeric" required value="<c:out value='${empty param.weightGrams ? 500 : param.weightGrams}'/>"></label>
            <label>Giá trị khai báo (đ)
                <input type="number" name="declaredValue" min="0" inputmode="numeric" placeholder="Nếu có" value="<c:out value='${param.declaredValue}'/>"></label>
            <label>Tiền thu hộ COD (đ)
                <input type="number" name="codAmount" min="0" inputmode="numeric" placeholder="Nếu có" value="<c:out value='${param.codAmount}'/>"></label>
            <div class="ute-estimate-action">
                <p>Cước tham khảo theo biểu phí niêm yết.</p>
                <button type="submit" class="btn btn-ute-accent">Ước tính cước</button>
            </div>
        </form>
        <c:if test="${asked}">
            <div class="ute-estimate-result" role="status" aria-live="polite">
                <c:choose>
                    <c:when test="${invalid}"><p>Thông tin chưa hợp lệ. Khối lượng từ 1 đến 1.000.000 gram, các số tiền không âm.</p></c:when>
                    <c:when test="${not empty estimate}">
                        <p class="ute-estimate-label">Cước ước tính</p>
                        <p class="ute-estimate-fee"><fmt:formatNumber value="${estimate}" pattern="#,##0" var="feeText"/>${fn:replace(feeText, ',', '.')} đ</p>
                        <p class="text-muted mb-0">Ước tính theo bảng giá hiện hành.</p>
                    </c:when>
                    <c:otherwise><p>Chưa có bảng giá cho dịch vụ và tuyến này.</p></c:otherwise>
                </c:choose>
            </div>
        </c:if>
    </section>

    <section class="ute-price-tables" aria-labelledby="table-title">
        <h2 id="table-title">Bảng giá đang áp dụng</h2>
        <p class="ute-estimate-sub">Giá chính thức hiện khi tạo đơn.</p>
        <c:choose>
            <c:when test="${empty policies}"><p class="text-muted">Chưa có bảng giá nào đang áp dụng.</p></c:when>
            <c:otherwise>
                <c:forEach items="${['STANDARD', 'FAST', 'EXPRESS']}" var="svc">
                    <h3 class="ute-svc-title"><c:choose><c:when test="${svc == 'STANDARD'}">Dịch vụ Tiêu chuẩn</c:when><c:when test="${svc == 'FAST'}">Dịch vụ Nhanh</c:when><c:otherwise>Dịch vụ Hỏa tốc</c:otherwise></c:choose><span><c:choose><c:when test="${svc == 'STANDARD'}">Nội địa · 3–5 ngày</c:when><c:when test="${svc == 'FAST'}">Nội địa · 1–2 ngày</c:when><c:otherwise>Nội địa · Trong ngày</c:otherwise></c:choose></span></h3>
                    <div class="table-responsive">
                        <table class="table">
                            <thead><tr><th>Tuyến</th><th>KL đầu</th><th>Giá khởi điểm</th><th>Mỗi bước vượt</th><th>Phí bảo hiểm</th><th>Phí thu hộ COD</th></tr></thead>
                            <tbody>
                            <c:forEach items="${policies}" var="p">
                                <c:if test="${p.serviceCode == svc}">
                                <tr>
                                    <td><strong><c:forEach items="${[p.fromRegion, p.toRegion]}" var="r" varStatus="st"><c:choose><c:when test="${r == 'NORTH'}">Bắc</c:when><c:when test="${r == 'CENTRAL'}">Trung</c:when><c:when test="${r == 'SOUTH'}">Nam</c:when><c:otherwise><c:out value="${r}"/></c:otherwise></c:choose><c:if test="${st.first}"> → </c:if></c:forEach></strong></td>
                                    <td>đến <fmt:formatNumber value="${p.baseWeightG}" pattern="#,##0" var="f_baseWeightG"/>${fn:replace(f_baseWeightG, ',', '.')} g</td>
                                    <td><fmt:formatNumber value="${p.basePrice}" pattern="#,##0" var="f_basePrice"/>${fn:replace(f_basePrice, ',', '.')} đ</td>
                                    <td>+<fmt:formatNumber value="${p.extraWeightPrice}" pattern="#,##0" var="f_extraWeightPrice"/>${fn:replace(f_extraWeightPrice, ',', '.')} đ / <fmt:formatNumber value="${p.extraWeightStepG}" pattern="#,##0" var="f_extraWeightStepG"/>${fn:replace(f_extraWeightStepG, ',', '.')} g</td>
                                    <td><fmt:formatNumber value="${empty p.insuranceRatePercent ? 0 : p.insuranceRatePercent}" pattern="0.00"/>% giá trị khai báo</td>
                                    <td><fmt:formatNumber value="${empty p.codFeePercent ? 0 : p.codFeePercent}" pattern="0.00"/>% tiền thu hộ</td>
                                </tr>
                                </c:if>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:forEach>
            </c:otherwise>
        </c:choose>
        <div class="ute-price-note">
            <div><b>Lưu ý</b><p>Cước theo khối lượng thực tế. Có thể có phụ phí nếu bưu cục đo lại. Giá chính thức là giá khi tạo đơn.</p></div>
            <c:if test="${empty pageContext.request.userPrincipal or pageContext.request.isUserInRole('USER')}">
            <a class="btn btn-ute-accent" href="${pageContext.request.contextPath}/user/shipments/create">Tạo đơn gửi hàng</a>
            </c:if>
        </div>
    </section>
    </div>
</main>
<jsp:include page="/WEB-INF/views/layout/footer.jsp" />
