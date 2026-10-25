<template>
  <CustomDialog
    :title="title"
    :visible.sync="visible"
    width="720px"
    class="param-dialog-wrapper"
    @confirm="submit"
    @close="cancel"
    :confirmLoading="saving"
    :confirmText="$t('button.ok')"
    :cancelText="$t('button.cancel')"
  >
    <div class="dialog-container">
      <el-form :model="form" :rules="rules" ref="form" label-width="auto" label-position="left" class="param-form">
        <el-form-item :label="$t('paramDialog.paramCode')" prop="paramCode" class="form-item">
          <el-input v-model="form.paramCode" :placeholder="$t('paramDialog.paramCodePlaceholder')"
            class="custom-input"></el-input>
        </el-form-item>

        <el-form-item :label="$t('paramDialog.valueType')" prop="valueType" class="form-item">
          <el-select
            v-model="form.valueType"
            :placeholder="$t('paramDialog.valueTypePlaceholder')"
            class="custom-select"
          >
            <el-option
              v-for="item in valueTypeOptions"
              :key="item.value"
              :label="$t(`paramDialog.${item.value}Type`)"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="$t('paramDialog.paramValue')" prop="paramValue" class="form-item">
          <el-input
            v-if="form.valueType !== 'json' && form.valueType !== 'array'"
            v-model="form.paramValue"
            :placeholder="$t('paramDialog.paramValuePlaceholder')"
            class="custom-input"
          ></el-input>
          <el-input
            v-else
            type="textarea"
            v-model="form.paramValue"
            :placeholder="$t('paramDialog.paramValuePlaceholder')"
            :rows="6"
            class="custom-textarea"
          ></el-input>
        </el-form-item>

        <el-form-item :label="$t('paramDialog.remark')" prop="remark" class="form-item remark-item">
          <el-input type="textarea" v-model="form.remark" :placeholder="$t('paramDialog.remarkPlaceholder')" :rows="3"
            class="custom-textarea"></el-input>
        </el-form-item>
      </el-form>
    </div>
  </CustomDialog>
</template>

<script>
import CustomDialog from './CustomDialog.vue';
export default {
  props: {
    title: {
      type: String,
      default: 'Add parameter'
    },
    visible: {
      type: Boolean,
      default: false
    },
    form: {
      type: Object,
      default: () => ({
        id: null,
        paramCode: '',
        paramValue: '',
        valueType: 'string',
        remark: ''
      })
    }
  },
  components: {
    CustomDialog
  },
  data() {
    return {
      dialogKey: Date.now(),
      saving: false,
      valueTypeOptions: [
        { value: 'string' },
        { value: 'number' },
        { value: 'boolean' },
        { value: 'array' },
        { value: 'json' }
      ],
      rules: {
        paramCode: [
          { required: true, message: this.$t('paramDialog.requiredParamCode'), trigger: "blur" }
        ],
        paramValue: [
          { required: true, message: this.$t('paramDialog.requiredParamValue'), trigger: "blur" }
        ],
        valueType: [
          { required: true, message: this.$t('paramDialog.requiredValueType'), trigger: "change" }
        ]
      }
    };
  },
  methods: {
    submit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          const submitData = { ...this.form };

          // For array type, validate the format and convert
          if (submitData.valueType === 'array' && submitData.paramValue) {
            const lines = submitData.paramValue.split('\n').filter(line => line.trim());

            // Check that every line except the last ends with a semicolon
            for (let i = 0; i < lines.length - 1; i++) {
              if (!lines[i].trim().endsWith(';')) {
                this.$message.error('Invalid array format: each item must end with a semicolon (;)');
                return;
              }
            }

            const items = lines
              .map(item => item.trim().replace(/;$/, ''))
              .filter(item => item);
            submitData.paramValue = items.join(';');
          }
          // For json type, minify the JSON before submitting
          else if (submitData.valueType === 'json' && submitData.paramValue) {
            try {
              const parsed = JSON.parse(submitData.paramValue);
              submitData.paramValue = JSON.stringify(parsed);
            } catch (e) {
              // If parsing fails, keep the original value
            }
          }

          this.saving = true; // Start loading
          this.$emit('submit', submitData);
        }
      });
    },
    cancel() {
      this.saving = false; // Reset state on cancel
      this.dialogKey = Date.now();
      this.$emit('update:visible', false);
      this.$emit('cancel');
    },

    // Called by the parent component to reset the saving state
    resetSaving() {
      this.saving = false;
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        if (this.form.paramValue) {
          // For json type, pretty-print for display
          if (this.form.valueType === 'json') {
            try {
              const parsed = JSON.parse(this.form.paramValue);
              this.form.paramValue = JSON.stringify(parsed, null, 2);
            } catch (e) {
              // If parsing fails, keep the original value
            }
          }
          // For array type, convert the semicolon-separated string to one item per line
          else if (this.form.valueType === 'array') {
            const items = this.form.paramValue.split(';').filter(item => item.trim());
            this.form.paramValue = items.join(';\n');
          }
        }
      } else {
        // Reset the saving state when the dialog closes
        this.saving = false;
      }
    }
  }
};
</script>

<style scoped lang="scss">
::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.param-dialog-wrapper {
  .param-form {
    .form-item {
      margin-bottom: 18px;

      :deep(.el-form-item__label) {
        color: var(--x-text);
        font-weight: 500;
        font-size: 14px;
        padding-inline-end: 16px;
      }
    }

    .custom-select {
      width: 100%;
    }

    .custom-textarea :deep(.el-textarea__inner) {
      font-family: var(--x-font-mono);
      font-size: 13px;
      line-height: 1.5;
    }

    .remark-item {
      margin-bottom: 0;

      .custom-textarea :deep(.el-textarea__inner) {
        font-family: var(--x-font);
        font-size: 14px;
      }
    }
  }
}

@media (max-width: 600px) {
  .param-dialog-wrapper {
    ::v-deep .el-form-item__label {
      float: none;
      display: block;
      width: auto !important;
      text-align: start;
      line-height: 1.5;
      padding-bottom: 6px;
    }

    ::v-deep .el-form-item__label-wrap {
      float: none;
      margin-inline-start: 0 !important;
    }

    ::v-deep .el-form-item__content {
      margin-inline-start: 0 !important;
    }
  }
}
</style>
