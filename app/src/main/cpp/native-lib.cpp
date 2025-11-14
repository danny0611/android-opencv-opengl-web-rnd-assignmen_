#include <jni.h>
#include <string>
#include <android/log.h>
#include "opencv_processor.h"

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Global processor instance
static OpenCVProcessor* g_processor = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_opencvgl_app_MainActivity_nativeInit(JNIEnv *env, jobject thiz, jint width, jint height) {
    LOGI("Initializing native processor: %dx%d", width, height);
    if (g_processor != nullptr) {
        delete g_processor;
    }
    g_processor = new OpenCVProcessor();
    if (!g_processor->init(width, height)) {
        LOGE("Failed to initialize processor");
        delete g_processor;
        g_processor = nullptr;
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_opencvgl_app_MainActivity_nativeProcessFrame(JNIEnv *env, jobject thiz, jbyteArray rgbaData, jint width, jint height) {
    if (g_processor == nullptr) {
        LOGE("Processor not initialized");
        return nullptr;
    }

    jbyte* data = env->GetByteArrayElements(rgbaData, nullptr);
    if (data == nullptr) {
        LOGE("Failed to get byte array elements");
        return nullptr;
    }

    cv::Mat processed;
    bool success = g_processor->processFrame(reinterpret_cast<unsigned char*>(data), width, height, processed);
    env->ReleaseByteArrayElements(rgbaData, data, JNI_ABORT);

    if (!success || processed.empty()) {
        LOGE("Failed to process frame");
        return nullptr;
    }

    jsize size = processed.rows * processed.cols * processed.channels();
    jbyteArray result = env->NewByteArray(size);
    if (result == nullptr) {
        LOGE("Failed to create result byte array");
        return nullptr;
    }

    env->SetByteArrayRegion(result, 0, size, reinterpret_cast<jbyte*>(processed.data));
    return result;
}

JNIEXPORT void JNICALL
Java_com_opencvgl_app_MainActivity_nativeSetProcessMode(JNIEnv *env, jobject thiz, jint mode) {
    if (g_processor == nullptr) {
        LOGE("Processor not initialized");
        return;
    }
    g_processor->setProcessMode(mode);
    LOGI("Process mode set to: %d", mode);
}

JNIEXPORT void JNICALL
Java_com_opencvgl_app_MainActivity_nativeRelease(JNIEnv *env, jobject thiz) {
    LOGI("Releasing native processor");
    if (g_processor != nullptr) {
        delete g_processor;
        g_processor = nullptr;
    }
}

} // extern "C"
