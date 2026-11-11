package com.example.spinnerguess;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class TopTenActivity extends AppCompatActivity {

    private ListView listViewTop10;
    private Button btnBackToGame;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten);

        listViewTop10 = findViewById(R.id.listViewTop10);
        btnBackToGame = findViewById(R.id.btnBackToGame);
        firestore = FirebaseFirestore.getInstance();

        btnBackToGame.setOnClickListener(v -> finish());

        loadTopPlayersFromFirestore();
    }

    private void loadTopPlayersFromFirestore() {
        // جلب أعلى 3 لاعبين مرتبين تنازلياً بحسب النقاط (score) من Firestore
        firestore.collection("users")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> displayList = new ArrayList<>();
                    int rank = 1;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String playerName = document.getString("name");
                        if (playerName == null || playerName.isEmpty()) {
                            playerName = document.getString("email");
                        }

                        Long scoreVal = document.getLong("score");
                        int playerScore = (scoreVal != null) ? scoreVal.intValue() : 0;

                        displayList.add(rank + ". " + playerName + "  -  Score: " + playerScore);
                        rank++;
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            TopTenActivity.this,
                            android.R.layout.simple_list_item_1,
                            displayList
                    );
                    listViewTop10.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(TopTenActivity.this, "Error loading top players: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}