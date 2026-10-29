<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="../layout/header.jsp" />

<main class="ute-staff flex-grow-1">
    <h1>Quản lý Bảng giá cước &amp; Phụ phí</h1>
    <p class="ute-staff-sub">Giá gói đầu áp dụng cho khối lượng cơ bản; mỗi bước khối lượng thêm cộng "giá thêm". Chỉ dòng đang áp dụng mới được dùng để báo giá. Đơn đang báo giá dở sẽ được yêu cầu báo giá lại khi giá đổi.</p>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <section class="ute-app-card">
        <div class="ute-app-card-head">
            <h2>Danh mục chính sách giá cước theo Dịch vụ &amp; Tuyến vùng (${policies.size()} tuyến)</h2>
            <p>Chỉnh sửa trực tiếp giá gói đầu, đơn giá bước vượt, tỷ lệ phí bảo hiểm (%) và phí thu hộ COD (%) rồi bấm Lưu trên từng dòng.</p>
        </div>
        <div class="table-responsive">
        <table class="table ute-list-table align-middle">
            <thead><tr><th>Gói dịch vụ</th><th>Tuyến Từ → Đến</th><th>Khối lượng đầu</th><th>Giá gói đầu (đ)</th><th>Bước vượt</th><th>Giá thêm (đ)</th><th>Bảo hiểm %</th><th>Phí COD %</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
            <tbody>
            <c:forEach items="${policies}" var="p">
                <tr>
                    <td><strong><c:choose><c:when test="${p.serviceCode == 'STANDARD'}">Tiêu chuẩn</c:when><c:when test="${p.serviceCode == 'FAST'}">Nhanh (Fast)</c:when><c:when test="${p.serviceCode == 'EXPRESS'}">Hỏa tốc (Express)</c:when><c:otherwise><c:out value="${p.serviceCode}"/></c:otherwise></c:choose></strong></td>
                    <td><c:choose><c:when test="${p.fromRegion == p.toRegion}">Nội miền</c:when><c:when test="${p.fromRegion != 'CENTRAL' and p.toRegion != 'CENTRAL'}">Liên miền</c:when><c:otherwise>Cận miền</c:otherwise></c:choose>
                        (<c:forEach items="${[p.fromRegion, p.toRegion]}" var="r" varStatus="st"><c:choose><c:when test="${r == 'NORTH'}">Bắc</c:when><c:when test="${r == 'CENTRAL'}">Trung</c:when><c:when test="${r == 'SOUTH'}">Nam</c:when><c:otherwise><c:out value="${r}"/></c:otherwise></c:choose><c:if test="${st.first}"> → </c:if></c:forEach>)</td>
                    <td>${p.baseWeightG} g</td>
                    <td><input form="price-${p.id}" class="ute-cell-input" name="basePrice" type="number" min="0" step="1" value="${p.basePrice}" required aria-label="Giá gói đầu"></td>
                    <td>${p.extraWeightStepG} g</td>
                    <td><input form="price-${p.id}" class="ute-cell-input" name="extraWeightPrice" type="number" min="0" step="1" value="${p.extraWeightPrice}" required aria-label="Giá thêm"></td>
                    <td><input form="price-${p.id}" class="ute-cell-input ute-cell-pct" name="insuranceRatePercent" type="number" min="0" max="100" step="0.01" value="${p.insuranceRatePercent}" required aria-label="Bảo hiểm phần trăm"></td>
                    <td><input form="price-${p.id}" class="ute-cell-input ute-cell-pct" name="codFeePercent" type="number" min="0" max="100" step="0.01" value="${p.codFeePercent}" required aria-label="COD phần trăm"></td>
                    <td><label class="ute-status-toggle"><input form="price-${p.id}" class="visually-hidden" type="checkbox" name="active" value="true" ${p.status == 'ACTIVE' ? 'checked' : ''}><span class="on">✓ Đang áp dụng</span><span class="off">Tạm dừng</span></label></td>
                    <td><form id="price-${p.id}" method="post" action="${pageContext.request.contextPath}/admin/pricing/${p.id}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button class="btn ute-btn-view" type="submit">Lưu</button></form></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
        </div>
    </section>
</main>

<jsp:include page="../layout/footer.jsp" />
