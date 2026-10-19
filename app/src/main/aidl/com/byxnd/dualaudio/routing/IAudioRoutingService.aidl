package com.byxnd.dualaudio.routing;

// Contract between the app and AudioRoutingUserService, which runs as shell via Shizuku.
interface IAudioRoutingService {

    // Transaction code reserved by Shizuku to shut down the UserService.
    void destroy() = 16777114;

    // Enables USB + A2DP and the watcher that reapplies when the devices reconnect.
    String enable() = 1;

    // Stops the watcher and restores normal routing.
    String disable() = 2;

    // Strategy, connected outputs, current preferred devices, and whether the watcher is on.
    String status() = 3;

    boolean isEnabled() = 4;
}
