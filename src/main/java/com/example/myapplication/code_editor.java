package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class code_editor extends AppCompatActivity {

    EditText etCode;
    TextView tvOutput, tvQuestion, tvExpected;
    Button btnRun, btnClear;
    Spinner spinnerQuestions, spinnerLanguages;
    DatabaseHelper DB;
    String userEmail;

    // Seçilen dilin API'deki karşılığını tutacak (Örn: "Java" -> "java")
    String selectedLanguageApiName ;
    String selectedLanguageVersion ;

    // Dil Listesi ve Versiyonları
    HashMap<String, String[]> languageMap = new HashMap<>();

    static class CodingQuestion {
        String title;
        String description;
        String expectedOutput;

        public CodingQuestion(String title, String description, String expectedOutput) {
            this.title = title;
            this.description = description;
            this.expectedOutput = expectedOutput;
        }
        @Override
        public String toString() { return title; }
    }

    ArrayList<CodingQuestion> questionList;
    CodingQuestion currentQuestion;

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code_editor);

        // Bağlamalar
        etCode = findViewById(R.id.etCodeEditor);
        tvOutput = findViewById(R.id.tvOutput);
        tvQuestion = findViewById(R.id.tvQuestionText);
        tvExpected = findViewById(R.id.tvExpectedOutput);
        btnRun = findViewById(R.id.btnRunCode);
        btnClear = findViewById(R.id.btnClearCode);
        spinnerQuestions = findViewById(R.id.spinnerQuestions);
        spinnerLanguages = findViewById(R.id.spinnerLanguages);

        DB = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // DİL AYARLARI
        setupLanguages();

        // SORULARI YÜKLE
        loadQuestions();

        // SPINNER (SORULAR)
        ArrayAdapter<CodingQuestion> qAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, questionList);
        spinnerQuestions.setAdapter(qAdapter);

        spinnerQuestions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentQuestion = questionList.get(position);
                updateUI();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // ÇALIŞTIR BUTONU
        btnRun.setOnClickListener(v -> {
            String userCode = etCode.getText().toString();
            if (userCode.trim().isEmpty()) {
                Toast.makeText(code_editor.this, "Kod alanı boş!", Toast.LENGTH_SHORT).show();
            } else {
                tvOutput.setText("🚀 " + selectedLanguageApiName.toUpperCase() + " Derleniyor...");
                tvOutput.setTextColor(getResources().getColor(android.R.color.white));
                runCode(userCode);
            }
        });

        // TEMİZLE BUTONU
        btnClear.setOnClickListener(v -> {
            etCode.setText("");
            tvOutput.setText("");
        });
    }

    private void setupLanguages() {
        // Piston API Dil ve Versiyon Eşleşmeleri
        // Format: { "API_NAME", "VERSION" }
        languageMap.put("Python", new String[]{"python", "3.10.0"});
        languageMap.put("Java", new String[]{"java", "15.0.2"});
        languageMap.put("C++", new String[]{"cpp", "10.2.0"});
        languageMap.put("C", new String[]{"c", "10.2.0"});
        languageMap.put("C#", new String[]{"csharp", "6.12.0"});
        languageMap.put("JavaScript", new String[]{"javascript", "18.15.0"});

        // Spinner Listesi
        String[] languages = {"Python", "Java", "C++", "C", "C#", "JavaScript"};

        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguages.setAdapter(langAdapter);

        spinnerLanguages.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLangLabel = languages[position];
                String[] apiData = languageMap.get(selectedLangLabel);

                if(apiData != null) {
                    selectedLanguageApiName = apiData[0];
                    selectedLanguageVersion = apiData[1];

                    // Dil değişince editöre o dilin başlangıç kodunu koy

                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadQuestions() {
        questionList = new ArrayList<>();
        // ==========================================
        // 🟢 SEVİYE 1: BAŞLANGIÇ (ISINMA TURLARI)
        // ==========================================

        questionList.add(new CodingQuestion(
                "1. Merhaba Dünya (Isınma)",
                "Kodlama dünyasının geleneğidir. Ekrana tam olarak şunu yazdır:\nHello World",
                "Hello World"
        ));

        questionList.add(new CodingQuestion(
                "2. Basit Matematik",
                "Ekrana 8 ile 7'nin toplamını yazdıran kodu yaz.\n(İpucu: 8 + 7 işlemini yaptır)",
                "15"
        ));

        questionList.add(new CodingQuestion(
                "3. Ad ve Soyad",
                "Ekrana alt alta kendi ismini değil, şu isimleri yazdır:\nAli\nVeli\n(Not: Her biri yeni satırda olmalı)",
                "Ali\nVeli"
        ));

        questionList.add(new CodingQuestion(
                "4. Çarpım Tablosu (Basit)",
                "5 kere 5 kaç eder? İşlemi koda yaptırıp sonucu yazdır.",
                "25"
        ));

        // ==========================================
        // 🟡 SEVİYE 2: ORTA (DÖNGÜLER VE KOŞULLAR)
        // ==========================================

        questionList.add(new CodingQuestion(
                "5. Sayı Sayma (Loop)",
                "1'den 3'e kadar olan sayıları alt alta yazdır.\n(Döngü kullanman önerilir)",
                "1\n2\n3"
        ));

        questionList.add(new CodingQuestion(
                "6. Çift mi Tek mi?",
                "Bir değişken tanımla ve değerini 10 yap. Eğer sayı çift ise ekrana 'Çift', tek ise 'Tek' yazdır.",
                "Çift"
        ));

        questionList.add(new CodingQuestion(
                "7. Geri Sayım",
                "5'ten geriye doğru 1'e kadar sayıları alt alta yazdır.\n(5, 4, 3, 2, 1)",
                "5\n4\n3\n2\n1"
        ));

        questionList.add(new CodingQuestion(
                "8. Negatif Kontrolü",
                "Ekrana -5 sayısının mutlak değerini (pozitif halini) yazdıran kodu yaz.\n(Sonuç 5 olmalı)",
                "5"
        ));

        // ==========================================
        // 🔴 SEVİYE 3: İLERİ (ALGORİTMA VE MANTIK)
        // ==========================================

        questionList.add(new CodingQuestion(
                "9. Faktöriyel Hesabı",
                "5 sayısının faktöriyelini hesapla ve sonucu yazdır.\n(5! = 5*4*3*2*1)",
                "120"
        ));

        questionList.add(new CodingQuestion(
                "10. Mini FizzBuzz",
                "1'den 5'e kadar sayıları yazdır. Ancak 3 sayısı yerine 'Fizz' yazdır.\nBeklenen:\n1\n2\nFizz\n4\n5",
                "1\n2\nFizz\n4\n5"
        ));

        questionList.add(new CodingQuestion(
                "11. Yıldız Merdiveni",
                "Döngü kullanarak ekrana şu deseni çizdir:\n*\n**\n***",
                "*\n**\n***"
        ));

        questionList.add(new CodingQuestion(
                "12. Kare Alanı ve Çevresi",
                "Bir kenarı 6 olan karenin önce Alanını (6*6), sonra bir alt satıra Çevresini (6*4) yazdır.",
                "36\n24"
        ));

        questionList.add(new CodingQuestion(
                "13. Üs Alma (Power)",
                "2'nin 5. kuvvetini (2 üzeri 5) hesaplayıp yazdır.",
                "32"
        ));
    }

    private void updateUI() {
        tvQuestion.setText(currentQuestion.description);
        tvExpected.setText("Beklenen Çıktı: \n" + currentQuestion.expectedOutput);
        tvOutput.setText("");
    }




    private void runCode(String code) {
        executorService.execute(() -> {
            String result = "";
            try {
                URL url = new URL("https://emkc.org/api/v2/piston/execute");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Kodu JSON formatına uygun hale getir (Escape işlemi)
                String escapedCode = code.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "");

                // DİNAMİK JSON OLUŞTURMA (Seçilen dil ve versiyonu kullanıyoruz)
                String jsonInputString = "{" +
                        "\"language\": \"" + selectedLanguageApiName + "\"," +
                        "\"version\": \"" + selectedLanguageVersion + "\"," +
                        "\"files\": [ { \"content\": \"" + escapedCode + "\" } ]" +
                        "}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) response.append(line);

                JSONObject jsonResponse = new JSONObject(response.toString());

                // Hata kontrolü
                if (jsonResponse.has("run")) {
                    result = jsonResponse.getJSONObject("run").getString("output");
                } else if (jsonResponse.has("message")) {
                    result = "Hata: " + jsonResponse.getString("message");
                } else {
                    result = "Bilinmeyen hata.";
                }

            } catch (Exception e) {
                result = "Bağlantı Hatası: " + e.getMessage();
            }

            String finalResult = result.trim();
            handler.post(() -> checkAnswer(finalResult));
        });
    }

    private void checkAnswer(String apiOutput) {
        tvOutput.setText(apiOutput);

        if (apiOutput.equals(currentQuestion.expectedOutput.trim())) {
            tvOutput.setTextColor(getResources().getColor(android.R.color.holo_green_light));

            if(userEmail != null) {
                // Hangi dilde çözdüyse o dile puan veriyoruz!
                String langTitle = selectedLanguageApiName.substring(0, 1).toUpperCase() + selectedLanguageApiName.substring(1);
                // "cpp" -> "C++", "csharp" -> "C#" düzeltmeleri yapılabilir ama veritabanında "C++" ve "C#" diye kayıtlı.

                if(selectedLanguageApiName.equals("cpp")) langTitle = "C++";
                if(selectedLanguageApiName.equals("csharp")) langTitle = "C#";
                if(selectedLanguageApiName.equals("javascript")) langTitle = "JavaScript";
                if(selectedLanguageApiName.equals("python")) langTitle = "Python";
                if(selectedLanguageApiName.equals("java")) langTitle = "Java";
                if(selectedLanguageApiName.equals("c")) langTitle = "C";

                DB.addScore(userEmail, langTitle, 20);
            }

            new AlertDialog.Builder(this)
                    .setTitle("TEBRİKLER! 🎉")
                    .setMessage("Kodun başarıyla çalıştı!\n+20 XP eklendi.")
                    .setPositiveButton("Tamam", null)
                    .show();
        } else {
            tvOutput.setTextColor(getResources().getColor(android.R.color.holo_red_light));
            Toast.makeText(this, "Sonuç beklenenle eşleşmedi.", Toast.LENGTH_SHORT).show();
        }
    }
}