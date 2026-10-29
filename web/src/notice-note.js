import { reactive } from 'vue';

export function createNoticeNoteEditor({ request, getSession, onExpire }) {
  const empty = () => ({ id: null, content: '', baseline: '', loading: false, saving: false, loaded: false, error: '', success: '' });
  const state = reactive(empty());
  let version = 0;
  function reset() { version++; Object.assign(state, empty()); }
  function context() {
    const token = getSession()?.token, currentVersion = version;
    return { token, current: () => version === currentVersion && token === getSession()?.token && getSession()?.role === 'ADMIN' };
  }
  function fail(error) {
    if ([401, 403].includes(error.status)) onExpire(error);
    else state.error = error.message;
  }
  async function load() {
    if (!state.id || state.loading || getSession()?.role !== 'ADMIN') return;
    const { token, current } = context();
    state.loading = true; state.error = ''; state.loaded = false;
    try {
      const result = await request(`/admin/company-notices/${state.id}/note`, { token });
      if (!current()) return;
      state.content = result.myNote || ''; state.baseline = state.content; state.loaded = true;
    } catch (error) { if (current()) fail(error); }
    finally { if (current()) state.loading = false; }
  }
  async function select(id) {
    if (id === state.id) return;
    reset(); state.id = id ?? null;
    await load();
  }
  async function save() {
    if (!state.id || !state.loaded || state.loading || state.saving || getSession()?.role !== 'ADMIN') return;
    const { token, current } = context(), submitted = state.content;
    state.saving = true; state.error = ''; state.success = '';
    try {
      const result = await request(`/admin/company-notices/${state.id}/note`, { token, method: 'PUT', body: { content: submitted.trim() } });
      if (!current()) return;
      state.baseline = result.myNote || '';
      if (state.content === submitted) state.content = state.baseline;
      state.success = '备注已保存，仅本人可见。';
    } catch (error) { if (current()) fail(error); }
    finally { if (current()) state.saving = false; }
  }
  return { state, select, load, save, reset };
}
