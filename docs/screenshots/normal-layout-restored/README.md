# Restore normal timer layout

When Larger buttons is off, timer controls stay beside the duration in the original single row, with 14 dp vertical padding, a centered floating Add button, and the original list bottom padding. The two-row controls and header Add button apply only in larger mode.

Validated on Android 35 emulator at 360 dp width with Notes enabled. Debug/test APK builds, 11 JVM tests and AccessibilitySettingsTest passed. Regression assertions check normal controls stay beside the duration, the floating Add button exists only in normal mode, and enlarged restart icons still grow by 50%.

- [Restored normal layout](normal-timer.png)
- [Larger layout](larger-timer.png)
