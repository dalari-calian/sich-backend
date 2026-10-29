package com.sich.workinghours;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkingHoursRepository extends JpaRepository<WorkingHoursEntity, Long> {

    List<WorkingHoursEntity> findByProviderIdOrderByWeekdayAscStartTimeAsc(Long providerId);

    List<WorkingHoursEntity> findByProviderIdAndWeekdayOrderByStartTimeAsc(Long providerId, Integer weekday);

    @Modifying
    @Query("delete from WorkingHoursEntity w where w.provider.id = :providerId")
    void deleteByProviderId(@Param("providerId") Long providerId);
}
