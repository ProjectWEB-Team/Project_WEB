# Triển khai UTEExpress lên AWS (EC2 + RDS SQL Server)

Mô hình: trình duyệt → **Nginx (HTTPS, domain của bạn)** → **ứng dụng Spring Boot trên EC2** → **Amazon RDS for SQL Server Express**.
Schema database do **Flyway** tự tạo/cập nhật khi ứng dụng khởi động (`src/main/resources/db/migration`), không cần chạy file SQL bằng tay.

Website đang chạy: dùng [CI/CD với GitHub Actions](GITHUB_ACTIONS_CICD.md) để kiểm thử và đưa bản đã kiểm thử lên EC2. Hướng dẫn dưới đây dành cho việc dựng hạ tầng lần đầu.

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

## 2. Tạo máy chủ EC2

AWS Console → chọn một region (góc trên bên phải), ví dụ Singapore `ap-southeast-1` hoặc Sydney `ap-southeast-2`. Tạo RDS **cùng region với EC2**.

1. **EC2 → Launch instance**
   - Name: `uteexpress-web`
   - AMI: **Ubuntu Server 24.04 LTS**
   - Instance type: **t3.small** (2 GB RAM) hoặc **t3.micro** (1 GB, miễn phí; bắt buộc tạo swap ở mục 5 và không build trên server)
   - Key pair: **Create new key pair** → tải file `.pem`, giữ kỹ
   - Network settings → **Create security group** `uteexpress-web-sg`:
     - SSH (22): **My IP**
     - HTTP (80): Anywhere
     - HTTPS (443): Anywhere
   - Storage: 12 GB gp3
2. **Elastic IP**: EC2 → Elastic IPs → Allocate → Associate với `uteexpress-web`. Ghi lại IP này. Bắt buộc: IP public mặc định **đổi mỗi lần stop/start** máy, domain sẽ trỏ sai nếu không có Elastic IP.

## 3. Tạo database RDS for SQL Server

