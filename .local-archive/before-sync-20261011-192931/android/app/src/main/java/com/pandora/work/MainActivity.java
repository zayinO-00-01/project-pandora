package com.pandora.work;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageView;
import android.app.Dialog;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Calendar;

public class MainActivity extends Activity {
    // Material You tokens from the local adaptive-ui design skill.
    static final int BG = DesignTokens.BACKGROUND, INK = DesignTokens.TEXT_PRIMARY, MUTED = DesignTokens.TEXT_SECONDARY;
    static final int PRIMARY = DesignTokens.PRIMARY, PRIMARY_CONTAINER = DesignTokens.PRIMARY_SOFT;
    static final int SURFACE = DesignTokens.SURFACE, SURFACE_CONTAINER = DesignTokens.SURFACE_MUTED;
    static final int OUTLINE = DesignTokens.BORDER, GREEN = DesignTokens.SUCCESS, ORANGE = DesignTokens.WARNING;
    static final int BLUE = PRIMARY;
    LinearLayout root, content;
    AppScreen current;
    final ArrayDeque<AppScreen> history = new ArrayDeque<>();
    final List<TaskModel> tasks = new ArrayList<>();
    SharedPreferences store;
    final PandoraApi api = new PandoraApi();
    String selectedView = "日";
    String selectedTaskFilter = "全部";
    boolean loggedIn;
    boolean onboardingShown;
    Calendar selectedDate = Calendar.getInstance();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = getSharedPreferences("pandora", MODE_PRIVATE);
        loggedIn = store.getBoolean("loggedIn", false);
        onboardingShown = false;
        // ponytail: local demo data keeps the APK runnable; replace with repository/API when backend is available.
        tasks.addAll(TaskRepository.demoTasks());
        showScreen(loggedIn ? AppScreen.HOME : null, false);
    }

    int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    TextView text(String value, float size, int color) { return UiComponents.text(this, value, size, color); }
    GradientDrawable bg(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    LinearLayout box(int color, int pad) { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); v.setPadding(dp(pad), dp(pad), dp(pad), dp(pad)); GradientDrawable d = bg(color, 8); d.setStroke(dp(1), OUTLINE); v.setBackground(d); return v; }
    // Legacy name kept for call-site compatibility; enterprise surfaces are opaque by default.
    LinearLayout surfaceBox(int pad) { return box(SURFACE, pad); }
    void add(View view, int height, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.topMargin = dp(top); view.setMinimumHeight(dp(height)); content.addView(view, p); }
    void inside(LinearLayout parent, View child) { parent.addView(child, new LinearLayout.LayoutParams(-1, -2)); }
    void insideWrap(LinearLayout parent, View child) { parent.addView(child, new LinearLayout.LayoutParams(-2, -2)); }
    void title(TextView v) { v.setTypeface(null, 1); }
    void toast(String message) { android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show(); }

    void showScreen(AppScreen screen, boolean push) {
        if (screen == null) { showLogin(); return; }
        if (push && current != null) history.push(current);
        current = screen;
        render();
    }
    void open(AppScreen screen) { showScreen(screen, true); }
    @Override public void onBackPressed() { if (!history.isEmpty()) showScreen(history.pop(), false); else if (current != AppScreen.HOME) showScreen(AppScreen.HOME, false); else super.onBackPressed(); }

    void showLogin() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setGravity(Gravity.CENTER_VERTICAL); root.setPadding(dp(28), dp(28), dp(28), dp(28)); root.setBackgroundColor(BG); prepareWindow(root);
        TextView brand = text("潘多拉", 30, INK); title(brand); root.addView(brand); root.addView(text("智能掌上工作系统", 14, MUTED));
        TextView heading = text("登录你的工作台", 25, INK); title(heading); addLogin(heading, 48);
        EditText account = new EditText(this); account.setHint("手机号或邮箱"); addLogin(account, 16);
        EditText password = new EditText(this); password.setHint("登录密码"); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); addLogin(password, 10);
        Button login = button("登录", BLUE); login.setOnClickListener(v -> { String username = account.getText().toString().trim(); String passwordValue = password.getText().toString(); if (api.isConfigured()) { login.setEnabled(false); api.login(username, passwordValue, new PandoraApi.Callback<PandoraApi.Session>() { public void success(PandoraApi.Session session) { login.setEnabled(true); loggedIn = true; store.edit().putBoolean("loggedIn", true).putString("role", session.role).putString("displayName", session.displayName).apply(); showScreen(AppScreen.HOME, false); } public void failure(int status, String message) { login.setEnabled(true); toast(status == 401 ? "账号或密码错误" : "登录服务暂不可用"); } }); } else { loggedIn = true; store.edit().putBoolean("loggedIn", true).apply(); showScreen(AppScreen.HOME, false); } }); addLogin(login, 24);
        root.addView(text("登录即表示你同意系统使用规范", 12, MUTED)); setContentView(root);
    }
    void addLogin(View view, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, view instanceof EditText ? dp(58) : view instanceof Button ? dp(54) : -2); p.topMargin = dp(top); root.addView(view, p); }
    Button button(String label, int color) {
        int foreground = color == PRIMARY ? Color.WHITE : (color == BG || color == SURFACE ? PRIMARY : INK);
        return UiComponents.button(this, label, color, foreground, 48);
    }
    Button filterButton(String label) { return UiComponents.filterButton(this, label); }
    Button iconButton(String label) { Button b = UiComponents.button(this, label, Color.TRANSPARENT, INK, 40); b.setTextSize(24); b.setPadding(0, 0, 0, 0); return b; }
    TextView pill(String label, int color, int foreground) {
        TextView v = text(label, 12, foreground); v.setGravity(Gravity.CENTER); v.setMinHeight(dp(32)); v.setSingleLine(true); v.setMaxLines(1); v.setEllipsize(android.text.TextUtils.TruncateAt.END);
        v.setPadding(dp(10), dp(4), dp(10), dp(4)); v.setBackground(bg(color, 8)); return v;
    }
    EditText filledField(String hint) {
        EditText e = field(hint); e.setTextSize(15); e.setTextColor(INK); e.setHintTextColor(MUTED);
        e.setSingleLine(false); e.setPadding(dp(16), dp(10), dp(16), dp(10)); e.setBackground(bg(SURFACE_CONTAINER, 16)); return e;
    }
    LinearLayout row() { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER_VERTICAL); return r; }
    void rowText(LinearLayout parent, TextView child) { parent.addView(child, new LinearLayout.LayoutParams(0, -2, 1)); }

    void render() {
        base(toolbarTitle());
        switch (current) {
            case HOME: home(); break; case WORKBENCH: workbench(); break; case VIEW: view(); break; case JOURNAL: journal(); break; case ANALYTICS: analytics(); break; case PROFILE: profile(); break;
            case TASKS: tasks(); break; case TASK_DETAIL: taskDetail(); break; case TASK_CREATE: taskCreate(); break; case NOTICE: notice(); break; case FOCUS: focus(); break;
            case NOTIFICATIONS: notifications(); break; case SETTINGS: settings(); break; case AI_ASSISTANT: aiAssistant(); break;
        }
        if (current == AppScreen.HOME && !store.getBoolean("onboardingDone", false) && !onboardingShown) {
            onboardingShown = true; content.postDelayed(this::showOnboarding, 240);
        }
    }
    String toolbarTitle() {
        switch (current) {
            case HOME: return "首页"; case WORKBENCH: return "工作台"; case VIEW: return "日历"; case JOURNAL: return "今日工作日志"; case ANALYTICS: return "基础分析"; case PROFILE: return "我的";
            case TASKS: return "任务管理"; case TASK_DETAIL: return "任务详情"; case TASK_CREATE: return "创建任务"; case NOTICE: return "公司重要事项"; case FOCUS: return "个人重要事项";
            case NOTIFICATIONS: return "通知提醒"; case AI_ASSISTANT: return "AI 工作摘要"; default: return "系统设置";
        }
    }
    void base(String pageTitle) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); prepareWindow(root);
        boolean compactPage = current == AppScreen.HOME || current == AppScreen.VIEW;
        if (!compactPage) {
            LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(16), dp(12), dp(16), dp(4));
            if (!history.isEmpty()) { Button back = iconButton("‹"); back.setOnClickListener(v -> onBackPressed()); bar.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44))); }
            TextView t = text(pageTitle, 20, INK); title(t); bar.addView(t, new LinearLayout.LayoutParams(0, dp(48), 1));
            root.addView(bar);
        }
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(20), dp(4), dp(20), dp(16));
        if (compactPage) { content.setPadding(dp(20), dp(10), dp(20), dp(18)); content.setBackgroundColor(Color.TRANSPARENT); }
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false); scroll.setPadding(0, compactPage ? dp(2) : 0, 0, 0); scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (current == AppScreen.HOME || current == AppScreen.WORKBENCH || current == AppScreen.TASKS || current == AppScreen.VIEW || current == AppScreen.PROFILE) nav();
        setContentView(root);
    }
    void prepareWindow(LinearLayout host) {
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        host.setFitsSystemWindows(true);
        host.setPadding(host.getPaddingLeft(), host.getPaddingTop() + dp(8), host.getPaddingRight(), host.getPaddingBottom() + dp(4));
    }
    void nav() {
        LinearLayout n = new LinearLayout(this); n.setGravity(Gravity.CENTER); n.setPadding(0, dp(4), 0, dp(5)); n.setBackgroundColor(SURFACE);
        String[] labels = {"首页", "工作台", "任务", "日历", "我的"}; AppScreen[] screens = {AppScreen.HOME, AppScreen.WORKBENCH, AppScreen.TASKS, AppScreen.VIEW, AppScreen.PROFILE};
        for (int i = 0; i < labels.length; i++) {
            final AppScreen target = screens[i]; boolean selected = current == target;
            LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER); item.setPadding(0, 0, 0, dp(3));
            int[] icons = {com.pandora.work.R.drawable.ic_nav_home, com.pandora.work.R.drawable.ic_nav_workbench, com.pandora.work.R.drawable.ic_nav_tasks, com.pandora.work.R.drawable.ic_nav_calendar, com.pandora.work.R.drawable.ic_nav_profile}; ImageView icon = new ImageView(this); icon.setImageResource(icons[i]); if (icon.getDrawable() != null) icon.getDrawable().setTint(selected ? PRIMARY : MUTED); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); item.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22))); TextView label = text(labels[i], 11, selected ? PRIMARY : MUTED); label.setGravity(Gravity.CENTER); label.setSingleLine(true); label.setPadding(0, 0, 0, 0); item.addView(label, new LinearLayout.LayoutParams(-1, 0, 1));
            View indicator = new View(this); indicator.setBackground(bg(selected ? PRIMARY : Color.TRANSPARENT, 3)); LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(28), dp(3)); item.addView(indicator, ip);
            item.setOnClickListener(v -> { history.clear(); showScreen(target, false); }); n.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, dp(64)); np.topMargin = dp(1); root.addView(n, np);
    }

    void home() {
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(dp(16), dp(7), dp(10), dp(7)); header.setBackground(bg(SURFACE, 14));
        LinearLayout identity = new LinearLayout(this); identity.setOrientation(LinearLayout.VERTICAL); TextView name = text("潘多拉", 18, INK); title(name); identity.addView(name, new LinearLayout.LayoutParams(-1, -2)); TextView workspace = text("工作台", 11, MUTED); workspace.setMaxLines(1); workspace.setEllipsize(android.text.TextUtils.TruncateAt.END); LinearLayout.LayoutParams wsp = new LinearLayout.LayoutParams(-1, -2); wsp.topMargin = dp(2); identity.addView(workspace, wsp); header.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        TextView avatar = text("祝", 13, PRIMARY); avatar.setGravity(Gravity.CENTER); avatar.setPadding(0, 0, 0, 0); avatar.setBackground(bg(PRIMARY_CONTAINER, 18)); header.addView(avatar, new LinearLayout.LayoutParams(dp(32), dp(32))); add(header, 56, 0);

        LinearLayout greeting = row(); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView hello = text("早上好，祝", 24, INK); title(hello); copy.addView(hello, new LinearLayout.LayoutParams(-1, -2)); copy.addView(text("把今天最重要的工作推进一点点", 13, MUTED), new LinearLayout.LayoutParams(-1, -2)); greeting.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); Button journalAction = button("去记录", PRIMARY); journalAction.setTextSize(12); journalAction.setOnClickListener(v -> open(AppScreen.JOURNAL)); greeting.addView(journalAction, new LinearLayout.LayoutParams(-2, -2)); add(greeting, 54, 18);
        LinearLayout stats = row(); homeStat(stats, "03", "进行中任务", Color.rgb(232, 242, 255), PRIMARY); homeStat(stats, "02", "今日到期", Color.rgb(242, 240, 232), ORANGE); homeStat(stats, "80%", "日志完成度", Color.rgb(232, 245, 240), GREEN); add(stats, 78, 14);
        LinearLayout notice = new LinearLayout(this); notice.setOrientation(LinearLayout.VERTICAL); notice.setPadding(dp(16), dp(12), dp(16), dp(10)); notice.setBackground(bg(Color.rgb(31, 46, 77), 18)); inside(notice, text("公司重要事项", 11, Color.rgb(173, 196, 255))); TextView nt = text("本周产品评审周四 14:00 开始", 16, Color.WHITE); title(nt); inside(notice, nt); inside(notice, text("产品组 · 今天更新", 11, Color.rgb(186, 199, 219))); notice.setOnClickListener(v -> open(AppScreen.NOTICE)); add(notice, 94, 16);
        homeSectionHeader("今日任务", "查看全部", () -> open(AppScreen.TASKS)); homeTaskRow(tasks.get(0)); homeTaskRow(tasks.get(1));
        LinearLayout journal = new LinearLayout(this); journal.setOrientation(LinearLayout.VERTICAL); journal.setPadding(dp(16), dp(12), dp(16), dp(12)); journal.setBackground(bg(Color.rgb(240, 245, 255), 18)); inside(journal, text("今日工作日志", 11, PRIMARY)); TextView jt = text("还没有记录今天的日志", 16, INK); title(jt); inside(journal, jt); inside(journal, text("记录进展、问题和下一步计划", 12, MUTED)); journal.setOnClickListener(v -> open(AppScreen.JOURNAL)); add(journal, 112, 16);
    }
    void workbench() {
        LinearLayout head = row(); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); TextView workTitle = text("工作台", 20, INK); title(workTitle); copy.addView(workTitle, new LinearLayout.LayoutParams(-1, dp(30))); copy.addView(text("只展示当前需要你处理的工作", 13, MUTED), new LinearLayout.LayoutParams(-1, dp(22))); head.addView(copy, new LinearLayout.LayoutParams(0, -2, 1)); Button create = button("+ 新建任务", PRIMARY); create.setOnClickListener(v -> open(AppScreen.TASK_CREATE)); head.addView(create, new LinearLayout.LayoutParams(-2, dp(44))); add(head, 54, 0);
        workbenchSummary();
        LinearLayout toolbar = row(); EditText search = filledField("搜索我的任务"); search.setSingleLine(true); search.setMaxLines(1); toolbar.addView(search, new LinearLayout.LayoutParams(0, dp(40), 1)); Button filter = filterButton("筛选"); filter.setOnClickListener(v -> toast("筛选：进行中、待开始、今日到期")); LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(-2, -2); filterParams.leftMargin = dp(8); toolbar.addView(filter, filterParams); add(toolbar, 44, 16);
        sectionHeader("我的任务", "查看全部", () -> open(AppScreen.TASKS)); for (int i = 0; i < 3; i++) denseTaskRow(tasks.get(i));
        sectionHeader("今日安排", "打开日历", () -> open(AppScreen.VIEW)); agendaRow("14:00", "产品评审会议", "会议室 A · 重要");
        sectionHeader("工作日志", "去记录", () -> open(AppScreen.JOURNAL)); LinearLayout log = new LinearLayout(this); log.setOrientation(LinearLayout.VERTICAL); log.setPadding(dp(16), dp(12), dp(16), dp(12)); log.setBackground(bg(SURFACE, 8)); inside(log, text("今天还没有提交日志", 15, INK)); inside(log, text("记录进展、问题和下一步计划", 12, MUTED)); log.setOnClickListener(v -> open(AppScreen.JOURNAL)); add(log, 72, 8);
    }
    void workbenchSummary() { GridLayout grid = new GridLayout(this); grid.setColumnCount(2); grid.setUseDefaultMargins(false); metricTile(grid, "团队任务", "24", "进行中 12", PRIMARY, 0, 0); metricTile(grid, "今日日志", "18 / 20", "完成度 90%", GREEN, 0, 1); metricTile(grid, "逾期任务", "03", "需要跟进", ORANGE, 1, 0); metricTile(grid, "待处理提醒", "07", "新通知 2", PRIMARY, 1, 1); add(grid, 184, 16); }
    void metricTile(GridLayout grid, String label, String value, String meta, int color, int rowIndex, int columnIndex) { LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setPadding(dp(12), dp(9), dp(12), dp(8)); tile.setBackground(bg(SURFACE, 8)); TextView labelView = text(label, 12, MUTED); labelView.setMaxLines(1); labelView.setEllipsize(android.text.TextUtils.TruncateAt.END); tile.addView(labelView, new LinearLayout.LayoutParams(-1, -2)); TextView valueView = text(value, 24, color); title(valueView); valueView.setMaxLines(1); valueView.setEllipsize(android.text.TextUtils.TruncateAt.END); LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2); vp.topMargin = dp(4); tile.addView(valueView, vp); TextView metaView = text(meta, 11, MUTED); metaView.setMaxLines(1); metaView.setEllipsize(android.text.TextUtils.TruncateAt.END); LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(-1, -2); mp.topMargin = dp(2); tile.addView(metaView, mp); GridLayout.LayoutParams p = new GridLayout.LayoutParams(GridLayout.spec(rowIndex, 1, 1f), GridLayout.spec(columnIndex, 1, 1f)); p.width = 0; p.setMargins(columnIndex == 0 ? 0 : dp(6), rowIndex == 0 ? 0 : dp(6), columnIndex == 1 ? 0 : dp(6), 0); grid.addView(tile, p); }
    void denseTaskRow(TaskModel task) { LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(dp(12), dp(7), dp(12), dp(7)); View marker = new View(this); marker.setBackground(bg(task.status.equals("待开始") ? ORANGE : PRIMARY, 3)); r.addView(marker, new LinearLayout.LayoutParams(dp(4), dp(36))); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(12), 0, dp(8), 0); TextView name = text(task.title, 14, INK); title(name); name.setMaxLines(1); name.setEllipsize(android.text.TextUtils.TruncateAt.END); copy.addView(name, new LinearLayout.LayoutParams(-1, dp(23))); TextView meta = text(task.status + " · " + task.meta, 12, MUTED); meta.setMaxLines(1); meta.setEllipsize(android.text.TextUtils.TruncateAt.END); copy.addView(meta, new LinearLayout.LayoutParams(-1, dp(20))); r.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1)); TextView progress = text(task.status.equals("待开始") ? "待开始" : task.progress + "%", 12, task.status.equals("待开始") ? ORANGE : PRIMARY); progress.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); r.addView(progress, new LinearLayout.LayoutParams(dp(58), dp(40))); r.setOnClickListener(v -> open(AppScreen.TASK_DETAIL)); content.addView(r, new LinearLayout.LayoutParams(-1, dp(64))); View divider = new View(this); divider.setBackgroundColor(OUTLINE); content.addView(divider, new LinearLayout.LayoutParams(-1, dp(1))); }
    void homeStat(LinearLayout parent, String number, String label, int color, int numberColor) { LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setPadding(dp(14), dp(10), dp(8), dp(8)); tile.setBackground(bg(color, 16)); TextView n = text(number, 24, numberColor); title(n); tile.addView(n, new LinearLayout.LayoutParams(-1, dp(34))); tile.addView(text(label, 12, MUTED), new LinearLayout.LayoutParams(-1, dp(20))); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(78), 1); if (parent.getChildCount() > 0) p.leftMargin = dp(13); parent.addView(tile, p); }
    void homeSectionHeader(String left, String right, final Runnable action) { LinearLayout h = row(); TextView l = text(left, 18, INK); title(l); h.addView(l, new LinearLayout.LayoutParams(0, -2, 1)); TextView r = text(right, 12, PRIMARY); r.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); r.setSingleLine(true); r.setOnClickListener(v -> { if (action != null) action.run(); }); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2); rp.leftMargin = dp(8); h.addView(r, rp); add(h, 32, 16); }
    void homeTaskRow(TaskModel task) { LinearLayout card = new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(16), dp(8), dp(14), dp(8)); card.setBackground(bg(SURFACE, 14)); View bar = new View(this); bar.setBackground(bg(task.status.equals("待开始") ? ORANGE : PRIMARY, 4)); card.addView(bar, new LinearLayout.LayoutParams(dp(8), dp(28))); LinearLayout copy = new LinearLayout(this); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(dp(12), 0, 0, 0); TextView t = text(task.title, 14, INK); title(t); copy.addView(t, new LinearLayout.LayoutParams(-1, dp(24))); copy.addView(text(task.status + " · " + task.meta, 11, MUTED), new LinearLayout.LayoutParams(-1, dp(20))); card.addView(copy, new LinearLayout.LayoutParams(0, dp(48), 1)); TextView p = text(task.status.equals("待开始") ? "待开始" : task.progress + "%", 13, task.status.equals("待开始") ? ORANGE : PRIMARY); p.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); card.addView(p, new LinearLayout.LayoutParams(dp(58), dp(42))); card.setOnClickListener(v -> open(AppScreen.TASK_DETAIL)); add(card, 64, 8); }
    void sectionHeader(String left, String right, final Runnable action) { LinearLayout h = new LinearLayout(this); h.setGravity(Gravity.CENTER_VERTICAL); TextView l = text(left, 18, INK); title(l); h.addView(l, new LinearLayout.LayoutParams(0, -2, 1)); if (!right.isEmpty()) { TextView r = text(right, 12, BLUE); r.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT); r.setSingleLine(true); r.setOnClickListener(v -> { if (action != null) action.run(); }); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-2, -2); rp.leftMargin = dp(8); h.addView(r, rp); } add(h, 34, 18); }
    void quickRow(String label, String meta, AppScreen screen) { LinearLayout row = box(Color.WHITE, 14); TextView a = text(label, 14, INK); title(a); inside(row, a); inside(row, text(meta, 12, MUTED)); row.setOnClickListener(v -> open(screen)); add(row, 68, 8); }
    void shortcutRow() { LinearLayout shortcuts = new LinearLayout(this); shortcuts.setWeightSum(2); shortcut(shortcuts, "+", "新建任务", AppScreen.TASK_CREATE); shortcut(shortcuts, "日", "查看日历", AppScreen.VIEW); add(shortcuts, 72, 8); }
    void shortcut(LinearLayout parent, String icon, String label, AppScreen screen) { LinearLayout item = box(SURFACE, 10); TextView glyph = text(icon, 13, PRIMARY); glyph.setGravity(Gravity.CENTER); glyph.setBackground(bg(PRIMARY_CONTAINER, 8)); inside(item, glyph); TextView name = text(label, 12, INK); name.setGravity(Gravity.CENTER); inside(item, name); item.setOnClickListener(v -> open(screen)); LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(82), 1); p.rightMargin = dp(6); parent.addView(item, p); }
    void taskRow(TaskModel task) { LinearLayout card = box(SURFACE, 14); LinearLayout top = row(); TextView t = text(task.title, 15, INK); title(t); t.setMaxLines(1); t.setEllipsize(android.text.TextUtils.TruncateAt.END); rowText(top, t); insideWrap(top, pill(task.status, task.status.equals("已完成") ? Color.rgb(220, 237, 225) : PRIMARY_CONTAINER, task.status.equals("已完成") ? GREEN : PRIMARY)); inside(card, top); LinearLayout meta = row(); rowText(meta, text(task.meta, 12, MUTED)); TextView progress = text(task.progress + "%", 12, task.status.equals("已完成") ? GREEN : PRIMARY); progress.setGravity(Gravity.RIGHT); meta.addView(progress, new LinearLayout.LayoutParams(dp(48), -2)); inside(card, meta); card.setOnClickListener(v -> open(AppScreen.TASK_DETAIL)); add(card, 76, 8); }

    void tasks() { Button create = button("+ 创建任务", PRIMARY); create.setOnClickListener(v -> open(AppScreen.TASK_CREATE)); add(create, 54, 8); EditText search = filledField("搜索任务、负责人或状态"); add(search, 56, 12); LinearLayout filters = row(); for (String label : new String[]{"全部", "进行中", "待开始", "已完成"}) { TextView chip = pill(label, selectedTaskFilter.equals(label) ? PRIMARY : SURFACE_CONTAINER, selectedTaskFilter.equals(label) ? Color.WHITE : MUTED); chip.setOnClickListener(v -> { selectedTaskFilter = label; render(); }); filters.addView(chip, new LinearLayout.LayoutParams(0, dp(38), 1)); } add(filters, 42, 10); int shown = 0; for (TaskModel task : tasks) if (selectedTaskFilter.equals("全部") || task.status.equals(selectedTaskFilter)) shown++; sectionHeader("任务列表", shown + " 项", null); for (TaskModel task : tasks) if (selectedTaskFilter.equals("全部") || task.status.equals(selectedTaskFilter)) taskRow(task); }
    void taskDetail() { TextView t = text(tasks.get(0).title, 21, INK); title(t); add(t, 34, 12); add(text("负责人：祝 · 截止：今天 18:00", 12, MUTED), 28, 0); LinearLayout info = box(SURFACE, 12); inside(info, text("任务说明", 12, MUTED)); inside(info, text("整理移动端首页信息层级，并完成关键入口的优先级确认。", 14, INK)); add(info, 98, 12); LinearLayout progressLabel = row(); rowText(progressLabel, text("当前进度", 13, INK)); TextView percent = text("65%", 13, PRIMARY); percent.setGravity(Gravity.RIGHT); progressLabel.addView(percent, new LinearLayout.LayoutParams(dp(48), -2)); inside(content, progressLabel); ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); p.setProgress(65); add(p, 18, 4); EditText note = filledField("补充进度、风险或下一步"); note.setGravity(Gravity.TOP); add(note, 100, 12); Button save = button("保存进度", PRIMARY); save.setOnClickListener(v -> toast("进度已保存")); add(save, 54, 12); Button done = button("标记为已完成", Color.rgb(227, 246, 237)); done.setTextColor(GREEN); done.setOnClickListener(v -> { tasks.get(0).progress = 100; tasks.get(0).status = "已完成"; toast("任务已完成"); }); add(done, 54, 8); quickRow("关联日志", "查看本任务关联的工作日志", AppScreen.JOURNAL); }
    void taskCreate() { add(text("创建任务", 20, INK), 30, 8); add(text("填写必要信息后，任务会出现在任务列表和日历中。", 13, MUTED), 38, 0); EditText name = filledField("任务名称"); add(name, 60, 12); EditText owner = filledField("责任人"); add(owner, 60, 10); EditText priority = filledField("优先级：普通 / 重要 / 紧急"); add(priority, 60, 10); EditText due = filledField("截止时间"); add(due, 60, 10); EditText detail = filledField("任务详情"); detail.setGravity(Gravity.TOP); add(detail, 120, 10); LinearLayout actions = row(); Button cancel = button("取消", SURFACE); cancel.setTextColor(INK); cancel.setOnClickListener(v -> onBackPressed()); actions.addView(cancel, new LinearLayout.LayoutParams(0, -2, 1)); Button save = button("创建任务", PRIMARY); save.setOnClickListener(v -> { tasks.add(new TaskModel(name.getText().toString().isEmpty() ? "新建任务" : name.getText().toString(), "刚刚创建 · 负责人：" + owner.getText(), "待开始", 0)); toast("任务已创建"); onBackPressed(); }); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, -2, 1); sp.leftMargin = dp(8); actions.addView(save, sp); add(actions, 48, 18); }
    EditText field(String hint) { EditText e = new EditText(this); e.setHint(hint); return e; }

    void view() { TextView headingTitle = text("工作视图", 21, INK); title(headingTitle); add(headingTitle, 30, 0); LinearLayout monthBar = row(); TextView month = text(dateLabel(), 13, MUTED); rowText(monthBar, month); Button previous = iconButton("‹"); previous.setTextSize(22); previous.setOnClickListener(v -> { selectedDate.add(Calendar.MONTH, -1); render(); }); Button next = iconButton("›"); next.setTextSize(22); next.setOnClickListener(v -> { selectedDate.add(Calendar.MONTH, 1); render(); }); monthBar.addView(previous, new LinearLayout.LayoutParams(dp(40), dp(40))); monthBar.addView(next, new LinearLayout.LayoutParams(dp(40), dp(40))); add(monthBar, 40, 4); add(calendarGrid(), 0, 8); sectionHeader("今日安排", "查看任务", () -> open(AppScreen.TASKS)); agendaRow("09:30", "产品评审准备", "准备材料 · 进行中"); agendaRow("14:00", "产品评审会议", "会议室 A · 重要"); agendaRow("18:00", "首页信息架构截止", "任务 · 祝"); agendaRow("", "整理 Sprint 0 需求", "任务 · 待开始 · 今天 20:00"); }
    String dateLabel() { return selectedDate.get(Calendar.YEAR) + " 年 " + (selectedDate.get(Calendar.MONTH) + 1) + " 月"; }
    View calendarGrid() { LinearLayout wrapper = new LinearLayout(this); wrapper.setOrientation(LinearLayout.VERTICAL); wrapper.setPadding(dp(4), dp(4), dp(4), dp(4)); wrapper.setBackground(bg(SURFACE, 8)); String[] days = {"一", "二", "三", "四", "五", "六", "日"}; LinearLayout weekdays = new LinearLayout(this); for (String day : days) { TextView v = text(day, 11, MUTED); v.setGravity(Gravity.CENTER); v.setPadding(0, 0, 0, 0); weekdays.addView(v, new LinearLayout.LayoutParams(0, dp(24), 1)); } wrapper.addView(weekdays); Calendar start = (Calendar) selectedDate.clone(); start.set(Calendar.DAY_OF_MONTH, 1); int first = start.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY; first = (first + 7) % 7; first = (first + 6) % 7; int max = start.getActualMaximum(Calendar.DAY_OF_MONTH); int count = ((first + max + 6) / 7) * 7; for (int offset = 0; offset < count; offset += 7) { int[] values = new int[7]; for (int i = 0; i < 7; i++) values[i] = offset + i < first || offset + i >= first + max ? 0 : offset + i - first + 1; wrapper.addView(calendarRow(values, 7), new LinearLayout.LayoutParams(-1, dp(34))); } return wrapper; }
    LinearLayout calendarRow(int[] values, int count) { LinearLayout row = new LinearLayout(this); for (int value : values) { TextView cell = text(value == 0 ? "" : String.valueOf(value), 12, value == 0 ? MUTED : INK); cell.setGravity(Gravity.CENTER); cell.setPadding(0, 0, 0, 0); cell.setMaxLines(1); if (value != 0) cell.setOnClickListener(v -> { selectedDate.set(Calendar.DAY_OF_MONTH, value); render(); }); if (value != 0 && value == selectedDate.get(Calendar.DAY_OF_MONTH)) { cell.setTextColor(Color.WHITE); cell.setBackground(bg(PRIMARY, 7)); } else if (value != 0 && value == Calendar.getInstance().get(Calendar.DAY_OF_MONTH) && selectedDate.get(Calendar.MONTH) == Calendar.getInstance().get(Calendar.MONTH)) { cell.setTextColor(PRIMARY); cell.setBackground(bg(PRIMARY_CONTAINER, 7)); } row.addView(cell, new LinearLayout.LayoutParams(0, dp(30), 1)); } return row; }
    void agendaRow(String time, String label, String meta) { LinearLayout r = row(); TextView tm = text(time, 12, PRIMARY); tm.setGravity(Gravity.TOP); r.addView(tm, new LinearLayout.LayoutParams(dp(58), -2)); LinearLayout c = surfaceBox(12); TextView name = text(label, 14, INK); title(name); name.setMaxLines(1); name.setEllipsize(android.text.TextUtils.TruncateAt.END); TextView detail = text(meta, 12, MUTED); detail.setMaxLines(2); detail.setEllipsize(android.text.TextUtils.TruncateAt.END); inside(c, name); inside(c, detail); r.addView(c, new LinearLayout.LayoutParams(0, -2, 1)); add(r, 0, 8); }
    void journal() { add(text("草稿会自动保存在本机，提交后记录提交时间。", 13, MUTED), 36, 8); TextView date = text("2026 年 10 月 1 日 · 周四", 19, INK); title(date); add(date, 36, 10); for (String f : new String[]{"工作内容", "进展与结果", "问题与风险", "下一步计划"}) { add(text(f, 13, INK), 24, 12); EditText e = filledField("填写" + f); e.setGravity(Gravity.TOP); add(e, 104, 2); } quickRow("关联任务", "完成首页信息架构 · 可关联多个任务", AppScreen.TASKS); Button save = button("保存草稿", SURFACE_CONTAINER); save.setOnClickListener(v -> { store.edit().putBoolean("draft", true).apply(); toast("草稿已保存"); }); add(save, 54, 12); Button submit = button("提交日志", PRIMARY); submit.setOnClickListener(v -> toast("日志已提交，负责人可在授权范围内查看")); add(submit, 54, 8); }
    void analytics() { add(text("统计口径：当前用户授权范围内，时间范围为本周。", 13, MUTED), 36, 8); metric("任务完成率", "68%", BLUE); metric("任务状态", "进行中 12 · 待开始 8 · 已完成 16", GREEN); metric("逾期任务", "03", ORANGE); metric("日志提交情况", "18 / 20 · 提交率 90%", BLUE); quickRow("查看任务明细", "统计结果可回溯到任务列表", AppScreen.TASKS); quickRow("查看日志明细", "按日期和成员筛选日志", AppScreen.JOURNAL); }
    void metric(String label, String value, int color) { LinearLayout m = box(Color.WHITE, 16); inside(m, text(label, 12, MUTED)); TextView v = text(value, 21, color); title(v); inside(m, v); add(m, 104, 10); }
    void profile() { LinearLayout user = box(SURFACE, 12); TextView name = text("祝", 20, INK); title(name); inside(user, name); inside(user, text("部门负责人 · 产品研发部", 13, MUTED)); add(user, 78, 10); quickRow("通知提醒", "3 条未读 · 新任务和截止提醒", AppScreen.NOTIFICATIONS); quickRow("个人重要事项", "当前 2 / 10 条", AppScreen.FOCUS); quickRow("权限与数据范围", "可查看产品研发部成员数据", AppScreen.SETTINGS); quickRow("系统设置", "账号、退出和隐私说明", AppScreen.SETTINGS); Button logout = button("退出登录", SURFACE); logout.setTextColor(ORANGE); logout.setOnClickListener(v -> { loggedIn = false; store.edit().putBoolean("loggedIn", false).apply(); history.clear(); showScreen(null, false); }); add(logout, 54, 20); }
    void notice() { add(text("面向产品研发部成员展示的公司重要事项。原文只读，个人备注独立保存。", 13, MUTED), 42, 8); LinearLayout n = box(Color.WHITE, 16); TextView t = text("本周产品评审周四 14:00 开始", 18, INK); title(t); inside(n, t); inside(n, text("发布范围：产品研发部\n有效期：2026-10-01 至 2026-10-03\n发布人：公司负责人", 13, MUTED)); add(n, 150, 12); add(text("个人备注", 14, INK), 28, 14); EditText note = field("添加仅自己可见的备注"); note.setGravity(Gravity.TOP); add(note, 100, 4); Button save = button("保存备注", BLUE); save.setOnClickListener(v -> toast("个人备注已保存")); add(save, 54, 12); }
    void focus() { add(text("最多维护 10 条个人重要事项，可排序和移除。", 13, MUTED), 36, 8); for (String item : new String[]{"完成首页信息架构", "准备周四产品评审"}) { LinearLayout row = box(Color.WHITE, 14); TextView t = text(item, 14, INK); title(t); inside(row, t); inside(row, text("拖动排序 · 编辑 · 移除", 12, MUTED)); add(row, 70, 8); } Button add = button("+ 新增重要事项", BLUE); add.setOnClickListener(v -> toast("已打开新增事项表单")); this.add(add, 54, 16); }
    void notifications() { add(text("新任务、任务变更、临近截止和重要事项更新会出现在这里。", 13, MUTED), 42, 8); notification("新任务", "你收到任务：整理 Sprint 0 需求", "刚刚"); notification("截止提醒", "任务完成首页信息架构将在今天 18:00 截止", "10 分钟前"); notification("事项更新", "产品研发部发布了新的公司重要事项", "今天 09:30"); Button read = button("全部标记为已读", Color.WHITE); read.setTextColor(BLUE); read.setOnClickListener(v -> toast("通知已全部标记为已读")); add(read, 54, 16); }
    void notification(String type, String body, String time) { LinearLayout n = box(Color.WHITE, 14); TextView t = text(type, 12, BLUE); title(t); inside(n, t); inside(n, text(body, 14, INK)); inside(n, text(time, 11, MUTED)); add(n, 88, 8); }
    void settings() { add(text("当前角色：部门负责人\n数据范围：产品研发部及下属团队", 14, INK), 68, 8); quickRow("权限说明", "任务创建、任务跟踪、组织范围日志查看", AppScreen.PROFILE); quickRow("隐私说明", "日志只在授权范围内展示，AI 分析暂未启用", AppScreen.PROFILE); quickRow("审计记录", "关键发布、派发、状态变更会被记录", AppScreen.PROFILE); }
    void aiAssistant() { add(text("AI 仅提供辅助判断，工作数据仍以任务、日历和日志为准。", 13, MUTED), 36, 8); LinearLayout answer = box(SURFACE, 12); inside(answer, text("本周工作判断", 13, MUTED)); TextView title = text("先完成高优先级任务，再补齐日志记录", 20, INK); title(title); inside(answer, title); inside(answer, text("你有 3 个进行中任务，其中“完成首页信息架构”最接近截止时间。", 14, MUTED)); inside(answer, text("来源：任务列表 · 日历安排 · 工作日志", 11, MUTED)); add(answer, 150, 12); sectionHeader("建议动作", "", null); aiAction("查看首页信息架构", "打开任务详情并更新进度", AppScreen.TASK_DETAIL); aiAction("准备产品评审", "查看今天 14:00 的日程安排", AppScreen.VIEW); aiAction("记录今日进展", "把完成情况写入工作日志", AppScreen.JOURNAL); }
    void aiAction(String label, String meta, AppScreen screen) { LinearLayout item = box(SURFACE, 12); LinearLayout r = row(); TextView a = text(label, 14, INK); title(a); rowText(r, a); inside(r, pill("建议", PRIMARY_CONTAINER, PRIMARY)); inside(item, r); inside(item, text(meta, 12, MUTED)); item.setOnClickListener(v -> open(screen)); add(item, 70, 8); }

    void showOnboarding() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout card = box(Color.argb(248, 255, 251, 254), 22);
        GradientDrawable cardBg = bg(Color.argb(248, 255, 251, 254), 24); cardBg.setStroke(dp(1), Color.argb(170, 255, 255, 255)); card.setBackground(cardBg); card.setElevation(dp(10));
        TextView eyebrow = text("首次使用引导", 12, PRIMARY); title(eyebrow); inside(card, eyebrow);
        TextView stepIcon = text("01", 25, PRIMARY); stepIcon.setGravity(Gravity.CENTER); stepIcon.setBackground(bg(PRIMARY_CONTAINER, 18)); LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(58), dp(58)); iconParams.topMargin = dp(12); card.addView(stepIcon, iconParams);
        TextView heading = text("先从今日工作开始", 22, INK); title(heading); heading.setPadding(0, dp(18), 0, dp(4)); inside(card, heading);
        TextView body = text("潘多拉会把你今天最重要的任务、提醒和工作日志集中在首页，打开就知道下一步做什么。", 14, MUTED); inside(card, body);
        LinearLayout dots = row(); dots.setPadding(0, dp(18), 0, dp(8)); for (int i = 0; i < 4; i++) { TextView dot = text(i == 0 ? "●" : "○", 16, i == 0 ? PRIMARY : OUTLINE); dot.setGravity(Gravity.CENTER); dots.addView(dot, new LinearLayout.LayoutParams(dp(24), dp(24))); } inside(card, dots);
        LinearLayout actions = row(); TextView skip = pill("跳过引导", SURFACE_CONTAINER, MUTED); skip.setOnClickListener(v -> finishOnboarding(dialog)); rowText(actions, skip); Button next = button("下一步", PRIMARY); actions.addView(next, new LinearLayout.LayoutParams(dp(112), dp(48))); inside(card, actions);
        final String[] icons = {"01", "02", "03", "04"}; final String[] titles = {"先从今日工作开始", "任务会有清晰的优先级", "日志让进展可追踪", "提醒让重要事项不遗漏"}; final String[] bodies = {"潘多拉会把你今天最重要的任务、提醒和工作日志集中在首页，打开就知道下一步做什么。", "进入任务管理，可以创建任务、查看负责人、截止时间和当前进度。", "在工作日志中记录进展、风险和下一步计划，形成可回溯的工作记录。", "通知提醒会汇总新任务、状态变化、临近截止和公司重要事项。"};
        final int[] index = {0};
        next.setOnClickListener(v -> { index[0]++; if (index[0] >= titles.length) { finishOnboarding(dialog); return; } stepIcon.setText(icons[index[0]]); heading.setText(titles[index[0]]); body.setText(bodies[index[0]]); for (int i = 0; i < dots.getChildCount(); i++) ((TextView) dots.getChildAt(i)).setTextColor(i == index[0] ? PRIMARY : OUTLINE); next.setText(index[0] == titles.length - 1 ? "开始使用" : "下一步"); });
        dialog.setContentView(card); dialog.setCanceledOnTouchOutside(false); Window window = dialog.getWindow(); if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent); window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); WindowManager.LayoutParams lp = window.getAttributes(); lp.dimAmount = 0.38f; window.setAttributes(lp); window.setLayout(-1, -2); }
        dialog.show(); if (dialog.getWindow() != null) dialog.getWindow().setLayout(-1, -2);
    }
    void finishOnboarding(Dialog dialog) { store.edit().putBoolean("onboardingDone", true).apply(); dialog.dismiss(); toast("引导已完成，开始使用潘多拉吧"); }
}
