package com.sich.appointment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    /** Agendamentos do prestador com o status que encostam no período {@code [from, to)}. */
    @Query("""
            select a from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt < :to and a.endAt > :from
            order by a.startAt""")
    List<AppointmentEntity> findProviderOverlapping(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select count(a) > 0 from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt < :to and a.endAt > :from""")
    boolean existsProviderOverlap(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select count(a) > 0 from AppointmentEntity a
            where a.customer.id = :customerId and a.status = :status
              and a.startAt < :to and a.endAt > :from""")
    boolean existsCustomerOverlap(@Param("customerId") Long customerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    List<AppointmentEntity> findByProviderIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Long providerId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    List<AppointmentEntity> findByCustomerIdAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Long customerId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    Optional<AppointmentEntity> findByIdAndProviderId(Long id, Long providerId);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    Optional<AppointmentEntity> findByIdAndCustomerId(Long id, Long customerId);
}
