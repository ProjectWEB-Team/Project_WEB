<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">
    <h1 class="h3 mb-1 text-ute-dark">Mạng lưới bưu cục và tuyến</h1>
    <p class="text-ute-muted small">Cây 3 cấp: SOC → hub vùng → bưu cục giao. Mã, loại và cấp không đổi sau khi tạo. Bưu cục không bị xoá, chỉ ngừng hoạt động khi không còn đơn, nhân viên, bao, chuyến, phiếu bàn giao hay quy tắc định tuyến nào phụ thuộc.</p>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <section class="card-ute p-3 mb-4">
        <h2 class="h5">Thêm bưu cục</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/network/hubs" class="row g-2">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="col-md-2"><label class="form-label" for="code">Mã</label><input class="form-control" id="code" name="code" required maxlength="20" pattern="[A-Z0-9-]{2,20}"></div>
            <div class="col-md-3"><label class="form-label" for="name">Tên</label><input class="form-control" id="name" name="name" required maxlength="150"></div>
            <div class="col-md-2"><label class="form-label" for="type">Loại</label>
                <select class="form-select" id="type" name="type"><option value="DELIVERY_HUB">Bưu cục giao (cấp 1)</option><option value="REGIONAL_HUB">Hub vùng (cấp 2)</option><option value="SOC">SOC (cấp 3)</option></select></div>
            <div class="col-md-1"><label class="form-label" for="region">Miền</label>
                <select class="form-select" id="region" name="region"><option>NORTH</option><option>CENTRAL</option><option>SOUTH</option></select></div>
            <div class="col-md-2"><label class="form-label" for="province">Tỉnh/thành</label><input class="form-control" id="province" name="province" required maxlength="100"></div>
            <div class="col-md-2"><label class="form-label" for="district">Quận/huyện</label><input class="form-control" id="district" name="district" maxlength="100"></div>
            <div class="col-md-4"><label class="form-label" for="address">Địa chỉ</label><input class="form-control" id="address" name="address" maxlength="255"></div>
            <div class="col-md-3"><label class="form-label" for="parentId">Bưu cục cha</label>
                <select class="form-select" id="parentId" name="parentId"><option value="">(không – chỉ cho SOC)</option>
                    <c:forEach items="${hubs}" var="h"><c:if test="${h.level > 1 and h.status == 'ACTIVE'}"><option value="${h.id}"><c:out value="${h.code}"/> (cấp ${h.level})</option></c:if></c:forEach></select></div>
            <div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit">Thêm</button></div>
        </form>
    </section>

    <section class="card-ute p-3 mb-4 table-responsive">
        <h2 class="h5">Cây bưu cục (${hubs.size()})</h2>
        <table class="table table-sm align-middle">
            <thead><tr><th>Mã</th><th>Cấp</th><th>Miền</th><th>Tên</th><th>Tỉnh/thành</th><th>Quận/huyện</th><th>Địa chỉ</th><th>Bưu cục cha</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${hubs}" var="h">
                <tr>
                    <td><c:out value="${h.code}"/></td><td>${h.level}</td><td><c:out value="${h.region}"/></td>
                    <td><input form="hub-${h.id}" class="form-control form-control-sm" name="name" value="<c:out value='${h.name}'/>" required aria-label="Tên"></td>
                    <td><input form="hub-${h.id}" class="form-control form-control-sm" name="province" value="<c:out value='${h.province}'/>" required aria-label="Tỉnh/thành"></td>
                    <td><input form="hub-${h.id}" class="form-control form-control-sm" name="district" value="<c:out value='${h.district}'/>" aria-label="Quận/huyện"></td>
                    <td><input form="hub-${h.id}" class="form-control form-control-sm" name="address" value="<c:out value='${h.address}'/>" aria-label="Địa chỉ"></td>
                    <td><select form="hub-${h.id}" class="form-select form-select-sm" name="parentId" aria-label="Bưu cục cha"><option value="">(không)</option>
                        <c:forEach items="${hubs}" var="p"><c:if test="${p.level == h.level + 1}"><option value="${p.id}" ${p.id == h.parentId ? 'selected' : ''}><c:out value="${p.code}"/></option></c:if></c:forEach></select></td>
                    <td><c:out value="${h.status}"/></td>
                    <td class="text-nowrap">
                        <form id="hub-${h.id}" method="post" action="${pageContext.request.contextPath}/admin/network/hubs/${h.id}" class="d-inline">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <button class="btn btn-sm btn-outline-primary" type="submit">Lưu</button></form>
                        <form method="post" action="${pageContext.request.contextPath}/admin/network/hubs/${h.id}/status" class="d-inline">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <input type="hidden" name="active" value="${h.status != 'ACTIVE'}">
                            <button class="btn btn-sm btn-outline-danger" type="submit">${h.status == 'ACTIVE' ? 'Ngừng' : 'Bật lại'}</button></form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </section>

    <section class="card-ute p-3 table-responsive">
        <h2 class="h5">Quy tắc định tuyến (${routes.size()})</h2>
        <form method="post" action="${pageContext.request.contextPath}/admin/network/rules" class="row g-2 mb-3">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="col-md-3"><label class="form-label" for="currentHubId">Đang ở</label>
                <select class="form-select" id="currentHubId" name="currentHubId"><c:forEach items="${hubs}" var="h"><c:if test="${h.status == 'ACTIVE'}"><option value="${h.id}"><c:out value="${h.code}"/></option></c:if></c:forEach></select></div>
            <div class="col-md-2"><label class="form-label" for="ruleRegion">Miền đích</label>
                <select class="form-select" id="ruleRegion" name="region"><option>NORTH</option><option>CENTRAL</option><option>SOUTH</option></select></div>
            <div class="col-md-3"><label class="form-label" for="ruleProvince">Tỉnh đích (để trống = cả miền)</label><input class="form-control" id="ruleProvince" name="province" maxlength="100"></div>
            <div class="col-md-2"><label class="form-label" for="nextHubId">Chuyển tới</label>
                <select class="form-select" id="nextHubId" name="nextHubId"><c:forEach items="${hubs}" var="h"><c:if test="${h.status == 'ACTIVE'}"><option value="${h.id}"><c:out value="${h.code}"/></option></c:if></c:forEach></select></div>
            <div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit">Thêm quy tắc</button></div>
        </form>
        <table class="table table-sm">
            <thead><tr><th>Đang ở</th><th>Miền đích</th><th>Tỉnh đích</th><th>Chuyển tới</th><th>Trạng thái</th><th></th></tr></thead>
            <tbody>
            <c:forEach items="${routes}" var="r">
                <tr><td><c:out value="${r.currentHub}"/></td><td><c:out value="${r.destinationRegion}"/></td><td><c:out value="${r.destinationProvince}" default="(cả miền)"/></td>
                    <td><c:out value="${r.nextHub}"/></td><td><c:out value="${r.status}"/></td>
                    <td><c:if test="${r.status == 'ACTIVE'}"><form method="post" action="${pageContext.request.contextPath}/admin/network/rules/${r.id}/deactivate">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                        <button class="btn btn-sm btn-outline-danger" type="submit">Ngừng</button></form></c:if></td></tr>
            </c:forEach>
            </tbody>
        </table>
    </section>
</main>

<jsp:include page="../layout/footer.jsp" />
