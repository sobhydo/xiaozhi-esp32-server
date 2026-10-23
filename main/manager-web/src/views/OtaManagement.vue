<template>
    <div class="welcome">
        <HeaderBar />

        <main class="admin-page">
            <header class="page-header">
                <div class="page-heading">
                    <h1 class="page-title">{{ $t('otaManagement.firmwareManagement') }}</h1>
                    <p class="page-description">{{ $t('console.otaDescription') }}</p>
                </div>
                <div class="page-actions">
                    <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
                        {{ $t('otaManagement.addNew') }}
                    </el-button>
                </div>
            </header>

            <section class="table-card">
                <div class="table-toolbar">
                    <div class="search-group">
                        <el-input :placeholder="$t('otaManagement.searchPlaceholder')"
                            :aria-label="$t('otaManagement.searchPlaceholder')" v-model="searchName" class="search-input"
                            prefix-icon="el-icon-search" @keyup.enter.native="handleSearch" clearable />
                        <el-button class="btn-search" @click="handleSearch">{{ $t('otaManagement.search') }}</el-button>
                    </div>
                    <div class="toolbar-actions">
                        <el-button size="small" @click="handleSelectAll">
                            {{ isAllSelected ? $t('otaManagement.deselectAll') : $t('otaManagement.selectAll') }}
                        </el-button>
                        <el-button size="small" class="btn-danger-outline" @click="deleteSelectedParams">
                            {{ $t('otaManagement.delete') }}
                        </el-button>
                    </div>
                </div>
                <CustomTable ref="paramsTable" class="flush-table" :data="paramsList" :columns="tableColumns"
                    :loading="loading" :show-selection="true" :show-operations="true"
                    :operations-label="$t('otaManagement.action')" :operations-width="210" :total="total"
                    :current-page="currentPage" :page-size="pageSize" :page-size-options="pageSizeOptions"
                    @size-change="handlePageSizeChange" @page-change="goToPage">
                    <template slot="selection" slot-scope="scope">
                        <el-checkbox v-model="scope.row.selected"></el-checkbox>
                    </template>
                    <template slot="firmwareName" slot-scope="scope">
                        <span class="cell-strong">{{ scope.row.firmwareName }}</span>
                    </template>
                    <template slot="type" slot-scope="scope">
                        {{ getFirmwareTypeName(scope.row.type) }}
                    </template>
                    <template slot="version" slot-scope="scope">
                        <span class="mono">{{ scope.row.version }}</span>
                    </template>
                    <template slot="size" slot-scope="scope">
                        {{ formatFileSize(scope.row.size) }}
                    </template>
                    <template slot="remark" slot-scope="scope">
                        <span class="muted">{{ scope.row.remark }}</span>
                    </template>
                    <template slot="createDate" slot-scope="scope">
                        {{ formatDate(scope.row.createDate) }}
                    </template>
                    <template slot="updateDate" slot-scope="scope">
                        {{ formatDate(scope.row.updateDate) }}
                    </template>
                    <template slot="operations" slot-scope="scope">
                        <div class="row-actions">
                            <el-button size="mini" type="text" class="row-action" @click="downloadFirmware(scope.row)">
                                {{ $t('otaManagement.download') }}
                            </el-button>
                            <el-button size="mini" type="text" class="row-action" @click="editParam(scope.row)">
                                {{ $t('otaManagement.edit') }}
                            </el-button>
                            <el-button size="mini" type="text" class="row-action row-action--danger"
                                @click="deleteParam(scope.row)">
                                {{ $t('otaManagement.delete') }}
                            </el-button>
                        </div>
                    </template>
                </CustomTable>
            </section>
        </main>

        <!-- Add/edit firmware dialog -->
        <firmware-dialog ref="firmwareDialog" :title="dialogTitle" :visible.sync="dialogVisible" :form="firmwareForm"
            :firmware-types="firmwareTypes" @submit="handleSubmit" @cancel="dialogVisible = false" />
        <el-footer>
            <version-footer />
        </el-footer>
    </div>
</template>

<script>
import Api from "@/apis/api";
import FirmwareDialog from "@/components/FirmwareDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import CustomTable from "@/components/CustomTable.vue";
import { formatDate, formatFileSize } from "@/utils/format";

