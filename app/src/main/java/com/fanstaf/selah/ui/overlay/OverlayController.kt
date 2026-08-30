package com.fanstaf.selah.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.data.Verse
import com.fanstaf.selah.ui.theme.SelahTheme

/**
 * Owns the single verse overlay window. All calls must happen on the main thread (the service posts
 * there). The window is wrap-content and non-touch-modal, so the phone stays fully usable — taps
 * outside the card fall through, and an outside tap also dismisses the card.
 */
class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    private var currentView: ComposeView? = null
    private var currentOwner: OverlayLifecycleOwner? = null

    fun show(verse: Verse, mode: DisplayMode, durationSeconds: Int, fontScale: Float) {
        removeNow()

        val owner = OverlayLifecycleOwner().apply { onCreate(); onResume() }
        val readMs = durationSeconds.coerceIn(2, 30) * 1000L
        val revealDelayMs = minOf(2000L, readMs / 2)

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            alpha = 0f
            setContent {
                SelahTheme {
                    VerseOverlay(
                        reference = verse.reference,
                        text = verse.text,
                        translation = verse.translation,
                        mode = mode,
                        fontScale = fontScale,
                        revealDelayMs = revealDelayMs,
                        // Once revealed, give the full read time before auto-dismiss.
                        onRevealed = { scheduleDismiss(readMs) },
                        onClose = { dismiss() },
                    )
                }
            }
            // A tap outside the card (delivered because of FLAG_WATCH_OUTSIDE_TOUCH) dismisses it,
            // without consuming the touch from the app behind.
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismiss()
                }
                false
            }
        }

        currentView = view
        currentOwner = owner

        runCatching { windowManager.addView(view, layoutParams()) }
            .onSuccess { android.util.Log.d("SelahOverlay", "addView ok: ${verse.reference}") }
            .onFailure { android.util.Log.e("SelahOverlay", "addView failed", it); removeNow(); return }

        view.animate().alpha(1f).setDuration(180L).start()

        // READ shows for the full duration; RECALL adds the "can you say it?" beat up front.
        val initial = if (mode == DisplayMode.RECALL) revealDelayMs + readMs else readMs
        scheduleDismiss(initial)
    }

    fun dismiss() {
        handler.removeCallbacksAndMessages(null)
        val view = currentView ?: return
        val owner = currentOwner
        currentView = null
        currentOwner = null
        view.animate().alpha(0f).setDuration(150L).withEndAction {
            runCatching { windowManager.removeView(view) }
            owner?.onDestroy()
        }.start()
    }

    private fun scheduleDismiss(delayMs: Long) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ dismiss() }, delayMs)
    }

    /** Immediate, no-animation teardown (used before showing a new card and on service stop). */
    fun removeNow() {
        handler.removeCallbacksAndMessages(null)
        currentView?.let { runCatching { windowManager.removeView(it) } }
        currentOwner?.onDestroy()
        currentView = null
        currentOwner = null
    }

    private fun layoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (72 * context.resources.displayMetrics.density).toInt()
        }
    }
}
