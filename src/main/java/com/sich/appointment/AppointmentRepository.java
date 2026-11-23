package com.sich.appointment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Um total por status dos agendamentos do prestador que começam em {@code [from, to)}. */
    @Query("""
            select new com.sich.appointment.AppointmentStatusTotals(a.status, count(a), sum(a.totalMinutes), sum(a.totalPrice))
            from AppointmentEntity a
            where a.provider.id = :providerId and a.startAt >= :from and a.startAt < :to
            group by a.status""")
    List<AppointmentStatusTotals> sumByStatus(@Param("providerId") Long providerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Prestadores com produto ativo ordenados pela quantidade de agendamentos com o
     * status criados desde {@code since} (mais agendados primeiro).
     */
    @Query("""
            select new com.sich.appointment.ProviderAppointmentCount(a.provider.id, count(a))
            from AppointmentEntity a
            where a.status = :status and a.createdAt >= :since
              and exists (select 1 from ProductEntity p where p.provider = a.provider and p.active = true)
            group by a.provider.id
            order by count(a) desc, a.provider.id""")
    List<ProviderAppointmentCount> countByProviderSince(@Param("status") AppointmentStatus status,
            @Param("since") Instant since,
            Pageable pageable);

    /** Histórico paginado; os itens vêm em lote pelo {@code @BatchSize} da coleção. */
    @EntityGraph(attributePaths = { "provider", "customer" })
    Page<AppointmentEntity> findByProviderIdAndStartAtLessThanOrderByStartAtDesc(Long providerId, LocalDateTime before,
            Pageable pageable);

    @EntityGraph(attributePaths = { "provider", "customer" })
    Page<AppointmentEntity> findByCustomerIdAndStartAtLessThanOrderByStartAtDesc(Long customerId, LocalDateTime before,
            Pageable pageable);

    /** Totais dos agendamentos do prestador com o status que começam em {@code [from, to)} e já terminaram. */
    @Query("""
            select new com.sich.appointment.RevenueTotals(count(a), sum(a.totalPrice), sum(a.cashbackUsed))
            from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt >= :from and a.startAt < :to and a.endAt <= :now""")
    RevenueTotals sumFinishedRevenue(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("now") LocalDateTime now);

    /** Totais dos agendamentos do prestador com o status que começam em {@code [from, to)} e ainda não terminaram. */
    @Query("""
            select new com.sich.appointment.RevenueTotals(count(a), sum(a.totalPrice), sum(a.cashbackUsed))
            from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt >= :from and a.startAt < :to and a.endAt > :now""")
    RevenueTotals sumUpcomingRevenue(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("now") LocalDateTime now);

    /** Totais dos agendamentos do prestador com o status que começam em {@code [from, to)}. */
    @Query("""
            select new com.sich.appointment.RevenueTotals(count(a), sum(a.totalPrice), sum(a.cashbackUsed))
            from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt >= :from and a.startAt < :to""")
    RevenueTotals sumRevenue(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Faturamento por dia dos agendamentos do prestador com o status que começam em {@code [from, to)} e já terminaram. */
    @Query("""
            select new com.sich.appointment.DailyRevenue(cast(a.startAt as LocalDate), count(a), sum(a.totalPrice), sum(a.cashbackUsed))
            from AppointmentEntity a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt >= :from and a.startAt < :to and a.endAt <= :now
            group by cast(a.startAt as LocalDate)
            order by cast(a.startAt as LocalDate)""")
    List<DailyRevenue> sumFinishedRevenueByDay(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("now") LocalDateTime now);

    /** Serviços que mais faturaram nos agendamentos do prestador com o status que começam em {@code [from, to)} e já terminaram. */
    @Query("""
            select new com.sich.appointment.ProductRevenue(i.productName, count(i), sum(i.price))
            from AppointmentItemEntity i join i.appointment a
            where a.provider.id = :providerId and a.status = :status
              and a.startAt >= :from and a.startAt < :to and a.endAt <= :now
            group by i.productName
            order by sum(i.price) desc, i.productName""")
    List<ProductRevenue> sumFinishedRevenueByProduct(@Param("providerId") Long providerId,
            @Param("status") AppointmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    /** Cashback ganho nos agendamentos do cliente com o status que já terminaram ({@code null} se não houver). */
    @Query("""
            select sum(a.cashbackEarned) from AppointmentEntity a
            where a.customer.id = :customerId and a.status = :status and a.endAt <= :now""")
    BigDecimal sumCashbackEarnedFinished(@Param("customerId") Long customerId,
            @Param("status") AppointmentStatus status,
            @Param("now") LocalDateTime now);

    /** Cashback a ganhar nos agendamentos do cliente com o status que ainda não terminaram ({@code null} se não houver). */
    @Query("""
            select sum(a.cashbackEarned) from AppointmentEntity a
            where a.customer.id = :customerId and a.status = :status and a.endAt > :now""")
    BigDecimal sumCashbackEarnedUpcoming(@Param("customerId") Long customerId,
            @Param("status") AppointmentStatus status,
            @Param("now") LocalDateTime now);

    /** Cashback usado como desconto nos agendamentos do cliente com o status ({@code null} se não houver). */
    @Query("""
            select sum(a.cashbackUsed) from AppointmentEntity a
            where a.customer.id = :customerId and a.status = :status""")
    BigDecimal sumCashbackUsed(@Param("customerId") Long customerId, @Param("status") AppointmentStatus status);

    long countByCustomerIdAndStatusAndEndAtLessThanEqual(Long customerId, AppointmentStatus status, LocalDateTime now);

    long countByCustomerIdAndStatusAndEndAtGreaterThan(Long customerId, AppointmentStatus status, LocalDateTime now);

    long countByCustomerIdAndStatus(Long customerId, AppointmentStatus status);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    Optional<AppointmentEntity> findByIdAndProviderId(Long id, Long providerId);

    @EntityGraph(attributePaths = { "items", "provider", "customer" })
    Optional<AppointmentEntity> findByIdAndCustomerId(Long id, Long customerId);
}
