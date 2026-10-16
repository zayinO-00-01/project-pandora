import test from 'node:test';
import assert from 'node:assert/strict';
import { eligibleManagers, draftForStorage, restoreDraft } from '../src/admin-state.js';

test('manager choices follow business hierarchy and exclude disabled accounts and self', () => {
  const users = [
    { id: 1, role: 'ADMIN' }, { id: 2, role: 'FOUNDER' },
    { id: 3, role: 'DEPT_HEAD' }, { id: 4, role: 'TEAM_LEAD' },
    { id: 5, role: 'STAFF' }, { id: 6, role: 'FOUNDER', disabled: true },
  ];
  assert.deepEqual(eligibleManagers(users, 'TEAM_LEAD', 4).map(u => u.id), [2, 3]);
  assert.deepEqual(eligibleManagers(users, 'STAFF', 5).map(u => u.id), [2, 3, 4]);
  assert.deepEqual(eligibleManagers(users, 'FOUNDER', 2), []);
  assert.deepEqual(eligibleManagers(users, 'ADMIN', 1), []);
});

test('recovery preserves account form only for the same administrator and server without caching passwords', () => {
  const scope = { server: 'https://pandora.example', userId: 12 };
  const form = { id: 21, username: 'staff-new', displayName: '新同事', role: 'STAFF', managerId: '4', disabled: false, password: 'never-cache-this' };
  const cached = draftForStorage(scope, form);
  assert.equal(JSON.stringify(cached).includes(form.password), false);
  assert.deepEqual(restoreDraft(cached, scope), { ...form, password: '' });
  assert.equal(restoreDraft(cached, { ...scope, userId: 13 }), null);
  assert.equal(restoreDraft(cached, { ...scope, server: 'https://another.example' }), null);
});
