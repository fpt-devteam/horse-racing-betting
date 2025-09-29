package com.example.horse_racing_betting.fragment;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;
import com.example.horse_racing_betting.skin.SkinManager;

import java.util.ArrayList;
import java.util.List;

public class RaceFragment extends Fragment {
    private GameViewModel gameViewModel;
    private LinearLayout countdownOverlay;
    private TextView tvCountdown;
    private TextView tvLapCounter;
    private TextView tvRaceStatus;
    private ImageView horse1, horse2, horse3, horse4;
    private List<ImageView> horseViews;
    private List<ObjectAnimator> horseAnimators;
    private SkinManager skinManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
    skinManager = SkinManager.getInstance(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_race, container, false);
        initViews(view);
        setupObservers();
        return view;
    }

    private void initViews(View view) {
        countdownOverlay = view.findViewById(R.id.countdownOverlay);
        tvCountdown = view.findViewById(R.id.tvCountdown);
        tvLapCounter = view.findViewById(R.id.tvLapCounter);
        tvRaceStatus = view.findViewById(R.id.tvRaceStatus);

        horse1 = view.findViewById(R.id.horse1);
        horse2 = view.findViewById(R.id.horse2);
        horse3 = view.findViewById(R.id.horse3);
        horse4 = view.findViewById(R.id.horse4);

        horseViews = new ArrayList<>();
        horseViews.add(horse1);
        horseViews.add(horse2);
        horseViews.add(horse3);
        horseViews.add(horse4);

        // Apply skins
        for (int i = 0; i < horseViews.size(); i++) {
            skinManager.applyHorseIcon(horseViews.get(i), i + 1);
        }

        horseAnimators = new ArrayList<>();
        for (ImageView horse : horseViews) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(horse, "translationX", 0f, 0f);
            horseAnimators.add(animator);
        }
    }

    private void setupObservers() {
        gameViewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            updateRaceStatus(state);

            if (GameViewModel.STATE_RESULT.equals(state)) {
                // Play finish fanfare
                ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.fanfare);
                // Navigate to results after a short delay
                tvRaceStatus.postDelayed(() -> {
                    if (getActivity() != null) {
                        // Show results as a dialog
                        ResultFragment dialog = new ResultFragment();
                        dialog.show(getParentFragmentManager(), "results");
                    }
                }, 2000);
            }
        });

        gameViewModel.getCountdown().observe(getViewLifecycleOwner(), countdown -> {
            if (countdown != null) {
//                ((MainActivity) requireActivity()).getAudioManager().stopBgm();
                ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.race_start_beeps);

                if (countdown > 0) {
                    countdownOverlay.setVisibility(View.VISIBLE);
                    tvCountdown.setText(String.valueOf(countdown));
                    // Beep during countdown
                } else {
                    countdownOverlay.setVisibility(View.GONE);
                    // Start sound when countdown ends
                    ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.horse_whinny);
                }
            }
        });

        gameViewModel.getCurrentLap().observe(getViewLifecycleOwner(), lap -> {
            if (lap != null) {
                tvLapCounter.setText(lap);
            }
        });

        gameViewModel.getHorses().observe(getViewLifecycleOwner(), horses -> {
            if (horses != null) {
                updateHorsePositions(horses);
            }
        });
    }

    private void updateRaceStatus(String state) {
        switch (state) {
            case GameViewModel.STATE_COUNTDOWN:
                tvRaceStatus.setText("Countdown State");
                tvRaceStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_timer, 0, 0, 0);
                break;
            case GameViewModel.STATE_RUNNING:
                tvRaceStatus.setText("Race In Progress");
                tvRaceStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_timer, 0, 0, 0);
                break;
            case GameViewModel.STATE_RESULT:
                tvRaceStatus.setText("Race Finished!");
                tvRaceStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_timer, 0, 0, 0);
                break;
            default:
                tvRaceStatus.setText("Preparing Race");
                break;
        }
    }

    private void updateHorsePositions(List<Horse> horses) {
        if (horses.size() != horseViews.size()) return;

        for (int i = 0; i < horses.size(); i++) {
            Horse horse = horses.get(i);
            ImageView horseView = horseViews.get(i);

            // Calculate position based on track width (leaving space for finish line)
            float trackWidth = getResources().getDisplayMetrics().widthPixels - 200; // Account for margins and finish line
            float maxPosition = 300.0f; // Max position in the simulation
            float translationX = (horse.getPosition() / maxPosition) * trackWidth;

            // Clamp to track bounds
            translationX = Math.min(translationX, trackWidth);
            translationX = Math.max(translationX, 0);

            // Animate horse movement smoothly
            ObjectAnimator animator = horseAnimators.get(i);
            if (animator != null) {
                animator.cancel();
            }

            animator = ObjectAnimator.ofFloat(horseView, "translationX", horseView.getTranslationX(), translationX);
            animator.setDuration(100);
            animator.start();
            horseAnimators.set(i, animator);

            // Add slight rotation for movement effect
            if (GameViewModel.STATE_RUNNING.equals(gameViewModel.getGameState().getValue())) {
                horseView.animate()
                    .rotation(horse.getPosition() % 2 == 0 ? 2f : -2f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        horseView.animate().rotation(0f).setDuration(100);
                    });
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up animators
        for (ObjectAnimator animator : horseAnimators) {
            if (animator != null) {
                animator.cancel();
            }
        }
        horseAnimators.clear();
    }
}