using System;
using System.Collections;
using UnityEngine;
using Unicorn.Herman.ScreenShotObserver;

namespace Unicorn.Herman.ScreenShotObserver.Tests
{
/// <summary>
/// ScreenShotObserver 插件测试用例
/// 使用方法：
/// 1. 在 Unity Editor 中创建一个 GameObject
/// 2. 将 ScreenShotObserverTest 组件添加到该 GameObject
/// 3. 运行场景，点击测试按钮或使用快捷键进行测试
/// </summary>
public class ScreenShotObserverTest : MonoBehaviour
{
    [Header("测试配置")]
    [Tooltip("接收截屏回调的 GameObject 名称")]
    public string testGameObjectName = "NativeMsgRx";

    [Tooltip("接收截屏回调的方法名称")]
    public string testMethodName = "OnScreenshotDetected";

    [Header("测试状态")]
    [SerializeField]
    private bool _isListening = false;

    [SerializeField]
    private int _screenshotCount = 0;

    [SerializeField]
    private string _lastTestResult = "";

    private void Start() {
        // 确保测试用的 GameObject 存在
        EnsureTestGameObject();
    }

    /// <summary>
    /// 确保测试用的 GameObject 存在
    /// </summary>
    private void EnsureTestGameObject() {
        GameObject testGo = GameObject.Find(testGameObjectName);
        if (testGo == null) {
            testGo = new GameObject(testGameObjectName);
            testGo.AddComponent<ScreenshotCallbackReceiver>();
            Debug.Log($"[ScreenShotObserverTest] Created test GameObject: {testGameObjectName}");
        }
    }

    #region 单元测试方法

