package org.nayagram.platform.audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * NGVoiceIcons - Handcrafted studio vector icon renderer for all 17 NayaGram Voice Studio models
 * and action controls (Play/Stop, Text-to-Voice, Auto Demo, Tune, Noise Gate, Send, Reset).
 *
 * Zero external image/font bloat — crisp anti-aliased vector art at every screen DPI.
 */
public final class NGVoiceIcons {

    public static final int ACTION_SPEAKER_ON = 1;
    public static final int ACTION_SPEAKER_OFF = 2;
    public static final int ACTION_TEXT_TO_VOICE = 3;
    public static final int ACTION_TUNE = 4;
    public static final int ACTION_SHIELD = 5;
    public static final int ACTION_PLAY = 6;
    public static final int ACTION_STOP = 7;
    public static final int ACTION_RESET = 8;
    public static final int ACTION_SEND_VOICE = 9;
    public static final int ACTION_SETTINGS = 10;

    private NGVoiceIcons() {}

    /**
     * Reusable View displaying the handcrafted character badge for any voice mode.
     */
    public static final class CharacterBadgeView extends View {
        private int mode;

        public CharacterBadgeView(Context context, int mode) {
            super(context);
            this.mode = mode;
        }

        public void setMode(int mode) {
            if (this.mode != mode) {
                this.mode = mode;
                invalidate();
            }
        }

        public int getMode() {
            return mode;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth();
            float h = getHeight();
            if (w > 0 && h > 0) {
                drawModeBadge(canvas, mode, w, h);
            }
        }
    }

