package com.seamline.service.simulation;

import com.seamline.domain.MachineType;

/** Per-station outcome of one simulated shift. */
public record StationMetrics(int index,
                             String stationCode,
                             MachineType machineType,
                             String operatorCode,
                             String operatorName,
                             String operationCodes,
                             double minutesPerPiece,
                             double utilisationPercent,
                             double workingMinutes,
                             double blockedMinutes,
                             double starvedMinutes,
                             double idleMinutes,
                             int piecesDone) {
}
