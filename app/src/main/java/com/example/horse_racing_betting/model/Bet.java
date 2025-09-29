package com.example.horse_racing_betting.model;

public class Bet {
    private int horseNumber;
    private int amount;
    private int laps;

    public Bet(int horseNumber, int amount, int laps) {
        this.horseNumber = horseNumber;
        this.amount = amount;
        this.laps = laps;
    }

    public int getHorseNumber() {
        return horseNumber;
    }

    public void setHorseNumber(int horseNumber) {
        this.horseNumber = horseNumber;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getLaps() {
        return laps;
    }

    public void setLaps(int laps) {
        this.laps = laps;
    }

    @Override
    public String toString() {
        return "Horse #" + horseNumber + " - " + amount + " Coins - " + laps + " Lap" + (laps > 1 ? "s" : "");
    }
}