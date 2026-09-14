# LECTURE_STANDARDS.md

> **Ground Truth kỹ thuật cho môn Lập trình Web — HCMUTE**
>
> Nguồn chuẩn: Slide môn **Lập trình Web — ThS. Nguyễn Hữu Trung, HCMUTE, 2026**.
>
> Tài liệu này là bộ quy tắc bắt buộc dành cho AI Coding Agent và thành viên phát triển dự án.
> Mọi mã nguồn được sinh ra, sửa đổi hoặc refactor PHẢI tuân thủ các quy tắc trong tài liệu này.

---

## Phạm vi và nguyên tắc áp dụng

### Mức độ bắt buộc

Các từ khóa sau được hiểu theo nghĩa quy chuẩn:

- **MUST / BẮT BUỘC**: không được vi phạm.
- **MUST NOT / CẤM**: tuyệt đối không được sinh code theo cách đó.
- **SHOULD / NÊN**: mặc định phải làm như vậy, chỉ được khác khi có lý do kỹ thuật rõ ràng.
- **MAY / CÓ THỂ**: tùy ngữ cảnh.
- **[LECTURE]**: quy tắc thể hiện trực tiếp hoặc nhất quán trong slide.
- **[OWNER HARDENING]**: quy tắc do chủ dự án yêu cầu chặt hơn slide và vẫn là Ground Truth bắt buộc.
- **[LECTURE SYNTHESIS]**: kết luận kỹ thuật được tổng hợp từ nhiều sơ đồ/quy tắc trong slide thay vì một câu nguyên văn.

### Thứ tự ưu tiên khi có xung đột

1. Quy tắc trong file `docs/LECTURE_STANDARDS.md`.
2. Yêu cầu cụ thể của đề bài/project hiện tại.
3. Kiến trúc chuyên biệt ở các slide phía sau.
4. Ví dụ tổng quan ở các slide Java/MVC phía trước.
5. Không tự ý áp dụng framework/library ngoài phạm vi trên.

Ví dụ:

- Slide Java tổng quát có thể minh họa interface với tiền tố `I`, nhưng các slide Spring Boot sử dụng nhất quán:
  - `UserService`
  - `UserServiceImpl`
  - `UserRepository`
- Vì vậy trong dự án Spring Boot, chuẩn chính thức là **không bắt buộc tiền tố `I`**, Service implementation dùng hậu tố `Impl`.

### Không tự ý thay đổi technology stack

AI Agent MUST:

- Giữ nguyên Servlet/JSP nếu bài đang dùng Servlet/JSP.
- Giữ nguyên Spring Boot nếu bài đang dùng Spring Boot.
- Không tự chuyển JSP sang Thymeleaf.
- Không tự chuyển JDBC sang JPA hoặc ngược lại.
- Không tự chuyển monolith sang microservices.
- Không thêm Redis, JWT, MapStruct, Lombok, HikariCP, Cloudinary hoặc thư viện khác chỉ vì chúng xuất hiện trong slide.
- Chỉ sử dụng dependency đã có trong project hoặc dependency được đề bài/người dùng yêu cầu rõ ràng.

---

# 1. Kiến trúc tổng thể 3 tầng (Three-Tier Architecture) & Phân rã Package

## 1.1. Kiến trúc bắt buộc

Kiến trúc chuẩn:

```text
Client / Browser
      |
      | HTTP Request
      v
+-----------------------+
| Presentation Layer    |
| Controller / Servlet  |
+-----------------------+
      |
      | DTO / Parameters
      v
+-----------------------+
| Business Logic Layer  |
| Service Interface     |
| ServiceImpl           |
+-----------------------+
      |
      | Domain operation
      v
+-----------------------+
| Data Access Layer     |
| Repository / DAO      |
+-----------------------+
      |
      | JDBC / JPA
      v
+-----------------------+
| Database              |
+-----------------------+
      |
      v
Repository -> Service -> Controller -> View/JSON -> Client
```

**[LECTURE] Quy tắc quan trọng:**

```text
Controller chỉ xử lý HTTP.
Service xử lý logic nghiệp vụ.
Repository xử lý truy cập dữ liệu.
```

Dependency direction MUST là:

```text
Controller
    |
    v
Service Interface
    |
    v
Service Implementation
    |
    v
Repository / DAO
    |
    v
Database
```

Không được đảo chiều dependency.

---

## 1.2. Presentation Layer — Controller / Servlet

Controller chịu trách nhiệm:

- Nhận HTTP request.
- Đọc path variable, query parameter, form field, request body, header.
- Xác thực người dùng ở mức request khi cần.
- Kích hoạt validation dữ liệu đầu vào.
- Chuyển dữ liệu sang DTO phù hợp.
- Gọi Service.
- Nhận kết quả từ Service.
- Chọn View hoặc tạo HTTP Response.
- Thiết lập status code, redirect, forward khi phù hợp.

Controller MUST NOT:

- Chứa business rule.
- Tự viết SQL.
- Tạo `Connection`.
- Gọi `EntityManager`.
- Gọi Repository trực tiếp.
- Thực hiện transaction nghiệp vụ.
- Chứa thuật toán xử lý nghiệp vụ dài.
- Chứa logic phân quyền dữ liệu phức tạp.
- Thao tác database trực tiếp.

Ví dụ đúng:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(user);
    }
}
```

Ví dụ CẤM:

```java
@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // CẤM: Controller gọi Repository trực tiếp.
        return userRepository.findById(id).orElseThrow();
    }
}
```

---

## 1.3. Business Logic Layer — Service

Service là nơi xử lý và điều phối logic nghiệp vụ.

Service chịu trách nhiệm:

- Validation nghiệp vụ.
- Áp dụng business rules.
- Kiểm tra điều kiện trước khi cập nhật dữ liệu.
- Kiểm tra authorization nghiệp vụ.
- Điều phối một hoặc nhiều Repository.
- Điều phối transaction.
- Chuyển đổi dữ liệu Entity ↔ DTO khi phù hợp.
- Tích hợp external service nếu nghiệp vụ cần.
- Xử lý exception nghiệp vụ.
- Giữ Controller mỏng.
- Cho phép tái sử dụng logic từ nhiều Controller.
- Tạo boundary rõ ràng để Unit Test.

### Service MUST tách Interface và Implementation

Cấu trúc chuẩn:

```text
service/
├── UserService.java
└── impl/
    └── UserServiceImpl.java
```

Interface:

```java
public interface UserService {

