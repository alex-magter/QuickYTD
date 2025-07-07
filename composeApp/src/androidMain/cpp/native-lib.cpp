#include <jni.h>
#include "<string>"

extern "C"
JNIEXPORT jstring JNICALL
Java_org_alexmagter_QuickYTD_TestNativeCode_stringFromFFmpeg(JNIEnv* env, jobject /* this */) {
    return env->NewStringUTF("Hola desde FFmpeg nativo!");
}