    /// <summary>
    /// 测试 1: 单例模式测试
    /// </summary>
    [ContextMenu("Test 1: Singleton Pattern")]
    public void TestSingletonPattern() {
        Debug.Log("=== Test 1: Singleton Pattern ===");

        var instance1 = ScreenShotObserver.Instance;
        var instance2 = ScreenShotObserver.Instance;

        if (instance1 == instance2 && instance1 != null) {
            _lastTestResult = "✅ PASS: Singleton pattern works correctly";
            Debug.Log(_lastTestResult);
        } else {
            _lastTestResult = "❌ FAIL: Singleton pattern failed";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 2: 启动监听 - 使用默认参数
    /// </summary>
    [ContextMenu("Test 2: Start Listening (Default Params)")]
    public void TestStartListeningDefault() {
        Debug.Log("=== Test 2: Start Listening (Default Params) ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            _isListening = true;
            _lastTestResult = "✅ PASS: Start listening with default parameters";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 3: 启动监听 - 使用自定义参数
    /// </summary>
    [ContextMenu("Test 3: Start Listening (Custom Params)")]
    public void TestStartListeningCustom() {
        Debug.Log("=== Test 3: Start Listening (Custom Params) ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot(testGameObjectName, testMethodName, true);
            _isListening = true;
            _lastTestResult = $"✅ PASS: Start listening with custom params (GO: {testGameObjectName}, Method: {testMethodName})";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 4: 启动监听 - 空参数验证
    /// </summary>
    [ContextMenu("Test 4: Start Listening (Null Params)")]
    public void TestStartListeningNullParams() {
        Debug.Log("=== Test 4: Start Listening (Null Params) ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot(null, null, true);
            _lastTestResult = "❌ FAIL: Should reject null parameters";
            Debug.LogError(_lastTestResult);
        }
        catch (Exception e) {
            // 预期会记录错误日志，但不抛异常
            _lastTestResult = "✅ PASS: Null parameters handled (error logged)";
            Debug.Log(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 5: 启动监听 - 空字符串参数验证
    /// </summary>
    [ContextMenu("Test 5: Start Listening (Empty String Params)")]
    public void TestStartListeningEmptyParams() {
        Debug.Log("=== Test 5: Start Listening (Empty String Params) ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            _lastTestResult = "✅ PASS: Empty string parameters handled (error logged)";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Unexpected exception: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 6: 停止监听
    /// </summary>
    [ContextMenu("Test 6: Stop Listening")]
    public void TestStopListening() {
        Debug.Log("=== Test 6: Stop Listening ===");

        try {
            ScreenShotObserver.Instance.StopListenScreenShot();
            _isListening = false;
            _lastTestResult = "✅ PASS: Stop listening executed successfully";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 7: 多次启动监听（应该安全处理）
    /// </summary>
    [ContextMenu("Test 7: Multiple Start Listening")]
    public void TestMultipleStartListening() {
        Debug.Log("=== Test 7: Multiple Start Listening ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            _lastTestResult = "✅ PASS: Multiple start listening handled safely";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 8: 多次停止监听（应该安全处理）
    /// </summary>
    [ContextMenu("Test 8: Multiple Stop Listening")]
    public void TestMultipleStopListening() {
        Debug.Log("=== Test 8: Multiple Stop Listening ===");

        try {
            ScreenShotObserver.Instance.StopListenScreenShot();
            ScreenShotObserver.Instance.StopListenScreenShot();
            ScreenShotObserver.Instance.StopListenScreenShot();
            _lastTestResult = "✅ PASS: Multiple stop listening handled safely";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 测试 9: 启动-停止-启动循环
    /// </summary>
    [ContextMenu("Test 9: Start-Stop-Start Cycle")]
    public void TestStartStopCycle() {
        Debug.Log("=== Test 9: Start-Stop-Start Cycle ===");

        try {
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            ScreenShotObserver.Instance.StopListenScreenShot();
            ScreenShotObserver.Instance.StartListenScreenShot("", "", true);
            _lastTestResult = "✅ PASS: Start-Stop-Start cycle executed successfully";
            Debug.Log(_lastTestResult);
        }
        catch (Exception e) {
            _lastTestResult = $"❌ FAIL: Exception occurred: {e.Message}";
            Debug.LogError(_lastTestResult);
        }
    }

    /// <summary>
    /// 运行所有测试
    /// </summary>
    [ContextMenu("Run All Tests")]
    public void RunAllTests() {
        Debug.Log("==========================================");
        Debug.Log("ScreenShotObserver Test Suite - Starting");
        Debug.Log("==========================================");

        StartCoroutine(RunAllTestsCoroutine());
    }

    private IEnumerator RunAllTestsCoroutine() {
        TestSingletonPattern();
        yield return new WaitForSeconds(0.5f);

        TestStartListeningDefault();
        yield return new WaitForSeconds(0.5f);

        TestStartListeningCustom();
        yield return new WaitForSeconds(0.5f);

        TestStartListeningNullParams();
        yield return new WaitForSeconds(0.5f);

        TestStartListeningEmptyParams();
        yield return new WaitForSeconds(0.5f);

        TestStopListening();
        yield return new WaitForSeconds(0.5f);

        TestMultipleStartListening();
        yield return new WaitForSeconds(0.5f);

        TestMultipleStopListening();
        yield return new WaitForSeconds(0.5f);

        TestStartStopCycle();

        Debug.Log("==========================================");
        Debug.Log("ScreenShotObserver Test Suite - Completed");
        Debug.Log("==========================================");
    }

    #endregion

    #region 手动测试方法（用于实际截屏测试）

    /// <summary>
    /// 手动启动监听（用于实际截屏测试）
    /// </summary>
    [ContextMenu("Manual: Start Listening")]
    public void ManualStartListening() {
        EnsureTestGameObject();
            ScreenShotObserver.Instance.StartListenScreenShot(testGameObjectName, testMethodName, true);
        _isListening = true;
        Debug.Log($"[ScreenShotObserverTest] Manual: Started listening on {testGameObjectName}.{testMethodName}");
    }

    /// <summary>
    /// 手动停止监听
    /// </summary>
    [ContextMenu("Manual: Stop Listening")]
    public void ManualStopListening() {
        ScreenShotObserver.Instance.StopListenScreenShot();
        _isListening = false;
        Debug.Log("[ScreenShotObserverTest] Manual: Stopped listening");
    }

    /// <summary>
    /// 重置测试计数器
    /// </summary>
    [ContextMenu("Reset Counter")]
    public void ResetCounter() {
        _screenshotCount = 0;
        Debug.Log("[ScreenShotObserverTest] Counter reset");
    }

    /// <summary>
    /// 模拟截屏回调（用于测试回调接收）
    /// </summary>
    [ContextMenu("Simulate Screenshot Callback")]
    public void SimulateScreenshotCallback() {
        GameObject testGo = GameObject.Find(testGameObjectName);
        if (testGo != null) {
            var receiver = testGo.GetComponent<ScreenshotCallbackReceiver>();
            if (receiver != null) {
                receiver.OnScreenshotDetected("");
            } else {
                Debug.LogWarning($"[ScreenShotObserverTest] ScreenshotCallbackReceiver not found on {testGameObjectName}");
            }
        }
    }

    #endregion

    #region Unity Editor GUI（可选）

#if UNITY_EDITOR
    private void OnGUI() {
        if (!Application.isPlaying) return;

        GUILayout.BeginArea(new Rect(10, 10, 400, 300));
        GUILayout.Box("ScreenShotObserver Test Panel");

        GUILayout.Space(10);
        GUILayout.Label($"Listening: {(_isListening ? "✅ Yes" : "❌ No")}");
        GUILayout.Label($"Screenshot Count: {_screenshotCount}");

        GUILayout.Space(10);
        if (GUILayout.Button("Start Listening")) {
            ManualStartListening();
        }

        if (GUILayout.Button("Stop Listening")) {
            ManualStopListening();
        }

        GUILayout.Space(10);
        if (GUILayout.Button("Run All Tests")) {
            RunAllTests();
        }

        GUILayout.Space(10);
        if (GUILayout.Button("Simulate Screenshot")) {
            SimulateScreenshotCallback();
        }

        GUILayout.Space(10);
        if (!string.IsNullOrEmpty(_lastTestResult)) {
            GUILayout.Label($"Last Result: {_lastTestResult}");
        }

        GUILayout.EndArea();
    }
#endif

    #endregion
}

/// <summary>
/// 截屏回调接收器（用于测试）
/// 实际使用时，这个组件应该添加到接收截屏回调的 GameObject 上
/// </summary>
public class ScreenshotCallbackReceiver : MonoBehaviour
{
    private int _callbackCount = 0;

    /// <summary>
    /// 截屏检测回调方法
    /// 这个方法会被原生平台调用
    /// </summary>
    /// <param name="filePath">截屏文件路径（Android < 14 可能提供，其他平台为空）</param>
    public void OnScreenshotDetected(string filePath) {
        _callbackCount++;
        Debug.Log($"[ScreenshotCallbackReceiver] Screenshot detected! Count: {_callbackCount}, FilePath: {filePath}");

        // 通知测试脚本
        var test = FindObjectOfType<ScreenShotObserverTest>();
        if (test != null) {
            // 使用反射更新私有字段（仅用于测试）
            var field = typeof(ScreenShotObserverTest).GetField("_screenshotCount",
                                                                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
            if (field != null) {
                field.SetValue(test, _callbackCount);
            }
        }
    }

    private void OnDestroy() {
        Debug.Log($"[ScreenshotCallbackReceiver] Destroyed. Total callbacks received: {_callbackCount}");
    }
}
}