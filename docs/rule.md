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

---

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

---

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

nếu đó là nhiều trách nhiệm độc lập.

Nên tách:

```java
validateUser();
User user = loadUser();
Result result = calculateResult(user);
notifyUser(result);
```

Mục tiêu:

- dễ đọc;
- dễ test;
- dễ tái sử dụng.

---

### 4. Ưu tiên sự đơn giản — KISS

**Keep It Simple, Stupid.**

Không tạo:

- abstraction dư thừa;
- generic phức tạp không cần thiết;
- design pattern chỉ để "cho đẹp";
- 5 class cho một thao tác đơn giản.

Giải pháp đơn giản, đúng trách nhiệm và dễ bảo trì được ưu tiên.

---

### 5. Đừng Copy-Paste Logic — DRY

CẤM copy cùng business rule vào nhiều Controller/Service.

Nếu một rule dùng nhiều nơi, đưa về nơi phù hợp:

```text
Service
Validator
Utility
Domain method
Policy component
```

Không lạm dụng utility cho business logic.

---

### 6. Tách Business Logic khỏi hạ tầng kỹ thuật

Mô hình:

```text
Business Logic
      |
      v
Application Layer
Service / Use Case
      |
      v
Infrastructure Layer
Database / API / Cache
```

Thay đổi:

```text
MySQL -> PostgreSQL
REST API A -> REST API B
Cache implementation
```

không được buộc Controller/business rule thay đổi hàng loạt.

---

### 7. Luôn nghĩ đến trường hợp lỗi

Code phải xử lý rõ:

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

Không chỉ viết happy path.

---

### 8. Viết code dễ Test

Code dễ test thường:

- constructor injection;
- phụ thuộc interface;
- không dùng global mutable state;
- business logic nằm trong Service;
- method nhỏ;
- ít side effect không kiểm soát.

---

### 9. Refactor liên tục

Refactor:

- đổi tên rõ hơn;
- xóa duplicate code;
- tách function;
- tách class quá lớn;
- giảm dependency;
- bổ sung test trước/sau thay đổi quan trọng.

Không đợi code "quá tệ" mới refactor.

---

### 10. Luôn để code tốt hơn khi bạn tìm thấy nó

**Leave the code better than you found it.**

Khi sửa một khu vực, có thể:

- xóa dead code;
- đặt tên rõ hơn;
- tách logic hợp lý;
- sửa exception handling;
- bổ sung test;
- loại duplicate code.

Không refactor vô hạn ngoài phạm vi nhiệm vụ.

---

## 2.9. Quản lý Resource an toàn

Quy tắc này xuất phát từ Java/JDBC best practices trong bài giảng.

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

Không:

```java
Connection connection = null;

try {
    connection = ...;
} finally {
    if (connection != null) {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }
}
```

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

Spring Boot nên xử lý tập trung:

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

Không catch chỉ để im lặng:

```java
try {
    service.execute();
} catch (Exception e) {
    // CẤM
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

Vòng đời:

```text
Một HTTP request
      |
      v
HttpServletRequest được tạo
      |
      v
Filter -> Servlet -> JSP
      |
      v
Request kết thúc
```

Request attribute chỉ tồn tại trong request hiện tại.

---

## 3.2. HttpServletResponse

`HttpServletResponse` dùng để tạo HTTP response.

Các thao tác chính:

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

Output:

```java
PrintWriter writer = response.getWriter();
writer.println("Hello");
```

Không vừa commit body rồi mới forward/redirect.

---

## 3.3. ServletConfig

`ServletConfig`:

- thuộc về một Servlet;
- chứa init parameter riêng của Servlet;
- được Servlet Container tạo;
- có thể truy cập `ServletContext`.

API:

```java
ServletConfig config = getServletConfig();

String value =
        config.getInitParameter("parameterName");

Enumeration<String> names =
        config.getInitParameterNames();

ServletContext context =
        config.getServletContext();

String servletName =
        config.getServletName();
```

Không dùng `ServletConfig` để lưu global application state.

---

## 3.4. ServletContext

`ServletContext`:

- một context cho toàn bộ web application;
- dùng cho dữ liệu/config/resource ở application scope;
- được chia sẻ giữa Servlet trong cùng ứng dụng.

Ví dụ:

```java
ServletContext context = getServletContext();

context.setAttribute("appName", "UTEExpress");

Object value = context.getAttribute("appName");

context.removeAttribute("appName");
```

Application scope không được dùng để lưu state riêng của từng user.

---

## 3.5. `RequestDispatcher.forward()`

Dùng khi server chuyển xử lý sang resource khác trong cùng application.

```java
request.setAttribute("user", user);

RequestDispatcher dispatcher =
        request.getRequestDispatcher(
                "/WEB-INF/views/user-detail.jsp"
        );

dispatcher.forward(request, response);
```

Đặc điểm:

```text
Browser URL: KHÔNG đổi
Request:     GIỮ nguyên
Attributes:  GIỮ được
Server-side: YES
New request: NO
```

Dùng `forward()` khi:

- Controller chuẩn bị dữ liệu cho JSP.
- Validation lỗi và phải hiển thị lại form.
- Render View.
- Muốn giữ `request.setAttribute(...)`.

Sau `forward()` nên `return`.

```java
dispatcher.forward(request, response);
return;
```

---

## 3.6. `RequestDispatcher.include()`

Dùng để chèn output của resource khác vào response hiện tại.

```java
RequestDispatcher dispatcher =
        request.getRequestDispatcher(
                "/WEB-INF/views/fragments/header.jsp"
        );

dispatcher.include(request, response);
```

Đặc điểm:

- URL không đổi.
- Cùng request/response.
- Nội dung resource được chèn vào output hiện tại.
- Caller có thể tiếp tục xử lý sau include.

Phù hợp:

```text
Header
Footer
Sidebar
Menu
Reusable component
```

Ở JSP, ưu tiên:

```jsp
<jsp:include page="/WEB-INF/views/fragments/header.jsp" />
```

---

## 3.7. `response.sendRedirect()`

Redirect yêu cầu browser tạo request mới.

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
HTTP:        redirect response, thông thường 302 với sendRedirect()
```

Không:

```java
request.setAttribute("message", "Success");
response.sendRedirect("/home");
```

rồi mong `/home` đọc được request attribute cũ.

Nếu cần truyền dữ liệu qua redirect, sử dụng phù hợp:

- Session.
- Query string đối với dữ liệu nhỏ, không nhạy cảm.
- Flash-message mechanism nếu project có.

Không đưa:

```text
password
token
secret
sensitive information
```

vào query string.

---

## 3.8. Forward vs Include vs Redirect

| Tiêu chí | `forward()` | `include()` | `sendRedirect()` |
|---|---|---|---|
| Thực hiện | Server | Server | Client |
| URL đổi | Không | Không | Có |
| Request mới | Không | Không | Có |
| Request attributes | Giữ | Giữ | Mất |
| Render JSP | Rất phù hợp | Component | Không trực tiếp |
| Header/Footer | Không phải mục tiêu chính | Phù hợp | Không |
| PRG | Không | Không | Có |

---

## 3.9. Post/Redirect/Get — PRG Pattern

### Quy tắc bắt buộc

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

MUST dùng redirect để tránh F5 gửi lại POST.

Sai:

```java
protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException {

    userService.create(...);

    request.getRequestDispatcher(
            "/WEB-INF/views/success.jsp"
    ).forward(request, response);
}
```

