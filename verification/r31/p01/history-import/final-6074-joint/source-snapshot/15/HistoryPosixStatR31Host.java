package com.ethk3.novaonepro.strategyv2;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import sun.misc.Unsafe;

/** HOST measurement boundary only. Real stat(2), production traversal/config/admission unchanged. */
public final class HistoryPosixStatR31Host {
    private HistoryPosixStatR31Host() {}
    public static final AtomicLong calls = new AtomicLong();
    public static final class Stat {
        public final long st_blocks;
        Stat(long blocks) { st_blocks = blocks; }
    }
    private static native long blocks0(String path) throws IOException;
    public static Stat stat(String path) throws IOException { calls.incrementAndGet(); return new Stat(blocks0(path)); }
    public static long allocated(File file) throws IOException { return Math.multiplyExact(stat(file.getPath()).st_blocks, 512L); }
    public static void install(String library) throws Exception {
        String vm = System.getProperty("java.vm.name"), runtime = System.getProperty("java.runtime.name");
        System.load(new File(library).getCanonicalPath());
        Class<?> boundary = Class.forName("com.ethk3.novaonepro.StorageCensusSU2$Allocated");
        Field stat = boundary.getDeclaredField("STAT"), blocks = boundary.getDeclaredField("BLOCKS");
        stat.setAccessible(true); blocks.setAccessible(true);
        if (stat.get(null) != null || blocks.get(null) != null) throw new IllegalStateException("HOST_STOCK_BOUNDARY_NOT_UNKNOWN");
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Method method = HistoryPosixStatR31Host.class.getMethod("stat", String.class);
        Field member = Stat.class.getField("st_blocks");
        unsafe.putObjectVolatile(unsafe.staticFieldBase(stat), unsafe.staticFieldOffset(stat), method);
        unsafe.putObjectVolatile(unsafe.staticFieldBase(blocks), unsafe.staticFieldOffset(blocks), member);
        if (stat.get(null) != method || blocks.get(null) != member) throw new IllegalStateException("HOST_STAT_BINDING_FAILED");
        if (!vm.equals(System.getProperty("java.vm.name")) || !runtime.equals(System.getProperty("java.runtime.name"))) throw new IllegalStateException("HOST_VM_PROPERTIES_CHANGED");
        System.out.println("HISTORY_HOST_POSIX_BOUNDARY|before=UNKNOWN|after=JNI_REAL_STAT_ST_BLOCKS|vm=" + vm + "|runtime=" + runtime + "|productionConfig=UNCHANGED|androidBoundary=NOT_RUN");
    }
}
