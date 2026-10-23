<template>
  <div class="welcome">
    <HeaderBar />

    <main class="address-page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('addressBookManagement.mainTitle') }}</h1>
          <p class="page-description">{{ $t('addressBookManagement.subTitle') }}</p>
        </div>
      </header>

      <div class="page-content">
        <!-- Left: device list -->
        <aside class="left-panel">
          <div class="search-bar">
            <el-input
              :placeholder="$t('addressBookManagement.searchPlaceholder')"
              :aria-label="$t('addressBookManagement.searchPlaceholder')"
              v-model="searchKeyword"
              class="search-input"
              @input="handleSearch"
              clearable
            >
              <i slot="prefix" class="el-input__icon el-icon-search" aria-hidden="true"></i>
            </el-input>
          </div>

          <!-- Agent list -->
          <el-collapse accordion class="agent-collapse" v-model="expandedAgentId">
            <el-collapse-item v-for="agent in filteredAgents" :key="agent.id" :name="agent.id">
              <template slot="title">
                <span class="agent-name">{{ agent.agentName }}</span>
                <span class="agent-count">{{ agent.deviceCount || 0 }}</span>
              </template>
              <ul class="device-list">
                <li v-for="device in agent.devices" :key="device.id">
                  <button
                    type="button"
                    class="device-item"
                    :class="{ active: selectedDevice && selectedDevice.id === device.id }"
                    :aria-current="selectedDevice && selectedDevice.id === device.id ? 'true' : null"
                    @click="handleDeviceClick(device, agent)"
                  >
                    <span class="device-avatar" aria-hidden="true">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                        stroke-linecap="round" stroke-linejoin="round">
                        <rect x="6" y="3" width="12" height="18" rx="3" />
                        <circle cx="12" cy="14" r="3" />
                        <path d="M11 7h2" />
                      </svg>
                    </span>
                    <span class="device-content">
                      <span class="device-name">
                        <MacAddressMask :macAddress="device.name" />
                      </span>
                      <span class="device-row">
                        <span class="device-id">
                          <MacAddressMask :macAddress="device.deviceId" />
                        </span>
                        <span class="status-pill" :class="device.online ? 'status-pill--success' : 'status-pill--neutral'">
                          <span class="status-pill-dot" aria-hidden="true"></span>
                          {{ device.online ? $t('addressBookManagement.online') : $t('addressBookManagement.offline') }}
                        </span>
                      </span>
                    </span>
                  </button>
                </li>
              </ul>
            </el-collapse-item>
          </el-collapse>
        </aside>

        <!-- Right: address book details -->
        <section class="right-panel">
          <div class="device-detail" v-if="selectedDevice">
            <!-- Device info header -->
            <div class="device-header">
              <div class="device-left">
                <div class="device-avatar-large" aria-hidden="true">
                  <svg width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                    stroke-linecap="round" stroke-linejoin="round">
                    <rect x="6" y="3" width="12" height="18" rx="3" />
                    <circle cx="12" cy="14" r="3" />
                    <path d="M11 7h2" />
                  </svg>
                </div>
                <div class="device-info">
                  <div class="device-name-row">
                    <input
                      v-if="isEditingAlias"
                      v-model="editAliasValue"
                      class="inline-input alias-input"
                      ref="aliasInput"
                      :aria-label="$t('addressBookManagement.deviceName')"
                      @blur="handleAliasBlur"
                      @keyup.enter="handleAliasBlur"
                      autofocus
                    />
                    <button v-else type="button" class="device-name-text" @click="handleStartEdit">
                      <MacAddressMask :macAddress="selectedDevice.name" />
                    </button>
                    <button v-if="!isEditingAlias" type="button" class="icon-btn"
                      :aria-label="$t('addressBookManagement.edit')" @click="handleStartEdit">
                      <i class="el-icon-edit" aria-hidden="true"></i>
                    </button>
                  </div>
                  <div class="device-mac">
                    <span class="meta-label">{{ $t('addressBookManagement.macAddress') }}</span>
                    <span class="mono"><MacAddressMask :macAddress="selectedDevice.deviceId" /></span>
                  </div>
                  <div class="device-status">
                    <span class="meta-label">{{ $t('addressBookManagement.status') }}</span>
                    <span class="status-pill" :class="selectedDevice.online ? 'status-pill--success' : 'status-pill--neutral'">
                      <span class="status-pill-dot" aria-hidden="true"></span>
                      {{ selectedDevice.online ? $t('addressBookManagement.online') : $t('addressBookManagement.offline') }}
                    </span>
                  </div>
                </div>
              </div>
              <dl class="device-stats">
                <div class="stat-item">
                  <dt class="stat-label">
                    <i class="el-icon-folder" aria-hidden="true"></i>{{ $t('addressBookManagement.deviceGroup') }}
                  </dt>
                  <dd class="stat-value-row">
                    <input
                      v-if="isEditingAgentName"
                      v-model="editAgentNameValue"
                      class="inline-input stat-edit-input"
                      ref="agentNameInput"
                      :aria-label="$t('addressBookManagement.deviceGroup')"
                      @blur="handleAgentNameBlur"
                      @keyup.enter="handleAgentNameBlur"
                      autofocus
                    />
                    <button v-else type="button" class="stat-value stat-value--button" @click="handleStartEditAgentName">
                      {{ selectedAgent ? selectedAgent.agentName : '-' }}
                    </button>
                    <button v-if="!isEditingAgentName && selectedAgent" type="button" class="icon-btn icon-btn--sm"
                      :aria-label="$t('addressBookManagement.edit')" @click="handleStartEditAgentName">
                      <i class="el-icon-edit" aria-hidden="true"></i>
                    </button>
                  </dd>
                </div>
                <div class="stat-item">
                  <dt class="stat-label">
                    <i class="el-icon-time" aria-hidden="true"></i>{{ $t('addressBookManagement.addTime') }}
                  </dt>
                  <dd class="stat-value">{{ formatDeviceCreateDate(selectedDevice) }}</dd>
                </div>
                <div class="stat-item">
                  <dt class="stat-label">
                    <i class="el-icon-link" aria-hidden="true"></i>{{ $t('addressBookManagement.lastOnline') }}
                  </dt>
                  <dd class="stat-value">{{ getTimeAgo(selectedDevice.lastConnectedAt) }}</dd>
                </div>
              </dl>
            </div>

            <!-- Permission management section -->
            <div class="permission-section">
              <div class="section-header">
                <div class="section-title">
                  <h2>{{ $t('addressBookManagement.deviceCallPermission') }}</h2>
                  <p class="section-desc">{{ $t('addressBookManagement.setPermissionDesc', { count: selectedPermissions.length }) }}</p>
                </div>
                <div class="section-actions">
                  <el-button size="small" :disabled="permissionsLoading" @click="handleCancel">{{ $t('common.cancel') }}</el-button>
                  <el-button size="small" :disabled="permissionsLoading" @click="handleToggleSelectAll">{{ isAllSelected ? $t('addressBookManagement.deselectAll') : $t('addressBookManagement.selectAll') }}</el-button>
                  <el-button type="primary" size="small" :disabled="permissionsLoading" @click="handleSavePermissions">{{ $t('addressBookManagement.save') }}</el-button>
                </div>
              </div>

              <div v-loading="permissionsLoading" class="permission-grid">
                <div
                  v-for="device in allDevices"
                  :key="device.id"
                  class="permission-item"
                  :class="{ active: selectedPermissions.includes(device.deviceId) }"
                >
                  <el-checkbox
                    class="permission-checkbox"
                    :disabled="permissionsLoading"
                    :value="selectedPermissions.includes(device.deviceId)"
                    :aria-label="device.addressBookAlias || device.name"
                    @change="(checked) => handlePermissionToggle(device.deviceId, checked)"
                  ></el-checkbox>
                  <div class="permission-avatar" aria-hidden="true">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                      stroke-linecap="round" stroke-linejoin="round">
                      <rect x="6" y="3" width="12" height="18" rx="3" />
                      <circle cx="12" cy="14" r="3" />
                      <path d="M11 7h2" />
                    </svg>
                  </div>
                  <div class="permission-content">
                    <div class="permission-name-row">
                      <input
                        v-if="editingDeviceId === device.id"
                        v-model="editingDeviceName"
                        class="inline-input permission-edit-input"
                        :aria-label="$t('addressBookManagement.deviceName')"
                        @blur="handleEditPermissionBlur(device)"
                        @keyup.enter="handleEditPermissionBlur(device)"
                        autofocus
                      />
                      <button v-else type="button" class="permission-name" @click.stop="handleStartEditPermission(device)">
                        <MacAddressMask :macAddress="device.addressBookAlias || device.name" />
                      </button>
                      <button v-if="editingDeviceId !== device.id" type="button" class="icon-btn icon-btn--sm permission-edit-btn"
                        :aria-label="$t('addressBookManagement.edit')" @click.stop="handleStartEditPermission(device)">
                        <i class="el-icon-edit" aria-hidden="true"></i>
                      </button>
                    </div>
                    <div class="permission-id mono">
                      <MacAddressMask :macAddress="device.deviceId" />
                    </div>
                    <span class="status-pill" :class="device.online ? 'status-pill--success' : 'status-pill--neutral'">
                      <span class="status-pill-dot" aria-hidden="true"></span>
                      {{ device.online ? $t('addressBookManagement.online') : $t('addressBookManagement.offline') }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <el-empty :description="$t('addressBookManagement.selectDevice')" v-else>
          </el-empty>
        </section>
      </div>
    </main>

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import Api from "@/apis/api.js";
import AddressBookApi from "@/apis/module/addressBook.js";
import MacAddressMask from "@/components/MacAddressMask.vue";
import { formatCreateDate, parseTimestamp } from '@/utils/deviceTime.mjs';

export default {
  name: "AddressBookManagement",
  components: { HeaderBar, VersionFooter, MacAddressMask },
  data() {
    return {
      searchKeyword: "",
      filteredAgents: [],
      selectedAgent: null,
      selectedDevice: null,
      expandedAgentId: null,
      agentDeviceOptions: [],
      allDevices: [],
      selectedPermissions: [],
      originalPermissions: [],
      isEditingAlias: false,
      editAliasValue: '',
      isEditingAgentName: false,
      editAgentNameValue: '',
      editingDeviceId: null,
      editingDeviceName: '',
      mqttServiceAvailable: false,
      permissionRequestSequence: 0,
      permissionsLoading: false
    };
  },
  created() {
    this.fetchAgentList();
  },
  computed: {
    isAllSelected() {
      return this.allDevices.length > 0 && this.selectedPermissions.length === this.allDevices.length;
    }
  },
  methods: {
    handleSearch() {
      const keyword = this.searchKeyword.toLowerCase().trim();
      if (!keyword) {
        this.filteredAgents = this.agentDeviceOptions;
        return;
      }
      // Filter: match agent name, device name or MAC address
      this.filteredAgents = this.agentDeviceOptions.filter(agent => {
        // Match agent name
        if (agent.agentName.toLowerCase().includes(keyword)) {
          return true;
        }
        // Match device name or MAC address
        if (agent.devices && agent.devices.some(device => {
          const name = (device.name || '').toLowerCase();
          const mac = (device.deviceId || '').toLowerCase();
          return name.includes(keyword) || mac.includes(keyword);
        })) {
          return true;
        }
        return false;
      });
    },
    fetchAgentList() {
      Api.agent.getAgentList((res) => {
        const agentList = res.data?.data || [];
        if (agentList.length > 0) {
          const agentPromises = agentList.map(agent => {
            return new Promise((resolve) => {
              Api.device.getAgentBindDevices(agent.id, (deviceRes) => {
                const deviceList = deviceRes.data?.data || deviceRes.data || [];
                agent.devices = deviceList.map(device => ({
                  id: device.id,
                  name: (device.alias && device.alias.trim()) ? device.alias : device.id,
                  alias: device.alias || '',
                  addressBookAlias: '',  // Address book alias, initially empty
                  type: device.board,
                  deviceId: device.macAddress,
                  remarks: device.alias || '',
                  online: false,
                  createDate: device.createDate,
                  createDateTimestamp: device.createDateTimestamp,
                  lastConnectedAt: device.lastConnectedAtTimestamp,
                  deviceStatus: 'offline'
                }));
                resolve();
              });
            });
          });
          Promise.all(agentPromises).then(() => {
            const firstDevice = agentList[0] || {};
            this.agentDeviceOptions = agentList;
            this.filteredAgents = agentList;
            // Select the first item by default
            this.handleDeviceClick(firstDevice.devices?.[0] || {}, firstDevice);
            // Fetch device status
            this.fetchDeviceStatus();
          });
        } else {
          this.agentDeviceOptions = [];
          this.filteredAgents = [];
        }
      });
    },
    fetchDeviceStatus() {
      // Fetch device status for each agent
      this.agentDeviceOptions.forEach(agent => {
        if (!agent.id) return;
        Api.device.getDeviceStatus(agent.id, (statusRes) => {
          if (statusRes.data?.code === 0) {
            try {
              const statusData = JSON.parse(statusRes.data.data);
              if (statusData && typeof statusData === 'object') {
                this.mqttServiceAvailable = true;
                this.updateDeviceStatusFromResponse(agent, statusData);
              }
            } catch (e) {
              console.error('Parse device status error:', e);
            }
          }
        });
      });
    },
    updateDeviceStatusFromResponse(agent, statusData) {
      if (!agent.devices) return;
      agent.devices.forEach(device => {
        const macAddress = device.deviceId ? device.deviceId.replace(/:/g, '_') : 'unknown';
        const groupId = device.type ? device.type.replace(/:/g, '_') : 'GID_default';
        const mqttClientId = `${groupId}@@@${macAddress}@@@${macAddress}`;

        if (statusData[mqttClientId]) {
          const statusInfo = statusData[mqttClientId];
          let isOnline = false;
          if (statusInfo.isAlive === true) {
            isOnline = true;
          } else if (statusInfo.isAlive === false) {
            isOnline = false;
          } else if (statusInfo.isAlive === null && statusInfo.exists === true) {
            isOnline = true;
          } else {
            isOnline = false;
          }
          device.online = isOnline;
          device.deviceStatus = isOnline ? 'online' : 'offline';
        } else {
          device.online = false;
          device.deviceStatus = 'offline';
        }
      });
    },
    handleDeviceClick(device, agent) {
      this.expandedAgentId = agent.id;
      this.selectedAgent = agent;
      this.selectedDevice = device;
      // Load all devices for permission selection (excluding the current device)
      this.allDevices = this.agentDeviceOptions
        .flatMap(agent => agent.devices || [])
        .filter(d => d.id !== device.id);
      // Load address book permissions
      this.loadAddressBookPermissions(device.deviceId);
    },
    loadAddressBookPermissions(macAddress) {
      const requestId = ++this.permissionRequestSequence;
      this.permissionsLoading = true;
      this.selectedPermissions = [];
      this.originalPermissions = [];
      this.editingDeviceId = null;
      this.editingDeviceName = '';
      this.allDevices.forEach(device => {
        device.addressBookAlias = '';
      });
      AddressBookApi.getAddressBookList(macAddress, (res) => {
        if (
          requestId !== this.permissionRequestSequence ||
          this.selectedDevice?.deviceId !== macAddress
        ) {
          return;
        }
        this.permissionsLoading = false;
        if (res.data?.code === 0) {
          const permissions = res.data.data || [];
          const permissionsByTargetMac = new Map(
            permissions.map(permission => [
              (permission.targetMac || '').toLowerCase(),
              permission
            ])
          );
          // Set the selected permissions
          const permittedTargetMacs = new Set(
            permissions
              .filter(p => p.hasPermission)
              .map(p => (p.targetMac || '').toLowerCase())
          );
          this.selectedPermissions = this.allDevices
            .filter(device => permittedTargetMacs.has((device.deviceId || '').toLowerCase()))
            .map(device => device.deviceId);
          // Save the initial permission state (to detect changes)
          this.originalPermissions = [...this.selectedPermissions];
          // Update the device's address book alias
          this.allDevices.forEach(device => {
            const addrBook = permissionsByTargetMac.get((device.deviceId || '').toLowerCase());
            if (addrBook) {
              device.addressBookAlias = addrBook.alias || '';
            } else {
              device.addressBookAlias = '';
            }
          });
        }
      });
    },
    handleStartEditPermission(device) {
      if (this.permissionsLoading) return;
      this.editingDeviceId = device.id;
      this.editingDeviceName = device.addressBookAlias || device.name;
      this.$nextTick(() => {
        const input = document.querySelector('.permission-edit-input');
        if (input) input.focus();
      });
    },
    handleEditPermissionBlur(device) {
      if (!this.editingDeviceId) return;
      const newName = this.editingDeviceName.trim();
      if (newName && newName !== (device.addressBookAlias || device.name)) {
        AddressBookApi.updateAlias({
          macAddress: this.selectedDevice.deviceId,
          targetMac: device.deviceId,
          alias: newName
        }, (res) => {
          if (res.data?.code === 0) {
            this.$message.success(this.$t('addressBookManagement.aliasSaved'));
            this.loadAddressBookPermissions(this.selectedDevice.deviceId);
          } else {
            this.$message.error(res.data?.msg || this.$t('addressBookManagement.saveFailed'));
          }
        });
      }
      this.editingDeviceId = null;
      this.editingDeviceName = '';
    },
    handlePermissionToggle(targetMac, checked) {
      if (this.permissionsLoading) return;
      if (checked) {
        if (!this.selectedPermissions.includes(targetMac)) {
          this.selectedPermissions.push(targetMac);
        }
      } else {
        const index = this.selectedPermissions.indexOf(targetMac);
        if (index > -1) {
          this.selectedPermissions.splice(index, 1);
        }
      }
    },
    handleToggleSelectAll() {
      if (this.isAllSelected) {
        this.selectedPermissions = [];
      } else {
        this.selectedPermissions = this.allDevices.map(d => d.deviceId);
      }
    },
    handleCancel() {
      this.selectedPermissions = [];
    },
    handleSavePermissions() {
      if (this.permissionsLoading) return;
      const promises = this.allDevices
        .filter(device => {
          const isNowSelected = this.selectedPermissions.includes(device.deviceId);
          const wasOriginallySelected = this.originalPermissions.includes(device.deviceId);
          return isNowSelected !== wasOriginallySelected;
        })
        .map(device => {
          const hasPermission = this.selectedPermissions.includes(device.deviceId);
          return new Promise((resolve) => {
            AddressBookApi.updatePermission({
              macAddress: this.selectedDevice.deviceId,
              targetMac: device.deviceId,
              hasPermission: hasPermission
            }, (res) => {
              resolve(res.data?.code === 0);
            });
          });
        });
      Promise.all(promises).then((results) => {
        if (promises.length === 0) {
          this.$message.info(this.$t('addressBookManagement.noChanges'));
        } else if (results.every(r => r)) {
          this.$message.success(this.$t('addressBookManagement.permissionSaved'));
          this.originalPermissions = [...this.selectedPermissions];
          this.loadAddressBookPermissions(this.selectedDevice.deviceId);
        } else {
          this.$message.error(this.$t('addressBookManagement.partialSaveFailed'));
        }
      });
    },
    handleStartEdit() {
      this.isEditingAlias = true;
      this.editAliasValue = this.selectedDevice.name;
      this.$nextTick(() => {
        if (this.$refs.aliasInput) {
          this.$refs.aliasInput.focus();
        }
      });
    },
    handleAliasBlur() {
      if (!this.selectedDevice) return;
      const newAlias = this.editAliasValue.trim();
      if (newAlias) {
        Api.device.updateDeviceInfo(this.selectedDevice.id, { alias: newAlias }, ({ data }) => {
          if (data.code === 0) {
            this.selectedDevice.name = newAlias;
            for (const agent of this.agentDeviceOptions) {
              const device = agent.devices?.find(d => d.id === this.selectedDevice.id);
              if (device) {
                device.name = newAlias;
                device.alias = newAlias;
                break;
              }
            }
            this.$message.success(this.$t('addressBookManagement.aliasSaved'));
          } else {
            this.$message.error(data.msg || this.$t('addressBookManagement.saveFailed'));
          }
        });
      }
      this.isEditingAlias = false;
    },
    handleStartEditAgentName() {
      if (!this.selectedAgent) return;
      this.isEditingAgentName = true;
      this.editAgentNameValue = this.selectedAgent.agentName;
      this.$nextTick(() => {
        if (this.$refs.agentNameInput) {
          this.$refs.agentNameInput.focus();
        }
      });
    },
    handleAgentNameBlur() {
      if (!this.selectedAgent) return;
      if (this.editAgentNameValue.trim() && this.editAgentNameValue.trim() !== this.selectedAgent.agentName) {
        const newName = this.editAgentNameValue.trim();
        Api.agent.updateAgentConfig(this.selectedAgent.id, { agentName: newName }, ({ data }) => {
          if (data.code === 0) {
            this.selectedAgent.agentName = newName;
            // Update the agent name in the list
            const agentInList = this.agentDeviceOptions.find(a => a.id === this.selectedAgent.id);
            if (agentInList) {
              agentInList.agentName = newName;
            }
            this.$message.success(this.$t('addressBookManagement.agentNameUpdated'));
          } else {
            this.$message.error(data.msg || this.$t('addressBookManagement.updateFailed'));
          }
        });
      }
      this.isEditingAgentName = false;
    },
    getTimeAgo(timestamp) {
      const ts = parseTimestamp(timestamp);
      if (ts === null) return '-';
      const now = new Date();
      const date = new Date(ts);
      const diff = now - date;

      const seconds = Math.floor(diff / 1000);
      const minutes = Math.floor(seconds / 60);
      const hours = Math.floor(minutes / 60);
      const days = Math.floor(hours / 24);
      const months = Math.floor(days / 30);
      const years = Math.floor(days / 365);

      if (seconds < 60) return this.$t('addressBookManagement.justNow');
      if (minutes < 60) return this.$t('addressBookManagement.minutesAgo', { minutes });
      if (hours < 24) return this.$t('addressBookManagement.hoursAgo', { hours });
      if (days < 30) return this.$t('addressBookManagement.daysAgo', { days });
      if (months < 12) return this.$t('addressBookManagement.monthsAgo', { months });
      return this.$t('addressBookManagement.yearsAgo', { years });
    },
    formatDeviceCreateDate(device) {
      if (!device) return '-';
      return formatCreateDate(device.createDateTimestamp, device.createDate);
    }
  },
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

.address-page {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  padding: 24px 32px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
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

.page-content {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.left-panel,
.right-panel {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  min-width: 0;
}

/* ---------- Left: agents and devices ---------- */
.left-panel {
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 160px);
  overflow: hidden;
}

.search-bar {
  padding: 12px;
  border-bottom: 1px solid var(--x-border);
}

.search-input {
  width: 100%;
}

.agent-collapse {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 6px 8px 8px;
  border: 0;

  ::v-deep .el-collapse-item__header {
    height: 40px;
    line-height: 40px;
    padding: 0 8px;
    border: 0;
    border-radius: var(--x-radius-control);
    background: transparent;
    gap: 8px;

    &:hover {
      background: var(--x-surface-raised);
    }

    &.focusing:focus:not(:hover) {
      color: inherit;
      box-shadow: 0 0 0 3px var(--x-focus-ring);
    }
  }

  ::v-deep .el-collapse-item__arrow {
    margin: 0 0 0 4px;
    color: var(--x-text-muted);
  }

  ::v-deep .el-collapse-item__wrap {
    border: 0;
    background: transparent;
  }

  ::v-deep .el-collapse-item__content {
    padding-bottom: 4px;
  }
}

.agent-name {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-count {
  min-width: 20px;
  padding: 0 6px;
  box-sizing: border-box;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
  text-align: center;
}

.device-list {
  list-style: none;
  margin: 0;
  padding: 2px 0 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  font: inherit;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:hover {
    background: var(--x-surface-raised);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }

  &.active {
    background: var(--x-selected);

    .device-avatar {
      background: var(--x-ink);
      color: var(--x-on-ink);
    }
  }
}

.device-avatar,
.permission-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--x-divider);
  color: var(--x-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.device-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.device-id {
  min-width: 0;
  font-family: var(--x-font-mono);
  font-size: 12px;
  color: var(--x-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- Status pills ---------- */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;

  &--success {
    background: var(--x-success-bg);
    color: var(--x-success-text);

    .status-pill-dot {
      background: var(--x-success);
    }
  }

  &--neutral {
    background: var(--x-divider);
    color: var(--x-text-secondary);

    .status-pill-dot {
      background: var(--x-text-muted);
    }
  }
}

.status-pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

/* ---------- Right: device detail ---------- */
.right-panel {
  padding: 20px;
  box-sizing: border-box;
  min-height: 420px;
}

.device-detail {
  display: flex;
  flex-direction: column;
}

.device-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--x-border);
}

.device-left {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.device-avatar-large {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
  border-radius: 12px;
  background: var(--x-ink);
  color: var(--x-on-ink);
  display: flex;
  align-items: center;
  justify-content: center;
}

.device-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.device-name-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.device-name-text {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.01em;
  line-height: 28px;
  color: var(--x-text);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.inline-input {
  box-sizing: border-box;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--x-ink);
  border-radius: var(--x-radius-control);
  box-shadow: 0 0 0 4px var(--x-focus-ring);
  background: var(--x-surface);
  font: inherit;
  font-size: 14px;
  color: var(--x-text);
  outline: none;
}

.alias-input {
  width: 220px;
  max-width: 100%;
  font-size: 16px;
  font-weight: 600;
}

.icon-btn {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--x-text-muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &--sm {
    width: 24px;
    height: 24px;
    font-size: 13px;
    border-radius: 6px;
  }
}

.device-mac,
.device-status {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: var(--x-text-secondary);
}

.meta-label {
  min-width: 88px;
  font-size: 12px;
  color: var(--x-text-muted);
}

.mono {
  font-family: var(--x-font-mono);
}

.device-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
}

.stat-item {
  min-width: 150px;
  padding: 12px 14px;
  border: 1px solid var(--x-border);
  border-radius: 12px;
  background: var(--x-surface-raised);
  box-sizing: border-box;
}

.stat-label {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--x-text-muted);

  i {
    font-size: 14px;
  }
}

.stat-value,
.stat-value-row {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
}

.stat-value-row {
  display: flex;
  align-items: center;
  gap: 4px;
  min-height: 24px;
}

.stat-value--button {
  min-width: 0;
  max-width: 180px;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-weight: 500;
  color: var(--x-text);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stat-edit-input {
  width: 140px;
  height: 28px;
}

/* ---------- Permissions ---------- */
.permission-section {
  display: flex;
  flex-direction: column;
  margin-top: 20px;
}

.section-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
}

.section-title {
  min-width: 0;

  h2 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    line-height: 1.4;
    color: var(--x-text);
  }

  .section-desc {
    margin: 2px 0 0;
    font-size: 13px;
    color: var(--x-text-muted);
  }
}

.section-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
  min-height: 100px;
}

