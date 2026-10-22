package com.Group13_65CNTT.AutoGarage.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PaginationPolicyTest {
    private final PaginationPolicy pagination = new PaginationPolicy(new PaginationProperties());

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"id desc; DROP TABLE users; --", "passwordHash", "LENGTH(username)"})
    void rejectsMissingOrUnapprovedSortProperty(String property) {
        assertThatThrownBy(
                        () -> pagination.page(0, 20, property, "asc", UserQueryConstants.SORT_FIELDS))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode()).isEqualTo(ResponseCode.BAD_REQUEST));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "desc; DROP TABLE users; --", "asc, username"})
    void rejectsSortDirectionsOutsideAscAndDesc(String direction) {
        assertThatThrownBy(
                        () -> pagination.page(0, 20, "id", direction, UserQueryConstants.SORT_FIELDS))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        error ->
                                assertThat(error.getResponseCode()).isEqualTo(ResponseCode.BAD_REQUEST));
    }
}
