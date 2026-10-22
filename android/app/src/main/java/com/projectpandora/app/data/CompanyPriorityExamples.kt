package com.projectpandora.app.data

/** Explicit local design samples, never substituted for published server data. */
object CompanyPriorityExamples {
    val items=listOf(
        "完成本月关键交付" to "完成核心模块联调与验收，核对交付清单，及时同步阻塞问题。",
        "完善工作日报闭环" to "统一日报补录、当日修改与历史查阅流程，组织一次完整场景验收。",
        "推进年度重点项目里程碑与跨部门协同" to "明确阶段目标、责任人及交付节点，提前协调跨部门依赖与资源。",
        "建立客户问题闭环" to "汇总客户反馈，明确问题负责人，跟进解决结果并完成回访。",
        "提升版本发布质量" to "覆盖核心业务回归、异常恢复和权限边界，发布前确认验收结果。",
        "优化部门资源安排" to "结合项目优先级分配人力，识别资源冲突并制定调整方案。",
        "沉淀团队工作规范" to "统一任务派发、进度反馈和会议纪要模板，减少重复沟通。",
        "完善数据安全检查" to "核对账号权限、敏感信息展示与历史记录访问，形成整改清单。",
        "准备季度经营复盘" to "汇总重点项目成果与风险，明确下一阶段的目标和行动计划。",
        "组织阶段成果演示" to "准备业务场景、演示账号和验收材料，展示任务到日报的工作闭环。"
    ).mapIndexed {index,(title,detail)->PanelItem(-101L-index,title,index,null,detail)}
}
