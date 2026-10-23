<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('replacementWordManagement.pageTitle') }}</h1>
          <p class="page-description">{{ $t('console.replacementWordsDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
            {{ $t('replacementWordManagement.addFile') }}
          </el-button>
        </div>
      </header>

      <section class="word-card">
        <div class="table-toolbar">
          <div class="search-group">
            <el-input
              :placeholder="$t('replacementWordManagement.searchPlaceholder')"
              :aria-label="$t('replacementWordManagement.searchPlaceholder')"
              v-model="searchKeyword"
              class="search-input"
              prefix-icon="el-icon-search"
              @keyup.enter.native="handleSearch"
              clearable
            />
            <el-button class="btn-search" @click="handleSearch">
              {{ $t('replacementWordManagement.search') }}
            </el-button>
          </div>
        </div>
        <CustomTable
          ref="paramsTable"
          class="word-table"
          :data="filteredParamsList"
          :columns="tableColumns"
          :loading="loading"
          :show-selection="true"
          :show-operations="true"
          :operations-label="$t('replacementWordManagement.operation')"
          :operations-width="210"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-size-options="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @page-change="goToPage"
        >
          <template slot="fileName" slot-scope="scope">
            <span class="cell-strong">{{ scope.row.fileName }}</span>
          </template>
          <template slot="content" slot-scope="scope">
            <el-tooltip placement="right" effect="light" popper-class="replace-word-tooltip">
              <div slot="content" class="replace-word-content">
                <span
                  v-for="(item, index) in scope.row.content"
                  :key="index"
                  class="custom-tag"
                >
                  {{ item }}
                </span>
              </div>
              <span class="content-text">{{ formatContent(scope.row.content) }}</span>
            </el-tooltip>
          </template>
          <template slot="operations" slot-scope="scope">
            <div class="row-actions">
              <el-button size="mini" type="text" class="action-btn" @click="handleEdit(scope.row)">
                {{ $t('replacementWordManagement.edit') }}
              </el-button>
              <el-button size="mini" type="text" class="action-btn" @click="handleDownload(scope.row)">
                {{ $t('replacementWordManagement.download') }}
              </el-button>
              <el-button size="mini" type="text" class="action-btn action-btn--danger" @click="handleDelete(scope.row)">
                {{ $t('replacementWordManagement.delete') }}
              </el-button>
            </div>
          </template>
          <template slot="footer-btns">
            <div class="batch-actions">
              <CustomButton
                :icon="allSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'"
                size="small"
                @click="handleSelectAll"
              >
                {{ allSelected ? $t('user.deselectAll') : $t('user.selectAll') }}
              </CustomButton>
              <CustomButton size="small" type="delete" icon="el-icon-delete" @click="handleBatchDelete">
                {{ $t('replacementWordManagement.batchDelete') }}
              </CustomButton>
            </div>
          </template>
        </CustomTable>
      </section>
    </main>

    <ReplacementWordDialog
      ref="paramDialog"
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      :form="dialogForm"
      @submit="handleSubmit"
      @cancel="dialogVisible = false"
    />
    <el-footer><VersionFooter/></el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import ReplacementWordDialog from "@/components/ReplacementWordDialog.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";

export default {
  components: { HeaderBar, VersionFooter, ReplacementWordDialog, CustomButton, CustomTable },
  data() {
    return {
      searchKeyword: "",
      paramsList: [],
      selectedRows: new Set(),
      currentPage: 1,
      loading: false,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      dialogForm: {},
      tableColumns: []
    };
  },
  computed: {
    filteredParamsList() {
      if (!this.searchKeyword) {
        return this.paramsList;
      }
      const keyword = this.searchKeyword.toLowerCase();
      return this.paramsList.filter(row =>
        (row.fileName || '').toLowerCase().includes(keyword)
      );
    },

    hasAnySelected() {
      return this.selectedRows.size > 0;
    },

    allSelected() {
      if (this.filteredParamsList.length === 0) {
        return false;
      }
      return this.filteredParamsList.every(row => this.selectedRows.has(row.id));
    }
  },
  created() {
    this.initTableColumns();
    this.fetchFileList();
  },
  mounted() {
    this.dialogTitle = this.$t('replacementWordManagement.addFile');
  },
  methods: {
    initTableColumns() {
      this.tableColumns = [
        {
          prop: 'fileName',
          label: this.$t('replacementWordManagement.fileName'),
          align: 'left',
          minWidth: 140
        },
        {
          prop: 'wordCount',
          label: this.$t('replacementWordManagement.replacementWordCount'),
          align: 'left',
          minWidth: 110
        },
        {
          prop: 'content',
          label: this.$t('replacementWordManagement.replacementWordContent'),
          align: 'left',
          minWidth: 200,
          slot: 'content'
        },
        {
          prop: 'createdAt',
          label: this.$t('replacementWordManagement.createTime'),
          align: 'left',
          minWidth: 150
        },
        {
          prop: 'updatedAt',
          label: this.$t('replacementWordManagement.updateTime'),
          align: 'left',
          minWidth: 150
        }
      ];
    },

    formatContent(content) {
      if (!content) return '';
      if (Array.isArray(content)) {
        return content.join(',');
      }
      return content;
    },

    handleSearch() {
      this.currentPage = 1;
    },

    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchFileList();
    },

    goToPage(page) {
      if (page !== this.currentPage) {
        this.currentPage = page;
        this.fetchFileList();
      }
    },

    handleCheckboxChange(row) {
      const newSet = new Set(this.selectedRows);
      if (row.selected) {
        newSet.add(row.id);
      } else {
        newSet.delete(row.id);
      }
      this.selectedRows = newSet;
    },

    handleSelectAll() {
      if (this.allSelected) {
        this.filteredParamsList.forEach(row => {
          this.$set(row, 'selected', false);
        });
        this.selectedRows = new Set();
      } else {
        this.filteredParamsList.forEach(row => {
          this.$set(row, 'selected', true);
        });
        this.selectedRows = new Set(this.filteredParamsList.map(row => row.id));
      }
    },

    handleBatchDelete() {
      const ids = Array.from(this.filteredParamsList)
        .filter(row => row.selected)
        .map(row => row.id);

      if (ids.length === 0) {
        return;
      }

      this.$confirm(
        this.$t('replacementWordManagement.confirmBatchDelete', { count: ids.length }),
        this.$t('replacementWordManagement.batchDeleteHint'),
        {
          confirmButtonText: this.$t('common.confirm'),
          cancelButtonText: this.$t('common.cancel')
        }
      ).then(() => {
        Api.correctWord.batchDeleteFile(ids, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('common.deleteSuccess'));

            const newSet = new Set(this.selectedRows);
            ids.forEach(id => {
              newSet.delete(id);
            });
            this.selectedRows = newSet;

            this.fetchFileList();
          } else {
            this.$message.error(data.msg || this.$t('common.deleteFailure'));
          }
        });
      }).catch(() => {});
    },

    fetchFileList() {
      this.loading = true;
      Api.correctWord.getFileList({
        page: this.currentPage,
        pageSize: this.pageSize
      }, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.paramsList = (data.data.list || []).map(row => ({
            ...row,
            selected: this.selectedRows.has(row.id)
          }));

          this.total = data.data.total || 0;
        } else {
          this.$message.error({
            message: data.msg || this.$t('replacementWordManagement.getListFailed'),
            showClose: true
          });
        }
      });
    },

    handleAdd() {
      this.dialogForm = {
        id: undefined,
        fileName: '',
        content: ''
      };
      this.dialogTitle = this.$t('replacementWordManagement.addFile');
      this.dialogVisible = true;
    },

    handleEdit(row) {
      this.dialogForm = {
        id: row.id,
        fileName: row.fileName,
        content: row.content || ''
      };
      this.dialogTitle = this.$t('replacementWordManagement.edit');
      this.dialogVisible = true;
    },

    handleDownload(row) {
      Api.correctWord.downloadFile(row.id, (res) => {
        const url = window.URL.createObjectURL(new Blob([res.data]));
        const link = document.createElement('a');
        link.href = url;
        link.download = `${row.fileName}.txt`;
        link.click();
        window.URL.revokeObjectURL(url);
      });
    },

    handleDelete(row) {
      this.$confirm(
        this.$t('replacementWordManagement.confirmDelete'),
        this.$t('replacementWordManagement.deleteHint'),
        {
          confirmButtonText: this.$t('common.confirm'),
          cancelButtonText: this.$t('common.cancel')
        }
      ).then(() => {
        Api.correctWord.deleteFile(row.id, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('common.deleteSuccess'));
            const newSet = new Set(this.selectedRows);
            newSet.delete(row.id);
            this.selectedRows = newSet;
            this.fetchFileList();
          } else {
            this.$message.error(data.msg || this.$t('common.deleteFailure'));
          }
        });
      }).catch(() => {});
    },

    handleSubmit(formData) {
      if (formData.id) {
        Api.correctWord.updateFile(formData, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('replacementWordManagement.saveSuccess'));
            this.dialogVisible = false;
            this.fetchFileList();
          } else {
            this.$message.error(data.msg || this.$t('replacementWordManagement.saveFailed'));
          }
          if (this.$refs.paramDialog) {
            this.$refs.paramDialog.resetSaving();
          }
        });
      } else {
        Api.correctWord.addFile(formData, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t('common.addSuccess'));
            this.dialogVisible = false;
            this.fetchFileList();
          } else {
            this.$message.error(data.msg || this.$t('replacementWordManagement.addFailed'));
          }
          if (this.$refs.paramDialog) {
            this.$refs.paramDialog.resetSaving();
          }
        });
      }
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
  text-align: left;
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

.word-card {
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

/* Table sits flush inside the card; CustomTable's own frame is removed */
.word-table {
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
    padding-left: 12px;
    padding-right: 12px;
  }
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  ::v-deep .el-button + .el-button {
    margin-left: 0;
  }
}

.cell-strong {
  font-weight: 500;
  color: var(--x-text);
}

.content-text {
  display: block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.custom-tag {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface-raised);
  color: var(--x-text-secondary);
  font-size: 12px;
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

  .word-card {
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

<style>
.replace-word-tooltip.is-light {
  border-color: var(--x-border);
  border-radius: 12px;
  box-shadow: var(--x-shadow-raised);
}

.replace-word-tooltip .popper__arrow {
  border-top-color: transparent !important;
  border-right-color: var(--x-border) !important;
}

.replace-word-content {
  max-width: 400px;
  max-height: 60vh;
  overflow-y: auto;
  scrollbar-width: thin;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
