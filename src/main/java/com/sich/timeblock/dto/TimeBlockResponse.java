package com.sich.timeblock.dto;

import java.time.LocalDateTime;

import com.sich.timeblock.TimeBlockEntity;

public record TimeBlockResponse(
        Long id,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String reason) {

    public static TimeBlockResponse of(TimeBlockEntity block) {
        return new TimeBlockResponse(block.getId(), block.getStartAt(), block.getEndAt(), block.getReason());
    }
}
