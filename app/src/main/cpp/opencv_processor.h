#ifndef OPENCV_PROCESSOR_H
#define OPENCV_PROCESSOR_H

#include <opencv2/opencv.hpp>

class OpenCVProcessor {
public:
    OpenCVProcessor();
    ~OpenCVProcessor();

    bool init(int width, int height);
    bool processFrame(unsigned char* yData, unsigned char* uData, unsigned char* vData, int yStride, int uStride, int vStride, int uvPixelStride, int width, int height, cv::Mat& output);
    void setProcessMode(int mode);
    void release();

private:
    int processMode;
    cv::Mat yuvMat;
    cv::Mat rgbMat;
    cv::Mat grayMat;
    int frameWidth;
    int frameHeight;
    bool initialized;
};

#endif // OPENCV_PROCESSOR_H
