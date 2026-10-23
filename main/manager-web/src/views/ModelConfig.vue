<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t("header.modelConfig") }}</h1>
          <p class="page-description">{{ $t("console.modelsDescription") }}</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="addModel">
            {{ $t("modelConfigDialog.addModel") }}
          </el-button>
        </div>
      </header>

      <div class="model-layout">
        <!-- Model type list -->
        <div class="type-list" role="tablist" :aria-label="$t('header.modelConfig')">
          <button
            v-for="type in modelTypes"
            :key="type"
            type="button"
            role="tab"
            class="type-tab"
            :class="{ 'is-active': activeTab === type }"
            :aria-selected="activeTab === type ? 'true' : 'false'"
            @click="handleMenuSelect(type)"
          >
            {{ $t("modelConfig." + type) }}
          </button>
        </div>

        <!-- Model table -->
        <section class="model-card">
          <div class="card-header">
            <h2 class="card-title">{{ $t("modelConfig." + activeTab) }}</h2>
            <div class="search-group">
              <el-input
                :placeholder="$t('modelConfig.searchPlaceholder')"
                :aria-label="$t('modelConfig.searchPlaceholder')"
                v-model="search"
                class="search-input"
                prefix-icon="el-icon-search"
                clearable
                @keyup.enter.native="handleSearch"
              />
              <el-button class="btn-search" @click="handleSearch">
                {{ $t("modelConfig.search") }}
              </el-button>
            </div>
          </div>

          <div class="table-scroll">
            <el-table
              ref="modelTable"
              style="width: 100%"
              v-loading="loading"
              :element-loading-text="$t('modelConfig.loading')"
              element-loading-spinner="el-icon-loading"
              element-loading-background="rgba(255, 255, 255, 0.7)"
              :data="modelList"
              class="model-table"
              :header-cell-class-name="headerCellClassName"
              @selection-change="handleSelectionChange"
            >
              <el-table-column
                type="selection"
                width="64"
                align="center"
                :cell-class-name="selectionCellClassName"
              ></el-table-column>
              <el-table-column :label="$t('modelConfig.modelName')" min-width="180">
                <template slot-scope="scope">
                  <div class="name-cell">
                    <div class="name-line">
                      <span class="model-name">{{ scope.row.modelName }}</span>
                      <span v-if="scope.row.isDefault === 1" class="badge-default">
                        {{ $t("modelConfig.isDefault") }}
                      </span>
                    </div>
                    <span class="model-id" :title="$t('modelConfig.modelId')">{{ scope.row.id }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modelConfig.provider')" min-width="120">
                <template slot-scope="scope">
                  <span class="provider">{{ scope.row.configJson.type || $t("modelConfig.unknown") }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('modelConfig.isEnabled')" width="110">
                <template slot-scope="scope">
                  <el-tooltip
                    v-if="scope.row.isDefault === 1 && scope.row.isEnabled === 1"
                    :content="$t('modelConfig.defaultModelCannotDisable')"
                    placement="top"
                  >
                    <span class="switch-wrap">
                      <el-switch
                        v-model="scope.row.isEnabled"
                        :active-value="1"
                        :inactive-value="0"
                        :aria-label="$t('modelConfig.isEnabled')"
                        disabled
                        @change="handleStatusChange(scope.row)"
                      />
                    </span>
                  </el-tooltip>
                  <el-switch
                    v-else
                    v-model="scope.row.isEnabled"
                    :active-value="1"
                    :inactive-value="0"
                    :aria-label="$t('modelConfig.isEnabled')"
                    @change="handleStatusChange(scope.row)"
                  />
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('modelConfig.action')"
                align="right"
                :width="activeTab === 'tts' ? 390 : 270"
              >
                <template slot-scope="scope">
                  <div class="row-actions">
                    <el-button type="text" size="mini" class="action-btn" @click="editModel(scope.row)">
                      {{ $t("modelConfig.edit") }}
                    </el-button>
                    <el-button
                      v-if="scope.row.isDefault !== 1"
                      type="text"
                      size="mini"
                      class="action-btn"
                      @click="handleDefaultChange(scope.row)"
                    >
                      {{ $t("modelConfigDialog.setDefault") }}
                    </el-button>
                    <el-button
                      v-if="activeTab === 'tts'"
                      type="text"
                      size="mini"
                      class="action-btn"
                      @click="openTtsDialog(scope.row)"
                    >
                      {{ $t("modelConfig.voiceManagement") }}
                    </el-button>
                    <el-tooltip :content="$t('modelConfig.duplicate')" placement="top">
                      <el-button
                        type="text"
                        size="mini"
                        class="icon-btn"
                        icon="el-icon-document-copy"
                        :aria-label="$t('modelConfig.duplicate')"
                        @click="duplicateModel(scope.row)"
                      />
                    </el-tooltip>
                    <el-tooltip :content="$t('modelConfig.delete')" placement="top">
                      <el-button
                        type="text"
                        size="mini"
                        class="icon-btn icon-btn--danger"
                        icon="el-icon-delete"
                        :aria-label="$t('modelConfig.delete')"
                        @click="deleteModel(scope.row)"
                      />
                    </el-tooltip>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="table-footer">
            <div class="batch-actions">
              <CustomButton
                :icon="isAllSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'"
                type="default"
                size="small"
                @click="selectAll"
              >
                {{ isAllSelected ? $t("modelConfig.deselectAll") : $t("modelConfig.selectAll") }}
              </CustomButton>
              <CustomButton type="delete" size="small" icon="el-icon-delete" @click="batchDelete">
                {{ $t("modelConfig.delete") }}
              </CustomButton>
            </div>
            <CustomPagination
              :total="total"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-size-options="pageSizeOptions"
              @size-change="handlePageSizeChange"
              @page-change="handlePageChange"
            />
          </div>
        </section>
      </div>

      <ModelEditDialog
        :modelType="activeTab"
        :visible.sync="editDialogVisible"
        :modelData="editModelData"
        @save="handleModelSave"
      />
      <TtsModel
        :visible.sync="ttsDialogVisible"
        :ttsModelId="selectedTtsModelId"
        :modelConfig="selectedModelConfig"
      />
      <AddModelDialog
        :modelType="activeTab"
        :visible.sync="addDialogVisible"
        @confirm="handleAddConfirm"
      />
    </main>
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import AddModelDialog from "@/components/AddModelDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import ModelEditDialog from "@/components/ModelEditDialog.vue";
import TtsModel from "@/components/TtsModel.vue";
import CustomPagination from "@/components/CustomPagination.vue";
import CustomButton from "@/components/CustomButton.vue";
import VersionFooter from "@/components/VersionFooter.vue";
export default {
  components: { HeaderBar, ModelEditDialog, TtsModel, AddModelDialog, VersionFooter, CustomPagination, CustomButton },
  data() {
    return {
      addDialogVisible: false,
      modelTypes: ["vad", "asr", "llm", "vllm", "intent", "tts", "memory", "rag"],
      activeTab: "llm",
      search: "",
      editDialogVisible: false,
      editModelData: {},
      ttsDialogVisible: false,
      selectedTtsModelId: "",
      modelList: [],
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      selectedModels: [],
      isAllSelected: false,
      loading: false,
      selectedModelConfig: {},
    };
  },

  created() {
    this.loadData();
  },

  mounted() {
    // Make sure the translated header text is displayed after the component mounts
    setTimeout(() => {
      this.updateSelectionHeaderText();
    }, 100);
  },

  updated() {
    // Re-apply the translated header text after the component updates
    this.updateSelectionHeaderText();
  },

  computed: {
    modelTypeText() {
      return (
        this.$t("modelConfig." + this.activeTab) || this.$t("modelConfig.modelConfig")
      );
    },
  },

  methods: {
    // Update the translated text of the selection column header
    updateSelectionHeaderText() {
      const thElement = document.querySelector(`.el-table__header th:nth-child(1) .cell`);
      if (thElement) {
        thElement.setAttribute("data-content", this.$t("modelConfig.select"));
      }
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.loadData();
    },
    openTtsDialog(row) {
      this.selectedTtsModelId = row.id;
      this.selectedModelConfig = row;
      this.ttsDialogVisible = true;
    },
    headerCellClassName({ column, columnIndex }) {
      if (columnIndex === 0) {
        return "custom-selection-header";
      }
      return "";
    },
    selectionCellClassName({ row, column, rowIndex, columnIndex }) {
      // Only set data-content on the header row
      if (rowIndex === undefined) {
        // Use setTimeout to make sure the DOM has finished rendering
        setTimeout(() => {
          const thElement = document.querySelector(
            `.el-table__header th:nth-child(1) .cell`
          );
          if (thElement) {
            thElement.setAttribute("data-content", this.$t("modelConfig.select"));
          }
        }, 0);
      }
      return "";
    },
    handleMenuSelect(index) {
      this.activeTab = index;
      this.currentPage = 1; // Reset to the first page
      this.pageSize = 10; // Optional: reset the page size
      this.loadData();
    },
    handleSearch() {
      this.currentPage = 1;
      this.loadData();
    },
    // Batch delete
    batchDelete() {
      if (this.selectedModels.length === 0) {
        this.$message.warning(this.$t("modelConfig.selectModelsFirst"));
        return;
      }

      this.$confirm(this.$t("modelConfig.confirmBatchDelete"), this.$t("message.info"), {
        confirmButtonText: this.$t("common.confirm"),
        cancelButtonText: this.$t("common.cancel"),
        type: "warning",
      })
        .then(() => {
          const deletePromises = this.selectedModels.map(
            (model) =>
              new Promise((resolve) => {
                Api.model.deleteModel(model.id, ({ data }) => resolve(data.code === 0));
              })
          );

          Promise.all(deletePromises).then((results) => {
            if (results.every(Boolean)) {
              this.$message.success({
                message: this.$t("modelConfig.batchDeleteSuccess"),
                showClose: true,
              });
              this.loadData();
            } else {
              this.$message.error({
                message: this.$t("modelConfig.partialDeleteFailed"),
                showClose: true,
              });
            }
          });
        })
        .catch(() => {
          this.$message.info(this.$t("modelConfig.deleteCancelled"));
        });
    },
    addModel() {
      this.addDialogVisible = true;
    },
    editModel(model) {
      this.editModelData = JSON.parse(JSON.stringify(model));
      this.editDialogVisible = true;
    },
    duplicateModel(model) {
      this.editModelData = JSON.parse(JSON.stringify(model));
      this.editModelData.duplicateMode = true;
      this.editDialogVisible = true;
    },
    // Delete a single model
    deleteModel(model) {
      this.$confirm(this.$t("modelConfig.confirmDelete"), this.$t("message.info"), {
        confirmButtonText: this.$t("common.confirm"),
        cancelButtonText: this.$t("common.cancel"),
        type: "warning",
      })
        .then(() => {
          Api.model.deleteModel(model.id, ({ data }) => {
            if (data.code === 0) {
              this.$message.success({
                message: this.$t("modelConfig.deleteSuccess"),
                showClose: true,
              });
              this.loadData();
            } else {
              this.$message.error({
                message: data.msg || this.$t("modelConfig.deleteFailed"),
                showClose: true,
              });
            }
          });
        })
        .catch(() => {
          this.$message.info(this.$t("modelConfig.deleteCancelled"));
        });
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.loadData();
    },
    handleModelSave({ provideCode, formData, done }) {
      const modelType = this.activeTab;
      const id = formData.id;

      if (this.editModelData.duplicateMode) {
        formData.id = "";
        Api.model.addModel({ modelType, provideCode, formData }, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t("modelConfig.duplicateSuccess"));
            this.loadData();
            this.editDialogVisible = false;
          } else {
            this.$message.error(data.msg || this.$t("modelConfig.duplicateFailed"));
          }
          done && done(); // Call the done callback to clear the loading state
        });
      } else {
        Api.model.updateModel({ modelType, provideCode, id, formData }, ({ data }) => {
          if (data.code === 0) {
            this.$message.success(this.$t("modelConfig.saveSuccess"));
            this.loadData();
            this.editDialogVisible = false;
          } else {
            this.$message.error(data.msg || this.$t("modelConfig.saveFailed"));
          }
          done && done(); // Call the done callback to clear the loading state
        });
      }
    },
    selectAll() {
      if (this.isAllSelected) {
        this.$refs.modelTable.clearSelection();
      } else {
        this.$refs.modelTable.toggleAllSelection();
      }
    },
    handleSelectionChange(val) {
      this.selectedModels = val;
      this.isAllSelected = val.length === this.modelList.length;
      if (val.length === 0) {
        this.isAllSelected = false;
      }
    },

    // Add a model configuration
    handleAddConfirm(newModel) {
      const params = {
        modelType: this.activeTab,
        provideCode: newModel.provideCode,
        formData: {
          ...newModel,
          isDefault: newModel.isDefault ? 1 : 0,
          isEnabled: newModel.isEnabled ? 1 : 0,
          configJson: newModel.configJson,
        },
      };

      Api.model.addModel(params, ({ data }) => {
        if (data.code === 0) {
          this.$message.success({
            message: this.$t("modelConfig.addSuccess"),
            showClose: true,
          });
          this.loadData();
        } else {
          this.$message.error({
            message: data.msg || this.$t("modelConfig.addFailed"),
            showClose: true,
          });
        }
      });
    },

    // Fetch the model configuration list
    loadData() {
      this.loading = true; // Start loading
      const params = {
        modelType: this.activeTab,
        modelName: this.search,
        page: this.currentPage,
        limit: this.pageSize,
      };

      Api.model.getModelList(params, ({ data }) => {
        this.loading = false; // Stop loading
        if (data.code === 0) {
          this.modelList = data.data.list;
          this.total = data.data.total;
        } else {
          this.$message.error(data.msg || this.$t("modelConfig.fetchModelsFailed"));
        }
      });
    },
    // Handle enable/disable status changes
    handleStatusChange(model) {
      const newStatus = model.isEnabled ? 1 : 0;
      const originalStatus = model.isEnabled;

      model.isEnabled = !model.isEnabled;

      Api.model.updateModelStatus(model.id, newStatus, ({ data }) => {
        if (data.code === 0) {
          this.$message.success(
            newStatus === 1
              ? this.$t("modelConfig.enableSuccess")
              : this.$t("modelConfig.disableSuccess")
          );
          // Keep the new state
          model.isEnabled = newStatus;
          // Refresh table data
          this.loadData();
        } else {
          // Restore the previous state if the operation fails
          model.isEnabled = originalStatus;
          this.$message.error(data.msg || this.$t("modelConfig.operationFailed"));
        }
      });
    },
    handleDefaultChange(model) {
      Api.model.setDefaultModel(model.id, ({ data }) => {
        if (data.code === 0) {
          this.$message.success(this.$t("modelConfig.setDefaultSuccess"));
          this.loadData();
        }
      });
    },
  },
};
</script>