Nếu user F5 có nguy cơ submit lại request.

Đúng:

```java
protected void doPost(
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException {

    userService.create(...);

    response.sendRedirect(
            request.getContextPath() + "/users"
    );
}
```

### Validation lỗi không dùng PRG bắt buộc

Nếu POST invalid:

```text
POST form
   |
   v
Validation fails
   |
   v
request.setAttribute(errors)
   |
   v
forward(form.jsp)
```

Điều này giữ:

- error messages;
- form values;
- cùng request.

### POST thành công

```text
POST form
   |
   v
Validation OK
   |
   v
Service
   |
   v
Database commit
   |
   v
sendRedirect()
   |
   v
GET page
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

và database constraint là lớp bảo vệ cuối cùng khi phù hợp.

### Client-side

Mục tiêu:

- UX nhanh.
- Báo lỗi sớm.
- Giảm request không cần thiết.

Nhưng client-side có thể bị bypass.

### Server-side

Mục tiêu:

- bảo mật;
- dữ liệu chính xác;
- business rules;
- source of truth.

**Server-side validation là bắt buộc.**

Không bao giờ tin dữ liệu client chỉ vì browser có HTML validation.

---

## 4.2. HTML5 Validation

Ví dụ:

```html
<form method="post" action="${pageContext.request.contextPath}/register">

    <input
        type="text"
        name="name"
        required
        minlength="2"
        maxlength="50"
    >

    <input
        type="email"
        name="email"
        required
        maxlength="100"
    >

    <input
        type="number"
        name="age"
        min="1"
        max="120"
        required
    >

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

JavaScript MAY bổ sung UX nhưng không thay thế server validation.

---

## 4.3. Server-side Servlet Validation

Chuẩn:

1. Nhận parameter.
2. Normalize dữ liệu nếu phù hợp.
3. Validate từng field.
4. Đưa lỗi vào `Map<String, String>`.
5. Nếu có lỗi:
   - giữ `formData`;
   - forward lại form.
6. Nếu hợp lệ:
   - gọi Service;
   - lưu dữ liệu;
   - redirect theo PRG.

Ví dụ:

```java
String name = request.getParameter("name");
String email = request.getParameter("email");
String ageRaw = request.getParameter("age");

Map<String, String> errors = new HashMap<>();
Map<String, String> formData = new HashMap<>();

formData.put("name", name == null ? "" : name);
formData.put("email", email == null ? "" : email);
formData.put("age", ageRaw == null ? "" : ageRaw);

if (name == null || name.trim().isEmpty()) {
    errors.put("name", "Họ tên không được để trống");
} else if (name.trim().length() < 2) {
    errors.put("name", "Họ tên phải có ít nhất 2 ký tự");
}

if (email == null || email.trim().isEmpty()) {
    errors.put("email", "Email không được để trống");
} else if (!email.matches(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {

    errors.put("email", "Email không đúng định dạng");
}

if (ageRaw == null || ageRaw.isBlank()) {
    errors.put("age", "Tuổi không được để trống");
} else {
    try {
        int age = Integer.parseInt(ageRaw);

        if (age < 1 || age > 120) {
            errors.put(
                    "age",
                    "Tuổi phải nằm trong khoảng 1 đến 120"
            );
        }
    } catch (NumberFormatException ex) {
        errors.put("age", "Tuổi phải là số");
    }
}

if (!errors.isEmpty()) {
    request.setAttribute("errors", errors);
    request.setAttribute("formData", formData);

    request.getRequestDispatcher(
            "/WEB-INF/views/register.jsp"
    ).forward(request, response);

    return;
}

userService.register(name.trim(), email.trim(), Integer.parseInt(ageRaw));

response.sendRedirect(
        request.getContextPath() + "/login"
);
```

### Tên attribute chuẩn

**[OWNER HARDENING]**

Trong tài liệu này thống nhất:

```text
errors
formData
```

Slide minh họa có thể dùng tên `data`, nhưng AI Agent trong project này SHOULD dùng thống nhất `formData` để tránh nhiều convention khác nhau.

---

## 4.4. Các kiểm tra Server-side thường gặp

MUST kiểm tra theo requirement:

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

Không chỉ kiểm tra format.

Ví dụ:

```java
if (repository.existsByEmail(email)) {
    throw new DuplicateResourceException(
            "Email already exists"
    );
}
```

Business validation nên ở Service, không nhét tất cả vào Controller.

---

## 4.5. Phone Validation

Regex số điện thoại phải theo specification của project.

Không tự đoán format quốc gia nếu đề bài chưa quy định.

Nên khai báo centralized constant hoặc Bean Validation rule:

```java
public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String PHONE =
            "^[0-9+()\\-\\s]{8,20}$";
}
```

Regex trên chỉ là format tổng quát; business specification có quyền thay thế.

---

## 4.6. Hiển thị lỗi JSP bằng JSTL/EL

Khai báo JSTL:

```jsp
<%@ taglib prefix="c"
    uri="https://jakarta.ee/tags/jstl/core" %>
```

Hiển thị lỗi:

```jsp
<c:if test="${not empty errors.name}">
    <span class="error">
        <c:out value="${errors.name}" />
    </span>
</c:if>
```

Email:

```jsp
<c:if test="${not empty errors.email}">
    <span class="error">
        <c:out value="${errors.email}" />
    </span>
</c:if>
```

Giữ dữ liệu người dùng:

```jsp
<input
    type="text"
    name="name"
    value="${fn:escapeXml(formData.name)}"
/>
```

Khi dùng `fn:`:

```jsp
<%@ taglib prefix="fn"
    uri="https://jakarta.ee/tags/jstl/functions" %>
```

Không bắt user nhập lại tất cả field sau validation error.

Password là ngoại lệ:

- không nên echo password trở lại form;
- không lưu password vào `formData`.

---

## 4.7. Spring Boot Bean Validation

Dependency phải tương thích với Spring Boot project hiện tại.

DTO:

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Tên không được để trống")
    @Size(
        min = 2,
        max = 50,
        message = "Tên phải từ 2 đến 50 ký tự"
    )
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Min(value = 18, message = "Tuổi phải >= 18")
    @Max(value = 100, message = "Tuổi phải <= 100")
    private Integer age;

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

    UserResponse created =
            userService.create(request);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(created);
}
```

---

## 4.8. Global Validation Error Handler

Spring Boot SHOULD xử lý lỗi validation tập trung:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<ValidationErrorResponse>
            handleValidation(
                    MethodArgumentNotValidException ex) {

        List<FieldErrorResponse> errors =
                ex.getBindingResult()
                  .getFieldErrors()
                  .stream()
                  .map(error ->
                      new FieldErrorResponse(
                          error.getField(),
                          error.getDefaultMessage()
                      )
                  )
                  .toList();

        return ResponseEntity
                .badRequest()
                .body(
                    new ValidationErrorResponse(
                        400,
                        "Validation failed",
                        errors
                    )
                );
    }
}
```

Không copy cùng validation error handling vào mọi Controller.

---

# 5. Quản lý Trạng thái: Session, Cookie & Servlet Filter

## 5.1. HttpSession

Session lưu state người dùng ở server giữa nhiều request.

Khởi tạo/lấy session:

```java
HttpSession session =
        request.getSession();
```

Chỉ lấy nếu đã tồn tại:

```java
HttpSession session =
        request.getSession(false);
```

Thao tác:

