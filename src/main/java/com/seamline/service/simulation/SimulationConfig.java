package com.seamline.service.simulation;

/**
 * Everything that can be varied between two runs of the same balanced line.
 *
 * @param shiftMinutes     length of the shift, normally 480
 * @param bufferPerStation how many bundles may wait in front of a station
 * @param variability      standard deviation of operator pace, e.g. 0.12 = 12 %
 * @param seed             random seed, so a run can be repeated exactly
 * @param bundleSize       pieces per bundle moving down the line
 */
public record SimulationConfig(int shiftMinutes,
                               int bufferPerStation,
                               double variability,
                               long seed,
                               int bundleSize) {

    public static final int DEFAULT_BUNDLE_SIZE = 20;

    public static SimulationConfig of(int shiftMinutes, int bufferPerStation, double variability, long seed) {
        return new SimulationConfig(shiftMinutes, bufferPerStation, variability, seed, DEFAULT_BUNDLE_SIZE);
    }
}
