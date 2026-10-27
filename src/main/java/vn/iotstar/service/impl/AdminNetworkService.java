package vn.iotstar.service.impl;

import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.iotstar.entity.Hub;
import vn.iotstar.entity.UserRole;
import vn.iotstar.repository.HubRepository;
import vn.iotstar.repository.HubRoutingRuleRepository;
import vn.iotstar.security.AuthenticatedActor;

import java.util.List;

/**
 * Read-only view of the hub tree and static routing rules for the Administrator.
 * shortcut: no editing yet, routing is owned by the Phase-03 operations slice; add edits once its owner agrees.
 */
@Service
@Transactional(readOnly = true)
public class AdminNetworkService {
    // JavaBean getters: the configured Jasper/EL 5 runtime does not read record accessors.
    public record HubRow(String code, String name, Integer level, String type, String region, String province,
                         String parentCode, String status) {
        public String getCode() { return code; }
        public String getName() { return name; }
        public Integer getLevel() { return level; }
        public String getType() { return type; }
        public String getRegion() { return region; }
        public String getProvince() { return province; }
        public String getParentCode() { return parentCode; }
        public String getStatus() { return status; }
    }
    public record RouteRow(String currentHub, String destinationRegion, String destinationProvince, String nextHub,
                           String status) {
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

    public List<HubRow> hubs() {
        requireAdmin();
        return hubs.findAll(Sort.by("level").descending().and(Sort.by("code"))).stream().map(h -> new HubRow(
                h.getCode(), h.getName(), h.getLevel(), h.getType(), h.getRegion(), h.getProvince(),
                h.getParentHub() == null ? null : h.getParentHub().getCode(), h.getStatus())).toList();
    }

    public List<RouteRow> routes() {
        requireAdmin();
        return rules.findAllWithHubs().stream().map(r -> new RouteRow(r.getCurrentHub().getCode(), r.getDestinationRegion(),
                r.getDestinationProvince(), r.getNextHub().getCode(), r.getStatus())).toList();
    }

    private static void requireAdmin() {
        if (!AuthenticatedActor.requireCurrent().hasRole(UserRole.ROLE_ADMIN)) throw new AccessDeniedException("ROLE_ADMIN required");
    }
}
