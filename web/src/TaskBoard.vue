<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue';
import { api, timeLabel } from './api';
const props = defineProps({ session: { type: Object, required: true }, members: { type: Array, default: () => [] }, active: Boolean });
const emit = defineEmits(['expired', 'busy']);
const tasks = ref([]), selectedId = ref(null), filter = ref('all');
const loading = ref(false), pending = ref(false), loadError = ref(''), saveError = ref(''), message = ref(''), refreshedAt = ref(null);
const title = ref(''), detail = ref(''), priority = ref('normal'), assigneeId = ref(''), dueDate = ref('');
const draftKey = 'pandora-task-unsaved';
let alive = true, requestId = 0, loadController, saveController;
// /users already limits leaders to themselves and their direct members.
const assignees = computed(() => props.members.filter(m => m.id !== props.session.userId && (props.session.role === 'ADMIN' ? m.role !== 'ADMIN' : m.role === 'STAFF')));
const visibleTasks = computed(() => tasks.value.filter(task => filter.value === 'all' || task.status === filter.value));
const selected = computed(() => tasks.value.find(task => task.id === selectedId.value) || null);
const statuses = { todo: '待开始', doing: '进行中', done: '已完成' };
const priorities = { low: '低', normal: '普通', high: '高' };
const completed = computed(() => tasks.value.filter(task => task.status === 'done').length);
function dateLabel(value) {
  return value ? new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(new Date(value)) : '未设截止日期';
}
function overdue(task) { return task.status !== 'done' && task.dueAt && new Date(task.dueAt).getTime() < Date.now(); }
function draft() { return { userId: props.session.userId, title: title.value, detail: detail.value, priority: priority.value, assigneeId: assigneeId.value, dueDate: dueDate.value }; }
function expire(error) {
  if (title.value || detail.value || dueDate.value) sessionStorage.setItem(draftKey, JSON.stringify(draft()));
  emit('expired', error.message + (title.value || detail.value || dueDate.value ? ' 任务草稿将在同一账号登录后恢复。' : ''));
}
try {
  const saved = JSON.parse(sessionStorage.getItem(draftKey) || 'null');
  if (saved?.userId === props.session.userId) {
    title.value = saved.title || ''; detail.value = saved.detail || ''; priority.value = saved.priority || 'normal';
    assigneeId.value = saved.assigneeId || ''; dueDate.value = saved.dueDate || '';
    message.value = '已恢复未派发的任务，请检查后提交。';
  }
} catch { /* Ignore invalid local cache. */ }
watch(assignees, people => {
  if (!people.some(person => String(person.id) === String(assigneeId.value))) assigneeId.value = people[0] ? String(people[0].id) : '';
}, { immediate: true });
watch(visibleTasks, list => {
  if (!list.some(task => task.id === selectedId.value)) selectedId.value = list[0]?.id ?? null;
});
async function loadTasks(preferredId = selectedId.value) {
  loadController?.abort(); loadController = new AbortController(); const thisId = ++requestId;
  const token = props.session.token; loading.value = true; loadError.value = '';
  try {
    const result = await api('/tasks', { token, signal: loadController.signal });
    if (!alive || thisId !== requestId || token !== props.session.token) return;
    tasks.value = result;
    selectedId.value = visibleTasks.value.some(task => task.id === preferredId) ? preferredId : visibleTasks.value[0]?.id ?? null;
    refreshedAt.value = new Date().toISOString();
  } catch (error) {
    if (!alive || thisId !== requestId || error.name === 'AbortError' || token !== props.session.token) return;
    if (error.status === 401) expire(error); else loadError.value = error.message;
  } finally { if (alive && thisId === requestId) loading.value = false; }
}
async function createTask() {
  if (pending.value) return;
  saveError.value = ''; message.value = '';
  const cleanTitle = title.value.trim();
  if (!cleanTitle || cleanTitle.length > 128 || detail.value.length > 5000 || !assignees.value.some(person => String(person.id) === String(assigneeId.value))) {
    saveError.value = '请填写 1–128 字的任务标题，说明不超过 5000 字，并选择责任人。'; return;
  }
  let dueAt = null;
  if (dueDate.value) {
    const value = new Date(dueDate.value + 'T23:59:59+08:00');
    if (Number.isNaN(value.getTime())) { saveError.value = '请选择有效的截止日期。'; return; }
    dueAt = value.toISOString();
  }
  const token = props.session.token;
  saveController = new AbortController(); pending.value = true; emit('busy', true);
  try {
    const task = await api('/tasks', { token, method: 'POST', signal: saveController.signal, body: { title: cleanTitle, detail: detail.value, priority: priority.value, assigneeId: Number(assigneeId.value), dueAt } });
    if (!alive || token !== props.session.token) return;
    sessionStorage.removeItem(draftKey);
    title.value = ''; detail.value = ''; priority.value = 'normal'; dueDate.value = ''; filter.value = 'all';
    tasks.value = [task, ...tasks.value.filter(item => item.id !== task.id)]; selectedId.value = task.id;
    message.value = '任务已派发，责任人可在 Android 端查看并反馈进度。';
    await loadTasks(task.id);
  } catch (error) {
    if (!alive || error.name === 'AbortError' || token !== props.session.token) return;
    if (error.status === 401) expire(error); else saveError.value = error.message;
  } finally { if (alive) { pending.value = false; emit('busy', false); } }
}
watch(() => props.active, active => { if (active) loadTasks(); }, { immediate: true });
onBeforeUnmount(() => {
  // Also retain the draft if another request in the workspace expires the session.
  if (title.value || detail.value || dueDate.value) sessionStorage.setItem(draftKey, JSON.stringify(draft()));
  alive = false; requestId++; loadController?.abort(); saveController?.abort();
});
</script>
<template>
  <section class="task-board" data-testid="task-board">
    <div class="toolbar panel task-toolbar"><div class="task-summary"><span class="eyebrow">DISPATCH & FOLLOW THROUGH</span><strong>{{ tasks.length }} <small>项任务</small><span class="summary-divider">/</span> {{ completed }} <small>项已完成</small></strong></div><button class="button secondary" data-testid="task-refresh" @click="loadTasks()" :disabled="loading || pending">{{ loading ? '加载中…' : '↻ 刷新任务' }}</button></div>
    <p v-if="message" class="success" role="status">✓ {{ message }}</p>
    <div class="task-layout">
      <section class="task-create panel"><div class="detail-top"><span class="eyebrow">NEW DISPATCH</span><span class="badge">新任务</span></div><h2>把下一步安排好</h2><p class="muted">明确责任人，让进展有回应。</p>
        <form data-testid="task-create-form" @submit.prevent="createTask">
          <label for="task-title">任务标题</label><input id="task-title" v-model="title" maxlength="128" :disabled="pending" required placeholder="需要完成什么？">
          <label for="task-detail">任务说明</label><textarea id="task-detail" v-model="detail" maxlength="5000" :disabled="pending" placeholder="写下要求、交付内容或需要关注的事项"></textarea><div class="editor-meta"><span>让责任人清楚下一步</span><span>{{ detail.length }} / 5000</span></div>
          <label for="task-assignee">责任人</label><select id="task-assignee" v-model="assigneeId" :disabled="pending || !assignees.length" required><option value="" disabled>选择责任人</option><option v-for="person in assignees" :key="person.id" :value="String(person.id)">{{ person.displayName || person.username }}</option></select><p v-if="!assignees.length" class="muted no-assignee">当前没有可派发的成员。</p>
          <div class="task-form-row"><div><label for="task-priority">优先级</label><select id="task-priority" v-model="priority" :disabled="pending"><option value="low">低</option><option value="normal">普通</option><option value="high">高</option></select></div><div><label for="task-due-date">截止日期 <span>（可选）</span></label><input id="task-due-date" type="date" v-model="dueDate" :disabled="pending"></div></div>
          <p v-if="saveError" class="error" role="alert">{{ saveError }}</p><div class="editor-actions"><button class="button primary" type="submit" data-testid="task-submit" :disabled="pending || !assignees.length">{{ pending ? '正在派发…' : '派发任务 →' }}</button></div>
        </form>
      </section>
      <section class="task-tracking"><div class="section-heading task-section-heading"><h2>任务进展 <span>{{ visibleTasks.length.toString().padStart(2, '0') }}</span></h2><span v-if="refreshedAt">最近刷新 {{ timeLabel(refreshedAt) }}</span></div>
        <div class="task-filters" role="group" aria-label="任务状态筛选"><button v-for="(label, key) in { all: '全部', ...statuses }" :key="key" :class="{ active: filter === key }" :aria-pressed="filter === key" @click="filter = key">{{ label }}</button></div>
        <div v-if="loading" class="state panel" role="status"><div class="loader"></div><h3>正在同步任务</h3><p>读取最新派发和进度反馈。</p></div>
        <div v-else-if="loadError" class="state panel" data-testid="task-load-error"><span class="state-symbol">!</span><h3>任务暂时没有加载成功</h3><p class="error" role="alert">{{ loadError }}</p><button class="button secondary" @click="loadTasks()" :disabled="pending">重新加载</button></div>
        <template v-else><div class="task-list panel" data-testid="task-list"><div v-if="!visibleTasks.length" class="list-empty" data-testid="task-empty"><span>↗</span><h3>{{ tasks.length ? '这个状态下还没有任务' : '从一个明确的任务开始' }}</h3><p>{{ tasks.length ? '选择其他状态，查看已有任务。' : '在左侧安排任务，随后在这里跟进进展。' }}</p></div>
          <button v-for="task in visibleTasks" :key="task.id" class="task-row" :class="{ selected: selectedId === task.id }" :data-task-id="task.id" @click="selectedId = task.id"><div class="row-top"><span class="row-number">#{{ String(task.id).padStart(3, '0') }}</span><span class="badge" :class="task.status">{{ statuses[task.status] }}</span><span class="task-percent">{{ task.progress }}%</span></div><h3>{{ task.title }}</h3><div class="task-row-meta"><span>{{ task.assigneeName }}</span><span :class="{ overdue: overdue(task) }">{{ overdue(task) ? '已逾期 · ' : '' }}{{ task.dueAt ? dateLabel(task.dueAt) : '未设截止日期' }}</span></div><div class="progress-track" aria-hidden="true"><span :style="{ width: task.progress + '%' }"></span></div></button>
        </div>
        <article v-if="selected" class="task-detail panel" data-testid="task-detail"><div class="detail-top"><span class="eyebrow">TASK / {{ String(selected.id).padStart(3, '0') }}</span><span class="badge" :class="selected.status">{{ statuses[selected.status] }}</span></div><h2>{{ selected.title }}</h2><dl class="task-facts"><div><dt>责任人</dt><dd>{{ selected.assigneeName }}</dd></div><div><dt>派发人</dt><dd>{{ selected.creatorName }}</dd></div><div><dt>优先级</dt><dd :class="{ 'priority-high': selected.priority === 'high' }">{{ priorities[selected.priority] }}</dd></div><div><dt>截止时间 · 北京时间</dt><dd :class="{ overdue: overdue(selected) }">{{ dateLabel(selected.dueAt) }}{{ overdue(selected) ? ' · 已逾期' : '' }}</dd></div></dl><div v-if="selected.detail" class="task-description">{{ selected.detail }}</div><div class="task-progress-heading"><h3>完成进度</h3><strong data-testid="task-progress">{{ selected.progress }}<small>%</small></strong></div><div class="progress-track detail-progress" role="progressbar" :aria-valuenow="selected.progress" aria-valuemin="0" aria-valuemax="100" aria-label="完成进度"><span :style="{ width: selected.progress + '%' }"></span></div><div class="progress-note"><span>最新进度说明</span><p data-testid="task-progress-note">{{ selected.progressNote || '责任人尚未反馈进度。' }}</p></div><footer class="detail-footer"><span>最后更新 {{ dateLabel(selected.updatedAt) }}</span><span>任务 #{{ selected.id }}</span></footer></article>
        </template>
        <p class="muted task-refresh-hint">责任人在 Android 端反馈后，刷新任务即可查看最新进展。</p>
      </section>
    </div>
  </section>
