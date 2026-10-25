<template>
  <div class="welcome">
    <HeaderBar />

    <main class="admin-page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('header.userManagement') }}</h1>
          <p class="page-description">{{ $t('console.usersDescription') }}</p>
        </div>
      </header>

      <section class="table-card">
        <div class="table-toolbar">
          <div class="search-group">
            <el-input :placeholder="$t('user.searchPhone')" :aria-label="$t('user.searchPhone')" v-model="searchPhone"
              class="search-input" prefix-icon="el-icon-search" clearable @keyup.enter.native="handleSearch" />
            <el-button class="btn-search" @click="handleSearch">{{ $t('user.search') }}</el-button>
          </div>
          <div class="toolbar-actions">
            <el-button size="small" @click="handleSelectAll">
              {{ isAllSelected ? $t('user.deselectAll') : $t('user.selectAll') }}
            </el-button>
            <el-button size="small" @click="batchDisable">{{ $t('user.disable') }}</el-button>
            <el-button size="small" @click="batchEnable">{{ $t('user.enable') }}</el-button>
            <el-button size="small" class="btn-danger-outline" @click="batchDelete">{{ $t('user.delete') }}</el-button>
          </div>
        </div>
        <CustomTable
          ref="userTable"
          class="flush-table"
          :data="userList"
          :columns="tableColumns"
          :loading="loading"
          :loading-text="$t('modelConfig.loading')"
          :show-selection="true"
          :show-operations="true"
          :operations-label="$t('modelConfig.action')"
          :operations-width="350"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-size-options="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @page-change="goToPage"
        >
          <template slot="selection" slot-scope="scope">
            <el-checkbox v-model="scope.row.selected"></el-checkbox>
          </template>
          <template slot="userid" slot-scope="scope">
            <span class="mono">{{ scope.row.userid }}</span>
          </template>
          <template slot="mobile" slot-scope="scope">
            <span class="cell-strong">{{ scope.row.mobile }}</span>
          </template>
          <template slot="status" slot-scope="scope">
            <span v-if="scope.row.status === 1" class="status-pill status-pill--success">
              <span class="status-pill-dot" aria-hidden="true"></span>{{ $t('user.normal') }}
            </span>
            <span v-else class="status-pill status-pill--danger">
              <span class="status-pill-dot" aria-hidden="true"></span>{{ $t('user.disabled') }}
            </span>
          </template>
          <template slot="operations" slot-scope="scope">
            <div class="row-actions">
              <el-button size="mini" type="text" class="row-action" @click="resetPassword(scope.row)">
                {{ $t('user.resetPassword') }}
              </el-button>
              <el-button size="mini" type="text" class="row-action" v-if="scope.row.status === 1"
                @click="handleChangeStatus(scope.row, 0)">{{ $t('user.disableAccount') }}</el-button>
              <el-button size="mini" type="text" class="row-action" v-if="scope.row.status === 0"
                @click="handleChangeStatus(scope.row, 1)">{{ $t('user.enableAccount') }}</el-button>
              <el-button size="mini" type="text" class="row-action row-action--danger" @click="deleteUser(scope.row)">
                {{ $t('user.deleteUser') }}
              </el-button>
            </div>
          </template>
        </CustomTable>
      </section>
    </main>

    <view-password-dialog :visible.sync="showViewPassword" :password="currentPassword" />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import ViewPasswordDialog from "@/components/ViewPasswordDialog.vue";
