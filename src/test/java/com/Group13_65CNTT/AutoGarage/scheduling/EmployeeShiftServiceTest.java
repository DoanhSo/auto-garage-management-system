package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeShiftMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.SchedulingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeShiftServiceTest {

    @Mock
    private EmployeeShiftRepository employeeShiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeShiftMapper employeeShiftMapper;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private Employee employee;
    private EmployeeShift shift;
    private ShiftCreateRequest createRequest;
    private ShiftResponse response;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        employee = Employee.builder()
                .id(1L)
                .userId(10L)
                .employeeCode("EMP001")
                .isActive(true)
                .build();

        createRequest = ShiftCreateRequest.builder()
                .employeeId(1L)
                .startsAt(now)
                .endsAt(now.plus(Duration.ofHours(8)))
                .notes("Ca sang")
                .build();

        shift = EmployeeShift.builder()
                .id(1L)
                .employeeId(1L)
                .startsAt(now)
                .endsAt(now.plus(Duration.ofHours(8)))
                .status(ShiftStatus.ACTIVE)
                .notes("Ca sang")
                .createdBy(100L)
                .createdAt(now)
                .version(0L)
                .build();

        response = ShiftResponse.builder()
                .id(1L)
                .employeeId(1L)
                .startsAt(now)
                .endsAt(now.plus(Duration.ofHours(8)))
                .status(ShiftStatus.ACTIVE)
                .notes("Ca sang")
                .createdBy(100L)
                .createdAt(now)
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Tạo ca làm việc thành công")
    void createShift_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findOverlappingShifts(eq(1L), eq(ShiftStatus.ACTIVE), any(), any()))
                .thenReturn(Collections.emptyList());
        when(employeeShiftMapper.toEntity(createRequest)).thenReturn(shift);
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenReturn(shift);
        when(employeeShiftMapper.toResponse(shift)).thenReturn(response);

        ShiftResponse actual = schedulingService.createShift(100L, createRequest);

        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(ShiftStatus.ACTIVE);
        verify(employeeShiftRepository).save(any(EmployeeShift.class));
    }

    @Test
    @DisplayName("Tạo ca làm thất bại khi giờ kết thúc <= giờ bắt đầu")
    void createShift_endsBeforeStarts_throwsBadRequest() {
        ShiftCreateRequest invalid = ShiftCreateRequest.builder()
                .employeeId(1L)
                .startsAt(now.plus(Duration.ofHours(2)))
                .endsAt(now)
                .build();

        assertThatThrownBy(() -> schedulingService.createShift(100L, invalid))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);

        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo ca làm thất bại khi nhân viên không tồn tại")
    void createShift_employeeNotFound_throwsResourceNotFound() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.createShift(100L, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.RESOURCE_NOT_FOUND);

        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo ca làm thất bại khi nhân viên đã bị vô hiệu hóa")
    void createShift_employeeInactive_throwsBadRequest() {
        employee.setIsActive(false);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> schedulingService.createShift(100L, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);

        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo ca làm thất bại khi bị trùng ca làm đang active của nhân viên (UC37)")
    void createShift_overlappingShift_throwsDuplicate() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findOverlappingShifts(eq(1L), eq(ShiftStatus.ACTIVE), any(), any()))
                .thenReturn(List.of(shift));

        assertThatThrownBy(() -> schedulingService.createShift(100L, createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Hủy ca làm việc thành công (CANCELLED)")
    void cancelShift_success() {
        when(employeeShiftRepository.findById(1L)).thenReturn(Optional.of(shift));

        schedulingService.cancelShift(100L, 1L);

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
        verify(employeeShiftRepository).save(shift);
    }

    @Test
    @DisplayName("Lấy danh sách tất cả ca làm việc thành công")
    void getShifts_success() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<EmployeeShift> page = new org.springframework.data.domain.PageImpl<>(List.of(shift));
        when(employeeShiftRepository.findAll(pageable)).thenReturn(page);
        when(employeeShiftMapper.toResponse(shift)).thenReturn(response);

        org.springframework.data.domain.Page<ShiftResponse> result = schedulingService.getShifts(null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(employeeShiftRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Kỹ thuật viên đăng ký ca: ca lưu ở trạng thái chờ duyệt, chưa tính là ca làm việc")
    void registerShift_savesPendingShift() {
        Instant start = now.plus(Duration.ofHours(24));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenAnswer(inv -> inv.getArgument(0));

        schedulingService.registerShift(10L, com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftRegisterRequest.builder()
                .startsAt(start).endsAt(start.plus(Duration.ofHours(4))).build());

        org.mockito.ArgumentCaptor<EmployeeShift> saved = org.mockito.ArgumentCaptor.forClass(EmployeeShift.class);
        verify(employeeShiftRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ShiftStatus.PENDING);
        assertThat(saved.getValue().getEmployeeId()).isEqualTo(1L);
        assertThat(saved.getValue().getRegisteredAt()).isNotNull();
    }

    @Test
    @DisplayName("Quản đốc duyệt đăng ký: ca chuyển sang ACTIVE và ghi người duyệt")
    void approveRegistration_activatesShift() {
        shift.setStatus(ShiftStatus.PENDING);
        when(employeeShiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenAnswer(inv -> inv.getArgument(0));

        schedulingService.approveShiftRegistration(100L, 1L);

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.ACTIVE);
        assertThat(shift.getApprovedBy()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Từ chối đăng ký ca lưu lý do; ca đã xử lý thì không duyệt lại được")
    void rejectRegistration_storesReasonAndBlocksReprocessing() {
        shift.setStatus(ShiftStatus.PENDING);
        when(employeeShiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(employeeShiftRepository.save(any(EmployeeShift.class))).thenAnswer(inv -> inv.getArgument(0));

        schedulingService.rejectShiftRegistration(100L, 1L, " Thiếu quân số khoang ");

        assertThat(shift.getStatus()).isEqualTo(ShiftStatus.REJECTED);
        assertThat(shift.getRejectionReason()).isEqualTo("Thiếu quân số khoang");
        assertThatThrownBy(() -> schedulingService.approveShiftRegistration(100L, 1L))
                .hasMessageContaining("đã được xử lý");
    }
}
