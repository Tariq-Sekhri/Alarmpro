# AlarmPro R8 / ProGuard rules

# Preserve source attributes and line numbers for Play Console de-obfuscation stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve annotations and type signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve enum names and constants persisted to SharedPreferences (e.g., AlarmSortMode, TimePickerStyle)
-keepclassmembers enum ca.sekhrit.alarmpro.data.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static final ** *;
}
