<template>
  <CustomDialog
    :title="title"
    :visible.sync="visible"
    width="960px"
    size="large"
    class="provider-dialog-wrapper"
    @confirm="submit"
    @close="handleClose"
    :confirmLoading="saving"
  >
    <div class="dialog-container">
      <el-form :model="form" :rules="rules" ref="form" label-width="auto" label-position="left" class="provider-form">
        <div class="form-row">
          <el-form-item :label="$t('providerDialog.category')" prop="modelType" class="form-item">
            <el-select v-model="form.modelType" :placeholder="$t('providerDialog.selectCategory')" class="custom-select">
              <el-option v-for="item in modelTypes" :key="item.value" :label="item.label" :value="item.value">
              </el-option>
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('providerDialog.code')" prop="providerCode" class="form-item">
            <el-input v-model="form.providerCode" :placeholder="$t('providerDialog.inputCode')" class="custom-input"></el-input>
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item :label="$t('providerDialog.name')" prop="name" class="form-item">
            <el-input v-model="form.name" :placeholder="$t('providerDialog.inputName')" class="custom-input"></el-input>
          </el-form-item>
          <el-form-item :label="$t('providerDialog.sort')" prop="sort" class="form-item">
            <el-input-number v-model="form.sort" :min="0" controls-position="right" class="custom-input-number"
              style="width: 100%;"></el-input-number>
          </el-form-item>
        </div>

        <div class="field-config-header">
          <span class="field-config-title">{{ $t('providerDialog.fieldConfig') }}</span>
          <div class="field-config-actions">
            <el-button @click="addField" size="small" icon="el-icon-plus" class="btn-add"
              :disabled="hasIncompleteFields">
              {{ $t('providerDialog.add') }}
            </el-button>
            <el-button @click="toggleSelectAllFields" size="small" class="btn-select-all">
              {{ isAllFieldsSelected ? $t('providerDialog.deselectAll') : $t('providerDialog.selectAll') }}
            </el-button>
            <el-button @click="batchRemoveFields" size="small" icon="el-icon-delete" class="btn-batch-delete">
              {{ $t('providerDialog.batchDelete') }}
            </el-button>
          </div>
        </div>

        <div class="fields-container">
          <el-table :data="form.fields" class="fields-table" style="width: 100%;" size="small" :key="tableKey">
            <el-table-column :label="$t('modelConfig.select')" align="center" width="72">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.selected" :aria-label="scope.row.key"
                  @change="handleFieldSelectChange"></el-checkbox>
              </template>
            </el-table-column>
            <el-table-column :label="$t('providerDialog.fieldKey')" min-width="140">
              <template slot-scope="scope">
                <template v-if="scope.row.editing">
                  <el-input v-model="scope.row.key" :placeholder="$t('providerDialog.fieldKey')"></el-input>
                </template>
                <template v-else>
                  <span class="mono">{{ scope.row.key }}</span>
                </template>
              </template>
            </el-table-column>
            <el-table-column :label="$t('providerDialog.fieldLabel')" min-width="140">
              <template slot-scope="scope">
                <template v-if="scope.row.editing">
                  <el-input v-model="scope.row.label" :placeholder="$t('providerDialog.fieldLabel')"></el-input>
                </template>
                <template v-else>
                  {{ scope.row.label }}
                </template>
              </template>
            </el-table-column>
            <el-table-column :label="$t('providerDialog.fieldType')" min-width="120">
              <template slot-scope="scope">
                <template v-if="scope.row.editing">
                  <el-select v-model="scope.row.type" :placeholder="$t('providerDialog.type')">
                    <el-option :label="$t('providerDialog.stringType')" value="string"></el-option>
                    <el-option :label="$t('providerDialog.numberType')" value="number"></el-option>
                    <el-option :label="$t('providerDialog.booleanType')" value="boolean"></el-option>
                    <el-option :label="$t('providerDialog.dictType')" value="dict"></el-option>
                    <el-option :label="$t('providerDialog.arrayType')" value="array"></el-option>
                  </el-select>
                </template>
                <template v-else>
                  {{ getTypeLabel(scope.row.type) }}
                </template>
              </template>
            </el-table-column>
            <el-table-column :label="$t('providerDialog.defaultValue')" min-width="140">
              <template slot-scope="scope">
                <template v-if="scope.row.editing">
                  <el-input v-model="scope.row.default" :placeholder="$t('providerDialog.inputDefaultValue')"></el-input>
                </template>
                <template v-else>
                  {{ scope.row.default }}
                </template>
              </template>
            </el-table-column>
            <el-table-column :label="$t('providerDialog.operation')" width="150" align="right">
              <template slot-scope="scope">
                <div class="row-actions">
                  <el-button v-if="!scope.row.editing" type="text" size="mini" class="action-btn"
                    @click="startEditing(scope.row)">
                    {{ $t('providerDialog.edit') }}
                  </el-button>
                  <el-button v-else type="text" size="mini" class="action-btn action-btn--strong"
                    @click="stopEditing(scope.row)">
                    {{ $t('providerDialog.complete') }}
                  </el-button>
                  <el-button type="text" size="mini" class="action-btn action-btn--danger"
                    @click="removeField(scope.$index)">
                    {{ $t('providerDialog.delete') }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-form>
    </div>
  </CustomDialog>
</template>

<script>
import CustomDialog from './CustomDialog.vue';
export default {
  name: 'ProviderDialog',
  props: {
    title: String,
    visible: Boolean,
    form: Object,
    modelTypes: Array
  },
  components: {
    CustomDialog
  },
  data() {
    return {
      saving: false,
      isAllFieldsSelected: false,
      tableKey: 0
    };
  },
  computed: {
    rules() {
      return {
        modelType: [{ required: true, message: this.$t('providerDialog.requiredCategory'), trigger: 'change' }],
        providerCode: [{ required: true, message: this.$t('providerDialog.requiredCode'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('providerDialog.requiredName'), trigger: 'blur' }]
      };
    },
    hasIncompleteFields() {
      return this.form.fields && this.form.fields.some(field =>
        !field.key || !field.label || !field.type
      );
    }
  },
  methods: {
    getTypeLabel(type) {
      const typeMap = {
        'string': this.$t('providerDialog.stringType'),
        'number': this.$t('providerDialog.numberType'),
        'boolean': this.$t('providerDialog.booleanType'),
        'dict': this.$t('providerDialog.dictType'),
        'array': this.$t('providerDialog.arrayType'),
        'RAG': this.$t('providerDialog.ragType')
      };
      return typeMap[type];
    },

    startEditing(row) {
      this.$set(row, 'editing', true);
    },

    stopEditing(row) {
      this.$set(row, 'editing', false);

      const index = this.form.fields.indexOf(row);
      if (index > -1) {
        this.form.fields.splice(index, 1);
        this.form.fields.push(row);
        this.forceTableRerender();
      }
    },

    handleFieldSelectChange() {
      this.isAllFieldsSelected = this.form.fields.length > 0 &&
        this.form.fields.every(field => field.selected);
    },

    toggleSelectAllFields() {
      this.isAllFieldsSelected = !this.isAllFieldsSelected;
      this.form.fields = this.form.fields.map(field => ({
        ...field,
        selected: this.isAllFieldsSelected
      }));
    },

    handleClose() {
      this.resetForm();
      this.$emit('cancel');
    },

    addField() {
      if (this.hasIncompleteFields) {
        this.$message.warning({
          message: this.$t('providerDialog.completeFieldEdit'),
          showClose: true
        });
        return;
      }

      this.form.fields.unshift({
        key: '',
        label: '',
        type: 'string',
        default: '',
        selected: false,
        editing: true
      });
      this.forceTableRerender();
    },

    removeField(index) {
      this.$confirm(this.$t('providerDialog.confirmDeleteField'), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      }).then(() => {
        this.form.fields = this.form.fields.filter((_, i) => i !== index);
        this.updateSelectAllStatus();
        this.forceTableRerender();
        this.$message.success({
            message: this.$t('common.deleteSuccess'),
            showClose: true
          });
      }).catch(() => {
          this.$message.info({
            message: this.$t('common.deleteCancelled'),
            showClose: true
          });
        });
    },

    batchRemoveFields() {
      const selectedFields = this.form.fields.filter(field => field.selected);
      if (selectedFields.length === 0) {
        this.$message.warning({
          message: this.$t('providerDialog.selectFieldsToDelete'),
          showClose: true
        });
        return;
      }
      this.$confirm(this.$t('providerDialog.confirmBatchDeleteFields', { count: selectedFields.length }), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      }).then(() => {
        this.form.fields = this.form.fields.filter(field => !field.selected);
        this.isAllFieldsSelected = false;
        this.forceTableRerender();
        this.$message.success({
            message: this.$t('providerDialog.batchDeleteFieldsSuccess', { count: selectedFields.length }),
            showClose: true
          });
      }).catch(() => {
        this.$message.info({
          message: 'Deletion cancelled',
          showClose: true
        });
      });
    },

    updateSelectAllStatus() {
      this.isAllFieldsSelected = this.form.fields.length > 0 &&
        this.form.fields.every(field => field.selected);
    },

    forceTableRerender() {
      this.tableKey += 1;
    },

    submit() {
      this.$refs.form.validate(valid => {
        if (valid) {
          const editingField = this.form.fields.find(field => field.editing);
          if (editingField) {
          this.$message.warning({
            message: this.$t('providerDialog.completeFieldEdit'),
            showClose: true
          });
          return;
        }

          this.form.fields = this.form.fields.map(field => ({
            ...field,
            selected: false
          }));
          this.isAllFieldsSelected = false;

          this.saving = true;
          this.$emit('submit', {
            form: this.form,
            done: () => {
              this.saving = false;
              this.resetForm();
            }
          });

          // Fallback: if the parent does not clear saving in its callback, force-release the loading state after 3 seconds
          setTimeout(() => {
            this.saving = false;
          }, 3000);
        }
      });
    },

    resetForm() {
      this.$refs.form.resetFields();
      if (this.form.fields) {
        this.form.fields.forEach(field => {
          field.selected = false;
          field.editing = false;
        });
      }
      this.isAllFieldsSelected = false;
      this.forceTableRerender();
    }
  },
  watch: {
    visible(val) {
      if (!val) {
        this.resetForm();
      }
    }
  }
};
</script>

