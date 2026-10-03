package com.seamline.controller;

import com.seamline.dto.CheckInResponse;
import com.seamline.dto.MyCheckInResponse;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.CheckInService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The "I'm here and at my station" daily check-in, and the supervisor's read of who's tapped in. */
@RestController
@RequestMapping("/api/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public ResponseEntity<MyCheckInResponse> checkIn(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(checkInService.checkInToday(principal.getEmployee()));
    }

    @GetMapping("/mine")
    public ResponseEntity<MyCheckInResponse> mine(@AuthenticationPrincipal SeamlineUserDetails principal) {
        return ResponseEntity.ok(checkInService.myStatusToday(principal.getEmployee()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public ResponseEntity<List<CheckInResponse>> today() {
        return ResponseEntity.ok(checkInService.today());
    }
}
