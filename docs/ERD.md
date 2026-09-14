# UTEExpress Entity-Relationship Diagrams (ERD)

Do hệ thống có quy mô lớn với 24 bảng, việc dồn tất cả vào một sơ đồ duy nhất sẽ tạo ra một mạng nhện rất khó đọc. Vì vậy, tôi đã phân chia ERD thành **4 cụm chức năng (Modules)** chuẩn hóa theo tài liệu thiết kế mới nhất `UTEExpress_Workflow.md`.

## 1. Core Module (Người dùng & Cơ sở hạ tầng)
Quản lý thông tin người dùng, nhân viên, phân quyền, bảng ví điện tử độc lập và hệ thống phân cấp Hub.

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR username
        VARCHAR password
        VARCHAR full_name
        VARCHAR phone
        VARCHAR email
        VARCHAR role
        VARCHAR status
    }
    
    wallets {
        BIGINT id PK
        BIGINT user_id FK
        DECIMAL balance
        DECIMAL locked_balance
        VARCHAR status
    }

    hubs {
        BIGINT id PK
        VARCHAR code
        VARCHAR name
        INT level
        VARCHAR type
        BIGINT parent_hub_id FK
        VARCHAR region
        VARCHAR province
        VARCHAR district
        VARCHAR address
    }
    
    employee_profiles {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR employee_code
        BIGINT hub_id FK
        VARCHAR position
        VARCHAR status
    }

    pricing_policies {
        BIGINT id PK
        VARCHAR service_code
        VARCHAR from_region
        VARCHAR to_region
        DECIMAL base_price
    }

    users ||--o| wallets : "has"
    users ||--o{ employee_profiles : "acts as"
    hubs ||--o{ hubs : "parent of (tree)"
    hubs ||--o{ employee_profiles : "employs"
```

---

## 2. Shipment & Tracking Module (Vận đơn & Theo dõi)
Lưu trữ thông tin vận đơn với 13 State Machine chuẩn, cờ direction, cờ on_hold, định tuyến dự kiến, lịch sử quét mã (Events), phân công Shipper và các nỗ lực (Attempts).

```mermaid
erDiagram
    shipments {
        BIGINT id PK
        VARCHAR tracking_number
        BIGINT sender_user_id FK
        BIGINT origin_hub_id FK
        BIGINT current_hub_id FK
        BIGINT destination_hub_id FK
        VARCHAR direction
        VARCHAR current_status
        BOOLEAN on_hold
        VARCHAR fail_reason
        VARCHAR cancel_reason
        VARCHAR closure_reason
    }

    shipment_routes {
        BIGINT id PK
        BIGINT shipment_id FK
        BIGINT hub_id FK
        INT sequence_number
        VARCHAR status
    }

    shipment_events {
        BIGINT id PK
        BIGINT shipment_id FK
        BIGINT hub_id FK
        BIGINT actor_user_id FK
        VARCHAR status
    }

    shipment_assignments {
        BIGINT id PK
        BIGINT shipment_id FK
        BIGINT assignee_user_id FK
        VARCHAR assignment_type
        VARCHAR status
    }

    pickup_attempts {
        BIGINT id PK
        BIGINT assignment_id FK
        BIGINT shipment_id FK
        VARCHAR result
    }

    delivery_attempts {
        BIGINT id PK
        BIGINT assignment_id FK
        BIGINT shipment_id FK
        VARCHAR result
    }

    shipments ||--o{ shipment_routes : "routed via"
    shipments ||--o{ shipment_events : "tracked by"
    shipments ||--o{ shipment_assignments : "assigned via"
    shipment_assignments ||--o{ pickup_attempts : "results in"
    shipment_assignments ||--o{ delivery_attempts : "results in"
    shipments ||--o{ pickup_attempts : "has"
    shipments ||--o{ delivery_attempts : "has"
```

---

## 3. Linehaul & Operations Module (Vận hành & Trung chuyển)
Quản lý đóng bao (Bagging/Containers), chuyến xe (Trips), bàn giao (Handovers) giữa các trạm, và các sự cố (Incidents).

```mermaid
erDiagram
    vehicles {
        BIGINT id PK
        VARCHAR plate_number
        VARCHAR vehicle_type
    }

    transport_trips {
        BIGINT id PK
        VARCHAR trip_code
        BIGINT origin_hub_id FK
        BIGINT destination_hub_id FK
        BIGINT driver_user_id FK
        BIGINT vehicle_id FK
        VARCHAR status
    }

    parcel_containers {
        BIGINT id PK
        VARCHAR container_code
        BIGINT origin_hub_id FK
        BIGINT destination_hub_id FK
        VARCHAR status
    }

    container_items {
        BIGINT container_id PK,FK
        BIGINT shipment_id PK,FK
        VARCHAR status
    }

    handovers {
        BIGINT id PK
        BIGINT from_hub_id FK
        BIGINT to_hub_id FK
        BIGINT sender_employee_id FK
        BIGINT receiver_driver_id FK
        BIGINT transport_trip_id FK
        INT expected_containers
        INT accepted_containers
        VARCHAR status
    }

    handover_containers {
        BIGINT id PK
        BIGINT handover_id FK
        BIGINT container_id FK
        VARCHAR status
    }
    
    incidents {
        BIGINT id PK
        BIGINT shipment_id FK
        BIGINT trip_id FK
        VARCHAR type
        VARCHAR status
    }

    operation_reports {
        BIGINT id PK
        BIGINT hub_id FK
        DATE report_date
        INT inbound_count
    }

    vehicles ||--o{ transport_trips : "used for"
    transport_trips ||--o{ handovers : "transports"
    handovers ||--o{ handover_containers : "includes"
    parcel_containers ||--o{ handover_containers : "handled in"
    parcel_containers ||--o{ container_items : "contains"
    transport_trips ||--o{ incidents : "encounters"
```

---

## 4. Financial & Payment Module (Tài chính & Dòng tiền)
Toàn bộ hệ thống quản lý phí vận chuyển, thu hộ COD (phân biệt tiền mặt vs chuyển khoản), đối soát ca cho Shipper (Reconciliation) và lịch sử giao dịch Ví điện tử (Wallet Ledger).

```mermaid
erDiagram
    payments {
        BIGINT id PK
        BIGINT shipment_id FK
        VARCHAR payment_type
        DECIMAL amount
        VARCHAR method
        VARCHAR status
    }

    cod_transactions {
        BIGINT id PK
        BIGINT shipment_id FK
        BIGINT collected_by_shipper_id FK
        DECIMAL amount
        VARCHAR method
        VARCHAR status
        TIMESTAMP payout_at
    }

    shipper_reconciliations {
        BIGINT id PK
        BIGINT shipper_id FK
        BIGINT manager_id FK
        DECIMAL expected_amount
        DECIMAL actual_amount
        VARCHAR status
    }

    shipper_reconciliation_items {
        BIGINT id PK
        BIGINT reconciliation_id FK
        BIGINT shipment_id FK
        DECIMAL expected_amount
        DECIMAL actual_amount
    }

    withdrawal_requests {
        BIGINT id PK
        BIGINT user_id FK
        DECIMAL amount
        VARCHAR status
    }

    wallet_transactions {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR transaction_type
        DECIMAL amount
        BIGINT reference_id
        VARCHAR reference_type
    }

    shipper_reconciliations ||--o{ shipper_reconciliation_items : "contains"
```