export default {
    components: { HeaderBar, FirmwareDialog, VersionFooter, CustomTable },
    data() {
        return {
            searchName: "",
            loading: false,
            paramsList: [],
            firmwareList: [],
            currentPage: 1,
            pageSize: 10,
            pageSizeOptions: [10, 20, 50, 100],
            total: 0,
            dialogVisible: false,
            dialogTitle: "Add Firmware",
            isAllSelected: false,
            firmwareForm: {
                id: null,
                firmwareName: "",
                type: "",
                version: "",
                size: 0,
                remark: "",
                firmwarePath: ""
            },
            firmwareTypes: [],
            tableColumns: []
        };
    },
    created() {
        this.initTableColumns();
        this.fetchFirmwareList();
        this.getFirmwareTypes();
    },
    methods: {
        initTableColumns() {
            this.tableColumns = [
                { prop: 'firmwareName', label: this.$t('otaManagement.firmwareName'), align: 'left', minWidth: 140 },
                { prop: 'type', label: this.$t('otaManagement.firmwareType'), align: 'left', minWidth: 120 },
                { prop: 'version', label: this.$t('otaManagement.version'), align: 'left', minWidth: 80 },
                { prop: 'size', label: this.$t('otaManagement.fileSize'), align: 'left', minWidth: 80 },
                { prop: 'remark', label: this.$t('otaManagement.remark'), align: 'left', minWidth: 110 },
                { prop: 'createDate', label: this.$t('otaManagement.createTime'), align: 'left', minWidth: 150 },
                { prop: 'updateDate', label: this.$t('otaManagement.updateTime'), align: 'left', minWidth: 150 }
            ];
        },
        handlePageSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.fetchFirmwareList();
        },
        fetchFirmwareList() {
            this.loading = true;
            const params = {
                page: this.currentPage,
                limit: this.pageSize,
                firmwareName: this.searchName || "",
                orderField: "create_date",
                order: "desc"
            };
            Api.ota.getOtaList(params, (res) => {
                this.loading = false;
                res = res.data
                if (res.code === 0) {
                    this.firmwareList = res.data.list.map(item => ({
                        ...item,
                        selected: false
                    }));
                    this.paramsList = this.firmwareList;
                    this.total = res.data.total || 0;
                } else {
                    this.firmwareList = [];
                    this.paramsList = [];
                    this.total = 0;
                    this.$message.error({
                        message: res?.data?.msg || this.$t('otaManagement.fetchFirmwareListFailed'),
                        showClose: true
                    });
                }
            });
        },
        handleSearch() {
            this.currentPage = 1;
            this.fetchFirmwareList();
        },
        handleSelectAll() {
            this.isAllSelected = !this.isAllSelected;
            this.firmwareList.forEach(row => {
                row.selected = this.isAllSelected;
            });
        },
        showAddDialog() {
            this.dialogTitle = this.$t('otaManagement.addFirmware');
            this.firmwareForm = {
                id: null,
                firmwareName: "",
                type: "",
                version: "",
                size: 0,
                remark: "",
                firmwarePath: ""
            };
            this.dialogVisible = true;
        },
        editParam(row) {
            this.dialogTitle = this.$t('otaManagement.editFirmware');
            this.firmwareForm = { ...row };
            this.dialogVisible = true;
        },
        handleSubmit(form) {
            if (form.id) {
                Api.ota.updateOta(form.id, form, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: this.$t('otaManagement.updateSuccess'),
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || this.$t('otaManagement.updateFailed'),
                            showClose: true
                        });
                    }
                    this.$refs.firmwareDialog?.resetSaving();
                });
            } else {
                Api.ota.saveOta(form, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: this.$t('otaManagement.addSuccess'),
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || this.$t('otaManagement.addFailed'),
                            showClose: true
                        });
                    }
                    this.$refs.firmwareDialog?.resetSaving();
                });
            }
        },
        deleteSelectedParams() {
            const selectedRows = this.firmwareList.filter(row => row.selected);
            if (selectedRows.length === 0) {
                this.$message.warning({
                    message: this.$t('otaManagement.selectFirmwareFirst'),
                    showClose: true
                });
                return;
            }
            this.deleteParam(selectedRows);
        },
        deleteParam(row) {
            const params = Array.isArray(row) ? row : [row];

            if (Array.isArray(row) && row.length === 0) {
                this.$message.warning({
                    message: this.$t('otaManagement.selectFirmwareFirst'),
                    showClose: true
                });
                return;
            }

            const paramCount = params.length;
            this.$confirm(this.$t('otaManagement.confirmBatchDelete', { paramCount }), this.$t('common.warning'), {
                confirmButtonText: this.$t('common.confirm'),
                cancelButtonText: this.$t('common.cancel'),
                type: 'warning',
                distinguishCancelAndClose: true
            }).then(() => {
                const ids = params.map(param => param.id);
                if (ids.some(id => !id)) {
                    this.$message.error({
                        message: this.$t('otaManagement.invalidFirmwareId'),
                        showClose: true
                    });
                    return;
                }

                Api.ota.deleteOta(ids, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: this.$t('otaManagement.batchDeleteSuccess', { paramCount }),
                            showClose: true
                        });
                        this.fetchFirmwareList();
                    } else {
                        this.$message.error({
                            message: res.msg || this.$t('otaManagement.deleteFailed'),
                            showClose: true
                        });
                    }
                });
            }).catch(action => {
                if (action === 'cancel') {
                    this.$message({
                        type: 'info',
                        message: this.$t('otaManagement.operationCancelled'),
                        duration: 1000
                    });
                } else {
                    this.$message({
                        type: 'info',
                        message: this.$t('otaManagement.operationClosed'),
                        duration: 1000
                    });
                }
            });
        },
        goToPage(page) {
            if (page !== this.currentPage) {
                this.currentPage = page;
                this.fetchFirmwareList();
            }
        },
        downloadFirmware(firmware) {
            if (!firmware || !firmware.id) {
                this.$message.error(this.$t('otaManagement.incompleteFirmwareInfo'));
                return;
            }
            Api.ota.getDownloadUrl(firmware.id, (res) => {
                if (res.data.code === 0) {
                    const uuid = res.data.data;
                    const baseUrl = process.env.VUE_APP_API_BASE_URL || '';
                    window.open(`${window.location.origin}${baseUrl}/otaMag/download/${uuid}`);
                } else {
                    this.$message.error(this.$t('otaManagement.getDownloadUrlFailed'));
                }
            });
        },
        formatDate,
        formatFileSize,
        async getFirmwareTypes() {
            try {
                const res = await Api.dict.getDictDataByType('FIRMWARE_TYPE')
                this.firmwareTypes = res.data
            } catch (error) {
                console.error('Failed to fetch firmware types:', error)
                this.$message.error(error.message || this.$t('otaManagement.getFirmwareTypesFailed'))
            }
        },
        getFirmwareTypeName(type) {
            const firmwareType = this.firmwareTypes.find(item => item.key === type)
            return firmwareType ? firmwareType.name : type
        },
    },
};
</script>

