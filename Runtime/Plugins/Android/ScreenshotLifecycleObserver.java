package com.unicorn.tools;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.unity3d.player.UnityPlayer;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ScreenshotLifecycleObserver {
    private static final String TAG = "@ScreenshotObserver";
    private static final String SCREENSHOT_DETECTED = "screenshot_detected";

    // 监听状态
    private static boolean isListening = false;

    // Unity 通信相关
    private static String unityGoName;
    private static String unityMethodName;

    /**
     * 当前监听是否使用 Android 14+ 的 ScreenCaptureCallback 策略
     * - true: Android 14+ 使用 registerScreenCaptureCallback()，优点是准确，缺点是无法获取截图路径
     * - false: 即使 Android 14+ 也使用 legacy MediaStore(ContentObserver) 策略，优点是能拿到路径，缺点是无法 100% 准确
     */
    private static boolean usingAndroid14CallbackStrategy = false;

    // Android 14+ 专用变量
    private static Application.ActivityLifecycleCallbacks lifecycleCallbacks;
    private static Activity.ScreenCaptureCallback screenCaptureCallback;

    // Android < 14 专用变量
    private static ContentObserver contentObserver;
    private static long startListenTime = 0; // 开始监听的时间戳（秒）
    private static Point screenRealSize = null; // 屏幕真实尺寸

    // 已处理的文件路径集合，避免重复处理
    private static final Set<String> processedPaths = new HashSet<>();

    /**
     * 最大时间差（毫秒），文件创建时间与当前时间相差超过此值则忽略
     */
    private static final long MAX_COST_TIME = 10000; // 10秒

    /**
     * 截屏依据中的路径判断关键字
     */
    private static final String[] KEYWORDS = {
            "screenshot", "screen_shot", "screen-shot", "screen shot",
            "screencapture", "screen_capture", "screen-capture", "screen capture",
            "screencap", "screen_cap", "screen-cap", "screen cap"
    };


    /**
     * 开启监听（可选择 Android 14+ 是否使用 DETECT_SCREEN_CAPTURE 相关的新策略）
     *
     * @param goName                 Unity 接收消息的 GameObject 名字
     * @param methodName             Unity 接收消息的方法名
     * @param useDetectScreenCapture true：Android 14+ 使用 registerScreenCaptureCallback()；false：Android 14+ 也走 legacy 策略
     */
    public static void startListening(String goName, String methodName, boolean useDetectScreenCapture) {
        if (isListening) {
            Log.w(TAG, "startListening ignored: already listening. You must call stopListening() before calling startListening() again.");
            return;
        }

        unityGoName = (goName != null) ? goName : "NativeMsgRx";
        unityMethodName = methodName;

        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity == null) {
            isListening = false;
            return;
        }

        // 选择策略：
        // - Android 14+ 且 useDetectScreenCapture=true：优先尝试新策略（准确，但无路径）
        // - 其他情况：使用 legacy 策略（可能拿到路径，但判定不 100%）
        boolean shouldUseAndroid14Callback = Build.VERSION.SDK_INT >= 34 && useDetectScreenCapture;

        // 现在权限由接入方手动在项目 Manifest 中声明，因此需要考虑：
        // - 没声明权限（最终 Manifest 不包含该 permission）
        // - 声明了但没授权（仅媒体权限属于运行时授权）
        //
        // 对于 DETECT_SCREEN_CAPTURE：不是运行时权限，无法 requestPermissions() 申请；
        // 且部分设备/ROM 可能不会授予三方应用，调用 registerScreenCaptureCallback 会抛 SecurityException。
        if (shouldUseAndroid14Callback) {
            String detectPerm = "android.permission.DETECT_SCREEN_CAPTURE";
            if (!isPermissionDeclared(currentActivity, detectPerm)) {
                // 未声明权限：静默不执行任何逻辑
                return;
            } else if (!hasPermissionGranted(currentActivity, detectPerm)) {
                // 未授予权限：静默不执行任何逻辑
                return;
            }
        }

        // legacy 策略需要媒体读取权限（用于访问 MediaStore 获取截图路径）
        if (!shouldUseAndroid14Callback) {
            // 未声明媒体权限：静默不执行任何逻辑
            if (!isAnyMediaPermissionDeclared(currentActivity)) return;
            // 未授权媒体权限：静默不执行任何逻辑（不自动申请）
            if (!hasMediaPermission(currentActivity)) return;
        }

        isListening = true;
        usingAndroid14CallbackStrategy = shouldUseAndroid14Callback;

        if (shouldUseAndroid14Callback) {
            startAndroid14Strategy(currentActivity.getApplication());
            registerCallbackForActivity(currentActivity);
        } else {
            startLegacyStrategy(currentActivity);
        }
    }

    /**
     * 检查是否有媒体权限
     */
    public static boolean hasMediaPermission(Context context) {
        if (context == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要 READ_MEDIA_IMAGES
            return context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-12 需要 READ_EXTERNAL_STORAGE
            return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 5.x 及以下，权限在安装时授予
            return true;
        }
    }

    /**
     * 请求媒体权限
     */
    public static void requestMediaPermission(Activity activity) {
        if (activity == null) return;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Android 5.x 及以下不需要运行时权限
            return;
        }

        // 如果未在 Manifest 声明权限，requestPermissions 也不会生效；这里保持静默返回
        if (!isAnyMediaPermissionDeclared(activity)) return;

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        activity.requestPermissions(permissions, 1001);
    }

    /**
     * 处理权限请求结果（由 Unity 调用）
     */
    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1001 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Activity currentActivity = UnityPlayer.currentActivity;
            if (currentActivity != null) {
                isListening = true;
                // 权限请求只对 legacy 策略有意义；这里根据当前策略标记恢复监听
                if (usingAndroid14CallbackStrategy && Build.VERSION.SDK_INT >= 34) {
                    startAndroid14Strategy(currentActivity.getApplication());
                    registerCallbackForActivity(currentActivity);
                } else {
                    startLegacyStrategy(currentActivity);
                }
            }
        } else {
            isListening = false;
        }
    }

    /**
     * 停止监听
     */
    public static void stopListening() {
        isListening = false;
        Activity currentActivity = UnityPlayer.currentActivity;

        // 为了同时兼容 Android 14+ 新策略 / Android 14+ 强制 legacy / Android < 14 legacy，
        // 这里统一做一次“尽力清理”，避免策略切换后残留监听
        if (currentActivity != null) {
            // Android 14+ 新策略清理
            if (lifecycleCallbacks != null) {
                try {
                    currentActivity.getApplication().unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
                } catch (Exception ignored) {
                }
                lifecycleCallbacks = null;
            }
            unregisterCallbackForActivity(currentActivity);

            // legacy 策略清理
            if (contentObserver != null) {
                try {
                    currentActivity.getContentResolver().unregisterContentObserver(contentObserver);
                } catch (Exception ignored) {
                }
                contentObserver = null;
            }
        } else {
            lifecycleCallbacks = null;
            contentObserver = null;
        }

        usingAndroid14CallbackStrategy = false;
        startListenTime = 0;
        screenRealSize = null;
        synchronized (processedPaths) {
            processedPaths.clear();
        }
    }

    // ==========================================
    // 策略 A: Android 14+ (API 34) 官方 API + 生命周期注入
    // ==========================================

    private static void startAndroid14Strategy(Application app) {
        if (lifecycleCallbacks != null) return;

        // 定义截图回调逻辑
        if (screenCaptureCallback == null) {
            // Android 14+ 仅能监听到截屏事件，无法获取文件路径；按约定向 Unity 回传标记字符串
            screenCaptureCallback = () -> notifyUnity(SCREENSHOT_DETECTED);
        }

        // 定义生命周期回调
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                registerCallbackForActivity(activity);
            }

            @Override
            public void onActivityPaused(Activity activity) {
                unregisterCallbackForActivity(activity);
            }

            // 其他生命周期无需处理
            public void onActivityCreated(Activity a, Bundle b) {
            }

            public void onActivityStarted(Activity a) {
            }

            public void onActivityStopped(Activity a) {
            }

            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            public void onActivityDestroyed(Activity a) {
            }
        };

        app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    private static void registerCallbackForActivity(Activity activity) {
        if (Build.VERSION.SDK_INT >= 34 && activity != null && !activity.isFinishing()
                && screenCaptureCallback != null) {
            try {
                activity.registerScreenCaptureCallback(activity.getMainExecutor(), screenCaptureCallback);
            } catch (SecurityException se) {
                // 权限问题：静默退出，不执行任何逻辑（不降级、不报错日志）
                try {
                    unregisterCallbackForActivity(activity);
                    if (lifecycleCallbacks != null) {
                        activity.getApplication().unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
                        lifecycleCallbacks = null;
                    }
                } catch (Exception ignored) {
                }

                usingAndroid14CallbackStrategy = false;
                isListening = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to register callback for " + activity.getClass().getSimpleName(), e);
            }
        }
    }

    private static void unregisterCallbackForActivity(Activity activity) {
        if (Build.VERSION.SDK_INT >= 34 && activity != null && screenCaptureCallback != null) {
            try {
                activity.unregisterScreenCaptureCallback(screenCaptureCallback);
            } catch (Exception e) {
                // 忽略异常
            }
        }
    }

    // ==========================================
    // 策略 B: Android < 14 旧版 ContentObserver
    // ==========================================

    private static void startLegacyStrategy(Context context) {
        if (contentObserver == null) {
            // 记录开始监听的时间（秒）
            startListenTime = System.currentTimeMillis() / 1000;

            // 获取屏幕尺寸
            if (screenRealSize == null) {
                screenRealSize = getRealScreenSize(context);
            }

            final Context finalContext = context;
            final Handler handler = new Handler(Looper.getMainLooper());
            contentObserver = new ContentObserver(handler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    // 延迟处理，因为文件可能还在写入中
                    handler.postDelayed(() -> handleMediaContentChange(uri, finalContext), 300);
                }
            };

            try {
                context.getContentResolver().registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        Build.VERSION.SDK_INT > Build.VERSION_CODES.P,
                        contentObserver);
            } catch (Exception e) {
                Log.e(TAG, "Failed to register ContentObserver", e);
            }
        }
    }

    /**
     * 处理媒体内容变化
     */
    private static void handleMediaContentChange(Uri contentUri, Context context) {
        if (context == null) return;

        // 确保屏幕尺寸已获取
        if (screenRealSize == null) {
            screenRealSize = getRealScreenSize(context);
        }

        Cursor cursor = null;
        try {
            // 先尝试直接查询收到的具体 URI
            cursor = querySpecificUri(contentUri, context);

            // 如果查询失败或为空，尝试查询整个 MediaStore 并获取最新文件
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                cursor = getContentResolverCursor(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, context, 1);
                if (cursor == null || !cursor.moveToFirst()) {
                    return;
                }
            }

            // 获取列索引
            int dataIndex = cursor.getColumnIndex(Images.Media.DATA);
            int dateAddedIndex = cursor.getColumnIndex(Images.Media.DATE_ADDED);

            // 获取文件路径
            String filePath = null;
            if (dataIndex >= 0) {
                filePath = cursor.getString(dataIndex);
            }

            // 路径维度判断
            if (!isFilePathLegal(filePath)) {
                return;
            }

            // 检查是否已处理过
            synchronized (processedPaths) {
                if (processedPaths.contains(filePath)) {
                    return;
                }
            }

            // 时间维度判断
            Long dateAdded = null;
            if (dateAddedIndex >= 0) {
                dateAdded = cursor.getLong(dateAddedIndex) * 1000; // 转换为毫秒
            }
            if (!isFileCreationTimeLegal(dateAdded, startListenTime)) {
                return;
            }

            // 尺寸维度判断（仅在 Android 10+ 可用）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int widthIndex = cursor.getColumnIndex(Images.Media.WIDTH);
                int heightIndex = cursor.getColumnIndex(Images.Media.HEIGHT);

                Integer width = null;
                Integer height = null;
                if (widthIndex >= 0) {
                    width = cursor.getInt(widthIndex);
                }
                if (heightIndex >= 0) {
                    height = cursor.getInt(heightIndex);
                }
                if (!isFileSizeLegal(width, height)) {
                    return;
                }
            }

            // 路径判断通过，处理截图
            synchronized (processedPaths) {
                processedPaths.add(filePath);
            }
            notifyUnity(filePath);

        } catch (Exception e) {
            Log.e(TAG, "Error handling media content change", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * 查询具体的 URI
     */
    private static Cursor querySpecificUri(Uri uri, Context context) {
        try {
            String[] projection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                projection = new String[]{
                        Images.Media.DATA,
                        Images.Media.DATE_ADDED,
                        Images.Media.WIDTH,
                        Images.Media.HEIGHT
                };
            } else {
                projection = new String[]{
                        Images.Media.DATA,
                        Images.Media.DATE_ADDED
                };
            }
            return context.getContentResolver().query(uri, projection, null, null, null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 ContentResolver 查询游标
     */
    private static Cursor getContentResolverCursor(Uri contentUri, Context context, int maxCount) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 Bundle 查询
                Bundle bundle = new Bundle();
                bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        new String[]{Images.Media.DATE_MODIFIED});
                bundle.putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING);
                bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, maxCount);

                String[] projection = {
                        Images.Media.DATA,
                        Images.Media.DATE_ADDED,
                        Images.Media.WIDTH,
                        Images.Media.HEIGHT
                };

                return context.getContentResolver().query(contentUri, projection, bundle, null);
            } else {
                // Android 9 及以下使用传统查询
                String[] projection = {
                        Images.Media.DATA,
                        Images.Media.DATE_ADDED
                };

                String sortOrder = Images.Media.DATE_MODIFIED + " desc limit " + maxCount;
                return context.getContentResolver().query(contentUri, projection, null, null, sortOrder);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 路径维度判断：检查路径是否包含截图关键字
     */
    private static boolean isFilePathLegal(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }

        // 过滤临时文件（.pending- 开头的文件）
        if (filePath.contains("/.pending-")) {
            return false;
        }

        String lowerPath = filePath.toLowerCase(Locale.getDefault());
        for (String keyword : KEYWORDS) {
            if (lowerPath.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 时间维度判断：文件创建时间晚于开始监听时间，且与当前时间相差小于10秒
     */
    private static boolean isFileCreationTimeLegal(Long dateAdded, Long startListenTime) {
        if (dateAdded == null || startListenTime == null) {
            return false;
        }
        long startTimeMs = startListenTime * 1000;
        if (dateAdded < startTimeMs) {
            return false;
        }
        long currentTime = System.currentTimeMillis();
        return (currentTime - dateAdded) <= MAX_COST_TIME;
    }

    /**
     * 尺寸维度判断：图片尺寸与屏幕尺寸匹配
     */
    private static boolean isFileSizeLegal(Integer width, Integer height) {
        if (screenRealSize == null || width == null || height == null) {
            return false;
        }
        return (width <= screenRealSize.x && height <= screenRealSize.y) ||
                (height <= screenRealSize.x && width <= screenRealSize.y);
    }

    /**
     * 获取屏幕真实尺寸
     */
    private static Point getRealScreenSize(Context context) {
        Point screenSize = new Point();
        try {
            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (windowManager != null) {
                Display display = windowManager.getDefaultDisplay();
                display.getRealSize(screenSize);
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return screenSize;
    }

    // ==========================================
    // 通用方法
    // ==========================================

    /**
     * 是否在 Manifest 中声明了某个权限（用于“手动添加权限”的场景兜底提示）
     */
    private static boolean isPermissionDeclared(Context context, String permission) {
        if (context == null || TextUtils.isEmpty(permission)) return false;
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) return false;
            String pkg = context.getPackageName();
            if (TextUtils.isEmpty(pkg)) return false;
            String[] requested = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS).requestedPermissions;
            if (requested == null) return false;
            for (String p : requested) {
                if (permission.equals(p)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否已被系统授予权限（注意：DETECT_SCREEN_CAPTURE 不是运行时权限，但这里可用于判断“是否实际授予”）
     */
    private static boolean hasPermissionGranted(Context context, String permission) {
        if (context == null || TextUtils.isEmpty(permission)) return false;
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return true;
            }
            return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * legacy 策略所需的媒体权限是否至少声明了一个
     */
    private static boolean isAnyMediaPermissionDeclared(Context context) {
        if (context == null) return false;
        return isPermissionDeclared(context, Manifest.permission.READ_MEDIA_IMAGES)
                || isPermissionDeclared(context, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private static void notifyUnity(String filePath) {
        Log.d(TAG, "notifyUnity: " + filePath);
        if (filePath == null) return;
        if (unityGoName != null && unityMethodName != null) {
            UnityPlayer.UnitySendMessage(unityGoName, unityMethodName, filePath);
        }
    }
}
