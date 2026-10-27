# CI/CD UTEExpress với GitHub Actions

Repository: `RVAN1102/Project_WEB`. Website: https://uteexpress.event5g.fun. EC2: `54.253.15.25`, region `ap-southeast-2`.

## Luồng hoạt động

- Pull request: kiểm tra script triển khai, tạo SQL Server 2022 và database thử nghiệm trống, chạy migration, chạy toàn bộ kiểm thử, đóng gói WAR.
- Push vào `main`: thực hiện cùng kiểm thử, lưu WAR và SHA-256 trong 14 ngày. Khi biến repository `ENABLE_PRODUCTION_DEPLOY=true`, triển khai chính WAR đã kiểm thử qua SSH.
- Có thể chọn Actions → **CI and deploy** → Run workflow → branch `main` → tick `deploy` để chạy thủ công. Cờ bật triển khai vẫn phải là `true`.
- Máy chủ kiểm tra checksum, giữ một WAR dự phòng, đổi WAR, khởi động dịch vụ và kiểm tra database cùng mã commit qua `/health/ready`. Nếu thất bại, khôi phục WAR cũ và kiểm tra lại. GitHub kiểm tra thêm URL HTTPS công khai.

Job `Build and test` không cần secrets. Mật khẩu SQL/JWT xuất hiện trong workflow chỉ dành cho database dùng một lần trên runner; không dùng cho production. Private config local không được đóng gói vào WAR.

## Máy chủ hiện tại

Đã kiểm tra qua SSH: Java 21; service `uteexpress` chạy với user `uteexpress`, working directory `/opt/uteexpress`, WAR `/opt/uteexpress/app.war`, cổng nội bộ `127.0.0.1:8080`; có sẵn `curl`, `jq`, `flock`. Secrets ứng dụng ở `/etc/uteexpress.env`.

CI dùng tài khoản SSH riêng `uteexpress-deploy`. Không đưa khóa quản trị `uteexpress-web.pem` vào GitHub. `/opt/uteexpress` và script cài release thuộc root; chỉ thư mục `uploads` cần app được ghi. Pipeline không build trên VPS và không sửa file cấu hình bí mật.

Thiết lập ngày 2026-10-28: đã cài helper và tài khoản triển khai trên EC2; đã lưu hai SSH secrets và các biến host/user/port trên GitHub. `ENABLE_PRODUCTION_DEPLOY` đang là `false`. Tài khoản GitHub hiện có quyền ghi code và secrets, nhưng không có quyền tạo/cấu hình environment qua API hoặc quản trị bảo vệ nhánh; chủ repo cần hoàn tất các mục này. Chưa thay WAR đang phục vụ website.

## Chuẩn bị server một lần

Thực hiện với tài khoản quản trị trên EC2. Các lệnh dưới đây giả định đang đứng ở thư mục chứa source đã xem xét; không thay file service/Nginx đang chạy nếu chưa so sánh cấu hình hiện tại.

```bash
sudo apt-get install -y curl jq util-linux
sudo useradd --create-home --home-dir /var/lib/uteexpress-deploy --shell /bin/bash uteexpress-deploy
sudo install -d -o uteexpress-deploy -g uteexpress-deploy -m 700 /var/lib/uteexpress-deploy/.ssh
sudo install -d -o uteexpress-deploy -g uteexpress-deploy -m 700 /var/lib/uteexpress-deploy/incoming
sudo install -o uteexpress-deploy -g uteexpress-deploy -m 600 ci_deploy.pub /var/lib/uteexpress-deploy/.ssh/authorized_keys
sudo chown root:root /opt/uteexpress /opt/uteexpress/app.war
sudo chmod 755 /opt/uteexpress
sudo chmod 644 /opt/uteexpress/app.war
sudo install -d -o root -g root -m 755 /opt/uteexpress/releases
sudo install -d -o uteexpress -g uteexpress -m 755 /opt/uteexpress/uploads
sudo install -o root -g root -m 755 deploy/install-release.sh /usr/local/sbin/uteexpress-install-release
printf '%s\n' 'uteexpress-deploy ALL=(root) NOPASSWD: /usr/local/sbin/uteexpress-install-release *' | sudo tee /etc/sudoers.d/uteexpress-deploy > /dev/null
sudo chmod 440 /etc/sudoers.d/uteexpress-deploy
sudo visudo -cf /etc/sudoers.d/uteexpress-deploy
```

Nếu user đã tồn tại, bỏ lệnh `useradd`. `ci_deploy.pub` phải là public key riêng cho CI; thêm tiền tố `restrict ` vào dòng key để tắt chuyển tiếp cổng và terminal tương tác. Private key không có passphrase để Actions dùng tự động; giữ bản riêng ngoài Git. Không đổi chủ sở hữu toàn bộ `/opt/uteexpress` theo kiểu đệ quy vì sẽ ảnh hưởng file upload.

Đường SSH từ runner GitHub phải tới được EC2. Nếu security group chỉ cho IP cá nhân, pipeline sẽ không kết nối được. Chọn runner có IP cố định hoặc đường mạng riêng phù hợp rồi chỉ cho nguồn đó; không mặc định mở cổng 22 cho toàn Internet. Workflow hiện dùng runner GitHub `ubuntu-24.04`; nếu đổi runner triển khai, chỉ đổi `runs-on` của job `deploy`, giữ job kiểm thử trên runner riêng dùng một lần.

