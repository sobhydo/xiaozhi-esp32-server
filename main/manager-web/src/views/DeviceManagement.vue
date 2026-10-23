<template>
  <div class="welcome">
    <HeaderBar />

    <main class="devices-page">
      <nav class="breadcrumb" :aria-label="$t('device.management')">
        <router-link to="/home" class="breadcrumb-link">{{ $t('header.smartManagement') }}</router-link>
        <span class="breadcrumb-sep" aria-hidden="true">/</span>
        <span class="breadcrumb-current" aria-current="page">{{ $t('device.management') }}</span>
      </nav>

      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('device.management') }}</h1>
          <p class="page-description">{{ $t('console.devicesDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button @click="handleManualAddDevice">{{ $t('device.manualAdd') }}</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAddDevice">
            {{ $t('console.bindWithCode') }}
          </el-button>
        </div>
      </header>

      <section class="device-card">
        <div class="table-toolbar">
          <el-input :placeholder="$t('device.searchPlaceholder')" :aria-label="$t('device.searchPlaceholder')"
            v-model="searchKeyword" class="search-input" @keyup.enter.native="handleSearch" clearable>
            <button slot="prefix" type="button" class="search-icon-btn" :aria-label="$t('device.search')"
              @click="handleSearch">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
            </button>
          </el-input>
          <div class="toolbar-actions">
            <el-button size="small" @click="handleSelectAll">
              {{ isCurrentPageAllSelected ? $t('common.deselectAll') : $t('common.selectAll') }}
            </el-button>
            <el-button size="small" class="btn-danger-outline" @click="deleteSelected">
              {{ $t('device.unbind') }}
            </el-button>
          </div>
        </div>
        <CustomTable
          ref="deviceTable"
          class="device-table"
          :data="paginatedDeviceList"
          :columns="tableColumns"
          :loading="loading"
          :loading-text="$t('deviceManagement.loading')"
          :show-selection="true"
          :show-operations="true"
          :operations-label="$t('device.operation')"
          :operations-width="130"
          :total="filteredDeviceList.length"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-size-options="pageSizeOptions"
          @size-change="handlePageSizeChange"
          @page-change="goToPage"
        >
          <template slot="model" slot-scope="scope">
            <span class="cell-strong">{{ getFirmwareTypeName(scope.row.model) }}</span>
          </template>
          <template slot="macAddress" slot-scope="scope">
            <span class="mono">
              <MacAddressMask :macAddress="scope.row.macAddress" />
            </span>
          </template>
          <template slot="deviceStatus" slot-scope="scope">
            <span v-if="scope.row.deviceStatus === 'online'" class="status-pill status-pill--success">
              <span class="status-pill-dot" aria-hidden="true"></span>{{ $t('device.online') }}
            </span>
            <span v-else class="status-pill status-pill--neutral">
              <span class="status-pill-dot" aria-hidden="true"></span>{{ $t('device.offline') }}
            </span>
          </template>
          <template slot="alias" slot-scope="scope">
            <el-input v-show="scope.row.isEdit" v-model="scope.row.alias" size="mini" maxlength="64" show-word-limit
              :aria-label="$t('device.alias')" @blur="onAliasBlur(scope.row)"
              @keyup.enter.native="onAliasEnter(scope.row)" />
            <button v-show="!scope.row.isEdit" type="button" class="alias-view" @click="scope.row.isEdit = true">
              <span class="alias-text">{{ scope.row.alias || '-' }}</span>
              <i class="el-icon-edit" aria-hidden="true"></i>
            </button>
          </template>
          <template slot="otaSwitch" slot-scope="scope">
            <el-switch v-model="scope.row.otaSwitch" @change="handleOtaSwitchChange(scope.row)"></el-switch>
          </template>
          <template slot="operations" slot-scope="scope">
            <div class="row-actions">
              <el-button v-if="isGenerate(scope.row)" size="mini" type="text" class="row-action"
                @click="handleGenertor(scope.row)">
                {{ $t('device.deviceThemeGeneration') }}
              </el-button>
              <el-button size="mini" type="text" class="row-action row-action--danger"
                @click="handleUnbind(scope.row.device_id)">
                {{ $t('device.unbind') }}
              </el-button>
            </div>
          </template>
        </CustomTable>
      </section>
    </main>

    <AddDeviceDialog :visible.sync="addDeviceDialogVisible" :agent-id="currentAgentId"
      @refresh="fetchBindDevices(currentAgentId)" />
    <ManualAddDeviceDialog :visible.sync="manualAddDeviceDialogVisible" :agent-id="currentAgentId"
      @refresh="fetchBindDevices(currentAgentId)" />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import AddDeviceDialog from "@/components/AddDeviceDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import ManualAddDeviceDialog from "@/components/ManualAddDeviceDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import MacAddressMask from "@/components/MacAddressMask.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";
import {
  compareTimestamps,
  formatCreateDate,
  formatTimestamp,
  hasTimestampValue,
  parseLegacyDate,
  parseTimestamp,
} from '@/utils/deviceTime.mjs';

export default {
  components: {
    HeaderBar,
    AddDeviceDialog,
    ManualAddDeviceDialog,
    VersionFooter,
    MacAddressMask,
    CustomButton,
    CustomTable,
  },
  data() {
    return {
      addDeviceDialogVisible: false,
      manualAddDeviceDialogVisible: false,
      selectedDeviceId: '',
      searchKeyword: "",
      activeSearchKeyword: "",
      currentAgentId: this.$route.query.agentId || '',
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      deviceList: [],
      loading: false,
      userApi: null,
      firmwareTypes: [],
      mqttServiceAvailable: false,
    };
  },
  computed: {
    filteredDeviceList() {
      const keyword = this.activeSearchKeyword.toLowerCase();
      if (!keyword) return this.deviceList;
      return this.deviceList.filter(device =>
        (device.model && device.model.toLowerCase().includes(keyword)) ||
        (device.macAddress && device.macAddress.toLowerCase().includes(keyword))
      );
    },

    paginatedDeviceList() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredDeviceList.slice(start, end);
    },
    isCurrentPageAllSelected() {
      return this.paginatedDeviceList.length > 0 &&
        this.paginatedDeviceList.every(device => device.selected);
    },
    tableColumns() {
      const columns = [
        { prop: 'model', label: this.$t('device.model'), align: 'left', minWidth: 110 },
        { prop: 'firmwareVersion', label: this.$t('device.firmwareVersion'), align: 'left', minWidth: 90 },
        { prop: 'macAddress', label: this.$t('device.macAddress'), align: 'left', minWidth: 170 },
        { prop: 'bindTime', label: this.$t('device.bindTime'), align: 'left', minWidth: 140 },
        { prop: 'lastConversation', label: this.$t('device.lastConversation'), align: 'left', minWidth: 120 },
      ];
      if (this.mqttServiceAvailable) {
        columns.push({ prop: 'deviceStatus', label: this.$t('device.deviceStatus'), align: 'left', minWidth: 100 });
      }
      columns.push({ prop: 'alias', label: this.$t('device.alias'), align: 'left', minWidth: 110 });
      columns.push({ prop: 'otaSwitch', label: this.$t('device.autoUpdate'), align: 'left', minWidth: 90 });
      return columns;
    },
  },
  mounted() {
    const agentId = this.$route.query.agentId;
    if (agentId) {
      this.fetchBindDevices(agentId);
    }
  },
  created() {
    this.getFirmwareTypes()
  },
  methods: {
    async getFirmwareTypes() {
      try {
        const res = await Api.dict.getDictDataByType('FIRMWARE_TYPE')
        this.firmwareTypes = res.data
      } catch (error) {
        console.error(this.$t('device.getFirmwareTypeFailed') + ':', error)
        this.$message.error(error.message || this.$t('device.getFirmwareTypeFailed'))
      }
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },
    handleSearch() {
      this.activeSearchKeyword = this.searchKeyword;
      this.currentPage = 1;
    },

    handleSelectAll() {
      const shouldSelectAll = !this.isCurrentPageAllSelected;
      this.paginatedDeviceList.forEach(row => {
        row.selected = shouldSelectAll;
      });
    },

    deleteSelected() {
      const selectedDevices = this.paginatedDeviceList.filter(device => device.selected);
      if (selectedDevices.length === 0) {
        this.$message.warning({
          message: this.$t('device.selectAtLeastOne'),
          showClose: true
        });
        return;
      }

      this.$confirm(this.$t('device.confirmBatchUnbind').replace('{count}', selectedDevices.length), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(() => {
        const deviceIds = selectedDevices.map(device => device.device_id);
        this.batchUnbindDevices(deviceIds);
      });
    },
    batchUnbindDevices(deviceIds) {
      const promises = deviceIds.map(id => {
        return new Promise((resolve, reject) => {
          Api.device.unbindDevice(id, ({ data }) => {
            if (data.code === 0) {
              resolve();
            } else {
              reject(data.msg || this.$t('device.bindFailed'));
            }
          });
        });
      });
      Promise.all(promises)
        .then(() => {
          this.$message.success({
            message: this.$t('device.batchUnbindSuccess').replace('{count}', deviceIds.length),
            showClose: true
          });
          this.fetchBindDevices(this.currentAgentId);
        })
        .catch(error => {
          this.$message.error({
            message: error || this.$t('device.batchUnbindError'),
            showClose: true
          });
        });
    },
    handleAddDevice() {
      this.addDeviceDialogVisible = true;
    },
    handleManualAddDevice() {
      this.manualAddDeviceDialogVisible = true;
    },
    submitAlias(row) {
      if (row._submitting) return;

      const text = (row.alias || '').trim();
      if (text.length > 64) {
        this.$message.warning(this.$t('device.aliasTooLong'));
        return;
      }
      if (text === row._originalAlias) {
        return;
      }

      row._submitting = true;
      this.updateDeviceInfo(row.device_id, { alias: text }, (ok, resp) => {
        if (ok) {
          row._originalAlias = text;
          this.$message.success(this.$t('device.aliasSaved'));
        } else {
          row.alias = row._originalAlias;
          this.$message.error(resp.msg || this.$t('device.aliasSaveFailed'));
        }
        row._submitting = false;
      });
    },
    onAliasBlur(row) {
      row.isEdit = false;
      setTimeout(() => {
        this.submitAlias(row);
      }, 100);
    },
    onAliasEnter(row) {
      row.isEdit = false;
      this.submitAlias(row);
    },
    handleUnbind(device_id) {
      this.$confirm(this.$t('device.confirmUnbind'), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(() => {
        Api.device.unbindDevice(device_id, ({ data }) => {
          if (data.code === 0) {
            this.$message.success({
              message: this.$t('device.unbindSuccess'),
              showClose: true
            });
            this.fetchBindDevices(this.$route.query.agentId);
          } else {
            this.$message.error({
              message: data.msg || this.$t('device.unbindFailed'),
              showClose: true
            });
          }
        });
      });
    },
    handleGenertor(row) {
      const pathname = window.location.pathname;
      const basePath = pathname.split('/').slice(0, -1).join('/');
      const url = `${window.location.origin}${basePath}/generator/?deviceId=${row.device_id}`;
      sessionStorage.setItem('devicePath', window.location.href);
      window.location.href = url;
    },
    goToPage(page) {
      this.currentPage = page;
    },

    fetchBindDevices(agentId) {
      this.loading = true;
      Api.device.getAgentBindDevices(agentId, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.deviceList = data.data.map(device => {
            const hasCreateDateTimestamp = hasTimestampValue(device.createDateTimestamp);
            const rawBindTime = hasCreateDateTimestamp
              ? parseTimestamp(device.createDateTimestamp)
              : parseLegacyDate(device.createDate);
            return {
              device_id: device.id,
              model: device.board,
              firmwareVersion: device.appVersion,
              macAddress: device.macAddress,
              bindTime: formatCreateDate(device.createDateTimestamp, device.createDate),
              lastConversation: formatTimestamp(device.lastConnectedAtTimestamp),
              alias: device.alias,
              _originalAlias: device.alias,
              isEdit: false,
              _submitting: false,
              otaSwitch: device.autoUpdate === 1,
              rawBindTime,
              selected: false,
              deviceStatus: 'offline'
            };
          })
            .sort((firstDevice, secondDevice) => compareTimestamps(
              firstDevice.rawBindTime,
              secondDevice.rawBindTime,
            ));
          this.activeSearchKeyword = "";
          this.searchKeyword = "";

          this.fetchDeviceStatus(agentId);
        } else {
          this.$message.error(data.msg || this.$t('device.getListFailed'));
        }
      });
    },

    fetchDeviceStatus(agentId) {
      this.loading = true;
      Api.device.getDeviceStatus(agentId, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          try {
            const statusData = JSON.parse(data.data);

            if (statusData && typeof statusData === 'object') {
              this.mqttServiceAvailable = true;
              this.updateDeviceStatusFromResponse(statusData);
            } else {
              this.mqttServiceAvailable = false;
            }
          } catch (error) {
            this.mqttServiceAvailable = false;
          }
        } else {
          this.mqttServiceAvailable = false;
        }
      });
    },

    updateDeviceStatusFromResponse(deviceStatusMap) {
      this.deviceList.forEach(device => {
        const macAddress = device.macAddress ? device.macAddress.replace(/:/g, '_') : 'unknown';
        const groupId = device.model ? device.model.replace(/:/g, '_') : 'GID_default';
        const mqttClientId = `${groupId}@@@${macAddress}@@@${macAddress}`;

        if (deviceStatusMap[mqttClientId]) {
          const statusInfo = deviceStatusMap[mqttClientId];

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

          device.deviceStatus = isOnline ? 'online' : 'offline';
        } else {
          device.deviceStatus = 'offline';
        }
      });
    },
    getFirmwareTypeName(type) {
      const firmwareType = this.firmwareTypes.find(item => item.key === type)
      return firmwareType ? firmwareType.name : type
    },
    updateDeviceInfo(device_id, payload, callback) {
      return Api.device.updateDeviceInfo(device_id, payload, ({ data }) => {
        callback(data.code === 0, data);
      })
    },
    handleOtaSwitchChange(row) {
      this.updateDeviceInfo(row.device_id, { autoUpdate: row.otaSwitch ? 1 : 0 }, (result, { msg }) => {
        if (result) {
          this.$message.success(row.otaSwitch ? this.$t('device.autoUpdateEnabled') : this.$t('device.autoUpdateDisabled'));
          return;
        }
        row.otaSwitch = !row.otaSwitch
        this.$message.error(msg || this.$t('message.error'))
      })
    },
    isGenerate(row) {
      const version = row.firmwareVersion.replace(/\./g, '');
      return Number(version) >= 200;
    },
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  height: 100vh;
  min-height: 560px;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
}

.devices-page {
  flex: 1;
  min-height: 0;
  width: 100%;
  max-width: 1280px;
  padding: 24px 32px 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.breadcrumb {
  padding: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  font-size: 13px;
  color: var(--x-text-muted);
  margin-bottom: -8px;
}

.breadcrumb-link {
  font-weight: 400;
  color: var(--x-text-muted);
  text-decoration: none;

  &:hover {
    color: var(--x-text);
  }
}

.breadcrumb-sep {
  padding: 0 6px;
  color: var(--x-border-control);
}

.breadcrumb-current {
  color: var(--x-text);
  font-weight: 500;
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

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.device-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.table-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--x-border);
}

.search-input {
  width: 320px;
  max-width: 100%;

  ::v-deep .el-input__inner {
    height: 34px;
    line-height: 34px;
    padding-left: 34px;
  }

  ::v-deep .el-input__prefix {
    left: 0;
    display: flex;
    align-items: center;
  }
}

.search-icon-btn {
  width: 34px;
  height: 34px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--x-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover {
    color: var(--x-text);
  }
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.btn-danger-outline {
  color: var(--x-danger-text);
  border-color: var(--x-border-control);
  border-color: color-mix(in srgb, var(--x-danger) 30%, transparent);

  &:not(.is-disabled):hover,
  &:not(.is-disabled):focus {
    color: var(--x-danger-text);
    background: var(--x-danger-bg);
    border-color: color-mix(in srgb, var(--x-danger) 45%, transparent);
  }
}

/* Table sits flush inside the card; CustomTable's own frame is removed */
.device-table {
  ::v-deep .table-container {
    border: 0;
    border-radius: 0;
  }

  ::v-deep .table-footer {
    padding: 10px 16px;
    border-top: 1px solid var(--x-border);
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 8px;
  }

  ::v-deep .el-table th.el-table__cell > .cell {
    white-space: nowrap;
  }

  ::v-deep .el-table th.el-table__cell > .cell,
  ::v-deep .el-table td.el-table__cell > .cell {
    padding-left: 12px;
    padding-right: 12px;
  }
}

.cell-strong {
  font-weight: 500;
  color: var(--x-text);
}

.mono {
  font-family: var(--x-font-mono);
  font-size: 13px;
  color: var(--x-text-secondary);
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;

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

.alias-view {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
  padding: 2px 6px;
  margin-left: -6px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  font: inherit;
  color: var(--x-text);
  cursor: pointer;

  .alias-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .el-icon-edit {
    color: var(--x-text-muted);
  }

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);

    .el-icon-edit {
      color: var(--x-text);
    }
  }
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 2px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.row-action {
  padding: 6px 8px;
  font-size: 13px;
  color: var(--x-text-secondary);

  &:hover,
  &:focus {
    color: var(--x-text);
    background: var(--x-canvas);
  }

  &--danger,
  &--danger:hover,
  &--danger:focus {
    color: var(--x-danger-text);
  }

  &--danger:hover,
  &--danger:focus {
    background: var(--x-danger-bg);
  }
}

@media (max-width: 900px) {
  .welcome {
    height: auto;
    min-height: calc(100vh - var(--x-topbar-height));
  }

  .device-card {
    flex: none;
    height: 560px;
  }
}

@media (max-width: 768px) {
  .devices-page {
    padding: 16px 16px 0;
    gap: 16px;
  }

  .search-input {
    width: 100%;
  }
}
</style>
