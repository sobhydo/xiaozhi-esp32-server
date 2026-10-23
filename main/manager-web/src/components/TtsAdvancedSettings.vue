<template>
  <el-drawer
    :visible.sync="drawerVisible"
    :before-close="handleClose"
    direction="rtl"
    size="400px"
    :modal="true"
    :show-close="false"
    custom-class="tts-advanced-drawer"
  >
    <div class="drawer-header" slot="title">
      <h2 class="drawer-title">{{ $t('roleConfig.advancedSettings') }}</h2>
      <button type="button" class="drawer-close-btn" :aria-label="$t('button.close')" @click="handleClose">
        <i class="el-icon-close" aria-hidden="true"></i>
      </button>
    </div>

    <div class="drawer-content">
      <el-form label-position="top">
        <!-- Volume -->
        <el-form-item :label="$t('roleConfig.ttsVolume')">
          <div class="slider-container">
            <el-slider
              v-model="localSettings.volume"
              :min="-100"
              :max="100"
              :step="1"
              :format-tooltip="formatTooltip"
              class="tts-slider"
              @change="markTtsSettingChanged('volume')"
            />
            <span class="slider-hint">{{ $t('roleConfig.volumeHint') }}</span>
          </div>
        </el-form-item>

        <!-- Speed -->
        <el-form-item :label="$t('roleConfig.ttsRate')">
          <div class="slider-container">
            <el-slider
              v-model="localSettings.speed"
              :min="-100"
              :max="100"
              :step="1"
              :format-tooltip="formatTooltip"
              class="tts-slider"
              @change="markTtsSettingChanged('speed')"
            />
            <span class="slider-hint">{{ $t('roleConfig.speedHint') }}</span>
          </div>
        </el-form-item>

        <!-- Pitch -->
        <el-form-item :label="$t('roleConfig.ttsPitch')">
          <div class="slider-container">
            <el-slider
              v-model="localSettings.pitch"
              :min="-100"
              :max="100"
              :step="1"
              :format-tooltip="formatTooltip"
              class="tts-slider"
              @change="markTtsSettingChanged('pitch')"
            />
            <span class="slider-hint">{{ $t('roleConfig.pitchHint') }}</span>
        </div>
        </el-form-item>

      </el-form>
      <!-- Linked replacement words -->
      <div>
        <h4 class="replacement-label">
          {{ $t('roleConfig.replacementWordLabel') }}
          <el-tooltip popper-class="tts-tooltip" :content="$t('roleConfig.replacementWordTip')" effect="light" placement="top">
            <i class="el-icon-question" tabindex="0" :aria-label="$t('roleConfig.replacementWordTip')"></i>
          </el-tooltip>
        </h4>
        <el-select
          v-model="replacementWordIds"
          multiple
          filterable
          collapse-tags
          :placeholder="$t('replacementDialog.selectPlaceholder')"
          class="replacement-word-select"
        >
          <el-option
            v-for="item in replacementWordList"
            :key="item.id"
            :label="item.fileName"
            :value="item.id"
          />
        </el-select>
      </div>
    </div>

    <div class="drawer-footer">
      <el-button @click="handleCancel">{{ $t('button.cancel') }}</el-button>
      <el-button type="primary" @click="handleSave">{{ $t('button.save') }}</el-button>
    </div>
  </el-drawer>
</template>

<script>
import correctWord from '@/apis/module/correctWord';

