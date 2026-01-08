using System;
using System.Runtime.InteropServices;
using UnityEngine;

namespace Unicorn.Herman.ScreenShotObserver
{
/// <summary>
/// 截屏监听器，统一封装 Android 和 iOS 平台的截屏监听功能
/// 这是一个独立的插件，不依赖其他 SDK Helper
/// </summary>
public class ScreenShotObserver
{
    private static ScreenShotObserver _instance;

    public static ScreenShotObserver Instance {
        get {
            if (_instance == null) {
                _instance = new ScreenShotObserver();
            }
            return _instance;
        }
    }

#if UNITY_IOS && !UNITY_EDITOR
    [DllImport("__Internal")]
    private static extern void _iOS_StartScreenshotListening(string gameObjectName, string methodName);

    [DllImport("__Internal")]
    private static extern void _iOS_StopScreenshotListening();
#endif

    /// <summary>
    /// 启动截屏行为监听（Android 14+ 可选择是否使用 DETECT_SCREEN_CAPTURE 新逻辑）
    /// 注意：必须先调用 <see cref="StopListenScreenShot"/> 停止监听后，才能再次启动监听。
    /// </summary>
    /// <param name="gameObjectName">接收截屏回调的 GameObject 名称</param>
    /// <param name="methodName">接收截屏回调的方法名称</param>
    /// <param name="useDetectScreenCapture">
    /// true：Android 14+ 使用 registerScreenCaptureCallback()（准确，但无法获取截图路径）；
    /// false：Android 14+ 也使用 legacy MediaStore(ContentObserver)（可能获取路径，但无法 100% 准确）
    /// </param>
    public void StartListenScreenShot(string gameObjectName, string methodName, bool useDetectScreenCapture = true) {
        if (string.IsNullOrEmpty(gameObjectName) || string.IsNullOrEmpty(methodName)) {
            Debug.LogError($"[ScreenShotObserver] StartListenScreenShot failed - IsNullOrEmpty: gameObjectName = {gameObjectName}, methodName = {methodName}");
            return;
        }
#if UNITY_EDITOR
        Debug.Log($"[ScreenShotObserver] StartListenScreenShot in editor - GameObject: {gameObjectName}, Method: {methodName}");
#elif UNITY_IOS
        try {
            _iOS_StartScreenshotListening(gameObjectName, methodName);
            Debug.Log($"[ScreenShotObserver] iOS screenshot listening started - GameObject: {gameObjectName}, Method: {methodName}");
        }
        catch (Exception e) {
            Debug.LogError($"[ScreenShotObserver] StartListenScreenShot iOS exception: {e.Message}");
        }
#elif UNITY_ANDROID
        try {
            AndroidJavaClass screenshotLifecycleObserver = new AndroidJavaClass("com.unicorn.tools.ScreenshotLifecycleObserver");
            screenshotLifecycleObserver.CallStatic("startListening", gameObjectName, methodName, useDetectScreenCapture);
            Debug.Log($"[ScreenShotObserver] Android screenshot listening started - GameObject: {gameObjectName}, Method: {methodName}");
        }
        catch (Exception e) {
            Debug.LogError($"[ScreenShotObserver] StartListenScreenShot Android exception: {e.Message}");
        }
#else
        Debug.LogWarning("[ScreenShotObserver] StartListenScreenShot not supported on this platform");
#endif
    }

    /// <summary>
    /// 停止截屏行为监听
    /// </summary>
    public void StopListenScreenShot() {
#if UNITY_EDITOR
        Debug.Log("[ScreenShotObserver] StopListenScreenShot in editor");
#elif UNITY_IOS
        try {
            _iOS_StopScreenshotListening();
            Debug.Log("[ScreenShotObserver] iOS screenshot listening stopped");
        }
        catch (Exception e) {
            Debug.LogError($"[ScreenShotObserver] StopListenScreenShot iOS exception: {e.Message}");
        }
#elif UNITY_ANDROID
        try {
            AndroidJavaClass screenshotLifecycleObserver = new AndroidJavaClass("com.unicorn.tools.ScreenshotLifecycleObserver");
            screenshotLifecycleObserver.CallStatic("stopListening");
            Debug.Log("[ScreenShotObserver] Android screenshot listening stopped");
        }
        catch (Exception e) {
            Debug.LogError($"[ScreenShotObserver] StopListenScreenShot Android exception: {e.Message}");
        }
#else
        Debug.LogWarning("[ScreenShotObserver] StopListenScreenShot not supported on this platform");
#endif
    }
}
}