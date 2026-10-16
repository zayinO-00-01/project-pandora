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
export function draftForStorage(scope, form) {
  const { id, username, displayName, role, managerId, disabled, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address } = form;
  return { ...scope, form: { id, username, displayName, role, managerId, disabled, phone, idCard, birthDate, gender, hireDate, emergencyContact, emergencyPhone, address } };
}
export function restoreDraft(cached, scope) {
  if (!cached || cached.server !== scope.server || cached.userId !== scope.userId
    || !roles.some(role => role.value === cached.form?.role)) return null;
  return { ...cached.form, password: '' };
}
