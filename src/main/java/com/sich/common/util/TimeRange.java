package com.sich.common.util;

import java.time.Duration;
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

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }

    /** Minutos em comum com {@code other} (0 quando não se sobrepõem). */
    public long overlapMinutes(TimeRange other) {
        if (!overlaps(other)) {
            return 0;
        }
        LocalDateTime overlapStart = start.isAfter(other.start) ? start : other.start;
        LocalDateTime overlapEnd = end.isBefore(other.end) ? end : other.end;
        return Duration.between(overlapStart, overlapEnd).toMinutes();
    }
}
