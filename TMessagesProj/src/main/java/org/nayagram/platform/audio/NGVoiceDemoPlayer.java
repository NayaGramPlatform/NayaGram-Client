package org.nayagram.platform.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.utils.recorder.audio.NGVoiceOpusWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NGVoiceDemoPlayer - Studio human-touch TTS synthesizer & preview player for NayaGram.
 *
 * Key upgrades:
 * - Strips all emojis, pictographs, dingbats, variation selectors, markdown, and URLs so TTS
 *   never reads emoji names aloud.
 * - Natural human speaking pace (default 0.86x) with sentence-level breathing pauses (210 ms)
 *   and warm studio acoustic polish (clean 95 Hz HPF, subtle 190 Hz chest warmth, smooth fades).
 * - Bulletproof 2-tier synthesis engine:
 *   Tier 1: Device Android TTS (prefers com.google.android.tts, exact bn-BD/bn-IN/bn & en matching,
 *           never silently falls back to English for Bangla text).
 *   Tier 2: Automatic Cloud Neural TTS fallback decoded natively via Android MediaExtractor +
 *           MediaCodec (MP3 -> 48 kHz mono PCM) + NGSonic speed control, so Bangla & English
 *           always work even on devices without offline Bangla TTS packs.
 */
public final class NGVoiceDemoPlayer {

    public static final int SAMPLE_RATE = 48_000;
    public static final int LANG_AUTO = 0;
    public static final int LANG_ENGLISH = 1;
    public static final int LANG_BANGLA = 2;
    public static final float DEFAULT_HUMAN_SPEED = 0.86f;

