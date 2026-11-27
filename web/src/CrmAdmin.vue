<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { api } from './api';
import { cashSubmission } from './cash-submission';

const props = defineProps({ session: Object, capabilities: Object, users: Array });
const emit = defineEmits(['expired']);
const rights = {
  SHOP_IMPORT: '商城订单导入', FINANCE: '收退款与台账', CONTRACTS: '员工档案与合同',
  CUSTOMER_REVIEW: '疑似重复客户复核', CARD_CORRECT: '年卡核销撤销', SERVICE_CORRECT: '服务更正及停用账号交接',
};
const kindNames = {
  VISIT: '企业参访', BANQUET: '私宴', SALON: '沙龙', SHOW: '演出观演', OTHER: '其他服务',
  ANNUAL_CARD: '演出年卡', MEMBERSHIP: '商城会员', GENERAL: '商城订单',
};
const permissions = code => props.capabilities.permissions?.includes(code);
const sections = computed(() => [
  ['imports', '商城导入', permissions('SHOP_IMPORT')],
  ['finance', '财务台账', props.capabilities.revenue],
  ['contracts', '员工与合同', permissions('CONTRACTS')],
  ['review', '客户查重复核', permissions('CUSTOMER_REVIEW')],
  ['grants', '二期授权', props.capabilities.administrator],
].filter(([, , allowed]) => allowed));
const tab = ref(sections.value[0]?.[0]);
const busy = ref(false), error = ref(''), success = ref('');
const shanghaiToday = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Shanghai' }).format(new Date());
const money = cents => (cents / 100).toLocaleString('zh-CN', { style: 'currency', currency: 'CNY' });
const centsFor = yuan => {
  const value = String(yuan).trim();
  if (!/^\d{1,9}(?:\.\d{1,2})?$/.test(value)) throw new Error('金额请填写最多两位小数。');
  const [whole, fraction = ''] = value.split('.');
  return Number(whole) * 100 + Number(fraction.padEnd(2, '0'));
};
const controller = new AbortController();
onBeforeUnmount(() => controller.abort());
const request = (path, options = {}) => api(`/crm${path}`, { signal: controller.signal, ...options, token: props.session.token });
async function run(action) {
  if (busy.value) return;
  busy.value = true; error.value = ''; success.value = '';
  try { await action(); }
  catch (reason) {
    if (reason.name === 'AbortError') return;
    if (reason.status === 401) emit('expired', reason);
    else error.value = reason.message;
  } finally { busy.value = false; }
}
function download(blob, name) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url; link.download = name; link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
const members = ref([]);

const csv = ref(''), importResult = ref(null), importHistory = ref([]), demoOwner = ref(null), matching = ref([]);
const links = ref({});
const match = reactive({ kind: 'PERSON', name: '', phone: '' });
const choice = reactive({ memberKey: '', customerId: null, userId: null, reason: '' });
async function readCsv(event) {
  const file = event.target.files[0];
  csv.value = ''; importResult.value = null; links.value = {}; choice.memberKey = ''; matching.value = [];
  if (!file) return;
  await run(async () => {
    if (file.size > 128000) throw new Error('请选择不超过 128 KB 的 CSV。');
    csv.value = await file.text();
  });
}
function demoCsv() {
  const today = shanghaiToday();
  const header = 'source,memberNumber,orderNumber,product,status,amount,refundAmount,paidOn,refundedOn,name,phone,ownerId';
  const lines = [
    `demo,m001,a001,ANNUAL_CARD,PAID,600,,${today},,演示客户林晴,19900000001,${demoOwner.value}`,
    `demo,m001,m001,MEMBERSHIP,PAID,99,,${today},,演示客户林晴,19900000001,${demoOwner.value}`,
  ];
  download(new Blob(['\uFEFF', header, '\r\n', lines.join('\r\n'), '\r\n'], { type: 'text/csv;charset=utf-8' }), 'CRM-商城订单样例.csv');
}
function importCsv(apply) {
  run(async () => {
    const result = await request('/admin/imports', { method: 'POST', body: { csv: csv.value, links: links.value, apply } });
    importResult.value = result;
    if (result.committed) {
      success.value = '已导入。再次导入相同订单会跳过。';
      importHistory.value = await request('/admin/imports');
    }
  });
}
function findMatch() {
  run(async () => {
    choice.customerId = null; matching.value = [];
    matching.value = await request(tab.value === 'imports' ? '/admin/imports/matches' : '/customer-matches', { method: 'POST', body: match });
    if (!matching.value.length) success.value = '没有查到匹配的客户。';
  });
}
function bindCustomer() {
  const key = choice.memberKey.trim().toLowerCase();
  if (!key || !choice.customerId) return;
  links.value = { ...links.value, [key]: choice.customerId };
  importResult.value = null; success.value = '已选中关联档案，请重新预览订单。';
}
function reviewCustomer() {
  run(async () => {
    await request(`/customer-matches/${choice.customerId}/assign`, { method: 'PUT', body: { userId: choice.userId, reason: choice.reason } });
    choice.reason = ''; success.value = '已关联。该成员可以从客户档案登记自己的服务。';
  });
}

