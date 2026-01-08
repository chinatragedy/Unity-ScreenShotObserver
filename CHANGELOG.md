# 更新日志

所有重要的变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [1.0.0] - 2026-01-07

### 新增
- 支持 Android 平台截屏监听
  - Android 14+ (API 34) 使用官方 `Activity.registerScreenCaptureCallback()` API
  - Android < 14 使用 `ContentObserver` 监听媒体库变化
- 支持 iOS 平台截屏监听
  - 使用 `UIApplicationUserDidTakeScreenshotNotification` 系统通知
- 提供统一的 C# API 接口
- 单例模式管理实例
- 完善的错误处理和参数验证
- 完整的测试用例（包含 9 个单元测试）
- 支持 Unity Package Manager 安装

### 重要说明
- ⚠️ **功能限制**：本插件仅能监听到用户执行截屏的行为，无法获取截图的文件路径
  - Android 14+ 和 iOS 系统只提供截屏事件通知，不提供文件路径
  - Android < 14 虽然通过监听媒体库检测截屏，但由于系统限制和隐私保护，无法可靠获取文件路径
  - 回调方法中的 `filePath` 参数在大多数情况下为空字符串，仅用于通知发生了截屏事件

### 技术细节
- Android 实现支持自动根据系统版本选择最佳实现方式
- iOS 实现通过 Unity 的 `UnitySendMessage` 回调到 C# 层
- 原生回调通过 Unity 主线程执行，确保线程安全
- Editor 模式下安全处理，不会实际监听截屏
- Android 权限配置变更：不再提供可自动合并的 `AndroidManifest.xml`，需要在项目主 `Assets/Plugins/Android/AndroidManifest.xml` 中手动添加权限
- Android 平台的实现思路参考了 [DoubleD0721/Screenshot](https://github.com/DoubleD0721/Screenshot) 项目

### 文档
- 完整的 README.md 使用文档
- API 文档和示例代码
- 测试用例说明
- GitHub 发布指南

[1.0.0]: https://github.com/chinatragedy/Unity-ScreenShotObserver/releases/tag/v1.0.0

