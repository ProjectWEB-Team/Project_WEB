<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<jsp:include page="../layout/header.jsp" />

<main class="ute-app ute-profile flex-grow-1 py-4" style="font-family: 'Geist', sans-serif;">
  <div class="container ute-wrap">

    <!-- Flash notifications -->
    <c:if test="${not empty successMessage}">
      <div class="alert alert-success alert-dismissible fade show shadow-sm d-flex align-items-center mb-4" role="alert">
        <i class="bi bi-check-circle-fill fs-5 me-2 text-success"></i>
        <div class="flex-grow-1"><c:out value="${successMessage}"/></div>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
      </div>
    </c:if>
    <c:if test="${not empty errorMessage}">
      <div class="alert alert-danger alert-dismissible fade show shadow-sm d-flex align-items-center mb-4" role="alert">
        <i class="bi bi-exclamation-triangle-fill fs-5 me-2 text-danger"></i>
        <div class="flex-grow-1"><c:out value="${errorMessage}"/></div>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
      </div>
    </c:if>

    <!-- Profile Header Banner Card -->
    <div class="card border-0 shadow-sm rounded-4 mb-4 overflow-hidden" style="background: linear-gradient(135deg, var(--ink) 0%, #0a1a2e 100%); color: #fff;">
      <div class="card-body p-4 p-md-5">
        <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-4">
          <div class="d-flex align-items-center gap-3">
            <div class="position-relative">
              <span class="rounded-circle d-flex align-items-center justify-content-center text-white fw-bold shadow"
                    style="width: 80px; height: 80px; font-size: 1.75rem; background: linear-gradient(135deg, #ff6b35 0%, #f7c59f 100%);">
                <c:choose>
                  <c:when test="${not empty profileUser.fullName}">
                    <c:out value="${fn:toUpperCase(fn:substring(profileUser.fullName, 0, 2))}" />
                  </c:when>
                  <c:otherwise>
                    <c:out value="${fn:toUpperCase(fn:substring(profileUser.username, 0, 2))}" />
                  </c:otherwise>
                </c:choose>
              </span>
            </div>
            <div>
              <h1 class="h3 fw-bold mb-1 text-white">
                <c:out value="${profileUser.fullName != null && !profileUser.fullName.isBlank() ? profileUser.fullName : profileUser.username}" />
              </h1>
              <div class="d-flex flex-wrap align-items-center gap-2 small text-white">
                <span><i class="bi bi-person-badge me-1"></i>@<c:out value="${profileUser.username}" /></span>
                <span>&bull;</span>
                <span>
                  <c:choose>
                    <c:when test="${profileUser.role == 'ROLE_USER'}">Khách hàng</c:when>
                    <c:when test="${profileUser.role == 'ROLE_ADMIN'}">Quản trị viên</c:when>
                    <c:when test="${profileUser.role == 'ROLE_MANAGER'}">Quản lý điều hành</c:when>
                    <c:when test="${profileUser.role == 'ROLE_HUB_STAFF'}">Nhân viên Hub</c:when>
                    <c:when test="${profileUser.role == 'ROLE_SHIPPER'}">Nhân viên giao nhận</c:when>
                    <c:when test="${profileUser.role == 'ROLE_LINEHAUL_DRIVER'}">Tài xế trung chuyển</c:when>
                    <c:otherwise><c:out value="${profileUser.role}" /></c:otherwise>
                  </c:choose>
                </span>
                <span>&bull;</span>
                <span>Tham gia: <c:out value="${profileUser.createdAt}" /></span>
              </div>
            </div>
          </div>

          <!-- Quick badges -->
          <div class="d-flex flex-wrap gap-2">
            <c:choose>
              <c:when test="${profileUser.phoneVerified}">
                <span class="badge bg-success bg-opacity-25 text-success-emphasis border border-success border-opacity-50 px-3 py-2 rounded-pill d-inline-flex align-items-center gap-1">
                  Đã xác thực SĐT
                </span>
              </c:when>
              <c:otherwise>
                <span class="badge bg-warning bg-opacity-25 text-warning-emphasis border border-warning border-opacity-50 px-3 py-2 rounded-pill d-inline-flex align-items-center gap-1">
                  <i class="bi bi-exclamation-triangle-fill text-warning"></i> SĐT chưa xác thực
                </span>
              </c:otherwise>
            </c:choose>

            <c:choose>
              <c:when test="${profileUser.emailVerified}">
                <span class="badge bg-success bg-opacity-25 text-success-emphasis border border-success border-opacity-50 px-3 py-2 rounded-pill d-inline-flex align-items-center gap-1">
                  Đã xác thực Email
                </span>
              </c:when>
              <c:otherwise>
                <span class="badge bg-warning bg-opacity-25 text-warning-emphasis border border-warning border-opacity-50 px-3 py-2 rounded-pill d-inline-flex align-items-center gap-1">
                  <i class="bi bi-exclamation-triangle-fill text-warning"></i> Email chưa xác thực
                </span>
              </c:otherwise>
            </c:choose>

            <c:if test="${not empty profileUser.googleSubject}">
              <span class="badge bg-light text-dark px-3 py-2 rounded-pill d-inline-flex align-items-center gap-1">
                <i class="bi bi-google text-danger"></i> Google Linked
              </span>
            </c:if>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content Tabs / Sections -->
    <div class="row g-4">

      <!-- Navigation Column / Shortcuts -->
      <div class="col-lg-3">
        <div class="card border-0 shadow-sm rounded-4 sticky-top" style="top: 2rem;">
          <div class="list-group list-group-flush rounded-4 py-2">
            <a href="#info-section" class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3 border-0 active" id="tab-info-btn">
              <i class="bi bi-person-gear fs-5"></i>
              <span class="fw-semibold">Thông tin chung</span>
            </a>
            <a href="#phone-section" class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3 border-0" id="tab-phone-btn">
              <i class="bi bi-telephone fs-5"></i>
              <div class="flex-grow-1">
                <div class="fw-semibold">Số điện thoại</div>
                <div class="small text-muted">SMS OTP</div>
              </div>
              <c:choose>
                <c:when test="${profileUser.phoneVerified}"><i class="bi bi-check-circle-fill text-success"></i></c:when>
                <c:otherwise><i class="bi bi-exclamation-circle-fill text-warning"></i></c:otherwise>
              </c:choose>
            </a>
            <a href="#email-section" class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3 border-0" id="tab-email-btn">
              <i class="bi bi-envelope-at fs-5"></i>
              <div class="flex-grow-1">
                <div class="fw-semibold">Email</div>
                <div class="small text-muted">Email OTP</div>
              </div>
              <c:choose>
                <c:when test="${profileUser.emailVerified}"><i class="bi bi-check-circle-fill text-success"></i></c:when>
                <c:otherwise><i class="bi bi-exclamation-circle-fill text-warning"></i></c:otherwise>
              </c:choose>
            </a>
            <a href="#security-section" class="list-group-item list-group-item-action d-flex align-items-center gap-3 py-3 border-0" id="tab-security-btn">
              <i class="bi bi-shield-lock fs-5"></i>
              <span class="fw-semibold">Đổi mật khẩu</span>
            </a>
          </div>
        </div>
      </div>

      <!-- Detail Panels Column -->
      <div class="col-lg-9 d-flex flex-column gap-4">

        <!-- 1. THÔNG TIN CHUNG -->
        <section id="info-section" class="card border-0 shadow-sm rounded-4">
          <div class="card-header bg-transparent border-0 pt-4 px-4 pb-0">
            <h2 class="h5 fw-bold mb-1 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-person-lines-fill text-primary"></i> Thông tin tài khoản
            </h2>
            <p class="text-muted small mb-0">Cập nhật họ tên hiển thị và xem thông tin tài khoản.</p>
          </div>
          <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/user/profile/update-info" method="post">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label fw-semibold text-secondary small">Tên đăng nhập (Username)</label>
                  <input type="text" class="form-control bg-light" value="${fn:escapeXml(profileUser.username)}" readonly disabled />
                  <div class="form-text">Tên đăng nhập không thể thay đổi sau khi đăng ký.</div>
                </div>

                <div class="col-md-6">
                  <label for="fullName" class="form-label fw-semibold text-dark small">Họ và tên <span class="text-danger">*</span></label>
                  <input type="text" id="fullName" name="fullName" class="form-control"
                         value="${fn:escapeXml(profileUser.fullName)}" required minlength="2" maxlength="100" />
                  <div class="form-text">Đây là tên sẽ hiển thị trong hệ thống và trên phiếu gửi hàng của bạn.</div>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-semibold text-secondary small">Vai trò người dùng</label>
                  <input type="text" class="form-control bg-light" value="${profileUser.role}" readonly disabled />
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-semibold text-secondary small">Trạng thái tài khoản</label>
                  <div>
                    <span class="badge bg-success text-success bg-opacity-10 fs-6 fw-semibold px-3 py-2 rounded-3">
                      <i class="bi me-1"></i><c:out value="${profileUser.status}" />
                    </span>
                  </div>
                </div>
              </div>

              <div class="mt-4 pt-2 border-top d-flex justify-content-end">
                <button type="submit" class="btn btn-primary rounded-pill px-4">
                  <i class="bi bi-save me-1"></i> Lưu thay đổi
                </button>
              </div>
            </form>
          </div>
        </section>

        <!-- 2. SỐ ĐIỆN THOẠI & SMS OTP -->
        <section id="phone-section" class="card border-0 shadow-sm rounded-4">
          <div class="card-header bg-transparent border-0 pt-4 px-4 pb-0">
            <div class="d-flex align-items-center justify-content-between">
              <h2 class="h5 fw-bold mb-1 text-dark d-flex align-items-center gap-2">
                <i class="bi bi-phone text-success"></i> Số điện thoại
              </h2>
              <span class="badge ${profileUser.phoneVerified ? 'bg-success' : 'bg-warning'} px-3 py-2 rounded-pill">
                ${profileUser.phoneVerified ? 'Đã xác thực' : 'Chưa xác thực'}
              </span>
            </div>
            <p class="text-muted small mb-0">Xác thực số điện thoại để đăng nhập an toàn, nhận mã OTP và liên kết các đơn hàng của bạn.</p>
          </div>

          <div class="card-body p-4">
            <!-- Current Phone Info -->
            <div class="p-3 bg-light rounded-4 mb-4 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
              <div>
                <span class="text-secondary small d-block">Số điện thoại hiện tại:</span>
                <span class="fs-5 fw-bold text-dark font-monospace"><c:out value="${profileUser.phone}" /></span>
                <c:if test="${profileUser.phoneVerified && not empty profileUser.phoneVerifiedAt}">
                  <span class="text-success small d-block"><i class="bi bi-check2-circle"></i> Đã xác thực lúc: <c:out value="${profileUser.phoneVerifiedAt}"/></span>
                </c:if>
              </div>

              <c:if test="${!profileUser.phoneVerified}">
                <form action="${pageContext.request.contextPath}/user/profile/phone/send-otp" method="post" class="m-0">
                  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                  <input type="hidden" name="phone" value="${profileUser.phone}" />
                  <button type="submit" class="btn btn-warning rounded-pill px-3 fw-semibold">
                    <i class="bi bi-shield-check me-1"></i> Gửi OTP xác thực số này
                  </button>
                </form>
              </c:if>
            </div>

            <!-- Active Phone Challenge Input Box -->
            <c:if test="${not empty phoneChallengeId}">
              <div class="card border-warning border-2 bg-warning bg-opacity-10 rounded-4 p-4 mb-4">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <h3 class="h6 fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                    <i class="bi bi-chat-left-dots-fill text-warning"></i> Nhập mã OTP SMS xác nhận
                  </h3>
                  <span class="badge bg-warning text-dark">Hiệu lực 5 phút</span>
                </div>
                <p class="text-muted small mb-3">
                  Mã OTP 6 số đã được tạo cho số: <strong class="text-dark"><c:out value="${phoneTarget}" /></strong>.<br>
                </p>

                <form action="${pageContext.request.contextPath}/user/profile/phone/verify-otp" method="post" class="row g-2 align-items-center">
                  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                  <div class="col-sm-6 col-md-5">
                    <input type="text" name="code" class="form-control form-control-lg text-center font-monospace fw-bold tracking-wider"
                           placeholder="000000" maxlength="6" pattern="[0-9]{6}" required autofocus />
                  </div>
                  <div class="col-sm-6 col-md-4">
                    <button type="submit" class="btn btn-success btn-lg w-100 rounded-pill fw-semibold">
                      <i class="bi bi-check2-circle me-1"></i> Xác thực ngay
                    </button>
                  </div>
                </form>

                <div class="mt-3">
                  <form action="${pageContext.request.contextPath}/user/profile/phone/resend-otp" method="post" class="d-inline">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <button type="submit" class="btn btn-link text-secondary text-decoration-none p-0 small">
                      <i class="bi bi-arrow-clockwise"></i> Chưa nhận được mã? Gửi lại OTP
                    </button>
                  </form>
                </div>
              </div>
            </c:if>

            <!-- Update Phone Form -->
            <div class="border rounded-4 p-4">
              <h3 class="h6 fw-bold text-dark mb-2">
                <i class="bi bi-arrow-repeat text-primary me-1"></i> Đổi hoặc cập nhật số điện thoại mới
              </h3>
              <p class="text-muted small mb-3">Hệ thống sẽ gửi mã OTP đến số điện thoại mới. Sau khi nhập đúng mã OTP, số điện thoại mới sẽ được cập nhật và đánh dấu là đã xác thực.</p>

              <form action="${pageContext.request.contextPath}/user/profile/phone/send-otp" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="row g-2">
                  <div class="col-md-7">
                    <div class="input-group">
                      <span class="input-group-text bg-white"><i class="bi bi-telephone text-muted"></i></span>
                      <input type="tel" name="phone" class="form-control" placeholder="VD: 0987654321 hoặc +84987654321" required
                             pattern="^(0|\+84)[35789][0-9]{8}$" title="Số điện thoại di động Việt Nam 10 chữ số (03, 05, 07, 08, 09)" />
                    </div>
                  </div>
                  <div class="col-md-5">
                    <button type="submit" class="btn btn-outline-primary rounded-pill w-100">
                      <i class="bi bi-send me-1"></i> Gửi mã OTP xác nhận
                    </button>
                  </div>
                </div>
                <div class="form-text mt-2">Hỗ trợ các đầu số Viettel, Mobifone, Vinaphone, Vietnamobile, Itelecom (dạng 03x, 05x, 07x, 08x, 09x).</div>
              </form>
            </div>
          </div>
        </section>

        <!-- 3. EMAIL & EMAIL OTP -->
        <section id="email-section" class="card border-0 shadow-sm rounded-4">
          <div class="card-header bg-transparent border-0 pt-4 px-4 pb-0">
            <div class="d-flex align-items-center justify-content-between">
              <h2 class="h5 fw-bold mb-1 text-dark d-flex align-items-center gap-2">
                <i class="bi bi-envelope-check text-info"></i> Email
              </h2>
              <span class="badge ${profileUser.emailVerified ? 'bg-success' : 'bg-warning'} px-3 py-2 rounded-pill">
                ${profileUser.emailVerified ? 'Đã xác thực' : 'Chưa xác thực'}
              </span>
            </div>
            <p class="text-muted small mb-0">Xác thực email để nhận thông báo hành trình đơn hàng, biên lai thanh toán và khôi phục tài khoản khi quên mật khẩu.</p>
          </div>

          <div class="card-body p-4">
            <!-- Current Email Info -->
            <div class="p-3 bg-light rounded-4 mb-4 d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
              <div>
                <span class="text-secondary small d-block">Email hiện tại:</span>
                <span class="fs-5 fw-bold text-dark"><c:out value="${profileUser.email}" /></span>
                <c:if test="${profileUser.emailVerified && not empty profileUser.emailVerifiedAt}">
                  <span class="text-success small d-block"><i class="bi bi-check2-circle"></i> Đã xác thực lúc: <c:out value="${profileUser.emailVerifiedAt}"/></span>
                </c:if>
              </div>

              <c:if test="${!profileUser.emailVerified}">
                <form action="${pageContext.request.contextPath}/user/profile/email/send-otp" method="post" class="m-0">
                  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                  <input type="hidden" name="email" value="${profileUser.email}" />
                  <button type="submit" class="btn btn-warning rounded-pill px-3 fw-semibold">
                    <i class="bi bi-shield-check me-1"></i> Gửi OTP xác thực email này
                  </button>
                </form>
              </c:if>
            </div>

            <!-- Active Email Challenge Input Box -->
            <c:if test="${not empty emailChallengeId}">
              <div class="card border-info border-2 bg-info bg-opacity-10 rounded-4 p-4 mb-4">
                <div class="d-flex align-items-center justify-content-between mb-2">
                  <h3 class="h6 fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                    <i class="bi bi-mailbox2 text-info"></i> Nhập mã OTP Email xác nhận
                  </h3>
                  <span class="badge bg-info text-dark">Hiệu lực 5 phút</span>
                </div>
                <p class="text-muted small mb-3">
                  Mã OTP đã được gửi đến địa chỉ: <strong class="text-dark"><c:out value="${emailTarget}" /></strong>.<br>
                  Vui lòng kiểm tra hộp thư đến (inbox) hoặc thư rác (Spam/Junk).
                </p>

                <form action="${pageContext.request.contextPath}/user/profile/email/verify-otp" method="post" class="row g-2 align-items-center">
                  <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                  <div class="col-sm-6 col-md-5">
                    <input type="text" name="code" class="form-control form-control-lg text-center font-monospace fw-bold tracking-wider"
                           placeholder="000000" maxlength="6" pattern="[0-9]{6}" required autofocus />
                  </div>
                  <div class="col-sm-6 col-md-4">
                    <button type="submit" class="btn btn-success btn-lg w-100 rounded-pill fw-semibold">
                      <i class="bi bi-check2-circle me-1"></i> Xác thực ngay
                    </button>
                  </div>
                </form>

                <div class="mt-3">
                  <form action="${pageContext.request.contextPath}/user/profile/email/resend-otp" method="post" class="d-inline">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                    <button type="submit" class="btn btn-link text-secondary text-decoration-none p-0 small">
                      <i class="bi bi-arrow-clockwise"></i> Chưa nhận được email? Gửi lại mã OTP
                    </button>
                  </form>
                </div>
              </div>
            </c:if>

            <!-- Update Email Form -->
            <div class="border rounded-4 p-4">
              <h3 class="h6 fw-bold text-dark mb-2">
                <i class="bi bi-envelope-plus text-primary me-1"></i> Đổi hoặc cập nhật email mới
              </h3>
              <p class="text-muted small mb-3">Hệ thống sẽ gửi email chứa mã OTP xác thực tới địa chỉ mới. Sau khi nhập đúng mã OTP, email tài khoản sẽ được cập nhật.</p>

              <form action="${pageContext.request.contextPath}/user/profile/email/send-otp" method="post">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                <div class="row g-2">
                  <div class="col-md-7">
                    <div class="input-group">
                      <span class="input-group-text bg-white"><i class="bi bi-envelope text-muted"></i></span>
                      <input type="email" name="email" class="form-control" placeholder="VD: yourname@gmail.com" required maxlength="100" />
                    </div>
                  </div>
                  <div class="col-md-5">
                    <button type="submit" class="btn btn-outline-primary rounded-pill w-100">
                      <i class="bi bi-send me-1"></i> Gửi mã OTP xác thực
                    </button>
                  </div>
                </div>
                <div class="form-text mt-2">Mã OTP sẽ được gửi từ máy chủ Gmail của UTEExpress qua cổng kết nối bảo mật TLS.</div>
              </form>
            </div>
          </div>
        </section>

        <!-- 4. BẢO MẬT & ĐỔI MẬT KHẨU -->
        <section id="security-section" class="card border-0 shadow-sm rounded-4 ${not empty passwordError ? 'border-danger border-2' : ''}">
          <div class="card-header bg-transparent border-0 pt-4 px-4 pb-0">
            <h2 class="h5 fw-bold mb-1 text-dark d-flex align-items-center gap-2">
              <i class="bi bi-shield-lock-fill text-danger"></i> Đổi mật khẩu đăng nhập
            </h2>
            <p class="text-muted small mb-0">Đổi mật khẩu định kỳ để nâng cao tính bảo mật cho tài khoản.</p>
          </div>
          <div class="card-body p-4">
            <form action="${pageContext.request.contextPath}/user/profile/change-password" method="post">
              <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />

              <div class="row g-3">
                <div class="col-12">
                  <label for="currentPassword" class="form-label fw-semibold text-dark small">Mật khẩu hiện tại <span class="text-danger">*</span></label>
                  <input type="password" id="currentPassword" name="currentPassword" class="form-control ${not empty passwordError ? 'is-invalid' : ''}" required />
                </div>

                <div class="col-md-6">
                  <label for="newPassword" class="form-label fw-semibold text-dark small">Mật khẩu mới <span class="text-danger">*</span></label>
                  <input type="password" id="newPassword" name="newPassword" class="form-control ${not empty passwordError ? 'is-invalid' : ''}" required minlength="8" maxlength="72" />
                  <div class="form-text">Tối thiểu 8 ký tự, tối đa 72 byte.</div>
                </div>

                <div class="col-md-6">
                  <label for="confirmPassword" class="form-label fw-semibold text-dark small">Xác nhận mật khẩu mới <span class="text-danger">*</span></label>
                  <input type="password" id="confirmPassword" name="confirmPassword" class="form-control ${not empty passwordError ? 'is-invalid' : ''}" required minlength="8" maxlength="72" />
                </div>
              </div>

              <c:if test="${not empty passwordError}">
                <div class="col-12 mt-3">
                  <div class="alert alert-danger alert-dismissible fade show p-3" role="alert">
                    <i class="bi bi-exclamation-circle-fill me-2"></i>
                    <c:out value="${passwordError}" />
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>
                  </div>
                </div>
              </c:if>

              <div class="mt-4 pt-2 border-top d-flex justify-content-end">
                <button type="submit" class="btn btn-danger rounded-pill px-4">
                  <i class="bi bi-key me-1"></i> Cập nhật mật khẩu mới
                </button>
              </div>
            </form>
          </div>
        </section>

      </div>
    </div>

  </div>
</main>

<jsp:include page="../layout/footer.jsp" />

