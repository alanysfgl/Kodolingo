package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class TutorialTopicsActivity extends AppCompatActivity {

    ListView listView;
    TextView tvHeader;
    DatabaseHelper DB;
    String userEmail, language;
    ArrayList<String> topicList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_topics);

        listView = findViewById(R.id.listViewTopics);
        tvHeader = findViewById(R.id.tvLanguageHeader);
        DB = new DatabaseHelper(this);

        userEmail = getIntent().getStringExtra("USER_EMAIL");
        language = getIntent().getStringExtra("LANGUAGE");

        tvHeader.setText(language + " Dersleri");

        loadTopics();
    }

    private void loadTopics() {
        topicList = new ArrayList<>();
        Cursor cursor = DB.getTutorialTopics(language);

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Bu dil için henüz ders eklenmedi.", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                // Sadece başlıkları listeye ekle
                topicList.add(cursor.getString(0)); // title sütunu
            }
        }

        // Listeyi Ekrana Bas
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, topicList);
        listView.setAdapter(adapter);

        // Tıklama Olayı (Dersi Aç)
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // position 0'dan başlar, veritabanında chapter_order 1'den başlar.
                // O yüzden position + 1 gönderiyoruz.
                Intent intent = new Intent(TutorialTopicsActivity.this, TutorialReadActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("LANGUAGE", language);
                intent.putExtra("CHAPTER_ORDER", position + 1); // Seçilen dersin sırası
                startActivity(intent);
            }
        });
    }
}