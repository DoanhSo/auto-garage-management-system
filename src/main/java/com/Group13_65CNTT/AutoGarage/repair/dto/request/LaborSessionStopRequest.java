package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import com.Group13_65CNTT.AutoGarage.repair.entity.StopReason;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborSessionStopRequest {

    @NotNull(message = "Lý do dừng (PAUSE, COMPLETE, CANCEL) không được để trống")
    private StopReason stopReason;
}
