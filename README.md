# 🤖 AuraAI — Exam Assistant Overlay

Floating bubble AI yang muncul di atas semua app ujian (Wayground, Google Form, CBT, dll).  
Paste soal → AI jawab otomatis.

## Fitur
- 🫧 **Floating bubble** — drag ke mana saja, tap untuk buka panel
- ✨ **Jawab otomatis** — paste soal, AI jawab dalam detik
- 📖 **Penjelasan lengkap** — minta AI jelaskan step-by-step
- 🫣 **Mode samaran** — sembunyikan indikator AI
- 🔄 **Multi provider** — Auto cascade, Pollinations (no key!), Gemini, Groq, Mistral, Cohere, Together AI
- 🌐 **Universal** — jalan di atas semua app

## Setup GitHub Actions

### 1. Buat Keystore
```bash
# Di Termux:
keytool -genkey -v -keystore auraai-release.jks \
  -alias auraai -keyalg RSA -keysize 2048 -validity 10000
```

### 2. Encode ke Base64
```bash
base64 -w 0 auraai-release.jks
# Copy hasilnya
```

### 3. Tambah Secrets di GitHub
Buka: `repo → Settings → Secrets → Actions → New secret`

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | hasil base64 dari langkah 2 |
| `KEYSTORE_PASSWORD` | password keystore |
| `KEY_ALIAS` | `auraai` |
| `KEY_PASSWORD` | password key |

### 4. Trigger Build
```bash
git tag v1.0.0
git push origin v1.0.0
```
APK langsung muncul di GitHub Releases!

## Providers Gratis

| Provider | Daftar di | Key |
|----------|-----------|-----|
| Pollinations | — | ❌ Tidak perlu |
| Gemini Flash | aistudio.google.com | ✅ Gratis |
| Groq (Llama3) | console.groq.com | ✅ Gratis |
| Mistral | console.mistral.ai | ✅ Gratis |
| Cohere | dashboard.cohere.com | ✅ Gratis |
| Together AI | api.together.ai | ✅ Gratis |
