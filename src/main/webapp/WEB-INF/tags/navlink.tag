<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="href" required="true" %>
<%-- One navigation link; marks itself current when the original request path starts with its target. --%>
<c:set var="full" value="${pageContext.request.contextPath}${href}" />
<c:set var="uri" value="${requestScope['jakarta.servlet.forward.request_uri']}" />
<c:set var="here" value="${not empty uri and fn:startsWith(uri, full)}" />
<li class="nav-item"><a class="nav-link${here ? ' active' : ''}" href="${full}"${here ? ' aria-current="page"' : ''}><jsp:doBody /></a></li>
