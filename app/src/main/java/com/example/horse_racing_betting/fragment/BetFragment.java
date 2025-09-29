package com.example.horse_racing_betting.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.horse_racing_betting.MainActivity;
import com.example.horse_racing_betting.R;
import com.example.horse_racing_betting.adapter.BetAdapter;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.viewmodel.GameViewModel;
import com.example.horse_racing_betting.audio.AudioManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BetFragment extends Fragment implements BetAdapter.OnBetClickListener {
    private GameViewModel gameViewModel;
    private Spinner spinnerHorse;
    private EditText etAmount;
    private Button btnAddBet;
    private TextView tvCurrentCoins;
    private RecyclerView recyclerViewBets;
    private TextView tvTotalStake;
    private Button btnStartRace;
    private BetAdapter betAdapter;
    private final List<Integer> spinnerHorseNumbers = new ArrayList<>();
    private ArrayAdapter<String> horseAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameViewModel = ((MainActivity) requireActivity()).getGameViewModel();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bet, container, false);
        initViews(view);
        setupSpinners();
        setupRecyclerView();
        setupObservers();
        setupClickListeners();
        return view;
    }

    private void initViews(View view) {
        spinnerHorse = view.findViewById(R.id.spinnerHorse);
        etAmount = view.findViewById(R.id.etAmount);
        btnAddBet = view.findViewById(R.id.btnAddBet);
        tvCurrentCoins = view.findViewById(R.id.tvCurrentCoins);
        recyclerViewBets = view.findViewById(R.id.recyclerViewBets);
        tvTotalStake = view.findViewById(R.id.tvTotalStake);
        btnStartRace = view.findViewById(R.id.btnStartRace);
    }

//    private void setupSpinners() {
//
//        gameViewModel.getHorses().observe(getViewLifecycleOwner(), list -> {
//            // Tạo danh sách string từ list horse
//            List<String> display = new ArrayList<>();
//            display.add("Choose Horse");
//            for (Horse h : list) {
//                display.add("Horse #" + h.getNumber());
//            }
//
//            // Adapter cho spinner
//            ArrayAdapter<String> horseAdapter = new ArrayAdapter<>(
//                    requireContext(),
//                    android.R.layout.simple_spinner_item,
//                    display
//            );
//            horseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            spinnerHorse.setAdapter(horseAdapter);
//        });
//    }

    private void setupSpinners() {
        horseAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        horseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHorse.setAdapter(horseAdapter);

        // Quan sát và render
        gameViewModel.getHorses().observe(getViewLifecycleOwner(), l -> rebuildHorseSpinner());
        gameViewModel.getPicked().observe(getViewLifecycleOwner(), m -> rebuildHorseSpinner());
    }

    private void rebuildHorseSpinner() {
        List<Horse> horses = gameViewModel.getHorses().getValue();
        Map<Integer, Boolean> pickedMap = gameViewModel.getPicked().getValue();
        if (horses == null) return;

        List<Integer> available = getAvailableHorseNumbers(horses, pickedMap);
        List<String> display = buildDisplay(available);

        horseAdapter.clear();
        horseAdapter.addAll(display);
        horseAdapter.notifyDataSetChanged();

        spinnerHorse.setEnabled(available.size() > 1);
    }

    @OptIn(markerClass = UnstableApi.class)
    private List<Integer> getAvailableHorseNumbers(List<Horse> horses, Map<Integer, Boolean> pickedMap) {
        if (pickedMap == null) pickedMap = Collections.emptyMap();

        List<Integer> nums = new ArrayList<>();
        nums.add(0); // header
        for (Horse h : horses) {
            int num = h.getNumber();
            if (!Boolean.TRUE.equals(pickedMap.get(num))) {
                nums.add(num);
            }
        }
        for (int i = 0; i < nums.size(); i++) {
            androidx.media3.common.util.Log.d("Show num", Integer.toString(nums.get(i)));
        }
        // lưu mapping dùng khi onItemSelected
        spinnerHorseNumbers.clear();
        spinnerHorseNumbers.addAll(nums);
        return nums;
    }
    private List<String> buildDisplay(List<Integer> nums) {
        List<String> display = new ArrayList<>();
        display.add("Choose Horse");
        for (int i = 1; i < nums.size(); i++) {
            display.add("Horse #" + nums.get(i));
        }
        return display;
    }
    private void setupRecyclerView() {
        betAdapter = new BetAdapter(this);
        recyclerViewBets.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewBets.setAdapter(betAdapter);
    }

    private void setupObservers() {
        gameViewModel.getCoins().observe(getViewLifecycleOwner(), coins -> {
            if (coins != null) {
                tvCurrentCoins.setText(String.format("Current Coins: %d", coins));
            }
        });

        gameViewModel.getBets().observe(getViewLifecycleOwner(), bets -> {
            betAdapter.updateBets(bets);
            updateTotalStake();
            updateStartRaceButton();
        });

        gameViewModel.getGameState().observe(getViewLifecycleOwner(), state -> {
            if (GameViewModel.STATE_COUNTDOWN.equals(state) || GameViewModel.STATE_RUNNING.equals(state)) {
                ((MainActivity) requireActivity()).replaceFragment(new RaceFragment());
            }
        });
    }

    private void setupClickListeners() {
        btnAddBet.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
            addBet();
        });
        btnStartRace.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
            startRace();
        });
    }

    private void addBet() {
        // Validate horse selection
        if (spinnerHorse.getSelectedItemPosition() == 0) {
            Toast.makeText(requireContext(), "Please select a horse", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate amount
        String amountStr = etAmount.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), "Please enter bet amount", Toast.LENGTH_SHORT).show();
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(requireContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user has enough coins
        Integer currentCoins = gameViewModel.getCoins().getValue();
        int currentTotalStake = gameViewModel.getTotalStake();
        if (currentCoins != null && (currentTotalStake + amount) > currentCoins) {
            Toast.makeText(requireContext(), "Insufficient coins", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerHorse.getSelectedItemPosition();
        if (pos <= 0 || pos >= spinnerHorseNumbers.size()) {
            Toast.makeText(requireContext(), "Invalid horse selection", Toast.LENGTH_SHORT).show();
            return;
        }
        int horseNumber = spinnerHorseNumbers.get(pos);

        try {
            boolean success = gameViewModel.addBet(horseNumber, amount);
            if (success) {
                etAmount.setText("");
                spinnerHorse.setSelection(0);
                Toast.makeText(requireContext(), "Bet added successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Failed to add bet", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Exception by Game View Model" + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void startRace() {
        if (gameViewModel.canStartRace()) {
            gameViewModel.startRace();
        } else {
            Toast.makeText(requireContext(), "Please add at least one bet", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateTotalStake() {
        int totalStake = gameViewModel.getTotalStake();
        tvTotalStake.setText(String.format("%d Coins", totalStake));
    }

    private void updateStartRaceButton() {
        btnStartRace.setEnabled(gameViewModel.canStartRace());
        btnStartRace.setAlpha(gameViewModel.canStartRace() ? 1.0f : 0.5f);
    }

    @Override
    public void onRemoveBet(int position) {
        ((MainActivity) requireActivity()).getAudioManager().playSfx(R.raw.mouse_click);
        gameViewModel.removeBet(position);

    }
}