.permission-item {
  position: relative;
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--x-border);
  border-radius: 12px;
  background: var(--x-surface);
  display: flex;
  align-items: flex-start;
  gap: 10px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }

  &.active {
    border-color: var(--x-ink);
    box-shadow: inset 0 0 0 1px var(--x-ink);
  }
}

.permission-checkbox {
  position: absolute;
  top: 10px;
  right: 10px;
}

.permission-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.permission-name-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 4px;
  padding-right: 24px;
  box-sizing: border-box;
  min-height: 24px;
}

.permission-name {
  min-width: 0;
  padding: 0;
  border: 0;
  background: transparent;
  font: inherit;
  font-size: 14px;
  font-weight: 500;
  color: var(--x-text);
  text-align: left;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-edit-input {
  flex: 1;
  min-width: 0;
  height: 28px;
}

.permission-id {
  max-width: 100%;
  font-size: 12px;
  color: var(--x-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

::v-deep .el-empty {
  min-height: 380px;
}

@media (max-width: 1100px) {
  .page-content {
    grid-template-columns: 260px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .page-content {
    grid-template-columns: minmax(0, 1fr);
  }

  .left-panel {
    max-height: 420px;
  }
}

@media (max-width: 768px) {
  .address-page {
    padding: 16px 16px 0;
    gap: 16px;
  }

  .right-panel {
    padding: 16px;
  }

  .device-stats,
  .stat-item {
    width: 100%;
  }
}
</style>
