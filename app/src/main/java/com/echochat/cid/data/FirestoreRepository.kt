package com.echochat.cid.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

data class RemoteUser(
    val uid: String,
    val displayName: String,
    val avatarBase64: String?
)

data class RemoteMessage(
    val remoteId: String,
    val senderUid: String,
    val content: String,
    val timestampMillis: Long
)

/**
 * Lapisan sinkron lewat Cloud Firestore. Tidak pakai Firebase Auth sama sekali —
 * UID akun tamu dipakai langsung sebagai document id, jadi tidak butuh SHA-1/login apa pun.
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val usersRef = db.collection("users")
    private val chatsRef = db.collection("chats")

    /** Daftarkan/perbarui keberadaan UID sendiri supaya bisa ditemukan teman. */
    fun registerPresence(uid: String, displayName: String, avatarBase64: String?) {
        val data = hashMapOf<String, Any>(
            "displayName" to displayName,
            "lastSeen" to System.currentTimeMillis()
        )
        if (avatarBase64 != null) data["avatarBase64"] = avatarBase64
        usersRef.document(uid).set(data, SetOptions.merge())
    }

    /** Cek apakah sebuah UID benar-benar pernah terdaftar (aktif). */
    suspend fun uidExists(uid: String): Boolean {
        val snapshot = usersRef.document(uid).get().await()
        return snapshot.exists()
    }

    suspend fun fetchUser(uid: String): RemoteUser? {
        val snapshot = usersRef.document(uid).get().await()
        if (!snapshot.exists()) return null
        return RemoteUser(
            uid = uid,
            displayName = snapshot.getString("displayName") ?: "",
            avatarBase64 = snapshot.getString("avatarBase64")
        )
    }

    fun chatIdFor(uidA: String, uidB: String): String {
        return listOf(uidA, uidB).sorted().joinToString("_")
    }

    /** Kirim pesan; id dokumen dibuat duluan supaya bisa langsung dipakai untuk dedupe lokal. */
    fun sendMessage(chatId: String, senderUid: String, content: String): String {
        val docRef = chatsRef.document(chatId).collection("messages").document()
        val data = hashMapOf(
            "senderUid" to senderUid,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )
        docRef.set(data)
        return docRef.id
    }

    fun listenMessages(chatId: String, onNewMessage: (RemoteMessage) -> Unit): ListenerRegistration {
        return chatsRef.document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                for (change in snapshot.documentChanges) {
                    if (change.type != com.google.firebase.firestore.DocumentChange.Type.ADDED) continue
                    val doc = change.document
                    onNewMessage(
                        RemoteMessage(
                            remoteId = doc.id,
                            senderUid = doc.getString("senderUid") ?: continue,
                            content = doc.getString("content") ?: "",
                            timestampMillis = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    )
                }
            }
    }
}
