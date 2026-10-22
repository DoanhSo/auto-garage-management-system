package com.Group13_65CNTT.AutoGarage.common.util;

import com.Group13_65CNTT.AutoGarage.common.config.PaginationProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PaginationPolicy {
    private final PaginationProperties properties;

    public PageRequest page(int page, int size, Sort sort) {
        if (page < 0
                || page > properties.getMaxPage()
                || size < 1
                || size > properties.getMaxSize()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        return PageRequest.of(page, size, sort);
    }

    public PageRequest page(
            int page, int size, String sortBy, String sortDir, Set<String> allowedFields) {
        if (sortBy == null
                || !allowedFields.contains(sortBy)
                || !(UserQueryConstants.SORT_ASC.equalsIgnoreCase(sortDir)
                        || UserQueryConstants.SORT_DESC.equalsIgnoreCase(sortDir))) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        return page(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
    }

    public void validate(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        page(pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());
    }

    public void validate(Pageable pageable, Set<String> allowedFields) {
        validate(pageable);
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedFields.contains(order.getProperty())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            }
        }
    }
}
