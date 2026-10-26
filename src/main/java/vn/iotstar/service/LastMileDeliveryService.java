package vn.iotstar.service;

import vn.iotstar.dto.delivery.DeliveryCollectionRequest;
import vn.iotstar.dto.delivery.DeliveryFailureRequest;
import vn.iotstar.dto.delivery.DeliveryScanRequest;
import vn.iotstar.dto.delivery.DeliveryTaskResponse;
import vn.iotstar.dto.returnflow.ReturnHandoverRequest;
import vn.iotstar.dto.returnflow.ReturnTaskResponse;

import java.util.List;

public interface LastMileDeliveryService {

    DeliveryTaskResponse scanForDelivery(DeliveryScanRequest request);

    DeliveryTaskResponse recordCollection(DeliveryCollectionRequest request);

    DeliveryTaskResponse confirmDelivery(Long shipmentId);

    DeliveryTaskResponse recordFailure(DeliveryFailureRequest request);

    ReturnTaskResponse processReturnHandover(ReturnHandoverRequest request);

    ReturnTaskResponse confirmReturnDelivery(Long shipmentId);
    ReturnTaskResponse scanReturnDelivery(Long shipmentId);
    void receiveFailedDelivery(Long shipmentId, String trackingNumber);

    List<DeliveryTaskResponse> getAssignedDeliveryTasks();

    DeliveryTaskResponse getDeliveryTask(Long shipmentId);

    List<ReturnTaskResponse> getAssignedReturnTasks();
}
