package com.projectpandora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NewWorkNotificationsTest {
    private fun item(id: Long, read: Boolean = false) =
        AppNotification(id, "task", "任务", "内容", 1L, read, "2026-11-26T12:00:00Z")

    @Test fun firstSnapshotIsQuietAndOnlyNewUnreadMessagesAlertOnce() {
        val notifications = NewWorkNotifications()
        assertEquals(emptyList<AppNotification>(), notifications.accept(listOf(item(12))))
        assertEquals(listOf(13L), notifications.accept(listOf(item(14, true), item(13), item(12))).map { it.id })
        assertEquals(emptyList<AppNotification>(), notifications.accept(listOf(item(14, true), item(13), item(12))))
    }

    @Test fun anEmptyFirstSnapshotIsStillReadyAndAnAccountResetDoesNotReplayHistory() {
        val notifications = NewWorkNotifications()
        notifications.accept(emptyList())
        assertEquals(listOf(1L), notifications.accept(listOf(item(1))).map { it.id })
        notifications.reset()
        assertEquals(emptyList<AppNotification>(), notifications.accept(listOf(item(100))))
        assertEquals(listOf(101L), notifications.accept(listOf(item(101), item(100))).map { it.id })
    }
}