    UserResponse getById(Long id);

    List<UserResponse> getAll();

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    void delete(Long id);
}
```

Implementation:

```java
@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + id));

        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
```

### Naming Service

Chuẩn:

```text
UserService
UserServiceImpl

ProductService
ProductServiceImpl

CategoryService
CategoryServiceImpl
```

Không dùng:

```text
UserServiceInterface
UserServiceImplementation
IUserService
UserServiceClass
```

trừ khi đề bài cụ thể yêu cầu convention khác.

---

## 1.4. Data Access Layer — Repository / DAO

Repository/DAO chỉ chịu trách nhiệm truy cập dữ liệu:

- CRUD.
- Query database.
- Mapping dữ liệu.
- Pagination.
- Sorting.
- Specification/dynamic query.
- JPQL/Criteria/PreparedStatement.
- Không chứa business logic.

Spring Data JPA:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}
```

Repository MUST NOT:

- Gọi Controller.
- Gọi View.
- Xử lý HTTP request.
- Quyết định nghiệp vụ.
- Trả `ResponseEntity`.
- Biết URL endpoint.
- Chứa logic UI.

---

## 1.5. DTO và Entity

Luồng dữ liệu khuyến nghị:

```text
HTTP Request
   |
   v
Request DTO
   |
   v
Controller
   |
   v
Service
   |
   v
Entity
   |
   v
Repository
   |
   v
Database

Database
   |
   v
Entity
   |
   v
Service / Mapper
   |
   v
Response DTO
   |
   v
Controller
   |
   v
JSON / View
```

Entity là mô hình persistence.

DTO là dữ liệu qua boundary.

Controller SHOULD trao đổi bằng DTO thay vì expose trực tiếp Entity.

Ví dụ:

```java
public record UserResponse(
        Long id,
        String name,
        String email
) {
}
```

```java
public class CreateUserRequest {

    private String name;
    private String email;

    // getter/setter
}
```

Không trả các trường nội bộ như:

```text
password
passwordHash
refreshToken
secretKey
internal security flags
```

---

## 1.6. Constructor Injection

**Constructor Injection là kiểu DI mặc định.**

```java
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }
}
```

Ưu điểm:

- Dependency bắt buộc rõ ràng.
- Field có thể `final`.
- Dễ Unit Test.
- Dễ mock.
- Không che giấu dependency.
- Giảm lỗi runtime.

CẤM Field Injection:

```java
@Autowired
private UserRepository userRepository;
```

Nếu project đã sử dụng Lombok thì MAY dùng:

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
}
```

AI Agent MUST NOT tự thêm Lombok dependency chỉ để rút ngắn constructor.

---

## 1.7. Cấu trúc Maven chuẩn

```text
project-root/
├── pom.xml
├── docs/
│   └── LECTURE_STANDARDS.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/project/
    │   │       ├── controller/
    │   │       │   └── UserController.java
    │   │       │
    │   │       ├── service/
    │   │       │   ├── UserService.java
    │   │       │   └── impl/
    │   │       │       └── UserServiceImpl.java
    │   │       │
    │   │       ├── repository/
    │   │       │   └── UserRepository.java
    │   │       │
    │   │       ├── model/
    │   │       │   └── User.java
    │   │       │
    │   │       ├── entity/
    │   │       │   └── ...
    │   │       │
    │   │       ├── dto/
    │   │       │   ├── request/
    │   │       │   └── response/
    │   │       │
    │   │       ├── mapper/
    │   │       ├── exception/
    │   │       ├── filter/
    │   │       ├── config/
    │   │       └── util/
    │   │
    │   ├── resources/
    │   │   ├── application.properties
    │   │   └── ...
    │   │
    │   └── webapp/
    │       ├── assets/
    │       │   ├── css/
    │       │   ├── js/
    │       │   └── images/
    │       │
    │       └── WEB-INF/
    │           └── views/
    │               ├── index.jsp
    │               ├── login.jsp
    │               ├── error.jsp
    │               └── fragments/
    │                   ├── header.jsp
    │                   ├── footer.jsp
    │                   └── sidebar.jsp
    │
    └── test/
        └── java/
            └── com/example/project/
```

Không tạo cả `model/` và `entity/` cho cùng một loại object chỉ để nhân đôi class.

Quy ước:

- Nếu project gọi persistence class là `entity`, dùng `entity`.
- Nếu project hiện tại dùng `model`, giữ `model`.
- Không rename package hàng loạt nếu đề bài không yêu cầu.

---

## 1.8. Luồng chuẩn Spring Boot

```text
Client
  |
  v
Spring Security Filter Chain
  |
  v
DispatcherServlet
  |
  v
Controller
  |
  v
Service Interface
  |
  v
ServiceImpl
  |
  v
Repository
  |
  v
JPA / Hibernate
  |
  v
Database
  |
  v
Entity
  |
  v
Mapper / Service
  |
  v
Response DTO
  |
  v
Controller
  |
  v
HTTP Response
```

---

# 2. Java Core, OOP & 10 Nguyên tắc Clean Code

## 2.1. Java Naming Conventions

| Thành phần | Quy tắc | Ví dụ |
|---|---|---|
| Package | lowercase | `com.example.project.service` |
| Class | PascalCase | `UserServiceImpl` |
| Interface | PascalCase | `UserService` |
| Record | PascalCase | `UserResponse` |
| Enum type | PascalCase | `OrderStatus` |
| Enum constant | UPPER_SNAKE_CASE | `WAITING_PAYMENT` |
| Method | camelCase | `findUserById()` |
| Variable | camelCase | `userRepository` |
| Boolean | tên mang nghĩa boolean | `isActive`, `hasPermission` |
| Constant | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| Generic type | chữ hoa ngắn | `T`, `E`, `K`, `V` |

Ví dụ:

```java
package com.example.project.service;

public interface ProductService {

    int DEFAULT_PAGE_SIZE = 20;

    ProductResponse findById(Long productId);

    boolean isProductAvailable(Long productId);
}
```

Không dùng:

```java
int x;
List data;
boolean flag;
```

khi có thể đặt tên rõ hơn:

```java
int activeUsers;
List<Order> orders;
boolean isEligible;
```

Tên phải giúp người đọc hiểu mục đích code mà không cần đoán.

---

## 2.2. Quy tắc Package

Package:

- Viết thường.
- Phân tách theo trách nhiệm.
- Không gom mọi class vào một package.
- Không để Controller, Service, Entity, Repository chung một thư mục.

Đúng:

```text
com.example.shop.controller
com.example.shop.service
com.example.shop.service.impl
com.example.shop.repository
com.example.shop.entity
com.example.shop.dto
com.example.shop.config
```

Sai:

```text
com.example.shop.classes
com.example.shop.all
com.example.shop.misc
```

---

## 2.3. Encapsulation

Field của object domain SHOULD là `private`.

```java
public class Product {

