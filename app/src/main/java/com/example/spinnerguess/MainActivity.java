package com.example.spinnerguess;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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

    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);

        initViews();
        setupSpinner();

        // إعداد حقل الاسم ليعرض اسم المستخدم بوضوح طوال اللعبة مع منعه من التعديل اليدوي
        etName.setEnabled(false);
        etName.setFocusable(false);
        etName.setClickable(false);

        showLoginDialog();

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

        Intent intent = new Intent(MainActivity.this, TopTenActivity.class);
        startActivity(intent);
    }

    private void updateDB() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) return;

        int currentScore = game.getScore();
        ArrayList<ModelUser> users = db.genericSelectByUserName(name);

        if (!users.isEmpty()) {
            ModelUser existingUser = users.get(0);
            if (currentScore > existingUser.getScore()) {
                existingUser.setScore(currentScore);
                db.update(existingUser);
                Toast.makeText(this, "New High Score Saved!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 6) return false;
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_login, null);
        builder.setView(view);
        builder.setCancelable(false);

        EditText etLoginName = view.findViewById(R.id.etLoginName);
        EditText etLoginPassword = view.findViewById(R.id.etLoginPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnGoToRegister = view.findViewById(R.id.btnGoToRegister);

        AlertDialog dialog = builder.create();

        btnLogin.setOnClickListener(v -> {
            String name = etLoginName.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (name.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<ModelUser> users = db.genericSelectByUserName(name);
            if (!users.isEmpty() && users.get(0).getPassword().equals(pass)) {
                etName.setText(name);
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                startNewGame();
            } else {
                Toast.makeText(this, "Invalid Username or Password!", Toast.LENGTH_LONG).show();
            }
        });

        btnGoToRegister.setOnClickListener(v -> {
            dialog.dismiss();
            showRegisterDialog();
        });

        dialog.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_register, null);
        builder.setView(view);
        builder.setCancelable(false);

        EditText etRegName = view.findViewById(R.id.etRegName);
        EditText etRegPassword = view.findViewById(R.id.etRegPassword);
        EditText etRegConfirmPassword = view.findViewById(R.id.etRegConfirmPassword);
        Button btnRegCancel = view.findViewById(R.id.btnRegCancel);
        Button btnRegSubmit = view.findViewById(R.id.btnRegSubmit);

        AlertDialog dialog = builder.create();

        btnRegCancel.setOnClickListener(v -> {
            dialog.dismiss();
            showLoginDialog();
        });

        btnRegSubmit.setOnClickListener(v -> {
            String nameVal = etRegName.getText().toString().trim();
            String pass = etRegPassword.getText().toString().trim();
            String confirmPass = etRegConfirmPassword.getText().toString().trim();

            if (nameVal.isEmpty()) {
                Toast.makeText(this, "Please enter username", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPassword(pass)) {
                Toast.makeText(this, "Password must be >= 6 chars and contain letters & numbers", Toast.LENGTH_LONG).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<ModelUser> existing = db.genericSelectByUserName(nameVal);
            if (!existing.isEmpty()) {
                Toast.makeText(this, "Username already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            ModelUser newUser = new ModelUser(nameVal, 0, 0, pass);
            db.insert(newUser);
            Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

            dialog.dismiss();
            showLoginDialog();
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}