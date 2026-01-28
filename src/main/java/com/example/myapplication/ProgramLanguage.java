package com.example.myapplication;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
public class ProgramLanguage extends AppCompatActivity {


    CardView cardPython, cardJava, cardCpp, cardCsharp, cardC,cardJavascript;
    String userEmail; // Kullanıcı bilgisini taşımak için

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_program_language);

        // 1. Bir önceki sayfadan (MainMenu) gelen kullanıcı mailini al
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // 2. Görünümleri Tanımla
        cardPython = findViewById(R.id.cardPython);
        cardJava = findViewById(R.id.cardJava);
        cardCpp = findViewById(R.id.cardCpp);
        cardCsharp = findViewById(R.id.cardCsharp);
        cardC=findViewById(R.id.cardC);
        cardJavascript=findViewById(R.id.Javascript);

        // 3. Tıklama Olayları
        cardPython.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("Python");
            }
        });

        cardJava.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("Java");
            }
        });

        cardCpp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("C++");
            }
        });

        cardCsharp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("C#");
            }
        });
        cardC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("C");
            }
        }); cardJavascript.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExercise("JavaScript");
            }
        });

    }

    // Ortak Fonksiyon: Seçilen dili bir sonraki sayfaya (Egzersiz) gönderecek
    private void openExercise(String language) {
        Toast.makeText(this, language + " seçildi, geçiş yapılıyor...", Toast.LENGTH_SHORT).show();

        //İleride ExerciseActivity (Soru Ekranı) yapıldığında burası açılacak:

        Intent intent = new Intent(ProgramLanguage.this, LevelSelectionActivity.class);
        intent.putExtra("SELECTED_LANGUAGE", language);
        intent.putExtra("USER_EMAIL", userEmail);
        startActivity(intent);

    }
}