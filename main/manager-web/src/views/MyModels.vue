<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t("header.myModels") }}</h1>
          <p class="page-description">{{ $t("myModels.description") }}</p>
        </div>
        <div v-if="models.length" class="page-actions">
          <el-button type="primary" icon="el-icon-plus" @click="openAdd">
            {{ $t("myModels.addModel") }}
          </el-button>
        </div>
      </header>

      <!-- Model type switch; only language models are available in phase 1 -->
      <div class="type-switch" role="tablist" :aria-label="$t('header.myModels')">
        <button
          v-for="type in modelTypes"
          :key="type.value"
          type="button"
          role="tab"
          class="type-tab"
          :class="{ 'is-active': activeType === type.value }"
          :aria-selected="activeType === type.value ? 'true' : 'false'"
          :disabled="!type.enabled"
          :title="type.enabled ? null : $t('myModels.comingSoon')"
          @click="selectType(type)"
        >
          <span>{{ $t(type.label) }}</span>
          <span v-if="!type.enabled" class="soon">{{ $t("myModels.comingSoon") }}</span>
        </button>
      </div>

      <section class="models-card" v-loading="loading" element-loading-background="rgba(255, 255, 255, 0.7)">
        <!-- Empty state -->
        <div v-if="!loading && !models.length" class="empty">
          <span class="empty__icon" aria-hidden="true">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round">
              <circle cx="7.5" cy="15.5" r="4.5" /><path d="m10.7 12.3 9.3-9.3M16 7l3 3M14 9l2 2" />
            </svg>
          </span>
          <h2 class="empty__title">{{ $t("myModels.emptyTitle") }}</h2>
          <p class="empty__text">{{ $t("myModels.emptyDescription") }}</p>
          <el-button type="primary" icon="el-icon-plus" @click="openAdd">
            {{ $t("myModels.addModel") }}
          </el-button>
        </div>

        <ul v-else class="model-list">
          <li v-for="row in models" :key="row.id" class="model-row">
            <span class="model-row__avatar" aria-hidden="true">{{ providerInitial(row) }}</span>
            <div class="model-row__main">
              <div class="model-row__name">{{ row.modelName }}</div>
              <div class="model-row__meta">
                <span>{{ providerLabel(row) }}</span>
                <span class="dot" aria-hidden="true">·</span>
                <span class="mono" dir="ltr">{{ (row.configJson && row.configJson.model_name) || "—" }}</span>
              </div>
            </div>
            <div class="model-row__side">
              <label class="model-row__switch">
                <span class="switch-label">{{ $t("myModels.enabled") }}</span>
                <el-switch
                  :value="row.isEnabled === 1"
                  :disabled="!!busy[row.id]"
                  :aria-label="$t('myModels.enabled')"
                  @change="(val) => toggleEnabled(row, val)"
                />
              </label>
              <div class="model-row__actions">
                <el-button size="small" :loading="testingId === row.id" @click="testRow(row)">
                  {{ $t("myModels.test") }}
                </el-button>
                <el-button size="small" @click="openEdit(row)">{{ $t("common.edit") }}</el-button>
                <el-tooltip :content="$t('common.delete')" placement="top">
                  <el-button
                    size="small"
                    class="icon-btn icon-btn--danger"
                    icon="el-icon-delete"
                    :aria-label="$t('common.delete')"
                    @click="removeRow(row)"
                  />
                </el-tooltip>
              </div>
            </div>
          </li>
        </ul>
      </section>

      <!-- Add / edit dialog -->
      <CustomDialog
        :visible.sync="dialogVisible"
        :title="form.id ? $t('myModels.editModel') : $t('myModels.addModel')"
        width="560px"
        class="my-model-dialog"
        :confirm-loading="saving"
        :cancel-text="$t('common.cancel')"
        :confirm-text="$t('myModels.save')"
        @confirm="save"
      >
        <el-form ref="form" :model="form" :rules="rules" label-position="top" class="model-form" @submit.native.prevent>
          <el-form-item :label="$t('myModels.provider')" prop="preset">
            <div class="preset-grid" role="radiogroup" :aria-label="$t('myModels.provider')">
              <button
                v-for="preset in availablePresets"
                :key="preset.key"
                type="button"
                role="radio"
                class="preset"
                :class="{ 'is-active': form.preset === preset.key }"
                :aria-checked="form.preset === preset.key ? 'true' : 'false'"
                :disabled="!!form.id && form.provideCode !== preset.provideCode"
                @click="choosePreset(preset.key)"
              >
                <span class="preset__name">{{ $t(preset.label) }}</span>
              </button>
            </div>
          </el-form-item>

          <el-form-item v-if="form.preset === 'compatible'" :label="$t('myModels.service')">
            <el-select v-model="form.service" class="full" @change="chooseService">
              <el-option
                v-for="service in compatibleServices"
                :key="service.key"
                :label="service.label ? $t(service.label) : service.name"
                :value="service.key"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('myModels.apiKey')" prop="apiKey">
            <el-input
              v-model="form.apiKey"
              type="password"
              show-password
              autocomplete="new-password"
              :placeholder="$t('myModels.apiKeyPlaceholder')"
              @input="clearTest"
            />
            <p v-if="form.id && form.savedKey" class="field-hint">
              {{ $t("myModels.apiKeyKeepHint") }}
              <span class="mono" dir="ltr">{{ form.savedKey }}</span>
            </p>
          </el-form-item>

          <el-form-item :label="$t('myModels.model')" prop="modelName">
            <el-select
              v-model="form.modelName"
              class="full"
              filterable
              allow-create
              default-first-option
              :placeholder="$t('myModels.modelPlaceholder')"
              @change="chooseModel"
            >
              <el-option v-for="name in modelSuggestions" :key="name" :label="name" :value="name" />
            </el-select>
          </el-form-item>

          <el-form-item v-if="form.preset === 'compatible'" :label="$t('myModels.baseUrl')" prop="baseUrl">
            <el-input v-model="form.baseUrl" dir="ltr" placeholder="https://api.example.com/v1" @input="clearTest" />
          </el-form-item>

          <el-form-item v-if="form.preset === 'claude'" :label="$t('myModels.effort')">
            <el-select v-model="form.effort" class="full">
              <el-option v-for="level in effortLevels" :key="level" :label="$t('myModels.effort_' + level)" :value="level" />
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('myModels.displayName')" prop="displayName">
            <el-input v-model="form.displayName" maxlength="64" :placeholder="$t('myModels.displayNamePlaceholder')" />
          </el-form-item>

          <div class="test-row">
            <el-button size="small" icon="el-icon-connection" :loading="testingDialog" @click="testDialog">
              {{ $t("myModels.testConnection") }}
            </el-button>
            <span
              v-if="testResult"
              class="test-result"
              :class="testResult.ok ? 'is-ok' : 'is-error'"
              role="status"
            >
              <i :class="testResult.ok ? 'el-icon-success' : 'el-icon-error'" aria-hidden="true"></i>
              <span>{{ testResultText(testResult) }}</span>
            </span>
          </div>
        </el-form>
      </CustomDialog>
    </main>
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import CustomDialog from "@/components/CustomDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";

