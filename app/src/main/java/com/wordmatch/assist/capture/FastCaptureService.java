package com.wordmatch.assist.capture;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import com.wordmatch.assist.MainActivity;
import com.wordmatch.assist.accessibility.DuolingoNodeService;

/** Explicitly authorized, notification-backed continuous color confirmation. */
public final class FastCaptureService extends Service {
    private static volatile boolean running;
    private static volatile String fallbackReason = "未开启共享";
    private boolean destroying;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private int density;
    private long lastFrameAt;
    public static boolean isRunning() { return running; }
    public static String getFallbackReason() { return fallbackReason; }
    public static Intent startIntent(Context context, int code, Intent data) {
        return new Intent(context, FastCaptureService.class).putExtra("code", code).putExtra("data", data);
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (running) return START_NOT_STICKY;
        fallbackReason = "共享启动中";
        String channel = "word_match_fast";
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
                new NotificationChannel(channel, "自动模式画面加速", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, channel) : new Notification.Builder(this);
        Notification notification = builder.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("自动模式运行中").setContentText("仅本机检查词卡颜色；可在应用中停止")
                .setContentIntent(open).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(42, notification);
        if (intent == null) { fallbackReason = "共享未启动"; stopSelf(); return START_NOT_STICKY; }
        Intent data = Build.VERSION.SDK_INT >= 33 ? intent.getParcelableExtra("data", Intent.class)
                : intent.getParcelableExtra("data");
        if (data == null) { fallbackReason = "共享未启动"; stopSelf(); return START_NOT_STICKY; }
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
            density = metrics.densityDpi;
            projection = getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("code", 0), data);
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    if (destroying) return;
                    running = false;
                    fallbackReason = "共享已中断";
                    DuolingoNodeService.onFastCaptureStopped();
                    stopSelf();
                }
                @Override public void onCapturedContentResize(int width, int height) {
                    if (reader != null && (width != reader.getWidth() || height != reader.getHeight())) resize(width, height);
                }
            }, handler);
            resize(metrics.widthPixels, metrics.heightPixels);
            display = projection.createVirtualDisplay("WordMatchFastColors", metrics.widthPixels, metrics.heightPixels,
                    density, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, handler);
            running = true;
            fallbackReason = "等待共享画面";
            handler.post(watchEnabled);
        } catch (RuntimeException failure) {
            fallbackReason = "共享启动失败";
            Toast.makeText(this, "画面加速未启动，自动模式将继续使用节点确认", Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }
    private void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        ImageReader previous = reader;
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(source -> {
            try (Image frame = source.acquireLatestImage()) {
                if (frame == null || !running) return;
                long now = SystemClock.uptimeMillis();
                if (now - lastFrameAt < 16L) return;
                lastFrameAt = now;
                Image.Plane plane = frame.getPlanes()[0];
                DuolingoNodeService.onFastColorFrame(new RgbaPlanePixels(plane.getBuffer(), frame.getWidth(), frame.getHeight(),
                        plane.getRowStride(), plane.getPixelStride()), frame.getWidth(), frame.getHeight());
            } catch (IllegalStateException ignored) { /* Reader replaced or projection revoked. */ }
        }, handler);
        if (display != null) { display.resize(width, height, density); display.setSurface(reader.getSurface()); }
        if (previous != null) previous.close();
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(metrics);
        if (reader != null) resize(metrics.widthPixels, metrics.heightPixels);
    }
    private final Runnable watchEnabled = new Runnable() {
        @Override public void run() {
            if (!DuolingoNodeService.isAssistEnabled(FastCaptureService.this)
                    || !DuolingoNodeService.isAutoModeEnabled(FastCaptureService.this)) { stopSelf(); return; }
            handler.postDelayed(this, 500L);
        }
    };
    @Override public void onDestroy() {
        destroying = true;
        if (running) {
            fallbackReason = DuolingoNodeService.isAssistEnabled(this) && DuolingoNodeService.isAutoModeEnabled(this)
                    ? "共享已中断" : "共享已停止";
        }
        running = false;
        DuolingoNodeService.onFastCaptureStopped();
        handler.removeCallbacksAndMessages(null);
        if (display != null) { display.release(); display = null; }
        if (reader != null) { reader.close(); reader = null; }
        if (projection != null) { projection.stop(); projection = null; }
        stopForeground(true);
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
