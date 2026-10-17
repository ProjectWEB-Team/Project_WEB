-- UTEExpress Database Schema
-- Microsoft SQL Server version
-- Synchronized with UTEExpress_Workflow.md
-- Target DBMS: Microsoft SQL Server 2019+ / SQL Server 2022+
-- Unicode text columns use NVARCHAR.
-- Identity columns use IDENTITY(1,1).
-- Boolean flags use BIT.
-- Date/time columns use DATETIME2(0).

IF DB_ID(N'ute_express') IS NULL
BEGIN
    CREATE DATABASE [ute_express];
END;
GO

USE [ute_express];
GO

-- 1. users table (Chỉ lưu thông tin định danh và tài khoản)
CREATE TABLE dbo.users (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    username NVARCHAR(50) NOT NULL UNIQUE,
    password NVARCHAR(255) NOT NULL,
    full_name NVARCHAR(100) NOT NULL,
    phone NVARCHAR(20) NOT NULL UNIQUE,
    email NVARCHAR(100) NOT NULL UNIQUE,
    role NVARCHAR(50) NOT NULL, -- ROLE_USER, ROLE_SHIPPER, ROLE_HUB_STAFF, ROLE_LINEHAUL_DRIVER, ROLE_MANAGER, ROLE_ADMIN
    status NVARCHAR(20) NOT NULL, -- ACTIVE, INACTIVE, LOCKED
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
);

-- 1.1. wallets table (Bảng ví điện tử độc lập cho User/Sender nhận COD)
CREATE TABLE dbo.wallets (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(15,2) DEFAULT 0,
    locked_balance DECIMAL(15,2) DEFAULT 0,
    status NVARCHAR(20) NOT NULL, -- ACTIVE, LOCKED
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- 2. hubs table (Mạng lưới bưu cục và trung tâm khai thác)
CREATE TABLE dbo.hubs (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(20) NOT NULL UNIQUE,
    name NVARCHAR(100) NOT NULL,
    level INT NOT NULL, -- 1: DELIVERY_HUB, 2: REGIONAL_HUB, 3: SOC
    type NVARCHAR(50) NOT NULL, -- DELIVERY_HUB, REGIONAL_HUB, SOC
    parent_hub_id BIGINT,
    region NVARCHAR(50) NOT NULL, -- NORTH, CENTRAL, SOUTH
    province NVARCHAR(100),
    district NVARCHAR(100),
    address NVARCHAR(255),
    status NVARCHAR(20) NOT NULL, -- ACTIVE, INACTIVE
    FOREIGN KEY (parent_hub_id) REFERENCES dbo.hubs(id)
);

-- 3. employee_profiles table (Hồ sơ nhân viên gắn với Hub cơ sở)
CREATE TABLE dbo.employee_profiles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    employee_code NVARCHAR(50) NOT NULL UNIQUE,
    hub_id BIGINT,
    position NVARCHAR(50), -- INBOUND, SORTER, OUTBOUND, COORDINATOR, GENERAL_STAFF, PICKUP_SHIPPER, DELIVERY_SHIPPER, LINEHAUL_DRIVER
    status NVARCHAR(20) NOT NULL, -- ACTIVE, ON_LEAVE, RESIGNED
    FOREIGN KEY (user_id) REFERENCES dbo.users(id),
    FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id)
);

-- 4. pricing_policies (Biểu phí tính cước theo vùng miền)
CREATE TABLE dbo.pricing_policies (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    service_code NVARCHAR(50) NOT NULL, -- STANDARD, FAST, EXPRESS
    from_region NVARCHAR(50) NOT NULL, -- NORTH, CENTRAL, SOUTH
    to_region NVARCHAR(50) NOT NULL, -- NORTH, CENTRAL, SOUTH
    base_weight_g INT NOT NULL,
    base_price DECIMAL(15,2) NOT NULL,
    extra_weight_step_g INT NOT NULL,
    extra_weight_price DECIMAL(15,2) NOT NULL,
    insurance_rate_percent DECIMAL(5,2) DEFAULT 0,
    cod_fee_percent DECIMAL(5,2) DEFAULT 0,
    status NVARCHAR(20) NOT NULL, -- ACTIVE, INACTIVE
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    updated_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME()
);