    private Long id;
    private String name;
    private BigDecimal price;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

Không public field:

```java
public String password;
public BigDecimal price;
```

Getter/Setter phải đơn giản.

Không nhét business logic phức tạp vào getter/setter.

Nếu project đã có Lombok:

```java
@Getter
@Setter
public class Product {
    private Long id;
    private String name;
}
```

nhưng AI Agent không được tự thêm Lombok nếu project chưa dùng.

---

## 2.4. Abstraction và Polymorphism qua Interface

Code phụ thuộc abstraction thay vì implementation.

Đúng:

```java
private final PaymentService paymentService;
```

Không nên:

```java
private final VNPayPaymentService paymentService;
```

khi nghiệp vụ chỉ cần abstraction:

```java
public interface PaymentService {

    PaymentResult pay(PaymentRequest request);

    PaymentResult refund(RefundRequest request);
}
```

Implementation:

```java
@Service
public class VNPayPaymentService implements PaymentService {

    @Override
    public PaymentResult pay(PaymentRequest request) {
        // implementation
    }

    @Override
    public PaymentResult refund(RefundRequest request) {
        // implementation
    }
}
```

Nhờ đó có thể thêm:

```text
CashPaymentService
CardPaymentService
VNPayPaymentService
```

mà không sửa code client phụ thuộc interface.

---

## 2.5. Inheritance

Chỉ dùng inheritance khi thật sự có quan hệ **is-a**.

```java
public abstract class Payment {
    public abstract void process();
}
```

```java
public class CardPayment extends Payment {

    @Override
    public void process() {
        // ...
    }
}
```

Không dùng inheritance chỉ để tái sử dụng vài dòng code nếu quan hệ object không hợp lý.

---

## 2.6. Enum chuẩn

Enum dùng cho tập giá trị hữu hạn, cố định và type-safe.

```java
public enum OrderStatus {

    CREATED(1, "Đã tạo"),
    CONFIRMED(2, "Đã xác nhận"),
    SHIPPING(3, "Đang giao"),
    COMPLETED(4, "Hoàn thành"),
    CANCELLED(5, "Đã hủy");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
```

Enum có thể có abstract method:

```java
public enum PaymentMethod {

    CASH {
        @Override
        public void pay(double amount) {
            System.out.println("Cash: " + amount);
        }
    },

    CARD {
        @Override
        public void pay(double amount) {
            System.out.println("Card: " + amount);
        }
    };

    public abstract void pay(double amount);
}
```

Không thay enum hữu hạn bằng chuỗi magic:

```java
if ("admin".equals(role)) {
    ...
}
```

khi domain đã có Enum/Role object phù hợp.

---

## 2.7. SOLID

### S — Single Responsibility Principle

Một class chỉ nên có một nhóm trách nhiệm chính.

Đúng:

```text
StudentController -> HTTP
StudentService    -> nghiệp vụ
StudentRepository -> database
ValidationService -> validation chuyên biệt
EmailService      -> gửi email
```

CẤM God Class làm tất cả:

```text
validate
query database
send email
generate report
authorize
upload file
```

trong cùng một class.

### O — Open/Closed Principle

Class/module nên:

- mở cho extension;
- đóng với modification không cần thiết.

Ví dụ Payment Strategy:

```java
public interface PaymentService {
    void pay(BigDecimal amount);
}
```

Có thể thêm implementation mới mà không sửa Controller.

### L — Liskov Substitution Principle

Implementation của abstraction phải thay thế được abstraction mà không phá hợp đồng.

### I — Interface Segregation Principle

Không tạo interface khổng lồ:

```java
public interface UserService {
    void save();
    void update();
    void delete();
    void exportExcel();
    void sendEmail();
    void changePassword();
}
```

nếu các client không cần toàn bộ.

Nên tách abstraction theo trách nhiệm khi cần.

### D — Dependency Inversion Principle

High-level module phụ thuộc abstraction.

```text
Controller -> Service interface
Service    -> Repository interface
```

Không phụ thuộc trực tiếp implementation nếu không cần.

---

## 2.8. 10 nguyên tắc Clean Code của bài giảng

### 1. Viết code cho người đọc, không phải cho máy

Code được đọc nhiều hơn được viết.

Code phải:

- rõ ràng;
- dễ hiểu;
- dễ bảo trì;
- không khoe kỹ thuật phức tạp không cần thiết.

### 2. Tên gọi phải mô tả được ý nghĩa

Sai:

```java
int x = 0;
List<Order> data = get();
boolean flag = true;
```

Đúng:

```java
int activeUsers = 0;
List<Order> orders = getOrders();
boolean isEligible = true;
```

### 3. Mỗi function chỉ nên giải quyết một vấn đề

CẤM:

```java
public void processUser() {
    validate();
    queryDatabase();
    calculate();
    sendEmail();
    writeLog();
}
```

Nên tách:

```java
validateUser();
User user = loadUser();
Result result = calculateResult(user);
notifyUser(result);
```

### 4. Ưu tiên sự đơn giản — KISS

Không tạo abstraction, generic, design pattern phức tạp khi không cần.

### 5. Đừng Copy-Paste Logic — DRY

Business rule dùng nhiều nơi phải được đặt ở Service/Validator/Domain component phù hợp.

### 6. Tách Business Logic khỏi hạ tầng kỹ thuật

```text
Business Logic
      |
Application Layer
Service / Use Case
      |
Infrastructure Layer
Database / API / Cache
```

### 7. Luôn nghĩ đến trường hợp lỗi

Phải xem xét:

```text
Database down
API timeout
Invalid input
Entity not found
Duplicate data
Unauthorized access
Forbidden access
Transaction failure
```

### 8. Viết code dễ Test

Ưu tiên constructor injection, interface, method nhỏ, ít global state và business logic nằm ở Service.

### 9. Refactor liên tục

Đổi tên, xóa duplicate, tách method/class và giảm dependency khi có cơ hội hợp lý.

### 10. Luôn để code tốt hơn khi bạn tìm thấy nó

**Leave the code better than you found it.**

---

## 2.9. Quản lý Resource an toàn

MUST dùng `try-with-resources` khi object implement `AutoCloseable`.

```java
try (Connection connection = dataSource.getConnection();
     PreparedStatement statement =
             connection.prepareStatement(SQL);
     ResultSet resultSet = statement.executeQuery()) {

    while (resultSet.next()) {
        // read data
    }
}
```

Không được nuốt exception khi đóng resource.

---

## 2.10. Exception Standards

Exception phải:

- có ý nghĩa;
- không bị nuốt;
- không dùng `catch` rỗng;
- không expose stack trace cho user;
- được log ở boundary phù hợp;
- được chuyển thành thông báo HTTP/UI phù hợp.

Custom exception:

```java
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

Spring Boot:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex) {

        ErrorResponse body = new ErrorResponse(
                404,
                ex.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(body);
    }
}
```

CẤM:

```java
try {
    service.execute();
} catch (Exception e) {
}
```

---

# 3. Xử lý Request/Response, Điều hướng & Mô hình PRG

## 3.1. HttpServletRequest

`HttpServletRequest` đại diện cho HTTP request hiện tại.

Dùng để:

- đọc parameter;
- đọc header;
- đọc cookie;
- đọc HTTP method;
- đọc URL/URI;
- lấy session;
- lưu request attribute;
- lấy `RequestDispatcher`.

Ví dụ:

```java
String email = request.getParameter("email");
request.setAttribute("user", user);
HttpSession session = request.getSession();
String method = request.getMethod();
String uri = request.getRequestURI();
```

Request attribute chỉ tồn tại trong request hiện tại.

---

## 3.2. HttpServletResponse

```java
response.setStatus(HttpServletResponse.SC_OK);
response.setContentType("text/html");
response.setCharacterEncoding("UTF-8");
response.setHeader("X-Example", "value");
response.sendRedirect("/path");
response.sendError(
        HttpServletResponse.SC_FORBIDDEN,
        "Forbidden"
);
```

Không vừa commit body rồi mới forward/redirect.

---

## 3.3. ServletConfig

`ServletConfig`:

- thuộc về một Servlet;
- chứa init parameter riêng của Servlet;
- có thể truy cập `ServletContext`.

```java
ServletConfig config = getServletConfig();
String value = config.getInitParameter("parameterName");
ServletContext context = config.getServletContext();
String servletName = config.getServletName();
```

Không dùng `ServletConfig` để lưu global application state.

---

## 3.4. ServletContext

`ServletContext` là context của toàn bộ web application.

```java
ServletContext context = getServletContext();
context.setAttribute("appName", "UTEExpress");
Object value = context.getAttribute("appName");
context.removeAttribute("appName");
```

Không lưu state riêng của từng user trong Application Scope.

---

## 3.5. `RequestDispatcher.forward()`

```java
request.setAttribute("user", user);

RequestDispatcher dispatcher =
        request.getRequestDispatcher(
                "/WEB-INF/views/user-detail.jsp"
        );

dispatcher.forward(request, response);
return;
```

Đặc điểm:

```text
Browser URL: KHÔNG đổi
Request:     GIỮ nguyên
Attributes:  GIỮ được
Server-side: YES
New request: NO
```

Dùng khi render View hoặc forward lại form có lỗi.

---

## 3.6. `RequestDispatcher.include()`

```java
RequestDispatcher dispatcher =
        request.getRequestDispatcher(
                "/WEB-INF/views/fragments/header.jsp"
        );

dispatcher.include(request, response);
```

Phù hợp cho Header, Footer, Sidebar, Menu và component dùng lại.

---

## 3.7. `response.sendRedirect()`

```java
response.sendRedirect(
        request.getContextPath() + "/home"
);
```

Đặc điểm:

```text
Browser URL: ĐỔI
Request:     request mới
Attributes:  request attributes bị mất
Client-side: YES
HTTP:        redirect response
```

Không dùng request attribute để truyền dữ liệu qua redirect.

---

## 3.8. Forward vs Include vs Redirect

| Tiêu chí | `forward()` | `include()` | `sendRedirect()` |
|---|---|---|---|
| Thực hiện | Server | Server | Client |
| URL đổi | Không | Không | Có |
| Request mới | Không | Không | Có |
| Request attributes | Giữ | Giữ | Mất |
| Render JSP | Rất phù hợp | Component | Không trực tiếp |
| PRG | Không | Không | Có |

---

## 3.9. Post/Redirect/Get — PRG Pattern

Sau POST thành công làm thay đổi dữ liệu:

```text
POST
 |
 | validate + business logic + save
 v
Redirect
 |
 v
GET
 |
 v
Render page
```

MUST dùng redirect để tránh F5 submit lại POST.

Validation lỗi:

```text
POST
 -> validation fails
 -> set errors + formData
 -> forward form.jsp
```

POST thành công:

```text
POST
 -> validation OK
 -> Service
 -> Commit
 -> sendRedirect()
 -> GET
```

---

# 4. Dual Validation & Quản lý Thông báo lỗi trên Giao diện

## 4.1. Validation phải có hai đầu

MUST có:

```text
Client-side Validation
        +
Server-side Validation
```

Client-side chỉ hỗ trợ UX; Server-side là source of truth.

---

## 4.2. HTML5 Validation

```html
<form method="post" action="${pageContext.request.contextPath}/register">
    <input type="text" name="name" required minlength="2" maxlength="50">
    <input type="email" name="email" required maxlength="100">
    <input type="number" name="age" min="1" max="120" required>
    <button type="submit">Register</button>
</form>
```

Có thể dùng:

```text
required
type="email"
type="number"
type="date"
min
max
minlength
maxlength
pattern
```

JavaScript không thay thế Server-side Validation.

---

## 4.3. Server-side Servlet Validation

Chuẩn:

1. Nhận parameter.
2. Normalize.
3. Validate.
4. Nạp lỗi vào `Map<String, String> errors`.
5. Nếu lỗi: giữ `formData` và forward lại form.
6. Nếu hợp lệ: gọi Service và redirect theo PRG.

Ví dụ:

```java
String name = request.getParameter("name");
String email = request.getParameter("email");

Map<String, String> errors = new HashMap<>();
Map<String, String> formData = new HashMap<>();

formData.put("name", name == null ? "" : name);
formData.put("email", email == null ? "" : email);

if (name == null || name.trim().isEmpty()) {
    errors.put("name", "Họ tên không được để trống");
}

if (email == null || email.trim().isEmpty()) {
    errors.put("email", "Email không được để trống");
} else if (!email.matches(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
    errors.put("email", "Email không đúng định dạng");
}

if (!errors.isEmpty()) {
    request.setAttribute("errors", errors);
    request.setAttribute("formData", formData);

    request.getRequestDispatcher(
            "/WEB-INF/views/register.jsp"
    ).forward(request, response);

    return;
}

userService.register(name.trim(), email.trim());

response.sendRedirect(
        request.getContextPath() + "/login"
);
```

### Tên attribute chuẩn

**[OWNER HARDENING]**

```text
errors
formData
```

Password không được echo trở lại form và không lưu trong `formData`.

---

## 4.4. Các kiểm tra Server-side thường gặp

```text
null
blank
empty
minimum length
maximum length
email format
phone format
number format
minimum
maximum
date range
enum value
ID existence
duplicate value
business condition
authorization
```

Business validation nên nằm trong Service.

---

## 4.5. Hiển thị lỗi JSP bằng JSTL/EL

```jsp
<%@ taglib prefix="c"
    uri="https://jakarta.ee/tags/jstl/core" %>

<c:if test="${not empty errors.name}">
    <span class="error">
        <c:out value="${errors.name}" />
    </span>
</c:if>
```

Giữ dữ liệu:

```jsp
<%@ taglib prefix="fn"
    uri="https://jakarta.ee/tags/jstl/functions" %>

<input
    type="text"
    name="name"
    value="${fn:escapeXml(formData.name)}"
/>
```

---

## 4.6. Spring Boot Bean Validation

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, max = 50)
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    // getters/setters
}
```

Các annotation phổ biến:

```text
@NotNull
@NotBlank
@NotEmpty
@Size
@Min
@Max
@Email
@Pattern
@Positive
@Future
@Valid
@Validated
```

Controller:

```java
@PostMapping
public ResponseEntity<UserResponse> create(
        @Valid @RequestBody CreateUserRequest request) {

    UserResponse created = userService.create(request);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
}
```

---

# 5. Quản lý Trạng thái: Session, Cookie & Servlet Filter

## 5.1. HttpSession

```java
HttpSession session = request.getSession();
HttpSession existing = request.getSession(false);

