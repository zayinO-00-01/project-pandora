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
