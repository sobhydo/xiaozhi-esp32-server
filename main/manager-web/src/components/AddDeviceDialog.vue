<template>
  <CustomDialog
    :title="$t('device.dialogTitle')"
    :visible.sync="visible"
    width="420px"
    :confirmLoading="loading"
    @confirm="confirm"
    @cancel="cancel"
    @close="handleClose"
    :confirmText="$t('device.confirmButton')"
    :cancelText="$t('device.cancelButton')"
  >
    <div class="add-device-body">
      <label class="add-device-label" for="add-device-code">
        {{ $t('device.verificationCode') }}<span class="required" aria-hidden="true">*</span>
      </label>
      <el-input
        id="add-device-code"
        :placeholder="$t('device.verificationCodePlaceholder')"
        v-model="deviceCode"
        class="add-device-input"
        @keyup.enter.native="confirm"
      />
    </div>
  </CustomDialog>
</template>

<script>
import Api from '@/apis/api';
import CustomDialog from './CustomDialog.vue';

export default {
  name: 'AddDeviceDialog',
  components: { CustomDialog },
  props: {
    visible: { type: Boolean, required: true },
    agentId: { type: String, required: true }
  },
  data() {
    return {
      deviceCode: "",
      loading: false,
    }
  },
  methods: {
    confirm() {
      if (!/^\d{6}$/.test(this.deviceCode)) {
        this.$message.error(this.$t('device.input6DigitCode'));
        return;
      }
      this.loading = true;
      Api.device.bindDevice(
        this.agentId,
        this.deviceCode,
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.$emit('refresh');
            this.$message.success({
              message: this.$t('device.bindSuccess'),
              showClose: true
            });
            this.closeDialog();
          } else {
            this.$message.error({
              message: data.msg || this.$t('device.bindFailed'),
              showClose: true
            });
          }
        },
        (err) => {
          this.loading = false;
          const msg = err && err.data && err.data.msg;
          this.$message.error({
            message: msg || this.$t('device.bindFailed'),
            showClose: true
          });
        }
      );
    },
    closeDialog() {
      this.loading = false;
      this.deviceCode = '';
      this.$emit('update:visible', false);
    },
    cancel() {
      this.closeDialog();
    },
    handleClose() {
      this.closeDialog();
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.add-device-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.add-device-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
  text-align: start;

  .required {
    color: var(--x-danger);
    margin-inline-start: 2px;
  }
}

.add-device-input ::v-deep .el-input__inner {
  height: 40px;
}
</style>