const OPENAI_BASE_URL = "https://api.openai.com/v1";

// Frontend-only presets; each maps to a backend provider code
const LLM_PRESETS = [
  {
    key: "openai",
    provideCode: "openai",
    label: "myModels.providerOpenai",
    baseUrl: OPENAI_BASE_URL,
    models: ["gpt-4o-mini", "gpt-4.1-mini", "gpt-5.4-nano"],
  },
  {
    key: "claude",
    provideCode: "claude",
    label: "myModels.providerClaude",
    models: ["claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5"],
    defaults: { effort: "low", max_tokens: 16000, fallbacks: "default" },
  },
  {
    key: "gemini",
    provideCode: "gemini",
    label: "myModels.providerGemini",
    models: ["gemini-3.5-flash"],
  },
  {
    key: "compatible",
    provideCode: "openai",
    label: "myModels.providerCompatible",
    models: [],
  },
];

// Sub-presets of the OpenAI-compatible provider
const COMPATIBLE_SERVICES = [
  { key: "ifm", name: "MBZUAI IFM", baseUrl: "https://api.ifm.ai/v1",
    models: ["IFM/K2-Horizon-375B-A23B", "IFM/Jais-3-MoVA-36B-A4B"] },
  { key: "deepseek", name: "DeepSeek", baseUrl: "https://api.deepseek.com", models: ["deepseek-v4-flash"] },
  { key: "openrouter", name: "OpenRouter", baseUrl: "https://openrouter.ai/api/v1", models: [] },
  { key: "groq", name: "Groq", baseUrl: "https://api.groq.com/openai/v1", models: [] },
  { key: "custom", label: "myModels.serviceCustom", baseUrl: "", models: [] },
];

