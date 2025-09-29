package com.example.horse_racing_betting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.viewmodel.GameViewModel;

import java.util.ArrayList;
import java.util.List;

public class RaceFragment extends Fragment {
    private GameViewModel gameViewModel;
    private LinearLayout countdownOverlay;
    private TextView tvCountdown;
    private TextView tvRaceStatus;
    private SeekBar seekBar1, seekBar2, seekBar3, seekBar4;
    private List<SeekBar> seekBars;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
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
        tvRaceStatus = view.findViewById(R.id.tvRaceStatus);

        seekBar1 = view.findViewById(R.id.seekBar1);
        seekBar2 = view.findViewById(R.id.seekBar2);
        seekBar3 = view.findViewById(R.id.seekBar3);
        seekBar4 = view.findViewById(R.id.seekBar4);

        seekBars = new ArrayList<>();
        seekBars.add(seekBar1);
        seekBars.add(seekBar2);
        seekBars.add(seekBar3);
        seekBars.add(seekBar4);
    }

    private void setupObservers() {
        gameViewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            updateRaceStatus(state);

            if (GameViewModel.STATE_RESULT.equals(state)) {
                // Navigate to results after a short delay
                tvRaceStatus.postDelayed(() -> {
                    if (getActivity() != null) {
                        ((MainActivity) requireActivity()).replaceFragment(new ResultFragment());
                    }
                }, 2000);
            }
        });

        gameViewModel.getCountdown().observe(getViewLifecycleOwner(), countdown -> {
            if (countdown != null) {
                if (countdown > 0) {
                    countdownOverlay.setVisibility(View.VISIBLE);
                    tvCountdown.setText(String.valueOf(countdown));
                } else {
                    countdownOverlay.setVisibility(View.GONE);
                }
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
        if (horses.size() != seekBars.size()) return;

        for (int i = 0; i < horses.size(); i++) {
            Horse horse = horses.get(i);
            SeekBar seekBar = seekBars.get(i);

            // Update SeekBar progress (0-100 maps directly to horse position)
            int progress = (int) Math.min(horse.getPosition(), 100.0f);
            seekBar.setProgress(progress);

            // Add visual feedback for running horses
            if (GameViewModel.STATE_RUNNING.equals(gameViewModel.getGameState().getValue()) && !horse.isFinished()) {
                // Slightly animate the SeekBar for visual effect
                seekBar.animate()
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        seekBar.animate()
                            .scaleY(1.0f)
                            .setDuration(100);
                    });
            }
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references
        if (seekBars != null) {
            seekBars.clear();
        }
    }
}