1. **RDS → Create database**
   - Creation method: **Standard create**
   - Engine: **Microsoft SQL Server**, Edition: **SQL Server Express Edition**
   - Templates: **Free tier** (nếu có)
   - DB instance identifier: `uteexpress-db`
   - Master username: `admin`, đặt mật khẩu mạnh và lưu lại
   - Instance class: `db.t3.micro`, Storage: 20 GB gp3 (mức tối thiểu của SQL Server), bỏ tick storage autoscaling
   - Connectivity: chọn **Connect to an EC2 compute resource** → chọn `uteexpress-web`. AWS tự đặt Public access **No** và tự tạo security group cho phép EC2 vào cổng 1433 (khi đó bỏ qua bước 3).
     (Cách thủ công: Don't connect, Public access **No**, VPC security group **Create new** `uteexpress-db-sg`, rồi làm bước 3.)
2. Bấm **Create database**, chờ trạng thái **Available** (10–20 phút). Ghi lại **Endpoint** (dạng `uteexpress-db.xxxx.<region>.rds.amazonaws.com`).
3. Chỉ với cách thủ công: **EC2 → Security Groups → uteexpress-db-sg → Edit inbound rules** → xoá rule cũ, thêm Type **MSSQL** (1433), Source: security group của EC2.

## 4. Trỏ domain ở TenTen về EC2

Đăng nhập TenTen → Quản lý tên miền → **Quản lý DNS** của domain:

| Loại | Tên (Host) | Giá trị | TTL |
|---|---|---|---|
| A | `@` | Elastic IP ở bước 2.2 | 300 |
| A | `www` | Elastic IP ở bước 2.2 | 300 |

Xoá các bản ghi A/CNAME cũ trùng `@` hoặc `www` nếu có. Kiểm tra sau 5–30 phút: `nslookup tenmiencuaban.vn` trả về đúng IP.

## 5. Cài đặt trên EC2

Kết nối từ Windows (PowerShell, trong thư mục chứa file `.pem`):

```powershell
ssh -i .\uteexpress.pem ubuntu@<ELASTIC_IP>
```

Trên EC2:

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install openjdk-21-jre-headless nginx certbot python3-certbot-nginx apache2-utils git maven
sudo useradd --system --create-home --home-dir /opt/uteexpress uteexpress
```

Máy **t3.micro (1 GB RAM)**: tạo swap 2 GB để Java không bị hết bộ nhớ (t3.small có thể bỏ qua):

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h        # dòng Swap hiện 2.0Gi là được
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

Điền: `UTEEXPRESS_SQLSERVER_URL` (thay `<rds-endpoint>`), `UTEEXPRESS_SQLSERVER_PASSWORD` (= `<MAT_KHAU_APP>`), `UTEEXPRESS_JWT_SECRET` (tạo bằng `openssl rand -base64 48`), `SPRING_PROFILES_ACTIVE`. File này **không** nằm trong Git.

- Giai đoạn test: `SPRING_PROFILES_ACTIVE=prod,demo` và làm thêm **mục 8b** (bắt buộc).
- Chạy thật: `SPRING_PROFILES_ACTIVE=prod` (xem mục 11).

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

Cách B (chỉ t3.small trở lên; t3.micro dễ treo khi build Maven): clone repo trên EC2 (`git clone https://github.com/RVAN1102/Project_WEB.git`) và dùng `deploy/deploy.sh` mỗi lần cập nhật.

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
sudo install -o root -g root -m 644 deploy/nginx-auth-limits.conf /etc/nginx/conf.d/uteexpress-auth-limits.conf
sudo nano /etc/nginx/sites-available/uteexpress     # dán deploy/nginx-uteexpress.conf, thay example.com bằng domain
sudo ln -s /etc/nginx/sites-available/uteexpress /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d tenmiencuaban.vn -d www.tenmiencuaban.vn
```

Certbot tự cấu hình HTTPS và tự gia hạn chứng chỉ. Cấu hình có sẵn khối `/ws` để WebSocket (STOMP realtime) đi qua Nginx.

Mở `https://tenmiencuaban.vn` để kiểm tra.

## 8b. Giai đoạn test: bật demo (bắt buộc khoá site bằng mật khẩu)

Profile `demo` nạp 38 hub mô phỏng, tài khoản `demo_*` cho mọi vai trò và rút thời gian chờ còn 2 phút. Mật khẩu mặc định của các tài khoản demo **nằm công khai trong repo**, nên khi bật demo trên Internet phải làm cả hai việc sau.

1. **Mật khẩu demo riêng** (làm *trước lần khởi động đầu* với demo):

   ```bash
   sudo apt -y install apache2-utils
   htpasswd -bnBC 10 "" 'MAT_KHAU_DEMO_CUA_BAN' | tr -d ':
'; echo
   ```

   Dán chuỗi `$2y$10$...` vào `/etc/uteexpress.env`: `UTEEXPRESS_DEMO_PASSWORD_HASH=<chuỗi đó>`, đặt `SPRING_PROFILES_ACTIVE=prod,demo`, rồi `sudo systemctl restart uteexpress`. Mọi tài khoản `demo_*` dùng mật khẩu này.

2. **Khoá toàn site bằng Nginx basic auth** (trình duyệt hỏi tên/mật khẩu trước khi vào web):

   ```bash
   sudo htpasswd -c /etc/nginx/.htpasswd tester      # đặt mật khẩu; thêm người: bỏ -c
   ```

   Trong `/etc/nginx/sites-available/uteexpress`, bỏ dấu `#` ở hai dòng `auth_basic` trong khối `server` (đã có sẵn trong file mẫu), rồi `sudo nginx -t && sudo systemctl reload nginx`. Không commit file `.htpasswd`. WebSocket `/ws` vẫn hoạt động: trình duyệt tự gửi lại thông tin basic auth và cookie đăng nhập cho cùng domain.

OTP: profile `otp-local` ghi mã vào file trên server (`/opt/uteexpress/.local/otp-inbox`), chỉ người SSH vào máy đọc được. Chỉ thêm (`prod,demo,otp-local`) nếu cần thử đăng ký; bình thường dùng tài khoản demo.

## 9. Cập nhật phiên bản mới

1. Kéo code mới / build WAR mới (cách A hoặc `./deploy/deploy.sh`).
2. `sudo systemctl restart uteexpress`. Flyway tự chạy migration mới nếu có.

## 10. Lưu ý vận hành

- Profile production là `prod`; `demo` chỉ dùng giai đoạn test (mục 8b), tắt theo mục 11. Đăng ký bằng OTP điện thoại cần nhà cung cấp SMS thật; trước đó dùng tài khoản tạo sẵn hoặc đăng nhập Google (profile `google`, cần client ID/secret).
- Tài khoản nhân viên (shipper, hub, tài xế, quản lý, admin) cần được tạo trong DB; không có trang tự đăng ký cho các vai trò này.
- File upload lưu ở `/opt/uteexpress/uploads`. Sao lưu RDS: bật **Automated backups** (mặc định 7 ngày).
- Lỗi thường gặp:
  - `Login failed` / timeout tới RDS: kiểm tra security group bước 3.3 và URL trong `/etc/uteexpress.env`.
  - `502 Bad Gateway`: app chưa chạy xong hoặc lỗi, xem `journalctl -u uteexpress -n 200`.
  - Certbot lỗi: domain chưa trỏ đúng IP (bước 4).

## 11. Chuyển sang chạy thật (tắt demo)

1. `/etc/uteexpress.env`: `SPRING_PROFILES_ACTIVE=prod` (bỏ `demo`, `otp-local`); đổi `UTEEXPRESS_JWT_SECRET` mới (`openssl rand -base64 48`), mọi phiên đăng nhập cũ sẽ hết hiệu lực.
2. Khoá các tài khoản demo trên RDS (chỉ chạy khi bạn quyết định; dữ liệu demo không tự mất khi tắt profile):

   ```sql
   UPDATE dbo.users SET status = N'LOCKED' WHERE username LIKE N'demo[_]%';
   ```

   Hub mô phỏng, bảng giá demo vẫn còn; thay bằng dữ liệu thật trước khi mở cho khách.
3. Bỏ basic auth: thêm lại dấu `#` ở hai dòng `auth_basic`, `sudo nginx -t && sudo systemctl reload nginx`.
4. `sudo systemctl restart uteexpress`.
