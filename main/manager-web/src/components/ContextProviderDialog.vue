<template>
  <CustomDialog
    :title="$t('contextProviderDialog.title')"
    :visible.sync="dialogVisible"
    width="860px"
    class="context-provider-dialog"
    :closeOnClickModal="false"
    :confirmText="$t('contextProviderDialog.confirm')"
    :cancelText="$t('contextProviderDialog.cancel')"
    @confirm="handleConfirm"
  >
    <div class="dialog-content">
      <el-empty v-if="localProviders.length === 0" :image-size="64" :description="$t('contextProviderDialog.noContextApi')">
        <el-button type="primary" icon="el-icon-plus" @click="addProvider(0)">{{ $t('contextProviderDialog.add') }}</el-button>
      </el-empty>

      <section
        v-for="(provider, pIndex) in localProviders"
        :key="pIndex"
        class="provider-item"
      >
        <div class="provider-head">
          <span class="provider-index">#{{ pIndex + 1 }}</span>
          <!-- Provider block controls -->
          <div class="block-controls">
            <el-button size="mini" icon="el-icon-plus" @click="addProvider(pIndex + 1)">
              {{ $t('contextProviderDialog.add') }}
            </el-button>
            <button type="button" class="icon-btn icon-btn--danger" :aria-label="$t('common.delete')"
              @click="removeProvider(pIndex)">
              <i class="el-icon-delete" aria-hidden="true"></i>
            </button>
          </div>
        </div>

        <div class="provider-body">
          <!-- URL Row -->
          <div class="input-row">
            <label class="label-text" :for="'ctx-url-' + pIndex">{{ $t('contextProviderDialog.apiUrl') }}</label>
            <el-input
              :id="'ctx-url-' + pIndex"
              v-model="provider.url"
              :placeholder="$t('contextProviderDialog.apiUrlPlaceholder')"
              size="small"
              class="flex-1 mono-input"
            ></el-input>
          </div>

          <!-- Headers Section -->
          <div class="headers-section">
            <div class="label-text">{{ $t('contextProviderDialog.requestHeaders') }}</div>
            <div class="headers-list">
              <div
                v-for="(header, hIndex) in provider.headers"
                :key="hIndex"
                class="header-row"
              >
                <el-input
                  v-model="header.key"
                  :placeholder="$t('contextProviderDialog.headerKeyPlaceholder')"
                  :aria-label="$t('contextProviderDialog.headerKeyPlaceholder')"
                  size="small"
                  class="header-key"
                ></el-input>
                <span class="separator" aria-hidden="true">:</span>
                <el-input
                  v-model="header.value"
                  :placeholder="$t('contextProviderDialog.headerValuePlaceholder')"
                  :aria-label="$t('contextProviderDialog.headerValuePlaceholder')"
                  size="small"
                  class="flex-1"
                ></el-input>

                <div class="row-controls">
                  <button type="button" class="icon-btn" :aria-label="$t('contextProviderDialog.addHeader')"
                    @click="addHeader(pIndex, hIndex + 1)">
                    <i class="el-icon-plus" aria-hidden="true"></i>
                  </button>
                  <button type="button" class="icon-btn icon-btn--danger" :aria-label="$t('common.delete')"
                    @click="removeHeader(pIndex, hIndex)">
                    <i class="el-icon-minus" aria-hidden="true"></i>
                  </button>
                </div>
              </div>
              <!-- Empty Headers State -->
              <div v-if="provider.headers.length === 0" class="header-row empty-header">
                 <span class="no-header-text">{{ $t('contextProviderDialog.noHeaders') }}</span>
                 <el-button
                    size="mini"
                    icon="el-icon-plus"
                    @click="addHeader(pIndex, 0)"
                  >{{ $t('contextProviderDialog.addHeader') }}</el-button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </CustomDialog>
</template>

<script>
import CustomDialog from './CustomDialog.vue';

export default {
  name: 'ContextProviderDialog',
  components: { CustomDialog },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    providers: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      localProviders: []
    };
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible;
      },
      set(val) {
        this.$emit('update:visible', val);
      }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.initLocalData();
      }
    }
  },
  methods: {
    initLocalData() {
      // Deep copy and convert the headers object to an array
      this.localProviders = this.providers.map(p => {
        const headers = p.headers || {};
        return {
          url: p.url || '',
          headers: Object.entries(headers).map(([key, value]) => ({ key, value }))
        };
      });

      // If empty, add a default block
      if (this.localProviders.length === 0) {
         this.localProviders.push({ url: '', headers: [{ key: '', value: '' }] });
      }
    },
    addProvider(index) {
      this.localProviders.splice(index, 0, {
        url: '',
        headers: [{ key: '', value: '' }]
      });
    },
    removeProvider(index) {
      this.localProviders.splice(index, 1);
    },
    addHeader(pIndex, hIndex) {
      this.localProviders[pIndex].headers.splice(hIndex, 0, { key: '', value: '' });
    },
    removeHeader(pIndex, hIndex) {
      this.localProviders[pIndex].headers.splice(hIndex, 1);
    },
    handleConfirm() {
      const result = this.localProviders
        .filter(p => p.url.trim() !== '')
        .map(p => {
          const headersObj = {};
          p.headers.forEach(h => {
            if (h.key.trim()) {
              headersObj[h.key.trim()] = h.value;
            }
          });
          return {
            url: p.url.trim(),
            headers: headersObj
          };
        });

      this.$emit('confirm', result);
      this.$emit('update:visible', false);
    }
  }
};
</script>

<style lang="scss" scoped>
.context-provider-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.dialog-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}

.provider-item {
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  background: var(--x-surface);
  overflow: hidden;
  transition: border-color 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }
}

.provider-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 8px 8px 16px;
  background: var(--x-surface-raised);
  border-bottom: 1px solid var(--x-divider);
}

.provider-index {
  font-family: var(--x-font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--x-text-muted);
}

.block-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.provider-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.label-text {
  flex-shrink: 0;
  width: 120px;
  font-weight: 500;
  color: var(--x-text);
  font-size: 13px;
  line-height: 32px;
}

.flex-1 {
  flex: 1;
  min-width: 0;
}

.mono-input ::v-deep .el-input__inner {
  font-family: var(--x-font-mono);
  font-size: 12px;
}

.headers-section {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.headers-list {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-key {
  width: 180px;
  flex-shrink: 0;
}

.separator {
  color: var(--x-text-muted);
  font-weight: 600;
}

.row-controls {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.icon-btn {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-muted);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);
    color: var(--x-text);
    outline: none;
  }

  &--danger:hover,
  &--danger:focus-visible {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

.empty-header {
  justify-content: space-between;
  padding: 6px 6px 6px 12px;
  border: 1px dashed var(--x-border-control);
  border-radius: var(--x-radius-control);
  color: var(--x-text-muted);
  font-size: 13px;
}

@media (max-width: 640px) {
  .input-row,
  .headers-section {
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }

  .label-text {
    width: auto;
    line-height: 20px;
  }

  .header-row {
    flex-wrap: wrap;
  }

  .header-key {
    width: auto;
    flex: 1 1 100px;
  }

  .header-row .flex-1 {
    flex: 1 1 140px;
  }
}
</style>
