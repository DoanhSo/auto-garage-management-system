package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import com.Group13_65CNTT.AutoGarage.repair.repository.LaborSessionRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.TaskAssignmentRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse.Availability;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeShiftRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.DispatchServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceTest {

    @Mock private EmployeeShiftRepository shiftRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ServiceBayRepository bayRepository;
    @Mock private UserRepository userRepository;
    @Mock private TaskAssignmentRepository assignmentRepository;
    @Mock private RepairTaskRepository taskRepository;
    @Mock private LaborSessionRepository laborSessionRepository;

    @InjectMocks private DispatchServiceImpl service;

    @Test
    @DisplayName("Thợ đang trực kèm khoang, kỹ năng và trạng thái bận khi có timer mở")
    void onShiftTechnicianCarriesBaySkillsAndBusyState() {
        Instant now = Instant.now();
        EmployeeShift shift = EmployeeShift.builder()
                .id(14L).employeeId(6L).shiftType(ShiftType.MORNING).serviceBayId(2L)
                .startsAt(now.minusSeconds(600)).endsAt(now.plusSeconds(600)).build();
        Employee employee = Employee.builder()
                .id(6L).userId(60L).employeeCode("KT-MINH").canQc(true).isActive(true)
                .skillsJson("[\"ENGINE\",\"BRAKE\"]").build();
        ServiceBay bay = ServiceBay.builder().id(2L).bayCode("BAY-02").bayName("Khoang 2").build();
        TaskAssignment assignment = TaskAssignment.builder().id(9L).taskId(25L).employeeId(6L).build();
        LaborSession timer = LaborSession.builder().assignmentId(9L).employeeId(6L).startsAt(now).build();

        when(shiftRepository.findAllActiveAt(any())).thenReturn(List.of(shift));
        when(employeeRepository.findAllById(anyCollection())).thenReturn(List.of(employee));
        User user = User.builder().fullName("Nguyễn Văn Minh").build();
        user.setId(60L);
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
        when(bayRepository.findAllById(anyCollection())).thenReturn(List.of(bay));
        when(assignmentRepository.findByEmployeeIdAndIsActiveTrue(6L)).thenReturn(List.of(assignment));
        when(taskRepository.findAllById(anyCollection()))
                .thenReturn(List.of(RepairTask.builder().id(25L).repairOrderId(5L).build()));
        when(laborSessionRepository.findTopByEmployeeIdAndEndsAtIsNull(6L)).thenReturn(Optional.of(timer));

        List<OnShiftTechnicianResponse> result = service.getOnShiftTechnicians();

        assertThat(result).hasSize(1);
        OnShiftTechnicianResponse tech = result.get(0);
        assertThat(tech.fullName()).isEqualTo("Nguyễn Văn Minh");
        assertThat(tech.bayCode()).isEqualTo("BAY-02");
        assertThat(tech.skills()).containsExactly("ENGINE", "BRAKE");
        assertThat(tech.canQc()).isTrue();
        assertThat(tech.activeOrdersCount()).isEqualTo(1);
        assertThat(tech.hasActiveTimer()).isTrue();
        assertThat(tech.activeTaskId()).isEqualTo(25L);
        assertThat(tech.availabilityStatus()).isEqualTo(Availability.BUSY);
    }

    @Test
    @DisplayName("Không có ca trực nào đang hoạt động thì danh sách rỗng")
    void emptyWhenNobodyIsOnShift() {
        when(shiftRepository.findAllActiveAt(any())).thenReturn(List.of());
        assertThat(service.getOnShiftTechnicians()).isEmpty();
    }
}
