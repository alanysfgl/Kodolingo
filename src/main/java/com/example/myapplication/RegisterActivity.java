package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView; // TextView kütüphanesi eklendi
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etName;
    Button btnRegister;
    TextView btnGoLogin; // DÜZELTME: Button yerine TextView yaptık
    DatabaseHelper DB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        etName = findViewById(R.id.etRegisterName);
        btnRegister = findViewById(R.id.btnRegister);

        // XML'de ID'si "tvGoToLogin" ve türü TextView olduğu için düzelttik
        btnGoLogin = findViewById(R.id.tvGoToLogin);

        DB = new DatabaseHelper(this);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = etEmail.getText().toString().trim();
                String pass = etPassword.getText().toString();
                String name = etName.getText().toString();

                if(user.equals("") || pass.equals("") || name.equals("")) {
                    Toast.makeText(RegisterActivity.this, "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show();
                }
                else if (!user.endsWith("@gmail.com")) {
                    Toast.makeText(RegisterActivity.this, "HATA: Sadece @gmail.com adresleri kabul edilir!", Toast.LENGTH_LONG).show();
                }
                else if (pass.length() < 4) {
                    Toast.makeText(RegisterActivity.this, "HATA: Şifre en az 4 karakter olmalı!", Toast.LENGTH_SHORT).show();
                }
                else {
                    Boolean checkUser = DB.checkEmail(user);
                    if(!checkUser) {
                        Boolean insert = DB.insertData(user, pass, name);
                        if(insert) {
                            Toast.makeText(RegisterActivity.this, "Kayıt Başarılı! Giriş yapabilirsiniz.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Kayıt Başarısız!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Bu Gmail adresi zaten kayıtlı!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        btnGoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}