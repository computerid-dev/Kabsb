package com.echochat.cid.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Menyimpan data akun tamu (UID lokal, nama panggilan, avatar, wallpaper chat)
 * di SharedPreferences. Identitas dibuat sendiri oleh tiap perangkat, tanpa login.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val myUid: String
        get() {
            var uid = prefs.getString(KEY_UID, null)
            if (uid == null) {
                uid = UidGenerator.generate()
                prefs.edit().putString(KEY_UID, uid).apply()
            }
            return uid
        }

    var displayName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    /** Avatar disimpan sebagai string base64 (gambar sudah dikompres kecil). */
    var avatarBase64: String?
        get() = prefs.getString(KEY_AVATAR, null)
        set(value) = prefs.edit().putString(KEY_AVATAR, value).apply()

    /** Wallpaper layar chat, disimpan sebagai content Uri string. */
    var wallpaperUri: String?
        get() = prefs.getString(KEY_WALLPAPER, null)
        set(value) = prefs.edit().putString(KEY_WALLPAPER, value).apply()

    val hasCompletedSetup: Boolean
        get() = displayName.isNotBlank()

    /** Dipakai saat impor backup, supaya identitas UID lama bisa dipulihkan di perangkat baru. */
    fun overwriteUid(uid: String) {
        prefs.edit().putString(KEY_UID, uid).apply()
    }

    companion object {
        private const val PREFS_NAME = "echochat_session"
        private const val KEY_UID = "my_uid"
        private const val KEY_NAME = "display_name"
        private const val KEY_AVATAR = "avatar_base64"
        private const val KEY_WALLPAPER = "wallpaper_uri"
    }
}