```java
session.setAttribute(
        "currentUser",
        currentUser
);

Object user =
        session.getAttribute("currentUser");

session.removeAttribute("currentUser");

session.invalidate();
```

---

## 5.2. Session Timeout

Theo cấu hình mặc định được trình bày trong bài giảng:

```text
30 phút = 1800 giây
```

Có thể cấu hình:

```java
session.setMaxInactiveInterval(30 * 60);
```

hoặc `web.xml`:

```xml
<session-config>
    <session-timeout>30</session-timeout>
</session-config>
```

Nếu container/project đã cấu hình giá trị khác thì cấu hình explicit của project thắng.

---

## 5.3. Session Best Practices

MUST:

- invalidate session khi logout;
- chỉ lưu dữ liệu cần thiết;
- đặt timeout hợp lý;
- bảo vệ session cookie;
- xóa attribute không còn dùng.

Không lưu trong session:

```text
Connection
PreparedStatement
ResultSet
InputStream lớn
Entity graph cực lớn
password plaintext
secret key
```

Logout:

```java
HttpSession session =
        request.getSession(false);

if (session != null) {
    session.invalidate();
}

response.sendRedirect(
        request.getContextPath() + "/login"
);
```

---

## 5.4. Scope

| Scope | Đối tượng | Thời gian sống | Phạm vi |
|---|---|---|---|
| Page | `PageContext` | một JSP | JSP hiện tại |
| Request | `HttpServletRequest` | một request | forward/include |
| Session | `HttpSession` | nhiều request | một user/session |
| Application | `ServletContext` | vòng đời app | toàn application |

Không đưa dữ liệu vào Session chỉ vì muốn "giữ cho tiện".

Nếu dữ liệu chỉ cần render một lần, ưu tiên Request Scope.

---

## 5.5. Cookie

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

Tạo cookie:

```java
Cookie cookie =
        new Cookie("language", "vi");

cookie.setMaxAge(7 * 24 * 60 * 60);

cookie.setPath("/");

cookie.setHttpOnly(true);

cookie.setSecure(true);

cookie.setAttribute(
        "SameSite",
        "Lax"
);

response.addCookie(cookie);
```

---

## 5.6. MaxAge

Ý nghĩa:

```text
MaxAge = -1
    Session cookie.
    Thường mất khi browser session kết thúc.

MaxAge = 0
    Xóa cookie.

MaxAge > 0
    Thời gian sống tính bằng giây.
```

Xóa:

```java
Cookie cookie =
        new Cookie("rememberMe", "");

cookie.setMaxAge(0);
cookie.setPath("/");
cookie.setHttpOnly(true);

response.addCookie(cookie);
```

Path khi xóa phải phù hợp với Path đã dùng khi tạo cookie.

---

## 5.7. Cookie Security

**MUST:**

```java
cookie.setHttpOnly(true);
```

đối với cookie authentication/session liên quan đến bảo mật.

Mục đích:

```text
Giảm khả năng JavaScript truy cập cookie khi xảy ra XSS.
```

Production HTTPS MUST:

```java
cookie.setSecure(true);
```

**SameSite MUST được cấu hình phù hợp.**

```java
cookie.setAttribute(
        "SameSite",
        "Lax"
);
```

Hoặc với yêu cầu bảo mật cao:

```text
SameSite=Strict
```

Ý nghĩa:

```text
Strict
    Chỉ gửi cookie trong same-site context.

Lax
    Cho phép một số top-level navigation,
    hạn chế phần lớn cross-site POST.

None
    Cho phép cross-site.
    Phải kết hợp Secure.
```

Không lưu dữ liệu nhạy cảm trực tiếp trong cookie.

---

## 5.8. Servlet Filter

Filter xử lý Request/Response trước hoặc sau Servlet.

Luồng:

```text
Client
  |
  v
Filter 1
  |
  v
Filter 2
  |
  v
Servlet / Controller
  |
  v
Filter 2
  |
  v
Filter 1
  |
  v
Response
```

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

---

## 5.9. `FilterChain`

Nếu request được phép đi tiếp:

```java
chain.doFilter(request, response);
```

Nếu Filter đã chặn request:

```java
response.sendError(
        HttpServletResponse.SC_FORBIDDEN
);
return;
```

Không gọi:

```java
chain.doFilter(...)
```

sau khi đã chặn/redirect/commit response.

---

## 5.10. Authentication Filter

Ví dụ:

```java
@WebFilter("/*")
public class AuthenticationFilter
        implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest =
                (HttpServletRequest) request;

        HttpServletResponse httpResponse =
                (HttpServletResponse) response;

        String contextPath =
                httpRequest.getContextPath();

        String uri =
                httpRequest.getRequestURI();

        boolean publicUrl =
                uri.equals(contextPath + "/login")
                || uri.startsWith(
                    contextPath + "/assets/"
                );

        HttpSession session =
                httpRequest.getSession(false);

        boolean authenticated =
                session != null
                && session.getAttribute(
                    "currentUser"
                ) != null;

        if (!publicUrl && !authenticated) {
            httpResponse.sendRedirect(
                    contextPath + "/login"
            );
            return;
        }

        chain.doFilter(request, response);
    }
}
```

---

## 5.11. Authorization Filter

Nếu authenticated nhưng không đủ quyền:

```java
response.sendError(
        HttpServletResponse.SC_FORBIDDEN,
        "Forbidden"
);
return;
```

Không redirect user đã login về trang login chỉ vì thiếu role.

Đó là authorization failure, không phải authentication failure.

---

## 5.12. Logging Filter

Filter phù hợp để:

- request logging;
- timing;
- correlation information;
- audit metadata.

Filter SHOULD nhẹ.

CẤM nhét:

```text
business transaction
heavy database query
order calculation
payment processing
large domain workflow
```

vào Servlet Filter.

---

# 6. Tầng Presentation: JSP & JSTL 3.0 (Jakarta EE)

## 6.1. JSP chỉ là View

JSP chịu trách nhiệm:

- render HTML;
- đọc model/request/session attributes;
- hiển thị dữ liệu;
- điều kiện hiển thị đơn giản;
- vòng lặp trình bày;
- include fragment.

JSP MUST NOT:

- chứa business logic;
- truy cập Repository;
- truy cập DAO;
- tạo JDBC Connection;
- viết SQL;
- gọi EntityManager;
- thực hiện transaction;
- chứa Java scriptlet.

---

## 6.2. JSP dưới `/WEB-INF`

View SHOULD nằm tại:

```text
src/main/webapp/
└── WEB-INF/
    └── views/
        ├── index.jsp
        ├── login.jsp
        ├── users.jsp
        └── fragments/
```

Controller forward:

```java
request.getRequestDispatcher(
        "/WEB-INF/views/users.jsp"
).forward(request, response);
```

Lợi ích:

- không truy cập JSP trực tiếp qua URL;
- request phải đi qua Controller;
- giữ MVC rõ ràng.

---

## 6.3. Jakarta Namespace

Jakarta EE 10+ sử dụng namespace:

```java
jakarta.servlet.*
jakarta.servlet.http.*
jakarta.servlet.annotation.*
jakarta.persistence.*
jakarta.validation.*
```

CẤM dùng legacy EE namespace:

```java
javax.servlet.*
javax.persistence.*
javax.validation.*
```

### Ngoại lệ quan trọng

`javax.sql.DataSource` là API JDBC thuộc Java SE và vẫn hợp lệ:

```java
import javax.sql.DataSource;
```

Không được máy móc đổi:

