package com.Group13_65CNTT.AutoGarage.common.util;

import org.springframework.dao.DuplicateKeyException;

import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class DatabaseFailures {
    private static final int MYSQL_DUPLICATE_KEY = 1062;
    private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

    private DatabaseFailures() {}

    public static boolean isDuplicateKey(Throwable failure) {
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = failure;
                current != null && visited.add(current);
                current = current.getCause()) {
            if (current instanceof DuplicateKeyException) return true;
            if (current instanceof SQLException sql
                    && (sql.getErrorCode() == MYSQL_DUPLICATE_KEY
                            || UNIQUE_VIOLATION_SQL_STATE.equals(sql.getSQLState()))) return true;
        }
        return false;
    }
}
