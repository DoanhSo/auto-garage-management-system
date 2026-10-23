package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.OrderType;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairOrderResponse {

    private Long id;
    private String orderCode;
    private Long bookingId;
    private Long customerId;
    private Long vehicleId;
    private OrderType orderType;
    private RepairOrderStatus status;
    private Integer odometerIn;
    private Integer odometerOut;
    private BigDecimal fuelLevelPercent;
    private String customerComplaint;
    private String diagnosis;
    private Long receivedBy;
    private Instant receivedAt;
    private Long latestQcCheckId;
    private Long handedOverBy;
    private Instant handedOverAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
