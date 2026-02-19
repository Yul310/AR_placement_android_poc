# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep ARCore classes
-keep class com.google.ar.** { *; }
-keep class com.google.ar.sceneform.** { *; }

# Keep data models for Gson
-keep class com.willitfit.data.model.** { *; }
