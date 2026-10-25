<template>
  <div class="welcome">
    <HeaderBar />

    <div class="config-page">
      <nav class="breadcrumb" :aria-label="$t('templateQuickConfig.title')">
        <button type="button" class="breadcrumb-link" @click="goToHome">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m15 18-6-6 6-6" />
          </svg>
          {{ $t('agentTemplateManagement.title') }}
        </button>
        <span class="breadcrumb-sep" aria-hidden="true">/</span>
        <span class="breadcrumb-current">{{ $t('templateQuickConfig.title') }}</span>
      </nav>

      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ form.agentName || $t('templateQuickConfig.title') }}</h1>
        </div>
        <div class="header-actions">
          <el-button size="small" class="reset-btn" @click="resetConfig">{{ $t('templateQuickConfig.resetConfig') }}</el-button>
          <el-button type="primary" size="small" class="save-btn" @click="saveConfig">{{ $t('templateQuickConfig.saveConfig') }}</el-button>
        </div>
      </header>

      <el-form ref="form" :model="form" label-position="top" class="config-form">
        <section class="config-card">
          <div class="card-head">
            <h2 class="card-title">{{ $t('templateQuickConfig.title') }}</h2>
          </div>
          <div class="card-body grid-2">
            <!-- Assistant nickname -->
            <el-form-item :label="$t('templateQuickConfig.agentSettings.agentName')" prop="agentName">
              <el-input
                v-model="form.agentName"
                :placeholder="$t('templateQuickConfig.agentSettings.agentNamePlaceholder')"
                :validate-event="false"
                class="form-input"
              />
            </el-form-item>

            <!-- Role introduction -->
            <el-form-item :label="$t('templateQuickConfig.agentSettings.systemPrompt')" prop="systemPrompt"
              class="span-all description-item">
              <el-input
                v-model="form.systemPrompt"
                type="textarea"
                :placeholder="$t('templateQuickConfig.agentSettings.systemPromptPlaceholder')"
                :validate-event="false"
                :autosize="{ minRows: 12, maxRows: 24 }"
                show-word-limit
                maxlength="2000"
              />
            </el-form-item>
          </div>
        </section>
      </el-form>
    </div>
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import HeaderBar from "@/components/HeaderBar.vue";
import agentApi from '@/apis/module/agent';
import VersionFooter from "@/components/VersionFooter.vue";

