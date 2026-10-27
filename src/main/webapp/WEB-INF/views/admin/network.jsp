<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">
    <h1 class="h3 mb-1 text-ute-dark">Mạng lưới bưu cục và tuyến</h1>
    <p class="text-ute-muted small">Chỉ xem. Việc sửa cây bưu cục và tuyến cần nhóm vận hành (Phase 3) thống nhất trước.</p>

    <section class="card-ute p-3 mb-4 table-responsive">
        <h2 class="h5">Cây bưu cục (${hubs.size()})</h2>
        <table class="table table-sm">
            <thead><tr><th>Mã</th><th>Tên</th><th>Cấp</th><th>Loại</th><th>Miền</th><th>Tỉnh/thành</th><th>Bưu cục cha</th><th>Trạng thái</th></tr></thead>
            <tbody>
            <c:forEach items="${hubs}" var="h">
                <tr><td><c:out value="${h.code}"/></td><td><c:out value="${h.name}"/></td><td>${h.level}</td><td><c:out value="${h.type}"/></td>
                    <td><c:out value="${h.region}"/></td><td><c:out value="${h.province}"/></td><td><c:out value="${h.parentCode}" default="–"/></td><td><c:out value="${h.status}"/></td></tr>
            </c:forEach>
            </tbody>
        </table>
    </section>

    <section class="card-ute p-3 table-responsive">
        <h2 class="h5">Quy tắc định tuyến (${routes.size()})</h2>
        <table class="table table-sm">
            <thead><tr><th>Đang ở</th><th>Miền đích</th><th>Tỉnh đích</th><th>Chuyển tới</th><th>Trạng thái</th></tr></thead>
            <tbody>
            <c:forEach items="${routes}" var="r">
                <tr><td><c:out value="${r.currentHub}"/></td><td><c:out value="${r.destinationRegion}"/></td><td><c:out value="${r.destinationProvince}" default="(cả miền)"/></td>
                    <td><c:out value="${r.nextHub}"/></td><td><c:out value="${r.status}"/></td></tr>
            </c:forEach>
            </tbody>
        </table>
    </section>
</main>

<jsp:include page="../layout/footer.jsp" />
