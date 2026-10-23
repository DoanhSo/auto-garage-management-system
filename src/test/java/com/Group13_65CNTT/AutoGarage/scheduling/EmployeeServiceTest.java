package com.Group13_65CNTT.AutoGarage.scheduling;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.mapper.EmployeeMapper;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.service.impl.SchedulingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private SchedulingServiceImpl schedulingService;

    private Employee employee;
    private EmployeeCreateRequest createRequest;
    private EmployeeResponse response;

    @BeforeEach
    void setUp() {
        createRequest = EmployeeCreateRequest.builder()
                .userId(10L)
                .employeeCode("EMP001")
                .skillsJson("[\"ENGINE\", \"BRAKES\"]")
                .canQc(true)
                .hourlyCost(new BigDecimal("150000.00"))
                .build();

        employee = Employee.builder()
                .id(1L)
                .userId(10L)
                .employeeCode("EMP001")
                .skillsJson("[\"ENGINE\", \"BRAKES\"]")
                .canQc(true)
                .hourlyCost(new BigDecimal("150000.00"))
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        response = EmployeeResponse.builder()
                .id(1L)
                .userId(10L)
                .employeeCode("EMP001")
                .skillsJson("[\"ENGINE\", \"BRAKES\"]")
                .canQc(true)
                .hourlyCost(new BigDecimal("150000.00"))
                .isActive(true)
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .version(0L)
                .build();
    }

    @Test
    @DisplayName("Tạo nhân viên thành công")
    void createEmployee_success() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(employeeRepository.existsByUserId(10L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeMapper.toEntity(createRequest)).thenReturn(employee);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse actual = schedulingService.createEmployee(createRequest);

        assertThat(actual).isNotNull();
        assertThat(actual.getEmployeeCode()).isEqualTo("EMP001");
        assertThat(actual.getCanQc()).isTrue();
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("Tạo nhân viên thất bại khi user_id không tồn tại")
    void createEmployee_userNotFound_throwsResourceNotFound() {
        when(userRepository.existsById(10L)).thenReturn(false);

        assertThatThrownBy(() -> schedulingService.createEmployee(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.RESOURCE_NOT_FOUND);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo nhân viên thất bại khi user_id đã được gán cho nhân viên khác")
    void createEmployee_duplicateUser_throwsDuplicate() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(employeeRepository.existsByUserId(10L)).thenReturn(true);

        assertThatThrownBy(() -> schedulingService.createEmployee(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo nhân viên thất bại khi mã nhân viên đã tồn tại")
    void createEmployee_duplicateCode_throwsDuplicate() {
        when(userRepository.existsById(10L)).thenReturn(true);
        when(employeeRepository.existsByUserId(10L)).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> schedulingService.createEmployee(createRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lấy thông tin nhân viên theo ID thành công")
    void getEmployeeById_found() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse actual = schedulingService.getEmployeeById(1L);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lấy thông tin nhân viên theo ID không tìm thấy trả về 404")
    void getEmployeeById_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> schedulingService.getEmployeeById(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("Cập nhật thông tin nhân viên thành công")
    void updateEmployee_success() {
        EmployeeUpdateRequest updateRequest = EmployeeUpdateRequest.builder()
                .hourlyCost(new BigDecimal("180000.00"))
                .canQc(false)
                .isActive(true)
                .build();

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);
        when(employeeMapper.toResponse(employee)).thenReturn(response);

        EmployeeResponse actual = schedulingService.updateEmployee(1L, updateRequest);

        assertThat(actual).isNotNull();
        verify(employeeMapper).updateEntityFromRequest(updateRequest, employee);
        verify(employeeRepository).save(employee);
    }

    @Test
    @DisplayName("Vô hiệu hóa nhân viên thành công (soft deactivate)")
    void deactivateEmployee_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        schedulingService.deactivateEmployee(1L);

        assertThat(employee.getIsActive()).isFalse();
        verify(employeeRepository).save(employee);
    }
}