## Cấu hình GitHub

Settings → Environments → tạo **production**, chỉ cho nhánh `main` triển khai; bật người duyệt nếu gói GitHub hỗ trợ. Có thể cần chủ repository thực hiện phần cài đặt này.

Settings → Secrets and variables → Actions:

| Loại | Tên | Giá trị |
|---|---|---|
| Repository variable | `ENABLE_PRODUCTION_DEPLOY` | Ban đầu `false`; chuyển `true` sau khi chuẩn bị xong |
| Variable | `DEPLOY_HOST` | `54.253.15.25` |
| Variable | `DEPLOY_USER` | `uteexpress-deploy` |
| Variable | `DEPLOY_PORT` | `22` (có thể bỏ qua) |
| Secret | `DEPLOY_SSH_KEY` | Toàn bộ private key riêng cho CI |
| Secret | `DEPLOY_KNOWN_HOSTS` | Dòng known_hosts đã xác minh cho `54.253.15.25` |

Các giá trị `DEPLOY_*` có thể đặt ở environment `production` hoặc repository. Riêng `ENABLE_PRODUCTION_DEPLOY` phải là **repository variable** vì được đọc trước khi job vào environment. Không ghi secrets vào YAML, issue, pull request hoặc chat.

Xác minh SSH host fingerprint qua kết nối quản trị đã tin cậy hoặc EC2 console. Không dùng `StrictHostKeyChecking=no`; workflow không tự tin khóa lấy qua `ssh-keyscan`. Thay máy hoặc host key cần xác minh rồi cập nhật `DEPLOY_KNOWN_HOSTS`.

Settings → Rules/Branches: bảo vệ `main`, yêu cầu pull request và check **Build and test** trước khi merge. Hạn chế người được sửa workflow và secrets. Workflow dùng `contents: read`, không cấp quyền ghi repository cho runner.

## Lần chạy đầu và vận hành

1. Sao lưu database và upload; kiểm tra khả năng phục hồi bản sao lưu. Xem lại migration trước khi merge.
2. Đưa thay đổi lên nhánh và tạo PR. Chờ `Build and test` thành công trên GitHub.
3. Để `ENABLE_PRODUCTION_DEPLOY=false` trong lần merge chuẩn bị. Sau đó bật `true` và chạy thủ công trên `main` với `deploy=true` để giám sát lần đầu. Những lần push main tiếp theo tự triển khai.
4. Kiểm tra Actions và https://uteexpress.event5g.fun/health/ready: phải có `status=UP` và mã commit đúng.
5. Nếu lỗi, xem `sudo journalctl -u uteexpress -n 200 --no-pager`. Không đăng log chứa dữ liệu khách hàng/credentials công khai.

Để tạm dừng tự triển khai, đặt `ENABLE_PRODUCTION_DEPLOY=false`. Job kiểm thử vẫn chạy. Khóa SSH CI có thể thu hồi bằng cách gỡ public key trên EC2 và thay secret trên GitHub.

Giới hạn: dịch vụ có gián đoạn ngắn khi khởi động lại, đây không phải triển khai không downtime. Rollback chỉ đổi WAR, **không hoàn tác migration hoặc dữ liệu**; bản cũ phải tương thích schema mới. Bản đầu tiên có thể phục hồi WAR cũ chưa có `/health/ready`, khi đó cần kiểm tra thủ công. Nếu kiểm tra HTTPS bên ngoài thất bại nhưng readiness nội bộ đã thành công, workflow báo lỗi và không tự rollback. Nếu tiến trình triển khai bị dừng cưỡng bức giữa chừng, phải kiểm tra service thủ công.

Server giữ một `previous.war`. Các file upload WAR ở `/var/lib/uteexpress-deploy/incoming/` không tự xóa; sau khi xác nhận bản chạy ổn, người vận hành dọn các thư mục commit cũ để tránh đầy đĩa. Bản artifact trên GitHub hết hạn sau 14 ngày.

## Google, email và giới hạn yêu cầu đăng nhập

Xem [EMAIL_GOOGLE_SMS_SETUP.md](EMAIL_GOOGLE_SMS_SETUP.md). Với site thật, bật `prod,google,smtp` chỉ sau khi điền đủ credentials; không bật `demo` hoặc `otp-local`. CI/CD không cấp Google/SMTP credentials hay gửi OTP điện thoại thay nhà cung cấp SMS.

File `deploy/nginx-auth-limits.conf` giới hạn các POST đăng nhập/đăng ký/OTP/quên mật khẩu theo IP. Cài file vào `/etc/nginx/conf.d/uteexpress-auth-limits.conf`, rồi thêm vào khối HTTPS `server` đang phục vụ site:

```nginx
limit_req zone=uteexpress_auth burst=10 nodelay;
limit_req_status 429;
```

Chạy `sudo nginx -t` trước `sudo systemctl reload nginx`; không ghi đè cấu hình chứng chỉ TLS đang có. Mặc định 10 yêu cầu/phút/IP và burst 10; điều chỉnh nếu nhiều khách dùng chung IP. Khi có CDN/load balancer, cấu hình real IP chỉ tin các proxy xác định trước để giới hạn áp dụng đúng người dùng. Đây là lớp bổ sung cho giới hạn OTP theo tài khoản, không thay thế theo dõi chi phí gửi SMS/email.
