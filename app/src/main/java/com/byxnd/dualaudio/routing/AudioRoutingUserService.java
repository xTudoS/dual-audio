package com.byxnd.dualaudio.routing;

import android.util.Log;

/**
 * Shizuku UserService: Shizuku instantiates this class in an app_process process with the shell uid,
 * where AudioRouter has MODIFY_AUDIO_ROUTING.
 *
 * Runs as a daemon: stays alive with the app closed and reapplies USB + A2DP whenever both are
 * connected and the preference was lost (disconnect, codec change, etc.).
 */
public class AudioRoutingUserService extends IAudioRoutingService.Stub {

    private static final String TAG = "AudioRoutingUserService";
    private static final long WATCH_INTERVAL_MS = 3000;

    private final AudioRouter router;
    private volatile boolean enabled;
    private Thread watcher;

    public AudioRoutingUserService() {
        try {
            router = new AudioRouter();
        } catch (Exception e) {
            throw new IllegalStateException("could not access AudioService", e);
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }

    @Override
    public synchronized String enable() {
        enabled = true;
        String result;
        try {
            result = "setPreferredDevicesForStrategy -> " + router.apply();
        } catch (Exception e) {
            // both devices aren't connected yet: the watcher applies it once they are
            result = "waiting for devices: " + rootMessage(e);
        }
        startWatcher();
        return result;
    }

    @Override
    public synchronized String disable() {
        enabled = false;
        if (watcher != null) {
            watcher.interrupt();
            watcher = null;
        }
        try {
            return "removePreferredDevicesForStrategy -> " + router.clear();
        } catch (Exception e) {
            throw new IllegalStateException(rootMessage(e));
        }
    }

    @Override
    public String status() {
        try {
            return router.describe() + "\nauto-reapply: " + (enabled ? "on" : "off");
        } catch (Exception e) {
            throw new IllegalStateException(rootMessage(e));
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private void startWatcher() {
        if (watcher != null) return;
        watcher = new Thread(() -> {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                try {
                    if (router.bothConnected() && !router.isApplied()) {
                        Log.i(TAG, "reapplying USB + A2DP -> " + router.apply());
                    }
                    Thread.sleep(WATCH_INTERVAL_MS);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "reapply failed", e);
                }
            }
        }, "audio-routing-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + ": " + t.getMessage();
    }
}
