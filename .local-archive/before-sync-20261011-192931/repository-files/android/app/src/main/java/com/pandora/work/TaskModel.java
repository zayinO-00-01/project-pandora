package com.pandora.work;

final class TaskModel {
    String title;
    String meta;
    String status;
    int progress;

    TaskModel(String title, String meta, String status, int progress) {
        this.title = title;
        this.meta = meta;
        this.status = status;
        this.progress = progress;
    }
}
