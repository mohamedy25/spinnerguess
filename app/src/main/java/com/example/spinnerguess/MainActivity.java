package com.example.spinnerguess;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerDifficulty;
    private EditText etGuess;
    private TextView tvFeedback, tvAttempts, tvTimer;
    private Button btnGuess, btnNewGame;

    private GuessGame game;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinner();
        startNewGame();

        etGuess.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isTimerRunning && !game.isGameOver()) {
                GuessGame.Difficulty selectedDifficulty = (GuessGame.Difficulty) spinnerDifficulty.getSelectedItem();
                startTimer(selectedDifficulty.getTimeInSeconds());
            }
        });

        btnGuess.setOnClickListener(v -> handleGuess());
        btnNewGame.setOnClickListener(v -> startNewGame());
    }

    private void initViews() {
        spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        etGuess = findViewById(R.id.etGuess);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvAttempts = findViewById(R.id.tvAttempts);
        tvTimer = findViewById(R.id.tvTimer);
        btnGuess = findViewById(R.id.btnGuess);
        btnNewGame = findViewById(R.id.btnNewGame);
    }

    private void setupSpinner() {
        ArrayAdapter<GuessGame.Difficulty> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                GuessGame.Difficulty.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

    private void startNewGame() {
        cancelTimer();

        GuessGame.Difficulty selectedDifficulty = (GuessGame.Difficulty) spinnerDifficulty.getSelectedItem();
        game = new GuessGame(selectedDifficulty);

        etGuess.setText("");
        etGuess.setEnabled(true);
        btnGuess.setEnabled(true);
        tvTimer.setText("Time remaining: " + selectedDifficulty.getTimeInSeconds() + "s");
        tvFeedback.setText("Click on the input field to start the timer!");
        updateAttemptsUI();
    }

    private void handleGuess() {
        String input = etGuess.getText().toString().trim();

        if (input.isEmpty()) {
            Toast.makeText(this, "Please enter a number to guess", Toast.LENGTH_SHORT).show();
            return;
        }

        int userGuess = Integer.parseInt(input);
        String feedback = game.makeGuess(userGuess);

        tvFeedback.setText(feedback);
        updateAttemptsUI();

        if (game.isGameOver()) {
            endGame();
        }

        etGuess.setText("");
    }

    private void startTimer(int seconds) {
        isTimerRunning = true;
        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvTimer.setText("Time remaining: " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                tvTimer.setText("Time's up!");
                game.timeOut();
                tvFeedback.setText("Time is up! You lost.");
                endGame();
            }
        }.start();
    }

    private void cancelTimer() {
        isTimerRunning = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private void updateAttemptsUI() {
        tvAttempts.setText("Attempts remaining: " + game.getRemainingAttempts());
    }

    private void endGame() {
        cancelTimer();
        etGuess.setEnabled(false);
        btnGuess.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}