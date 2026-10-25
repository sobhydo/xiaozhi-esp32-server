<template>
  <CustomDialog
    :title="$t('addAgentDialog.title')"
    :visible.sync="visible"
    width="420px"
    @confirm="confirm"
    @cancel="cancel"
    @close="handleClose"
    :confirmText="$t('addAgentDialog.confirm')"
    :cancelText="$t('addAgentDialog.cancel')"
    @open="handleOpen"
  >
    <div class="add-agent-body">
      <label class="add-agent-label" for="add-agent-name">
        {{ $t('addAgentDialog.agentName') }}<span class="required" aria-hidden="true">*</span>
      </label>
      <el-input
        id="add-agent-name"
        maxLength="64"
        ref="inputRef"
        :placeholder="$t('addAgentDialog.placeholder')"
        v-model="wisdomBodyName"
        class="add-agent-input"
        @keyup.enter.native="confirm"
      />
    </div>
  </CustomDialog>
</template>

<script>
import Api from '@/apis/api';
import CustomDialog from './CustomDialog.vue';

export default {
  name: 'AddWisdomBodyDialog',
  components: { CustomDialog },
  props: {
    visible: { type: Boolean, required: true }
  },
  data() {
    return {
      wisdomBodyName: ""
    }
  },
  methods: {
    handleOpen() {
      this.$nextTick(() => {
        if (this.$refs.inputRef && this.$refs.inputRef.focus) {
          this.$refs.inputRef.focus();
        }
      });
    },
    confirm() {
      if (!this.wisdomBodyName.trim()) {
        this.$message.error(this.$t('addAgentDialog.nameRequired'));
        return;
      }
      Api.agent.addAgent(this.wisdomBodyName, (res) => {
        this.$message.success({
          message: this.$t('addAgentDialog.addSuccess'),
          showClose: true
        });
        this.$emit('confirm', res);
        this.$emit('update:visible', false);
        this.wisdomBodyName = "";
      });
    },
    cancel() {
      this.wisdomBodyName = "";
      this.$emit('update:visible', false);
    },
    handleClose() {
      this.cancel();
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.add-agent-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-agent-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
  text-align: start;

  .required {
    color: var(--x-danger);
    margin-inline-start: 2px;
  }
}

.add-agent-input ::v-deep .el-input__inner {
  height: 40px;
}
</style>
