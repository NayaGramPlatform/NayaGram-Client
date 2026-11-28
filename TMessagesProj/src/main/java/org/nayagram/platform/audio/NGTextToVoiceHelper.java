package org.nayagram.platform.audio;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.utils.recorder.audio.NGVoiceOpusWriter;

import java.io.File;
import java.util.Locale;

/**
 * NGTextToVoiceHelper - Studio Text-to-Voice dialog supporting English & Bangla with
 * automatic emoji stripping, natural human pacing (default 0.86x), seamless Cloud+Device TTS,
 * live preview, and direct sending as a real Telegram Ogg/Opus voice message.
 */
public final class NGTextToVoiceHelper {

    private NGTextToVoiceHelper() {}

    public static void showDialog(Context context, @Nullable ChatActivity chatActivity) {
        if (context == null) return;
        final boolean isDark = Theme.isCurrentThemeDark();
        final int[] selectedLang = new int[]{NGVoiceDemoPlayer.LANG_AUTO};
        // Natural human pace default = 0.86x (range 0.60x .. 1.35x)
        final float[] speakingRate = new float[]{NGVoiceDemoPlayer.DEFAULT_HUMAN_SPEED};
        final int[] voiceMode = new int[]{NGVoiceChangerManager.getSelectedMode(context)};
        final boolean[] busy = new boolean[]{false};

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(12));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 1. Studio Header Banner with custom vector badge
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable headerBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                isDark ? new int[]{0xFF172554, 0xFF312E81} : new int[]{0xFFE0F2FE, 0xFFEDE9FE}
        );
        headerBg.setCornerRadius(AndroidUtilities.dp(18));
        headerBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF3730A3 : 0xFFBAE6FD);
        header.setBackground(headerBg);

        View headerIcon = new View(context) {
            @Override
            protected void onDraw(android.graphics.Canvas c) {
                float w = getWidth(), h = getHeight();
                if (w <= 0 || h <= 0) return;
                android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                p.setShader(new android.graphics.LinearGradient(0, 0, w, h, 0xFF0EA5E9, 0xFF7C3AED, android.graphics.Shader.TileMode.CLAMP));
                c.drawRoundRect(new android.graphics.RectF(0, 0, w, h), w * 0.30f, h * 0.30f, p);
                Drawable d = NGVoiceIcons.createActionIconDrawable(NGVoiceIcons.ACTION_TEXT_TO_VOICE, (int) (w * 0.62f), Color.WHITE);
                int pad = (int) (w * 0.19f);
                d.setBounds(pad, pad, (int) w - pad, (int) h - pad);
                d.draw(c);
            }
        };
        LinearLayout.LayoutParams hiLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(42), AndroidUtilities.dp(42));
        hiLp.rightMargin = AndroidUtilities.dp(11);
        header.addView(headerIcon, hiLp);

        LinearLayout headerTextCol = new LinearLayout(context);
        headerTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleTv = new TextView(context);
        titleTv.setText("Text to Voice Studio");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f);
        titleTv.setTypeface(AndroidUtilities.bold());
        titleTv.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        headerTextCol.addView(titleTv);

        TextView subTv = new TextView(context);
        subTv.setText("Natural human Bangla & English • Emojis auto-skipped");
        subTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        subTv.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF475569);
        subTv.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        headerTextCol.addView(subTv);

        header.addView(headerTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.bottomMargin = AndroidUtilities.dp(12);
        root.addView(header, headerLp);

        // 2. Active Voice Character Selector Row with handcrafted vector icon
        LinearLayout voiceRow = new LinearLayout(context);
        voiceRow.setOrientation(LinearLayout.HORIZONTAL);
        voiceRow.setGravity(Gravity.CENTER_VERTICAL);
        voiceRow.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(8));
        GradientDrawable voiceRowBg = new GradientDrawable();
        voiceRowBg.setCornerRadius(AndroidUtilities.dp(14));
        voiceRowBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
        voiceRowBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);
        voiceRow.setBackground(voiceRowBg);

        final NGVoiceIcons.CharacterBadgeView voiceBadge = new NGVoiceIcons.CharacterBadgeView(context, voiceMode[0]);
        LinearLayout.LayoutParams vbLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(36));
        vbLp.rightMargin = AndroidUtilities.dp(10);
        voiceRow.addView(voiceBadge, vbLp);

        LinearLayout voiceTextCol = new LinearLayout(context);
        voiceTextCol.setOrientation(LinearLayout.VERTICAL);

        TextView voiceKicker = new TextView(context);
        voiceKicker.setText("VOICE CHARACTER");
        voiceKicker.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9.5f);
        voiceKicker.setTypeface(AndroidUtilities.bold());
        voiceKicker.setLetterSpacing(0.06f);
        voiceKicker.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        voiceTextCol.addView(voiceKicker);

        final TextView voiceLabel = new TextView(context);
        voiceLabel.setText(NGVoiceChangerManager.getModeTitle(voiceMode[0]));
        voiceLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.8f);
        voiceLabel.setTypeface(AndroidUtilities.bold());
        voiceLabel.setTextColor(isDark ? 0xFFF1F5F9 : 0xFF0F172A);
        voiceTextCol.addView(voiceLabel);

        voiceRow.addView(voiceTextCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView changeVoiceBtn = new TextView(context);
        changeVoiceBtn.setText("Change");
        changeVoiceBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.2f);
        changeVoiceBtn.setTypeface(AndroidUtilities.bold());
        changeVoiceBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        changeVoiceBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        GradientDrawable chBg = new GradientDrawable();
        chBg.setCornerRadius(AndroidUtilities.dp(10));
        chBg.setColor(isDark ? 0xFF0F172A : 0xFFE0F2FE);
        changeVoiceBtn.setBackground(chBg);
        voiceRow.addView(changeVoiceBtn);

        LinearLayout.LayoutParams vrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vrLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(voiceRow, vrLp);

        // 3. Language Selector Chips (Auto / English / বাংলা)
        LinearLayout langRow = new LinearLayout(context);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        final TextView[] langBtns = new TextView[3];
        final String[] langTitles = new String[]{"Auto (EN/বাংলা)", "English", "বাংলা"};
        final int[] langModes = new int[]{
                NGVoiceDemoPlayer.LANG_AUTO,
                NGVoiceDemoPlayer.LANG_ENGLISH,
                NGVoiceDemoPlayer.LANG_BANGLA
        };

        Runnable refreshLang = () -> {
            for (int i = 0; i < 3; i++) {
                boolean active = selectedLang[0] == langModes[i];
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(AndroidUtilities.dp(11));
                if (active) {
                    bg.setColors(new int[]{0xFF0284C7, 0xFF2563EB});
                    bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                } else {
                    bg.setColor(isDark ? 0xFF1E293B : 0xFFF1F5F9);
                    bg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);
                }
                langBtns[i].setBackground(bg);
                langBtns[i].setTextColor(active ? Color.WHITE : (isDark ? 0xFFCBD5E1 : 0xFF334155));
            }
        };

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView btn = new TextView(context);
            btn.setText(langTitles[i]);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            btn.setTypeface(AndroidUtilities.bold());
            btn.setGravity(Gravity.CENTER);
            btn.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(9), AndroidUtilities.dp(8), AndroidUtilities.dp(9));
            btn.setOnClickListener(v -> {
                selectedLang[0] = langModes[idx];
                refreshLang.run();
            });
            langBtns[i] = btn;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.leftMargin = AndroidUtilities.dp(6);
            langRow.addView(btn, lp);
        }
        refreshLang.run();
        LinearLayout.LayoutParams lrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lrLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(langRow, lrLp);

        // 4. Text Input Box
        final EditText input = new EditText(context);
        input.setHint("Type in English or বাংলা...\nউদাহরণ: আসসালামু আলাইকুম, কেমন আছেন?");
        input.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.5f);
        input.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        input.setHintTextColor(isDark ? 0xFF64748B : 0xFF94A3B8);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setMinLines(3);
        input.setMaxLines(6);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(11), AndroidUtilities.dp(12), AndroidUtilities.dp(11));
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(AndroidUtilities.dp(14));
        inputBg.setColor(isDark ? 0xFF0F172A : 0xFFFFFFFF);
        inputBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        input.setBackground(inputBg);
        LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inLp.bottomMargin = AndroidUtilities.dp(10);
        root.addView(input, inLp);

        // 5. Speaking Speed Slider (Default 0.86x Natural Human Pace)
        final TextView rateLabel = new TextView(context);
        rateLabel.setText(formatSpeedLabel(speakingRate[0]));
        rateLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.2f);
        rateLabel.setTypeface(AndroidUtilities.bold());
        rateLabel.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        root.addView(rateLabel);

        SeekBar rateSeek = new SeekBar(context);
        rateSeek.setMax(75); // 0.60x to 1.35x in 0.01 steps
        rateSeek.setProgress(Math.round((speakingRate[0] - 0.60f) * 100f));
        rateSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speakingRate[0] = 0.60f + (progress / 100f);
                rateLabel.setText(formatSpeedLabel(speakingRate[0]));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        LinearLayout.LayoutParams skLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        skLp.bottomMargin = AndroidUtilities.dp(8);
        root.addView(rateSeek, skLp);

        // 6. Status & Optional TTS Settings Fallback Row
        final TextView statusTv = new TextView(context);
        statusTv.setText("");
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        statusTv.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        statusTv.setVisibility(View.GONE);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.bottomMargin = AndroidUtilities.dp(6);
        root.addView(statusTv, stLp);

        final TextView ttsSettingsBtn = new TextView(context);
        ttsSettingsBtn.setText("Open Device Voice / TTS Settings");
        ttsSettingsBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        ttsSettingsBtn.setTypeface(AndroidUtilities.bold());
        ttsSettingsBtn.setGravity(Gravity.CENTER);
        ttsSettingsBtn.setTextColor(isDark ? 0xFF93C5FD : 0xFF1D4ED8);
        ttsSettingsBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
        GradientDrawable tsBg = new GradientDrawable();
        tsBg.setCornerRadius(AndroidUtilities.dp(10));
        tsBg.setColor(isDark ? 0xFF1E293B : 0xFFEFF6FF);
        tsBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFBFDBFE);
        ttsSettingsBtn.setBackground(tsBg);
        ttsSettingsBtn.setCompoundDrawablePadding(AndroidUtilities.dp(6));
        ttsSettingsBtn.setCompoundDrawablesWithIntrinsicBounds(
                NGVoiceIcons.createActionIconDrawable(NGVoiceIcons.ACTION_SETTINGS, AndroidUtilities.dp(15), isDark ? 0xFF93C5FD : 0xFF1D4ED8),
                null, null, null);
        ttsSettingsBtn.setVisibility(View.GONE);
        ttsSettingsBtn.setOnClickListener(v -> {
            try {
                Intent i = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                if (!(context instanceof Activity)) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Throwable ignored) {
                try {
                    Intent i2 = new Intent("com.android.settings.TTS_SETTINGS");
                    if (!(context instanceof Activity)) i2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i2);
                } catch (Throwable ignored2) {}
            }
        });
        LinearLayout.LayoutParams tsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tsLp.bottomMargin = AndroidUtilities.dp(8);
        root.addView(ttsSettingsBtn, tsLp);

        // 7. Action Buttons Row: Preview + Send Voice Note (with crisp custom vector icons)
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        final TextView previewBtn = new TextView(context);
        previewBtn.setText("Preview");
        previewBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        previewBtn.setTypeface(AndroidUtilities.bold());
        previewBtn.setGravity(Gravity.CENTER);
        previewBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0369A1);
        previewBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        previewBtn.setCompoundDrawablePadding(AndroidUtilities.dp(6));
        previewBtn.setCompoundDrawablesWithIntrinsicBounds(
                NGVoiceIcons.createActionIconDrawable(NGVoiceIcons.ACTION_PLAY, AndroidUtilities.dp(15), isDark ? 0xFF38BDF8 : 0xFF0369A1),
                null, null, null);
        GradientDrawable prevBg = new GradientDrawable();
        prevBg.setCornerRadius(AndroidUtilities.dp(13));
        prevBg.setColor(isDark ? 0xFF1E293B : 0xFFE0F2FE);
        prevBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFBAE6FD);
        previewBtn.setBackground(prevBg);
        actions.addView(previewBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final TextView sendBtn = new TextView(context);
        sendBtn.setText(chatActivity != null ? "Send Voice" : "Save Preview");
        sendBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        sendBtn.setTypeface(AndroidUtilities.bold());
        sendBtn.setGravity(Gravity.CENTER);
        sendBtn.setTextColor(Color.WHITE);
        sendBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        sendBtn.setCompoundDrawablePadding(AndroidUtilities.dp(7));
        sendBtn.setCompoundDrawablesWithIntrinsicBounds(
                NGVoiceIcons.createActionIconDrawable(NGVoiceIcons.ACTION_SEND_VOICE, AndroidUtilities.dp(16), Color.WHITE),
                null, null, null);
        GradientDrawable sendBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0EA5E9, 0xFF2563EB}
        );
        sendBg.setCornerRadius(AndroidUtilities.dp(13));
        sendBtn.setBackground(sendBg);
        LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        sendLp.leftMargin = AndroidUtilities.dp(8);
        actions.addView(sendBtn, sendLp);

        root.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(scroll);
        builder.setNegativeButton("Close", (d, w) -> NGVoiceDemoPlayer.stopPlayback());
        final AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> NGVoiceDemoPlayer.stopPlayback());

        View.OnClickListener openVoicePicker = v -> {
            NGVoiceDemoPlayer.stopPlayback();
            NGVoiceChangerManager.showDialog(context, () -> {
                voiceMode[0] = NGVoiceChangerManager.getSelectedMode(context);
                voiceLabel.setText(NGVoiceChangerManager.getModeTitle(voiceMode[0]));
                voiceBadge.setMode(voiceMode[0]);
            });
        };
        changeVoiceBtn.setOnClickListener(openVoicePicker);
        voiceRow.setOnClickListener(openVoicePicker);

        previewBtn.setOnClickListener(v -> {
            if (busy[0]) return;
            String rawText = input.getText() == null ? "" : input.getText().toString().trim();
            if (TextUtils.isEmpty(rawText)) {
                Toast.makeText(context, "Please type text first", Toast.LENGTH_SHORT).show();
                return;
            }
            busy[0] = true;
            ttsSettingsBtn.setVisibility(View.GONE);
            statusTv.setVisibility(View.VISIBLE);
            statusTv.setText(NGVoiceDemoPlayer.containsEmojisOrSymbols(rawText)
                    ? "Emojis skipped • Synthesizing natural voice..."
                    : "Synthesizing natural voice preview...");
            int m = voiceMode[0];
            NGVoiceDemoPlayer.synthesizeTextWithVoice(
                    context,
                    rawText,
                    selectedLang[0],
                    speakingRate[0],
                    m,
                    NGVoiceChangerManager.getCustomPitch(context, m),
                    NGVoiceChangerManager.getCustomSpeed(context, m),
                    NGVoiceChangerManager.getCustomFxMix(context, m),
                    NGVoiceChangerManager.isNoiseGateEnabled(context),
                    new NGVoiceDemoPlayer.PcmCallback() {
                        @Override
                        public void onSuccess(short[] pcm48k, long durationMs) {
                            statusTv.setText(String.format(Locale.US, "Playing preview (%.1fs)...", durationMs / 1000f));
                            NGVoiceDemoPlayer.playPcmAsync(pcm48k, () -> {
                                busy[0] = false;
                                statusTv.setText("Preview ready — tap Send Voice to share");
                            });
                        }

                        @Override
                        public void onError(String message) {
                            busy[0] = false;
                            statusTv.setText(message);
                            ttsSettingsBtn.setVisibility(View.VISIBLE);
                        }
                    }
            );
        });

        sendBtn.setOnClickListener(v -> {
            if (busy[0]) return;
            String rawText = input.getText() == null ? "" : input.getText().toString().trim();
            if (TextUtils.isEmpty(rawText)) {
                Toast.makeText(context, "Please type text first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (chatActivity == null) {
                previewBtn.performClick();
                return;
            }
            busy[0] = true;
            ttsSettingsBtn.setVisibility(View.GONE);
            statusTv.setVisibility(View.VISIBLE);
            statusTv.setText("Creating natural studio voice note...");
            NGVoiceDemoPlayer.stopPlayback();

            final int m = voiceMode[0];
            final int currentAccount = chatActivity.getCurrentAccount();
            final long dialogId = chatActivity.getDialogId();
            final MessageObject threadMsg = chatActivity.getThreadMessage();

            NGVoiceDemoPlayer.synthesizeTextWithVoice(
                    context,
                    rawText,
                    selectedLang[0],
                    speakingRate[0],
                    m,
                    NGVoiceChangerManager.getCustomPitch(context, m),
                    NGVoiceChangerManager.getCustomSpeed(context, m),
                    NGVoiceChangerManager.getCustomFxMix(context, m),
                    NGVoiceChangerManager.isNoiseGateEnabled(context),
                    new NGVoiceDemoPlayer.PcmCallback() {
                        @Override
                        public void onSuccess(short[] pcm48k, long durationMs) {
                            Utilities.globalQueue.postRunnable(() -> {
                                long localId = SharedConfig.getLastLocalId();
                                SharedConfig.saveConfig();
                                TLRPC.TL_document doc = new TLRPC.TL_document();
                                doc.file_reference = new byte[0];
                                doc.dc_id = Integer.MIN_VALUE;
                                doc.id = localId;
                                doc.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                                doc.mime_type = NGVoiceOpusWriter.MIME_TYPE;

                                File audioDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_AUDIO);
                                File outFile = new File(audioDir, System.currentTimeMillis() + "_" + FileLoader.getAttachFileName(doc));
                                NGVoiceOpusWriter.Result encoded = NGVoiceOpusWriter.encodePcmToOgg(
                                        outFile, pcm48k, NGVoiceDemoPlayer.SAMPLE_RATE);

                                AndroidUtilities.runOnUIThread(() -> {
                                    busy[0] = false;
                                    if (encoded == null || !encoded.file.exists()) {
                                        statusTv.setText("Could not encode voice note");
                                        Toast.makeText(context, "Could not encode voice message", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    doc.date = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
                                    doc.size = encoded.file.length();

                                    TLRPC.TL_documentAttributeAudio attr = new TLRPC.TL_documentAttributeAudio();
                                    attr.voice = true;
                                    attr.duration = encoded.durationMs / 1000.0;
                                    if (encoded.waveform != null && encoded.waveform.length > 0) {
                                        attr.waveform = encoded.waveform;
                                        attr.flags |= 4;
                                    }
                                    doc.attributes.add(attr);

                                    SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                                            doc,
                                            null,
                                            encoded.file.getAbsolutePath(),
                                            dialogId,
                                            threadMsg,
                                            threadMsg,
                                            null,
                                            null,
                                            null,
                                            null,
                                            true,
                                            0,
                                            0,
                                            0,
                                            null,
                                            null,
                                            false
                                    );
                                    SendMessagesHelper.getInstance(currentAccount).sendMessage(params);
                                    Toast.makeText(context, "Voice note sent", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                            });
                        }

                        @Override
                        public void onError(String message) {
                            busy[0] = false;
                            statusTv.setText(message);
                            ttsSettingsBtn.setVisibility(View.VISIBLE);
                        }
                    }
            );
        });

        try {
            if (context instanceof Activity && ((Activity) context).isFinishing()) {
                return;
            }
            dialog.show();
        } catch (Throwable ignored) {}
    }

    private static String formatSpeedLabel(float rate) {
        String tag;
        if (rate < 0.78f) tag = "Calm & Slow";
        else if (rate <= 0.94f) tag = "Natural Human";
        else if (rate <= 1.12f) tag = "Brisk";
        else tag = "Fast";
        return String.format(Locale.US, "Speaking Pace: %.2fx (%s)", rate, tag);
    }
}