<style scoped lang="scss">
::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.provider-dialog-wrapper {
  .provider-form {
    .form-row {
      display: flex;
      flex-wrap: wrap;
      column-gap: 20px;
    }

    .form-item {
      flex: 1;
      min-width: 260px;
      margin-bottom: 18px;
    }

    .custom-select,
    .custom-input-number {
      width: 100%;
    }
  }

  .field-config-header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin: 8px 0 12px;
    padding-top: 16px;
    border-top: 1px solid var(--x-divider);

    .field-config-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--x-text);
    }
  }

  .field-config-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .el-button + .el-button {
      margin-left: 0;
    }
  }

  .btn-batch-delete {
    color: var(--x-danger-text);

    &:not(.is-disabled):hover,
    &:not(.is-disabled):focus {
      color: var(--x-danger-text);
      background: var(--x-danger-bg);
      border-color: var(--x-danger-bg);
    }
  }

  .fields-container {
    border: 1px solid var(--x-border);
    border-radius: 12px;
    overflow-x: auto;
  }

  .fields-table {
    min-width: 720px;

    &::before {
      display: none;
    }

    ::v-deep th.el-table__cell > .cell {
      white-space: nowrap;
    }

    ::v-deep th.el-table__cell {
      background: var(--x-surface-raised);
      border-bottom: 1px solid var(--x-divider);
    }

    ::v-deep td.el-table__cell {
      border-bottom: 1px solid var(--x-divider);
    }

    ::v-deep .el-table__row:last-child td.el-table__cell {
      border-bottom: 0;
    }
  }

  .mono {
    font-family: var(--x-font-mono);
    font-size: 13px;
  }

  .row-actions {
    display: inline-flex;
    justify-content: flex-end;
    gap: 2px;
    white-space: nowrap;
  }

  .action-btn {
    height: 28px;
    padding: 0 8px;
    margin: 0 !important;
    color: var(--x-text-secondary);
    font-size: 13px;

    &:hover,
    &:focus {
      background: var(--x-canvas);
      color: var(--x-text);
    }

    &--strong {
      color: var(--x-text);
      font-weight: 600;
    }

    &--danger:hover,
    &--danger:focus {
      background: var(--x-danger-bg);
      color: var(--x-danger-text);
    }
  }
}

@media (max-width: 600px) {
  .provider-dialog-wrapper {
    .provider-form .form-item {
      min-width: 100%;
    }

    ::v-deep .provider-form > .form-row .el-form-item__label {
      float: none;
      display: block;
      width: auto !important;
      text-align: left;
      line-height: 1.5;
      padding-bottom: 6px;
    }

    ::v-deep .provider-form > .form-row .el-form-item__label-wrap {
      float: none;
      margin-left: 0 !important;
    }

    ::v-deep .provider-form > .form-row .el-form-item__content {
      margin-left: 0 !important;
    }
  }
}
</style>
