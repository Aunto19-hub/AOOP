package com.seamline.repository;

import com.seamline.domain.DailyCheckIn;
import com.seamline.domain.Employee;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCheckInRepository extends JpaRepository<DailyCheckIn, Long> {

    Optional<DailyCheckIn> findByEmployeeAndCheckDate(Employee employee, LocalDate checkDate);

    List<DailyCheckIn> findByCheckDateOrderByCreatedAtAsc(LocalDate checkDate);
}
