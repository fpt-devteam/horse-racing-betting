package com.example.horse_racing_betting.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.OptIn;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.util.UnstableApi;

import com.example.horse_racing_betting.model.Bet;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.model.RaceResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import android.util.Log;

public class GameViewModel extends AndroidViewModel {
    private static final String PREFS_NAME = "GamePrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_COINS = "coins";
    private static final String KEY_FIRST_RUN = "firstRun";
    private static final int INITIAL_COINS = 100;
    private static final int TOTAL_HORSES = 4;

    // Payout multipliers
    private static final double FIRST_PLACE_MULTIPLIER = 2.0;
    private static final double SECOND_PLACE_MULTIPLIER = 1.3;
    private static final double THIRD_PLACE_MULTIPLIER = 0.5;
    private static final double FOURTH_PLACE_MULTIPLIER = 0.0;

    private final SharedPreferences sharedPreferences;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    // LiveData
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Integer> coins = new MutableLiveData<>();
    private final MutableLiveData<Boolean> firstRun = new MutableLiveData<>();
    private final MutableLiveData<List<Bet>> bets = new MutableLiveData<>();
    private final MutableLiveData<List<Horse>> horses = new MutableLiveData<>();
    private final MutableLiveData<String> gameState = new MutableLiveData<>();
    private final MutableLiveData<Integer> countdown = new MutableLiveData<>();
    private final MutableLiveData<RaceResult> raceResult = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Boolean>> picked = new MutableLiveData<>();

    // Game states
    public static final String STATE_IDLE = "IDLE";
    public static final String STATE_COUNTDOWN = "COUNTDOWN";
    public static final String STATE_RUNNING = "RUNNING";
    public static final String STATE_RESULT = "RESULT";