session.setAttribute("currentUser", currentUser);
Object user = session.getAttribute("currentUser");
session.removeAttribute("currentUser");
session.invalidate();
```

---

## 5.2. Session Timeout

Theo bài giảng:

```text
30 phút = 1800 giây
```

```java
session.setMaxInactiveInterval(30 * 60);
```

Hoặc:

```xml
<session-config>
    <session-timeout>30</session-timeout>
</session-config>
```

---

## 5.3. Scope

| Scope | Đối tượng | Thời gian sống | Phạm vi |
|---|---|---|---|
| Page | `PageContext` | một JSP | JSP hiện tại |
| Request | `HttpServletRequest` | một request | forward/include |
| Session | `HttpSession` | nhiều request | một user/session |
| Application | `ServletContext` | vòng đời app | toàn application |

Nếu dữ liệu chỉ cần render một lần, dùng Request Scope.

---

## 5.4. Cookie

Cookie gồm:

```text
Name
Value
MaxAge
Path
Domain
Secure
HttpOnly
SameSite
```

```java
Cookie cookie = new Cookie("language", "vi");
cookie.setMaxAge(7 * 24 * 60 * 60);
cookie.setPath("/");
cookie.setHttpOnly(true);
cookie.setSecure(true);
cookie.setAttribute("SameSite", "Lax");
response.addCookie(cookie);
```

### MaxAge

```text
-1 = session cookie
0  = xóa cookie
>0 = thời gian sống tính bằng giây
```

### Security

Authentication/security cookie MUST dùng `HttpOnly=true`.

Production HTTPS MUST dùng `Secure=true`.

SameSite phải là `Lax` hoặc `Strict` theo yêu cầu.

Không lưu password plaintext hay secret trực tiếp trong cookie.

---

## 5.5. Servlet Filter

Lifecycle:

```java
init(FilterConfig filterConfig)

