package com.carriez.flutter_hbb

import android.view.InputDevice
import android.view.KeyEvent
import android.accessibilityservice.AccessibilityServiceInfo
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import java.lang.ref.WeakReference

object KeyboardCapture {
    private var activity = WeakReference<MainActivity>(null)
    var enabled = false
        private set
    private val pressed = linkedMapOf<Pair<Int, Int>, KeyEvent>()

    fun attach(target: MainActivity, messenger: BinaryMessenger) {
        setEnabled(false)
        activity = WeakReference(target)
        MethodChannel(messenger, "org.rustdesk/keyboard-capture")
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "setEnabled" -> {
                        setEnabled(call.arguments == true)
                        result.success(InputService.isOpen)
                    }
                    "isServiceOpen" -> result.success(InputService.isOpen)
                    else -> result.notImplemented()
                }
            }
    }

    fun setEnabled(value: Boolean) {
        if (!value) release()
        if (enabled == value) return
        enabled = value
        InputService.ctx?.let { service ->
            val info = service.serviceInfo ?: return@let
            val flag = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            info.flags = if (value) info.flags or flag else info.flags and flag.inv()
            service.serviceInfo = info
        }
    }

    fun detach(target: MainActivity) {
        if (activity.get() !== target) return
        setEnabled(false)
        activity.clear()
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        val target = activity.get() ?: return false
        if (!enabled || !target.hasWindowFocus() || !InputService.isOpen ||
            !event.isFromSource(InputDevice.SOURCE_KEYBOARD) || event.deviceId < 0 ||
            (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP)) return false

        val key = event.deviceId to (if (event.scanCode != 0) event.scanCode else -event.keyCode)
        // Xiaomi keyboards report the physical Escape key as Android Back.
        val forwarded = if (event.keyCode == KeyEvent.KEYCODE_BACK && event.scanCode == 1) {
            KeyEvent(event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_ESCAPE,
                event.repeatCount, event.metaState, event.deviceId, event.scanCode,
                event.flags, event.source)
        } else event
        if (event.action == KeyEvent.ACTION_DOWN) {
            pressed[key] = KeyEvent(forwarded)
        } else {
            if (pressed.remove(key) == null) return false
        }
        target.dispatchKeyEvent(forwarded)
        return true
    }

    fun release() {
        val target = activity.get()
        val keys = pressed.values.toList().asReversed()
        pressed.clear()
        if (target != null) {
            for (event in keys) {
                target.dispatchKeyEvent(KeyEvent.changeAction(event, KeyEvent.ACTION_UP))
            }
        }
    }
}
