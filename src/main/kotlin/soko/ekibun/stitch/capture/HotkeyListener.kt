package soko.ekibun.stitch.capture

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener

class HotkeyListener(private val onCapture: () -> Unit) : NativeKeyListener {

    private var registered = false

    fun register() {
        try {
            GlobalScreen.registerNativeHook()
            GlobalScreen.addNativeKeyListener(this)
            registered = true
        } catch (e: Exception) {
            System.err.println("注册全局热键失败：${e.message}")
        }
    }

    fun unregister() {
        if (registered) {
            try {
                GlobalScreen.removeNativeKeyListener(this)
                GlobalScreen.unregisterNativeHook()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            registered = false
        }
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) {
        if (e.keyCode == NativeKeyEvent.VC_S &&
            (e.modifiers and NativeKeyEvent.CTRL_MASK) != 0 &&
            (e.modifiers and NativeKeyEvent.SHIFT_MASK) != 0) {
            onCapture()
        }
    }
}
