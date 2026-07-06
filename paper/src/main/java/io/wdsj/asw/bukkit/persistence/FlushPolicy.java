package io.wdsj.asw.bukkit.persistence;

public enum FlushPolicy {
    /**
     * Default write-back behavior. Writes only update memory and mark keys dirty; data is flushed
     * by the periodic task, explicit flush calls, invalidation, or cache close.
     */
    PERIODIC,

    /**
     * Writes still update memory first, but each {@code put/delete} also queues an immediate
     * asynchronous flush for the changed key. Periodic flush remains enabled as a retry fallback.
     */
    ASYNC_AFTER_WRITE
}
