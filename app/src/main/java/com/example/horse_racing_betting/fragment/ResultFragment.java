package com.example.horse_racing_betting.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.model.RaceResult;
import com.example.horse_racing_betting.viewmodel.GameViewModel;

import java.util.List;

public class ResultFragment extends Fragment {
    private GameViewModel gameViewModel;
    private ImageButton btnClose;
    private ImageView ivFirst, ivSecond, ivThird, ivFourth;
    private TextView tvTotalWinnings, tvNetChange, tvNewBalance;
    private Button btnRaceAgain, btnMainMenu;

    // Horse icon resources
    private final int[] horseIcons = {
        R.drawable.ic_horse_1,
        R.drawable.ic_horse_2,
        R.drawable.ic_horse_3,
        R.drawable.ic_horse_4
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
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
        btnClose = view.findViewById(R.id.btnClose);
        ivFirst = view.findViewById(R.id.ivFirst);
        ivSecond = view.findViewById(R.id.ivSecond);
        ivThird = view.findViewById(R.id.ivThird);
        ivFourth = view.findViewById(R.id.ivFourth);
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
        btnClose.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).replaceFragment(new StartFragment());
        });

        btnRaceAgain.setOnClickListener(v -> {
            gameViewModel.returnToMainMenu();
            ((MainActivity) requireActivity()).replaceFragment(new BetFragment());
        });

        btnMainMenu.setOnClickListener(v -> {
            gameViewModel.returnToMainMenu();
            ((MainActivity) requireActivity()).replaceFragment(new StartFragment());
        });
    }

    private void displayRaceResult(RaceResult result) {
        List<Integer> finishOrder = result.getFinishOrder();

        // Display podium with correct horse icons
        if (finishOrder.size() >= 4) {
            ivFirst.setImageResource(horseIcons[finishOrder.get(0) - 1]);
            ivSecond.setImageResource(horseIcons[finishOrder.get(1) - 1]);
            ivThird.setImageResource(horseIcons[finishOrder.get(2) - 1]);
            ivFourth.setImageResource(horseIcons[finishOrder.get(3) - 1]);
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
}