    public GameViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        initializeGame();
    }

    private void initializeGame() {
        // Load saved data
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        int savedCoins = sharedPreferences.getInt(KEY_COINS, INITIAL_COINS);
        boolean isFirstRun = sharedPreferences.getBoolean(KEY_FIRST_RUN, true);

        username.setValue(savedUsername);
        coins.setValue(savedCoins);
        firstRun.setValue(isFirstRun);
        gameState.setValue(STATE_IDLE);
        bets.setValue(new ArrayList<>());
        initializeHorses();
    }

    private void initializeHorses() {
        List<Horse> horseList = new ArrayList<>();
        for (int i = 1; i <= TOTAL_HORSES; i++) {
            horseList.add(new Horse(i));
        }
        horses.setValue(horseList);
        Map<Integer, Boolean> m = new HashMap<>();
        for (Horse h : horseList) m.put(h.getNumber(), false);
        picked.setValue(m);
    }

    // Getters for LiveData
    public LiveData<String> getUsername() { return username; }
    public LiveData<Integer> getCoins() { return coins; }
    public LiveData<Boolean> getFirstRun() { return firstRun; }
    public LiveData<List<Bet>> getBets() { return bets; }
    public LiveData<List<Horse>> getHorses() { return horses; }
    public LiveData<String> getGameState() { return gameState; }
    public LiveData<Integer> getCountdown() { return countdown; }
    public LiveData<RaceResult> getRaceResult() { return raceResult; }
    public LiveData<Map<Integer, Boolean>> getPicked() { return picked; }

    // User management
    public void setUsername(String name) {
        username.setValue(name);
        firstRun.setValue(false);
        sharedPreferences.edit()
                .putString(KEY_USERNAME, name)
                .putBoolean(KEY_FIRST_RUN, false)
                .apply();
    }

    // Betting
    public void markPicked(int horseNumber) {
        Map<Integer, Boolean> m = new HashMap<>(Objects.requireNonNull(picked.getValue()));
        m.put(horseNumber, true);
        picked.setValue(m);
    }

    public boolean isPicked(int horseNumber) {
        Map<Integer, Boolean> m = picked.getValue();
        return m != null && Boolean.TRUE.equals(m.get(horseNumber));
    }

    @OptIn(markerClass = UnstableApi.class)
    public boolean addBet(int horseNumber, int amount) throws IllegalArgumentException{
        if (coins.getValue() == null || coins.getValue() < amount) {
            throw new IllegalArgumentException("coin error");
        }

        List<Bet> currentBets = bets.getValue();
        if (currentBets == null) {
            currentBets = new ArrayList<>();
        }

        androidx.media3.common.util.Log.d("Horse number picked", Integer.toString(horseNumber));


        if (!isPicked(horseNumber)){
            currentBets.add(new Bet(horseNumber, amount));
            bets.setValue(currentBets);
            markPicked(horseNumber);
            return true;
        };

        throw new IllegalArgumentException("pick error");
    }

    public void removeBet(int index) {
        List<Bet> currentBets = bets.getValue();
        Bet removedBet = null;
        if (currentBets != null && index >= 0 && index < currentBets.size()) {
            removedBet = currentBets.remove(index);
            bets.setValue(currentBets);
        }
        Map<Integer, Boolean> pickedMap = getPicked().getValue();
        if (pickedMap != null && removedBet != null) {
            Map<Integer, Boolean> newMap = new HashMap<>(pickedMap);
            newMap.put(removedBet.getHorseNumber(), false);
            picked.setValue(newMap);
        }
    }

    public int getTotalStake() {
        List<Bet> currentBets = bets.getValue();
        if (currentBets == null) return 0;

        int total = 0;
        for (Bet bet : currentBets) {
            total += bet.getAmount();
        }
        return total;
    }

    public boolean canStartRace() {
        List<Bet> currentBets = bets.getValue();
        return currentBets != null && !currentBets.isEmpty() && getTotalStake() <= (coins.getValue() != null ? coins.getValue() : 0);
    }

    // Race management
    public void startRace() {
        if (!canStartRace()) return;

        // Deduct coins for bets
        int currentCoins = coins.getValue() != null ? coins.getValue() : 0;
        int newCoins = currentCoins - getTotalStake();
        coins.setValue(newCoins);
        sharedPreferences.edit().putInt(KEY_COINS, newCoins).apply();

        // Reset horses
        initializeHorses();

        // Start countdown
        gameState.setValue(STATE_COUNTDOWN);
        startCountdown();
    }

    private void startCountdown() {
        // Start countdown from 5 to 0 (where 0 means "Go")
        performCountdown(3);
    }

    private void performCountdown(int currentCount) {
        if (currentCount > 0) {
            // Display the number
            countdown.setValue(currentCount);
            // Schedule next countdown after 1 second
            handler.postDelayed(() -> performCountdown(currentCount - 1), 1000);
        } else {
            // Display "Go" (represented by 0) and start the race
            countdown.setValue(0);
            handler.postDelayed(() -> {
                gameState.setValue(STATE_RUNNING);
                runRace();
            }, 1000);
        }
    }

    private void runRace() {
        simulateRace();
    }

    private void simulateRace() {
        List<Horse> raceHorses = horses.getValue();
        if (raceHorses == null) return;

        // Simulate race with random movement
        Runnable raceAnimation = new Runnable() {
            @Override
            public void run() {
                // Move horses randomly - each horse gets different random speed each loop
                // Speed range: 0.1 to 0.9 for slower, more dramatic differences
                for (Horse horse : raceHorses) {
                    if (!horse.isFinished()) {
                        // Generate random speed between 0.1 and 0.9
                        // This creates dramatic speed differences: slow horse ~0.1-0.3, fast horse ~0.6-0.9
                        float movement = random.nextFloat() * 0.8f + 0.1f;
                        horse.move(movement);

                        // Check if horse finished the race
                        if (horse.getPosition() >= 100.0f) {
                            horse.setFinished(true);
                        }
                    }
                }

                horses.setValue(new ArrayList<>(raceHorses));

                // Count how many horses haven't finished
                int unfinishedCount = 0;
                for (Horse horse : raceHorses) {
                    if (!horse.isFinished()) {
                        unfinishedCount++;
                    }
                }

                if (unfinishedCount <= 1) {
                    // Race finished - only one or no horses left unfinished
                    finishRace();
                } else {
                    handler.postDelayed(this, 100);
                }
            }
        };

        handler.post(raceAnimation);
    }

    private void finishRace() {
        List<Horse> raceHorses = horses.getValue();
        if (raceHorses == null) return;

        // Sort horses by position to determine finish order
        List<Horse> sortedHorses = new ArrayList<>(raceHorses);
        Collections.sort(sortedHorses, (h1, h2) -> Float.compare(h2.getPosition(), h1.getPosition()));

        // Set finish positions
        List<Integer> finishOrder = new ArrayList<>();
        for (int i = 0; i < sortedHorses.size(); i++) {
            sortedHorses.get(i).setFinishPosition(i + 1);
            finishOrder.add(sortedHorses.get(i).getNumber());
        }

        horses.setValue(new ArrayList<>(raceHorses));

        // Calculate winnings
        calculateWinnings(finishOrder);
        gameState.setValue(STATE_RESULT);
    }

    private void calculateWinnings(List<Integer> finishOrder) {
        List<Bet> currentBets = bets.getValue();
        if (currentBets == null) return;

        int totalWinnings = 0;
        int totalLosses = 0;

        for (Bet bet : currentBets) {
            totalLosses += bet.getAmount();

            int horsePosition = finishOrder.indexOf(bet.getHorseNumber()) + 1;
            double multiplier = getMultiplierForPosition(horsePosition);

            if (multiplier > 0) {
                totalWinnings += (int) (bet.getAmount() * multiplier);
            }
        }

        int netChange = totalWinnings - totalLosses;
        int currentCoins = coins.getValue() != null ? coins.getValue() : 0;
        int newBalance = currentCoins + totalWinnings;

        coins.setValue(newBalance);
        sharedPreferences.edit().putInt(KEY_COINS, newBalance).apply();

        RaceResult result = new RaceResult(finishOrder, totalWinnings, totalLosses, netChange, newBalance);
        raceResult.setValue(result);
    }

    private double getMultiplierForPosition(int position) {
        switch (position) {
            case 1: return FIRST_PLACE_MULTIPLIER;
            case 2: return SECOND_PLACE_MULTIPLIER;
            case 3: return THIRD_PLACE_MULTIPLIER;
            case 4: return FOURTH_PLACE_MULTIPLIER;
            default: return 0.0;
        }
    }

    // Game reset - Full reset including username
    public void resetGame() {
        // Clear all data
        username.setValue("");
        coins.setValue(INITIAL_COINS);
        firstRun.setValue(true);
        bets.setValue(new ArrayList<>());
        initializeHorses();
        gameState.setValue(STATE_IDLE);
        raceResult.setValue(null);

        // Clear SharedPreferences
        sharedPreferences.edit()
                .putString(KEY_USERNAME, "")
                .putInt(KEY_COINS, INITIAL_COINS)
                .putBoolean(KEY_FIRST_RUN, true)
                .apply();
    }

    public void clearBets() {
        bets.setValue(new ArrayList<>());
    }

    public void returnToMainMenu() {
        clearBets();
        initializeHorses();
        gameState.setValue(STATE_IDLE);
    }
}