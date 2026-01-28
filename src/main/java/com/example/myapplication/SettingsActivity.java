
package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.textfield.TextInputEditText;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.LayoutInflater;
import androidx.cardview.widget.CardView;



public  class SettingsActivity extends AppCompatActivity {

    // UI Bileşenleri
    TextInputEditText etName, etNewPass;
    TextView tvProfileEmail, tvTotalScore, tvArrowIcon, tvNoData;
    Button btnSaveProfile, btnUpdatePass, btnReset, btnDelete,btnSaveBaseCamp;

    // Açılır Kapanır Menu İçin Layoutlar
    LinearLayout layoutScoreHeader, layoutDetailSection, layoutStatsContainer;

    DatabaseHelper DB;
    String userEmail;
    boolean isExpanded = false; // Listenin açık/kapalı durumu

    ImageView imgProfileAvatar;


    TextView tvWifiStatus, tvLocationStatus;
    Button btnCheckStatus;
    FusedLocationProviderClient fusedLocationClient;

    SwitchCompat switchDarkMode;
    SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Veritabanı ve Intent Başlatma
        DB = new DatabaseHelper(this);
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        // --- ID BAĞLAMALARI ---


        tvTotalScore = findViewById(R.id.tvTotalScore);

        // Açılır Menü Bileşenleri
        layoutScoreHeader = findViewById(R.id.layoutScoreHeader);
        layoutDetailSection = findViewById(R.id.layoutDetailSection);
        layoutStatsContainer = findViewById(R.id.layoutStatsContainer);
        tvArrowIcon = findViewById(R.id.tvArrowIcon);
        tvNoData = findViewById(R.id.tvNoData);

        // Butonlar

        etNewPass = findViewById(R.id.etNewPassword);
        btnUpdatePass = findViewById(R.id.btnUpdatePassword);
        btnReset = findViewById(R.id.btnResetProgress);
        btnDelete = findViewById(R.id.btnDeleteAccount);
        btnSaveBaseCamp=findViewById(R.id.btnSaveBaseCamp);

        imgProfileAvatar = findViewById(R.id.imgProfileAvatar);

        // Sayfa açılınca verileri yükle
        loadUserProfile();
        loadSavedAvatar();

