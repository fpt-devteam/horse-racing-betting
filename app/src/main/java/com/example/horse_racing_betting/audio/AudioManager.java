package com.example.horse_racing_betting.audio;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.os.Build;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RawRes;

import com.example.horse_racing_betting.R;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Random;

public class AudioManager {
    private static final String PREFS_NAME = "AudioPrefs";
    private static final String KEY_MUTE_SFX = "mute_sfx";
    private static final String KEY_MUTE_BGM = "mute_bgm";

    private static AudioManager instance;

    private final Context appContext;
    private final SharedPreferences prefs;

    private SoundPool soundPool;
    private final Map<Integer, Integer> sfxMap = new HashMap<>();
    private final Set<Integer> loadedSfxIds = new HashSet<>();

    private MediaPlayer bgmPlayer;
    private boolean muteSfx;
    private boolean muteBgm;

    // AudioFocus & ducking
    private android.media.AudioManager systemAudioManager;
    private android.media.AudioManager.OnAudioFocusChangeListener focusChangeListener;
    private Object audioFocusRequest; // android.media.AudioFocusRequest for API >= 26
    private boolean hasAudioFocus = false;
    private boolean wasPlayingBeforeLoss = false;
    private float currentBgmVolumeMultiplier = 1f;
    private float currentSfxVolumeMultiplier = 1f;

    // Race SFX
    private boolean raceSfxActive = false;
    private Integer gallopStreamId = null; // SoundPool stream id for looping gallop
    private final Handler sfxHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable randomVocalTask = new Runnable() {
        @Override
        public void run() {
            if (!raceSfxActive) return;
            // Randomly play a horse vocal
            int choice = random.nextBoolean() ? R.raw.horse_whinny : R.raw.horse_neigh;
            playSfx(choice);
            scheduleNextVocal();
        }
    };

    private AudioManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.muteSfx = prefs.getBoolean(KEY_MUTE_SFX, false);
        this.muteBgm = prefs.getBoolean(KEY_MUTE_BGM, false);
        this.systemAudioManager = (android.media.AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        initFocusListener();
        initSoundPool();
        preloadDefaultSfx();
        prepareBgm(R.raw.bgm);
    }

    public static synchronized AudioManager getInstance(Context context) {
        if (instance == null) {
            instance = new AudioManager(context);
        }
        return instance;
    }

