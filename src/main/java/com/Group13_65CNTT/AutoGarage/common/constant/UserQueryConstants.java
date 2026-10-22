package com.Group13_65CNTT.AutoGarage.common.constant;

import java.util.Set;

public final class UserQueryConstants {
    public static final Set<String> SORT_FIELDS =
            Set.of("id", "username", "fullName", "role", "active", "createdAt");

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE_EXPRESSION = "${garage.pagination.default-size:20}";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";

    private UserQueryConstants() {}
}
