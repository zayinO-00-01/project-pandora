<script setup>
import { computed } from 'vue';
import { roles, departmentLabel } from './admin-state';

const props = defineProps({
  nodes: { type: Array, required: true },
  departmentIds: { type: Array, default: () => [] },
  departmentColors: Map,
  nested: Boolean,
});
const emit = defineEmits(['edit']);
const colors = [
  ['#34644b', '#94b29a', '#eff5ec'],
  ['#345e82', '#9ebad1', '#edf4f9'],
  ['#894b35', '#d1a48f', '#faf0e9'],
  ['#6d527d', '#baa7c6', '#f5eff8'],
  ['#766025', '#c6b57b', '#f9f4e5'],
  ['#286666', '#90b9b7', '#eaf5f3'],
];
const palette = computed(() => {
  if (props.departmentColors) return props.departmentColors;
  const ids = new Set(props.departmentIds.map(String));
  function collect(nodes) {
    for (const node of nodes) {
      if (node.user?.role === 'DEPT_HEAD') ids.add(String(node.user.id));
      collect(node.children);
    }
  }
  collect(props.nodes);
  return new Map([...ids].map((id, index) => {
    const [ink, outline, fill] = colors[index % colors.length];
    return [id, { '--department-ink': ink, '--department-outline': outline, '--department-fill': fill }];
  }));
});
const name = user => user.displayName || user.username;
const roleName = role => roles.find(item => item.value === role)?.label ?? role;
const isStaff = node => node.user?.role === 'STAFF' && !node.children.length;
const branches = computed(() => {
  const staff = props.nested ? props.nodes.filter(isStaff) : [];
  if (staff.length < 2) return props.nodes.map(node => ({ node }));
  // Leaf siblings share a branch line so large teams fit in a narrow column.
  const branches = props.nodes.filter(node => !isStaff(node)).map(node => ({ node }));
  return [...branches, { staff }];
});
</script>

<template>
  <component :is="nested ? 'div' : 'section'"
    :class="nested ? 'org-chart-children' : 'org-chart-scroll'"
    :tabindex="nested ? undefined : 0" :aria-label="nested ? undefined : '组织架构图'">
    <ul class="org-chart-siblings" :class="{ 'org-chart-roots': !nested }">
      <li v-for="branch in branches" :key="branch.staff ? 'staff' : branch.node.user?.id ?? 'unassigned'" class="org-chart-branch">
        <ul v-if="branch.staff" class="org-chart-staff">
          <li v-for="{ user } in branch.staff" :key="user.id">
            <button type="button" class="org-chart-person org-chart-person-staff" @click="emit('edit', user)">
              <strong>{{ name(user) }}</strong>
              <span>{{ roleName(user.role) }}</span>
              <span v-if="user.disabled" class="org-chart-disabled">已停用</span>
            </button>
          </li>
        </ul>
        <div v-else class="org-chart-group"
          :class="{ 'org-chart-department': branch.node.user?.role === 'DEPT_HEAD' }"
          :style="palette.get(String(branch.node.user?.id))">
          <h3 v-if="branch.node.user?.role === 'DEPT_HEAD'" class="org-chart-department-name">{{ departmentLabel(branch.node.user) }}</h3>
          <button v-if="branch.node.user" type="button" class="org-chart-person"
            :class="`org-chart-person-${branch.node.user.role.toLowerCase()}`"
            @click="emit('edit', branch.node.user)">
            <strong>{{ name(branch.node.user) }}</strong>
            <span>{{ roleName(branch.node.user.role) }}</span>
            <span v-if="branch.node.user.disabled" class="org-chart-disabled">已停用</span>
          </button>
          <strong v-else class="org-chart-unassigned">未分配有效上级</strong>
          <OrganizationTree v-if="branch.node.children.length" :nodes="branch.node.children" :department-colors="palette" nested
            @edit="emit('edit', $event)" />
        </div>
      </li>
    </ul>
  </component>
</template>

