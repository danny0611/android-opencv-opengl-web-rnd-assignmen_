# Expected Project Output

## Android App Runtime Output

### When App Starts:

```
[Log] MainActivity: onCreate() called
[Log] MainActivity: Loading native library: opencvgl
[Log] NativeLib: Library loaded successfully
[Log] MainActivity: Checking camera permission...
```

### Camera Initialization:

```
[Log] MainActivity: Surface texture available: 1920 x 1080
[Log] MainActivity: Opening camera...
[Log] MainActivity: Camera opened: 0
[Log] MainActivity: Initializing native processor: 1920x1080
[Log] OpenCVProcessor: Initialized processor: 1920x1080
[Log] MainActivity: Resolution: 1920x1080
[Log] MainActivity: Capture session configured
[Log] MainActivity: Repeating capture request started
```

### Frame Processing (Every Frame):

```
[Log] MainActivity: Frame captured
[Log] MainActivity: Processing frame: 1920x1080
[Log] NativeLib: Processing frame: 1920x1080
[Log] OpenCVProcessor: Processing frame in mode: 1 (Grayscale)
[Log] OpenCVProcessor: Frame processed successfully
[Log] GLRenderer: Texture updated: 1920x1080
[Log] MainActivity: FPS: 30
```

### Processing Mode Toggle:

```
[Log] MainActivity: Toggle button clicked
[Log] MainActivity: Process mode changed to: 2 (Canny)
[Log] NativeLib: Process mode set to: 2
[Log] OpenCVProcessor: Process mode set to: 2
```

### UI Display:

**On Screen:**
- Camera preview in TextureView (raw camera feed)
- Processed frame in GLSurfaceView (Grayscale/Canny/Raw)
- FPS overlay: "FPS: 30" (updates every second)
- Resolution overlay: "Resolution: 1920x1080"
- Toggle button: "Toggle Processed" (cycles: Raw → Grayscale → Canny)

## Web Viewer Output

### Browser Console (When Page Loads):

```
Camera viewer initialized
[Log] DOMContentLoaded event fired
[Log] Canvas initialized: 1920x1080
[Log] Sample frame loaded
[Log] FPS counter started
```

### UI Display:

**Visual Elements:**
- Header: "📷 OpenCV GL Camera - Real-time Processed Frame Viewer"
- Canvas displaying processed frame (simulated pattern)
- Overlay showing:
  - FPS: 30 (updating)
  - Resolution: 1920x1080
  - Status: Connected (green indicator)
- Frame Information Panel:
  - Frame Count: 1, 2, 3... (incrementing)
  - Last Update: [current time]
  - Processing Mode: Grayscale

### Button Interactions:

**Load Sample Frame:**
```
[Log] Loading sample frame...
[Log] Frame generated: 1920x1080
[Log] Frame count: 1
```

**Toggle Processing:**
```
[Log] Processing mode changed: grayscale → canny
[Log] Process mode element updated
[Log] Frame regenerated with new mode
```

**Reset:**
```
[Log] Viewer reset
[Log] Frame count: 0
[Log] Process mode: grayscale
```

## Build Output

### Android Studio Build:

```
> Task :app:compileDebugKotlin
> Task :app:generateDebugBuildConfig
> Task :app:externalNativeBuildDebug
  Building CXX object CMakeFiles/opencvgl.dir/native-lib.cpp.o
  Building CXX object CMakeFiles/opencvgl.dir/opencv_processor.cpp.o
  Linking CXX shared library libopencvgl.so
> Task :app:packageDebug
  APK built successfully: app-debug.apk
```

### TypeScript Compilation:

```
$ npm run build
> tsc

Compiling main.ts...
main.ts → dist/main.js
Build complete!
```

## Expected Logcat Output (Android)

### Successful Run:

```
D/MainActivity: onCreate() called
D/MainActivity: Surface texture available: 1920 x 1080
D/MainActivity: Opening camera...
D/MainActivity: Camera opened: 0
I/NativeLib: Initializing native processor: 1920x1080
I/OpenCVProcessor: Initialized processor: 1920x1080
D/MainActivity: Resolution: 1920x1080
D/MainActivity: Capture session configured
D/MainActivity: Repeating capture request started
D/MainActivity: Frame captured
I/NativeLib: Processing frame: 1920x1080
I/OpenCVProcessor: Processing frame in mode: 1
D/GLRenderer: Texture updated: 1920x1080
D/MainActivity: FPS: 30
```

