package com.example.spinnerguess;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ClientListActivity extends AppCompatActivity {
    private ClientDatabase database;
    private TableLayout table;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_list);
        database = new ClientDatabase(this);
        table = findViewById(R.id.clientTable);
        findViewById(R.id.btnAddClient).setOnClickListener(v -> showAddClientDialog());
        findViewById(R.id.btnClientListBack).setOnClickListener(v -> finish());
        refreshClients();
    }

    private void refreshClients() {
        table.removeAllViews();
        TableRow header = new TableRow(this);
        addCell(header, "ID", true, null);
        addCell(header, "Name", true, null);
        addCell(header, "Balance", true, null);
        table.addView(header);

        for (ClientRecord client : database.getClients()) {
            TableRow row = new TableRow(this);
            addCell(row, String.valueOf(client.id), false, null);
            addCell(row, client.name, false, v -> {
                Intent intent = new Intent(this, ClientDetailActivity.class);
                intent.putExtra("client_id", client.id);
                startActivity(intent);
            });
            addCell(row, String.format(Locale.getDefault(), "%.2f", client.balance), false, null);
            table.addView(row);
        }
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
        balance.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
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
                        database.addClient(clientName, Double.parseDouble(balanceText));
                        refreshClients();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Enter a valid balance", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Back", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (table != null && database != null) refreshClients();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}