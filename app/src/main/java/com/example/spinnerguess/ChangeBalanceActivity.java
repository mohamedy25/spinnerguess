package com.example.spinnerguess;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ChangeBalanceActivity extends AppCompatActivity {
    private ClientDatabase database;
    private long clientId;
    private EditText amountInput;
    private RadioGroup operation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_balance);
        database = new ClientDatabase(this);
        clientId = getIntent().getLongExtra("client_id", -1);
        amountInput = findViewById(R.id.etBalanceAmount);
        operation = findViewById(R.id.balanceOperation);
        findViewById(R.id.btnConfirmBalance).setOnClickListener(v -> confirmChange());
        findViewById(R.id.btnCancelBalance).setOnClickListener(v -> finish());
    }

    private void confirmChange() {
        String text = amountInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount < 0) {
            Toast.makeText(this, "Amount must be zero or greater", Toast.LENGTH_SHORT).show();
            return;
        }
        ClientRecord client = database.getClient(clientId);
        if (client == null) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        boolean add = operation.getCheckedRadioButtonId() == R.id.radioAdd;
        double updated = add ? client.balance + amount : client.balance - amount;
        String action = add ? "add to" : "remove from";
        new AlertDialog.Builder(this)
                .setTitle("Confirm balance change")
                .setMessage(String.format(Locale.getDefault(),
                        "Are you sure you want to %s %s?\nNew balance: %.2f", action, client.name, updated))
                .setPositiveButton("Yes", (dialog, which) -> {
                    database.updateBalance(clientId, updated);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> finish())
                .show();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}