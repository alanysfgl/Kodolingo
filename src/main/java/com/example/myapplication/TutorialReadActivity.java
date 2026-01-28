package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TutorialReadActivity extends AppCompatActivity {

    TextView tvTitle, tvContent, tvProgress;
    Button btnNext, btnPrev;
    DatabaseHelper DB;

    String userEmail, language;
    int currentChapter = 1;
    int totalChapters = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutoial_read);

        tvTitle = findViewById(R.id.tvLessonTitle);
        tvContent = findViewById(R.id.tvLessonContent);
        tvProgress = findViewById(R.id.tvProgress); // XML'e eklemeyi unutma
        btnNext = findViewById(R.id.btnNext);       // XML'e eklemeyi unutma
        btnPrev = findViewById(R.id.btnPrev);       // XML'e eklemeyi unutma

        DB = new DatabaseHelper(this);

        // Verileri Al
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        language = getIntent().getStringExtra("LANGUAGE");
        currentChapter = getIntent().getIntExtra("CHAPTER_ORDER", 1);

        // Toplam ders sayısını bul
        totalChapters = DB.getTutorialCount(language);

        loadLesson();

        // İLERİ BUTONU
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentChapter < totalChapters) {
                    currentChapter++;
                    loadLesson();
                } else {
                    // Son ders bitti, puan ver ve çık
                    completeCourse();
                }
            }
        });

        // GERİ BUTONU
        btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentChapter > 1) {
                    currentChapter--;
                    loadLesson();
                } else {
                    Toast.makeText(TutorialReadActivity.this, "İlk derstesiniz.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    private void loadLesson() {
        Cursor cursor = DB.getTutorialByOrder(language, currentChapter);

        if (cursor.moveToFirst()) {
            String title = cursor.getString(0);
            String content = cursor.getString(1);

            tvTitle.setText(title);
            tvContent.setText(content);
            tvProgress.setText(currentChapter + " / " + totalChapters);

            // --- BUTON RENK VE METİN AYARLARI ---
            if(currentChapter == totalChapters) {
                // SON DERS: Yeşil ve "TAMAMLA"
                btnNext.setText("TAMAMLA ✔");
                btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50"))); // Yeşil
            } else {
                // NORMAL DERS: Teal ve "İLERİ"
                btnNext.setText("İLERİ >");
                btnNext.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4DABAA"))); // Teal
            }

            // Geri butonu kontrolü (İlk derste pasif yapalım)
            if (currentChapter == 1) {
                btnPrev.setEnabled(false);
                btnPrev.setAlpha(0.5f); // Soluklaştır
            } else {
                btnPrev.setEnabled(true);
                btnPrev.setAlpha(1f); // Normal
            }

        } else {
            tvContent.setText("İçerik yüklenemedi.");
        }
    }

    private void completeCourse() {
        // Her ders bitiminde veya sadece kurs sonunda puan verebilirsin.
        // Burada bitişte 100 Puan verelim
        DB.addScore(userEmail, language, 100);
        Toast.makeText(this, "Kurs Tamamlandı! +100 XP", Toast.LENGTH_LONG).show();
        finish();
    }
}