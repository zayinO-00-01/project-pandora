package com.projectpandora.app.data

import android.content.Context
import com.google.gson.Gson

data class LocalPriorities(val personal: List<PanelItem>? = null, val remarks: Map<Long,String> = emptyMap())

/** Frontend-only snapshot. A future sync must explicitly reconcile it with server data. */
class PriorityStore(context: Context) {
    private val prefs=context.getSharedPreferences("pandora-priorities",Context.MODE_PRIVATE)
    private val gson=Gson()
    private fun key(session: Session)="${session.server}:${session.userId}"
    fun read(session: Session): LocalPriorities = runCatching {
        prefs.getString(key(session),null)?.let {gson.fromJson(it,LocalPriorities::class.java)} ?: LocalPriorities()
    }.getOrDefault(LocalPriorities())
    fun save(session: Session,value: LocalPriorities) {prefs.edit().putString(key(session),gson.toJson(value)).apply()}
}

fun filterMembers(members: List<UserSummary>,query: String): List<UserSummary> {
    val text=query.trim()
    return members.filter {text.isEmpty() || it.displayName.orEmpty().contains(text,true) || it.username.contains(text,true)}
}

fun calendarSummaries(date: java.time.LocalDate,logs: List<WorkLog>,tasks: List<DispatchTask>): List<String> =
    orderedTasks(tasks.filter {taskDate(it)==date}).map {it.title} +
        logs.filter {it.logDate==date.toString()}.map {it.content.lineSequence().firstOrNull {line->line.isNotBlank()}?.trim().orEmpty()}.filter {it.isNotBlank()}
