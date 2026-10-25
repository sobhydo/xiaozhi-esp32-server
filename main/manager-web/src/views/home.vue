<template>
  <div class="welcome">
    <!-- Common header -->
    <HeaderBar :devices="devices" />
    <main class="agents-page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('header.smartManagement') }}</h1>
          <p class="page-description">{{ $t('console.agentsDescription') }}</p>
        </div>
        <div class="page-actions">
          <div class="search-wrapper">
            <el-input
              v-model="search"
              :placeholder="$t('header.searchPlaceholder')"
              :aria-label="$t('header.searchPlaceholder')"
              class="search-input"
              @keyup.enter.native="handleSearch"
              @clear="handleSearchReset"
              clearable
              ref="searchInput"
              @focus="showSearchHistory"
              @blur="hideSearchHistory"
            >
              <button
                slot="prefix"
                type="button"
                class="search-icon-btn"
                :aria-label="$t('common.search')"
                @click="handleSearch"
              >
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                  stroke-linecap="round" aria-hidden="true">
                  <circle cx="11" cy="11" r="7" />
                  <path d="m20 20-3.5-3.5" />
                </svg>
              </button>
            </el-input>
            <!-- Search history dropdown -->
            <div v-if="showHistory && searchHistory.length > 0" class="search-history-dropdown">
              <div class="search-history-header">
                <span>{{ $t("header.searchHistory") }}</span>
                <el-button type="text" size="small" class="clear-history-btn" @click="clearSearchHistory">
                  {{ $t("header.clearHistory") }}
                </el-button>
              </div>
              <div class="search-history-list">
                <div v-for="(item, index) in searchHistory" :key="index" class="search-history-item"
                  @click.stop="selectSearchHistory(item)">
                  <span class="history-text">{{ item }}</span>
                  <i class="el-icon-close clear-item-icon" @click.stop="removeSearchHistory(index)"></i>
                </div>
              </div>
            </div>
          </div>
          <el-button type="primary" icon="el-icon-plus" class="add-agent-btn" @click="showAddDialog">
            {{ $t('home.addAgent') }}
          </el-button>
        </div>
      </header>

      <div class="device-list-container">
        <template v-if="isLoading">
          <div v-for="i in skeletonCount" :key="'skeleton-' + i" class="skeleton-item" aria-hidden="true">
            <div class="skeleton-head">
              <div class="skeleton-image"></div>
              <div class="skeleton-content">
                <div class="skeleton-line"></div>
                <div class="skeleton-line-short"></div>
              </div>
            </div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line-short"></div>
          </div>
        </template>

        <template v-else>
          <DeviceItem v-for="(item, index) in devices" :key="index" :device="item" :feature-status="featureStatus"
            @configure="goToRoleConfig" @deviceManage="handleDeviceManage" @delete="handleDeleteAgent"
            @chat-history="handleShowChatHistory" />
        </template>
      </div>
      <el-empty v-if="!isLoading && devices.length === 0" class="agents-empty" :image-size="96" />

      <AddWisdomBodyDialog :visible.sync="addDeviceDialogVisible" @confirm="handleWisdomBodyAdded" />
      <el-dialog
        :visible.sync="deleteAgentDialogVisible"
        :close-on-click-modal="!isDeletingAgent"
        :close-on-press-escape="!isDeletingAgent"
        :show-close="!isDeletingAgent"
        width="480px"
        append-to-body
        class="delete-agent-dialog"
        @closed="resetDeleteAgentDialog"
      >
        <template slot="title">
          <div class="delete-agent-title">
            <span class="delete-agent-title-icon" aria-hidden="true">
              <i class="el-icon-warning-outline"></i>
            </span>
            <span>{{ $t('home.deleteConfirmTitle') }}</span>
          </div>
        </template>
        <div class="delete-agent-message">
          <div class="delete-agent-copy-guard" @copy.prevent @cut.prevent @contextmenu.prevent>
            {{ $t('home.confirmDeleteAgent', { agentName: deleteTargetAgentName }) }}
          </div>
          <div class="delete-agent-target delete-agent-copy-guard" @copy.prevent @cut.prevent @contextmenu.prevent>
            {{ deleteTargetAgentName }}
          </div>
          <el-input
            ref="deleteAgentConfirmInput"
            v-model="deleteAgentConfirmText"
            class="delete-agent-input"
            :placeholder="$t('home.deleteAgentNamePlaceholder')"
            :aria-label="$t('home.deleteAgentNamePlaceholder')"
            clearable
            @paste.native.prevent="handleDeleteAgentPaste"
            @drop.native.prevent="handleDeleteAgentPaste"
            @contextmenu.native.prevent
            @keyup.enter.native="confirmDeleteAgent"
          />
          <div v-if="deleteAgentConfirmText && !isDeleteAgentNameMatched" class="delete-agent-helper" role="alert">
            {{ $t('home.deleteAgentNameMismatch') }}
          </div>
        </div>
        <span slot="footer" class="delete-agent-footer">
          <el-button :disabled="isDeletingAgent" @click="closeDeleteAgentDialog">{{ $t('button.cancel') }}</el-button>
          <el-button
            type="danger"
            :loading="isDeletingAgent"
            :disabled="!isDeleteAgentNameMatched"
            @click="confirmDeleteAgent"
          >
            {{ $t('button.ok') }}
          </el-button>
        </span>
      </el-dialog>
    </main>
    <el-footer>
      <version-footer />
    </el-footer>
    <chat-history-dialog :visible.sync="showChatHistory" :agent-id="currentAgentId" :agent-name="currentAgentName" />
  </div>

