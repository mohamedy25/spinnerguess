package com.example.spinnerguess;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ClientDetailActivity extends AppCompatActivity {
    private ClientDatabase database;
    private long clientId;
    private TextView welcome, balance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);
        database = new ClientDatabase(this);
        clientId = getIntent().getLongExtra("client_id", -1);
        welcome = findViewById(R.id.tvClientWelcome);
        balance = findViewById(R.id.tvClientBalance);
        findViewById(R.id.btnClientDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangeBalance).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeBalanceActivity.class);
            intent.putExtra("client_id", clientId);
            startActivity(intent);
        });
        showClient();
    }

    private void showClient() {
        ClientRecord client = database.getClient(clientId);
        if (client == null) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        welcome.setText("Welcome " + client.name);
        balance.setText(String.format(Locale.getDefault(), "Balance: %.2f", client.balance));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (database != null && welcome != null) showClient();
    }

    @Override
    protected void onDestroy() {
        database.close();
        super.onDestroy();
    }
}