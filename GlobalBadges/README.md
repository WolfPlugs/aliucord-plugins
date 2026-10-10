# Global Badges for Aliucord

Ports the badge feed and settings from `revenge-community-plugins/plugins/global-badges` to Aliucord's native profile header. It uses `https://badges.equicord.org/users`, keeps Discord and other plugins' badges, and refreshes every 30 minutes. Aliucord badges from the shared feed are excluded because Aliucord already supplies its own badges. Unknown providers remain enabled and use their feed name.

The settings page uses Discord-themed switches, section headings, and radio choices with label previews, following ServerSettingsFix's native control styling. It also links to BadgeVault. Settings include provider toggles, custom badges first or last, and client names hidden, prefixed, or suffixed. Reopen a profile after changing settings. Click a badge to read its label. Images are static, downsampled to at most 128 pixels per side, cached in 2 MiB of memory, and limited to 1 MiB downloads. Failed images retry on a later bind after 30 seconds. A failed feed refresh preserves the last successful data until the plugin stops.

This displays badges supplied by the shared service. It does not grant Discord badges or make other users see badges that the service does not assign to your account.

## Build locally

The active plugins use Java. This repository uses Gradle 7.4 and Android Gradle Plugin 7.0.4. Run Gradle with JDK 11 or 17, not the system JDK 27. Also install JDK 21, which this module selects through Gradle's Java toolchain to read the current Aliucord API. The plugin still emits Java 11 bytecode. Install Android SDK Platform 31, Build Tools 30.0.2, and Platform Tools using Android Studio's SDK Manager. Set `ANDROID_SDK_ROOT` to your SDK directory, or put `sdk.dir=/absolute/path/to/Android/Sdk` in the ignored root `local.properties` file.

```bash
cd /home/wolf/Coding/hot-plugins
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
bash gradlew :GlobalBadges:make --no-daemon
find GlobalBadges/build -name 'GlobalBadges.zip'
```

The first build needs internet access for Gradle, Aliucord, and Discord stubs. The build uses D8 8.3.37 because the older bundled dex compiler crashes on classes emitted by current javac. The existing Jetifier setup may warn about upstream Java 21 bytecode. If Gradle cannot find JDK 21, pass `-Porg.gradle.java.installations.paths=/absolute/path/to/jdk-21`.

Use the ZIP reported by `find` below. Do not install an AAR or an APK as the plugin.

## Install and check on a device

1. Install and launch Aliucord, sign in, and grant its requested storage access. Use an Android device or emulator running Aliucord's legacy Discord build.
2. Enable USB debugging and connect the device. Run `adb devices` and accept its authorization prompt. If several devices are connected, pass `-s SERIAL` to the commands below.
3. Back up an existing `GlobalBadges.zip` before replacing it. Copy the newly built ZIP into `/sdcard/Aliucord/plugins/GlobalBadges.zip`. For example, if the build reports `GlobalBadges/build/GlobalBadges.zip`:

   ```bash
   adb shell mkdir -p /sdcard/Aliucord/plugins
   adb push GlobalBadges/build/GlobalBadges.zip /sdcard/Aliucord/plugins/GlobalBadges.zip
   ```

4. Fully close and reopen Aliucord. In Settings > Aliucord > Plugins, enable GlobalBadges. Open its settings using the plugin's settings control.
5. Open a profile listed in the live feed. To find a real ID to test, run:

   ```bash
   curl -fsS https://badges.equicord.org/users -o /tmp/badges.json
   python -c 'import json; d=json.load(open("/tmp/badges.json")); print(next((uid, badges) for uid, badges in d["users"].items() if uid != "0" and badges))'
   ```

6. Confirm custom images load and their click labels match the feed. Check that native Discord badges remain. Reopen the profile several times to check for duplicates, then rapidly switch between profiles to check for stale images. Check a user absent from the feed too.
7. Change order, client-name display, and one provider toggle. Reopen the profile after each change and verify the result. If NewDiscordBadges is installed, repeat with it enabled to check that both plugins' badges remain.
8. Disable GlobalBadges while a profile is open. Its badges should disappear and native badges should remain. Re-enable it and check that badges return. Restart offline to check that network failures do not crash the profile. There is no disk cache, so an offline cold start shows native badges only.

For diagnostics, use Aliucord's debug log and search for `Could not refresh global badges`, or capture `adb logcat` while opening the profile. To remove the local build, disable the plugin, remove its ZIP, and restart Aliucord. Keep automatic plugin updates off while testing a local build so the published version does not replace it.

## Parser checks

`tests/BadgeFeedTest.java` checks valid entries, malformed data, unknown providers, empty feeds, and 64-bit user IDs. It runs independently of Android. With `org.json:json:20240303` downloaded as `/tmp/json-20240303.jar`:

```bash
javac -cp /tmp/json-20240303.jar -d /tmp/global-badges-tests \
  GlobalBadges/src/main/java/com/aliucord/plugins/GlobalBadgeData.java \
  GlobalBadges/tests/BadgeFeedTest.java
java -cp /tmp/json-20240303.jar:/tmp/global-badges-tests \
  com.aliucord.plugins.BadgeFeedTest /tmp/badges.json
```

Omit `/tmp/badges.json` to run fixture checks only. These checks do not verify Android hooks or rendering.

## Validation for this port

`:GlobalBadges:make` passed using JDK 17 for Gradle, the JDK 21 compiler toolchain, SDK Platform 31, and Build Tools 30.0.2. The generated ZIP contains `manifest.json` and `classes.dex`, with entry point `com.aliucord.plugins.GlobalBadges` and version `1.1.0`. Parser checks passed against the fixtures and the live feed, with 6,147 users containing valid badges at the time of testing. Android profile rendering, settings navigation, and coexistence with NewDiscordBadges still need the device checks above.

## References

- [Aliucord](https://github.com/Aliucord/Aliucord)
- [NewDiscordBadges](https://github.com/yutaplug/yutaplugins/tree/main/plugins/NewDiscordBadges), used to verify native badge holder binding and recycled-view handling. This port does not replace Discord profile requests.

- [ServerSettingsFix settings](https://github.com/pilotbellyt-spec/AliucordPlugins/blob/main/plugins/ServerSettingsFix/src/main/kotlin/com/github/pilotbellytspec/serversettingsfix/PluginSettings.kt), used as the reference for Discord-themed text and checked settings rows.
