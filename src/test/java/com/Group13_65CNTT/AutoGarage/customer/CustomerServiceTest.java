package com.Group13_65CNTT.AutoGarage.customer;

import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.entity.CustomerType;
import com.Group13_65CNTT.AutoGarage.customer.mapper.CustomerMapper;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.customer.service.CustomerServiceImpl;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock private com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;

    @Mock
    private CustomerRepository customerRepository;

    @Spy
    private CustomerMapper customerMapper = new CustomerMapper();

    @InjectMocks
    private CustomerServiceImpl customerService;

    private CreateCustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new CreateCustomerRequest();
        validRequest.setFullName("Nguyen Van A");
        validRequest.setPhone("0987654321");
        validRequest.setCustomerType(CustomerType.INDIVIDUAL);
        validRequest.setCreditLimit(BigDecimal.valueOf(10000000));
        validRequest.setPaymentTermDays(30);
    }

    @Test
    @DisplayName("TV2-01: Tạo khách hàng thành công khi dữ liệu hợp lệ")
    void testCreateCustomer_Success() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(false);
        when(customerRepository.existsByCustomerCode(any())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });

        CustomerResponse response = customerService.createCustomer(validRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Nguyen Van A", response.getFullName());
        assertEquals("0987654321", response.getPhone());
        assertNotNull(response.getCustomerCode());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    @DisplayName("TV2-01: Báo lỗi Conflict khi số điện thoại đã tồn tại")
    void testCreateCustomer_DuplicatePhone() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> customerService.createCustomer(validRequest));
        assertTrue(ex.getMessage().contains("đã được đăng ký"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-01: Báo lỗi BusinessRule khi hạn mức nợ âm")
    void testCreateCustomer_NegativeCreditLimit() {
        validRequest.setCreditLimit(BigDecimal.valueOf(-100));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> customerService.createCustomer(validRequest));
        assertTrue(ex.getMessage().contains("không được âm"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("TV2-01: Cập nhật thông tin khách hàng thành công")
    void testUpdateCustomer_Success() {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setCustomerCode("CUS-001");
        existing.setFullName("Nguyen Van A");
        existing.setPhone("0987654321");
        existing.setCreditLimit(BigDecimal.ZERO);
        existing.setPaymentTermDays(0);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateCustomerRequest updateReq = new UpdateCustomerRequest();
        updateReq.setFullName("Nguyen Van B");
        updateReq.setPhone("0987654321");
        updateReq.setCreditLimit(BigDecimal.valueOf(5000000));
        updateReq.setPaymentTermDays(15);

        CustomerResponse updated = customerService.updateCustomer(1L, updateReq);

        assertNotNull(updated);
        assertEquals("Nguyen Van B", updated.getFullName());
        assertEquals(BigDecimal.valueOf(5000000), updated.getCreditLimit());
        assertEquals(15, updated.getPaymentTermDays());
    }
}
