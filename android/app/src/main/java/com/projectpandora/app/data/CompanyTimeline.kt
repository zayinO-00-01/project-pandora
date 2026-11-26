package com.projectpandora.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal data class CompanyTimelineItem(
    val task: DispatchTask,
    val startMinute: Int,
    val endMinute: Int,
    val lane: Int=0,
    val laneCount: Int=1
) {
    val layoutEnd: Int get()=maxOf(endMinute,startMinute+20).coerceAtMost(1440)
}

internal fun companyTimeline(date: LocalDate,tasks: List<DispatchTask>): List<CompanyTimelineItem> {
    val zone=TaskDispatchValidation.zone
    val dayStart=date.atStartOfDay(zone).toInstant()
    val dayEnd=date.plusDays(1).atStartOfDay(zone).toInstant()
    val timed=tasks.mapNotNull {task->
        if(!taskIsApproved(task) || task.deleted)return@mapNotNull null
        val due=task.dueAt?.let {runCatching {Instant.parse(it)}.getOrNull()} ?: return@mapNotNull null
        val rawStart=task.startAt?.let {runCatching {Instant.parse(it)}.getOrNull()}
        val start=rawStart?.takeIf {it<=due} ?: due
        if(start>=dayEnd || due<dayStart || (start<dayStart && due==dayStart))return@mapNotNull null
        val from=ChronoUnit.MINUTES.between(dayStart,maxOf(start,dayStart)).toInt().coerceIn(0,1439)
        val until=ChronoUnit.MINUTES.between(dayStart,minOf(due,dayEnd)).toInt().coerceIn(from+1,1440)
        CompanyTimelineItem(task,from,until)
    }.sortedWith(compareBy<CompanyTimelineItem> {it.startMinute}.thenBy {it.endMinute}.thenBy {it.task.id})
    val result=mutableListOf<CompanyTimelineItem>()
    val group=mutableListOf<CompanyTimelineItem>()
    val laneEnds=mutableListOf<Int>()
    fun flush() {
        result+=group.map {it.copy(laneCount=laneEnds.size)}
        group.clear();laneEnds.clear()
    }
    for(item in timed) {
        if(laneEnds.isNotEmpty() && laneEnds.all {it<=item.startMinute})flush()
        val lane=laneEnds.indexOfFirst {it<=item.startMinute}.takeIf {it>=0} ?: laneEnds.size
        if(lane==laneEnds.size)laneEnds.add(item.layoutEnd) else laneEnds[lane]=item.layoutEnd
        group.add(item.copy(lane=lane))
    }
    if(group.isNotEmpty())flush()
    return result
}
