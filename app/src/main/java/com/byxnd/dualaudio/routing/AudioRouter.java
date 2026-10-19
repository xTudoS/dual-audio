package com.byxnd.dualaudio.routing;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.os.IBinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Routes STRATEGY_MEDIA to USB + Bluetooth A2DP at the same time via
 * IAudioService.setPreferredDevicesForStrategy (combined audio device routing).
 *
 * Must run in a process with MODIFY_AUDIO_ROUTING and no hidden-API restrictions,
 * i.e. as the shell uid (2000): via adb + app_process (tools/audio-dup) or via the
 * Shizuku UserService (AudioRoutingUserService). It fails in a regular app process.
 *
 * Uses only the public android.jar + reflection, so it compiles both in Gradle and with run.sh's javac.
 */
public final class AudioRouter {

    private static final int ROLE_OUTPUT = 2; // AudioDeviceAttributes.ROLE_OUTPUT
    private static final int GET_DEVICES_OUTPUTS = 2;
    private static final int FALLBACK_MEDIA_STRATEGY_ID = 0;

    private static final int[] USB_TYPES = {AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE};
    private static final int[] A2DP_TYPES = {AudioDeviceInfo.TYPE_BLUETOOTH_A2DP};

    private final Object audioService;
    private final int strategyId;

    public AudioRouter() throws Exception {
        IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class)
                .invoke(null, "audio");
        audioService = Class.forName("android.media.IAudioService$Stub")
                .getMethod("asInterface", IBinder.class)
                .invoke(null, binder);
        strategyId = findMediaStrategyId();
    }

    public int getStrategyId() {
        return strategyId;
    }

    /** true when a USB audio device and an A2DP device are both connected. */
    public boolean bothConnected() throws Exception {
        return findOutput(USB_TYPES) != null && findOutput(A2DP_TYPES) != null;
    }

    /** true when the strategy's preferred devices are already exactly USB + A2DP. */
    public boolean isApplied() throws Exception {
        List<?> preferred = getPreferred();
        if (preferred.size() != 2) return false;
        boolean usb = false, a2dp = false;
        for (Object d : preferred) {
            int type = (int) d.getClass().getMethod("getType").invoke(d);
            usb |= contains(USB_TYPES, type);
            a2dp |= contains(A2DP_TYPES, type);
        }
        return usb && a2dp;
    }

    /** Applies USB + A2DP. Returns the AudioService result code (0 = success). */
    public int apply() throws Exception {
        AudioDeviceInfo usb = findOutput(USB_TYPES);
        AudioDeviceInfo a2dp = findOutput(A2DP_TYPES);
        if (usb == null) throw new IllegalStateException("no USB audio device connected");
        if (a2dp == null) throw new IllegalStateException("no Bluetooth A2DP device connected");

        List<Object> devices = new ArrayList<>();
        devices.add(toAttributes(usb));
        devices.add(toAttributes(a2dp));
        return (int) invoke(new String[]{"setPreferredDevicesForStrategy"}, strategyId, devices);
    }

    /** Removes the preferred devices and restores normal routing. Returns the AudioService result code (0 = success). */
    public int clear() throws Exception {
        return (int) invoke(new String[]{"removePreferredDevicesForStrategy", "clearPreferredDevicesForStrategy"},
                strategyId);
    }

    public List<?> getPreferred() throws Exception {
        return (List<?>) invoke(new String[]{"getPreferredDevicesForStrategy"}, strategyId);
    }

    /** Human-readable strategy, connected outputs, and current preferred devices. */
    public String describe() throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("STRATEGY_MEDIA id = ").append(strategyId).append('\n');
        sb.append("connected outputs:\n");
        for (AudioDeviceInfo info : getOutputDevices()) {
            sb.append("  type=").append(info.getType())
                    .append(" name=").append(info.getProductName())
                    .append(" addr=").append(info.getAddress()).append('\n');
        }
        sb.append("current preferred devices: ").append(getPreferred());
        return sb.toString();
    }

    /** Finds the strategy that handles USAGE_MEDIA; falls back to id 0 as seen in dumpsys. */
    private static int findMediaStrategyId() {
        try {
            AudioAttributes media = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            List<?> strategies = (List<?>) Class.forName("android.media.AudioManager")
                    .getMethod("getAudioProductStrategies")
                    .invoke(null);
            for (Object s : strategies) {
                boolean supports = (boolean) s.getClass()
                        .getMethod("supportsAudioAttributes", AudioAttributes.class)
                        .invoke(s, media);
                if (supports) {
                    return (int) s.getClass().getMethod("getId").invoke(s);
                }
            }
        } catch (Throwable ignored) {
            // use the fallback
        }
        return FALLBACK_MEDIA_STRATEGY_ID;
    }

    private static AudioDeviceInfo[] getOutputDevices() throws Exception {
        Method m = Class.forName("android.media.AudioManager")
                .getDeclaredMethod("getDevicesStatic", int.class);
        m.setAccessible(true);
        return (AudioDeviceInfo[]) m.invoke(null, GET_DEVICES_OUTPUTS);
    }

    private static AudioDeviceInfo findOutput(int[] types) throws Exception {
        for (AudioDeviceInfo info : getOutputDevices()) {
            if (contains(types, info.getType())) return info;
        }
        return null;
    }

    /** AudioDeviceAttributes is a SystemApi, so it's created via reflection. */
    private static Object toAttributes(AudioDeviceInfo info) throws Exception {
        Constructor<?> c = Class.forName("android.media.AudioDeviceAttributes")
                .getConstructor(int.class, int.class, String.class);
        return c.newInstance(ROLE_OUTPUT, info.getType(), info.getAddress());
    }

    private static boolean contains(int[] values, int value) {
        for (int v : values) if (v == value) return true;
        return false;
    }

    /** Calls the first method matching one of the names and the arity (the AIDL changes across versions). */
    private Object invoke(String[] names, Object... args) throws Exception {
        for (String name : names) {
            for (Method m : audioService.getClass().getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                    return m.invoke(audioService, args);
                }
            }
        }
        throw new NoSuchMethodException(Arrays.toString(names) + " with " + args.length + " args");
    }
}
