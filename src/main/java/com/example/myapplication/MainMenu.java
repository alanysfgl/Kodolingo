package com.example.myapplication;

import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.SharedPreferences;
import android.location.Location;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;

public class MainMenu extends AppCompatActivity {


    TextView tvWelcome, tvUserRank, tvRankIcon;
    CardView cardProgramLang, cardTutorial, cardSetting, cardLogout,cardHardware,cardCodeEditor;
    DatabaseHelper DB;
    String userEmail;
    ProgressBar progressBarRank; TextView tvXpProgress;
    ImageView imgUserAvatar;
    String[] rankNames = {
            "Stajyer",       // Rütbe 1
            "Junior Dev",    // Rütbe 2
            "Mid-Level Dev", // Rütbe 3
            "Senior Dev",    // Rütbe 4
            "Tech Lead",     // Rütbe 5
            "Architect",     // Rütbe 6
            "CTO",           // Rütbe 7
            "Legend"         // Rütbe 8+
    };
    FusedLocationProviderClient fusedLocationClient; // Konum servisi
    private static final String CHANNEL_ID = "base_camp_channel"; // Bildirim kanalı kimliği

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        tvWelcome = findViewById(R.id.tvWelcomeName);
        cardProgramLang = findViewById(R.id.cardAlgo);
        cardLogout = findViewById(R.id.cardLogout);
        cardTutorial =findViewById(R.id.cardTutorial);
        cardSetting=findViewById(R.id.cardSettings);
        cardHardware=findViewById(R.id.cardHardware);
        cardCodeEditor=findViewById(R.id.cardCodeEditor);


        tvUserRank = findViewById(R.id.tvUserRank);
        tvRankIcon = findViewById(R.id.tvRankIcon);


        progressBarRank = findViewById(R.id.progressBarRank);
        tvXpProgress = findViewById(R.id.tvXpProgress);

        imgUserAvatar = findViewById(R.id.imgUserAvatar);

        // Veritabanını başlat
        DB = new DatabaseHelper(this);

        // Önceki sayfadan (Login) gelen E-postayı al
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("USER_EMAIL");

        // Eğer email geldiyse, veritabanından ismini çek ve yazdır
        if (userEmail != null) {
            String userName = DB.getName(userEmail);
            tvWelcome.setText("Merhaba, " + userName + "!");
        }


        // 1. Konum Servisini Başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2. Bildirim Kanalını Oluştur
        createNotificationChannel();

        // 3. Ana Üs Kontrolü Yap
        checkIfAtBaseCamp();

        // --- BUTON TIKLAMA OLAYLARI ---

