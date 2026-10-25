<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('dictManagement.pageTitle') }}</h1>
          <p class="page-description">{{ $t('console.dictDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="showAddDictDataDialog">
            {{ $t('dictManagement.addDictData') }}
          </el-button>
        </div>
      </header>

      <div class="dict-layout">
        <!-- Left: dictionary type list -->
        <aside class="dict-type-panel">
          <div class="dict-type-header">
            <h2 class="dict-type-title">{{ $t('dictManagement.dictTypeCategory') }}</h2>
            <div class="dict-type-tools">
              <el-tooltip :content="$t('dictManagement.addDictType')" placement="top">
                <el-button type="text" class="icon-btn" icon="el-icon-plus"
                  :aria-label="$t('dictManagement.addDictType')" @click="showAddDictTypeDialog" />
              </el-tooltip>
              <el-tooltip :content="$t('dictManagement.batchDeleteDictType')" placement="top">
                <el-button type="text" class="icon-btn icon-btn--danger" icon="el-icon-delete"
                  :aria-label="$t('dictManagement.batchDeleteDictType')" @click="batchDeleteDictType" />
              </el-tooltip>
            </div>
          </div>
          <div class="dict-type-checkbox" v-loading="dictTypeLoading">
            <el-checkbox class="dict-type-all" :indeterminate="isIndeterminate" v-model="checkAll"
              @change="handleCheckAllChange">{{ $t('common.selectAll') }}</el-checkbox>
            <el-checkbox-group v-model="checkedDictTypesIds" @change="handleDictTypeSelectionChange">
              <div
                class="dict-type-item"
                :class="{ 'dict-type-active': selectedDictType?.id === item.id }"
                v-for="item in dictTypeList"
                :key="item.id"
              >
                <el-checkbox :label="item.dictName" :key="item.id">
                </el-checkbox>
                <button type="button" class="dict-type-item-content"
                  :aria-current="selectedDictType?.id === item.id ? 'true' : null"
                  @click="handleDictTypeRowClick(item)">
                  <span class="dict-type-name">{{ item.dictName }}</span>
                  <span v-if="item.dictType" class="dict-type-code">{{ item.dictType }}</span>
                </button>
                <el-button class="dict-type-edit-btn icon-btn" type="text" icon="el-icon-edit"
                  :aria-label="$t('dictManagement.editDictType')" @click.stop="editDictType(item)" />
              </div>
            </el-checkbox-group>
          </div>
        </aside>

        <!-- Right: dictionary data list -->
        <section class="dict-card">
          <div class="card-header">
            <h2 class="card-title">{{ selectedDictType ? selectedDictType.dictName : $t('dictManagement.pageTitle') }}</h2>
            <div class="search-group">
              <el-input :placeholder="$t('dictManagement.searchPlaceholder')"
                :aria-label="$t('dictManagement.searchPlaceholder')" v-model="search" class="search-input"
                prefix-icon="el-icon-search" @keyup.enter.native="handleSearch" clearable />
              <el-button class="btn-search" @click="handleSearch">
                {{ $t('dictManagement.search') }}
              </el-button>
            </div>
          </div>
          <CustomTable class="dict-table" :data="dictDataList" :columns="tableColumns" :loading="dictDataLoading"
            :show-selection="true" :show-operations="true" :operations-label="$t('dictManagement.operation')"
            :operations-width="150" :total="total" :current-page="currentPage" :page-size="pageSize"
            :page-size-options="pageSizeOptions" @size-change="handlePageSizeChange" @page-change="goToPage">
            <template slot="selection" slot-scope="scope">
              <el-checkbox v-model="scope.row.selected" :aria-label="scope.row.dictLabel"></el-checkbox>
            </template>
            <template slot="dictValue" slot-scope="scope">
              <span class="mono">{{ scope.row.dictValue }}</span>
            </template>
            <template slot="operations" slot-scope="scope">
              <div class="row-actions">
                <el-button type="text" size="mini" class="action-btn" @click="editDictData(scope.row)">
                  {{ $t('dictManagement.edit') }}
                </el-button>
                <el-button type="text" size="mini" class="action-btn action-btn--danger"
                  @click="deleteDictData(scope.row)">
                  {{ $t('dictManagement.delete') }}
                </el-button>
              </div>
            </template>
            <template slot="footer-btns">
              <div class="batch-actions">
                <CustomButton :icon="isAllDictDataSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'"
                  size="small" @click="selectAllDictData">
                  {{ isAllDictDataSelected ? $t('dictManagement.deselectAll') : $t('dictManagement.selectAll') }}
                </CustomButton>
                <CustomButton size="small" type="delete" icon="el-icon-delete" @click="batchDeleteDictData">
                  {{ $t('dictManagement.batchDeleteDictData') }}
                </CustomButton>
              </div>
            </template>
          </CustomTable>
        </section>
      </div>
    </main>

    <!-- Dictionary type edit dialog component -->
    <DictTypeDialog :visible.sync="dictTypeDialogVisible" :title="dictTypeDialogTitle" :dictTypeData="dictTypeForm"
      @save="saveDictType" />

    <!-- Dictionary data edit dialog component -->
    <DictDataDialog :visible.sync="dictDataDialogVisible" :title="dictDataDialogTitle" :dictData="dictDataForm"
      :dictTypeId="selectedDictType?.id" @save="saveDictData" />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import dictApi from '@/apis/module/dict'
import DictDataDialog from '@/components/DictDataDialog.vue'
import DictTypeDialog from '@/components/DictTypeDialog.vue'
import HeaderBar from '@/components/HeaderBar.vue'
import VersionFooter from '@/components/VersionFooter.vue'
import CustomButton from '@/components/CustomButton.vue'
import CustomTable from '@/components/CustomTable.vue'
export default {
  name: 'DictManagement',
  components: {
    HeaderBar,
    DictTypeDialog,
    DictDataDialog,
    VersionFooter,
    CustomButton,
    CustomTable
  },
  data() {
    return {
      // Dictionary type state
      dictTypeList: [],
      dictTypeLoading: false,
      selectedDictType: null,
      selectedDictTypes: [],
      dictTypeDialogVisible: false,
      dictTypeDialogTitle: 'Add Dictionary Type',
      dictTypeForm: {
        id: null,
        dictName: '',
        dictType: ''
      },

      // Dictionary data state
      dictDataList: [],
      dictDataLoading: false,
      isAllDictDataSelected: false,
      dictDataDialogVisible: false,
      dictDataDialogTitle: 'Add Dictionary Data',
      dictDataForm: {
        id: null,
        dictTypeId: null,
        dictLabel: '',
        dictValue: '',
        sort: 0
      },
      search: '',
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      tableColumns: [],
      isIndeterminate: false,
      checkAll: false,
      checkedDictTypesIds: []
    }
  },
  created() {
    this.initTableColumns()
    this.loadDictTypeList()
  },
  methods: {
    handleCheckAllChange(val) {
      // Decide whether to select all or deselect all based on the current selection
      const isAllSelected = this.checkedDictTypesIds.length === this.dictTypeList.length;
      if (isAllSelected) {
        // All selected: deselect all
        this.checkedDictTypesIds = [];
        this.checkAll = false;
        this.selectedDictTypes = [];
      } else {
        // Not all selected: select all
        this.checkedDictTypesIds = this.dictTypeList.map(item => item.dictName);
        this.checkAll = true;
        this.selectedDictTypes = [...this.dictTypeList];
      }
      // Reset the indeterminate state
      this.isIndeterminate = false;
    },
    handleDictTypeSelectionChange(checkedIds) {
      // Update the list of selected dictionary types
      this.selectedDictTypes = this.dictTypeList.filter(item => checkedIds.includes(item.dictName));
      // When partially selected, checkAll is true and isIndeterminate is true to show the indeterminate state
      this.checkAll = checkedIds.length > 0;
      this.isIndeterminate = checkedIds.length > 0 && checkedIds.length < this.dictTypeList.length;
    },
    initTableColumns() {
      this.tableColumns = [
        {
          prop: 'dictLabel',
          label: this.$t('dictManagement.dictLabel'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'dictValue',
          label: this.$t('dictManagement.dictValue'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'sort',
          label: this.$t('dictManagement.sort'),
          align: 'left',
          minWidth: 80
        }
      ]
    },
    // Dictionary type methods
    loadDictTypeList() {
      this.dictTypeLoading = true
      dictApi.getDictTypeList({
        page: 1,
        limit: 100,
        dictName: this.search
      }, ({ data }) => {
        if (data.code === 0) {
          this.dictTypeList = data.data.list
          if (this.dictTypeList.length > 0) {
            this.selectedDictType = this.dictTypeList[0]
            this.loadDictDataList(this.dictTypeList[0].id)
          }
        }
        this.dictTypeLoading = false
      })
    },
    handleDictTypeRowClick(row) {
      this.selectedDictType = row
      this.loadDictDataList(row.id)
    },
    showAddDictTypeDialog() {
      this.dictTypeDialogTitle = this.$t('dictManagement.addDictType')
      this.dictTypeForm = {
        id: null,
        dictName: '',
        dictType: ''
      }
      this.dictTypeDialogVisible = true
    },
    editDictType(row) {
      this.dictTypeDialogTitle = this.$t('dictManagement.editDictType')
      this.dictTypeForm = { ...row }
      this.dictTypeDialogVisible = true
    },
    saveDictType(formData) {
      const api = formData.id ? dictApi.updateDictType : dictApi.addDictType
      api(formData, ({ data }) => {
        if (data.code === 0) {
          this.$message.success(this.$t('dictManagement.saveSuccess'))
          this.dictTypeDialogVisible = false
          this.loadDictTypeList()
        }
      })
    },
    batchDeleteDictType() {
      if (this.selectedDictTypes.length === 0) {
        this.$message.warning(this.$t('dictManagement.selectDictTypeToDelete'))
        return
      }

      this.$confirm(this.$t('dictManagement.confirmDeleteDictType'), this.$t('dictManagement.confirm'), {
        confirmButtonText: this.$t('dictManagement.confirm'),
        cancelButtonText: this.$t('dictManagement.cancel'),
        type: 'warning'
      }).then(() => {
        const ids = this.selectedDictTypes.map(item => item.id)
        dictApi.deleteDictType(ids, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('dictManagement.deleteSuccess'))
            this.loadDictTypeList()
          }
        })
      })
    },

    // Dictionary data methods
    loadDictDataList(dictTypeId) {
      if (!dictTypeId) return
      this.dictDataLoading = true
      dictApi.getDictDataList({
        dictTypeId,
        page: this.currentPage,
        limit: this.pageSize,
        dictLabel: this.search,
        dictValue: ''
      }, ({ data }) => {
        if (data.code === 0) {
          this.dictDataList = data.data.list.map(item => ({
            ...item,
            selected: false
          }))
          this.total = data.data.total
        } else {
          this.$message.error(data.msg || this.$t('dictManagement.getDictDataFailed'))
        }
        this.dictDataLoading = false
      })
    },
    selectAllDictData() {
      this.isAllDictDataSelected = !this.isAllDictDataSelected
      this.dictDataList.forEach(row => {
        row.selected = this.isAllDictDataSelected
      })
    },
    showAddDictDataDialog() {
      if (!this.selectedDictType) {
        this.$message.warning(this.$t('dictManagement.selectDictTypeFirst'))
        return
      }
      this.dictDataDialogTitle = this.$t('dictManagement.addDictData')
      this.dictDataForm = {
        id: null,
        dictTypeId: this.selectedDictType.id,
        dictLabel: '',
        dictValue: '',
        sort: 0
      }
      this.dictDataDialogVisible = true
    },
    editDictData(row) {
      this.dictDataDialogTitle = this.$t('dictManagement.editDictData')
      this.dictDataForm = { ...row }
      this.dictDataDialogVisible = true
    },
    saveDictData(formData) {
      const api = formData.id ? dictApi.updateDictData : dictApi.addDictData
      api(formData, ({ data }) => {
        if (data.code === 0) {
          this.$message.success(this.$t('dictManagement.saveSuccess'))
          this.dictDataDialogVisible = false
          this.loadDictDataList(formData.dictTypeId)
        }
      })
    },
    deleteDictData(row) {
      this.$confirm(this.$t('dictManagement.confirmDeleteDictData'), this.$t('dictManagement.confirm'), {
        confirmButtonText: this.$t('dictManagement.confirm'),
        cancelButtonText: this.$t('dictManagement.cancel'),
        type: 'warning'
      }).then(() => {
        dictApi.deleteDictData([row.id], ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('dictManagement.deleteSuccess'))
            this.loadDictDataList(row.dictTypeId)
          }
        })
      })
    },
    batchDeleteDictData() {
      const selectedRows = this.dictDataList.filter(row => row.selected)
      if (selectedRows.length === 0) {
        this.$message.warning(this.$t('dictManagement.selectDictDataToDelete'))
        return
      }

      this.$confirm(this.$t('dictManagement.confirmBatchDeleteDictData', { count: selectedRows.length }), this.$t('dictManagement.confirm'), {
        confirmButtonText: this.$t('dictManagement.confirm'),
        cancelButtonText: this.$t('dictManagement.cancel'),
        type: 'warning'
      }).then(() => {
        const ids = selectedRows.map(item => item.id)
        dictApi.deleteDictData(ids, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('dictManagement.deleteSuccess'))
            this.loadDictDataList(this.selectedDictType.id)
          }
        })
      })
    },
    handleSearch() {
      if (!this.selectedDictType) {
        this.$message.warning(this.$t('dictManagement.selectDictTypeFirst'))
        return
      }
      this.currentPage = 1
      this.loadDictDataList(this.selectedDictType.id)
    },
    handlePageSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
      this.loadDictDataList(this.selectedDictType?.id)
    },
    goToPage(page) {
      if (page !== this.currentPage) {
        this.currentPage = page
        this.loadDictDataList(this.selectedDictType?.id)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.welcome {
  height: 100vh;
  min-height: 600px;
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

.dict-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 20px;
}

// Dictionary type list
.dict-type-panel {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.dict-type-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  padding: 10px 10px 10px 16px;
  border-bottom: 1px solid var(--x-border);
}

.dict-type-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
}

.dict-type-tools {
  display: flex;
  gap: 2px;
}

.icon-btn {
  width: 30px;
  height: 30px;
  padding: 0;
  margin: 0 !important;
  color: var(--x-text-muted);
  font-size: 15px;

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

.dict-type-checkbox {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;

  .dict-type-all {
    padding: 8px 10px;
    margin-inline-end: 0;
    color: var(--x-text-muted);

    ::v-deep .el-checkbox__label {
      font-size: 13px;
    }
  }

  ::v-deep .el-checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  // Item checkboxes keep their label for screen readers only
  ::v-deep .el-checkbox-group .el-checkbox__label {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip: rect(0 0 0 0);
    white-space: nowrap;
  }
}

.dict-type-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-inline-start: 10px;
  border-radius: var(--x-radius-control);
  transition: background 0.15s;

  .el-checkbox {
    margin-inline-end: 0;
  }

  &:hover {
    background: var(--x-surface-raised);
  }

  &.dict-type-active {
    background: var(--x-selected);

    .dict-type-name {
      font-weight: 600;
    }
  }

  .dict-type-edit-btn {
    flex-shrink: 0;
  }
}

.dict-type-item-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  padding: 7px 4px;
  border: 0;
  background: transparent;
  font: inherit;
  text-align: start;
  cursor: pointer;
  border-radius: 8px;

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.dict-type-name {
  max-width: 100%;
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dict-type-code {
  max-width: 100%;
  font-family: var(--x-font-mono);
  font-size: 11px;
  color: var(--x-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// Dictionary data card
.dict-card {
  flex: 1;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.card-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--x-border);
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
}

.search-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 240px;
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

/* Table sits flush inside the card; CustomTable's own frame is removed */
.dict-table {
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

.mono {
  font-family: var(--x-font-mono);
  font-size: 13px;
  color: var(--x-text-secondary);
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

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 900px) {
  .welcome {
    height: auto;
    min-height: calc(100vh - var(--x-topbar-height));
  }

  .dict-layout {
    flex: none;
    flex-direction: column;
  }

  .dict-type-panel {
    width: 100%;
    max-height: 280px;
  }

  .dict-card {
    flex: none;
    height: 560px;
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
