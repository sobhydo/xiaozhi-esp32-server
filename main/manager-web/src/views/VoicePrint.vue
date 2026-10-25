<template>
    <div class="welcome">
        <HeaderBar />

        <main class="page">
            <nav class="breadcrumb" :aria-label="$t('voicePrint.pageTitle')">
                <router-link to="/home" class="breadcrumb-link">{{ $t('header.smartManagement') }}</router-link>
                <span class="breadcrumb-sep" aria-hidden="true">/</span>
                <span class="breadcrumb-current" aria-current="page">{{ $t('voicePrint.pageTitle') }}</span>
            </nav>

            <header class="page-header">
                <div class="page-heading">
                    <h1 class="page-title">{{ $t('voicePrint.pageTitle') }}</h1>
                    <p class="page-description">{{ $t('console.voicePrintDescription') }}</p>
                </div>
                <div class="page-actions">
                    <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
                        {{ $t('voicePrint.add') }}
                    </el-button>
                </div>
            </header>

            <section class="table-card">
                <CustomTable
                    ref="paramsTable"
                    class="voice-print-table"
                    :data="voicePrintList"
                    :columns="tableColumns"
                    :loading="loading"
                    :loading-text="$t('voicePrint.loading')"
                    :show-operations="true"
                    :operations-label="$t('voicePrint.action')"
                    :operations-width="140"
                    :total="total"
                    :show-pagination="false"
                    :current-page="currentPage"
                    :page-size="pageSize"
                    :page-size-options="pageSizeOptions"
                    @size-change="handlePageSizeChange"
                    @page-change="goToPage"
                >
                    <template slot="sourceName" slot-scope="scope">
                        <span class="cell-strong">{{ scope.row.sourceName }}</span>
                    </template>
                    <template slot="operations" slot-scope="scope">
                        <div class="row-actions">
                            <el-button size="mini" type="text" class="row-action"
                                @click="editVoicePrint(scope.row)">{{ $t('voicePrint.edit') }}</el-button>
                            <el-button size="mini" type="text" class="row-action row-action--danger"
                                @click="deleteVoicePrint(scope.row.id)">{{ $t('voicePrint.delete') }}</el-button>
                        </div>
                    </template>
                </CustomTable>
            </section>
        </main>

        <!-- Add/edit parameter dialog -->
        <voice-print-dialog :title="dialogTitle" :visible.sync="dialogVisible" :agentId="agentId" :form="paramForm"
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
import VoicePrintDialog from "@/components/VoicePrintDialog.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";

