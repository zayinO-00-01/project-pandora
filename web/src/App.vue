<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { api, timeLabel } from './api';
import { roles, eligibleManagers, draftForStorage, restoreDraft } from './admin-state';

const session = ref(null), booting = ref(true), loginBusy = ref(false);
const username = ref(''), password = ref(''), loginError = ref('');
const users = ref([]), loading = ref(false), saving = ref(false), loadError = ref('');
const formError = ref(''), notice = ref(''), success = ref(''), refreshedAt = ref(null);
const search = ref(''), roleFilter = ref(''), statusFilter = ref('');
const emptyForm = () => ({ id: null, username: '', displayName: '', role: 'STAFF', managerId: '', disabled: false, password: '',
  phone: '', idCard: '', birthDate: '', gender: '', hireDate: '', emergencyContact: '', emergencyPhone: '', address: '' });
const form = reactive(emptyForm());
const baseline = ref(JSON.stringify(form));
const dirty = computed(() => JSON.stringify(form) !== baseline.value);
const isAdmin = computed(() => session.value?.role === 'ADMIN');
const activeTab = ref('accounts');
const notices = ref([]), noticeLoading = ref(false), noticeSaving = ref(false), noticeError = ref(''), noticeSuccess = ref('');
const emptyNotice = () => ({ id: null, title: '', content: '', sortOrder: 0, enabled: true });
const noticeForm = reactive(emptyNotice());
const enabledNoticeCount = computed(() => notices.value.filter(notice => notice.enabled).length);
function sortNotices() { notices.value = [...notices.value].sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id); }
function chooseNotice(notice = null) {
  Object.assign(noticeForm, emptyNotice(), notice ? { id: notice.id, title: notice.title, content: notice.content, sortOrder: notice.sortOrder, enabled: notice.enabled } : {});
  noticeError.value = ''; noticeSuccess.value = '';
}
async function loadNotices() {
  if (!isAdmin.value) return;
  noticeLoading.value = true; noticeError.value = '';
  try { notices.value = await api('/admin/company-notices', { token: session.value.token }); sortNotices(); }
  catch (error) { if ([401, 403].includes(error.status)) expire(error); else noticeError.value = error.message; }
  finally { noticeLoading.value = false; }
}
function switchTab(tab) { activeTab.value = tab; if (tab === 'notices' && !notices.value.length) loadNotices(); }
async function saveNotice() {
  if (noticeSaving.value || !isAdmin.value) return;
  noticeError.value = ''; noticeSuccess.value = '';
  if (!noticeForm.title.trim()) { noticeError.value = '标题不能为空。'; return; }
  if (!noticeForm.content.trim()) { noticeError.value = '内容不能为空。'; return; }
  const editing = noticeForm.id !== null;
  const payload = { title: noticeForm.title.trim(), content: noticeForm.content.trim(), sortOrder: Number(noticeForm.sortOrder) || 0, enabled: noticeForm.enabled };
  noticeSaving.value = true;
  try {
    const result = await api(editing ? `/admin/company-notices/${noticeForm.id}` : '/admin/company-notices', { token: session.value.token, method: editing ? 'PATCH' : 'POST', body: payload });
    notices.value = editing ? notices.value.map(notice => notice.id === result.id ? result : notice) : [...notices.value, result];
    sortNotices(); chooseNotice(result);
    noticeSuccess.value = `${result.title} 已${editing ? '更新' : '发布'}。`;
  } catch (error) { if ([401, 403].includes(error.status)) expire(error); else noticeError.value = error.message; }
  finally { noticeSaving.value = false; }
}
async function toggleNotice(notice) {
  noticeError.value = ''; noticeSuccess.value = '';
  try {
    const result = await api(`/admin/company-notices/${notice.id}`, { token: session.value.token, method: 'PATCH', body: { enabled: !notice.enabled } });
    notices.value = notices.value.map(item => item.id === result.id ? result : item); sortNotices();
    noticeSuccess.value = `${result.title} 已${result.enabled ? '启用' : '停用'}。`;
  } catch (error) { if ([401, 403].includes(error.status)) expire(error); else noticeError.value = error.message; }
}
const managers = computed(() => eligibleManagers(users.value, form.role, form.id));
const canHaveManager = computed(() => ['DEPT_HEAD', 'TEAM_LEAD', 'STAFF'].includes(form.role));
const selectedManagerValid = computed(() => !form.managerId || managers.value.some(user => String(user.id) === String(form.managerId)));
const roleName = value => roles.find(role => role.value === value)?.label || value;
const displayName = user => user.displayName || user.username;
const managerName = id => users.value.find(user => String(user.id) === String(id))?.displayName || users.value.find(user => String(user.id) === String(id))?.username || (id ? `账号 #${id}` : '未设置');
const filteredUsers = computed(() => {
  const query = search.value.trim().toLowerCase();
  return users.value.filter(user => (!query || `${user.username} ${user.displayName || ''}`.toLowerCase().includes(query))
    && (!roleFilter.value || user.role === roleFilter.value)
    && (!statusFilter.value || (statusFilter.value === 'disabled') === user.disabled));
});
const enabledCount = computed(() => users.value.filter(user => !user.disabled).length);
const scope = user => ({ server: window.location.origin, userId: user.userId });
const draftKey = user => `pandora-admin-draft:${window.location.origin}:${user.userId}`;
let loadController, loadVersion = 0, memoryRecovery = null;
const readCache = key => { try { return JSON.parse(sessionStorage.getItem(key) || 'null'); } catch { return null; } };
const writeCache = (key, value) => { try { sessionStorage.setItem(key, JSON.stringify(value)); } catch { /* The current form still works when browser storage is unavailable. */ } };
const removeCache = key => { try { sessionStorage.removeItem(key); } catch { /* Storage is optional. */ } };