doFilter(
    ServletRequest request,
    ServletResponse response,
    FilterChain chain
)

destroy()
```

Luồng:

```text
Client
 -> Filter 1
 -> Filter 2
 -> Servlet/Controller
 -> Filter 2
 -> Filter 1
 -> Response
```

Cho phép request:

```java
chain.doFilter(request, response);
```

Chặn:

```java
response.sendError(
        HttpServletResponse.SC_FORBIDDEN
);
return;
```

Không gọi `chain.doFilter()` sau khi đã redirect/chặn response.

Ứng dụng:

- Authentication Filter.
- Authorization Filter.
- Logging Filter.
- URL access control.

---

# 6. Tầng Presentation: JSP & JSTL 3.0 (Jakarta EE)

## 6.1. JSP chỉ là View

JSP dùng để:

- render HTML;
- đọc model/request/session attributes;
- hiển thị dữ liệu;
- điều kiện hiển thị đơn giản;
- vòng lặp;
- include fragment.

JSP MUST NOT:

- chứa business logic;
- truy cập Repository/DAO;
- tạo JDBC Connection;
- viết SQL;
- gọi EntityManager;
- thực hiện transaction;
- chứa Java scriptlet.

---

## 6.2. JSP dưới `/WEB-INF`

```text
src/main/webapp/
└── WEB-INF/
    └── views/
        ├── index.jsp
        ├── login.jsp
        ├── users.jsp
        └── fragments/
```

Forward:

```java
request.getRequestDispatcher(
        "/WEB-INF/views/users.jsp"
).forward(request, response);
```

---

## 6.3. Jakarta Namespace

Dùng:

```java
jakarta.servlet.*
jakarta.servlet.http.*
jakarta.servlet.annotation.*
jakarta.persistence.*
jakarta.validation.*
```

CẤM legacy:

```java
javax.servlet.*
javax.persistence.*
javax.validation.*
```

Ngoại lệ: `javax.sql.DataSource` vẫn hợp lệ vì thuộc JDBC/Java SE.

---

## 6.4. JSTL Jakarta 3.0 — Taglib URI chuẩn

Core:

```jsp
<%@ taglib prefix="c"
    uri="https://jakarta.ee/tags/jstl/core" %>
```

Formatting:

```jsp
<%@ taglib prefix="fmt"
    uri="https://jakarta.ee/tags/jstl/fmt" %>