const month = ref(shanghaiToday().slice(0, 7)), revenue = ref(null), receivables = ref([]), cash = ref([]), shopCash = ref([]);
const selectedService = ref(null), due = ref('');
const booking = reactive({ direction: 'RECEIPT', amount: '', bookedOn: shanghaiToday(), note: '' });
async function postCash({ serviceId, body }) {
  const deadline = new AbortController();
  const stop = () => deadline.abort();
  controller.signal.addEventListener('abort', stop, { once: true });
  const timer = setTimeout(stop, 15000);
  try {
    await request(`/admin/receivables/${serviceId}/cash`, { method: 'POST', body, signal: deadline.signal });
  } catch (reason) {
    if (reason.name === 'AbortError' && !controller.signal.aborted) throw new Error('登记超时。请重试确认这笔流水。');
    throw reason;
  } finally {
    clearTimeout(timer);
    controller.signal.removeEventListener('abort', stop);
  }
}
const submission = cashSubmission(window.sessionStorage, props.session.userId, postCash);
const pendingCash = ref(submission.pending());
const retryingCash = computed(() => pendingCash.value?.serviceId === selectedService.value);
async function getRevenue() {
  const filter = `?month=${encodeURIComponent(month.value)}`;
  [revenue.value, shopCash.value] = await Promise.all([
    request(`/admin/revenue${filter}`),
    permissions('FINANCE') ? request(`/admin/shop-cash${filter}`) : [],
  ]);
}
async function getReceivables() {
  if (permissions('FINANCE')) receivables.value = await request('/admin/receivables');
}
function chooseService(row) {
  run(async () => {
    selectedService.value = row.id; cash.value = [];
    due.value = row.receivableCents == null ? '' : (row.receivableCents / 100).toFixed(2);
    const draft = retryingCash.value ? pendingCash.value.body : null;
    Object.assign(booking, draft ? {
      direction: draft.direction, amount: (draft.amountCents / 100).toFixed(2), bookedOn: draft.bookedOn, note: draft.note,
    } : { direction: 'RECEIPT', amount: '', bookedOn: shanghaiToday(), note: '' });
    cash.value = await request(`/admin/receivables/${row.id}/cash`);
  });
}
function openPendingCash() {
  const id = pendingCash.value?.serviceId;
  if (id) chooseService(receivables.value.find(row => row.id === id) ?? { id });
}
function saveDue() {
  run(async () => {
    await request(`/admin/receivables/${selectedService.value}`, { method: 'PUT', body: { amountCents: centsFor(due.value) } });
    success.value = '已保存应收。'; await getReceivables();
  });
}
function addCash() {
  run(async () => {
    const id = selectedService.value;
    const { direction, amount, bookedOn, note } = booking;
    const draft = retryingCash.value ? pendingCash.value
      : submission.prepare(id, { direction, amountCents: centsFor(amount), bookedOn, note });
    pendingCash.value = draft;
    try { await submission.submit(draft); }
    finally { pendingCash.value = submission.pending(); }
    booking.amount = ''; booking.note = ''; success.value = '已登记流水。';
    await Promise.all([getRevenue(), getReceivables(), request(`/admin/receivables/${id}/cash`).then(lines => { cash.value = lines; })]);
  });
}

