#ifndef OPENCV_PROCESSOR_H
#define OPENCV_PROCESSOR_H

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>

/**
 * OpenCV Processor class
 * Handles frame processing: YUV/NV21 -> Mat -> RGBA conversion
 * Applies Canny Edge Detection or Grayscale processing
 * Reuses Mats for performance optimization
 */
class OpenCVProcessor {
public:
    OpenCVProcessor();
    ~OpenCVProcessor();
    
    /**
     * Initialize processor with frame dimensions
     * @param width Frame width
     * @param height Frame height
     * @return true if successful
     */
    bool init(int width, int height);
    
    /**
     * Process frame
     * @param rgbaData Input RGBA data
     * @param width Frame width
     * @param height Frame height
     * @param output Processed output Mat
     * @return true if successful
     */
    bool processFrame(unsigned char* rgbaData, int width, int height, cv::Mat& output);
    
    /**
     * Set processing mode
     * @param mode 0=raw, 1=grayscale, 2=canny
     */
    void setProcessMode(int mode);
    
    /**
     * Release resources
     */
    void release();

private:
    // Processing mode: 0=raw, 1=grayscale, 2=canny
    int processMode;
    
    // Reusable Mats for performance
    cv::Mat inputMat;
    cv::Mat grayMat;
    cv::Mat processedMat;
    cv::Mat outputMat;
    
    // Frame dimensions
    int frameWidth;
    int frameHeight;
    
    // Initialization flag
    bool initialized;
    
    /**
     * Process grayscale
     */
    void processGrayscale(const cv::Mat& input, cv::Mat& output);
    
    /**
     * Process Canny edge detection
     */
    void processCanny(const cv::Mat& input, cv::Mat& output);
};

#endif // OPENCV_PROCESSOR_H

