package com.example.horse_racing_betting.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;

public class SettingsFragment extends DialogFragment {
    private GameViewModel gameViewModel;
    private SwitchCompat switchSoundEffects, switchBackgroundMusic;
    private Button btnCancel, btnRestart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        switchSoundEffects = view.findViewById(R.id.switchSoundEffects);
        switchBackgroundMusic = view.findViewById(R.id.switchBackgroundMusic);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnRestart = view.findViewById(R.id.btnRestart);

        // Initialize switches with persisted states
        AudioManager am = ((MainActivity) requireActivity()).getAudioManager();
        switchSoundEffects.setChecked(!am.isMuteSfx());
        switchBackgroundMusic.setChecked(!am.isMuteBgm());
        switchSoundEffects.setText(am.isMuteSfx() ? "Off" : "On");
        switchBackgroundMusic.setText(am.isMuteBgm() ? "Off" : "On");
    }

    private void setupClickListeners() {
        switchSoundEffects.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AudioManager am = ((MainActivity) requireActivity()).getAudioManager();
            am.setMuteSfx(!isChecked);
            switchSoundEffects.setText(isChecked ? "On" : "Off");
            am.playSfx(R.raw.mouse_click);
        });

        switchBackgroundMusic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AudioManager am = ((MainActivity) requireActivity()).getAudioManager();
            am.setMuteBgm(!isChecked);
            switchBackgroundMusic.setText(isChecked ? "On" : "Off");
            if (isChecked) am.playSfx(R.raw.mouse_click);
        });

        btnCancel.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
            dismiss();
        });

        btnRestart.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);

            // Full game reset - clears username, sets coins to 100, enables first run
            gameViewModel.resetGame();

            Toast.makeText(requireContext(), "Game reset! Please enter your username again.", Toast.LENGTH_LONG).show();
            dismiss();

            // Navigate back to start screen which will show username entry
            ((MainActivity) requireActivity()).replaceFragment(new StartFragment());
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}