function resetForm(user = null) {
  Object.assign(form, emptyForm(), user ? { ...user, managerId: user.managerId == null ? '' : String(user.managerId), password: '' } : {});
  baseline.value = JSON.stringify(form);
  formError.value = ''; notice.value = '';
}
function rememberDraft() {
  if (!isAdmin.value) return;
  if (!dirty.value) { removeCache(draftKey(session.value)); return; }
  writeCache(draftKey(session.value), draftForStorage(scope(session.value), form));
}
function endSession(message = '', preserve = false) {
  if (session.value) {
    if (preserve && isAdmin.value && dirty.value) {
      rememberDraft();
      memoryRecovery = { ...scope(session.value), form: { ...form } };
    } else {
      removeCache(draftKey(session.value)); memoryRecovery = null;
    }
  }
  loadController?.abort(); loadVersion++;
  removeCache('pandora-admin-session'); removeCache('pandora-session');
  session.value = null; users.value = []; loading.value = false;
  resetForm(); success.value = ''; loadError.value = ''; loginError.value = message;
}
function expire(error) {
  endSession(`${error.message} 未保存的账号信息会在同一管理员重新登录后恢复。`, true);
}
function chooseUser(user = null) {
  if (saving.value || (user && form.id === user.id)) return;
  if (dirty.value && !window.confirm('当前表单有未保存的修改，是否放弃并切换？')) return;
  if (session.value) removeCache(draftKey(session.value));
  memoryRecovery = null; resetForm(user); success.value = '';
}
async function loadUsers() {
  if (!isAdmin.value) return;
  loadController?.abort(); loadController = new AbortController();
  const version = ++loadVersion;
  loading.value = true; loadError.value = '';
  try {
    const result = await api('/admin/users', { token: session.value.token, signal: loadController.signal });
    if (version !== loadVersion) return;
    const me = result.find(user => user.id === session.value.userId);
    if (!me || me.disabled || me.role !== 'ADMIN') {
      expire(new Error('此账号已无法使用账号管理，请重新登录。')); return;
    }
    users.value = result; refreshedAt.value = new Date().toISOString();
    session.value = { ...session.value, displayName: me.displayName };
    writeCache('pandora-admin-session', session.value);
  } catch (error) {
    if (version !== loadVersion || error.name === 'AbortError') return;
    if ([401, 403].includes(error.status)) expire(error);
    else loadError.value = error.message;
  } finally { if (version === loadVersion) loading.value = false; }
}
async function establish(user) {
  session.value = user; resetForm(); search.value = ''; roleFilter.value = ''; statusFilter.value = '';
  activeTab.value = 'accounts'; notices.value = []; chooseNotice();
  writeCache('pandora-admin-session', user);
  if (user.role !== 'ADMIN') return;
  const recovered = restoreDraft(readCache(draftKey(user)), scope(user));
  if (recovered) {
    Object.assign(form, recovered);
    if (memoryRecovery?.server === scope(user).server && memoryRecovery?.userId === user.userId) Object.assign(form, memoryRecovery.form);
    notice.value = '已恢复未保存的账号信息，请检查后保存。刷新过页面时，请重新填写所需密码。';
  }
  memoryRecovery = null;
  await loadUsers();
}
async function login() {
  if (loginBusy.value) return;
  loginBusy.value = true; loginError.value = '';
  try {
    const user = await api('/auth/login', { method: 'POST', body: { username: username.value.trim(), password: password.value } });
    password.value = ''; await establish(user);
  } catch (error) { loginError.value = error.message; }
  finally { loginBusy.value = false; }
}
async function saveUser() {
  if (saving.value || !isAdmin.value) return;
  formError.value = ''; success.value = '';
  if (!form.id && (form.username.trim().length < 3 || form.username.trim().length > 64)) { formError.value = '账号须为 3–64 个字符。'; return; }
  if ((!form.id || form.password) && (form.password.length < 6 || form.password.length > 72)) { formError.value = '密码须为 6–72 个字符。'; return; }
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone.trim())) { formError.value = '手机号须为 11 位有效号码。'; return; }
  if (form.idCard && !/^(\d{15}|\d{17}[0-9Xx])$/.test(form.idCard.trim())) { formError.value = '身份证号格式不正确。'; return; }
  if (!selectedManagerValid.value) { formError.value = '请选择角色更高且已启用的业务上级，或选择未设置。'; return; }
  const editing = form.id !== null;
  const profile = { phone: form.phone.trim(), idCard: form.idCard.trim() || null, birthDate: form.birthDate || null,
    gender: form.gender || null, hireDate: form.hireDate || null, emergencyContact: form.emergencyContact.trim() || null,
    emergencyPhone: form.emergencyPhone.trim() || null, address: form.address.trim() || null };
  const payload = { role: form.role, displayName: form.displayName.trim(), disabled: form.disabled, ...profile };
  if (editing) {
    if (form.managerId) payload.managerId = Number(form.managerId); else payload.clearManager = true;
    if (form.password) payload.password = form.password;
  } else Object.assign(payload, { username: form.username.trim(), password: form.password, managerId: form.managerId ? Number(form.managerId) : null });
  saving.value = true;
  try {
    const result = await api(editing ? `/admin/users/${form.id}` : '/admin/users', { token: session.value.token, method: editing ? 'PATCH' : 'POST', body: payload });
    users.value = editing ? users.value.map(user => user.id === result.id ? result : user) : [...users.value, result];
    removeCache(draftKey(session.value)); memoryRecovery = null;
    const changedSelf = result.id === session.value.userId;
    const resetOwnPassword = changedSelf && Boolean(payload.password);
    resetForm(result);
    if (changedSelf && (result.disabled || result.role !== 'ADMIN' || resetOwnPassword)) {
      endSession('账号修改已保存，请使用当前账号信息重新登录。'); return;
    }
    if (changedSelf) { session.value.displayName = result.displayName; writeCache('pandora-admin-session', session.value); }
    success.value = `${displayName(result)}的账号已${editing ? '更新' : '创建'}。`;
  } catch (error) {
    if ([401, 403].includes(error.status)) expire(error);
    else {
      formError.value = error.message + (!editing ? ' 若连接在提交后中断，请先刷新列表确认账号是否已创建。' : '');
      rememberDraft();
    }
  } finally { saving.value = false; }
}
watch(form, rememberDraft, { deep: true });
watch(() => form.role, () => { if (!canHaveManager.value) form.managerId = ''; });
function beforeUnload(event) { if (isAdmin.value && dirty.value) { rememberDraft(); event.preventDefault(); event.returnValue = ''; } }
onMounted(async () => {
  const saved = readCache('pandora-admin-session');
  if (saved?.token && saved?.userId && roles.some(role => role.value === saved.role)) await establish(saved);
  booting.value = false;
  window.addEventListener('beforeunload', beforeUnload);
});
onBeforeUnmount(() => { loadController?.abort(); window.removeEventListener('beforeunload', beforeUnload); });
</script>