const normalizeUrl = (url) => String(url || "").trim().replace(/\/+$/, "").toLowerCase();

function emptyForm() {
  return {
    id: "",
    preset: "",
    provideCode: "",
    service: "ifm",
    apiKey: "",
    savedKey: "",
    modelName: "",
    baseUrl: "",
    effort: "low",
    displayName: "",
    isEnabled: 1,
    remark: "",
    original: {},
  };
}

export default {
  name: "MyModels",
  components: { HeaderBar, CustomDialog, VersionFooter },
  data() {
    return {
      modelTypes: [
        { value: "LLM", label: "myModels.typeLlm", enabled: true },
        { value: "ASR", label: "myModels.typeAsr", enabled: false },
        { value: "TTS", label: "myModels.typeTts", enabled: false },
      ],
      activeType: "LLM",
      models: [],
      providerCodes: null,
      loading: false,
      busy: {},
      testingId: "",
      dialogVisible: false,
      saving: false,
      testingDialog: false,
      testResult: null,
      autoName: "",
      form: emptyForm(),
      effortLevels: ["low", "medium", "high"],
      compatibleServices: COMPATIBLE_SERVICES,
    };
  },
  computed: {
    rules() {
      const required = (key) => [{ required: true, whitespace: true, message: this.$t(key), trigger: ["blur", "change"] }];
      return {
        preset: required("myModels.requiredProvider"),
        apiKey: required("myModels.requiredApiKey"),
        modelName: required("myModels.requiredModel"),
        baseUrl: required("myModels.requiredBaseUrl"),
        displayName: required("myModels.requiredName"),
      };
    },
    availablePresets() {
      // Show presets whose provider the backend allows; show all if the list is unknown
      if (!this.providerCodes || !this.providerCodes.length) return LLM_PRESETS;
      return LLM_PRESETS.filter((p) => this.providerCodes.includes(p.provideCode));
    },
    currentPreset() {
      return LLM_PRESETS.find((p) => p.key === this.form.preset) || null;
    },
    modelSuggestions() {
      if (!this.currentPreset) return [];
      if (this.currentPreset.key === "compatible") {
        const service = COMPATIBLE_SERVICES.find((s) => s.key === this.form.service);
        return service ? service.models : [];
      }
      return this.currentPreset.models;
    },
  },
  created() {
    this.loadProviders();
    this.loadModels();
  },
  methods: {
    selectType(type) {
      if (!type.enabled || this.activeType === type.value) return;
      this.activeType = type.value;
      this.loadProviders();
      this.loadModels();
    },
    loadProviders() {
      Api.model.getUserModelProviders(this.activeType, ({ data }) => {
        if (data && data.code === 0 && Array.isArray(data.data)) {
          this.providerCodes = data.data.map((p) => p.providerCode).filter(Boolean);
        }
      });
    },
    loadModels() {
      this.loading = true;
      Api.model.getUserModels(this.activeType, ({ data }) => {
        this.loading = false;
        if (data && data.code === 0) {
          this.models = Array.isArray(data.data) ? data.data : [];
        } else {
          this.$message.error((data && data.msg) || this.$t("myModels.loadFailed"));
        }
      });
    },

    // ---- list helpers ----
    presetOf(configJson) {
      const config = configJson || {};
      if (config.type === "claude") return { preset: "claude" };
      if (config.type === "gemini") return { preset: "gemini" };
      const url = normalizeUrl(config.base_url);
      if (config.type === "openai" && (!url || url === normalizeUrl(OPENAI_BASE_URL))) return { preset: "openai" };
      const service = COMPATIBLE_SERVICES.find((s) => s.baseUrl && normalizeUrl(s.baseUrl) === url);
      return { preset: "compatible", service: service ? service.key : "custom" };
    },
    providerLabel(row) {
      const config = row.configJson || {};
      const { preset, service } = this.presetOf(config);
      if (preset === "compatible") {
        const match = COMPATIBLE_SERVICES.find((s) => s.key === service);
        return match && match.name ? match.name : this.$t("myModels.providerCompatible");
      }
      const found = LLM_PRESETS.find((p) => p.key === preset);
      return found ? this.$t(found.label) : config.type || "—";
    },
    providerInitial(row) {
      return (this.providerLabel(row) || "?").trim().charAt(0).toUpperCase();
    },

    // ---- dialog ----
    openAdd() {
      this.form = emptyForm();
      this.autoName = "";
      this.testResult = null;
      if (this.availablePresets.length) this.choosePreset(this.availablePresets[0].key);
      this.dialogVisible = true;
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate());
    },
    openEdit(row) {
      const config = JSON.parse(JSON.stringify(row.configJson || {}));
      const { preset, service } = this.presetOf(config);
      this.form = {
        ...emptyForm(),
        id: row.id,
        preset,
        provideCode: config.type || "",
        service: service || "custom",
        apiKey: config.api_key || "",
        savedKey: config.api_key || "",
        modelName: config.model_name || "",
        baseUrl: config.base_url || "",
        effort: config.effort || "low",
        displayName: row.modelName || "",
        isEnabled: row.isEnabled === 0 ? 0 : 1,
        remark: row.remark || "",
        original: config,
      };
      this.autoName = "";
      this.testResult = null;
      this.dialogVisible = true;
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate());
    },
    choosePreset(key) {
      const preset = LLM_PRESETS.find((p) => p.key === key);
      if (!preset || this.form.preset === key) return;
      this.form.preset = key;
      this.form.provideCode = preset.provideCode;
      if (key === "compatible") {
        this.chooseService(this.form.service || "ifm");
      } else {
        this.form.baseUrl = preset.baseUrl || "";
        if (!this.form.id) this.setModel(preset.models[0] || "");
      }
      this.clearTest();
    },
    chooseService(key) {
      const service = COMPATIBLE_SERVICES.find((s) => s.key === key);
      if (!service) return;
      this.form.service = key;
      if (service.key !== "custom" || !this.form.baseUrl) this.form.baseUrl = service.baseUrl;
      if (!this.form.id) this.setModel(service.models[0] || "");
      this.clearTest();
    },
    setModel(name) {
      this.form.modelName = name;
      this.chooseModel(name);
    },
    chooseModel(name) {
      // Keep the display name in step with the model until the user edits it
      if (!this.form.displayName || this.form.displayName === this.autoName) {
        this.form.displayName = name || "";
        this.autoName = name || "";
      }
      this.clearTest();
    },
    clearTest() {
      this.testResult = null;
    },
    buildConfigJson() {
      const f = this.form;
      const preset = this.currentPreset || {};
      const config = { ...(preset.defaults || {}), ...(f.original || {}) };
      config.type = f.provideCode;
      config.api_key = f.apiKey.trim();
      config.model_name = String(f.modelName || "").trim();
      if (f.preset === "openai") {
        config.base_url = OPENAI_BASE_URL;
      } else if (f.preset === "compatible") {
        config.base_url = f.baseUrl.trim();
      } else {
        delete config.base_url;
      }
      if (f.preset === "claude") {
        config.effort = f.effort || "low";
      } else {
        delete config.effort;
      }
      return config;
    },
    validate() {
      return new Promise((resolve) => this.$refs.form.validate((valid) => resolve(valid)));
    },
    async testDialog() {
      if (!(await this.validate())) return;
      this.testingDialog = true;
      this.testResult = null;
      this.runTest(this.form.id || undefined, this.form.provideCode, this.buildConfigJson(), (result) => {
        this.testingDialog = false;
        this.testResult = result;
      });
    },
    testRow(row) {
      const config = row.configJson || {};
      this.testingId = row.id;
      this.runTest(row.id, config.type, config, (result) => {
        this.testingId = "";
        if (result.ok) {
          this.$message.success({ message: this.testResultText(result), showClose: true });
        } else {
          this.$message.error({ message: this.testResultText(result), showClose: true, duration: 6000 });
        }
      });
    },
    runTest(id, provideCode, configJson, done) {
      const body = { modelType: this.activeType, provideCode, configJson };
      if (id) body.id = id;
      Api.model.testUserModel(body, ({ data }) => {
        if (data && data.code === 0 && data.data) {
          done({ ok: !!data.data.ok, message: data.data.message || "", latencyMs: data.data.latencyMs });
        } else {
          done({ ok: false, message: (data && data.msg) || "" });
        }
      });
    },
    testResultText(result) {
      if (result.ok) {
        return result.latencyMs != null
          ? this.$t("myModels.testOkLatency", { ms: result.latencyMs })
          : this.$t("myModels.testOk");
      }
      return result.message
        ? `${this.$t("myModels.testFailed")}: ${result.message}`
        : this.$t("myModels.testFailed");
    },
    async save() {
      if (this.saving || !(await this.validate())) return;
      const f = this.form;
      const body = {
        modelName: f.displayName.trim(),
        configJson: this.buildConfigJson(),
        remark: f.remark || "",
        isEnabled: f.isEnabled,
      };
      this.saving = true;
      const done = ({ data }) => {
        this.saving = false;
        if (data && data.code === 0) {
          this.$message.success({ message: this.$t(f.id ? "myModels.saveSuccess" : "myModels.addSuccess"), showClose: true });
          this.dialogVisible = false;
          this.loadModels();
        } else {
          this.$message.error({ message: (data && data.msg) || this.$t("myModels.operationFailed"), showClose: true });
        }
      };
      if (f.id) {
        Api.model.updateUserModel(f.id, body, done);
      } else {
        Api.model.addUserModel(this.activeType, f.provideCode, body, done);
      }
    },

    // ---- row actions ----
    toggleEnabled(row, value) {
      const next = value ? 1 : 0;
      this.$set(this.busy, row.id, true);
      const body = { modelName: row.modelName, configJson: row.configJson, remark: row.remark || "", isEnabled: next };
      Api.model.updateUserModel(row.id, body, ({ data }) => {
        this.$delete(this.busy, row.id);
        if (data && data.code === 0) {
          row.isEnabled = next;
          this.$message.success(this.$t(next ? "myModels.enableSuccess" : "myModels.disableSuccess"));
        } else {
          this.$message.error((data && data.msg) || this.$t("myModels.operationFailed"));
        }
      });
    },
    removeRow(row) {
      this.$confirm(this.$t("myModels.deleteConfirm", { name: row.modelName }), this.$t("common.delete"), {
        confirmButtonText: this.$t("common.delete"),
        cancelButtonText: this.$t("common.cancel"),
        confirmButtonClass: "el-button--danger",
        type: "warning",
      })
        .then(() => {
          Api.model.deleteUserModel(row.id, ({ data }) => {
            if (data && data.code === 0) {
              this.$message.success({ message: this.$t("myModels.deleteSuccess"), showClose: true });
              this.loadModels();
            } else {
              this.$message.error({ message: (data && data.msg) || this.$t("myModels.operationFailed"), showClose: true });
            }
          });
        })
        .catch(() => {});
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

.page-heading {
  max-width: 720px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--x-text);
}