    private void initSoundPool() {
    loadedSfxIds.clear();
    sfxMap.clear();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attrs)
                .build();

        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0) {
                loadedSfxIds.add(sampleId);
            }
        });
    }

    private void preloadDefaultSfx() {
        // Map logical ids to raw resources
        loadSfx(R.raw.mouse_click);
        loadSfx(R.raw.race_start_beeps);
        loadSfx(R.raw.horse_whinny);
        loadSfx(R.raw.fanfare);
        // For race ambience
        loadSfx(R.raw.horse_galloping);
        loadSfx(R.raw.horse_neigh);
    }

    public void loadSfx(@RawRes int resId) {
        if (soundPool == null) {
            initSoundPool();
        }
        int id = soundPool.load(appContext, resId, 1);
        sfxMap.put(resId, id);
    }
    private void prepareBgm(@RawRes int resId) {
        releaseBgm();
        try {
            android.content.res.AssetFileDescriptor afd = appContext.getResources().openRawResourceFd(resId);
            if (afd == null) return;

            MediaPlayer mp = new MediaPlayer();
            // Set attributes BEFORE setting data source / prepare
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();

            // Manual loop is more reliable across devices/codecs than setLooping alone
            mp.setOnCompletionListener(player -> {
                try {
                    player.seekTo(0);
                    if (!muteBgm && hasAudioFocus) {
                        player.start();
                    }
                } catch (Exception ignored) {}
            });

            // Auto-recover if decoder/server dies
            mp.setOnErrorListener((player, what, extra) -> {
                // Recreate the player cleanly
                prepareBgm(resId);
                return true;
            });

            mp.prepare();
            bgmPlayer = mp;

            if (!muteBgm && hasAudioFocus) {
                try {
                    float vol = clamp01(currentBgmVolumeMultiplier);
                    bgmPlayer.setVolume(vol, vol);
                    bgmPlayer.start();
                } catch (Exception ignored) {
                    // recreate on bad state
                    prepareBgm(resId);
                }
            }
        } catch (Exception e) {
            // As a fallback, give up this cycle; next startBgm() can retry
            bgmPlayer = null;
        }
    }


    private void releaseBgm() {
        if (bgmPlayer != null) {
            try { bgmPlayer.stop(); } catch (Exception ignored) {}
            bgmPlayer.release();
            bgmPlayer = null;
        }
    }

    public void playSfx(@RawRes int resId) {
        if (muteSfx) return;
        if (soundPool == null) {
            initSoundPool();
        }
        Integer id = sfxMap.get(resId);
        if (id == null) {
            loadSfx(resId);
            id = sfxMap.get(resId);
        }
        if (id != null && loadedSfxIds.contains(id)) {
            float vol = clamp01(currentSfxVolumeMultiplier);
            soundPool.play(id, vol, vol, 1, 0, 1f);
        }
    }

    public void startBgm() {
        if (muteBgm) return;
        if (!requestAudioFocus()) {
            // GAIN will call us back via focus listener
            wasPlayingBeforeLoss = true;
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
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
    }

    public void stopBgm() {
        if (bgmPlayer != null) {
            try { bgmPlayer.stop(); } catch (Exception ignored) {}
            try { bgmPlayer.release(); } catch (Exception ignored) {}
            bgmPlayer = null;
        }
        abandonAudioFocus();
    }

    public boolean isMuteSfx() { return muteSfx; }
    public boolean isMuteBgm() { return muteBgm; }

    public void setMuteSfx(boolean mute) {
        this.muteSfx = mute;
        prefs.edit().putBoolean(KEY_MUTE_SFX, mute).apply();
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

    public void onAppForeground() {
        startBgm();
    }

    public void onAppBackground() {
        pauseBgm();
        abandonAudioFocus();
        // Ensure looping SFX are stopped when app backgrounds
        stopRaceSfx();
    }

    public void release() {
        releaseBgm();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        loadedSfxIds.clear();
        sfxMap.clear();
        abandonAudioFocus();
    }

    // -------- Audio Focus & Ducking --------
    private void initFocusListener() {
        focusChangeListener = focusChange -> {
            switch (focusChange) {
                case android.media.AudioManager.AUDIOFOCUS_GAIN:
                    hasAudioFocus = true;
                    setDucking(false);
                    if (!muteBgm && wasPlayingBeforeLoss) {
                        startBgm();
                    }
                    wasPlayingBeforeLoss = false;
                    break;

                case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Lower volume
                    setDucking(true);
                    break;
                case android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    // Pause but remember
                    wasPlayingBeforeLoss = isBgmPlaying();
                    pauseBgm();
                    setDucking(false);
                    break;
                case android.media.AudioManager.AUDIOFOCUS_LOSS:
                    // Stop/pause and do not auto-resume
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
            android.media.AudioFocusRequest afr = new android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .setWillPauseWhenDucked(false)
                    .build();
            audioFocusRequest = afr;
            result = systemAudioManager.requestAudioFocus(afr);
        } else {
            // Deprecated path for older devices
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
        if (systemAudioManager == null) return;
        if (!hasAudioFocus) return;
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
        currentBgmVolumeMultiplier = duck ? 0.2f : 1f;
        currentSfxVolumeMultiplier = duck ? 0.5f : 1f;
        if (bgmPlayer != null) {
            float vol = clamp01(currentBgmVolumeMultiplier);
            try { bgmPlayer.setVolume(vol, vol); } catch (Exception ignored) {}
        }
    }

    private float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    // -------- Race SFX control (gallop loop + random vocals) --------
    public void startRaceSfx() {
        raceSfxActive = true;
        // Start looping gallop if not already playing
        if (gallopStreamId == null) {
            Integer gallopSampleId = sfxMap.get(R.raw.horse_galloping);
            if (gallopSampleId == null) {
                loadSfx(R.raw.horse_galloping);
                gallopSampleId = sfxMap.get(R.raw.horse_galloping);
            }
            if (gallopSampleId != null && loadedSfxIds.contains(gallopSampleId) && !muteSfx) {
                float vol = clamp01(currentSfxVolumeMultiplier);
                gallopStreamId = soundPool.play(gallopSampleId, vol, vol, 1, -1, 1f);
            } else {
                // Retry shortly until loaded
                sfxHandler.postDelayed(this::startRaceSfx, 150);
            }
        }

        // Schedule vocal effects if not already scheduled
        sfxHandler.removeCallbacks(randomVocalTask);
        scheduleNextVocal();
    }

    public void stopRaceSfx() {
        raceSfxActive = false;
        // Stop gallop loop
        if (gallopStreamId != null && soundPool != null) {
            try { soundPool.stop(gallopStreamId); } catch (Exception ignored) {}
        }
        gallopStreamId = null;
        // Cancel scheduled vocals
        sfxHandler.removeCallbacks(randomVocalTask);
    }

    private void scheduleNextVocal() {
        if (!raceSfxActive) return;
        // Random delay between 2s and 6s
        int delayMs = 2000 + random.nextInt(4000);
        sfxHandler.postDelayed(randomVocalTask, delayMs);
    }
}
