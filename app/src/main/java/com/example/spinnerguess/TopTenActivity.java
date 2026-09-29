package com.example.spinnerguess;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class TopTenActivity extends AppCompatActivity {

    private ListView listViewTop10;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_ten);

        listViewTop10 = findViewById(R.id.listViewTop10);
        dbHelper = new DBHelper(this);

        // جلب أعلى 10 مستخدمين من قاعدة البيانات
        ArrayList<ModelUser> topUsers = dbHelper.getTop10Users();

        // تجهيز النصوص لعرضها في القائمة (الاسم والنقاط)
        ArrayList<String> displayList = new ArrayList<>();
        int rank = 1;
        for (ModelUser user : topUsers) {
            displayList.add(rank + ". " + user.getUserName() + "  -  " + user.getScore() + " نقطة");
            rank++;
        }

        // ربط البيانات بالـ ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                displayList
        );
        listViewTop10.setAdapter(adapter);
    }
}