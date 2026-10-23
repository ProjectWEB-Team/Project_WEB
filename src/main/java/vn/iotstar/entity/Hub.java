package vn.iotstar.entity;

import jakarta.persistence.*;

/** Read-only hub view for Phase-02 scope checks; hub administration belongs to Phase 03. */
@Entity
@Table(name = "hubs")
public class Hub {
    @Id private Long id;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String status;

    protected Hub() { }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getStatus() { return status; }
}