<template>
  <div v-if="booting" class="boot" role="status">正在打开潘多拉…</div>
  <main v-else-if="!session" class="login-layout">
    <section class="login-story">
      <div class="brand"><span class="brand-mark">P</span><span>潘多拉<span class="brand-en">PANDORA</span></span></div>
      <div class="story-content"><span class="eyebrow">PEOPLE, CONNECTED</span><h1>让每一位同事，<br>各就其位。</h1><p>维护账号与组织关系，<br>让协作从清楚的连接开始。</p><div class="org-art" aria-hidden="true"><span>创始人</span><i></i><span>部门 · 团队</span><i></i><span>每一位同事</span></div></div>
      <div class="story-foot">账号管理 · 组织关系</div>
    </section>
    <section class="login-side"><div class="login-card"><span class="eyebrow">ADMINISTRATION</span><h2>登录账号管理</h2><p class="muted">管理员在此维护账号。工作日志与任务请使用 Android App。</p>
      <form @submit.prevent="login"><label for="username">账号</label><input id="username" v-model="username" autocomplete="username" required maxlength="64" :disabled="loginBusy"><label for="password">密码</label><input id="password" v-model="password" type="password" autocomplete="current-password" required :disabled="loginBusy"><p v-if="loginError" class="error" role="alert">{{ loginError }}</p><button class="button primary login-button" :disabled="loginBusy">{{ loginBusy ? '正在登录…' : '登录 →' }}</button></form>
      <p class="login-help">账号由管理员统一创建，暂不开放注册。<br>忘记密码或账号停用，请联系管理员。</p>
    </div><a class="credit" href="https://deerflow.tech" target="_blank" rel="noopener noreferrer">Created By Deerflow</a></section>
  </main>
  <main v-else-if="!isAdmin" class="app-guidance"><section class="panel guidance-card"><div class="brand"><span class="brand-mark">P</span><span>潘多拉<span class="brand-en">PANDORA</span></span></div><span class="eyebrow">YOUR WORK, IN THE APP</span><h1>请前往 Android App</h1><p>{{ session.displayName || username }}，你当前的角色是「{{ roleName(session.role) }}」。</p><p class="muted">使用同一账号登录 Android App，记录工作日志、处理任务及查看组织。Web 入口供管理员维护账号。</p><button class="button primary" @click="endSession()">返回登录</button><a class="credit" href="https://deerflow.tech" target="_blank" rel="noopener noreferrer">Created By Deerflow</a></section></main>
  <div v-else class="workspace">
    <aside class="sidebar"><div class="brand"><span class="brand-mark">P</span><span>潘多拉<span class="brand-en">PANDORA</span></span></div><div class="workspace-label">管理空间</div><nav aria-label="管理导航"><button type="button" class="nav-item" :class="{ 'active-nav': activeTab === 'accounts' }" @click="switchTab('accounts')">▦ <strong>账号管理</strong></button><button type="button" class="nav-item" :class="{ 'active-nav': activeTab === 'notices' }" @click="switchTab('notices')">★ <strong>公司十大事项</strong></button></nav><div class="sidebar-note"><span class="note-dot"></span>清楚的关系，顺畅的协作。<p>业务角色使用 App。<br>管理员在这里维护账号。</p></div><div class="profile"><div class="avatar">{{ (session.displayName || '管').slice(0, 1) }}</div><div><strong>{{ session.displayName || '管理员' }}</strong><small>管理员</small></div><button @click="endSession()" :disabled="saving" title="退出登录" aria-label="退出登录">↪</button></div><a class="credit sidebar-credit" href="https://deerflow.tech" target="_blank" rel="noopener noreferrer">Created By Deerflow</a></aside>
    <main class="main-content"><template v-if="activeTab === 'accounts'">
      <header class="page-header"><div><span class="eyebrow">ACCOUNT DIRECTORY</span><h1>账号管理<span class="heading-dot">.</span></h1><p>维护角色、直属上级、档案与账号状态。</p></div><div class="account-total"><strong>{{ users.length }}</strong><span>个账号 · {{ enabledCount }} 个启用</span></div></header>
      <section class="toolbar panel" aria-label="筛选账号"><div class="filter-fields"><div class="search-field"><label for="search">搜索账号</label><input id="search" v-model="search" placeholder="账号或姓名" type="search"></div><div><label for="role-filter">角色</label><select id="role-filter" v-model="roleFilter"><option value="">全部角色</option><option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option></select></div><div><label for="status-filter">状态</label><select id="status-filter" v-model="statusFilter"><option value="">全部状态</option><option value="enabled">启用</option><option value="disabled">停用</option></select></div></div><button class="button secondary" @click="loadUsers" :disabled="loading || saving">{{ loading ? '刷新中…' : '↻ 刷新' }}</button></section>
      <p v-if="success" class="success" role="status">✓ {{ success }}</p><p v-if="loadError" class="error" role="alert">{{ loadError }} <button class="text-button" @click="loadUsers" :disabled="loading || saving">重新加载</button></p>
      <div class="account-grid"><section class="panel directory"><div class="section-heading"><h2>全部账号 <span>{{ filteredUsers.length }}</span></h2><span v-if="refreshedAt">刷新于 {{ timeLabel(refreshedAt) }}</span></div><p v-if="loading && !users.length" class="empty-state" role="status">正在读取账号…</p><p v-else-if="!filteredUsers.length" class="empty-state">{{ loadError ? '账号列表暂不可用，请重试。' : users.length ? '没有符合筛选条件的账号。' : '暂无账号。' }}</p><div v-else class="table-scroll"><table><thead><tr><th scope="col">账号 / 姓名</th><th scope="col">角色 / 直属上级</th><th scope="col">状态</th><th scope="col"><span class="sr-only">操作</span></th></tr></thead><tbody><tr v-for="user in filteredUsers" :key="user.id" :class="{ selected: form.id === user.id }"><td><strong>{{ displayName(user) }} <small v-if="user.id === session.userId">我</small></strong><span class="account-username">{{ user.username }}</span></td><td>{{ roleName(user.role) }}<span class="account-manager">{{ managerName(user.managerId) }}</span></td><td><span class="badge" :class="{ disabled: user.disabled }">{{ user.disabled ? '停用' : '启用' }}</span></td><td><button class="text-button" :aria-label="`编辑${user.username}`" :disabled="saving" @click="chooseUser(user)">编辑 ↗</button></td></tr></tbody></table></div></section>
        <section class="panel account-editor"><div class="editor-heading"><div><span class="eyebrow">{{ form.id ? `ACCOUNT / ${form.id}` : 'NEW ACCOUNT' }}</span><h2>{{ form.id ? '编辑账号' : '新建账号' }}</h2></div><button v-if="form.id" class="button secondary small" @click="chooseUser()" :disabled="saving">＋ 新建</button></div><p class="muted">{{ form.id ? '修改账号资料，密码留空则保持原密码。' : '创建后，请将账号信息交给对应同事。' }}</p><p v-if="notice" class="notice" role="status">{{ notice }}</p>
          <form @submit.prevent="saveUser"><fieldset :disabled="saving"><label for="account-username">账号 <span v-if="form.id">创建后不可修改</span></label><input id="account-username" v-model="form.username" autocomplete="off" :readonly="form.id !== null" required minlength="3" maxlength="64"><label for="display-name">姓名 <span>选填</span></label><input id="display-name" v-model="form.displayName" autocomplete="off" maxlength="64" placeholder="留空时使用账号名"><div class="form-pair"><div><label for="account-role">角色</label><select id="account-role" v-model="form.role"><option v-for="role in roles" :key="role.value" :value="role.value">{{ role.label }}</option></select></div><div><label for="account-status">账号状态</label><select id="account-status" v-model="form.disabled"><option :value="false">启用</option><option :value="true">停用</option></select></div></div><label for="account-manager">直属上级 <span>选填</span></label><select id="account-manager" v-model="form.managerId" :disabled="!canHaveManager"><option value="">未设置</option><option v-if="!selectedManagerValid" :value="form.managerId" disabled>{{ managerName(form.managerId) }}（请重新选择）</option><option v-for="manager in managers" :key="manager.id" :value="String(manager.id)">{{ displayName(manager) }} · {{ roleName(manager.role) }}</option></select><p class="field-help">{{ canHaveManager ? '仅显示已启用、角色层级更高的业务账号。' : '管理员和创始人无需设置直属上级。' }}</p><label for="account-phone">手机号 <span>必填 · 唯一</span></label><input id="account-phone" v-model="form.phone" autocomplete="off" maxlength="20" placeholder="11 位手机号"><label for="account-idcard">身份证号 <span>选填 · 唯一</span></label><input id="account-idcard" v-model="form.idCard" autocomplete="off" maxlength="20" placeholder="15 或 18 位"><div class="form-pair"><div><label for="account-birth">出生年月 <span>选填</span></label><input id="account-birth" v-model="form.birthDate" type="date"></div><div><label for="account-gender">性别 <span>选填</span></label><select id="account-gender" v-model="form.gender"><option value="">未填写</option><option value="MALE">男</option><option value="FEMALE">女</option><option value="UNSPECIFIED">不透露</option></select></div></div><div class="form-pair"><div><label for="account-hire">入职日期 <span>选填</span></label><input id="account-hire" v-model="form.hireDate" type="date"></div><div><label for="account-ec">紧急联系人 <span>选填</span></label><input id="account-ec" v-model="form.emergencyContact" maxlength="64" autocomplete="off"></div></div><label for="account-ep">紧急联系电话 <span>选填</span></label><input id="account-ep" v-model="form.emergencyPhone" maxlength="20" autocomplete="off"><label for="account-address">住址 <span>选填</span></label><input id="account-address" v-model="form.address" maxlength="255" autocomplete="off"><label for="account-password">{{ form.id ? '重置密码' : '初始密码' }} <span>{{ form.id ? '选填' : '6–72 个字符' }}</span></label><input id="account-password" v-model="form.password" type="password" autocomplete="new-password" :required="!form.id" minlength="6" maxlength="72" :placeholder="form.id ? '留空则不修改' : '设置初始密码'"><p v-if="form.id === session.userId" class="field-help">修改自己的角色、停用状态或密码后，需要重新登录。</p><p v-if="form.disabled" class="field-help warning">停用后，该账号将无法登录或继续使用已有会话。</p><p v-if="formError" class="error" role="alert">{{ formError }}</p><div class="editor-actions"><span v-if="dirty">有未保存的修改</span><button class="button primary" :disabled="saving || loading || Boolean(loadError)">{{ saving ? '正在保存…' : form.id ? '保存修改' : '创建账号' }}</button></div></fieldset></form>
        </section></div></template><template v-if="activeTab === 'notices'">
      <header class="page-header"><div><span class="eyebrow">COMPANY TOP 10</span><h1>公司十大重要事项<span class="heading-dot">.</span></h1><p>集中发布、排序与维护，最多 10 条启用事项，全体员工只读查看。</p></div><div class="account-total"><strong>{{ enabledNoticeCount }}</strong><span>条启用 · 上限 10</span></div></header>
      <p v-if="noticeSuccess" class="success" role="status">✓ {{ noticeSuccess }}</p><p v-if="noticeError" class="error" role="alert">{{ noticeError }}</p>
      <div class="account-grid"><section class="panel directory"><div class="section-heading"><h2>全部事项 <span>{{ notices.length }}</span></h2><button class="button secondary small" @click="loadNotices" :disabled="noticeLoading || noticeSaving">{{ noticeLoading ? '刷新中…' : '↻ 刷新' }}</button></div><p v-if="noticeLoading && !notices.length" class="empty-state" role="status">正在读取事项…</p><p v-else-if="!notices.length" class="empty-state">暂无公司事项，右侧可发布第一条。</p><ul v-else class="notice-list"><li v-for="notice in notices" :key="notice.id" class="notice-row" :class="{ selected: noticeForm.id === notice.id }" @click="chooseNotice(notice)"><div class="notice-main"><strong>{{ notice.sortOrder }}. {{ notice.title }}</strong><span class="notice-state" :class="{ disabled: !notice.enabled }">{{ notice.enabled ? '已在列' : '已停用' }}</span></div><button class="text-button" :disabled="noticeSaving" @click.stop="toggleNotice(notice)">{{ notice.enabled ? '停用' : '启用' }}</button></li></ul></section>
        <section class="panel account-editor"><div class="editor-heading"><div><span class="eyebrow">{{ noticeForm.id ? `NOTICE / ${noticeForm.id}` : 'NEW NOTICE' }}</span><h2>{{ noticeForm.id ? '编辑事项' : '发布事项' }}</h2></div><button v-if="noticeForm.id" class="button secondary small" @click="chooseNotice()" :disabled="noticeSaving">＋ 新建</button></div><p class="muted">{{ noticeForm.id ? '修改事项内容、排序或状态。' : '标题与内容将展示给全体员工。' }}</p>
          <form @submit.prevent="saveNotice"><fieldset :disabled="noticeSaving"><label for="notice-title">标题</label><input id="notice-title" v-model="noticeForm.title" maxlength="128" required placeholder="例如：本月安全巡查"><label for="notice-content">内容</label><textarea id="notice-content" v-model="noticeForm.content" rows="6" required placeholder="填写事项说明"></textarea><div class="form-pair"><div><label for="notice-order">排序 <span>数字越小越靠前</span></label><input id="notice-order" v-model.number="noticeForm.sortOrder" type="number" min="0" max="999"></div><div><label for="notice-enabled">状态</label><select id="notice-enabled" v-model="noticeForm.enabled"><option :value="true">启用</option><option :value="false">停用</option></select></div></div><div class="editor-actions"><button class="button primary" :disabled="noticeSaving || noticeLoading">{{ noticeSaving ? '正在保存…' : noticeForm.id ? '保存事项' : '发布事项' }}</button></div></fieldset></form>
        </section></div></template><footer class="page-footer"><span>PANDORA / 管理空间</span><span>账号与公司事项集中维护。</span></footer>
    </main>
  </div>
</template>
