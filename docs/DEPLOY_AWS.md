# Triển khai UTEExpress lên AWS (EC2 + RDS SQL Server)

Mô hình: trình duyệt → **Nginx (HTTPS, domain của bạn)** → **ứng dụng Spring Boot trên EC2** → **Amazon RDS for SQL Server Express**.
Schema database do **Flyway** tự tạo/cập nhật khi ứng dụng khởi động (`src/main/resources/db/migration`), không cần chạy file SQL bằng tay.

Thời gian làm lần đầu: khoảng 1–2 giờ. Làm đúng thứ tự từ bước 1.

---

## 0. Flyway hoạt động thế nào (đọc 1 phút)

- Mỗi file `V<số>__<mô tả>.sql` trong `db/migration` là một bước thay đổi schema. `V1__baseline.sql` là toàn bộ bảng gốc (lấy từ `docs/schema.sql`).
- Khi app khởi động, Flyway xem bảng `flyway_schema_history` trong DB: DB đang ở bước mấy, rồi chạy nốt các bước còn thiếu theo thứ tự.
- DB cũ chưa có `flyway_schema_history` (ví dụ `ute_express` dựng tay trước đây) được đánh dấu là đã có V1, rồi Flyway chạy V2 trở đi. Các script V2+ chạy lại an toàn, không xoá dữ liệu.
- Thêm thay đổi schema mới: tạo file `V18__mo_ta.sql` (số tiếp theo). **Không sửa file V đã chạy.**
- Dữ liệu mẫu (bảng giá demo `db/demo/R__demo_pricing.sql`) chỉ nạp khi bật profile `demo`. Production không bật `demo`.

## 1. Chạy trên máy cá nhân (Windows)

Chỉ cần một database trống, ví dụ tạo `ute_express_v2` trong SQL Server Management Studio, cấp quyền `db_owner` cho login của bạn, rồi trỏ `spring.datasource.url` trong `application-local.properties` tới DB đó. Khởi động app với profile `local,otp-local,demo`: Flyway tự dựng 30 bảng và nạp bảng giá demo.

Chạy SQL Server bằng Docker (không bắt buộc):

```powershell
$env:MSSQL_SA_PASSWORD = "Mat-khau-manh-123"
docker compose up -d
docker exec -it project_web-sqlserver-1 /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P $env:MSSQL_SA_PASSWORD -C -Q "CREATE DATABASE ute_express"
```

## 2. Tạo database RDS for SQL Server

AWS Console → chọn region gần Việt Nam, ví dụ **Asia Pacific (Singapore) ap-southeast-1**. Dùng **cùng region** cho RDS và EC2.

1. **RDS → Create database**
   - Creation method: **Standard create**
   - Engine: **Microsoft SQL Server**, Edition: **SQL Server Express Edition**
   - Templates: **Free tier** (nếu có)
   - DB instance identifier: `uteexpress-db`
   - Master username: `admin`, đặt mật khẩu mạnh và lưu lại
   - Instance class: `db.t3.micro`, Storage: 20 GB
   - Connectivity: **Public access: No**. VPC: Default VPC
   - VPC security group: **Create new**, tên `uteexpress-db-sg`
2. Bấm **Create database**, chờ trạng thái **Available** (10–20 phút). Ghi lại **Endpoint** (dạng `uteexpress-db.xxxx.ap-southeast-1.rds.amazonaws.com`).
3. Security group chỉ cho EC2 vào (làm sau khi có EC2 ở bước 3): **EC2 → Security Groups → uteexpress-db-sg → Edit inbound rules** → xoá rule cũ, thêm: Type **MSSQL** (1433), Source: security group của EC2 (`uteexpress-web-sg`).

## 3. Tạo máy chủ EC2

1. **EC2 → Launch instance**
   - Name: `uteexpress-web`
   - AMI: **Ubuntu Server 24.04 LTS**
   - Instance type: **t3.small** (2 GB RAM; t3.micro 1 GB dễ thiếu bộ nhớ)
   - Key pair: **Create new key pair** → tải file `.pem`, giữ kỹ
   - Network settings → **Create security group** `uteexpress-web-sg`:
     - SSH (22): **My IP**
     - HTTP (80): Anywhere
     - HTTPS (443): Anywhere
   - Storage: 20 GB
2. **Elastic IP**: EC2 → Elastic IPs → Allocate → Associate với `uteexpress-web`. Ghi lại IP này (IP cố định, không đổi khi khởi động lại).
3. Quay lại bước 2.3 để mở 1433 cho `uteexpress-web-sg`.

## 4. Trỏ domain ở TenTen về EC2

Đăng nhập TenTen → Quản lý tên miền → **Quản lý DNS** của domain:

| Loại | Tên (Host) | Giá trị | TTL |
|---|---|---|---|
| A | `@` | Elastic IP ở bước 3.2 | 300 |
| A | `www` | Elastic IP ở bước 3.2 | 300 |

Xoá các bản ghi A/CNAME cũ trùng `@` hoặc `www` nếu có. Kiểm tra sau 5–30 phút: `nslookup tenmiencuaban.vn` trả về đúng IP.

## 5. Cài đặt trên EC2

