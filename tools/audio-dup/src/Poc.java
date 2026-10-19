import com.byxnd.dualaudio.routing.AudioRouter;

/**
 * adb version: CLI over AudioRouter (same logic as the app), running as the shell uid:
 *   adb shell CLASSPATH=/data/local/tmp/audio-dup.dex app_process / Poc <cmd>
 *
 * Commands: status | set | clear
 */
public class Poc {

    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "status";
        AudioRouter router = new AudioRouter();

        switch (cmd) {
            case "status":
                break;
            case "set":
                System.out.println("setPreferredDevicesForStrategy -> " + router.apply() + " (0 = success)");
                break;
            case "clear":
                System.out.println("removePreferredDevicesForStrategy -> " + router.clear() + " (0 = success)");
                break;
            default:
                System.err.println("usage: Poc status|set|clear");
                System.exit(2);
        }
        System.out.println(router.describe());
        // app_process binder threads keep the JVM from exiting on its own
        System.exit(0);
    }
}
