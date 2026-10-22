package com.Group13_65CNTT.AutoGarage.common.util;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Keeps exception types and source locations, excluding SQL, credentials and provider messages. */
public final class SafeFailureLog {
    private static final int MAX_FRAMES_PER_CAUSE = 12;

    private SafeFailureLog() {}

    public static void error(Logger logger, String operation, Throwable failure) {
        StringBuilder trace = new StringBuilder();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable current = failure;
                current != null && visited.add(current);
                current = current.getCause()) {
            trace.append(current.getClass().getName())
                    .append(' ')
                    .append(
                            Arrays.toString(
                                    Arrays.copyOf(
                                            current.getStackTrace(),
                                            Math.min(
                                                    current.getStackTrace().length,
                                                    MAX_FRAMES_PER_CAUSE))))
                    .append('\n');
        }
        logger.error("{}; exception trace (messages excluded): {}", operation, trace);
    }
}
