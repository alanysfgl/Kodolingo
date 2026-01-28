package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView btnGoRegister;
    DatabaseHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);


        btnGoRegister = findViewById(R.id.btnGoRegister);

        DB = new DatabaseHelper(this);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString();

                if(user.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Lütfen E-posta ve Şifrenizi girin.", Toast.LENGTH_SHORT).show();
                }

                // --- ADMIN KONTROLÜ admin ---
                // Kullanıcı adı: admin
                // Şifre: 1234
                else if (user.equals("admin") && pass.equals("1234")) {
                    Toast.makeText(MainActivity.this, "Yönetici Girişi Yapıldı 🛠️", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                    startActivity(intent);
                    finish();
                }


                else {
                    Boolean checkuserpass = DB.checkEmailPassword(user, pass);
                    if(checkuserpass) {
                        Toast.makeText(MainActivity.this, "Giriş Başarılı!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, MainMenu.class);
                        intent.putExtra("USER_EMAIL", user);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Hatalı E-posta veya Şifre!", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        // Linke tıklama olayı
        btnGoRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}