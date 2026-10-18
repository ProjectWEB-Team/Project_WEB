package vn.iotstar.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite key preserves the one-shipment-once-per-container contract. */
public class ContainerItemId implements Serializable {
    private Long container;
    private Long shipment;
    public ContainerItemId() { }
    public ContainerItemId(Long container, Long shipment) { this.container = container; this.shipment = shipment; }
    @Override public boolean equals(Object other) {
        return other instanceof ContainerItemId id && Objects.equals(container, id.container) && Objects.equals(shipment, id.shipment);
    }
    @Override public int hashCode() { return Objects.hash(container, shipment); }
}
