package com.example.spinnerguess;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class ClientDetailActivity extends AppCompatActivity {
    private String clientId;
    private TextView welcome;
    private TextView balance;
    private DocumentReference clientDocument;
    private ListenerRegistration clientListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_detail);
        clientId = getIntent().getStringExtra("client_id");
        welcome = findViewById(R.id.tvClientWelcome);
        balance = findViewById(R.id.tvClientBalance);
        findViewById(R.id.btnClientDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnChangeBalance).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChangeBalanceActivity.class);
            intent.putExtra("client_id", clientId);
            startActivity(intent);
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || clientId == null) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        clientDocument = FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .collection("clients").document(clientId);
        clientListener = clientDocument.addSnapshotListener((snapshot, error) -> {
            if (isFinishing() || isDestroyed()) return;
            if (error != null) {
                Toast.makeText(this, "Could not load client: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                return;
            }
            showClient(snapshot);
        });
    }

    private void showClient(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            Toast.makeText(this, "Client not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String name = snapshot.getString("name");
        Double clientBalance = snapshot.getDouble("balance");
        welcome.setText("Welcome " + (name == null ? "" : name));
        balance.setText(String.format(Locale.getDefault(), "Balance: %.2f",
                clientBalance == null ? 0 : clientBalance));
    }

    @Override
    protected void onDestroy() {
        if (clientListener != null) clientListener.remove();
        super.onDestroy();
    }
}