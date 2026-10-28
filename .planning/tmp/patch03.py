from pathlib import Path
r=Path('src/main/java/vn/iotstar')
def edit(path,old,new):
 p=r/path;s=p.read_text(encoding='utf-8');assert old in s,path;p.write_text(s.replace(old,new),encoding='utf-8')
edit(Path('service/impl/OperationsAuditServiceImpl.java'),'        if (currentHub == null) return false;\n        if (actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) {\n            return !references.isEmpty();\n        }','        if (actor.hasRole(UserRole.ROLE_LINEHAUL_DRIVER)) return !references.isEmpty();\n        if (currentHub == null) return false;')
edit(Path('repository/HubOperationsRepository.java'),'    Optional<ShipmentRoute> findByRouteDecisionKey(String routeDecisionKey);','    @Query("select h from Hub h where h.id = :id and h.status = \'ACTIVE\'")\n    Optional<vn.iotstar.entity.Hub> findActiveHub(@Param("id") Long id);\n\n    Optional<ShipmentRoute> findByRouteDecisionKey(String routeDecisionKey);')
edit(Path('service/impl/HubOperationsServiceImpl.java'),'shipment.isOnHold() || shipment.getCurrentStatus() != ShipmentStatus.AT_HUB || currentHub == null','shipment.isOnHold() || !(shipment.getCurrentStatus() == ShipmentStatus.AT_HUB && "FORWARD".equals(shipment.getDirection())\n                || shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER && "RETURN".equals(shipment.getDirection())) || currentHub == null')
edit(Path('service/impl/HubOperationsServiceImpl.java'),'Objects.equals(shipment.getDestinationHubId(), currentHub.getId())','Objects.equals("RETURN".equals(shipment.getDirection()) ? shipment.getOriginHubId() : shipment.getDestinationHubId(), currentHub.getId())')
edit(Path('service/impl/HubOperationsServiceImpl.java'),'String decisionKey = "shipment:" + shipment.getId() + ":hub:" + currentHub.getId();','String decisionKey = shipment.routeDecisionKey(currentHub.getId());')
# Resolve return target from the authoritative origin hub without overwriting forward geography.
p=r/'service/impl/HubOperationsServiceImpl.java';s=p.read_text(encoding='utf-8');s=s.replace('shipment.getDestinationRegion()', 'targetRegion(shipment)').replace('shipment.getDestinationProvince()', 'targetProvince(shipment)')
idx=s.index('    private HubRoutingRule blocked(')
s=s[:idx]+'''    private Hub returnTarget(Shipment s) {
        if (s.getOriginHubId() == null) throw new RouteConfigurationException();
        return hubOperationsRepository.findActiveHub(s.getOriginHubId()).orElseThrow(RouteConfigurationException::new);
    }
    private String targetRegion(Shipment s) { return "RETURN".equals(s.getDirection()) ? returnTarget(s).getRegion() : s.getDestinationRegion(); }
    private String targetProvince(Shipment s) { return "RETURN".equals(s.getDirection()) ? returnTarget(s).getProvince() : s.getDestinationProvince(); }

'''+s[idx:];p.write_text(s,encoding='utf-8')
edit(Path('service/impl/ContainerHandoverServiceImpl.java'),'!"OPEN".equals(container.getStatus()) || shipment.getCurrentStatus() != ShipmentStatus.AT_HUB','!"OPEN".equals(container.getStatus()) || shipment.isOnHold()\n                || !("FORWARD".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.AT_HUB\n                     || "RETURN".equals(shipment.getDirection()) && shipment.getCurrentStatus() == ShipmentStatus.RETURN_TO_SENDER)')
edit(Path('service/impl/ContainerHandoverServiceImpl.java'),'if (!sameHub(route.getHub(), container.getDestinationHub()))','if (!shipment.routeDecisionKey(profile.getHub().getId()).equals(route.getRouteDecisionKey())\n                || !sameHub(route.getHub(), container.getDestinationHub()))')
# Route identity must match direction in the query too; don't let a later stale forward route hide the return route.
edit(Path('repository/ContainerHandoverRepository.java'),"and r.currentHub.id = :currentHubId and r.status = 'PENDING' order by r.id desc", "and r.currentHub.id = :currentHubId and r.status = 'PENDING' and r.routeDecisionKey = concat('shipment:', cast(r.shipment.id as string), ':hub:', cast(r.currentHub.id as string), ':', r.shipment.direction) order by r.id desc")
p=r/'repository/ManagerOperationsRepository.java';s=p.read_text(encoding='utf-8');a=s.index('    public long countSortingBacklog(');b=s.index('\n    ',s.index('\n    }',a)+6)
s=s[:a]+'''    public long countSortingBacklog(Collection<Long> hubIds) {
        return entityManager.createQuery("select count(s) from Shipment s where s.currentHub.id in :hubIds and s.onHold = false " +
                "and ((s.direction = 'FORWARD' and s.currentStatus = :forward and (s.destinationHubId is null or s.destinationHubId <> s.currentHub.id)) " +
                "or (s.direction = 'RETURN' and s.currentStatus = :returning and s.originHubId <> s.currentHub.id)) " +
                "and not exists (select r.id from ShipmentRoute r where r.shipment = s and r.currentHub.id = s.currentHub.id " +
                "and r.routeDecisionKey = concat('shipment:', cast(s.id as string), ':hub:', cast(s.currentHub.id as string), ':', s.direction))", Long.class)
                .setParameter("hubIds", hubIds).setParameter("forward", ShipmentStatus.AT_HUB)
                .setParameter("returning", ShipmentStatus.RETURN_TO_SENDER).getSingleResult();
    }
'''+s[b:];p.write_text(s,encoding='utf-8')