import CustomTable from "@/components/CustomTable.vue";
export default {
  components: { HeaderBar, ViewPasswordDialog, VersionFooter, CustomTable },
  data() {
    return {
      showViewPassword: false,
      currentPassword: "",
      searchPhone: "",
      userList: [],
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      isAllSelected: false,
      loading: false,
      tableColumns: []
    };
  },
  created() {
    this.initTableColumns();
    this.fetchUsers();
  },
  methods: {
    initTableColumns() {
      this.tableColumns = [
        {
          prop: 'userid',
          label: this.$t('user.userid'),
          align: 'left',
          minWidth: 110
        },
        {
          prop: 'mobile',
          label: this.$t('user.mobile'),
          align: 'left',
          minWidth: 150
        },
        {
          prop: 'deviceCount',
          label: this.$t('user.deviceCount'),
          align: 'left',
          minWidth: 110
        },
        {
          prop: 'createDate',
          label: this.$t('user.createDate'),
          align: 'left',
          minWidth: 170
        },
        {
          prop: 'status',
          label: this.$t('user.status'),
          align: 'left',
          minWidth: 120
        }
      ];
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchUsers();
    },

    fetchUsers() {
      this.loading = true;
      Api.admin.getUserList(
        {
          page: this.currentPage,
          limit: this.pageSize,
          mobile: this.searchPhone,
        },
        ({ data }) => {
          this.loading = false; // Stop loading
          if (data.code === 0) {
            this.userList = data.data.list.map(item => ({
              ...item,
              selected: false
            }));
            this.total = data.data.total;
          }
        }
      );
    },
    handleSearch() {
      this.currentPage = 1;
      this.fetchUsers();
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.userList.forEach(row => {
        row.selected = this.isAllSelected;
      });
    },
    batchDelete() {
      const selectedUsers = this.userList.filter(user => user.selected);
      if (selectedUsers.length === 0) {
        this.$message.warning(this.$t('user.selectUsersFirst'));
        return;
      }

      this.$confirm(this.$t('user.confirmDeleteSelected', { count: selectedUsers.length }), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: "warning",
      })
        .then(async () => {
          const loading = this.$loading({
            lock: true,
            text: this.$t('user.deleting'),
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });

          try {
            const results = await Promise.all(
              selectedUsers.map((user) => {
                return new Promise((resolve) => {
                  Api.admin.deleteUser(user.userid, ({ data }) => {
                    if (data.code === 0) {
                      resolve({ success: true, userid: user.userid });
                    } else {
                      resolve({ success: false, userid: user.userid, msg: data.msg });
                    }
                  });
                });
              })
            );

            const successCount = results.filter((r) => r.success).length;
            const failCount = results.length - successCount;

            if (failCount === 0) {
              this.$message.success({
                message: this.$t('user.deleteSuccess', { count: successCount }),
                showClose: true
              });
            } else if (successCount === 0) {
              this.$message.error({
                message: this.$t('user.deleteFailed'),
                showClose: true
              });
            } else {
              this.$message.warning(
                this.$t('user.partialDelete', { successCount: successCount, failCount: failCount })
              );
            }

            this.fetchUsers();
          } catch (error) {
            this.$message.error(this.$t('user.deleteError'));
          } finally {
            loading.close();
          }
        })
        .catch(() => {
          this.$message.info(this.$t('user.deleteCancelled'));
        });
    },
    batchEnable() {
      const selectedUsers = this.userList.filter(user => user.selected);
      if (selectedUsers.length === 0) {
        this.$message.warning(this.$t('user.selectUsersToEnable'));
        return;
      }
      this.handleChangeStatus(selectedUsers, 1);
    },
    batchDisable() {
      const selectedUsers = this.userList.filter(user => user.selected);
      if (selectedUsers.length === 0) {
        this.$message.warning(this.$t('user.selectUsersToDisable'));
        return;
      }
      this.handleChangeStatus(selectedUsers, 0);
    },
    resetPassword(row) {
      this.$confirm(this.$t('user.confirmResetPassword'), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      }).then(() => {
        Api.admin.resetUserPassword(row.userid, ({ data }) => {
          if (data.code === 0) {
            // Show the generated default password
            this.$alert(this.$t('user.resetPasswordSuccess') + '\n\n' + this.$t('user.generatedPassword') + ': ' + data.data, this.$t('common.success'), {
              confirmButtonText: this.$t('common.confirm'),
              dangerouslyUseHTMLString: true
            });
            this.fetchUsers();
          } else {
            this.$message.error(data.msg || this.$t('user.operationFailed'));
          }
        });
      }).catch(() => {
        this.$message.info(this.$t('common.deleteCancelled'));
      });
    },
    deleteUser(row) {
      this.$confirm(this.$t('user.confirmDeleteUser'), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      }).then(() => {
        Api.admin.deleteUser(row.userid, ({ data }) => {
          if (data.code === 0) {
            // After deletion, check whether the page number needs adjusting
            const newTotal = this.total - 1;
            const maxPage = Math.max(1, Math.ceil(newTotal / this.pageSize));
            if (this.currentPage > maxPage) {
              this.currentPage = maxPage;
            }
            this.$message.success(this.$t('user.deleteUserSuccess'));
            this.fetchUsers();
          } else {
            this.$message.error(data.msg || this.$t('user.operationFailed'));
          }
        });
      }).catch(() => {
        this.$message.info(this.$t('common.deleteCancelled'));
      });
    },
    goToPage(page) {
      if (page !== this.currentPage) {
        this.currentPage = page;
        this.fetchUsers();
      }
    },
    handleChangeStatus(row, status) {
      // Handle a single user or an array of users
      const users = Array.isArray(row) ? row : [row];
      const actionText = status === 0 ? this.$t('user.disable') : this.$t('user.enable');
      const userCount = users.length;

      this.$confirm(this.$t('user.confirmStatusChange', { action: actionText, count: userCount }), this.$t('common.warning'), {
        confirmButtonText: this.$t('common.confirm'),
        cancelButtonText: this.$t('common.cancel'),
        type: 'warning'
      }).then(() => {
        const userIds = users.map(user => user.userid);
        if (userIds.some(id => isNaN(id))) {
          this.$message.error(this.$t('user.invalidUserId'));
          return;
        }

        Api.user.changeUserStatus(status, userIds, ({ data }) => {
          if (data.code === 0) {
            this.$message.success({
              message: this.$t('user.statusChangeSuccess', { action: actionText, count: userCount }),
              showClose: true
            });
            this.fetchUsers(); // Refresh the user list
          } else {
            this.$message.error({
              message: this.$t('user.operationFailed'),
              showClose: true
            });
          }
        });
      }).catch(() => {
        // User cancelled the action
      });
    },
    // This method has been replaced by batchDelete; kept for backward compatibility
    handleBatchDelete() {
      this.batchDelete();
    },
    // This method has been fixed to use existing functionality
    handleBatchStatusChange(status) {
      const selectedUsers = this.userList.filter(user => user.selected);
      if (selectedUsers.length === 0) {
        this.$message.warning(this.$t('user.selectUsersFirst'));
        return;
      }

      // Call the existing handleChangeStatus method which already handles both single and multiple users
      this.handleChangeStatus(selectedUsers, status);
    },
  },
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

