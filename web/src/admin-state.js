export const roles = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'FOUNDER', label: '创始人' },
  { value: 'DEPT_HEAD', label: '部门负责人' },
  { value: 'TEAM_LEAD', label: '团队负责人' },
  { value: 'STAFF', label: '员工' },
];
export const requiresManager = role => ['DEPT_HEAD', 'TEAM_LEAD', 'STAFF'].includes(role);
const rank = { FOUNDER: 0, DEPT_HEAD: 1, TEAM_LEAD: 2, STAFF: 3 };
export function eligibleManagers(users, role, userId) {
  return users.filter(user => !user.disabled && String(user.id) !== String(userId)
    && rank[user.role] !== undefined && rank[user.role] < rank[role]);
}
export function departmentFor(users, person) {
  const byId = users instanceof Map ? users : new Map(users.map(user => [String(user.id), user]));
  const seen = new Set();
  let current = person;
  if (current?.disabled) return null;
  while (current && rank[current.role] !== undefined) {
    if (current.role === 'DEPT_HEAD' && !current.disabled) return current;
    if (!current.managerId || seen.has(String(current.managerId))) return null;
    seen.add(String(current.managerId));
    current = byId.get(String(current.managerId));
  }
  return null;
}
export function draftForStorage(scope, form) {
  const { id, username, displayName, role, managerId, disabled, departmentName, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address } = form;
  return { ...scope, form: Object.fromEntries(Object.entries({ id, username, displayName, role, managerId, disabled, departmentName, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address }).filter(([, value]) => value !== undefined)) };
}
export function restoreDraft(cached, scope) {
  if (!cached || cached.server !== scope.server || cached.userId !== scope.userId
    || !roles.some(role => role.value === cached.form?.role)) return null;
  return { ...cached.form, password: '' };
}

const profileFields = ['phone', 'idCard', 'birthDate', 'gender', 'hireDate', 'emergencyContact', 'emergencyPhone', 'address'];
export function editableProfile(user = {}) {
  return { displayName: user.displayName ?? '', ...Object.fromEntries(profileFields.map(field => [field, user[field] ?? ''])) };
}
export function phoneError(form, original) {
  const phone = (form.phone ?? '').trim();
  if (!form.id && !phone) return '新建账号请填写手机号。';
  if (original && original.id === form.id && phone === (original.phone ?? '').trim()) return '';
  return phone && !/^1[3-9]\d{9}$/.test(phone) ? '手机号须为 11 位有效号码。' : '';
}
export function profileForSave(form, original) {
  const profile = Object.fromEntries(profileFields.map(field => [field, (form[field] ?? '').trim()]));
  if (form.id && !profile.phone) delete profile.phone;
  if (original && original.id === form.id) {
    for (const field of profileFields) {
      if (profile[field] === (original[field] ?? '').trim()) delete profile[field];
    }
  }
  for (const field of ['birthDate','hireDate']) if (profile[field] === '') profile[field] = null;
  if (profile.idCard) profile.idCard = inspectIdCard(profile.idCard).value;
  return profile;
}

export function departmentLabel(department) {
  return department?.departmentName?.trim() || (department ? '未命名部门 #' + department.id : '未分配部门');
}
export function organizationGroups(users) {
  const byId = new Map(users.map(user => [String(user.id), user]));
  const groups = new Map();
  for (const user of users) {
    if (user.role === 'ADMIN') continue;
    const department = departmentFor(byId, user);
    const id = department?.id ?? (user.role === 'FOUNDER' ? 'company' : 'unassigned');
    if (!groups.has(id)) groups.set(id, { id, label: department ? departmentLabel(department) : id === 'company' ? '公司管理' : '未分配部门', members: [] });
    groups.get(id).members.push(user);
  }
  const order = id => id === 'company' ? 0 : id === 'unassigned' ? 2 : 1;
  return [...groups.values()].sort((a,b) => order(a.id) - order(b.id) || a.label.localeCompare(b.label,'zh-CN')).map(({members,...group}) => {
    const ids = new Set(members.map(u => u.id));
    const children = new Map();
    for (const user of members) {
      const parent = ids.has(user.managerId) ? user.managerId : null;
      if (!children.has(parent)) children.set(parent,[]);
      children.get(parent).push(user);
    }
    const seen = new Set(), rows = [];
    function visit(user,depth) {
      if (seen.has(user.id)) return;
      seen.add(user.id); rows.push({ user,depth });
      for (const child of children.get(user.id) ?? []) visit(child,depth+1);
    }
    for (const root of children.get(null) ?? []) visit(root,0);
    for (const user of members) visit(user,0);
    return { ...group,rows };
  });
}

const idWeights = [7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2];
export function inspectIdCard(input) {
  const value = (input ?? '').trim().toUpperCase();
  if (!value) return { value, birthDate: '', error: '' };
  const invalid = error => ({ value, birthDate: '', error });
  if (!/^[0-9]{17}[0-9X]$/.test(value) || value.startsWith('000000')) return invalid('请输入 18 位有效身份证号，末位可为数字或 X。');
  if (value[17] !== '10X98765432'[idWeights.reduce((sum, weight, i) => sum + Number(value[i]) * weight, 0) % 11])
    return invalid('身份证校验位不正确。');
  const digits = value.slice(6,14);
  const birthDate = digits.slice(0,4) + '-' + digits.slice(4,6) + '-' + digits.slice(6);
  const parsed = new Date(birthDate + 'T00:00:00Z');
  const now = new Date();
  const today = [now.getFullYear(),String(now.getMonth()+1).padStart(2,'0'),String(now.getDate()).padStart(2,'0')].join('-');
  if (digits.startsWith('0000') || Number.isNaN(parsed.valueOf()) || parsed.toISOString().slice(0,10) !== birthDate || birthDate > today)
    return invalid('身份证中的出生日期无效。');
  return { value,birthDate,error: '' };
}
export function fieldChanged(form, original, field) {
  return !original || original.id !== form.id || (form[field] ?? '').trim() !== (original[field] ?? '').trim();
}
export function emergencyPhoneError(form, original) {
  const phone = (form.emergencyPhone ?? '').trim();
  return phone && fieldChanged(form,original,'emergencyPhone') && !/^(1[3-9]\d{9}|0[1-9]\d{1,2}[- ]?[1-9]\d{6,7})$/.test(phone)
    ? '紧急联系电话请输入有效的 11 位手机号或带区号的座机。' : '';
}
export function profileInputError(form, original) {
  if (!(form.displayName ?? '').trim()) return '请填写姓名。';
  if (!(form.phone ?? '').trim()) return '请填写账号手机号。';
  const phone = phoneError({ phone: form.phone });
  if (phone) return phone;
  const identity = inspectIdCard(form.idCard);
  if (!identity.value) return '请填写身份证号。';
  if (identity.error) return identity.error;
  if (requiresManager(form.role) && !form.managerId) return '请选择直属上级。';
  return emergencyPhoneError(form, original);
}
