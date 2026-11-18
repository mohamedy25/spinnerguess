package com.example.spinnerguess;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ChangeBalanceActivity extends AppCompatActivity {
    private String clientId;
    private EditText amountInput;
    private RadioGroup operation;
    private DocumentReference clientDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_balance);
        clientId = getIntent().getStringExtra("client_id");
        amountInput = findViewById(R.id.etBalanceAmount);
        operation = findViewById(R.id.balanceOperation);
        findViewById(R.id.btnConfirmBalance).setOnClickListener(v -> confirmChange());
        findViewById(R.id.btnCancelBalance).setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || clientId == null) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        clientDocument = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("clients").document(clientId);
    }

    private void confirmChange() {
        String text = amountInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }
        final double amount;
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

        clientDocument.get().addOnSuccessListener(snapshot -> confirmWithCurrentBalance(snapshot, amount))
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not load client: " + error.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void confirmWithCurrentBalance(DocumentSnapshot snapshot, double amount) {
        if (!snapshot.exists()) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String name = snapshot.getString("name");
        Double value = snapshot.getDouble("balance");
        double currentBalance = value == null ? 0 : value;
        boolean add = operation.getCheckedRadioButtonId() == R.id.radioAdd;
        double updatedBalance = add ? currentBalance + amount : currentBalance - amount;
        String action = add ? "add to" : "remove from";
        new AlertDialog.Builder(this)
                .setTitle("Confirm balance change")
                .setMessage(String.format(Locale.getDefault(),
                        "Are you sure you want to %s %s?\nNew balance: %.2f",
                        action, name == null ? "this client" : name, updatedBalance))
                .setPositiveButton("Yes", (dialog, which) -> applyBalanceChange(amount, add))
                .setNegativeButton("No", (dialog, which) -> finish())
                .show();
    }

    private void applyBalanceChange(double amount, boolean add) {
        FirebaseFirestore.getInstance().runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(clientDocument);
            if (!snapshot.exists()) throw new IllegalStateException("Client no longer exists");
            Double currentValue = snapshot.getDouble("balance");
            double currentBalance = currentValue == null ? 0 : currentValue;
            double updatedBalance = add ? currentBalance + amount : currentBalance - amount;
            transaction.update(clientDocument, "balance", updatedBalance);
            return updatedBalance;
        }).addOnSuccessListener(updatedBalance -> finish())
                .addOnFailureListener(error -> Toast.makeText(this,
                        "Could not update balance: " + error.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}