```java
javax.sql.DataSource
```

thành package không tồn tại.

Quy tắc "cấm `javax`" chỉ áp dụng cho các API Jakarta EE đã chuyển namespace.

---

## 6.4. JSTL Jakarta 3.0 — Taglib URI chuẩn

### Core

```jsp
<%@ taglib prefix="c"
    uri="https://jakarta.ee/tags/jstl/core" %>
```

### Formatting

```jsp
<%@ taglib prefix="fmt"
    uri="https://jakarta.ee/tags/jstl/fmt" %>
```

### Functions

```jsp
<%@ taglib prefix="fn"
    uri="https://jakarta.ee/tags/jstl/functions" %>
```

### SQL

```jsp
<%@ taglib prefix="sql"
    uri="https://jakarta.ee/tags/jstl/sql" %>
```

### XML

```jsp
<%@ taglib prefix="x"
    uri="https://jakarta.ee/tags/jstl/xml" %>
```

CẤM URI JSTL legacy kiểu:

```jsp
http://java.sun.com/jsp/jstl/core
```

trong project Jakarta EE 10+ theo standard này.

---

## 6.5. Cấm Scriptlet

CẤM TUYỆT ĐỐI:

```jsp
<%
    User user =
        (User) request.getAttribute("user");

    if (user != null) {
        out.println(user.getName());
    }
%>
```

Không dùng:

```jsp
<%= user.getName() %>
```

Thay bằng EL/JSTL.

---

## 6.6. `<c:out>`

Dùng để xuất dữ liệu:

```jsp
<c:out value="${user.name}" />
```

Đặc biệt ưu tiên cho dữ liệu do user hoặc database cung cấp vì `c:out` hỗ trợ escape XML/HTML.

```jsp
<c:out
    value="${user.name}"
    escapeXml="true"
/>
```

Không render dữ liệu không tin cậy bằng scriptlet/raw Java.

---

## 6.7. `<c:if>`

```jsp
<c:if test="${not empty sessionScope.currentUser}">
    <p>
        Xin chào
        <c:out
            value="${sessionScope.currentUser.name}"
        />
    </p>
</c:if>
```

---

## 6.8. `<c:choose>`

```jsp
<c:choose>

    <c:when test="${order.status == 'COMPLETED'}">
        <span>Hoàn thành</span>
    </c:when>

    <c:when test="${order.status == 'CANCELLED'}">
        <span>Đã hủy</span>
    </c:when>

    <c:otherwise>
        <span>Đang xử lý</span>
    </c:otherwise>

</c:choose>
```

---

## 6.9. `<c:forEach>`

```jsp
<table>
    <tbody>

        <c:forEach
            var="user"
            items="${users}">

            <tr>
                <td>
                    <c:out value="${user.id}" />
                </td>

                <td>
                    <c:out value="${user.name}" />
                </td>
            </tr>

        </c:forEach>

    </tbody>
</table>
```

Không loop Java:

```jsp
<%
for (...) {
}
%>
```

---

## 6.10. `<c:url>`

Dùng để sinh URL context-safe:

```jsp
<c:url
    var="loginUrl"
    value="/login"
/>

<a href="${loginUrl}">
    Login
</a>
```

URL có parameter:

```jsp
<c:url
    var="detailUrl"
    value="/products/detail">

    <c:param
        name="id"
        value="${product.id}"
    />

</c:url>
```

---

## 6.11. Formatting

```jsp
<fmt:formatNumber
    value="${product.price}"
    type="number"
/>
```

Ngày:

```jsp
<fmt:formatDate
    value="${order.createdAt}"
    pattern="dd/MM/yyyy"
/>
```

---

## 6.12. JSTL Functions

```jsp
${fn:length(users)}
```

```jsp
${fn:toUpperCase(user.name)}
```

```jsp
${fn:contains(text, keyword)}
```

```jsp
${fn:escapeXml(formData.name)}
```

---

## 6.13. Module hóa JSP

Header:

```jsp
<jsp:include
    page="/WEB-INF/views/fragments/header.jsp"
/>
```

Sidebar:

```jsp
<jsp:include
    page="/WEB-INF/views/fragments/sidebar.jsp"
/>
```

Footer:

```jsp
<jsp:include
    page="/WEB-INF/views/fragments/footer.jsp"
/>
```

Không copy nguyên header/footer vào mọi JSP.

---

## 6.14. EL Scopes

Explicit scope khi cần tránh ambiguity:

```jsp
${pageScope.value}

${requestScope.user}

${sessionScope.currentUser}

${applicationScope.appName}

${param.keyword}

${initParam.appName}
```

---

## 6.15. SQL JSTL — tồn tại nhưng không được dùng trong kiến trúc 3 tầng

Jakarta JSTL có SQL library:

```jsp
<%@ taglib prefix="sql"
    uri="https://jakarta.ee/tags/jstl/sql" %>
```

và các tag như:

```text
sql:setDataSource
sql:query
sql:update
sql:param
sql:dateParam
sql:transaction
```

Tuy nhiên trong project 3 tầng:

**CẤM dùng `<sql:query>` hoặc `<sql:update>` trong JSP.**

Lý do:

```text
JSP = Presentation
Repository/DAO = Data Access
```

Ví dụ CẤM:

```jsp
<sql:query
    dataSource="${db}"
    var="result">

    SELECT * FROM users

</sql:query>
```

SQL JSTL chỉ được dùng nếu bài lab riêng yêu cầu minh họa JSTL SQL.

---

# 7. Tầng Data Access: JDBC & JPA / Hibernate ORM

# 7.1. JDBC Standards

## 7.1.1. Các thành phần chính

JDBC sử dụng:

```text
DriverManager
DataSource
Connection
PreparedStatement
Statement
CallableStatement
ResultSet
SQLException
DatabaseMetaData
```

Data Access Layer là nơi thao tác các API này.

Controller không được biết chúng tồn tại.

---

## 7.1.2. PreparedStatement là bắt buộc cho SQL có input

Đúng:

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
            // map row
        }
    }
}
```

CẤM:

```java
String sql =
        "SELECT * FROM users WHERE email = '"
        + email
        + "'";

Statement statement =
        connection.createStatement();

statement.executeQuery(sql);
```

Lý do:

- SQL Injection.
- Khó bảo trì.
- Khó binding type.
- Query không parameterized.

---

## 7.1.3. Không dùng `SELECT *` mặc định

Nên:

```sql
SELECT id, name, email
FROM users
WHERE id = ?
```

Không:

```sql
SELECT *
FROM users
```

khi chỉ cần vài column.

---

## 7.1.4. try-with-resources

MUST tự động đóng:

```text
ResultSet
PreparedStatement
Connection
```

Ví dụ:

```java
try (Connection connection =
         dataSource.getConnection();

     PreparedStatement statement =
         connection.prepareStatement(SQL);

     ResultSet resultSet =
         statement.executeQuery()) {

    // ...
}
```

---

## 7.1.5. Manual Transaction

Khi nhiều thao tác tạo thành một nghiệp vụ:

```java
try (Connection connection =
         dataSource.getConnection()) {

    connection.setAutoCommit(false);

    try (
        PreparedStatement debitStatement =
            connection.prepareStatement(
                DEBIT_SQL
            );

        PreparedStatement creditStatement =
            connection.prepareStatement(
                CREDIT_SQL
            )
    ) {

        debitStatement.setBigDecimal(
                1,
                amount
        );

        debitStatement.setLong(
                2,
                sourceAccountId
        );

        debitStatement.executeUpdate();

        creditStatement.setBigDecimal(
                1,
                amount
        );

        creditStatement.setLong(
                2,
                destinationAccountId
        );

        creditStatement.executeUpdate();

        connection.commit();

    } catch (SQLException ex) {

        connection.rollback();

        throw ex;
    }
}
```

Quy tắc:

```text
setAutoCommit(false)
    |