Kết nối từ Windows (PowerShell, trong thư mục chứa file `.pem`):

```powershell
ssh -i .\uteexpress.pem ubuntu@<ELASTIC_IP>
```

Trên EC2:

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install openjdk-21-jre-headless nginx certbot python3-certbot-nginx git maven
sudo useradd --system --create-home --home-dir /opt/uteexpress uteexpress
```

Công cụ `sqlcmd` để tạo database trên RDS (một lần):

```bash
curl -sSL https://packages.microsoft.com/keys/microsoft.asc | sudo tee /etc/apt/trusted.gpg.d/microsoft.asc
curl -sSL https://packages.microsoft.com/config/ubuntu/24.04/prod.list | sudo tee /etc/apt/sources.list.d/mssql-release.list
sudo apt update && sudo ACCEPT_EULA=Y apt -y install mssql-tools18 unixodbc-dev
/opt/mssql-tools18/bin/sqlcmd -S <RDS_ENDPOINT> -U admin -P '<MAT_KHAU_ADMIN>' -C -Q "
CREATE DATABASE ute_express;
CREATE LOGIN uteexpress_app WITH PASSWORD = '<MAT_KHAU_APP>';"
/opt/mssql-tools18/bin/sqlcmd -S <RDS_ENDPOINT> -U admin -P '<MAT_KHAU_ADMIN>' -C -d ute_express -Q "
CREATE USER uteexpress_app FOR LOGIN uteexpress_app;
ALTER ROLE db_owner ADD MEMBER uteexpress_app;"
```

## 6. Cấu hình bí mật (biến môi trường)

```bash
sudo cp deploy/uteexpress.env.example /etc/uteexpress.env   # hoặc tạo mới bằng nano
sudo nano /etc/uteexpress.env
sudo chmod 600 /etc/uteexpress.env
```

Điền: `UTEEXPRESS_SQLSERVER_URL` (thay `<rds-endpoint>`), `UTEEXPRESS_SQLSERVER_PASSWORD` (= `<MAT_KHAU_APP>`), `UTEEXPRESS_JWT_SECRET` (tạo bằng `openssl rand -base64 48`). File này **không** nằm trong Git.

## 7. Đưa ứng dụng lên và chạy như dịch vụ

Cách A (đơn giản nhất): build trên Windows rồi copy lên.

```powershell
mvn -B -DskipTests package
scp -i .\uteexpress.pem target\project-web-1.0.war ubuntu@<ELASTIC_IP>:/tmp/app.war
```

Trên EC2:

```bash
sudo install -o uteexpress -g uteexpress -m 0644 /tmp/app.war /opt/uteexpress/app.war
```

Cách B: clone repo trên EC2 (`git clone https://github.com/RVAN1102/Project_WEB.git`) và dùng `deploy/deploy.sh` mỗi lần cập nhật.

Cài service (file mẫu `deploy/uteexpress.service`):

```bash
sudo nano /etc/systemd/system/uteexpress.service   # dán nội dung deploy/uteexpress.service
sudo systemctl daemon-reload
sudo systemctl enable --now uteexpress
sudo journalctl -u uteexpress -f                    # xem log; chờ dòng "Started UteExpressApplication"
```

Lần khởi động đầu, log có các dòng `Migrating schema [dbo] to version "1 - baseline"` … `"17 - …"`: Flyway đang dựng database.

## 8. Nginx + HTTPS

```bash
sudo nano /etc/nginx/sites-available/uteexpress     # dán deploy/nginx-uteexpress.conf, thay example.com bằng domain
sudo ln -s /etc/nginx/sites-available/uteexpress /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d tenmiencuaban.vn -d www.tenmiencuaban.vn
```

Certbot tự cấu hình HTTPS và tự gia hạn chứng chỉ. Cấu hình có sẵn khối `/ws` để WebSocket (STOMP realtime) đi qua Nginx.

Mở `https://tenmiencuaban.vn` để kiểm tra.

## 9. Cập nhật phiên bản mới

1. Kéo code mới / build WAR mới (cách A hoặc `./deploy/deploy.sh`).
2. `sudo systemctl restart uteexpress`. Flyway tự chạy migration mới nếu có.

## 10. Lưu ý vận hành

- Profile production là `prod`: không bật `otp-local`, `demo`. Đăng ký bằng OTP điện thoại cần nhà cung cấp SMS thật; trước đó dùng tài khoản tạo sẵn hoặc đăng nhập Google (profile `google`, cần client ID/secret).
- Tài khoản nhân viên (shipper, hub, tài xế, quản lý, admin) cần được tạo trong DB; không có trang tự đăng ký cho các vai trò này.
- File upload lưu ở `/opt/uteexpress/uploads`. Sao lưu RDS: bật **Automated backups** (mặc định 7 ngày).
- Lỗi thường gặp:
  - `Login failed` / timeout tới RDS: kiểm tra security group bước 2.3 và URL trong `/etc/uteexpress.env`.
  - `502 Bad Gateway`: app chưa chạy xong hoặc lỗi, xem `journalctl -u uteexpress -n 200`.
  - Certbot lỗi: domain chưa trỏ đúng IP (bước 4).
