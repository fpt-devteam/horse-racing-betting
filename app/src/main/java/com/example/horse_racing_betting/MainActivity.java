package com.example.horse_racing_betting;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.horse_racing_betting.fragment.StartFragment;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;

public class MainActivity extends AppCompatActivity {
    private GameViewModel gameViewModel;
    private AudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameViewModel = new ViewModelProvider(this).get(GameViewModel.class);
        audioManager = AudioManager.getInstance(this);
        audioManager.startBgm();

        // Pause BGM during race; do NOT auto-resume here
        gameViewModel.getGameState().observe(this, state -> {
            if (GameViewModel.STATE_COUNTDOWN.equals(state) || GameViewModel.STATE_RUNNING.equals(state)) {
                audioManager.pauseBgm();
                if (GameViewModel.STATE_RUNNING.equals(state)) {
                    audioManager.startRaceSfx();
                } else {
                    audioManager.stopRaceSfx();
                }
            } else {
                // Not racing
                audioManager.stopRaceSfx();
            }
        });

        if (savedInstanceState == null) {
            replaceFragment(new StartFragment());
        }
    }

    public void replaceFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public GameViewModel getGameViewModel() {
        return gameViewModel;
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (audioManager != null) {
            String state = gameViewModel != null ? gameViewModel.getGameState().getValue() : null;
            if (GameViewModel.STATE_COUNTDOWN.equals(state) || GameViewModel.STATE_RUNNING.equals(state)) {
                audioManager.pauseBgm();
                // Ensure race SFX reflect the state
                if (GameViewModel.STATE_RUNNING.equals(state)) {
                    audioManager.startRaceSfx();
                } else {
                    audioManager.stopRaceSfx();
                }
                return;
            }

            // If the results dialog is visible, keep BGM paused until it is dismissed
            androidx.fragment.app.Fragment f = getSupportFragmentManager().findFragmentByTag("results");
            if (f != null && f.isVisible()) {
                audioManager.pauseBgm();
                audioManager.stopRaceSfx();
                return;
            }

            // Otherwise, resume as app returns to foreground (menus, etc.)
            audioManager.onAppForeground();
            audioManager.stopRaceSfx();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (audioManager != null) audioManager.onAppBackground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioManager != null && isFinishing()) {
            audioManager.release();
        }
    }

}