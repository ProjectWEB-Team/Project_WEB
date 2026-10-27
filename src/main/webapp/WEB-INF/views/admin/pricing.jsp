<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">
    <h1 class="h3 mb-1 text-ute-dark">Bảng giá cước</h1>
    <p class="text-ute-muted small">Giá gói đầu áp dụng cho khối lượng cơ bản; mỗi bước khối lượng thêm cộng "giá thêm". Chỉ dòng đang áp dụng mới được dùng để báo giá. Đơn đang báo giá dở sẽ được yêu cầu báo giá lại khi giá đổi.</p>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <div class="card-ute p-3 table-responsive">
        <table class="table table-sm align-middle">
            <thead><tr><th>Dịch vụ</th><th>Từ → Đến</th><th>Gói đầu (g)</th><th>Giá gói đầu</th><th>Bước thêm (g)</th><th>Giá thêm</th><th>Bảo hiểm %</th><th>COD %</th><th>Áp dụng</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${policies}" var="p">
                <tr>
                    <td><c:out value="${p.serviceCode}"/></td>
                    <td><c:out value="${p.fromRegion}"/> → <c:out value="${p.toRegion}"/></td>
                    <td>${p.baseWeightG}</td>
                    <td><input form="price-${p.id}" class="form-control form-control-sm" name="basePrice" type="number" min="0" step="1" value="${p.basePrice}" required aria-label="Giá gói đầu"></td>
                    <td>${p.extraWeightStepG}</td>
                    <td><input form="price-${p.id}" class="form-control form-control-sm" name="extraWeightPrice" type="number" min="0" step="1" value="${p.extraWeightPrice}" required aria-label="Giá thêm"></td>
                    <td><input form="price-${p.id}" class="form-control form-control-sm" name="insuranceRatePercent" type="number" min="0" max="100" step="0.01" value="${p.insuranceRatePercent}" required aria-label="Bảo hiểm phần trăm"></td>
                    <td><input form="price-${p.id}" class="form-control form-control-sm" name="codFeePercent" type="number" min="0" max="100" step="0.01" value="${p.codFeePercent}" required aria-label="COD phần trăm"></td>
                    <td><input form="price-${p.id}" class="form-check-input" type="checkbox" name="active" value="true" ${p.status == 'ACTIVE' ? 'checked' : ''} aria-label="Đang áp dụng"></td>
                    <td><form id="price-${p.id}" method="post" action="${pageContext.request.contextPath}/admin/pricing/${p.id}">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button class="btn btn-sm btn-outline-primary" type="submit">Lưu</button></form></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</main>

<jsp:include page="../layout/footer.jsp" />
