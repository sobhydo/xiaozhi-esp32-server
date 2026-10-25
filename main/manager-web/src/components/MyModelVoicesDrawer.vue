<template>
  <el-drawer
    :visible="visible"
    :direction="drawerDirection"
    size="520px"
    :show-close="false"
    :with-header="true"
    custom-class="my-voices-drawer"
    @update:visible="(val) => $emit('update:visible', val)"
    @open="onOpen"
    @closed="onClosed"
  >
    <div slot="title" class="drawer-header">
      <div class="drawer-heading">
        <h2 class="drawer-title">{{ $t("myModels.voicesTitle") }}</h2>
        <p v-if="model" class="drawer-subtitle">{{ model.modelName }}</p>
      </div>
      <button type="button" class="drawer-close-btn" :aria-label="$t('button.close')" @click="close">
        <i class="el-icon-close" aria-hidden="true"></i>
      </button>
    </div>

    <div class="drawer-content">
      <p class="intro">{{ $t("myModels.voicesIntro") }}</p>

      <!-- Saved voices of the model -->
      <section class="block" v-loading="loadingVoices" element-loading-background="rgba(255, 255, 255, 0.7)">
        <h3 class="block-title">
          {{ $t("myModels.yourVoices") }}
          <span v-if="voices.length" class="count">{{ voices.length }}</span>
        </h3>
        <ul v-if="voices.length" class="voice-list">
          <li v-for="voice in voices" :key="voice.id" class="voice-row">
            <div class="voice-row__main">
              <div class="voice-row__name">{{ voice.name }}</div>
              <div class="voice-row__meta">
                <span class="mono" dir="ltr">{{ voice.ttsVoice }}</span>
                <template v-if="voice.languages">
                  <span class="dot" aria-hidden="true">·</span>
                  <span>{{ voice.languages }}</span>
                </template>
              </div>
            </div>
            <el-tooltip :content="$t('common.delete')" placement="top">
              <el-button
                size="small"
                class="icon-btn icon-btn--danger"
                icon="el-icon-delete"
                :loading="deletingId === voice.id"
                :aria-label="$t('common.delete')"
                @click="removeVoice(voice)"
              />
            </el-tooltip>
          </li>
        </ul>
        <p v-else-if="!loadingVoices" class="muted">{{ $t("myModels.noVoices") }}</p>
      </section>

      <!-- Add a voice by id -->
      <section class="block block--card">
        <h3 class="block-title">{{ $t("myModels.addVoice") }}</h3>
        <el-form ref="voiceForm" :model="voiceForm" :rules="rules" label-position="top" class="voice-form"
          @submit.native.prevent>
          <div class="form-grid">
            <el-form-item :label="$t('myModels.voiceName')" prop="name">
              <el-input ref="nameInput" v-model="voiceForm.name" maxlength="64" />
            </el-form-item>
            <el-form-item :label="$t('myModels.voiceId')" prop="ttsVoice">
              <el-input v-model="voiceForm.ttsVoice" dir="ltr" maxlength="50" />
            </el-form-item>
          </div>
          <el-form-item :label="$t('myModels.voiceLanguages')" prop="languages">
            <el-select
              v-model="voiceForm.languages"
              class="full"
              multiple
              filterable
              allow-create
              default-first-option
              :placeholder="$t('myModels.voiceLanguagesPlaceholder')"
            >
              <el-option v-for="lang in languageOptions" :key="lang.value" :label="$t(lang.label)" :value="lang.value" />
            </el-select>
          </el-form-item>
          <div class="form-actions">
            <el-button type="primary" size="small" icon="el-icon-plus" :loading="adding" @click="addVoice">
              {{ $t("myModels.addVoice") }}
            </el-button>
          </div>
        </el-form>
      </section>

      <!-- Voices available on the provider account -->
      <section class="block">
        <div class="block-head">
          <h3 class="block-title">{{ $t("myModels.providerVoices") }}</h3>
          <el-button v-if="providerLoaded" size="small" type="text" icon="el-icon-refresh" :loading="loadingProvider"
            @click="loadProviderVoices">
            {{ $t("myModels.refresh") }}
          </el-button>
        </div>
        <div v-if="!providerLoaded" class="browse">
          <p class="muted">{{ $t("myModels.providerVoicesHint") }}</p>
          <el-button size="small" icon="el-icon-search" :loading="loadingProvider" @click="loadProviderVoices">
            {{ $t("myModels.browseProviderVoices") }}
          </el-button>
          <p v-if="providerError" class="error-text" role="alert">{{ providerError }}</p>
        </div>
        <template v-else>
          <el-input v-model="search" size="small" clearable prefix-icon="el-icon-search"
            :placeholder="$t('myModels.searchVoices')" :aria-label="$t('myModels.searchVoices')" class="search" />
          <p v-if="providerError" class="error-text" role="alert">{{ providerError }}</p>
          <ul v-if="filteredProviderVoices.length" class="voice-list voice-list--provider">
            <li v-for="voice in filteredProviderVoices" :key="voice.voiceId" class="voice-row">
              <el-button
                v-if="voice.previewUrl"
                size="small"
                class="icon-btn"
                :icon="playingId === voice.voiceId ? 'el-icon-video-pause' : 'el-icon-video-play'"
                :aria-label="$t('ttsModel.preview')"
                @click="togglePreview(voice)"
              />
              <span v-else class="icon-placeholder" aria-hidden="true"></span>
              <div class="voice-row__main">
                <div class="voice-row__name">
                  {{ voice.name || voice.voiceId }}
                  <span v-if="voice.category" class="tag">{{ voice.category }}</span>
                </div>
                <div class="voice-row__meta">
                  <span class="mono" dir="ltr">{{ voice.voiceId }}</span>
                  <template v-if="labelText(voice)">
                    <span class="dot" aria-hidden="true">·</span>
                    <span class="labels">{{ labelText(voice) }}</span>
                  </template>
                </div>
              </div>
              <el-button v-if="isAdded(voice)" size="small" disabled>{{ $t("myModels.voiceAdded") }}</el-button>
              <el-button v-else size="small" @click="pickProviderVoice(voice)">{{ $t("myModels.addThisVoice") }}</el-button>
            </li>
          </ul>
          <p v-else-if="!loadingProvider" class="muted">{{ $t("myModels.noProviderVoices") }}</p>
        </template>
      </section>

      <!-- ElevenLabs: clone a new voice with this key -->
      <section v-if="isElevenlabs" class="block block--card clone-callout">
        <div>
          <h3 class="block-title">{{ $t("myModels.cloneVoiceTitle") }}</h3>
          <p class="muted">{{ $t("myModels.cloneVoiceHint") }}</p>
        </div>
        <el-button size="small" icon="el-icon-microphone" @click="goToClone">{{ $t("myModels.cloneVoice") }}</el-button>
      </section>
    </div>
  </el-drawer>
