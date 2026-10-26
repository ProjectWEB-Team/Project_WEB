package vn.iotstar.service;

import org.junit.jupiter.api.Test;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import static org.assertj.core.api.Assertions.*;

class VietnamAddressCatalogTest {
    private final VietnamAddressCatalog catalog = VietnamAddressCatalog.standard();
    @Test void bundlesAllCurrentAdministrativeUnitsWithUniqueCodes() {
        assertThat(catalog.provinces()).hasSize(34).extracting(VietnamAddressCatalog.Province::code).doesNotHaveDuplicates();
        var wards = catalog.provinces().stream().flatMap(p -> p.wards().stream()).toList();
        assertThat(wards).hasSize(3321).extracting(VietnamAddressCatalog.Ward::code).doesNotHaveDuplicates();
        assertThat(catalog.wards("unknown")).isEmpty();
    }
    @Test void rejectsWardFromAnotherProvinceAndUnknownCodes() {
        assertThatThrownBy(() -> catalog.resolve("79", "00004", "12 Street")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.resolve("XX", "26740", "12 Street")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void derivesTariffRegionsAndCanonicalProvinceNamesIgnoringSubmittedLabels() {
        var request = new CreateShipmentRequest();
        request.setSenderProvinceCode("79"); request.setSenderWardCode("26740"); request.setSenderAddress("12 Nguyễn Huệ");
        request.setReceiverProvinceCode("01"); request.setReceiverWardCode("00004"); request.setReceiverAddress("8 Hoàng Diệu");
        request.setFromRegion("NORTH"); request.setToRegion("SOUTH"); request.setReceiverDistrict("Quận giả");
        catalog.normalize(request);
        assertThat(request.getFromRegion()).isEqualTo("SOUTH");
        assertThat(request.getToRegion()).isEqualTo("NORTH");
        assertThat(request.getReceiverProvince()).isEqualTo("Thành phố Hà Nội");
        assertThat(request.getReceiverDistrict()).isNull();
        assertThat(catalog.resolve("79", "26740", request.getSenderAddress()).fullAddress())
                .isEqualTo("12 Nguyễn Huệ, Phường Sài Gòn, Thành phố Hồ Chí Minh");
    }
    @Test void validatesTheCompletePersistedAddressLength() {
        assertThatThrownBy(() -> catalog.resolve("79", "26740", "x".repeat(250))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.resolve("79", "26740", " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
