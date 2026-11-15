#include <jni.h>
#include <sys/stat.h>
#include <errno.h>
#include <string.h>

JNIEXPORT jlong JNICALL Java_com_ethk3_novaonepro_strategyv2_HistoryPosixStatR31Host_blocks0
  (JNIEnv *env, jclass type, jstring path) {
    (void) type;
    if (path == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), "POSIX_STAT_NULL_PATH");
        return -1;
    }
    const char *name = (*env)->GetStringUTFChars(env, path, NULL);
    if (name == NULL) return -1;
    struct stat measured;
    int result = stat(name, &measured);
    int failure = errno;
    (*env)->ReleaseStringUTFChars(env, path, name);
    if (result != 0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/io/IOException"), strerror(failure));
        return -1;
    }
    return (jlong) measured.st_blocks;
}
