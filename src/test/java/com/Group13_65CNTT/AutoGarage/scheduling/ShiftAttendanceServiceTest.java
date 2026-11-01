package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeShiftMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.ShiftAttendanceServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShiftAttendanceServiceTest {

    @Mock private EmployeeShiftRepository shiftRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private LaborSessionRepository laborSessionRepository;
    @Mock private com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService laborExecutionService;
    @Mock private EmployeeShiftMapper shiftMapper;
    @Mock private CurrentUserProvider currentUser;

    @InjectMocks private ShiftAttendanceServiceImpl service;

    private EmployeeShift shiftStarting(Duration offsetFromNow, Duration length) {
        Instant start = Instant.now().plus(offsetFromNow);
        return EmployeeShift.builder().id(3L).employeeId(1L).startsAt(start).endsAt(start.plus(length))
                .status(ShiftStatus.ACTIVE).attendanceStatus(AttendanceStatus.SCHEDULED).build();
    }

    private void stubSave() {
        lenient().when(shiftRepository.save(any(EmployeeShift.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(shiftMapper.toResponse(any(EmployeeShift.class)))
                .thenAnswer(call -> ShiftResponse.builder()
                        .attendanceStatus(((EmployeeShift) call.getArgument(0)).getAttendanceStatus()).build());
    }

    @Test
    @DisplayName("Vào ca đúng giờ được ghi CHECKED_IN, muộn quá 10 phút được ghi LATE")
    void checkInMarksOnTimeAndLate() {
        stubSave();
        when(currentUser.role()).thenReturn(Role.RECEPTIONIST);

        EmployeeShift onTime = shiftStarting(Duration.ofMinutes(-5), Duration.ofHours(4));
        when(shiftRepository.findById(3L)).thenReturn(Optional.of(onTime));
        assertThat(service.checkIn(10L, 3L).getAttendanceStatus()).isEqualTo(AttendanceStatus.CHECKED_IN);

        EmployeeShift late = shiftStarting(Duration.ofMinutes(-40), Duration.ofHours(4));
        when(shiftRepository.findById(3L)).thenReturn(Optional.of(late));
        assertThat(service.checkIn(10L, 3L).getAttendanceStatus()).isEqualTo(AttendanceStatus.LATE);
        assertThat(late.getActualStartsAt()).isNotNull();
    }

    @Test
    @DisplayName("Không cho chấm công quá sớm hoặc khi ca đã kết thúc")
    void checkInRejectsOutsideTheShiftWindow() {
        when(currentUser.role()).thenReturn(Role.ADMIN);

        when(shiftRepository.findById(3L))
                .thenReturn(Optional.of(shiftStarting(Duration.ofHours(3), Duration.ofHours(4))));
        assertThatThrownBy(() -> service.checkIn(10L, 3L)).isInstanceOf(BusinessRuleException.class);

        when(shiftRepository.findById(3L))
                .thenReturn(Optional.of(shiftStarting(Duration.ofHours(-6), Duration.ofHours(4))));
        assertThatThrownBy(() -> service.checkIn(10L, 3L)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Kỹ thuật viên chỉ chấm công cho ca của chính mình")
    void technicianCanOnlyCheckInOwnShift() {
        when(currentUser.role()).thenReturn(Role.TECHNICIAN);
        when(shiftRepository.findById(3L))
                .thenReturn(Optional.of(shiftStarting(Duration.ofMinutes(-1), Duration.ofHours(4))));
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(Employee.builder().id(1L).userId(99L).build()));

        assertThatThrownBy(() -> service.checkIn(10L, 3L)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("Ra ca cần đã vào ca và tự dừng timer đang chạy")
    void checkOutNeedsCheckInAndPausesRunningTimers() {
        stubSave();
        when(currentUser.role()).thenReturn(Role.ADMIN);
        EmployeeShift shift = shiftStarting(Duration.ofMinutes(-60), Duration.ofHours(4));
        when(shiftRepository.findById(3L)).thenReturn(Optional.of(shift));
        assertThatThrownBy(() -> service.checkOut(10L, 3L)).isInstanceOf(BusinessRuleException.class);

        shift.setAttendanceStatus(AttendanceStatus.CHECKED_IN);
        assertThat(service.checkOut(10L, 3L).getAttendanceStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(shift.getActualEndsAt()).isNotNull();
        verify(laborExecutionService).pauseOpenTimersForEmployee(eq(shift.getEmployeeId()), any());
    }
}
