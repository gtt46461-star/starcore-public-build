package com.ethk3.novaonepro;

/** Fixed-size ring cache for recent valid permutations. */
public final class V51IncrementalWindowCache {
    private final V51Models.DrawFrame[] ring;
    private int size, next;

    public V51IncrementalWindowCache(int capacity) {
        ring = new V51Models.DrawFrame[Math.max(96, capacity)];
    }

    public synchronized void add(V51Models.DrawFrame frame) {
        if (frame == null) return;
        ring[next] = frame;
        next = (next + 1) % ring.length;
        if (size < ring.length) size++;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized V51Models.DrawFrame[] snapshot() {
        V51Models.DrawFrame[] out = new V51Models.DrawFrame[size];
        int start = (next - size + ring.length) % ring.length;
        for (int i = 0; i < size; i++) out[i] = ring[(start + i) % ring.length];
        return out;
    }

    public synchronized void reset() {
        for (int i = 0; i < ring.length; i++) ring[i] = null;
        size = 0;
        next = 0;
    }
}