// Default model configuration constants
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
  name: 'TemplateQuickConfig',
  components: { HeaderBar, VersionFooter },
  data() {
    return {
      form: {
        agentCode: "小智",
        agentName: "",
        systemPrompt: "",
        sort: 0,
        model: { ...DEFAULT_MODEL_CONFIG }
      },
      templateId: '',
      originalForm: null
    };
  },
  methods: {
    // Return to the template management page
    goToHome() {
      this.$router.push('/agent-template-management');
    },
    
    // Save configuration
    saveConfig() {
      const configData = this.prepareConfigData();
      
      if (this.templateId) {
        this.updateExistingTemplate(configData);
      } else {
        this.createNewTemplate(configData);
      }
    },
    
    // Prepare configuration data
    prepareConfigData() {
      return {
        id: this.templateId || '',
        agentCode: this.form.agentCode,
        agentName: this.form.agentName,
        systemPrompt: this.form.systemPrompt,
        sort: this.form.sort,
        functions: [],
        // Include the required model fields so the API call succeeds
        ...this.form.model
      };
    },
    
    // Update an existing template
    updateExistingTemplate(configData) {
      agentApi.updateAgentTemplate(configData, (res) => {
        if (res && res.data && res.data.code === 0) {
          this.$message.success({ 
            message: this.$t('templateQuickConfig.saveSuccess'), 
            showClose: true 
          });
          this.originalForm = JSON.parse(JSON.stringify(this.form));
        } else {
          this.$message.error({ 
            message: res?.data?.msg || this.$t('templateQuickConfig.saveFailed'), 
            showClose: true 
          });
        }
      });
    },
    
    // Create a new template
    createNewTemplate(configData) {
      agentApi.addAgentTemplate(configData, (res) => {
        if (res && res.data && res.data.code === 0) {
          this.$message.success({ 
            message: this.$t('templateQuickConfig.saveSuccess'), 
            showClose: true 
          });
          this.goToHome();
        } else {
          this.$message.error({ 
            message: res?.data?.msg || this.$t('templateQuickConfig.saveFailed'), 
            showClose: true 
          });
        }
      });
    },
    
    // Reset configuration
    resetConfig() {
      this.$confirm(
        this.$t('templateQuickConfig.confirmReset'), 
        this.$t('common.tip'), 
        {
          confirmButtonText: this.$t('common.confirm'),
          cancelButtonText: this.$t('common.cancel'),
          type: 'warning'
        }
      ).then(() => {
        if (this.originalForm) {
          this.form = JSON.parse(JSON.stringify(this.originalForm));
        }
        this.$message.success({ 
          message: this.$t('templateQuickConfig.resetSuccess'), 
          showClose: true 
        });
      }).catch(() => {});
    },
    
    // Fetch a template by ID
    fetchTemplateById(templateId) {
      agentApi.getAgentTemplateById(templateId, (res) => {
        if (res && res.data && res.data.code === 0 && res.data.data) {
          const template = res.data.data;
          this.applyTemplateData(template);
          this.templateId = templateId;
          this.originalForm = JSON.parse(JSON.stringify(this.form));
        } else {
          this.$message.error(res?.data?.msg || this.$t('templateQuickConfig.templateNotFound'));
        }
      });
    },
    
    // Apply template data
    applyTemplateData(templateData) {
      this.form = {
        ...this.form,
        agentName: templateData.agentName || this.form.agentName,
        agentCode: templateData.agentCode || this.form.agentCode,
        systemPrompt: templateData.systemPrompt || this.form.systemPrompt,
        sort: templateData.sort || this.form.sort,
        model: {
          ttsModelId: templateData.ttsModelId || this.form.model.ttsModelId,
          vadModelId: templateData.vadModelId || this.form.model.vadModelId,
          asrModelId: templateData.asrModelId || this.form.model.asrModelId,
          llmModelId: templateData.llmModelId || this.form.model.llmModelId,
          vllmModelId: templateData.vllmModelId || this.form.model.vllmModelId,
          memModelId: templateData.memModelId || this.form.model.memModelId,
          intentModelId: templateData.intentModelId || this.form.model.intentModelId
        }
      };
    },
    
    // Set default template values
    setDefaultTemplateValues() {
      this.form = {
        ...this.form,
        agentName: this.$t('templateQuickConfig.newTemplate'),
        agentCode: '小智',
        systemPrompt: '',
        sort: 1
      };
      
      this.originalForm = JSON.parse(JSON.stringify(this.form));
    },
    
    // Fetch the template list and set the sort number
    fetchTemplateListForSort() {
      agentApi.getAgentTemplate((res) => {
        if (res && res.data && res.data.code === 0) {
          const templateList = res.data.data || [];
          if (templateList.length > 0) {
            const maxSort = Math.max(...templateList.map(t => t.sort || 0));
            this.form.sort = maxSort + 1;
          } else {
            this.form.sort = 1;
          }
        } else {
          this.form.sort = 1;
        }
        
        this.originalForm = JSON.parse(JSON.stringify(this.form));
      });
    }
  },
  
  // Initialize when the component is mounted
  mounted() {
    const templateId = this.$route.query.templateId;
    
    if (templateId) {
      // Edit mode: load the existing template
      this.fetchTemplateById(templateId);
    } else {
      // Create mode: set defaults and fetch the sort number
      this.form.agentName = this.$t('templateQuickConfig.newTemplate');
      this.fetchTemplateListForSort();
    }
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
}

.config-page {
  flex: 1;
  width: 100%;
  max-width: 1120px;
  box-sizing: border-box;
  padding: 24px 32px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  text-align: start;
}

// Breadcrumb
.breadcrumb {
  padding: 0; // App.vue has a global `nav { padding: 30px }`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--x-text-muted);
  min-width: 0;
}

.breadcrumb-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 2px 4px 2px 0;
  border: 0;
  background: none;
  font: inherit;
  color: var(--x-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 6px;

  &:hover {
    color: var(--x-ink);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.breadcrumb-sep {
  color: var(--x-border-control);
}

.breadcrumb-current {
  color: var(--x-text);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// Page header
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.page-heading {
  min-width: 0;
  flex: 1 1 320px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  line-height: 1.3;
  color: var(--x-text);
  overflow-wrap: anywhere;
}

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

// Card
.config-card {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  min-width: 0;
}

.card-head {
  padding: 16px 20px;
  border-bottom: 1px solid var(--x-divider);
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
}

.card-body {
  padding: 20px;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
}

.span-all {
  grid-column: 1 / -1;
}

.config-form ::v-deep .el-form-item {
  margin-bottom: 0;
  min-width: 0;
}

.config-form ::v-deep .el-form-item__label {
  float: none;
  display: block;
  padding: 0 0 6px;
  line-height: 20px;
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text);
  text-align: start;
}

.config-form ::v-deep .el-form-item__content {
  line-height: normal;
}

.form-input {
  width: 100%;
}

::v-deep .description-item .el-textarea__inner {
  min-height: 240px !important;
  resize: vertical;
  line-height: 1.6;
  font-size: 14px;
  padding: 10px 12px;
}

::v-deep .description-item .el-input__count {
  color: var(--x-text-muted);
  background: transparent;
  font-size: 12px;
  right: 12px;
  bottom: 8px;
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 768px) {
  .config-page {
    padding: 16px 16px 24px;
    gap: 16px;
  }

  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
  }

  .card-body {
    padding: 16px;
  }
}
</style>
