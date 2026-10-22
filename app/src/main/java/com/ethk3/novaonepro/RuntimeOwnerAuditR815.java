package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit owner registry; it audits registered runtime owners without reflection-based creation. */
public final class RuntimeOwnerAuditR815 {
    private static final Object LOCK = new Object();
    private static final LinkedHashMap<String, Owner> OWNERS = new LinkedHashMap<String, Owner>();

    public static final class Snapshot {
        public final long capturedAt;
        public final long generation;
        public final List<OwnerView> owners;
        public final String status;

        Snapshot(long capturedAt, long generation, List<OwnerView> owners, String status) {
            this.capturedAt = capturedAt;
            this.generation = generation;
            this.owners = Collections.unmodifiableList(owners);
            this.status = status;
        }

        public String summary() {
            return status + "|generation=" + generation + "|owners=" + owners.size();
        }
    }

    public static final class OwnerView {
        public final String domain;
        public final long generation;
        public final boolean alive;
        public final int queueDepth;

        OwnerView(String domain, long generation, boolean alive, int queueDepth) {
            this.domain = domain;
            this.generation = generation;
            this.alive = alive;
            this.queueDepth = queueDepth;
        }
    }

    private static final class Owner {
        final String domain;
        final Thread thread;
        final long generation;
        final int queueDepth;

        Owner(String domain, Thread thread, long generation, int queueDepth) {
            this.domain = domain;
            this.thread = thread;
            this.generation = generation;
            this.queueDepth = queueDepth;
        }
    }

    private RuntimeOwnerAuditR815() {}

    public static void register(String domain, Thread thread, long generation, int queueDepth) {
        String key = safe(domain);
        if (key.length() == 0 || thread == null || generation < 0L || queueDepth < 0)
            throw new IllegalArgumentException("R815_OWNER_REGISTER_INVALID");
        synchronized (LOCK) {
            Owner prior = OWNERS.get(key);
            if (prior != null && prior.thread != thread && prior.thread.isAlive())
                throw new IllegalStateException("R815_OWNER_CARDINALITY_DUPLICATE:" + key);
            OWNERS.put(key, new Owner(key, thread, generation, queueDepth));
        }
    }

    public static void unregister(String domain, Thread expected) {
        synchronized (LOCK) {
            Owner owner = OWNERS.get(safe(domain));
            if (owner != null && (expected == null || owner.thread == expected)) OWNERS.remove(owner.domain);
        }
    }

    public static Snapshot snapshot(AutoFetchService service) {
        long generation = service == null ? 0L : RecoveryCoordinatorR815.currentGeneration(service);
        ArrayList<OwnerView> copy = new ArrayList<OwnerView>();
        synchronized (LOCK) {
            for (Map.Entry<String, Owner> entry : OWNERS.entrySet()) {
                Owner owner = entry.getValue();
                copy.add(
                        new OwnerView(
                                owner.domain,
                                owner.generation,
                                owner.thread.isAlive(),
                                owner.queueDepth));
            }
        }
        String status = "OK";
        for (int i = 0; i < copy.size(); i++) {
            if (!copy.get(i).alive) {
                status = "DEAD_OWNER:" + copy.get(i).domain;
                break;
            }
        }
        return new Snapshot(System.currentTimeMillis(), generation, copy, status);
    }

    public static void assertCardinality() {
        synchronized (LOCK) {
            for (Owner owner : OWNERS.values()) {
                if (owner.thread == null || !owner.thread.isAlive())
                    throw new IllegalStateException("R815_OWNER_DEAD:" + owner.domain);
            }
        }
    }

    public static void assertDestroyed() {
        synchronized (LOCK) {
            if (!OWNERS.isEmpty())
                throw new IllegalStateException("R815_OWNER_NOT_DESTROYED:" + OWNERS.keySet().toString());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
