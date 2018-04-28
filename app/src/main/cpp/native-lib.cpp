#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring

JNICALL
Java_org_notmuchmail_notmuch_MainActivity_stringFromJNI(
        JNIEnv *env,
        jobject /* this */) {
    std::string hello = "Hello from C++foo";
    return env->NewStringUTF(hello.c_str());
}
