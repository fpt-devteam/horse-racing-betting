package com.example.horse_racing_betting.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.horse_racing_betting.model.Bet;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.model.RaceResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
    private static final double THIRD_PLACE_MULTIPLIER = 1.1;
    private static final double FOURTH_PLACE_MULTIPLIER = 0.0;

    private SharedPreferences sharedPreferences;
    private Random random = new Random();
    private Handler handler = new Handler(Looper.getMainLooper());

    // LiveData
    private MutableLiveData<String> username = new MutableLiveData<>();
    private MutableLiveData<Integer> coins = new MutableLiveData<>();
    private MutableLiveData<Boolean> firstRun = new MutableLiveData<>();
    private MutableLiveData<List<Bet>> bets = new MutableLiveData<>();
    private MutableLiveData<List<Horse>> horses = new MutableLiveData<>();
    private MutableLiveData<String> gameState = new MutableLiveData<>();
    private MutableLiveData<Integer> countdown = new MutableLiveData<>();
    private MutableLiveData<String> currentLap = new MutableLiveData<>();
    private MutableLiveData<RaceResult> raceResult = new MutableLiveData<>();

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
    }

    // Getters for LiveData
    public LiveData<String> getUsername() { return username; }
    public LiveData<Integer> getCoins() { return coins; }
    public LiveData<Boolean> getFirstRun() { return firstRun; }
    public LiveData<List<Bet>> getBets() { return bets; }
    public LiveData<List<Horse>> getHorses() { return horses; }
    public LiveData<String> getGameState() { return gameState; }
    public LiveData<Integer> getCountdown() { return countdown; }
    public LiveData<String> getCurrentLap() { return currentLap; }
    public LiveData<RaceResult> getRaceResult() { return raceResult; }

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
    public boolean addBet(int horseNumber, int amount, int laps) {
        if (coins.getValue() == null || coins.getValue() < amount) {
            return false;
        }

        List<Bet> currentBets = bets.getValue();
        if (currentBets == null) {
            currentBets = new ArrayList<>();
        }

        currentBets.add(new Bet(horseNumber, amount, laps));
        bets.setValue(currentBets);
        return true;
    }

    public void removeBet(int index) {
        List<Bet> currentBets = bets.getValue();
        if (currentBets != null && index >= 0 && index < currentBets.size()) {
            currentBets.remove(index);
            bets.setValue(currentBets);
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
        countdown.setValue(3);
        handler.postDelayed(() -> {
            countdown.setValue(2);
            handler.postDelayed(() -> {
                countdown.setValue(1);
                handler.postDelayed(() -> {
                    countdown.setValue(0);
                    gameState.setValue(STATE_RUNNING);
                    runRace();
                }, 1000);
            }, 1000);
        }, 1000);
    }

    private void runRace() {
        List<Bet> currentBets = bets.getValue();
        if (currentBets == null || currentBets.isEmpty()) return;

        int maxLaps = 1;
        for (Bet bet : currentBets) {
            maxLaps = Math.max(maxLaps, bet.getLaps());
        }

        simulateRace(maxLaps);
    }

    private void simulateRace(int totalLaps) {
        List<Horse> raceHorses = horses.getValue();
        if (raceHorses == null) return;

        // Simulate race with random movement
        Runnable raceAnimation = new Runnable() {
            int currentLap = 1;

            @Override
            public void run() {
                GameViewModel.this.currentLap.setValue("Lap " + currentLap + "/" + totalLaps);

                // Move horses randomly
                for (Horse horse : raceHorses) {
                    if (!horse.isFinished()) {
                        float movement = random.nextFloat() * 2.0f + 0.5f; // Random movement between 0.5 and 2.5
                        horse.move(movement);

                        // Check if horse finished current lap
                        if (horse.getPosition() >= 100.0f * currentLap) {
                            if (currentLap >= totalLaps) {
                                horse.setFinished(true);
                            }
                        }
                    }
                }

                horses.setValue(new ArrayList<>(raceHorses));

                // Check if all horses finished or current lap is complete
                boolean lapComplete = true;
                for (Horse horse : raceHorses) {
                    if (horse.getPosition() < 100.0f * currentLap) {
                        lapComplete = false;
                        break;
                    }
                }

                if (lapComplete && currentLap < totalLaps) {
                    currentLap++;
                    handler.postDelayed(this, 100);
                } else if (currentLap >= totalLaps) {
                    // Race finished
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

    // Game reset
    public void resetGame() {
        coins.setValue(INITIAL_COINS);
        bets.setValue(new ArrayList<>());
        initializeHorses();
        gameState.setValue(STATE_IDLE);
        sharedPreferences.edit().putInt(KEY_COINS, INITIAL_COINS).apply();
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