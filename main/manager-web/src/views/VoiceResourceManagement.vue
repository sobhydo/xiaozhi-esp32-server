<template>
    <div class="welcome">
        <HeaderBar />

        <main class="page">
            <header class="page-header">
                <div class="page-heading">
                    <h1 class="page-title">{{ $t('voiceResource.title') }}</h1>
                    <p class="page-description">{{ $t('console.voiceResourceDescription') }}</p>
                </div>
                <div class="page-actions">
                    <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
                        {{ $t('voiceClone.addNew') }}
                    </el-button>
                </div>
            </header>

            <section class="table-card">
                <div class="table-toolbar">
                    <div class="search-group">
                        <el-input :placeholder="$t('voiceClone.searchPlaceholder')"
                            :aria-label="$t('voiceClone.searchPlaceholder')" v-model="searchName"
                            class="search-input" prefix-icon="el-icon-search" @keyup.enter.native="handleSearch"
                            clearable />
                        <el-button size="small" @click="handleSearch">{{ $t('voiceClone.search') }}</el-button>
                    </div>
                    <div class="toolbar-actions">
                        <el-button size="small" @click="handleSelectAll">
                            {{ isAllSelected ? $t('voiceClone.deselectAll') : $t('voiceClone.selectAll') }}
                        </el-button>
                        <el-button size="small" class="btn-danger-outline" icon="el-icon-delete"
                            @click="deleteSelectedVoiceClones">{{ $t('voiceClone.delete') }}</el-button>
                    </div>
                </div>
                <CustomTable ref="voiceTable" class="resource-table" :data="voiceCloneList" :columns="tableColumns"
                    :loading="loading" :show-selection="true" :show-operations="true"
                    :operations-label="$t('voiceClone.action')" :operations-width="110"
                    :total="total" :current-page="currentPage" :page-size="pageSize"
                    :page-size-options="pageSizeOptions" @size-change="handlePageSizeChange"
                    @page-change="goToPage">
                    <template slot="voiceId" slot-scope="scope">
                        <span class="mono">{{ scope.row.voiceId }}</span>
                    </template>
                    <template slot="name" slot-scope="scope">
                        <span class="cell-strong">{{ scope.row.name }}</span>
                    </template>
                    <template slot="trainStatus" slot-scope="scope">
                        <span class="status-pill" :class="getTrainStatusClass(scope.row)">
                            <span class="status-pill-dot" aria-hidden="true"></span>{{ getTrainStatusText(scope.row) }}
                        </span>
                    </template>
                    <template slot="createdAt" slot-scope="scope">
                        {{ formatDate(scope.row.createDate) }}
                    </template>
                    <template slot="operations" slot-scope="scope">
                        <el-button size="mini" type="text" class="row-action row-action--danger"
                            @click="deleteVoiceClone(scope.row)">{{ $t('voiceClone.delete') }}</el-button>
                    </template>
                </CustomTable>
            </section>
        </main>

        <!-- Add voice resource dialog -->
        <voice-clone-dialog :title="$t('voiceClone.addVoiceClone')" :visible.sync="dialogVisible" :form="voiceCloneForm"
            @submit="handleSubmit" @cancel="dialogVisible = false" />

        <el-footer>
            <version-footer />
        </el-footer>
    </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import VoiceCloneDialog from "@/components/VoiceResourceDialog.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";
import { formatDate } from "@/utils/format";

