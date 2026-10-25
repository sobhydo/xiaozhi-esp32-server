<template>
  <div class="welcome">
    <HeaderBar />

    <div class="role-page" v-loading="agentReloading">
      <nav class="breadcrumb" :aria-label="$t('roleConfig.title')">
        <button type="button" class="breadcrumb-link" @click="goToHome">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="m15 18-6-6 6-6" />
          </svg>
          {{ $t("header.smartManagement") }}
        </button>
        <span class="breadcrumb-sep" aria-hidden="true">/</span>
        <span class="breadcrumb-current">{{ $t("roleConfig.title") }}</span>
      </nav>

      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ form.agentName || $t("roleConfig.title") }}</h1>
          <p class="page-desc">{{ $t("roleConfig.restartNotice") }}</p>
        </div>
        <div class="header-actions">
          <span v-if="currentVersionNo" class="current-version-tag">
            {{ $t("roleConfig.currentVersion", { version: currentVersionNo }) }}
          </span>
          <el-button class="history-btn" size="small" @click="showSnapshotDialog = true">
            {{ $t("roleConfig.snapshotHistory") }}
          </el-button>
          <el-button class="reset-btn" size="small" @click="resetConfig">{{
            $t("roleConfig.reset")
          }}</el-button>
          <el-button
            type="primary"
            size="small"
            class="save-btn"
            :disabled="configInteractionBlocked"
            @click="saveConfig"
          >
            {{ $t("roleConfig.saveConfig") }}
          </el-button>
        </div>
      </header>

      <div class="header-tags">
        <el-tag
          v-for="tag in dynamicTags"
          :key="tag.id"
          class="custom-tag"
          closable
          :disable-transitions="false"
          @close="handleClose(tag.id)">
          {{tag.tagName}}
        </el-tag>
        <el-input
          class="input-new-tag"
          v-if="inputVisible"
          v-model="inputValue"
          ref="saveTagInput"
          size="small"
          maxLength="20"
          :aria-label="$t('roleConfig.addTag')"
          @keyup.enter.native="handleInputConfirm"
          @blur="handleInputConfirm"
        >
        </el-input>
        <el-button class="custom-tag-btn" v-else size="mini" @click="showInput">+ {{ $t("roleConfig.addTag") }}</el-button>
      </div>

      <el-form ref="form" :model="form" label-position="top" class="role-form">
        <!-- Role -->
        <section class="config-card">
          <div class="card-head">
            <h2 class="card-title">{{ $t("roleConfig.title") }}</h2>
          </div>
          <div class="card-body grid-2">
            <el-form-item>
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.agentName')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.agentName') }}</span>
                </el-tooltip>
              </template>
              <el-input
                v-model="form.agentName"
                class="form-input"
                maxlength="64"
              />
            </el-form-item>
            <el-form-item
              style="display: none"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.languageCode')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.languageCode') }}</span>
                </el-tooltip>
              </template>
              <el-input
                v-model="form.langCode"
                :placeholder="$t('roleConfig.pleaseEnterLangCode')"
                maxlength="10"
                show-word-limit
                class="form-input"
              />
            </el-form-item>
            <el-form-item
              style="display: none"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.interactionLanguage')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.interactionLanguage') }}</span>
                </el-tooltip>
              </template>
              <el-input
                v-model="form.language"
                :placeholder="$t('roleConfig.pleaseEnterLangName')"
                maxlength="10"
                show-word-limit
                class="form-input"
              />
            </el-form-item>
            <el-form-item class="span-all">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.roleTemplate')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.roleTemplate') }}</span>
                </el-tooltip>
              </template>
              <div class="template-container">
                <button
                  v-for="(template, index) in templates"
                  :key="`template-${index}`"
                  type="button"
                  class="template-item"
                  :class="{ 'template-loading': loadingTemplate }"
                  @click="selectTemplate(template)"
                >
                  {{ template.agentName }}
                </button>
              </div>
            </el-form-item>
            <el-form-item class="span-all">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.roleIntroduction')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.roleIntroduction') }}</span>
                </el-tooltip>
              </template>
              <el-input
                type="textarea"
                rows="8"
                resize="vertical"
                :placeholder="$t('roleConfig.pleaseEnterContent')"
                v-model="form.systemPrompt"
                maxlength="2000"
                show-word-limit
                class="form-textarea"
              />
            </el-form-item>
            <el-form-item class="span-all">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.memoryHis')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.memoryHis') }}</span>
                </el-tooltip>
              </template>
              <el-input
                type="textarea"
                rows="4"
                resize="vertical"
                v-model="form.summaryMemory"
                maxlength="2000"
                show-word-limit
                class="form-textarea"
                :disabled="form.model.memModelId !== 'Memory_mem_local_short'"
              />
            </el-form-item>
          </div>
        </section>

        <!-- Models -->
        <section class="config-card">
          <div class="card-head">
            <h2 class="card-title">{{ $t("header.modelConfig") }}</h2>
          </div>
          <div class="card-body model-grid">
            <el-form-item
              v-if="featureStatus.vad"
              class="model-item"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.vad')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.vad') }} <span class="model-code">· VAD</span></span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.model.vadModelId"
                filterable
                :placeholder="$t('roleConfig.pleaseSelect')"
                class="form-select"
                @change="handleModelChange('VAD', $event)"
              >
                <el-option
                  v-for="(item, optionIndex) in modelOptions['VAD']"
                  :key="`option-vad-${optionIndex}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              v-if="featureStatus.asr"
              class="model-item"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.asr')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.asr') }} <span class="model-code">· ASR</span></span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.model.asrModelId"
                filterable
                :placeholder="$t('roleConfig.pleaseSelect')"
                class="form-select"
                @change="handleModelChange('ASR', $event)"
              >
                <el-option
                  v-for="(item, optionIndex) in modelOptions['ASR']"
                  :key="`option-asr-${optionIndex}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item class="model-item">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.llm')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.llm') }} <span class="model-code">· LLM</span></span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.model.llmModelId"
                filterable
                :placeholder="$t('roleConfig.pleaseSelect')"
                class="form-select"
                @change="handleModelChange('LLM', $event)"
              >
                <el-option
                  v-for="(item, optionIndex) in modelOptions['LLM']"
                  :key="`option-asr-${optionIndex}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item class="model-item">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.slm')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.slm') }} <span class="model-code">· SLM</span></span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.model.slmModelId"
                filterable
                :placeholder="$t('roleConfig.pleaseSelect')"
                class="form-select"
              >
                <el-option
                  v-for="(item, optionIndex) in modelOptions['LLM']"
                  :key="`option-asr-${optionIndex}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item
              v-for="(model, index) in models.slice(4)"
              v-if="model.type !== 'TTS'"
              :key="`model-${index}`"
              class="model-item"
              :class="{ 'model-item--wide': model.type === 'Memory' && form.model.memModelId !== 'Memory_nomem' }"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.' + model.type.toLowerCase())" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.' + model.type.toLowerCase()) }} <span class="model-code">· {{ model.type }}</span></span>
                </el-tooltip>
              </template>
              <div class="model-select-wrapper">
                <el-select
                  v-model="form.model[model.key]"
                  filterable
                  :placeholder="$t('roleConfig.pleaseSelect')"
                  class="form-select"
                  @change="handleModelChange(model.type, $event)"
                >
                  <el-option
                    v-for="(item, optionIndex) in modelOptions[model.type]"
                    v-if="!item.isHidden"
                    :key="`option-${index}-${optionIndex}`"
                    :label="item.label"
                    :value="item.value"
                  >
                    <span>{{ item.label }}</span>
                    <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                  </el-option>
                </el-select>
                <div
                  v-if="
                    model.type === 'Memory' &&
                    form.model.memModelId !== 'Memory_nomem'
                  "
                  class="chat-history-options"
                >
                  <el-radio-group
                    v-model="form.chatHistoryConf"
                    size="small"
                    @change="updateChatHistoryConf"
                  >
                    <el-radio-button :label="1">{{
                      $t("roleConfig.reportText")
                    }}</el-radio-button>
                    <el-radio-button :label="2">{{
                      $t("roleConfig.reportTextVoice")
                    }}</el-radio-button>
                  </el-radio-group>
                </div>
              </div>
            </el-form-item>
          </div>
        </section>

        <!-- Voice -->
        <section class="config-card">
          <div class="card-head card-head--actions">
            <h2 class="card-title">{{ $t("roleConfig.tts") }}</h2>
            <div class="card-actions">
              <el-button
                size="small"
                :icon="playingVoice && currentPlayingVoiceId === form.ttsVoiceId && !isPaused ? 'el-icon-video-pause' : 'el-icon-video-play'"
                :disabled="!form.ttsVoiceId || voiceOptionsLoading"
                @click="toggleAudioPlayback(form.ttsVoiceId)"
              >
                {{ $t("ttsModel.preview") }}
              </el-button>
              <el-button size="small" class="edit-function-btn" @click="openTtsAdvancedSettings">
                {{ $t('roleConfig.advancedSettings') }}
              </el-button>
            </div>
          </div>
          <div class="card-body grid-2">
            <el-form-item
              v-for="(model, index) in models.slice(4)"
              v-if="model.type === 'TTS'"
              :key="`tts-model-${index}`"
              class="model-item span-all"
            >
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.' + model.type.toLowerCase())" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.' + model.type.toLowerCase()) }} <span class="model-code">· {{ model.type }}</span></span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.model[model.key]"
                filterable
                :disabled="model.type === 'TTS' && voiceOptionsLoading"
                :placeholder="$t('roleConfig.pleaseSelect')"
                class="form-select"
                @change="handleModelChange(model.type, $event)"
              >
                <el-option
                  v-for="(item, optionIndex) in modelOptions[model.type]"
                  v-if="!item.isHidden"
                  :key="`option-${index}-${optionIndex}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <span>{{ item.label }}</span>
                  <span v-if="item.owned" class="owned-model-tag">{{ $t("myModels.mineTag") }}</span>
                </el-option>
              </el-select>
            </el-form-item>

            <!-- Language filter -->
            <el-form-item class="model-item language-select-item">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.language')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.language') }}</span>
                </el-tooltip>
              </template>
              <el-select
                v-model="selectedLanguage"
                :disabled="voiceOptionsLoading"
                :placeholder="$t('roleConfig.selectLanguage')"
                class="form-select language-select"
                @change="handleLanguageChange"
              >
                <el-option
                  v-for="(lang, index) in languageOptions"
                  :key="`lang-${index}`"
                  :label="lang.label"
                  :value="lang.value"
                />
              </el-select>
            </el-form-item>

            <!-- Voice selector -->
            <el-form-item class="model-item">
              <template #label>
                <el-tooltip :content="$t('roleConfig.tooltip.voiceType')" placement="top" effect="light" popper-class="custom-tooltip">
                  <span>{{ $t('roleConfig.voiceType') }}</span>
                </el-tooltip>
              </template>
              <el-select
                v-model="form.ttsVoiceId"
                filterable
                :disabled="voiceOptionsLoading"
                :placeholder="ownedTtsWithoutVoices ? $t('myModels.defaultVoice') : $t('roleConfig.pleaseSelect')"
                class="form-select"
                @change="handleVoiceChange"
              >
                <el-option
                  v-for="(item, index) in voiceOptions"
                  :key="`voice-${index}`"
                  :label="item.label"
                  :value="item.value"
                >
                  <div class="voice-option">
                    <span>{{ item.label }}</span>
                    <template v-if="hasAudioPreview(item)">
                      <el-button
                        type="text"
                        :icon="
                          playingVoice &&
                          currentPlayingVoiceId === item.value &&
                          !isPaused
                            ? 'el-icon-video-pause'
                            : 'el-icon-video-play'
                        "
                        size="small"
                        :aria-label="$t('ttsModel.preview')"
                        @click.stop="toggleAudioPlayback(item.value)"
                        :loading="false"
                        class="play-button"
                      />
                    </template>
                  </div>
                </el-option>
              </el-select>
              <p v-if="ownedTtsWithoutVoices" class="owned-voice-hint">
                {{ $t("roleConfig.ownTtsDefaultVoice") }}
                <router-link :to="{ path: '/my-models', query: { type: 'tts' } }">{{ $t("header.myModels") }}</router-link>
              </p>
            </el-form-item>

            <!-- Read-only summary of the TTS parameters edited in the advanced dialog -->
            <div class="span-all tts-params">
              <div
                v-for="param in [
                  { key: 'volume', label: $t('roleConfig.ttsVolume'), hint: $t('roleConfig.volumeHint') },
                  { key: 'speed', label: $t('roleConfig.ttsRate'), hint: $t('roleConfig.speedHint') },
                  { key: 'pitch', label: $t('roleConfig.ttsPitch'), hint: $t('roleConfig.pitchHint') }
                ]"
                :key="param.key"
                class="tts-param"
              >
                <div class="tts-param-head">
                  <span class="tts-param-label">{{ param.label }}</span>
                  <span class="tts-param-value">{{ ttsSettings[param.key] || 0 }}</span>
                </div>
                <div class="tts-param-track" aria-hidden="true">
                  <span
                    class="tts-param-fill"
                    :style="{
                      left: Math.min(50, 50 + (ttsSettings[param.key] || 0) / 2) + '%',
                      width: Math.abs(ttsSettings[param.key] || 0) / 2 + '%'
                    }"
                  ></span>
                  <span
                    class="tts-param-thumb"
                    :style="{ left: 50 + (ttsSettings[param.key] || 0) / 2 + '%' }"
                  ></span>
                </div>
                <span class="tts-param-hint">{{ param.hint }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- Functions (only when an intent model is active) -->
        <section v-if="showFunctionIcons('Intent')" class="config-card">
          <div class="card-head card-head--actions">
            <h2 class="card-title">{{ $t("agentSnapshot.field.functions") }}</h2>
            <div class="card-actions">
              <el-button
                size="small"
                class="edit-function-btn"
                @click="openFunctionDialog"
                :class="{ 'active-btn': showFunctionDialog }"
              >
                {{ $t("roleConfig.editFunctions") }}
              </el-button>
            </div>
          </div>
          <div class="card-body">
            <div class="function-icons">
              <el-tooltip
                v-for="func in currentFunctions"
                :key="func.name"
                effect="light"
                placement="top"
              >
                <div slot="content">
                  <div><strong>{{ $t("roleConfig.functionName") }}:</strong> {{ func.name }}</div>
                </div>
                <span class="function-chip">
                  <span class="icon-dot">{{ getFunctionDisplayChar(func.name) }}</span>
                  {{ func.name }}
                </span>
              </el-tooltip>
            </div>
          </div>
        </section>

        <!-- Context providers -->
        <section class="config-card">
          <div class="card-head card-head--actions">
            <el-tooltip :content="$t('roleConfig.tooltip.contextProvider')" placement="top" effect="light" popper-class="custom-tooltip">
              <h2 class="card-title">{{ $t('roleConfig.contextProvider') }}</h2>
            </el-tooltip>
            <div class="card-actions">
              <el-button
                class="edit-function-btn"
                size="small"
                @click="openContextProviderDialog"
              >
                {{ $t('roleConfig.editContextProvider') }}
              </el-button>
            </div>
          </div>
          <div class="card-body">
            <p class="context-text">
              {{ $t('roleConfig.contextProviderSuccess', { count: currentContextProviders.length }) }}
              <a href="https://github.com/xinnan-tech/xiaozhi-esp32-server/blob/main/docs/context-provider-integration.md" target="_blank" rel="noopener" class="doc-link">{{ $t('roleConfig.contextProviderDocLink') }}</a>
            </p>
          </div>
        </section>
      </el-form>
    </div>
    <function-dialog
      v-model="showFunctionDialog"
      :functions="currentFunctions"
      :all-functions="allFunctions"
      :agent-id="$route.query.agentId"
      @update-functions="handleUpdateFunctions"
      @dialog-closed="handleDialogClosed"
    />
    <context-provider-dialog
      :visible.sync="showContextProviderDialog"
      :providers="currentContextProviders"
      @confirm="handleUpdateContext"
    />
    <tts-advanced-settings
      :visible.sync="showTtsAdvancedDialog"
      :settings="ttsSettings"
      :checked-replacement-word-ids="checkedReplacementWordIds"
      @save="handleTtsSettingsSave"
    />
      <agent-snapshot-dialog
        v-if="$route.query.agentId"
        :visible.sync="showSnapshotDialog"
        :agent-id="$route.query.agentId"
        :current-version-no="currentVersionNo"
        @restored="handleSnapshotRestored"
      />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import { getServiceUrl } from "@/apis/api";
import RequestService from "@/apis/httpRequest";
import FunctionDialog from "@/components/FunctionDialog.vue";
import ContextProviderDialog from "@/components/ContextProviderDialog.vue";
import TtsAdvancedSettings from "@/components/TtsAdvancedSettings.vue";
import AgentSnapshotDialog from "@/components/AgentSnapshotDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import i18n from "@/i18n";
import featureManager from "@/utils/featureManager"; 
import VersionFooter from "@/components/VersionFooter.vue";

export default {
  name: "RoleConfigPage",
  components: { HeaderBar, FunctionDialog, ContextProviderDialog, TtsAdvancedSettings, AgentSnapshotDialog, VersionFooter },
  data() {
    return {
      showContextProviderDialog: false,
      showTtsAdvancedDialog: false,
      showSnapshotDialog: false,
      ttsSettings: {
        volume: 0,
        speed: 0,
        pitch: 0
      },
      tempSummaryMemory: "",
      form: {
        agentCode: "",
        agentName: "",
        ttsVoiceId: "",
        ttsVolume: null,
        ttsRate: null,
        ttsPitch: null,
        chatHistoryConf: 0,
        systemPrompt: "",
        summaryMemory: "",
        langCode: "",
        language: "",
        sort: "",
        model: {
          ttsModelId: "",
          vadModelId: "",
          asrModelId: "",
          llmModelId: "",
          slmModelId: "",
          vllmModelId: "",
          memModelId: "",
          intentModelId: "",
        },
      },
      models: [
        { label: this.$t("roleConfig.vad"), key: "vadModelId", type: "VAD" },
        { label: this.$t("roleConfig.asr"), key: "asrModelId", type: "ASR" },
        { label: this.$t("roleConfig.llm"), key: "llmModelId", type: "LLM" },
        { label: this.$t("roleConfig.slm"), key: "slmModelId", type: "SLM" },
        { label: this.$t("roleConfig.vllm"), key: "vllmModelId", type: "VLLM" },
        { label: this.$t("roleConfig.intent"), key: "intentModelId", type: "Intent" },
        { label: this.$t("roleConfig.memory"), key: "memModelId", type: "Memory" },
        { label: this.$t("roleConfig.tts"), key: "ttsModelId", type: "TTS" },
      ],
      llmModeTypeMap: new Map(),
      modelOptions: {},
      templates: [],
      loadingTemplate: false,
      voiceOptions: [],
      voiceDetails: {}, // Full voice details
      showFunctionDialog: false,
      currentVersionNo: null,
      currentFunctions: [],
      currentContextProviders: [],
      allFunctions: [],
      originalFunctions: [],
      playingVoice: false,
      isPaused: false,
      currentAudio: null,
      currentPlayingVoiceId: null,
      // Language filter state
      languageOptions: [], // Language options
      selectedLanguage: '', // Currently selected language
      ttsLanguageTouched: false,
      ttsVoiceTouched: false,
      voiceFetchSeq: 0,
      voiceOptionsLoading: false,
      lastValidTtsDraft: null,
      agentReloading: false,
      agentReloadSeq: 0,
      agentConfigFetchSeq: 0,
      agentTagsFetchSeq: 0,
      currentVersionFetchSeq: 0,
      agentConfigLoaded: false,
      agentFunctionsLoaded: false,
      agentTagsLoaded: false,
      currentVersionLoaded: false,
      pluginMetadataReady: false,
      pluginMetadataLoading: null,
      // Feature status
      featureStatus: {
        vad: false, // Voice activity detection feature status
        asr: false, // Speech recognition feature status
      },
      dynamicTags: [],
      originalTagNames: [],
      inputVisible: false,
      inputValue: '',
      checkedReplacementWordIds: []
    };
  },
  computed: {
    configInteractionBlocked() {
      return this.agentReloading
        || this.voiceOptionsLoading
        || !this.agentConfigLoaded
        || !this.agentFunctionsLoaded
        || !this.agentTagsLoaded;
    },
    // An own TTS model without saved voices runs with its configured default voice
    ownedTtsWithoutVoices() {
      return !this.voiceOptionsLoading
        && this.voiceOptions.length === 0
        && this.isOwnedTtsModel(this.form.model.ttsModelId, this.modelOptions.TTS);
    }
  },
  methods: {
    goToHome() {
      this.$router.push("/home");
    },
    normalizeFunctionParams(params, fallback = {}) {
      if (params === null || params === undefined || params === '') {
        return { ...fallback };
      }
      if (typeof params === 'string') {
        try {
          const parsed = JSON.parse(params);
          return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
            ? parsed
            : { ...fallback };
        } catch (error) {
          return { ...fallback };
        }
      }
      if (typeof params === 'object' && !Array.isArray(params)) {
        return { ...params };
      }
      return { ...fallback };
    },
    async saveConfig() {
      if (this.configInteractionBlocked) {
        return;
      }
      const configData = {
        agentCode: this.form.agentCode,
        agentName: this.form.agentName,
        asrModelId: this.form.model.asrModelId,
        vadModelId: this.form.model.vadModelId,
        llmModelId: this.form.model.llmModelId,
        slmModelId: this.form.model.slmModelId,
        vllmModelId: this.form.model.vllmModelId,
        ttsModelId: this.form.model.ttsModelId,
        chatHistoryConf: this.form.chatHistoryConf,
        memModelId: this.form.model.memModelId,
        intentModelId: this.form.model.intentModelId,
        systemPrompt: this.form.systemPrompt,
        summaryMemory: this.form.summaryMemory,
        langCode: this.form.langCode,
        language: this.form.language,
        sort: this.form.sort,
        functions: this.currentFunctions.map((item) => {
          return {
            pluginId: item.id,
            paramInfo: this.normalizeFunctionParams(item.params),
          };
        }),
        contextProviders: this.currentContextProviders,
        correctWordFileIds: this.checkedReplacementWordIds,
      };
      const tagNames = this.dynamicTags.map(tag => tag.tagName);
      const tagsChanged = !this.isSameStringList(tagNames, this.originalTagNames);
      if (tagsChanged) {
        configData.tagNames = tagNames;
      }
      if (this.shouldSubmitTtsLanguage()) {
        configData.ttsLanguage = this.selectedLanguage;
      }
      if (this.ttsVoiceTouched && this.form.ttsVoiceId !== null && this.form.ttsVoiceId !== undefined) {
        configData.ttsVoiceId = this.form.ttsVoiceId;
      }
      const submittedTtsLanguageTouched = this.ttsLanguageTouched;
      const submittedTtsVoiceTouched = this.ttsVoiceTouched;
      const submittedTtsLanguage = configData.ttsLanguage;
      const submittedTtsVoiceId = configData.ttsVoiceId;
      const submittedVoiceFetchSeq = this.voiceFetchSeq;

      // Only send TTS params when the user set them (not null/undefined)
      if (this.form.ttsVolume !== null && this.form.ttsVolume !== undefined) {
        configData.ttsVolume = this.form.ttsVolume;
      }
      if (this.form.ttsRate !== null && this.form.ttsRate !== undefined) {
        configData.ttsRate = this.form.ttsRate;
      }
      if (this.form.ttsPitch !== null && this.form.ttsPitch !== undefined) {
        configData.ttsPitch = this.form.ttsPitch;
      }
      const agentId = this.$route.query.agentId;
      Api.agent.updateAgentConfig(agentId, configData, ({ data }) => {
        if (data.code === 0) {
          const afterSave = () => {
            if (tagsChanged) {
              this.originalTagNames = [...tagNames];
            }
            if (submittedVoiceFetchSeq === this.voiceFetchSeq
              && submittedTtsLanguageTouched
              && this.selectedLanguage === submittedTtsLanguage) {
              this.form.ttsLanguage = submittedTtsLanguage;
              this.ttsLanguageTouched = false;
            }
            if (submittedVoiceFetchSeq === this.voiceFetchSeq
              && submittedTtsVoiceTouched
              && this.form.ttsVoiceId === submittedTtsVoiceId) {
              this.ttsVoiceTouched = false;
            }
            if (submittedVoiceFetchSeq === this.voiceFetchSeq) {
              this.lastValidTtsDraft = this.captureTtsDraft();
            }
            this.$message.success({
              message: i18n.t("roleConfig.saveSuccess"),
              showClose: true,
            });
            this.fetchCurrentVersion(agentId);
          };
          afterSave();
        } else {
          this.$message.error({
            message: data.msg || i18n.t("roleConfig.saveFailed"),
            showClose: true,
          });
        }
      });
      
    },
    async reloadAgentPage(agentId, options = {}) {
      if (!agentId) {
        return false;
      }
      const requestSeq = ++this.agentReloadSeq;
      this.agentReloading = true;
      if (options.closeEditors) {
        this.showSnapshotDialog = false;
        this.showFunctionDialog = false;
        this.showContextProviderDialog = false;
        this.showTtsAdvancedDialog = false;
        this.inputVisible = false;
      }

      const results = await Promise.all([
        this.fetchAgentConfig(agentId, { showError: false }),
        this.getAgentTags(agentId, { showError: false }),
        this.fetchCurrentVersion(agentId, { showError: false })
      ]);
      if (requestSeq !== this.agentReloadSeq) {
        return false;
      }

      this.agentReloading = false;
      if (!this.pluginMetadataReady && this.agentConfigLoaded) {
        this.$message.error(i18n.t("roleConfig.fetchPluginsFailed"));
      } else if (!results.every(Boolean)) {
        this.$message.error(i18n.t("roleConfig.fetchConfigFailed"));
      }
      return results.every(Boolean);
    },
    handleSnapshotRestored() {
      const agentId = this.$route.query.agentId;
      if (agentId) {
        this.reloadAgentPage(agentId, { closeEditors: true });
      }
    },
    fetchCurrentVersion(agentId, options = {}) {
      const requestSeq = ++this.currentVersionFetchSeq;
      this.currentVersionLoaded = false;
      if (!agentId) {
        this.currentVersionNo = null;
        this.currentVersionLoaded = true;
        return Promise.resolve(true);
      }

      return new Promise((resolve) => {
        const handleFailure = (error) => {
          if (requestSeq !== this.currentVersionFetchSeq) {
            resolve(false);
            return;
          }
          this.currentVersionLoaded = false;
          if (options.showError !== false) {
            this.$message.error(error?.data?.msg || i18n.t("roleConfig.fetchConfigFailed"));
          }
          resolve(false);
        };
        Api.agent.getDeviceConfig(agentId, ({ data }) => {
          if (requestSeq !== this.currentVersionFetchSeq) {
            resolve(false);
            return;
          }
          if (data?.code === 0) {
            this.currentVersionNo = data.data?.currentVersionNo || null;
            this.currentVersionLoaded = true;
            resolve(true);
          } else {
            handleFailure(data);
          }
        }, handleFailure);
      });
    },
    resetConfig() {
      this.$confirm(i18n.t("roleConfig.confirmReset"), i18n.t("message.info"), {
        confirmButtonText: i18n.t("button.ok"),
        cancelButtonText: i18n.t("button.cancel"),
        type: "warning",
      })
        .then(() => {
          this.selectedLanguage = "";
          this.ttsLanguageTouched = true;
          this.ttsVoiceTouched = true;
          this.form = {
            agentCode: "",
            agentName: "",
            ttsVoiceId: "",
            ttsLanguage: "",
            chatHistoryConf: 0,
            systemPrompt: "",
            summaryMemory: "",
            langCode: "",
            language: "",
            sort: "",
            model: {
              ttsModelId: "",
              vadModelId: "",
              asrModelId: "",
              llmModelId: "",
              slmModelId: "",
              vllmModelId: "",
              memModelId: "",
              intentModelId: "",
            },
          };
          this.fetchVoiceOptions("");
          this.dynamicTags = [];
          this.currentFunctions = [];
          this.$message.success({
            message: i18n.t("roleConfig.resetSuccess"),
            showClose: true,
          });
        })
        .catch(() => {});
    },
    fetchTemplates() {
      Api.agent.getAgentTemplate(({ data }) => {
        if (data.code === 0) {
          this.templates = data.data;
        } else {
          this.$message.error(data.msg || i18n.t("roleConfig.fetchTemplatesFailed"));
        }
      });
    },
    selectTemplate(template) {
      if (this.loadingTemplate) return;
      this.loadingTemplate = true;
      try {
        this.applyTemplateData(template);
        this.$message.success({
          message: `${template.agentName}${i18n.t("roleConfig.templateApplied")}`,
          showClose: true,
        });
      } catch (error) {
        this.$message.error({
          message: i18n.t("roleConfig.applyTemplateFailed"),
          showClose: true,
        });
        console.error("Failed to apply template:", error);
      } finally {
        this.loadingTemplate = false;
      }
    },
    applyTemplateData(templateData) {
      const rollbackState = this.cloneTtsDraft(this.lastValidTtsDraft) || this.captureTtsDraft();
      const currentLanguage = this.selectedLanguage;
      this.form = {
        ...this.form,
        agentName: templateData.agentName || this.form.agentName,
        ttsVoiceId: templateData.ttsVoiceId || this.form.ttsVoiceId,
        chatHistoryConf: templateData.chatHistoryConf || this.form.chatHistoryConf,
        systemPrompt: templateData.systemPrompt || this.form.systemPrompt,
        summaryMemory: templateData.summaryMemory || this.form.summaryMemory,
        langCode: templateData.langCode || this.form.langCode,
        model: {
          ttsModelId: templateData.ttsModelId || this.form.model.ttsModelId,
          vadModelId: templateData.vadModelId || this.form.model.vadModelId,
          asrModelId: templateData.asrModelId || this.form.model.asrModelId,
          llmModelId: templateData.llmModelId || this.form.model.llmModelId,
          slmModelId: templateData.llmModelId || this.form.model.slmModelId,
          vllmModelId: templateData.vllmModelId || this.form.model.vllmModelId,
          memModelId: templateData.memModelId || this.form.model.memModelId,
          intentModelId: templateData.intentModelId || this.form.model.intentModelId,
        },
      };
      if (templateData.ttsLanguage) {
        this.selectedLanguage = templateData.ttsLanguage;
      }
      if (templateData.ttsModelId || templateData.ttsVoiceId || templateData.ttsLanguage) {
        this.fetchVoiceOptions(this.form.model.ttsModelId, {
          autoSelectVoice: true,
          preferredLanguage: templateData.ttsLanguage || (templateData.ttsVoiceId ? "" : currentLanguage),
          preferredVoiceId: templateData.ttsVoiceId || "",
          rollbackState,
          markTouched: true
        });
      }
    },
    buildCurrentFunctions(savedMappings) {
      if (!Array.isArray(savedMappings)) {
        throw new TypeError("Invalid agent function mappings");
      }
      return savedMappings.map((mapping) => {
        const pluginId = mapping.pluginId || mapping.id;
        const meta = this.pluginMetadataReady
          ? this.allFunctions.find((item) => item.id === pluginId)
          : null;
        return {
          id: pluginId,
          name: meta?.name || mapping.name || pluginId,
          params: this.normalizeFunctionParams(mapping.paramInfo ?? mapping.params, meta?.params || {}),
          fieldsMeta: meta?.fieldsMeta || []
        };
      }).filter((item) => item.id);
    },
    enrichCurrentFunctionsWithMetadata() {
      if (!this.agentFunctionsLoaded || !this.pluginMetadataReady) {
        return;
      }
      this.currentFunctions = this.currentFunctions.map((item) => {
        const meta = this.allFunctions.find((candidate) => candidate.id === item.id);
        if (!meta) {
          return {
            ...item,
            params: this.normalizeFunctionParams(item.params),
            fieldsMeta: item.fieldsMeta || []
          };
        }
        return {
          ...item,
          name: meta.name || item.name || item.id,
          params: this.normalizeFunctionParams(item.params, meta.params),
          fieldsMeta: meta.fieldsMeta || []
        };
      });
      this.originalFunctions = JSON.parse(JSON.stringify(this.currentFunctions));
    },
    fetchAgentConfig(agentId, options = {}) {
      const requestSeq = ++this.agentConfigFetchSeq;
      this.agentConfigLoaded = false;
      this.agentFunctionsLoaded = false;
      this.voiceFetchSeq += 1;
      this.voiceOptionsLoading = false;

      return new Promise((resolve) => {
        const handleFailure = (error) => {
          if (requestSeq !== this.agentConfigFetchSeq) {
            resolve(false);
            return;
          }
          this.agentConfigLoaded = false;
          this.agentFunctionsLoaded = false;
          if (options.showError !== false) {
            this.$message.error(error?.data?.msg || i18n.t("roleConfig.fetchConfigFailed"));
          }
          resolve(false);
        };

        Api.agent.getDeviceConfig(agentId, ({ data }) => {
          if (requestSeq !== this.agentConfigFetchSeq) {
            resolve(false);
            return;
          }
          if (data?.code !== 0 || !data.data) {
            handleFailure(data);
            return;
          }

          try {
            const agentData = data.data;
            if (agentData.functions != null && !Array.isArray(agentData.functions)) {
              throw new TypeError("Invalid agent function mappings");
            }
            if (agentData.contextProviders != null && !Array.isArray(agentData.contextProviders)) {
              throw new TypeError("Invalid context providers");
            }
            if (agentData.correctWordFileIds != null && !Array.isArray(agentData.correctWordFileIds)) {
              throw new TypeError("Invalid correct-word mappings");
            }
            this.tempSummaryMemory = "";
            this.ttsLanguageTouched = false;
            this.ttsVoiceTouched = false;
            this.form = {
              ...this.form,
              ...agentData,
              model: {
                ttsModelId: agentData.ttsModelId,
                vadModelId: agentData.vadModelId,
                asrModelId: agentData.asrModelId,
                llmModelId: agentData.llmModelId,
                slmModelId: agentData.slmModelId,
                vllmModelId: agentData.vllmModelId,
                memModelId: agentData.memModelId,
                intentModelId: agentData.intentModelId,
              },
            };
            this.selectedLanguage = agentData.ttsLanguage || "";
            this.voiceOptions = [];
            this.voiceDetails = {};
            this.languageOptions = [];
            this.lastValidTtsDraft = this.captureTtsDraft();
            this.fetchVoiceOptions(agentData.ttsModelId, {
              preferredLanguage: agentData.ttsLanguage,
              preferredVoiceId: agentData.ttsVoiceId
            });

            this.ttsSettings = {
              volume: this.form.ttsVolume || 0,
              speed: this.form.ttsRate || 0,
              pitch: this.form.ttsPitch || 0
            };
            this.checkedReplacementWordIds = agentData.correctWordFileIds || [];
            this.currentContextProviders = agentData.contextProviders || [];
            this.currentFunctions = this.buildCurrentFunctions(agentData.functions || []);
            this.originalFunctions = JSON.parse(JSON.stringify(this.currentFunctions));
            this.agentFunctionsLoaded = true;
            this.agentConfigLoaded = true;

            const metadataPromise = this.pluginMetadataReady
              ? Promise.resolve(true)
              : this.fetchAllFunctions({ showError: options.showError });
            metadataPromise.then((metadataReady) => {
              if (requestSeq === this.agentConfigFetchSeq && metadataReady) {
                this.enrichCurrentFunctionsWithMetadata();
                this.updateIntentOptionsVisibility();
              }
              resolve(true);
            }).catch(handleFailure);
          } catch (error) {
            handleFailure(error);
          }
        }, handleFailure);
      });
    },
    // The user's own models (bring your own key) are listed before system models; the sort is stable
    ownedModelsFirst(options) {
      return options.slice().sort((a, b) => Number(!!b.owned) - Number(!!a.owned));
    },
    fetchModelOptions() {
      this.models.forEach((model) => {
        if (model.type != "LLM") {
          Api.model.getModelNames(model.type, "", ({ data }) => {
            if (data.code === 0) {
              this.$set(
                this.modelOptions,
                model.type,
                this.ownedModelsFirst(
                  data.data.map((item) => ({
                    value: item.id,
                    label: item.modelName,
                    isHidden: false,
                    owned: !!item.owned,
                  }))
                )
              );

              // For intent options, update visibility based on the current LLM type
              if (model.type === "Intent") {
                this.updateIntentOptionsVisibility();
              }
            } else {
              this.$message.error(data.msg || i18n.t("roleConfig.fetchModelsFailed"));
            }
          });
        } else {
          Api.model.getLlmModelCodeList("", ({ data }) => {
            if (data.code === 0) {
              let LLMdata = [];
              data.data.forEach((item) => {
                LLMdata.push({
                  value: item.id,
                  label: item.modelName,
                  isHidden: false,
                  owned: !!item.owned,
                });
                this.llmModeTypeMap.set(item.id, item.type);
              });
              this.$set(this.modelOptions, model.type, this.ownedModelsFirst(LLMdata));
            } else {
              this.$message.error(data.msg || i18n.t("roleConfig.fetchModelsFailed"));
            }
          });
        }
      });
    },
    fetchVoiceOptions(modelId, options = {}) {
      const requestSeq = ++this.voiceFetchSeq;
      if (!modelId) {
        this.voiceOptionsLoading = false;
        this.voiceOptions = [];
        this.voiceDetails = {};
        this.languageOptions = [];
        this.selectedLanguage = '';
        this.lastValidTtsDraft = this.captureTtsDraft();
        return;
      }
      this.voiceOptionsLoading = true;
      Api.model.getModelVoices(modelId, "", ({ data }) => {
        if (requestSeq !== this.voiceFetchSeq) {
          return;
        }
        if (data.code === 0 && !(Array.isArray(data.data) && data.data.length)) {
          this.acceptOwnedTtsWithoutVoices(requestSeq, modelId, options);
          return;
        }
        const draft = data.code === 0
          ? this.buildTtsDraft(modelId, data.data, options)
          : null;
        if (!draft) {
          this.handleVoiceOptionsFailure(requestSeq, options.rollbackState);
          return;
        }
        this.applyTtsDraft(draft, options);
        this.voiceOptionsLoading = false;
        this.lastValidTtsDraft = this.captureTtsDraft();
      }, () => {
        this.handleVoiceOptionsFailure(requestSeq, options.rollbackState);
      });
    },
    isOwnedTtsModel(modelId, ttsOptions) {
      return !!modelId && (ttsOptions || []).some((item) => item.value === modelId && item.owned);
    },
    // Own TTS models may have no voices: keep the model and clear the voice so its default voice is used.
    // System models keep the old behaviour (an empty voice list is an error).
    acceptOwnedTtsWithoutVoices(requestSeq, modelId, options) {
      const decide = (ttsOptions) => {
        if (requestSeq !== this.voiceFetchSeq) {
          return;
        }
        if (!this.isOwnedTtsModel(modelId, ttsOptions)) {
          this.handleVoiceOptionsFailure(requestSeq, options.rollbackState);
          return;
        }
        this.applyTtsDraft({
          modelId, language: "", voiceId: "", voiceDetails: {}, languageOptions: [], voiceOptions: []
        }, options);
        this.voiceOptionsLoading = false;
        this.lastValidTtsDraft = this.captureTtsDraft();
      };
      if (this.modelOptions.TTS) {
        decide(this.modelOptions.TTS);
        return;
      }
      // Model names are still loading; decide once they arrive
      const unwatch = this.$watch(() => this.modelOptions.TTS, (ttsOptions) => {
        if (ttsOptions) {
          unwatch();
          decide(ttsOptions);
        }
      });
    },
    cloneTtsDraft(draft) {
      return draft ? JSON.parse(JSON.stringify(draft)) : null;
    },
    captureTtsDraft() {
      return {
        modelId: this.form.model.ttsModelId,
        language: this.selectedLanguage,
        storedLanguage: this.form.ttsLanguage,
        voiceId: this.form.ttsVoiceId,
        languageTouched: this.ttsLanguageTouched,
        voiceTouched: this.ttsVoiceTouched,
        voiceOptions: this.cloneTtsDraft(this.voiceOptions) || [],
        voiceDetails: this.cloneTtsDraft(this.voiceDetails) || {},
        languageOptions: this.cloneTtsDraft(this.languageOptions) || []
      };
    },
    restoreTtsDraft(draft) {
      const restored = this.cloneTtsDraft(draft);
      if (!restored) {
        return false;
      }
      this.form.model.ttsModelId = restored.modelId;
      this.selectedLanguage = restored.language;
      this.form.ttsLanguage = restored.storedLanguage;
      this.form.ttsVoiceId = restored.voiceId;
      this.ttsLanguageTouched = restored.languageTouched;
      this.ttsVoiceTouched = restored.voiceTouched;
      this.voiceOptions = restored.voiceOptions;
      this.voiceDetails = restored.voiceDetails;
      this.languageOptions = restored.languageOptions;
      this.lastValidTtsDraft = restored;
      return true;
    },
    splitVoiceLanguages(voice) {
      return voice && voice.languages
        ? voice.languages.split(/[、；;,，]/).map(lang => lang.trim()).filter(Boolean)
        : [];
    },
    buildTtsDraft(modelId, voices, options = {}) {
      if (!Array.isArray(voices) || voices.length === 0) {
        return null;
      }
      const voiceDetails = voices.reduce((result, voice) => {
        if (voice && voice.id) {
          result[voice.id] = voice;
        }
        return result;
      }, {});
      const validVoices = Object.values(voiceDetails);
      if (validVoices.length === 0) {
        return null;
      }

      const allLanguages = new Set();
      validVoices.forEach((voice) => {
        this.splitVoiceLanguages(voice).forEach((language) => allLanguages.add(language));
      });
      const languageRank = (language) => (/english/i.test(language) ? 0 : /arabic/i.test(language) ? 1 : 2);
      const languageOptions = Array.from(allLanguages)
        .map((language, index) => ({ language, index }))
        .sort((a, b) => languageRank(a.language) - languageRank(b.language) || a.index - b.index)
        .map(({ language }) => ({
          value: language,
          label: language
        }));
      const languageExists = (language) => language
        && languageOptions.some((option) => option.value === language);
      const preferredVoiceId = options.preferredVoiceId || this.form.ttsVoiceId;
      const preferredVoiceLanguage = this.splitVoiceLanguages(voiceDetails[preferredVoiceId])[0] || "";
      const languageCandidates = [
        options.preferredLanguage,
        preferredVoiceLanguage,
        this.form.ttsLanguage,
        this.selectedLanguage,
        languageOptions[0]?.value
      ];
      const preferredVoiceHasNoLanguage = Boolean(
        voiceDetails[preferredVoiceId]
        && this.splitVoiceLanguages(voiceDetails[preferredVoiceId]).length === 0
      );
      let language = options.preferredLanguage === "" && preferredVoiceHasNoLanguage
        ? ""
        : languageCandidates.find(languageExists) || "";
      const filterVoices = (targetLanguage) => validVoices.filter((voice) => {
        const languages = this.splitVoiceLanguages(voice);
        return languages.length === 0 || languages.includes(targetLanguage);
      });
      let filteredVoices = filterVoices(language);
      if (filteredVoices.length === 0) {
        const fallbackVoice = validVoices.find((voice) => this.splitVoiceLanguages(voice).length > 0);
        if (fallbackVoice) {
          language = this.splitVoiceLanguages(fallbackVoice)[0];
          filteredVoices = filterVoices(language);
        } else {
          filteredVoices = validVoices;
        }
      }
      if (filteredVoices.length === 0) {
        return null;
      }

      const preferredVoice = filteredVoices.find((voice) => voice.id === preferredVoiceId);
      const voice = preferredVoice || (options.autoSelectVoice ? filteredVoices[0] : null);
      return {
        modelId,
        language,
        voiceId: voice?.id || preferredVoiceId || "",
        voiceDetails,
        languageOptions,
        voiceOptions: filteredVoices.map((item) => ({
          value: item.id,
          label: item.name,
          voiceDemo: item.voiceDemo,
          voice_demo: item.voice_demo,
          isClone: Boolean(item.isClone),
          train_status: item.trainStatus
        }))
      };
    },
    applyTtsDraft(draft, options = {}) {
      this.form.model.ttsModelId = draft.modelId;
      this.voiceDetails = draft.voiceDetails;
      this.languageOptions = draft.languageOptions;
      this.voiceOptions = draft.voiceOptions;
      this.selectedLanguage = draft.language;
      this.form.ttsLanguage = draft.language;
      this.form.ttsVoiceId = draft.voiceId;
      if (options.markTouched) {
        this.ttsLanguageTouched = true;
        this.ttsVoiceTouched = true;
      }
      this.ttsSettings = {
        volume: this.form.ttsVolume !== null && this.form.ttsVolume !== undefined ? this.form.ttsVolume : 0,
        speed: this.form.ttsRate !== null && this.form.ttsRate !== undefined ? this.form.ttsRate : 0,
        pitch: this.form.ttsPitch !== null && this.form.ttsPitch !== undefined ? this.form.ttsPitch : 0
      };
    },
    handleVoiceOptionsFailure(requestSeq, rollbackState) {
      if (requestSeq !== this.voiceFetchSeq) {
        return;
      }
      this.voiceOptionsLoading = false;
      if (!this.restoreTtsDraft(rollbackState)) {
        this.voiceOptions = [];
        this.voiceDetails = {};
        this.languageOptions = [];
      }
      this.$message.error(i18n.t("ttsModel.fetchVoicesFailed"));
    },
    getVoiceDefaultLanguage(voiceId) {
      if (!voiceId || !this.voiceDetails || !this.voiceDetails[voiceId]?.languages) {
        return "";
      }
      const languages = this.voiceDetails[voiceId].languages
        .split(/[、；;,，]/)
        .map(lang => lang.trim())
        .filter(Boolean);
      return languages[0] || "";
    },
    
    // Filter voices by language
    filterVoicesByLanguage(options = {}) {
      if (!this.voiceDetails || Object.keys(this.voiceDetails).length === 0) {
        this.voiceOptions = [];
        return;
      }

      const allVoices = Object.values(this.voiceDetails);

      // Filter voices by the selected language
      const filteredVoices = allVoices.filter(voice => {
        const languagesArray = this.splitVoiceLanguages(voice);
        if (languagesArray.length === 0) {
          // Valid voices without declared languages are interpreted by the provider; don't filter them on the frontend.
          return true;
        }
        return languagesArray.includes(this.selectedLanguage);
      });

      this.voiceOptions = filteredVoices.map((voice) => ({
        value: voice.id,
        label: voice.name,
        voiceDemo: voice.voiceDemo,
        voice_demo: voice.voice_demo,
        isClone: Boolean(voice.isClone),
        train_status: voice.trainStatus,
      }));

      // Check whether the selected voice supports the current language; if not, select the first one
      const currentVoiceSupportsLanguage = this.form.ttsVoiceId &&
        filteredVoices.some(voice => voice.id === this.form.ttsVoiceId);

      if (!currentVoiceSupportsLanguage && options.autoSelectVoice) {
        this.form.ttsVoiceId = filteredVoices.length > 0 ? filteredVoices[0].id : '';
        this.ttsVoiceTouched = true;
      }

      // Sync to ttsSettings (use 0 as display default when null, without modifying the form value)
      this.ttsSettings = {
        volume: this.form.ttsVolume !== null && this.form.ttsVolume !== undefined ? this.form.ttsVolume : 0,
        speed: this.form.ttsRate !== null && this.form.ttsRate !== undefined ? this.form.ttsRate : 0,
        pitch: this.form.ttsPitch !== null && this.form.ttsPitch !== undefined ? this.form.ttsPitch : 0
      };
    },
    handleLanguageChange() {
      this.ttsLanguageTouched = true;
      this.form.ttsLanguage = this.selectedLanguage;
      this.filterVoicesByLanguage({ autoSelectVoice: true });
      if (this.form.ttsVoiceId) {
        this.lastValidTtsDraft = this.captureTtsDraft();
      }
    },
    handleVoiceChange() {
      this.ttsVoiceTouched = true;
      if (this.selectedLanguage) {
        this.form.ttsLanguage = this.selectedLanguage;
        this.ttsLanguageTouched = true;
      }
      if (this.form.ttsVoiceId) {
        this.lastValidTtsDraft = this.captureTtsDraft();
      }
    },
    shouldSubmitTtsLanguage() {
      return this.ttsLanguageTouched;
    },

    getFunctionDisplayChar(name) {
      if (!name || name.length === 0) return "";

      for (let i = 0; i < name.length; i++) {
        const char = name[i];
        if (/[\u4e00-\u9fa5a-zA-Z0-9]/.test(char)) {
          return char;
        }
      }

      // If no valid character is found, return the first character
      return name.charAt(0);
    },
    showFunctionIcons(type) {
      return type === "Intent" && this.form.model.intentModelId !== "Intent_nointent";
    },
    handleModelChange(type, value) {
      if (type === "Intent" && value !== "Intent_nointent") {
        this.fetchAllFunctions().then((metadataReady) => {
          if (metadataReady) {
            this.enrichCurrentFunctionsWithMetadata();
          }
        });
      }
      if (type === "Memory") {
        if (value === "Memory_nomem") {
          // Models without memory don't record chat history by default
          this.form.chatHistoryConf = 0;
        } else {
          // Models with memory record text and audio by default
          this.form.chatHistoryConf = 2;
        }
        if (value === "Memory_nomem" || value === "Memory_mem_report_only") {
          this.tempSummaryMemory = this.form.summaryMemory;
          this.form.summaryMemory = "";
        } else if (this.tempSummaryMemory !== "" && this.form.summaryMemory === "") {
          this.form.summaryMemory = this.tempSummaryMemory;
          this.tempSummaryMemory = "";
        }
      }
      if (type === "LLM") {
        // Update intent option visibility when the LLM type changes
        this.updateIntentOptionsVisibility();
      }
      if (type === "TTS") {
        const rollbackState = this.cloneTtsDraft(this.lastValidTtsDraft);
        this.fetchVoiceOptions(value, {
          autoSelectVoice: true,
          preferredLanguage: rollbackState?.language || this.selectedLanguage,
          rollbackState,
          markTouched: true
        });
      }
    },
    parsePluginFields(fields) {
      if (Array.isArray(fields)) {
        return fields;
      }
      if (typeof fields !== "string" || !fields.trim()) {
        return [];
      }
      try {
        const parsed = JSON.parse(fields);
        return Array.isArray(parsed) ? parsed : [];
      } catch (error) {
        return [];
      }
    },
    fetchAllFunctions(options = {}) {
      if (this.pluginMetadataReady) {
        return Promise.resolve(true);
      }
      if (this.pluginMetadataLoading) {
        return this.pluginMetadataLoading;
      }

      this.pluginMetadataLoading = new Promise((resolve) => {
        let settled = false;
        const finish = (ready, error) => {
          if (settled) {
            return;
          }
          settled = true;
          this.pluginMetadataReady = ready;
          if (!ready && options.showError !== false) {
            this.$message.error(error?.data?.msg || error?.msg || i18n.t("roleConfig.fetchPluginsFailed"));
          }
          resolve(ready);
        };

        Api.model.getPluginFunctionList(null, ({ data }) => {
          if (data?.code !== 0) {
            finish(false, data);
            return;
          }
          try {
            this.allFunctions = (data.data || []).map((item) => {
              const fieldsMeta = this.parsePluginFields(item.fields);
              const params = fieldsMeta.reduce((result, field) => {
                if (field?.key) {
                  result[field.key] = field.default;
                }
                return result;
              }, {});
              return { ...item, fieldsMeta, params };
            });
            finish(true);
          } catch (error) {
            finish(false, error);
          }
        }, (error) => finish(false, error));
      }).finally(() => {
        this.pluginMetadataLoading = null;
      });

      return this.pluginMetadataLoading;
    },
    openFunctionDialog() {
      if (this.agentReloading || !this.agentFunctionsLoaded) {
        return;
      }
      if (this.pluginMetadataReady) {
        this.enrichCurrentFunctionsWithMetadata();
        this.showFunctionDialog = true;
        return;
      }
      this.fetchAllFunctions().then((metadataReady) => {
        if (metadataReady) {
          this.enrichCurrentFunctionsWithMetadata();
          this.showFunctionDialog = true;
        }
      });
    },
    openContextProviderDialog() {
      this.showContextProviderDialog = true;
    },
    openTtsAdvancedSettings() {
      this.showTtsAdvancedDialog = true;
    },
    handleTtsSettingsSave(settings) {
      const { replacementWordIds, changedTtsFields = [], ...ttsSettings } = settings;
      this.checkedReplacementWordIds = replacementWordIds;
      // Save TTS settings
      this.ttsSettings = ttsSettings;
      const changedFields = new Set(changedTtsFields);
      if (changedFields.has("volume")) {
        this.form.ttsVolume = ttsSettings.volume;
      }
      if (changedFields.has("speed")) {
        this.form.ttsRate = ttsSettings.speed;
      }
      if (changedFields.has("pitch")) {
        this.form.ttsPitch = ttsSettings.pitch;
      }
    },
    handleUpdateContext(providers) {
      this.currentContextProviders = providers;
    },
    handleUpdateFunctions(selected) {
      this.currentFunctions = selected;
    },
    handleDialogClosed(saved) {
      if (!saved) {
        this.currentFunctions = JSON.parse(JSON.stringify(this.originalFunctions));
      } else {
        this.originalFunctions = JSON.parse(JSON.stringify(this.currentFunctions));
      }
      this.showFunctionDialog = false;
    },
    updateIntentOptionsVisibility() {
      // Update intent option visibility based on the selected LLM type
      const currentLlmId = this.form.model.llmModelId;
      if (!currentLlmId || !this.modelOptions["Intent"]) return;

      const llmType = this.llmModeTypeMap.get(currentLlmId);
      if (!llmType) return;

      this.modelOptions["Intent"].forEach((item) => {
        if (item.value === "Intent_function_call") {
          // If llmType is openai or ollama, allow function_call
          // Otherwise hide the function_call option
          if (llmType === "openai" || llmType === "ollama") {
            item.isHidden = false;
          } else {
            item.isHidden = true;
          }
        } else {
          // Other intent options are always visible
          item.isHidden = false;
        }
      });

      // If function_call is selected but unsupported by the LLM type, fall back to the first available option
      if (
        this.form.model.intentModelId === "Intent_function_call" &&
        llmType !== "openai" &&
        llmType !== "ollama"
      ) {
        // Find the first visible option
        const firstVisibleOption = this.modelOptions["Intent"].find(
          (item) => !item.isHidden
        );
        if (firstVisibleOption) {
          this.form.model.intentModelId = firstVisibleOption.value;
        } else {
          // If no visible option, set to Intent_nointent
          this.form.model.intentModelId = "Intent_nointent";
        }
      }
    },
    // Check whether an audio preview exists
    hasAudioPreview(item) {
      // Check whether it is cloned audio
      // Use the isClone field returned by the backend
      const isCloneAudio = Boolean(item.isClone);
      
      // Check for a valid audio URL, using only fields returned by the backend
      const hasValidAudioUrl = !!((item.voice_demo || item.voiceDemo)?.trim());
      
      // Cloned audio always shows the play button; regular audio needs a valid URL
      return isCloneAudio || hasValidAudioUrl;
    },

    // Toggle audio play/pause
    toggleAudioPlayback(voiceId) {
      // If clicking the currently playing audio, toggle pause/play
      if (this.playingVoice && this.currentPlayingVoiceId === voiceId) {
        if (this.isPaused) {
          // Resume from pause
          this.currentAudio.play().catch((error) => {
            console.error("Failed to resume playback:", error);
            this.$message.warning(this.$t('roleConfig.cannotResumeAudio'));
          });
          this.isPaused = false;
        } else {
          // Pause playback
          this.currentAudio.pause();
          this.isPaused = true;
        }
        return;
      }

      // Otherwise start playing the new audio
      this.playVoicePreview(voiceId);
    },

    // Play voice preview
    playVoicePreview(voiceId = null) {
      // Use the given voiceId if provided, otherwise the currently selected one
      const targetVoiceId = voiceId || this.form.ttsVoiceId;

      if (!targetVoiceId) {
        this.$message.warning(this.$t('roleConfig.selectVoiceFirst'));
        return;
      }

      // Stop the currently playing audio
      if (this.currentAudio) {
        this.currentAudio.pause();
        this.currentAudio = null;
      }

      // Reset playback state
      this.isPaused = false;
      this.currentPlayingVoiceId = targetVoiceId;

      try {
        // Get the audio URL from saved voice details
        const voiceDetail = this.voiceDetails[targetVoiceId];

        // Debug info
        console.log("Selected voice ID:", targetVoiceId);
        console.log("Voice details:", voiceDetail);

        // Try several possible audio property names
        let audioUrl = null;
        let isCloneAudio = false;

        if (voiceDetail) {
          // Use the isClone field returned by the backend to detect cloned audio
          isCloneAudio = Boolean(voiceDetail.isClone);
          console.log(
            "Is cloned audio:",
            isCloneAudio,
            "Training status:",
            voiceDetail.train_status
          );

          // Get audio URL
          if (isCloneAudio && voiceDetail.id) {
            // For cloned audio, use the dedicated backend API
            // Note: getting the audio URL takes two steps
            // 1. First get the audio download ID
            // 2. Then build the playback URL from that ID
            // Since it's async, request getAudioId first
            console.log("Cloned audio detected, fetching audio URL:", voiceDetail.id);

            // Create a Promise to fetch the audio URL asynchronously
            const getCloneAudioUrl = () => {
              return new Promise((resolve) => {
                // First call getAudioId to get a temporary UUID
                RequestService.sendRequest()
                  .url(`${getServiceUrl()}/voiceClone/audio/${voiceDetail.id}`)
                  .method("POST")
                  .success((res) => {
                    if (res.data.code === 0 && res.data.data) {
                      // Handle the response format: data is nested one level deeper under res.data
                      const audioId = res.data.data;
                      console.log("Got audio ID:", audioId);
                      // Build the playback URL from the returned UUID
                      const playUrl = `${getServiceUrl()}/voiceClone/play/${audioId}`;
                      console.log("Built cloned audio playback URL:", playUrl);
                      resolve(playUrl);
                    } else {
                      console.error("Failed to get audio ID:", res.msg);
                      resolve(null);
                    }
                  })
                  .networkFail((err) => {
                    console.error("Audio ID request failed:", err);
                    resolve(null);
                  })
                  .send();
              });
            };

            // Set playback state
            this.playingVoice = true;
            // Create Audio instance
            this.currentAudio = new Audio();
            // Set volume
            this.currentAudio.volume = 1.0;

            // Set a timeout to avoid loading too long
            const timeoutId = setTimeout(() => {
              if (this.currentAudio && this.playingVoice) {
                this.$message.warning(this.$t('roleConfig.audioLoadTimeout'));
                this.playingVoice = false;
              }
            }, 10000); // 10-second timeout

            // Listen for playback errors
            this.currentAudio.onerror = () => {
              clearTimeout(timeoutId);
              console.error("Cloned audio playback error");
              this.$message.warning(this.$t('roleConfig.cloneAudioPlayFailed'));
              this.playingVoice = false;
            };

            // Clear the timeout when playback starts
            this.currentAudio.onplay = () => {
              clearTimeout(timeoutId);
            };

            // Listen for playback end
            this.currentAudio.onended = () => {
              this.playingVoice = false;
            };

            // Handle async URL fetch and play
            getCloneAudioUrl().then((url) => {
              if (url) {
                // Set audio URL and play
                this.currentAudio.src = url;
                this.currentAudio.play().catch((error) => {
                  clearTimeout(timeoutId);
                  console.error("Failed to play cloned audio:", error);
                  this.$message.warning(this.$t('roleConfig.cannotPlayCloneAudio'));
                  this.playingVoice = false;
                });
              } else {
                clearTimeout(timeoutId);
                this.$message.warning(this.$t('roleConfig.getCloneAudioFailed'));
                this.playingVoice = false;
              }
            });

            // Return to skip the regular audio playback logic below
            return;
          } else {
            // For regular audio, only use fields returned by the backend
            audioUrl =
              voiceDetail.voiceDemo ||
              voiceDetail.voice_demo;
          }

          // If not found, look for fields that look like URLs
          if (!audioUrl) {
            for (const key in voiceDetail) {
              const value = voiceDetail[key];
              if (
                typeof value === "string" &&
                (value.startsWith("http://") ||
                  value.startsWith("https://") ||
                  value.endsWith(".mp3") ||
                  value.endsWith(".wav") ||
                  value.endsWith(".ogg"))
              ) {
                audioUrl = value;
                console.log(`Possible audio URL found in field '${key}':`, audioUrl);
                break;
              }
            }
          }
        }

        if (!audioUrl) {
          // If no audio URL, show a friendly message
          this.$message.warning(this.$t('roleConfig.noPreviewAudio'));
          return;
        }

        // Handling for non-cloned audio
        if (!isCloneAudio) {
          // Set playback state
          this.playingVoice = true;

          // Create and play audio
          this.currentAudio = new Audio();
          this.currentAudio.src = audioUrl;

          // Set volume
          this.currentAudio.volume = 1.0;

          // Set a timeout to avoid loading too long
          const timeoutId = setTimeout(() => {
            if (this.currentAudio && this.playingVoice) {
              this.$message.warning(this.$t('roleConfig.audioLoadTimeout'));
              this.playingVoice = false;
            }
          }, 10000); // 10-second timeout

          // Listen for playback errors
          this.currentAudio.onerror = () => {
            clearTimeout(timeoutId);
            console.error("Audio playback error");
            this.$message.warning(this.$t('roleConfig.audioPlayFailed'));
            this.playingVoice = false;
          };

          // Clear the timeout when playback starts
          this.currentAudio.onplay = () => {
            clearTimeout(timeoutId);
          };

          // Listen for playback end
          this.currentAudio.onended = () => {
            this.playingVoice = false;
          };

          // Start audio playback
          this.currentAudio.play().catch((error) => {
            clearTimeout(timeoutId);
            console.error("Playback failed:", error);
            this.$message.warning(this.$t('roleConfig.cannotPlayAudio'));
            this.playingVoice = false;
          });
        }
      } catch (error) {
        console.error("Error while playing audio:", error);
        this.$message.error(this.$t('roleConfig.audioPlayError'));
        this.playingVoice = false;
      }
    },
    updateChatHistoryConf() {
      if (this.form.model.memModelId === "Memory_nomem") {
        this.form.chatHistoryConf = 0;
      }
    },
    // Load feature status
    async loadFeatureStatus() {
      try {
        // Make sure featureManager is initialized
        await featureManager.waitForInitialization();
        const config = featureManager.getConfig();
        this.featureStatus.voiceprintRecognition = config.voiceprintRecognition || false;
        this.featureStatus.vad = config.vad || false;
        this.featureStatus.asr = config.asr || false;
      } catch (error) {
        console.error("Failed to load feature status:", error);
      }
    },
    handleClose(id) {
      this.dynamicTags = this.dynamicTags.filter((item) => item.id !== id);
    },

    showInput() {
      this.inputVisible = true;
      this.$nextTick(_ => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },

    handleInputConfirm() {
      let inputValue = this.inputValue;
      if (inputValue) {
        const tag = { id: `tmp-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, tagName: inputValue };
        this.dynamicTags.push(tag);
      }
      this.inputVisible = false;
      this.inputValue = '';
    },
    getAgentTags(agentId, options = {}) {
      const requestSeq = ++this.agentTagsFetchSeq;
      this.agentTagsLoaded = false;
      if (!agentId) {
        this.dynamicTags = [];
        this.originalTagNames = [];
        this.agentTagsLoaded = true;
        return Promise.resolve(true);
      }

      return new Promise((resolve) => {
        const handleFailure = (error) => {
          if (requestSeq !== this.agentTagsFetchSeq) {
            resolve(false);
            return;
          }
          this.agentTagsLoaded = false;
          if (options.showError !== false) {
            this.$message.error(error?.data?.msg || i18n.t("roleConfig.fetchConfigFailed"));
          }
          resolve(false);
        };
        Api.agent.getAgentTags(agentId, ({ data }) => {
          if (requestSeq !== this.agentTagsFetchSeq) {
            resolve(false);
            return;
          }
          if (data?.code === 0) {
            try {
              this.dynamicTags = Array.isArray(data.data) ? data.data : [];
              this.originalTagNames = this.dynamicTags.map(tag => tag.tagName);
              this.agentTagsLoaded = true;
              resolve(true);
            } catch (error) {
              handleFailure(error);
            }
          } else {
            handleFailure(data);
          }
        }, handleFailure);
      });
    },
    isSameStringList(left, right) {
      if (!Array.isArray(left) || !Array.isArray(right) || left.length !== right.length) {
        return false;
      }
      return left.every((value, index) => value === right[index]);
    },
    handleSaveAgentTags(agentId, tagNames = this.dynamicTags.map(tag => tag.tagName)) {
      return new Promise((resolve, reject) => {
        Api.agent.saveAgentTags(agentId, { tagNames }, ({ data }) => {
          if (data.code === 0) {
            this.originalTagNames = [...tagNames];
            resolve();
          } else {
            reject(data.msg);
          }
        });
      });
    }
  },
  beforeDestroy() {
    this.agentReloadSeq += 1;
    this.agentConfigFetchSeq += 1;
    this.agentTagsFetchSeq += 1;
    this.currentVersionFetchSeq += 1;
    this.voiceFetchSeq += 1;
  },
  async mounted() {
    this.lastValidTtsDraft = this.captureTtsDraft();
    const agentId = this.$route.query.agentId;
    if (agentId) {
      this.reloadAgentPage(agentId);
    }
    this.fetchModelOptions();
    this.fetchTemplates();
    // Load feature status, ensuring featureManager is initialized
    await this.loadFeatureStatus();
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

.role-page {
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
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.page-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: var(--x-text-muted);
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

.current-version-tag {
  font-size: 12px;
  color: var(--x-text-muted);
  font-variant-numeric: tabular-nums;
  margin-inline-end: 4px;
}

// Tags
.header-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: -8px;
}

.custom-tag {
  height: 26px;
  line-height: 24px;
  padding: 0 10px;
  border-radius: var(--x-radius-pill);
  border: 1px solid var(--x-border);
  background: var(--x-surface);
  color: var(--x-text-secondary);
  font-size: 12px;

  ::v-deep .el-tag__close {
    color: var(--x-text-muted);

    &:hover {
      background: var(--x-selected);
      color: var(--x-ink);
    }
  }
}

.custom-tag-btn {
  height: 26px;
  padding: 0 10px;
  border-style: dashed;
  font-size: 12px;
  color: var(--x-text-muted);
}

.input-new-tag {
  width: 120px;
}

// Cards
.role-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

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

.card-head--actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.card-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
}

.card-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.card-body {
  padding: 20px;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 20px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px 20px;
}

.span-all {
  grid-column: 1 / -1;
}

.model-item--wide {
  grid-column: span 2;
}

// Form items: label on top, no extra bottom margin (grid gap handles spacing)
.role-form ::v-deep .el-form-item {
  margin-bottom: 0;
  min-width: 0;
}

.role-form ::v-deep .el-form-item__label {
  float: none;
  display: block;
  padding: 0 0 6px;
  line-height: 20px;
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text);
  text-align: start;
}

.role-form ::v-deep .el-form-item__content {
  line-height: normal;
}

.model-code {
  font-weight: 400;
  color: var(--x-text-muted);
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
}

.model-select-wrapper {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;

  .form-select {
    flex: 1 1 200px;
    width: auto;
  }
}

.chat-history-options {
  display: flex;
  flex: 0 0 auto;
}

// Theme the radio group as an ink segmented control
.chat-history-options ::v-deep .el-radio-button__inner {
  color: var(--x-text-secondary);
  border-color: var(--x-border-control);
  background: var(--x-surface);
  box-shadow: none;

  &:hover {
    color: var(--x-ink);
  }
}

.chat-history-options ::v-deep .el-radio-button:first-child .el-radio-button__inner {
  border-inline-start-color: var(--x-border-control);
}

.chat-history-options ::v-deep .el-radio-button.is-active .el-radio-button__inner {
  background: var(--x-ink);
  border-color: var(--x-ink);
  color: var(--x-on-ink);
  box-shadow: -1px 0 0 0 var(--x-ink);
}

// Textarea counter
.form-textarea ::v-deep .el-textarea__inner {
  line-height: 1.55;
  padding: 10px 12px 26px;
}

.form-textarea ::v-deep .el-input__count {
  color: var(--x-text-muted);
  background: none;
  font-size: 12px;
  right: 12px;
  bottom: 6px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

// Template pills
.template-container {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.template-item {
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--x-border-control);
  border-radius: var(--x-radius-control);
  background: var(--x-surface);
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover {
    background: var(--x-selected);
    border-color: var(--x-ink);
    color: var(--x-text);
  }

  &:focus-visible {
    outline: none;
    border-color: var(--x-ink);
    box-shadow: 0 0 0 4px var(--x-focus-ring);
  }

  &.template-loading {
    cursor: progress;
    opacity: 0.6;
  }
}

// Voice
.voice-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.play-button {
  color: var(--x-text-secondary);

  &:hover,
  &:focus {
    color: var(--x-ink);
  }
}

.tts-params {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 20px;
  padding-top: 4px;
}

.tts-param {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.tts-param-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.tts-param-label {
  font-weight: 500;
  color: var(--x-text);
}

.tts-param-value {
  color: var(--x-text-secondary);
  font-variant-numeric: tabular-nums;
}

.tts-param-track {
  position: relative;
  height: 4px;
  margin: 4px 0;
  border-radius: var(--x-radius-pill);
  background: var(--x-border);
}

.tts-param-fill {
  position: absolute;
  top: 0;
  bottom: 0;
  border-radius: var(--x-radius-pill);
  background: var(--x-ink);
}

.tts-param-thumb {
  position: absolute;
  top: 50%;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--x-surface);
  border: 2px solid var(--x-ink);
  box-sizing: border-box;
  transform: translate(-50%, -50%);
}

.tts-param-hint {
  font-size: 12px;
  line-height: 1.4;
  color: var(--x-text-muted);
}

// Functions
.function-icons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.function-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 30px;
  padding: 0 12px 0 4px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-pill);
  background: var(--x-surface-raised);
  font-size: 13px;
  color: var(--x-text-secondary);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.icon-dot {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--x-ink);
  color: var(--x-on-ink);
  font-weight: 600;
  font-size: 11px;
}

.edit-function-btn.active-btn {
  background: var(--x-ink);
  border-color: var(--x-ink);
  color: var(--x-on-ink);
}

// Context
.context-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--x-text-secondary);
}

.doc-link {
  color: var(--x-ink);
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 2px;
  margin-inline-start: 4px;

  &:hover {
    color: var(--x-ink-pressed);
  }
}

@media (max-width: 900px) {
  .tts-params {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .role-page {
    padding: 16px;
    gap: 16px;
  }

  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
  }

  .model-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .model-item--wide {
    grid-column: auto;
  }

  .card-head,
  .card-body {
    padding-inline-start: 16px;
    padding-inline-end: 16px;
  }
}

// Hint under the voice select for own TTS models without voices
.owned-voice-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--x-text-muted);
  text-align: start;

  a {
    color: var(--x-text);
    font-weight: 500;
    text-decoration: underline;
    text-underline-offset: 2px;
  }
}

// "Mine" tag on the user's own models in the model dropdowns
.owned-model-tag {
  display: inline-flex;
  align-items: center;
  height: 18px;
  margin-inline-start: 8px;
  padding: 0 6px;
  border: 1px solid var(--x-border-control);
  border-radius: var(--x-radius-pill);
  color: var(--x-text-muted);
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  vertical-align: middle;
}
</style>

<style>
.custom-tooltip {
  max-width: 400px !important;
  word-break: break-word;
}
</style>