```

Functions:

```jsp
<%@ taglib prefix="fn"
    uri="https://jakarta.ee/tags/jstl/functions" %>
```

SQL:

```jsp
<%@ taglib prefix="sql"
    uri="https://jakarta.ee/tags/jstl/sql" %>
```

CẤM URI legacy:

```jsp
http://java.sun.com/jsp/jstl/core
```

---

## 6.5. Cấm Scriptlet

CẤM:

```jsp
<%
    User user =
        (User) request.getAttribute("user");
%>
```

CẤM:

```jsp
<%= user.getName() %>
```

Dùng EL/JSTL.

---

## 6.6. JSTL thay mã Java

`c:out`:

```jsp
<c:out value="${user.name}" />
```

`c:if`:

```jsp
<c:if test="${not empty sessionScope.currentUser}">
    <c:out value="${sessionScope.currentUser.name}" />
</c:if>
```

`c:choose`:

```jsp
<c:choose>
    <c:when test="${order.status == 'COMPLETED'}">
        Hoàn thành
    </c:when>
    <c:otherwise>
        Đang xử lý
    </c:otherwise>
</c:choose>
```

`c:forEach`:

```jsp
<c:forEach var="user" items="${users}">
    <c:out value="${user.name}" />
</c:forEach>
```

`c:url`:

```jsp
<c:url var="loginUrl" value="/login" />
<a href="${loginUrl}">Login</a>
```

---

## 6.7. Module hóa template

```jsp
<jsp:include
    page="/WEB-INF/views/fragments/header.jsp"
/>

<jsp:include
    page="/WEB-INF/views/fragments/sidebar.jsp"
/>

<jsp:include
    page="/WEB-INF/views/fragments/footer.jsp"
/>
```

Không copy cùng header/footer vào nhiều JSP.

---

## 6.8. SQL JSTL

Jakarta JSTL có SQL tags nhưng trong kiến trúc 3 tầng:

**CẤM dùng `<sql:query>` hoặc `<sql:update>` trong JSP.**

SQL phải thuộc DAO/Repository.

Chỉ dùng JSTL SQL nếu bài lab riêng yêu cầu minh họa.

---

# 7. Tầng Data Access: JDBC & JPA / Hibernate ORM

## 7.1. JDBC Best Practices

### PreparedStatement

MUST dùng parameterized query:

```java
String sql =
        """
        SELECT id, name, email
        FROM users
        WHERE email = ?
        """;

try (Connection connection =
         dataSource.getConnection();

     PreparedStatement statement =
         connection.prepareStatement(sql)) {

    statement.setString(1, email);

    try (ResultSet resultSet =
             statement.executeQuery()) {

        if (resultSet.next()) {
            // map data
        }
    }
}
```

CẤM nối chuỗi:

```java
String sql =
        "SELECT * FROM users WHERE email = '"
        + email
        + "'";
```

### Transaction thủ công

```java
try (Connection connection =
         dataSource.getConnection()) {

    connection.setAutoCommit(false);

    try {
        // SQL operations
        connection.commit();
    } catch (SQLException ex) {
        connection.rollback();
        throw ex;
    }
}
```

### try-with-resources

MUST đóng:

```text
ResultSet
Statement / PreparedStatement
Connection
```

bằng `try-with-resources`.

### SQLException

Có thể dùng:

```java
ex.getMessage();
ex.getSQLState();
ex.getErrorCode();
ex.getNextException();
```

Không được `catch` rỗng.

### Connection Pool

Các pool được bài giảng đề cập:

```text
HikariCP
Apache DBCP
C3P0
Tomcat JDBC Pool
```

Nếu Spring Boot đã quản lý `DataSource`, không tự tạo pool thứ hai.

---

## 7.2. JPA / Hibernate Standards

### Namespace

```java
import jakarta.persistence.*;
```

Không dùng `javax.persistence.*` trong Jakarta EE hiện đại/Spring Boot 3.

### Entity lifecycle

Bốn trạng thái:

```text
Transient
Managed
Detached
Removed
```

### Dirty Checking

**[LECTURE SYNTHESIS]**

Entity đang Managed trong transaction có thể tự được đồng bộ khi flush/commit:

```java
@Transactional
public void renameUser(Long id, String newName) {
    User user = userRepository.findById(id)
            .orElseThrow();

    user.setName(newName);
}
```

### Transaction Boundary

Transaction nghiệp vụ SHOULD nằm ở Service:

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Transactional
    @Override
    public void checkout(CheckoutRequest request) {
        // business transaction
    }
}
```

### Relationship Mapping

`@ManyToOne`:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User user;
```

`@OneToOne`:

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "profile_id")
private UserProfile profile;
```

**[LECTURE]** `@ManyToOne` MUST dùng `LAZY`.

**[OWNER HARDENING]** `@OneToOne` MUST dùng `LAZY`.

Không chuyển relation sang EAGER để né lazy-loading issue.

### N+1 Query

Ví dụ:

```text
1 query Orders
+ N query Customer
= N+1
```

Giải pháp:

`JOIN FETCH`:

```java
@Query("""
    SELECT o
    FROM Order o
    JOIN FETCH o.customer
    WHERE o.id = :id
    """)
Optional<Order> findDetailById(
        @Param("id") Long id
);
```

`@EntityGraph`:

```java
@EntityGraph(
    attributePaths = {
        "customer",
        "items"
    }
)
Optional<Order> findWithDetailsById(Long id);
```

Không giải quyết N+1 bằng EAGER toàn hệ thống.

### JPQL

```java
TypedQuery<User> query =
        entityManager.createQuery(
            """
            SELECT u
            FROM User u
            WHERE u.email = :email
            """,
            User.class
        );

query.setParameter("email", email);
```

### NamedQuery

```java
@Entity
@NamedQuery(
    name = "Category.findAll",
    query = """
            SELECT c
            FROM Category c
            ORDER BY c.id DESC
            """
)
public class Category {
}
```

Naming:

```text
<Entity>.<operation>
```

### Criteria API

Dùng cho dynamic query khi phù hợp:

```java
CriteriaBuilder cb =
        entityManager.getCriteriaBuilder();

CriteriaQuery<User> cq =
        cb.createQuery(User.class);

Root<User> root =
        cq.from(User.class);

cq.select(root);
```

### Pagination

JPA:

```java
query.setFirstResult(page * size);
query.setMaxResults(size);
```

Spring Data:

