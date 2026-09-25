package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelRecurringSeriesResponse {
    private String groupId;
    private int cancelledCount;
}
