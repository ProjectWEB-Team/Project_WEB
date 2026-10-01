# PROJECT_SPEC_KNOWLEDGE_BASE: LOGISTIC UTEEXPRESS
## GROUND TRUTH CONTEXT FOR AI CODING AGENT (CODEX / GSD CORE)

> **MỤC TIÊU:** Tài liệu này là **Core Lecture Ground Truth**, chứa kiến thức kỹ thuật, quy chuẩn lập trình, cấu hình và code pattern nguyên bản được trích xuất từ 17 tài liệu bài giảng Lập trình Web. Nó authoritative cho kiến thức lecture nhưng không exhaustive cho mọi yêu cầu của Logistic UTEExpress.
>
> Supplementary engineering knowledge MAY được dùng khi `UTEExpress_Workflow.md` hoặc `ARCHITECTURE_DECISIONS.md` yêu cầu một feature, hoặc khi lecture chưa định nghĩa đủ để triển khai feature bắt buộc. Supplementary knowledge MUST NOT override quyết định ở tầng authority cao hơn, tự thay đổi technology stack, thay thế pattern đã được định nghĩa mà không có justification, hoặc tạo business requirement mới.
>
> **Authority order:** `ARCHITECTURE_DECISIONS.md` > `UTEExpress_Workflow.md` > `ERD.md` > `PROJECT_SPEC_KNOWLEDGE_BASE.md` > `ENGINEERING_STANDARDS.md` > supplementary engineering knowledge.
>
> Code và naming pattern trong tài liệu được giữ nguyên như **SOURCE EXAMPLE**. Chúng không tự động là project rule khi ADR hoặc tài liệu authority cao hơn đã quyết định khác.

---

## CHƯƠNG 1: TECH STACK & MAVEN DEPENDENCIES (`pom.xml`)

### 1.1 Quản lý Thư viện Dự án
* **Concept:** Khhai báo toàn bộ các thư viện Maven cần thiết cho dự án Spring Boot 3/4 kết hợp JSP/JSTL, Sitemesh 3, Spring Security 6, JWT, MapStruct, JPA và SQL Server.
* **Rule:** Mọi dependency phải khai báo chính xác `groupId`, `artifactId`, `version` và `scope` theo đúng quy chuẩn giảng viên. Không dùng Thymeleaf cho giao diện JSP chính.
* **Source behavior:** Giảng viên yêu cầu nạp `tomcat-embed-jasper` (scope compile) và `jakarta.servlet.jsp.jstl` để Spring Boot có thể render file JSP từ nhúng Tomcat.