const staff = ref([]), contracts = ref([]), staffId = ref(null), profile = reactive({ department: '', jobTitle: '' });
const contractForm = ref(null), editingDates = ref(null);
const correction = reactive({ startOn: '', endOn: '', reason: '' });
function chooseStaff() {
  const row = staff.value.find(item => item.id === staffId.value);
  Object.assign(profile, { department: row?.department || '', jobTitle: row?.jobTitle || '' });
  editingDates.value = null;
}
async function loadContracts() {
  [staff.value, contracts.value] = await Promise.all([request('/admin/staff'), request('/admin/contracts')]);
}
function saveProfile() {
  run(async () => {
    await request(`/admin/staff/${staffId.value}`, { method: 'PUT', body: profile });
    success.value = '已保存部门和岗位。'; await loadContracts();
  });
}
function uploadContract() {
  run(async () => {
    const body = new FormData(contractForm.value);
    if (!body.get('file')?.size || body.get('file').size > 1000000) throw new Error('请选择不超过 1 MB 的 PDF。');
    body.set('userId', String(staffId.value));
    await request('/admin/contracts', { method: 'POST', body });
    contractForm.value?.reset(); success.value = '合同已归档。'; await loadContracts();
  });
}
function openPdf(contract) {
  run(async () => {
    const response = await request(`/admin/contracts/${contract.id}/pdf`, { raw: true });
    download(await response.blob(), contract.fileName);
  });
}
function dateForm(contract) {
  editingDates.value = contract.id;
  Object.assign(correction, { startOn: contract.startOn, endOn: contract.endOn, reason: '' });
}
function saveDates() {
  run(async () => {
    await request(`/admin/contracts/${editingDates.value}/dates`, { method: 'PUT', body: correction });
    editingDates.value = null; success.value = '起止日期已更新。'; await loadContracts();
  });
}
function remind() {
  run(async () => {
    const result = await request('/admin/contract-reminders', { method: 'POST' });
    success.value = `本次发送 ${result.sent} 条到期提醒。`;
  });
}

const grantUser = ref(null), grantReady = ref(false), grantCodes = ref([]);
function getGrants() {
  run(async () => {
    grantReady.value = false; grantCodes.value = [];
    if (grantUser.value === null) return;
    grantCodes.value = await request(`/admin/permissions/${grantUser.value}`);
    grantReady.value = true;
  });
}
function saveGrants() {
  run(async () => {
    grantCodes.value = await request(`/admin/permissions/${grantUser.value}`, { method: 'PUT', body: { permissions: grantCodes.value } });
    success.value = '已保存模块权限。';
  });
}

function activate(next) {
  tab.value = next;
  choice.customerId = null; matching.value = [];
  run(async () => {
    if (next === 'imports') [importHistory.value, members.value] = await Promise.all([request('/admin/imports'), request('/members')]);
    if (next === 'review') members.value = await request('/members');
    if (next === 'finance') await Promise.all([getRevenue(), getReceivables()]);
    if (next === 'contracts') await loadContracts();
  });
}
onMounted(() => activate(tab.value));
</script>

