package com.regain.stopwatch

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var panel: LinearLayout
    private lateinit var timerText: TextView
    private lateinit var totalText: TextView
    private var params: WindowManager.LayoutParams? = null
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("regain", MODE_PRIVATE) }
    private var running = false
    private var startedAt = 0L
    private var accumulated = 0L
    private var sessionStartWall = 0L
    private val tick = object : Runnable { override fun run() { if (running) { timerText.text = fmt(accumulated + System.currentTimeMillis() - startedAt); handler.postDelayed(this, 50) } } }
    private fun fmt(ms: Long): String { val cs = ms.coerceAtLeast(0) / 10; return "%02d:%02d.%02d".format(cs / 6000, (cs / 100) % 60, cs % 100) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; textSize = 12f; setTextColor(Color.WHITE); setBackgroundColor(0xFF243248.toInt()); setOnClickListener { action() } }
    override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; buildPanel(); showPanel() }
    private fun buildPanel() {
        panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 10, 14, 10); setBackgroundColor(0xF20B1020.toInt()) }
        val head = TextView(this).apply { text = "REGAIN  ⋮  drag to move"; textSize = 12f; setTextColor(0xFF67E8F9.toInt()); setPadding(0, 0, 0, 8) }
        head.setOnTouchListener(object : View.OnTouchListener { var sx=0f; var sy=0f; var wx=0; var wy=0; override fun onTouch(v: View, e: MotionEvent): Boolean { val p=params ?: return false; when(e.action) { MotionEvent.ACTION_DOWN -> { sx=e.rawX; sy=e.rawY; wx=p.x; wy=p.y; return true }; MotionEvent.ACTION_MOVE -> { p.x=wx+(e.rawX-sx).toInt(); p.y=wy+(e.rawY-sy).toInt(); wm.updateViewLayout(panel,p); return true } }; return true } })
        panel.addView(head)
        timerText = TextView(this).apply { text = "00:00.00"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(0xFF67E8F9.toInt()); setPadding(0, 2, 0, 8) }; panel.addView(timerText)
        totalText = TextView(this).apply { textSize=12f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setPadding(0,0,0,6) }; panel.addView(totalText); updateTotal()
        val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        row.addView(button("START / PAUSE") { toggle() }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(button("SAVE + ADD") { saveSession() }, LinearLayout.LayoutParams(0, -2, 1f))
        panel.addView(row)
        val row2 = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        row2.addView(button("HISTORY") { showHistory() }, LinearLayout.LayoutParams(0, -2, 1f))
        row2.addView(button("RESET") { running=false; handler.removeCallbacks(tick); accumulated=0; timerText.text=fmt(0) }, LinearLayout.LayoutParams(0, -2, 1f))
        row2.addView(button("CLOSE ×") { stopSelf() }, LinearLayout.LayoutParams(0, -2, 1f)); panel.addView(row2)
    }
    private fun showPanel() { val type = if (android.os.Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=20; y=160 }
        wm.addView(panel, params)
    }
    private fun toggle() { if (running) { accumulated += System.currentTimeMillis()-startedAt; running=false; handler.removeCallbacks(tick) } else { startedAt=System.currentTimeMillis(); running=true; handler.post(tick) } }
    private fun saveSession() {
        if (running) { accumulated += System.currentTimeMillis()-startedAt; running=false; handler.removeCallbacks(tick) }
        val old = prefs.getLong("total", 0L); val now = accumulated
        if (now > 0) {
            val history = JSONArray(prefs.getString("history", "[]") ?: "[]")
            history.put(JSONObject().put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())).put("session", now).put("total", old+now))
            prefs.edit().putLong("total", old+now).putString("history", history.toString()).putInt("count", history.length()).apply()
        }
        accumulated=0; timerText.text=fmt(0); updateTotal(); Toast.makeText(this, if(now>0) "Session added to total" else "Start the stopwatch first", Toast.LENGTH_SHORT).show()
    }
    private fun updateTotal() { if (::totalText.isInitialized) totalText.text = "TOTAL  ${fmt(prefs.getLong("total",0L))}" }
    private fun showHistory() {
        val arr=JSONArray(prefs.getString("history","[]") ?: "[]"); val lines=StringBuilder("Saved sessions: ${arr.length()}\n\n")
        for (i in arr.length()-1 downTo 0) { val o=arr.getJSONObject(i); lines.append("${i+1}. ${o.optString("date")}\n   Session: ${fmt(o.optLong("session"))}   Total: ${fmt(o.optLong("total"))}\n\n") }
        if (arr.length()==0) lines.append("No saved sessions yet.")
        AlertDialogCompat.show(this, lines.toString())
    }
    override fun onDestroy() { handler.removeCallbacks(tick); if (::panel.isInitialized) try { wm.removeView(panel) } catch (_: Exception) {}; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
