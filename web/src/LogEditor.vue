<script setup>
import { ref, watch, computed } from 'vue';
import { api, today, firstWritableDate, canWriteLog } from './api';
const props = defineProps({ log: Object, token: String, userId: Number, recovery: Object });
const emit = defineEmits(['saved', 'expired', 'busy']);
const date = ref(today()), content = ref(''), pending = ref(false), error = ref('');
watch(() => [props.log, props.recovery], ([log, pending]) => { date.value = pending?.logDate || log?.logDate || today(); content.value = pending?.content ?? log?.content ?? ''; error.value = ''; }, { immediate: true });
const submitted = computed(() => props.log?.status === 'submitted');
const readOnly = computed(() => !!props.log && !canWriteLog(props.log.logDate));
async function save(status) {
  if (pending.value) return;
  error.value = '';
  if (props.log && !canWriteLog(props.log.logDate)) { error.value = '历史日志已超出修改期限，仅供查看。'; return; }
  if (!canWriteLog(date.value) || !content.value.trim()) { error.value = '请选择七天前至今天的日期，并填写日志正文。'; return; }
  pending.value = true; emit('busy', true);
  try {
    const log = await api(props.log ? `/logs/${props.log.id}` : '/logs', {
      token: props.token, method: props.log ? 'PUT' : 'POST', body: { logDate: date.value, content: content.value, status },
    });
    emit('saved', log);
  } catch (e) {
    error.value = e.message;
    if (e.status === 401) {
      sessionStorage.setItem('pandora-unsaved', JSON.stringify({
        userId: props.userId, logId: props.log?.id ?? null,
        logDate: date.value, content: content.value,
      }));
      emit('expired', e.message + ' 未保存内容将在同一账号登录后恢复。');
    }
  }
  finally { pending.value = false; emit('busy', false); }
}
</script>
<template>
  <section class="editor panel">
    <div class="detail-top"><span class="eyebrow">PERSONAL JOURNAL</span><span class="badge" :class="{ draft: !submitted }">{{ submitted ? '已提交' : '草稿' }}</span></div>
    <h2>{{ readOnly ? '查看历史日志' : log ? '编辑这篇日志' : '记录今天的工作' }}</h2>
    <p class="muted">{{ readOnly ? '此记录已超出修改期限，完整内容仍可查看。' : '仅可更新七天前至今天的日志。' }}</p>
    <form @submit.prevent="save('submitted')">
      <label for="log-date">日志日期</label><input id="log-date" type="date" v-model="date" :min="readOnly ? undefined : firstWritableDate()" :max="today()" :disabled="pending || readOnly" required>
      <label for="log-content">工作记录</label><textarea id="log-content" v-model="content" maxlength="5000" :readonly="readOnly" :disabled="pending" required placeholder="今天完成了什么？遇到了哪些问题？接下来准备做什么？"></textarea>
      <div class="editor-meta"><span>保留你自己的记录方式</span><span>{{ content.length }} / 5000</span></div>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <div v-if="!readOnly" class="editor-actions"><button v-if="!submitted" type="button" class="button secondary" :disabled="pending" @click="save('draft')">保存草稿</button><button type="submit" class="button primary" :disabled="pending">{{ pending ? '正在保存…' : submitted ? '保存修改' : '提交日志' }}</button></div>
    </form>
  </section>
</template>