<template>
  <section class="crm-admin" aria-label="客户与业务管理">
    <h1>{{ capabilities.administrator ? '二期模块授权' : '客户与业务管理' }}</h1>
    <nav v-if="sections.length > 1" class="crm-actions" aria-label="业务模块">
      <button v-for="[key, name] in sections" :key="key" class="button" :disabled="busy" :aria-pressed="tab === key"
        :class="tab === key ? 'primary' : 'secondary'" @click="activate(key)">{{ name }}</button>
    </nav>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="success" class="success" role="status">{{ success }}</p>
    <p v-if="busy" class="muted" role="status">正在处理…</p>

    <template v-if="tab === 'imports'">
      <div class="panel crm-block">
        <h2>模拟商城订单</h2>
        <p>上传 UTF-8 CSV，每次最多 200 条。订单对照确认后再导入。</p>
        <div class="crm-actions">
          <label>样例负责人<select v-model="demoOwner" :disabled="busy"><option :value="null">请选择</option>
            <option v-for="m in members" :key="m.id" :value="m.id">{{ m.name }}（{{ m.id }}）</option></select></label>
          <button class="button secondary" :disabled="busy || !demoOwner" @click="demoCsv">下载样例 CSV</button>
        </div>
        <label>订单 CSV <input type="file" accept=".csv,text/csv" :disabled="busy" @change="readCsv"></label>
        <div class="crm-actions">
          <button class="button secondary" :disabled="busy || !csv" @click="importCsv(false)">预览订单</button>
          <button class="button primary" :disabled="busy || !importResult?.ready || importResult.committed" @click="importCsv(true)">确认导入</button>
        </div>
        <div v-if="importResult" class="table-scroll">
          <table><thead><tr><th>行</th><th>会员匹配键</th><th>订单号</th><th>结果</th></tr></thead>
            <tbody><tr v-for="row in importResult.rows" :key="row.row"><td>{{ row.row }}</td><td>{{ row.memberKey }}</td><td>{{ row.orderNumber }}</td><td>{{ row.message }}</td></tr></tbody></table>
        </div>
      </div>
      <div class="panel crm-block"><h2>最近导入</h2><p v-if="!importHistory.length">暂无导入记录。</p>
        <ul v-else><li v-for="item in importHistory" :key="item.id">{{ item.recordedAt.replace('T', ' ') }}：{{ item.detail }}</li></ul>
      </div>
    </template>

    <section v-if="tab === 'imports' || tab === 'review'" class="panel crm-block">
      <h2>{{ tab === 'imports' ? '关联已有个人档案' : '疑似重复客户复核' }}</h2>
      <p>按准确的客户名称或电话核对。请先确认是同一客户，再做关联。</p>
      <form class="crm-actions" @submit.prevent="findMatch">
        <label v-if="tab === 'review'">类型<select v-model="match.kind"><option value="PERSON">个人</option><option value="COMPANY">企业</option></select></label>
        <label>客户名称<input v-model="match.name" maxlength="120"></label>
        <label>联系电话<input v-model="match.phone" type="tel" maxlength="30"></label>
        <button class="button secondary" :disabled="busy || (!match.name.trim() && !match.phone.trim())">核对档案</button>
      </form>
      <fieldset v-if="matching.length" :disabled="busy" class="crm-block">
        <label>选择档案<select v-model="choice.customerId"><option :value="null">请选择</option>
          <option v-for="m in matching" :key="m.id" :value="m.id">#{{ m.id }} {{ m.name }} {{ m.contactName }} {{ m.phone }}</option></select></label>
        <form v-if="tab === 'imports'" class="crm-actions" @submit.prevent="bindCustomer">
          <label>预览中的会员匹配键<input v-model="choice.memberKey" placeholder="例如 demo:m001" required maxlength="121"></label>
          <button class="button primary" :disabled="!choice.customerId">选为导入关联</button>
        </form>
        <form v-else class="crm-actions" @submit.prevent="reviewCustomer">
          <label>申请关联的员工<select v-model="choice.userId" required><option :value="null">请选择</option>
            <option v-for="m in members" :key="m.id" :value="m.id">{{ m.name }}</option></select></label>
          <label>核对原因<input v-model.trim="choice.reason" maxlength="500" required></label>
          <button class="button primary" :disabled="!choice.customerId || !choice.userId">关联客户</button>
        </form>
      </fieldset>
      <ul v-if="tab === 'imports' && Object.keys(links).length">
        <li v-for="(customerId, key) in links" :key="key">{{ key }} → 客户 #{{ customerId }}
          <button class="text-button" :disabled="busy" @click="delete links[key]; importResult = null">移除</button>
        </li>
      </ul>
    </section>

    <template v-if="tab === 'finance'">
      <section class="panel crm-block">
        <h2>月度营收</h2>
        <form class="crm-actions" @submit.prevent="run(getRevenue)"><label>月份<input v-model="month" type="month" min="2000-01" max="2099-12" required></label>
          <button class="button secondary" :disabled="busy">查询</button></form>
        <template v-if="revenue">
          <p><strong>{{ revenue.month }} 净实收 {{ money(revenue.net) }}</strong> · 到账 {{ money(revenue.receipts) }} · 退款 {{ money(revenue.refunds) }}</p>
          <ul><li v-for="line in revenue.byKind" :key="line.kind">{{ kindNames[line.kind] }}：到账 {{ money(line.receipts) }}，退款 {{ money(line.refunds) }}</li></ul>
        </template>
      </section>
      <section v-if="permissions('FINANCE')" class="panel crm-block">
        <h2>线下服务台账</h2>
        <p v-if="pendingCash" role="status">
          有一笔流水待确认（服务 #{{ pendingCash.serviceId }}）。
          <button v-if="!retryingCash" type="button" class="text-button" :disabled="busy" @click="openPendingCash">打开待确认流水</button>
          <span v-else>点击「重试确认」可继续登记同一笔流水。</span>
        </p>
        <p v-if="receivables.length === 0">暂无线下服务。</p>
        <div v-else class="table-scroll"><table><thead><tr><th>客户与服务</th><th>应收</th><th>净实收</th><th>操作</th></tr></thead>
          <tbody><tr v-for="row in receivables" :key="row.id"><td>#{{ row.id }} {{ row.customerName }} · {{ kindNames[row.kind] }}</td>
            <td>{{ money(row.receivableCents) }}</td><td>{{ money(row.net) }}</td>
            <td><button class="text-button" :disabled="busy" @click="chooseService(row)">登记／查看流水</button></td>
          </tr></tbody></table></div>
        <fieldset v-if="selectedService" class="crm-block" :disabled="busy"><legend>服务 #{{ selectedService }}</legend>
          <form class="crm-actions" @submit.prevent="saveDue"><label>应收（元）<input v-model="due" inputmode="decimal" required></label>
            <button class="button secondary">保存应收</button></form>
          <form class="crm-actions" @submit.prevent="addCash">
            <label>类型<select v-model="booking.direction" :disabled="!!pendingCash"><option value="RECEIPT">收款</option><option value="REFUND">退款</option></select></label>
            <label>金额（元）<input v-model="booking.amount" :disabled="!!pendingCash" inputmode="decimal" required></label>
            <label>实际发生日期<input v-model="booking.bookedOn" :disabled="!!pendingCash" type="date" :max="shanghaiToday()" required></label>
            <label>款项说明<input v-model.trim="booking.note" :disabled="!!pendingCash" maxlength="500" required></label>
            <button class="button primary" :disabled="!!pendingCash && !retryingCash">{{ retryingCash ? '重试确认' : '登记流水' }}</button>
          </form>
          <ul><li v-for="entry in cash" :key="entry.id">{{ entry.bookedOn }} {{ entry.direction === 'RECEIPT' ? '收款' : '退款' }} {{ money(entry.amountCents) }} · {{ entry.note }}</li></ul>
        </fieldset>
      </section>
      <section v-if="permissions('FINANCE')" class="panel crm-block">
        <h2>商城流水</h2>
        <p v-if="!shopCash.length">当前月份暂无商城流水。</p>
        <div v-else class="table-scroll"><table><thead><tr><th>订单</th><th>商品</th><th>实际发生日期</th><th>款项</th></tr></thead>
          <tbody><tr v-for="row in shopCash" :key="row.id"><td>{{ row.source }} · {{ row.orderNumber }}</td>
            <td>{{ kindNames[row.product] }}</td><td>{{ row.bookedOn }}</td>
            <td>{{ row.direction === 'RECEIPT' ? '到账' : '退款' }} {{ money(row.amountCents) }}</td></tr></tbody>
        </table></div>
      </section>
    </template>

    <template v-if="tab === 'contracts'">
      <section class="panel crm-block">
        <h2>员工部门和岗位</h2>
        <label>员工<select v-model="staffId" :disabled="busy" @change="chooseStaff"><option :value="null">全部员工</option>
          <option v-for="person in staff" :key="person.id" :value="person.id">{{ person.name }}{{ person.disabled ? '（停用）' : '' }}</option></select></label>
        <form v-if="staffId" class="crm-actions" @submit.prevent="saveProfile">
          <label>部门<input v-model="profile.department" maxlength="64"></label>
          <label>岗位<input v-model="profile.jobTitle" maxlength="64"></label>
          <button class="button primary" :disabled="busy">保存档案</button>
        </form>
      </section>
      <section class="panel crm-block">
        <h2>合同归档</h2>
        <button class="button secondary" :disabled="busy" @click="remind">检查 30 天内到期合同</button>
        <form v-if="staffId" ref="contractForm" class="crm-actions" @submit.prevent="uploadContract">
          <label>合同名称<input name="title" maxlength="120" required></label>
          <label>起始日期<input name="startOn" type="date" required></label>
          <label>到期日期<input name="endOn" type="date" required></label>
          <label>PDF（1 MB 以内）<input name="file" type="file" accept="application/pdf,.pdf" required></label>
          <button class="button primary" :disabled="busy">归档合同</button>
        </form>
        <p v-else>选择员工后可上传合同。</p>
        <div class="table-scroll"><table><thead><tr><th>合同</th><th>员工</th><th>起止</th><th>操作</th></tr></thead>
          <tbody><tr v-for="contract in contracts.filter(c => !staffId || c.userId === staffId)" :key="contract.id">
            <td>{{ contract.title }}</td><td>{{ staff.find(u => u.id === contract.userId)?.name }}</td><td>{{ contract.startOn }} 至 {{ contract.endOn }}</td>
            <td><button class="text-button" :disabled="busy" @click="openPdf(contract)">下载 PDF</button>
              <button class="text-button" :disabled="busy" @click="dateForm(contract)">修改日期</button></td>
          </tr></tbody>
        </table></div>
        <form v-if="editingDates" class="crm-actions" @submit.prevent="saveDates">
          <label>起始日期<input v-model="correction.startOn" type="date" required></label>
          <label>到期日期<input v-model="correction.endOn" type="date" required></label>
          <label>更改原因<input v-model.trim="correction.reason" maxlength="500" required></label>
          <button class="button primary" :disabled="busy">保存更改</button>
          <button class="button secondary" :disabled="busy" type="button" @click="editingDates = null">返回</button>
        </form>
      </section>
    </template>

    <section v-if="tab === 'grants'" class="panel crm-block">
      <p>为公司业务账号分配权限。授权不会改变账号的组织层级。</p>
      <label>业务账号<select v-model="grantUser" :disabled="busy" @change="getGrants">
        <option :value="null">请选择</option>
        <option v-for="user in users.filter(user => user.role !== 'ADMIN' && !user.disabled)" :key="user.id" :value="user.id">
          {{ user.displayName || user.username }}（{{ user.username }}）
        </option>
      </select></label>
      <form v-if="grantReady" @submit.prevent="saveGrants">
        <fieldset :disabled="busy" class="crm-block"><legend>模块权限</legend>
          <label v-for="(label, code) in rights" :key="code" class="crm-option"><input v-model="grantCodes" type="checkbox" :value="code">{{ label }}</label>
          <button class="button primary">保存授权</button>
        </fieldset>
      </form>
    </section>
  </section>
</template>

<style scoped>
.crm-admin { display: grid; gap: 14px; }
.crm-block { display: grid; gap: 10px; padding: 16px; min-width: 0; }
.crm-block :is(h1, h2, p, ul), .crm-admin > h1 { margin: 0; }
.crm-actions { display: flex; flex-wrap: wrap; align-items: end; gap: 10px; }
.crm-admin label { display: grid; gap: 4px; max-width: 100%; }
.crm-admin label.crm-option { display: flex; align-items: center; }
.crm-option input { width: auto; margin-right: 6px; }
.crm-admin input[type=file] { max-width: 100%; }
.crm-admin select { max-width: min(100%, 32rem); }
</style>
