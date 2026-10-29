package com.projectpandora.app.data

import java.time.Instant
import java.time.ZoneId

/** Explicitly local examples, never written to a business account or server. */
object PreviewWorkspace {
    val labels=listOf(WorkLabel(-301,"潘多拉 · 产品","#B9DAD0"),WorkLabel(-302,"品牌运营","#F3CCA8"),WorkLabel(-303,"客户项目","#BCD3EF"))
    fun tasks(): List<DispatchTask> {
        val now=Instant.now().toString()
        val due=today().atTime(18,0).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString()
        return listOf("产品需求评审" to labels[0],"品牌活动准备" to labels[1],"客户交付确认" to labels[2]).mapIndexed {index,(title,label)->
            DispatchTask(-401L-index,title,"本地示例：查看品牌标签、任务底色与日历颜色的一致效果。","normal","todo",due,0,"准备中",-1,-1,"开发预览","开发预览",now,now,label,keywords=listOf(TaskKeyword(-501L-index,"需求",-1),TaskKeyword(-511L-index,"协作",-2)))
        }
    }
}
