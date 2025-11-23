#include "opencv_processor.h"
#include <android/log.h>
#include <vector>

#define LOG_TAG "OpenCVProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

OpenCVProcessor::OpenCVProcessor() : processMode(0), frameWidth(0), frameHeight(0), initialized(false) {}

OpenCVProcessor::~OpenCVProcessor() {
    release();
}

bool OpenCVProcessor::init(int width, int height) {
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions for init: %dx%d", width, height);
        return false;
    }
    frameWidth = width;
    frameHeight = height;

    yuvMat.create(height * 3 / 2, width, CV_8UC1);
    rgbMat.create(height, width, CV_8UC4);
    grayMat.create(height, width, CV_8UC1);

    initialized = true;
    LOGI("Processor initialized with dimensions: %dx%d", width, height);
    return true;
}

bool OpenCVProcessor::processFrame(unsigned char* yData, unsigned char* uData, unsigned char* vData, int yStride, int uStride, int vStride, int uvPixelStride, int width, int height, cv::Mat& output) {
    if (!initialized || !yData || !uData || !vData) {
        LOGE("Not initialized or null data");
        return false;
    }

    try {
        cv::Mat yMat(height, width, CV_8UC1, yData, yStride);

        // Manually reconstruct the YUV image in a single contiguous Mat to pass to cvtColor.
        // This is necessary to handle row strides which can have padding.
        if (uvPixelStride == 2) { // Semi-planar (NV21)
            cv::Mat uvMat(height / 2, width / 2, CV_8UC2, vData, vStride);
            for (int i = 0; i < height; i++) {
                memcpy(yuvMat.data + i * width, yMat.data + i * yStride, width);
            }
            unsigned char* uvDst = yuvMat.data + height * width;
            for (int i = 0; i < height / 2; i++) {
                memcpy(uvDst + i * width, uvMat.data + i * vStride, width);
            }
            cv::cvtColor(yuvMat, rgbMat, cv::COLOR_YUV2RGBA_NV21);
        } else { // Planar (I420)
            cv::Mat uMat(height / 2, width / 2, CV_8UC1, uData, uStride);
            cv::Mat vMat(height / 2, width / 2, CV_8UC1, vData, vStride);
            for (int i = 0; i < height; i++) {
                memcpy(yuvMat.data + i * width, yMat.data + i * yStride, width);
            }
            unsigned char* uDst = yuvMat.data + height * width;
            for (int i = 0; i < height / 2; i++) {
                memcpy(uDst + i * (width / 2), uMat.data + i * uStride, width / 2);
            }
            unsigned char* vDst = yuvMat.data + height * width + (height / 2) * (width / 2);
            for (int i = 0; i < height / 2; i++) {
                memcpy(vDst + i * (width / 2), vMat.data + i * vStride, width / 2);
            }
            cv::cvtColor(yuvMat, rgbMat, cv::COLOR_YUV2RGBA_I420);
        }

        switch (processMode) {
            case 1: // Grayscale
                cv::cvtColor(rgbMat, grayMat, cv::COLOR_RGBA2GRAY);
                cv::cvtColor(grayMat, output, cv::COLOR_GRAY2RGBA);
                break;
            case 2: // Canny
                cv::cvtColor(rgbMat, grayMat, cv::COLOR_RGBA2GRAY);
                cv::GaussianBlur(grayMat, grayMat, cv::Size(5, 5), 1.5);
                cv::Canny(grayMat, grayMat, 50, 150);
                cv::cvtColor(grayMat, output, cv::COLOR_GRAY2RGBA);
                break;
            default: // Raw
                rgbMat.copyTo(output);
                break;
        }
        return true;
    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception in processFrame: %s", e.what());
        return false;
    }
}

void OpenCVProcessor::setProcessMode(int mode) {
    if (mode >= 0 && mode <= 2) {
        processMode = mode;
    }
}

void OpenCVProcessor::release() {
    if (initialized) {
        yuvMat.release();
        rgbMat.release();
        grayMat.release();
        initialized = false;
        LOGI("Processor resources released");
    }
}