</template>

<script>
import Api from '@/apis/api';
import { mapState } from "vuex";
import AddWisdomBodyDialog from '@/components/AddWisdomBodyDialog.vue';
import ChatHistoryDialog from '@/components/ChatHistoryDialog.vue';
import DeviceItem from '@/components/DeviceItem.vue';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import featureManager from '@/utils/featureManager';

export default {
  name: 'HomePage',
  components: { DeviceItem, AddWisdomBodyDialog, HeaderBar, VersionFooter, ChatHistoryDialog },
  data() {
    return {
      addDeviceDialogVisible: false,
      devices: [],
      originalDevices: [],
      isSearching: false,
      searchRegex: null,
      isLoading: true,
      skeletonCount: localStorage.getItem('skeletonCount') || 8,
      showChatHistory: false,
      currentAgentId: '',
      currentAgentName: '',
      // Feature status
      featureStatus: {
        voiceprintRecognition: false,
        voiceClone: false,
        knowledgeBase: false
      },
      search: "",
      showHistory: false,
      searchHistory: [],
      deleteAgentDialogVisible: false,
      deleteTargetAgentId: '',
      deleteTargetAgentName: '',
      deleteAgentConfirmText: '',
      isDeletingAgent: false,
    }
  },

  computed: {
    ...mapState({
      userInfo: (state) => state.userInfo,
    }),
    isDeleteAgentNameMatched() {
      return !!this.deleteTargetAgentName && this.deleteAgentConfirmText === this.deleteTargetAgentName;
    },
  },

  async mounted() {
    this.fetchAgentList();
    await this.loadFeatureStatus();
    // Load search history from localStorage
    this.loadSearchHistory();
  },

  methods: {
    // Load feature status
    async loadFeatureStatus() {
      await featureManager.waitForInitialization();
      const config = featureManager.getConfig();
      this.featureStatus = {
        voiceprintRecognition: config.voiceprintRecognition,
        voiceClone: config.voiceClone,
        knowledgeBase: config.knowledgeBase
      };
    },
    
    showAddDialog() {
      this.addDeviceDialogVisible = true
    },
    goToRoleConfig() {
      // Navigate to role config page after clicking configure role
      this.$router.push('/role-config')
    },
    handleWisdomBodyAdded(res) {
      this.fetchAgentList();
      this.addDeviceDialogVisible = false;
    },
    handleDeviceManage() {
      this.$router.push('/device-management');
    },
    handleSearchReset() {
      this.isSearching = false;
      // Assign the original device list to the displayed list directly to avoid reloading
      this.devices = [...this.originalDevices];
    },

    // Update agent list from search
    handleSearchResult(filteredList) {
      this.devices = filteredList; // Update device list
    },
    // Fetch agent list
    fetchAgentList() {
      this.isLoading = true;
      Api.agent.getAgentList(({ data }) => {
        if (data?.data) {
          this.originalDevices = data.data.map(item => ({
            ...item,
            agentId: item.id
          }));

          // Dynamically set skeleton count (optional)
          this.skeletonCount = Math.min(
            Math.max(this.originalDevices.length, 3), // At least 3
            10 // At most 10
          );

          this.handleSearchReset();
        }
        this.isLoading = false;
      }, (error) => {
        console.error('Failed to fetch agent list:', error);
        this.isLoading = false;
      });
    },
    // Delete agent
    handleDeleteAgent(device) {
      const targetAgent = typeof device === 'object'
        ? device
        : this.devices.find((item) => item.agentId === device || item.id === device);
      const agentId = targetAgent?.agentId || targetAgent?.id;
      const agentName = targetAgent?.agentName || '';

      if (!agentId || !agentName) {
        this.$message.error(this.$t('home.deleteAgentMissingInfo'));
        return;
      }

      this.deleteTargetAgentId = agentId;
      this.deleteTargetAgentName = agentName;
      this.deleteAgentConfirmText = '';
      this.deleteAgentDialogVisible = true;
      this.$nextTick(() => {
        if (this.$refs.deleteAgentConfirmInput) {
          this.$refs.deleteAgentConfirmInput.focus();
        }
      });
    },
    handleDeleteAgentPaste() {
      this.$message.warning(this.$t('home.deleteAgentPasteForbidden'));
    },
    closeDeleteAgentDialog() {
      if (this.isDeletingAgent) return;
      this.deleteAgentDialogVisible = false;
    },
    resetDeleteAgentDialog() {
      this.deleteTargetAgentId = '';
      this.deleteTargetAgentName = '';
      this.deleteAgentConfirmText = '';
      this.isDeletingAgent = false;
    },
    confirmDeleteAgent() {
      if (!this.isDeleteAgentNameMatched || this.isDeletingAgent) return;

      this.isDeletingAgent = true;
      Api.agent.deleteAgent(this.deleteTargetAgentId, (res) => {
        this.isDeletingAgent = false;
        if (res.data.code === 0) {
          this.$message.success({
            message: this.$t('home.deleteSuccess'),
            showClose: true
          });
          this.deleteAgentDialogVisible = false;
          this.fetchAgentList(); // Refresh list
        } else {
          this.$message.error({
            message: res.data.msg || this.$t('home.deleteFailed'),
            showClose: true
          });
        }
      });
    },
    handleShowChatHistory({ agentId, agentName }) {
      this.currentAgentId = agentId;
      this.currentAgentName = agentName;
      this.showChatHistory = true;
    },
    // Handle search
    handleSearch() {
      const searchValue = this.search.trim();

      // If the search is empty, trigger reset
      if (!searchValue) {
        this.handleSearchReset();
        return;
      }

      // Save search history
      this.saveSearchHistory(searchValue);

      // Blur the input after searching to trigger blur and hide search history
      if (this.$refs.searchInput) {
        this.$refs.searchInput.blur();
      }

      this.isSearching = true;
      this.isLoading = true;
      // Detect MAC address format: contains 4 colons
      const isMac = /^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$/.test(searchValue)
      const searchType = isMac ? 'mac' : 'name';
      Api.agent.searchAgent(searchValue, searchType, ({ data }) => {
        if (data?.data) {
          this.devices = data.data.map(item => ({
            ...item,
            agentId: item.id
          }));
        }
        this.isLoading = false;
      }, (error) => {
        console.error('Failed to search agents:', error);
        this.isLoading = false;
        this.$message.error(this.$t('message.searchFailed'));
      });
    },

    // Show search history
    showSearchHistory() {
      this.showHistory = true;
    },

    // Hide search history
    hideSearchHistory() {
      // Delay hiding so click events can fire
      setTimeout(() => {
        this.showHistory = false;
      }, 200);
    },

    // Load search history
    loadSearchHistory() {
      try {
        const history = localStorage.getItem(this.SEARCH_HISTORY_KEY);
        if (history) {
          this.searchHistory = JSON.parse(history);
        }
      } catch (error) {
        console.error("Failed to load search history:", error);
        this.searchHistory = [];
      }
    },

    // Save search history
    saveSearchHistory(keyword) {
      if (!keyword || this.searchHistory.includes(keyword)) {
        return;
      }

      // Add to the beginning of history
      this.searchHistory.unshift(keyword);

      // Limit history size
      if (this.searchHistory.length > this.MAX_HISTORY_COUNT) {
        this.searchHistory = this.searchHistory.slice(0, this.MAX_HISTORY_COUNT);
      }

      // Save to localStorage
      try {
        localStorage.setItem(this.SEARCH_HISTORY_KEY, JSON.stringify(this.searchHistory));
      } catch (error) {
        console.error("Failed to save search history:", error);
      }
    },

    // Select search history item
    selectSearchHistory(keyword) {
      this.search = keyword;
      this.handleSearch();
    },

    // Remove a single search history item
    removeSearchHistory(index) {
      this.searchHistory.splice(index, 1);
      try {
        localStorage.setItem(this.SEARCH_HISTORY_KEY, JSON.stringify(this.searchHistory));
      } catch (error) {
        console.error("Failed to update search history:", error);
      }
    },

    // Clear all search history
    clearSearchHistory() {
      this.searchHistory = [];
      try {
        localStorage.removeItem(this.SEARCH_HISTORY_KEY);
      } catch (error) {
        console.error("Failed to clear search history:", error);
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
}

.agents-page {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  padding: 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.page-heading {
  display: flex;
  flex-direction: column;
  gap: 4px;
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
  align-items: center;
  gap: 8px;
}

.search-wrapper {
  position: relative;
  width: 280px;
  max-width: 100%;
}

.search-input {
  ::v-deep .el-input__inner {
    height: 36px;
    line-height: 36px;
    padding-inline-start: 34px;
    background: var(--x-surface);
  }

  ::v-deep .el-input__prefix {
    left: 0;
    display: flex;
    align-items: center;
  }
}

.search-icon-btn {
  width: 34px;
  height: 36px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--x-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: var(--x-radius-control);

  &:hover {
    color: var(--x-text);
  }
}

.add-agent-btn {
  height: 36px;
  padding: 0 16px;
}

.search-history-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 6px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: 12px;
  box-shadow: var(--x-shadow-raised);
  z-index: 1000;
  overflow: hidden;
}

.search-history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--x-divider);
  font-size: 12px;
  color: var(--x-text-muted);
}

.clear-history-btn {
  color: var(--x-text-muted);
  font-size: 12px;
  padding: 0;
  height: auto;

  &:hover,
  &:focus {
    color: var(--x-text);
  }
}

.search-history-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 4px;
}

