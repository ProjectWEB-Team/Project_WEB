<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="ie=edge">
    <title><c:out value="${pageTitle}" default="UTEExpress - Hệ thống Quản lý Chuỗi Giao nhận Logistics" /></title>

    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">

    <!-- Bootstrap 5.3.3 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Bootstrap Icons 1.11.3 -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <!-- FontAwesome 6.5.1 -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">

    <!-- UTEExpress Logistics Brand CSS -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/ute-express.css">
</head>
<body>

    <!-- Topbar Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-ute-primary sticky-top shadow-sm py-2">
        <div class="container-fluid px-lg-4">
            <!-- Brand Logo -->
            <a class="navbar-brand d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/">
                <div class="bg-ute-accent text-white rounded-2 d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                    <i class="bi bi-box-seam-fill fs-5"></i>
                </div>
                <div>
                    <span class="fw-bold fs-5 tracking-wide text-white">UTE<span class="text-ute-accent">EXPRESS</span></span>
                    <span class="d-block small text-light opacity-75" style="font-size: 0.68rem; margin-top: -3px;">HCMUTE LOGISTICS SYSTEM</span>
                </div>
            </a>

            <!-- Mobile Toggle -->
            <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain">
                <span class="navbar-toggler-icon"></span>
            </button>

            <!-- Navbar Links -->
            <div class="collapse navbar-collapse" id="navbarMain">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3">
                    <li class="nav-item">
                        <a class="nav-link active" href="${pageContext.request.contextPath}/">
                            <i class="bi bi-house-door me-1"></i>Trang chủ
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-white opacity-90" href="${pageContext.request.contextPath}/tracking">
                            <i class="bi bi-search me-1"></i>Tra cứu vận đơn
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-white opacity-90" href="${pageContext.request.contextPath}/calculator">
                            <i class="bi bi-calculator me-1"></i>Ước tính cước phí
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-white opacity-90" href="${pageContext.request.contextPath}/network">
                            <i class="bi bi-diagram-3 me-1"></i>Mạng lưới Bưu cục
                        </a>
                    </li>
                </ul>

                <!-- Right Actions -->
                <div class="d-flex align-items-center gap-2">
                    <a href="${pageContext.request.contextPath}/user/shipments/create" class="btn btn-ute-accent btn-sm px-3 rounded-pill">
                        <i class="bi bi-plus-circle me-1"></i>Tạo đơn nhanh
                    </a>
                    <a href="${pageContext.request.contextPath}/login" class="btn btn-outline-light btn-sm px-3 rounded-pill">
                        <i class="bi bi-person me-1"></i>Đăng nhập
                    </a>
                </div>
            </div>
        </div>
    </nav>
