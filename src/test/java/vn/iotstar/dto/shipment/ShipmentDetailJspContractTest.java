package vn.iotstar.dto.shipment;

import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** JSP EL requires JavaBean getters, including for DTOs that are represented as records. */
class ShipmentDetailJspContractTest {
    @Test
    void detailAndEventDtoPropertiesAreVisibleToJspEl() throws Exception {
        assertThat(propertiesOf(ShipmentDetailResponse.class))
                .contains("trackingNumber", "currentStatus", "createdAt", "events");
        assertThat(propertiesOf(ShipmentEventResponse.class))
                .contains("eventType", "description", "occurredAt");
    }

    private Set<String> propertiesOf(Class<?> type) throws Exception {
        return Arrays.stream(Introspector.getBeanInfo(type).getPropertyDescriptors())
                .map(PropertyDescriptor::getName)
                .collect(java.util.stream.Collectors.toSet());
    }
}