<style lang="scss" scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
}

.page {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  padding: 32px;
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

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--x-text);
}

.model-layout {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 20px;
}

// Model type tabs
.type-list {
  flex: 1 1 200px;
  max-width: 240px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  box-sizing: border-box;
}

.type-tab {
  min-height: 38px;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-align: left;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;

  &:hover {
    background: var(--x-surface-raised);
    color: var(--x-text);
  }

  &.is-active {
    background: var(--x-selected);
    color: var(--x-text);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

// Table card
.model-card {
  flex: 999 1 560px;
  min-width: 0;
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
  padding: 14px 16px;
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
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
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

.table-scroll {
  overflow-x: auto;
}

.model-table {
  min-width: 720px;

  &::before {
    display: none;
  }
}

::v-deep .model-table {
  th.el-table__cell {
    padding: 8px 0;
    border-bottom: 1px solid var(--x-divider);
  }

  td.el-table__cell {
    padding: 10px 0;
    border-bottom: 1px solid var(--x-divider);
  }

  .cell {
    padding-left: 12px;
    padding-right: 12px;
  }

  .el-table__row:last-child td.el-table__cell {
    border-bottom: 0;
  }
}

::v-deep .model-table .el-checkbox__inner {
  border-color: var(--x-text-disabled);
  border-radius: 4px;
}

.name-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.model-name {
  font-weight: 500;
  color: var(--x-text);
}

.badge-default {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: var(--x-radius-pill);
  background: var(--x-ink);
  color: var(--x-on-ink);
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
}

.model-id {
  font-family: var(--x-font-mono);
  font-size: 12px;
  color: var(--x-text-muted);
  word-break: break-all;
}

.provider {
  color: var(--x-text-secondary);
}

.switch-wrap {
  display: inline-flex;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  white-space: nowrap;
}

.action-btn {
  height: 30px;
  padding: 0 10px;
  margin: 0 !important;
  color: var(--x-text-secondary);
  font-size: 13px;

  &:hover,
  &:focus {
    background: var(--x-canvas);
    color: var(--x-text);
  }
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

// The selection header shows a "Select" label instead of the select-all checkbox
::v-deep .model-table .custom-selection-header .cell {
  position: relative;
  min-height: 23px;

  .el-checkbox {
    display: none;
  }

  &::before {
    content: attr(data-content);
    display: block;
    text-align: center;
    color: var(--x-text-muted);
  }
}

.table-footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--x-border);
}

.batch-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  ::v-deep .el-button + .el-button {
    margin-left: 0;
  }
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 900px) {
  .type-list {
    flex: 1 1 100%;
    max-width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
  }

  .type-tab {
    flex: 0 0 auto;
    min-height: 34px;
    padding: 6px 12px;
  }
}

@media (max-width: 768px) {
  .page {
    padding: 16px;
  }

  .search-group,
  .search-input {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
  }
}
</style>