operations
    |
success -> commit()
failure -> rollback()
```

Không commit từng bước của một transaction nghiệp vụ nếu tất cả phải atomic.

---

## 7.1.6. SQLException

`SQLException` cung cấp:

```java
ex.getMessage();

ex.getSQLState();

ex.getErrorCode();

ex.getNextException();
```

Xử lý chain:

```java
private void logSQLException(SQLException exception) {

    SQLException current = exception;

    while (current != null) {

        logger.error(
            "SQL error. state={}, vendorCode={}, message={}",
            current.getSQLState(),
            current.getErrorCode(),
            current.getMessage()
        );

        current = current.getNextException();
    }
}
```

Không:

```java
catch (SQLException ex) {
}
```

Không:

```java
catch (Exception ex) {
    // ignore
}
```

Không gửi raw SQL exception/stack trace ra browser.

---

## 7.1.7. Connection Pool

Ứng dụng thực tế SHOULD sử dụng Connection Pool thay vì tạo kết nối mới thủ công cho mỗi request.

Bài giảng đề cập:

```text
HikariCP
Apache DBCP
C3P0
Tomcat JDBC Pool
```

HikariCP là lựa chọn phổ biến/nhanh được minh họa.

Nếu project đã có HikariCP:

```java
HikariConfig config =
        new HikariConfig();

config.setJdbcUrl(jdbcUrl);
config.setUsername(username);
config.setPassword(password);

HikariDataSource dataSource =
        new HikariDataSource(config);
```

AI Agent MUST NOT tự thêm HikariCP nếu framework/project đã quản lý pool hoặc đề bài không yêu cầu.

Spring Boot thường đã quản lý `DataSource` thông qua configuration; không tự tạo pool thứ hai.

---

## 7.1.8. JDBC Repository Example

```java
public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    void save(User user);
}
```

Implementation:

```java
public class JdbcUserRepository
        implements UserRepository {

    private final DataSource dataSource;

    public JdbcUserRepository(
            DataSource dataSource) {

        this.dataSource = dataSource;
    }

    @Override
    public Optional<User> findByEmail(
            String email) {

        String sql =
                """
                SELECT id, name, email
                FROM users
                WHERE email = ?
                """;

        try (
            Connection connection =
                dataSource.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);

            try (ResultSet resultSet =
                     statement.executeQuery()) {

                if (!resultSet.next()) {
                    return Optional.empty();
                }

                User user = new User();

                user.setId(
                    resultSet.getLong("id")
                );

                user.setName(
                    resultSet.getString("name")
                );

                user.setEmail(
                    resultSet.getString("email")
                );

                return Optional.of(user);
            }

        } catch (SQLException ex) {

            throw new DataAccessException(
                    "Cannot find user by email",
                    ex
            );
        }
    }
}
```

---

# 7.2. JPA / Hibernate Standards

## 7.2.1. Jakarta Persistence Namespace

Use:

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
```

Không dùng:

```java
javax.persistence.Entity
```

trong Jakarta EE 10+/Spring Boot 3 project.

---

## 7.2.2. Entity chuẩn

```java
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        nullable = false,
        length = 100
    )
    private String name;

    @Column(
        nullable = false,
        unique = true,
        length = 150
    )
    private String email;

    protected User() {
    }

    // getters/setters
}
```

Tên table/column phải khớp database schema.

Không đặt business/UI logic phức tạp trong Entity.

---

## 7.2.3. Entity Lifecycle

Bốn trạng thái phải hiểu rõ:

```text
Transient / New
Managed
Detached
Removed
```

### Transient

Object Java mới:

```java
User user = new User();
```

Chưa được Persistence Context quản lý.

### Managed

Sau:

```java
entityManager.persist(user);
```

hoặc entity được load bởi EntityManager/Repository trong persistence context.

### Detached

Entity từng managed nhưng đã rời Persistence Context, ví dụ:

```text
EntityManager closed
detach()
clear()
```

### Removed

Entity đã được đánh dấu để xóa:

```java
entityManager.remove(user);
```

---

## 7.2.4. Dirty Checking

**[LECTURE SYNTHESIS]**

Slide trình bày Persistence Context, trạng thái Managed và transaction. Quy tắc thực hành tương ứng:

Khi Entity đang **Managed**, thay đổi field bên trong transaction có thể được persistence provider đồng bộ xuống database khi flush/commit.

Ví dụ:

```java
@Transactional
public void renameUser(
        Long id,
        String newName) {

    User user =
            userRepository.findById(id)
                    .orElseThrow();

    user.setName(newName);

    // Không nhất thiết phải gọi save()
    // nếu entity vẫn đang Managed.
}
```

Không dựa vào cơ chế này cho Entity đang:

```text
Transient
Detached
```

mà không đưa nó trở lại persistence context.

---

## 7.2.5. Transaction Boundary

Transaction nghiệp vụ SHOULD đặt ở Service Layer.

Đúng:

```java
@Service
public class OrderServiceImpl
        implements OrderService {

    @Transactional
    @Override
    public void checkout(
            CheckoutRequest request) {

        // business transaction
    }
}
```

Không:

```java
@RestController
public class OrderController {

    @Transactional
    @PostMapping("/checkout")
    public void checkout() {
        // CẤM transaction business ở Controller.
    }
}
```

Read-only:

```java
@Transactional(readOnly = true)
public UserResponse findById(Long id) {
    ...
}
```

---

## 7.2.6. Relationship Mapping

Ví dụ:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(
    name = "user_id",
    nullable = false
)
private User user;
```

One-to-many:

```java
@OneToMany(
    mappedBy = "user",
    fetch = FetchType.LAZY,
    cascade = CascadeType.ALL
)
private List<Order> orders;
```

### Hard rule về fetch

**[LECTURE]**

`@ManyToOne` trong project MUST khai báo rõ:

```java
fetch = FetchType.LAZY
```

**[OWNER HARDENING]**

`@OneToOne` cũng MUST khai báo:

```java
fetch = FetchType.LAZY
```

Ví dụ:

```java
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "profile_id")
private UserProfile profile;
```

Không dựa vào default `EAGER`.

---

## 7.2.7. Không tự chuyển mọi relation sang EAGER

CẤM:

```java
@ManyToOne(fetch = FetchType.EAGER)
```

chỉ để "sửa nhanh" lỗi lazy loading.

CẤM:

```java
@OneToMany(fetch = FetchType.EAGER)
```

một cách mặc định.

EAGER có thể:

- load dữ liệu không cần thiết;
- tăng query;
- tăng memory;
- gây N+1;
- tạo object graph quá lớn.

Giải quyết use case cụ thể bằng query cụ thể.

---

## 7.2.8. N+1 Query

Ví dụ vấn đề:

```text
1 query lấy 100 Orders
+
100 query lấy Customer từng Order
=
101 queries
```

Không sửa bằng cách bật toàn bộ relation thành EAGER.

Các giải pháp chuẩn:

### JPQL JOIN FETCH

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

### EntityGraph

```java
@EntityGraph(
    attributePaths = {
        "customer",
        "items"
    }
)
Optional<Order> findWithDetailsById(
        Long id
);
```

### Batch loading

MAY dùng khi phù hợp và framework/project đã cấu hình.

---

## 7.2.9. JPQL

JPQL query trên Entity/property, không trực tiếp dựa trên tên table/column như SQL.

Ví dụ:

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

query.setParameter(
        "email",
        email
);

User user =
        query.getSingleResult();
```