    /**
     * Draws a rich studio gradient squircle badge + bespoke white vector character illustration.
     */
    public static void drawModeBadge(@NonNull Canvas canvas, int mode, float w, float h) {
        int cStart = NGVoiceChangerManager.getModeStartColor(mode);
        int cEnd = NGVoiceChangerManager.getModeEndColor(mode);

        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setShader(new LinearGradient(0f, 0f, w, h, cStart, cEnd, Shader.TileMode.CLAMP));
        float r = Math.min(w, h) * 0.30f;
        RectF rect = new RectF(0f, 0f, w, h);
        canvas.drawRoundRect(rect, r, r, bg);

        // Soft top-left specular glass sheen
        Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
        sheen.setShader(new LinearGradient(0f, 0f, 0f, h * 0.52f, 0x3DFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        RectF innerTop = new RectF(w * 0.05f, h * 0.05f, w * 0.95f, h * 0.52f);
        canvas.drawRoundRect(innerTop, r * 0.85f, r * 0.85f, sheen);

        // Crisp inner glass rim
        Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Math.max(1.2f, w * 0.032f));
        rim.setColor(0x38FFFFFF);
        canvas.drawRoundRect(
                w * 0.035f, h * 0.035f, w * 0.965f, h * 0.965f,
                r * 0.92f, r * 0.92f, rim
        );

        // White vector foreground paints
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(Color.WHITE);

        Paint softFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        softFill.setStyle(Paint.Style.FILL);
        softFill.setColor(0xCCFFFFFF);

        Paint cutout = new Paint(Paint.ANTI_ALIAS_FLAG);
        cutout.setStyle(Paint.Style.FILL);
        cutout.setColor(cEnd);

        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setColor(Color.WHITE);
        stroke.setStrokeWidth(Math.max(1.8f, w * 0.068f));

        Paint cutoutStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        cutoutStroke.setStyle(Paint.Style.STROKE);
        cutoutStroke.setStrokeCap(Paint.Cap.ROUND);
        cutoutStroke.setStrokeJoin(Paint.Join.ROUND);
        cutoutStroke.setColor(cEnd);
        cutoutStroke.setStrokeWidth(Math.max(1.6f, w * 0.055f));

        switch (mode) {
            case NGVoiceChangerManager.MODE_NORMAL:
                drawMicIcon(canvas, w, h, fill, stroke);
                break;
            case NGVoiceChangerManager.MODE_CHILD:
                drawChildIcon(canvas, w, h, fill, cutout, stroke, cutoutStroke);
                break;
            case NGVoiceChangerManager.MODE_MALE:
                drawManIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_FEMALE:
                drawWomanIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_MONSTER:
                drawMonsterIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_MOUSE:
                drawMouseIcon(canvas, w, h, fill, softFill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_HELIUM:
                drawHeliumBalloonIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_CHIPMUNK:
                drawChipmunkIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_DEEP:
                drawDeepBassIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_ROBOT:
                drawRobotIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_ALIEN:
                drawAlienIcon(canvas, w, h, fill, cutout);
                break;
            case NGVoiceChangerManager.MODE_HOARSE:
                drawHoarseWindIcon(canvas, w, h, fill, stroke);
                break;
            case NGVoiceChangerManager.MODE_ECHO:
                drawEchoIcon(canvas, w, h, fill, stroke);
                break;
            case NGVoiceChangerManager.MODE_CAVE:
                drawCaveIcon(canvas, w, h, fill, cutout, stroke);
                break;
            case NGVoiceChangerManager.MODE_TELEPHONE:
                drawTelephoneIcon(canvas, w, h, fill);
                break;
            case NGVoiceChangerManager.MODE_SPEED:
                drawSpeedDialIcon(canvas, w, h, fill, stroke);
                break;
            case NGVoiceChangerManager.MODE_MODULATION:
                drawModulationEqIcon(canvas, w, h, fill, stroke);
                break;
            default:
                drawMicIcon(canvas, w, h, fill, stroke);
                break;
        }
    }

    // 1. Real Voice — Studio Condenser Microphone
    private static void drawMicIcon(Canvas c, float w, float h, Paint fill, Paint stroke) {
        RectF capsule = new RectF(w * 0.37f, h * 0.19f, w * 0.63f, h * 0.56f);
        c.drawRoundRect(capsule, w * 0.13f, w * 0.13f, fill);

        RectF arc = new RectF(w * 0.27f, h * 0.34f, w * 0.73f, h * 0.68f);
        c.drawArc(arc, 0f, 180f, false, stroke);
        c.drawLine(w * 0.50f, h * 0.68f, w * 0.50f, h * 0.80f, stroke);
        c.drawLine(w * 0.36f, h * 0.80f, w * 0.64f, h * 0.80f, stroke);
    }

    // 2. Child Voice — Playful Smiling Kid Face with Hair Swirl (Emerald-Cyan badge, zero yellow!)
    private static void drawChildIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke, Paint cutoutStroke) {
        // Cute little side ears
        c.drawCircle(w * 0.23f, h * 0.55f, w * 0.08f, fill);
        c.drawCircle(w * 0.77f, h * 0.55f, w * 0.08f, fill);
        // Round cheerful kid face
        c.drawCircle(w * 0.50f, h * 0.55f, w * 0.27f, fill);
        // Playful hair tuft on top
        Path tuft = new Path();
        tuft.moveTo(w * 0.42f, h * 0.30f);
        tuft.cubicTo(w * 0.40f, h * 0.16f, w * 0.56f, h * 0.14f, w * 0.57f, h * 0.24f);
        tuft.cubicTo(w * 0.62f, h * 0.18f, w * 0.68f, h * 0.24f, w * 0.61f, h * 0.31f);
        tuft.close();
        c.drawPath(tuft, fill);
        // Bright friendly eyes
        c.drawCircle(w * 0.40f, h * 0.51f, w * 0.042f, cutout);
        c.drawCircle(w * 0.60f, h * 0.51f, w * 0.042f, cutout);
        // Happy kid smile
        RectF smile = new RectF(w * 0.38f, h * 0.55f, w * 0.62f, h * 0.70f);
        c.drawArc(smile, 15f, 150f, false, cutoutStroke);
    }

    // 3. Man Voice — Gentleman Portrait with Collar & Tie
    private static void drawManIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        // Head
        c.drawCircle(w * 0.50f, h * 0.35f, w * 0.16f, fill);
        // Broad shoulders
        Path body = new Path();
        body.moveTo(w * 0.21f, h * 0.80f);
        body.quadTo(w * 0.23f, h * 0.56f, w * 0.50f, h * 0.56f);
        body.quadTo(w * 0.77f, h * 0.56f, w * 0.79f, h * 0.80f);
        body.close();
        c.drawPath(body, fill);
        // Crisp V-collar & tie cutout
        Path tie = new Path();
        tie.moveTo(w * 0.46f, h * 0.57f);
        tie.lineTo(w * 0.54f, h * 0.57f);
        tie.lineTo(w * 0.525f, h * 0.74f);
        tie.lineTo(w * 0.50f, h * 0.78f);
        tie.lineTo(w * 0.475f, h * 0.74f);
        tie.close();
        c.drawPath(tie, cutout);
    }