        // Programlama Dilleri Kartı
        cardProgramLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainMenu.this, "Programlama dilleri yükleniyor...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainMenu.this, ProgramLanguage.class);
                // Kullanıcı bilgisini kaybetmemek için bir sonraki sayfaya da taşıyoruz
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });

        // Önce tanımla: CardView cardCodeEditor;
        // Sonra findViewById ile bağla...

        cardCodeEditor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, code_editor.class);
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });

        cardTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainMenu.this, "Tutorial'lar yükleniyor...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainMenu.this, Tutorial.class);
                // Kullanıcı bilgisini kaybetmemek için bir sonraki sayfaya da taşıyoruz
                intent.putExtra("USER_EMAIL", userEmail);
                startActivity(intent);
            }
        });

        // Çıkış Yap Kartı
        cardLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Giriş ekranına geri dön ve bu sayfayı kapat
                Intent intent = new Intent(MainMenu.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Ayarlar Kartı
        cardSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, SettingsActivity.class);
                // Email göndermeyi unutma, yoksa SettingsActivity açılınca çöker
                intent.putExtra("USER_EMAIL", getIntent().getStringExtra("USER_EMAIL"));
                startActivity(intent);
            }
        });

        // DONANIM KARTI
        cardHardware.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, LevelSelectionActivity.class);
                intent.putExtra("USER_EMAIL", userEmail);

                // Burası ÇOK ÖNEMLİ: Veritabanındaki "language" sütunuyla aynı olmalı
                intent.putExtra("SELECTED_LANGUAGE", "Donanım");

                startActivity(intent);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

    }




    // SAYFAYA HER GERİ DÖNÜLDÜĞÜNDE RÜTBEYİ GÜNCELLE
    @Override
    protected void onResume() {
        super.onResume();
        updateRank();
        loadUserAvatar();
    }



    private void updateRank() {
        // 1. Kullanıcının TOPLAM PUANINI çek
        int totalXP = DB.getTotalUserScore(userEmail);

        // --- ZORLAŞAN SEVİYE ALGORİTMASI ---
        int currentRank = 1;
        int requiredXP = 900; // İlk seviye için gereken (Base XP)
        int remainingXP = totalXP; // Hesaplama için geçici değişken

        // Döngü: Puanımız barajı geçtiği sürece rütbe artır ve barajı ikiye katla
        while (remainingXP >= requiredXP) {
            remainingXP = remainingXP - requiredXP; // Barajı geçince puanı eksilt (Sonraki seviyeye sıfırdan başlasın)
            currentRank++;       // Rütbe atla
            requiredXP = requiredXP * 2; // ZORLUK İKİ KATINA ÇIKTI (900 -> 1800 -> 3600...)
        }

        // --- EKRANA YAZDIRMA ---

        // 1. Rütbe İsmi
        String rankTitle = "Kodlayıcı";
        if (currentRank <= rankNames.length) {
            rankTitle = rankNames[currentRank - 1];
        } else {
            rankTitle = rankNames[rankNames.length - 1];
        }

        tvUserRank.setText(rankTitle + " (Rütbe " + currentRank + ")");

        // 2. Progress Bar ve Puan Yazısı
        // requiredXP = Şu anki rütbeyi bitirmek için gereken toplam puan
        // remainingXP = Şu anki rütbede kazandığın puan
        progressBarRank.setMax(requiredXP);
        progressBarRank.setProgress(remainingXP);

        tvXpProgress.setText(remainingXP + " / " + requiredXP + " XP");

        // --- RENK VE İKON AYARLARI (AYNI) ---
        String firstLetter = rankTitle.substring(0, 1);
        tvRankIcon.setText(firstLetter);

        int colorCode;
        switch (currentRank) {
            case 1: colorCode = android.graphics.Color.parseColor("#9E9E9E"); break; // Gri
            case 2: colorCode = android.graphics.Color.parseColor("#4CAF50"); break; // Yeşil
            case 3: colorCode = android.graphics.Color.parseColor("#03A9F4"); break; // Mavi
            case 4: colorCode = android.graphics.Color.parseColor("#9C27B0"); break; // Mor
            case 5: colorCode = android.graphics.Color.parseColor("#FF9800"); break; // Turuncu
            case 6:
            case 7: colorCode = android.graphics.Color.parseColor("#FFD700"); break; // Altın
            default: colorCode = android.graphics.Color.parseColor("#D50000"); break; // Kırmızı
        }
        tvRankIcon.setBackgroundColor(colorCode);
    }

    // --- BİLDİRİM VE KONUM METODLARI ---

    private void checkIfAtBaseCamp() {
        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    // Kayıtlı Üs Var mı?
                    SharedPreferences prefs = getSharedPreferences("BaseCampPrefs", MODE_PRIVATE);
                    boolean hasBase = prefs.getBoolean("HasBase", false);

                    if (hasBase) {
                        float baseLat = prefs.getFloat("BaseLat", 0);
                        float baseLng = prefs.getFloat("BaseLng", 0);

                        float[] results = new float[1];
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(), baseLat, baseLng, results);
                        float distanceInMeters = results[0];

                        // 100 Metreden yakınsa BİLDİRİM GÖNDER
                        if (distanceInMeters < 100) {
                            sendBaseCampNotification();
                        }
                    }
                }
            });
        }
    }

    private void sendBaseCampNotification() {
        // Bildirime tıklayınca ne olsun? (Uygulama zaten açık ama yine de MainMenu'yu tazeleyelim)
        Intent intent = new Intent(this, MainMenu.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map) // İstersen kendi ikonunu koy: R.drawable.ic_base
                .setContentTitle("🏠 Ana Üsse Hoş Geldin!")
                .setContentText("Burada çözdüğün testlerden 2 KAT PUAN kazanacaksın! 🚀")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true); // Tıklayınca kaybolsun

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Android 13+ Bildirim izni kontrolü
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            // Bildirimi Gönder (ID: 1)
            notificationManager.notify(1, builder.build());
        }
    }

    private void createNotificationChannel() {
        // Android 8.0 (API 26) ve üzeri için kanal oluşturmak şarttır
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Ana Üs Bildirimleri";
            String description = "Konum bonusu bildirimleri";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void loadUserAvatar() {
        if (userEmail == null) return;

        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String key = "selected_avatar_" + userEmail;
        // Kayıtlı avatarı bul, yoksa 'avatar_1' getir
        String savedAvatarName = prefs.getString(key, "avatar_1");

        int resId;
        switch (savedAvatarName) {
            case "avatar_2": resId = R.drawable.avatar_2; break;
            case "avatar_3": resId = R.drawable.avatar_3; break;
            case "avatar_4": resId = R.drawable.avatar_4; break;
            case "avatar_5": resId = R.drawable.avatar_5; break;
            case "avatar_6": resId = R.drawable.avatar_6; break;
            default: resId = R.drawable.avatar_1; break;
        }

        if (imgUserAvatar != null) {
            imgUserAvatar.setImageResource(resId);
        }
    }

}


