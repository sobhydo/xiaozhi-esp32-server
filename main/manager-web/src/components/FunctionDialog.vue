<template>
  <el-drawer :visible.sync="dialogVisible" direction="rtl" size="80%" :wrapperClosable="false" :withHeader="false"
    custom-class="function-drawer">
    <!-- Custom title area -->
    <div class="custom-header">
      <div class="header-left">
        <h2 class="bold-title">{{ $t('functionDialog.title') }}</h2>
      </div>
      <button type="button" class="custom-close-btn" :aria-label="$t('button.close')" @click="closeDialog">
        <i class="el-icon-close" aria-hidden="true"></i>
      </button>
    </div>

    <div class="drawer-body">
      <div class="function-manager">
        <!-- Left: unselected functions -->
        <section class="function-column">
          <div class="column-header">
            <h3 class="column-title">
              {{ $t('functionDialog.unselectedFunctions') }}
              <span class="column-count">{{ unselected.length }}</span>
            </h3>
            <el-button type="text" @click="selectAll" class="select-all-btn">
              {{ $t('functionDialog.selectAll') }}
            </el-button>
          </div>
          <div class="function-list">
            <template v-if="unselected.length">
              <div v-for="func in unselected" :key="func.name" class="function-item">
                <el-checkbox :label="func.name" v-model="selectedNames" :aria-label="func.name"
                  @change="(val) => handleCheckboxChange(func, val)" @click.native.stop></el-checkbox>
                <button type="button" class="func-tag" @click="handleFunctionClick(func)">
                  <span class="func-name">{{ func.name }}</span>
                </button>
              </div>
            </template>
            <div v-else class="list-empty">
              <el-empty :image-size="64" :description="$t('functionDialog.noMorePlugins')" />
            </div>
          </div>
        </section>

        <!-- Middle: selected functions -->
        <section class="function-column">
          <div class="column-header">
            <h3 class="column-title">
              {{ $t('functionDialog.selectedFunctions') }}
              <span class="column-count">{{ selectedList.length }}</span>
            </h3>
            <el-button type="text" @click="deselectAll" class="select-all-btn">
              {{ $t('functionDialog.selectAll') }}
            </el-button>
          </div>
          <div class="function-list">
            <template v-if="selectedList.length > 0">
              <div v-for="func in selectedList" :key="func.name" class="function-item"
                :class="{ 'is-active': currentFunction && currentFunction.name === func.name }">
                <el-checkbox :label="func.name" v-model="selectedNames" :aria-label="func.name"
                  @change="(val) => handleCheckboxChange(func, val)" @click.native.stop></el-checkbox>
                <button type="button" class="func-tag" @click="handleFunctionClick(func)">
                  <span class="func-name">{{ func.name }}</span>
                  <i class="el-icon-arrow-right func-arrow" aria-hidden="true"></i>
                </button>
              </div>
            </template>
            <div v-else class="list-empty">
              <el-empty :image-size="64" :description="$t('functionDialog.pleaseSelectPlugin')" />
            </div>
          </div>
        </section>

        <!-- Right: parameter config -->
        <section class="params-column">
          <div v-if="currentFunction" class="column-header">
            <h3 class="column-title">
              {{ $t('functionDialog.paramConfig') }} · <span class="column-title-strong">{{ currentFunction.name }}</span>
            </h3>
          </div>
          <div v-if="currentFunction" class="params-container">
            <el-form :model="currentFunction" class="param-form">
              <!-- Iterate over fieldsMeta, not the keys of params -->
              <div v-if="currentFunction.fieldsMeta.length == 0">
                <el-empty :image-size="64" :description="currentFunction.name + $t('functionDialog.noNeedToConfig')" />
              </div>
              <el-form-item v-for="field in currentFunction.fieldsMeta" :key="field.key" :label="field.label"
                class="param-item" :class="{ 'textarea-field': field.type === 'array' || field.type === 'json' }">
                <template #label>
                  <span class="param-label">{{ field.label }}</span>
                  <el-tooltip effect="dark" :content="fieldRemark(field)" placement="top">
                    <i class="el-icon-info info-icon" tabindex="0" :aria-label="fieldRemark(field)"></i>
                  </el-tooltip>
                </template>
                <!-- ARRAY -->
                <el-input v-if="field.type === 'array'" type="textarea" v-model="currentFunction.params[field.key]"
                  @change="val => handleParamChange(currentFunction, field.key, val)" />

                <!-- JSON -->
                <el-input v-else-if="field.type === 'json'" type="textarea" :rows="6" placeholder="Please enter valid JSON"
                  class="mono-input" v-model="textCache[field.key]" @blur="flushJson(field)" />

                <!-- number -->
                <el-input-number v-else-if="field.type === 'number'" :value="currentFunction.params[field.key]"
                  @change="val => handleParamChange(currentFunction, field.key, val)" />

                <!-- boolean -->
                <el-switch v-else-if="field.type === 'boolean' || field.type === 'bool'"
                  :value="currentFunction.params[field.key]"
                  @change="val => handleParamChange(currentFunction, field.key, val)" />

                <!-- string or fallback -->
                <el-input v-else v-model="currentFunction.params[field.key]"
                  @change="val => handleParamChange(currentFunction, field.key, val)" />
              </el-form-item>
            </el-form>
          </div>
          <div v-else class="empty-tip">{{ $t('functionDialog.pleaseSelectFunctionForParam') }}</div>
        </section>
      </div>

      <!-- MCP area -->
      <div class="mcp-access-point" v-if="featureStatus.mcpAccessPoint">
        <div class="mcp-container">
          <!-- Left area -->
          <section class="mcp-left">
            <div class="mcp-header">
              <h3 class="bold-title">{{ $t('functionDialog.mcpAccessPoint') }}</h3>
            </div>
            <div class="url-header">
              <div class="address-desc">
                <span>{{ $t('functionDialog.mcpAddressDesc') }}</span>
                <span class="doc-links">
                  <a href="https://github.com/xinnan-tech/xiaozhi-esp32-server/blob/main/docs/mcp-endpoint-enable.md"
                    target="_blank" rel="noopener" class="doc-link">{{ $t('functionDialog.howToDeployMcp') }}</a>
                  <span class="doc-sep" aria-hidden="true">|</span>
                  <a href="https://github.com/xinnan-tech/xiaozhi-esp32-server/blob/main/docs/mcp-endpoint-integration.md"
                    target="_blank" rel="noopener" class="doc-link">{{ $t('functionDialog.howToIntegrateMcp') }}</a>
                </span>
              </div>
            </div>
            <div class="url-row">
              <el-input v-model="mcpUrl" readonly class="url-input" :aria-label="$t('functionDialog.mcpAccessPoint')" />
              <el-button @click="copyUrl" class="inner-copy-btn" icon="el-icon-document-copy">
                {{ $t('functionDialog.copy') }}
              </el-button>
            </div>
          </section>

          <!-- Right area -->
          <section class="mcp-right">
            <div class="mcp-header">
              <h3 class="bold-title">{{ $t('functionDialog.accessPointStatus') }}</h3>
            </div>
            <div class="status-container">
              <span class="status-pill" :class="mcpStatus">
                <span class="status-indicator" aria-hidden="true"></span>
                <span class="status-text">{{
                  mcpStatus === 'connected' ? $t('functionDialog.connected') :
                    mcpStatus === 'loading' ? $t('functionDialog.loading') : $t('functionDialog.disconnected')
                }}</span>
              </span>
              <el-button size="small" icon="el-icon-refresh" class="refresh-btn" @click="refreshStatus">
                {{ $t('functionDialog.refresh') }}
              </el-button>
            </div>
            <div class="mcp-tools-list">
              <ul v-if="mcpTools.length > 0" class="tools-grid">
                <li v-for="tool in mcpTools" :key="tool" class="tool-btn">{{ tool }}</li>
              </ul>
              <div v-else class="no-tools">
                <span>{{ $t('functionDialog.noAvailableTools') }}</span>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>

    <div class="drawer-footer">
      <el-button @click="closeDialog">{{ $t('functionDialog.cancel') }}</el-button>
      <el-button type="primary" @click="saveSelection">{{ $t('functionDialog.saveConfig') }}</el-button>
    </div>
  </el-drawer>