-- Keep pricing_policies.updated_at synchronized after every UPDATE.
GO
CREATE OR ALTER TRIGGER dbo.trg_pricing_policies_updated_at
ON dbo.pricing_policies
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE pp
    SET pp.updated_at = SYSDATETIME()
    FROM dbo.pricing_policies AS pp
    INNER JOIN inserted AS i
        ON i.id = pp.id;
END;
GO

-- 5. shipments table (Vận đơn cốt lõi với 13 trạng thái chuẩn)
CREATE TABLE dbo.shipments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    tracking_number NVARCHAR(50) NOT NULL UNIQUE,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT,
    sender_name NVARCHAR(100) NOT NULL,
    sender_phone NVARCHAR(20) NOT NULL,
    sender_address NVARCHAR(255) NOT NULL,
    receiver_name NVARCHAR(100) NOT NULL,
    receiver_phone NVARCHAR(20) NOT NULL,
    receiver_address NVARCHAR(255) NOT NULL,
    receiver_province NVARCHAR(100),
    receiver_district NVARCHAR(100),
    pickup_method NVARCHAR(50) NOT NULL, -- HOME_PICKUP, SELF_DROPOFF
    fee_payer NVARCHAR(50) NOT NULL, -- SENDER, RECEIVER
    inspection_policy NVARCHAR(50) NOT NULL, -- INSPECTION_ONLY, TRIAL_ALLOWED, NO_INSPECTION
    weight INT,
    length INT,
    width INT,
    height INT,
    declared_value DECIMAL(15,2),
    shipping_fee DECIMAL(15,2),
    cod_amount DECIMAL(15,2),
    return_fee DECIMAL(15,2),
    payment_status NVARCHAR(50) NOT NULL, -- UNPAID, PENDING, PAID, FAILED, REFUNDED
    origin_hub_id BIGINT,
    destination_hub_id BIGINT,
    current_hub_id BIGINT,
    direction NVARCHAR(20) DEFAULT N'FORWARD', -- FORWARD, RETURN
    current_status NVARCHAR(50) NOT NULL, -- 13 States: CREATED, WAITING_FOR_PAYMENT, WAITING_FOR_HANDOVER, PICKED_UP, AT_HUB, IN_TRANSIT, OUT_FOR_DELIVERY, FAILED, DELIVERED, RETURN_TO_SENDER, RETURNED_TO_SENDER, CANCELLED, CLOSED_EXCEPTION
    on_hold BIT NOT NULL DEFAULT 0,
    fail_reason NVARCHAR(50), -- NO_ANSWER, PAYMENT_NOT_AVAILABLE, RESCHEDULED, OTHER, DELIVERY_ATTEMPTS_EXHAUSTED, RECEIVER_REFUSED, WRONG_ADDRESS
    cancel_reason NVARCHAR(255), -- SENDER_REQUEST, PAYMENT_TIMEOUT, PICKUP_TIMEOUT, WEIGHT_DISCREPANCY
    closure_reason NVARCHAR(50), -- LOST, DISPOSED
    payment_deadline DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    picked_up_at DATETIME2(0) NULL,
    delivered_at DATETIME2(0) NULL,
    returned_at DATETIME2(0) NULL,
    FOREIGN KEY (sender_user_id) REFERENCES dbo.users(id),
    FOREIGN KEY (receiver_user_id) REFERENCES dbo.users(id),
    FOREIGN KEY (origin_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (destination_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (current_hub_id) REFERENCES dbo.hubs(id)
);

-- 6. shipment_routes table (Lộ trình dự kiến qua các Hub)
CREATE TABLE dbo.shipment_routes (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    hub_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    status NVARCHAR(50) NOT NULL, -- PENDING, IN_TRANSIT, COMPLETED, SKIPPED
    arrival_time DATETIME2(0) NULL,
    departure_time DATETIME2(0) NULL,
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id)
);

