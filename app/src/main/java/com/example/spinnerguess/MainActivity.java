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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerDifficulty;
    private EditText etGuess, etName;
    private TextView tvFeedback, tvAttempts, tvTimer, tvScore;
    private Button btnGuess, btnNewGame;

    private GuessGame game;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private int secondsRemaining = 0;

    // استخدام DBHelper الحالي بدون أي تعديل عليه
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);

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
        tvScore = findViewById(R.id.tvScore);
        btnGuess = findViewById(R.id.btnGuess);
        btnNewGame = findViewById(R.id.btnNewGame);
        etName = findViewById(R.id.name);
    }

    private void setupSpinner() {
        ArrayAdapter<GuessGame.Difficulty> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item, GuessGame.Difficulty.values()
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
        tvScore.setText("Score: 0");
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
        String feedback = game.makeGuess(userGuess, secondsRemaining);

        tvFeedback.setText(feedback);
        tvScore.setText("Score: " + game.getScore());
        updateAttemptsUI();

        if (game.isGameOver()) {
            endGame();
        }

        etGuess.setText("");
    }

    private void startTimer(int seconds) {
        isTimerRunning = true;
        secondsRemaining = seconds;
        countDownTimer = new CountDownTimer(seconds * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                secondsRemaining = (int) (millisUntilFinished / 1000);
                tvTimer.setText("Time remaining: " + secondsRemaining + "s");
            }

            @Override
            public void onFinish() {
                secondsRemaining = 0;
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
        updateDB();
    }

    private void updateDB() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            name = "Player"; // Default name
        }

        int currentScore = game.getScore();
        ArrayList<ModelUser> users = db.genericSelectByUserName(name);

        if (!users.isEmpty()) {
            // Player exists: update if current score is higher than saved score
            ModelUser existingUser = users.get(0);
            if (currentScore > existingUser.getScore()) {
                existingUser.setScore(currentScore);
                db.update(existingUser);
                Toast.makeText(this, "New High Score Saved!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // New player: insert record
            ModelUser newUser = new ModelUser(name, currentScore, 0);
            db.insert(newUser);
            Toast.makeText(this, "Score Saved!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}