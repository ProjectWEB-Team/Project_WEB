package vn.iotstar.dto.wallet;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class WithdrawalReviewDto implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Request ID is required")
    private Long requestId;

    private String rejectionReason;

    public WithdrawalReviewDto() {
    }

    public WithdrawalReviewDto(Long requestId, String rejectionReason) {
        this.requestId = requestId;
        this.rejectionReason = rejectionReason;
    }

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}

