package vn.iotstar.service;

import vn.iotstar.dto.hub.CorrectHandoverCommand;
import vn.iotstar.dto.hub.CreateContainerCommand;
import vn.iotstar.dto.hub.CreateHandoverCommand;
import vn.iotstar.dto.hub.HandoverAcceptanceResponse;
import vn.iotstar.dto.hub.LoadContainerCommand;
import vn.iotstar.dto.hub.ScanHandoverContainerCommand;

public interface ContainerHandoverService {
    Long createContainer(CreateContainerCommand command);
    void loadShipment(LoadContainerCommand command);
    Long createHandover(CreateHandoverCommand command);
    HandoverAcceptanceResponse scanAndAccept(ScanHandoverContainerCommand command);
    Long correctByCancellationAndRecreation(CorrectHandoverCommand command);
}
