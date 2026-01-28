package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "Login.db";


    public DatabaseHelper(Context context) {
        super(context, "Login.db", null, 15);
    }

    @Override
    public void onCreate(SQLiteDatabase MyDB) {
        // 1. Kullanıcılar Tablosu
        MyDB.execSQL("create Table users(email TEXT primary key, password TEXT, name TEXT)");

        // 2. Sorular Tablosu (Level ve Kategori destekli)
        MyDB.execSQL("create Table questions(id INTEGER PRIMARY KEY AUTOINCREMENT, question TEXT, opt1 TEXT, opt2 TEXT, opt3 TEXT, opt4 TEXT, answer TEXT, language TEXT, level INTEGER)");

        // 3. İlerleme Tablosu (Hangi kullanıcı, hangi dilde kaç puanda)
        // Composite Primary Key: Bir kullanıcı bir dilde sadece bir satıra sahip olabilir.
        MyDB.execSQL("create Table user_progress(email TEXT, language TEXT, total_score INTEGER, current_level INTEGER, PRIMARY KEY(email, language))");
        // 4. EĞİTİM TABLOSU
        // chapter_order: Dersin sırası (1, 2, 3...)
        MyDB.execSQL("create Table tutorials(id INTEGER PRIMARY KEY AUTOINCREMENT, language TEXT, title TEXT, content TEXT, chapter_order INTEGER)");

        // Başlangıç sorularını yükler
        addInitialQuestions(MyDB);

        addInitialTutorials(MyDB); // Dersleri ekler
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int oldVersion, int newVersion) {
        MyDB.execSQL("drop Table if exists users");
        MyDB.execSQL("drop Table if exists questions");
        MyDB.execSQL("drop Table if exists user_progress");
        MyDB.execSQL("drop Table if exists tutorials");
        onCreate(MyDB);
    }

    // ==========================================
    //       KULLANICI GİRİŞ / KAYIT İŞLEMLERİ
    // ==========================================

    public Boolean insertData(String email, String password, String name) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("email", email);
        contentValues.put("password", password);
        contentValues.put("name", name);
        long result = MyDB.insert("users", null, contentValues);
        return result != -1;
    }

    public Boolean checkEmail(String email) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where email = ?", new String[]{email});
        return cursor.getCount() > 0;
    }

    public Boolean checkEmailPassword(String email, String password) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where email = ? and password = ?", new String[]{email, password});
        return cursor.getCount() > 0;
    }

    public String getName(String email) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select name from users where email = ?", new String[]{email});
        if(cursor.moveToFirst()){
            return cursor.getString(0);
        }
        return "Kullanıcı";
    }

    // ==========================================
    //       PROFİL VE AYARLAR YÖNETİMİ
    // ==========================================


    // 2. Şifre Güncelleme
    public boolean updatePassword(String email, String newPassword) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("password", newPassword);
        long result = db.update("users", cv, "email = ?", new String[]{email});
        return result != -1;
    }

    // 3. Toplam Puanı Getirme (Header İçin)
    public int getTotalUserScore(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total_score) FROM user_progress WHERE email = ?", new String[]{email});
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 0;
    }

    // 4. Detaylı Ders Karnesi Getirme (Açılır Liste İçin - YENİ)
    public Cursor getUserProgressDetails(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Dil, Puan ve Seviye bilgisini çekiyoruz
        return db.rawQuery("SELECT language, total_score, current_level FROM user_progress WHERE email = ?", new String[]{email});

    }

    // 5. İlerlemeyi Sıfırla
    public void resetUserProgress(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("user_progress", "email = ?", new String[]{email});
    }

    // 6. Hesabı Sil
    public void deleteUserAccount(String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("users", "email = ?", new String[]{email});
        db.delete("user_progress", "email = ?", new String[]{email});
    }

    // ==========================================
    //       OYUNLAŞTIRMA (GAMIFICATION)
    // ==========================================

    public void addScore(String email, String language, int points) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Önce kullanıcının bu dilde kaydı var mı bak
        Cursor cursor = db.rawQuery("Select * from user_progress where email = ? and language = ?", new String[]{email, language});

        if (cursor.getCount() > 0) {
            // Varsa puanı güncelle
            db.execSQL("UPDATE user_progress SET total_score = total_score + ? WHERE email = ? AND language = ?", new Object[]{points, email, language});
        } else {
            // Yoksa yeni kayıt oluştur (Level 1'den başlar)
            ContentValues cv = new ContentValues();
            cv.put("email", email);
            cv.put("language", language);
            cv.put("total_score", points);
            cv.put("current_level", 1);
            db.insert("user_progress", null, cv);
        }
    }

    public int getUserLevel(String email, String language) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("Select current_level from user_progress where email = ? and language = ?", new String[]{email, language});
        if (cursor.moveToFirst()) {
            return cursor.getInt(0);
        }
        return 1; // Kayıt yoksa varsayılan 1. seviye
    }


    // Seviye Atlatma Kontrolü

    public boolean checkLevelUp(String email, String language) {
        SQLiteDatabase db = this.getWritableDatabase(); // Yazılabilir modda aç

        // 1. Kullanıcının şu anki seviyesini öğren
        int currentLevel = 1;
        Cursor cursor = db.rawQuery("SELECT current_level FROM user_progress WHERE email = ? AND language = ?", new String[]{email, language});
        if (cursor.moveToFirst()) {
            currentLevel = cursor.getInt(0);
        }

        // 2. Seviyeyi 1 Artır
        int nextLevel = currentLevel + 1;

        // 3. Veritabanını GÜNCELLE (UPDATE)
        ContentValues contentValues = new ContentValues();
        contentValues.put("current_level", nextLevel);

        long result = db.update("user_progress", contentValues, "email = ? AND language = ?", new String[]{email, language});

        // Eğer güncelleme başarılıysa (result != -1) true döndür
        return result != -1;
    }
    // ADMIN İÇİN: Tüm kullanıcıların listesini getir
    // ADMIN İÇİN: Tüm kullanıcıların verilerini (Şifre dahil) getir
    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM users", null);
    }
    public boolean deleteUser(String email) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 1. Önce kullanıcının ilerlemesini (Puanlarını) sil
        // Bunu yapmazsak veritabanında sahipsiz puanlar kalır.
        db.delete("user_progress", "email = ?", new String[]{email});

        // 2. Sonra kullanıcının kendisini (Hesabını) sil
        long result = db.delete("users", "email = ?", new String[]{email});

        // Eğer silinen satır sayısı 0'dan büyükse işlem başarılıdır
        return result > 0;
    }


    // ==========================================
    //       SORU YÖNETİMİ
    // ==========================================

    // Seviyeye ve Dile Göre Soru Çekme
    public ArrayList<QuestionModel> getQuestionsByLevel(String language, int level) {
        ArrayList<QuestionModel> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM questions WHERE language = ? AND level = ?", new String[]{language, String.valueOf(level)});

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String q = cursor.getString(cursor.getColumnIndex("question"));
                @SuppressLint("Range") String o1 = cursor.getString(cursor.getColumnIndex("opt1"));
                @SuppressLint("Range") String o2 = cursor.getString(cursor.getColumnIndex("opt2"));
                @SuppressLint("Range") String o3 = cursor.getString(cursor.getColumnIndex("opt3"));
                @SuppressLint("Range") String o4 = cursor.getString(cursor.getColumnIndex("opt4"));
                @SuppressLint("Range") String ans = cursor.getString(cursor.getColumnIndex("answer"));

                list.add(new QuestionModel(q, o1, o2, o3, o4, ans));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    private void addQuestion(SQLiteDatabase db, String q, String o1, String o2, String o3, String o4, String ans, String lang, int level) {
        ContentValues cv = new ContentValues();
        cv.put("question", q);
        cv.put("opt1", o1); cv.put("opt2", o2); cv.put("opt3", o3); cv.put("opt4", o4);
        cv.put("answer", ans);
        cv.put("language", lang);
        cv.put("level", level);
        db.insert("questions", null, cv);
    }


    private void addInitialTutorials(SQLiteDatabase db) {
        addPythonTutorials(db);
        addHardwareTutorials(db);
        addJavaTutorials(db);
        addCPPTutorials(db);
        addCsharpTutorials(db);
        addCTutorials(db);
        addJSTutorials(db);


    }

    // Belirli bir dilin TÜM konu başlıklarını getir (Müfredat Listesi İçin)
    public Cursor getTutorialTopics(String language) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT title, chapter_order FROM tutorials WHERE language = ? ORDER BY chapter_order ASC", new String[]{language});
    }

    // Belirli bir sıradaki dersin içeriğini getir (Okuma Ekranı İçin)
    public Cursor getTutorialByOrder(String language, int order) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT title, content FROM tutorials WHERE language = ? AND chapter_order = ?", new String[]{language, String.valueOf(order)});
    }

    // O dilde kaç ders var? (Son ders mi kontrolü için)
    public int getTutorialCount(String language) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT count(*) FROM tutorials WHERE language = ?", new String[]{language});
        if(cursor.moveToFirst()){
            return cursor.getInt(0);
        }
        return 0;
    }

    // Ders Ekleme Metodu
    private void addTutorial(SQLiteDatabase db, String lang, String title, String content, int order) {
        ContentValues cv = new ContentValues();
        cv.put("language", lang);
        cv.put("title", title);
        cv.put("content", content);
        cv.put("chapter_order", order);
        db.insert("tutorials", null, cv);
    }

    // ==========================================
    //       BAŞLANGIÇ SORULARI (SEED DATA)
    // ==========================================
    private void addInitialQuestions(SQLiteDatabase db) {

        // LEVEL 1 SORULARI
        //JAVA
        addQuestion(db, "Java'da ekrana çıktı vermek için hangisi kullanılır?", "console.log()", "print()", "System.out.println()", "echo", "System.out.println()", "Java",1);
        addQuestion(db, "Java'da bir sınıfın başka bir sınıftan miras almasını sağlayan anahtar kelime nedir?", "implements", "extends", "imports", "inherits", "extends", "Java",1);
        addQuestion(db, "Java'da 'int' veri tipi bellekte ne kadar yer kaplar?", "8 bit", "16 bit", "32 bit", "64 bit", "32 bit", "Java",1);
        addQuestion(db, "Java programlarının başlangıç noktası hangi metottur?", "start()", "init()", "main()", "run()", "main()", "Java",1);
        addQuestion(db, "Java'da değişmez (sabit) değişken tanımlamak için hangi kelime kullanılır?", "const", "final", "static", "var", "final", "Java",1);
        addQuestion(db, "Hangisi Java'da bir erişim belirleyici (access modifier) değildir?", "public", "private", "protected", "global", "global", "Java",1);
        addQuestion(db, "Java kodları derlendiğinde hangi dosya uzantısı oluşur?", ".java", ".exe", ".class", ".jar", ".class", "Java",1);
        addQuestion(db, "Java'da mantıksal 've' operatörü hangisidir?", "&", "&&", "||", "AND", "&&", "Java",1);
        addQuestion(db, "Bir arayüzü (interface) uygulamak için hangi anahtar kelime kullanılır?", "extends", "implements", "using", "interface", "implements", "Java",1);
        addQuestion(db, "Java'da String ifadeleri birleştirmek için hangi operatör kullanılır?", ".", "&", "+", ",", "+", "Java",1);
        // PYTHON
        addQuestion(db, "Python'da bir fonksiyon tanımlamak için hangi kelime kullanılır?", "func", "def", "function", "define", "def", "Python",1);
        addQuestion(db, "Python'da liste oluşturmak için hangi parantez kullanılır?", "()", "[]", "{}", "<>", "[]", "Python",1);
        addQuestion(db, "Python'da bir değişkenin tipini öğrenmek için hangi fonksiyon kullanılır?", "type()", "typeof()", "getClass()", "id()", "type()", "Python",1);
        addQuestion(db, "Python'da üs alma işlemi (kuvvet) hangi operatörle yapılır?", "^", "**", "//", "pow", "**", "Python",1);
        addQuestion(db, "Python'da kod bloklarını belirtmek için ne kullanılır?", "Süslü parantez {}", "Noktalı virgül ;", "Girinti (Indentation)", "Begin-End", "Girinti (Indentation)", "Python",1);
        addQuestion(db, "Python'da bir kütüphaneyi dahil etmek için hangi komut kullanılır?", "include", "using", "import", "require", "import", "Python",1);
        addQuestion(db, "Python'da bir dizinin uzunluğunu almak için hangisi kullanılır?", "length()", "size()", "count()", "len()", "len()", "Python",1);
        addQuestion(db, "Python'da kullanıcıdan veri almak için hangisi kullanılır?", "cin", "scanf", "input()", "get()", "input()", "Python",1);
        addQuestion(db, "Python'da 'True' ve 'False' hangi veri tipine aittir?", "int", "str", "bool", "float", "bool", "Python",1);
        addQuestion(db, "Python'da hangisi değiştirilemez (immutable) bir veri yapısıdır?", "List", "Dictionary", "Set", "Tuple", "Tuple", "Python",1);

        // C++
        addQuestion(db, "C++'da ekrana çıktı vermek için hangisi kullanılır?", "printf", "cout", "print", "System.out", "cout", "C++",1);
        addQuestion(db, "C++'da kullanıcıdan veri almak için hangisi kullanılır?", "cin", "input", "scanf", "read", "cin", "C++",1);
        addQuestion(db, "C++'da bir işaretçinin (pointer) adresini almak için hangi operatör kullanılır?", "*", "&", "->", "::", "&", "C++",1);
        addQuestion(db, "C++'da standart kütüphaneyi kullanmak için hangisi yazılır?", "using namespace std;", "import std;", "include std;", "define std;", "using namespace std;", "C++",1);
        addQuestion(db, "C++'da bellekten dinamik yer ayırmak için hangi komut kullanılır?", "alloc", "malloc", "new", "create", "new", "C++",1);
        addQuestion(db, "C++'da sınıfın (class) üyeleri varsayılan olarak hangi erişim türündedir?", "public", "private", "protected", "static", "private", "C++",1);
        addQuestion(db, "C++'da bir sınıfın yıkıcı (destructor) metodu hangi sembolle başlar?", "#", "~", "!", "-", "~", "C++",1);
        addQuestion(db, "C++'da satır sonu (yeni satır) için hangisi kullanılır?", "endl", "newline", "break", "next", "endl", "C++",1);
        addQuestion(db, "C++'da başlık dosyaları (header files) genellikle hangi uzantıya sahiptir?", ".cpp", ".txt", ".h", ".c", ".h", "C++",1);
        addQuestion(db, "Hangisi C++'da mantıksal 'veya' operatörüdür?", "&&", "||", "OR", "!", "||", "C++",1);


        // C#
        addQuestion(db, "C#'ta ekrana yazı yazdırmak için hangisi kullanılır?", "Console.WriteLine()", "print()", "cout", "System.out.println()", "Console.WriteLine()", "C#",1);
        addQuestion(db, "C#'ta tüm sınıfların türetildiği temel sınıf hangisidir?", "Main", "Master", "Object", "Base", "Object", "C#",1);
        addQuestion(db, "C#'ta değişken tipini derleyicinin belirlemesi için hangisi kullanılır?", "auto", "dynamic", "var", "let", "var", "C#",1);
        addQuestion(db, "C#'ta bir sınıfın miras alması için hangi sembol kullanılır?", ":", "extends", "->", "implements", ":", "C#",1);
        addQuestion(db, "C#'ta metin interpolasyonu (text interpolation) için string başına ne konur?", "@", "$", "#", "%", "$", "C#",1);
        addQuestion(db, "C#'ta LINQ sorgularında 'seçmek' için hangi kelime kullanılır?", "choose", "pick", "select", "where", "select", "C#",1);
        addQuestion(db, "C# kodu hangi platform üzerinde çalışır?", "JDK", ".NET Framework", "JVM", "V8 Engine", ".NET Framework", "C#",1);
        addQuestion(db, "C#'ta bir özelliği (property) tanımlamak için kullanılan bloklar nelerdir?", "get/set", "read/write", "in/out", "input/output", "get/set", "C#",1);
        addQuestion(db, "C#'ta hangisi bir erişim belirleyici değildir?", "internal", "public", "private", "friend", "friend", "C#",1);
        addQuestion(db, "C#'ta tam sayıları ondalıklı sayıya dönüştürme işlemine ne denir?", "Boxing", "Casting", "Parsing", "Splitting", "Casting", "C#",1);
        // C
        addQuestion(db, "C dilinde ekrana yazdırmak için hangi fonksiyon kullanılır?", "print()", "printf()", "cout", "write()", "printf()", "C",1);
        addQuestion(db, "C dilinde kullanıcıdan değer okumak için hangi fonksiyon kullanılır?", "input()", "cin", "scanf()", "read()", "scanf()", "C",1);
        addQuestion(db, "C dilinde bir değişkenin bellekteki adresini tutan yapı nedir?", "Array", "Pointer", "Struct", "String", "Pointer", "C",1);
        addQuestion(db, "C dilinde yapı (structure) tanımlamak için hangi kelime kullanılır?", "class", "object", "struct", "type", "struct", "C",1);
        addQuestion(db, "C dilinde dinamik bellek ayırmak için hangi fonksiyon kullanılır?", "new", "malloc()", "alloc()", "create()", "malloc()", "C",1);
        addQuestion(db, "C dilinde string ifadelerin sonuna otomatik olarak hangi karakter eklenir?", "\\n", "\\0", "\\t", "\\end", "\\0", "C",1);
        addQuestion(db, "C dilinde kütüphane eklemek için hangi önişlemci komutu kullanılır?", "#include", "#import", "#using", "#define", "#include", "C",1);
        addQuestion(db, "C dilinde sabit (macro) tanımlamak için ne kullanılır?", "const", "#define", "final", "static", "#define", "C",1);
        addQuestion(db, "C programlama dosyalarının uzantısı nedir?", ".cpp", ".java", ".c", ".txt", ".c", "C",1);
        addQuestion(db, "C dilinde 've' mantıksal operatörü hangisidir?", "&&", "||", "&", "AND", "&&", "C",1);

        // JAVASCRIPT
        addQuestion(db, "JavaScript'te tarayıcı konsoluna yazı yazmak için ne kullanılır?", "print()", "console.log()", "System.out.println", "echo", "console.log()", "JavaScript",1);
        addQuestion(db, "JavaScript'te değişken tanımlamak için kullanılan modern anahtar kelimeler nelerdir?", "var/int", "let/const", "dim/set", "string/number", "let/const", "JavaScript",1);
        addQuestion(db, "JavaScript kodu HTML içinde hangi etiketler arasına yazılır?", "<js>", "<javascript>", "<script>", "<code>", "<script>", "JavaScript",1);
        addQuestion(db, "JavaScript'te bir HTML elemanını ID ile seçmek için hangisi kullanılır?", "getElementByClass", "querySelector", "getElementById", "selectID", "getElementById", "JavaScript",1);
        addQuestion(db, "JavaScript'te 'Not a Number' ifadesinin kısaltması nedir?", "Null", "Undefined", "NaN", "Error", "NaN", "JavaScript",1);
        addQuestion(db, "JavaScript'te hem değeri hem de türü kontrol eden eşitlik operatörü hangisidir?", "==", "=", "===", "!=", "===", "JavaScript",1);
        addQuestion(db, "JavaScript'te bir uyarı kutusu (popup) çıkarmak için ne kullanılır?", "msgBox()", "alert()", "popup()", "warn()", "alert()", "JavaScript",1);
        addQuestion(db, "JavaScript nesneleri (objects) hangi formatta veri tutar?", "Key-Value", "Index-Value", "XML", "Table", "Key-Value", "JavaScript",1);
        addQuestion(db, "JavaScript'te JSON'un açılımı nedir?", "Java Standard Object Notation", "JavaScript Object Notation", "JavaScript Online Node", "Java Syntax On Net", "JavaScript Object Notation", "JavaScript",1);
        addQuestion(db, "JavaScript'te bir olayı dinlemek için hangi metod kullanılır?", "onEvent", "addEventListener", "listen", "handleEvent", "addEventListener", "JavaScript",1);

        // --- DONANIM (HARDWARE) LEVEL 1 ---
        addQuestion(db, "Bilgisayarın beyni olarak bilinen parça hangisidir?", "RAM", "CPU (İşlemci)", "HDD", "Kasa", "CPU (İşlemci)", "Donanım", 1);
        addQuestion(db, "Elektrik kesilince içindeki bilgilerin silindiği bellek hangisidir?", "ROM", "Harddisk", "Flash Bellek", "RAM", "RAM", "Donanım", 1);
        addQuestion(db, "Görüntüyü işleyip monitöre aktaran parça hangisidir?", "Ses Kartı", "Ethernet Kartı", "GPU (Ekran Kartı)", "CPU", "GPU (Ekran Kartı)", "Donanım", 1);
        addQuestion(db, "Hangisi bir 'Giriş Birimi' (Input Device) dir?", "Hoparlör", "Yazıcı", "Klavye", "Monitör", "Klavye", "Donanım", 1);
        addQuestion(db, "Bilgisayarın 0 ve 1'lerden oluşan diline ne ad verilir?", "Decimal", "Binary (İkili)", "Hexadecimal", "Octal", "Binary (İkili)", "Donanım", 1);
        addQuestion(db, "1 Byte kaç Bit'ten oluşur?", "4", "8", "16", "32", "8", "Donanım", 1);
        addQuestion(db, "Bilgisayar kasasındaki tüm parçaların takıldığı ana levha nedir?", "Anakart (Motherboard)", "Ekran Kartı", "Güç Kaynağı", "İşlemci", "Anakart (Motherboard)", "Donanım", 1);
        addQuestion(db, "Bilgisayara güç (elektrik) sağlayan birim hangisidir?", "UPS", "CPU", "PSU (Power Supply)", "Pil", "PSU (Power Supply)", "Donanım", 1);
        addQuestion(db, "Hangisi bir kalıcı depolama birimidir?", "RAM", "Cache", "Register", "SSD", "SSD", "Donanım", 1);
        addQuestion(db, "Klavye ve Fare genellikle hangi porttan bağlanır?", "VGA", "HDMI", "USB", "Ethernet", "USB", "Donanım", 1);

        // LEVEL 2 SORULARI

// C++ LEVEL 2 (Devamı)

// --- JAVA LEVEL 2 (Orta Seviye - OOP ,Bellek ve Performans (Memory & Performance),Veri Yapıları ve Hata Yönetimi Kavramları) ---
        addQuestion(db, "Java'da nesnesi oluşturulamayan sınıf türü hangisidir?", "Static Class", "Abstract Class", "Final Class", "Public Class", "Abstract Class", "Java", 2);
        addQuestion(db, "Bir sınıfın başka bir sınıftaki metodları kullanabilmesi için hangisi gereklidir?", "Polymorphism", "Encapsulation", "Inheritance", "Abstraction", "Inheritance", "Java", 2);
        addQuestion(db, "Java'da 'super' anahtar kelimesi ne işe yarar?", "Üst sınıfı çağırır", "Nesneyi siler", "Döngüyü kırar", "Hata fırlatır", "Üst sınıfı çağırır", "Java", 2);
        addQuestion(db, "Hangisi Java'da bir hata yakalama bloğudur?", "try-catch", "if-else", "for-loop", "switch-case", "try-catch", "Java", 2);
        addQuestion(db, "ArrayList hangi paketin içindedir?", "java.io", "java.util", "java.sql", "java.net", "java.util", "Java", 2);
        addQuestion(db, "Java'da çoklu kalıtım (multiple inheritance) nasıl yapılır?", "Extends ile", "Doğrudan yapılamaz (Interface gerekir)", "Implements ile", "Super ile", "Doğrudan yapılamaz (Interface gerekir)", "Java", 2);
        addQuestion(db, "Hangisi Thread (iş parçacığı) başlatmak için kullanılır?", "run()", "start()", "init()", "go()", "start()", "Java", 2);
        addQuestion(db, "Java'da garbage collector ne işe yarar?", "Dosya siler", "Gereksiz bellek alanını temizler", "Virüs tarar", "Veritabanını temizler", "Gereksiz bellek alanını temizler", "Java", 2);
        addQuestion(db, "Final tanımlanan bir değişkenin değeri...", "Değiştirilebilir", "Değiştirilemez", "Null olabilir", "Sıfırlanır", "Değiştirilemez", "Java", 2);
        addQuestion(db, "Java'da bir değişkeni 'static' yaparsak ne olur?", "Sınıfa ait olur, nesneye değil", "Silinemez", "Gizli olur", "Sadece okunabilir", "Sınıfa ait olur, nesneye değil", "Java", 2);

// --- PYTHON LEVEL 2 (Orta Seviye - Listeler ve Fonksiyonlar,Sınıflar ve Modüller (Classes & Modules)) ---
        addQuestion(db, "Python'da bir listenin sonuna eleman eklemek için hangisi kullanılır?", "add()", "insert()", "append()", "push()", "append()", "Python", 2);
        addQuestion(db, "Python'da 'dictionary' yapısında veriler nasıl tutulur?", "Key-Value", "Index-Value", "LIFO", "FIFO", "Key-Value", "Python", 2);
        addQuestion(db, "Python'da 'lambda' nedir?", "Döngü çeşidi", "Hata yakalama", "İsimsiz (anonim) fonksiyon", "Modül", "İsimsiz (anonim) fonksiyon", "Python", 2);
        addQuestion(db, "Bir string ifadeyi küçük harfe çevirmek için hangisi kullanılır?", "toLower()", "lower()", "min()", "small()", "lower()", "Python", 2);
        addQuestion(db, "Python'da modül yüklemek için dosyanın başına ne yazılır?", "include", "using", "import", "require", "import", "Python", 2);
        addQuestion(db, "range(5) fonksiyonu hangi sayıları üretir?", "1,2,3,4,5", "0,1,2,3,4", "5,5,5,5,5", "1,2,3,4", "0,1,2,3,4", "Python", 2);
        addQuestion(db, "Python'da hangisi bir hata (Exception) türü değildir?", "ValueError", "SyntaxError", "LoopError", "TypeError", "LoopError", "Python", 2);
        addQuestion(db, "Python'da __init__ metodunun görevi nedir?", "Programı bitirir", "Sınıfın yapıcı (constructor) metodudur", "Değişken tanımlar", "Dosya açar", "Sınıfın yapıcı (constructor) metodudur", "Python", 2);
        addQuestion(db, "List comprehension örneği hangisidir?", "[x for x in list]", "for x in list:", "list.map()", "while list:", "[x for x in list]", "Python", 2);
        addQuestion(db, "Global değişkeni fonksiyon içinde değiştirmek için hangi kelime kullanılır?", "static", "global", "extern", "public", "global", "Python", 2);

// --- C++ LEVEL 2 (Pointerlar ve Bellek,Nesne Yönelimli Programlama (OOP)) ---
        addQuestion(db, "C++'da 'pointer' neyi tutar?", "Değeri", "Bellek adresini", "Dosya yolunu", "Döngü sayısını", "Bellek adresini", "C++", 2);
        addQuestion(db, "Bir pointer'ın gösterdiği değeri okumak için başına ne konur?", "&", "*", "->", "::", "*", "C++", 2);
        addQuestion(db, "C++'da sınıf içinde 'private' olan bir değişkene dışarıdan nasıl erişilir?", "Erişilemez", "Getter/Setter metodlarıyla", "Pointer ile", "New komutuyla", "Getter/Setter metodlarıyla", "C++", 2);
        addQuestion(db, "C++'da aynı isme sahip ama farklı parametre alan fonksiyonlara ne denir?", "Overriding", "Overloading", "Virtual", "Abstract", "Overloading", "C++", 2);
        addQuestion(db, "Sanal (Virtual) fonksiyonlar hangi kavramın temelidir?", "Kapsülleme", "Polimorfizm (Çok biçimlilik)", "Miras", "Statiklik", "Polimorfizm (Çok biçimlilik)", "C++", 2);
        addQuestion(db, "C++'da bir yapıcı (constructor) ne zaman çalışır?", "Nesne silinirken", "Program biterken", "Nesne oluşturulurken", "Fonksiyon çağrılınca", "Nesne oluşturulurken", "C++", 2);
        addQuestion(db, "Bellekte ayrılan dinamik alanı (new) temizlemek için hangisi kullanılır?", "clean", "delete", "free", "remove", "delete", "C++", 2);
        addQuestion(db, "Hangisi C++'da bir döngüden aniden çıkmak için kullanılır?", "continue", "return", "break", "exit", "break", "C++", 2);
        addQuestion(db, "C++'da 'referans' atamak için hangi sembol kullanılır?", "*", "&", "->", "#", "&", "C++", 2);
        addQuestion(db, "Bir sınıfın üyelerine varsayılan (default) erişim türü nedir?", "Public", "Private", "Protected", "Static", "Private", "C++", 2);

// C# LEVEL 2(Modern C# Yapıları ve .NET)
        addQuestion(db, "C#'ta bir sınıfın miras almasını engellemek için hangi kelime kullanılır?", "static", "sealed", "final", "locked", "sealed", "C#", 2);
        addQuestion(db, "Hangi yapı C#'ta 'Value Type' (Değer Tipi) olarak saklanır?", "Class", "Interface", "Struct", "Delegate", "Struct", "C#", 2);
        addQuestion(db, "C#'ta hata fırlatmak için hangi kelime kullanılır?", "catch", "try", "throw", "error", "throw", "C#", 2);
        addQuestion(db, "Bir değişkenin null olabileceğini belirtmek için yanına ne konur?", "!", "?", "*", "&", "?", "C#", 2);
        addQuestion(db, "C#'ta özellikleri (Properties) tanımlarken hangi bloklar kullanılır?", "get/set", "read/write", "in/out", "push/pop", "get/set", "C#", 2);
        addQuestion(db, "Kullanılmayan kaynakları bellekten temizleyen otomatik yapı nedir?", "Destructor", "Garbage Collector", "Cleaner", "Eraser", "Garbage Collector", "C#", 2);
        addQuestion(db, "C#'ta bir arayüzü (Interface) uygulamak için hangi sembol kullanılır?", "implements", "extends", ":", "->", ":", "C#", 2);
        addQuestion(db, "Metinleri daha performanslı birleştirmek için hangi sınıf kullanılır?", "String", "StringBuilder", "TextMerger", "Concat", "StringBuilder", "C#", 2);
        addQuestion(db, "Foreach döngüsü hangi arayüzü uygulayan koleksiyonlarda çalışır?", "IEnumerable", "IList", "ICollection", "IDisposable", "IEnumerable", "C#", 2);
        addQuestion(db, "Asenkron metotların geri dönüş tipi genellikle nedir?", "Void", "Task", "Future", "Thread", "Task", "C#", 2);



        // C LEVEL 2(Pointer Aritmetiği ve Sistem Programlama)
        addQuestion(db, "C dilinde bir tamsayıyı (int) yazdırmak için hangi format belirteci kullanılır?", "%s", "%c", "%d", "%f", "%d", "C", 2);
        addQuestion(db, "C dilinde 'sizeof' operatörü ne işe yarar?", "Değeri verir", "Adresi verir", "Bellekteki boyutunu verir", "Diziyi siler", "Bellekteki boyutunu verir", "C", 2);
        addQuestion(db, "Bir pointer'ın tuttuğu adresteki değere erişmeye ne denir?", "Dereferencing", "Allocating", "Indexing", "Looping", "Dereferencing", "C", 2);
        addQuestion(db, "C'de dosya işlemleri için hangi veri tipi kullanılır?", "File", "FILE*", "Document", "IO", "FILE*", "C", 2);
        addQuestion(db, "Hangisi sonsuz döngü oluşturur?", "for(;;)", "while(false)", "do-while(0)", "loop()", "for(;;)", "C", 2);
        addQuestion(db, "C dilinde bir makro tanımlamak için hangisi kullanılır?", "const", "#define", "macro", "static", "#define", "C", 2);
        addQuestion(db, "Switch-case yapısında 'default' ne zaman çalışır?", "Her zaman", "Hiçbir case eşleşmezse", "İlk case eşleşirse", "Döngü bitince", "Hiçbir case eşleşmezse", "C", 2);
        addQuestion(db, "C dilinde 'void' dönüş tipi ne anlama gelir?", "Sıfır döndürür", "Hata döndürür", "Değer döndürmez", "String döndürür", "Değer döndürmez", "C", 2);
        addQuestion(db, "Bir dizinin adı (örneğin 'arr') aslında nedir?", "İlk elemanın değeri", "Dizinin boyutu", "İlk elemanın adresi (Pointer)", "Son eleman", "İlk elemanın adresi (Pointer)", "C", 2);
        addQuestion(db, "Yerel (local) bir değişkenin değerini fonksiyon bitse bile koruması için ne kullanılır?", "const", "static", "volatile", "extern", "static", "C", 2);


// JAVASCRIPT LEVEL 2 (ES6 ve Asenkron Yapı)
        addQuestion(db, "JavaScript'te bir fonksiyonu başka bir fonksiyona parametre olarak geçmeye ne denir?", "Callback", "Loop", "Recursion", "Promise", "Callback", "JavaScript", 2);
        addQuestion(db, "ES6 ile gelen, 'var' yerine kullanılan blok kapsamlı değişken tanımlama kelimesi nedir?", "int", "dim", "let", "global", "let", "JavaScript", 2);
        addQuestion(db, "JavaScript nesnesini String formatına çeviren metod hangisidir?", "JSON.parse()", "JSON.stringify()", "toString()", "convert()", "JSON.stringify()", "JavaScript", 2);
        addQuestion(db, "DOM üzerinde CSS seçicisi (örneğin '.class') ile eleman seçmek için ne kullanılır?", "getElementById", "querySelector", "getTagName", "select()", "querySelector", "JavaScript", 2);
        addQuestion(db, "Bir dizinin sonundaki elemanı silip döndüren metod hangisidir?", "push()", "shift()", "pop()", "slice()", "pop()", "JavaScript", 2);
        addQuestion(db, "JavaScript'te '===' operatörünün '==' operatöründen farkı nedir?", "Sadece değeri kontrol eder", "Hem değeri hem türü kontrol eder", "Atama yapar", "Farklı değildir", "Hem değeri hem türü kontrol eder", "JavaScript", 2);
        addQuestion(db, "Hangisi ES6 ile gelen 'Arrow Function' sözdizimidir?", "func => {}", "() => {}", "-> {}", "function() {}", "() => {}", "JavaScript", 2);
        addQuestion(db, "JavaScript senkron mu yoksa asenkron mu çalışabilir?", "Sadece Senkron", "Sadece Asenkron", "Asenkron yapıyı destekler (Event Loop)", "Hiçbiri", "Asenkron yapıyı destekler (Event Loop)", "JavaScript", 2);
        addQuestion(db, "Bir metin içinde değişken kullanmak için (Template Literals) hangi tırnak işareti kullanılır?", "Tek tırnak ' '", "Çift tırnak \" \"", "Backtick ` `", "Parantez ( )", "Backtick ` `", "JavaScript", 2);
        addQuestion(db, "JavaScript'te 'this' anahtar kelimesi neyi ifade eder?", "Her zaman global nesneyi", "Fonksiyonun kendisini", "Çağırıldığı bağlamdaki (context) nesneyi", "HTML dosyasını", "Çağırıldığı bağlamdaki (context) nesneyi", "JavaScript", 2);


// --- DONANIM (HARDWARE) LEVEL 2 ---
        addQuestion(db, "CPU içinde aritmetik ve mantıksal işlemleri yapan birim hangisidir?", "CU (Control Unit)", "ALU", "Register", "Cache", "ALU", "Donanım", 2);
        addQuestion(db, "İşlemci ile RAM arasındaki hız farkını kapatan süper hızlı bellek?", "HDD", "SSD", "Önbellek (Cache)", "ROM", "Önbellek (Cache)", "Donanım", 2);
        addQuestion(db, "Bilgisayarın ilk açılış komutlarını (Boot) barındıran yazılım?", "Windows", "Linux", "BIOS / UEFI", "Office", "BIOS / UEFI", "Donanım", 2);
        addQuestion(db, "Mantık kapılarından hangisi, girişlerin hepsi 1 ise 1 verir?", "OR", "NOT", "NAND", "AND", "AND", "Donanım", 2);
        addQuestion(db, "SSD disklerin HDD disklerden en büyük farkı nedir?", "Daha ucuzdur", "Mekanik parça içermez, daha hızlıdır", "Daha yavaştır", "Daha çok ısınır", "Mekanik parça içermez, daha hızlıdır", "Donanım", 2);
        addQuestion(db, "İşlemci hızını ifade eden 'GHz' ne anlama gelir?", "Gigabyte", "Gigaherz", "Gigabit", "Global Host", "Gigaherz", "Donanım", 2);
        addQuestion(db, "Aşağıdakilerden hangisi bir Çıkış Birimi (Output Device) değildir?", "Yazıcı", "Hoparlör", "Mikrofon", "Projeksiyon", "Mikrofon", "Donanım", 2);
        addQuestion(db, "Veri yolunda (Bus) veri iletim hızı ne ile ölçülür?", "RPM", "DPI", "Bandwidth (Bant Genişliği)", "Pixel", "Bandwidth (Bant Genişliği)", "Donanım", 2);
        addQuestion(db, "Termal macun ne işe yarar?", "Bilgisayarı hızlandırır", "İşlemci ile soğutucu arasındaki ısı iletimini artırır", "Elektrik tasarrufu sağlar", "Tozlanmayı önler", "İşlemci ile soğutucu arasındaki ısı iletimini artırır", "Donanım", 2);
        addQuestion(db, "GPU'nun kendi belleğine ne ad verilir?", "DDR4", "VRAM", "SRAM", "NVRAM", "VRAM", "Donanım", 2);


        //LEVEL 3 SORULARI

        // AŞAĞIDA YENİ EKLEYECEĞİMİZ LEVEL SORULARI OLACAK
        }



    private void addPythonTutorials(SQLiteDatabase db) {
        String lang = "Python";

        // --- BÖLÜM 1: TEMELLER ---
        addTutorial(db, lang, "1. Python Giriş", "Python; web geliştirme, veri bilimi, yapay zeka ve otomasyon için kullanılan yüksek seviyeli bir dildir.\nOkunabilirliği yüksektir ve İngilizce sözdizimine benzer.", 1);
        addTutorial(db, lang, "2. Sözdizimi (Syntax)", "Python, kod bloklarını ayırmak için süslü parantez {} yerine 'Girinti' (Indentation) kullanır.\nif 5 > 2:\n  print('Beş büyüktür iki')", 2);
        addTutorial(db, lang, "3. Yorum Satırları", "Kodun çalışmayan açıklama kısımlarıdır.\n# Tek satırlı yorum\n'''\nÇok satırlı\nyorum bloğu\n'''", 3);
        addTutorial(db, lang, "4. Değişkenler", "Veri saklayan kaplardır. Tip belirtmeye gerek yoktur.\nx = 5\ny = 'Ali'\nDeğişken isimleri harf veya alt çizgi ile başlamalıdır.", 4);
        addTutorial(db, lang, "5. Veri Tipleri", "Temel tipler:\nstr (Metin), int (Tam Sayı), float (Ondalıklı), complex (Karmaşık), list, tuple, range, dict, set, bool.", 5);
        addTutorial(db, lang, "6. Sayılar (Numbers)", "int: Tam sayılar (10, -5)\nfloat: Ondalıklı sayılar (10.5)\ncomplex: Karmaşık sayılar (3+5j)\nTip dönüşümü: int(3.8) -> 3", 6);
        addTutorial(db, lang, "7. Casting", "Veri tipini değiştirmek için kullanılır.\nx = str(3) -> '3'\ny = int(3.9) -> 3\nz = float(3) -> 3.0", 7);
        addTutorial(db, lang, "8. Stringler", "Metin ifadeleridir.\na = 'Merhaba'\nprint(a[1]) -> 'e' (İndeks 0'dan başlar).", 8);
        addTutorial(db, lang, "9. String Slicing (Dilimleme)", "Metnin bir kısmını almak için kullanılır.\nb = 'Merhaba'\nprint(b[2:5]) -> 'rha' (2 dahil, 5 hariç).", 9);
        addTutorial(db, lang, "10. String Metodları", "a.upper() -> BÜYÜK HARF\na.lower() -> küçük harf\na.strip() -> Boşlukları siler\na.replace('H', 'J') -> Harf değiştirir.", 10);
        addTutorial(db, lang, "11. String Formatlama", "Metin içine değişken koymak için f-string kullanılır.\nyas = 25\ntxt = f'Benim adım Ali ve yaşım {yas}'", 11);
        addTutorial(db, lang, "12. Booleans", "True (Doğru) veya False (Yanlış) değerini alır.\nprint(10 > 9) -> True\nif ifadelerinde karar mekanizmasıdır.", 12);
        addTutorial(db, lang, "13. Operatörler", "Aritmetik (+, -, *, /, %, **)\nAtama (=, +=)\nKarşılaştırma (==, !=, >, <)\nMantıksal (and, or, not)", 13);
        addTutorial(db, lang, "14. Listeler (Lists)", "Sıralı, değiştirilebilir, çoklu veri yapısıdır. [] kullanılır.\nmeyveler = ['Elma', 'Muz']\nmeyveler.append('Kivi')", 14);
        addTutorial(db, lang, "15. Liste Metodları", "sort(): Sıralar\nreverse(): Ters çevirir\ncopy(): Kopyalar\ncount(): Sayar\nremove(): Siler", 15);
        addTutorial(db, lang, "16. Demetler (Tuples)", "Sıralı ancak DEĞİŞTİRİLEMEZ listelerdir. () kullanılır.\nx = ('Elma', 'Muz')\nDeğişmemesi gereken veriler (koordinatlar vb.) için kullanılır.", 16);
        addTutorial(db, lang, "17. Kümeler (Sets)", "Sırasız ve indekslenemez koleksiyondur. {} kullanılır.\nx = {'Elma', 'Muz'}\nTekrar eden verileri otomatik siler.", 17);
        addTutorial(db, lang, "18. Sözlükler (Dictionaries)", "Anahtar:Değer (Key:Value) çiftleri tutar.\naraba = {'marka': 'Ford', 'yil': 1964}\nprint(araba['marka'])", 18);
        addTutorial(db, lang, "19. If ... Else", "Koşul yapıları.\nif a > b:\n  print('a büyük')\nelif a == b:\n  print('eşit')\nelse:\n  print('b büyük')", 19);
        addTutorial(db, lang, "20. While Döngüsü", "Koşul doğru olduğu sürece çalışır.\ni = 1\nwhile i < 6:\n  print(i)\n  i += 1", 20);

        // --- BÖLÜM 2: ORTA SEVİYE ---
        addTutorial(db, lang, "21. For Döngüsü", "Bir dizi üzerinde gezinmek için kullanılır.\nfor x in meyveler:\n  print(x)\nrange(6) fonksiyonu 0-5 arası sayı üretir.", 21);
        addTutorial(db, lang, "22. Fonksiyonlar", "Kod tekrarını önleyen bloklardır.\ndef selamla(isim):\n  print('Merhaba ' + isim)\nselamla('Ahmet')", 22);
        addTutorial(db, lang, "23. Lambda", "Tek satırlık anonim fonksiyonlardır.\nx = lambda a, b : a * b\nprint(x(5, 6)) -> 30", 23);
        addTutorial(db, lang, "24. Diziler (Arrays)", "Python'da yerleşik dizi yoktur, Listeler kullanılır. Ancak NumPy kütüphanesi ile gerçek diziler oluşturulabilir.", 24);
        addTutorial(db, lang, "25. Sınıflar ve Nesneler (OOP)", "Python Nesne Yönelimli bir dildir.\nclass Person:\n  def __init__(self, name):\n    self.name = name\np1 = Person('Ali')", 25);
        addTutorial(db, lang, "26. Miras Alma (Inheritance)", "Bir sınıfın özelliklerini başka bir sınıfa aktarmaktır.\nclass Ogrenci(Person):\n  pass", 26);
        addTutorial(db, lang, "27. İteratörler (Iterators)", "Sayılabilir sayıda değer içeren nesnelerdir.\n__iter__() ve __next__() metodlarını kullanır.", 27);
        addTutorial(db, lang, "28. Kapsam (Scope)", "Değişkenin nerede geçerli olduğunu belirtir.\nLocal Scope: Fonksiyon içi.\nGlobal Scope: Ana gövde.", 28);
        addTutorial(db, lang, "29. Modüller", "Kod kütüphaneleridir.\nimport mymodule\nmymodule.selamla('Ali')\nDosya uzantısı .py olmalıdır.", 29);
        addTutorial(db, lang, "30. Tarih (Dates)", "datetime modülü ile kullanılır.\nimport datetime\nx = datetime.datetime.now()\nprint(x.year)", 30);

        // --- BÖLÜM 3: İLERİ SEVİYE ---
        addTutorial(db, lang, "31. Matematik (Math)", "Gelişmiş matematik işlemleri.\nimport math\nx = math.sqrt(64) -> 8.0\ny = math.ceil(1.4) -> 2", 31);
        addTutorial(db, lang, "32. JSON", "Veri değişim formatıdır.\nimport json\nx = '{ \"name\":\"John\" }'\ny = json.loads(x)\nprint(y['name'])", 32);
        addTutorial(db, lang, "33. RegEx (Düzenli İfadeler)", "Metin arama desenleridir.\nimport re\ntxt = 'Hava yağmurlu'\nx = re.search('^Hava.*lu$', txt)", 33);
        addTutorial(db, lang, "34. PIP (Paket Yöneticisi)", "Python paketlerini yüklemek için kullanılır.\npip install numpy\nPaketler PyPI deposundan çekilir.", 34);
        addTutorial(db, lang, "35. Try...Except (Hata Yakalama)", "Hata oluşsa bile programın çökmesini engeller.\ntry:\n  print(x)\nexcept:\n  print('Bir hata oluştu')", 35);
        addTutorial(db, lang, "36. User Input (Girdi)", "Kullanıcıdan veri almak için kullanılır.\nusername = input('Adınızı girin:')\nprint('Merhaba ' + username)", 36);
        addTutorial(db, lang, "37. String Formatlama (Advanced)", "f-string dışında format() metodu da vardır.\ntxt = 'Fiyatı {:.2f} TL'\nprint(txt.format(45))", 37);
        addTutorial(db, lang, "38. Dosya Açma (File Open)", "Dosya işlemleri için open() kullanılır.\nf = open('dosya.txt', 'r')\nModlar: 'r' (oku), 'a' (ekle), 'w' (yaz).", 38);
        addTutorial(db, lang, "39. Dosya Okuma", "f.read() tüm dosyayı okur.\nf.readline() tek satır okur.\nDosyayı kapatmayı unutmayın: f.close()", 39);
        addTutorial(db, lang, "40. Dosya Yazma", "f = open('dosya.txt', 'a')\nf.write('Yeni satır ekle')\n'w' modu kullanırsanız eski içerik silinir!", 40);
        addTutorial(db, lang, "41. Dosya Silme", "os modülü gerekir.\nimport os\nif os.path.exists('dosya.txt'):\n  os.remove('dosya.txt')", 41);
        addTutorial(db, lang, "42. Python MySQL", "Veritabanı bağlantısı için mysql-connector kullanılır.\nVerileri saklamak ve sorgulamak için idealdir.", 42);
        addTutorial(db, lang, "43. Python MongoDB", "NoSQL veritabanı bağlantısıdır.\nVerileri JSON benzeri formatta tutar.", 43);
        addTutorial(db, lang, "44. Requests Modülü", "HTTP istekleri yapmak için kullanılır.\nWeb sitelerinden veri çekmek (Scraping) veya API kullanmak için gereklidir.", 44);
        addTutorial(db, lang, "45. Matplotlib", "Veri görselleştirme kütüphanesidir.\nGrafikler, pastalar ve histogramlar çizmek için kullanılır.", 45);
    }

    private void addHardwareTutorials(SQLiteDatabase db) {
        String lang = "Donanım";

        // --- BÖLÜM 1: TEMEL BİLEŞENLER ---
        addTutorial(db, lang, "1. Bilgisayar Nedir?", "Bilgisayar; veriyi alan (Input), işleyen (Process), saklayan (Storage) ve çıktı (Output) veren elektronik bir cihazdır. Donanım (fiziksel) ve Yazılım (sanal) olarak ikiye ayrılır.", 1);
        addTutorial(db, lang, "2. Çalışma Mantığı (IPO)", "Input-Process-Output döngüsü:\n1. Girdi: Klavye veya fare ile veri girilir.\n2. İşlem: CPU veriyi işler.\n3. Çıktı: Sonuç ekrana yansır.", 2);
        addTutorial(db, lang, "3. Anakart (Motherboard)", "Bilgisayarın sinir sistemidir. CPU, RAM, Ekran Kartı gibi tüm bileşenler buraya takılır ve birbirleriyle iletişim kurar. Form faktörleri: ATX, Micro-ATX, Mini-ITX.", 3);
        addTutorial(db, lang, "4. CPU (İşlemci)", "Bilgisayarın beynidir. Komutları işler. Hızı GHz ile ölçülür. Çekirdek (Core) sayısı aynı anda yapılan iş miktarını belirler. Intel ve AMD ana üreticilerdir.", 4);
        addTutorial(db, lang, "5. RAM (Bellek)", "Rastgele Erişimli Bellek. İşlemcinin çalışma masasıdır. Geçici hafızadır; elektrik kesilince içindeki veriler silinir. DDR4 ve DDR5 güncel teknolojilerdir.", 5);
        addTutorial(db, lang, "6. Depolama: HDD", "Hard Disk Drive. İçinde dönen manyetik diskler bulunur. Kapasitesi yüksek ve ucuzdur ancak yavaştır ve darbelere dayanıksızdır.", 6);
        addTutorial(db, lang, "7. Depolama: SSD", "Solid State Drive. Çip tabanlı depolamadır. HDD'ye göre 10-50 kat daha hızlıdır. Bilgisayarın açılış hızını doğrudan etkiler. SATA ve NVMe (M.2) çeşitleri vardır.", 7);
        addTutorial(db, lang, "8. Ekran Kartı (GPU)", "Görüntü işleme birimidir. CPU'nun yükünü hafifletir. Oyunlar, video kurgu ve 3D modelleme için kritiktir. Kendi işlemcisi ve belleği (VRAM) vardır.", 8);
        addTutorial(db, lang, "9. Güç Kaynağı (PSU)", "Şebeke elektriğini (AC), bilgisayar parçalarının kullanabileceği düşük voltaja (DC) çevirir. Sistemin kalbidir. 80+ sertifikası verimliliği gösterir.", 9);
        addTutorial(db, lang, "10. Kasa ve Soğutma", "Parçaları bir arada tutan kutudur. Hava akışı (Airflow) çok önemlidir. İşlemci ısınmasını engellemek için Hava Soğutma (Fan) veya Sıvı Soğutma kullanılır.", 10);

        // --- BÖLÜM 2: ÇEVRE BİRİMLERİ VE SİSTEM ---
        addTutorial(db, lang, "11. Giriş Birimleri", "Veri girişi sağlayan cihazlar:\n- Klavye (Mekanik/Membran)\n- Fare (Optik/Lazer)\n- Mikrofon\n- Tarayıcı\n- Web Kamerası", 11);
        addTutorial(db, lang, "12. Çıkış Birimleri", "Sonucu gösteren cihazlar:\n- Monitör (IPS, TN, VA paneller)\n- Hoparlör\n- Yazıcı (Lazer/Mürekkep)\n- Projeksiyon", 12);
        addTutorial(db, lang, "13. Portlar ve Bağlantılar", "USB (Veri), HDMI/DisplayPort (Görüntü), Ethernet (İnternet), Jack (Ses). USB Type-C hem veri hem görüntü hem de güç taşıyabilir.", 13);
        addTutorial(db, lang, "14. Binary (İkili Sistem)", "Bilgisayarların ana dilidir. Sadece 0 ve 1'lerden oluşur.\n0: Kapalı (Elektrik yok)\n1: Açık (Elektrik var).", 14);
        addTutorial(db, lang, "15. Bit, Byte ve Veri Boyutları", "1 Bit = 0 veya 1\n1 Byte = 8 Bit (Bir karakter)\n1 KB = 1024 Byte\n1 MB = 1024 KB\n1 GB = 1024 MB\n1 TB = 1024 GB", 15);
        addTutorial(db, lang, "16. BIOS / UEFI", "Anakart üzerindeki yazılımdır. Bilgisayar açılınca donanımları test eder (POST) ve işletim sistemini başlatır.", 16);
        addTutorial(db, lang, "17. İşletim Sistemi (OS)", "Donanım ile kullanıcı arasındaki köprüdür. Kaynakları yönetir.\nÖrn: Windows, macOS, Linux, Android.", 17);
        addTutorial(db, lang, "18. Sürücüler (Drivers)", "Donanımların işletim sistemiyle konuşmasını sağlayan küçük yazılımlardır. Ekran kartı sürücüsü oyun performansı için kritiktir.", 18);
        addTutorial(db, lang, "19. Ağ Donanımları", "Modem: Sinyali dönüştürür.\nRouter: İnterneti dağıtır.\nSwitch: Cihazları kabloyla bağlar.\nWi-Fi Kartı: Kablosuz bağlantı sağlar.", 19);
        addTutorial(db, lang, "20. İnternet Altyapısı", "Fiber Optik: Işık hızında veri taşır.\nADSL/VDSL: Bakır kablo kullanır.\n5G: Yüksek hızlı mobil internet.", 20);

        // --- BÖLÜM 3: İLERİ TEKNOLOJİLER ---
        addTutorial(db, lang, "21. Sunucular (Servers)", "7/24 çalışan güçlü bilgisayarlardır. Web sitelerini barındırır, veritabanlarını yönetir ve oyun sunuculuğu yaparlar.", 21);
        addTutorial(db, lang, "22. Bulut Bilişim (Cloud)", "Verilerin ve işlemlerin internet üzerindeki uzak sunucularda yapılmasıdır. (Google Drive, AWS, Azure).", 22);
        addTutorial(db, lang, "23. Sanallaştırma", "Bir fiziksel bilgisayar üzerinde birden fazla sanal bilgisayar (VM) çalıştırma teknolojisidir.", 23);
        addTutorial(db, lang, "24. Yapay Zeka Donanımları", "NPU (Neural Processing Unit): Yapay zeka işlemleri için özelleşmiş işlemcilerdir. Tensor çekirdekleri (NVIDIA) buna örnektir.", 24);
        addTutorial(db, lang, "25. Nesnelerin İnterneti (IoT)", "Akıllı saat, buzdolabı, lamba gibi cihazların internete bağlanıp veri paylaşmasıdır.", 25);
        addTutorial(db, lang, "26. Siber Güvenlik Donanımları", "Firewall (Güvenlik Duvarı) cihazları ağ trafiğini denetler ve saldırıları engeller.", 26);
        addTutorial(db, lang, "27. Kuantum Bilgisayarlar", "Bit yerine Qubit kullanır. Süperpozisyon sayesinde klasik bilgisayarların çözemediği problemleri saniyeler içinde çözer.", 27);
        addTutorial(db, lang, "28. Gömülü Sistemler", "Arduino, Raspberry Pi gibi tek bir işi yapmak üzere tasarlanmış minik bilgisayarlardır.", 28);
        addTutorial(db, lang, "29. Overclock (Hız Aşırtma)", "İşlemci veya Ekran Kartının voltajını artırarak fabrika hızının üzerine çıkarma işlemidir. Soğutma çok iyi olmalıdır.", 29);
        addTutorial(db, lang, "30. Ergonomi", "Donanımların insan sağlığına uygun kullanımıdır. Monitör yüksekliği, klavye açısı ve koltuk desteği önemlidir.", 30);
    }

    private void addJavaTutorials(SQLiteDatabase db) {
        String lang = "Java";

        // --- BÖLÜM 1: TEMELLER ---
        addTutorial(db, lang, "1. Java Giriş", "Java; popüler, platform bağımsız (WORA), nesne yönelimli ve güvenli bir dildir. Mobil, Web ve Kurumsal uygulamalarda kullanılır.", 1);
        addTutorial(db, lang, "2. JDK, JRE ve JVM", "JDK: Geliştirme Kiti.\nJRE: Çalıştırma Ortamı.\nJVM: Java Sanal Makinesi (Kodun her bilgisayarda çalışmasını sağlar).", 2);
        addTutorial(db, lang, "3. Sözdizimi (Syntax)", "Her Java kodu bir 'class' içinde olmalıdır.\npublic class Main {\n  public static void main(String[] args) {\n    // Kodlar buraya\n  }\n}", 3);
        addTutorial(db, lang, "4. Çıktı (Output)", "System.out.println() -> Yazar ve alt satıra geçer.\nSystem.out.print() -> Yazar ve aynı satırda kalır.", 4);
        addTutorial(db, lang, "5. Yorum Satırları", "// Tek satırlı yorum\n/* Çok satırlı\nyorum bloğu */\nDerleyici bu kısımları atlar.", 5);
        addTutorial(db, lang, "6. Değişkenler", "Veri saklama alanlarıdır. Tip zorunludur.\nString isim = 'Ali';\nint yas = 25;\nfinal int PI = 3 (Sabit).", 6);
        addTutorial(db, lang, "7. Veri Tipleri", "İlkel (Primitive): int, float, char, boolean, byte.\nReferans: String, Array, Class.\nJava 'Strongly Typed' bir dildir.", 7);
        addTutorial(db, lang, "8. Tip Dönüşümü (Casting)", "Otomatik (Widening): int -> double\nManuel (Narrowing): double -> int\nint myInt = (int) 9.78;", 8);
        addTutorial(db, lang, "9. Operatörler", "Aritmetik (+, -), Atama (=, +=), Karşılaştırma (==, >), Mantıksal (&&, ||).", 9);
        addTutorial(db, lang, "10. Stringler", "Metin sınıfıdır.\nString txt = 'Hello';\ntxt.length(); // Uzunluk\ntxt.toUpperCase(); // Büyük harf", 10);
        addTutorial(db, lang, "11. Matematik (Math)", "Math.max(5, 10); // 10\nMath.sqrt(64); // 8.0\nMath.random(); // 0.0 - 1.0 arası sayı.", 11);
        addTutorial(db, lang, "12. Booleans", "true veya false değeri alır.\nboolean isJavaFun = true;\nif (isJavaFun) System.out.println('Evet!');", 12);
        addTutorial(db, lang, "13. If ... Else", "Şart bloklarıdır.\nif (x > y) {\n  // x büyükse çalışır\n} else {\n  // Değilse çalışır\n}", 13);
        addTutorial(db, lang, "14. Switch Case", "Bir değişkenin değerine göre ilgili 'case' bloğunu çalıştırır.\nbreak; komutu kullanımı önemlidir.", 14);
        addTutorial(db, lang, "15. While Döngüsü", "Koşul doğru olduğu sürece çalışır.\nint i = 0;\nwhile (i < 5) {\n  System.out.println(i);\n  i++;\n}", 15);
        addTutorial(db, lang, "16. For Döngüsü", "Tekrar sayısı belliyse kullanılır.\nfor (int i = 0; i < 5; i++) {\n  // Kodlar\n}", 16);
        addTutorial(db, lang, "17. Break ve Continue", "break: Döngüyü tamamen durdurur.\ncontinue: O anki adımı atlar ve döngüye devam eder.", 17);
        addTutorial(db, lang, "18. Diziler (Arrays)", "Sabit boyutlu veri listesidir.\nString[] arabalar = {'BMW', 'Volvo'};\nSystem.out.println(arabalar[0]);", 18);

        // --- BÖLÜM 2: NESNE YÖNELİMLİ PROGRAMLAMA (OOP) ---
        addTutorial(db, lang, "19. Metodlar", "İşlevsel kod bloklarıdır.\nstatic void myMethod() {\n  System.out.println('Çalıştı');\n}", 19);
        addTutorial(db, lang, "20. Parametreler", "Metoda veri gönderme.\nvoid selamla(String isim) {\n  System.out.println(isim);\n}", 20);
        addTutorial(db, lang, "21. Method Overloading", "Aynı isimde farklı parametreli metodlar.\nint topla(int a, int b)\ndouble topla(double a, double b)", 21);
        addTutorial(db, lang, "22. Sınıflar ve Nesneler", "Class: Araba (Şablon)\nObject: Kırmızı BMW (Örnek)\nAraba myCar = new Araba();", 22);
        addTutorial(db, lang, "23. Class Attributes", "Sınıf içindeki değişkenlerdir.\nclass Person {\n  String name = 'John';\n}", 23);
        addTutorial(db, lang, "24. Class Methods", "Sınıf içindeki fonksiyonlardır.\npublic void run() { ... }", 24);
        addTutorial(db, lang, "25. Constructor (Yapıcı)", "Nesne oluşturulurken çalışan özel metottur.\nSınıf ismiyle aynı olmalıdır.\npublic MyClass() { x = 5; }", 25);
        addTutorial(db, lang, "26. Erişim Belirleyiciler", "public: Her yerden\nprivate: Sadece sınıf içinden\nprotected: Paket ve alt sınıflardan erişim.", 26);
        addTutorial(db, lang, "27. Encapsulation (Kapsülleme)", "Veriyi gizleme sanatıdır.\nPrivate değişkenlere erişmek için get ve set metodları kullanılır.", 27);
        addTutorial(db, lang, "28. Paketler (Packages)", "Sınıfları gruplamak için kullanılır.\nYerleşik paketler: java.util, java.io\nKullanıcı tanımlı paketler.", 28);
        addTutorial(db, lang, "29. Inheritance (Kalıtım)", "Bir sınıfın özelliklerini diğerine aktarır.\nclass Arac { ... }\nclass Araba extends Arac { ... }", 29);
        addTutorial(db, lang, "30. Polymorphism (Çok Biçimlilik)", "Aynı isimli metodun farklı sınıflarda farklı işler yapmasıdır.\nAnimal -> sesCikar() (Kedi miyavlar, Köpek havlar).", 30);

        // --- BÖLÜM 3: İLERİ JAVA ---
        addTutorial(db, lang, "31. Inner Classes", "Sınıf içinde sınıf tanımlamaktır.\nGruplama ve erişim kolaylığı sağlar.", 31);
        addTutorial(db, lang, "32. Abstraction (Soyutlama)", "Detayları gizleyip sadece önemli bilgileri göstermektir.\nabstract class ve interface kullanılır.", 32);
        addTutorial(db, lang, "33. Interface (Arayüz)", "Tamamen soyut bir sınıftır.\nclass Demo implements IFirstInterface { ... }", 33);
        addTutorial(db, lang, "34. Enums", "Sabitlerden oluşan özel bir sınıftır.\nenum Level { LOW, MEDIUM, HIGH }\nDeğişmeyen değerler için kullanılır.", 34);
        addTutorial(db, lang, "35. User Input (Scanner)", "Kullanıcıdan veri almak için kullanılır.\nScanner sc = new Scanner(System.in);\nString ad = sc.nextLine();", 35);
        addTutorial(db, lang, "36. Date & Time", "Tarih işlemleri.\nLocalDate, LocalTime, LocalDateTime sınıfları (Java 8+) kullanılır.", 36);
        addTutorial(db, lang, "37. ArrayList", "Boyutu değiştirilebilir dizidir.\nArrayList<String> list = new ArrayList<>();\nlist.add('Elma');", 37);
        addTutorial(db, lang, "38. HashMap", "Anahtar-Değer çiftleri tutar.\nHashMap<String, String> cities = new HashMap<>();\ncities.put('TR', 'Ankara');", 38);
        addTutorial(db, lang, "39. HashSet", "Benzersiz öğeler koleksiyonudur.\nTekrar eden elemanları barındırmaz.", 39);
        addTutorial(db, lang, "40. Iterator", "Koleksiyonlar üzerinde gezinmek için kullanılan nesnedir.\nit.next(); ile sıradaki elemana geçilir.", 40);
        addTutorial(db, lang, "41. Wrapper Classes", "İlkel tipleri nesneye çevirir.\nint -> Integer\nchar -> Character\nArrayList gibi yapılarda gereklidir.", 41);
        addTutorial(db, lang, "42. Exceptions (Hata Yönetimi)", "try-catch blokları ile programın çökmesi engellenir.\ntry { ... } catch (Exception e) { ... } finally { ... }", 42);
        addTutorial(db, lang, "43. RegEx", "Metin içinde desen aramak için kullanılır.\nPattern ve Matcher sınıfları vardır.", 43);
        addTutorial(db, lang, "44. Threads (İş Parçacıkları)", "Java'da çoklu işlem (Multitasking) yapmayı sağlar.\nThread sınıfı veya Runnable arayüzü ile oluşturulur.", 44);
        addTutorial(db, lang, "45. Lambda Expressions", "Kısa ve anonim fonksiyonlardır (Java 8+).\n(parametre) -> ifade\nKodun okunabilirliğini artırır.", 45);
        addTutorial(db, lang, "46. File Handling", "Dosya oluşturma, okuma, yazma ve silme işlemleri.\nFile sınıfı kullanılır.", 46);
    }

    private void addCPPTutorials(SQLiteDatabase db) {
        String lang = "C++";

        // --- TEMELLER ---
        addTutorial(db, lang, "1. C++ Giriş", "C++, 1979'da Bjarne Stroustrup tarafından geliştirilmiştir.\nC'nin 'Sınıflar' eklenmiş halidir.\nYüksek performanslı oyunlar ve sistemler için kullanılır.", 1);
        addTutorial(db, lang, "2. Sözdizimi", "#include <iostream>\nusing namespace std;\nint main() {\n  cout << 'Merhaba';\n  return 0;\n}", 2);
        addTutorial(db, lang, "3. Çıktı (Output)", "cout << 'Yazı';\n<< operatörü kullanılır.\nYeni satır için \\n veya endl kullanılır.", 3);
        addTutorial(db, lang, "4. Girdi (Input)", "Kullanıcıdan veri almak için cin kullanılır.\nint x;\ncin >> x;\n>> operatörü kullanılır.", 4);
        addTutorial(db, lang, "5. Değişkenler", "int (Tam sayı), double (Ondalıklı), char (Karakter), string (Metin), bool (Mantık).", 5);
        addTutorial(db, lang, "6. Veri Tipleri", "int: 4 bytes\ndouble: 8 bytes\nchar: 1 byte ('A')\nbool: 1 byte (true/false)", 6);
        addTutorial(db, lang, "7. Operatörler", "Aritmetik (+, -, *, /, %), Atama (=), Karşılaştırma (==, !=), Mantıksal (&&, ||, !).", 7);
        addTutorial(db, lang, "8. Stringler", "Metin saklar.\n#include <string> gerekir.\nstring ad = 'Ali';\nstring soyad = 'Veli';", 8);
        addTutorial(db, lang, "9. Matematik", "<cmath> kütüphanesi.\nsqrt(64); // Karekök\nround(2.6); // Yuvarlama\nmax(5, 10); // En büyük", 9);
        addTutorial(db, lang, "10. If ... Else", "Koşullar.\nif (condition) {\n  // kod\n} else {\n  // kod\n}", 10);
        addTutorial(db, lang, "11. Switch", "Çoklu seçim yapısı.\nswitch(deger) {\n  case 1: ... break;\n  default: ...\n}", 11);
        addTutorial(db, lang, "12. While Döngüsü", "Şart sağlandıkça döner.\nwhile (i < 5) {\n  cout << i;\n  i++;\n}", 12);
        addTutorial(db, lang, "13. For Döngüsü", "Sayaçlı döngü.\nfor (int i = 0; i < 5; i++) {\n  cout << i;\n}", 13);
        addTutorial(db, lang, "14. Break/Continue", "break: Döngüden çıkar.\ncontinue: Bir sonraki adıma geçer.", 14);
        addTutorial(db, lang, "15. Diziler", "Sabit boyutlu listeler.\nstring cars[3] = {'Volvo', 'BMW', 'Ford'};\ncout << cars[0];", 15);
        addTutorial(db, lang, "16. Structures (Struct)", "Farklı tipteki verileri gruplar.\nstruct Car {\n  string brand;\n  int year;\n};", 16);
        addTutorial(db, lang, "17. References", "Değişkenin takma adıdır (Alias).\nstring food = 'Pizza';\nstring &meal = food;\nİkisi de aynı belleği gösterir.", 17);
        addTutorial(db, lang, "18. Pointerlar", "Bellek adresini tutan değişkendir.\nstring* ptr = &food;\n*ptr ile değere ulaşılır.", 18);

        // --- İLERİ SEVİYE & OOP ---
        addTutorial(db, lang, "19. Fonksiyonlar", "Kod bloklarıdır.\nvoid myFunction() {\n  cout << 'Çalıştı';\n}", 19);
        addTutorial(db, lang, "20. Parametreler", "Fonksiyona değer gönderme.\nvoid isimYaz(string ad) {\n  cout << ad;\n}", 20);
        addTutorial(db, lang, "21. Function Overloading", "Aynı isimde farklı parametreli fonksiyonlar.\nint topla(int x)\ndouble topla(double x)", 21);
        addTutorial(db, lang, "22. OOP Giriş", "Nesne Yönelimli Programlama.\nSınıflar (Class) ve Nesneler (Object) üzerine kuruludur.", 22);
        addTutorial(db, lang, "23. Class/Object", "class MyClass { ... };\nMyClass myObj;\nmyObj.myNum = 15;", 23);
        addTutorial(db, lang, "24. Constructors", "Nesne oluşturulurken otomatik çalışan özel metottur.", 24);
        addTutorial(db, lang, "25. Access Specifiers", "public: Her yerden erişim.\nprivate: Sadece sınıf içi.\nprotected: Kalıtım alanlar.", 25);
        addTutorial(db, lang, "26. Encapsulation", "Veriyi korumak için private değişkenler ve public get/set metodları kullanma.", 26);
        addTutorial(db, lang, "27. Inheritance", "Miras alma.\nclass Car : public Vehicle { ... };\nKod tekrarını azaltır.", 27);
        addTutorial(db, lang, "28. Polymorphism", "Çok biçimlilik.\nAynı metodun farklı sınıflarda farklı çalışması.", 28);
        addTutorial(db, lang, "29. Files (Dosyalar)", "<fstream> kütüphanesi.\nofstream: Dosya yazar.\nifstream: Dosya okur.", 29);
        addTutorial(db, lang, "30. Exceptions", "Hata yakalama.\ntry { ... } catch (int myNum) { ... }\nProgramın çökmesini engeller.", 30);
    }






    private void addCsharpTutorials(SQLiteDatabase db) {
        String lang = "C#";

        // --- TEMELLER ---
        addTutorial(db, lang, "1. C# Giriş", "C# (C-Sharp), Microsoft tarafından geliştirilen modern, nesne yönelimli ve tip güvenli bir dildir. .NET platformunda çalışır.", 1);
        addTutorial(db, lang, "2. Sözdizimi", "namespace HelloApp {\n  class Program {\n    static void Main(string[] args) {\n      Console.WriteLine('Merhaba');\n    }\n  }\n}", 2);
        addTutorial(db, lang, "3. Çıktı (Output)", "Console.WriteLine() -> Yazar ve alt satıra geçer.\nConsole.Write() -> Yan yana yazar.", 3);
        addTutorial(db, lang, "4. Değişkenler", "int (Tam sayı), double (Ondalıklı), char (Karakter), string (Metin), bool (Mantık).\nint yas = 25;\nstring ad = 'Ali';", 4);
        addTutorial(db, lang, "5. Veri Tipleri", "int: 4 byte\nlong: 8 byte\nfloat: 4 byte (sonuna F konur: 5.75F)\ndouble: 8 byte\nbool: true/false", 5);
        addTutorial(db, lang, "6. Tip Dönüşümü", "Implicit (Otomatik): int -> double\nExplicit (Manuel): double -> int\nint i = (int) 9.78;", 6);
        addTutorial(db, lang, "7. Kullanıcı Girdisi", "Console.ReadLine() her zaman string döndürür.\nint yas = Convert.ToInt32(Console.ReadLine());", 7);
        addTutorial(db, lang, "8. Operatörler", "Aritmetik (+, -, *, /, %), Atama (=, +=), Karşılaştırma (==, >), Mantıksal (&&, ||, !).", 8);
        addTutorial(db, lang, "9. Math Sınıfı", "Math.Max(5, 10); // 10\nMath.Sqrt(64); // 8\nMath.Round(9.99); // 10\nMath.Abs(-5); // 5", 9);
        addTutorial(db, lang, "10. Stringler", "string txt = 'Merhaba';\ntxt.Length; // Uzunluk\ntxt.ToUpper(); // Büyük harf\nString Interpolation: $\"Adım {ad}\"", 10);
        addTutorial(db, lang, "11. Booleans", "Mantıksal ifadeler.\nbool isCodingFun = true;\nConsole.WriteLine(isCodingFun); // True", 11);
        addTutorial(db, lang, "12. If ... Else", "if (condition) {\n  // kod\n} else if (condition) {\n  // kod\n} else {\n  // kod\n}", 12);
        addTutorial(db, lang, "13. Switch", "switch(day) {\n  case 1:\n    Console.WriteLine('Pazartesi');\n    break;\n  default:\n    Console.WriteLine('Haftasonu');\n    break;\n}", 13);
        addTutorial(db, lang, "14. While Loop", "Döngüler.\nint i = 0;\nwhile (i < 5) {\n  Console.WriteLine(i);\n  i++;\n}", 14);
        addTutorial(db, lang, "15. For Loop", "Sayaçlı döngü.\nfor (int i = 0; i < 5; i++) {\n  Console.WriteLine(i);\n}", 15);
        addTutorial(db, lang, "16. Foreach Loop", "Diziler için özel döngü.\nstring[] cars = {'Volvo', 'BMW'};\nforeach (string i in cars) {\n  Console.WriteLine(i);\n}", 16);
        addTutorial(db, lang, "17. Break/Continue", "break: Döngüden atar.\ncontinue: Bir sonraki adıma geçer.", 17);
        addTutorial(db, lang, "18. Diziler", "string[] cars = {'Volvo', 'BMW'};\nArray.Sort(cars); // Sıralama\nConsole.WriteLine(cars.Length);", 18);

        // --- OOP & İLERİ SEVİYE ---
        addTutorial(db, lang, "19. Metodlar", "Fonksiyonlar.\nstatic void MyMethod() {\n  Console.WriteLine('Çalıştı');\n}", 19);
        addTutorial(db, lang, "20. Parametreler", "Metoda veri gönderme.\nstatic void IsimYaz(string ad) {\n  Console.WriteLine(ad);\n}", 20);
        addTutorial(db, lang, "21. Method Overloading", "Aynı isimde farklı parametreli metodlar.\nint Plus(int x, int y)\ndouble Plus(double x, double y)", 21);
        addTutorial(db, lang, "22. Sınıflar (Class)", "Nesne şablonudur.\nclass Car {\n  string color = 'red';\n}", 22);
        addTutorial(db, lang, "23. Nesneler (Object)", "Sınıfın örneğidir.\nCar myCar = new Car();\nConsole.WriteLine(myCar.color);", 23);
        addTutorial(db, lang, "24. Constructors", "Yapıcı metodlar.\nclass Car {\n  public Car() {\n    model = 'Mustang';\n  }\n}", 24);
        addTutorial(db, lang, "25. Access Modifiers", "public: Her yerden.\nprivate: Sadece sınıf içi.\nprotected: Kalıtım alanlar.\ninternal: Sadece aynı proje (assembly) içi.", 25);
        addTutorial(db, lang, "26. Properties", "Encapsulation (Kapsülleme) için kullanılır.\npublic string Name { get; set; }", 26);
        addTutorial(db, lang, "27. Inheritance", "Miras alma.\nclass Vehicle { ... }\nclass Car : Vehicle { ... }", 27);
        addTutorial(db, lang, "28. Polymorphism", "Çok biçimlilik. virtual ve override anahtar kelimeleri kullanılır.", 28);
        addTutorial(db, lang, "29. Interface", "Sadece metod imzalarını içerir.\ninterface IAnimal {\n  void animalSound();\n}", 29);
        addTutorial(db, lang, "30. Enum", "Sabitler grubu.\nenum Level { Low, Medium, High }\nLevel myVar = Level.Medium;", 30);
        addTutorial(db, lang, "31. Files", "System.IO kütüphanesi.\nFile.WriteAllText('filename.txt', 'Hello');\nstring content = File.ReadAllText('filename.txt');", 31);
        addTutorial(db, lang, "32. Exceptions", "Hata yönetimi.\ntry { ... } catch (Exception e) { ... } finally { ... }", 32);
    }



    private void addCTutorials(SQLiteDatabase db) {
        String lang = "C";

        // --- TEMELLER ---
        addTutorial(db, lang, "1. C Giriş", "C, 1972'de Dennis Ritchie tarafından geliştirildi. İşletim sistemleri, sürücüler ve gömülü sistemler için kullanılan çok hızlı bir dildir.", 1);
        addTutorial(db, lang, "2. Sözdizimi", "#include <stdio.h>\nint main() {\n  printf('Merhaba Dünya');\n  return 0;\n}", 2);
        addTutorial(db, lang, "3. Çıktı (Output)", "printf() fonksiyonu kullanılır.\nYeni satır için \\n kullanılır.\nprintf('Merhaba\\nDünya');", 3);
        addTutorial(db, lang, "4. Yorumlar", "// Tek satırlı yorum\n/* Çok satırlı\nyorum */", 4);
        addTutorial(db, lang, "5. Değişkenler", "int myNum = 15;\nfloat myFloat = 5.99;\nchar myLetter = 'D';\nC dilinde String veri tipi yoktur (char dizisi kullanılır).", 5);
        addTutorial(db, lang, "6. Format Belirleyiciler", "Değişken yazdırmak için kullanılır.\n%d -> int\n%f -> float\n%c -> char\nprintf('Sayım: %d', myNum);", 6);
        addTutorial(db, lang, "7. Değişken Değiştirme", "int x = 5;\nx = 10;\nprintf('%d', x); // 10 yazar", 7);
        addTutorial(db, lang, "8. Çoklu Değişken", "int x = 5, y = 6, z = 50;\nprintf('%d', x + y + z);", 8);
        addTutorial(db, lang, "9. Sabitler (Constants)", "const int MYNUM = 15;\nDeğeri sonradan değiştirilemez.", 9);
        addTutorial(db, lang, "10. Operatörler", "Aritmetik (+, -, *, /, %)\nArtırma (++), Azaltma (--)\nsizeof(type) -> Boyutu byte olarak verir.", 10);
        addTutorial(db, lang, "11. If ... Else", "if (20 > 18) {\n  printf('20 büyüktür');\n} else {\n  printf('değildir');\n}", 11);
        addTutorial(db, lang, "12. Switch", "switch(expression) {\n  case x:\n    // kod\n    break;\n  default:\n    // kod\n}", 12);
        addTutorial(db, lang, "13. While Loop", "int i = 0;\nwhile (i < 5) {\n  printf('%d\\n', i);\n  i++;\n}", 13);
        addTutorial(db, lang, "14. For Loop", "for (int i = 0; i < 5; i++) {\n  printf('%d\\n', i);\n}", 14);
        addTutorial(db, lang, "15. Break/Continue", "break: Döngüden çıkar.\ncontinue: O adımı atlar.", 15);
        addTutorial(db, lang, "16. Diziler", "int myNumbers[] = {25, 50, 75, 100};\nprintf('%d', myNumbers[0]);", 16);
        addTutorial(db, lang, "17. Stringler", "char greetings[] = 'Hello World!';\nprintf('%s', greetings);", 17);
        addTutorial(db, lang, "18. Kullanıcı Girdisi", "scanf() kullanılır.\nint myNum;\nscanf('%d', &myNum);\n& işareti değişkenin adresini belirtir.", 18);

        // --- İLERİ SEVİYE & POINTERS ---
        addTutorial(db, lang, "19. Bellek Adresi", "Değişkenin bellekteki yerini bulmak için & kullanılır.\nprintf('%p', &myNum); // 0x7ffe5367e044", 19);
        addTutorial(db, lang, "20. Pointerlar (İşaretçiler)", "Başka bir değişkenin adresini tutan değişkendir.\nint* ptr = &myNum;\nprintf('%p', ptr);", 20);
        addTutorial(db, lang, "21. Dereference (*)", "Pointer'ın gösterdiği adresteki değeri almak için kullanılır.\nprintf('%d', *ptr); // myNum değerini verir.", 21);
        addTutorial(db, lang, "22. Fonksiyonlar", "void myFunction() {\n  printf('Çalıştı');\n}\nmain'den önce tanımlanmalı veya deklare edilmelidir.", 22);
        addTutorial(db, lang, "23. Parametreler", "void isimYaz(char name[]) {\n  printf('Hello %s', name);\n}", 23);
        addTutorial(db, lang, "24. Scope (Kapsam)", "Local: Fonksiyon içi.\nGlobal: Tüm dosya.\nGlobal değişkenlere her yerden erişilebilir.", 24);
        addTutorial(db, lang, "25. Math Functions", "#include <math.h>\nsqrt(16); // 4\nceil(1.4); // 2\npow(4, 3); // 64", 25);
        addTutorial(db, lang, "26. Files (Dosyalar)", "FILE *fptr;\nfptr = fopen('filename.txt', 'w');\nfprintf(fptr, 'Yazı yaz');\nfclose(fptr);", 26);
        addTutorial(db, lang, "27. Dosya Okuma", "fptr = fopen('filename.txt', 'r');\nfgets(myString, 100, fptr);", 27);
        addTutorial(db, lang, "28. Structs (Yapılar)", "Farklı tipte verileri gruplar.\nstruct Person {\n  int myNum;\n  char myLetter;\n};\nstruct Person s1;", 28);
        addTutorial(db, lang, "29. Enum", "enum Level {LOW, MEDIUM, HIGH};\nenum Level myVar = MEDIUM;", 29);
        addTutorial(db, lang, "30. Memory Management", "malloc(), calloc(), free().\nDinamik bellek yönetimi C'nin en güçlü yanıdır ama dikkatli kullanılmalıdır.", 30);
    }






    private void addJSTutorials(SQLiteDatabase db) {
        String lang = "JavaScript";

        // --- TEMELLER ---
        addTutorial(db, lang, "1. JS Giriş", "JavaScript, web sayfalarını interaktif hale getiren dünyanın en popüler programlama dilidir.", 1);
        addTutorial(db, lang, "2. Nereye Yazılır?", "HTML içinde <script> etiketleri arasına veya harici .js dosyasına yazılır.\n<script src='script.js'></script>", 2);
        addTutorial(db, lang, "3. Çıktı (Output)", "innerHTML: HTML içine yazar.\ndocument.write(): Sayfaya yazar.\nwindow.alert(): Uyarı kutusu.\nconsole.log(): Konsola yazar.", 3);
        addTutorial(db, lang, "4. Değişkenler", "var (Eski), let (Güncel), const (Sabit).\nlet x = 5;\nconst PI = 3.14;", 4);
        addTutorial(db, lang, "5. Operatörler", "Atama (=), Aritmetik (+, -), Karşılaştırma (==, ===), Mantıksal (&&, ||).\n=== hem değeri hem tipi kontrol eder.", 5);
        addTutorial(db, lang, "6. Veri Tipleri", "String, Number, Boolean, Object, Array, Undefined, Null.\nJavaScript dinamik tiplidir.", 6);
        addTutorial(db, lang, "7. Fonksiyonlar", "function topla(p1, p2) {\n  return p1 + p2;\n}", 7);
        addTutorial(db, lang, "8. Objeler (Objects)", "const car = {type:'Fiat', model:'500', color:'white'};\nErişim: car.type veya car['type']", 8);
        addTutorial(db, lang, "9. Olaylar (Events)", "onclick, onchange, onmouseover, onkeydown.\n<button onclick='this.innerHTML=Date()'>Zaman</button>", 9);
        addTutorial(db, lang, "10. Stringler", "Metinler. Tek veya çift tırnak.\nlet text = 'Hello';\ntext.length; // Uzunluk", 10);
        addTutorial(db, lang, "11. String Metodları", "slice(), substring(), replace(), toUpperCase(), trim(), split(), includes().", 11);
        addTutorial(db, lang, "12. Template Literals", "Back-tick (`) kullanılır.\nlet text = `Welcome ${firstName}, ${lastName}`;\nÇok satırlı metin yazılabilir.", 12);
        addTutorial(db, lang, "13. Sayılar", "Tek tür sayı vardır (Number). Her zaman 64-bit Floating Point.\nNaN: Not a Number (Sayı değil hatası).", 13);
        addTutorial(db, lang, "14. Diziler (Arrays)", "const cars = ['Saab', 'Volvo', 'BMW'];\nFarklı veri tiplerini aynı dizide tutabilir.", 14);
        addTutorial(db, lang, "15. Dizi Metodları", "push() (Ekle), pop() (Çıkar), shift(), unshift(), join(), concat().", 15);
        addTutorial(db, lang, "16. Dizi Sıralama", "sort(): Alfabetik sıralar.\nreverse(): Ters çevirir.\nSayısal sıralama için karşılaştırma fonksiyonu gerekir.", 16);
        addTutorial(db, lang, "17. Dizi İterasyon", "forEach(), map(), filter(), reduce(), every(), find().\nMap yeni bir dizi oluşturur.", 17);
        addTutorial(db, lang, "18. Tarih (Dates)", "const d = new Date();\ngetFullYear(), getMonth(), getDate(), getHours().", 18);
        addTutorial(db, lang, "19. Math", "Math.round(), Math.ceil(), Math.floor(), Math.random(), Math.max().", 19);
        addTutorial(db, lang, "20. Koşullar", "if, else, else if.\nKısa if: (condition) ? true : false;", 20);
        addTutorial(db, lang, "21. Switch", "switch(expression) {\n  case x:\n    // code\n    break;\n}", 21);
        addTutorial(db, lang, "22. Döngüler", "for loop, for/in (Objeler), for/of (Diziler), while loop.", 22);
        addTutorial(db, lang, "23. Type Conversion", "String() ile metne, Number() ile sayıya çevirme.\n'5' + 5 = '55' (String birleştirme).", 23);
        addTutorial(db, lang, "24. RegEx", "Arama desenleri.\n/w3schools/i  (i: büyük/küçük harf duyarsız).\nsearch() ve replace() ile kullanılır.", 24);
        addTutorial(db, lang, "25. Errors", "try, catch, throw, finally.\nÖzel hata fırlatmak için: throw 'Çok büyük';", 25);

        // --- İLERİ SEVİYE & MODERN JS (ES6+) ---
        addTutorial(db, lang, "26. this Anahtar Kelimesi", "Çağırıldığı yere göre değişir.\nObjede: Objeyi temsil eder.\nGlobalde: Window objesini temsil eder.", 26);
        addTutorial(db, lang, "27. Arrow Function", "ES6 ile gelen kısa fonksiyon.\nconst hello = () => 'Hello World!';", 27);
        addTutorial(db, lang, "28. Classes", "ES6 Sınıfları.\nclass Car {\n  constructor(name) {\n    this.name = name;\n  }\n}", 28);
        addTutorial(db, lang, "29. Modules", "Import ve Export.\nexport const name = 'Jesse';\nimport { name } from './person.js';", 29);
        addTutorial(db, lang, "30. JSON", "Veri değişim formatı.\nJSON.parse(): Metni objeye çevirir.\nJSON.stringify(): Objeyi metne çevirir.", 30);
        addTutorial(db, lang, "31. Asynchronous", "Eş zamanlı olmayan işlemler.\nsetTimeout(), setInterval(), Callback fonksiyonlar.", 31);
        addTutorial(db, lang, "32. Promises", "Gelecekteki bir değeri temsil eder.\nmyPromise.then().catch();", 32);
        addTutorial(db, lang, "33. Async / Await", "Promise yazımını kolaylaştırır.\nasync function myFunction() {\n  await myPromise;\n}", 33);
        addTutorial(db, lang, "34. DOM Giriş", "Document Object Model.\nHTML elemanlarına erişmek ve değiştirmek için kullanılır.", 34);
        addTutorial(db, lang, "35. DOM Methods", "getElementById(), getElementsByClassName(), querySelector().\ndocument.getElementById('demo').innerHTML = 'Hello';", 35);
        addTutorial(db, lang, "36. Web API", "Tarayıcı özelliklerini kullanma.\nGeolocation (Konum), Fetch (Veri çekme), LocalStorage (Veri saklama).", 36);
    }
}