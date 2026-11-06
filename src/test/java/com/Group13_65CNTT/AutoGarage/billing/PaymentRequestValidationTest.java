package com.Group13_65CNTT.AutoGarage.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** Bean Validation of the optional, editable transaction code on both payment requests. */
class PaymentRequestValidationTest {
    private static jakarta.validation.ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static PaymentReceiveRequest receipt(String code) {
        return PaymentReceiveRequest.builder()
                .invoiceId(1L)
                .amount(new BigDecimal("1000"))
                .paymentMethod(BillingOperations.PaymentMethod.CASH)
                .transactionCode(code)
                .build();
    }

    private static SupplierPaymentRequest payout(String code) {
        return SupplierPaymentRequest.builder()
                .inventoryDocumentId(1L)
                .amount(new BigDecimal("1000"))
                .paymentMethod(BillingOperations.PaymentMethod.BANK_TRANSFER)
                .transactionCode(code)
                .build();
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "pt-20261106-0001", "PT-20261106-0001", "ABC_123.x-9"})
    @DisplayName("Mã giao dịch rỗng, chữ thường hay có - _ . đều hợp lệ (máy chủ tự chuẩn hóa)")
    void acceptsBlankAndAllowedCharacters(String code) {
        assertThat(validator.validate(receipt(code))).isEmpty();
        assertThat(validator.validate(payout(code))).isEmpty();
    }

    @Test
    @DisplayName("Không gửi mã giao dịch (null) vẫn hợp lệ")
    void acceptsMissingCode() {
        assertThat(validator.validate(receipt(null))).isEmpty();
        assertThat(validator.validate(payout(null))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT 01", "PT/01", "PT#01", "mã-có-dấu", "PT-01;DROP"})
    @DisplayName("Mã giao dịch có khoảng trắng hoặc ký tự lạ bị từ chối kèm thông điệp tiếng Việt")
    void rejectsUnexpectedCharacters(String code) {
        assertThat(messages(validator.validate(receipt(code)))).contains("Mã giao dịch chỉ gồm chữ, số và các ký tự - _ .");
        assertThat(messages(validator.validate(payout(code)))).contains("Mã giao dịch chỉ gồm chữ, số và các ký tự - _ .");
    }

    @Test
    @DisplayName("Mã giao dịch dài đúng 50 ký tự hợp lệ, 51 ký tự bị từ chối")
    void enforcesMaximumLength() {
        assertThat(validator.validate(receipt("A".repeat(50)))).isEmpty();
        assertThat(validator.validate(payout("A".repeat(50)))).isEmpty();

        assertThat(messages(validator.validate(receipt("A".repeat(51))))).contains("Mã giao dịch tối đa 50 ký tự");
        assertThat(messages(validator.validate(payout("A".repeat(51))))).contains("Mã giao dịch tối đa 50 ký tự");
    }
}
