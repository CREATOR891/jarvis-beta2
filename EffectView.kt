package com.jarvis.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import java.util.Random
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

class EffectView(context: Context, private val effect: Effect) : View(context) {

    private class P(
        var x: Float = 0f, var y: Float = 0f,
        var vx: Float = 0f, var vy: Float = 0f,
        var size: Float = 0f, var rot: Float = 0f, var vrot: Float = 0f,
        var color: Int = Color.WHITE, var life: Float = 1f, var phase: Float = 0f,
        var glyph: String = ""
    )

    private val d = context.resources.displayMetrics.density
    private val rnd = Random()
    private val ps = ArrayList<P>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L

    private val confettiColors = intArrayOf(
        Color.parseColor("#FF5252"), Color.parseColor("#FFD740"), Color.parseColor("#69F0AE"),
        Color.parseColor("#40C4FF"), Color.parseColor("#E040FB"), Color.parseColor("#FF6E40")
    )
    private val hearts = arrayOf("❤️", "💖", "💗", "💕", "💘")
    private val leaves = arrayOf("🍂", "🍁", "🍃")

    private fun r(a: Float, b: Float): Float = a + rnd.nextFloat() * (b - a)

    private fun count(): Int = when (effect) {
        Effect.SNOW -> 90
        Effect.RAIN -> 140
        Effect.CONFETTI -> 80
        Effect.STARS -> 50
        Effect.HEARTS -> 22
        Effect.FIRE -> 90
        Effect.FIREFLIES -> 35
        Effect.BUBBLES -> 28
        Effect.LEAVES -> 24
        else -> 1
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ps.clear()
        repeat(count()) {
            val p = P()
            spawn(p, true)
            ps.add(p)
        }
    }

