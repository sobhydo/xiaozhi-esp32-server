<template>
  <div class="welcome">
    <HeaderBar />

    <main class="admin-page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t("agentTemplateManagement.title") }}</h1>
          <p class="page-description">{{ $t('console.templatesDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="showAddTemplateDialog">
            {{ $t("agentTemplateManagement.createTemplate") }}
          </el-button>
        </div>
      </header>

      <section class="table-card">
        <div class="table-toolbar">
          <div class="search-group">
            <el-input
              :placeholder="$t('agentTemplateManagement.searchPlaceholder')"
              :aria-label="$t('agentTemplateManagement.searchPlaceholder')"
              v-model="search"
              class="search-input"
              prefix-icon="el-icon-search"
              clearable
              @keyup.enter.native="handleSearch"
            />
            <el-button class="btn-search" @click="handleSearch">
              {{ $t("agentTemplateManagement.search") }}
            </el-button>
          </div>
          <div class="toolbar-actions">
            <el-button size="small" @click="handleSelectAll">
              {{ isAllSelected ? $t("agentTemplateManagement.deselectAll") : $t("agentTemplateManagement.selectAll") }}
            </el-button>
            <el-button size="small" class="btn-danger-outline" @click="batchDeleteTemplate">
              {{ $t("agentTemplateManagement.batchDelete") }}
            </el-button>
          </div>
        </div>
        <CustomTable
          ref="templateTable"
          class="flush-table"
          :data="templateList"
          :columns="tableColumns"
          :loading="templateLoading"
          :show-selection="true"
          :show-operations="true"
          :operations-label="$t('agentTemplateManagement.action')"
          :operations-width="250"
          :total="total"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-size-options="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @page-change="handlePageChange"
        >
          <template slot="selection" slot-scope="scope">
            <el-checkbox v-model="scope.row.selected" @change="handleRowSelectionChange(scope.row)"></el-checkbox>
          </template>
          <template slot="agentName" slot-scope="scope">
            <span class="cell-strong">{{ scope.row.agentName }}</span>
          </template>
          <template slot="serialNumber" slot-scope="scope">
            <span class="mono">{{ (currentPage - 1) * pageSize + scope.$index + 1 }}</span>
          </template>
          <template slot="operations" slot-scope="scope">
            <div class="row-actions">
              <el-button size="mini" type="text" class="row-action" @click="editTemplate(scope.row)">
                {{ $t("agentTemplateManagement.editTemplate") }}
              </el-button>
              <el-button size="mini" type="text" class="row-action row-action--danger" @click="deleteTemplate(scope.row)">
                {{ $t("agentTemplateManagement.deleteTemplate") }}
              </el-button>
            </div>
          </template>
        </CustomTable>
      </section>
    </main>

    <!-- Add/edit template dialog -->
    <CustomDialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      :confirm-loading="confirmLoading"
      :footer="true"
      :width="'1080px'"
      size="large"
      @confirm="handleDialogConfirm"
      @cancel="dialogVisible = false"
    >
      <el-form ref="dialogForm" :model="form" :rules="formRules" label-position="top" class="template-form">
        <el-form-item :label="$t('templateQuickConfig.agentSettings.agentName')" prop="agentName">
          <el-input
            v-model="form.agentName"
            :placeholder="$t('templateQuickConfig.agentSettings.agentNamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('templateQuickConfig.agentSettings.systemPrompt')" prop="systemPrompt">
          <el-input
            v-model="form.systemPrompt"
            type="textarea"
            :placeholder="$t('templateQuickConfig.agentSettings.systemPromptPlaceholder')"
            show-word-limit
            maxlength="2000"
            :autosize="{ minRows: 16 }"
          />
        </el-form-item>
      </el-form>
    </CustomDialog>

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import HeaderBar from "@/components/HeaderBar";
import agentApi from "@/apis/module/agent";
import VersionFooter from "@/components/VersionFooter.vue";
import CustomTable from "@/components/CustomTable.vue";
import CustomDialog from "@/components/CustomDialog.vue";

const DEFAULT_MODEL_CONFIG = {
  ttsModelId: "TTS_EdgeTTS",
  vadModelId: "VAD_SileroVAD",
  asrModelId: "ASR_FunASR",
  llmModelId: "LLM_ChatGLMLLM",
  vllmModelId: "VLLM_ChatGLMVLLM",
  memModelId: "Memory_nomem",
  intentModelId: "Intent_function_call"
};

export default {
  name: "AgentTemplateManagement",
  components: {
    HeaderBar,
    VersionFooter,
    CustomTable,
    CustomDialog
  },

  data() {
    return {
      templateList: [],
      templateLoading: false,
      selectedTemplates: [],
      isAllSelected: false,
      search: "",
      pageSizeOptions: [10, 20, 50, 100],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      tableColumns: [],
      dialogVisible: false,
      dialogTitle: "",
      confirmLoading: false,
      form: {
        id: null,
        agentCode: "小智",
        agentName: "",
        systemPrompt: "",
        sort: 0,
        model: { ...DEFAULT_MODEL_CONFIG }
      },
      formRules: {
        agentName: [
          { required: true, message: "Please enter the assistant nickname", trigger: "blur" }
        ],
        systemPrompt: [
          { required: true, message: "Please enter the role introduction", trigger: "blur" }
        ]
      },
      originalForm: null
    };
  },
  created() {
    this.initTableColumns();
    this.loadTemplateList();
  },
  computed: {
    hasSelected() {
      return this.selectedTemplates.length > 0;
    }
  },
  methods: {
    initTableColumns() {
      this.tableColumns = [
        {
          prop: "agentName",
          label: this.$t("agentTemplateManagement.templateName"),
          align: "left",
          minWidth: 200
        },
        {
          prop: "serialNumber",
          label: this.$t("agentTemplateManagement.serialNumber"),
          align: "left",
          minWidth: 100
        }
      ];
    },
    loadTemplateList() {
      this.templateLoading = true;
      const params = {
        page: this.currentPage,
        limit: this.pageSize
      };
      if (this.search) {
        params.agentName = this.search;
      }

      agentApi.getAgentTemplatesPage(
        params,
        (res) => {
          if (res && res.data && res.data.code === 0) {
            const responseData = res.data.data || {};
            this.templateList = Array.isArray(responseData.list)
              ? responseData.list.map((item) => ({ ...item, selected: false }))
              : [];
            this.total = typeof responseData.total === "number" ? responseData.total : 0;
          } else {
            this.templateList = [];
            this.total = 0;
            this.$message.error(res?.data?.msg || this.$t("agentTemplateManagement.fetchTemplateFailed"));
          }
          this.templateLoading = false;
        },
        (error) => {
          this.templateList = [];
          this.total = 0;
          this.templateLoading = false;
          this.$message.error(this.$t("common.networkError"));
        }
      );
    },
    handleSearch() {
      this.currentPage = 1;
      this.loadTemplateList();
    },
    showAddTemplateDialog() {
      this.dialogTitle = this.$t("templateQuickConfig.addTemplate");
      this.form = {
        id: null,
        agentCode: "小智",
        agentName: this.$t("templateQuickConfig.newTemplate"),
        systemPrompt: "",
        sort: 1,
        model: { ...DEFAULT_MODEL_CONFIG }
      };
      this.originalForm = JSON.parse(JSON.stringify(this.form));
      this.dialogVisible = true;
    },
    editTemplate(row) {
      this.dialogTitle = this.$t("templateQuickConfig.editTemplate");
      this.fetchTemplateById(row.id);
    },
    fetchTemplateById(templateId) {
      agentApi.getAgentTemplateById(templateId, (res) => {
        if (res && res.data && res.data.code === 0 && res.data.data) {
          const template = res.data.data;
          this.form = {
            id: template.id,
            agentCode: template.agentCode || "小智",
            agentName: template.agentName || "",
            systemPrompt: template.systemPrompt || "",
            sort: template.sort || 0,
            model: {
              ttsModelId: template.ttsModelId || DEFAULT_MODEL_CONFIG.ttsModelId,
              vadModelId: template.vadModelId || DEFAULT_MODEL_CONFIG.vadModelId,
              asrModelId: template.asrModelId || DEFAULT_MODEL_CONFIG.asrModelId,
              llmModelId: template.llmModelId || DEFAULT_MODEL_CONFIG.llmModelId,
              vllmModelId: template.vllmModelId || DEFAULT_MODEL_CONFIG.vllmModelId,
              memModelId: template.memModelId || DEFAULT_MODEL_CONFIG.memModelId,
              intentModelId: template.intentModelId || DEFAULT_MODEL_CONFIG.intentModelId
            }
          };
          this.originalForm = JSON.parse(JSON.stringify(this.form));
          this.dialogVisible = true;
        } else {
          this.$message.error(res?.data?.msg || this.$t("templateQuickConfig.templateNotFound"));
        }
      });
    },
    handleDialogConfirm() {
      this.$refs.dialogForm.validate((valid) => {
        if (!valid) return;

        const configData = {
          id: this.form.id || "",
          agentCode: this.form.agentCode,
          agentName: this.form.agentName,
          systemPrompt: this.form.systemPrompt,
          sort: this.form.sort,
          functions: [],
          ...this.form.model
        };

        this.confirmLoading = true;
        const apiCall = this.form.id
          ? agentApi.updateAgentTemplate
          : agentApi.addAgentTemplate;

      apiCall(configData, (res) => {
        this.confirmLoading = false;
        if (res && res.data && res.data.code === 0) {
          this.$message.success({
            message: this.$t("templateQuickConfig.saveSuccess"),
            showClose: true
          });
          this.dialogVisible = false;
          this.loadTemplateList();
        } else {
          this.$message.error({
            message: res?.data?.msg || this.$t("templateQuickConfig.saveFailed"),
            showClose: true
          });
        }
      }, (error) => {
        this.confirmLoading = false;
        this.$message.error(this.$t("common.networkError"));
      });
      });
    },
    deleteTemplate(row) {
      this.$confirm(
        this.$t("agentTemplateManagement.confirmSingleDelete"),
        this.$t("common.warning"),
        {
          confirmButtonText: this.$t("common.confirm"),
          cancelButtonText: this.$t("common.cancel"),
          type: "warning"
        }
      )
        .then(() => {
          agentApi.deleteAgentTemplate(row.id, (res) => {
            if (res && res.data && res.data.code === 0) {
              this.$message.success(this.$t("agentTemplateManagement.deleteSuccess"));
              this.selectedTemplates = [];
              this.isAllSelected = false;
              this.loadTemplateList();
            } else {
              this.$message.error(res?.data?.msg || this.$t("agentTemplateManagement.deleteFailed"));
            }
          });
        })
        .catch(() => {
          this.$message.info(this.$t("common.deleteCancelled"));
        });
    },
    batchDeleteTemplate() {
      if (this.selectedTemplates.length === 0) {
        this.$message.warning(this.$t("agentTemplateManagement.selectTemplate"));
        return;
      }
      this.$confirm(
        this.$t("agentTemplateManagement.confirmBatchDelete", { count: this.selectedTemplates.length }),
        this.$t("common.warning"),
        {
          confirmButtonText: this.$t("common.confirm"),
          cancelButtonText: this.$t("common.cancel"),
          type: "warning"
        }
      )
        .then(() => {
          const ids = this.selectedTemplates.map((template) => template.id);
          agentApi.batchDeleteAgentTemplate(ids, (res) => {
            if (res && res.data && res.data.code === 0) {
              this.$message.success(this.$t("agentTemplateManagement.batchDeleteSuccess"));
              this.loadTemplateList();
              this.selectedTemplates = [];
              this.isAllSelected = false;
            } else {
              this.$message.error(res?.data?.msg || this.$t("agentTemplateManagement.batchDeleteFailed"));
            }
          });
        })
        .catch(() => {
          this.$message.info(this.$t("common.deleteCancelled"));
        });
    },
    handlePageChange(page) {
      this.currentPage = page;
      this.loadTemplateList();
    },
    handlePageSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
      this.loadTemplateList();
    },
    handleSelectAll() {
      this.isAllSelected = !this.isAllSelected;
      this.templateList.forEach((row) => {
        row.selected = this.isAllSelected;
      });
      this.selectedTemplates = this.isAllSelected ? [...this.templateList] : [];
    },
    handleRowSelectionChange(row) {
      this.selectedTemplates = this.templateList.filter((template) => template.selected);
      this.isAllSelected =
        this.templateList.length > 0 &&
        this.selectedTemplates.length === this.templateList.length;
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
    margin-left: 0;
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
    margin-left: 0;
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
    padding-left: 12px;
    padding-right: 12px;
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
    margin-left: 0;
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

.template-form {
  ::v-deep .el-form-item {
    margin-bottom: 18px;
  }

  ::v-deep .el-form-item__label {
    padding: 0 0 6px;
    line-height: 20px;
    font-size: 13px;
  }

  ::v-deep .el-textarea__inner {
    line-height: 1.6;
  }
}

::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
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
