<template>
  <CustomDialog
    :visible.sync="dialogVisible"
    :title="$t('modelConfigDialog.addModel')"
    width="960px"
    size="large"
    class="add-model-dialog"
    :confirmLoading="saving"
    @close="handleClose"
    @confirm="confirm"
  >
    <div class="dialog-content">
      <!-- Model info section -->
      <div class="section-header">
        <div class="section-title">{{ $t('modelConfigDialog.modelInfo') }}</div>
        <div class="switch-group">
          <div class="switch-item">
            <span>{{ $t('modelConfigDialog.enable') }}</span>
            <el-switch v-model="formData.isEnabled" class="custom-switch"></el-switch>
          </div>
          <div class="switch-item" style="display: none;">
            <span>{{ $t('modelConfigDialog.setDefault') }}</span>
            <el-switch v-model="formData.isDefault" class="custom-switch"></el-switch>
          </div>
        </div>
      </div>

      <div class="divider"></div>
      <el-form :model="formData" label-width="auto" label-position="left" class="custom-form">
        <div class="form-row">
          <el-form-item :label="$t('modelConfigDialog.modelId')" prop="id" style="flex: 1;">
            <el-input v-model="formData.id" :placeholder="$t('modelConfigDialog.enterModelId')" class="custom-input-bg"
              maxlength="32"></el-input>
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item :label="$t('modelConfigDialog.modelName')" prop="modelName" style="flex: 1;">
            <el-input v-model="formData.modelName" :placeholder="$t('modelConfigDialog.enterModelName')"
              class="custom-input-bg"></el-input>
          </el-form-item>
          <el-form-item :label="$t('modelConfigDialog.modelCode')" prop="modelCode" style="flex: 1;">
            <el-input v-model="formData.modelCode" :placeholder="$t('modelConfigDialog.enterModelCode')"
              class="custom-input-bg"></el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item :label="$t('modelConfigDialog.supplier')" prop="supplier" style="flex: 1;">
            <el-select v-model="formData.supplier" :placeholder="$t('modelConfigDialog.selectSupplier')"
              class="custom-select custom-input-bg" style="width: 100%;" @focus="loadProviders" filterable>
              <el-option v-for="item in providers" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('modelConfigDialog.sortOrder')" prop="sortOrder" style="flex: 1;">
            <el-input v-model="formData.sort" type="number" :placeholder="$t('modelConfigDialog.enterSortOrder')"
              class="custom-input-bg"></el-input>
          </el-form-item>
        </div>

        <el-form-item :label="$t('modelConfigDialog.docLink')" prop="docLink" style="margin-bottom: 27px;">
          <el-input v-model="formData.docLink" :placeholder="$t('modelConfigDialog.enterDocLink')"
            class="custom-input-bg"></el-input>
        </el-form-item>

        <el-form-item :label="$t('modelConfigDialog.remark')" prop="remark" class="prop-remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3"
            :placeholder="$t('modelConfigDialog.enterRemark')" :autosize="{ minRows: 3, maxRows: 5 }"
            class="custom-input-bg"></el-input>
        </el-form-item>
      </el-form>

      <teleport v-if="chunkedCallInfoFields.length">
        <div class="section-title">{{ $t('modelConfigDialog.callInfo') }}</div>
        <div class="divider"></div>
      </teleport>

      <el-form :model="formData.configJson" label-width="auto" label-position="left" class="custom-form">
        <div v-for="(row, rowIndex) in chunkedCallInfoFields" :key="rowIndex" class="form-row">
          <el-form-item v-for="field in row" :key="field.prop" :label="field.label" :prop="field.prop" style="flex: 1;">
            <el-input v-if="field.type === 'json-textarea'" v-model="formData.configJson[field.prop]" type="textarea"
              :rows="3" :placeholder="field.placeholder" class="custom-input-bg">
            </el-input>
            <el-input v-else v-model="formData.configJson[field.prop]" :placeholder="field.placeholder"
              :type="field.type || 'text'" class="custom-input-bg" :show-password="field.type === 'password'">
            </el-input>
          </el-form-item>
        </div>
      </el-form>
    </div>
  </CustomDialog>
</template>

