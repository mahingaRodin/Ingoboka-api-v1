package com.ingoboka_api.v1.common.responses;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerStaffOverviewResponse {

    long totalStaff;
    long pendingPasswordChange;
    long pendingEmailVerification;
    long activeStaff;
    long disabledOrLocked;
    List<StaffResponse> staff;
}
