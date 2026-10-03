package com.seamline.controller;

import com.seamline.dto.RunRequest;
import com.seamline.dto.ScenarioResponse;
import com.seamline.dto.SimulationResponse;
import com.seamline.security.SeamlineUserDetails;
import com.seamline.service.LineSimulationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The Industrial Engineer's what-if endpoints: run a shift, compare scenarios. */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final LineSimulationService simulationService;

    public SimulationController(LineSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /** POST /api/simulation/run — balance with these settings and simulate one shift. */
    @PostMapping("/run")
    public ResponseEntity<SimulationResponse> run(
            @RequestParam(name = "line", required = false) String line,
            @Valid @RequestBody(required = false) RunRequest request,
            @AuthenticationPrincipal SeamlineUserDetails principal) {

        return ResponseEntity.ok(simulationService.run(line,
                request == null ? RunRequest.defaults() : request, principal.getEmployee()));
    }

    /** GET /api/simulation/scenarios?seed=42 — five plans, one seed. */
    @GetMapping("/scenarios")
    public ResponseEntity<ScenarioResponse> scenarios(
            @RequestParam(name = "line", required = false) String line,
            @RequestParam(name = "seed", required = false) Long seed,
            @AuthenticationPrincipal SeamlineUserDetails principal) {

        return ResponseEntity.ok(simulationService.compare(line, seed, principal.getEmployee()));
    }
}
