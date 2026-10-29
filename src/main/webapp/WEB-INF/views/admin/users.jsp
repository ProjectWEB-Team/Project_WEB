<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<jsp:useBean id="roleNames" class="java.util.HashMap"/>
<c:set target="${roleNames}" property="ROLE_USER" value="Khách hàng"/>
<c:set target="${roleNames}" property="ROLE_SHIPPER" value="Shipper giao hàng"/>
<c:set target="${roleNames}" property="ROLE_LINEHAUL_DRIVER" value="Tài xế trung chuyển"/>
<c:set target="${roleNames}" property="ROLE_HUB_STAFF" value="Nhân viên bưu cục"/>
<c:set target="${roleNames}" property="ROLE_MANAGER" value="Quản lý vận hành"/>
<c:set target="${roleNames}" property="ROLE_ADMIN" value="Quản trị hệ thống"/>
<main class="ute-staff flex-grow-1">
    <h1>Quản lý người dùng và nhân viên</h1>
    <p class="ute-staff-sub">Tạo tài khoản và phân quyền nhân viên.</p>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <section class="card-ute p-4 mb-4">
        <h2 class="ute-staff-h3">Tạo tài khoản nhân viên</h2>
        <p class="ute-staff-note mb-3">Nhân viên cần bưu cục và vị trí.</p>
        <form method="post" action="${pageContext.request.contextPath}/admin/users" class="row g-2 ute-user-create">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="col-md-2"><label class="form-label" for="username">Tên đăng nhập</label><input class="form-control" id="username" name="username" required maxlength="50" placeholder="VD: nv_buucuc_dn01"></div>
            <div class="col-md-2"><label class="form-label" for="fullName">Họ tên</label><input class="form-control" id="fullName" name="fullName" required maxlength="100" placeholder="VD: Nguyễn Văn An"></div>
            <div class="col-md-2"><label class="form-label" for="phone">Số điện thoại</label><input class="form-control" id="phone" name="phone" type="tel" required maxlength="20" placeholder="VD: 0912 345 678"></div>
            <div class="col-md-2"><label class="form-label" for="email">Email</label><input class="form-control" id="email" name="email" type="email" required maxlength="100" placeholder="VD: an.nv@uteexpress.vn"></div>
            <div class="col-md-2"><label class="form-label" for="role">Vai trò</label>
                <select class="form-select" id="role" name="role"><c:forEach items="${roles}" var="r"><option value="${r}">${empty roleNames[fn:trim(r)] ? r : roleNames[fn:trim(r)]}</option></c:forEach></select></div>
            <div class="col-md-2"><label class="form-label" for="hubId">Bưu cục</label>
                <select class="form-select" id="hubId" name="hubId"><option value="">(không)</option><c:forEach items="${hubs}" var="h"><option value="${h.id}"><c:out value="${h.code}"/> – <c:out value="${h.name}"/></option></c:forEach></select></div>
            <div class="col-md-2"><label class="form-label" for="position">Vị trí</label>
                <select class="form-select" id="position" name="position"><option value="">(không)</option><c:forEach items="${positions}" var="p"><option value="${p}">${p}</option></c:forEach></select></div>
            <div class="col-md-2 d-flex align-items-end"><button class="btn btn-ute-orange w-100" type="submit">+ Tạo tài khoản</button></div>
        </form>
    </section>

    <section class="card-ute p-4">
        <h2 class="ute-staff-h3">Danh sách người dùng (${users.totalElements})</h2>
        <form method="get" action="${pageContext.request.contextPath}/admin/users" class="d-flex gap-2 mb-3">
            <input class="form-control" name="q" value="<c:out value='${q}'/>" placeholder="Tìm theo tên đăng nhập, họ tên, số điện thoại" aria-label="Tìm kiếm">
            <button class="btn btn-ute-orange" type="submit">Tìm</button>
        </form>
        <div class="table-responsive">
            <table class="table ute-list-table align-middle">
                <thead><tr><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th>Nơi làm việc</th><th>Phân quyền & Thao tác</th></tr></thead>
                <tbody>
                <c:forEach items="${users.content}" var="u">
                    <tr>
                        <td><strong><c:out value="${u.username}"/></strong></td>
                        <td><c:out value="${u.fullName}"/><br><span class="small text-ute-muted"><c:out value="${u.phone}"/></span></td>
                        <td><c:out value="${empty roleNames[fn:trim(u.role)] ? u.role : roleNames[fn:trim(u.role)]}"/></td>
                        <td><c:choose><c:when test="${u.status == 'ACTIVE'}"><span class="ute-status-toggle"><span class="on" style="display:inline">&#10003; Hoạt động</span></span></c:when><c:otherwise><span class="ute-status-toggle"><span class="off"><c:out value="${u.status == 'LOCKED' ? 'Đã khóa' : u.status}"/></span></span></c:otherwise></c:choose></td>
                        <td><c:out value="${workplaces[u.id]}" default="–"/></td>
                        <td>
                            <div class="d-flex align-items-center gap-2 flex-wrap">
                                <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/assign" class="d-flex align-items-center gap-2 flex-wrap m-0">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <select class="form-select form-select-sm" style="min-width: 140px;" name="role" aria-label="Vai trò">
                                        <c:forEach items="${roles}" var="r"><option value="${r}" ${r == u.role ? 'selected' : ''}>${empty roleNames[fn:trim(r)] ? r : roleNames[fn:trim(r)]}</option></c:forEach>
                                    </select>
                                    <select class="form-select form-select-sm" style="min-width: 110px;" name="hubId" aria-label="Bưu cục">
                                        <option value="">(Bưu cục)</option>
                                        <c:forEach items="${hubs}" var="h"><option value="${h.id}" ${workplaces[u.id] == h.code ? 'selected' : ''}><c:out value="${h.code}"/></option></c:forEach>
                                    </select>
                                    <button class="btn btn-sm btn-ute-primary" type="submit">Lưu</button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/lock" class="m-0 d-inline">
                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                    <input type="hidden" name="locked" value="${u.status != 'LOCKED'}">
                                    <button class="btn btn-sm btn-ute-danger-soft" type="submit">${u.status == 'LOCKED' ? 'Mở khoá' : 'Khoá'}</button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
        <nav class="d-flex gap-2" aria-label="Phân trang">
            <c:if test="${users.number > 0}"><a class="btn btn-sm btn-outline-secondary" href="?q=${fn:escapeXml(q)}&page=${users.number - 1}">Trang trước</a></c:if>
            <span class="small align-self-center">Trang ${users.number + 1} / ${users.totalPages}</span>
            <c:if test="${users.number + 1 < users.totalPages}"><a class="btn btn-sm btn-outline-secondary" href="?q=${fn:escapeXml(q)}&page=${users.number + 1}">Trang sau</a></c:if>
        </nav>
    </section>
</main>

<jsp:include page="../layout/footer.jsp" />
