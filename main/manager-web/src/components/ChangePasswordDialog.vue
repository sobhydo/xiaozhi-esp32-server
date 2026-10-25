<template>
  <form>
    <CustomDialog
      :title="$t('changePassword.title')"
      :visible.sync="dialogVisible"
      width="600px"
      @cancel="cancel"
      @confirm="confirm">
      <div class="password-form">
        <div class="form-field">
          <label class="form-label" for="change-password-old">
            <span class="required" aria-hidden="true">*</span>
            {{ $t('changePassword.oldPasswordLabel') }}
          </label>
          <el-input id="change-password-old" :placeholder="$t('changePassword.oldPasswordPlaceholder')" v-model="oldPassword"
            type="password" autocomplete="current-password" show-password />
        </div>
        <div class="form-field">
          <label class="form-label" for="change-password-new">
            <span class="required" aria-hidden="true">*</span>
            {{ $t('changePassword.newPasswordLabel') }}
          </label>
          <el-input id="change-password-new" :placeholder="$t('changePassword.newPasswordPlaceholder')" v-model="newPassword"
            type="password" autocomplete="new-password" show-password />
        </div>
        <div class="form-field">
          <label class="form-label" for="change-password-confirm">
            <span class="required" aria-hidden="true">*</span>
            {{ $t('changePassword.confirmPasswordLabel') }}
          </label>
          <el-input id="change-password-confirm" :placeholder="$t('changePassword.confirmPasswordPlaceholder')"
            v-model="confirmNewPassword" type="password" autocomplete="new-password" show-password />
        </div>
      </div>
    </CustomDialog>
  </form>
</template>

<script>
import userApi from '@/apis/module/user';
import { mapActions } from 'vuex';
import CustomDialog from '@/components/CustomDialog.vue';

export default {
  name: 'ChangePasswordDialog',
  props: {
    value: {
      type: Boolean,
      required: true
    }
  },
  data() {
    return {
      dialogVisible: this.value,
      oldPassword: "",
      newPassword: "",
      confirmNewPassword: ""
    }
  },
  components: {
    CustomDialog
  },
  watch: {
    value(val) {
      this.dialogVisible = val;
    },
    dialogVisible(val) {
      this.$emit('input', val);
    }
  },
  methods: {
    ...mapActions(['logout']), // Map the Vuex logout action
    confirm() {
      if (!this.oldPassword.trim() || !this.newPassword.trim() || !this.confirmNewPassword.trim()) {
        this.$message.error(this.$t('changePassword.allFieldsRequired'));
        return;
      }
      if (this.newPassword !== this.confirmNewPassword) {
        this.$message.error(this.$t('changePassword.passwordsNotMatch'));
        return;
      }
      if (this.newPassword === this.oldPassword) {
        this.$message.error(this.$t('changePassword.newPasswordSameAsOld'));
        return;
      }

      // Updated API call
      userApi.changePassword(this.oldPassword, this.newPassword, (res) => {
        if (res.data.code === 0) {
          this.$message.success({
            message: this.$t('changePassword.passwordChangedSuccessfully'),
            showClose: true
          });
          this.logout().then(() => {
            this.$router.push('/login');
          });
        } else {
          this.$message.error(res.data.msg || this.$t('changePassword.changeFailed'));
        }
      }, (err) => {
        this.$message.error(err.msg || this.$t('changePassword.changeFailed'));
      });
      this.dialogVisible = false;
    },
    cancel() {
      this.resetForm();
    },
    resetForm() {
      this.oldPassword = "";
      this.newPassword = "";
      this.confirmNewPassword = "";
    }
  }
}
</script>

<style lang="scss" scoped>
.password-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: start;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  line-height: 20px;
  color: var(--x-text);
}

.required {
  margin-inline-end: 2px;
  color: var(--x-danger);
}

::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}
</style>
