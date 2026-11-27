import test from 'node:test';
import assert from 'node:assert/strict';
import { cashSubmission } from '../src/cash-submission.js';

const browserStorage = () => {
  const data = new Map();
  return { getItem: key => data.get(key) ?? null, setItem: (key, value) => data.set(key, value), removeItem: key => data.delete(key) };
};
const receipt = { direction: 'RECEIPT', amountCents: 300000, bookedOn: '2026-11-27', note: '参访到账' };

test('lost response keeps the exact request across a page reload, then a new receipt gets a new ID', async () => {
  const storage = browserStorage();
  const sent = [];
  const first = cashSubmission(storage, 7, async draft => {
    sent.push(draft);
    throw new Error('response lost');
  });
  const original = first.prepare(42, receipt);
  await assert.rejects(first.submit(original), /response lost/);
  const reloaded = cashSubmission(storage, 7, async draft => { sent.push(draft); });
  assert.deepEqual(reloaded.pending(), original);
  assert.equal(cashSubmission(storage, 8).pending(), null);
  assert.throws(() => reloaded.prepare(43, receipt), /待确认/);
  await reloaded.submit(reloaded.prepare(42, { ...receipt, amountCents: 100 }));
  assert.deepEqual(sent, [original, original]);
  assert.equal(reloaded.pending(), null);
  assert.notEqual(reloaded.prepare(42, receipt).body.requestId, original.body.requestId);
});

test('validated rejection releases the draft, but a session expiry preserves it', async () => {
  const storage = browserStorage();
  let status = 401;
  const submission = cashSubmission(storage, 7, async () => { throw Object.assign(new Error('request rejected'), { status }); });
  const draft = submission.prepare(42, receipt);
  await assert.rejects(submission.submit(draft), { status: 401 });
  assert.deepEqual(submission.pending(), draft);
  status = 409;
  await assert.rejects(submission.submit(draft), { status: 409 });
  assert.equal(submission.pending(), null);
});
