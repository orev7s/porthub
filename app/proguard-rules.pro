-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.porthub.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.porthub.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.porthub.app.data.**$$serializer { *; }
