export const roles = [
  { value: 'ADMIN', label: '管理员' },
  { value: 'FOUNDER', label: '创始人' },
  { value: 'DEPT_HEAD', label: '部门负责人' },
  { value: 'TEAM_LEAD', label: '团队负责人' },
  { value: 'STAFF', label: '员工' },
];
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
  const { id, username, displayName, role, managerId, disabled, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address } = form;
  return { ...scope, form: Object.fromEntries(Object.entries({ id, username, displayName, role, managerId, disabled, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address }).filter(([, value]) => value !== undefined)) };
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
  if (form.id && (!profile.phone || (original?.id === form.id && profile.phone === (original.phone ?? '').trim()))) delete profile.phone;
  profile.birthDate ||= null;
  profile.hireDate ||= null;
  return profile;
}