</template>

<script>
import Api from '@/apis/api';
import i18n from '@/i18n';
import featureManager from '@/utils/featureManager';

export default {
  i18n,

  props: {
    value: Boolean,
    functions: {
      type: Array,
      default: () => []
    },
    allFunctions: {
      type: Array,
      default: () => []
    },
    agentId: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      textCache: {},
      dialogVisible: this.value,
      selectedNames: [],
      currentFunction: null,
      modifiedFunctions: {},
      tempFunctions: {},
      // Flag to track whether it has been saved
      hasSaved: false,
      loading: false,

      mcpUrl: "",
      mcpStatus: "disconnected",
      mcpTools: [],
      
      // Feature status
      featureStatus: {
        mcpAccessPoint: false,
        addressBook: false
      }
    }
  },
  computed: {
    selectedList() {
      const list = this.allFunctions.filter(f => this.selectedNames.includes(f.name));
      // If the contacts feature is disabled, filter out the device-to-device call plugin
      if (!this.featureStatus.addressBook) {
        return list.filter(f => f.providerCode !== 'call_device');
      }
      return list;
    },
    unselected() {
      const list = this.allFunctions.filter(f => !this.selectedNames.includes(f.name));
      // If the contacts feature is disabled, filter out the device-to-device call plugin
      if (!this.featureStatus.addressBook) {
        return list.filter(f => f.providerCode !== 'call_device');
      }
      return list;
    }
  },
  watch: {
    currentFunction(newFn) {
      if (!newFn) return;
      // For each array or json field, generate the initial string in textCache
      newFn.fieldsMeta.forEach(f => {
        const v = newFn.params[f.key];
        if (f.type === 'array') {
          this.$set(this.textCache, f.key, Array.isArray(v) ? v.join('\n') : '');
        }
        else if (f.type === 'json') {
          try {
            this.$set(this.textCache, f.key, JSON.stringify(v ?? {}, null, 2));
          } catch {
            this.$set(this.textCache, f.key, '');
          }
        }
      });
    },
    async value(v) {
      this.dialogVisible = v;
      if (v) {
        // Load feature status (must happen before initializing the selection)
        await this.loadFeatureStatus();

        // Initialize the selection when the dialog opens
        this.selectedNames = this.functions.map(f => f.name);

        // If the contacts feature is disabled, remove the device-to-device call plugin from the selection
        if (!this.featureStatus.addressBook) {
          this.selectedNames = this.selectedNames.filter(name => {
            const func = this.allFunctions.find(f => f.name === name);
            return func && func.providerCode !== 'call_device';
          });
        }

        // Merge this.functions from the backend (with params) into allFunctions
        this.functions.forEach(saved => {
          const idx = this.allFunctions.findIndex(f => f.name === saved.name);
          if (idx >= 0) {
            // Keep the user's earlier changes to saved.params
            this.allFunctions[idx].params = { ...saved.params };
          }
        });
        // Right panel defaults to the first item
        this.currentFunction = this.selectedList[0] || null;

        // Load MCP data
        this.loadMcpAddress();
        this.loadMcpTools();
      }
    },
    dialogVisible(newVal) {
      this.$emit('input', newVal);
    }
  },
  methods: {
    /**
     * Load feature status
     */
    async loadFeatureStatus() {
      // Make sure featureManager has finished initializing
      await featureManager.waitForInitialization();

      const config = featureManager.getConfig();
      this.featureStatus = {
        mcpAccessPoint: config.mcpAccessPoint || false,
        addressBook: config.addressBook || false
      };
    },
    
    copyUrl() {
      const textarea = document.createElement('textarea');
      textarea.value = this.mcpUrl;
      textarea.style.position = 'fixed';  // Prevent page scrolling
      document.body.appendChild(textarea);
      textarea.select();

      try {
        const successful = document.execCommand('copy');
        if (successful) {
          this.$message.success(this.$t('functionDialog.copiedToClipboard'));
        } else {
          this.$message.error(this.$t('functionDialog.copyFailed'));
        }
      } catch (err) {
        this.$message.error('Copy failed, please copy manually');
        console.error('Copy failed:', err);
      } finally {
        document.body.removeChild(textarea);
      }
    },

    refreshStatus() {
      this.mcpStatus = "loading";
      this.loadMcpTools();
    },

    // Load MCP endpoint address
    loadMcpAddress() {
      Api.agent.getAgentMcpAccessAddress(this.agentId, (res) => {
        if (res.data.code === 0) {
          this.mcpUrl = res.data.data || "";
        } else {
          this.mcpUrl = res.data.msg;
          console.error('Failed to get MCP address:', res.data.msg);
        }
      });
    },

    // Load MCP tool list
    loadMcpTools() {
      Api.agent.getAgentMcpToolsList(this.agentId, (res) => {
        if (res.data.code === 0) {
          this.mcpTools = res.data.data || [];
          // Update status based on the tool list
          this.mcpStatus = this.mcpTools.length > 0 ? "connected" : "disconnected";
        } else {
          this.mcpTools = [];
          this.mcpStatus = "disconnected";
          console.error('Failed to get MCP tool list:', res.data.msg);
        }
      });
    },

    flushArray(key) {
      const text = this.textCache[key] || '';
      const arr = text
        .split('\n')
        .map(s => s.trim())
        .filter(Boolean);
      this.handleParamChange(this.currentFunction, key, arr);
    },

    flushJson(field) {
      const key = field.key;
      if (!key) {
        return;
      }
      const text = this.textCache[key] || '';
      try {
        const obj = JSON.parse(text);
        this.handleParamChange(this.currentFunction, key, obj);
      } catch {
        this.$message.error(`${this.currentFunction.name}${this.$t('functionDialog.jsonFormatError')}`);
      }
    },
    handleFunctionClick(func) {
      if (this.selectedNames.includes(func.name)) {
        const tempFunc = this.tempFunctions[func.name];
        this.currentFunction = tempFunc ? tempFunc : func;
      }
    },
    handleParamChange(func, key, value) {
      if (!this.tempFunctions[func.name]) {
        this.tempFunctions[func.name] = JSON.parse(JSON.stringify(func));
      }
      this.tempFunctions[func.name].params[key] = value;
    },
    handleCheckboxChange(func, checked) {
      if (checked) {
        if (!this.selectedNames.includes(func.name)) {
          this.selectedNames = [...this.selectedNames, func.name];
        }
      } else {
        this.selectedNames = this.selectedNames.filter(name => name !== func.name);
      }

      if (this.selectedList.length > 0) {
        this.currentFunction = this.selectedList[0];
      } else {
        this.currentFunction = null;
      }
    },

    selectAll() {
      this.selectedNames = [...this.allFunctions.map(f => f.name)];
      if (this.selectedList.length > 0) {
        this.currentFunction = JSON.parse(JSON.stringify(this.selectedList[0]));
      }
    },

    deselectAll() {
      this.selectedNames = [];
      this.currentFunction = null;
    },

    closeDialog() {
      this.tempFunctions = {};
      this.selectedNames = this.functions.map(f => f.name);
      this.currentFunction = null;
      this.dialogVisible = false;
      this.$emit('input', false);
      this.$emit('dialog-closed', false);
    },

    saveSelection() {
      Object.keys(this.tempFunctions).forEach(name => {
        this.modifiedFunctions[name] = JSON.parse(JSON.stringify(this.tempFunctions[name]));
      });
      this.tempFunctions = {};
      this.hasSaved = true;

      let selected = this.selectedList.map(f => {
        const modified = this.modifiedFunctions[f.name];
        return {
          id: f.id,
          name: f.name,
          params: modified
            ? { ...modified.params }
            : { ...f.params }
        }
      });

      // If the contacts feature is disabled, automatically deselect the device-to-device call plugin
      if (!this.featureStatus.addressBook) {
        selected = selected.filter(f => f.providerCode !== 'call_device');
      }

      this.$emit('update-functions', selected);
      this.dialogVisible = false;
      // Notify the parent that the dialog was closed and saved
      this.$emit('dialog-closed', true);
    },
    fieldRemark(field) {
      let description = (field && field.label) ? field.label : '';
      if (field.default) {
        description += `（${this.$t('functionDialog.defaultValue')}：${field.default}）`;
      }
      return description;
    },
  }
}
</script>

