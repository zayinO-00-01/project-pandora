package com.pandora.work;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class TaskRepository {
    private TaskRepository() {}

    static List<TaskModel> demoTasks() {
        return new ArrayList<>(Arrays.asList(
                new TaskModel("完成首页信息架构", "今天 18:00 · 祝", "进行中", 65),
                new TaskModel("整理 Sprint 0 需求", "今天 20:00 · 祝", "待开始", 0),
                new TaskModel("准备产品评审材料", "明天 12:00 · 陈晨", "进行中", 40),
                new TaskModel("补充接口错误状态", "昨天 17:00 · 王宁", "已完成", 100)
        ));
    }
}
