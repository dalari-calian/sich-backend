package com.sich.timeblock;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TimeBlockRepository extends JpaRepository<TimeBlockEntity, Long> {

    /** Bloqueios do prestador que encostam no período {@code [from, to)}, por início. */
    @Query("""
            select b from TimeBlockEntity b
            where b.provider.id = :providerId and b.startAt < :to and b.endAt > :from
            order by b.startAt""")
    List<TimeBlockEntity> findOverlapping(@Param("providerId") Long providerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select count(b) > 0 from TimeBlockEntity b
            where b.provider.id = :providerId and b.startAt < :to and b.endAt > :from""")
    boolean existsOverlap(@Param("providerId") Long providerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    Optional<TimeBlockEntity> findByIdAndProviderId(Long id, Long providerId);
}
