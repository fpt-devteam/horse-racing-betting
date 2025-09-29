package com.example.horse_racing_betting.model;

import java.util.List;

public class RaceResult {
    private List<Integer> finishOrder;
    private int totalWinnings;
    private int totalLosses;
    private int netChange;
    private int newBalance;

    public RaceResult(List<Integer> finishOrder, int totalWinnings, int totalLosses, int netChange, int newBalance) {
        this.finishOrder = finishOrder;
        this.totalWinnings = totalWinnings;
        this.totalLosses = totalLosses;
        this.netChange = netChange;
        this.newBalance = newBalance;
    }

    public List<Integer> getFinishOrder() {
        return finishOrder;
    }

    public void setFinishOrder(List<Integer> finishOrder) {
        this.finishOrder = finishOrder;
    }

    public int getTotalWinnings() {
        return totalWinnings;
    }

    public void setTotalWinnings(int totalWinnings) {
        this.totalWinnings = totalWinnings;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(int totalLosses) {
        this.totalLosses = totalLosses;
    }

    public int getNetChange() {
        return netChange;
    }

    public void setNetChange(int netChange) {
        this.netChange = netChange;
    }

    public int getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(int newBalance) {
        this.newBalance = newBalance;
    }

    public double getNetChangePercentage() {
        if (totalLosses == 0) return 0.0;
        return ((double) netChange / totalLosses) * 100;
    }
}