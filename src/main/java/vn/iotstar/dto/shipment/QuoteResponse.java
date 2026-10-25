package vn.iotstar.dto.shipment;

import java.math.BigDecimal;

/** Server-calculated fee shown before the sender confirms; it is recomputed on submit. */
public record QuoteResponse(BigDecimal shippingFee) {
    public BigDecimal getShippingFee() { return shippingFee; }
}
