package com.example.horse_racing_betting.model;

public class Horse {
    private final int number;
    private final String name;
    private float position;
    private boolean isFinished;
    private int finishPosition;

    public Horse(int number) {
        this.number = number;
        this.name = getHorseName(number);
        this.position = 0.0f;
        this.isFinished = false;
        this.finishPosition = 0;
    }

    private String getHorseName(int number) {
        switch (number) {
            case 1: return "Thunder";
            case 2: return "Lightning";
            case 3: return "Storm";
            case 4: return "Blaze";
            default: return "Horse " + number;
        }
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public float getPosition() {
        return position;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public int getFinishPosition() {
        return finishPosition;
    }

    public void setFinishPosition(int finishPosition) {
        this.finishPosition = finishPosition;
    }

    public void move(float distance) {
        if (!isFinished) {
            position += distance;
        }
    }

    public void reset() {
        position = 0.0f;
        isFinished = false;
        finishPosition = 0;
    }
}