<style lang="scss" scoped>
.custom-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 20px 16px 24px;
  border-bottom: 1px solid var(--x-divider);

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    min-width: 0;
  }

  .bold-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--x-text);
  }
}

.custom-close-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: var(--x-radius-control);
  border: 0;
  background: transparent;
  font-size: 16px;
  color: var(--x-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: background 0.15s ease, color 0.15s ease;

  &:hover,
  &:focus-visible {
    color: var(--x-text);
    background: var(--x-canvas);
    outline: none;
  }
}

.drawer-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  background: var(--x-canvas);
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-align: start;
}

.function-manager {
  display: grid;
  grid-template-columns: minmax(200px, 1fr) minmax(200px, 1fr) minmax(280px, 1.5fr);
  gap: 16px;
  height: 56vh;
  min-height: 360px;
}

.function-column,
.params-column {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.column-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  min-height: 48px;
  padding: 0 12px 0 16px;
  border-bottom: 1px solid var(--x-divider);
  box-sizing: border-box;
}

.column-title {
  margin: 0;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.column-title-strong {
  overflow: hidden;
  text-overflow: ellipsis;
}

.column-count {
  padding: 0 7px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
}

.select-all-btn {
  flex-shrink: 0;
  padding: 6px 8px;
  font-size: 13px;
  color: var(--x-text-secondary);

  &:hover,
  &:focus {
    color: var(--x-text);
    background: var(--x-canvas);
  }
}

.function-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.function-item {
  padding: 0 4px 0 10px;
  border-radius: var(--x-radius-control);
  transition: background-color 0.15s ease;
  display: flex;
  align-items: center;

  &:hover {
    background-color: var(--x-surface-raised);
  }

  &.is-active {
    background-color: var(--x-selected);
  }
}

.func-tag {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  margin-inline-start: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-size: 14px;
  color: var(--x-text);
  text-align: start;
  cursor: pointer;

  &:focus-visible {
    outline: none;
    text-decoration: underline;
  }
}

.func-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.func-arrow {
  font-size: 12px;
  color: var(--x-text-muted);
}

.list-empty {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;

  ::v-deep .el-empty__description p {
    color: var(--x-text-muted);
  }
}

.params-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 16px;
}