JOIN FETCH:

```java
SELECT DISTINCT c
FROM Category c
LEFT JOIN FETCH c.products
```

---

## 7.2.10. NamedQuery

Entity:

```java
@Entity
@NamedQueries({
    @NamedQuery(
        name = "Category.findAll",
        query = """
                SELECT c
                FROM Category c
                ORDER BY c.id DESC
                """
    ),

    @NamedQuery(
        name = "Category.findByName",
        query = """
                SELECT c
                FROM Category c
                WHERE c.name = :name
                """
    )
})
public class Category {
    // ...
}
```

Naming:

```text
<Entity>.<operation>
```

Ví dụ:

```text
Category.findAll
Category.findByName
User.findByEmail
```

Named query phù hợp khi query được tái sử dụng và ổn định.

---

## 7.2.11. Criteria API

Criteria phù hợp dynamic query.

```java
CriteriaBuilder cb =
        entityManager.getCriteriaBuilder();

CriteriaQuery<User> cq =
        cb.createQuery(User.class);

Root<User> root =
        cq.from(User.class);

List<Predicate> predicates =
        new ArrayList<>();

if (name != null
        && !name.isBlank()) {

    predicates.add(
        cb.like(
            cb.lower(root.get("name")),
            "%" + name.toLowerCase() + "%"
        )
    );
}

cq.where(
    predicates.toArray(
        Predicate[]::new
    )
);

TypedQuery<User> query =
        entityManager.createQuery(cq);

List<User> users =
        query.getResultList();
```

Criteria API:

- type-oriented;
- phù hợp dynamic filtering;
- không nên dùng để làm query đơn giản khó đọc hơn JPQL.

---

## 7.2.12. Criteria Fetch

Có thể fetch association:

```java
root.fetch(
    "products",
    JoinType.LEFT
);

cq.select(root)
  .distinct(true);
```

Không fetch nhiều collection một cách tùy tiện vì có thể tạo:

- duplicate rows;
- Cartesian explosion;
- hiệu năng kém.

---

## 7.2.13. Pagination JPA

```java
TypedQuery<User> query =
        entityManager.createQuery(
            """
            SELECT u
            FROM User u
            ORDER BY u.id DESC
            """,
            User.class
        );

query.setFirstResult(
        page * size
);

query.setMaxResults(
        size
);
```

Spring Data:

```java
Pageable pageable =
        PageRequest.of(
            page,
            size,
            Sort.by("createdAt")
                .descending()
        );

Page<User> users =
        userRepository.findAll(pageable);
```

Danh sách lớn MUST phân trang.

Không mặc định:

```java
repository.findAll();
```

đối với bảng có khả năng rất lớn.

---

## 7.2.14. Spring Data Repository Queries

Derived query:

```java
Optional<User> findByEmail(
        String email
);
```

```java
List<User>
findByNameContainingIgnoreCase(
        String keyword
);
```

Custom JPQL:

```java
@Query("""
    SELECT u
    FROM User u
    WHERE u.email = :email
    """)
Optional<User> findUserByEmail(
        @Param("email") String email
);
```

Modification:

```java
@Modifying
@Transactional
@Query("""
    UPDATE User u
    SET u.name = :name
    WHERE u.id = :id
    """)
int updateNameById(
        @Param("id") Long id,
        @Param("name") String name
);
```

Trong kiến trúc course project, transaction nghiệp vụ tổng thể vẫn SHOULD nằm tại Service Layer.

---

# 8. Bảo mật: Authentication, Mã hóa & RBAC

## 8.1. Authentication vs Authorization

### Authentication — Xác thực

Trả lời:

```text
Bạn là ai?
```

Ví dụ:

```text
username/password
JWT
OAuth2/OIDC
API Key
```

### Authorization — Phân quyền

Trả lời:

```text
Bạn được phép làm gì?
```

Ví dụ:

```text
ADMIN được tạo user.
VENDOR được quản lý sản phẩm.
USER chỉ xem profile của mình.
```

Authentication phải xảy ra trước authorization.

---

## 8.2. HTTP status

### Chưa xác thực

Thông thường:

```text
401 Unauthorized
```

### Đã xác thực nhưng không đủ quyền

MUST:

```text
403 Forbidden
```

Không trả:

```text
200 OK
```

rồi nhét:

```json
{
  "error": "No permission"
}
```

---

## 8.3. Password Storage

**MUST sử dụng password hash an toàn.**

Chuẩn bài giảng Spring Security:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

Đăng ký:

```java
String encodedPassword =
        passwordEncoder.encode(
            request.getPassword()
        );

user.setPassword(
        encodedPassword
);
```

Xác thực:

```java
boolean matches =
        passwordEncoder.matches(
            rawPassword,
            user.getPassword()
        );
```

CẤM:

```java
user.setPassword(
    request.getPassword()
);
```

---

## 8.4. Thuật toán password bị cấm

**[OWNER HARDENING]**

CẤM dùng:

```text
plaintext
MD5
SHA-1
plain SHA-256
plain SHA-512
Base64
AES encryption as password storage
custom homemade hash
```

cho password.

Password phải dùng dedicated password hashing function như BCrypt theo standard của bài giảng/project.

---

## 8.5. Không tự implement password comparison

CẤM:

```java
if (rawPassword.equals(
        user.getPassword())) {
}
```

CẤM tự hash bằng:

```java
MessageDigest.getInstance("MD5");
```

Dùng:

```java
passwordEncoder.matches(
        rawPassword,
        encodedPassword
);
```

---

## 8.6. Spring Security Architecture

Luồng:

```text
Client Request
     |
     v
SecurityFilterChain
     |
     v
Authentication
     |
     v
SecurityContext
     |
     v
Authorization
     |
     +---- denied ----> 403
     |
     v
Controller
```

Các thành phần quan trọng:

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

## 8.7. SecurityFilterChain

Ví dụ:

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

                .requestMatchers(
                    "/admin/**"
                )
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

Không copy cấu hình `csrf.disable()` từ ví dụ JWT sang mọi loại application một cách máy móc.

CSRF policy phải phù hợp:

- browser session/cookie authentication;
- stateless API;
- architecture thực tế.

---

## 8.8. Role-Based Access Control — RBAC

Mô hình:

```text
User
 |
 v
Role
 |
 v
Permission
```

Ví dụ:

```text
ROLE_ADMIN
ROLE_VENDOR
ROLE_USER
```

Role matrix mẫu theo bài giảng:

```text
Category CREATE/UPDATE/DELETE
    ADMIN  -> allowed
    VENDOR -> allowed
    USER   -> denied

Category READ
    ADMIN  -> allowed
    VENDOR -> allowed
    USER   -> allowed

User Management
    ADMIN  -> allowed
    VENDOR -> denied
    USER   -> denied
```

Không hardcode role rải rác trong JSP/Controller nếu Spring Security đã quản lý.

---

## 8.9. Method-Level Security

Role:

```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteUser(Long id) {
    ...
}
```

Authority:

