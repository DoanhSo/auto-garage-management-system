package com.Group13_65CNTT.AutoGarage.customer.service;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.common.util.DatabaseFailures;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse;
import com.Group13_65CNTT.AutoGarage.customer.dto.UpdateCustomerRequest;
import com.Group13_65CNTT.AutoGarage.customer.entity.Customer;
import com.Group13_65CNTT.AutoGarage.customer.mapper.CustomerMapper;
import com.Group13_65CNTT.AutoGarage.customer.repository.CustomerRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@lombok.extern.slf4j.Slf4j
@lombok.RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {
    private final CustomerScope customerScope;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository userRepository;
    private final com.Group13_65CNTT.AutoGarage.identity.service.KeycloakIdentityService keycloakIdentityService;
    private final com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy passwordPolicy;
    private final com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final Random random = new Random();

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_CREATED, entity = AuditConstants.ENTITY_CUSTOMER)
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        customerScope.requireFrontDesk();
        validateCreditAndTerm(request.getCreditLimit(), request.getPaymentTermDays());

        String normalizedPhone = normalizePhone(request.getPhone());
        if (customerRepository.existsByPhone(normalizedPhone)) {
            throw new BusinessException(
                    ResponseCode.CUSTOMER_PHONE_DUPLICATE,
                    "Số điện thoại " + normalizedPhone + " đã được đăng ký bởi khách hàng khác");
        }
        request.setPhone(normalizedPhone);

        if (request.getCustomerCode() == null || request.getCustomerCode().isBlank()) {
            request.setCustomerCode(generateCustomerCode());
        } else {
            String code = request.getCustomerCode().trim().toUpperCase();
            if (customerRepository.existsByCustomerCode(code)) {
                throw new ConflictException("Mã khách hàng " + code + " đã tồn tại");
            }
            request.setCustomerCode(code);
        }

        Customer customer = customerMapper.toEntity(request);
        return customerMapper.toResponse(saveCustomer(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCurrentCustomer() {
        return getCustomerById(customerScope.currentCustomerId());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id) {
        customerScope.requireResource(AccessResource.CUSTOMER, id);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        return withAccounts(List.of(customer)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCode(String code) {
        Customer customer = customerRepository.findByCustomerCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với mã: " + code));
        customerScope.requireResource(AccessResource.CUSTOMER, customer.getId());
        return withAccounts(List.of(customer)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String search, Boolean isActive, Boolean hasAccount) {
        customerScope.requireFrontDesk();
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return withAccounts(customerRepository.searchCustomers(cleanSearch, isActive, hasAccount));
    }

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_ACCOUNT_CREATED, entity = AuditConstants.ENTITY_CUSTOMER, idArgument = 0)
    public CustomerResponse createAccount(
            Long id, com.Group13_65CNTT.AutoGarage.customer.dto.CreateCustomerAccountRequest request) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        if (!Boolean.TRUE.equals(customer.getIsActive())) {
            throw new BusinessRuleException("Khách hàng đã ngừng hoạt động. Hãy mở lại hồ sơ trước khi tạo tài khoản.");
        }
        if (customer.getUserId() != null) {
            throw new BusinessRuleException("Khách hàng này đã có tài khoản đăng nhập.");
        }
        String username = request.getUsername().strip().toLowerCase(java.util.Locale.ROOT);
        String requestedEmail = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail() : customer.getEmail();
        String email = requestedEmail == null || requestedEmail.isBlank()
                ? null : requestedEmail.strip().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE,
                    "Tên đăng nhập " + username + " đã được dùng. Hãy chọn tên khác.");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE,
                    "Email " + email + " đã gắn với một tài khoản khác. Nếu khách đã tự đăng ký, hãy dùng Nối tài khoản.");
        }
        passwordPolicy.validateNewPassword(request.getPassword());

        String identityId = keycloakIdentityService
                .createOrLinkUser(username, email, customer.getFullName(),
                        com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER, request.getPassword())
                .orElseThrow(() -> new BusinessException(ResponseCode.AUTHENTICATION_UNAVAILABLE));
        long actorId = currentUser.userId();
        com.Group13_65CNTT.AutoGarage.identity.entity.User user =
                com.Group13_65CNTT.AutoGarage.identity.entity.User.builder()
                        .identityId(identityId)
                        .username(username)
                        .email(email)
                        .fullName(customer.getFullName())
                        .role(com.Group13_65CNTT.AutoGarage.common.enums.Role.CUSTOMER)
                        .active(true)
                        .build();
        user.setCreatedBy(actorId);
        user.setUpdatedBy(actorId);
        try {
            user = userRepository.saveAndFlush(user);
            customer.setUserId(user.getId());
            customerRepository.saveAndFlush(customer);
        } catch (RuntimeException failure) {
            log.warn("Keycloak account {} was created but could not be saved for customer {}; remove it in Keycloak",
                    username, id);
            throw failure;
        }
        return withAccounts(List.of(customer)).get(0);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_ACCOUNT_LINKED, entity = AuditConstants.ENTITY_CUSTOMER, idArgument = 0)
    public CustomerResponse linkAccount(
            Long id, com.Group13_65CNTT.AutoGarage.customer.dto.LinkCustomerAccountRequest request) {
        customerScope.requireFrontDesk();
        Long sourceId = request.getSourceCustomerId();
        if (java.util.Objects.equals(id, sourceId)) {
            throw new BusinessRuleException("Hãy chọn một hồ sơ khác để lấy tài khoản.");
        }
        // Lock both profiles in ascending id order so two concurrent merges cannot deadlock.
        Customer first = lockCustomer(Math.min(id, sourceId));
        Customer second = lockCustomer(Math.max(id, sourceId));
        Customer target = first.getId().equals(id) ? first : second;
        Customer source = target == first ? second : first;
        if (!Boolean.TRUE.equals(target.getIsActive())) {
            throw new BusinessRuleException("Hồ sơ " + target.getCustomerCode() + " đã ngừng hoạt động. Hãy mở lại trước khi nối tài khoản.");
        }
        if (target.getUserId() != null) {
            throw new BusinessRuleException("Hồ sơ " + target.getCustomerCode() + " đã có tài khoản đăng nhập.");
        }
        if (source.getUserId() == null) {
            throw new BusinessRuleException("Hồ sơ " + source.getCustomerCode() + " chưa có tài khoản để chuyển.");
        }
        List<String> blockers = businessDataOf(source.getId());
        if (!blockers.isEmpty()) {
            throw new BusinessRuleException("Hồ sơ " + source.getCustomerCode() + " còn " + String.join(", ", blockers)
                    + " nên không gộp được. Hãy chuyển hoặc xử lý dữ liệu này trước.");
        }
        Long userId = source.getUserId();
        source.setUserId(null);
        source.setIsActive(false);
        customerRepository.saveAndFlush(source);
        target.setUserId(userId);
        customerRepository.saveAndFlush(target);
        log.info("Customer account of user {} moved from profile {} to profile {}", userId, source.getId(), target.getId());
        return withAccounts(List.of(target)).get(0);
    }

    private Customer lockCustomer(Long id) {
        return customerRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
    }

    /** Business records still pointing at the profile, as "2 xe", "1 hóa đơn"…; empty when none. */
    private List<String> businessDataOf(Long customerId) {
        java.util.Map<String, Object> counts = jdbcTemplate.queryForMap(
                "SELECT (SELECT COUNT(*) FROM vehicles WHERE customer_id = ?) AS vehicles, "
                        + "(SELECT COUNT(*) FROM bookings WHERE customer_id = ?) AS bookings, "
                        + "(SELECT COUNT(*) FROM repair_orders WHERE customer_id = ?) AS orders, "
                        + "(SELECT COUNT(*) FROM invoices WHERE customer_id = ?) AS invoices, "
                        + "(SELECT COUNT(*) FROM warranties WHERE customer_id = ?) AS warranties",
                customerId, customerId, customerId, customerId, customerId);
        java.util.Map<String, String> labels = new java.util.LinkedHashMap<>();
        labels.put("vehicles", "xe");
        labels.put("bookings", "lịch hẹn");
        labels.put("orders", "phiếu sửa chữa");
        labels.put("invoices", "hóa đơn");
        labels.put("warranties", "sổ bảo hành");
        List<String> blockers = new java.util.ArrayList<>();
        labels.forEach((column, label) -> {
            Object value = counts.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(column))
                    .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
            long count = value instanceof Number number ? number.longValue() : 0;
            if (count > 0) {
                blockers.add(count + " " + label);
            }
        });
        return blockers;
    }

    /** Maps customers and adds their sign-in account, loading every account in one query. */
    private List<CustomerResponse> withAccounts(List<Customer> customers) {
        java.util.Map<Long, com.Group13_65CNTT.AutoGarage.identity.entity.User> accounts = userRepository
                .findAllById(customers.stream().map(Customer::getUserId).filter(java.util.Objects::nonNull)
                        .distinct().toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.Group13_65CNTT.AutoGarage.identity.entity.User::getId, java.util.function.Function.identity()));
        return customers.stream().map(customer -> {
            CustomerResponse response = customerMapper.toResponse(customer);
            response.setAccount(com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountSummary.of(
                    customer.getUserId() == null ? null : accounts.get(customer.getUserId())));
            return response;
        }).toList();
    }

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_UPDATED, entity = AuditConstants.ENTITY_CUSTOMER, idArgument = 0)
    public CustomerResponse updateCustomer(Long id, UpdateCustomerRequest request) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));

        if (request.getCreditLimit() != null || request.getPaymentTermDays() != null) {
            BigDecimal credit = request.getCreditLimit() != null ? request.getCreditLimit() : customer.getCreditLimit();
            Integer days = request.getPaymentTermDays() != null ? request.getPaymentTermDays() : customer.getPaymentTermDays();
            validateCreditAndTerm(credit, days);
        }

        if (request.getPhone() != null) {
            String normalizedPhone = normalizePhone(request.getPhone());
            if (customerRepository.existsByPhoneAndIdNot(normalizedPhone, id)) {
                throw new BusinessException(
                        ResponseCode.CUSTOMER_PHONE_DUPLICATE,
                        "Số điện thoại " + normalizedPhone + " đã được đăng ký bởi khách hàng khác");
            }
            request.setPhone(normalizedPhone);
        }

        customerMapper.updateEntity(customer, request);
        return customerMapper.toResponse(saveCustomer(customer));
    }

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_DEACTIVATED, entity = AuditConstants.ENTITY_CUSTOMER, idArgument = 0)
    public void deactivateCustomer(Long id) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        customer.setIsActive(false);
        customerRepository.save(customer);
    }

    @Override
    @Audited(action = AuditConstants.ACTION_CUSTOMER_ACTIVATED, entity = AuditConstants.ENTITY_CUSTOMER, idArgument = 0)
    public CustomerResponse activateCustomer(Long id) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        customer.setIsActive(true);
        return customerMapper.toResponse(customerRepository.save(customer));
    }

    private Customer saveCustomer(Customer customer) {
        try {
            return customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException ex) {
            if (DatabaseFailures.isDuplicateKey(ex)) {
                throw new BusinessException(DatabaseFailures.isConstraint(ex, "uq_customers_phone")
                        ? ResponseCode.CUSTOMER_PHONE_DUPLICATE : ResponseCode.DUPLICATE_RESOURCE, ex);
            }
            throw ex;
        }
    }

    private void validateCreditAndTerm(BigDecimal creditLimit, Integer paymentTermDays) {
        if (creditLimit != null && creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("Hạn mức nợ (credit_limit) không được âm");
        }
        if (paymentTermDays != null && paymentTermDays < 0) {
            throw new BusinessRuleException("Thời hạn nợ (payment_term_days) không được âm");
        }
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9+]", "");
    }

    private String generateCustomerCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            int randomPart = 1000 + random.nextInt(9000);
            code = "CUS-" + datePart + "-" + randomPart;
        } while (customerRepository.existsByCustomerCode(code));
        return code;
    }
}
