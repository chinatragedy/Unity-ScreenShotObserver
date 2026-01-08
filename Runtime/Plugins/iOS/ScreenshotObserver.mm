#import "UnityInterface.h" // 引入 Unity 标准头文件
#import <Foundation/Foundation.h>

// 定义一个简单的 Objective-C 类来处理通知
@interface ScreenshotDelegate : NSObject
@property(nonatomic, strong) NSString *callbackGameObject;
@property(nonatomic, strong) NSString *callbackMethod;
+ (instancetype)sharedInstance;
- (void)startListening:(NSString *)goName method:(NSString *)methodName;
- (void)stopListening;
@end

@implementation ScreenshotDelegate

static ScreenshotDelegate *instance = nil;

+ (instancetype)sharedInstance {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[ScreenshotDelegate alloc] init];
    });
    return instance;
}

- (void)startListening:(NSString *)goName method:(NSString *)methodName {
    self.callbackGameObject = goName;
    self.callbackMethod = methodName;
    
    // 移除旧的监听，防止重复注册
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    
    // 注册系统截屏通知
    [[NSNotificationCenter defaultCenter]
     addObserver:self
     selector:@selector(handleScreenshotNotification:)
     name:UIApplicationUserDidTakeScreenshotNotification
     object:nil];
    
    NSLog(@"[iOS Screenshot] Start listening...");
}

- (void)stopListening {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    NSLog(@"[iOS Screenshot] Stop listening.");
}

// 接收到通知时的回调
- (void)handleScreenshotNotification:(NSNotification *)notification {
    NSLog(@"[iOS Screenshot] Detected!");
    
    if (self.callbackGameObject && self.callbackMethod) {
        // 发送消息回 Unity
        // UnitySendMessage 是线程安全的，它会将消息放入 Unity 主线程队列
        // iOS 仅能监听到截屏事件，无法获取文件路径；按约定向 Unity 回传标记字符串
        UnitySendMessage([self.callbackGameObject UTF8String],[self.callbackMethod UTF8String], "screenshot_detected");
    }
}

@end

// =================================================================
// C Linkage (供 C# DllImport 调用)
// =================================================================

extern "C" {
void _iOS_StartScreenshotListening(const char *goName, const char *methodName) {
    NSString *go = [NSString stringWithUTF8String:goName];
    NSString *method = [NSString stringWithUTF8String:methodName];
    [[ScreenshotDelegate sharedInstance] startListening:go method:method];
}

void _iOS_StopScreenshotListening() {
    [[ScreenshotDelegate sharedInstance] stopListening];
}
}