-- 7. shipment_events table (Lịch sử quét mã và dòng thời gian Tracking)
CREATE TABLE dbo.shipment_events (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    hub_id BIGINT,
    actor_user_id BIGINT,
    status NVARCHAR(50) NOT NULL, -- INBOUND_SCAN, SORTING_COMPLETED, BAG_CREATED, OUTBOUND_SCANNED, ARRIVED_AT_HUB...
    description NVARCHAR(255),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (actor_user_id) REFERENCES dbo.users(id)
);

-- 8. shipment_assignments table (Phân công Shipper lấy/giao hàng)
CREATE TABLE dbo.shipment_assignments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    assignee_user_id BIGINT NOT NULL,
    assignment_type NVARCHAR(50) NOT NULL, -- PICKUP, DELIVERY, RETURN_DELIVERY
    status NVARCHAR(50) NOT NULL, -- ASSIGNED, ACCEPTED, REJECTED, IN_PROGRESS, COMPLETED, CANCELLED
    assigned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    accepted_at DATETIME2(0) NULL,
    completed_at DATETIME2(0) NULL,
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (assignee_user_id) REFERENCES dbo.users(id)
);

-- 9. pickup_attempts table (Nhật ký nỗ lực lấy hàng)
CREATE TABLE dbo.pickup_attempts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    shipper_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    attempt_time DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    result NVARCHAR(50) NOT NULL, -- SUCCESS, FAILED_NO_ANSWER, FAILED_SENDER_REFUSED, FAILED_CANCELLED, FAILED_OTHER
    note NVARCHAR(255),
    FOREIGN KEY (assignment_id) REFERENCES dbo.shipment_assignments(id),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (shipper_id) REFERENCES dbo.users(id)
);

-- 10. delivery_attempts table (Nhật ký nỗ lực giao hàng)
CREATE TABLE dbo.delivery_attempts (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    shipper_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    attempt_date DATE NOT NULL,
    attempt_time DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    rescheduled_date DATE,
    result NVARCHAR(50) NOT NULL, -- DELIVERED, NO_ANSWER, RECEIVER_REFUSED, PAYMENT_NOT_AVAILABLE, WRONG_ADDRESS, RESCHEDULED, OTHER
    note NVARCHAR(255),
    FOREIGN KEY (assignment_id) REFERENCES dbo.shipment_assignments(id),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (shipper_id) REFERENCES dbo.users(id)
);

-- 11. vehicles table (Phương tiện vận chuyển trung chuyển)
CREATE TABLE dbo.vehicles (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    plate_number NVARCHAR(20) NOT NULL UNIQUE,
    vehicle_type NVARCHAR(50) NOT NULL,
    capacity INT,
    status NVARCHAR(50) NOT NULL -- AVAILABLE, IN_USE, MAINTENANCE, INACTIVE
);

