package com.echochat.cid.data

import com.echochat.cid.util.SessionManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Menyimpan/memuat seluruh data lokal (profil, teman, pesan) dalam satu file JSON,
 * supaya bisa dipindahkan manual antar perangkat atau disimpan sebagai cadangan.
 */
class BackupManager(
    private val database: AppDatabase,
    private val session: SessionManager
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())

        val profile = JSONObject()
        profile.put("uid", session.myUid)
        profile.put("displayName", session.displayName)
        profile.put("avatarBase64", session.avatarBase64 ?: JSONObject.NULL)
        root.put("profile", profile)

        val friendsArray = JSONArray()
        val friends = database.friendDao().snapshotAll()
        for (friend in friends) {
            val obj = JSONObject()
            obj.put("friendUid", friend.friendUid)
            obj.put("nickname", friend.nickname)
            obj.put("isBlocked", friend.isBlocked)
            obj.put("addedAt", friend.addedAt)
            friendsArray.put(obj)
        }
        root.put("friends", friendsArray)

        val messagesArray = JSONArray()
        val messages = database.messageDao().getAllOnce()
        for (message in messages) {
            val obj = JSONObject()
            obj.put("chatWithUid", message.chatWithUid)
            obj.put("content", message.content)
            obj.put("isMine", message.isMine)
            obj.put("remoteId", message.remoteId)
            obj.put("timestamp", message.timestamp)
            messagesArray.put(obj)
        }
        root.put("messages", messagesArray)

        return root.toString(2)
    }

    suspend fun importFromJson(json: String) {
        val root = JSONObject(json)

        root.optJSONObject("profile")?.let { profile ->
            val importedUid = profile.optString("uid").takeIf { it.isNotBlank() }
            val importedName = profile.optString("displayName").takeIf { it.isNotBlank() }
            val importedAvatar = if (profile.isNull("avatarBase64")) null
                else profile.optString("avatarBase64").takeIf { it.isNotBlank() }

            if (importedUid != null) session.overwriteUid(importedUid)
            if (importedName != null) session.displayName = importedName
            if (importedAvatar != null) session.avatarBase64 = importedAvatar
        }

        val friendDao = database.friendDao()
        val friendsArray = root.optJSONArray("friends")
        if (friendsArray != null) {
            for (i in 0 until friendsArray.length()) {
                val obj = friendsArray.getJSONObject(i)
                val friendUid = obj.getString("friendUid")
                val existing = friendDao.findByUid(friendUid)
                if (existing == null) {
                    friendDao.insert(
                        Friend(
                            friendUid = friendUid,
                            nickname = obj.optString("nickname", friendUid),
                            isBlocked = obj.optBoolean("isBlocked", false),
                            addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val messageDao = database.messageDao()
        val messagesArray = root.optJSONArray("messages")
        if (messagesArray != null) {
            for (i in 0 until messagesArray.length()) {
                val obj = messagesArray.getJSONObject(i)
                val remoteId = obj.optString("remoteId")
                if (remoteId.isBlank()) continue
                val existing = messageDao.findByRemoteId(remoteId)
                if (existing == null) {
                    messageDao.insert(
                        Message(
                            chatWithUid = obj.getString("chatWithUid"),
                            content = obj.getString("content"),
                            isMine = obj.getBoolean("isMine"),
                            remoteId = remoteId,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        }
    }
}