.param-label {
  font-size: 13px;
  margin-inline-end: 4px;
}

.info-icon {
  font-size: 14px;
  color: var(--x-text-muted);
  cursor: help;

  &:hover,
  &:focus-visible {
    color: var(--x-text);
    outline: none;
  }
}

.param-form {
  ::v-deep .el-form-item {
    display: flex;
    flex-direction: column;
    margin-bottom: 16px;

    .el-form-item__label {
      font-size: 13px !important;
      line-height: 20px;
      color: var(--x-text);
      text-align: start;
      padding: 0 0 6px;
      flex-shrink: 0;
      width: auto !important;
    }

    .el-form-item__content {
      margin-inline-start: 0 !important;
      flex-grow: 1;
      line-height: 36px;

      .el-input__inner {
        text-align: start;
        width: 100%;
      }
    }
  }

  .mono-input ::v-deep .el-textarea__inner {
    font-family: var(--x-font-mono);
    font-size: 12px;
  }
}

.empty-tip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: var(--x-text-muted);
  font-size: 13px;
  text-align: center;
}

.drawer-footer {
  position: relative;
  z-index: 2;
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 24px 18px;
  border-top: 1px solid var(--x-divider);
  background: var(--x-surface);

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

::v-deep .el-checkbox__label {
  display: none;
}

.mcp-access-point {
  position: relative;
  z-index: 1;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  padding: 20px;
}

.mcp-header .bold-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--x-text);
}