-- 12. transport_trips table (Chuyến xe Linehaul liên Hub)
CREATE TABLE dbo.transport_trips (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    trip_code NVARCHAR(50) NOT NULL UNIQUE,
    origin_hub_id BIGINT NOT NULL,
    destination_hub_id BIGINT NOT NULL,
    driver_user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    departure_time DATETIME2(0) NULL,
    arrival_time DATETIME2(0) NULL,
    status NVARCHAR(50) NOT NULL, -- SCHEDULED, DEPARTED, IN_TRANSIT, ARRIVED, COMPLETED, COMPLETED_WITH_DISCREPANCY, DELAYED, CANCELLED
    FOREIGN KEY (origin_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (destination_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (driver_user_id) REFERENCES dbo.users(id),
    FOREIGN KEY (vehicle_id) REFERENCES dbo.vehicles(id)
);

-- 13. parcel_containers table (Bao hàng / Túi gom chuyển tiếp)
CREATE TABLE dbo.parcel_containers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    container_code NVARCHAR(50) NOT NULL UNIQUE,
    origin_hub_id BIGINT NOT NULL,
    destination_hub_id BIGINT NOT NULL,
    status NVARCHAR(50) NOT NULL, -- OPEN, SEALED, LOADED, UNPACKED_FULL, UNPACKED_WITH_DISCREPANCY
    created_by BIGINT NOT NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (origin_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (destination_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (created_by) REFERENCES dbo.users(id)
);

-- 14. container_items table (Chi tiết kiện hàng nằm trong Bao)
CREATE TABLE dbo.container_items (
    container_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    status NVARCHAR(50) NOT NULL, -- IN_CONTAINER, RECEIVED, MISSING, ON_HOLD
    scanned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    PRIMARY KEY (container_id, shipment_id),
    FOREIGN KEY (container_id) REFERENCES dbo.parcel_containers(id),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id)
);

-- 15. handovers table (Phiếu bàn giao bao hàng cho tài xế)
CREATE TABLE dbo.handovers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    from_hub_id BIGINT NOT NULL,
    to_hub_id BIGINT NOT NULL,
    sender_employee_id BIGINT NOT NULL,
    receiver_driver_id BIGINT NOT NULL,
    transport_trip_id BIGINT NOT NULL,
    expected_containers INT NOT NULL DEFAULT 0,
    accepted_containers INT NOT NULL DEFAULT 0,
    status NVARCHAR(50) NOT NULL, -- PENDING_ACCEPTANCE, IN_TRANSIT, COMPLETED, COMPLETED_WITH_DISCREPANCY, REJECTED
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    accepted_at DATETIME2(0) NULL,
    completed_at DATETIME2(0) NULL,
    FOREIGN KEY (from_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (to_hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (sender_employee_id) REFERENCES dbo.employee_profiles(id),
    FOREIGN KEY (receiver_driver_id) REFERENCES dbo.users(id),
    FOREIGN KEY (transport_trip_id) REFERENCES dbo.transport_trips(id)
);

-- 16. handover_containers table (Danh sách bao hàng thuộc phiếu bàn giao)
CREATE TABLE dbo.handover_containers (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    handover_id BIGINT NOT NULL,
    container_id BIGINT NOT NULL,
    status NVARCHAR(50) NOT NULL, -- LOADED, DELIVERED, DISCREPANCY
    scanned_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (handover_id) REFERENCES dbo.handovers(id),
    FOREIGN KEY (container_id) REFERENCES dbo.parcel_containers(id)
);

-- 17. payments table (Giao dịch cước vận chuyển và phụ phí)
CREATE TABLE dbo.payments (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    payer_type NVARCHAR(50) NOT NULL, -- SENDER, RECEIVER
    payment_type NVARCHAR(50) NOT NULL, -- SHIPPING_FEE, RETURN_FEE, WEIGHT_SURCHARGE
    amount DECIMAL(15,2) NOT NULL,
    method NVARCHAR(50) NOT NULL, -- CASH, VNPAY, E_WALLET
    status NVARCHAR(50) NOT NULL, -- UNPAID, PENDING, PAID, FAILED, REFUNDED
    transaction_ref NVARCHAR(100),
    paid_at DATETIME2(0) NULL,
    refunded_at DATETIME2(0) NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id)
);

-- 18. cod_transactions table (Thu hộ và chi trả tiền COD)
CREATE TABLE dbo.cod_transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    collected_by_shipper_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    method NVARCHAR(50) NOT NULL, -- CASH, VNPAY, TRANSFER
    status NVARCHAR(50) NOT NULL, -- NOT_APPLICABLE, WAITING_COLLECTION, COLLECTED, RECONCILED, HOLD_DISPUTE, READY_FOR_PAYOUT, PAID_TO_SENDER, CANCELLED
    collected_at DATETIME2(0) NULL,
    reconciled_at DATETIME2(0) NULL,
    payout_at DATETIME2(0) NULL,
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (collected_by_shipper_id) REFERENCES dbo.users(id)
);

-- 19. shipper_reconciliations table (Phiếu đối soát kết ca của Shipper)
CREATE TABLE dbo.shipper_reconciliations (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipper_id BIGINT NOT NULL,
    manager_id BIGINT NOT NULL,
    reconciliation_date DATE NOT NULL,
    expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    actual_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    difference_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status NVARCHAR(50) NOT NULL, -- PENDING, COMPLETED, COMPLETED_WITH_DISCREPANCY
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    completed_at DATETIME2(0) NULL,
    FOREIGN KEY (shipper_id) REFERENCES dbo.users(id),
    FOREIGN KEY (manager_id) REFERENCES dbo.users(id)
);

-- 20. shipper_reconciliation_items table (Chi tiết từng đơn trong ca đối soát)
CREATE TABLE dbo.shipper_reconciliation_items (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    reconciliation_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    expected_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    actual_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    difference_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
    status NVARCHAR(50) NOT NULL, -- MATCHED, SHORTAGE, OVERAGE
    note NVARCHAR(255),
    FOREIGN KEY (reconciliation_id) REFERENCES dbo.shipper_reconciliations(id),
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id)
);

