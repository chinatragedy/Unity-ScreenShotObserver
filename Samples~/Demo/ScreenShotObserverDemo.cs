using UnityEngine;
using Unicorn.Herman.ScreenShotObserver;

public class ScreenShotObserverDemo : MonoBehaviour
{
    // Start is called before the first frame update
    void Start() {
        // useDetectScreenCapture: true=Android14+ 使用新回调(更准确但无路径)，false=Android14+ 也走 legacy(可能有路径但不100%准确)
        ScreenShotObserver.Instance.StartListenScreenShot(gameObject.name, "OnNativeScreenShot", true);
    }

    // Called by Native Code
    public void OnNativeScreenShot(string path) {
        Debug.Log("OnNativeScreenShot: " + path.ToString());
    }
}