<script>
import Api from '@/apis/api';
import CustomDialog from './CustomDialog.vue';
export default {
  name: 'AddModelDialog',
  components: {
    CustomDialog
  },
  props: {
    visible: { type: Boolean, required: true },
    modelType: { type: String, required: true }
  },
  data() {
    return {
      saving: false,
      providers: [],
      dialogVisible: false,
      providersLoaded: false,
      providerFields: [],
      currentProvider: null,
      formData: {
        id: '',
        modelName: '',
        modelCode: '',
        supplier: '',
        sort: 1,
        docLink: '',
        remark: '',
        isEnabled: true,
        isDefault: true,
        configJson: {}
      }
    }
  },
  watch: {
    visible(val) {
      this.dialogVisible = val;
      if (val) {
        this.initConfigJson();
      } else {
        this.resetForm();
      }
    },
    'formData.supplier'(newVal) {
      this.currentProvider = this.providers.find(p => p.value === newVal);
      this.providerFields = this.currentProvider?.fields || [];
      this.initDynamicConfig();
    }
  },
  computed: {
    dynamicCallInfoFields() {
      return this.providerFields;
    },
    chunkedCallInfoFields() {
      const chunkSize = 2;
      const result = [];
      for (let i = 0; i < this.dynamicCallInfoFields.length; i += chunkSize) {
        result.push(this.dynamicCallInfoFields.slice(i, i + chunkSize));
      }
      return result;
    }
  },
  methods: {
    loadProviders() {
      if (this.providersLoaded)
        return

      Api.model.getModelProviders(this.modelType, (data) => {
        this.providers = data.map(item => ({
          label: item.name,
          value: item.providerCode,
          fields: JSON.parse(item.fields || '[]').map(f => ({
            label: f.label,
            prop: f.key,
            type: f.type === 'dict' ? 'json-textarea' : f.type === 'password' ? 'password' : 'text',
            placeholder: `Please enter ${f.key}`
          }))
        }))
        this.providersLoaded = true
      })
    },
    initConfigJson() {
      const defaultConfig = {};
      this.providerFields.forEach(field => {
        defaultConfig[field.prop] = '';
      });
      this.formData.configJson = { ...defaultConfig };
    },

    handleClose() {
      this.saving = false;
      this.dialogVisible = false;
      this.$emit('update:visible', false);
    },
    initDynamicConfig() {
      const newConfig = {};
      this.providerFields.forEach(field => {
        newConfig[field.prop] = this.formData.configJson[field.prop] || '';
      });
      this.formData.configJson = newConfig;
    },
    confirm() {
      this.saving = true;

      // Validate that the model ID is not letters only or whitespace
      if (this.formData.id && !this.validateModelId(this.formData.id)) {
        this.$message.error(this.$t('modelConfigDialog.invalidModelId'));
        this.saving = false;
        return;
      }

      if (!this.formData.supplier) {
        this.$message.error(this.$t('addModelDialog.requiredSupplier'));
        this.saving = false;
        return;
      }

      // dict fields must be parsed into a JSON object before submit, otherwise they are stored as strings
      for (const field of this.providerFields) {
        if (field.type !== 'json-textarea') continue;
        const raw = this.formData.configJson[field.prop];
        if (!raw || typeof raw === 'object') continue;
        try {
          const parsed = JSON.parse(raw);
          if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
            throw new Error('not object');
          }
          this.formData.configJson[field.prop] = parsed;
        } catch (e) {
          this.$message.error('Must be a dictionary (e.g. {"key":"value"})');
          this.saving = false;
          return;
        }
      }

      const submitData = {
        id: this.formData.id || '',
        modelName: this.formData.modelName || '',
        modelCode: this.formData.modelCode || '',
        supplier: this.formData.supplier,
        sort: this.formData.sort || 1,
        docLink: this.formData.docLink || '',
        remark: this.formData.remark || '',
        isEnabled: this.formData.isEnabled ? 1 : 0,
        isDefault: this.formData.isDefault ? 1 : 0,
        provideCode: this.formData.supplier,
        configJson: {
          ...this.formData.configJson,
          type: this.formData.supplier
        }
      };

      try {
        this.$emit('confirm', submitData);
        this.handleClose();
        this.resetForm();
      } catch (e) {
        console.error(e);
      } finally {
        this.saving = false;
      }
    },
    resetForm() {
      this.saving = false;
      this.formData = {
        id: '',
        modelName: '',
        modelCode: '',
        supplier: '',
        sort: 1,
        docLink: '',
        remark: '',
        isEnabled: true,
        isDefault: true,
        configJson: {}
      };
      // Reset loading state
      this.providers = [];
      this.providersLoaded = false;
      // Reset field configuration
      this.providerFields = [];
      this.currentProvider = null;
    },
    
    // Validate model ID: must not be letters only or whitespace
    validateModelId(modelId) {
      if (!modelId || typeof modelId !== 'string') {
        return false;
      }
      
      // Trim leading/trailing whitespace
      const trimmedId = modelId.trim();
      
      // Check for empty or whitespace-only
      if (trimmedId === '') {
        return false;
      }
      
      // Check whether it contains only letters
      if (/^[a-zA-Z]+$/.test(trimmedId)) {
        return false;
      }
      
      // Check whether it contains whitespace
      if (/\s/.test(trimmedId)) {
        return false;
      }
      
      // Allow letters, digits, underscores and hyphens
      if (!/^[a-zA-Z0-9_-]+$/.test(trimmedId)) {
        return false;
      }
      
      return true;
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}
::v-deep .el-dialog__body {
  overflow-y: auto;
  @include scrollbar-style;
}
.add-model-dialog {
  .section-header {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
  }

  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: var(--x-text);
    text-align: start;
  }

  .section-header .section-title {
    margin-bottom: 0;
  }

  .section-title + .divider {
    margin-top: 10px;
  }

  .switch-group {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .switch-item {
    display: flex;
    align-items: center;
    color: var(--x-text-secondary);

    span {
      margin-inline-end: 8px;
    }
  }

  .divider {
    height: 1px;
    background: var(--x-divider);
    margin-bottom: 16px;
  }

  .form-row {
    display: flex;
    flex-wrap: wrap;
    column-gap: 20px;
    margin-bottom: 0;

    > .el-form-item {
      min-width: 260px;
    }
  }
}

.custom-form {
  margin-bottom: 8px;
}

.custom-form .el-form-item {
  margin-bottom: 14px;
}

@media (max-width: 600px) {
  .add-model-dialog {
    .form-row > .el-form-item {
      min-width: 100%;
    }

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