    private fun spawn(p: P, initial: Boolean) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (effect) {
            Effect.SNOW -> {
                p.x = r(0f, w); p.y = if (initial) r(0f, h) else -10f * d
                p.size = r(1.5f, 5f) * d; p.vy = r(40f, 110f) * d
                p.vx = r(-8f, 8f) * d; p.phase = r(0f, 6.28f)
            }
            Effect.RAIN -> {
                p.x = r(0f, w + 100f * d); p.y = if (initial) r(0f, h) else -30f * d
                p.size = r(10f, 22f) * d; p.vy = r(700f, 1100f) * d; p.vx = -90f * d
            }
            Effect.CONFETTI -> {
                p.x = r(0f, w); p.y = if (initial) r(0f, h) else -10f * d
                p.size = r(5f, 10f) * d; p.vy = r(110f, 260f) * d; p.vx = r(-30f, 30f) * d
                p.rot = r(0f, 360f); p.vrot = r(-300f, 300f); p.phase = r(0f, 6.28f)
                p.color = confettiColors[rnd.nextInt(confettiColors.size)]
            }
            Effect.STARS -> {
                p.x = r(0f, w); p.y = r(0f, h); p.size = r(1.5f, 4.5f) * d
                p.phase = r(0f, 6.28f); p.vrot = r(1f, 3f)
                p.color = if (rnd.nextInt(3) == 0) Color.parseColor("#FFE082") else Color.WHITE
            }
            Effect.HEARTS -> {
                p.x = r(0f, w); p.y = if (initial) r(0f, h) else h + 30f * d
                p.size = r(16f, 34f) * d; p.vy = -r(60f, 140f) * d; p.phase = r(0f, 6.28f)
                p.glyph = hearts[rnd.nextInt(hearts.size)]
            }
            Effect.FIRE -> {
                p.x = r(0f, w); p.y = h + 5f * d; p.vy = -r(90f, 260f) * d; p.vx = r(-20f, 20f) * d
                p.size = r(3f, 9f) * d; p.phase = r(0.5f, 1.2f)
                p.life = if (initial) r(0.1f, 1f) else 1f
            }
            Effect.FIREFLIES -> {
                p.x = r(0f, w); p.y = r(h * 0.3f, h)
                p.vx = r(-25f, 25f) * d; p.vy = r(-25f, 25f) * d
                p.size = r(2f, 4f) * d; p.phase = r(0f, 6.28f); p.vrot = r(1f, 3f)
            }
            Effect.BUBBLES -> {
                p.x = r(0f, w); p.y = if (initial) r(0f, h) else h + 40f * d
                p.size = r(8f, 30f) * d; p.vy = -r(40f, 100f) * d; p.phase = r(0f, 6.28f)
            }
            Effect.LEAVES -> {
                p.x = r(0f, w); p.y = if (initial) r(0f, h) else -30f * d
                p.size = r(18f, 32f) * d; p.vy = r(50f, 120f) * d; p.vx = r(-20f, 20f) * d
                p.rot = r(0f, 360f); p.vrot = r(-90f, 90f); p.phase = r(0f, 6.28f)
                p.glyph = leaves[rnd.nextInt(leaves.size)]
            }
            else -> {
                p.size = 64f * d; p.x = w / 2f; p.vx = 70f * d
                p.glyph = effect.glyph ?: "🐱"
            }
        }
    }

    private fun update(p: P, dt: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (effect) {
            Effect.SNOW -> {
                p.phase += dt * 2f
                p.x += (p.vx + sin(p.phase) * 14f * d) * dt
                p.y += p.vy * dt
                if (p.y > h + p.size) spawn(p, false)
            }
            Effect.RAIN -> {
                p.x += p.vx * dt; p.y += p.vy * dt
                if (p.y > h + p.size) spawn(p, false)
            }
            Effect.CONFETTI -> {
                p.phase += dt * 3f
                p.x += (p.vx + sin(p.phase) * 30f * d) * dt
                p.y += p.vy * dt; p.rot += p.vrot * dt
                if (p.y > h + p.size) spawn(p, false)
            }
            Effect.STARS -> p.phase += dt * p.vrot
            Effect.HEARTS -> {
                p.phase += dt * 2f
                p.x += sin(p.phase) * 30f * d * dt
                p.y += p.vy * dt
                if (p.y < -p.size) spawn(p, false)
            }
            Effect.FIRE -> {
                p.x += p.vx * dt; p.y += p.vy * dt
                p.life -= dt * p.phase
                if (p.life <= 0f) spawn(p, false)
            }
            Effect.FIREFLIES -> {
                p.phase += dt * p.vrot
                p.x += p.vx * dt; p.y += p.vy * dt
                if (rnd.nextFloat() < dt * 0.8f) {
                    p.vx = r(-25f, 25f) * d; p.vy = r(-25f, 25f) * d
                }
                if (p.x < 0f) p.x = w
                if (p.x > w) p.x = 0f
                if (p.y < 0f) p.y = h
                if (p.y > h) p.y = 0f
            }
            Effect.BUBBLES -> {
                p.phase += dt * 1.5f
                p.x += sin(p.phase) * 20f * d * dt
                p.y += p.vy * dt
                if (p.y < -p.size * 2f) spawn(p, false)
            }
            Effect.LEAVES -> {
                p.phase += dt * 1.5f
                p.x += (p.vx + sin(p.phase) * 40f * d) * dt
                p.y += p.vy * dt; p.rot += p.vrot * dt
                if (p.y > h + p.size) spawn(p, false)
            }
            else -> {
                p.x += p.vx * dt
                if (p.x > w - p.size / 2f) { p.x = w - p.size / 2f; p.vx = -abs(p.vx) }
                if (p.x < p.size / 2f) { p.x = p.size / 2f; p.vx = abs(p.vx) }
                p.phase += dt * 8f
                if (rnd.nextFloat() < dt * 0.15f) p.vx = -p.vx
            }
        }
    }

    private fun drawGlyph(c: Canvas, g: String, x: Float, y: Float, size: Float, rot: Float, flip: Boolean, alpha: Int) {
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.alpha = alpha
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        c.save()
        c.translate(x, y)
        c.rotate(rot)
        if (flip) c.scale(-1f, 1f)
        c.drawText(g, 0f, size * 0.35f, paint)
        c.restore()
    }

    private fun draw(c: Canvas, p: P) {
        when (effect) {
            Effect.SNOW -> {
                paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.alpha = 220
                c.drawCircle(p.x, p.y, p.size, paint)
            }
            Effect.RAIN -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d
                paint.color = Color.parseColor("#AEE1FF"); paint.alpha = 160
                c.drawLine(p.x, p.y, p.x - p.vx * 0.03f, p.y - p.size, paint)
            }
            Effect.CONFETTI -> {
                paint.style = Paint.Style.FILL; paint.color = p.color
                c.save(); c.translate(p.x, p.y); c.rotate(p.rot)
                c.drawRect(-p.size / 2f, -p.size / 4f, p.size / 2f, p.size / 4f, paint)
                c.restore()
            }
            Effect.STARS -> {
                val a = (140 + 115 * sin(p.phase)).toInt().coerceIn(0, 255)
                paint.style = Paint.Style.FILL; paint.color = p.color; paint.alpha = a
                c.drawCircle(p.x, p.y, p.size, paint)
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 0.8f * d
                paint.color = p.color; paint.alpha = a
                c.drawLine(p.x - p.size * 2.2f, p.y, p.x + p.size * 2.2f, p.y, paint)
                c.drawLine(p.x, p.y - p.size * 2.2f, p.x, p.y + p.size * 2.2f, paint)
            }
            Effect.HEARTS -> drawGlyph(c, p.glyph, p.x, p.y, p.size, 0f, false, 230)
            Effect.FIRE -> {
                val l = p.life.coerceIn(0f, 1f)
                val col = when {
                    l > 0.66f -> Color.parseColor("#FFEB3B")
                    l > 0.33f -> Color.parseColor("#FF9800")
                    else -> Color.parseColor("#F44336")
                }
                paint.style = Paint.Style.FILL; paint.color = col; paint.alpha = (l * 220).toInt()
                c.drawCircle(p.x, p.y, p.size * (0.4f + l * 0.6f), paint)
            }
            Effect.FIREFLIES -> {
                val a = 0.5f + 0.5f * sin(p.phase)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#E8FF6B"); paint.alpha = (60 * a).toInt()
                c.drawCircle(p.x, p.y, p.size * 4f, paint)
                paint.color = Color.parseColor("#E8FF6B"); paint.alpha = (80 + 120 * a).toInt().coerceIn(0, 255)
                c.drawCircle(p.x, p.y, p.size, paint)
            }
            Effect.BUBBLES -> {
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f * d
                paint.color = Color.WHITE; paint.alpha = 140
                c.drawCircle(p.x, p.y, p.size, paint)
                paint.style = Paint.Style.FILL; paint.color = Color.WHITE; paint.alpha = 120
                c.drawCircle(p.x - p.size * 0.35f, p.y - p.size * 0.35f, p.size * 0.15f, paint)
            }
            Effect.LEAVES -> drawGlyph(c, p.glyph, p.x, p.y, p.size, p.rot, false, 235)
            else -> {
                val bottom = height - 96f * d
                val bob = abs(sin(p.phase)) * 8f * d
                drawGlyph(c, p.glyph, p.x, bottom - bob, p.size, 0f, p.vx < 0f, 255)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else min(0.05f, (now - last) / 1e9f)
        last = now
        for (p in ps) {
            update(p, dt)
            draw(canvas, p)
        }
        postInvalidateOnAnimation()
    }
}
