<%@ tag body-content="scriptless" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ attribute name="href" required="true" %>
<%-- Forms rendered by several actions can name their current page; other links match a route segment. --%>
<c:set var="full" value="${pageContext.request.contextPath}${href}" />
<c:set var="subpath" value="${full}/" />
<c:set var="uri" value="${requestScope['jakarta.servlet.forward.request_uri']}" />
<c:set var="here" value="${not empty requestScope.uteActiveNav ? requestScope.uteActiveNav eq href : not empty uri and (uri eq full or fn:startsWith(uri, subpath))}" />
<li class="nav-item"><a class="nav-link${here ? ' active' : ''}" href="${full}"${here ? ' aria-current="page"' : ''}><jsp:doBody /></a></li>
