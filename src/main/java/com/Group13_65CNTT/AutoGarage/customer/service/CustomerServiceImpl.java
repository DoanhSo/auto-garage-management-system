package com.Group13_65CNTT.AutoGarage.customer.service;

import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    private final CustomerScope customerScope;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final Random random = new Random();

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper, CustomerScope customerScope) {
        this.customerScope = customerScope;
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        customerScope.requireFrontDesk();
        validateCreditAndTerm(request.getCreditLimit(), request.getPaymentTermDays());

        String normalizedPhone = normalizePhone(request.getPhone());
        if (customerRepository.existsByPhone(normalizedPhone)) {
            throw new ConflictException("Số điện thoại " + normalizedPhone + " đã được đăng ký bởi khách hàng khác");
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
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
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
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByCode(String code) {
        Customer customer = customerRepository.findByCustomerCode(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với mã: " + code));
        customerScope.requireResource(AccessResource.CUSTOMER, customer.getId());
        return customerMapper.toResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomers(String search, Boolean isActive) {
        customerScope.requireFrontDesk();
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        return customerRepository.searchCustomers(cleanSearch, isActive)
                .stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
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
                throw new ConflictException("Số điện thoại " + normalizedPhone + " đã được đăng ký bởi khách hàng khác");
            }
            request.setPhone(normalizedPhone);
        }

        customerMapper.updateEntity(customer, request);
        Customer updated = customerRepository.save(customer);
        return customerMapper.toResponse(updated);
    }

    @Override
    public void deactivateCustomer(Long id) {
        customerScope.requireFrontDesk();
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        customer.setIsActive(false);
        customerRepository.save(customer);
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
