import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { ref, reactive, computed } from 'vue';
import * as adminState from '../src/admin-state.js';
import { createNoticeNoteEditor } from '../src/notice-note.js';

// Execute the actual component setup, replacing only browser lifecycle and transport.
const script = readFileSync(new URL('../src/App.vue', import.meta.url), 'utf8')
  .match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\r?\n/gm, '');
function setup() {
  const pending = [], storage = new Map();
  let confirmations = 0;
  const dependencies = {
    ref, reactive, computed, watch() {}, onMounted() {}, onBeforeUnmount() {},
    api: (path, options) => new Promise((resolve, reject) => pending.push({ path, options, resolve, reject })),
    timeLabel() {}, ...adminState, createNoticeNoteEditor,
    window: { location: { origin: 'https://example.test' }, confirm: () => { confirmations++; return true; } },
    sessionStorage: { getItem: key => storage.get(key), setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) },
  };
  const app = new Function(...Object.keys(dependencies), `${script}\nreturn { session, notices, noticeLoading, noticeSaving, noticeError, noticeSuccess, noticeForm, noteState, noteEditor, loadNotices, saveNotice, toggleNotice, chooseNotice, logout, endSession, establish };`)(...Object.values(dependencies));
  app.session.value = { token: 'one', userId: 1, role: 'ADMIN' };
  return { app, pending, confirmations: () => confirmations };
}
const item = (id, title = `notice ${id}`) => ({ id, title, content: 'content', sortOrder: id, enabled: true });
const error401 = () => Object.assign(new Error('old expired'), { status: 401 });
async function newSession(app, pending) {
  app.endSession();
  const login = app.establish({ token: 'two', userId: 2, role: 'ADMIN' });
  pending.at(-1).resolve([{ id: 2, role: 'ADMIN' }]);
  await login;
}

test('a superseded list cannot replace the latest list or clear its loading indicator', async () => {
  const { app, pending } = setup();
  const first = app.loadNotices(), second = app.loadNotices();
  pending[0].resolve([item(1)]); await first;
  assert.deepEqual(app.notices.value, []);
  assert.equal(app.noticeLoading.value, true);
  pending[1].resolve([item(2)]); await second;
  assert.deepEqual(app.notices.value.map(row => row.id), [2]);
});

test('an old list authorization failure cannot expire a newly logged in administrator', async () => {
  const { app, pending } = setup();
  const first = app.loadNotices();
  await newSession(app, pending);
  const current = app.loadNotices();
  pending[0].reject(error401()); await first;
  assert.equal(app.session.value?.token, 'two');
  assert.equal(app.noticeLoading.value, true);
  pending.at(-1).resolve([item(2)]); await current;
});

test('an old save cannot overwrite a new session form or release its save indicator', async () => {
  const { app, pending } = setup();
  Object.assign(app.noticeForm, item(1));
  const first = app.saveNotice();
  await newSession(app, pending);
  Object.assign(app.noticeForm, item(2));
  const current = app.saveNotice();
  pending[0].resolve(item(1, 'old result')); await first;
  assert.equal(app.noticeForm.id, 2);
  assert.equal(app.noticeSaving.value, true);
  pending.at(-1).resolve(item(2)); await current;
});

test('an old toggle authorization failure cannot expire the new session or clear its busy state', async () => {
  const { app, pending } = setup();
  const first = app.toggleNotice(item(1));
  await newSession(app, pending);
  const current = app.toggleNotice(item(2));
  pending[0].reject(error401()); await first;
  assert.equal(app.session.value?.token, 'two');
  assert.equal(app.noticeSaving.value, true);
  pending.at(-1).resolve({ ...item(2), enabled: false }); await current;
});

test('repeated toggles are serialized and a pre-mutation list cannot roll back a successful toggle', async () => {
  const { app, pending } = setup();
  app.notices.value = [item(1)];
  const list = app.loadNotices(), toggle = app.toggleNotice(item(1));
  app.toggleNotice(item(1));
  assert.equal(pending.length, 2);
  pending[1].resolve({ ...item(1), enabled: false }); await toggle;
  pending[0].resolve([item(1)]); await list;
  assert.equal(app.notices.value[0].enabled, false);
  assert.equal(app.noticeSaving.value, false);
});

test('selecting the current notice again preserves unsaved form changes', async () => {
  const { app, pending } = setup();
  app.chooseNotice(item(1));
  pending[0].resolve({ myNote: '' }); await Promise.resolve();
  app.noticeForm.title = 'my unsaved title';
  app.chooseNotice(item(1));
  assert.equal(app.noticeForm.title, 'my unsaved title');
});

test('note saving blocks notice selection and logout without discarding its input', async () => {
  const { app, pending, confirmations } = setup();
  app.chooseNotice(item(1));
  pending[0].resolve({ myNote: 'before' }); await Promise.resolve();
  app.noteState.content = 'in flight input';
  const save = app.noteEditor.save();
  app.chooseNotice(item(2));
  app.logout();
  assert.equal(app.session.value?.token, 'one');
  assert.equal(app.noticeForm.id, 1);
  assert.equal(app.noteState.content, 'in flight input');
  assert.equal(confirmations(), 0);
  pending[1].resolve({ myNote: 'in flight input' }); await save;
  assert.equal(app.noteState.saving, false);
});

test('toggling the selected notice syncs only its enabled field and keeps unsaved text', async () => {
  const { app, pending } = setup();
  Object.assign(app.noticeForm, item(1));
  app.noticeForm.title = 'my draft';
  const toggle = app.toggleNotice(item(1));
  pending[0].resolve({ ...item(1), enabled: false }); await toggle;
  assert.equal(app.noticeForm.title, 'my draft');
  assert.equal(app.noticeForm.enabled, false);
});
