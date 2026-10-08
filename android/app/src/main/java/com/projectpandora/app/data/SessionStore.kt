package com.projectpandora.app.data

import android.content.Context
import com.google.gson.Gson

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("pandora-session", Context.MODE_PRIVATE)
    private val json = Gson()
    fun session(): Session? = read("session", Session::class.java)
    fun save(session: Session) { prefs.edit().putString("session", json.toJson(session)).putString("server", session.server).putString("username", session.username).apply() }
    fun clearSession() { prefs.edit().remove("session").apply() }
    fun server(): String = prefs.getString("server", "") ?: ""
    fun username(): String = prefs.getString("username", "staff") ?: "staff"
    fun saveBuffer(buffer: EditBuffer) { prefs.edit().putString(bufferKey(buffer.server,buffer.userId), json.toJson(buffer)).apply() }
    fun buffer(session: Session): EditBuffer? = read(bufferKey(session.server,session.userId), EditBuffer::class.java)
    fun clearBuffer(session: Session) { prefs.edit().remove(bufferKey(session.server,session.userId)).apply() }
    private fun bufferKey(server: String, id: Long) = "edit:$server:$id"
    private fun <T> read(key: String, type: Class<T>): T? = try { prefs.getString(key, null)?.let { json.fromJson(it,type) } } catch (_: Exception) { null }
}
