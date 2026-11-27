export async function api(path, { token, method = 'GET', body, signal, raw = false } = {}) {
  let response;
  const multipart = body instanceof FormData;
  try {
    response = await fetch(`/api/v1${path}`, {
      method, signal,
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(body && !multipart ? { 'Content-Type': 'application/json' } : {}) },
      ...(body ? { body: multipart ? body : JSON.stringify(body) } : {}),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('连接失败，请检查服务或网络后重试。');
  }
  if (response.ok && raw) return response;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const message = response.status === 401 && path !== '/auth/login'
      ? '登录已失效，请重新登录。'
      : data?.message || (response.status === 401 ? '账号或密码错误。' : `请求失败（${response.status}），请重试。`);
    const error = new Error(message);
    error.status = response.status; throw error;
  }
  return data;
}
export function timeLabel(value) {
  return value ? new Intl.DateTimeFormat('zh-CN', { timeZone: 'Asia/Shanghai', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' }).format(new Date(value)) : '—';
}
