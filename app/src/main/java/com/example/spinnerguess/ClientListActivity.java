package com.example.spinnerguess;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class ClientListActivity extends AppCompatActivity {
    private TableLayout table;
    private CollectionReference clients;
    private ListenerRegistration clientsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);
        table = findViewById(R.id.clientTable);
        findViewById(R.id.btnAddClient).setOnClickListener(v -> showAddClientDialog());
        findViewById(R.id.btnClientListBack).setOnClickListener(v -> finish());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to view clients", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        clients = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid()).collection("clients");
        listenForClients();
    }

    private void listenForClients() {
        clientsListener = clients.orderBy("createdAt")
                .addSnapshotListener((snapshot, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        Toast.makeText(this, "Could not load clients: " + error.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    table.removeAllViews();
                    TableRow header = new TableRow(this);
                    addCell(header, "ID", true, null);
                    addCell(header, "Name", true, null);
                    addCell(header, "Balance", true, null);
                    table.addView(header);
                    if (snapshot == null) return;

                    int displayId = 1;
                    for (DocumentSnapshot document : snapshot.getDocuments()) {
                        String name = document.getString("name");
                        Double storedBalance = document.getDouble("balance");
                        if (name == null) continue;
                        double balance = storedBalance == null ? 0 : storedBalance;
                        ClientRecord client = new ClientRecord(document.getId(), name, balance);
                        TableRow row = new TableRow(this);
                        addCell(row, String.valueOf(displayId++), false, null);
                        addCell(row, client.name, false, v -> {
                            Intent intent = new Intent(this, ClientDetailActivity.class);
                            intent.putExtra("client_id", client.documentId);
                            startActivity(intent);
                        });
                        addCell(row, String.format(java.util.Locale.getDefault(), "%.2f", client.balance),
                                false, null);
                        table.addView(row);
                    }
                });
    }

    private void addCell(TableRow row, String text, boolean header, View.OnClickListener click) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setTextSize(header ? 16 : 15);
        cell.setTextColor(0xFF222222);
        cell.setPadding(10, 14, 10, 14);
        cell.setGravity(Gravity.CENTER);
        if (header) cell.setTypeface(null, android.graphics.Typeface.BOLD);
        if (click != null) {
            cell.setOnClickListener(click);
            cell.setTextColor(0xFF1565C0);
        }
        row.addView(cell, new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f));
    }

    private void showAddClientDialog() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(24, 8, 24, 0);
        EditText name = new EditText(this);
        name.setHint("Name");
        name.setSingleLine(true);
        EditText balance = new EditText(this);
        balance.setHint("Balance");
        balance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        fields.addView(name);
        fields.addView(balance);

        new AlertDialog.Builder(this)
                .setTitle("Add Client")
                .setView(fields)
                .setPositiveButton("Add", (dialog, which) -> {
                    String clientName = name.getText().toString().trim();
                    String balanceText = balance.getText().toString().trim();
                    if (clientName.isEmpty() || balanceText.isEmpty()) {
                        Toast.makeText(this, "Enter a name and balance", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double initialBalance = Double.parseDouble(balanceText);
                        Map<String, Object> data = new HashMap<>();
                        data.put("name", clientName);
                        data.put("balance", initialBalance);
                        data.put("createdAt", FieldValue.serverTimestamp());
                        clients.add(data)
                                .addOnFailureListener(error -> Toast.makeText(this,
                                        "Could not add client: " + error.getMessage(),
                                        Toast.LENGTH_LONG).show());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a valid balance", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Back", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onDestroy() {
        if (clientsListener != null) clientsListener.remove();
        super.onDestroy();
    }
}