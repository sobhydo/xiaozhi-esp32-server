<template>
  <el-dialog :visible.sync="visible" width="400px" custom-class="view-password-dialog" :show-close="false">
    <div slot="title" class="dialog-head">
      <span class="head-icon" aria-hidden="true"><i class="el-icon-lock"></i></span>
      <span class="head-title">New User Password</span>
    </div>
    <div class="dialog-body">
      <label class="field-label" for="view-password-input">New User Password</label>
      <el-input id="view-password-input" v-model="password" type="text" :readonly="true" class="password-input" />
    </div>
    <div slot="footer" class="dialog-actions">
      <el-button @click="closeDialog">Close</el-button>
      <el-button type="primary" icon="el-icon-document-copy" @click="copyPassword">Copy Password</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ViewPasswordDialog',
  props: {
    visible: { type: Boolean, required: true },
    password: { type: String, default: '' }
  },
  methods: {
    closeDialog() {
      this.$emit('update:visible', false)
    },
    copyPassword() {
      navigator.clipboard.writeText(this.password)
      this.$message.success({
        message: 'Password copied',
        showClose: true
      })
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .view-password-dialog {
  max-width: calc(100vw - 32px);

  .el-dialog__header {
    padding: 20px 24px 0;
  }

  .el-dialog__body {
    padding: 16px 24px 8px;
  }

  .el-dialog__footer {
    padding: 12px 24px 20px;
  }
}

.dialog-head {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.head-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--x-selected);
  color: var(--x-ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.head-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

.dialog-body {
  text-align: left;
}

.field-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text);
}

.password-input ::v-deep .el-input__inner {
  background: var(--x-surface-raised);
  font-family: var(--x-font-mono);
  font-weight: 600;
  color: var(--x-text);
  cursor: default;
}

.dialog-actions {
  display: flex;
  gap: 8px;

  .el-button {
    flex: 1;
  }

  .el-button + .el-button {
    margin-left: 0;
  }
}
</style>
