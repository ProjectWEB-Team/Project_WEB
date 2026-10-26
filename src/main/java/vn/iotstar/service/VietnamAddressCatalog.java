package vn.iotstar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.iotstar.dto.shipment.CreateShipmentRequest;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Immutable, bundled 2025 two-level geography. No network calls during checkout. */
public final class VietnamAddressCatalog {
    public record Ward(String code, String name) { }
    public record Province(String code, String name, String region, List<Ward> wards) {
        public Province { wards = List.copyOf(wards); }
    }
    public record Address(Province province, Ward ward, String fullAddress) { }
    private final List<Province> provinces;
    private final Map<String, Province> byCode;

    private VietnamAddressCatalog() {
        try (var stream = VietnamAddressCatalog.class.getResourceAsStream("/geography/vietnam-admin-2025.json")) {
            if (stream == null) throw new IllegalStateException("Missing bundled Vietnam address catalog");
            provinces = List.copyOf(new ObjectMapper().readValue(stream, new TypeReference<List<Province>>() { }));
            byCode = provinces.stream().collect(Collectors.toUnmodifiableMap(Province::code, Function.identity()));
            if (byCode.size() != 34 || provinces.stream().mapToInt(p -> p.wards().size()).sum() != 3321)
                throw new IllegalStateException("Incomplete Vietnam address catalog");
        } catch (IOException e) { throw new IllegalStateException("Cannot read Vietnam address catalog", e); }
    }
    private static final class Holder { private static final VietnamAddressCatalog INSTANCE = new VietnamAddressCatalog(); }
    public static VietnamAddressCatalog standard() { return Holder.INSTANCE; }
    public List<Province> provinces() { return provinces; }
    public List<Ward> wards(String provinceCode) {
        Province province = byCode.get(provinceCode == null ? "" : provinceCode);
        return province == null ? List.of() : province.wards();
    }
    public Address resolve(String provinceCode, String wardCode, String street) {
        Province province = byCode.get(provinceCode == null ? "" : provinceCode);
        if (province == null) throw new IllegalArgumentException("Vui lòng chọn tỉnh/thành hợp lệ.");
        Ward ward = province.wards().stream().filter(w -> w.code().equals(wardCode)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Phường/xã không thuộc tỉnh/thành đã chọn. Vui lòng chọn lại."));
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Vui lòng nhập số nhà, tên đường hoặc thôn/ấp.");
        String full = street.trim() + ", " + ward.name() + ", " + province.name();
        if (full.length() > 255) throw new IllegalArgumentException("Địa chỉ đầy đủ vượt 255 ký tự. Vui lòng rút gọn phần số nhà, tên đường.");
        return new Address(province, ward, full);
    }
    /** Re-derive pricing regions and receiver grouping on the server, ignoring client region labels. */
    public void normalize(CreateShipmentRequest request) {
        Address sender = resolve(request.getSenderProvinceCode(), request.getSenderWardCode(), request.getSenderAddress());
        Address receiver = resolve(request.getReceiverProvinceCode(), request.getReceiverWardCode(), request.getReceiverAddress());
        request.setFromRegion(sender.province().region());
        request.setToRegion(receiver.province().region());
        request.setReceiverProvince(receiver.province().name());
        request.setReceiverDistrict(null); // Districts are retained only on legacy shipments.
    }
}