```java
@PreAuthorize(
    "hasAuthority('ORDER:READ')"
)
public OrderResponse getOrder(Long id) {
    ...
}
```

Có thể kết hợp URL security và method security.

---

## 8.10. Least Privilege

Mỗi user chỉ được cấp quyền tối thiểu cần thiết.

Không:

```text
ROLE_USER được gọi admin API
```

chỉ vì Controller vẫn "ẩn button".

Frontend ẩn button KHÔNG phải authorization.

Backend phải chặn.

---

## 8.11. Data Scoping

**[OWNER HARDENING]**

RBAC chỉ trả lời user có loại quyền nào.

Đối với dữ liệu có phạm vi, authorization bắt buộc thêm **Data Scoping**.

Ví dụ:

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

Điều kiện thực tế:

```text
ALLOW
=
roleAllowed
AND scopeAllowed
AND resourceAllowed
```

Ví dụ:

```text
ROLE_MANAGER
```

không đồng nghĩa Manager A được thao tác dữ liệu của mọi Hub.

Nếu user quản lý:

```text
Hub A
```

thì request cập nhật shipment thuộc:

```text
Hub B
```

phải bị từ chối.

---

## 8.12. Không tin resource ID do Client gửi

Sai:

```java
Shipment shipment =
        shipmentRepository
                .findById(shipmentId)
                .orElseThrow();

shipment.setStatus(newStatus);
```

nếu không kiểm tra phạm vi quyền.

Đúng về mặt rule:

```java
Shipment shipment =
        shipmentRepository
                .findById(shipmentId)
                .orElseThrow();

authorizationService
        .checkShipmentAccess(
                currentUser,
                shipment
        );

shipment.changeStatus(newStatus);
```

Hoặc scope ngay query nếu phù hợp:

```java
Optional<Shipment>
findByIdAndHubId(
        Long shipmentId,
        Long hubId
);
```

Không lấy `hubId` từ request rồi tin nó là hub của user.

Hub/scope phải lấy từ authenticated principal hoặc dữ liệu server-side đáng tin cậy.

---

## 8.13. HTTP 403 khi thiếu quyền

Servlet:

```java
response.sendError(
        HttpServletResponse.SC_FORBIDDEN,
        "Forbidden"
);
```

Spring:

```java
throw new AccessDeniedException(
        "Forbidden"
);
```

Spring Security sẽ chuyển authorization failure về flow phù hợp.

---

## 8.14. Sensitive Data

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

Sai:

```java
@GetMapping("/users/{id}")
public User getUser(...) {
    return userRepository.findById(...);
}
```

Đúng:

```java
@GetMapping("/users/{id}")
public UserResponse getUser(...) {
    return userService.getById(...);
}
```

---

## 8.15. HTTPS

Production authentication/API traffic MUST dùng HTTPS.

Không gửi:

```text
password
JWT
session cookie
API key
```

qua HTTP plaintext.

---

## 8.16. Input Validation vẫn là Security Requirement

Security không thay validation.

MUST kết hợp:

```text
Authentication
Authorization
Input Validation
Parameterized SQL/JPA
Output Escaping
HTTPS
Secure Cookies
Logging/Monitoring
```

---

## 8.17. SQL Injection Protection

JDBC:

```text
PreparedStatement
```

JPA:

```text
bound parameters
```

CẤM ghép user input vào JPQL/SQL.

Sai:

```java
String jpql =
        "SELECT u FROM User u WHERE u.name = '"
        + name
        + "'";
```

Đúng:

```java
TypedQuery<User> query =
        entityManager.createQuery(
            """
            SELECT u
            FROM User u
            WHERE u.name = :name
            """,
            User.class
        );

query.setParameter(
        "name",
        name
);
```

---

# 9. Danh mục Cấm Tuyệt đối — Prohibited Checklist for AI Agent

AI Coding Agent MUST kiểm tra danh sách này trước khi tạo hoặc sửa code.

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
- **CẤM đặt toàn bộ Controller/Service/Repository/Entity vào một package.**
- **CẤM circular dependency giữa Bean.**
- **CẤM God Class/God Service chịu quá nhiều trách nhiệm.**
- **CẤM để View truy cập Database trực tiếp.**
- **CẤM tự chuyển kiến trúc/framework chỉ vì Agent cho rằng cách khác "tốt hơn".**

## Dependency Injection

- **CẤM Field Injection làm mặc định.**
- **CẤM `@Autowired` trực tiếp vào field khi có thể dùng constructor injection.**
- **CẤM `new UserServiceImpl(...)` trong Controller khi Spring IoC đang quản lý Bean.**
- **CẤM phụ thuộc implementation khi interface đã tồn tại.**

## Dependency / Library

- **CẤM tự thêm library ngoài phạm vi đề bài.**
- **CẤM tự thêm Lombok nếu project chưa dùng.**
- **CẤM tự thêm MapStruct nếu project chưa dùng.**
- **CẤM tự thêm Redis nếu requirement không có.**
- **CẤM tự thêm JWT nếu authentication hiện tại không yêu cầu JWT.**
- **CẤM tự chuyển JSP sang Thymeleaf.**
- **CẤM tự chuyển JDBC sang JPA.**
- **CẤM tự chuyển JPA sang JDBC.**
- **CẤM pin version dependency ngẫu nhiên làm phá Spring Boot dependency management.**

## Java / Clean Code

- **CẤM tên biến vô nghĩa như `a`, `x`, `tmp`, `data`, `flag` khi có thể đặt tên domain rõ ràng.**
- **CẤM copy-paste business rule.**
- **CẤM method làm quá nhiều trách nhiệm.**
- **CẤM hardcode configuration/secrets trong source code.**
- **CẤM magic string nếu domain phù hợp với Enum/constant.**
- **CẤM dead code.**
- **CẤM empty catch.**
- **CẤM nuốt Exception rồi tiếp tục như không có lỗi.**
- **CẤM catch `Exception` chung chung nếu có thể xử lý exception cụ thể.**
- **CẤM expose stack trace/raw exception cho user.**
- **CẤM quên đóng resource.**
- **CẤM quản lý resource thủ công khi `try-with-resources` phù hợp.**

## Servlet

- **CẤM dùng `javax.servlet.*` trong Jakarta EE 10+/Servlet 6 project.**
- **CẤM dùng `javax.persistence.*` trong Jakarta Persistence hiện đại.**
- **CẤM dùng `javax.validation.*` trong Spring Boot 3/Jakarta project.**
- `javax.sql.DataSource` **KHÔNG bị cấm**, vì thuộc JDBC/Java SE.
- **CẤM write response body rồi mới `forward()`.**
- **CẤM tiếp tục xử lý sau redirect/forward nếu response đã hoàn tất.**
- **CẤM dùng `request.setAttribute()` rồi redirect và mong attribute còn tồn tại.**
- **CẤM POST thành công thay đổi dữ liệu rồi forward trực tiếp thay vì PRG.**
- **CẤM request state của user được lưu trong `ServletContext`.**

## JSP / JSTL

- **CẤM Java Scriptlet `<% ... %>` trong JSP.**
- **CẤM expression scriptlet `<%= ... %>`.**
- **CẤM JDBC trong JSP.**
- **CẤM JPA/EntityManager trong JSP.**
- **CẤM `<sql:query>` / `<sql:update>` trong JSP của project 3 tầng.**
- **CẤM business validation trong JSP.**
- **CẤM dùng legacy JSTL URI `http://java.sun.com/...` trong Jakarta JSTL 3.0 standard này.**
- **CẤM render dữ liệu không tin cậy bằng raw output khi có thể dùng `<c:out>`/escaping.**
- **CẤM copy header/footer/sidebar vào nhiều file nếu có thể dùng `<jsp:include>`.**
- **CẤM public JSP access khi project đã áp dụng `/WEB-INF/views`.**

