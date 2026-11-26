import test from 'node:test';
import assert from 'node:assert/strict';
import { editableProfile, profileForSave, phoneError } from '../src/admin-state.js';

test('legacy nullable profiles can be opened and saved without inventing a phone', () => {
  const form = { id: 5, ...editableProfile({ displayName: null, phone: null, idCard: null, address: null }) };
  assert.equal(form.displayName.trim(), '');
  assert.equal(form.phone, '');
  assert.equal(form.idCard, '');
  assert.equal(phoneError(form), '');
  assert.equal('phone' in profileForSave(form), false);
  assert.equal(profileForSave(form).idCard, '');
});

test('new users require a valid phone and edits reject malformed nonempty phones', () => {
  assert.ok(phoneError({ id: null, phone: '' }));
  assert.ok(phoneError({ id: 5, phone: '123' }));
  assert.equal(phoneError({ id: null, phone: ' 13800000000 ' }), '');
  assert.equal(profileForSave({ id: 5, phone: ' 13800000000 ', address: ' 新地址 ' }).phone, '13800000000');
  assert.equal(profileForSave({ id: 5, address: ' 新地址 ' }).address, '新地址');
});

test('editing unrelated account details does not validate or submit the unchanged legacy contact number', () => {
  const source = { id: 5, phone: '010-12345678' };
  const form = { ...source, displayName: 'New name' };
  assert.equal(phoneError(form, source), '');
  assert.equal('phone' in profileForSave(form, source), false);
  assert.ok(phoneError({ ...form, phone: '020-12345678' }, source));
});

test('editing a staff profile preserves and can clear the emergency contact independently of the account phone', () => {
  const source = { id: 5, phone: '13800000000', emergencyContact: '同事', emergencyPhone: '010-12345678' };
  const form = { id: source.id, ...editableProfile(source) };
  assert.equal(form.emergencyPhone, '010-12345678');
  const next = profileForSave({ ...form, emergencyContact: ' 新联系人 ', emergencyPhone: ' ' }, source);
  assert.equal(next.emergencyContact, '新联系人');
  assert.equal(next.emergencyPhone, '');
  assert.equal('phone' in next, false);
});