<style lang="scss" scoped>
.welcome {
    height: 100vh;
    min-height: 560px;
    display: flex;
    flex-direction: column;
    background: var(--x-canvas);
    text-align: left;
}

.admin-page {
    flex: 1;
    min-height: 0;
    width: 100%;
    max-width: 1280px;
    padding: 32px 32px 0;
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

.page-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .el-button + .el-button {
        margin-left: 0;
    }
}

.table-card {
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

.search-group {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
}

.search-input {
    width: 280px;
    max-width: 100%;

    ::v-deep .el-input__inner {
        height: 34px;
        line-height: 34px;
    }

    ::v-deep .el-input__icon {
        line-height: 34px;
        color: var(--x-text-muted);
    }
}

.btn-search {
    height: 34px;
    padding: 0 14px;
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
.flush-table {
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

    ::v-deep .el-checkbox__inner {
        border-color: var(--x-text-disabled);
        border-radius: 4px;
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

.muted {
    color: var(--x-text-muted);
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

    &--danger {
        background: var(--x-danger-bg);
        color: var(--x-danger-text);

        .status-pill-dot {
            background: var(--x-danger);
        }
    }
}

.status-pill-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
}

.row-actions {
    display: flex;
    flex-wrap: nowrap;
    white-space: nowrap;
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

.el-footer {
    height: auto !important;
    padding: 0;
}

@media (max-width: 900px) {
    .welcome {
        height: auto;
        min-height: calc(100vh - var(--x-topbar-height));
    }

    .table-card {
        flex: none;
        height: 600px;
    }
}

@media (max-width: 768px) {
    .admin-page {
        padding: 16px 16px 0;
        gap: 16px;
    }

    .search-group {
        width: 100%;
    }

    .search-input {
        flex: 1;
        width: auto;
    }

    .flush-table ::v-deep .table-footer {
        justify-content: flex-start;
        overflow-x: auto;
    }
}
</style>
