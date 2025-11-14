# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI classes
-keep class com.opencvgl.app.** { *; }

# Keep OpenCV classes if any
-keep class org.opencv.** { *; }

# Keep GL renderer
-keep class com.opencvgl.app.GLRenderer { *; }

