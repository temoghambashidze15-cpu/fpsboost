package dev.fpsboost;

/** Fixed ring buffer of frame intervals in nanoseconds. Render thread only; allocation-free per frame. */
public final class FrameStats {
    private static long[] ring = new long[20000];
    private static int head = 0;
    private static int size = 0;
    private static long last = 0;

    private FrameStats() {}

    public static void init(int capacity) {
        ring = new long[capacity];
        reset();
    }

    public static void reset() {
        head = 0;
        size = 0;
        last = 0;
    }

    public static void onFrame() {
        long now = System.nanoTime();
        if (last != 0) {
            ring[head] = now - last;
            head++;
            if (head == ring.length) head = 0;
            if (size < ring.length) size++;
        }
        last = now;
    }

    /** Chronological copy. Allocates, so only call on demand. */
    public static long[] snapshot() {
        long[] out = new long[size];
        int start = (size < ring.length) ? 0 : head;
        for (int i = 0; i < size; i++) {
            out[i] = ring[(start + i) % ring.length];
        }
        return out;
    }
}
