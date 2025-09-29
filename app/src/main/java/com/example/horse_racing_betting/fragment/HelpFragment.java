package com.example.horse_racing_betting.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.viewmodel.GameViewModel;

public class HelpFragment extends DialogFragment {
    private GameViewModel gameViewModel;
    private Button btnRaceAgain, btnMainMenu;

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
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        initViews(view);
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        btnRaceAgain = view.findViewById(R.id.btnRaceAgain);
        btnMainMenu = view.findViewById(R.id.btnMainMenu);
    }

    private void setupClickListeners() {
        btnRaceAgain.setOnClickListener(v -> {
            dismiss();
            gameViewModel.returnToMainMenu();
            ((MainActivity) requireActivity()).replaceFragment(new BetFragment());
        });

        btnMainMenu.setOnClickListener(v -> {
            dismiss();
            gameViewModel.returnToMainMenu();
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