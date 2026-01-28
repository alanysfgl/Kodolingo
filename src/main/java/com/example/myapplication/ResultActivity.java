package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color; // Renkler için gerekli
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    TextView tvTitle, tvScore, tvCorrect, tvWrong, tvMessage; // tvMessage yeni eklendi (XML'de yoksa başlığı kullanırız)
    Button btnNext, btnRetry, btnHome;

    String userEmail, selectedLanguage;
    int currentLevel, score, correct, wrong;
    boolean isLevelUp, isSuccess;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // UI Bağlamaları
        tvTitle = findViewById(R.id.tvResultTitle);
        tvScore = findViewById(R.id.tvTotalScore);
        tvCorrect = findViewById(R.id.tvCorrectCount);
        tvWrong = findViewById(R.id.tvWrongCount);
        btnNext = findViewById(R.id.btnNextLevel);
        btnRetry = findViewById(R.id.btnRetry);
        btnHome = findViewById(R.id.btnHome);

        // Verileri Al
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("USER_EMAIL");
        selectedLanguage = intent.getStringExtra("SELECTED_LANGUAGE");
        currentLevel = intent.getIntExtra("CURRENT_LEVEL", 1);
        score = intent.getIntExtra("SCORE", 0);
        correct = intent.getIntExtra("CORRECT", 0);
        wrong = intent.getIntExtra("WRONG", 0);

        isLevelUp = intent.getBooleanExtra("IS_LEVEL_UP", false);
        isSuccess = intent.getBooleanExtra("IS_SUCCESS", false); // Yeni veri

        // İstatistikleri Yaz
        tvScore.setText(String.valueOf(score));
        tvCorrect.setText(String.valueOf(correct));
        tvWrong.setText(String.valueOf(wrong));

        // --- BAŞARI DURUMU KONTROLÜ (GÜNCELLENDİ) ---

        if (isSuccess) {
            // 1. Durum: BAŞARILI (Barajı Geçti)
            if (isLevelUp) {
                tvTitle.setText("HARİKA! 🎉\nSEVİYE ATLADIN");
                tvTitle.setTextColor(Color.parseColor("#4CAF50")); // Yeşil
                btnNext.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setText("TEBRİKLER!\nTESTİ GEÇTİN");
                tvTitle.setTextColor(Color.parseColor("#4CAF50")); // Yeşil
                btnNext.setVisibility(View.GONE); // Seviye atlamadıysa sonraki buton yok
            }
        } else {
            // 2. Durum: BAŞARISIZ (Barajın Altında Kaldı)
            tvTitle.setText("BAŞARISIZ 😔\n(Baraj %70)");
            tvTitle.setTextColor(Color.parseColor("#F44336")); // Kırmızı

            // Puanı kırmızı yapıp yanına not düşebiliriz
            tvScore.setText("0"); // Veritabanına eklenmediği için 0 gösteriyoruz
            tvScore.setTextColor(Color.parseColor("#F44336"));

            btnNext.setVisibility(View.GONE);
        }

        // --- BUTON İŞLEVLERİ (Aynı kalıyor) ---
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextIntent = new Intent(ResultActivity.this, Exercise.class);
                nextIntent.putExtra("USER_EMAIL", userEmail);
                nextIntent.putExtra("SELECTED_LANGUAGE", selectedLanguage);
                nextIntent.putExtra("TARGET_LEVEL", currentLevel + 1);
                startActivity(nextIntent);
                finish();
            }
        });

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent retryIntent = new Intent(ResultActivity.this, Exercise.class);
                retryIntent.putExtra("USER_EMAIL", userEmail);
                retryIntent.putExtra("SELECTED_LANGUAGE", selectedLanguage);
                retryIntent.putExtra("TARGET_LEVEL", currentLevel);
                startActivity(retryIntent);
                finish();
            }
        });

        btnHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(ResultActivity.this, LevelSelectionActivity.class);
                homeIntent.putExtra("USER_EMAIL", userEmail);
                homeIntent.putExtra("SELECTED_LANGUAGE", selectedLanguage);
                homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(homeIntent);
                finish();
            }
        });
    }
}