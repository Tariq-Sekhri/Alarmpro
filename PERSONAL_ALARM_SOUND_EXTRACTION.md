# Reuse an alarm sound from an installed Android app (personal phone)

This is the working process used to get the **Good Morning** sound from the old **Talking Alarm Clock Beyond** app. It keeps the sound as a local file on the phone; it does not add it to the Alarm Pro app binary.

## What you need

- **ML Manager** — extracts the installed app package.
- **ZArchiver** — opens the extracted package and copies out the audio.

## Steps

1. In **ML Manager**, find the *old/source* app: **Talking Alarm Clock Beyond** (Sentry Apps).
   - Do **not** extract Alarm Pro; that is the new app and does not contain the old sound.
2. Tap **Extract APK**.
3. In a file manager, find the extraction at:

   `Internal storage/Android/media/com.javiersantos.mlmanager/files`

4. Open the extracted `com.sentryapplications.alarmclock_...` file with **ZArchiver**.
5. Choose **View**.
   - The first archive is a split-package container, so it will contain `base.apk` plus files named `split_config...apk`.
6. Tap `base.apk`.
7. Choose **Extract...** and extract it to `Download`.
   - The nested `base.apk` offers **Install** and **Extract**, not View. This extraction step is required.
8. Go to `Download` in ZArchiver and open the newly extracted standalone `base.apk` using **View**.
9. Look for the sound files:
   - Check `assets` first.
   - Then check `res/raw`.
   - If needed, use ZArchiver's search for `ogg`, then `mp3`.
10. Identify the wanted sound by playing the files. Copy the wanted audio file (or the `raw` folder if needed) out of the APK.
11. Paste the audio file into:

    `Internal storage/Alarms`

12. In Alarm Pro, use **Select ringtone** / **Custom sound** and choose the file from the `Alarms` folder.

## Notes

- Keep only the intended sound in `Alarms` if you do not want every old alarm sound appearing in the picker.
- Do not choose **Install**, **Compress**, or **Delete** while opening the APK. Use **View** for the outer archive and extracted standalone `base.apk`.
