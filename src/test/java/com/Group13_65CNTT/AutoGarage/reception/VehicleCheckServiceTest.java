package com.Group13_65CNTT.AutoGarage.reception;

import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.reception.service.VehicleCheckServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleCheckServiceTest {
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;

    @Mock
    private VehicleCheckRepository vehicleCheckRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Spy
    private VehicleCheckMapper vehicleCheckMapper = new VehicleCheckMapper();

    @InjectMocks
    private VehicleCheckServiceImpl vehicleCheckService;

    private CreateVehicleCheckRequest checkInReq;

    @BeforeEach
    void setUp() {
        checkInReq = new CreateVehicleCheckRequest();
        checkInReq.setRepairOrderId(100L);
        checkInReq.setCheckType(CheckType.CHECK_IN);
        checkInReq.setChecklistJson("{\"exterior\":{\"scratches\":[\"left_door\"]},\"interior\":{\"fuel\":\"half\"}}");
        checkInReq.setOdometer(25000);
        checkInReq.setNotes("Xe co tray xuoc cua trai");
        checkInReq.setCheckedBy(2L);
    }

    @Test
    void forbiddenCheckReadDoesNotLoadAttachments() {
        VehicleCheck check = new VehicleCheck();
        check.setId(1L);
        check.setRepairOrderId(100L);
        when(vehicleCheckRepository.findById(1L)).thenReturn(Optional.of(check));
        doThrow(new com.Group13_65CNTT.AutoGarage.common.exception.BusinessException(
                com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN))
                .when(customerScope).requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.REPAIR_ORDER, 100L);

        assertThrows(com.Group13_65CNTT.AutoGarage.common.exception.BusinessException.class,
                () -> vehicleCheckService.getVehicleCheckById(1L));
        verifyNoInteractions(attachmentRepository);
    }

    @Test
    @DisplayName("TV2-08: Lưu biên bản kiểm tra tiếp nhận thành công (attemptNo = 1, result = RECORDED)")
    void testRecordVehicleCheck_Success() {
        when(vehicleCheckRepository.findMaxAttemptNo(100L, CheckType.CHECK_IN)).thenReturn(0);
        when(vehicleCheckRepository.save(any(VehicleCheck.class))).thenAnswer(invocation -> {
            VehicleCheck vc = invocation.getArgument(0);
            vc.setId(1L);
            return vc;
        });

        VehicleCheckResponse response = vehicleCheckService.recordVehicleCheck(checkInReq);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getRepairOrderId());
        assertEquals(CheckType.CHECK_IN, response.getCheckType());
        assertEquals(1, response.getAttemptNo());
        assertEquals(CheckResult.RECORDED, response.getResult());
        verify(vehicleCheckRepository, times(1)).save(any(VehicleCheck.class));
    }

    @Test
    @DisplayName("TV2-08: Tự động tăng attemptNo khi thực hiện kiểm tra lần 2 của cùng loại check")
    void testRecordVehicleCheck_IncrementAttemptNo() {
        when(vehicleCheckRepository.findMaxAttemptNo(100L, CheckType.CHECK_IN)).thenReturn(1);
        when(vehicleCheckRepository.save(any(VehicleCheck.class))).thenAnswer(invocation -> {
            VehicleCheck vc = invocation.getArgument(0);
            vc.setId(2L);
            return vc;
        });

        VehicleCheckResponse response = vehicleCheckService.recordVehicleCheck(checkInReq);

        assertNotNull(response);
        assertEquals(2, response.getAttemptNo());
    }

    @Test
    @DisplayName("TV2-08: Từ chối lưu khi checklist JSON rỗng")
    void testRecordVehicleCheck_EmptyChecklistJson() {
        checkInReq.setChecklistJson("  ");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> vehicleCheckService.recordVehicleCheck(checkInReq));
        assertTrue(ex.getMessage().contains("checklist JSON"));
        verify(vehicleCheckRepository, never()).save(any());
    }
}
