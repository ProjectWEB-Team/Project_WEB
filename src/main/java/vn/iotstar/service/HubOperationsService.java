package vn.iotstar.service;

import vn.iotstar.dto.hub.InboundShipmentCommand;
import vn.iotstar.dto.hub.SortShipmentCommand;
import vn.iotstar.dto.hub.SortingDecisionResponse;
import vn.iotstar.dto.hub.RouteDiagnosticResponse;

public interface HubOperationsService {
    void receiveAtHub(InboundShipmentCommand command);
    SortingDecisionResponse sortAtHub(SortShipmentCommand command);
    RouteDiagnosticResponse getRouteDiagnostic(Long shipmentId);
    java.util.List<vn.iotstar.dto.hub.WaitingParcelResponse> waitingParcels();
}
