# Settings and accessibility review

Verified on Android 35 emulator `TimeTrackerTauriQA35`, at 1080 x 1920 / 480 dpi (360 dp width), on 2026-10-20.

Both new switches live in General Settings and default off:

- Remember last tab restores the most recently settled main tab on a cold launch. Explicit notification/shortcut tab targets take priority. With the setting off, a cold launch opens Alarm.
- Larger buttons enlarges the main app controls, touch targets, and labels by 20%, retaining Android font scaling. Timer controls move below their labels on narrow layouts. The fixed-width analog picker fits its available space so AM/PM stays visible.

Validation passed:

- Debug app and instrumentation APK builds, and all 11 JVM unit tests.
- `AccessibilitySettingsTest`: switch defaults, saved settings, increased button bounds, all four main tabs, timer dialog, alarm editor, tab restoration, explicit intent override, and disabled restoration.
- Enabled Remember last tab using the emulator UI, selected Timer, force-stopped the app, launched it without a target extra, and verified Timer remained selected.
- Inspected the captured layouts. Lower settings and stopwatch controls remain reachable by scrolling; long timer labels and durations fit with controls on a separate row.

Screenshots:

- [Settings, defaults off](01-settings-default.png)
- [Settings, both enabled](02-settings-larger.png)
- [Alarm](03-alarm-larger.png)
- [Timer](04-timer-larger.png)
- [Timer dialog](05-add-timer-larger.png)
- [Stopwatch](06-stopwatch-larger.png)
- [Clock](07-clock-larger.png)
- [Restored Timer](08-restored-timer.png)
- [Alarm editor](09-alarm-editor-larger.png)
- [Timer after force-stop and cold launch](10-force-stop-restored-timer.png)
- [Long timer and controls after scrolling](11-timer-scrolled-larger.png)

The Android 37 preview emulator could not run the project's existing Espresso version (`InputManager.getInstance` is missing); final UI validation used Android 35.
