function requestId() {
  // getRandomValues is also available on HTTP demo deployments.
  const bytes = globalThis.crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function cashSubmission(storage, userId, send) {
  const key = `pandora:crm:pending-cash:${userId}`;
  function pending() {
    try { return JSON.parse(storage.getItem(key)); }
    catch { return null; }
  }
  function prepare(serviceId, body) {
    const existing = pending();
    if (existing) {
      if (existing.serviceId !== serviceId) throw new Error('请先处理待确认的流水。');
      return existing;
    }
    const draft = { serviceId, body: { ...body, requestId: requestId() } };
    try { storage.setItem(key, JSON.stringify(draft)); }
    catch { throw new Error('无法保存待确认流水，请检查浏览器存储设置后重试。'); }
    return draft;
  }
  async function submit(draft) {
    try { await send(draft); }
    catch (error) {
      // A transport error (or failed login on a retry) doesn't tell us whether it was saved.
      if ([400, 404, 409].includes(error.status)) storage.removeItem(key);
      throw error;
    }
    storage.removeItem(key);
  }
  return { pending, prepare, submit };
}