.search-history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--x-text-secondary);

  &:hover {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &:hover .clear-item-icon {
    visibility: visible;
  }
}

.clear-item-icon {
  font-size: 12px;
  color: var(--x-text-muted);
  visibility: hidden;

  &:hover {
    color: var(--x-danger);
  }
}

.device-list-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.agents-empty {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
}

/* Skeleton */
@keyframes skeleton-pulse {
  50% {
    opacity: 0.55;
  }
}

.skeleton-item {
  height: 212px;
  padding: 16px;
  box-sizing: border-box;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-item {
    animation: none;
  }
}

.skeleton-head {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.skeleton-image {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: var(--x-divider);
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  padding-top: 2px;
}

.skeleton-line,
.skeleton-line-short {
  height: 12px;
  border-radius: 6px;
  background: var(--x-divider);
  margin-bottom: 10px;
  width: 70%;
}

.skeleton-line-short {
  width: 45%;
}

/* Delete agent dialog */
.delete-agent-title {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: var(--x-text);
}

.delete-agent-title-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--x-danger-bg);
  color: var(--x-danger-text);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.delete-agent-message {
  word-break: normal;
  overflow-wrap: anywhere;
  color: var(--x-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.delete-agent-target {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  background: var(--x-surface-raised);
  color: var(--x-text);
  font-weight: 600;
  word-break: break-all;
}

.delete-agent-copy-guard {
  user-select: none;
  -webkit-user-select: none;
}

.delete-agent-input {
  margin-top: 16px;

  ::v-deep .el-input__inner {
    height: 40px;
  }
}

.delete-agent-helper {
  margin-top: 6px;
  color: var(--x-danger-text);
  font-size: 12px;
}

.delete-agent-footer {
  display: inline-flex;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.delete-agent-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.delete-agent-dialog ::v-deep .el-dialog__header {
  padding: 18px 24px 16px;
  border-bottom: 1px solid var(--x-divider);
  text-align: start;
}

.delete-agent-dialog ::v-deep .el-dialog__headerbtn {
  top: 18px;
  right: 16px;
  width: 32px;
  height: 32px;
  border-radius: var(--x-radius-control);
  display: flex;
  align-items: center;
  justify-content: center;

  .el-dialog__close {
    font-size: 16px;
    color: var(--x-text-muted);
  }

  &:hover {
    background: var(--x-canvas);

    .el-dialog__close {
      color: var(--x-text);
    }
  }
}

.delete-agent-dialog ::v-deep .el-dialog__body {
  padding: 20px 24px;
}

.delete-agent-dialog ::v-deep .el-dialog__footer {
  padding: 14px 24px 18px;
  border-top: 1px solid var(--x-divider);
}

@media (max-width: 768px) {
  .agents-page {
    padding: 16px;
    gap: 16px;
  }

  .page-actions,
  .search-wrapper {
    width: 100%;
  }

  .search-wrapper {
    flex: 1 1 100%;
  }

  .device-list-container {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