</template>

<script>
import Api from "@/apis/api";

const DEFAULT_LANGUAGES = ["English", "Arabic"];

function emptyVoiceForm() {
  return { name: "", ttsVoice: "", languages: DEFAULT_LANGUAGES.slice() };
}

export default {
  name: "MyModelVoicesDrawer",
  props: {
    visible: { type: Boolean, default: false },
    // The own TTS model row from /models/mine
    model: { type: Object, default: null },
  },
  data() {
    return {
      voices: [],
      loadingVoices: false,
      deletingId: "",
      adding: false,
      voiceForm: emptyVoiceForm(),
      languageOptions: [
        { value: "English", label: "myModels.languageEnglish" },
        { value: "Arabic", label: "myModels.languageArabic" },
      ],
      providerVoices: [],
      providerLoaded: false,
      loadingProvider: false,
      providerError: "",
      search: "",
      audio: null,
      playingId: "",
    };
  },
  computed: {
    drawerDirection() {
      // Open from the inline end: right in LTR, left in RTL
      return this.$i18n.locale === "ar" ? "ltr" : "rtl";
    },
    isElevenlabs() {
      return !!(this.model && this.model.configJson && this.model.configJson.type === "elevenlabs");
    },
    rules() {
      const required = (key) => [{ required: true, whitespace: true, message: this.$t(key), trigger: ["blur", "change"] }];
      return {
        name: required("myModels.requiredVoiceName"),
        ttsVoice: required("myModels.requiredVoice"),
      };
    },
    filteredProviderVoices() {
      const term = this.search.trim().toLowerCase();
      if (!term) return this.providerVoices;
      return this.providerVoices.filter((voice) =>
        [voice.name, voice.voiceId, voice.category, this.labelText(voice)]
          .some((value) => String(value || "").toLowerCase().includes(term))
      );
    },
  },
  beforeDestroy() {
    this.stopPreview();
  },
  methods: {
    close() {
      this.$emit("update:visible", false);
    },
    onOpen() {
      this.voices = [];
      this.providerVoices = [];
      this.providerLoaded = false;
      this.providerError = "";
      this.search = "";
      this.voiceForm = emptyVoiceForm();
      this.$nextTick(() => this.$refs.voiceForm && this.$refs.voiceForm.clearValidate());
      this.loadVoices();
    },
    onClosed() {
      this.stopPreview();
    },
    loadVoices() {
      if (!this.model) return;
      const modelId = this.model.id;
      this.loadingVoices = true;
      Api.model.getUserModelVoices(modelId, ({ data }) => {
        if (!this.model || this.model.id !== modelId) return;
        this.loadingVoices = false;
        if (data && data.code === 0) {
          this.voices = Array.isArray(data.data) ? data.data : [];
        } else {
          this.$message.error({ message: (data && data.msg) || this.$t("myModels.loadVoicesFailed"), showClose: true });
        }
      });
    },
    labelText(voice) {
      const labels = voice && voice.labels;
      if (!labels) return "";
      if (Array.isArray(labels)) return labels.filter(Boolean).join(", ");
      if (typeof labels === "object") return Object.values(labels).filter(Boolean).join(", ");
      return String(labels);
    },
    isAdded(voice) {
      return this.voices.some((item) => item.ttsVoice === voice.voiceId);
    },
    addVoice() {
      if (this.adding || !this.model) return;
      this.$refs.voiceForm.validate((valid) => {
        if (!valid) return;
        const languages = this.voiceForm.languages.map((lang) => String(lang).trim()).filter(Boolean);
        const body = {
          name: this.voiceForm.name.trim(),
          ttsVoice: this.voiceForm.ttsVoice.trim(),
          languages: (languages.length ? languages : DEFAULT_LANGUAGES).join(", "),
        };
        this.adding = true;
        Api.model.addUserModelVoice(this.model.id, body, ({ data }) => {
          this.adding = false;
          if (data && data.code === 0) {
            this.$message.success({ message: this.$t("myModels.voiceAddSuccess"), showClose: true });
            this.voiceForm = emptyVoiceForm();
            this.$nextTick(() => this.$refs.voiceForm && this.$refs.voiceForm.clearValidate());
            this.loadVoices();
          } else {
            this.$message.error({ message: (data && data.msg) || this.$t("myModels.operationFailed"), showClose: true });
          }
        });
      });
    },
    removeVoice(voice) {
      this.$confirm(this.$t("myModels.deleteVoiceConfirm", { name: voice.name }), this.$t("common.delete"), {
        confirmButtonText: this.$t("common.delete"),
        cancelButtonText: this.$t("common.cancel"),
        confirmButtonClass: "el-button--danger",
        type: "warning",
      })
        .then(() => {
          this.deletingId = voice.id;
          Api.model.deleteUserModelVoice(this.model.id, voice.id, ({ data }) => {
            this.deletingId = "";
            if (data && data.code === 0) {
              this.$message.success({ message: this.$t("myModels.voiceDeleteSuccess"), showClose: true });
              this.loadVoices();
            } else {
              this.$message.error({ message: (data && data.msg) || this.$t("myModels.operationFailed"), showClose: true });
            }
          });
        })
        .catch(() => {});
    },
    loadProviderVoices() {
      if (!this.model || this.loadingProvider) return;
      const modelId = this.model.id;
      this.loadingProvider = true;
      this.providerError = "";
      Api.model.getUserModelProviderVoices(modelId, ({ data }) => {
        if (!this.model || this.model.id !== modelId) return;
        this.loadingProvider = false;
        if (data && data.code === 0) {
          // Accept plain voice-name strings too (OpenAI returns a fixed list)
          this.providerVoices = (Array.isArray(data.data) ? data.data : [])
            .map((item) => (typeof item === "string" ? { voiceId: item, name: item } : item))
            .filter((item) => item && item.voiceId);
          this.providerLoaded = true;
        } else {
          this.providerError = (data && data.msg) || this.$t("myModels.loadProviderVoicesFailed");
        }
      });
    },
    pickProviderVoice(voice) {
      this.voiceForm.name = String(voice.name || voice.voiceId).slice(0, 64);
      this.voiceForm.ttsVoice = voice.voiceId;
      this.$nextTick(() => {
        if (this.$refs.voiceForm) this.$refs.voiceForm.clearValidate();
        const input = this.$refs.nameInput;
        if (input) {
          input.$el.scrollIntoView({ block: "center", behavior: "smooth" });
          input.focus();
        }
      });
    },
    togglePreview(voice) {
      if (this.playingId === voice.voiceId) {
        this.stopPreview();
        return;
      }
      this.stopPreview();
      const audio = new Audio(voice.previewUrl);
      this.audio = audio;
      this.playingId = voice.voiceId;
      const reset = () => {
        if (this.audio === audio) {
          this.audio = null;
          this.playingId = "";
        }
      };
      audio.addEventListener("ended", reset);
      audio.addEventListener("error", reset);
      audio.play().catch(() => {
        reset();
        this.$message.error(this.$t("myModels.previewFailed"));
      });
    },
    stopPreview() {
      if (this.audio) {
        this.audio.pause();
        this.audio = null;
      }
      this.playingId = "";
    },
    goToClone() {
      const modelId = this.model && this.model.id;
      this.close();
      this.$router.push({ path: "/voice-clone-management", query: modelId ? { modelId } : {} });
    },
  },
};
</script>

