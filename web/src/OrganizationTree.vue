<script setup>
import { ref } from 'vue';
import { roles, departmentLabel } from './admin-state';

defineProps({ nodes: { type: Array, required: true }, searching: Boolean, root: Boolean });
const emit = defineEmits(['edit']);
const expanded = ref({});
const key = node => node.user?.id ?? 'unassigned';
const isOpen = (node, searching) => searching || (expanded.value[key(node)] ?? node.user?.role === 'FOUNDER');
const roleName = role => roles.find(item => item.value === role)?.label ?? role;
</script>

<template>
  <ul class="report-tree" :class="{ 'report-tree-root': root }">
    <li v-for="node in nodes" :key="key(node)">
      <div class="report-row">
        <button v-if="node.children.length && !searching" type="button" class="report-toggle"
          :aria-expanded="isOpen(node, searching)" :aria-label="`${node.user?.displayName || node.user?.username || '未分配上级'}的下级`"
          @click="expanded[key(node)] = !isOpen(node, searching)">
          <span aria-hidden="true">{{ isOpen(node, searching) ? '▾' : '▸' }}</span>
        </button>
        <span v-else class="report-toggle-spacer" aria-hidden="true"></span>
        <template v-if="node.user">
          <button type="button" class="text-button report-name" @click="emit('edit', node.user)">{{ node.user.displayName || node.user.username }}</button>
          <span class="report-role">{{ roleName(node.user.role) }}</span>
          <span v-if="node.user.role === 'DEPT_HEAD'" class="report-department">{{ departmentLabel(node.user) }}</span>
          <span v-if="node.user.disabled" class="badge disabled">已停用</span>
        </template>
        <strong v-else>未分配有效上级</strong>
        <span v-if="node.children.length && !searching" class="report-count">{{ node.children.length }} 人</span>
      </div>
      <OrganizationTree v-if="node.children.length && isOpen(node, searching)" :nodes="node.children" :searching="searching" @edit="emit('edit', $event)" />
    </li>
  </ul>
</template>
