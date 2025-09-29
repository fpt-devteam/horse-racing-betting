package com.example.horse_racing_betting.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.viewmodel.GameViewModel;

public class StartFragment extends Fragment {
    private GameViewModel gameViewModel;
    private CardView usernameCard;
    private CardView mainMenuCard;
    private EditText etUsername;
    private Button btnConfirm;
    private TextView tvWelcome;
    private TextView tvCoins;
    private Button btnStartBetting;
    private Button btnHelp;
    private Button btnSettings;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        initViews(view);
        setupObservers();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        usernameCard = view.findViewById(R.id.usernameCard);
        mainMenuCard = view.findViewById(R.id.mainMenuCard);
        etUsername = view.findViewById(R.id.etUsername);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvCoins = view.findViewById(R.id.tvCoins);
        btnStartBetting = view.findViewById(R.id.btnStartBetting);
        btnHelp = view.findViewById(R.id.btnHelp);
        btnSettings = view.findViewById(R.id.btnSettings);
    }

    private void setupObservers() {
        gameViewModel.getFirstRun().observe(getViewLifecycleOwner(), firstRun -> {
            if (firstRun != null) {
                usernameCard.setVisibility(firstRun ? View.VISIBLE : View.GONE);
                mainMenuCard.setVisibility(firstRun ? View.GONE : View.VISIBLE);
            }
        });

        gameViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (username != null && !username.isEmpty()) {
                tvWelcome.setText(String.format("Welcome,\n%s!", username));
            }
        });

        gameViewModel.getCoins().observe(getViewLifecycleOwner(), coins -> {
            if (coins != null) {
                tvCoins.setText(String.format("%d Coins", coins));
            }
        });
    }

    private void setupClickListeners() {
        btnConfirm.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            if (TextUtils.isEmpty(username)) {
                Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show();
                return;
            }
            gameViewModel.setUsername(username);
        });

        btnStartBetting.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).replaceFragment(new BetFragment());
        });

        btnHelp.setOnClickListener(v -> {
            HelpFragment helpFragment = new HelpFragment();
            helpFragment.show(getParentFragmentManager(), "help");
        });

        btnSettings.setOnClickListener(v -> {
            SettingsFragment settingsFragment = new SettingsFragment();
            settingsFragment.show(getParentFragmentManager(), "settings");
        });
    }
}