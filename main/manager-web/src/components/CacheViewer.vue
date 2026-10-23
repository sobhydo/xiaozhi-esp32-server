<template>
  <el-dialog
    :title="$t('cache.dialogTitle')"
    :visible.sync="visible"
    width="80%"
    custom-class="cache-viewer-dialog"
    :before-close="handleClose"
  >
    <div v-if="isLoading" class="loading-container">
      <p>{{ $t('cache.loading') }}</p>
    </div>
    
    <div v-else>
      <div v-if="!cacheAvailable" class="no-cache-message">
        <i class="el-icon-warning-outline" aria-hidden="true"></i>
        <p>{{ $t('cache.notSupported') }}</p>
        <el-button type="primary" @click="refreshPage">{{ $t('cache.refreshPage') }}</el-button>
      </div>
      
      <div v-else>
        <el-alert
            v-if="cacheData.totalCached === 0"
            :title="$t('cache.noCachedResources')"
            type="warning"
            :closable="false"
            show-icon
          >
            <p>{{ $t('cache.noCachedResourcesDesc') }}</p>
          </el-alert>
        
        <div v-else>
          <el-alert
            :title="$t('cache.cdnCacheStatus')"
            type="success"
            :closable="false"
            show-icon
          >
            {{ $t('cache.totalCachedResources').replace('{count}', cacheData.totalCached) }}
          </el-alert>
          
          <h3 class="section-title">{{ $t('cache.jsResources').replace('{count}', cacheData.js.length) }}</h3>
          <el-table :data="cacheData.js" class="cache-table" style="width: 100%">
            <el-table-column prop="url" label="URL" width="auto" show-overflow-tooltip />
            <el-table-column prop="cached" :label="$t('cache.status')" width="100">
              <template slot-scope="scope">
                <span v-if="scope.row.cached" class="status-pill status-pill--success">{{ $t('cache.cached') }}</span>
                <span v-else class="status-pill status-pill--danger">{{ $t('cache.notCached') }}</span>
              </template>
            </el-table-column>
          </el-table>
          
          <h3 class="section-title">{{ $t('cache.cssResources').replace('{count}', cacheData.css.length) }}</h3>
          <el-table :data="cacheData.css" class="cache-table" style="width: 100%">
            <el-table-column prop="url" label="URL" width="auto" show-overflow-tooltip />
            <el-table-column prop="cached" :label="$t('cache.status')" width="100">
              <template slot-scope="scope">
                <span v-if="scope.row.cached" class="status-pill status-pill--success">{{ $t('cache.cached') }}</span>
                <span v-else class="status-pill status-pill--danger">{{ $t('cache.notCached') }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
    
    <span slot="footer" class="dialog-footer">
      <el-button @click="handleClose">{{ $t('button.close') }}</el-button>
      <el-button type="primary" @click="refreshCache">{{ $t('cache.refreshStatus') }}</el-button>
      <el-button type="danger" @click="clearCache">{{ $t('cache.clearCache') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import {
  getCacheNames,
  checkCdnCacheStatus,
  clearAllCaches,
  logCacheStatus
} from '../utils/cacheViewer';

export default {
  name: 'CacheViewer',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      isLoading: true,
      cacheAvailable: false,
      cacheData: {
        css: [],
        js: [],
        totalCached: 0,
        totalNotCached: 0
      }
    };
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadCacheData();
      }
    }
  },
  methods: {
    async loadCacheData() {
      this.isLoading = true;
      
      try {
        // First check whether the Cache API is supported
        if (!('caches' in window)) {
          this.cacheAvailable = false;
          this.isLoading = false;
          return;
        }
        
        // Check for a Service Worker cache
        const cacheNames = await getCacheNames();
        this.cacheAvailable = cacheNames.length > 0;
        
        if (this.cacheAvailable) {
          // Get the CDN cache status
          this.cacheData = await checkCdnCacheStatus();
          
          // Log the full cache status to the console
          await logCacheStatus();
        }
      } catch (error) {
        console.error('Failed to load cache data:', error);
        this.$message.error('Failed to load cache data');
      } finally {
        this.isLoading = false;
      }
    },
    
    async refreshCache() {
      this.loadCacheData();
      this.$message.success(this.$t('cache.refreshingStatus'));
    },
    
    async clearCache() {
      this.$confirm(this.$t('cache.confirmClear'), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(async () => {
        try {
          const success = await clearAllCaches();
          if (success) {
            this.$message.success(this.$t('cache.clearedSuccess'));
            await this.loadCacheData();
          } else {
            this.$message.error(this.$t('cache.clearFailed'));
          }
        } catch (error) {
          console.error('Failed to clear cache:', error);
          this.$message.error(this.$t('cache.clearFailed'));
        }
      }).catch(() => {
        this.$message.info(this.$t('cache.clearCanceled'));
      });
    },
    
    refreshPage() {
      window.location.reload();
    },
    
    handleClose() {
      this.$emit('update:visible', false);
    }
  }
};
</script>

<style lang="scss" scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: var(--x-text-muted);
}

.no-cache-message {
  text-align: center;
  padding: 32px 20px;
  color: var(--x-text-secondary);
}

.no-cache-message i {
  font-size: 40px;
  color: var(--x-warning);
  margin-bottom: 8px;
}

.section-title {
  margin: 24px 0 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
  text-align: left;
}

.cache-table {
  border: 1px solid var(--x-border);
  border-radius: 12px;

  &::before {
    display: none;
  }

  ::v-deep td.el-table__cell,
  ::v-deep th.el-table__cell.is-leaf {
    border-bottom-color: var(--x-divider);
  }

  ::v-deep .el-table__row:last-child td.el-table__cell {
    border-bottom: 0;
  }

  ::v-deep td.el-table__cell .cell {
    font-family: var(--x-font-mono);
    font-size: 12px;
    color: var(--x-text-secondary);
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  font-family: var(--x-font);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;

  &--success {
    background: var(--x-success-bg);
    color: var(--x-success-text);
  }

  &--danger {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

/* global.scss styles every `.is-light` (meant for light tooltips) with !important */
::v-deep .el-alert {
  border-radius: 12px;
  box-shadow: none;
  text-align: left;
}

::v-deep .el-alert--success.is-light {
  background: var(--x-success-bg) !important;
  border: 1px solid transparent !important;
  color: var(--x-success-text);
}

::v-deep .el-alert--warning.is-light {
  background: var(--x-warning-bg) !important;
  border: 1px solid transparent !important;
  color: var(--x-warning-text);
}

::v-deep .cache-viewer-dialog {
  margin: 10px auto !important;
  height: calc(100vh - 20px);
  max-height: calc(100vh - 20px);
  max-width: min(1280px, calc(100vw - 32px));
  display: flex;
  flex-direction: column;

  .el-dialog__header {
    padding: 18px 24px 16px;
    border-bottom: 1px solid var(--x-divider);
    text-align: left;
  }

  .el-dialog__body {
    flex: 1;
    max-height: calc(100vh - 20px - 130px);
    overflow-y: auto;
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid var(--x-divider);
  }
}

::v-deep .el-dialog__wrapper {
  align-items: stretch;
}

.dialog-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}
</style>
