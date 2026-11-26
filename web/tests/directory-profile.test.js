import test from 'node:test';
import assert from 'node:assert/strict';
import * as state from '../src/admin-state.js';

test('ID validates the calendar and checksum and reads the birth date', () => {
  assert.deepEqual(state.inspectIdCard(' 11010519491231002x '), { value: '11010519491231002X', birthDate: '1949-12-31', error: '' });
  assert.equal(state.inspectIdCard('130503900101001').birthDate, '1990-01-01');
  assert.ok(state.inspectIdCard('110105194912310021').error);
  assert.ok(state.inspectIdCard('130503900230001').error);
});

test('profile save preserves unchanged legacy values but validates any replacement', () => {
  const original = { id: 55, idCard: '000000198001010001', emergencyPhone: '123', birthDate: '1980-01-01' };
  const form = { id: 55, ...state.editableProfile(original) };
  assert.equal(state.profileInputError(form, original), '');
  assert.ok(!('idCard' in state.profileForSave(form, original)));
  assert.ok(!('emergencyPhone' in state.profileForSave(form, original)));
  assert.ok(state.profileInputError({ ...form, emergencyPhone: '12012345678' }, original));
  assert.equal(state.profileInputError({ ...form, emergencyPhone: '13812345678' }, original), '');
  assert.equal(state.profileInputError({ ...form, emergencyPhone: '010-12345678' }, original), '');
});

test('the organization roster nests the actual team with its staff', () => {
  const people = [{ id: 1, role: 'FOUNDER' }, { id: 2, role: 'DEPT_HEAD', departmentName: '技术研发部', managerId: 1 },
    { id: 3, role: 'TEAM_LEAD', managerId: 2 }, { id: 4, role: 'STAFF', managerId: 3 }, { id: 5, role: 'STAFF' }];
  const groups = state.organizationGroups(people);
  assert.deepEqual(groups.find(group => group.id === 2)?.rows.map(row => [row.user.id,row.depth]), [[2,0],[3,1],[4,2]]);
  assert.equal(groups.find(group => group.id === 2)?.label,'技术研发部');
  assert.deepEqual(groups.find(group => group.id === 'unassigned')?.rows.map(row => row.user.id),[5]);
});
