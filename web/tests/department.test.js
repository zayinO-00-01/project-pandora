import test from 'node:test';
import assert from 'node:assert/strict';
import * as accounts from '../src/admin-state.js';

test('department follows the selected reporting line and changes when the direct manager changes', () => {
  const users = [
    { id: 1, role: 'FOUNDER' },
    { id: 2, role: 'DEPT_HEAD', displayName: '陈经理', managerId: 1 },
    { id: 3, role: 'DEPT_HEAD', displayName: '吴经理', managerId: 1 },
    { id: 4, role: 'TEAM_LEAD', managerId: 2 },
  ];
  assert.equal(accounts.departmentFor(users, { role: 'STAFF', managerId: '4' }).id, 2);
  const disabledLead = users.map(user => user.id === 4 ? { ...user, disabled: true } : user);
  assert.equal(accounts.departmentFor(disabledLead, { role: 'STAFF', managerId: '4' })?.id, 2);
  assert.equal(accounts.departmentFor(users, { role: 'STAFF', managerId: '3' }).id, 3);
  assert.equal(accounts.departmentFor(users, { id: 3, role: 'DEPT_HEAD', managerId: '1' }).id, 3);
  assert.equal(accounts.departmentFor(users, { role: 'STAFF', managerId: '1' }), null);
});

test('missing or disabled managers and a cycle cannot assign a department', () => {
  const users = [
    { id: 2, role: 'DEPT_HEAD', disabled: true },
    { id: 4, role: 'TEAM_LEAD', managerId: 5 },
    { id: 5, role: 'TEAM_LEAD', managerId: 4 },
  ];
  for (const managerId of [0, 2, 4, 99]) {
    assert.equal(accounts.departmentFor(users, { role: 'STAFF', managerId }), null);
  }
});