<template>
  <el-button
    :class="['custom-button', `custom-button--${type}`]"
    v-bind="$attrs"
    @click="$emit('click', $event)"
  >
    <slot />
  </el-button>
</template>

<script>
export default {
  name: 'CustomButton',
  props: {
    type: {
      type: String,
      default: 'default',
      validator: (val) => ['default', 'confirm', 'delete', 'add'].includes(val)
    }
  }
};
</script>

<style lang="scss" scoped>
// default = secondary (outlined), confirm/add = primary (ink), delete = destructive
.custom-button {
  width: fit-content;
  height: 36px;
  padding: 0 16px;
  border-radius: var(--x-radius-pill);
  font-weight: 500;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;

  &.el-button--small {
    height: 32px;
    padding: 0 14px;
    font-size: 13px;
  }
  &.el-button--medium {
    height: 38px;
    padding: 0 18px;
  }

  &.is-disabled,
  &.is-disabled:hover,
  &.is-disabled:focus {
    background: var(--x-divider);
    border-color: var(--x-divider);
    color: var(--x-text-disabled);
    cursor: not-allowed;
  }
}

.custom-button--default {
  background: var(--x-surface);
  border: 1px solid var(--x-border-control);
  color: var(--x-ink);

  &:hover,
  &:focus {
    background: var(--x-surface-raised);
    border-color: var(--x-text-disabled);
    color: var(--x-ink);
  }
}

.custom-button--confirm,
.custom-button--add {
  background: var(--x-ink);
  border: 1px solid var(--x-ink);
  color: var(--x-on-ink);

  &:hover,
  &:focus {
    background: var(--x-ink-hover);
    border-color: var(--x-ink-hover);
    color: var(--x-on-ink);
  }

  &:active {
    background: var(--x-ink-pressed);
    border-color: var(--x-ink-pressed);
  }
}

.custom-button--delete {
  background: var(--x-danger);
  border: 1px solid var(--x-danger);
  color: #ffffff;

  &:hover,
  &:focus {
    background: var(--x-danger-text);
    border-color: var(--x-danger-text);
    color: #ffffff;
  }
}
</style>
