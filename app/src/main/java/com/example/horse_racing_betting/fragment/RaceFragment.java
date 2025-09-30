package com.example.horse_racing_betting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;
import com.example.horse_racing_betting.skin.SkinManager;
import com.example.horse_racing_betting.ui.graphics.FrameSequenceDrawable;

import java.util.ArrayList;
import java.util.List;

public class RaceFragment extends Fragment {
    private GameViewModel gameViewModel;
    private LinearLayout countdownOverlay;
    private TextView tvCountdown;
    private TextView tvRaceStatus;
    private SeekBar seekBar1, seekBar2, seekBar3, seekBar4;
    private List<SeekBar> seekBars;
    private SkinManager skinManager;
    private android.graphics.drawable.Animatable[] animThumbs;
    private android.graphics.drawable.Drawable[] idleThumbs;
    private boolean isPlayingCountdown;

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

        isPlayingCountdown = false;

        // Build animated thumbs from per-frame images (fallback to static icons if not found)
        setupHorseThumbs();
    }

    private void setupObservers() {
        gameViewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            updateRaceStatus(state);

            if (GameViewModel.STATE_RESULT.equals(state)) {
                stopGallop();
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
            } else if (GameViewModel.STATE_RUNNING.equals(state)) {
                startGallop();
            } else if (GameViewModel.STATE_COUNTDOWN.equals(state)) {
                stopGallop();
            }
        });

        gameViewModel.getCountdown().observe(getViewLifecycleOwner(), countdown -> {

            if (countdown != null) {
                if (!isPlayingCountdown) {
                    ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.race_start_beeps);
                }
                isPlayingCountdown = true;
                if (countdown > 0) {
                    // Display countdown numbers (5, 4, 3, 2, 1)
                    countdownOverlay.setVisibility(View.VISIBLE);
                    tvCountdown.setText(String.valueOf(countdown));
                    // Play beep sound
                } else {
                    // Display "Go!" when countdown reaches 0
                    countdownOverlay.setVisibility(View.VISIBLE);
                    tvCountdown.setText("Go!");
                    // Play start sound
//                    ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.horse_whinny);
                    // Hide overlay after showing "Go!" briefly
                    tvCountdown.postDelayed(() -> {
                        if (countdownOverlay != null) {
                            countdownOverlay.setVisibility(View.GONE);
                        }
                    }, 1000);
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
                // Ensure this horse's thumb shows running animation and is started
                if (animThumbs != null && i < animThumbs.length && animThumbs[i] != null) {
                    android.graphics.drawable.Animatable a = animThumbs[i];
                    if (a instanceof android.graphics.drawable.Drawable) {
                        seekBar.setThumb((android.graphics.drawable.Drawable) a);
                    }
                    if (!a.isRunning()) a.start();
                }
                // Slightly animate the SeekBar for visual effect
                seekBar.animate()
                    .scaleY(1.1f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        seekBar.animate()
                            .scaleY(1.0f)
                            .setDuration(100);
                    });
            } else if (horse.isFinished()) {
                // Stop this horse's animation when finished
                if (animThumbs != null && i < animThumbs.length && animThumbs[i] != null) {
                    if (animThumbs[i].isRunning()) animThumbs[i].stop();
                }
                // Swap back to idle thumb for this lane
                if (idleThumbs != null && i < idleThumbs.length && idleThumbs[i] != null) {
                    seekBar.setThumb(idleThumbs[i]);
                }
            }
        }
    }

    private void setupHorseThumbs() {
        animThumbs = new android.graphics.drawable.Animatable[seekBars.size()];
        idleThumbs = new android.graphics.drawable.Drawable[seekBars.size()];
        // Map each horse lane to a frame prefix. You can change this mapping as you like.
        String[] folders = new String[]{
                "black_horse",  // lane 1
                "yellow_horse", // lane 2
                "brown_horse",  // lane 3
                "white_horse"   // lane 4
        };

        for (int i = 0; i < seekBars.size(); i++) {
            SeekBar sb = seekBars.get(i);
            String prefix = i < folders.length ? folders[i] : folders[0];
            int idleId = resolveIdleFrameId(prefix);
            int[] animIds = resolveAnimFrameIds(prefix);

            if (idleId != 0 && animIds.length > 0) {
                // Create idle (tile00) drawable and running sequence (tile01..tile06)
                FrameSequenceDrawable idle = new FrameSequenceDrawable(getResources(), new int[]{idleId}, 1000).prepare();
                FrameSequenceDrawable running = new FrameSequenceDrawable(getResources(), animIds, 60).prepare();

                // Size the thumb ~lane height (fallback 48dp)
                int laneH = sb.getHeight();
                int dp48 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
                int size = laneH > 0 ? Math.max(dp48, laneH) : dp48;
                idle.setDesiredSizePx(size, size);
                running.setDesiredSizePx(size, size);

                // Start in IDLE
                sb.setThumb(idle);
                idleThumbs[i] = idle;
                animThumbs[i] = running;

                // If height not ready yet, adjust later for both
                if (laneH <= 0) {
                    sb.post(() -> {
                        int h2 = sb.getHeight();
                        int sizeLater = h2 > 0 ? Math.max(dp48, h2) : dp48;
                        idle.setDesiredSizePx(sizeLater, sizeLater);
                        running.setDesiredSizePx(sizeLater, sizeLater);
                        sb.invalidate();
                    });
                }
            } else {
                // Fallback to static icon
                int horseNumber = i + 1;
                android.graphics.drawable.Drawable fallback = getResources().getDrawable(skinManager.getHorseIconRes(horseNumber), requireContext().getTheme());
                sb.setThumb(fallback);
                idleThumbs[i] = fallback;
                animThumbs[i] = null;
            }
        }
    }

    // Resolve idle frame: <prefix>_tile00
    private int resolveIdleFrameId(String prefix) {
        String name = String.format("%s_tile%02d", prefix, 0); // e.g., black_horse_tile00
        return getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
    }

    // Resolve animation frames: <prefix>_tile01 .. <prefix>_tile06
    private int[] resolveAnimFrameIds(String prefix) {
        List<Integer> ids = new ArrayList<>();
        for (int f = 1; f <= 6; f++) { // exactly frames 01..06 as specified
            String name = String.format("%s_tile%02d", prefix, f);
            int id = getResources().getIdentifier(name, "drawable", requireContext().getPackageName());
            if (id != 0) ids.add(id);
        }
        int[] arr = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) arr[i] = ids.get(i);
        return arr;
    }

    private void startGallop() {
        if (animThumbs == null) return;
        for (int i = 0; i < animThumbs.length; i++) {
            android.graphics.drawable.Animatable a = animThumbs[i];
            if (a != null) {
                // swap to running drawable if needed
                if (a instanceof android.graphics.drawable.Drawable && i < seekBars.size()) {
                    seekBars.get(i).setThumb((android.graphics.drawable.Drawable) a);
                }
                if (!a.isRunning()) a.start();
            }
        }
    }

    private void stopGallop() {
        if (animThumbs == null) return;
        for (int i = 0; i < animThumbs.length; i++) {
            android.graphics.drawable.Animatable a = animThumbs[i];
            if (a != null && a.isRunning()) a.stop();
            // swap back to idle
            if (idleThumbs != null && i < idleThumbs.length && idleThumbs[i] != null && i < seekBars.size()) {
                seekBars.get(i).setThumb(idleThumbs[i]);
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
        stopGallop();
    }
}