        // --- 1. AÇILIR/KAPANIR MENU TIKLAMA OLAYI ---
        layoutScoreHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDetails();
            }
        });

        imgProfileAvatar.setOnClickListener(v -> {
            showAvatarPickerDialog();
        });


        tvWifiStatus = findViewById(R.id.tvWifiStatus);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        btnCheckStatus = findViewById(R.id.btnCheckStatus);

        // Konum servisini başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // BUTONA TIKLAYINCA KONTROL ET
        btnCheckStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkWifiConnection();
                checkUserLocation();
            }

        });









        // --- 3. ŞİFRE GÜNCELLEME ---
        btnUpdatePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPass = etNewPass.getText().toString();
                if (newPass.isEmpty()) {
                    Toast.makeText(SettingsActivity.this, "Yeni şifre giriniz", Toast.LENGTH_SHORT).show();
                } else {
                    if(DB.updatePassword(userEmail, newPass)){
                        Toast.makeText(SettingsActivity.this, "Şifre değiştirildi!", Toast.LENGTH_SHORT).show();
                        etNewPass.setText("");
                    }
                }
            }
        });

        // --- 4. İLERLEMEYİ SIFIRLAMA ---
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Dikkat")
                        .setMessage("Tüm puanlarınız ve seviyeleriniz silinecek. Emin misiniz?")
                        .setPositiveButton("Evet, Sıfırla", (dialog, which) -> {
                            DB.resetUserProgress(userEmail);
                            loadUserProfile(); // Ekranı yenile (Puanları sıfırla)
                            Toast.makeText(SettingsActivity.this, "Tüm ilerleme silindi.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Hayır", null)
                        .show();
            }
        });

        // --- 5. HESABI SİLME ---
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Hesabı Sil")
                        .setMessage("Hesabınız kalıcı olarak silinecek. Bu işlem geri alınamaz!")
                        .setPositiveButton("SİL", (dialog, which) -> {
                            DB.deleteUserAccount(userEmail);
                            Toast.makeText(SettingsActivity.this, "Hesabınız silindi.", Toast.LENGTH_LONG).show();

                            // Giriş sayfasına yönlendir ve geçmişi temizle
                            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        })
                        .setNegativeButton("İptal", null)
                        .show();
            }
        });




        // ANA ÜS KAYDETME BUTONU
        btnSaveBaseCamp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCurrentLocationAsBase();
            }
        });

    }



    // --- YARDIMCI METODLAR ---

    // Menüyü Açma/Kapama Animasyonu
    private void toggleDetails() {
        if (isExpanded) {
            // Açıksa KAPAT
            layoutDetailSection.setVisibility(View.GONE);
            tvArrowIcon.animate().rotation(0).setDuration(300).start();
            isExpanded = false;
        } else {
            // Kapalıysa AÇ
            layoutDetailSection.setVisibility(View.VISIBLE);
            tvArrowIcon.animate().rotation(180).setDuration(300).start();
            isExpanded = true;
        }
    }

    // Verileri Yükleme ve Listeyi Oluşturma
    private void loadUserProfile() {


        // Toplam Puanı Getir (Header için)
        int totalScore = DB.getTotalUserScore(userEmail);
        tvTotalScore.setText(String.valueOf(totalScore));

        // --- DİNAMİK LİSTE OLUŞTURMA ---
        layoutStatsContainer.removeAllViews(); // Eski listeyi temizle

        Cursor cursor = DB.getUserProgressDetails(userEmail);

        if (cursor.getCount() == 0) {
            tvNoData.setVisibility(View.VISIBLE);
        } else {
            tvNoData.setVisibility(View.GONE);

            while (cursor.moveToNext()) {
                // Veritabanından oku: 0=Dil, 1=Puan, 2=Seviye (Sorgudaki sıraya göre)
                String lang = cursor.getString(0);
                int score = cursor.getInt(1);
                int level = cursor.getInt(2);

                // Satır (Row) Oluştur
                LinearLayout row = new LinearLayout(this);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, 20, 0, 20);
                row.setGravity(Gravity.CENTER_VERTICAL);

                // 1. Dil İsmi Text
                TextView tvLang = new TextView(this);
                tvLang.setText(lang);
                tvLang.setTextColor(Color.parseColor("#333333"));
                tvLang.setTextSize(16);
                tvLang.setTypeface(null, Typeface.BOLD);
                tvLang.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));

                // 2. Seviye Text
                TextView tvLevel = new TextView(this);
                tvLevel.setText("Seviye " + level);
                tvLevel.setTextColor(Color.parseColor("#4DABAA"));
                tvLevel.setGravity(Gravity.CENTER);
                tvLevel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                // 3. Puan Text
                TextView tvScore = new TextView(this);
                tvScore.setText(score + " XP");
                tvScore.setTextColor(Color.parseColor("#FF9800"));
                tvScore.setTypeface(null, Typeface.BOLD);
                tvScore.setGravity(Gravity.END);
                tvScore.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                // Elemanları satıra ekle
                row.addView(tvLang);
                row.addView(tvLevel);
                row.addView(tvScore);

                // Satırı ana kutuya ekle
                layoutStatsContainer.addView(row);

                // İnce gri çizgi (Ayırıcı) ekle
                View line = new View(this);
                line.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
                line.setBackgroundColor(Color.parseColor("#F0F0F0"));
                layoutStatsContainer.addView(line);
            }
        }
        cursor.close();
    }

    // 1. WI-FI KONTROL METODU
    private void checkWifiConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            tvWifiStatus.setText("Bağlı (Wi-Fi)");
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
            tvWifiStatus.setText("Bağlı (Mobil Veri)");
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        } else {
            tvWifiStatus.setText("İnternet Yok");
            tvWifiStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    // 2. KONUM KONTROL METODU
    private void checkUserLocation() {
        // İzin var mı kontrol et
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // İzin yoksa iste
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            // İzin varsa konumu al
            tvLocationStatus.setText("Konum aranıyor...");

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                // Koordinatları Şehir İsmine Çevir (Geocoder)
                                try {
                                    Geocoder geocoder = new Geocoder(SettingsActivity.this, Locale.getDefault());
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                                    String city = addresses.get(0).getAdminArea(); // Şehir (Örn: İstanbul)
                                    String district = addresses.get(0).getSubAdminArea(); // İlçe (Örn: Kadıköy)

                                    tvLocationStatus.setText(city + ", " + district);
                                    tvLocationStatus.setTextColor(getResources().getColor(android.R.color.black));

                                } catch (IOException e) {
                                    tvLocationStatus.setText("Adres bulunamadı (GPS: " + location.getLatitude() + ")");
                                }
                            } else {
                                tvLocationStatus.setText("Konum kapalı veya bulunamadı.");
                            }
                        }
                    });
        }
    }
    // --- YENİ AVATAR METODLARI ---

    // Alt Seçim Penceresini Gösterir
    private void showAvatarPickerDialog() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Tasarımı bağla
        View bottomSheetView = LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.layout_bottom_sheet_avatar, findViewById(R.id.layoutHeader)); // layoutHeader yerine null da olabilir ama bu daha güvenli

        // Seçenekleri bul
        CardView opt1 = bottomSheetView.findViewById(R.id.avatarOption1);
        CardView opt2 = bottomSheetView.findViewById(R.id.avatarOption2);
        CardView opt3 = bottomSheetView.findViewById(R.id.avatarOption3);
        CardView opt4 = bottomSheetView.findViewById(R.id.avatarOption4);
        CardView opt5 = bottomSheetView.findViewById(R.id.avatarOption5);
        CardView opt6 = bottomSheetView.findViewById(R.id.avatarOption6);

        // Tıklama Olayları - Hangisine tıklanırsa onu seç
        opt1.setOnClickListener(v -> selectAvatar("avatar_1", R.drawable.avatar_1, bottomSheetDialog));
        opt2.setOnClickListener(v -> selectAvatar("avatar_2", R.drawable.avatar_2, bottomSheetDialog));
        opt3.setOnClickListener(v -> selectAvatar("avatar_3", R.drawable.avatar_3, bottomSheetDialog));
        opt4.setOnClickListener(v -> selectAvatar("avatar_4", R.drawable.avatar_4, bottomSheetDialog));
        opt5.setOnClickListener(v -> selectAvatar("avatar_5", R.drawable.avatar_5, bottomSheetDialog));
        opt6.setOnClickListener(v -> selectAvatar("avatar_6", R.drawable.avatar_6, bottomSheetDialog));

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    // Seçilen Avatarı İşler ve Kaydeder
    private void selectAvatar(String avatarName, int resourceId, BottomSheetDialog dialog) {
        // 1. Görüntüyü anında güncelle
        imgProfileAvatar.setImageResource(resourceId);

        // 2. Seçimi Hafızaya Kaydet (Sadece ismini kaydediyoruz örn: "avatar_3")
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String key = "selected_avatar_" + (userEmail != null ? userEmail : "default");
        editor.putString(key, avatarName);
        editor.apply();

        // 3. Pencereyi kapat
        dialog.dismiss();
        Toast.makeText(this, "Avatar Güncellendi! 😎", Toast.LENGTH_SHORT).show();
    }

    // Kayıtlı Avatarı Yükler
    private void loadSavedAvatar() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String key = "selected_avatar_" + (userEmail != null ? userEmail : "default");
        // Varsayılan olarak "avatar_1" olsun
        String savedAvatarName = prefs.getString(key, "avatar_1");

        // Kayıtlı isme göre doğru resmi bul ve göster
        int resId;
        switch (savedAvatarName) {
            case "avatar_2": resId = R.drawable.avatar_2; break;
            case "avatar_3": resId = R.drawable.avatar_3; break;
            case "avatar_4": resId = R.drawable.avatar_4; break;
            case "avatar_5": resId = R.drawable.avatar_5; break;
            case "avatar_6": resId = R.drawable.avatar_6; break;
            default: resId = R.drawable.avatar_1; break; // avatar_1 ve diğer durumlar
        }
        imgProfileAvatar.setImageResource(resId);
    }


    // --- KONUMU ANA ÜS OLARAK KAYDET ---
    private void saveCurrentLocationAsBase() {
        // Konum izni var mı?
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // 1. Koordinatları Al
                        float lat = (float) location.getLatitude();
                        float lng = (float) location.getLongitude();

                        // 2. Hafızaya Kaydet (BaseCampPrefs dosyasına)
                        SharedPreferences prefs = getSharedPreferences("BaseCampPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putFloat("BaseLat", lat);
                        editor.putFloat("BaseLng", lng);
                        editor.putBoolean("HasBase", true); // Artık bir üssümüz var
                        editor.apply();

                        // 3. Kullanıcıya Bilgi Ver
                        Toast.makeText(SettingsActivity.this, "✅ Konum 'Ana Üs' olarak kaydedildi!", Toast.LENGTH_LONG).show();

                        // İstersen ekranda da göster
                        tvLocationStatus.setText("Ana Üs Kaydedildi:\nEnlem: " + lat + "\nBoylam: " + lng);
                    } else {
                        Toast.makeText(SettingsActivity.this, "Konum alınamadı. GPS açık mı?", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            // İzin yoksa iste
            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

}