### Error Scenarios:

**Missing OpenCV:**
```
E/CMake: OpenCV not found. Please set OpenCV_DIR
E/NativeLib: Failed to initialize processor
E/MainActivity: Failed to initialize native code
```

**Camera Permission Denied:**
```
W/MainActivity: Camera permission denied
E/MainActivity: Cannot open camera without permission
```

**Camera Not Available:**
```
E/MainActivity: No cameras available
E/MainActivity: Camera error: 4
```

## Performance Metrics

### Expected Performance:

- **Frame Rate**: 25-30 FPS (depending on device)
- **Processing Time**: 
  - Grayscale: ~5-10ms per frame
  - Canny: ~20-30ms per frame
  - Raw: ~1-2ms per frame
- **Memory Usage**: ~50-100MB
- **CPU Usage**: 20-40% on mid-range device

### Frame Processing Pipeline:

```
Camera Frame (1920x1080 YUV) 
  → TextureView Display (raw preview)
  → Convert to RGBA (5ms)
  → JNI Bridge (1ms)
  → OpenCV Processing (10-30ms)
  → Return RGBA (1ms)
  → OpenGL Texture Update (2ms)
  → GLSurfaceView Render (16ms @ 60fps)
Total: ~35-50ms per frame = 20-28 FPS
```

## Visual Output Description

### Android App Screen:

```
┌─────────────────────────────────────┐
│  [FPS: 30]  [Resolution: 1920x1080] │ ← Overlay
│                                     │
│  ┌───────────────────────────────┐ │
│  │                               │ │
│  │   Processed Frame (OpenGL)    │ │ ← GLSurfaceView
│  │   (Grayscale/Canny/Raw)       │ │
│  │                               │ │
│  └───────────────────────────────┘ │
│                                     │
│              [Toggle Processed]     │ ← Button
└─────────────────────────────────────┘
```

### Web Viewer Screen:

```
┌─────────────────────────────────────┐
│  📷 OpenCV GL Camera                │ ← Header
│  Real-time Processed Frame Viewer   │
├─────────────────────────────────────┤
│  ┌───────────────────────────────┐ │
│  │  FPS: 30                      │ │ ← Overlay
│  │  Resolution: 1920x1080        │ │
│  │  Status: ● Connected           │ │
│  │                               │ │
│  │   [Processed Frame Canvas]    │ │
│  │                               │ │
│  └───────────────────────────────┘ │
│                                     │
│  [Load Sample] [Toggle] [Reset]    │ ← Buttons
│                                     │
│  📊 Frame Information               │
│  Frame Count: 42                    │
│  Last Update: 14:32:15              │
│  Processing Mode: Grayscale         │
└─────────────────────────────────────┘
```

## Test Scenarios

### Scenario 1: First Launch
1. App opens → Permission dialog appears
2. User grants permission → Camera opens
3. Native code initializes → Processing starts
4. FPS counter shows ~30 FPS
5. Processed frame visible in GLSurfaceView

### Scenario 2: Mode Toggle
1. User clicks "Toggle Processed"
2. Mode changes: Grayscale → Canny → Raw → Grayscale
3. Processing time changes (visible in FPS)
4. Visual output changes accordingly

### Scenario 3: Web Viewer
1. Open index.html in browser
2. Canvas shows simulated processed frame
3. FPS counter increments
4. Click "Load Sample Frame" → New frame generated
5. Click "Toggle Processing" → Mode changes

## Error Handling Output

### Graceful Error Messages:

**Camera Error:**
```
Toast: "Camera error: 4"
Log: "E/MainActivity: Camera error: 4"
```

**OpenCV Not Found:**
```
Log: "E/CMake: OpenCV not found"
Log: "E/NativeLib: Failed to initialize processor"
```

**Permission Denied:**
```
Toast: "Camera permission is required for this app to work"
Log: "W/MainActivity: Camera permission denied"
```

## Success Indicators

✅ **App Running Successfully:**
- Camera preview visible
- Processed frame rendering
- FPS counter updating
- No error logs
- Smooth frame rate (25+ FPS)

✅ **Web Viewer Working:**
- Canvas displays frame
- Overlays showing correct values
- Buttons responsive
- No console errors

---

**Note**: Actual output may vary based on device capabilities, OpenCV version, and Android version.

