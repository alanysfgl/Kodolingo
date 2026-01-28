package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Tutorial extends AppCompatActivity {

    CardView cardPython, cardJava, cardCPP, cardCS, cardC, cardJS, cardHardware;
    String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // UI Bağlamaları
        cardPython = findViewById(R.id.cardPython);
        cardJava = findViewById(R.id.cardJava);
        cardCPP = findViewById(R.id.cardCPP);
        cardCS = findViewById(R.id.cardCS);
        cardC = findViewById(R.id.cardC);
        cardJS = findViewById(R.id.cardJS);
        cardHardware = findViewById(R.id.cardHardware);
// Python
        cardPython.setOnClickListener(v -> openTopicList("Python"));

        // Java
        cardJava.setOnClickListener(v -> openTopicList("Java"));

        // C++
        cardCPP.setOnClickListener(v -> openTopicList("C++"));

        // C#
        cardCS.setOnClickListener(v -> openTopicList("C#"));

        // C Dili
        cardC.setOnClickListener(v -> openTopicList("C"));

        // JavaScript
        cardJS.setOnClickListener(v -> openTopicList("JavaScript"));

        // Donanım
        cardHardware.setOnClickListener(v -> openTopicList("Donanım"));
    }

    // Kod tekrarını önleyen yardımcı metod
    private void openTopicList(String language) {
        Intent intent = new Intent(Tutorial.this, TutorialTopicsActivity.class);
        intent.putExtra("USER_EMAIL", userEmail);
        intent.putExtra("LANGUAGE", language); // Hangi dile tıklandığını gönderiyoruz
        startActivity(intent);
    }
}