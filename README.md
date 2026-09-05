# SafeKid View - Kids Video Player & Parental Control

ایک محفوظ ویڈیو پلیئر ایپ جو بچوں کے لیے محفوظ یوٹیوب کونٹینٹ، پن کوڈ پروٹیکٹڈ پیرنٹل کنٹرولز، اور ٹائم لمٹ فراہم کرتی ہے۔

A safe video streaming and parental control Android application built with Jetpack Compose, Room Database, and Material Design 3.

---

## 📱 GitHub پر APK کیسے ڈاؤن لوڈ / Build کریں؟ (How to Build APK on GitHub)

یہ ریپازیٹری **GitHub Actions** کے ساتھ مکمل طور پر تیار ہے تاکہ آپ کو اپنے کمپیوٹر پر اینڈرائیڈ اسٹوڈیو انسٹال کیے بغیر خودکار طریقے سے APK مل سکے۔

### طریقہ 1: GitHub Actions سے خودکار Build (Automatic Build via GitHub Actions)
1. اس کوڈ کو اپنے GitHub اکاؤنٹ میں Push کریں یا AI Studio کے **Push to GitHub** فیچر کا استعمال کریں۔
2. اپنے GitHub Repository میں جا کر اوپر **Actions** ٹیب پر کلک کریں۔
3. بائیں طرف **Build Android APK** ورک فلو پر کلک کریں۔
4. **Run workflow** بٹن دبا کر `main` برانچ منتخب کریں اور چلائیں۔
5. جب ورک فلو مکمل ہو جائے (سبز رنگ کا ٹک نشان آجائے)، تو اس رن پر کلک کریں۔
6. نیچے **Artifacts** سیکشن میں آپ کو **`SafeKidView-APK`** کی زپ فائل مل جائے گی جس کے اندر انسٹال کے لیے تیار **`app-debug.apk`** موجود ہوگی۔

---

### طریقہ 2: لوکل مشین پر Build کرنا (Build Locally on PC)

اگر آپ اپنے کمپیوٹر پر خود بنانا چاہتے ہیں:
```bash
# 1. کوڈ کلون کریں
git clone <your-repo-url>
cd <repo-folder>

# 2. .env فائل تیار کریں
cp .env.example .env

# 3. APK بنائیں (Linux / macOS)
chmod +x gradlew
./gradlew assembleDebug

# Windows (Command Prompt / PowerShell)
gradlew.bat assembleDebug
```

تیار شدہ APK یہاں موجود ہوگی:
`app/build/outputs/apk/debug/app-debug.apk`
