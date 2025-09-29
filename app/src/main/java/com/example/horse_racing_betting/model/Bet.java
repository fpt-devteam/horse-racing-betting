package com.example.horse_racing_betting.model;

public class Bet {
    private int horseNumber;
    private int amount;

    public Bet(int horseNumber, int amount) {
        this.horseNumber = horseNumber;
        this.amount = amount;
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



    @Override
    public String toString() {
        return "Horse #" + horseNumber + " - " + amount + " Coins";
    }
}