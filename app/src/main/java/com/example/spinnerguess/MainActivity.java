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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerDifficulty;
    private EditText etGuess, etName;
    private TextView tvFeedback, tvAttempts, tvTimer, tvScore;
    private Button btnGuess, btnNewGame, btnProfile, btnLogout;

    private GuessGame game;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private int secondsRemaining = 0;

    private DBHelper db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DBHelper(this);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSpinner();

        etName.setEnabled(false);
        etName.setFocusable(false);
        etName.setClickable(false);

        etGuess.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !isTimerRunning && game != null && !game.isGameOver()) {
                GuessGame.Difficulty selectedDifficulty = (GuessGame.Difficulty) spinnerDifficulty.getSelectedItem();
                startTimer(selectedDifficulty.getTimeInSeconds());
            }
        });

        btnGuess.setOnClickListener(v -> handleGuess());
        btnNewGame.setOnClickListener(v -> startNewGame());

        // מעבר למסך הפרופיל הלינארי (ProfileActivity)
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> handleLogout());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            etName.setText(currentUser.getEmail());
            startNewGame();
        } else {
            showLoginDialog();
        }
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
        btnProfile = findViewById(R.id.btnProfile);
        btnLogout = findViewById(R.id.btnLogout);
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

    // ביצוע יציאה מהחשבון ומעבר למסך/דיאלוג ההתחברות[cite: 8, 9]
    private void handleLogout() {
        cancelTimer();
        mAuth.signOut();
        etName.setText("");
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        showLoginDialog();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_login, null);
        builder.setView(view);
        builder.setCancelable(false);

        EditText etLoginEmail = view.findViewById(R.id.etLoginEmail);
        EditText etLoginPassword = view.findViewById(R.id.etLoginPassword);
        Button btnLogin = view.findViewById(R.id.btnLogin);
        Button btnGoToRegister = view.findViewById(R.id.btnGoToRegister);

        AlertDialog dialog = builder.create();

        btnLogin.setOnClickListener(v -> {
            String email = etLoginEmail.getText().toString().trim();
            String pass = etLoginPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // התחברות באמצעות Firebase Authentication[cite: 8]
            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            etName.setText(email);
                            Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            startNewGame();
                        } else {
                            Toast.makeText(this, "Authentication Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
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
        EditText etRegEmail = view.findViewById(R.id.etRegEmail);
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
            String email = etRegEmail.getText().toString().trim();
            String pass = etRegPassword.getText().toString().trim();
            String confirmPass = etRegConfirmPassword.getText().toString().trim();

            if (nameVal.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) { // בדיקת אורך סיסמה מעל 6 תווים כנדרש ב-Firebase[cite: 5, 7, 11]
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_LONG).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            ModelUser newUser = new ModelUser(email, 0, 0, pass);
                            db.insert(newUser);
                            Toast.makeText(this, "Registered Successfully!", Toast.LENGTH_SHORT).show();

                            dialog.dismiss();
                            showLoginDialog();
                        } else {
                            Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}