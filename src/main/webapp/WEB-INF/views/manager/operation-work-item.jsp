<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Việc cần xử lý | UTEExpress" scope="request"/>
<jsp:include page="../layout/header.jsp" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<main class="container flex-grow-1 py-4">
    <c:if test="${not empty notice}"><div class="alert alert-success" role="status"><c:out value="${notice}"/></div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-danger" role="alert"><c:out value="${error}"/></div></c:if>

    <h1 class="h3 mb-1"><c:out value="${item.summary}"/></h1>
    <p class="text-muted">
        <c:choose>
            <c:when test="${item.kind == 'MISSING'}">Sự cố</c:when>
            <c:when test="${item.kind == 'ROUTE'}">Tuyến</c:when>
            <c:otherwise>Bàn giao</c:otherwise>
        </c:choose>
        #<c:out value="${item.id}"/>
        <c:if test="${not empty item.shipmentId}"> · Đơn #<c:out value="${item.shipmentId}"/></c:if>
    </p>

    <c:if test="${not empty handover}">
        <section class="pf-card p-4 mb-3" aria-label="Chi tiết bàn giao">
            <dl class="row mb-0">
                <dt class="col-sm-3">Tuyến</dt><dd class="col-sm-9"><c:out value="${handover.fromHub}"/> → <c:out value="${handover.toHub}"/></dd>
                <dt class="col-sm-3">Tài xế</dt><dd class="col-sm-9"><c:out value="${handover.driver}"/></dd>
                <dt class="col-sm-3">Chuyến xe</dt><dd class="col-sm-9"><c:out value="${empty handover.trip ? 'Chưa có' : handover.trip}"/></dd>
                <dt class="col-sm-3">Bao đã nhận</dt><dd class="col-sm-9">${handover.accepted} / ${handover.expected}</dd>
            </dl>
        </section>

        <section class="pf-card p-4 mb-3" aria-label="Bao hàng">
            <h2 class="h5">Bao hàng</h2>
            <c:forEach items="${handover.containers}" var="box">
                <div class="py-2 border-top">
                    <strong><c:out value="${box.code}"/></strong> <span class="pf-pill"><c:out value="${box.status}"/></span>
                    <div class="d-flex flex-wrap gap-2 mt-2">
                        <c:forEach items="${box.shipments}" var="s">
                            <a class="btn btn-outline-secondary btn-sm" href="${ctx}/manager/audit/shipments/${s.id}"><c:out value="${s.tracking}"/></a>
                        </c:forEach>
                        <c:if test="${empty box.shipments}"><span class="text-muted">Bao trống</span></c:if>
                    </div>
                </div>
            </c:forEach>
        </section>

        <c:if test="${handover.reassignable}">
            <section class="pf-card p-4 mb-3" aria-label="Đổi tài xế">
                <h2 class="h5">Đổi tài xế</h2>
                <c:choose>
                    <c:when test="${empty handover.drivers}"><p class="text-muted mb-0">Bưu cục xuất chưa có tài xế khác.</p></c:when>
                    <c:otherwise>
                        <form method="post" action="${ctx}/manager/operations/handovers/${handover.id}/reassign" class="d-flex flex-wrap gap-2 align-items-center">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}">
                            <label class="visually-hidden" for="driverId">Tài xế mới</label>
                            <select class="form-select" id="driverId" name="driverId" style="max-width: 18rem;" required>
                                <c:forEach items="${handover.drivers}" var="d"><option value="${d.id}"><c:out value="${d.name}"/></option></c:forEach>
                            </select>
                            <button type="submit" class="btn btn-accent">Đổi tài xế</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </section>
        </c:if>
    </c:if>

    <div class="d-flex flex-wrap gap-2">
        <c:choose>
            <c:when test="${item.kind == 'MISSING'}"><a class="btn btn-primary" href="${ctx}/manager/incidents/missing/${item.id}">Điều tra</a></c:when>
            <c:when test="${not empty item.shipmentId}"><a class="btn btn-primary" href="${ctx}/manager/audit/shipments/${item.shipmentId}">Lịch sử đơn</a></c:when>
        </c:choose>
        <a class="btn btn-outline-secondary" href="${ctx}/manager/operations">Quay lại</a>
    </div>
</main>
<jsp:include page="../layout/footer.jsp" />