    private static final String GOOGLE_TTS_PACKAGE = "com.google.android.tts";
    private static final String ENGLISH_DEMO_TEXT =
            "Hello! Welcome to NayaGram Voice Studio. This is your live voice preview.";

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "NGVoiceDemoWorker");
        t.setDaemon(true);
        return t;
    });

    private static final Object PLAY_LOCK = new Object();
    private static final Object CACHE_LOCK = new Object();

    private static volatile AudioTrack activeTrack;
    private static final AtomicInteger playGeneration = new AtomicInteger(0);
    private static short[] cachedEnglishDemo48k;

    private NGVoiceDemoPlayer() {}

    public interface PcmCallback {
        void onSuccess(@NonNull short[] pcm48k, long durationMs);
        void onError(@NonNull String message);
    }

    public interface StatusCallback {
        void onStatus(boolean playing, @Nullable String error);
    }

    /**
     * Strips emojis, pictographs, dingbats, ZWJ/variation selectors, markdown markers, and URLs
     * while preserving Bengali Unicode letters/vowel signs, Latin letters, digits, and natural
     * speech punctuation so Text-to-Voice sounds like a real human speaker.
     */
    @NonNull
    public static String sanitizeForNaturalSpeech(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        // Remove raw URLs so TTS doesn't read "https colon slash slash..."
        String noUrls = raw.replaceAll("https?://\\S+|www\\.\\S+", " ");

        StringBuilder sb = new StringBuilder(noUrls.length());
        int len = noUrls.length();
        for (int i = 0; i < len; ) {
            int cp = noUrls.codePointAt(i);
            i += Character.charCount(cp);

            // Always preserve Bengali script block (letters + vowel signs + halant + chandrabindu)
            // plus Bengali danda (।) / double danda (॥) and ZWNJ used inside Bengali conjuncts
            if ((cp >= 0x0980 && cp <= 0x09FF) || cp == 0x0964 || cp == 0x0965 || cp == 0x200C) {
                sb.appendCodePoint(cp);
                continue;
            }

            // Explicitly skip emoji, dingbat, variation selector, ZWJ, keycap, tag & symbol blocks
            if (isEmojiOrUnwantedSymbol(cp)) {
                sb.append(' ');
                continue;
            }

            if (cp == '\n' || cp == '\r') {
                sb.append(", ");
                continue;
            }

            if (Character.isLetterOrDigit(cp) || cp == ' ' || cp == '\t') {
                sb.appendCodePoint(cp);
                continue;
            }

            int type = Character.getType(cp);
            if (type == Character.NON_SPACING_MARK || type == Character.COMBINING_SPACING_MARK) {
                // Keep non-emoji diacritics / combining accents, skip enclosing keycap (0x20E3)
                if (cp != 0x20E3) {
                    sb.appendCodePoint(cp);
                }
                continue;
            }

            // Keep only natural speaking punctuation that guides human intonation & pauses
            if (cp == '.' || cp == ',' || cp == '?' || cp == '!' || cp == ';'
                    || cp == ':' || cp == '\'' || cp == '-' || cp == '\u2019') {
                sb.appendCodePoint(cp);
            } else {
                sb.append(' ');
            }
        }

        String cleaned = sb.toString();
        // Normalize Bengali danda spacing & collapse repeated punctuation for natural rhythm
        cleaned = cleaned.replace("\u0965", "\u0964 ");
        cleaned = cleaned.replaceAll("[?]{2,}", "?");
        cleaned = cleaned.replaceAll("[!]{2,}", "!");
        cleaned = cleaned.replaceAll("\\.{2,}", ", ");
        cleaned = cleaned.replaceAll(",\\s*,+", ", ");
        cleaned = cleaned.replaceAll("\\s+([,.?!;:\u0964])", "$1");
        cleaned = cleaned.replaceAll("([,.?!;:\u0964])([^\\s])", "$1 $2");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        // Trim dangling leading/trailing punctuation-only fragments
        cleaned = cleaned.replaceAll("^[\\s,;:\\-]+", "").trim();
        return cleaned;
    }

    public static boolean containsEmojisOrSymbols(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) return false;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (isEmojiOrUnwantedSymbol(cp)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isEmojiOrUnwantedSymbol(int cp) {
        if (cp == 0x200D || cp == 0x20E3 || cp == 0xFE0E || cp == 0xFE0F) return true;
        if (cp >= 0x1F000 && cp <= 0x1FFFF) return true;
        if (cp >= 0x2600 && cp <= 0x27BF) return true;
        if (cp >= 0x2300 && cp <= 0x23FF) return true;
        if (cp >= 0x2B00 && cp <= 0x2BFF) return true;
        if (cp >= 0x2190 && cp <= 0x21FF) return true;
        if (cp >= 0xE0000 && cp <= 0xE007F) return true;
        if (cp == '*' || cp == '_' || cp == '`' || cp == '~' || cp == '#' || cp == '|' || cp == '^') return true;
        int type = Character.getType(cp);
        return type == Character.OTHER_SYMBOL
                || type == Character.MODIFIER_SYMBOL
                || type == Character.SURROGATE
                || type == Character.PRIVATE_USE;
    }

    /**
     * Plays the instant English voice demo processed through the chosen Voice Changer character.
     */
    public static void playEnglishDemo(
            @NonNull Context context,
            int mode,
            float pitchSemitones,
            float speed,
            float fxMix,
            boolean noiseGate,
            @Nullable StatusCallback callback
    ) {
        final Context appCtx = context.getApplicationContext();
        final int gen = playGeneration.incrementAndGet();
        stopActiveTrackOnly();

        WORKER.execute(() -> {
            if (gen != playGeneration.get()) return;
            short[] basePcm = getOrSynthesizeEnglishDemo(appCtx);
            if (gen != playGeneration.get()) return;
            if (basePcm == null || basePcm.length == 0) {
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() ->
                            callback.onStatus(false, "Could not generate voice preview. Check connection or TTS."));
                }
                return;
            }

            short[] processed = NGVoiceChangerManager.processOfflinePcm(
                    basePcm, SAMPLE_RATE, mode, pitchSemitones, speed, fxMix, noiseGate);
            if (mode == NGVoiceChangerManager.MODE_NORMAL) {
                processed = applyHumanTouchPolish(processed, true);
            }
            if (gen != playGeneration.get()) return;

            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onStatus(true, null));
            }
            playPcmBlocking(processed, gen);
            if (callback != null) {
                AndroidUtilities.runOnUIThread(() -> callback.onStatus(false, null));
            }
        });
    }

    /**
     * Synthesizes user text (English or Bangla, emojis cleanly removed, natural human pacing)
     * and applies the selected Voice Changer character to produce 48 kHz mono PCM.
     */
    public static void synthesizeTextWithVoice(
            @NonNull Context context,
            @NonNull String text,
            int langMode,
            float speechRate,
            int voiceMode,
            float pitchSemitones,
            float voiceSpeed,
            float fxMix,
            boolean noiseGate,
            @NonNull PcmCallback callback
    ) {
        final Context appCtx = context.getApplicationContext();
        final String cleaned = sanitizeForNaturalSpeech(text);
        if (cleaned.isEmpty()) {
            AndroidUtilities.runOnUIThread(() ->
                    callback.onError("Please type words to speak (emojis are skipped for natural voice)."));
            return;
        }
        final String trimmed = cleaned.length() > 800 ? cleaned.substring(0, 800) : cleaned;
        final Locale locale = chooseLocale(trimmed, langMode);
        final float humanRate = Math.max(0.58f, Math.min(1.45f, speechRate));

        WORKER.execute(() -> {
            short[] raw48k = synthesizeNaturalSpeech48k(appCtx, trimmed, locale, humanRate);
            if (raw48k == null || raw48k.length == 0) {
                AndroidUtilities.runOnUIThread(() ->
                        callback.onError("Could not synthesize speech right now. Connect to internet or install Bangla/English voice in TTS settings."));
                return;
            }
            // Add warm studio human-touch polish before voice character processing
            short[] polished = applyHumanTouchPolish(raw48k, voiceMode == NGVoiceChangerManager.MODE_NORMAL);
            short[] processed = NGVoiceChangerManager.processOfflinePcm(
                    polished, SAMPLE_RATE, voiceMode, pitchSemitones, voiceSpeed, fxMix, noiseGate);
            if (processed.length == 0) {
                processed = polished;
            }
            long durationMs = Math.max(500L, (processed.length * 1000L) / SAMPLE_RATE);
            final short[] finalPcm = processed;
            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(finalPcm, durationMs));
        });
    }

    public static void playPcmAsync(@NonNull short[] pcm48k, @Nullable Runnable onDone) {
        final int gen = playGeneration.incrementAndGet();
        stopActiveTrackOnly();
        WORKER.execute(() -> {
            if (gen != playGeneration.get()) return;
            playPcmBlocking(pcm48k, gen);
            if (onDone != null) {
                AndroidUtilities.runOnUIThread(onDone);
            }
        });
    }

    public static void stopPlayback() {
        playGeneration.incrementAndGet();
        stopActiveTrackOnly();
    }

    private static void stopActiveTrackOnly() {
        synchronized (PLAY_LOCK) {
            AudioTrack track = activeTrack;
            activeTrack = null;
            if (track != null) {
                try {
                    if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                        track.pause();
                        track.flush();
                    }
                    track.stop();
                } catch (Throwable ignored) {}
                try {
                    track.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static void playPcmBlocking(@NonNull short[] pcm48k, int gen) {
        if (pcm48k.length == 0 || gen != playGeneration.get()) return;
        int minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        int bufBytes = Math.max(minBuf * 2, 16384);

        AudioTrack track = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(bufBytes)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build();
            } else {
                track = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufBytes,
                        AudioTrack.MODE_STREAM
                );
            }
            if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                return;
            }
            synchronized (PLAY_LOCK) {
                if (gen != playGeneration.get()) {
                    track.release();
                    return;
                }
                activeTrack = track;
            }
            track.play();
            int offset = 0;
            final int chunk = 2048;
            while (offset < pcm48k.length && gen == playGeneration.get()) {
                int count = Math.min(chunk, pcm48k.length - offset);
                int written = track.write(pcm48k, offset, count);
                if (written <= 0) break;
                offset += written;
            }
            if (gen == playGeneration.get()) {
                try {
                    Thread.sleep(160);
                } catch (InterruptedException ignored) {}
            }
        } catch (Throwable ignored) {
        } finally {
            synchronized (PLAY_LOCK) {
                if (activeTrack == track) {
                    activeTrack = null;
                }
            }
            if (track != null) {
                try {
                    track.stop();
                } catch (Throwable ignored) {}
                try {
                    track.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    @Nullable
    private static short[] getOrSynthesizeEnglishDemo(@NonNull Context context) {
        short[] cached = cachedEnglishDemo48k;
        if (cached != null && cached.length > 0) {
            return cached;
        }
        synchronized (CACHE_LOCK) {
            if (cachedEnglishDemo48k != null && cachedEnglishDemo48k.length > 0) {
                return cachedEnglishDemo48k;
            }
            short[] synth = synthesizeNaturalSpeech48k(context, ENGLISH_DEMO_TEXT, Locale.US, 0.90f);
            if (synth != null && synth.length > 0) {
                cachedEnglishDemo48k = synth;
            }
            return synth;
        }
    }

    @NonNull
    public static Locale chooseLocale(@NonNull String text, int langMode) {
        if (langMode == LANG_ENGLISH) {
            return Locale.US;
        }
        if (langMode == LANG_BANGLA) {
            return new Locale("bn", "BD");
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0980 && c <= 0x09FF) {
                return new Locale("bn", "BD");
            }
        }
        return Locale.US;
    }

    /**
     * Synthesizes natural multi-sentence speech using Tier 1 (Device TTS with strict language check)
     * and seamless Tier 2 (Cloud Neural TTS decoded via MediaExtractor+MediaCodec + NGSonic speed).
     */
    @Nullable
    private static short[] synthesizeNaturalSpeech48k(
            @NonNull Context context,
            @NonNull String cleanedText,
            @NonNull Locale locale,
            float speechRate
    ) {
        boolean isBangla = "bn".equalsIgnoreCase(locale.getLanguage());
        List<String> sentences = splitIntoNaturalChunks(cleanedText, 165);
        if (sentences.isEmpty()) return null;

        // Tier 1: Device Android TTS (prefers Google TTS engine, strict language verification)
        short[] devicePcm = synthesizeChunksWithDeviceTts(context, sentences, locale, speechRate, isBangla);
        if (isAudibleSpeech(devicePcm)) {
            return devicePcm;
        }

        // Tier 2: Seamless Cloud Neural TTS fallback (never fails on devices missing Bangla TTS pack!)
        short[] cloudPcm = synthesizeChunksWithCloudNeuralTts(context, sentences, isBangla ? "bn" : "en", speechRate);
        if (isAudibleSpeech(cloudPcm)) {
            return cloudPcm;
        }

        return null;
    }

    /**
     * Splits text on sentence boundaries (., ।, ?, !) or safe word boundaries under maxLen chars
     * so each phrase gets natural intonation and a human breathing pause between sentences.
     */
    @NonNull
    private static List<String> splitIntoNaturalChunks(@NonNull String text, int maxLen) {
        List<String> chunks = new ArrayList<>();
        String[] rawSentences = text.split("(?<=[.?!\u0964])\\s+");
        for (String s : rawSentences) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            if (trimmed.length() <= maxLen) {
                chunks.add(trimmed);
            } else {
                // Split long sentence gently on commas or word boundaries
                String rem = trimmed;
                while (rem.length() > maxLen) {
                    int cut = rem.lastIndexOf(',', maxLen);
                    if (cut < maxLen / 2) {
                        cut = rem.lastIndexOf(' ', maxLen);
                    }
                    if (cut <= 0) {
                        cut = maxLen;
                    }
                    String part = rem.substring(0, cut).trim();
                    if (!part.isEmpty()) chunks.add(part);
                    rem = rem.substring(cut).trim();
                }
                if (!rem.isEmpty()) chunks.add(rem);
            }
        }
        return chunks;
    }

    @Nullable
    private static short[] synthesizeChunksWithDeviceTts(
            @NonNull Context context,
            @NonNull List<String> chunks,
            @NonNull Locale locale,
            float speechRate,
            boolean strictLanguageMatch
    ) {
        // Try Google TTS explicitly first (OEM engines on Xiaomi/Samsung often lack Bangla)
        short[] res = runDeviceTtsEngine(context, chunks, locale, speechRate, strictLanguageMatch, GOOGLE_TTS_PACKAGE);
        if (isAudibleSpeech(res)) {
            return res;
        }
        // Fallback to system default TTS engine
        return runDeviceTtsEngine(context, chunks, locale, speechRate, strictLanguageMatch, null);
    }

    @Nullable
    private static short[] runDeviceTtsEngine(
            @NonNull Context context,
            @NonNull List<String> chunks,
            @NonNull Locale locale,
            float speechRate,
            boolean strictLanguageMatch,
            @Nullable String enginePackage
    ) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) return null;

        final CountDownLatch initLatch = new CountDownLatch(1);
        final AtomicInteger initStatus = new AtomicInteger(TextToSpeech.ERROR);
        final AtomicReference<TextToSpeech> ttsRef = new AtomicReference<>();

        try {
            TextToSpeech tts = enginePackage != null
                    ? new TextToSpeech(context.getApplicationContext(), status -> {
                        initStatus.set(status);
                        initLatch.countDown();
                    }, enginePackage)
                    : new TextToSpeech(context.getApplicationContext(), status -> {
                        initStatus.set(status);
                        initLatch.countDown();
                    });
            ttsRef.set(tts);

            if (!initLatch.await(4, TimeUnit.SECONDS) || initStatus.get() != TextToSpeech.SUCCESS) {
                return null;
            }

            if (!configureBestVoice(tts, locale, strictLanguageMatch)) {
                return null;
            }

            // Human speaking rate & subtle natural warmth pitch
            tts.setSpeechRate(Math.max(0.58f, Math.min(1.45f, speechRate)));
            tts.setPitch(0.98f);

            List<short[]> parts = new ArrayList<>(chunks.size());
            for (String chunk : chunks) {
                short[] part = synthesizeSingleUtteranceToWav(tts, cacheDir, chunk);
                if (!isAudibleSpeech(part)) {
                    return null;
                }
                parts.add(trimSilence(part, 45, 70));
            }
            return joinChunksWithHumanPause(parts, 210);
        } catch (Throwable ignored) {
            return null;
        } finally {
            TextToSpeech tts = ttsRef.get();
            if (tts != null) {
                try {
                    tts.stop();
                    tts.shutdown();
                } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean configureBestVoice(
            @NonNull TextToSpeech tts,
            @NonNull Locale requested,
            boolean strictLanguageMatch
    ) {
        String targetLang = requested.getLanguage();
        boolean isBangla = "bn".equalsIgnoreCase(targetLang);

        // Try selecting the highest-quality matching Voice on API 21+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Set<Voice> voices = tts.getVoices();
                if (voices != null && !voices.isEmpty()) {
                    Voice best = null;
                    int bestScore = Integer.MIN_VALUE;
                    for (Voice v : voices) {
                        if (v == null || v.getLocale() == null) continue;
                        if (!targetLang.equalsIgnoreCase(v.getLocale().getLanguage())) continue;
                        if (v.getFeatures() != null && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) {
                            continue;
                        }
                        int score = v.getQuality();
                        if (!v.isNetworkConnectionRequired()) score += 100; // reliable offline voice
                        if (requested.getCountry().equalsIgnoreCase(v.getLocale().getCountry())) score += 50;
                        if (score > bestScore) {
                            bestScore = score;
                            best = v;
                        }
                    }
                    if (best != null && tts.setVoice(best) == TextToSpeech.SUCCESS) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Standard locale fallback chain within the SAME language
        Locale[] candidates = isBangla
                ? new Locale[]{new Locale("bn", "BD"), new Locale("bn", "IN"), new Locale("bn")}
                : new Locale[]{Locale.US, Locale.UK, Locale.ENGLISH};

        for (Locale cand : candidates) {
            int r = tts.setLanguage(cand);
            if (r >= TextToSpeech.LANG_AVAILABLE) {
                return true;
            }
        }

        // CRITICAL: Never send Bengali text to an English voice (causes error or silent WAV!)
        return !strictLanguageMatch && tts.setLanguage(Locale.US) >= TextToSpeech.LANG_AVAILABLE;
    }

    @Nullable
    private static short[] synthesizeSingleUtteranceToWav(
            @NonNull TextToSpeech tts,
            @NonNull File cacheDir,
            @NonNull String text
    ) {
        File wavFile = new File(cacheDir, "ng_tts_" + UUID.randomUUID().toString() + ".wav");
        final CountDownLatch doneLatch = new CountDownLatch(1);
        final AtomicReference<Boolean> ok = new AtomicReference<>(Boolean.FALSE);
        final String uttId = "ng_utt_" + System.nanoTime();

        try {
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String id) {}

                @Override
                public void onDone(String id) {
                    if (uttId.equals(id)) {
                        ok.set(Boolean.TRUE);
                        doneLatch.countDown();
                    }
                }

                @Override
                public void onError(String id) {
                    if (uttId.equals(id)) doneLatch.countDown();
                }

                @Override
                public void onError(String id, int errorCode) {
                    if (uttId.equals(id)) doneLatch.countDown();
                }
            });

            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "true");
            if (tts.synthesizeToFile(text, params, wavFile, uttId) != TextToSpeech.SUCCESS) {
                return null;
            }
            if (!doneLatch.await(14, TimeUnit.SECONDS) || !Boolean.TRUE.equals(ok.get())) {
                return null;
            }
            if (!wavFile.exists() || wavFile.length() <= 44) {
                return null;
            }
            return readWavToPcm48k(wavFile);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try {
                if (wavFile.exists()) wavFile.delete();
            } catch (Throwable ignored) {}
        }
    }

    /**
     * Tier 2: Cloud Neural TTS fallback decoded natively to 48 kHz mono PCM via Android
     * MediaExtractor + MediaCodec, with exact human speaking-speed shaping via NGSonic.
     */
    @Nullable
    private static short[] synthesizeChunksWithCloudNeuralTts(
            @NonNull Context context,
            @NonNull List<String> chunks,
            @NonNull String langCode,
            float speechRate
    ) {
        File cacheDir = context.getCacheDir();
        if (cacheDir == null) return null;

        List<short[]> parts = new ArrayList<>(chunks.size());
        for (String chunk : chunks) {
            short[] pcm = downloadAndDecodeCloudTtsChunk(cacheDir, chunk, langCode);
            if (!isAudibleSpeech(pcm)) {
                return null;
            }
            // Apply natural speaking pace via NGSonic when speechRate differs from 1.0x
            if (Math.abs(speechRate - 1.0f) > 0.03f) {
                pcm = applySpeedOnly(pcm, speechRate);
            }
            parts.add(trimSilence(pcm, 40, 65));
        }
        return joinChunksWithHumanPause(parts, 210);
    }

    @Nullable
    private static short[] downloadAndDecodeCloudTtsChunk(
            @NonNull File cacheDir,
            @NonNull String chunk,
            @NonNull String langCode
    ) {
        String[] clients = new String[]{"tw-ob", "gtx"};
        for (String client : clients) {
            File mp3File = new File(cacheDir, "ng_ctts_" + UUID.randomUUID().toString() + ".mp3");
            HttpURLConnection conn = null;
            try {
                String encoded = URLEncoder.encode(chunk, "UTF-8");
                String urlStr = "https://translate.google.com/translate_tts?ie=UTF-8&client="
                        + client + "&tl=" + langCode + "&ttsspeed=1&q=" + encoded;
                conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(10000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "audio/mpeg, audio/*;q=0.9, */*;q=0.5");

                int code = conn.getResponseCode();
                if (code != 200) continue;

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(mp3File)) {
                    byte[] buf = new byte[8192];
                    int total = 0;
                    int r;
                    while ((r = in.read(buf)) > 0) {
                        out.write(buf, 0, r);
                        total += r;
                        if (total > 4 * 1024 * 1024) break;
                    }
                }
                if (mp3File.exists() && mp3File.length() > 128) {
                    short[] decoded = decodeCompressedAudioToPcm48k(mp3File);
                    if (isAudibleSpeech(decoded)) {
                        return decoded;
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Throwable ignored) {}
                }
                try {
                    if (mp3File.exists()) mp3File.delete();
                } catch (Throwable ignored) {}
            }
        }
        return null;
    }

    /**
     * Decodes any Android-supported compressed audio file (MP3/AAC/OGG) into 48 kHz mono PCM
     * using built-in MediaExtractor + MediaCodec.
     */
    @Nullable
    private static short[] decodeCompressedAudioToPcm48k(@NonNull File file) {
        MediaExtractor extractor = null;
        MediaCodec codec = null;
        try {
            extractor = new MediaExtractor();
            extractor.setDataSource(file.getAbsolutePath());

            int audioTrack = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrack = i;
                    format = f;
                    break;
                }
            }
            if (audioTrack < 0 || format == null) return null;

            extractor.selectTrack(audioTrack);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime == null) return null;

            int srcSampleRate = format.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? format.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 24_000;
            int srcChannels = format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null, null, 0);
            codec.start();

            ByteArrayOutputStream pcmBytes = new ByteArrayOutputStream(65536);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            boolean outputDone = false;
            long deadline = System.currentTimeMillis() + 10_000L;

            while (!outputDone && System.currentTimeMillis() < deadline) {
                if (!inputDone) {
                    int inIdx = codec.dequeueInputBuffer(10_000);
                    if (inIdx >= 0) {
                        ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                        if (inBuf != null) {
                            inBuf.clear();
                            int sampleSize = extractor.readSampleData(inBuf, 0);
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                inputDone = true;
                            } else {
                                long pts = extractor.getSampleTime();
                                codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0);
                                extractor.advance();
                            }
                        }
                    }
                }

                int outIdx = codec.dequeueOutputBuffer(info, 10_000);
                if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat outFmt = codec.getOutputFormat();
                    if (outFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        srcSampleRate = outFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    }
                    if (outFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        srcChannels = outFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                } else if (outIdx >= 0) {
                    ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                    if (outBuf != null && info.size > 0) {
                        outBuf.position(info.offset);
                        outBuf.limit(info.offset + info.size);
                        byte[] chunk = new byte[info.size];
                        outBuf.get(chunk);
                        pcmBytes.write(chunk);
                    }
                    codec.releaseOutputBuffer(outIdx, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputDone = true;
                    }
                }
            }

            byte[] raw = pcmBytes.toByteArray();
            if (raw.length < 4) return null;

            int totalSamples = raw.length / 2;
            int frames = totalSamples / Math.max(1, srcChannels);
            if (frames <= 0) return null;

            short[] mono = new short[frames];
            ByteBuffer bb = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            for (int f = 0; f < frames; f++) {
                int acc = 0;
                for (int ch = 0; ch < srcChannels; ch++) {
                    acc += bb.getShort();
                }
                mono[f] = (short) (acc / srcChannels);
            }
            return NGVoiceOpusWriter.resampleLinear(mono, srcSampleRate, SAMPLE_RATE);
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (Throwable ignored) {}
                try {
                    codec.release();
                } catch (Throwable ignored) {}
            }
            if (extractor != null) {
                try {
                    extractor.release();
                } catch (Throwable ignored) {}
            }
        }
    }

    @NonNull
    private static short[] applySpeedOnly(@NonNull short[] pcm48k, float speed) {
        if (pcm48k.length == 0) return pcm48k;
        try {
            NGSonic sonic = new NGSonic(SAMPLE_RATE, 1, speed, 1.0f, SAMPLE_RATE);
            sonic.queueInput(ShortBuffer.wrap(pcm48k));
            sonic.queueEndOfStream();
            int outBytes = sonic.getOutputSize();
            int outSamples = outBytes >> 1;
            if (outSamples <= 0) return pcm48k;
            short[] out = new short[outSamples];
            sonic.getOutput(ShortBuffer.wrap(out));
            return out;
        } catch (Throwable ignored) {
            return pcm48k;
        }
    }

    @NonNull
    private static short[] joinChunksWithHumanPause(@NonNull List<short[]> parts, int pauseMs) {
        if (parts.size() == 1) return parts.get(0);
        int pauseSamples = (SAMPLE_RATE * pauseMs) / 1000;
        int total = 0;
        for (int i = 0; i < parts.size(); i++) {
            total += parts.get(i).length;
            if (i + 1 < parts.size()) total += pauseSamples;
        }
        short[] out = new short[total];
        int pos = 0;
        for (int i = 0; i < parts.size(); i++) {
            short[] p = parts.get(i);
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
            if (i + 1 < parts.size()) {
                pos += pauseSamples; // zero-initialized silence pause
            }
        }
        return out;
    }

    @NonNull
    private static short[] trimSilence(@NonNull short[] pcm, int leadKeepMs, int tailKeepMs) {
        if (pcm.length == 0) return pcm;
        final int threshold = 180;
        int first = 0;
        while (first < pcm.length && Math.abs((int) pcm[first]) < threshold) first++;
        int last = pcm.length - 1;
        while (last > first && Math.abs((int) pcm[last]) < threshold) last--;
        if (first >= last) return pcm;

        int leadKeep = (SAMPLE_RATE * leadKeepMs) / 1000;
        int tailKeep = (SAMPLE_RATE * tailKeepMs) / 1000;
        int start = Math.max(0, first - leadKeep);
        int end = Math.min(pcm.length, last + tailKeep + 1);
        short[] out = new short[end - start];
        System.arraycopy(pcm, start, out, 0, out.length);
        return out;
    }

    /**
     * Adds subtle human-studio acoustic polish: clean 95 Hz HPF, warm chest presence (190 Hz),
     * gentle de-harshing at 3.9 kHz, smooth raised-cosine fade-in/out, and natural breath cushion.
     */
    @NonNull
    private static short[] applyHumanTouchPolish(@NonNull short[] input, boolean applyWarmEq) {
        if (input.length == 0) return input;
        short[] trimmed = trimSilence(input, 80, 120);
        if (trimmed.length == 0) return input;

        float[] f = new float[trimmed.length];
        float peak = 0.001f;
        for (int i = 0; i < trimmed.length; i++) {
            f[i] = trimmed[i] / 32768f;
        }

        if (applyWarmEq) {
            SimpleBiquad hpf = SimpleBiquad.highPass(SAMPLE_RATE, 95.0, 0.707);
            SimpleBiquad warm = SimpleBiquad.peaking(SAMPLE_RATE, 195.0, 0.9, 1.8);
            SimpleBiquad smooth = SimpleBiquad.peaking(SAMPLE_RATE, 3900.0, 1.1, -1.3);
            for (int i = 0; i < f.length; i++) {
                float v = smooth.process(warm.process(hpf.process(f[i])));
                f[i] = v;
                float a = Math.abs(v);
                if (a > peak) peak = a;
            }
        } else {
            for (int i = 0; i < f.length; i++) {
                float a = Math.abs(f[i]);
                if (a > peak) peak = a;
            }
        }

        // Gentle normalization to -1.8 dBFS so preview & voice notes always sound confident & clear
        float normGain = Math.min(1.8f, 0.80f / peak);
        int fadeSamples = Math.min(f.length / 4, (SAMPLE_RATE * 18) / 1000);
        short[] out = new short[f.length];
        for (int i = 0; i < f.length; i++) {
            float env = 1f;
            if (fadeSamples > 0) {
                if (i < fadeSamples) {
                    env = 0.5f * (1f - (float) Math.cos(Math.PI * i / fadeSamples));
                } else if (i >= f.length - fadeSamples) {
                    int rem = f.length - 1 - i;
                    env = 0.5f * (1f - (float) Math.cos(Math.PI * rem / fadeSamples));
                }
            }
            float v = f[i] * normGain * env;
            if (v > 0.96f) v = 0.96f;
            else if (v < -0.96f) v = -0.96f;
            out[i] = (short) Math.round(v * 32767f);
        }
        return out;
    }

    private static boolean isAudibleSpeech(@Nullable short[] pcm) {
        if (pcm == null || pcm.length < 800) return false;
        double sumSq = 0.0;
        int step = Math.max(1, pcm.length / 2000);
        int count = 0;
        for (int i = 0; i < pcm.length; i += step) {
            double v = pcm[i] / 32768.0;
            sumSq += v * v;
            count++;
        }
        double rms = Math.sqrt(sumSq / Math.max(1, count));
        return rms > 0.0025;
    }

    @Nullable
    private static short[] readWavToPcm48k(@NonNull File file) {
        byte[] data;
        try (FileInputStream fis = new FileInputStream(file)) {
            int len = (int) file.length();
            if (len < 44 || len > 32 * 1024 * 1024) return null;
            data = new byte[len];
            int pos = 0;
            while (pos < len) {
                int r = fis.read(data, pos, len - pos);
                if (r <= 0) break;
                pos += r;
            }
            if (pos < 44) return null;
        } catch (IOException e) {
            return null;
        }

        if (data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F'
                || data[8] != 'W' || data[9] != 'A' || data[10] != 'V' || data[11] != 'E') {
            return null;
        }

        int channels = 1;
        int sampleRate = 22050;
        int bitsPerSample = 16;
        int dataOffset = -1;
        int dataSize = 0;

        int offset = 12;
        while (offset + 8 <= data.length) {
            int chunkId = ((data[offset] & 0xFF) << 24)
                    | ((data[offset + 1] & 0xFF) << 16)
                    | ((data[offset + 2] & 0xFF) << 8)
                    | (data[offset + 3] & 0xFF);
            int chunkSize = readLeInt(data, offset + 4);
            int payload = offset + 8;
            if (chunkSize < 0 || payload > data.length) break;

            if (chunkId == 0x666d7420 && payload + 16 <= data.length) {
                channels = Math.max(1, readLeShort(data, payload + 2));
                sampleRate = Math.max(8000, readLeInt(data, payload + 4));
                bitsPerSample = readLeShort(data, payload + 14);
            } else if (chunkId == 0x64617461) {
                dataOffset = payload;
                dataSize = Math.min(chunkSize, data.length - payload);
                break;
            }
            int next = payload + chunkSize + (chunkSize & 1);
            if (next <= offset) break;
            offset = next;
        }

        if (dataOffset < 0 || dataSize <= 0) return null;

        int bytesPerSample = Math.max(1, bitsPerSample / 8);
        int frameBytes = bytesPerSample * channels;
        int frameCount = dataSize / frameBytes;
        if (frameCount <= 0) return null;

        short[] mono = new short[frameCount];
        for (int f = 0; f < frameCount; f++) {
            int base = dataOffset + f * frameBytes;
            int acc = 0;
            for (int ch = 0; ch < channels; ch++) {
                int p = base + ch * bytesPerSample;
                if (bitsPerSample == 16) {
                    short s = (short) ((data[p] & 0xFF) | ((data[p + 1] & 0xFF) << 8));
                    acc += s;
                } else if (bitsPerSample == 8) {
                    int s = ((data[p] & 0xFF) - 128) << 8;
                    acc += s;
                }
            }
            mono[f] = (short) (acc / channels);
        }

        return NGVoiceOpusWriter.resampleLinear(mono, sampleRate, SAMPLE_RATE);
    }

    private static int readLeShort(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8);
    }

    private static int readLeInt(byte[] b, int o) {
        return (b[o] & 0xFF)
                | ((b[o + 1] & 0xFF) << 8)
                | ((b[o + 2] & 0xFF) << 16)
                | ((b[o + 3] & 0xFF) << 24);
    }

    private static final class SimpleBiquad {
        private final float b0, b1, b2, a1, a2;
        private float x1, x2, y1, y2;

        private SimpleBiquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = (float) (b0 / a0);
            this.b1 = (float) (b1 / a0);
            this.b2 = (float) (b2 / a0);
            this.a1 = (float) (a1 / a0);
            this.a2 = (float) (a2 / a0);
        }

        static SimpleBiquad highPass(int sr, double freq, double q) {
            double w0 = 2.0 * Math.PI * freq / sr;
            double cosW = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new SimpleBiquad((1.0 + cosW) * 0.5, -(1.0 + cosW), (1.0 + cosW) * 0.5,
                    1.0 + alpha, -2.0 * cosW, 1.0 - alpha);
        }

        static SimpleBiquad peaking(int sr, double freq, double q, double gainDb) {
            double a = Math.pow(10.0, gainDb / 40.0);
            double w0 = 2.0 * Math.PI * freq / sr;
            double cosW = Math.cos(w0);
            double alpha = Math.sin(w0) / (2.0 * q);
            return new SimpleBiquad(1.0 + alpha * a, -2.0 * cosW, 1.0 - alpha * a,
                    1.0 + alpha / a, -2.0 * cosW, 1.0 - alpha / a);
        }

        float process(float x) {
            float y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }
}