#### Code / Example (`pom.xml`):
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>vn.iotstar</groupId>
    <artifactId>UTEExpress</artifactId>
    <version>1.0.0</version>
    <name>UTEExpress</name>
    <description>Hệ thống Quản lý Logistic UTEExpress</description>

    <properties>
        <java.version>17</java.version>
        <org.mapstruct.version>1.5.5.Final</org.mapstruct.version>
        <jjwt.version>0.12.6</jjwt.version>
    </properties>

    <dependencies>
        <!-- 1. Spring Boot Web & Core -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>

        <!-- 2. Spring Security 6 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- 3. JSP & JSTL View Engine (Embedded Tomcat Jasper) -->
        <dependency>
            <groupId>org.apache.tomcat.embed</groupId>
            <artifactId>tomcat-embed-jasper</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet.jsp.jstl</groupId>
            <artifactId>jakarta.servlet.jsp.jstl-api</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>org.glassfish.web</groupId>
            <artifactId>jakarta.servlet.jsp.jstl</artifactId>
            <scope>compile</scope>
        </dependency>
        <dependency>
            <groupId>jakarta.servlet.jsp</groupId>
            <artifactId>jakarta.servlet.jsp-api</artifactId>
            <version>4.0.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- 4. Layout Decorator: Sitemesh 3 -->
        <dependency>
            <groupId>org.sitemesh</groupId>
            <artifactId>spring-boot-starter-sitemesh</artifactId>
            <version>3.3.0-RC1</version>
        </dependency>

        <!-- 5. JWT Authorization (JJWT) -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
        </dependency>

        <!-- 6. DTO Mapping: MapStruct & Lombok -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${org.mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 7. Database Drivers & File Utilities -->
        <dependency>
            <groupId>com.microsoft.sqlserver</groupId>
            <artifactId>mssql-jdbc</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>2.16.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.32</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${org.mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```
* **Constraints:** Phạm vi scope của `tomcat-embed-jasper` phải là `compile` (không dùng `provided` vì sẽ gây lỗi 404/500 khi chạy nhúng JAR/WAR trong Spring Boot).
* **Notes:** Nếu sử dụng SQL Server Windows Authentication, nguồn tài liệu yêu cầu chép các tập tin `.dll` (`mssql-jdbc_auth-9.4.1.x64.dll`) vào thư mục `bin` của JDK.

---

### 1.2 Cấu hình Môi trường (`application.properties`)
* **Concept:** Khai báo cấu hình hệ thống: View Resolver, CSDL SQL Server, JPA/Hibernate, Upload File Multipart và thông số JWT.
* **Rule:** Cấu hình chuẩn View Prefix `/WEB-INF/views/` và Suffix `.jsp`.
* **Source behavior:** `spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER` bắt buộc phải có để Sitemesh Filter và Swagger hoạt động ổn định trên Spring Boot 3+.

#### Code / Example (`application.properties`):
```properties
# Server Configuration
server.port=8080
spring.application.name=UTEExpress

# View Resolver cho JSP
spring.mvc.view.prefix=/WEB-INF/views/
spring.mvc.view.suffix=.jsp
spring.mvc.static-path-pattern=/static/**
spring.mvc.pathmatch.matching-strategy=ANT_PATH_MATCHER

# Encoding
spring.mandatory-file-encoding=UTF-8
spring.mail.default-encoding=UTF-8

# Kết nối CSDL SQL Server
spring.datasource.driverClassName=com.microsoft.sqlserver.jdbc.SQLServerDriver
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=LogisticUTEExpressDB;encrypt=false;trustServerCertificate=true;sslProtocol=TLSv1.2;characterEncoding=UTF-8
spring.datasource.username=sa
spring.datasource.password=1234567@a$

# JPA / Hibernate Configuration
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

# Cấu hình Upload File Multipart
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=10MB

# Cấu hình JWT Security
security.jwt.secret-key=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
security.jwt.expiration-time=3600000
```

---

### 1.3 Cấu trúc Thư mục Dự án (Directory Structure Convention)
* **Concept:** Phân chia Layer chuẩn theo kiến trúc 3 lớp (3-Tier Layered Architecture).
* **Rule:** Package gốc **bắt buộc** là `vn.iotstar.*`.

```text
src/main/java/vn/iotstar/
├── configs/            # WebConfig, SecurityConfiguration, SiteMeshFilter, TomcatJSPConfiguration
├── controllers/        # Spring MVC Controllers (Trả về JSP Views)
│   └── api/            # REST Controllers (@RestController xử lý AJAX / JSON)
├── dto/                # Data Transfer Objects & Validation Models
├── entity/             # JPA Entities (Mapping CSDL SQL Server)
├── filter/             # JwtAuthenticationFilter (OncePerRequestFilter)
├── mapper/             # Mapper Interface / Class (MapStruct & Custom)
├── repository/         # Spring Data JPA Repositories (JpaRepository)
└── services/           # Service Interfaces (tiền tố I) & Implementations (package impl)
    └── impl/           # Class triển khai Service (@Service, @Transactional)

src/main/resources/
└── META-INF/
    └── WEB-INF/
        ├── decorators/ # Layout Sitemesh (web.jsp, admin.jsp, login.jsp)
        └── views/      # JSP Views (web/home.jsp, admin/category/list.jsp)
```

**SOURCE EXAMPLE — service naming:** Cây source ở trên minh họa `IUserService`, `ICategoryService`, `IStorageService` và implementation tương ứng. Giữ nguyên các tên này để bảo toàn bài giảng; chúng không phải naming rule của UTEExpress. Theo ADR-008, project mặc định dùng `UserService` / `UserServiceImpl` trừ khi có Project Decision mới.

---

## CHƯƠNG 2: TEMPLATE ENGINE & LAYOUT DECORATOR

### 2.1 Cấu hình Tomcat Embedded JSP Dynamic Loader
* **Concept:** Đăng ký `LifecycleListener` cho Embedded Tomcat để nạp tĩnh tài nguyên JSP trong thư mục `META-INF/WEB-INF`.
* **Rule:** Khai báo 2 class `JSPStaticResourceConfigurer` và `TomcatJSPConfiguration`.

#### Code / Example:
```java
// File: vn/iotstar/configs/JSPStaticResourceConfigurer.java
package vn.iotstar.configs;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.WebResourceRoot;
import org.springframework.util.ResourceUtils;

import java.net.URI;
import java.net.URL;

public class JSPStaticResourceConfigurer implements LifecycleListener {
    private final Context context;
    private final String subPath = "/META-INF";

    public JSPStaticResourceConfigurer(Context context) {
        this.context = context;
    }

    @Override
    public void lifecycleEvent(org.apache.catalina.LifecycleEvent event) {
        if (!event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
            return;
        }

        final URL finalLocation = getUrl();
        this.context.getResources().createWebResourceSet(
                WebResourceRoot.ResourceSetType.RESOURCE_JAR,
                "/",
                finalLocation,
                subPath
        );
    }

    private URL getUrl() {
        final URL location = this.getClass().getProtectionDomain().getCodeSource().getLocation();

        if (ResourceUtils.isFileURL(location)) {
            return location;
        } else if (ResourceUtils.isJarURL(location)) {
            try {
                String locationStr = location.getPath()
                        .replaceFirst("^nested:", "")
                        .replaceFirst("/!BOOT-INF/classes/!/$", "!/");
                return new URI("jar:file", locationStr, null).toURL();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to add new JSP source URI to tomcat resources", e);
            }
        } else {
            throw new IllegalStateException("Can not add tomcat resources, unhandleable url: " + location);
        }
    }
}
```

```java
// File: vn/iotstar/configs/TomcatJSPConfiguration.java
package vn.iotstar.configs;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatJSPConfiguration {

    @Bean
    public WebServerFactoryCustomizer<WebServerFactory> staticResourceCustomizer() {
        return factory -> {
            if (factory instanceof TomcatServletWebServerFactory tomcatFactory) {
                tomcatFactory.addContextCustomizers(context -> 
                    context.addLifecycleListener(new JSPStaticResourceConfigurer(context))
                );
            }
        };
    }
}
```

---

### 2.2 Sitemesh 3 Layout Filter
* **Concept:** Trang trí giao diện (Decorator Pattern) giúp tách biệt khung Layout chính (Header, Footer, Sidebar) với phần ruột của từng trang.
* **Rule:** Kế thừa `ConfigurableSiteMeshFilter` và đăng ký Bean `FilterRegistrationBean`.

#### Code / Example:
```java
// File: vn/iotstar/configs/CustomSiteMeshFilter.java
package vn.iotstar.configs;

import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class CustomSiteMeshFilter extends ConfigurableSiteMeshFilter {
    @Override
    protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
        builder.addDecoratorPath("/*", "/WEB-INF/decorators/web.jsp")
               .addDecoratorPath("/admin/*", "/WEB-INF/decorators/admin.jsp")
               .addExcludedPath("/login*")
               .addExcludedPath("/login/*")
               .addExcludedPath("/alogin*")
               .addExcludedPath("/alogin/*")
               .addExcludedPath("/api/**")
               .addExcludedPath("/swagger-ui**");
    }
}

@Configuration
class SiteMeshConfig {
    @Bean
    public FilterRegistrationBean<CustomSiteMeshFilter> siteMeshFilter() {
        FilterRegistrationBean<CustomSiteMeshFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new CustomSiteMeshFilter());
        filterRegistrationBean.addUrlPatterns("/*");
        return filterRegistrationBean;
    }
}
```

---

### 2.3 Cấu hình Encoding Unicode Filter & JSP Taglib
* **Concept:** Đảm bảo tiếng Việt hiển thị chính xác (UTF-8).
* **Rule:** Đăng ký `CharacterEncodingFilter` với ưu tiên cao nhất (`Ordered.HIGHEST_PRECEDENCE`).

```java
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public CharacterEncodingFilter characterEncodingFilter() {
    CharacterEncodingFilter filter = new CharacterEncodingFilter();
    filter.setEncoding("UTF-8");
    filter.setForceEncoding(true);
    return filter;
}
```

Khai báo Taglib chuẩn trên đầu các file JSP (`/common/taglib.jsp`):
```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
```

---

## CHƯƠNG 3: ENTITY & DATABASE PERSISTENCE (SPRING DATA JPA)

### 3.1 Quy chuẩn JPA Entity Mapping
* **Rule 1:** Mọi Entity phải `implements Serializable` và có `private static final long serialVersionUID = 1L;`.
* **Rule 2:** Khóa chính tự động tăng dùng `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
* **Rule 3:** Cột chứa tiếng Việt dùng `columnDefinition = "nvarchar(...)"` hoặc `"NVARCHAR(...)"`.
* **Rule 4:** Tránh vòng lặp vô hạn JSON bằng cách gắn `@JsonIgnore` tại quan hệ `@OneToMany` hoặc `@ManyToOne`.

#### Code / Example:

```java
// File: vn/iotstar/entity/User.java
package vn.iotstar.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "users")
public class User implements UserDetails, Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String fullName;

    @Column(unique = true, length = 100, nullable = false)
    private String email;

    @Column(columnDefinition = "nvarchar(500)")
    private String images;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private Date createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Date updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

```java
// File: vn/iotstar/entity/Category.java
package vn.iotstar.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "categories")
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(name = "category_name", columnDefinition = "nvarchar(200)", nullable = false)
    private String categoryName;

    @Column(columnDefinition = "nvarchar(500)")
    private String icon;

    @JsonIgnore
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Product> products;
}
```

```java
// File: vn/iotstar/entity/Product.java
package vn.iotstar.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Entity
@Table(name = "products")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "nvarchar(200)", nullable = false)
    private String name;

    private Double price;
    private Integer quantity;

    @Column(columnDefinition = "nvarchar(1000)")
    private String description;

    @Column(length = 500)
    private String images;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "categoryId")
    private Category category;
}
```

---

### 3.2 Repositories & Dynamic Querying
* **Concept:** Khai báo Interface kế thừa `JpaRepository<Entity, ID>`.
* **Rule:** Tự động sinh SQL dựa vào quy ước đặt tên method (Query Keywords).

```java
package vn.iotstar.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.iotstar.entity.Category;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Page<Category> findByCategoryNameContaining(String name, Pageable pageable);
    Optional<Category> findByCategoryName(String name);
}

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
```

---

### 3.3 Phân Trang & Tìm Kiếm trong Controller (Pagination & Search Logic)
* **Concept:** Xử lý tham số `page`, `size` và tính toán danh sách số trang hiển thị trên View JSP.
* **Rule:** Trang mặc định bắt đầu từ `1` ở Client, nhưng Spring Data JPA tính index từ `0` -> `PageRequest.of(currentPage - 1, pageSize, sort)`.

```java
// Snippet xử lý phân trang chuẩn của giảng viên
int currentPage = page.orElse(1);
int pageSize = size.orElse(5);

Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("categoryName"));
Page<Category> resultPage;

if (StringUtils.hasText(name)) {
    resultPage = categoryService.findByCategoryNameContaining(name, pageable);
    model.addAttribute("name", name);
} else {
    resultPage = categoryService.findAll(pageable);
}

int totalPages = resultPage.getTotalPages();
if (totalPages > 0) {
    int start = Math.max(1, currentPage - 2);
    int end = Math.min(currentPage + 2, totalPages);

    List<Integer> pageNumbers = IntStream.rangeClosed(start, end)
            .boxed()
            .collect(Collectors.toList());
    model.addAttribute("pageNumbers", pageNumbers);
}

model.addAttribute("categoryPage", resultPage);
```

---

## CHƯƠNG 4: DATA TRANSFER OBJECT (DTO), MAPPER & FILE UPLOAD

### 4.1 Quy chuẩn DTO Pattern
* **Rule:** DTO dùng để trao đổi dữ liệu với Client (Form HTML / JSON Request), tách biệt với Entity CSDL. Với form Upload File, DTO chứa cả trường `String images` (đường dẫn lưu DB) và `MultipartFile image` (file nhị phân gửi từ Form).

```java
// File: vn/iotstar/dto/ProductDTO.java
package vn.iotstar.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    private Integer quantity;
    private String description;
    private String images;
    private MultipartFile image; // Nhận file upload từ HTML Form
    private Long categoryId;
}
```

---

### 4.2 Mapper Conventions (MapStruct & Manual Mapper)
* **Concept:** Chuyển đổi dữ liệu `Entity <-> DTO` và cập nhật thông tin Entity khi `UPDATE`.
* **CRITICAL RULE FOR UPDATE:** Khi cập nhật dữ liệu, **không tạo Entity mới** (`new Product()`). Phải lấy Entity đang tồn tại từ Repository, dùng `updateEntity(dto, entity)` để đè giá trị mới, sau đó gọi `repository.save(entity)`.

```java
// File: vn/iotstar/mapper/ProductMapper.java
package vn.iotstar.mapper;

import org.springframework.stereotype.Component;
import vn.iotstar.dto.ProductDTO;
import vn.iotstar.entity.Product;

@Component
public class ProductMapper {

    public ProductDTO toDTO(Product entity) {
        if (entity == null) return null;

        return ProductDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .description(entity.getDescription())
                .images(entity.getImages())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getCategoryId() : null)
                .build();
    }

    public Product toEntity(ProductDTO dto) {
        if (dto == null) return null;

        return Product.builder()
                .id(dto.getId())
                .name(dto.getName())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .description(dto.getDescription())
                .images(dto.getImages())
                .build();
    }

    // Cập nhật giá trị trực tiếp trên Entity hiện có trong Persistence Context
    public void updateEntity(ProductDTO dto, Product entity) {
        entity.setName(dto.getName());
        entity.setPrice(dto.getPrice());
        entity.setQuantity(dto.getQuantity());
        entity.setDescription(dto.getDescription());

        if (dto.getImages() != null) {
            entity.setImages(dto.getImages());
        }
    }
}
```

---

### 4.3 Multipart File Storage Service & Web Static Resource Handler
* **Concept:** Lưu trữ file đính kèm vào hệ thống tệp local (`uploads/`) và phục vụ truy cập file qua URL `/uploads/**`.

```java
// File: vn/iotstar/services/impl/FileSystemStorageServiceImpl.java
package vn.iotstar.services.impl;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.services.IStorageService;

import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileSystemStorageServiceImpl implements IStorageService {

    private final Path rootLocation = Paths.get("uploads");

    public FileSystemStorageServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    @Override
    public String getStorageFilename(MultipartFile file, String id) {
        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        return "p" + id + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
    }

    @Override
    public void store(MultipartFile file, String storeFilename) {
        try {
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file");
            }
            Path destinationFile = this.rootLocation.resolve(Paths.get(storeFilename))
                    .normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }
}
```

Đăng ký Handler ánh xạ tài nguyên tĩnh `/uploads/**` (`WebConfig.java`):
```java
package vn.iotstar.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

---

## CHƯƠNG 5: BẢO MẬT HỆ THỐNG (SPRING SECURITY 6 & JWT)

> **SOURCE EXAMPLE SCOPE:** Các ví dụ ở chương này mô tả Stateless JWT cho REST API. UTEExpress áp dụng hai security chains theo Workflow (session cho JSP/Web và JWT cho REST), vì vậy các ví dụ source không được đọc như một rule universal thay thế Workflow.

### 5.1 Kiến trúc Xác thực Vô trạng (Stateless JWT Security Architecture)
* **Rule 1:** Cấu hình Security 6 sử dụng `SessionCreationPolicy.STATELESS`.
* **Rule 2:** Tắt CSRF (`csrf.disable()`) đối với các ứng dụng RESTful API.
* **Rule 3:** Đánh chặn mọi request bằng `JwtAuthenticationFilter` chạy trước `UsernamePasswordAuthenticationFilter`.

---

### 5.2 Implement JWT Service (`JwtService.java`)

```java
package vn.iotstar.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration-time}")
    private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public long getExpirationTime() {
        return jwtExpiration;
    }

    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

---

### 5.3 Filter Đánh chặn JWT (`JwtAuthenticationFilter.java`)

```java
package vn.iotstar.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import vn.iotstar.services.JwtService;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final HandlerExceptionResolver handlerExceptionResolver;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService,
                                   HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception exception) {
            handlerExceptionResolver.resolveException(request, response, null, exception);
        }
    }
}
```

---

### 5.4 Cấu hình SecurityFilterChain (`SecurityConfiguration.java`)

```java
package vn.iotstar.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.iotstar.filter.JwtAuthenticationFilter;
import vn.iotstar.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                                 AuthenticationProvider authenticationProvider) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/login**", "/static/**", "/uploads/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}

@Configuration
class ApplicationConfiguration {

    private final UserRepository userRepository;

    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}
```

---

## CHƯƠNG 6: REST API & FRONTEND AJAX INTEGRATION

### 6.1 Wrapper Response Model & RestController
* **SOURCE PATTERN / CONFLICT:** Lecture mô tả wrapper `Response(Boolean status, String message, Object body)`, nhưng chính code example bên dưới trả raw `List<Category>` ở `getAll()`. Đây là source conflict/example distinction, không phải universal project rule. Không sửa code example; contract response của endpoint UTEExpress phải theo ADR/Workflow hoặc Project Decision áp dụng.

```java
// File: vn/iotstar/model/Response.java
package vn.iotstar.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Response {
    private Boolean status;
    private String message;
    private Object body;
}
```

```java
// File: vn/iotstar/controllers/api/CategoryAPIController.java
package vn.iotstar.controllers.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.iotstar.entity.Category;
import vn.iotstar.model.Response;
import vn.iotstar.services.ICategoryService;
import vn.iotstar.services.IStorageService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/category")
public class CategoryAPIController {

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IStorageService storageService;

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<Category> list = categoryService.findAll();
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @PostMapping(path = "/addCategory")
    public ResponseEntity<?> addCategory(@RequestParam("categoryName") String categoryName,
                                        @RequestParam("icon") MultipartFile icon) {
        Category category = new Category();
        category.setCategoryName(categoryName);

        if (!icon.isEmpty()) {
            String storeFilename = storageService.getStorageFilename(icon, "cat");
            storageService.store(icon, storeFilename);
            category.setIcon(storeFilename);
        }

        categoryService.save(category);
        return new ResponseEntity<>(new Response(true, "Thêm thành công", category), HttpStatus.OK);
    }

    @PutMapping(path = "/updateCategory")
    public ResponseEntity<?> updateCategory(@RequestParam("categoryId") Long categoryId,
                                           @RequestParam("categoryName") String categoryName,
                                           @RequestParam(value = "icon", required = false) MultipartFile icon) {
        Optional<Category> opt = categoryService.findById(categoryId);
        if (opt.isEmpty()) {
            return new ResponseEntity<>(new Response(false, "Không tìm thấy Category", null), HttpStatus.NOT_FOUND);
        }

        Category category = opt.get();
        category.setCategoryName(categoryName);

        if (icon != null && !icon.isEmpty()) {
            String storeFilename = storageService.getStorageFilename(icon, "cat");
            storageService.store(icon, storeFilename);
            category.setIcon(storeFilename);
        }

        categoryService.save(category);
        return new ResponseEntity<>(new Response(true, "Cập nhật thành công", category), HttpStatus.OK);
    }

    @DeleteMapping(path = "/deleteCategory")
    public ResponseEntity<?> deleteCategory(@RequestParam("categoryId") Long categoryId) {
        categoryService.deleteById(categoryId);
        return new ResponseEntity<>(new Response(true, "Xóa thành công", null), HttpStatus.OK);
    }
}
```

---

### 6.2 Frontend Mã AJAX & Modal Bootstrap trên JSP
* **Rule:** Sử dụng `FormData` cho request có đính kèm File, thiết lập `contentType: false` và `processData: false` trong `$.ajax()`.

```html
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="/common/taglib.jsp"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Quản lý Category AJAX</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css"/>
    <script src="https://code.jquery.com/jquery-3.6.4.min.js"></script>
    <script>var contextPath = "${pageContext.request.contextPath}";</script>
</head>
<body>
<div class="container mt-4">
    <h2>Danh sách Danh mục (AJAX REST API)</h2>
    <p>
        <button class="btn btn-success" onclick="showCreateNewCategoryModal()">
            <i class="fas fa-plus me-2"></i>Thêm Category
        </button>
    </p>

    <!-- Bảng danh sách Category -->
    <table class="table table-striped table-bordered" id="categoryTable">
        <thead>
            <tr>
                <th>ID</th>
                <th>Hình ảnh</th>
                <th>Tên danh mục</th>
                <th>Thao tác</th>
            </tr>
        </thead>
        <tbody></tbody>
    </table>
</div>

<!-- Modal Create Category -->
<div class="modal fade" id="createCategoryModal" tabindex="-1">
  <div class="modal-dialog">
   <div class="modal-content">
    <form id="addCategory" method="post" enctype="multipart/form-data">
        <div class="modal-header">
            <h5 class="modal-title">Thêm Category mới</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="form-group mb-3">
                <label for="new_categoryname">Tên Category</label>
                <input type="text" class="form-control" id="new_categoryname" name="categoryName" required>
            </div>
            <div class="form-group mb-3">
                <label for="new_icon">Icon Image</label>
                <input type="file" class="form-control" id="new_icon" name="icon" required>
            </div>
        </div>
        <div class="modal-footer">
            <button type="submit" class="btn btn-primary">Lưu</button>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
        </div>
    </form>
   </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.2/dist/js/bootstrap.bundle.min.js"></script>
<script type="text/javascript">
    $(document).ready(function() {
        loadCategories();
    });

    // 1. Tải dữ liệu danh mục
    function loadCategories() {
        $.getJSON(contextPath + '/api/category', function(json) {
            var tr = [];
            for (var i = 0; i < json.length; i++) {
                tr.push('<tr>');
                tr.push('<td>' + json[i].categoryId + '</td>');
                tr.push('<td><img src="/uploads/' + json[i].icon + '" style="width:60px;height:60px;object-fit:cover;"></td>');
                tr.push('<td>' + json[i].categoryName + '</td>');
                tr.push('<td>' 
                    + '<button onclick="openEditModal(' + json[i].categoryId + ',\'' + json[i].categoryName + '\')" class="btn btn-outline-warning me-2"><i class="fa fa-edit"></i></button>'
                    + '<button data-id="' + json[i].categoryId + '" id="btnDeleteCate" class="btn btn-outline-danger"><i class="fa fa-trash"></i></button>'
                    + '</td>');
                tr.push('</tr>');
            }
            $('#categoryTable tbody').html(tr.join(''));
        });
    }

    // 2. Submit Form Thêm Mới
    $("form#addCategory").submit(function(e) {
        e.preventDefault();
        var formData = new FormData(this);
        $.ajax({
            url: contextPath + '/api/category/addCategory',
            type: 'POST',
            data: formData,
            cache: false,
            contentType: false,
            processData: false,
            success: function (data) {
                bootstrap.Modal.getInstance(document.getElementById('createCategoryModal')).hide();
                loadCategories();
            }
        });
    });

    // 3. Xóa Category
    $(document).delegate('#btnDeleteCate', 'click', function() {
        var id = $(this).data('id');
        if (confirm('Bạn có chắc chắn muốn xóa danh mục này?')) {
            $.ajax({
                type: "DELETE",
                url: contextPath + '/api/category/deleteCategory?categoryId=' + id,
                success: function() {
                    loadCategories();
                }
            });
        }
    });
</script>
</body>
</html>
```

---

## CHƯƠNG 7: BẢNG TỔNG HỢP SOURCE CONFLICTS

Dưới đây là danh sách các điểm mâu thuẫn giữa các tài liệu nguồn bài giảng, được ghi nhận đầy đủ để AI Coding Agent nhận biết:

| Chủ đề / Nội dung | Phiên bản 1 (Mô tả & Nguồn) | Phiên bản 2 (Mô tả & Nguồn) | Đánh dấu Conflict | Trạng thái |
| :--- | :--- | :--- | :--- | :--- |
| **Sitemesh Layout Config** | Sử dụng XML `sitemesh3.xml` trong `WEB-INF` kết hợp `web.xml` filter `sitemesh`. (*Source 09_LayoutwithSitemesh3*) | Sử dụng Java Config `CustomSiteMeshFilter` kế thừa `ConfigurableSiteMeshFilter` và `@Bean FilterRegistrationBean`. (*Source WEPR330479_Cấu hình JSP*) | `[CONFLICT_SITEMESH_CONFIG]` | `UNRESOLVED_BY_SOURCE` |
| **Sitemesh Dependency** | `org.sitemesh:sitemesh:3.2.1` (*Source 09*) | `org.sitemesh:spring-boot-starter-sitemesh:3.3.0-RC1` (*Source WEPR330479_Cấu hình JSP*) | `[CONFLICT_SITEMESH_DEP]` | `UNRESOLVED_BY_SOURCE` |
| **User role storage** | Class `Users` mapped `@Table(name = "users")`, có `@ManyToMany` với `Role`. (*Source 03_springsecurity6*) | Class `User` implements `UserDetails`, `@Table(name = "users")`, không có bảng `Role` tách rời. (*Source 04_JWT*) | `[CONFLICT_USER_ENTITY]` | `RESOLVED_BY_PROJECT_ADR (ADR-002)`: MVP dùng `users.role`, không tự tạo role/permission tables. Tên class nguồn vẫn là SOURCE EXAMPLE. |
| **Thư viện JWT** | Sử dụng thư viện `io.jsonwebtoken:jjwt-api:0.12.6` (*Source 04_JWT*) | Trong đề bài người dùng ghi Nimbus JOSE JWT (*Tuy nhiên nguồn bài giảng sử dụng JJWT 0.12.6*) | `[CONFLICT_JWT_LIB]` | `UNRESOLVED_BY_SOURCE` |
| **Kiến trúc Frontend** | Bài giảng DTO Mapper số 4 minh họa giao diện Thymeleaf. (*Source HƯỚNG DẪN MAPPER DTO*) | Đề bài và toàn bộ bài giảng thực hành khác bắt buộc dùng JSP/JSTL. (*Source WEPR330479_Cấu hình JSP*) | `[CONFLICT_VIEW_ENGINE]` | `RESOLVED_BY_PROJECT_ADR (ADR-007)`: frontend chính dùng JSP/JSTL + Bootstrap. |
| **REST response wrapper** | Source rule mô tả `Response(Boolean status, String message, Object body)`. | `getAll()` trong chính source example trả raw `List<Category>`. | `[CONFLICT_REST_RESPONSE_WRAPPER]` | `SOURCE_EXAMPLE_CONFLICT`: không phải universal project rule; cần ADR/Workflow/Project Decision nếu endpoint yêu cầu contract cụ thể. |

---

## CHƯƠNG 8: TÍNH NĂNG KHÔNG CÓ TRONG TÀI LIỆU (`NOT_DEFINED_IN_SOURCE`)

Dưới đây là danh sách đánh giá các công nghệ/tính năng xuất hiện trong yêu cầu đề tài của người dùng đối chiếu với 17 tài liệu nguồn bài giảng:

> `NOT_DEFINED_IN_SOURCE` là **provenance marker**: lecture materials không định nghĩa item đó. Nó không phải lệnh cấm implement. Khi Workflow/ADR yêu cầu feature, Coding Agent có thể dùng supplementary engineering knowledge để triển khai, miễn không override tầng authority cao hơn hoặc tự tạo business requirement mới.

1. **WebSocket:**
   * Trạng thái: `NOT_DEFINED_IN_SOURCE`
   * Chi tiết: Không có tài liệu bài giảng nào hướng dẫn về WebSocket, STOMP, `@EnableWebSocketMessageBroker` hay SockJS.

2. **PostgreSQL:**
   * Trạng thái: `NOT_DEFINED_IN_SOURCE`
   * Chi tiết: Không có tài liệu bài giảng nào hướng dẫn cấu hình hoặc kết nối với CSDL PostgreSQL.

3. **MySQL:**
   * Trạng thái: **CÓ TRONG TÀI LIỆU NGUỒN** (Không phải `NOT_DEFINED_IN_SOURCE`)
   * Chi tiết: Được đề cập và cung cấp chuỗi cấu hình mẫu trong slide Spring Security (*Source 03_springsecurity6*), bao gồm driver `com.mysql.cj.jdbc.Driver` / `com.mysql.jdbc.Driver` và URL `jdbc:mysql://localhost:3306/...`. Hầu hết các bài giảng thực hành chi tiết còn lại tập trung vào SQL Server.

4. **Nghiệp vụ chi tiết của Logistic UTEExpress (Đơn hàng, Vận chuyển, Tracking):**
   * Trạng thái: `NOT_DEFINED_IN_SOURCE`
   * Chi tiết: Tài liệu nguồn bài giảng chỉ có các Entity mẫu về `User`, `Role`, `Category`, `Product`, `ProductImage`, `Cart`, `CartItem`, `Seller`. Các nghiệp vụ Vận chuyển/Logistic chi tiết chưa được định nghĩa trong nguồn.

---

## CHƯƠNG 9: CRITICAL RULES FOR CODING AGENT

AI Coding Agent (Codex / GSD Core) khi lập trình dự án **Logistic UTEExpress** bắt buộc phải tuân thủ nghiêm ngặt các quy tắc kỹ thuật sau:

1. **Source Conflicts Handling Rule:**
   * Giữ nguyên evidence của source conflicts; không tự chọn một phía chỉ dựa trên việc source nào xuất hiện nhiều hơn.
   * Trước khi coi conflict là unresolved cho UTEExpress, kiểm tra `ARCHITECTURE_DECISIONS.md`, Workflow và ERD theo authority order. Conflict đã được ADR giải quyết phải ghi `RESOLVED_BY_PROJECT_ADR` kèm mã ADR.
   * Conflict thực sự chưa có quyết định project vẫn giữ `UNRESOLVED_BY_SOURCE` và cần Project Decision/người dùng quyết định.

2. **Not Defined in Source Rule:**
   * `NOT_DEFINED_IN_SOURCE` chỉ ghi nhận rằng lecture chưa định nghĩa item, ví dụ WebSocket hoặc nghiệp vụ Logistics/Tracking chi tiết.
   * Khi Workflow/ADR yêu cầu feature đó, Coding Agent MAY triển khai bằng supplementary engineering knowledge. Knowledge bổ sung không được override authority cao hơn, thay stack âm thầm, thay thế một pattern đã được project định nghĩa không justification, hoặc tự tạo requirement mới.

3. **Ground Truth & Consistency Rule:**
   * Core Lecture Ground Truth authoritative cho phần lecture nhưng không exhaustive. Critical rule áp dụng cho UTEExpress phải tương thích authority order và không được mâu thuẫn với ADR, Workflow hoặc ERD.
   * Source code examples được giữ nguyên để bảo toàn knowledge bài giảng; chúng không tự động trở thành universal project rule.

4. **Tomcat Embedded JSP Configuration Rule:**
   * Khi triển khai ứng dụng Spring Boot sử dụng View JSP trên Embedded Tomcat, bắt buộc khai báo `TomcatJSPConfiguration` và `JSPStaticResourceConfigurer` (theo bài giảng *WEPR330479_Cấu hình JSP*) để nạp chính xác tài nguyên tĩnh JSP.

5. **Spring Security 6 Configuration Rule:**
   * Cấu hình bảo mật Spring Security 6 không được sử dụng class bị gỡ bỏ như `WebSecurityConfigurerAdapter`. Bắt buộc khai báo Bean `SecurityFilterChain` với Functional Lambdas (`csrf.disable()`, `authorizeHttpRequests(...)`) theo đúng bài giảng *03_springsecurity6*.

6. **Update Entity Persistence Rule:**
   * Khi thực hiện thao tác cập nhật dữ liệu (`UPDATE`), bắt buộc áp dụng phương thức `updateEntity(dto, existingEntity)` để cập nhật trực tiếp lên đối tượng Entity đã load từ Persistence Context trước khi gọi `repository.save()` (theo bài giảng *HƯỚNG DẪN MAPPER DTO*).

7. **Multipart File Upload Form Rule:**
   * Form HTML hỗ trợ upload file bắt buộc phải khai báo thuộc tính `enctype="multipart/form-data"`. Đăng ký ResourceHandler `/uploads/**` ánh xạ tới vị trí lưu trữ `file:uploads/` trong `WebMvcConfigurer` (theo bài giảng *Nội dung upload file bằng Multipart* và *HƯỚNG DẪN CRUD API*).

8. **AJAX Multipart Request Rule:**
   * Khi thực hiện gửi request AJAX bằng jQuery có kèm file Multipart, bắt buộc thiết lập `contentType: false` và `processData: false` để tránh trình duyệt tự động biến đổi `FormData` (theo bài giảng *HƯỚNG DẪN AJAX VỚI RESTFUL API*).

---
*Tài liệu kỹ thuật này là Core Lecture Ground Truth được trích xuất từ 17 tài liệu bài giảng nguồn. Nó là ngữ cảnh authoritative cho lecture, nhưng được áp dụng theo authority order của UTEExpress và không exhaustive.*