-- 21. withdrawal_requests table (Yêu cầu rút tiền từ Ví về Ngân hàng)
CREATE TABLE dbo.withdrawal_requests (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    bank_name NVARCHAR(100) NOT NULL,
    account_number NVARCHAR(50) NOT NULL,
    account_holder_name NVARCHAR(100) NOT NULL,
    status NVARCHAR(50) NOT NULL, -- PENDING, APPROVED, REJECTED, COMPLETED
    rejection_reason NVARCHAR(255),
    requested_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    approved_at DATETIME2(0) NULL,
    rejected_at DATETIME2(0) NULL,
    completed_at DATETIME2(0) NULL,
    FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- 22. wallet_transactions table (Sổ cái giao dịch Ví điện tử)
CREATE TABLE dbo.wallet_transactions (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type NVARCHAR(50) NOT NULL, -- COD_PAYOUT, SHIPPING_FEE_PAYMENT, RETURN_FEE_PAYMENT, WEIGHT_SURCHARGE_PAYMENT, REFUND, WITHDRAWAL_LOCK, WITHDRAWAL_DEDUCT, WITHDRAWAL_REFUND, COMPENSATION
    amount DECIMAL(15,2) NOT NULL,
    balance_before DECIMAL(15,2) NOT NULL,
    balance_after DECIMAL(15,2) NOT NULL,
    reference_id BIGINT NOT NULL,
    reference_type NVARCHAR(50) NOT NULL, -- SHIPMENT, WITHDRAWAL, INCIDENT
    description NVARCHAR(255),
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (user_id) REFERENCES dbo.users(id)
);

-- 23. incidents table (Ghi nhận sự cố vận hành)
CREATE TABLE dbo.incidents (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    shipment_id BIGINT,
    trip_id BIGINT,
    hub_id BIGINT,
    type NVARCHAR(50) NOT NULL, -- MISSING_PARCEL, DAMAGED_PARCEL, WRONG_ROUTE, TRIP_DELAY, MONEY_SHORTAGE, MONEY_OVERAGE, OTHER
    description NVARCHAR(500),
    reported_by BIGINT NOT NULL,
    status NVARCHAR(50) NOT NULL, -- OPEN, INVESTIGATING, RESOLVED, CLOSED
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    resolved_at DATETIME2(0) NULL,
    FOREIGN KEY (shipment_id) REFERENCES dbo.shipments(id),
    FOREIGN KEY (trip_id) REFERENCES dbo.transport_trips(id),
    FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (reported_by) REFERENCES dbo.users(id)
);

-- 24. operation_reports table (Báo cáo ca vận hành hàng ngày của Hub)
CREATE TABLE dbo.operation_reports (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    hub_id BIGINT NOT NULL,
    manager_user_id BIGINT NOT NULL,
    report_date DATE NOT NULL,
    inbound_count INT DEFAULT 0,
    outbound_count INT DEFAULT 0,
    delivered_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    backlog_count INT DEFAULT 0,
    missing_count INT DEFAULT 0,
    damaged_count INT DEFAULT 0,
    problem NVARCHAR(500),
    action NVARCHAR(500),
    status NVARCHAR(50) NOT NULL, -- DRAFT, SUBMITTED, ACKNOWLEDGED
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    FOREIGN KEY (hub_id) REFERENCES dbo.hubs(id),
    FOREIGN KEY (manager_user_id) REFERENCES dbo.users(id)
);
