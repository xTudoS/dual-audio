package com.byxnd.dualaudio

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.byxnd.dualaudio.routing.AudioRoutingUserService
import com.byxnd.dualaudio.routing.IAudioRoutingService
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

/**
 * App version: turns USB + Bluetooth on/off through AudioRoutingUserService,
 * which Shizuku runs as the shell uid (a daemon that stays alive with the app closed).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var shizukuState: TextView
    private lateinit var status: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnEnable: Button
    private lateinit var btnDisable: Button
    private lateinit var btnRefresh: Button

    private val io = Executors.newSingleThreadExecutor()

    @Volatile
    private var service: IAudioRoutingService? = null

    private val userServiceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, AudioRoutingUserService::class.java.name)
    )
        .daemon(true)
        .processNameSuffix("routing")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
            if (binder == null || !binder.pingBinder()) return
            service = IAudioRoutingService.Stub.asInterface(binder)
            render()
            refreshStatus()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            render()
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { onShizukuChanged() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        service = null
        onShizukuChanged()
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { requestCode, result ->
        if (requestCode == REQUEST_CODE && result == PackageManager.PERMISSION_GRANTED) onShizukuChanged()
        else render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shizukuState = findViewById(R.id.shizukuState)
        status = findViewById(R.id.status)
        btnPermission = findViewById(R.id.btnPermission)
        btnEnable = findViewById(R.id.btnEnable)
        btnDisable = findViewById(R.id.btnDisable)
        btnRefresh = findViewById(R.id.btnRefresh)

        btnPermission.setOnClickListener { Shizuku.requestPermission(REQUEST_CODE) }
        btnEnable.setOnClickListener { call { it.enable() + "\n\n" + it.status() } }
        btnDisable.setOnClickListener { call { it.disable() + "\n\n" + it.status() } }
        btnRefresh.setOnClickListener { refreshStatus() }
        findViewById<Button>(R.id.btnLicenses).setOnClickListener { showLicenses() }

        Shizuku.addBinderReceivedListenerSticky(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        render()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        if (service != null) {
            // remove = false: the daemon keeps running and reapplying with the app closed
            runCatching { Shizuku.unbindUserService(userServiceArgs, connection, false) }
        }
        io.shutdown()
        super.onDestroy()
    }

    private fun onShizukuChanged() {
        if (shizukuRunning() && hasPermission() && service == null) {
            Shizuku.bindUserService(userServiceArgs, connection)
        }
        render()
    }

    private fun shizukuRunning() = Shizuku.pingBinder()

    private fun hasPermission() =
        shizukuRunning() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    private fun render() = runOnUiThread {
        val connected = service != null
        shizukuState.setText(
            when {
                !shizukuRunning() -> R.string.shizuku_not_running
                Shizuku.isPreV11() -> R.string.shizuku_too_old
                !hasPermission() -> R.string.shizuku_no_permission
                !connected -> R.string.shizuku_connecting
                else -> R.string.shizuku_ready
            }
        )
        btnPermission.isEnabled = shizukuRunning() && !hasPermission()
        btnEnable.isEnabled = connected
        btnDisable.isEnabled = connected
        btnRefresh.isEnabled = connected
    }

    private fun refreshStatus() = call { it.status() }

    /** Service calls go over binder; keep them off the main thread. */
    private fun call(block: (IAudioRoutingService) -> String) {
        val s = service ?: return
        io.execute {
            val text = runCatching { block(s) }.getOrElse { "error: ${it.message ?: it}" }
            runOnUiThread { status.text = text }
        }
    }

    /** Lists the license files the build copies from the repository into assets/legal/. */
    private fun showLicenses() {
        val titles = arrayOf(getString(R.string.license_app), getString(R.string.license_third_party))
        AlertDialog.Builder(this)
            .setTitle(R.string.licenses_title)
            .setItems(titles) { _, which -> showText(titles[which], LEGAL_ASSETS[which]) }
            .show()
    }

    private fun showText(title: String, assetPath: String) {
        val text = runCatching { assets.open(assetPath).bufferedReader().use { it.readText() } }
            .getOrElse { "error: ${it.message ?: it}" }
        val pad = (16 * resources.displayMetrics.density).toInt()
        val body = TextView(this).apply {
            this.text = text
            typeface = Typeface.MONOSPACE
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(ScrollView(this).apply { addView(body) })
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private companion object {
        const val REQUEST_CODE = 1
        val LEGAL_ASSETS = arrayOf("legal/LICENSE", "legal/THIRD_PARTY_NOTICES.md")
    }
}