```java
Pageable pageable =
        PageRequest.of(
            page,
            size,
            Sort.by("createdAt").descending()
        );

Page<User> users =
        userRepository.findAll(pageable);
```

Danh sách lớn MUST phân trang.

---

# 8. Bảo mật: Xác thực, Mã hóa & RBAC

## 8.1. Authentication vs Authorization

Authentication:

```text
Bạn là ai?
```

Authorization:

```text
Bạn được phép làm gì?
```

Authentication phải xảy ra trước Authorization.

---

## 8.2. HTTP status

Chưa xác thực:

```text
401 Unauthorized
```

Đã xác thực nhưng không đủ quyền:

```text
403 Forbidden
```

---

## 8.3. Password Storage

MUST dùng BCrypt:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Hash:

```java
String encodedPassword =
        passwordEncoder.encode(
            request.getPassword()
        );
```

Verify:

```java
boolean matches =
        passwordEncoder.matches(
            rawPassword,
            encodedPassword
        );
```

**[OWNER HARDENING]** CẤM:

```text
plaintext
MD5
SHA-1
plain SHA-256
plain SHA-512
Base64
AES reversible encryption
custom password hash
```

---

## 8.4. Spring Security Architecture

```text
Client Request
     |
SecurityFilterChain
     |
Authentication
     |
SecurityContext
     |
Authorization
     |
Controller
```

Các thành phần:

```text
SecurityFilterChain
AuthenticationManager
UserDetailsService
UserDetails
PasswordEncoder
GrantedAuthority
Authentication
SecurityContext
SecurityContextHolder
AuthenticationEntryPoint
AccessDeniedHandler
```

---

## 8.5. SecurityFilterChain

```java
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http)
            throws Exception {

        http.authorizeHttpRequests(auth ->
            auth
                .requestMatchers(
                    "/login",
                    "/register",
                    "/assets/**"
                )
                .permitAll()
                .requestMatchers("/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

Không tắt CSRF/CORS một cách máy móc.

---

## 8.6. RBAC

Mô hình:

```text
User
 |
Role
 |
Permission
```

Ví dụ:

```text
ROLE_ADMIN
ROLE_VENDOR
ROLE_USER
```

Backend phải enforce quyền; frontend ẩn button không phải authorization.

Method security:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
}
```

---

## 8.7. Data Scoping

**[OWNER HARDENING]**

Authorization có thể cần:

```text
Role
+
Hub
+
Vị trí công việc
+
Nhiệm vụ được giao
+
Resource ownership
```

Điều kiện:

```text
ALLOW
=
roleAllowed
AND scopeAllowed
AND resourceAllowed
```

Không được tin `hubId`, `userId`, `vendorId` do client gửi nếu scope thực phải lấy từ authenticated principal/server-side data.

Thiếu quyền phải trả HTTP 403.

---

## 8.8. Sensitive Data

Response MUST NOT chứa:

```text
password
password hash
refresh token
secret key
private credential
internal security configuration
```

Dùng DTO để kiểm soát response.

---

# 9. Danh mục Cấm Tuyệt đối — Prohibited Checklist for AI Agent

## Architecture

- **CẤM Controller gọi Repository trực tiếp.**
- **CẤM Controller tạo `Connection`, `PreparedStatement`, `EntityManager`.**
- **CẤM business logic trong Controller.**
- **CẤM business logic trong JSP.**
- **CẤM business logic nặng trong Filter.**
- **CẤM Repository gọi Controller.**
- **CẤM Repository trả `ResponseEntity`.**
- **CẤM Repository biết HTTP request/response.**
- **CẤM Service Implementation bỏ qua Service Interface trong kiến trúc chuẩn của môn học.**
- **CẤM đặt Controller/Service/Repository/Entity chung một package.**
- **CẤM circular dependency.**
- **CẤM God Class/God Service.**
- **CẤM View truy cập Database trực tiếp.**
- **CẤM tự chuyển framework/architecture ngoài yêu cầu.**

## Dependency Injection

- **CẤM Field Injection làm mặc định.**
- **CẤM `@Autowired` trên field khi có thể Constructor Injection.**
- **CẤM tự `new ServiceImpl` trong Controller khi Spring quản lý Bean.**
- **CẤM phụ thuộc implementation khi interface đã có.**

## Dependency / Library

- **CẤM tự thêm library ngoài phạm vi đề bài.**
- **CẤM tự thêm Lombok, MapStruct, Redis, JWT, Cloudinary... nếu project không yêu cầu.**
- **CẤM tự chuyển JSP ↔ Thymeleaf.**
- **CẤM tự chuyển JDBC ↔ JPA.**

## Java / Clean Code

- **CẤM tên biến vô nghĩa nếu có thể đặt tên domain rõ ràng.**
- **CẤM copy-paste business rule.**
- **CẤM method quá nhiều trách nhiệm.**
- **CẤM hardcode secrets.**
- **CẤM empty catch.**
- **CẤM nuốt Exception.**
- **CẤM expose stack trace cho user.**
- **CẤM quên đóng resource.**
- **CẤM bỏ `try-with-resources` khi phù hợp.**

## Servlet

- **CẤM `javax.servlet.*` trong Jakarta EE 10+/Servlet 6.**
- **CẤM `javax.persistence.*` trong Jakarta Persistence hiện đại.**
- **CẤM `javax.validation.*` trong Spring Boot 3/Jakarta.**
- `javax.sql.DataSource` vẫn hợp lệ.
- **CẤM write response rồi mới forward.**
- **CẤM xử lý tiếp sau redirect/forward đã hoàn tất.**
- **CẤM dùng request attribute qua redirect.**
- **CẤM POST thành công rồi forward thay vì PRG.**
- **CẤM lưu user state trong `ServletContext`.**

## JSP / JSTL

- **CẤM Scriptlet `<% ... %>`.**
- **CẤM `<%= ... %>`.**
- **CẤM JDBC/JPA trong JSP.**
- **CẤM SQL JSTL trong project 3 tầng.**
- **CẤM business validation trong JSP.**
- **CẤM legacy JSTL URI `http://java.sun.com/...`.**
- **CẤM raw output dữ liệu không tin cậy khi có thể dùng escaping.**
- **CẤM copy header/footer/sidebar thay vì include.**

## Validation

- **CẤM chỉ validation phía Client.**
- **CẤM tin HTML `required` là đủ.**
- **CẤM bỏ Server-side Validation.**
- **CẤM làm mất dữ liệu form sau validation error nếu có thể giữ lại.**
- **CẤM echo password trở lại form.**
- **CẤM để database exception thay cho input validation.**

