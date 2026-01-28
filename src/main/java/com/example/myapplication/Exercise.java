package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import android.os.Vibrator;
import android.media.ToneGenerator;
import android.media.AudioManager;
import android.content.Context;

public class Exercise extends AppCompatActivity {

    TextView tvLanguageTitle, tvQuestionText, tvQuestionCount;
    RadioGroup radioGroup;
    RadioButton rbOption1, rbOption2, rbOption3, rbOption4;
    Button btnSubmit;

    DatabaseHelper DB;
    ArrayList<QuestionModel> questionList;

    int currentQuestionIndex = 0;
    int score = 0;

    //  Soruların kaç puan kazandıracağını tutan değişken
    int pointsPerQuestion = 10;

    String selectedLanguage;
    String userEmail;
    int correctCount = 0;
    int wrongCount = 0;

    int levelToSolve;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);

        selectedLanguage = getIntent().getStringExtra("SELECTED_LANGUAGE");
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // UI Bağlamaları
        tvLanguageTitle = findViewById(R.id.tvLanguageTitle);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        radioGroup = findViewById(R.id.radioGroupOptions);
        rbOption1 = findViewById(R.id.rbOption1);
        rbOption2 = findViewById(R.id.rbOption2);
        rbOption3 = findViewById(R.id.rbOption3);
        rbOption4 = findViewById(R.id.rbOption4);
        btnSubmit = findViewById(R.id.btnSubmit);

        int userRealLevel = 0;
        levelToSolve = getIntent().getIntExtra("TARGET_LEVEL", userRealLevel);

        DB = new DatabaseHelper(this);

        // 1. Kullanıcının GERÇEK seviyesini veritabanından öğren
        userRealLevel = DB.getUserLevel(userEmail, selectedLanguage);

        // 2. Bu aktiviteye dışarıdan (Intent ile) belirli bir seviye gönderildi mi?
        // Eğer gönderilmediyse (0 ise), varsayılan olarak kullanıcının kendi seviyesini aç.
        int levelToSolve = getIntent().getIntExtra("TARGET_LEVEL", userRealLevel);

        // 3. PUANLAMA MANTIĞI
        // Eğer çözülen testin seviyesi, kullanıcının gerçek seviyesinden düşükse (Eski test)
        if (levelToSolve < userRealLevel) {
            pointsPerQuestion = 1; // Sadece 1 puan ver
            Toast.makeText(this, "Tekrar Modu: Sorular 1 Puan Değerinde", Toast.LENGTH_LONG).show();
        } else {
            pointsPerQuestion = 10; // Normal Mod: 10 Puan
        }

        // Seçilen seviyenin sorularını getir
        questionList = DB.getQuestionsByLevel(selectedLanguage, levelToSolve);

        tvLanguageTitle.setText(selectedLanguage + " - Seviye " + levelToSolve);

        if (questionList == null || questionList.isEmpty()) {
            Toast.makeText(this, "Bu seviye için henüz soru yok!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            showQuestion();
        }
        if (selectedLanguage.equals("Donanım")) {
            // Örn: Başlık rengini turuncu yap
            tvLanguageTitle.setTextColor(android.graphics.Color.parseColor("#FF5722"));
        }

        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Şık seçildi mi kontrol et
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if (selectedId == -1) {
                    Toast.makeText(Exercise.this, "Lütfen bir şık seçin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                RadioButton selectedRb = findViewById(selectedId);
                String userAnswer = selectedRb.getText().toString();
                String correctAnswer = questionList.get(currentQuestionIndex).getCorrectAnswer();

                // --- SES MOTORUNU HAZIRLA ---
                // STREAM_MUSIC: Medya ses seviyesini kullanır
                // 100: Ses şiddeti (0-100 arası)
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

                if (userAnswer.equals(correctAnswer)) {
                    // ==============================
                    //         DOĞRU CEVAP
                    // ==============================

                    // 1. Puanı ve sayacı güncelle
                    score += pointsPerQuestion;
                    correctCount++;

                    // 2. DOĞRU SESİ (Kısa ve net bir "Bip")
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150); // 150ms sürer

                    // 3. Kullanıcıya Mesaj
                    if(pointsPerQuestion == 10) {
                        Toast.makeText(Exercise.this, "Doğru! +10 Puan 🚀", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Exercise.this, "Doğru (Tekrar) 👍", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    // ==============================
                    //         YANLIŞ CEVAP
                    // ==============================

                    wrongCount++;

                    // 1. TİTREŞİM (VIBRATION) - İsteğin Üzerine Eklendi
                    Vibrator v1 = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (v != null) {
                        // 500 milisaniye (Yarım saniye) boyunca titret
                        // Android API 26+ için yeni metodlar var ama bu eski metod tüm telefonlarda çalışır.
                        v1.vibrate(500);
                    }

                    // 2. YANLIŞ SESİ (Daha kalın bir uyarı tonu)
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

                    // 3. Kullanıcıya Mesaj
                    Toast.makeText(Exercise.this, "Yanlış! Doğru cevap: " + correctAnswer, Toast.LENGTH_LONG).show();
                }

                // --- SONRAKİ SORUYA GEÇİŞ ---
                currentQuestionIndex++;

                if (currentQuestionIndex < questionList.size()) {
                    radioGroup.clearCheck(); // Seçimi temizle
                    showQuestion();
                } else {
                    // Test bitti, hesaplamayı yap
                    finishTest();
                }
            }
        });
    }

    private void showQuestion() {
        QuestionModel currentQ = questionList.get(currentQuestionIndex);
        tvQuestionText.setText(currentQ.getQuestion());
        rbOption1.setText(currentQ.getOption1());
        rbOption2.setText(currentQ.getOption2());
        rbOption3.setText(currentQ.getOption3());
        rbOption4.setText(currentQ.getOption4());
        tvQuestionCount.setText("Soru " + (currentQuestionIndex + 1) + "/" + questionList.size());
    }


    private void finishTest() {
        // --- BAŞARI BARAJI ---

        // 1. Maksimum alınabilecek puanı hesapla (Örn: 10 soru * 10 puan = 100)
        // Not: Tekrar modundaysan (1 puanlık) barajı soru sayısına göre ayarla.
        int maxPossibleScore = questionList.size() * pointsPerQuestion;

        // 2. Barajı belirle (%70 Başarı)
        int passingThreshold = (maxPossibleScore * 70) / 100;

        boolean isSuccess = false;
        boolean isLevelUp = false;

        // 3. KONTROL: Kullanıcı barajı geçti mi?
        if (score >= passingThreshold) {
            isSuccess = true;

            // BAŞARILIYSA: Puanı kaydet
            DB.addScore(userEmail, selectedLanguage, score);

            // Seviye atlama kontrolü (Sadece normal modda)
            if (pointsPerQuestion == 10) {
                isLevelUp = DB.checkLevelUp(userEmail, selectedLanguage);
            }
        } else {
            isSuccess = false;
            // BAŞARISIZSA: DB.addScore ÇAĞIRMIYORUZ! (Puan verilmez)
        }

        // --- SONUÇ EKRANINA GİT ---
        Intent intent = new Intent(Exercise.this, ResultActivity.class);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("SELECTED_LANGUAGE", selectedLanguage);
        intent.putExtra("CURRENT_LEVEL", levelToSolve);
        intent.putExtra("SCORE", score);
        intent.putExtra("CORRECT", correctCount);
        intent.putExtra("WRONG", wrongCount);

        // Bu iki yeni bilgiyi gönderiyoruz
        intent.putExtra("IS_SUCCESS", isSuccess);
        intent.putExtra("IS_LEVEL_UP", isLevelUp);

        startActivity(intent);
        finish();
    }
}