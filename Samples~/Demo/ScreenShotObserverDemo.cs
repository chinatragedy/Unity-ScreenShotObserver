using UnityEngine;
using Unicorn.Herman.ScreenShotObserver;

public class ScreenShotObserverDemo : MonoBehaviour
{
    // Start is called before the first frame update
    void Start() {
        // 1) 先检查权限（legacy 需要媒体权限）
        if (!ScreenShotObserver.Instance.HasLegacyMediaPermissionGranted()) {
            // 2) 未授权则主动请求（注意：请求后需要等用户授权，再次调用 StartLegacyListener）
            ScreenShotObserver.Instance.RequestLegacyMediaPermission();
            return;
        }
        // 3) 已授权：用 legacy 策略启动（useDetectScreenCapture=false）
        ScreenShotObserver.Instance.StartListenScreenShot(gameObject.name, "OnNativeScreenShot", false);
    }

    // Called by Native Code
    public void OnNativeScreenShot(string path) {
        Debug.Log("OnNativeScreenShot: " + path.ToString());
    }
}