## Session / Cookie

- **CẤM lưu JDBC resource trong Session.**
- **CẤM lưu password plaintext trong Session/Cookie.**
- **CẤM quên `invalidate()` khi logout.**
- **CẤM authentication cookie thiếu `HttpOnly`.**
- **CẤM production security cookie thiếu `Secure` trên HTTPS.**
- **CẤM bỏ `SameSite` theo standard này.**
- **CẤM tin dữ liệu Cookie mà không validate.**

## Filter

- **CẤM quên `chain.doFilter()` khi được phép đi tiếp.**
- **CẤM gọi `chain.doFilter()` sau khi đã chặn/redirect.**
- **CẤM business transaction nặng trong Filter.**
- **CẤM authorization chỉ bằng frontend.**

## JDBC

- **CẤM nối chuỗi SQL với user input.**
- **CẤM `Statement` + string concatenation cho parameterized query.**
- **CẤM SQL Injection-prone code.**
- **CẤM quên rollback/commit transaction.**
- **CẤM để Connection/Statement/ResultSet không được đóng.**
- **CẤM empty `catch(SQLException)`.**
- **CẤM tạo pool thứ hai nếu framework đã quản lý DataSource.**
- **CẤM hardcode DB password.**
- **CẤM `SELECT *` mặc định khi chỉ cần vài column.**

## JPA / Hibernate

- **CẤM Controller gọi `EntityManager`.**
- **CẤM transaction nghiệp vụ ở Controller.**
- **CẤM đổi relation sang EAGER để né lỗi lazy loading.**
- **CẤM `@ManyToOne` thiếu `fetch = FetchType.LAZY`.**
- **CẤM `@OneToOne` thiếu `fetch = FetchType.LAZY` theo OWNER HARDENING.**
- **CẤM bỏ qua N+1 Query.**
- **CẤM sửa N+1 bằng EAGER toàn hệ thống.**
- **CẤM nối user input vào JPQL string.**
- **CẤM query danh sách lớn không phân trang.**
- **CẤM multiple collection fetch join tùy tiện.**
- **CẤM expose Entity trực tiếp khi có dữ liệu nhạy cảm.**
- **CẤM giả định Detached Entity tự dirty-check.**
- **CẤM business logic phức tạp trong Repository.**

## Authentication / Authorization

- **CẤM lưu password plaintext.**
- **CẤM MD5/SHA-1/plain SHA-256/plain SHA-512/Base64 cho password.**
- **CẤM compare raw password với database bằng `equals()`.**
- **BẮT BUỘC BCrypt/`PasswordEncoder`.**
- **CẤM coi Authentication = Authorization.**
- **CẤM chỉ kiểm tra Role khi còn data scope.**
- **CẤM tin scope ID do client gửi.**
- **CẤM bỏ qua resource ownership.**
- **CẤM trả `200 OK` cho forbidden request.**
- **CẤM dùng 401 khi user đã xác thực nhưng thiếu quyền; dùng 403.**
- **CẤM authorization chỉ ở JSP/JavaScript.**

---

# Final Architecture Invariants

AI Agent MUST tự kiểm tra:

```text
[ ] Controller chỉ xử lý HTTP/input/output.
[ ] Controller gọi Service, KHÔNG gọi Repository.
[ ] Service có Interface + Implementation hậu tố Impl.
[ ] Business logic nằm ở Service.
[ ] Repository/DAO chỉ xử lý data access.
[ ] Constructor Injection được ưu tiên.
[ ] Không Field Injection.
[ ] DTO được dùng tại boundary khi cần.
[ ] Entity không expose dữ liệu nhạy cảm.
[ ] Servlet/Jakarta dùng namespace jakarta.*.
[ ] JSP không có Scriptlet.
[ ] JSTL dùng Jakarta 3.0 URI.
[ ] JSP không truy cập database.
[ ] Validation có cả client-side và server-side.
[ ] Validation failure giữ errors + formData.
[ ] Successful POST áp dụng PRG.
[ ] Cookie có HttpOnly/SameSite và Secure trên HTTPS.
[ ] Logout invalidate Session.
[ ] Filter không chứa business logic nặng.
[ ] JDBC dùng PreparedStatement.
[ ] JDBC dùng try-with-resources.
[ ] Transaction lỗi có rollback.
[ ] SQLException không bị nuốt.
[ ] JPA relation được kiểm soát fetch.
[ ] @ManyToOne dùng LAZY.
[ ] @OneToOne dùng LAZY theo project standard.
[ ] N+1 được kiểm tra.
[ ] JOIN FETCH/@EntityGraph được dùng khi cần.
[ ] List lớn có pagination.
[ ] @Transactional đặt ở Service boundary phù hợp.
[ ] Password dùng BCrypt.
[ ] Không plaintext/MD5/weak password hashing.
[ ] Authorization thực thi ở backend.
[ ] RBAC kết hợp data scoping khi domain yêu cầu.
[ ] Forbidden access trả HTTP 403.
[ ] Không tự thêm dependency/framework ngoài yêu cầu.
```

---

> **QUY TẮC CUỐI CÙNG CHO AI CODING AGENT**
>
> Khi có nhiều cách để giải quyết một yêu cầu, ưu tiên:
>
> ```text
> Đúng kiến trúc
>     >
> Bảo mật
>     >
> Tính đúng đắn
>     >
> Dễ đọc
>     >
> Dễ test
>     >
> Dễ bảo trì
>     >
> Giải pháp đơn giản
>     >
> Tối ưu sớm
> ```
>
> Không được phá kiến trúc 3 tầng để viết ít code hơn.
>
> Không được tự thêm framework/library để giải quyết vấn đề mà JDK, Jakarta EE hoặc stack hiện tại đã xử lý được.
>
> Không được "sửa nhanh" bằng cách đưa business logic vào Controller, JSP, Filter hoặc Repository.
>
> **Canonical dependency flow:**
>
> ```text
> Request
>   -> Controller
>   -> Service Interface
>   -> ServiceImpl
>   -> Repository / DAO
>   -> Database
>   -> Repository / DAO
>   -> ServiceImpl
>   -> DTO
>   -> Controller
>   -> Response / JSP
> ```
>
> Đây là kiến trúc mặc định và bất biến của project trừ khi đề bài hoặc giảng viên yêu cầu rõ ràng một kiến trúc khác.
