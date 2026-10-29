package com.sich.common.util;

import java.time.LocalDateTime;

/**
 * Período semiaberto {@code [start, end)}: um período que termina às 10:00 não
 * conflita com outro que começa às 10:00 (mesma regra do {@code tsrange} do banco).
 */
public record TimeRange(LocalDateTime start, LocalDateTime end) {

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(TimeRange other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }
}
