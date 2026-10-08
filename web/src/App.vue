<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { api, timeLabel, today } from './api';
import LogEditor from './LogEditor.vue';
const session = ref(null), username = ref('leader'), password = ref('demo1234');
const loginBusy = ref(false), loginError = ref(''), booting = ref(true);
const members = ref([]), area = ref('team'), memberId = ref(''), date = ref('');
const logs = ref([]), selectedId = ref(null), loading = ref(false), loadError = ref('');
const savedMessage = ref(''), saving = ref(false), refreshedAt = ref(null), recovery = ref(null);
const editorRecovery = computed(() => recovery.value && (recovery.value.logId ?? null) === selectedId.value ? recovery.value : null);
let controller, requestId = 0;
const selected = computed(() => logs.value.find(l => l.id === selectedId.value) || null);
const teamMembers = computed(() => members.value.filter(m => m.id !== session.value?.userId));
const memberName = computed(() => members.value.find(m => String(m.id) === memberId.value)?.displayName || '团队成员');
const roleLabel = computed(() => session.value?.role === 'ADMIN' ? '公司管理员' : '部门领导');
const heading = computed(() => area.value === 'team' ? '团队工作日志' : '我的工作记录');
function logout(message = '') {
  controller?.abort(); requestId++; sessionStorage.removeItem('pandora-session');
  session.value = null; members.value = []; logs.value = []; loading.value = false;
  loginError.value = message; savedMessage.value = ''; saving.value = false; recovery.value = null;
}
async function establish(user) {
  const people = await api('/users', { token: user.token });
  const me = people.find(p => p.id === user.userId);
  if (!me || !['ADMIN', 'LEADER'].includes(me.role)) throw new Error('这里是管理工作台，请使用管理员或领导账号；员工请使用 Android 端。');
  members.value = people;
  memberId.value = String(people.find(p => p.username === 'staff' && p.id !== me.id)?.id || people.find(p => p.id !== me.id)?.id || '');
  session.value = { ...user, role: me.role, displayName: me.displayName };
  sessionStorage.setItem('pandora-session', JSON.stringify(session.value));
  let pending = null;
  try { pending = JSON.parse(sessionStorage.getItem('pandora-unsaved') || 'null'); } catch { /* Ignore invalid local cache. */ }
  recovery.value = pending?.userId === me.id ? pending : null;
  date.value = '';
  area.value = recovery.value ? 'personal' : memberId.value ? 'team' : 'personal';
  await nextTick(); // Let filter watchers finish before selecting a recovered record.
  await loadLogs(recovery.value ? recovery.value.logId : undefined);
  if (recovery.value) savedMessage.value = '已恢复上次登录过期时未保存的内容，请检查后保存。';
}
async function login() {
  if (loginBusy.value) return;
  loginBusy.value = true; loginError.value = '';
  try { await establish(await api('/auth/login', { method: 'POST', body: { username: username.value.trim(), password: password.value } })); }
  catch (e) { logout(e.message); }
  finally { loginBusy.value = false; }
}
function quick(name) { username.value = name; password.value = 'demo1234'; }
async function loadLogs(preferredId) {
  controller?.abort(); const thisId = ++requestId; controller = new AbortController();
  if (!session.value) return;
  const target = area.value === 'personal' ? session.value.userId : memberId.value;
  logs.value = []; selectedId.value = null; loadError.value = '';
  if (!target) { loading.value = false; return; }
  loading.value = true;
  const query = new URLSearchParams({ userId: target }); if (date.value) query.set('date', date.value);
  try {
    const result = await api(`/logs?${query}`, { token: session.value.token, signal: controller.signal });
    if (thisId !== requestId) return;
    logs.value = result; selectedId.value = preferredId === null ? null : result.some(l => l.id === preferredId) ? preferredId : result[0]?.id ?? null;
    refreshedAt.value = new Date().toISOString();
  } catch (e) {
    if (thisId !== requestId || e.name === 'AbortError') return;
    if (e.status === 401) logout(e.message); else loadError.value = e.message;
  } finally { if (thisId === requestId) loading.value = false; }
}
async function onSaved(log) {
  if (recovery.value?.userId === session.value.userId) {
    sessionStorage.removeItem('pandora-unsaved'); recovery.value = null;
  }
  // Wait for a changed filter's watcher before refreshing with the saved ID.
  if (date.value && date.value !== log.logDate) date.value = '';
  await nextTick();
  await loadLogs(log.id);
  if (session.value) savedMessage.value = log.status === 'draft' ? '草稿已保存，仅你自己可见。' : '日志已保存，管理者可查看最新内容。';
}
watch([area, memberId, date], () => { savedMessage.value = ''; if (session.value) loadLogs(); });
onMounted(async () => {
  try {
    const raw = sessionStorage.getItem('pandora-session');
    if (raw) await establish(JSON.parse(raw));
  } catch (e) { logout(e.message); }
  finally { booting.value = false; }
});
onBeforeUnmount(() => controller?.abort());
</script>
<template>
  <div v-if="booting" class="boot">正在打开潘多拉…</div>
  <main v-else-if="!session" class="login-layout">
    <section class="login-story">
      <div class="brand"><span class="brand-mark">P</span><span>潘多拉<span class="brand-en">PANDORA</span></span></div>
      <div class="story-content"><span class="eyebrow">EVERY DAY, A LITTLE FORWARD</span><h1>让每一天的工作，<br>都有迹可循。</h1><p>从一篇日志开始。<br>记录进展，看见团队正在发生的事。</p>
        <div class="journal-art" aria-hidden="true"><div class="paper back"></div><div class="paper front"><div class="paper-date">DAILY NOTES / 01</div><div class="ink-line wide"></div><div class="ink-line"></div><div class="ink-line short"></div><div class="paper-check">✓ &nbsp; 今日，有所进展</div><div class="paper-stamp">P</div></div><div class="art-note">A space for your work.</div></div>
      </div><div class="story-foot">工作日志 · 团队协作</div>
    </section>
    <section class="login-side"><div class="login-card"><span class="eyebrow">WELCOME BACK</span><h2>进入管理工作台</h2><p class="muted">查看团队日志，也记录自己的工作。</p>
      <form @submit.prevent="login"><label for="username">账号</label><input id="username" v-model="username" autocomplete="username" required :disabled="loginBusy"><label for="password">密码</label><input id="password" v-model="password" type="password" autocomplete="current-password" required :disabled="loginBusy"><p v-if="loginError" class="error" role="alert">{{ loginError }}</p><button class="button primary login-button" :disabled="loginBusy">{{ loginBusy ? '正在登录…' : '登录工作台 →' }}</button></form>
      <div class="demo-accounts"><span>本地演示账号</span><div><button @click="quick('leader')" :disabled="loginBusy">部门领导</button><button @click="quick('admin')" :disabled="loginBusy">公司管理员</button></div><small>密码 demo1234 · 员工端由 Android 接入</small></div>
    </div><a class="credit" href="https://deerflow.tech" target="_blank" rel="noopener noreferrer">Created By Deerflow</a></section>
  </main>
  <div v-else class="workspace">
    <aside class="sidebar"><div class="brand"><span class="brand-mark">P</span><span>潘多拉<span class="brand-en">PANDORA</span></span></div><div class="workspace-label">工作空间</div><nav><button :class="{ active: area === 'team' }" :disabled="saving" @click="area = 'team'"><span>▤</span> 团队日志 <span class="nav-arrow">↗</span></button><button :class="{ active: area === 'personal' }" :disabled="saving" @click="area = 'personal'"><span>✎</span> 我的日志 <span class="nav-arrow">↗</span></button></nav><div class="sidebar-note"><span class="note-dot"></span>记录进展，保持同步。<p>每一篇日志<br>都是工作向前的一步。</p></div><div class="profile"><div class="avatar">{{ (session.displayName || '管').slice(0, 1) }}</div><div><strong>{{ session.displayName }}</strong><small>{{ roleLabel }}</small></div><button @click="logout()" :disabled="saving" title="退出登录" aria-label="退出登录">↪</button></div><a class="credit sidebar-credit" href="https://deerflow.tech" target="_blank" rel="noopener noreferrer">Created By Deerflow</a></aside>
    <main class="main-content"><header class="page-header"><div><span class="eyebrow">{{ area === 'team' ? 'TEAM JOURNAL' : 'MY JOURNAL' }}</span><h1>{{ heading }}<span class="heading-dot">.</span></h1><p>{{ area === 'team' ? '把分散的记录，汇成清楚的团队进展。' : '写下完成的事，也留下接下来要解决的问题。' }}</p></div><div class="header-date"><span>{{ today().replaceAll('-', ' / ') }}</span><small>今天也是向前的一天</small></div></header>
      <section class="toolbar panel"><div class="filter-fields"><div v-if="area === 'team'"><label for="member">团队成员</label><select id="member" v-model="memberId" :disabled="saving"><option value="" disabled>选择成员</option><option v-for="m in teamMembers" :value="String(m.id)" :key="m.id">{{ m.displayName || m.username }}</option></select></div><div><label for="filter-date">日志日期</label><div class="date-filter"><input id="filter-date" type="date" v-model="date" :max="today()" :disabled="saving"><button v-if="date" @click="date = ''" :disabled="saving" title="查看全部日期">清除</button><span v-else class="all-dates">全部日期</span></div></div></div><button class="button secondary refresh" @click="loadLogs(selectedId)" :disabled="loading || saving">{{ loading ? '加载中…' : '↻ 刷新日志' }}</button></section>
      <p v-if="savedMessage" class="success" role="status">✓ {{ savedMessage }}</p>
      <div class="section-heading"><h2>{{ area === 'team' ? memberName + '的日志' : '我的记录' }} <span>{{ logs.length.toString().padStart(2, '0') }}</span></h2><span v-if="refreshedAt">最近刷新 {{ timeLabel(refreshedAt) }}</span><button v-if="area === 'personal'" class="button primary small" @click="selectedId = null; savedMessage = ''" :disabled="saving || loading">＋ 写日志</button></div>
      <div v-if="loading" class="state panel" role="status"><div class="loader"></div><h3>正在读取日志</h3><p>稍等，正在同步最新记录。</p></div>
      <div v-else-if="loadError" class="state panel"><span class="state-symbol">!</span><h3>日志暂时没有加载成功</h3><p class="error" role="alert">{{ loadError }}</p><button class="button secondary" @click="loadLogs()">重新加载</button></div>
      <div v-else class="journal-grid"><section class="log-list panel"><div class="list-label">{{ area === 'personal' ? '我的日志 · 含草稿' : '已提交日志' }}</div><div v-if="!logs.length" class="list-empty"><span>↗</span><h3>{{ area === 'team' ? '这里还没有日志' : '开始你的第一篇记录' }}</h3><p>{{ area === 'team' ? '换个日期看看，或等待成员提交。' : '在右侧写下今天的工作。' }}</p></div><button v-for="(log, i) in logs" :key="log.id" class="log-row" :class="{ selected: selectedId === log.id }" @click="selectedId = log.id; savedMessage = ''" :disabled="saving"><div class="row-top"><span class="row-number">{{ String(i + 1).padStart(2, '0') }}</span><time>{{ log.logDate }}</time><span class="badge" :class="{ draft: log.status === 'draft' }">{{ log.status === 'draft' ? '草稿' : '已提交' }}</span></div><h3>{{ log.content.split('\n')[0] }}</h3><p>{{ log.content }}</p><div class="row-bottom"><span>更新于 {{ timeLabel(log.updatedAt) }}</span><span>↗</span></div></button></section>
        <LogEditor v-if="area === 'personal'" :key="selectedId ?? 'new'" :log="selected" :token="session.token" :user-id="session.userId" :recovery="editorRecovery" @saved="onSaved" @expired="logout" @busy="saving = $event" />
        <article v-else-if="selected" class="log-detail panel"><div class="detail-top"><span class="eyebrow">JOURNAL / {{ String(selected.id).padStart(3, '0') }}</span><span class="badge">已提交</span></div><h2>{{ selected.logDate.replaceAll('-', ' / ') }}</h2><div class="author-line"><span class="avatar mini">{{ memberName.slice(0,1) }}</span><strong>{{ memberName }}</strong><span>工作日志</span></div><div class="log-body">{{ selected.content }}</div><footer class="detail-footer"><span>最后修改 {{ timeLabel(selected.updatedAt) }}</span><span>记录 #{{ selected.id }}</span></footer></article>
        <section v-else class="state detail-empty panel"><span class="state-symbol">▤</span><h3>记录，让进展可见</h3><p>选择一篇日志，在这里阅读完整内容。</p></section>
      </div><footer class="page-footer"><span>PANDORA / 工作日志</span><span>把今天记录好，把明天安排好。</span></footer>
    </main>
  </div>
</template>
