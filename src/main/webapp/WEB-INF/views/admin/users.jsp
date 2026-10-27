<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<main class="container-fluid px-lg-4 py-4 flex-grow-1">
    <h1 class="h3 mb-3 text-ute-dark">Quản lý người dùng và nhân viên</h1>

    <c:if test="${not empty successMessage}"><div class="alert alert-success" role="status"><c:out value="${successMessage}"/></div></c:if>
    <c:if test="${not empty errorMessage}"><div class="alert alert-danger" role="alert"><c:out value="${errorMessage}"/></div></c:if>

    <section class="card-ute p-4 mb-4">
        <h2 class="h5">Tạo tài khoản nhân viên</h2>
        <p class="text-ute-muted small mb-3">Hệ thống sinh mật khẩu tạm và chỉ hiện một lần. Shipper, nhân viên hub, tài xế và quản lý cần chọn bưu cục và vị trí.</p>
        <form method="post" action="${pageContext.request.contextPath}/admin/users" class="row g-2">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
            <div class="col-md-2"><label class="form-label" for="username">Tên đăng nhập</label><input class="form-control" id="username" name="username" required maxlength="50"></div>
            <div class="col-md-2"><label class="form-label" for="fullName">Họ tên</label><input class="form-control" id="fullName" name="fullName" required maxlength="100"></div>
            <div class="col-md-2"><label class="form-label" for="phone">Số điện thoại</label><input class="form-control" id="phone" name="phone" type="tel" required maxlength="20"></div>
            <div class="col-md-2"><label class="form-label" for="email">Email</label><input class="form-control" id="email" name="email" type="email" required maxlength="100"></div>
            <div class="col-md-2"><label class="form-label" for="role">Vai trò</label>
                <select class="form-select" id="role" name="role"><c:forEach items="${roles}" var="r"><option value="${r}">${r}</option></c:forEach></select></div>
            <div class="col-md-2"><label class="form-label" for="hubId">Bưu cục</label>
                <select class="form-select" id="hubId" name="hubId"><option value="">(không)</option><c:forEach items="${hubs}" var="h"><option value="${h.id}"><c:out value="${h.code}"/> – <c:out value="${h.name}"/></option></c:forEach></select></div>
            <div class="col-md-2"><label class="form-label" for="position">Vị trí</label>
                <select class="form-select" id="position" name="position"><option value="">(không)</option><c:forEach items="${positions}" var="p"><option value="${p}">${p}</option></c:forEach></select></div>
            <div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit">Tạo tài khoản</button></div>
        </form>
    </section>

    <section class="card-ute p-4">
        <form method="get" action="${pageContext.request.contextPath}/admin/users" class="d-flex gap-2 mb-3">
            <input class="form-control" name="q" value="<c:out value='${q}'/>" placeholder="Tìm theo tên đăng nhập, họ tên, số điện thoại" aria-label="Tìm kiếm">
            <button class="btn btn-outline-primary" type="submit">Tìm</button>
        </form>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead><tr><th>Tên đăng nhập</th><th>Họ tên</th><th>Vai trò</th><th>Trạng thái</th><th>Nơi làm việc</th><th>Thao tác</th></tr></thead>
                <tbody>
                <c:forEach items="${users.content}" var="u">
                    <tr>
                        <td><c:out value="${u.username}"/></td>
                        <td><c:out value="${u.fullName}"/><br><span class="small text-ute-muted"><c:out value="${u.phone}"/></span></td>
                        <td><c:out value="${u.role}"/></td>
                        <td><c:out value="${u.status}"/></td>
                        <td><c:out value="${workplaces[u.id]}" default="–"/></td>
                        <td>
                            <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/lock" class="d-inline">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <input type="hidden" name="locked" value="${u.status != 'LOCKED'}">
                                <button class="btn btn-sm btn-outline-danger" type="submit">${u.status == 'LOCKED' ? 'Mở khoá' : 'Khoá'}</button>
                            </form>
                            <form method="post" action="${pageContext.request.contextPath}/admin/users/${u.id}/assign" class="d-flex flex-wrap gap-1 mt-1">
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                                <select class="form-select form-select-sm w-auto" name="role" aria-label="Vai trò"><c:forEach items="${roles}" var="r"><option value="${r}" ${r == u.role ? 'selected' : ''}>${r}</option></c:forEach></select>
                                <select class="form-select form-select-sm w-auto" name="hubId" aria-label="Bưu cục"><option value="">(không)</option><c:forEach items="${hubs}" var="h"><option value="${h.id}"><c:out value="${h.code}"/></option></c:forEach></select>
                                <select class="form-select form-select-sm w-auto" name="position" aria-label="Vị trí"><option value="">(không)</option><c:forEach items="${positions}" var="p"><option value="${p}">${p}</option></c:forEach></select>
                                <button class="btn btn-sm btn-outline-primary" type="submit">Lưu</button>
                            </form>
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
