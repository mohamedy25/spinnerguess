package com.example.spinnerguess;

import java.util.Random;

public class GuessGame {

    public enum Difficulty {
        EASY(50, 10, 30),
        MEDIUM(100, 7, 20),
        HARD(200, 5, 10);

        private final int maxNumber;
        private final int maxAttempts;
        private final int timeInSeconds;

        Difficulty(int maxNumber, int maxAttempts, int timeInSeconds) {
            this.maxNumber = maxNumber;
            this.maxAttempts = maxAttempts;
            this.timeInSeconds = timeInSeconds;
        }

        public int getMaxNumber() { return maxNumber; }
        public int getMaxAttempts() { return maxAttempts; }
        public int getTimeInSeconds() { return timeInSeconds; }
    }

    private int targetNumber;
    private int remainingAttempts;
    private boolean isGameOver;
    private Difficulty currentDifficulty;

    public GuessGame(Difficulty difficulty) {
        resetGame(difficulty);
    }

    public void resetGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        this.remainingAttempts = difficulty.getMaxAttempts();
        this.isGameOver = false;

        Random random = new Random();
        this.targetNumber = random.nextInt(difficulty.getMaxNumber()) + 1;
    }

    public String makeGuess(int userGuess) {
        if (isGameOver) {
            return "Game is already over!";
        }

        remainingAttempts--;

        if (userGuess == targetNumber) {
            isGameOver = true;
            return "Correct guess! You won! 🎉";
        } else if (remainingAttempts <= 0) {
            isGameOver = true;
            return "Out of attempts! The correct number was: " + targetNumber;
        } else if (userGuess < targetNumber) {
            return "Target number is higher than " + userGuess;
        } else {
            return "Target number is lower than " + userGuess;
        }
    }

    public void timeOut() {
        this.isGameOver = true;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty;
    }
}