export default {
    components: { HeaderBar, VoicePrintDialog, VersionFooter, CustomButton, CustomTable },
    data() {
        return {
            voicePrintList: [],
            allVoicePrints: [],
            loading: false,
            dialogVisible: false,
            dialogTitle: this.$t('voicePrint.addSpeaker'),
            currentPage: 1,
            pageSize: 10,
            pageSizeOptions: [10, 20, 50, 100],
            total: 0,
            paramForm: {
                id: null,
                audioId: '',
                sourceName: '',
                introduce: ''
            },
            agentId: "1",
            tableColumns: []
        };
    },
    created() {
        this.initTableColumns();
    },
    mounted() {
        const agentId = this.$route.query.agentId;
        if (agentId) {
            this.agentId = agentId;
            this.fetchVoicePrints();
        }
    },
    methods: {
        initTableColumns() {
            this.tableColumns = [
                { prop: 'sourceName', label: this.$t('voicePrint.name'), align: 'left', minWidth: 140 },
                { prop: 'introduce', label: this.$t('voicePrint.description'), align: 'left', minWidth: 220 },
                { prop: 'createDate', label: this.$t('voicePrint.createTime'), align: 'left', minWidth: 160 }
            ];
        },
        fetchVoicePrints() {
            this.loading = true;
            Api.agent.getAgentVoicePrintList(this.agentId,
                ({ data }) => {
                    this.loading = false;
                    if (data.code === 0) {
                        this.allVoicePrints = data.data;
                        this.total = data.data.length;
                        this.currentPage = 1;
                        this.updatePagedData();
                    } else {
                        this.$message.error({
                            message: data.msg || this.$t('voicePrint.fetchFailed'),
                            showClose: true
                        });
                    }
                }
            );
        },
        updatePagedData() {
            const start = (this.currentPage - 1) * this.pageSize;
            this.voicePrintList = this.allVoicePrints.slice(start, start + this.pageSize);
        },
        handlePageSizeChange(val) {
            this.pageSize = val;
            this.currentPage = 1;
            this.updatePagedData();
        },
        goToPage(page) {
            if (page !== this.currentPage) {
                this.currentPage = page;
                this.updatePagedData();
            }
        },
        showAddDialog() {
            this.dialogTitle = this.$t('voicePrint.addSpeaker');
            this.paramForm = {
                id: null,
                audioId: '',
                sourceName: '',
                introduce: ''
            };
            this.dialogVisible = true;
        },
        editVoicePrint(row) {
            this.dialogTitle = this.$t('voicePrint.editSpeaker');
            this.paramForm = { ...row };
            this.dialogVisible = true;
        },
        handleSubmit({ form, done }) {
            if (form.id) {
                Api.agent.updateAgentVoicePrint(form, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: this.$t('voicePrint.updateSuccess'),
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchVoicePrints();
                    }
                    done && done();
                });
            } else {
                Api.agent.addAgentVoicePrint({
                    agentId: this.agentId,
                    audioId: form.audioId,
                    sourceName: form.sourceName,
                    introduce: form.introduce
                }, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: this.$t('voicePrint.addSuccess'),
                            showClose: true
                        });
                        this.dialogVisible = false;
                        this.fetchVoicePrints();
                    }
                    done && done();
                });
            }
        },
        deleteVoicePrint(id) {
            this.$confirm(this.$t('voicePrint.confirmDelete'), this.$t('voicePrint.warning'), {
                confirmButtonText: this.$t('voicePrint.confirm'),
                cancelButtonText: this.$t('voicePrint.cancel'),
                type: 'warning',
                distinguishCancelAndClose: true
            }).then(() => {
                Api.agent.deleteAgentVoicePrint(id, ({ data }) => {
                    if (data.code === 0) {
                        this.$message.success({
                            message: this.$t('voicePrint.deleteSuccess'),
                            showClose: true
                        });
                        this.fetchVoicePrints();
                    } else {
                        this.$message.error({
                            message: data.msg || this.$t('voicePrint.deleteFailed'),
                            showClose: true
                        });
                    }
                });
            }).catch(action => {
                if (action === 'cancel') {
                    this.$message({
                        type: 'info',
                        message: this.$t('voicePrint.cancelDelete'),
                        duration: 1000
                    });
                } else {
                    this.$message({
                        type: 'info',
                        message: this.$t('voicePrint.closeOperation'),
                        duration: 1000
                    });
                }
            });
        }
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
    text-align: start;
}

.page {
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
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    font-size: 13px;
    color: var(--x-text-muted);
    margin-bottom: -8px;
}

.breadcrumb-link {
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

/* Table sits flush inside the card; CustomTable's own frame is removed */
.voice-print-table {
    ::v-deep .table-container {
        border: 0;
        border-radius: 0;
    }

    ::v-deep .table-footer {
        display: none;
    }

    ::v-deep .el-table th.el-table__cell > .cell {
        white-space: nowrap;
    }

    ::v-deep .el-table th.el-table__cell > .cell,
    ::v-deep .el-table td.el-table__cell > .cell {
        padding-inline-start: 12px;
        padding-inline-end: 12px;
    }
}

.cell-strong {
    font-weight: 500;
    color: var(--x-text);
}

.row-actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 2px;

    .el-button + .el-button {
        margin-inline-start: 0;
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
        height: 480px;
    }
}

@media (max-width: 768px) {
    .page {
        padding: 16px 16px 0;
        gap: 16px;
    }
}
</style>