<style scoped>
.org-chart-scroll {
  --org-chart-line: #9aab9f;
  max-width: 100%;
  overflow-x: auto;
  background: #f7f8f3;
  border: 1px solid #e1e6dd;
  border-radius: 8px;
}
.org-chart-scroll:focus-visible {
  outline: 2px solid var(--green);
  outline-offset: 2px;
}
.org-chart-siblings, .org-chart-staff {
  margin: 0;
  list-style: none;
  padding: 0;
}
.org-chart-siblings {
  display: flex;
  align-items: stretch;
}
.org-chart-roots {
  width: max-content;
  min-width: 100%;
  justify-content: center;
  gap: 16px;
  padding: 22px 10px;
}
.org-chart-branch {
  position: relative;
  display: flex;
  justify-content: center;
  flex: none;
  padding-inline: 4px;
}
.org-chart-group {
  display: flex;
  width: 100%;
  flex-direction: column;
  align-items: center;
}
.org-chart-department {
  --org-chart-line: var(--department-outline);
  min-width: 130px;
  padding: 9px 7px 11px;
  border: 1px solid var(--department-outline);
  border-radius: 8px;
  background: var(--department-fill);
}
.org-chart-department-name {
  max-width: 180px;
  margin: 0 0 7px;
  color: var(--department-ink);
  font-size: 13px;
  line-height: 1.45;
  text-align: center;
  overflow-wrap: anywhere;
}
.org-chart-person {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 84px;
  min-height: 44px;
  padding: 5px 6px;
  border: 1px solid var(--department-outline, #c7d1c6);
  border-radius: 5px;
  background: #fff;
  color: #233b32;
  font-size: 12px;
  line-height: 1.35;
  text-align: center;
  overflow-wrap: anywhere;
}
.org-chart-person:hover {
  border-color: var(--department-ink, #426b57);
  background: #f4f6f1;
}
.org-chart-person strong {
  max-width: 100%;
}
.org-chart-person span {
  color: #52685e;
  font-size: 10px;
}
.org-chart-person .org-chart-disabled {
  color: #805f45;
}
.org-chart-person-founder {
  width: 144px;
  min-height: 54px;
  background: var(--green);
  border-color: var(--green);
  color: #fff;
  font-size: 15px;
}
.org-chart-person-founder:hover {
  background: #2d5749;
}
.org-chart-person-founder span {
  color: #dbe8de;
  font-size: 11px;
}
.org-chart-person-founder .org-chart-disabled {
  color: #efc8a5;
}
.org-chart-person-dept_head {
  width: 110px;
  color: var(--department-ink);
  font-size: 13px;
}
.org-chart-person-staff {
  width: 74px;
  min-height: 36px;
  padding-block: 4px;
}
.org-chart-unassigned {
  padding: 10px 12px;
  border: 1px dashed #abb8a9;
  border-radius: 5px;
  color: #52685e;
  font-size: 12px;
}

.org-chart-children {
  position: relative;
  padding-top: 10px;
}
.org-chart-children::before, .org-chart-children > ul > li::after {
  position: absolute;
  left: 50%;
  top: 0;
  height: 10px;
  border-left: 1px solid var(--org-chart-line);
  content: '';
}
.org-chart-children > ul > li {
  padding-top: 10px;
}
.org-chart-children > ul > li::before {
  position: absolute;
  inset: 0 0 auto;
  border-top: 1px solid var(--org-chart-line);
  content: '';
}
.org-chart-children > ul > li:first-child::before { left: 50%; }
.org-chart-children > ul > li:last-child::before { right: 50%; }
.org-chart-children > ul > li:only-child::before { content: none; }

.org-chart-staff {
  position: relative;
  width: 84px;
  padding: 7px 0 0 10px;
}
.org-chart-staff::before {
  position: absolute;
  inset: 0 50% auto 0;
  height: 7px;
  border-top: 1px solid var(--org-chart-line);
  border-left: 1px solid var(--org-chart-line);
  content: '';
}
.org-chart-staff > li {
  position: relative;
  margin-bottom: 4px;
}
.org-chart-staff > li::before, .org-chart-staff > li::after {
  position: absolute;
  left: -10px;
  content: '';
}
.org-chart-staff > li::before {
  top: 0;
  bottom: -4px;
  border-left: 1px solid var(--org-chart-line);
}
.org-chart-staff > li:last-child::before { bottom: 50%; }
.org-chart-staff > li::after {
  top: 50%;
  width: 10px;
  border-top: 1px solid var(--org-chart-line);
}
</style>
