package com.seamline.service;

import com.seamline.domain.DailyCheckIn;
import com.seamline.domain.Employee;
import com.seamline.dto.CheckInResponse;
import com.seamline.dto.MyCheckInResponse;
import com.seamline.repository.DailyCheckInRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The "I'm here and at my station" daily tap, and the supervisor's read of who's tapped in today. */
@Service
public class CheckInService {

    private final DailyCheckInRepository checkIns;

    public CheckInService(DailyCheckInRepository checkIns) {
        this.checkIns = checkIns;
    }

    @Transactional
    public MyCheckInResponse checkInToday(Employee employee) {
        LocalDate today = LocalDate.now();
        DailyCheckIn checkIn = checkIns.findByEmployeeAndCheckDate(employee, today)
                .orElseGet(() -> checkIns.save(new DailyCheckIn(employee, today)));
        return new MyCheckInResponse(true, checkIn.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public MyCheckInResponse myStatusToday(Employee employee) {
        return checkIns.findByEmployeeAndCheckDate(employee, LocalDate.now())
                .map(c -> new MyCheckInResponse(true, c.getCreatedAt()))
                .orElseGet(() -> new MyCheckInResponse(false, null));
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> today() {
        return checkIns.findByCheckDateOrderByCreatedAtAsc(LocalDate.now()).stream()
                .map(c -> new CheckInResponse(
                        c.getEmployee().getEmployeeId(),
                        c.getEmployee().getFullName(),
                        c.getEmployee().getRole().getLabel(),
                        c.getCreatedAt()))
                .toList();
    }
}
