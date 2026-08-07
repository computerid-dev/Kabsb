# EchoChat

Aplikasi chat pribadi ala WhatsApp — akun tamu (tanpa OTP/OAuth), UID acak per perangkat, dan sinkron pesan real-time lewat Firestore.

**Slogan:** Ngobrol tanpa jejak, sepenuhnya milikmu
**Package:** `com.echochat.cid`

## Fitur

- Akun tamu otomatis: kode ID unik (`XXXX-XXXX`) dibuat lokal saat pertama buka
- Tambah teman lewat kode ID — **divalidasi ke Firestore**, jadi hanya UID yang benar-benar pernah dibuat yang bisa ditambahkan
- Chat teks real-time antar dua perangkat (Firestore sebagai jalur sinkron, Room sebagai cache/riwayat lokal)
- Blokir & hapus pertemanan (otomatis menghapus riwayat chat)
- Navbar bawah: **Chat** (obrolan aktif), **Kontak** (semua teman tersimpan), **Info Akun** (avatar, nama, backup)
- Avatar profil (disimpan sebagai base64 kecil di Firestore + lokal)
- Wallpaper chat custom dari galeri
- Backup/restore seluruh data (profil, teman, pesan) dalam satu file JSON

Tidak ada fitur media (foto/video/audio) maupun telepon/panggilan video — chat sengaja dibatasi teks saja.

## Setup Firebase (wajib sebelum build)

1. `app/google-services.json` sudah disertakan di repo ini (project Firebase milik pembuat proyek). Kalau ganti project Firebase, timpa file ini dengan yang baru dari Firebase Console → Project Settings → Your apps.
2. Di Firebase Console → **Firestore Database**, aktifkan (mode Native).
3. Buka tab **Rules**, tempel isi file [`firestore.rules`](./firestore.rules) di repo ini, lalu **Publish**. Tanpa ini, akses Firestore akan ditolak ("permission denied") karena project baru defaultnya mengunci semua akses.
4. Tidak perlu isi SHA-1 apa pun — aplikasi ini tidak pakai Firebase Auth/Google Sign-In, jadi keystore debug yang berubah tiap build di CI tidak berpengaruh.

## Build APK

### Lewat Android Studio
Buka folder ini → Run/Build → `app-debug.apk` muncul di `app/build/outputs/apk/debug/`.

### Lewat GitHub Actions
Push ke branch `main`, atau jalankan manual lewat tab **Actions** → **Build APK** → *Run workflow*. Unduh `app-debug` dari bagian **Artifacts**.

## Struktur proyek

```
app/src/main/java/com/echochat/cid/
├── data/       Entity, DAO, Room database, FirestoreRepository, BackupManager
├── ui/         Activity, Fragment (navbar bawah), adapter RecyclerView
└── util/       SessionManager (akun tamu), UidGenerator, ImageUtils
```