<style lang="scss" scoped>
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-block: 18px 16px;
  padding-inline: 24px 20px;
  border-bottom: 1px solid var(--x-divider);
}

.drawer-heading {
  min-width: 0;
  text-align: start;
}

.drawer-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

.drawer-subtitle {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--x-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-close-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
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

  &:hover,
  &:focus-visible {
    color: var(--x-text);
    background: var(--x-canvas);
    outline: none;
  }
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  text-align: start;
}

.intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

.block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 40px;
  flex-shrink: 0;
}

.block--card {
  padding: 16px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  background: var(--x-surface-raised);
}

.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.block-title {
  margin: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--x-text);
}

.count {
  font-size: 12px;
  font-weight: 500;
  padding: 0 7px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
}

.muted {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

.error-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--x-danger-text);
  overflow-wrap: anywhere;
}

.voice-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  background: var(--x-surface);
}

.voice-list--provider {
  max-height: 420px;
  overflow-y: auto;
}

.voice-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--x-divider);

  &:last-child {
    border-bottom: 0;
  }

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.voice-row__main {
  flex: 1;
  min-width: 0;
}

.voice-row__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.voice-row__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  font-size: 12px;
  color: var(--x-text-muted);
  min-width: 0;

  .mono {
    overflow-wrap: anywhere;
  }
}

