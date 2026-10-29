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
    fun saveTaskBuffer(buffer: TaskEditBuffer) { prefs.edit().putString(taskBufferKey(buffer.server,buffer.userId), json.toJson(buffer)).apply() }
    fun taskBuffer(session: Session): TaskEditBuffer? = read(taskBufferKey(session.server,session.userId), TaskEditBuffer::class.java)?.let {
        it.copy(status=it.status ?: legacyTaskStatus(it.progress.toIntOrNull()),progress="")
    }
    fun clearTaskBuffer(session: Session) { prefs.edit().remove(taskBufferKey(session.server,session.userId)).apply() }
    fun saveDispatchBuffer(buffer: TaskDispatchBuffer) {prefs.edit().putString(dispatchKey(buffer.server,buffer.userId),json.toJson(buffer)).apply()}
    fun dispatchBuffer(session: Session): TaskDispatchBuffer?=read(dispatchKey(session.server,session.userId),TaskDispatchBuffer::class.java)
    fun clearDispatchBuffer(session: Session) {prefs.edit().remove(dispatchKey(session.server,session.userId)).apply()}
    fun taskNoteDraft(session: Session,taskId: Long): String? = prefs.getString(taskNoteDraftKey(session.server,session.userId,taskId),null)
    fun saveTaskNoteDraft(session: Session,taskId: Long,content: String) {prefs.edit().putString(taskNoteDraftKey(session.server,session.userId,taskId),content).apply()}
    fun clearTaskNoteDraft(session: Session,taskId: Long) {prefs.edit().remove(taskNoteDraftKey(session.server,session.userId,taskId)).apply()}
    private fun dispatchKey(server: String,id: Long)="task-dispatch:$server:$id"
    private fun taskBufferKey(server: String, id: Long) = "task-edit:$server:$id"
    private fun bufferKey(server: String, id: Long) = "edit:$server:$id"
    private fun <T> read(key: String, type: Class<T>): T? = try { prefs.getString(key, null)?.let { json.fromJson(it,type) } } catch (_: Exception) { null }
}
