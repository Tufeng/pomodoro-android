# Add project specific ProGuard rules here.

# ─── Room ────────────────────────────────────────────────────────
-keep class com.pomodoro.app.data.model.** { *; }
-keep interface com.pomodoro.app.data.repository.** { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}

# ─── Parcelable ──────────────────────────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ─── DataStore ───────────────────────────────────────────────────
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ─── Coroutines ──────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ─── Kotlin ──────────────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
