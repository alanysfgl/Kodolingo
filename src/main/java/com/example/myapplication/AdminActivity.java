package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;

public class AdminActivity extends AppCompatActivity {

    ListView listView;
    Button btnLogout;
    DatabaseHelper DB;

    // Ekranda görünen liste
    ArrayList<String> displayList;
    // Arka planda e-postaları tutan liste (Silme işlemi için ID görevi görür)
    ArrayList<String> emailList;

    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        listView = findViewById(R.id.listViewUsers);
        btnLogout = findViewById(R.id.btnAdminLogout);
        DB = new DatabaseHelper(this);

        displayList = new ArrayList<>();
        emailList = new ArrayList<>();

        // Kullanıcıları Listele
        loadUserList();

        // LİSTEDEKİ BİR KİŞİYE TIKLAYINCA
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Tıklanan kişinin e-postasını al
                String selectedEmail = emailList.get(position);

                // Detayları gösteren dialog aç
                showUserOptionsDialog(selectedEmail);
            }
        });

        // ÇIKIŞ YAP
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadUserList() {
        displayList.clear();
        emailList.clear();

        Cursor cursor = DB.getAllUsers();

        if (cursor.getCount() == 0) {
            Toast.makeText(this, "Kayıtlı kullanıcı yok.", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                // Veritabanı sütunları: 0=email, 1=password, 2=name (Sıraya dikkat et)
                // Genelde: email(0), password(1), name(2) diye tanımlamıştık
                String email = cursor.getString(0);
                String password = cursor.getString(1);
                String name = cursor.getString(2);

                // Listede görünecek yazı
                displayList.add("👤 " + name + "\n📧 " + email);

                // Gizli listeye e-postayı kaydet (Pozisyon eşleşmesi için)
                emailList.add(email);
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);
    }

    // KULLANICI DETAY VE BANLAMA PENCERESİ
    // GELİŞMİŞ KULLANICI DETAY VE BANLAMA PENCERESİ
    private void showUserOptionsDialog(String email) {
        // 1. Temel Bilgileri Çek (İsim, Şifre)
        Cursor userCursor = DB.getAllUsers();
        String password = "Bilinmiyor";
        String name = "Bilinmiyor";

        while(userCursor.moveToNext()){
            if(userCursor.getString(0).equals(email)){
                password = userCursor.getString(1); // Şifre sütunu
                name = userCursor.getString(2);     // İsim sütunu
                break;
            }
        }
        userCursor.close(); // İmleci kapatmayı unutma

        // 2. İlerleme Bilgilerini Çek (Puanlar, Seviyeler)
        StringBuilder progressInfo = new StringBuilder();
        Cursor progressCursor = DB.getUserProgressDetails(email);

        if (progressCursor.getCount() == 0) {
            progressInfo.append("❌ Henüz hiç ders çalışmamış.");
        } else {
            progressInfo.append("📊 İLERLEME RAPORU:\n");
            progressInfo.append("----------------------------\n");
            while (progressCursor.moveToNext()) {
                String lang = progressCursor.getString(0);
                int score = progressCursor.getInt(1);
                int level = progressCursor.getInt(2);

                // Örn: Python: Seviye 2 (150 XP)
                progressInfo.append("🔹 ").append(lang)
                        .append(": Seviye ").append(level)
                        .append(" (").append(score).append(" XP)\n");
            }
        }
        progressCursor.close();

        // 3. DİALOG OLUŞTUR
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("👤 " + name); // Başlıkta isim yazsın

        // Mesaj içeriğini oluşturuyoruz
        String message =
                "📧 E-posta: " + email + "\n" +
                        "🔑 Şifre: " + password + "\n\n" +
                        progressInfo.toString() + "\n\n" +
                        "⚠️ Bu kullanıcıyı silmek istiyor musunuz?";

        builder.setMessage(message);

        // BANLA BUTONU
        builder.setPositiveButton("KULLANICIYI SİL 🚫", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean isDeleted = DB.deleteUser(email);
                if (isDeleted) {
                    Toast.makeText(AdminActivity.this, "Kullanıcı ve tüm verileri silindi!", Toast.LENGTH_SHORT).show();
                    loadUserList(); // Listeyi yenile
                } else {
                    Toast.makeText(AdminActivity.this, "Silme işlemi başarısız.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // İPTAL BUTONU
        builder.setNegativeButton("Kapat", null);

        builder.show();
    }
}