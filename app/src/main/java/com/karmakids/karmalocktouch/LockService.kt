package com.karmakids.karmalocktouch

import android.animation.ValueAnimator
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout

class LockService : Service() {

    companion object {
        var isServiceRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingWidgetView: View
    private lateinit var lockView: View

    private lateinit var floatingWidgetParams: WindowManager.LayoutParams

    private var isMenuShowing = false
    private var isLockViewOnScreen = false

    private val menuDismissHandler = Handler(Looper.getMainLooper())
    private val menuDismissRunnable = Runnable { hideMenu() }

    private var tapCount = 0
    private val tapResetHandler = Handler(Looper.getMainLooper())
    private val tapResetRunnable = Runnable { tapCount = 0 }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        setupViews()
        setupTouchListeners()
        windowManager.addView(floatingWidgetView, floatingWidgetParams)
    }

    private fun setupViews() {
        floatingWidgetView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)
        floatingWidgetParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(5)
            y = 300
        }
        lockView = LayoutInflater.from(this).inflate(R.layout.lock_view, null)
    }

    private fun setupTouchListeners() {
        val mainIcon = floatingWidgetView.findViewById<ImageView>(R.id.main_floating_icon)
        val lockButton = floatingWidgetView.findViewById<ImageView>(R.id.lock_button_in_menu)
        val exitButton = floatingWidgetView.findViewById<ImageView>(R.id.exit_button_in_menu)

        // النقر على الأيقونة يفتح القائمة
        mainIcon.setOnClickListener {
            toggleMenuVisibility()
        }

        // أوامر أزرار القائمة
        lockButton.setOnClickListener {
            hideMenu()
            lockScreen()
        }
        exitButton.setOnClickListener {
            hideMenu()
            stopSelf()
        }

        setupLockViewTouchListener()
        setupWidgetDragListener()
    }

    private fun toggleMenuVisibility() {
        if (isMenuShowing) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    private fun showMenu() {
        if (isMenuShowing) return
        isMenuShowing = true
        // تشغيل أنيميشن التكبير
        animateIcon(dpToPx(48), dpToPx(56))

        val menuLayout = floatingWidgetView.findViewById<LinearLayout>(R.id.menu_buttons_layout)
        menuLayout.visibility = View.VISIBLE
        menuDismissHandler.postDelayed(menuDismissRunnable, 3000)
    }

    private fun hideMenu() {
        if (!isMenuShowing) return
        isMenuShowing = false
        // تشغيل أنيميشن التصغير
        animateIcon(dpToPx(56), dpToPx(48))

        val menuLayout = floatingWidgetView.findViewById<LinearLayout>(R.id.menu_buttons_layout)
        menuDismissHandler.removeCallbacks(menuDismissRunnable)
        menuLayout.visibility = View.GONE
    }

    // --- هذه هي دالة الأنيميشن التي تمت إضافتها ---
    private fun animateIcon(startSize: Int, endSize: Int) {
        val mainIcon = floatingWidgetView.findViewById<ImageView>(R.id.main_floating_icon)
        val animator = ValueAnimator.ofInt(startSize, endSize)
        animator.duration = 150
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            val newSize = animation.animatedValue as Int
            val params = mainIcon.layoutParams
            params.width = newSize
            params.height = newSize
            mainIcon.layoutParams = params
        }
        animator.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupWidgetDragListener() {
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        var isDragging = false

        floatingWidgetView.setOnTouchListener { _, event ->
            // إذا كانت القائمة ظاهرة، لا تسمح بالسحب
            if (isMenuShowing) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = floatingWidgetParams.x; initialY = floatingWidgetParams.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isDragging = false
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_MOVE -> {
                    // إذا تحرك الإصبع لمسافة كافية، اعتبرها سحباً
                    if (kotlin.math.abs(event.rawX - initialTouchX) > 10 || kotlin.math.abs(event.rawY - initialTouchY) > 10) {
                        isDragging = true
                    }
                    if (isDragging) {
                        floatingWidgetParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        floatingWidgetParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingWidgetView, floatingWidgetParams)
                    }
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_UP -> {
                    // إذا لم تكن عملية سحب، اعتبرها نقرة
                    if (!isDragging) {
                        floatingWidgetView.performClick()
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupLockViewTouchListener() {
        lockView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.pointerCount == 1) {
                    tapCount++
                    tapResetHandler.removeCallbacks(tapResetRunnable)
                    if (tapCount >= 6) {
                        unlockScreen()
                        tapCount = 0
                    } else {
                        tapResetHandler.postDelayed(tapResetRunnable, 210)
                    }
                } else {
                    tapCount = 0
                    tapResetHandler.removeCallbacks(tapResetRunnable)
                }
            }
            true
        }
    }

    private fun lockScreen() {
        try {
            isLockViewOnScreen = true
            floatingWidgetView.visibility = View.GONE
            windowManager.addView(lockView, createFullScreenParams())
            lockView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN)
        } catch (e: Exception) {}
    }

    private fun unlockScreen() {
        try {
            isLockViewOnScreen = false
            windowManager.removeView(lockView)
            floatingWidgetView.visibility = View.VISIBLE
        } catch (e: Exception) {}
    }

    private fun createFullScreenParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        menuDismissHandler.removeCallbacks(menuDismissRunnable)
        tapResetHandler.removeCallbacks(tapResetRunnable)
        if (isLockViewOnScreen) {
            try {
                windowManager.removeView(lockView)
            } catch (e: Exception) {}
        }
        if (floatingWidgetView.isAttachedToWindow) {
            try {
                windowManager.removeView(floatingWidgetView)
            } catch (e: Exception) {}
        }
    }
}