export default {
    components: { HeaderBar, VoiceCloneDialog, VersionFooter, CustomButton, CustomTable },
    data() {
        return {
            searchName: "",
            loading: false,
            voiceCloneList: [],
            currentPage: 1,
            pageSize: 10,
            pageSizeOptions: [10, 20, 50, 100],
            total: 0,
            dialogVisible: false,
            isAllSelected: false,
            voiceCloneForm: {
                modelId: "",
                voiceIds: [],
                userId: null,
                languages: ""
            },
            tableColumns: []
        };
    },
    created() {
        this.initTableColumns();
        this.fetchVoiceCloneList();
    },
    methods: {
        initTableColumns() {
            this.tableColumns = [
                { prop: 'voiceId', label: this.$t('voiceClone.voiceId'), align: 'left', minWidth: 140 },
                { prop: 'name', label: this.$t('voiceClone.name'), align: 'left', minWidth: 120 },
                { prop: 'userName', label: this.$t('voiceClone.userId'), align: 'left', minWidth: 120 },
                { prop: 'modelName', label: this.$t('voiceClone.platformName'), align: 'left', minWidth: 120 },
                { prop: 'languages', label: this.$t('voiceClone.languages'), align: 'left', minWidth: 110 },
                { prop: 'trainStatus', label: this.$t('voiceClone.trainStatus'), align: 'left', minWidth: 180, showOverflowTooltip: false },
                { prop: 'createdAt', label: this.$t('voiceClone.createdAt'), align: 'left', minWidth: 170 }
            ];
        },
        handlePageSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.fetchVoiceCloneList();
        },
        fetchVoiceCloneList() {
            this.loading = true;
            const params = {
                page: this.currentPage,
                limit: this.pageSize,
                name: this.searchName || "",
                orderField: "create_date",
                order: "desc"
            };
            Api.voiceResource.getVoiceResourceList(params, (res) => {
                this.loading = false;
                res = res.data
                if (res.code === 0) {
                    this.voiceCloneList = res.data.list.map(item => ({
                        ...item,
                        selected: false
                    }));
                    this.total = res.data.total || 0;
                } else {
                    this.voiceCloneList = [];
                    this.total = 0;
                    this.$message.error({
                        message: res?.data?.msg || this.$t('voiceClone.deleteFailed'),
                        showClose: true
                    });
                }
            });
        },
        handleSearch() {
            this.currentPage = 1;
            this.fetchVoiceCloneList();
        },
        handleSelectAll() {
            this.isAllSelected = !this.isAllSelected;
            this.voiceCloneList.forEach(row => {
                row.selected = this.isAllSelected;
            });
        },
        showAddDialog() {
            this.voiceCloneForm = {
                modelId: "",
                voiceIds: [],
                userId: null,
                languages: ""
            };
            this.$nextTick(() => {
                if (this.$refs.voiceCloneForm) {
                    this.$refs.voiceCloneForm.clearValidate();
                }
            });
            this.dialogVisible = true;
        },
        handleSubmit(formData) {
            Api.voiceResource.saveVoiceResource(formData, (res) => {
                res = res.data;
                if (res.code === 0) {
                    this.$message.success({
                        message: this.$t('voiceClone.addSuccess'),
                        showClose: true
                    });
                    this.dialogVisible = false;
                    this.fetchVoiceCloneList();
                } else {
                    this.$message.error({
                        message: res.msg || this.$t('voiceClone.addFailed'),
                        showClose: true
                    });
                }
            });
        },
        deleteSelectedVoiceClones() {
            const selectedRows = this.voiceCloneList.filter(row => row.selected);
            if (selectedRows.length === 0) {
                this.$message.warning({
                    message: this.$t('voiceClone.selectFirst'),
                    showClose: true
                });
                return;
            }
            this.deleteVoiceClone(selectedRows);
        },
        deleteVoiceClone(row) {
            const items = Array.isArray(row) ? row : [row];

            if (Array.isArray(row) && row.length === 0) {
                this.$message.warning({
                    message: this.$t('voiceClone.selectFirst'),
                    showClose: true
                });
                return;
            }

            const itemCount = items.length;
            this.$confirm(this.$t('voiceClone.confirmDelete', { count: itemCount }), this.$t('voiceClone.warning'), {
                confirmButtonText: this.$t('voiceClone.ok'),
                cancelButtonText: this.$t('voiceClone.cancel'),
                type: 'warning',
                distinguishCancelAndClose: true
            }).then(() => {
                const ids = items.map(item => item.id);
                if (ids.some(id => !id)) {
                    this.$message.error({
                        message: this.$t('voiceClone.deleteFailed'),
                        showClose: true
                    });
                    return;
                }

                Api.voiceResource.deleteVoiceResource(ids, (res) => {
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success({
                            message: this.$t('voiceClone.deleteSuccess', { count: itemCount }),
                            showClose: true
                        });
                        this.fetchVoiceCloneList();
                    } else {
                        this.$message.error({
                            message: res.msg || this.$t('voiceClone.deleteFailed'),
                            showClose: true
                        });
                    }
                });
            }).catch(action => {
                if (action === 'cancel') {
                    this.$message({
                        type: 'info',
                        message: this.$t('voiceClone.operationCancelled'),
                        duration: 1000
                    });
                } else {
                    this.$message({
                        type: 'info',
                        message: this.$t('voiceClone.operationClosed'),
                        duration: 1000
                    });
                }
            });
        },
        goToPage(page) {
            if (page !== this.currentPage) {
                this.currentPage = page;
                this.fetchVoiceCloneList();
            }
        },
        formatDate,
        getTrainStatusClass(row) {
            if (!row.hasVoice) return '';
            if (row.trainStatus === 1) return 'status-pill--warning';
            if (row.trainStatus === 2) return 'status-pill--success';
            if (row.trainStatus === 3) return 'status-pill--danger';
            return '';
        },
        getTrainStatusText(row) {
            if (!row.hasVoice) {
                return this.$t('voiceClone.waitingUpload');
            }
            switch (row.trainStatus) {
                case 0:
                    return this.$t('voiceClone.waitingTraining');
                case 1:
                    return this.$t('voiceClone.training');
                case 2:
                    return this.$t('voiceClone.trainSuccess');
                case 3:
                    return this.$t('voiceClone.trainFailed');
                default:
                    return '';
            }
        }
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

.page {
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
    max-width: 100%;
}

.search-input {
    width: 300px;
    max-width: 100%;

    ::v-deep .el-input__inner {
        height: 34px;
        line-height: 34px;
    }

    ::v-deep .el-input__icon {
        line-height: 34px;
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
.resource-table {
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
        overflow-x: auto;
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
    font-size: 12px;
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
    white-space: nowrap;
    background: var(--x-divider);
    color: var(--x-text-secondary);

    .status-pill-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: var(--x-text-muted);
    }

    &--success {
        background: var(--x-success-bg);
        color: var(--x-success-text);

        .status-pill-dot {
            background: var(--x-success);
        }
    }

    &--warning {
        background: var(--x-warning-bg);
        color: var(--x-warning-text);

        .status-pill-dot {
            background: var(--x-warning);
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

.row-action {
    padding: 6px 8px;
    font-size: 13px;
    color: var(--x-text-secondary);

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
        height: 560px;
    }
}

@media (max-width: 768px) {
    .page {
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
}
</style>
