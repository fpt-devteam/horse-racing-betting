package com.example.horse_racing_betting.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.RaceResult;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;
import com.example.horse_racing_betting.skin.SkinManager;

import java.util.List;

public class ResultFragment extends DialogFragment {
    private GameViewModel gameViewModel;
    private ImageView ivFirst, ivSecond, ivThird, ivFourth;
    private TextView tvFirstName, tvSecondName, tvThirdName, tvFourthName;
    private TextView tvTotalWinnings, tvNetChange, tvNewBalance;
    private Button btnRaceAgain, btnMainMenu;

    private SkinManager skinManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
    skinManager = SkinManager.getInstance(requireContext());
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
        View view = inflater.inflate(R.layout.fragment_result, container, false);
        initViews(view);
        setupObservers();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        ivFirst = view.findViewById(R.id.ivFirst);
        ivSecond = view.findViewById(R.id.ivSecond);
        ivThird = view.findViewById(R.id.ivThird);
        ivFourth = view.findViewById(R.id.ivFourth);
        tvFirstName = view.findViewById(R.id.tvFirstName);
        tvSecondName = view.findViewById(R.id.tvSecondName);
        tvThirdName = view.findViewById(R.id.tvThirdName);
        tvFourthName = view.findViewById(R.id.tvFourthName);
        tvTotalWinnings = view.findViewById(R.id.tvTotalWinnings);
        tvNetChange = view.findViewById(R.id.tvNetChange);
        tvNewBalance = view.findViewById(R.id.tvNewBalance);
        btnRaceAgain = view.findViewById(R.id.btnRaceAgain);
        btnMainMenu = view.findViewById(R.id.btnMainMenu);
    }

    private void setupObservers() {
        gameViewModel.getRaceResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                displayRaceResult(result);
            }
        });
    }

    private void setupClickListeners() {
        btnRaceAgain.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
            gameViewModel.returnToMainMenu();
            dismiss();
            ((MainActivity) requireActivity()).replaceFragment(new BetFragment());
        });

        btnMainMenu.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
            gameViewModel.returnToMainMenu();
            dismiss();
            ((MainActivity) requireActivity()).replaceFragment(new StartFragment());
        });
    }

    private void displayRaceResult(RaceResult result) {
        List<Integer> finishOrder = result.getFinishOrder();
        List<com.example.horse_racing_betting.model.Horse> horses = gameViewModel.getHorses().getValue();

        // Display podium with correct horse icons and names
        if (finishOrder.size() >= 4 && horses != null) {
            // 1st Place
            int firstHorseNum = finishOrder.get(0);
            skinManager.applyHorseIcon(ivFirst, firstHorseNum);
            com.example.horse_racing_betting.model.Horse firstHorse = getHorseByNumber(horses, firstHorseNum);
            if (firstHorse != null) {
                tvFirstName.setText(String.format("Horse %d: %s", firstHorse.getNumber(), firstHorse.getName()));
            }

            // 2nd Place
            int secondHorseNum = finishOrder.get(1);
            skinManager.applyHorseIcon(ivSecond, secondHorseNum);
            com.example.horse_racing_betting.model.Horse secondHorse = getHorseByNumber(horses, secondHorseNum);
            if (secondHorse != null) {
                tvSecondName.setText(String.format("Horse %d: %s", secondHorse.getNumber(), secondHorse.getName()));
            }

            // 3rd Place
            int thirdHorseNum = finishOrder.get(2);
            skinManager.applyHorseIcon(ivThird, thirdHorseNum);
            com.example.horse_racing_betting.model.Horse thirdHorse = getHorseByNumber(horses, thirdHorseNum);
            if (thirdHorse != null) {
                tvThirdName.setText(String.format("Horse %d: %s", thirdHorse.getNumber(), thirdHorse.getName()));
            }

            // 4th Place
            int fourthHorseNum = finishOrder.get(3);
            skinManager.applyHorseIcon(ivFourth, fourthHorseNum);
            com.example.horse_racing_betting.model.Horse fourthHorse = getHorseByNumber(horses, fourthHorseNum);
            if (fourthHorse != null) {
                tvFourthName.setText(String.format("Horse %d: %s", fourthHorse.getNumber(), fourthHorse.getName()));
            }
        }

        // Display payout information
        int netChange = result.getNetChange();
        String winningsText;
        if (netChange > 0) {
            winningsText = String.format("Total Winnings/Losses: +%d Coins", netChange);
        } else if (netChange < 0) {
            winningsText = String.format("Total Winnings/Losses: %d Coins", netChange);
        } else {
            winningsText = "Total Winnings/Losses: 0 Coins";
        }
        tvTotalWinnings.setText(winningsText);

        // Display net change percentage
        double netChangePercentage = result.getNetChangePercentage();
        String netChangeText;
        if (netChangePercentage > 0) {
            netChangeText = String.format("Net Change: +%.0f%%", netChangePercentage);
            tvNetChange.setBackgroundResource(R.drawable.progress_fill);
        } else if (netChangePercentage < 0) {
            netChangeText = String.format("Net Change: %.0f%%", netChangePercentage);
            tvNetChange.setBackgroundResource(R.drawable.multiplier_fourth_bar);
        } else {
            netChangeText = "Net Change: 0%";
            tvNetChange.setBackgroundResource(R.drawable.progress_background);
        }
        tvNetChange.setText(netChangeText);

        // Display new balance
        tvNewBalance.setText(String.format("New Coin Balance: %d Coins", result.getNewBalance()));
    }

    private com.example.horse_racing_betting.model.Horse getHorseByNumber(List<com.example.horse_racing_betting.model.Horse> horses, int number) {
        for (com.example.horse_racing_betting.model.Horse horse : horses) {
            if (horse.getNumber() == number) {
                return horse;
            }
        }
        return null;
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

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Resume BGM exactly when the results dialog is dismissed
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            AudioManager am = activity.getAudioManager();
            if (am != null && !am.isMuteBgm()) {
                am.startBgm();
            }
        }
    }
}