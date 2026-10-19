package dev.fpsboost;

import com.sun.management.GarbageCollectionNotificationInfo;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

/**
 * Counts GC events via JVM notifications (no polling thread of our own).
 * Duration is the collector's reported wall time; for concurrent collectors it is not always a pure stop-the-world pause.
 */
public final class GcWatcher {
    private static long events;
    private static long totalMs;
    private static long maxMs;
    private static long over50;

    private GcWatcher() {}

    public static void init() {
        try {
            NotificationListener listener = (n, handback) -> {
                if (!GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(n.getType())) return;
                GarbageCollectionNotificationInfo info =
                        GarbageCollectionNotificationInfo.from((CompositeData) n.getUserData());
                if (info.getGcName().contains("Concurrent")) return; // concurrent cycles don't pause the game thread
                long d = info.getGcInfo().getDuration();
                synchronized (GcWatcher.class) {
                    events++;
                    totalMs += d;
                    if (d > maxMs) maxMs = d;
                    if (d >= 50) over50++;
                }
            };
            for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) {
                if (b instanceof NotificationEmitter) {
                    ((NotificationEmitter) b).addNotificationListener(listener, null, null);
                }
            }
        } catch (Throwable t) {
            FpsBoostMod.LOGGER.warn("GC tracking unavailable on this JVM: {}", t.toString());
        }
    }

    public static synchronized void reset() {
        events = 0;
        totalMs = 0;
        maxMs = 0;
        over50 = 0;
    }

    public static synchronized long[] snapshot() {
        return new long[]{events, totalMs, maxMs, over50};
    }
}
