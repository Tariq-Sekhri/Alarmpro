# Corrected larger controls

Validated on the Android 35 emulator at 1080 x 1920, 480 dpi (360 dp wide), with four timers and Notes enabled.

The larger-controls setting now scales individual buttons, their content and switches by 50%, without changing screen density, ordinary text or card spacing. Timer card padding is reduced and the three controls are spread across the available row. Add timer is in the header so it cannot obscure a timer switch.

Validation: debug and test APK builds, 11 JVM unit tests, and the emulator instrumentation test passed. The UI test measures the restart icon itself before and after enabling the setting and requires at least 45% growth. It also checks switch persistence, all main tabs, the timer dialog, the alarm editor, remembered tabs and explicit intent targets.

- [Timer, setting off](00-timer-default.png)
- [Timer, setting on](04-timer-larger.png)
- [Settings](02-settings-larger.png)
- [Alarm](03-alarm-larger.png)
- [Timer dialog](05-add-timer-larger.png)
- [Stopwatch](06-stopwatch-larger.png)
- [Clock](07-clock-larger.png)
- [Alarm editor](09-alarm-editor-larger.png)