## Validation

- **CẤM chỉ validation phía Client.**
- **CẤM tin HTML `required` là đủ bảo mật.**
- **CẤM bỏ qua Server-side Validation.**
- **CẤM làm mất dữ liệu form của user sau validation failure nếu có thể giữ lại.**
- **CẤM echo lại password vào input sau khi form lỗi.**
- **CẤM nhập dữ liệu sai format rồi để lỗi database xử lý thay validation.**
- **CẤM trả validation error không chỉ rõ field khi có thể xác định field.**

## Session

- **CẤM lưu JDBC Connection trong Session.**
- **CẤM lưu ResultSet/PreparedStatement trong Session.**
- **CẤM lưu password plaintext trong Session.**
- **CẤM quên `invalidate()` khi logout.**
- **CẤM tạo Session mới chỉ để kiểm tra đăng nhập; dùng `getSession(false)` khi thích hợp.**
- **CẤM đưa state riêng user vào Application Scope.**

## Cookie

- **CẤM lưu password plaintext trong Cookie.**
- **CẤM lưu secret/token nhạy cảm tùy tiện trong client cookie.**
- **CẤM authentication cookie thiếu `HttpOnly`.**
- **CẤM production security cookie thiếu `Secure` khi hệ thống chạy HTTPS.**
- **CẤM bỏ qua `SameSite` đối với security-sensitive cookie theo standard này.**
- **CẤM tin dữ liệu Cookie là dữ liệu server-trusted mà không validate.**

## Filter

- **CẤM quên `chain.doFilter()` khi request được phép tiếp tục.**
- **CẤM gọi `chain.doFilter()` sau khi đã redirect/chặn request.**
- **CẤM business transaction nặng trong Filter.**
- **CẤM query DB phức tạp cho mọi request nếu không thực sự cần.**
- **CẤM authorization chỉ dựa vào việc frontend ẩn URL/button.**

## JDBC

- **CẤM nối chuỗi SQL với user input.**
- **CẤM dùng `Statement` + string concatenation cho parameterized query.**
- **CẤM SQL Injection-prone code.**
- **CẤM quên rollback khi transaction thất bại.**
- **CẤM quên commit khi transaction thành công.**
- **CẤM mở Connection mà không đóng.**
- **CẤM mở Statement mà không đóng.**
- **CẤM mở ResultSet mà không đóng.**
- **CẤM empty `catch(SQLException e)`.**
- **CẤM chỉ log message rồi bỏ qua SQLState/vendor code trong trường hợp cần điều tra lỗi DB.**
- **CẤM tạo connection pool thứ hai khi framework đã quản lý DataSource.**
- **CẤM hardcode database password trong Java source.**
- **CẤM `SELECT *` mặc định khi chỉ cần một số column.**

## JPA / Hibernate

- **CẤM Controller gọi `EntityManager`.**
- **CẤM transaction nghiệp vụ ở Controller.**
- **CẤM đổi relation sang EAGER chỉ để né LazyInitializationException.**
- **CẤM `@ManyToOne` không khai báo `fetch = FetchType.LAZY` theo standard này.**
- **CẤM `@OneToOne` không khai báo `fetch = FetchType.LAZY` theo OWNER HARDENING.**
- **CẤM bỏ qua N+1 Query.**
- **CẤM sửa N+1 bằng cách bật EAGER toàn hệ thống.**
- **CẤM ghép user input vào JPQL string.**
- **CẤM query danh sách cực lớn mà không pagination.**
- **CẤM multiple collection fetch join tùy tiện gây Cartesian explosion.**
- **CẤM dùng Entity trực tiếp làm API response khi có field nội bộ/nhạy cảm hoặc API cần contract riêng.**
- **CẤM giả định Detached Entity tự động được dirty-check.**
- **CẤM `@Modifying` query mà không xem xét transaction.**
- **CẤM business logic phức tạp trong Repository.**

## Authentication / Password

- **CẤM lưu password plaintext.**
- **CẤM MD5.**
- **CẤM SHA-1 cho password.**
- **CẤM plain SHA-256/SHA-512 làm password hash.**
- **CẤM Base64 password.**
- **CẤM reversible encryption thay cho password hashing.**
- **CẤM tự viết password hashing algorithm.**
- **CẤM compare raw password với database string bằng `equals()`.**
- **BẮT BUỘC dùng `PasswordEncoder`/BCrypt theo Spring Security standard của course project.**

## Authorization / RBAC

- **CẤM coi Authentication đồng nghĩa Authorization.**
- **CẤM user đã login mặc định được truy cập mọi resource.**
- **CẤM chỉ kiểm tra Role khi resource còn có Hub/ownership/task scope.**
- **CẤM lấy `hubId`, `userId`, `vendorId` từ request rồi mặc định tin đó là scope của authenticated user.**
- **CẤM bỏ qua resource ownership.**
- **CẤM trả `200 OK` cho request thực tế bị forbidden.**
- **CẤM dùng 401 cho trường hợp user đã xác thực nhưng không đủ quyền; trường hợp này phải là 403.**
- **CẤM authorization chỉ ở JSP/JavaScript.**
- **CẤM để endpoint nhạy cảm tồn tại mà không có backend authorization.**

## Sensitive Data / API Security

- **CẤM trả password/password hash trong DTO.**
- **CẤM trả secret key.**
- **CẤM log password.**
- **CẤM log raw access token/refresh token nếu không có lý do bảo mật đặc biệt.**
- **CẤM đặt credential vào source repository.**
- **CẤM gửi credential qua HTTP plaintext trong production.**
- **CẤM bỏ qua input validation chỉ vì endpoint đã được authentication.**
- **CẤM tắt CSRF/CORS/security feature một cách máy móc chỉ vì code mẫu trên mạng làm vậy.**

---

# Final Architecture Invariants

AI Agent trước khi hoàn thành bất kỳ thay đổi nào MUST tự kiểm tra:

```text
[ ] Controller chỉ xử lý HTTP/input/output.
[ ] Controller gọi Service, KHÔNG gọi Repository.
[ ] Service có Interface + Implementation hậu tố Impl.
[ ] Business logic nằm ở Service.
[ ] Repository/DAO chỉ xử lý data access.
[ ] Constructor Injection được ưu tiên.
[ ] Không Field Injection.
[ ] DTO được dùng tại boundary khi cần.
[ ] Entity không bị expose nhạy cảm.
[ ] Servlet/Jakarta dùng namespace jakarta.*.
[ ] JSP không có Scriptlet.
[ ] JSTL dùng Jakarta 3.0 URI.
[ ] JSP không truy cập database.
[ ] Validation có cả client-side và server-side.
[ ] Validation failure giữ errors + formData.
[ ] Successful POST áp dụng PRG.
[ ] Cookie security có HttpOnly/SameSite và Secure trên HTTPS.
[ ] Logout invalidate Session.
[ ] Filter không chứa business logic nặng.
[ ] JDBC sử dụng PreparedStatement.
[ ] JDBC sử dụng try-with-resources.
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
> Khi có nhiều cách để giải quyết một yêu cầu, ưu tiên theo thứ tự:
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