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
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLIntegrityConstraintViolationException;

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

    @Mock
    private com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository userRepository;

    @Mock
    private com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService keycloakIdentityService;

    @Mock
    private com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy passwordPolicy;

    @Mock
    private com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private Customer walkIn() {
        Customer customer = new Customer();
        customer.setId(7L);
        customer.setCustomerCode("KH-0007");
        customer.setFullName("Nguyễn Văn An");
        customer.setPhone("0900000007");
        customer.setEmail("An@Example.com");
        customer.setIsActive(true);
        return customer;
    }

    private com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerAccountRequest accountRequest() {
        return com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerAccountRequest.builder()
                .username("An.Nguyen").password("MatKhauBanDau#2026").build();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Tạo tài khoản cho khách có sẵn: tạo trên Keycloak, lưu users và nối hồ sơ")
    void createAccount_CreatesIdentityAndLinksProfile() {
        Customer customer = walkIn();
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(customer));
        when(keycloakIdentityService.createOrLinkUser("an.nguyen", "an@example.com", "Nguyễn Văn An",
                com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER, "MatKhauBanDau#2026"))
                .thenReturn(java.util.Optional.of("6f1c2c1e-0000-0000-0000-000000000007"));
        when(currentUser.userId()).thenReturn(1L);
        when(userRepository.saveAndFlush(any())).thenAnswer(call -> {
            com.Group13_65CNTT.AutoGarage.identity.entity.User saved = call.getArgument(0);
            saved.setId(70L);
            return saved;
        });
        when(customerRepository.saveAndFlush(customer)).thenReturn(customer);
        when(userRepository.findAllById(java.util.List.of(70L))).thenAnswer(call -> {
            com.Group13_65CNTT.AutoGarage.identity.entity.User linked =
                    com.Group13_65CNTT.AutoGarage.identity.entity.User.builder().username("an.nguyen").fullName("x")
                            .role(com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER).active(true)
                            .identityId("6f1c2c1e-0000-0000-0000-000000000007").build();
            linked.setId(70L);
            return java.util.List.of(linked);
        });

        var response = customerService.createAccount(7L, accountRequest());

        org.assertj.core.api.Assertions.assertThat(customer.getUserId()).isEqualTo(70L);
        org.assertj.core.api.Assertions.assertThat(response.getAccount().status())
                .isEqualTo(com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountStatus.PENDING);
        verify(passwordPolicy).validateNewPassword("MatKhauBanDau#2026");
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Không tạo tài khoản cho khách đã có tài khoản và không gọi Keycloak")
    void createAccount_AlreadyLinked_Rejected() {
        Customer customer = walkIn();
        customer.setUserId(5L);
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(customer));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> customerService.createAccount(7L, accountRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã có tài khoản");
        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Tên đăng nhập hoặc email đã dùng: từ chối trước khi gọi Keycloak")
    void createAccount_DuplicateUsernameOrEmail_Rejected() {
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(walkIn()));
        when(userRepository.existsByUsername("an.nguyen")).thenReturn(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> customerService.createAccount(7L, accountRequest()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE)
                .hasMessageContaining("an.nguyen");

        when(userRepository.existsByUsername("an.nguyen")).thenReturn(false);
        when(userRepository.existsByEmail("an@example.com")).thenReturn(true);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> customerService.createAccount(7L, accountRequest()))
                .hasMessageContaining("Nối tài khoản");
        verifyNoInteractions(keycloakIdentityService);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Khách ngừng hoạt động: không tạo tài khoản")
    void createAccount_InactiveCustomer_Rejected() {
        Customer customer = walkIn();
        customer.setIsActive(false);
        when(customerRepository.findByIdForUpdate(7L)).thenReturn(java.util.Optional.of(customer));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> customerService.createAccount(7L, accountRequest()))
                .hasMessageContaining("ngừng hoạt động");
        verifyNoInteractions(keycloakIdentityService);
    }

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
    @org.junit.jupiter.api.DisplayName("Danh sách khách kèm trạng thái tài khoản, nạp tài khoản một lần cho cả danh sách")
    void searchCustomers_AddsAccountStatusInOneLookup() {
        Customer walkIn = new Customer();
        walkIn.setId(1L);
        walkIn.setFullName("Khách vãng lai");
        walkIn.setPhone("0900000001");
        Customer registered = new Customer();
        registered.setId(2L);
        registered.setUserId(20L);
        registered.setFullName("Khách đã đăng ký");
        registered.setPhone("0900000002");
        com.Group13_65CNTT.AutoGarage.identity.entity.User account =
                com.Group13_65CNTT.AutoGarage.identity.entity.User.builder().username("khach2").fullName("Khách")
                        .role(com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER).active(true)
                        .identityId("6f1c2c1e-0000-0000-0000-000000000020")
                        .lastLoginAt(java.time.Instant.parse("2026-11-10T08:15:00Z")).build();
        account.setId(20L);
        when(customerRepository.searchCustomers(null, null, true)).thenReturn(java.util.List.of(walkIn, registered));
        when(userRepository.findAllById(java.util.List.of(20L))).thenReturn(java.util.List.of(account));

        var result = customerService.searchCustomers(" ", null, true);

        org.assertj.core.api.Assertions.assertThat(result.get(0).getAccount().status())
                .isEqualTo(com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountStatus.NONE);
        org.assertj.core.api.Assertions.assertThat(result.get(1).getAccount().status())
                .isEqualTo(com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountStatus.ACTIVE);
        org.assertj.core.api.Assertions.assertThat(result.get(1).getAccount().username()).isEqualTo("khach2");
        verify(userRepository, times(1)).findAllById(any());
    }

    @Test
    @DisplayName("TV2-01: Tạo khách hàng thành công khi dữ liệu hợp lệ")
    void testCreateCustomer_Success() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(false);
        when(customerRepository.existsByCustomerCode(any())).thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> {
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
        verify(customerRepository, times(1)).saveAndFlush(any(Customer.class));
    }

    @Test
    @DisplayName("TV2-01: Báo lỗi CUSTOMER_PHONE_DUPLICATE khi số điện thoại đã tồn tại")
    void testCreateCustomer_DuplicatePhone() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> customerService.createCustomer(validRequest));
        assertEquals(ResponseCode.CUSTOMER_PHONE_DUPLICATE, ex.getResponseCode());
        assertTrue(ex.getMessage().contains("đã được đăng ký"));
        verify(customerRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TV2-01: Race condition trùng SĐT ở DB chuyển thành CUSTOMER_PHONE_DUPLICATE và giữ cause")
    void testCreateCustomer_DuplicatePhoneRaceCondition() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(false);
        when(customerRepository.existsByCustomerCode(any())).thenReturn(false);
        DataIntegrityViolationException dbFailure = new DataIntegrityViolationException(
                "duplicate", new org.hibernate.exception.ConstraintViolationException("duplicate",
                        new SQLIntegrityConstraintViolationException("Duplicate entry", "23000", 1062), "uq_customers_phone"));
        when(customerRepository.saveAndFlush(any(Customer.class))).thenThrow(dbFailure);

        BusinessException ex = assertThrows(BusinessException.class, () -> customerService.createCustomer(validRequest));
        assertEquals(ResponseCode.CUSTOMER_PHONE_DUPLICATE, ex.getResponseCode());
        assertSame(dbFailure, ex.getCause());
    }

    @Test
    @DisplayName("TV2-01: Lỗi toàn vẹn dữ liệu khác không bị đổi thành trùng SĐT")
    void testCreateCustomer_OtherIntegrityFailureIsRethrown() {
        when(customerRepository.existsByPhone("0987654321")).thenReturn(false);
        when(customerRepository.existsByCustomerCode(any())).thenReturn(false);
        DataIntegrityViolationException dbFailure = new DataIntegrityViolationException("fk failure");
        when(customerRepository.saveAndFlush(any(Customer.class))).thenThrow(dbFailure);

        assertSame(dbFailure, assertThrows(DataIntegrityViolationException.class,
                () -> customerService.createCustomer(validRequest)));
    }

    @Test
    @DisplayName("TV2-01: Báo lỗi BusinessRule khi hạn mức nợ âm")
    void testCreateCustomer_NegativeCreditLimit() {
        validRequest.setCreditLimit(BigDecimal.valueOf(-100));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> customerService.createCustomer(validRequest));
        assertTrue(ex.getMessage().contains("không được âm"));
        verify(customerRepository, never()).saveAndFlush(any());
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
        when(customerRepository.saveAndFlush(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

    @Test
    void activateCustomer_reopensInactiveCustomer() {
        Customer customer = new Customer();
        customer.setId(3L);
        customer.setFullName("Tran Thi B");
        customer.setIsActive(false);
        when(customerRepository.findById(3L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.activateCustomer(3L);

        assertTrue(customer.getIsActive());
        assertEquals(3L, response.getId());
        verify(customerScope).requireFrontDesk();
    }
}
