package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.util.BengaliUtils
import com.example.util.EntryValidationResult
import com.example.util.EntryValidator

class OverlayManager(private val appContext: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private var entryContainer: View? = null
    private var entryWindowManager: WindowManager? = null
    private var currentEntryPackage: String? = null

    private var bannerContainer: View? = null
    private var bannerWindowManager: WindowManager? = null
    private var dismissBannerRunnable: Runnable? = null

    fun isEntryPopupShowing(): Boolean = entryContainer != null

    private fun resolveOverlayContext(): Context {
        val base: Context = AppMonitorAccessibilityService.instance ?: appContext
        val windowCtx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val dm = base.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val displayCtx = if (display != null) base.createDisplayContext(display) else base
            displayCtx.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        } else {
            base
        }
        return ContextThemeWrapper(windowCtx, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
    }

    private fun dp(context: Context, value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun loadBengaliFont(context: Context): Typeface? {
        return runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
        }.getOrNull()
    }

    fun showEntryPopup(
        packageName: String,
        appName: String,
        onConfirm: (purpose: String, minutes: Int) -> Unit,
        onCancelToHome: () -> Unit
    ) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(appContext)) {
                onCancelToHome()
                return@post
            }
            if (entryContainer != null && currentEntryPackage == packageName) {
                return@post
            }
            dismissEntryPopupImmediate()
            currentEntryPackage = packageName

            val ctx = resolveOverlayContext()
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bengaliTypeface = loadBengaliFont(ctx)

            val root = object : FrameLayout(ctx) {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        dismissEntryPopupImmediate()
                        onCancelToHome()
                        return true
                    }
                    return super.dispatchKeyEvent(event)
                }
            }.apply {
                setBackgroundColor(Color.argb(190, 0, 0, 0))
                isClickable = true
            }

            val scrollView = ScrollView(ctx).apply {
                isFillViewport = true
            }

            val outerCenterBox = FrameLayout(ctx).apply {
                setPadding(dp(ctx, 20), dp(ctx, 28), dp(ctx, 20), dp(ctx, 28))
            }

            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(ctx, 22), dp(ctx, 22), dp(ctx, 22), dp(ctx, 22))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = dp(ctx, 24).toFloat()
                }
                elevation = dp(ctx, 12).toFloat()
                isClickable = true
            }

            val titleView = TextView(ctx).apply {
                text = "আপনি কেন $appName খুলছেন?"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setTextColor(Color.parseColor("#161D1C"))
                setTypeface(bengaliTypeface, Typeface.BOLD)
            }
            card.addView(titleView)

            val subtitleView = TextView(ctx).apply {
                text = "শুরু করার আগে কাজের উদ্দেশ্য ও সময় লিখুন"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#4A5755"))
                typeface = bengaliTypeface
                setPadding(0, dp(ctx, 4), 0, dp(ctx, 16))
            }
            card.addView(subtitleView)

            val purposeLabel = TextView(ctx).apply {
                text = "কাজের উদ্দেশ্য লিখুন *"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#0C5753"))
                setTypeface(bengaliTypeface, Typeface.BOLD)
                setPadding(0, 0, 0, dp(ctx, 6))
            }
            card.addView(purposeLabel)

            val inputBg = {
                GradientDrawable().apply {
                    setColor(Color.parseColor("#F5F9F8"))
                    setStroke(dp(ctx, 1), Color.parseColor("#9DB3B0"))
                    cornerRadius = dp(ctx, 12).toFloat()
                }
            }

            val purposeInput = EditText(ctx).apply {
                hint = "যেমন: মেসেজ দেখা"
                setTextColor(Color.parseColor("#161D1C"))
                setHintTextColor(Color.parseColor("#7A8A87"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = bengaliTypeface
                background = inputBg()
                setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
                minLines = 1
                maxLines = 2
                imeOptions = EditorInfo.IME_ACTION_NEXT
                isFocusable = true
                isFocusableInTouchMode = true
            }
            card.addView(
                purposeInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val purposeErrorView = TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#BA1A1A"))
                typeface = bengaliTypeface
                visibility = View.GONE
                setPadding(dp(ctx, 4), dp(ctx, 4), 0, 0)
            }
            card.addView(purposeErrorView)

            val minutesLabel = TextView(ctx).apply {
                text = "কত মিনিট থাকতে চান? (১ থেকে ১৮০) *"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(Color.parseColor("#0C5753"))
                setTypeface(bengaliTypeface, Typeface.BOLD)
                setPadding(0, dp(ctx, 14), 0, dp(ctx, 6))
            }
            card.addView(minutesLabel)

            val minutesInput = EditText(ctx).apply {
                hint = "যেমন: 10"
                setTextColor(Color.parseColor("#161D1C"))
                setHintTextColor(Color.parseColor("#7A8A87"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = bengaliTypeface
                background = inputBg()
                setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(3))
                imeOptions = EditorInfo.IME_ACTION_DONE
                isFocusable = true
                isFocusableInTouchMode = true
            }
            card.addView(
                minutesInput,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val minutesErrorView = TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(Color.parseColor("#BA1A1A"))
                typeface = bengaliTypeface
                visibility = View.GONE
                setPadding(dp(ctx, 4), dp(ctx, 4), 0, 0)
            }
            card.addView(minutesErrorView)

            // Quick preset minute buttons for easy tapping
            val presetsRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(ctx, 12), 0, dp(ctx, 6))
            }
            listOf(5, 10, 15, 30).forEachIndexed { idx, preset ->
                val chipBtn = Button(ctx).apply {
                    text = "${BengaliUtils.formatInt(preset)} মি."
                    isAllCaps = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(Color.parseColor("#0C5753"))
                    typeface = bengaliTypeface
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#E3F3F1"))
                        cornerRadius = dp(ctx, 10).toFloat()
                    }
                    minHeight = dp(ctx, 40)
                    setPadding(dp(ctx, 8), dp(ctx, 6), dp(ctx, 8), dp(ctx, 6))
                    setOnClickListener {
                        minutesInput.setText(preset.toString())
                        minutesInput.setSelection(minutesInput.text.length)
                    }
                }
                val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (idx > 0) marginStart = dp(ctx, 6)
                }
                presetsRow.addView(chipBtn, lp)
            }
            card.addView(presetsRow)

            // Buttons row: Cancel and OK
            val buttonsRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(ctx, 16), 0, 0)
            }

            val cancelBtn = Button(ctx).apply {
                text = "বাতিল"
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(Color.parseColor("#0C5753"))
                setTypeface(bengaliTypeface, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    setStroke(dp(ctx, 1), Color.parseColor("#0C5753"))
                    cornerRadius = dp(ctx, 14).toFloat()
                }
                minHeight = dp(ctx, 50)
                setOnClickListener {
                    dismissEntryPopupImmediate()
                    onCancelToHome()
                }
            }

            val okBtn = Button(ctx).apply {
                text = "ওকে"
                isAllCaps = false
                isEnabled = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTextColor(Color.WHITE)
                setTypeface(bengaliTypeface, Typeface.BOLD)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#9EB5B3"))
                    cornerRadius = dp(ctx, 14).toFloat()
                }
                minHeight = dp(ctx, 50)
            }

            var purposeEdited = false
            var minutesEdited = false

            fun refreshValidation() {
                val res = EntryValidator.validate(
                    purposeInput.text?.toString().orEmpty(),
                    minutesInput.text?.toString().orEmpty()
                )
                when (res) {
                    is EntryValidationResult.Valid -> {
                        purposeErrorView.visibility = View.GONE
                        minutesErrorView.visibility = View.GONE
                        okBtn.isEnabled = true
                        (okBtn.background as? GradientDrawable)?.setColor(Color.parseColor("#0C5753"))
                    }
                    is EntryValidationResult.Invalid -> {
                        if (purposeEdited && res.purposeError != null) {
                            purposeErrorView.text = res.purposeError
                            purposeErrorView.visibility = View.VISIBLE
                        } else {
                            purposeErrorView.visibility = View.GONE
                        }
                        if (minutesEdited && res.minutesError != null) {
                            minutesErrorView.text = res.minutesError
                            minutesErrorView.visibility = View.VISIBLE
                        } else {
                            minutesErrorView.visibility = View.GONE
                        }
                        okBtn.isEnabled = false
                        (okBtn.background as? GradientDrawable)?.setColor(Color.parseColor("#9EB5B3"))
                    }
                }
            }

            purposeInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    purposeEdited = true
                    refreshValidation()
                }
            })

            minutesInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    minutesEdited = true
                    refreshValidation()
                }
            })

            minutesInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    okBtn.performClick()
                    true
                } else {
                    false
                }
            }

            okBtn.setOnClickListener {
                val res = EntryValidator.validate(
                    purposeInput.text?.toString().orEmpty(),
                    minutesInput.text?.toString().orEmpty()
                )
                if (res is EntryValidationResult.Valid) {
                    dismissEntryPopupImmediate()
                    onConfirm(res.purpose, res.minutes)
                } else {
                    purposeEdited = true
                    minutesEdited = true
                    refreshValidation()
                }
            }

            val cancelLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(ctx, 8)
            }
            val okLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(ctx, 8)
            }
            buttonsRow.addView(cancelBtn, cancelLp)
            buttonsRow.addView(okBtn, okLp)
            card.addView(buttonsRow)

            val cardLp = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            outerCenterBox.addView(card, cardLp)
            scrollView.addView(
                outerCenterBox,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            root.addView(
                scrollView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            @Suppress("DEPRECATION")
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                dimAmount = 0.72f
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            }

            runCatching {
                wm.addView(root, params)
                entryContainer = root
                entryWindowManager = wm

                purposeInput.post {
                    purposeInput.requestFocus()
                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showSoftInput(purposeInput, InputMethodManager.SHOW_IMPLICIT)
                }
            }.onFailure {
                currentEntryPackage = null
                onCancelToHome()
            }
        }
    }

    fun dismissEntryPopup() {
        mainHandler.post { dismissEntryPopupImmediate() }
    }

    private fun dismissEntryPopupImmediate() {
        val container = entryContainer
        val wm = entryWindowManager
        entryContainer = null
        entryWindowManager = null
        currentEntryPackage = null
        if (container != null && wm != null) {
            runCatching {
                val imm = container.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(container.windowToken, 0)
            }
            runCatching { wm.removeViewImmediate(container) }
        }
    }

    fun showBannerOverlay(
        message: String,
        isWarning: Boolean,
        durationMillis: Long = 5000L
    ) {
        mainHandler.post {
            if (!Settings.canDrawOverlays(appContext)) {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
                return@post
            }
            dismissBannerImmediate()

            val ctx = resolveOverlayContext()
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val bengaliTypeface = loadBengaliFont(ctx)

            val wrapper = FrameLayout(ctx).apply {
                setPadding(dp(ctx, 16), dp(ctx, 12), dp(ctx, 16), dp(ctx, 8))
            }

            val bannerCard = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(ctx, 16), dp(ctx, 14), dp(ctx, 16), dp(ctx, 14))
                background = GradientDrawable().apply {
                    setColor(
                        if (isWarning) Color.parseColor("#FEF3C7") else Color.parseColor("#FFDAD6")
                    )
                    cornerRadius = dp(ctx, 18).toFloat()
                }
                elevation = dp(ctx, 10).toFloat()
            }

            val textView = TextView(ctx).apply {
                text = message
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTextColor(
                    if (isWarning) Color.parseColor("#78350F") else Color.parseColor("#410002")
                )
                setTypeface(bengaliTypeface, Typeface.BOLD)
            }
            bannerCard.addView(textView)
            wrapper.addView(
                bannerCard,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(ctx, 32)
            }

            runCatching {
                wm.addView(wrapper, params)
                bannerContainer = wrapper
                bannerWindowManager = wm

                val runnable = Runnable { dismissBannerImmediate() }
                dismissBannerRunnable = runnable
                mainHandler.postDelayed(runnable, durationMillis)
            }.onFailure {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun dismissBannerImmediate() {
        dismissBannerRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissBannerRunnable = null
        val container = bannerContainer
        val wm = bannerWindowManager
        bannerContainer = null
        bannerWindowManager = null
        if (container != null && wm != null) {
            runCatching { wm.removeViewImmediate(container) }
        }
    }
}
