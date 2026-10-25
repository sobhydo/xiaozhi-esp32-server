<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('header.providerManagement') }}</h1>
          <p class="page-description">{{ $t('console.providersDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
            {{ $t('common.add') }}
          </el-button>
        </div>
      </header>

      <section class="provider-card">
        <div class="table-toolbar">
          <div class="search-group">
            <el-input :placeholder="$t('providerManagement.searchPlaceholder')"
              :aria-label="$t('providerManagement.searchPlaceholder')" v-model="searchName" class="search-input"
              prefix-icon="el-icon-search" @keyup.enter.native="handleSearch" clearable />
            <el-button class="btn-search" @click="handleSearch">{{ $t('common.search') }}</el-button>
          </div>
          <el-dropdown trigger="click" @command="handleSelectModelType" @visible-change="handleDropdownVisibleChange">
            <el-button size="small" class="filter-btn">
              {{ $t('providerManagement.categoryFilter') }}{{ selectedModelTypeLabel }}<i class="el-icon-arrow-down el-icon--right"
                :class="{ 'rotate-down': DropdownVisible }"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="">{{ $t('common.all') }}</el-dropdown-item>
              <el-dropdown-item v-for="item in translatedModelTypes" :key="item.value" :command="item.value">
                {{ item.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
        <CustomTable
          ref="providersTable"
          class="provider-table"
          :data="filteredProvidersList"
          :columns="tableColumns"
          :loading="loading"
          :show-selection="true"
          :show-operations="true"
          :operations-label="$t('common.action')"
          :operations-width="150"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-size-options="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @page-change="goToPage"
        >
          <template slot="selection" slot-scope="scope">
            <el-checkbox v-model="scope.row.selected" :aria-label="scope.row.name"></el-checkbox>
          </template>
          <template slot="modelType" slot-scope="scope">
            <span class="type-pill">{{ getModelTypeLabel(scope.row.modelType) }}</span>
          </template>
          <template slot="providerCode" slot-scope="scope">
            <span class="mono">{{ scope.row.providerCode }}</span>
          </template>
          <template slot="name" slot-scope="scope">
            <span class="cell-strong">{{ scope.row.name }}</span>
          </template>
          <template slot="fields" slot-scope="scope">
            <el-popover placement="top-start" width="400" trigger="hover" popper-class="provider-fields-popover">
              <div v-for="field in scope.row.fields" :key="field.key" class="field-item">
                <span class="field-label">{{ field.label }}</span>
                <span class="field-type">{{ field.type }}</span>
                <span v-if="isSensitiveField(field.key)" class="sensitive-tag">{{ $t('common.sensitive') }}</span>
              </div>
              <el-button slot="reference" size="mini" type="text" class="action-btn">
                {{ $t('providerManagement.viewFields') }}
                <span class="field-count">{{ (scope.row.fields || []).length }}</span>
              </el-button>
            </el-popover>
          </template>
          <template slot="operations" slot-scope="scope">
            <div class="row-actions">
              <el-button size="mini" type="text" class="action-btn" @click="editProvider(scope.row)">{{ $t('common.edit') }}</el-button>
              <el-button size="mini" type="text" class="action-btn action-btn--danger" @click="deleteProvider(scope.row)">{{ $t('common.delete') }}</el-button>
            </div>
          </template>
          <template slot="footer-btns">
            <div class="batch-actions">
              <CustomButton :icon="isAllSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'" size="small" @click="handleSelectAll">
                {{ isAllSelected ? $t('common.deselectAll') : $t('common.selectAll') }}
              </CustomButton>
              <CustomButton size="small" type="delete" icon="el-icon-delete" @click="deleteSelectedProviders">
                {{ $t('common.delete') }}
              </CustomButton>
            </div>
          </template>
        </CustomTable>
      </section>
    </main>

    <!-- Add/edit provider dialog -->
    <provider-dialog :title="dialogTitle" :visible.sync="dialogVisible" :form="providerForm" :model-types="modelTypes"
      @submit="handleSubmit" @cancel="dialogVisible = false" />

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import ProviderDialog from "@/components/ProviderDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";

export default {
  components: { HeaderBar, ProviderDialog, VersionFooter, CustomButton, CustomTable },
  data() {
    return {
      searchName: "",
      searchModelType: "",
      providersList: [],
      modelTypes: [
        { value: "ASR", labelKey: 'providerManagement.modelType.ASR' },
        { value: "TTS", labelKey: 'providerManagement.modelType.TTS' },
        { value: "LLM", labelKey: 'providerManagement.modelType.LLM' },
        { value: "VLLM", labelKey: 'providerManagement.modelType.VLLM' },
        { value: "Intent", labelKey: 'providerManagement.modelType.Intent' },
        { value: "Memory", labelKey: 'providerManagement.modelType.Memory' },
        { value: "VAD", labelKey: 'providerManagement.modelType.VAD' },
        { value: "Plugin", labelKey: 'providerManagement.modelType.Plugin' },
        { value: "RAG", labelKey: 'providerManagement.modelType.RAG' }
      ],
      currentPage: 1,
      loading: false,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      dialogVisible: false,
      dialogTitle: "Add Provider",
      isAllSelected: false,
      DropdownVisible: false,
      sensitive_keys: ["api_key", "personal_access_token", "access_token", "token", "secret", "access_key_secret", "secret_key"],
      providerForm: {
        id: null,
        modelType: "",
        providerCode: "",
        name: "",
        fields: [],
        sort: 0
      },
      tableColumns: []
    };
  },
  created() {
    this.initTableColumns();
    this.fetchProviders();
  },
  computed: {
    translatedModelTypes() {
      return this.modelTypes.map(type => ({
        value: type.value,
        label: this.$t(type.labelKey)
      }));
    },
    selectedModelTypeLabel() {
      if (!this.searchModelType) return `（${this.$t('providerManagement.all')}）`;
      const selectedType = this.modelTypes.find(item => item.value === this.searchModelType);
      return selectedType ? `（${this.$t(selectedType.labelKey)}）` : "";
    },
    filteredProvidersList() {
      return this.providersList;
    }
  },
  methods: {
    initTableColumns() {
      this.tableColumns = [
        {
          prop: 'modelType',
          label: this.$t('providerManagement.category'),
          align: 'left',
          minWidth: 170
        },
        {
          prop: 'providerCode',
          label: this.$t('providerManagement.providerCode'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'name',
          label: this.$t('common.name'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'fields',
          label: this.$t('providerManagement.fieldConfig'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'sort',
          label: this.$t('common.sort'),
          align: 'left',
          width: 80
        }
      ];
    },
    fetchProviders() {
      this.loading = true;

      Api.model.getModelProvidersPage(
        {
          page: this.currentPage,
          limit: this.pageSize,
          name: this.searchName,
          modelType: this.searchModelType
        },
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.providersList = data.data.list.map(item => {
              return {
                ...item,
                selected: false,
                fields: JSON.parse(item.fields)
              };
            });
            this.total = data.data.total;
          } else {
            this.$message.error({
              message: data.msg || 'Failed to fetch parameter list'
            });
          }
        }
      );
    },
    handleSearch() {
      this.currentPage = 1;
      this.fetchProviders();
    },
    handleSelectModelType(value) {
      this.searchModelType = value;
      this.handleSearch();
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.providersList.forEach(row => {
        row.selected = this.isAllSelected;
      });
    },
    showAddDialog() {
      this.dialogTitle = this.$t('common.addProvider');
      this.providerForm = {
        id: null,
        modelType: "",
        providerCode: "",
        name: "",
        fields: [],
        sort: 0
      };
      this.dialogVisible = true;
    },
    editProvider(row) {
      this.dialogTitle = this.$t('common.editProvider');
      this.providerForm = {
        ...row,
        fields: JSON.parse(JSON.stringify(row.fields))
      };
      this.dialogVisible = true;
    },
    handleSubmit({ form, done }) {
      if (form.id) {
        Api.model.updateModelProvider(form, ({ data }) => {
          if (data.code === 0) {
            this.fetchProviders();
            this.$message.success({
              message: this.$t('common.updateSuccess'),
              showClose: true
            });
            this.dialogVisible = false;
          } else {
            this.$message.error({
              message: data.msg || this.$t('common.updateFailure'),
              showClose: true
            });
          }
          done && done();
        });
      } else {
        Api.model.addModelProvider(form, ({ data }) => {
          if (data.code === 0) {
            this.fetchProviders();
            this.$message.success({
              message: this.$t('common.addSuccess'),
              showClose: true
            });
            this.total += 1;
            this.dialogVisible = false;
          } else {
            this.$message.error({
              message: data.msg || this.$t('common.addFailure'),
              showClose: true
            });
          }
          done && done();
        });
      }
    },
    deleteSelectedProviders() {
      const selectedRows = this.providersList.filter(row => row.selected);
      if (selectedRows.length === 0) {
        this.$message.warning({
          message: this.$t('providerManagement.selectToDelete'),
          showClose: true
        });
        return;
      }
      this.deleteProvider(selectedRows);
    },
    deleteProvider(row) {
      const providers = Array.isArray(row) ? row : [row];
      const providerCount = providers.length;

      this.$confirm(this.$t('providerManagement.confirmDelete', { count: providerCount }), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning',
      }).then(() => {
        const ids = providers.map(provider => provider.id);
        Api.model.deleteModelProviderByIds(ids, ({ data }) => {
          if (data.code === 0) {
            this.isAllSelected = false;
            this.fetchProviders();
            this.$message.success({
              message: this.$t('common.deleteSuccess'),
              showClose: true
            });
          } else {
            this.$message.error({
              message: data.msg || this.$t('common.deleteFailure'),
              showClose: true
            });
          }
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: this.$t('common.deleteCancelled'),
          showClose: true,
          duration: 1000
        });
      });
    },
    getModelTypeTag(type) {
      const typeMap = {
        'ASR': 'success',
        'TTS': 'warning',
        'LLM': 'danger',
        'Intent': 'info',
        'Memory': '',
        'VAD': 'primary',
        'RAG': 'warning'
      };
      return typeMap[type] || '';
    },
    getModelTypeLabel(type) {
      const typeItem = this.modelTypes.find(item => item.value === type);
      return typeItem ? this.$t(typeItem.labelKey) : type;
    },
    isSensitiveField(fieldKey) {
      if (typeof fieldKey !== 'string') return false;
      return this.sensitive_keys.some(key =>
        fieldKey.toLowerCase().includes(key.toLowerCase())
      );
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchProviders();
    },
    goToPage(page) {
      if (page !== this.currentPage) {
        this.currentPage = page;
        this.fetchProviders();
      }
    },
    handleDropdownVisibleChange(visible) {
      this.DropdownVisible = visible;
    }
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  height: 100vh;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  text-align: start;
}

.page {
  flex: 1;
  min-height: 0;
  width: 100%;
  max-width: 1280px;
  padding: 32px 32px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading {
  min-width: 0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.3;
  color: var(--x-text);
}

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.provider-card {
  flex: 1;
  min-height: 0;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--x-border);
}

.search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 280px;
  max-width: 100%;
}

::v-deep .search-input .el-input__inner {
  height: 34px;
  line-height: 34px;
}

::v-deep .search-input .el-input__icon {
  line-height: 34px;
  color: var(--x-text-muted);
}

.btn-search {
  height: 34px;
  padding: 0 14px;
}

.filter-btn {
  height: 34px;
}

/* Table sits flush inside the card; CustomTable's own frame is removed */
.provider-table {
  ::v-deep .table-container {
    border: 0;
    border-radius: 0;
  }

  ::v-deep .table-footer {
    padding: 10px 16px;
    border-top: 1px solid var(--x-border);
    flex-wrap: wrap;
    gap: 8px;
  }

  ::v-deep .el-table {
    min-width: 0;
  }

  ::v-deep .el-table th.el-table__cell > .cell {
    white-space: nowrap;
  }

  ::v-deep .el-table th.el-table__cell > .cell,
  ::v-deep .el-table td.el-table__cell > .cell {
    padding-inline-start: 12px;
    padding-inline-end: 12px;
  }
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  ::v-deep .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.type-pill {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 10px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface-raised);
  color: var(--x-text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.cell-strong {
  font-weight: 500;
  color: var(--x-text);
}

.mono {
  font-family: var(--x-font-mono);
  font-size: 13px;
  color: var(--x-text-secondary);
}

.field-count {
  display: inline-block;
  min-width: 18px;
  margin-inline-start: 4px;
  padding: 0 5px;
  box-sizing: border-box;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-muted);
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.field-item {
  padding: 6px 0;
  border-bottom: 1px solid var(--x-divider);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;

  &:last-child {
    border-bottom: 0;
  }

  .field-label {
    flex: 1;
    min-width: 0;
    font-weight: 500;
    color: var(--x-text);
  }

  .field-type {
    font-family: var(--x-font-mono);
    font-size: 12px;
    color: var(--x-text-muted);
  }

  .sensitive-tag {
    padding: 0 8px;
    border-radius: var(--x-radius-pill);
    background: var(--x-warning-bg);
    color: var(--x-warning-text);
    font-size: 11px;
    font-weight: 500;
    line-height: 18px;
  }
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

.rotate-down {
  transform: rotate(180deg);
  transition: transform 0.3s ease;
}

.el-icon-arrow-down {
  transition: transform 0.3s ease;
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 900px) {
  .welcome {
    height: auto;
    min-height: calc(100vh - var(--x-topbar-height));
  }

  .provider-card {
    flex: none;
    height: 600px;
  }
}

@media (max-width: 768px) {
  .page {
    padding: 16px 16px 0;
    gap: 16px;
  }

  .search-group {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
  }
}
</style>