// Type switch (segmented)
.type-switch {
  display: inline-flex;
  flex-wrap: wrap;
  align-self: flex-start;
  gap: 2px;
  padding: 4px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  max-width: 100%;
  box-sizing: border-box;
}

.type-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 6px 12px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;

  &:hover:not(:disabled) {
    background: var(--x-surface-raised);
    color: var(--x-text);
  }

  &.is-active {
    background: var(--x-selected);
    color: var(--x-text);
  }

  &:disabled {
    color: var(--x-text-disabled);
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.soon {
  font-size: 11px;
  font-weight: 500;
  padding: 1px 6px;
  border-radius: var(--x-radius-pill);
  background: var(--x-canvas);
  color: var(--x-text-muted);
}

// Card
.models-card {
  min-height: 120px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.model-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.model-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--x-divider);

  &:last-child {
    border-bottom: 0;
  }
}

.model-row__avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  background: var(--x-surface-raised);
  color: var(--x-text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.model-row__main {
  flex: 1;
  min-width: 0;
  text-align: start;
}

.model-row__name {
  font-weight: 500;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-row__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: 13px;
  color: var(--x-text-muted);
  min-width: 0;

  .mono {
    overflow-wrap: anywhere;
  }
}

.mono {
  font-family: var(--x-font-mono);
  font-size: 12px;
}

.model-row__side {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.model-row__switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.switch-label {
  font-size: 13px;
  color: var(--x-text-secondary);
}

.model-row__actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.icon-btn {
  width: 32px;
  padding-inline: 0;
  color: var(--x-text-muted);

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
    border-color: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

// Empty state
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 56px 24px;
  text-align: center;
}

.empty__icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: 12px;
  color: var(--x-text-secondary);
  margin-bottom: 4px;
}

.empty__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

.empty__text {
  margin: 0 0 12px;
  max-width: 420px;
  font-size: 14px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

// Dialog
.my-model-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

// Keep the close button on the inline end so it does not cover the title in RTL
.my-model-dialog ::v-deep .el-dialog__headerbtn {
  right: auto;
  inset-inline-end: 16px;
}

.my-model-dialog ::v-deep .el-dialog__header {
  padding-inline-end: 56px;
}

.model-form {
  text-align: start;

  ::v-deep .el-form-item {
    margin-bottom: 16px;
  }

  ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    line-height: 20px;
    float: none;
    display: block;
    text-align: start;
  }

  ::v-deep .el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before {
    display: none;
  }
}

.full {
  width: 100%;
}

.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.preset {
  display: flex;
  align-items: center;
  min-height: 42px;
  padding: 8px 12px;
  border: 1px solid var(--x-border-control);
  border-radius: var(--x-radius-control);
  background: var(--x-surface);
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.3;
  text-align: start;
  cursor: pointer;

  &:hover:not(:disabled) {
    border-color: var(--x-text-muted);
    color: var(--x-text);
  }

  &.is-active {
    border-color: var(--x-ink);
    box-shadow: inset 0 0 0 1px var(--x-ink);
    color: var(--x-text);
  }

  &:disabled {
    color: var(--x-text-disabled);
    background: var(--x-surface-raised);
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

.test-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding-top: 4px;
}

.test-result {
  display: inline-flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  line-height: 1.45;
  overflow-wrap: break-word;
  flex: 1 1 240px;
  min-width: 0;

  i {
    margin-top: 2px;
  }

  &.is-ok {
    color: var(--x-success-text);
  }

  &.is-error {
    color: var(--x-danger-text);
  }
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 768px) {
  .page {
    padding: 16px;
  }

  .model-row {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .model-row__main {
    flex: 1 1 calc(100% - 64px);
  }

  .type-switch {
    align-self: stretch;
    flex-wrap: nowrap;
    overflow-x: auto;
  }

  .type-tab {
    flex-shrink: 0;
    white-space: nowrap;
  }

  .soon {
    display: none;
  }

  .model-row__side {
    flex: 1 1 100%;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;
    padding-inline-start: 50px;
  }
}

@media (max-width: 480px) {
  .preset {
    padding: 8px 10px;
    font-size: 13px;
  }
}
</style>
