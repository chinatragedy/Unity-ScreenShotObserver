# ScreenShotObserver Unity 插件

[![Unity](https://img.shields.io/badge/Unity-2018.4%2B-blue.svg)](https://unity3d.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

[中文](README.md) | [English](README_EN.md)

## 概述

`ScreenShotObserver` 是一个独立的 Unity 插件，用于监听 Android 和 iOS 平台的截屏行为。插件封装了原生平台的截屏监听功能，提供统一的 C# API 接口。

**关键词**: Unity 截屏、Unity 插件、截屏观察者、屏幕截图检测、Unity Package, Unity screenshot, Unity Screencapture

> ⚠️ **重要说明**：关于截图文件路径的获取：
> - **Android 13 及以前**：可以获取截图的完整文件路径
> - **Android 14+**：仅能监听到截屏事件，无法获取文件路径
> - **iOS**：仅能监听到截屏事件，无法获取文件路径

> 📚 **参考实现**：本插件的 Android 实现参考了 [DoubleD0721/Screenshot](https://github.com/DoubleD0721/Screenshot) 项目的实现思路，在此表示感谢。

## 功能特性

- ✅ **跨平台支持**：支持 Android 和 iOS 平台
- ✅ **独立插件**：不依赖其他 SDK Helper，可独立使用
- ✅ **单例模式**：使用单例模式管理实例
- ✅ **文件路径支持**：Android 13 及以前系统可获取截图完整路径
- ✅ **Unity Package Manager**：支持通过 UPM 安装和管理

## 安装

### 通过 Unity Package Manager (推荐)

1. 打开 Unity 项目
2. 在 Unity Editor 中，选择 `Window` > `Package Manager`
3. 点击左上角的 `+` 按钮，选择 `Add package from git URL...`
4. 输入以下 URL：
   ```
   https://github.com/chinatragedy/Unity-ScreenShotObserver.git
   ```
5. 点击 `Add` 按钮

### 手动安装

将 `Runtime` 文件夹复制到你的 Unity 项目的 `Assets` 目录下即可。

## 使用方法

```csharp
using Unicorn.Herman.ScreenShotObserver;

// 启动截屏监听
ScreenShotObserver.Instance.StartListenScreenShot("NativeMsgRx", "OnScreenshotDetected", true);

// 停止截屏监听
ScreenShotObserver.Instance.StopListenScreenShot();
```

### 接收截屏回调

创建一个 GameObject（例如：`NativeMsgRx`），添加脚本并实现回调方法：

```csharp
public class ScreenshotReceiver : MonoBehaviour
{
    public void OnScreenshotDetected(string filePath)
    {
        // Android 13 及以前：filePath 包含完整的截图文件路径
        // Android 14+ 和 iOS：filePath 为 "screenshot_detected"（仅事件标记，无真实路径）
        Debug.Log($"Screenshot detected! FilePath: {filePath ?? "N/A"}");
    }
}
```

## API 文档

### ScreenShotObserver.Instance

单例实例，用于访问插件功能。

### StartListenScreenShot(string gameObjectName, string methodName, bool useDetectScreenCapture)

启动截屏行为监听。

**参数：**
- `gameObjectName`：接收截屏回调的 GameObject 名称（默认：`"NativeMsgRx"`）
- `methodName`：接收截屏回调的方法名称（默认：`"OnScreenshotDetected"`）
- `useDetectScreenCapture`：
  - `true`：Android 14+ 使用 DETECT 策略（更准确，但无法获取真实截图路径，回调 `"screenshot_detected"`）
  - `false`：使用 legacy 策略（尽力获取真实路径，但无法 100% 准确）

### StopListenScreenShot()

停止截屏行为监听。

### HasLegacyMediaPermissionGranted()

检查 legacy 策略所需的媒体权限是否已授权（仅 Android 有效）。该方法只检查**运行时授权**状态，不会检查 Manifest 是否声明。

### RequestLegacyMediaPermission()

请求 legacy 策略所需的媒体权限（仅 Android 有效）。该方法**只发起权限请求**，不会自动启动监听，需要在用户授权后再调用 `StartListenScreenShot(..., false)`。

## 平台实现细节

### Android

- **Android 14+ (API 34)**：使用 `Activity.registerScreenCaptureCallback()` 官方 API（仅事件通知，无文件路径）
- **Android 13 及以前**：使用 `ContentObserver` 监听媒体库变化（可获取完整文件路径）
- 自动根据系统版本选择最佳实现方式
- **权限配置（需要手动添加）**：本插件**不再**提供可自动合并的 `AndroidManifest.xml`。请在你项目的 `Assets/Plugins/Android/AndroidManifest.xml` 中手动添加所需权限（见下方“权限要求”）。

### iOS

- 使用 `UIApplicationUserDidTakeScreenshotNotification` 系统通知（仅事件通知，无文件路径）
- 通过 Unity 的 `UnitySendMessage` 回调到 C# 层

## 注意事项

1. **文件路径获取**：
   - **Android 13 及以前**：可以获取截图的完整文件路径
   - **Android 14+**：仅能监听到截屏事件，无法获取文件路径（系统限制）
   - **iOS**：仅能监听到截屏事件，无法获取文件路径（系统限制）

2. **回调方法签名**：
   - 方法必须是 `public` 的
   - 方法签名：`void MethodName(string filePath)`
   - `filePath` 参数在 Android 13 及以前包含完整路径；Android 14+ / iOS 返回 `"screenshot_detected"`（仅事件标记，无真实路径）

3. **权限要求**：
   - **Android**：需要你在项目主 `AndroidManifest.xml` 手动声明权限（本插件不自动合并）
     - Android 14+：`android.permission.DETECT_SCREEN_CAPTURE`
     - Android 13+：`android.permission.READ_MEDIA_IMAGES`
     - Android 12 及以下：`android.permission.READ_EXTERNAL_STORAGE`（建议加 `android:maxSdkVersion="32"`）
   - **iOS**：无需特殊权限

   **能力边界说明（重要）**：
   - 如果你**不声明媒体权限**（`READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`），则 **legacy 策略不会启用**：
     - Android < 14：将无法通过 legacy 监听（等价于无法监听/无法拿路径）
     - Android 14+：只有在 `DETECT_SCREEN_CAPTURE` 可用时才能走新策略监听事件；否则也无法监听

   **行为对照表（基于当前实现：未声明权限/未授权时均静默不执行）**：

| 平台/策略 | useDetectScreenCapture | Manifest 是否声明权限 | 系统是否授予权限 | 结果 |
|---|---:|---|---|---|
| Android 14+ DETECT 策略 | true | 未声明 `DETECT_SCREEN_CAPTURE` | - | 静默返回，不启动监听 |
| Android 14+ DETECT 策略 | true | 已声明 `DETECT_SCREEN_CAPTURE` | 未授予/ROM 限制 | 静默返回，不启动监听 |
| Android 14+ DETECT 策略 | true | 已声明 `DETECT_SCREEN_CAPTURE` | 已授予 | 启动监听；回调参数为 `"screenshot_detected"` |
| legacy 策略（Android 任意版本） | false（或 Android < 14） | 未声明 `READ_MEDIA_IMAGES/READ_EXTERNAL_STORAGE` | - | 静默返回，不启动监听 |
| legacy 策略（Android 任意版本） | false（或 Android < 14） | 已声明媒体权限 | 未授权 | 静默返回，不启动监听（可使用 `RequestLegacyMediaPermission()` 主动申请） |
| legacy 策略（Android 任意版本） | false（或 Android < 14） | 已声明媒体权限 | 已授权 | 启动监听；尽力回传真实路径（取不到则可能不回调） |

   **推荐流程（legacy 策略 + 权限）**：

```csharp
using Unicorn.Herman.ScreenShotObserver;
using UnityEngine;

public class LegacyScreenshotStarter : MonoBehaviour
{
    public void StartLegacyListener()
    {
        // 1) 先检查权限（legacy 需要媒体权限）
        if (!ScreenShotObserver.Instance.HasLegacyMediaPermissionGranted())
        {
            // 2) 未授权则主动请求（注意：请求后需要等用户授权，再次调用 StartLegacyListener）
            ScreenShotObserver.Instance.RequestLegacyMediaPermission();
            return;
        }

        // 3) 已授权：用 legacy 策略启动（useDetectScreenCapture=false）
        ScreenShotObserver.Instance.StartListenScreenShot("NativeMsgRx", "OnScreenshotDetected", false);
    }
}
```

> ⚠️ **已知缺陷（legacy + 运行时授权）**：在部分机型/系统版本上，即使用户已授予媒体权限，legacy 策略也可能需要**下次启动 App**后才生效。

   **代码示例（新建或修改：`Assets/Plugins/Android/AndroidManifest.xml`）**：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          package="com.yourcompany.yourapp">

    <!-- Android 14+ (API 34) 截屏检测权限（部分设备/ROM 可能不授予三方应用） -->
    <uses-permission android:name="android.permission.DETECT_SCREEN_CAPTURE" />

    <!-- Android 13+ (API 33+) 读取媒体图片权限（legacy 策略用于尝试获取截图路径） -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <!-- Android 12 及以下（API <= 32）读取外部存储权限（legacy 策略用于尝试获取截图路径） -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
                     android:maxSdkVersion="32" />

    <!-- 你项目原本的 application/activity 等内容保持不变 -->
</manifest>
```

4. **Editor 模式**：
   - 在 Editor 模式下不会实际监听截屏，只会记录日志
   - 需要在真机上测试实际功能

## Roadmap

- [ ] 支持 Android 14+ 平台返回截图完整路径
- [ ] 支持 iOS 平台返回截图完整路径
- [ ] 优化文件路径获取的可靠性

## 依赖项

- Unity 2018.4 或更高版本
- Android API Level 21 或更高
- iOS 9.0 或更高

## 许可证

本项目采用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

## 更新日志

详细的更新日志请参阅 [CHANGELOG.md](CHANGELOG.md)。

## 相关链接

- [GitHub 仓库](https://github.com/chinatragedy/Unity-ScreenShotObserver.git)
- [问题反馈](https://github.com/chinatragedy/Unity-ScreenShotObserver/issues)