.admin-page {
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

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.table-card {
  flex: 1;
  min-height: 0;
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
  min-width: 0;
}

.search-input {
  width: 280px;
  max-width: 100%;

  ::v-deep .el-input__inner {
    height: 34px;
    line-height: 34px;
  }

  ::v-deep .el-input__icon {
    line-height: 34px;
    color: var(--x-text-muted);
  }
}

.btn-search {
  height: 34px;
  padding: 0 14px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.btn-danger-outline {
  color: var(--x-danger-text);
  border-color: var(--x-border-control);
  border-color: color-mix(in srgb, var(--x-danger) 30%, transparent);

  &:not(.is-disabled):hover,
  &:not(.is-disabled):focus {
    color: var(--x-danger-text);
    background: var(--x-danger-bg);
    border-color: color-mix(in srgb, var(--x-danger) 45%, transparent);
  }
}

/* Table sits flush inside the card; CustomTable's own frame is removed */
.flush-table {
  ::v-deep .table-container {
    border: 0;
    border-radius: 0;
  }

  ::v-deep .table-footer {
    padding: 10px 16px;
    border-top: 1px solid var(--x-border);
    justify-content: flex-end;
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

  ::v-deep .el-checkbox__inner {
    border-color: var(--x-text-disabled);
    border-radius: 4px;
  }
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

.muted {
  color: var(--x-text-muted);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;

  &--success {
    background: var(--x-success-bg);
    color: var(--x-success-text);

    .status-pill-dot {
      background: var(--x-success);
    }
  }

  &--danger {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);

    .status-pill-dot {
      background: var(--x-danger);
    }
  }
}

.status-pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.row-actions {
  display: flex;
  flex-wrap: nowrap;
  white-space: nowrap;
  justify-content: center;
  gap: 2px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.row-action {
  padding: 6px 8px;
  font-size: 13px;
  color: var(--x-text-secondary);

  &:hover,
  &:focus {
    color: var(--x-text);
    background: var(--x-canvas);
  }

  &--danger,
  &--danger:hover,
  &--danger:focus {
    color: var(--x-danger-text);
  }

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
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

  .table-card {
    flex: none;
    height: 600px;
  }
}

@media (max-width: 768px) {
  .admin-page {
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

  .flush-table ::v-deep .table-footer {
    justify-content: flex-start;
    overflow-x: auto;
  }
}
</style>
