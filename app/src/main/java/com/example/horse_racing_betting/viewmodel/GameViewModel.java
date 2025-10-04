package com.example.horse_racing_betting.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.horse_racing_betting.model.Bet;
import com.example.horse_racing_betting.model.Horse;
import com.example.horse_racing_betting.model.RaceResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameViewModel extends AndroidViewModel {

    // -------------------- Persistence / user --------------------
    private static final String PREFS_NAME   = "GamePrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_COINS    = "coins";
    private static final String KEY_FIRST_RUN= "firstRun";

    private static final int INITIAL_COINS = 100;
    private static final int TOTAL_HORSES  = 4;

    // -------------------- Payout multipliers --------------------
    private static final double FIRST_PLACE_MULTIPLIER  = 2.0;
    private static final double SECOND_PLACE_MULTIPLIER = 1.3;
    private static final double THIRD_PLACE_MULTIPLIER  = 0.5;
    private static final double FOURTH_PLACE_MULTIPLIER = 0.0;

    // -------------------- Game states --------------------
    public static final String STATE_IDLE      = "IDLE";
    public static final String STATE_COUNTDOWN = "COUNTDOWN";
    public static final String STATE_RUNNING   = "RUNNING";
    public static final String STATE_RESULT    = "RESULT";

    // -------------------- Race config --------------------
    private static final int   RACE_TICK_MS           = 100;
    private static final int   COUNTDOWN_START        = 3;     // 3..0 (Go)
    private static final float FINISH_PERCENT         = 100f;
    private static final float BURST_TRIGGER_PERCENT  = 30f;

    // Speed ranges per tick
    private static final float BOOST_PRE_MIN   = 0.1f;
    private static final float BOOST_PRE_MAX   = 0.6f;
    private static final float BOOST_ACTIVE_MIN= 0.8f;
    private static final float BOOST_ACTIVE_MAX= 2.0f;
    private static final float NORMAL_MIN      = 0.1f;
    private static final float NORMAL_MAX      = 0.7f;

    // -------------------- Fields --------------------
    private final SharedPreferences sharedPreferences;
    private final Random  random  = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable private Integer boostedHorseNumber = null; // chosen per race
    private boolean burstActivated = false;              // flips when boosted horse crosses 30%

    // LiveData (mutable kept private)
    private final MutableLiveData<String> username = new MutableLiveData<>();
    private final MutableLiveData<Integer> coins = new MutableLiveData<>();
    private final MutableLiveData<Boolean> firstRun = new MutableLiveData<>();
    private final MutableLiveData<List<Bet>> bets = new MutableLiveData<>();
    private final MutableLiveData<List<Horse>> horses = new MutableLiveData<>();
    private final MutableLiveData<String> gameState = new MutableLiveData<>();
    private final MutableLiveData<Integer> countdown = new MutableLiveData<>();
    private final MutableLiveData<RaceResult> raceResult = new MutableLiveData<>();
    private final MutableLiveData<Map<Integer, Boolean>> picked = new MutableLiveData<>();

    public GameViewModel(Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        initializeGame();
    }

    // -------------------- Init --------------------
    private void initializeGame() {
        final String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        final int    savedCoins    = sharedPreferences.getInt(KEY_COINS, INITIAL_COINS);
        final boolean isFirstRun   = sharedPreferences.getBoolean(KEY_FIRST_RUN, true);

        username.setValue(savedUsername);
        coins.setValue(savedCoins);
        firstRun.setValue(isFirstRun);
        gameState.setValue(STATE_IDLE);
        bets.setValue(new ArrayList<>());
        initializeHorses();
    }

    private void initializeHorses() {
        List<Horse> horseList = new ArrayList<>();
        for (int i = 1; i <= TOTAL_HORSES; i++) horseList.add(new Horse(i));
        horses.setValue(horseList);

        Map<Integer, Boolean> m = new HashMap<>();
        for (Horse h : horseList) m.put(h.getNumber(), false);
        picked.setValue(m);
    }

    // -------------------- Getters --------------------
    public LiveData<String> getUsername() { return username; }
    public LiveData<Integer> getCoins() { return coins; }
    public LiveData<Boolean> getFirstRun() { return firstRun; }
    public LiveData<List<Bet>> getBets() { return bets; }
    public LiveData<List<Horse>> getHorses() { return horses; }
    public LiveData<String> getGameState() { return gameState; }
    public LiveData<Integer> getCountdown() { return countdown; }
    public LiveData<RaceResult> getRaceResult() { return raceResult; }
    public LiveData<Map<Integer, Boolean>> getPicked() { return picked; }

    // -------------------- User management --------------------
    public void setUsername(String name) {
        username.setValue(name);
        firstRun.setValue(false);
        sharedPreferences.edit()
                .putString(KEY_USERNAME, name)
                .putBoolean(KEY_FIRST_RUN, false)
                .apply();
    }

    // -------------------- Betting --------------------
    public void markPicked(int horseNumber) {
        Map<Integer, Boolean> cur = picked.getValue();
        Map<Integer, Boolean> m = (cur == null) ? new HashMap<>() : new HashMap<>(cur);
        m.put(horseNumber, true);
        picked.setValue(m);
    }

    public boolean isPicked(int horseNumber) {
        Map<Integer, Boolean> m = picked.getValue();
        return m != null && Boolean.TRUE.equals(m.get(horseNumber));
    }

    public boolean addBet(int horseNumber, int amount) throws IllegalArgumentException {
        Integer curCoins = coins.getValue();
        if (curCoins == null || curCoins < amount) {
            throw new IllegalArgumentException("coin error");
        }

        List<Bet> currentBets = bets.getValue();
        if (currentBets == null) currentBets = new ArrayList<>();

        if (!isPicked(horseNumber)) {
            currentBets.add(new Bet(horseNumber, amount));
            bets.setValue(currentBets);
            markPicked(horseNumber);
            return true;
        }
        throw new IllegalArgumentException("pick error");
    }

    public void removeBet(int index) {
        List<Bet> currentBets = bets.getValue();
        Bet removedBet = null;

        if (currentBets != null && index >= 0 && index < currentBets.size()) {
            removedBet = currentBets.remove(index);
            bets.setValue(currentBets);
        }

        Map<Integer, Boolean> pickedMap = picked.getValue();
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
        for (Bet bet : currentBets) total += bet.getAmount();
        return total;
    }

    public boolean canStartRace() {
        List<Bet> currentBets = bets.getValue();
        Integer curCoins = coins.getValue();
        return currentBets != null
                && !currentBets.isEmpty()
                && getTotalStake() <= (curCoins != null ? curCoins : 0);
    }

    // -------------------- Race management --------------------
    public void startRace() {
        if (!canStartRace()) return;

        // Deduct coins for total stake
        int cur = coins.getValue() != null ? coins.getValue() : 0;
        int stake = getTotalStake();
        int newCoins = Math.max(0, cur - stake);
        coins.setValue(newCoins);
        sharedPreferences.edit().putInt(KEY_COINS, newCoins).apply();

        // Reset horses & picked map
        initializeHorses();

        // Reset boosted-horse flags for this race
        boostedHorseNumber = null;
        burstActivated = false;

        // Start countdown
        gameState.setValue(STATE_COUNTDOWN);
        startCountdown();
    }

    private void startCountdown() {
        performCountdown(COUNTDOWN_START);
    }

    private void performCountdown(int currentCount) {
        if (currentCount > 0) {
            countdown.setValue(currentCount);
            handler.postDelayed(() -> performCountdown(currentCount - 1), 1000);
        } else {
            countdown.setValue(0); // "Go!"
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
        final List<Horse> raceHorses = horses.getValue();
        if (raceHorses == null || raceHorses.isEmpty()) return;

        chooseBoostedHorseOnce(raceHorses);

        Runnable raceAnimation = new Runnable() {
            @Override
            public void run() {
                int unfinished = 0;

                for (Horse horse : raceHorses) {
                    if (horse.isFinished()) continue;

                    advanceHorse(horse);
                    if (horse.getPosition() >= FINISH_PERCENT) {
                        horse.setFinished(true);
                    } else {
                        unfinished++;
                    }
                }

                // notify observers with a fresh list
                horses.setValue(new ArrayList<>(raceHorses));

                if (isRaceDone(unfinished)) {
                    finishRace();
                    boostedHorseNumber = null;
                    burstActivated = false;
                } else {
                    handler.postDelayed(this, RACE_TICK_MS);
                }
            }
        };

        handler.post(raceAnimation);
    }

    private void chooseBoostedHorseOnce(List<Horse> raceHorses) {
        if (boostedHorseNumber == null && !raceHorses.isEmpty()) {
            Horse chosen = raceHorses.get(random.nextInt(raceHorses.size()));
            boostedHorseNumber = chosen.getNumber();
            burstActivated = false;
        }
    }

    private void advanceHorse(Horse horse) {
        final int number = horse.getNumber();
        float movement;

        if (boostedHorseNumber != null && number == boostedHorseNumber) {
            if (!burstActivated && horse.getPosition() >= BURST_TRIGGER_PERCENT) {
                burstActivated = true;
            }
            if (burstActivated) {
                movement = randRange(BOOST_ACTIVE_MIN, BOOST_ACTIVE_MAX);
            } else {
                movement = randRange(BOOST_PRE_MIN, BOOST_PRE_MAX);
            }
        } else {
            movement = randRange(NORMAL_MIN, NORMAL_MAX);
        }

        horse.move(movement);
    }

    private boolean isRaceDone(int unfinished) {
        // finish when 0 or 1 horses remain unfinished (your original rule)
        return unfinished <= 1;
    }

    private float randRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private void finishRace() {
        List<Horse> raceHorses = horses.getValue();
        if (raceHorses == null) return;

        // Sort by position desc to get finish order
        List<Horse> sorted = new ArrayList<>(raceHorses);
        Collections.sort(sorted, (h1, h2) -> Float.compare(h2.getPosition(), h1.getPosition()));

        List<Integer> finishOrder = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setFinishPosition(i + 1);
            finishOrder.add(sorted.get(i).getNumber());
        }

        // push updated horses (positions/finishPos)
        horses.setValue(new ArrayList<>(raceHorses));

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
            int horsePosition = finishOrder.indexOf(bet.getHorseNumber()) + 1; // 1-based
            double multiplier = getMultiplierForPosition(horsePosition);
            if (multiplier > 0) {
                totalWinnings += (int) (bet.getAmount() * multiplier);
            }
        }

        int netChange = totalWinnings - totalLosses;

        // Coins were already deducted by stake at startRace(); add winnings back.
        int currentCoins = coins.getValue() != null ? coins.getValue() : 0;
        int newBalance = currentCoins + totalWinnings;

        coins.setValue(newBalance);
        sharedPreferences.edit().putInt(KEY_COINS, newBalance).apply();

        RaceResult result = new RaceResult(finishOrder, totalWinnings, totalLosses, netChange, newBalance);
        raceResult.setValue(result);
    }

    private double getMultiplierForPosition(int position) {
        switch (position) {
            case 1:  return FIRST_PLACE_MULTIPLIER;
            case 2:  return SECOND_PLACE_MULTIPLIER;
            case 3:  return THIRD_PLACE_MULTIPLIER;
            case 4:  return FOURTH_PLACE_MULTIPLIER;
            default: return 0.0;
        }
    }

    // -------------------- Resets --------------------
    public void resetGame() {
        username.setValue("");
        coins.setValue(INITIAL_COINS);
        firstRun.setValue(true);
        bets.setValue(new ArrayList<>());
        initializeHorses();
        gameState.setValue(STATE_IDLE);
        raceResult.setValue(null);

        sharedPreferences.edit()
                .putString(KEY_USERNAME, "")
                .putInt(KEY_COINS, INITIAL_COINS)
                .putBoolean(KEY_FIRST_RUN, true)
                .apply();
    }

    public void clearBets() {
        bets.setValue(new ArrayList<>());
        // Also clear picked flags
        Map<Integer, Boolean> pm = picked.getValue();
        if (pm != null) {
            Map<Integer, Boolean> reset = new HashMap<>(pm.size());
            for (Integer k : pm.keySet()) reset.put(k, false);
            picked.setValue(reset);
        }
    }

    public void returnToMainMenu() {
        clearBets();
        initializeHorses();
        gameState.setValue(STATE_IDLE);
    }
}
