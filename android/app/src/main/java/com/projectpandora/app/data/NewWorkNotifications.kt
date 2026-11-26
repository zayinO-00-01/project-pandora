package com.projectpandora.app.data

/** Old unread messages stay in the inbox; system reminders are only for new arrivals. */
internal class NewWorkNotifications {
    private var initialized = false
    private val seen = linkedSetOf<Long>()

    fun accept(items: List<AppNotification>): List<AppNotification> {
        val unread = if (initialized) items.distinctBy { it.id }.filter { !it.read && it.id !in seen } else emptyList()
        initialized = true
        items.asReversed().forEach { item -> if (!seen.contains(item.id)) seen.add(item.id) }
        while (seen.size > 600) seen.remove(seen.first())
        return unread
    }

    fun reset() {
        initialized = false
        seen.clear()
    }
}