    // 4. Woman Voice — Elegant Flowing Hair Silhouette
    private static void drawWomanIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        // Flowing hair contour behind head
        Path hair = new Path();
        hair.moveTo(w * 0.30f, h * 0.52f);
        hair.cubicTo(w * 0.27f, h * 0.24f, w * 0.37f, h * 0.16f, w * 0.50f, h * 0.16f);
        hair.cubicTo(w * 0.63f, h * 0.16f, w * 0.73f, h * 0.24f, w * 0.70f, h * 0.52f);
        hair.quadTo(w * 0.62f, h * 0.56f, w * 0.50f, h * 0.54f);
        hair.quadTo(w * 0.38f, h * 0.56f, w * 0.30f, h * 0.52f);
        hair.close();
        c.drawPath(hair, fill);
        // Face oval inside hair
        RectF face = new RectF(w * 0.38f, h * 0.27f, w * 0.62f, h * 0.52f);
        c.drawOval(face, cutout);
        // Inner face highlight
        RectF faceInner = new RectF(w * 0.405f, h * 0.30f, w * 0.595f, h * 0.50f);
        c.drawOval(faceInner, fill);
        // Graceful shoulders
        Path shoulders = new Path();
        shoulders.moveTo(w * 0.24f, h * 0.81f);
        shoulders.quadTo(w * 0.28f, h * 0.59f, w * 0.50f, h * 0.59f);
        shoulders.quadTo(w * 0.72f, h * 0.59f, w * 0.76f, h * 0.81f);
        shoulders.close();
        c.drawPath(shoulders, fill);
    }

    // 5. Monster — Horned Beast Face with Fangs
    private static void drawMonsterIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        // Curved horns
        Path leftHorn = new Path();
        leftHorn.moveTo(w * 0.32f, h * 0.38f);
        leftHorn.quadTo(w * 0.20f, h * 0.25f, w * 0.26f, h * 0.15f);
        leftHorn.quadTo(w * 0.32f, h * 0.24f, w * 0.42f, h * 0.32f);
        leftHorn.close();
        c.drawPath(leftHorn, fill);

        Path rightHorn = new Path();
        rightHorn.moveTo(w * 0.68f, h * 0.38f);
        rightHorn.quadTo(w * 0.80f, h * 0.25f, w * 0.74f, h * 0.15f);
        rightHorn.quadTo(w * 0.68f, h * 0.24f, w * 0.58f, h * 0.32f);
        rightHorn.close();
        c.drawPath(rightHorn, fill);

        // Head
        RectF head = new RectF(w * 0.24f, h * 0.29f, w * 0.76f, h * 0.80f);
        c.drawRoundRect(head, w * 0.20f, h * 0.20f, fill);

        // Fierce angled eyes
        Path lEye = new Path();
        lEye.moveTo(w * 0.33f, h * 0.45f);
        lEye.lineTo(w * 0.46f, h * 0.50f);
        lEye.lineTo(w * 0.35f, h * 0.53f);
        lEye.close();
        c.drawPath(lEye, cutout);

        Path rEye = new Path();
        rEye.moveTo(w * 0.67f, h * 0.45f);
        rEye.lineTo(w * 0.54f, h * 0.50f);
        rEye.lineTo(w * 0.65f, h * 0.53f);
        rEye.close();
        c.drawPath(rEye, cutout);

        // Roaring mouth + fangs
        RectF mouth = new RectF(w * 0.33f, h * 0.60f, w * 0.67f, h * 0.72f);
        c.drawRoundRect(mouth, w * 0.05f, w * 0.05f, cutout);

        Path fang1 = new Path();
        fang1.moveTo(w * 0.38f, h * 0.60f);
        fang1.lineTo(w * 0.43f, h * 0.60f);
        fang1.lineTo(w * 0.405f, h * 0.67f);
        fang1.close();
        c.drawPath(fang1, fill);

        Path fang2 = new Path();
        fang2.moveTo(w * 0.57f, h * 0.60f);
        fang2.lineTo(w * 0.62f, h * 0.60f);
        fang2.lineTo(w * 0.595f, h * 0.67f);
        fang2.close();
        c.drawPath(fang2, fill);
    }

    // 6. Mouse — Big Round Ears + Pointed Snout + Whiskers
    private static void drawMouseIcon(Canvas c, float w, float h, Paint fill, Paint softFill, Paint cutout, Paint stroke) {
        // Big round ears
        c.drawCircle(w * 0.28f, h * 0.32f, w * 0.145f, fill);
        c.drawCircle(w * 0.72f, h * 0.32f, w * 0.145f, fill);
        c.drawCircle(w * 0.28f, h * 0.32f, w * 0.075f, cutout);
        c.drawCircle(w * 0.72f, h * 0.32f, w * 0.075f, cutout);
        // Cute triangular-round mouse face
        Path face = new Path();
        face.moveTo(w * 0.27f, h * 0.47f);
        face.quadTo(w * 0.50f, h * 0.34f, w * 0.73f, h * 0.47f);
        face.quadTo(w * 0.70f, h * 0.72f, w * 0.50f, h * 0.79f);
        face.quadTo(w * 0.30f, h * 0.72f, w * 0.27f, h * 0.47f);
        face.close();
        c.drawPath(face, fill);
        // Eyes & nose
        c.drawCircle(w * 0.41f, h * 0.53f, w * 0.036f, cutout);
        c.drawCircle(w * 0.59f, h * 0.53f, w * 0.036f, cutout);
        c.drawCircle(w * 0.50f, h * 0.67f, w * 0.044f, cutout);
        // Tiny whiskers
        Paint wStroke = new Paint(stroke);
        wStroke.setStrokeWidth(Math.max(1.2f, w * 0.04f));
        c.drawLine(w * 0.17f, h * 0.61f, w * 0.30f, h * 0.63f, wStroke);
        c.drawLine(w * 0.19f, h * 0.69f, w * 0.31f, h * 0.67f, wStroke);
        c.drawLine(w * 0.83f, h * 0.61f, w * 0.70f, h * 0.63f, wStroke);
        c.drawLine(w * 0.81f, h * 0.69f, w * 0.69f, h * 0.67f, wStroke);
    }

    // 7. Helium — Floating Glossy Balloon with Wavy String
    private static void drawHeliumBalloonIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        RectF oval = new RectF(w * 0.28f, h * 0.15f, w * 0.72f, h * 0.64f);
        c.drawOval(oval, fill);
        // Specular highlight cutout
        Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
        shine.setStyle(Paint.Style.STROKE);
        shine.setStrokeCap(Paint.Cap.ROUND);
        shine.setColor(cutout.getColor());
        shine.setStrokeWidth(Math.max(1.5f, w * 0.052f));
        RectF shineArc = new RectF(w * 0.35f, h * 0.22f, w * 0.56f, h * 0.45f);
        c.drawArc(shineArc, 195f, 65f, false, shine);
        // Balloon knot
        Path knot = new Path();
        knot.moveTo(w * 0.45f, h * 0.63f);
        knot.lineTo(w * 0.55f, h * 0.63f);
        knot.lineTo(w * 0.50f, h * 0.70f);
        knot.close();
        c.drawPath(knot, fill);
        // Wavy string
        Path str = new Path();
        str.moveTo(w * 0.50f, h * 0.69f);
        str.cubicTo(w * 0.43f, h * 0.74f, w * 0.57f, h * 0.79f, w * 0.49f, h * 0.85f);
        Paint thin = new Paint(stroke);
        thin.setStrokeWidth(Math.max(1.5f, w * 0.052f));
        c.drawPath(str, thin);
    }

    // 8. Chipmunk — Perky Ears + Puffy Cheeks + Cute Buck Tooth
    private static void drawChipmunkIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        // Pointed perky ears
        Path le = new Path();
        le.moveTo(w * 0.30f, h * 0.42f);
        le.lineTo(w * 0.28f, h * 0.19f);
        le.lineTo(w * 0.44f, h * 0.33f);
        le.close();
        c.drawPath(le, fill);

        Path re = new Path();
        re.moveTo(w * 0.70f, h * 0.42f);
        re.lineTo(w * 0.72f, h * 0.19f);
        re.lineTo(w * 0.56f, h * 0.33f);
        re.close();
        c.drawPath(re, fill);

        // Chubby chipmunk cheeks + head
        c.drawCircle(w * 0.50f, h * 0.52f, w * 0.22f, fill);
        c.drawCircle(w * 0.32f, h * 0.62f, w * 0.14f, fill);
        c.drawCircle(w * 0.68f, h * 0.62f, w * 0.14f, fill);
        // Eyes & nose
        c.drawCircle(w * 0.41f, h * 0.48f, w * 0.038f, cutout);
        c.drawCircle(w * 0.59f, h * 0.48f, w * 0.038f, cutout);
        c.drawCircle(w * 0.50f, h * 0.58f, w * 0.036f, cutout);
        // Cute little tooth smile
        RectF tooth = new RectF(w * 0.45f, h * 0.64f, w * 0.55f, h * 0.72f);
        c.drawRect(tooth, cutout);
    }

    // 9. Deep Bass — Subwoofer Speaker Cone with Bass Ring
    private static void drawDeepBassIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        c.drawCircle(w * 0.50f, h * 0.50f, w * 0.32f, stroke);
        c.drawCircle(w * 0.50f, h * 0.50f, w * 0.21f, fill);
        c.drawCircle(w * 0.50f, h * 0.50f, w * 0.10f, cutout);
        c.drawCircle(w * 0.50f, h * 0.50f, w * 0.045f, fill);
    }

    // 10. Robot — Sci-Fi Android Head with Antenna & Glowing Visor
    private static void drawRobotIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        // Antenna
        c.drawLine(w * 0.50f, h * 0.19f, w * 0.50f, h * 0.31f, stroke);
        c.drawCircle(w * 0.50f, h * 0.17f, w * 0.055f, fill);
        // Side ear bolts
        c.drawRoundRect(new RectF(w * 0.16f, h * 0.46f, w * 0.23f, h * 0.62f), w * 0.03f, w * 0.03f, fill);
        c.drawRoundRect(new RectF(w * 0.77f, h * 0.46f, w * 0.84f, h * 0.62f), w * 0.03f, w * 0.03f, fill);
        // Android head box
        c.drawRoundRect(new RectF(w * 0.24f, h * 0.31f, w * 0.76f, h * 0.79f), w * 0.13f, w * 0.13f, fill);
        // Cyber visor bar
        c.drawRoundRect(new RectF(w * 0.31f, h * 0.43f, w * 0.69f, h * 0.56f), w * 0.06f, w * 0.06f, cutout);
        // Glowing visor eyes
        c.drawCircle(w * 0.41f, h * 0.495f, w * 0.038f, fill);
        c.drawCircle(w * 0.59f, h * 0.495f, w * 0.038f, fill);
        // Vocoder grille mouth
        c.drawRoundRect(new RectF(w * 0.36f, h * 0.64f, w * 0.64f, h * 0.70f), w * 0.03f, w * 0.03f, cutout);
    }

    // 11. Alien — Classic Extraterrestrial Head with Slanted Almond Eyes
    private static void drawAlienIcon(Canvas c, float w, float h, Paint fill, Paint cutout) {
        Path head = new Path();
        head.moveTo(w * 0.50f, h * 0.16f);
        head.cubicTo(w * 0.78f, h * 0.16f, w * 0.80f, h * 0.46f, w * 0.68f, h * 0.65f);
        head.cubicTo(w * 0.60f, h * 0.77f, w * 0.55f, h * 0.83f, w * 0.50f, h * 0.83f);
        head.cubicTo(w * 0.45f, h * 0.83f, w * 0.40f, h * 0.77f, w * 0.32f, h * 0.65f);
        head.cubicTo(w * 0.20f, h * 0.46f, w * 0.22f, h * 0.16f, w * 0.50f, h * 0.16f);
        head.close();
        c.drawPath(head, fill);

        // Slanted alien eyes
        c.save();
        c.rotate(24f, w * 0.37f, h * 0.50f);
        c.drawOval(new RectF(w * 0.29f, h * 0.43f, w * 0.46f, h * 0.55f), cutout);
        c.restore();

        c.save();
        c.rotate(-24f, w * 0.63f, h * 0.50f);
        c.drawOval(new RectF(w * 0.54f, h * 0.43f, w * 0.71f, h * 0.55f), cutout);
        c.restore();
    }

    // 12. Hoarseness — Smoky Whispering Breath Waves
    private static void drawHoarseWindIcon(Canvas c, float w, float h, Paint fill, Paint stroke) {
        Paint s = new Paint(stroke);
        s.setStrokeWidth(Math.max(1.8f, w * 0.072f));

        Path p1 = new Path();
        p1.moveTo(w * 0.20f, h * 0.36f);
        p1.cubicTo(w * 0.36f, h * 0.24f, w * 0.54f, h * 0.46f, w * 0.76f, h * 0.30f);
        c.drawPath(p1, s);

        Path p2 = new Path();
        p2.moveTo(w * 0.18f, h * 0.53f);
        p2.cubicTo(w * 0.38f, h * 0.42f, w * 0.58f, h * 0.64f, w * 0.82f, h * 0.49f);
        c.drawPath(p2, s);

        Path p3 = new Path();
        p3.moveTo(w * 0.26f, h * 0.70f);
        p3.cubicTo(w * 0.42f, h * 0.60f, w * 0.58f, h * 0.78f, w * 0.74f, h * 0.66f);
        c.drawPath(p3, s);
    }

    // 13. Echo — Expanding Concentric Sound Ripples
    private static void drawEchoIcon(Canvas c, float w, float h, Paint fill, Paint stroke) {
        c.drawCircle(w * 0.29f, h * 0.50f, w * 0.085f, fill);
        c.drawArc(new RectF(w * 0.16f, h * 0.34f, w * 0.52f, h * 0.66f), -52f, 104f, false, stroke);
        c.drawArc(new RectF(w * 0.12f, h * 0.23f, w * 0.68f, h * 0.77f), -50f, 100f, false, stroke);
        c.drawArc(new RectF(w * 0.08f, h * 0.13f, w * 0.83f, h * 0.87f), -46f, 92f, false, stroke);
    }

    // 14. Cave Reverb — Mountain Cave Arch with Acoustic Ripples
    private static void drawCaveIcon(Canvas c, float w, float h, Paint fill, Paint cutout, Paint stroke) {
        Path mtn = new Path();
        mtn.moveTo(w * 0.15f, h * 0.78f);
        mtn.lineTo(w * 0.43f, h * 0.24f);
        mtn.lineTo(w * 0.55f, h * 0.41f);
        mtn.lineTo(w * 0.66f, h * 0.28f);
        mtn.lineTo(w * 0.85f, h * 0.78f);
        mtn.close();
        c.drawPath(mtn, fill);

        // Dark cave tunnel arch
        Path arch = new Path();
        arch.moveTo(w * 0.37f, h * 0.78f);
        arch.lineTo(w * 0.37f, h * 0.59f);
        arch.arcTo(new RectF(w * 0.37f, h * 0.46f, w * 0.63f, h * 0.72f), 180f, 180f, false);
        arch.lineTo(w * 0.63f, h * 0.78f);
        arch.close();
        c.drawPath(arch, cutout);

        // Subtle acoustic dot inside cave
        c.drawCircle(w * 0.50f, h * 0.66f, w * 0.042f, fill);
    }

    // 15. Telephone — Classic Handset
    private static void drawTelephoneIcon(Canvas c, float w, float h, Paint fill) {
        c.save();
        c.rotate(-42f, w * 0.50f, h * 0.50f);
        Path phone = new Path();
        phone.addRoundRect(new RectF(w * 0.20f, h * 0.41f, w * 0.35f, h * 0.63f), w * 0.06f, w * 0.06f, Path.Direction.CW);
        phone.addRoundRect(new RectF(w * 0.65f, h * 0.41f, w * 0.80f, h * 0.63f), w * 0.06f, w * 0.06f, Path.Direction.CW);
        phone.addRoundRect(new RectF(w * 0.28f, h * 0.47f, w * 0.72f, h * 0.58f), w * 0.05f, w * 0.05f, Path.Direction.CW);
        c.drawPath(phone, fill);
        c.restore();
    }

    // 16. Record Speed — Dual Fast-Forward Speed Chevrons + Gauge Arc
    private static void drawSpeedDialIcon(Canvas c, float w, float h, Paint fill, Paint stroke) {
        Paint s = new Paint(stroke);
        s.setStrokeWidth(Math.max(1.8f, w * 0.065f));
        c.drawArc(new RectF(w * 0.19f, h * 0.22f, w * 0.81f, h * 0.84f), 165f, 210f, false, s);
        // Speed needle pointing right-up
        c.drawLine(w * 0.50f, h * 0.61f, w * 0.68f, h * 0.41f, s);
        c.drawCircle(w * 0.50f, h * 0.61f, w * 0.07f, fill);
    }

    // 17. Modulation — 3-Bar Audio Equalizer Sliders
    private static void drawModulationEqIcon(Canvas c, float w, float h, Paint fill, Paint stroke) {
        Paint s = new Paint(stroke);
        s.setStrokeWidth(Math.max(1.6f, w * 0.058f));
        float[] xs = new float[]{0.30f, 0.50f, 0.70f};
        float[] ys = new float[]{0.36f, 0.62f, 0.42f};
        for (int i = 0; i < 3; i++) {
            float x = w * xs[i];
            c.drawLine(x, h * 0.22f, x, h * 0.78f, s);
            c.drawCircle(x, h * ys[i], w * 0.075f, fill);
        }
    }

    /**
     * Creates a crisp custom vector Drawable for Voice Studio action chips & buttons.
     */
    @NonNull
    public static Drawable createActionIconDrawable(int actionType, int sizePx, int tintColor) {
        Drawable d = new ActionIconDrawable(actionType, sizePx, tintColor);
        d.setBounds(0, 0, sizePx, sizePx);
        return d;
    }

    private static final class ActionIconDrawable extends Drawable {
        private final int type;
        private final int size;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        ActionIconDrawable(int type, int size, int color) {
            this.type = type;
            this.size = size;
            fill.setStyle(Paint.Style.FILL);
            fill.setColor(color);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setColor(color);
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public void draw(@NonNull Canvas c) {
            Rect b = getBounds();
            float w = b.width();
            float h = b.height();
            if (w <= 0 || h <= 0) return;
            c.save();
            c.translate(b.left, b.top);
            stroke.setStrokeWidth(Math.max(1.6f, w * 0.105f));

            switch (type) {
                case ACTION_SPEAKER_ON: {
                    Path sp = new Path();
                    sp.moveTo(w * 0.14f, h * 0.39f);
                    sp.lineTo(w * 0.32f, h * 0.39f);
                    sp.lineTo(w * 0.54f, h * 0.21f);
                    sp.lineTo(w * 0.54f, h * 0.79f);
                    sp.lineTo(w * 0.32f, h * 0.61f);
                    sp.lineTo(w * 0.14f, h * 0.61f);
                    sp.close();
                    c.drawPath(sp, fill);
                    c.drawArc(new RectF(w * 0.48f, h * 0.32f, w * 0.74f, h * 0.68f), -50f, 100f, false, stroke);
                    c.drawArc(new RectF(w * 0.52f, h * 0.18f, w * 0.90f, h * 0.82f), -48f, 96f, false, stroke);
                    break;
                }
                case ACTION_SPEAKER_OFF: {
                    Path sp = new Path();
                    sp.moveTo(w * 0.14f, h * 0.39f);
                    sp.lineTo(w * 0.32f, h * 0.39f);
                    sp.lineTo(w * 0.54f, h * 0.21f);
                    sp.lineTo(w * 0.54f, h * 0.79f);
                    sp.lineTo(w * 0.32f, h * 0.61f);
                    sp.lineTo(w * 0.14f, h * 0.61f);
                    sp.close();
                    c.drawPath(sp, fill);
                    c.drawLine(w * 0.66f, h * 0.36f, w * 0.88f, h * 0.64f, stroke);
                    c.drawLine(w * 0.88f, h * 0.36f, w * 0.66f, h * 0.64f, stroke);
                    break;
                }
                case ACTION_TEXT_TO_VOICE: {
                    // Speech wave + text lines
                    c.drawRoundRect(new RectF(w * 0.10f, h * 0.18f, w * 0.66f, h * 0.72f), w * 0.12f, w * 0.12f, stroke);
                    c.drawLine(w * 0.23f, h * 0.38f, w * 0.53f, h * 0.38f, stroke);
                    c.drawLine(w * 0.23f, h * 0.54f, w * 0.44f, h * 0.54f, stroke);
                    c.drawArc(new RectF(w * 0.56f, h * 0.30f, w * 0.84f, h * 0.62f), -48f, 96f, false, stroke);
                    c.drawArc(new RectF(w * 0.64f, h * 0.18f, w * 0.95f, h * 0.74f), -45f, 90f, false, stroke);
                    break;
                }
                case ACTION_TUNE: {
                    float[] ys = new float[]{0.26f, 0.50f, 0.74f};
                    float[] kx = new float[]{0.35f, 0.68f, 0.44f};
                    for (int i = 0; i < 3; i++) {
                        c.drawLine(w * 0.14f, h * ys[i], w * 0.86f, h * ys[i], stroke);
                        c.drawCircle(w * kx[i], h * ys[i], w * 0.11f, fill);
                    }
                    break;
                }
                case ACTION_SHIELD: {
                    Path sh = new Path();
                    sh.moveTo(w * 0.50f, h * 0.12f);
                    sh.lineTo(w * 0.82f, h * 0.25f);
                    sh.lineTo(w * 0.80f, h * 0.55f);
                    sh.quadTo(w * 0.76f, h * 0.78f, w * 0.50f, h * 0.89f);
                    sh.quadTo(w * 0.24f, h * 0.78f, w * 0.20f, h * 0.55f);
                    sh.lineTo(w * 0.18f, h * 0.25f);
                    sh.close();
                    c.drawPath(sh, stroke);
                    Path chk = new Path();
                    chk.moveTo(w * 0.36f, h * 0.51f);
                    chk.lineTo(w * 0.46f, h * 0.61f);
                    chk.lineTo(w * 0.66f, h * 0.39f);
                    c.drawPath(chk, stroke);
                    break;
                }
                case ACTION_PLAY: {
                    Path tri = new Path();
                    tri.moveTo(w * 0.26f, h * 0.18f);
                    tri.lineTo(w * 0.82f, h * 0.50f);
                    tri.lineTo(w * 0.26f, h * 0.82f);
                    tri.close();
                    c.drawPath(tri, fill);
                    break;
                }
                case ACTION_STOP: {
                    c.drawRoundRect(new RectF(w * 0.24f, h * 0.24f, w * 0.76f, h * 0.76f), w * 0.10f, w * 0.10f, fill);
                    break;
                }
                case ACTION_RESET: {
                    c.drawArc(new RectF(w * 0.18f, h * 0.18f, w * 0.82f, h * 0.82f), 40f, 290f, false, stroke);
                    Path arr = new Path();
                    arr.moveTo(w * 0.66f, h * 0.14f);
                    arr.lineTo(w * 0.84f, h * 0.28f);
                    arr.lineTo(w * 0.64f, h * 0.34f);
                    arr.close();
                    c.drawPath(arr, fill);
                    break;
                }
                case ACTION_SEND_VOICE: {
                    Path plane = new Path();
                    plane.moveTo(w * 0.14f, h * 0.20f);
                    plane.lineTo(w * 0.88f, h * 0.50f);
                    plane.lineTo(w * 0.14f, h * 0.80f);
                    plane.lineTo(w * 0.27f, h * 0.54f);
                    plane.lineTo(w * 0.58f, h * 0.50f);
                    plane.lineTo(w * 0.27f, h * 0.46f);
                    plane.close();
                    c.drawPath(plane, fill);
                    break;
                }
                case ACTION_SETTINGS:
                default: {
                    c.drawCircle(w * 0.50f, h * 0.50f, w * 0.18f, stroke);
                    for (int i = 0; i < 6; i++) {
                        double a = i * Math.PI / 3.0;
                        float x1 = (float) (w * 0.50 + Math.cos(a) * w * 0.25);
                        float y1 = (float) (h * 0.50 + Math.sin(a) * h * 0.25);
                        float x2 = (float) (w * 0.50 + Math.cos(a) * w * 0.37);
                        float y2 = (float) (h * 0.50 + Math.sin(a) * h * 0.37);
                        c.drawLine(x1, y1, x2, y2, stroke);
                    }
                    break;
                }
            }
            c.restore();
        }

        @Override
        public void setAlpha(int alpha) {
            fill.setAlpha(alpha);
            stroke.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            fill.setColorFilter(colorFilter);
            stroke.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
