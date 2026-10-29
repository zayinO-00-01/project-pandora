import test from 'node:test';
import assert from 'node:assert/strict';
import { createNoticeNoteEditor } from '../src/notice-note.js';

function setup() {
  let session = { token: 'admin-one', role: 'ADMIN' };
  const pending = [], expired = [];
  const editor = createNoticeNoteEditor({
    getSession: () => session,
    request: (path, options) => new Promise((resolve, reject) => pending.push({ path, options, resolve, reject })),
    onExpire: error => expired.push(error),
  });
  return { editor, pending, expired, changeSession: value => { session = value; } };
}

test('switching A to B to A ignores the first A response and its loading completion', async () => {
  const { editor, pending } = setup();
  const first = editor.select(1), second = editor.select(2), third = editor.select(1);
  pending[0].resolve({ myNote: 'obsolete A' }); await first;
  assert.equal(editor.state.content, '');
  assert.equal(editor.state.loading, true);
  pending[1].resolve({ myNote: 'B' }); await second;
  assert.equal(editor.state.loading, true);
  pending[2].resolve({ myNote: 'current A' }); await third;
  assert.equal(editor.state.content, 'current A');
  assert.equal(editor.state.loading, false);
});

test('blank notes are submitted to clear the existing private note', async () => {
  const { editor, pending } = setup();
  const load = editor.select(7);
  pending[0].resolve({ myNote: 'old note' }); await load;
  editor.state.content = '   ';
  const save = editor.save();
  assert.equal(pending[1].path, '/admin/company-notices/7/note');
  assert.deepEqual(pending[1].options.body, { content: '' });
  assert.equal(pending[1].options.method, 'PUT');
  pending[1].resolve({ myNote: '' }); await save;
  assert.equal(editor.state.content, '');
  assert.equal(editor.state.baseline, '');
  assert.equal(editor.state.saving, false);
});

test('a save completing after selection changes leaves the new note intact', async () => {
  const { editor, pending } = setup();
  const first = editor.select(1); pending[0].resolve({ myNote: 'A' }); await first;
  editor.state.content = 'changed A';
  const save = editor.save(), second = editor.select(2);
  pending[2].resolve({ myNote: 'B' }); await second;
  pending[1].resolve({ myNote: 'changed A' }); await save;
  assert.equal(editor.state.content, 'B');
  assert.equal(editor.state.success, '');
});

test('logout invalidates pending responses and stale authorization errors cannot expire a new session', async () => {
  const { editor, pending, expired, changeSession } = setup();
  const first = editor.select(1);
  editor.reset(); changeSession({ token: 'admin-two', role: 'ADMIN' });
  const second = editor.select(1);
  pending[0].reject(Object.assign(new Error('expired old session'), { status: 401 })); await first;
  assert.deepEqual(expired, []);
  assert.equal(editor.state.loading, true);
  pending[1].resolve({ myNote: 'second administrator' }); await second;
  assert.equal(editor.state.content, 'second administrator');
});

test('a failed initial load cannot overwrite an unseen note, and can be retried', async () => {
  const { editor, pending } = setup();
  const first = editor.select(1);
  pending[0].reject(new Error('offline')); await first;
  editor.state.content = 'unsafe replacement';
  await editor.save();
  assert.equal(pending.length, 1);
  assert.equal(editor.state.error, 'offline');
  const retry = editor.load(); pending[1].resolve({ myNote: 'server note' }); await retry;
  assert.equal(editor.state.content, 'server note');
});
