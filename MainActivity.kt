package com.jarvis.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var assistant: Assistant
    private lateinit var status: TextView
    private lateinit var heard: TextView
    private lateinit var wakeBtn: Button
    private var recognizer: SpeechRecognizer? = null
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        assistant = Assistant.get(this)
        buildUi()
        assistant.onStatus = { s -> runOnUiThread { status.text = s } }
        requestAll(false)
    }

    override fun onResume() {
        super.onResume()
        refreshWakeButton()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun button(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun label(text: String, size: Float, color: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(6))
        }

    private fun addRow(parent: LinearLayout, vararg b: Button) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        b.forEach { row.addView(it, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)) }
        parent.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun buildUi() {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.parseColor("#0B1220")) }
        root.addView(EffectView(this, Effect.STARS))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(40), dp(16), dp(32))
        }

        col.addView(label("JARVIS  β 0.2", 28f, Color.parseColor("#7FDBFF")))
        status = label("Нажми на микрофон или скажи «Джарвис»", 16f, Color.WHITE)
        heard = label("", 14f, Color.LTGRAY)
        col.addView(status)
        col.addView(heard)

        col.addView(button("🎤  Говорить") { onMicPressed() })
        wakeBtn = button("") { toggleWake() }
        col.addView(wakeBtn)

        col.addView(label("Настройка (сделай один раз)", 16f, Color.parseColor("#7FDBFF")))
        addRow(col,
            button("1. Разрешения") { requestAll(true) },
            button("2. Доступность") { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
        addRow(col,
            button("3. Поверх окон") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            },
            button("4. Яркость") {
                startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            })
        addRow(col,
            button("5. Не засыпать в фоне") {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            },
            button("Инфо о приложении") {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            })

        col.addView(label("Эффекты и персонажи", 16f, Color.parseColor("#7FDBFF")))
        Effect.values().toList().chunked(2).forEach { pair ->
            addRow(col, *pair.map { e -> button("${e.emoji} ${e.label}") { assistant.startEffect(e) } }.toTypedArray())
        }
        col.addView(button("⛔ Выключить эффект") { assistant.stopEffect() })

        col.addView(label("Примеры команд", 16f, Color.parseColor("#7FDBFF")))
        col.addView(TextView(this).apply {
            text = "• возьми трубку / положи трубку\n" +
                "• позвони Вике\n" +
                "• напиши в ватсап Вике привет\n" +
                "• отправь смс маме я скоро буду\n" +
                "• включи блютуз / вайфай / фонарик\n" +
                "• громче / тише / ярче / темнее\n" +
                "• поставь будильник на 7:30\n" +
                "• таймер на 5 минут\n" +
                "• открой телеграм / найди рецепт борща\n" +
                "• назад / домой / закрой приложение / скриншот\n" +
                "• включи снег / дождь / кота / робота\n" +
                "• голос робота / следующий голос\n" +
                "• помощь"
            textSize = 14f
            setTextColor(Color.LTGRAY)
        })

        val sv = ScrollView(this)
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    // ------------------------------------------------------------ разрешения

    private fun requestAll(toast: Boolean) {
        val p = ArrayList<String>()
        p.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS)
        p.add(Manifest.permission.ANSWER_PHONE_CALLS)
        p.add(Manifest.permission.CALL_PHONE)
        p.add(Manifest.permission.READ_CONTACTS)
        p.add(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= 31) p.add(Manifest.permission.BLUETOOTH_CONNECT)
        val need = p.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
        else if (toast) Toast.makeText(this, "Все разрешения уже выданы", Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------------------------ слово "Джарвис" в фоне

    private fun refreshWakeButton() {
        wakeBtn.text = if (VoiceService.running) "👂 Слушаю «Джарвис» в фоне: ВКЛ" else "👂 Слушать «Джарвис» в фоне: ВЫКЛ"
    }

    private fun toggleWake() {
        if (VoiceService.running) {
            stopService(Intent(this, VoiceService::class.java))
        } else {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestAll(false)
                return
            }
            startForegroundService(Intent(this, VoiceService::class.java))
        }
        main.postDelayed({ refreshWakeButton() }, 400)
    }

    private fun wakeAction(action: String) {
        if (VoiceService.running) startService(Intent(this, VoiceService::class.java).setAction(action))
    }

    // ------------------------------------------------------------ кнопка микрофона

    private fun onMicPressed() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAll(false)
            return
        }
        val wasRunning = VoiceService.running
        wakeAction(VoiceService.ACTION_PAUSE)
        main.postDelayed({ startListening() }, if (wasRunning) 600L else 0L)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "На телефоне нет службы распознавания речи (нужно приложение Google)"
            wakeAction(VoiceService.ACTION_RESUME)
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status.text = "Слушаю…" }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    override fun onError(error: Int) {
                        status.text = "Не расслышал, попробуй ещё раз"
                        wakeAction(VoiceService.ACTION_RESUME)
                    }
                    override fun onResults(results: Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (text != null) {
                            heard.text = "«$text»"
                            assistant.handle(text)
                        }
                        wakeAction(VoiceService.ACTION_RESUME)
                    }
                })
            }
        }
        recognizer?.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            }
        )
    }

    override fun onDestroy() {
        assistant.onStatus = null
        recognizer?.destroy()
        super.onDestroy()
    }
}
