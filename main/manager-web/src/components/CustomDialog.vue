<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    :close-on-press-escape="closeOnPressEscape"
    :show-close="showClose"
    :destroy-on-close="destroyOnClose"
    :custom-class="dialogCustomClass"
    class="custom-dialog"
    @close="handleClose"
    @open="handleOpen"
  >
    <template slot="title">
      <slot name="title">
        <div class="dialog-title">
          <span>{{ title }}</span>
        </div>
      </slot>
    </template>
    <slot></slot>
    <template slot="footer">
      <div v-if="footer" class="dialog-footer">
        <CustomButton @click="handleCancel">{{ cancelText }}</CustomButton>
        <CustomButton :loading="confirmLoading" type="confirm" @click="handleConfirm">
          <span class="confirm-inner">{{ confirmText }}</span>
        </CustomButton>
      </div>
    </template>
  </el-dialog>
</template>

<script>
import CustomButton from './CustomButton.vue';
export default {
  name: "CustomDialog",
  props: {
    title: {
      type: String,
      default: ""
    },
    visible: {
      type: Boolean,
      default: false
    },
    confirmLoading: {
      type: Boolean,
      default: false
    },
    width: {
      type: String,
      default: "720px"
    },
    footer: {
      type: Boolean,
      default: true
    },
    closeOnClickModal: {
      type: Boolean,
      default: false
    },
    closeOnPressEscape: {
      type: Boolean,
      default: true
    },
    showClose: {
      type: Boolean,
      default: true
    },
    destroyOnClose: {
      type: Boolean,
      default: true
    },
    customClass: {
      type: String,
      default: ""
    },
    size: {
      type: String,
      default: "small",
      validator: (val) => ["small", "large"].includes(val),
    },
    cancelText: {
      type: String,
      default: "Cancel"
    },
    confirmText: {
      type: String,
      default: "Save"
    }
  },
  data() {
    return {
      dialogVisible: this.visible
    };
  },
  computed: {
    dialogCustomClass() {
      const classes = ["custom-dialog"];
      if (this.customClass) classes.push(this.customClass);
      if (this.size === "large") classes.push("custom-dialog--large");
      if (String(this.width).includes("%")) classes.push("custom-dialog--fluid");
      return classes.join(" ");
    },
  },
  components: {
    CustomButton
  },
  watch: {
    visible(val) {
      this.dialogVisible = val;
    }
  },
  methods: {
    handleClose() {
      this.dialogVisible = false;
      this.$emit("update:visible", false);
      this.$emit("close");
    },
    handleOpen() {
      this.$emit("open");
    },
    handleCancel() {
      this.dialogVisible = false;
      this.$emit("update:visible", false);
      this.$emit("cancel");
    },
    handleConfirm() {
      this.$emit("confirm");
    }
  }
};
</script>

<style lang="scss" scoped>
/* Large and small sizes are distinguished by .custom-dialog--large */
::v-deep .el-dialog.custom-dialog {
  border-radius: var(--x-radius-card);
  overflow: hidden;
  box-shadow: var(--x-shadow-raised);
  margin: 5vh auto !important;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 10vh);
}

::v-deep .el-dialog__body {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
  padding: 20px 24px;
  color: var(--x-text);
}

/* .v-modal styles are defined globally in global.scss */
::v-deep .el-dialog__header {
  padding: 18px 24px 16px;
  background: var(--x-surface);
  border-bottom: 1px solid var(--x-divider);
  text-align: left;
}

::v-deep .el-dialog__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

::v-deep .dialog-title {
  font-size: 16px;
  display: inline-flex;
  align-items: center;
  > span {
    line-height: 24px;
    font-weight: 600;
    color: var(--x-text);
  }
}

::v-deep .el-dialog__headerbtn {
  top: 14px;
  right: 16px;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--x-radius-control);
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;

  .el-dialog__close {
    font-size: 16px;
    color: var(--x-text-muted);
    position: static;
    transform: none;
  }

  &:hover {
    background: var(--x-canvas);

    .el-dialog__close {
      color: var(--x-text);
    }
  }
}

::v-deep .el-dialog__footer {
  padding: 14px 24px 18px;
  border-top: 1px solid var(--x-divider);
}

::v-deep .dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;

  .el-button {
    display: flex;
    align-items: center;
  }

  .el-button + .el-button {
    margin-left: 0;
  }

  .confirm-inner {
    display: inline-flex;
    align-items: center;
  }
}

/* Large size: leave 10px top and bottom so the border and rounded corners stay visible */
::v-deep .el-dialog.custom-dialog.custom-dialog--large {
  margin: 10px auto !important;
  height: calc(100vh - 20px);
  max-height: calc(100vh - 20px);
  border-radius: var(--x-radius-card);
}

::v-deep .el-dialog.custom-dialog.custom-dialog--large.custom-dialog--fluid {
  max-width: var(--dialog-table-max);
}

::v-deep .el-dialog.custom-dialog--large .el-dialog__body {
  max-height: calc(100vh - 20px - 110px);
  flex: 1;
  min-height: 0;
}
</style>
