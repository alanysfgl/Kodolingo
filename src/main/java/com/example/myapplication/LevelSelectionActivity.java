package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LevelSelectionActivity extends AppCompatActivity {

    Button btnLvl1, btnLvl2, btnLvl3;
    TextView tvTitle;
    DatabaseHelper DB;
    String userEmail, selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_selection);

        // Verileri al
        userEmail = getIntent().getStringExtra("USER_EMAIL");
        selectedLanguage = getIntent().getStringExtra("SELECTED_LANGUAGE");

        tvTitle = findViewById(R.id.tvLevelTitle);
        tvTitle.setText(selectedLanguage + " Seviyeleri");

        btnLvl1 = findViewById(R.id.btnLevel1);
        btnLvl2 = findViewById(R.id.btnLevel2);
        btnLvl3 = findViewById(R.id.btnLevel3);

        DB = new DatabaseHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Sayfaya her geri dönüldüğünde seviyeleri tekrar kontrol et (Belki seviye atladı)
        updateLevelButtons();
    }

    private void updateLevelButtons() {
        // Kullanıcının veritabanındaki güncel seviyesini çek
        int userCurrentLevel = DB.getUserLevel(userEmail, selectedLanguage);

        // --- LEVEL 1 (Herkes girebilir) ---
        setupButton(btnLvl1, 1, userCurrentLevel);

        // --- LEVEL 2 ---
        if (userCurrentLevel >= 2) {
            setupButton(btnLvl2, 2, userCurrentLevel);
        } else {
            lockButton(btnLvl2, 2);
        }

        // --- LEVEL 3 ---
        if (userCurrentLevel >= 3) {
            setupButton(btnLvl3, 3, userCurrentLevel);
        } else {
            lockButton(btnLvl3, 3);
        }
    }

    // Butonu aktifleştiren ve tıklama özelliği veren yardımcı metod
    private void setupButton(Button btn, int levelNo, int userMaxLevel) {
        btn.setEnabled(true);
        btn.setBackgroundColor(Color.parseColor("#4DABAA")); // Aktif Rengi (Teal)

        // Eğer bu seviye kullanıcının zaten geçtiği bir seviye ise yanına (Tamamlandı) yazabilirsin
        if (userMaxLevel > levelNo) {
            btn.setText("SEVİYE " + levelNo + "\n(Tamamlandı - Tekrar Et)");
            btn.setBackgroundColor(Color.parseColor("#FF9800")); // Turuncu (Tekrar modu rengi)
        } else {
            btn.setText("SEVİYE " + levelNo);
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LevelSelectionActivity.this, Exercise.class);
                intent.putExtra("USER_EMAIL", userEmail);
                intent.putExtra("SELECTED_LANGUAGE", selectedLanguage);
                intent.putExtra("TARGET_LEVEL", levelNo); // Hedef seviyeyi gönderiyoruz!
                startActivity(intent);
            }
        });
    }

    // Butonu kilitleyen metod
    private void lockButton(Button btn, int levelNo) {
        btn.setEnabled(false);
        btn.setText("SEVİYE " + levelNo + " 🔒");
        btn.setBackgroundColor(Color.parseColor("#757575")); // Gri
    }
}