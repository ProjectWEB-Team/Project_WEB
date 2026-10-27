package vn.iotstar.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.HubRoutingRule;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.HubRoutingRuleRepository;
import vn.iotstar.security.AuthenticatedActor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Administrator maintenance of the hub tree and static routing rules (Workflow §23).
 * Hubs are never deleted, only deactivated, and only once nothing depends on them.
 */
@Service
@Transactional
public class AdminNetworkService {
    /** SOC (3) → REGIONAL_HUB (2) → DELIVERY_HUB (1): a parent is exactly one level up, so no cycle can form. */
    private static final Map<String, Integer> LEVELS = Map.of("SOC", 3, "REGIONAL_HUB", 2, "DELIVERY_HUB", 1);
    private static final Set<String> REGIONS = Set.of("NORTH", "CENTRAL", "SOUTH");
    private static final String[] BLOCKERS = {"bưu cục con đang hoạt động", "nhân viên đang làm việc", "quy tắc định tuyến đang dùng",
            "đơn đang nằm tại bưu cục", "bao hàng chưa xong", "chuyến xe chưa xong", "phiếu bàn giao đang mở"};

    // JavaBean getters: the configured Jasper/EL 5 runtime does not read record accessors.
    public record HubRow(Long id, String code, String name, Integer level, String type, String region, String province,
                         String district, String address, Long parentId, String parentCode, String status) {
        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public Integer getLevel() { return level; }
        public String getType() { return type; }
        public String getRegion() { return region; }
        public String getProvince() { return province; }
        public String getDistrict() { return district; }
        public String getAddress() { return address; }
        public Long getParentId() { return parentId; }
        public String getParentCode() { return parentCode; }
        public String getStatus() { return status; }
    }
    public record RouteRow(Long id, String currentHub, String destinationRegion, String destinationProvince, String nextHub,
                           String status) {
        public Long getId() { return id; }
        public String getCurrentHub() { return currentHub; }
        public String getDestinationRegion() { return destinationRegion; }
        public String getDestinationProvince() { return destinationProvince; }
        public String getNextHub() { return nextHub; }
        public String getStatus() { return status; }
    }

    private final HubRepository hubs;
    private final HubRoutingRuleRepository rules;

    public AdminNetworkService(HubRepository hubs, HubRoutingRuleRepository rules) {
        this.hubs = hubs; this.rules = rules;
    }

    @Transactional(readOnly = true)
    public List<HubRow> hubs() {
        requireAdmin();
        return hubs.findAll(Sort.by("level").descending().and(Sort.by("code"))).stream().map(h -> new HubRow(
                h.getId(), h.getCode(), h.getName(), h.getLevel(), h.getType(), h.getRegion(), h.getProvince(),
                h.getDistrict(), h.getAddress(), h.getParentHub() == null ? null : h.getParentHub().getId(),
                h.getParentHub() == null ? null : h.getParentHub().getCode(), h.getStatus())).toList();
    }

    @Transactional(readOnly = true)
    public List<RouteRow> routes() {
        requireAdmin();
        return rules.findAllWithHubs().stream().map(r -> new RouteRow(r.getId(), r.getCurrentHub().getCode(),
                r.getDestinationRegion(), r.getDestinationProvince(), r.getNextHub().getCode(), r.getStatus())).toList();
    }

    public void createHub(String code, String name, String type, String region, String province, String district,
                          String address, Long parentId) {
        requireAdmin();
        if (code == null || !code.matches("[A-Z0-9-]{2,20}"))
            throw new IllegalArgumentException("Mã bưu cục gồm 2–20 ký tự chữ in hoa, số hoặc dấu gạch ngang.");
        if (!LEVELS.containsKey(type)) throw new IllegalArgumentException("Loại bưu cục không hợp lệ.");
        if (!REGIONS.contains(region)) throw new IllegalArgumentException("Miền không hợp lệ.");
        requireText(name, province);
        if (hubs.existsByCodeIgnoreCase(code))
            throw new IllegalArgumentException("Mã bưu cục đã tồn tại.");
        Hub hub = Hub.active(code, name.strip(), LEVELS.get(type), type, region, province.strip());
        hub.edit(name.strip(), province.strip(), blankToNull(district), blankToNull(address), validParent(LEVELS.get(type), parentId));
        hubs.save(hub);
    }

    public void editHub(Long hubId, String name, String province, String district, String address, Long parentId) {
        requireAdmin();
        requireText(name, province);
        Hub hub = hub(hubId);
        hub.edit(name.strip(), province.strip(), blankToNull(district), blankToNull(address), validParent(hub.getLevel(), parentId));
    }

    public void setHubActive(Long hubId, boolean active) {
        requireAdmin();
        Hub hub = hub(hubId);
        if (active) {
            if (hub.getParentHub() != null && !"ACTIVE".equals(hub.getParentHub().getStatus()))
                throw new IllegalArgumentException("Hãy bật lại bưu cục cha trước.");
            hub.changeStatus("ACTIVE");
            return;
        }
        Object[] counts = rules.hubDependencies(hubId).get(0);
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < BLOCKERS.length; i++)
            if (((Number) counts[i]).longValue() > 0) reasons.add(counts[i] + " " + BLOCKERS[i]);
        if (!reasons.isEmpty())
            throw new IllegalArgumentException("Chưa thể ngừng bưu cục vì còn: " + String.join(", ", reasons) + ".");
        hub.changeStatus("INACTIVE");
    }

    public void addRule(Long currentHubId, String region, String province, Long nextHubId) {
        requireAdmin();
        if (!REGIONS.contains(region)) throw new IllegalArgumentException("Miền đích không hợp lệ.");
        Hub current = activeHub(currentHubId), next = activeHub(nextHubId);
        if (current.getId().equals(next.getId())) throw new IllegalArgumentException("Bưu cục kế tiếp phải khác bưu cục hiện tại.");
        String targetProvince = blankToNull(province);
        if (rules.countActiveSameKey(currentHubId, region, targetProvince) > 0)
            throw new IllegalArgumentException("Đã có quy tắc đang áp dụng cho điểm đi và điểm đến này. Hãy ngừng quy tắc cũ trước.");
        rules.save(HubRoutingRule.active(current, region, targetProvince, next));
    }

    public void deactivateRule(Long ruleId) {
        requireAdmin();
        rules.findById(ruleId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy quy tắc.")).deactivate();
    }

    private Hub validParent(int level, Long parentId) {
        if (level == 3) {
            if (parentId != null) throw new IllegalArgumentException("Trung tâm SOC không có bưu cục cha.");
            return null;
        }
        if (parentId == null) throw new IllegalArgumentException("Chọn bưu cục cha.");
        Hub parent = activeHub(parentId);
        if (parent.getLevel() != level + 1)
            throw new IllegalArgumentException("Bưu cục cha phải ở cấp ngay trên (SOC → hub vùng → bưu cục giao).");
        return parent;
    }

    private Hub hub(Long id) {
        return hubs.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bưu cục."));
    }

    private Hub activeHub(Long id) {
        Hub hub = hub(id);
        if (!"ACTIVE".equals(hub.getStatus())) throw new IllegalArgumentException("Bưu cục " + hub.getCode() + " đang ngừng hoạt động.");
        return hub;
    }

    private static void requireText(String name, String province) {
        if (name == null || name.isBlank() || province == null || province.isBlank())
            throw new IllegalArgumentException("Nhập tên bưu cục và tỉnh/thành.");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static void requireAdmin() {
        if (!AuthenticatedActor.requireCurrent().hasRole(UserRole.ROLE_ADMIN)) throw new AccessDeniedException("ROLE_ADMIN required");
    }
}