.labels {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.mono {
  font-family: var(--x-font-mono);
  font-size: 12px;
}

.tag {
  flex-shrink: 0;
  font-size: 11px;
  font-weight: 500;
  padding: 0 6px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
  line-height: 18px;
}

.icon-btn {
  width: 32px;
  flex-shrink: 0;
  padding-inline: 0;
  color: var(--x-text-muted);

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
    border-color: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

.icon-placeholder {
  width: 32px;
  flex-shrink: 0;
}

.voice-form {

  // Element positions tag close icons with physical offsets; mirror them for RTL
  ::v-deep .el-select__tags .el-tag {
    margin-block: 2px;
    margin-inline: 6px 0;
  }

  ::v-deep .el-select__tags .el-tag__close.el-icon-close {
    right: auto;
    inset-inline-end: -7px;
  }
  ::v-deep .el-form-item {
    margin-bottom: 12px;
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

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 12px;
}

.full {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.browse {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.clone-callout {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  .muted {
    margin-top: 4px;
  }

  .el-button {
    flex-shrink: 0;
  }
}

@media (max-width: 520px) {
  .drawer-content {
    padding: 16px;
  }

  .drawer-header {
    padding-inline: 16px 12px;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .clone-callout {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<style>
.my-voices-drawer {
  max-width: 100vw;
}

.my-voices-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 0;
}

.my-voices-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0;
}
</style>
