package vn.iotstar.service;

import vn.iotstar.dto.wallet.WithdrawalRequestDto;
import vn.iotstar.dto.wallet.WithdrawalReviewDto;
import vn.iotstar.entity.WithdrawalRequest;

import java.util.List;

public interface WithdrawalService {

    WithdrawalRequest requestWithdrawal(WithdrawalRequestDto dto);

    WithdrawalRequest approveWithdrawal(WithdrawalReviewDto dto);

    WithdrawalRequest rejectWithdrawal(WithdrawalReviewDto dto);

    List<WithdrawalRequest> getUserWithdrawals(Long userId);

    List<WithdrawalRequest> getPendingWithdrawals();
}

