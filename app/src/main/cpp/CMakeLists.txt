cmake_minimum_required(VERSION 3.22.1)

project("opencvgl")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# --- Manual OpenCV Configuration ---
# 1. Set the path to your extracted OpenCV Android SDK
set(OpenCV_SDK_DIR "C:/Users/dhanu/Downloads/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk")

# 2. Include the OpenCV header files
include_directories(${OpenCV_SDK_DIR}/native/jni/include)

# 3. Add the pre-built OpenCV library
add_library(lib_opencv SHARED IMPORTED)
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION
    ${OpenCV_SDK_DIR}/native/libs/${ANDROID_ABI}/libopencv_java4.so)
# --- End Manual OpenCV Configuration ---


# Add include directories for your project
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Source files for your native library
set(NATIVE_SOURCES
    native-lib.cpp
    opencv_processor.cpp
)

# Create your shared library
add_library(opencvgl SHARED ${NATIVE_SOURCES})

# Link your library against OpenCV and other Android libraries
target_link_libraries(
    opencvgl
    lib_opencv # Link against the imported OpenCV library
    android
    log
    GLESv2
    EGL
)

# Compiler flags
target_compile_options(opencvgl PRIVATE
    -Wall
    -Wextra
    -fexceptions
    -frtti
)
