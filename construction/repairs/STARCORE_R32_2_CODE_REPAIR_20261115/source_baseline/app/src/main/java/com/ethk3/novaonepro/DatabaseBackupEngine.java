package com.ethk3.novaonepro;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;

/** Pre-open DB36/R811 quiet-lane backup with two-version retention and verified restore. */
public final class DatabaseBackupEngine {
    public static final class Backup {
        public final boolean present;
        public final File source;
        public final File databaseBackup;
        public final String checksum;

        Backup(boolean present, File source, File databaseBackup, String checksum) {
            this.present = present;
            this.source = source;
            this.databaseBackup = databaseBackup;
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    private DatabaseBackupEngine() {}

    public static Backup prepare(Context context) {
        if (context == null) return new Backup(false, null, null, "");
        if (RealtimePressureGateR811.isUnderPressure()) {
            throw new IllegalStateException("R811_DATABASE_BACKUP_REQUIRES_QUIET_LANE");
        }
        File source = context.getDatabasePath(AppConfig.DB_NAME);
        if (source == null || !source.exists() || source.length() == 0L) {
            return new Backup(false, source, null, "");
        }
        int userVersion = readUserVersion(source);
        if (userVersion > AppConfig.DB_VERSION) {
            return new Backup(false, source, null, "");
        }
        File parent = source.getParentFile();
        File directory = new File(parent, "r811-backups");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("R811_DATABASE_BACKUP_DIRECTORY_FAILED");
        }
        String name =
                AppConfig.DB_NAME
                        + ".pre-r811-db"
                        + userVersion
                        + '-'
                        + System.currentTimeMillis()
                        + ".bak";
        File backup = new File(directory, name);
        copy(source, backup);
        String sourceHash = sha256(source);
        String backupHash = sha256(backup);
        if (sourceHash.length() != 64 || !sourceHash.equals(backupHash)) {
            throw new IllegalStateException("R811_DATABASE_BACKUP_CHECKSUM_MISMATCH");
        }
        retainTwo(directory);
        return new Backup(true, source, backup, backupHash);
    }

    public static void restore(Backup backup) {
        if (backup == null
                || !backup.present
                || backup.source == null
                || backup.databaseBackup == null
                || !backup.databaseBackup.exists()) return;
        File temp =
                new File(
                        backup.source.getParentFile(),
                        backup.source.getName() + ".r811-restore-tmp");
        copy(backup.databaseBackup, temp);
        if (!backup.checksum.equals(sha256(temp))) {
            throw new IllegalStateException("R811_DATABASE_RESTORE_CHECKSUM_MISMATCH");
        }
        if (backup.source.exists() && !backup.source.delete()) {
            throw new IllegalStateException("R811_DATABASE_RESTORE_DELETE_FAILED");
        }
        if (!temp.renameTo(backup.source)) {
            copy(temp, backup.source);
            if (!temp.delete()) temp.deleteOnExit();
        }
        deleteSidecar(new File(backup.source.getPath() + "-wal"));
        deleteSidecar(new File(backup.source.getPath() + "-shm"));
        deleteSidecar(new File(backup.source.getPath() + "-journal"));
    }

    private static void copy(File source, File target) {
        FileInputStream input = null;
        FileOutputStream output = null;
        FileChannel in = null;
        FileChannel out = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(target, false);
            in = input.getChannel();
            out = output.getChannel();
            long size = in.size();
            long position = 0L;
            while (position < size) {
                long changed =
                        in.transferTo(position, Math.min(8L * 1024L * 1024L, size - position), out);
                if (changed <= 0L)
                    throw new IllegalStateException("R811_DATABASE_BACKUP_COPY_STALLED");
                position += changed;
            }
            output.getFD().sync();
            if (target.length() != source.length()) {
                throw new IllegalStateException("R811_DATABASE_BACKUP_SIZE_MISMATCH");
            }
        } catch (Throwable error) {
            throw new IllegalStateException("R811_DATABASE_BACKUP_FAILED", error);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Throwable ignore) {
            }
            try {
                if (in != null) in.close();
            } catch (Throwable ignore) {
            }
            try {
                if (output != null) output.close();
            } catch (Throwable ignore) {
            }
            try {
                if (input != null) input.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static void retainTwo(File directory) {
        File[] backups = directory.listFiles();
        if (backups == null || backups.length <= 2) return;
        Arrays.sort(
                backups,
                new Comparator<File>() {
                    public int compare(File a, File b) {
                        if (a.lastModified() == b.lastModified())
                            return a.getName().compareTo(b.getName());
                        return a.lastModified() < b.lastModified() ? 1 : -1;
                    }
                });
        for (int i = 2; i < backups.length; i++) {
            if (backups[i].isFile() && !backups[i].delete()) backups[i].deleteOnExit();
        }
    }

    private static String sha256(File file) {
        FileInputStream input = null;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            input = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count > 0) digest.update(buffer, 0, count);
            }
            byte[] value = digest.digest();
            StringBuilder out = new StringBuilder(value.length * 2);
            for (int i = 0; i < value.length; i++) {
                int v = value[i] & 0xff;
                if (v < 16) out.append('0');
                out.append(Integer.toHexString(v));
            }
            return out.toString();
        } catch (Throwable error) {
            throw new IllegalStateException("R811_DATABASE_BACKUP_HASH_FAILED", error);
        } finally {
            try {
                if (input != null) input.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static void deleteSidecar(File file) {
        if (file.exists() && !file.delete()) file.deleteOnExit();
    }

    private static int readUserVersion(File database) {
        RandomAccessFile input = null;
        try {
            if (database.length() < 64L) return 0;
            input = new RandomAccessFile(database, "r");
            input.seek(60L);
            return (input.readUnsignedByte() << 24)
                    | (input.readUnsignedByte() << 16)
                    | (input.readUnsignedByte() << 8)
                    | input.readUnsignedByte();
        } catch (Throwable ignore) {
            return 0;
        } finally {
            try {
                if (input != null) input.close();
            } catch (Throwable ignore) {
            }
        }
    }
}
