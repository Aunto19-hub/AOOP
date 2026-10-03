package com.seamline.service.simulation;

/**
 * Mulberry32 pseudo-random generator.
 *
 * <p>Seeded and reproducible on purpose: two scenarios can only be compared
 * fairly if they meet the same sequence of random events, so "run with seed 42"
 * must give the same shift every time, in Java and in the browser prototype.</p>
 */
public final class DeterministicRandom {

    private int state;

    public DeterministicRandom(long seed) {
        this.state = (int) seed;
    }

    /** Uniform value in [0, 1). */
    public double nextDouble() {
        state += 0x6D2B79F5;
        int t = state;
        t = (t ^ (t >>> 15)) * (t | 1);
        t ^= t + ((t ^ (t >>> 7)) * (t | 61));
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296d;
    }

    /** Standard normal value via the Box-Muller transform. */
    public double nextGaussian() {
        double u = 0d;
        double v = 0d;
        while (u == 0d) {
            u = nextDouble();
        }
        while (v == 0d) {
            v = nextDouble();
        }
        return Math.sqrt(-2d * Math.log(u)) * Math.cos(2d * Math.PI * v);
    }
}
