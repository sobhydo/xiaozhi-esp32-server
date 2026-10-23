<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('serverSideManager.pageTitle') }}</h1>
          <p class="page-description">{{ $t('console.serverDescription') }}</p>
        </div>
      </header>

      <div class="server-list" v-loading="loading">
        <section v-for="(item, index) in paramsList" :key="index" class="server-card">
          <div class="server-main">
            <span class="server-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="4" width="18" height="7" rx="2" />
                <rect x="3" y="13" width="18" height="7" rx="2" />
                <path d="M7 7.5h.01M7 16.5h.01" />
              </svg>
            </span>
            <div class="server-info">
              <span class="server-label">{{ $t('serverSideManager.wsAddress') }}</span>
              <div class="server-address">
                <code class="server-value">{{ item.address }}</code>
                <el-tooltip :content="$t('common.copyAddress')" placement="top">
                  <el-button class="copy-button" type="text" icon="el-icon-copy-document"
                    :aria-label="$t('common.copyAddress')" @click="copyAddress(item.address)" />
                </el-tooltip>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button icon="el-icon-refresh-right" size="small" @click="emitAction(item, actionMap.restart)">
              {{ $t('serverSideManager.restart') }}
            </el-button>
            <el-button icon="el-icon-setting" size="small" type="primary" @click="emitAction(item, actionMap.update_config)">
              {{ $t('serverSideManager.updateConfig') }}
            </el-button>
          </div>
        </section>
        <div v-if="paramsList.length === 0" class="empty-card">
          <p v-if="!loading" class="empty-text">
            {{ $t('common.noData') }}
          </p>
        </div>
      </div>
    </main>
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";

export default {
  components: { HeaderBar, VersionFooter },
  data() {
    return {
      paramsList: [],
      loading: false,
    };
  },
  created() {
    this.fetchParams();
  },
  computed: {
    actionMap() {
      return {
        restart: {
          value: 'restart',
          title: this.$t('serverSideManager.restartServer'),
          message: this.$t('serverSideManager.confirmRestart'),
          confirmText: this.$t('serverSideManager.restart'),
        },
        update_config: {
          value: 'update_config',
          title: this.$t('serverSideManager.updateConfigTitle'),
          message: this.$t('serverSideManager.confirmUpdateConfig'),
          confirmText: this.$t('serverSideManager.updateConfig'),
        }
      };
    }
  },
  methods: {
    fetchParams() {
      this.loading = true;
      Api.admin.getWsServerList(
        {},
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.paramsList = data.data.map(item => ({ address: item }));
          } else {
            this.$message.error({
              message: data.msg || this.$t('serverSideManager.getServerListFailed'),
              showClose: true
            });
          }
        }
      );
    },
    emitAction(rowItem, actionItem) {
      if (actionItem === undefined || rowItem.address === undefined) {
        return;
      }
      this.$confirm(actionItem.message, actionItem.title, {
        confirmButtonText: actionItem.confirmText,
        cancelButtonText: this.$t('common.cancel')
      }).then(() => {
        Api.admin.sendWsServerAction({
          targetWs: rowItem.address,
          action: actionItem.value
        }, ({ data }) => {
          if (data.code !== 0) {
            this.$message.error({
              message: data.msg || this.$t('serverSideManager.operationFailed'),
              showClose: true
            });
            return;
          }
          this.$message.success({
            message: actionItem.value === 'restart' ? this.$t('serverSideManager.restartSuccess') : this.$t('serverSideManager.updateConfigSuccess'),
            showClose: true
          })
        })
      })
    },
    copyAddress(address) {
      navigator.clipboard.writeText(address).then(() => {
        this.$message.success(this.$t('common.copySuccess'));
      }).catch(() => {
        this.$message.error(this.$t('common.copyFailed'));
      });
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
  text-align: left;
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

.server-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 120px;
}

.server-card,
.empty-card {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
}

.server-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
}

.server-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1 1 320px;
}

.server-icon {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: var(--x-canvas);
  color: var(--x-text);
}

.server-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}

.server-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--x-text-muted);
}

.server-address {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
  max-width: 100%;
}

.server-value {
  font-family: var(--x-font-mono);
  font-size: 14px;
  color: var(--x-text);
  word-break: break-all;
}

.copy-button {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  padding: 0;
  font-size: 15px;
  color: var(--x-text-muted);

  &:hover,
  &:focus {
    background: var(--x-canvas);
    color: var(--x-text);
  }
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.empty-card {
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-text {
  margin: 0;
  font-size: 14px;
  color: var(--x-text-muted);
}

.el-footer {
  height: auto !important;
  padding: 0;
}

@media (max-width: 768px) {
  .page {
    padding: 16px;
    gap: 16px;
  }

  .server-card {
    padding: 16px;
  }
}
</style>
