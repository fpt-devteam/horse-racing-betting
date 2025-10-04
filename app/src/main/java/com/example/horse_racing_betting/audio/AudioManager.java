package com.example.horse_racing_betting.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RawRes;

import com.example.horse_racing_betting.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class AudioManager {

    // -------------------- Prefs --------------------
    private static final String PREFS_NAME   = "AudioPrefs";
    private static final String KEY_MUTE_SFX = "mute_sfx";
    private static final String KEY_MUTE_BGM = "mute_bgm";

    // -------------------- SoundPool / SFX --------------------
    private static final int   MAX_STREAMS      = 6;
    private static final float SFX_DUCK_VOLUME  = 0.5f;
    private static final float SFX_FULL_VOLUME  = 1.0f;

    // -------------------- BGM --------------------
    private static final float BGM_DUCK_VOLUME  = 0.2f;
    private static final float BGM_FULL_VOLUME  = 1.0f;

    // -------------------- Race ambience --------------------
    private static final int VOCAL_DELAY_MIN_MS   = 2000; // 2s
    private static final int VOCAL_DELAY_SPAN_MS  = 4000; // -> 2..6s
    private static final int GALLOP_RETRY_MS      = 150;

    // -------------------- Singleton --------------------
    private static volatile AudioManager instance;

    public static AudioManager getInstance(Context context) {
        AudioManager local = instance;
        if (local == null) {
            synchronized (AudioManager.class) {
                local = instance;
                if (local == null) {
                    instance = local = new AudioManager(context.getApplicationContext());
                }
            }
        }
        return local;
    }

    // -------------------- Fields --------------------
    private final Context appContext;
    private final SharedPreferences prefs;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private SoundPool soundPool;
    private final Map<Integer, Integer> sfxMap = new HashMap<>(); // resId -> sampleId
    private final Set<Integer> loadedSfxIds = new HashSet<>();

    private MediaPlayer bgmPlayer;

    private boolean muteSfx;
    private boolean muteBgm;

    // System audio focus
    private android.media.AudioManager systemAudioManager;
    private android.media.AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private Object audioFocusRequest; // AudioFocusRequest on API >= 26
    private boolean hasAudioFocus = false;
    private boolean wasPlayingBeforeLoss = false;

    private float currentBgmVolumeMultiplier = BGM_FULL_VOLUME;
    private float currentSfxVolumeMultiplier = SFX_FULL_VOLUME;

    // Race SFX state
    private boolean raceSfxActive = false;
    private Integer gallopStreamId = null; // looping gallop stream id

    private final Runnable randomVocalTask = new Runnable() {
        @Override public void run() {
            if (!raceSfxActive) return;
            playSfx(random.nextBoolean() ? R.raw.horse_whinny : R.raw.horse_neigh);
            scheduleNextVocal();
        }
    };

    // -------------------- Init --------------------
    private AudioManager(Context context) {
        this.appContext = context;
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        this.muteSfx = prefs.getBoolean(KEY_MUTE_SFX, false);
        this.muteBgm = prefs.getBoolean(KEY_MUTE_BGM, false);

        this.systemAudioManager =
                (android.media.AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

        initFocusListener();
        initSoundPool();
        preloadDefaultSfx();
        prepareBgm(R.raw.bgm);
    }

    private void initSoundPool() {
        loadedSfxIds.clear();
        sfxMap.clear();

        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) loadedSfxIds.add(sampleId);
        });
    }

    private void ensureSoundPool() {
        if (soundPool == null) initSoundPool();
    }

    private void preloadDefaultSfx() {
        loadSfx(R.raw.mouse_click);
        loadSfx(R.raw.race_start_beeps);
        loadSfx(R.raw.horse_whinny);
        loadSfx(R.raw.fanfare);
        loadSfx(R.raw.horse_galloping);
        loadSfx(R.raw.horse_neigh);
    }

    // -------------------- SFX --------------------
    public void loadSfx(@RawRes int resId) {
        ensureSoundPool();
        if (sfxMap.containsKey(resId)) return;
        int sampleId = soundPool.load(appContext, resId, 1);
        sfxMap.put(resId, sampleId);
    }

    private Integer getOrLoadSfxId(@RawRes int resId) {
        ensureSoundPool();
        Integer id = sfxMap.get(resId);
        if (id == null) {
            loadSfx(resId);
            id = sfxMap.get(resId);
        }
        return id;
    }

    private void playIfLoaded(int sampleId, int loop) {
        if (loadedSfxIds.contains(sampleId)) {
            float vol = clamp01(currentSfxVolumeMultiplier);
            soundPool.play(sampleId, vol, vol, 1, loop, 1f);
        }
    }

    public void playSfx(@RawRes int resId) {
        if (muteSfx) return;
        Integer id = getOrLoadSfxId(resId);
        if (id != null) playIfLoaded(id, 0);
    }

    // -------------------- BGM --------------------
    private void prepareBgm(@RawRes int resId) {
        releaseBgm();
        try {
            android.content.res.AssetFileDescriptor afd =
                    appContext.getResources().openRawResourceFd(resId);
            if (afd == null) return;

            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // Robust looping
            mp.setOnCompletionListener(player -> {
                try {
                    player.seekTo(0);
                    if (!muteBgm && hasAudioFocus) player.start();
                } catch (Exception ignored) {}
            });

            // Auto-recover if decoder/server dies
            mp.setOnErrorListener((player, what, extra) -> {
                prepareBgm(resId);
                return true;
            });

            mp.prepare();
            bgmPlayer = mp;

            if (!muteBgm && hasAudioFocus) {
                float vol = clamp01(currentBgmVolumeMultiplier);
                bgmPlayer.setVolume(vol, vol);
                bgmPlayer.start();
            }
        } catch (Exception e) {
            bgmPlayer = null; // next startBgm() will retry
        }
    }

    private void releaseBgm() {
        if (bgmPlayer != null) {
            try { bgmPlayer.stop(); } catch (Exception ignored) {}
            try { bgmPlayer.release(); } catch (Exception ignored) {}
            bgmPlayer = null;
        }
    }

    public void startBgm() {
        if (muteBgm) return;
        if (!requestAudioFocus()) {
            wasPlayingBeforeLoss = true; // will resume on focus gain
            return;
        }
        if (bgmPlayer == null) {
            prepareBgm(R.raw.bgm);
            return;
        }
        if (!bgmPlayer.isPlaying()) {
            try {
                float vol = clamp01(currentBgmVolumeMultiplier);
                bgmPlayer.setVolume(vol, vol);
                bgmPlayer.start();
            } catch (IllegalStateException e) {
                prepareBgm(R.raw.bgm);
            }
        }
    }

    public void pauseBgm() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) bgmPlayer.pause();
    }

    public void stopBgm() {
        releaseBgm();
        abandonAudioFocus();
    }

    // -------------------- Mute --------------------
    public boolean isMuteSfx() { return muteSfx; }
    public boolean isMuteBgm() { return muteBgm; }

    public void setMuteSfx(boolean mute) {
        this.muteSfx = mute;
        prefs.edit().putBoolean(KEY_MUTE_SFX, mute).apply();
        if (mute) stopRaceSfx();
    }

    public void setMuteBgm(boolean mute) {
        this.muteBgm = mute;
        prefs.edit().putBoolean(KEY_MUTE_BGM, mute).apply();
        if (mute) {
            pauseBgm();
            abandonAudioFocus();
        } else {
            startBgm();
        }
    }

    // -------------------- App lifecycle hooks --------------------
    public void onAppForeground() { startBgm(); }
    public void onAppBackground() {
        pauseBgm();
        abandonAudioFocus();
        stopRaceSfx(); // ensure loops stop in background
    }

    public void release() {
        stopRaceSfx();
        releaseBgm();
        if (soundPool != null) {
            try { soundPool.release(); } catch (Exception ignored) {}
            soundPool = null;
        }
        loadedSfxIds.clear();
        sfxMap.clear();
        abandonAudioFocus();
    }

    // -------------------- Audio Focus & ducking --------------------
    private void initFocusListener() {
        focusChangeListener = focusChange -> {
            switch (focusChange) {
                case android.media.AudioManager.AUDIOFOCUS_GAIN:
                    hasAudioFocus = true;
                    setDucking(false);
                    if (!muteBgm && wasPlayingBeforeLoss) startBgm();
                    wasPlayingBeforeLoss = false;
                    break;

                case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    setDucking(true);
                    break;

                case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    wasPlayingBeforeLoss = isBgmPlaying();
                    pauseBgm();
                    setDucking(false);
                    break;

                case android.media.AudioManager.AUDIOFOCUS_LOSS:
                    wasPlayingBeforeLoss = false;
                    pauseBgm();
                    setDucking(false);
                    abandonAudioFocus();
                    break;
            }
        };
    }

    private boolean requestAudioFocus() {
        if (systemAudioManager == null) return true; // best effort
        if (hasAudioFocus) return true;

        int result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();

            android.media.AudioFocusRequest afr =
                    new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(attrs)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(focusChangeListener)
                            .setWillPauseWhenDucked(false)
                            .build();

            audioFocusRequest = afr;
            result = systemAudioManager.requestAudioFocus(afr);
        } else {
            result = systemAudioManager.requestAudioFocus(
                    focusChangeListener,
                    android.media.AudioManager.STREAM_MUSIC,
                    android.media.AudioManager.AUDIOFOCUS_GAIN
            );
        }
        hasAudioFocus = (result == android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
        return hasAudioFocus;
    }

    private void abandonAudioFocus() {
        if (systemAudioManager == null || !hasAudioFocus) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (audioFocusRequest instanceof android.media.AudioFocusRequest) {
                    systemAudioManager.abandonAudioFocusRequest((android.media.AudioFocusRequest) audioFocusRequest);
                }
            } else {
                systemAudioManager.abandonAudioFocus(focusChangeListener);
            }
        } catch (Exception ignored) {}
        hasAudioFocus = false;
    }

    private boolean isBgmPlaying() {
        return bgmPlayer != null && bgmPlayer.isPlaying();
    }

    private void setDucking(boolean duck) {
        currentBgmVolumeMultiplier = duck ? BGM_DUCK_VOLUME : BGM_FULL_VOLUME;
        currentSfxVolumeMultiplier = duck ? SFX_DUCK_VOLUME : SFX_FULL_VOLUME;
        if (bgmPlayer != null) {
            float vol = clamp01(currentBgmVolumeMultiplier);
            try { bgmPlayer.setVolume(vol, vol); } catch (Exception ignored) {}
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    // -------------------- Race ambience (loop + random vocals) --------------------
    public void startRaceSfx() {
        if (muteSfx) return;
        raceSfxActive = true;

        // Start/ensure gallop loop
        if (gallopStreamId == null) {
            Integer gallopSampleId = getOrLoadSfxId(R.raw.horse_galloping);
            if (gallopSampleId != null && loadedSfxIds.contains(gallopSampleId)) {
                float vol = clamp01(currentSfxVolumeMultiplier);
                gallopStreamId = soundPool.play(gallopSampleId, vol, vol, 1, -1, 1f);
            } else {
                // Retry shortly until loaded
                mainHandler.postDelayed(this::startRaceSfx, GALLOP_RETRY_MS);
            }
        }

        // Schedule vocal effects
        mainHandler.removeCallbacks(randomVocalTask);
        scheduleNextVocal();
    }

    public void stopRaceSfx() {
        raceSfxActive = false;

        if (gallopStreamId != null && soundPool != null) {
            try { soundPool.stop(gallopStreamId); } catch (Exception ignored) {}
        }
        gallopStreamId = null;

        mainHandler.removeCallbacks(randomVocalTask);
    }

    private void scheduleNextVocal() {
        if (!raceSfxActive) return;
        int delayMs = VOCAL_DELAY_MIN_MS + random.nextInt(VOCAL_DELAY_SPAN_MS);
        mainHandler.postDelayed(randomVocalTask, delayMs);
    }
}
