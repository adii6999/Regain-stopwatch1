package com.regain.stopwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("regain", MODE_PRIVATE) }
    private fun fmt(ms: Long): String {
        val cs = ms / 10
        return "%02d:%02d.%02d".format(cs / 6000, (cs / 100) % 60, cs % 100)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 36, 28, 24); gravity = Gravity.CENTER_HORIZONTAL; setBackgroundColor(0xFF0B1020.toInt()) }
        fun text(s: String, size: Float, color: Int = 0xFFFFFFFF.toInt()) = TextView(this).apply { text = s; textSize = size; setTextColor(color); gravity = Gravity.CENTER; setPadding(4, 12, 4, 12) }
        root.addView(text("REGAIN STOPWATCH", 25f, 0xFF67E8F9.toInt()))
        root.addView(text("Previous saved total", 16f, 0xFFCBD5E1.toInt()))
        root.addView(text(fmt(prefs.getLong("total", 0L)), 34f, 0xFF67E8F9.toInt()))
        root.addView(text("Saved sessions: ${prefs.getInt("count", 0)}", 16f, 0xFFCBD5E1.toInt()))
        val launch = Button(this).apply { text = "Show floating stopwatch"; setOnClickListener {
            if (Settings.canDrawOverlays(this@MainActivity)) startService(Intent(this@MainActivity, FloatingService::class.java))
            else { startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 20) }
        } }
        root.addView(launch, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(text("How to use", 19f, 0xFF67E8F9.toInt()))
        root.addView(text("1. Allow ‘Display over other apps’ permission.\n2. Tap Show floating stopwatch.\n3. Use it over other apps. Save adds this session to your previous total.\n4. Tap History in the floating panel to view saved sessions.", 16f, 0xFFE2E8F0.toInt()))
        root.addView(text("History stays on this device.", 13f, 0xFF94A3B8.toInt()))
        setContentView(ScrollView(this).apply { addView(root) })
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { super.onActivityResult(requestCode, resultCode, data); if (requestCode == 20 && Settings.canDrawOverlays(this)) startService(Intent(this, FloatingService::class.java)) }
}