</template>
<style scoped>
.task-summary{display:grid;gap:10px}.task-summary>.eyebrow{color:#8b9782;font-size:9px}.task-summary strong{font:23px Georgia,serif}.task-summary small{font:11px "Microsoft YaHei UI",sans-serif;color:#7d8876}.summary-divider{color:#ccd3c3;padding:0 13px}.task-layout{display:grid;grid-template-columns:minmax(275px,.82fr) minmax(350px,1.3fr);gap:22px;align-items:start;margin-top:24px}.task-create{padding:25px 27px}.task-create h2{font-size:21px;font-weight:500;margin-bottom:9px}.task-create .detail-top{margin-bottom:21px}.task-create>.muted{font-size:11px}.task-create input,.task-create select,.task-create textarea{font-size:12px}.task-create textarea{min-height:150px}.task-create label{font-size:11px}.task-create label span{color:#98a18e;font-weight:400}.task-form-row{display:grid;grid-template-columns:.8fr 1.2fr;gap:15px;align-items:start}.task-form-row>div{min-width:0}.task-create .error{margin-top:16px}.task-section-heading{margin:0 0 18px}.task-filters{display:flex;gap:7px;margin-bottom:15px;flex-wrap:wrap}.task-filters button{border:1px solid var(--line);border-radius:6px;padding:8px 13px;background:transparent;color:#7d8974;font-size:11px}.task-filters button.active{color:#183f35;background:#e5ebdd;border-color:#cbd7c1}.task-list{max-height:380px;overflow-y:auto}.task-row{display:block;width:100%;background:transparent;border:0;border-left:3px solid transparent;border-bottom:1px solid var(--line);text-align:left;color:inherit;padding:18px 20px}.task-row:last-child{border-bottom:0}.task-row.selected{background:#edf1e6;border-left-color:var(--green)}.task-row:hover{background:#f1f3eb}.task-row h3{font-size:14px;line-height:1.7;margin:12px 0 10px;overflow-wrap:anywhere}.task-percent{margin-left:auto;font:16px Georgia,serif;color:#506a50}.task-row-meta{display:flex;justify-content:space-between;gap:12px;font-size:10px;color:#8c9781;flex-wrap:wrap}.progress-track{height:4px;background:#e2e8d9;border-radius:4px;overflow:hidden;margin-top:15px}.progress-track>span{display:block;height:100%;background:#658465;border-radius:4px}.badge.todo{color:#8b7548;background:#f2ead6}.badge.doing{color:#4c6e74;background:#e3eeeb}.task-detail{padding:25px 27px;margin-top:20px}.task-detail h2{font-size:20px;font-weight:500;line-height:1.7;margin-bottom:20px;overflow-wrap:anywhere}.task-facts{display:grid;grid-template-columns:1fr 1fr;gap:17px 20px;margin:0;padding-bottom:22px;border-bottom:1px solid var(--line)}.task-facts dt{color:#9aA38e;font-size:10px;margin-bottom:7px}.task-facts dd{margin:0;font-size:12px;line-height:1.8;overflow-wrap:anywhere}.priority-high,.overdue{color:#b77749!important}.task-description{white-space:pre-wrap;overflow-wrap:anywhere;font-size:13px;line-height:2;padding:22px 0 5px}.task-progress-heading{display:flex;align-items:center;justify-content:space-between;margin-top:22px}.task-progress-heading h3{font-size:12px;font-weight:500;margin:0}.task-progress-heading strong{font:30px Georgia,serif;color:#426746}.task-progress-heading small{font-size:14px;margin-left:3px}.detail-progress{height:6px;margin-top:10px}.progress-note{background:#f3f5ed;border-radius:6px;padding:16px 18px;margin:22px 0}.progress-note>span{font-size:10px;color:#8e9983}.progress-note p{white-space:pre-wrap;overflow-wrap:anywhere;font-size:12px;line-height:1.9;margin:9px 0 0}.task-refresh-hint{font-size:10px;margin:15px 0 0}.no-assignee{font-size:10px;margin:8px 0 0}@media(max-width:1250px){.task-layout{grid-template-columns:minmax(260px,.9fr) minmax(300px,1.15fr)}.task-create,.task-detail{padding:22px 20px}.task-form-row{grid-template-columns:1fr}.task-form-row label{margin-top:16px}}@media(max-width:1000px){.task-layout{grid-template-columns:minmax(0,1fr)}.task-form-row{grid-template-columns:.8fr 1.2fr}.task-list{max-height:350px}.task-section-heading{margin-top:8px}.task-facts{gap:16px}.task-refresh-hint{margin-bottom:0}}@media(max-width:700px){.task-layout{gap:25px}.task-toolbar{flex-wrap:nowrap}.task-toolbar .button{padding:10px 11px}.task-summary strong{font-size:20px}.task-summary>.eyebrow{font-size:8px}.task-create textarea{min-height:130px}.task-facts{grid-template-columns:minmax(0,1fr) minmax(0,1fr)}.task-create,.task-detail{padding:22px 20px}}
</style>
