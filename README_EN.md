# ScreenShotObserver Unity Plugin

[![Unity](https://img.shields.io/badge/Unity-2018.4%2B-blue.svg)](https://unity3d.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[中文](README.md) | English

## Overview

`ScreenShotObserver` is a standalone Unity plugin for monitoring screenshot behavior on Android and iOS platforms. The plugin encapsulates native platform screenshot monitoring functionality and provides a unified C# API interface.

**Keywords**: Unity Screenshot, Unity Screencapture, Mobile Screenshot, Mobile Screencapture

> ⚠️ **Important Note**: Regarding screenshot file path retrieval:
> - **Android 13 and below**: Can get the full file path of the screenshot
> - **Android 14+**: Can only monitor screenshot events, cannot get file path
> - **iOS**: Can only monitor screenshot events, cannot get file path

> 📚 **Reference Implementation**: The Android implementation of this plugin references the implementation approach from the [DoubleD0721/Screenshot](https://github.com/DoubleD0721/Screenshot) project, and we express our gratitude here.

## Features

- ✅ **Cross-platform Support**: Supports Android and iOS platforms
- ✅ **Standalone Plugin**: No dependencies on other SDK Helpers, can be used independently
- ✅ **Singleton Pattern**: Uses singleton pattern to manage instances
- ✅ **File Path Support**: Android 13 and below systems can get full screenshot file paths
- ✅ **Unity Package Manager**: Supports installation and management through UPM

## Installation

### Via Unity Package Manager (Recommended)

1. Open your Unity project
2. In Unity Editor, select `Window` > `Package Manager`
3. Click the `+` button in the top left corner, select `Add package from git URL...`
4. Enter the following URL:
   ```
   https://github.com/chinatragedy/Unity-ScreenShotObserver.git
   ```
5. Click the `Add` button

### Manual Installation

Copy the `Runtime` folder to your Unity project's `Assets` directory.

## Usage

```csharp
using Unicorn.Herman.ScreenShotObserver;

// Start screenshot monitoring
ScreenShotObserver.Instance.StartListenScreenShot("NativeMsgRx", "OnScreenshotDetected", true);

// Stop screenshot monitoring
ScreenShotObserver.Instance.StopListenScreenShot();
```

### Receiving Screenshot Callbacks

Create a GameObject (e.g., `NativeMsgRx`), add a script and implement the callback method:

```csharp
public class ScreenshotReceiver : MonoBehaviour
{
    public void OnScreenshotDetected(string filePath)
    {
        // Android 13 and below: filePath contains the full screenshot file path
        // Android 14+ and iOS: filePath is "screenshot_detected" (event marker only, no real path)
        Debug.Log($"Screenshot detected! FilePath: {filePath ?? "N/A"}");
    }
}
```

## API Documentation

### ScreenShotObserver.Instance

Singleton instance for accessing plugin functionality.

### StartListenScreenShot(string gameObjectName, string methodName, bool useDetectScreenCapture)

Start screenshot behavior monitoring.

**Parameters:**
- `gameObjectName`: Name of the GameObject that receives screenshot callbacks
- `methodName`: Name of the method that receives screenshot callbacks
- `useDetectScreenCapture`: Whether to use Android 14+ ScreenCaptureCallback strategy (true = more accurate but no file path; false = legacy strategy, may get file path but not 100% accurate)

### StopListenScreenShot()

Stop screenshot behavior monitoring.

## Platform Implementation Details

### Android

- **Android 14+ (API 34)**: Uses `Activity.registerScreenCaptureCallback()` official API (event notification only, no file path)
- **Android 13 and below**: Uses `ContentObserver` to monitor media library changes (can get full file path)
- Automatically selects the best implementation based on system version
- **Permission configuration (manual)**: This plugin **no longer** ships an auto-merge `AndroidManifest.xml`. Please add the required permissions to your project's `Assets/Plugins/Android/AndroidManifest.xml` (see “Notes / Permissions” below).

### iOS

- Uses `UIApplicationUserDidTakeScreenshotNotification` system notification (event notification only, no file path)
- Callbacks to C# layer through Unity's `UnitySendMessage`

## Notes

1. **File Path Retrieval**:
   - **Android 13 and below**: Can get the full file path of the screenshot
   - **Android 14+**: Can only monitor screenshot events, cannot get file path (system limitation)
   - **iOS**: Can only monitor screenshot events, cannot get file path (system limitation)

2. **Callback Method Signature**:
   - Method must be `public`
   - Method signature: `void MethodName(string filePath)`
   - `filePath` parameter contains full path on Android 13 and below; Android 14+ / iOS returns `"screenshot_detected"` (event marker only, no real path)

3. **Permission Requirements**:
   - **Android**: You must manually declare permissions in your project's main `AndroidManifest.xml` (this plugin does not auto-merge)
     - Android 14+: `android.permission.DETECT_SCREEN_CAPTURE`
     - Android 13+: `android.permission.READ_MEDIA_IMAGES`
     - Android 12 and below: `android.permission.READ_EXTERNAL_STORAGE` (recommended with `android:maxSdkVersion="32"`)
   - **iOS**: No special permissions required

   **Capability boundaries (important)**:
   - If you **do not declare media permissions** (`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`), the **legacy strategy will be disabled**:
     - Android < 14: legacy listening will not work (effectively no listening / no path)
     - Android 14+: only the DETECT strategy can listen for events; if DETECT is unavailable, listening will not work either

   **Behavior matrix (current implementation: silent no-op when permission is not declared or not granted)**:

| Platform/Strategy | useDetectScreenCapture | Manifest permission declared | Permission granted | Result |
|---|---:|---|---|---|
| Android 14+ DETECT strategy | true | `DETECT_SCREEN_CAPTURE` not declared | - | Silent return, listener not started |
| Android 14+ DETECT strategy | true | `DETECT_SCREEN_CAPTURE` declared | Not granted / ROM restricted | Silent return, listener not started |
| Android 14+ DETECT strategy | true | `DETECT_SCREEN_CAPTURE` declared | Granted | Listener started; callback payload is `"screenshot_detected"` |
| Legacy strategy (any Android) | false (or Android < 14) | No `READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE` declared | - | Silent return, listener not started |
| Legacy strategy (any Android) | false (or Android < 14) | Media permission declared | Not granted | Silent return, listener not started |
| Legacy strategy (any Android) | false (or Android < 14) | Media permission declared | Granted | Listener started; tries to return real file path (may not callback if path cannot be resolved) |

   **Code example (create or edit: `Assets/Plugins/Android/AndroidManifest.xml`)**:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.yourcompany.yourapp">

    <!-- Android 14+ (API 34) screenshot detection permission (may not be granted on some devices/ROMs) -->
    <uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE" />

    <!-- Android 13+ (API 33+) media image read permission (legacy strategy tries to resolve screenshot path) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Android 12 and below (API <= 32) external storage read permission (legacy strategy tries to resolve screenshot path) -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                     android:maxSdkVersion="32" />

    <!-- Keep your original application/activity content -->
</manifest>
```

4. **Editor Mode**:
   - In Editor mode, screenshot monitoring will not actually work, only logs will be recorded
   - Need to test actual functionality on real devices

## Roadmap

- [ ] Support returning full screenshot file path on Android 14+ platform
- [ ] Support returning full screenshot file path on iOS platform
- [ ] Optimize reliability of file path retrieval

## Dependencies

- Unity 2018.4 or higher
- Android API Level 21 or higher
- iOS 9.0 or higher

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Changelog

For detailed changelog, please refer to [CHANGELOG.md](CHANGELOG.md).

## Related Links

- [GitHub Repository](https://github.com/chinatragedy/Unity-ScreenShotObserver.git)
- [Issue Tracker](https://github.com/chinatragedy/Unity-ScreenShotObserver/issues)

