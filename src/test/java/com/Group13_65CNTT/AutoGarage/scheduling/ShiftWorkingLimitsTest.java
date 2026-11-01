package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeShiftMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.SchedulingServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftWorkingLimitsTest {

    @Mock private EmployeeShiftRepository employeeShiftRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeShiftMapper employeeShiftMapper;
    @Mock private com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository serviceBayRepository;

    @InjectMocks private SchedulingServiceImpl schedulingService;

    private final Employee employee = Employee.builder().id(1L).userId(10L).employeeCode("EMP001").isActive(true).build();

    @Test
    @DisplayName("Từ chối xếp ca khi tổng giờ trong ngày vượt 12 giờ")
    void rejectsMoreThanTwelveHoursPerDay() {
        // 10:00-16:00 giờ Việt Nam (6h) cộng ca đã có 00:30-08:30 (8h) cùng ngày = 14h.
        Instant start = Instant.parse("2026-11-10T03:00:00Z");
        ShiftCreateRequest request = ShiftCreateRequest.builder()
                .employeeId(1L).startsAt(start).endsAt(Instant.parse("2026-11-10T09:00:00Z")).build();
        EmployeeShift existing = EmployeeShift.builder().id(5L).employeeId(1L)
                .startsAt(Instant.parse("2026-11-09T17:30:00Z")).endsAt(Instant.parse("2026-11-10T01:30:00Z")).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findOverlappingShifts(eq(1L), eq(ShiftStatus.ACTIVE), any(), any()))
                .thenAnswer(call -> call.getArgument(2).equals(start) ? List.<EmployeeShift>of() : List.of(existing));

        assertThatThrownBy(() -> schedulingService.createShift(100L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("12");
        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Từ chối xếp thợ thứ ba vào cùng khoang trong cùng khung giờ")
    void rejectsMoreThanTwoTechniciansInTheSameBay() {
        Instant start = Instant.parse("2026-11-10T00:30:00Z");
        ShiftCreateRequest request = ShiftCreateRequest.builder()
                .employeeId(1L).serviceBayId(4L).startsAt(start).endsAt(Instant.parse("2026-11-10T04:30:00Z")).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findOverlappingShifts(eq(1L), eq(ShiftStatus.ACTIVE), any(), any()))
                .thenReturn(List.of());
        when(serviceBayRepository.existsById(4L)).thenReturn(true);
        when(employeeShiftRepository.findOverlappingByBay(eq(4L), any(), any())).thenReturn(List.of(
                EmployeeShift.builder().id(8L).employeeId(2L).build(),
                EmployeeShift.builder().id(9L).employeeId(3L).build()));

        assertThatThrownBy(() -> schedulingService.createShift(100L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Khoang đã đủ");
        verify(employeeShiftRepository, never()).save(any());
    }

    @Test
    @DisplayName("Từ chối ca bắt đầu khi chưa nghỉ đủ 8 giờ sau ca của ngày hôm trước")
    void rejectsShortRestAcrossDays() {
        // Ca trước kết thúc 23:00 giờ Việt Nam, ca mới bắt đầu 05:00 hôm sau (nghỉ 6h).
        Instant start = Instant.parse("2026-11-09T22:00:00Z");
        ShiftCreateRequest request = ShiftCreateRequest.builder()
                .employeeId(1L).startsAt(start).endsAt(Instant.parse("2026-11-10T02:00:00Z")).build();
        EmployeeShift previous = EmployeeShift.builder().id(6L).employeeId(1L)
                .startsAt(Instant.parse("2026-11-09T11:00:00Z")).endsAt(Instant.parse("2026-11-09T16:00:00Z")).build();
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeShiftRepository.findOverlappingShifts(eq(1L), eq(ShiftStatus.ACTIVE), any(), any()))
                .thenAnswer(call -> call.getArgument(2).equals(start) ? List.<EmployeeShift>of() : List.of(previous));

        assertThatThrownBy(() -> schedulingService.createShift(100L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nghỉ");
    }
}
