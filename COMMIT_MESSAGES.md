# Recommended Commit Messages

## Module: Camera
```
feat(android): implement Camera2 API with TextureView

- Add Camera2 integration with TextureView for frame capture
- Implement repeating frame capture with CaptureSession
- Add runtime permissions handling for camera access
- Create MainActivity with camera lifecycle management
- Add frame capture callback and FPS tracking
- Implement background thread for camera operations
```

## Module: JNI
```
feat(jni): create JNI bridge between Android and native C++

- Add native-lib.cpp with JNI function implementations
- Implement nativeInit() for processor initialization
- Implement nativeProcessFrame() for frame processing
- Add nativeSetProcessMode() for mode switching
- Implement nativeRelease() for resource cleanup
- Add proper error handling and logging
```

## Module: OpenCV
```
feat(opencv): implement OpenCV C++ image processing

- Create OpenCVProcessor class with Mat reuse optimization
- Implement RGBA to Mat conversion
- Add grayscale conversion processing
- Implement Canny edge detection with Gaussian blur
- Add raw mode (no processing)
- Optimize memory usage with pre-allocated Mats
- Prevent memory leaks with proper resource management
```

## Module: GL (OpenGL)
```
feat(opengl): implement OpenGL ES 2.0 renderer

- Create GLRenderer class for frame rendering
- Add vertex shader for full-screen quad
- Add fragment shader for texture sampling
- Implement GL texture creation and updates
- Add real-time rendering of processed frames
- Support texture updates from background thread
- Add proper shader compilation error handling
```

## Module: Web
```
feat(web): create TypeScript web viewer for processed frames

- Add index.html with modern UI design
- Implement CameraViewer class in TypeScript
- Add FPS and resolution overlay displays
- Implement processing mode toggle
- Add status indicators and frame counter
- Create sample frame generation for testing
- Add clean modular TypeScript code structure
```

## Module: Docs
```
docs: add comprehensive README and project documentation

- Add project overview and architecture diagram
- Document all features implemented
- Add detailed folder structure
- Include complete build steps for Android + NDK + OpenCV
- Add web viewer usage instructions
- Include performance notes and optimization tips
- Add troubleshooting section
- Create commit messages documentation
```

## Additional Commits

### Build Configuration
```
build: configure Android project with CMake and OpenCV

- Add build.gradle.kts with NDK and CMake configuration
- Configure CMakeLists.txt with OpenCV integration
- Set up proper ABI filters for multiple architectures
- Add external native build configuration
```

### Resources
```
feat(resources): add Android layout and string resources

- Create activity_main.xml layout with TextureView and GLSurfaceView
- Add strings.xml with app strings
- Configure AndroidManifest.xml with camera permissions
- Add proper activity configuration
```

### Shaders
```
feat(shaders): add OpenGL ES shader files

- Create vertex_shader.glsl for full-screen quad rendering
- Create fragment_shader.glsl for texture sampling
- Add proper shader code for real-time rendering
```

