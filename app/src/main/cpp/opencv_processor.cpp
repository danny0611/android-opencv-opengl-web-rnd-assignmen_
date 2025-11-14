#include "opencv_processor.h"
#include <android/log.h>

#define LOG_TAG "OpenCVProcessor"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

OpenCVProcessor::OpenCVProcessor()
    : processMode(0)  // Default: raw
    , frameWidth(0)
    , frameHeight(0)
    , initialized(false)
{
}

OpenCVProcessor::~OpenCVProcessor() {
    release();
}

bool OpenCVProcessor::init(int width, int height) {
    if (width <= 0 || height <= 0) {
        LOGE("Invalid dimensions: %dx%d", width, height);
        return false;
    }
    
    frameWidth = width;
    frameHeight = height;
    
    // Pre-allocate Mats for performance
    inputMat.create(height, width, CV_8UC4);
    grayMat.create(height, width, CV_8UC1);
    processedMat.create(height, width, CV_8UC4);
    outputMat.create(height, width, CV_8UC4);
    
    initialized = true;
    LOGI("Initialized processor: %dx%d", width, height);
    
    return true;
}

bool OpenCVProcessor::processFrame(unsigned char* rgbaData, int width, int height, cv::Mat& output) {
    if (!initialized) {
        LOGE("Processor not initialized");
        return false;
    }
    
    if (rgbaData == nullptr) {
        LOGE("Null input data");
        return false;
    }
    
    if (width != frameWidth || height != frameHeight) {
        LOGE("Dimension mismatch: expected %dx%d, got %dx%d", 
             frameWidth, frameHeight, width, height);
        if (!init(width, height)) {
            return false;
        }
    }
    
    try {
        cv::Mat inputRGBA(height, width, CV_8UC4, rgbaData);
        inputRGBA.copyTo(inputMat);
        
        switch (processMode) {
            case 0: // Raw
                inputMat.copyTo(outputMat);
                break;
            case 1: // Grayscale
                processGrayscale(inputMat, outputMat);
                break;
            case 2: // Canny
                processCanny(inputMat, outputMat);
                break;
            default:
                LOGE("Unknown process mode: %d", processMode);
                inputMat.copyTo(outputMat); // Fallback to raw
                break;
        }
        
        output = outputMat.clone();
        
        return true;
    } catch (const cv::Exception& e) {
        LOGE("OpenCV exception: %s", e.what());
        return false;
    } catch (...) {
        LOGE("Unknown exception in processFrame");
        return false;
    }
}

void OpenCVProcessor::processGrayscale(const cv::Mat& input, cv::Mat& output) {
    cv::cvtColor(input, grayMat, cv::COLOR_RGBA2GRAY);
    cv::cvtColor(grayMat, output, cv::COLOR_GRAY2RGBA);
}

void OpenCVProcessor::processCanny(const cv::Mat& input, cv::Mat& output) {
    cv::cvtColor(input, grayMat, cv::COLOR_RGBA2GRAY);
    cv::GaussianBlur(grayMat, grayMat, cv::Size(5, 5), 1.5);
    cv::Canny(grayMat, grayMat, 50, 150);
    cv::cvtColor(grayMat, output, cv::COLOR_GRAY2RGBA);
}

void OpenCVProcessor::setProcessMode(int mode) {
    if (mode >= 0 && mode <= 2) {
        processMode = mode;
        LOGI("Process mode set to: %d", mode);
    } else {
        LOGE("Invalid process mode: %d", mode);
    }
}

void OpenCVProcessor::release() {
    inputMat.release();
    grayMat.release();
    processedMat.release();
    outputMat.release();
    
    initialized = false;
    LOGI("Processor released");
}
