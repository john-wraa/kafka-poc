package no.eksempel.kafka.connector.sources.github.utils;

import java.time.Instant;

public class DateUtils {
    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Instant maxInstant (Instant i1, Instant i2){
        return i1.compareTo(i2) > 0 ? i1 : i2;
    }
}
