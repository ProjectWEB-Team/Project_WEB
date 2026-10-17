package vn.iotstar.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the browser form route against silently posting back to /create. */
class ShipmentCreateJspContractTest {
    @Test
    void createFormPostsToTheShipmentCollectionControllerRoute() throws Exception {
        String jsp = Files.readString(Path.of("src/main/webapp/WEB-INF/views/shipment/create.jsp"));

        assertThat(jsp).contains("action=\"${pageContext.request.contextPath}/user/shipments\"");
    }
}