.mcp-container {
  display: flex;
  flex-wrap: wrap;
  gap: 24px 32px;
}

.mcp-left,
.mcp-right {
  flex: 1 1 320px;
  min-width: 0;
}

.address-desc {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-secondary);
}

.doc-links {
  display: inline-flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.doc-link {
  color: var(--x-ink);
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 2px;
  text-decoration-color: var(--x-border-control);

  &:hover {
    text-decoration-color: var(--x-ink);
  }
}

.doc-sep {
  color: var(--x-border-control);
}

.url-row {
  display: flex;
  gap: 8px;
}

.url-input {
  flex: 1;
  min-width: 0;

  ::v-deep .el-input__inner {
    background-color: var(--x-surface-raised);
    font-family: var(--x-font-mono);
    font-size: 12px;
    color: var(--x-text-secondary);
  }
}

.inner-copy-btn {
  flex-shrink: 0;
}

.status-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  background: var(--x-divider);
  color: var(--x-text-secondary);

  .status-indicator {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--x-text-muted);
  }

  &.connected {
    background: var(--x-success-bg);
    color: var(--x-success-text);

    .status-indicator {
      background: var(--x-success);
    }
  }

  &.loading {
    background: var(--x-warning-bg);
    color: var(--x-warning-text);

    .status-indicator {
      background: var(--x-warning);
      animation: pulse 1.5s infinite;
    }
  }
}

@keyframes pulse {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.4;
  }

  100% {
    opacity: 1;
  }
}

.mcp-tools-list {
  margin-top: 12px;

  .tools-grid {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .tool-btn {
    padding: 3px 10px;
    border: 1px solid var(--x-border);
    border-radius: var(--x-radius-pill);
    background: var(--x-surface-raised);
    color: var(--x-text-secondary);
    font-family: var(--x-font-mono);
    font-size: 12px;
    line-height: 18px;
  }

  .no-tools {
    color: var(--x-text-muted);
    font-size: 13px;
    padding: 8px 0;
  }
}

@media (max-width: 900px) {
  .function-manager {
    grid-template-columns: minmax(0, 1fr);
    height: auto;
    min-height: 0;
  }

  .function-list {
    max-height: 260px;
  }

  .params-container {
    max-height: none;
  }
}

@media (max-width: 600px) {
  .custom-header {
    padding: 14px 12px 12px 16px;
  }

  .drawer-body {
    padding: 16px;
  }

  .drawer-footer {
    padding: 12px 16px;
  }

  .url-row {
    flex-wrap: wrap;
  }
}
</style>

<style lang="scss">
.function-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

@media (max-width: 900px) {
  .el-drawer.function-drawer {
    width: 100% !important;
  }
}
</style>