export default {
  name: 'TtsAdvancedSettings',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    settings: {
      type: Object,
      default: () => ({
        volume: 0,
        speed: 0,
        pitch: 0
      })
    },
    checkedReplacementWordIds: {
      type: Array,
      default: () => []
    },
  },
  data() {
    return {
      localSettings: {
        volume: 0,
        speed: 0,
        pitch: 0,
      },
      changedTtsFields: {
        volume: false,
        speed: false,
        pitch: false,
      },
      replacementWordIds: [],
      replacementWordList: []
    };
  },
  computed: {
    drawerVisible: {
      get() {
        return this.visible;
      },
      set(val) {
        this.$emit('update:visible', val);
      }
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        // When the drawer opens, copy the current settings locally
        this.localSettings = { ...this.settings };
        this.changedTtsFields = {
          volume: false,
          speed: false,
          pitch: false,
        };
        this.replacementWordIds = [...this.checkedReplacementWordIds];
        this.fetchReplacementWordList();
      }
    }
  },
  methods: {
    handleClose() {
      this.$emit('update:visible', false);
    },
    handleCancel() {
      // On cancel, close without saving
      this.handleClose();
    },
    handleSave() {
      // Save settings and close
      const changedTtsFields = Object.keys(this.changedTtsFields)
        .filter((field) => this.changedTtsFields[field]);
      this.$emit('save', {
        ...this.localSettings,
        changedTtsFields,
        replacementWordIds: this.replacementWordIds
      });
      this.handleClose();
    },
    markTtsSettingChanged(field) {
      this.$set(this.changedTtsFields, field, true);
    },
    formatTooltip(val) {
      return `${val}%`;
    },
    fetchReplacementWordList() {
      correctWord.selectAll(({ data }) => {
        if (data.code === 0) {
          this.replacementWordList = data.data;
        }
      });
    },
    getTagName(id) {
      const item = this.replacementWordList.find(item => item.id === id);
      return item ? item.fileName : '';
    }
  }
};
</script>

<style scoped>
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 16px 24px;
  border-bottom: 1px solid var(--x-divider);
}

.drawer-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

.drawer-close-btn {
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
}

.drawer-close-btn:hover,
.drawer-close-btn:focus-visible {
  color: var(--x-text);
  background: var(--x-canvas);
  outline: none;
}

.drawer-content {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
  text-align: left;
}

.slider-container {
  width: 100%;
  padding: 0 8px;
  box-sizing: border-box;
}

.slider-hint {
  display: block;
  font-size: 12px;
  color: var(--x-text-muted);
  margin-top: 4px;
  line-height: 1.5;
}

.tts-slider {
  width: 100%;
}

.tts-slider ::v-deep .el-slider__runway {
  background: var(--x-border);
}

.tts-slider ::v-deep .el-slider__bar {
  background: var(--x-ink);
}

.tts-slider ::v-deep .el-slider__button {
  border-color: var(--x-ink);
}

.tts-slider ::v-deep .el-slider__input {
  width: 80px;
}

.tts-slider ::v-deep .el-input__inner {
  text-align: center;
  padding: 0 8px;
}

.drawer-footer {
  padding: 14px 24px 18px;
  border-top: 1px solid var(--x-divider);
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.drawer-footer .el-button + .el-button {
  margin-left: 0;
}

::v-deep .el-form-item__label {
  font-size: 13px !important;
  color: var(--x-text) !important;
  font-weight: 500;
  line-height: 20px;
  padding-bottom: 6px;
}

::v-deep .el-form-item {
  margin-bottom: 20px;
}

.replacement-label {
  margin: 4px 0 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text);
}

.replacement-label i {
  margin-left: 4px;
  color: var(--x-text-muted);
  cursor: pointer;
  font-size: 14px;
}

.replacement-label i:hover,
.replacement-label i:focus-visible {
  color: var(--x-text);
  outline: none;
}

.replacement-word-select ::v-deep .el-tag {
  background: var(--x-divider);
  color: var(--x-text-secondary);
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: normal;
  border: none;
}

.replacement-word-select ::v-deep .el-tag__close {
  color: var(--x-text-muted);
}

.replacement-word-select ::v-deep .el-tag .el-icon-close {
  background: transparent;
}

.replacement-word-select ::v-deep .el-tag .el-icon-close:hover {
  background: var(--x-border-control);
  color: var(--x-text);
}
</style>

<style>
.tts-advanced-drawer {
  max-width: 100vw;
}

.tts-advanced-drawer .el-drawer__header {
  margin-bottom: 0;
  padding: 0;
}

.tts-advanced-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  padding: 0;
}

.replacement-word-select {
  width: 100%;
  margin-bottom: 12px;
}

.replacement-label {
  text-align: left;
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.replacement-tag {
  background-color: var(--x-ink);
  border-color: var(--x-ink);
  color: var(--x-on-ink);
}

.tts-tooltip {
  max-height: 60vh !important;
  max-width: min(400px, calc(100vw - 32px)) !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  word-break: break-word;
}

.tts-tooltip .popper__arrow {
  display: none !important;
}

.tts-tooltip[x-placement^="top"] .popper__arrow {
  border-top-color: transparent !important;
}

.tts-tooltip[x-placement^="bottom"] .popper__arrow {
  border-bottom-color: transparent !important;
}
</style>
