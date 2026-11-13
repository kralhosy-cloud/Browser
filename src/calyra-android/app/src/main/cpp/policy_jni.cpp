#include <jni.h>
#include "calyra/policy_record.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_calyra_browser_policy_PolicyCodec_validate(JNIEnv* env, jclass, jbyteArray arr) {
    jbyte* buf = env->GetByteArrayElements(arr, nullptr);
    jsize len = env->GetArrayLength(arr);
    if (len != 64) { env->ReleaseByteArrayElements(arr, buf, JNI_ABORT); return -1; }
    CalyraPolicyRecord rec;
    CalyraPolicyStatus s = calyra_policy_decode((uint8_t*)buf, &rec);
    env->ReleaseByteArrayElements(arr, buf, JNI_ABORT);
    return (jint)s; // 0=OK
}
