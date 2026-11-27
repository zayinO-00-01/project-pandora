import test from 'node:test';
import assert from 'node:assert/strict';
import * as accounts from '../src/admin-state.js';

const users = [
  { id:1, role:'FOUNDER', username:'founder', displayName:'宋清远' },
  { id:2, role:'DEPT_HEAD', username:'department', displayName:'周衡', departmentName:'技术研发部', managerId:1 },
  { id:3, role:'TEAM_LEAD', username:'lead_backend', displayName:'陆文轩', managerId:2 },
  { id:4, role:'STAFF', username:'dev_1', displayName:'林昕', managerId:3 },
  { id:5, role:'STAFF', username:'dev_2', displayName:'唐婉宁', managerId:2 },
  { id:6, role:'STAFF', username:'unassigned', managerId:99 },
  { id:7, role:'ADMIN', username:'admin' },
];
test('the tree reflects actual managers, including staff who report directly to a department head', () => {
  const [founder, unassigned] = accounts.organizationTree(users);
  const [department] = founder.children;
  assert.equal(department.user.departmentName, '技术研发部');
  assert.deepEqual(department.children.map(n => n.user.id), [3,5]);
  assert.deepEqual(department.children[0].children.map(n => n.user.id), [4]);
  assert.deepEqual(unassigned.children.map(n => n.user.id), [6]);
});
test('search keeps the reports-to path for a matching member and hides unrelated branches', () => {
  const [founder] = accounts.organizationTree(users, 'DEV_1');
  assert.equal(founder.user.id,1);
  assert.deepEqual(founder.children[0].children.map(n => n.user.id), [3]);
  assert.deepEqual(founder.children[0].children[0].children.map(n => n.user.id), [4]);
  assert.equal(accounts.organizationTree(users, 'admin').length, 0);
});
test('account validation rejects special characters and duplicate names but excludes the record being edited', () => {
  assert.match(accounts.accountInputError({username:'sam-1', displayName:'欧阳明远'}, users), /账号/);
  assert.match(accounts.accountInputError({username:'Sam_1', displayName:'陆2'}, users), /姓名/);
  assert.match(accounts.accountInputError({username:'DEV_1', displayName:'Sam'}, users), /账号.*已/);
  assert.match(accounts.accountInputError({username:'Sam_1', displayName:'陆文轩'}, users), /姓名.*已/);
  assert.equal(accounts.accountInputError({id:3, username:'lead_backend', displayName:'陆文轩'}, users), '');
  assert.equal(accounts.accountInputError({username:'Sam_1', displayName:'Sam'}, users), '');
});
