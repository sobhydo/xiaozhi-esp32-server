<template>
  <section class="doc-section">
    <div class="doc-section-header">
      <div class="doc-section-heading">
        <h2 class="doc-section-title">{{ $t('knowledgeBaseManagement.currentKnowledgeBaseDocuments') }}</h2>
        <p class="doc-section-meta" v-if="kb">
          <span class="doc-section-kb">{{ kb.name }}</span>
          <span class="meta-sep" aria-hidden="true">·</span>
          <span>{{ $t('knowledgeBaseManagement.totalDocuments', { total }) }}</span>
        </p>
      </div>
      <div class="doc-section-actions">
        <el-input
          :placeholder="$t('knowledgeFileUpload.documentNamePlaceholder')"
          :aria-label="$t('knowledgeFileUpload.documentNamePlaceholder')"
          v-model="searchDocName"
          class="doc-search-input"
          size="small"
          clearable
          @keyup.enter.native="handleSearch"
        >
          <i slot="prefix" class="el-input__icon el-icon-search" aria-hidden="true"></i>
        </el-input>
        <el-button size="small" @click="handleSearch">{{ $t('knowledgeBaseManagement.search') }}</el-button>
        <el-button size="small" @click="handleRetrievalTest">{{ $t('knowledgeFileUpload.retrievalTest') }}</el-button>
        <el-button type="primary" size="small" icon="el-icon-upload2" @click="handleUpload">{{ $t('knowledgeBaseManagement.add') }}</el-button>
      </div>
    </div>
    <div class="doc-grid" v-loading="docLoading" :element-loading-text="$t('knowledgeBaseManagement.loading')">
      <article v-for="doc in documents" :key="doc.id" class="doc-card">
        <div class="doc-card-top">
          <span class="doc-file-icon" aria-hidden="true">{{ getFileExt(doc.name) }}</span>
          <div class="doc-card-info">
            <h3 class="doc-card-name" :title="doc.name">{{ doc.name }}</h3>
            <div class="doc-card-meta">{{ $t('knowledgeFileUpload.uploadTime') }} {{ formatDate(doc.createdAt) }}</div>
          </div>
        </div>
        <div class="doc-card-bottom">
          <div class="doc-card-progress">
            <div class="progress-ring" aria-hidden="true">
              <svg viewBox="0 0 40 40">
                <circle class="bg" cx="20" cy="20" r="15"/>
                <circle class="fg" :class="getProgressClass(doc)" cx="20" cy="20" r="15"
                  stroke-dasharray="94.25"
                  :stroke-dashoffset="getProgressOffset(doc)"/>
              </svg>
              <span class="progress-ring-text" :style="{ color: getProgressTextColor(doc) }">{{ getProgressText(doc) }}</span>
            </div>
            <div class="progress-container">
              <span class="status-pill" :class="getStatusPillClass(doc)">{{ getDocStatusText(doc) }}</span>
              <span class="doc-slice-count">{{ $t('knowledgeFileUpload.sliceCount') }} <b>{{ doc.sliceCount }}</b></span>
            </div>
          </div>
          <div class="doc-card-actions">
            <el-button
              v-if="doc.parseStatusCode === 3"
              type="text"
              size="mini"
              class="row-action"
              @click.stop="$emit('view-slices', doc)"
            >
              {{ $t('knowledgeFileUpload.viewSlices') }}
            </el-button>
            <el-button
              v-else
              type="text"
              size="mini"
              class="row-action"
              :disabled="doc.parseStatusCode === 1"
              :loading="doc.parsing"
              @click.stop="handleParse(doc)"
            >
              {{ $t('knowledgeFileUpload.parse') }}
            </el-button>
            <el-button type="text" size="mini" class="row-action row-action--danger" :loading="deleteLoadingMap[doc.id]"
              @click.stop="handleDelete(doc)">
              {{ $t('knowledgeBaseManagement.delete') }}
            </el-button>
          </div>
        </div>
      </article>
      <div v-if="documents.length === 0 && !docLoading" class="doc-empty">
        <el-empty :description="$t('knowledgeBaseManagement.noData')"></el-empty>
      </div>
    </div>
    <CustomPagination
      class="pagination-wrapper"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      :page-size-options="pageSizeOptions"
      @page-change="handlePageChange"
      @size-change="handleSizeChange"
    />
  </section>
</template>

<script>
import Api from "@/apis/api";
import CustomPagination from "@/components/CustomPagination.vue";

export default {
  name: 'LibraryItem',
  components: { CustomPagination },
  props: {
    kb: {
      type: Object,
      default: null
    }
  },
  data() {
    return {
      documents: [],
      docLoading: false,
      deleteLoadingMap: {},
      searchDocName: '',
      total: 0,
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
    };
  },
  watch: {
    kb: {
      handler(newKb) {
        if (newKb && newKb.datasetId) {
          this.currentPage = 1;
          this.fetchDocuments();
        } else {
          this.documents = [];
          this.total = 0;
        }
      },
      immediate: true
    }
  },
  methods: {
    fetchDocuments() {
      if (!this.kb || !this.kb.datasetId) return;
      this.docLoading = true;
      const params = {
        page: this.currentPage,
        page_size: this.pageSize,
        name: this.searchDocName
      };
      Api.knowledgeBase.getDocumentList(
        this.kb.datasetId,
        params,
        ({ data }) => {
          this.docLoading = false;
          if (data && data.code === 0) {
            this.documents = data.data.list || [];
            this.total = data.data.total || 0;
            this.fetchSliceCountsForDocuments();
          } else {
            this.$message.error(data?.msg || 'Failed to fetch document list');
            this.documents = [];
            this.total = 0;
          }
        },
        (err) => {
          this.docLoading = false;
          if (err && err.data) {
            this.$message.error(err.data.msg || err.msg || 'Failed to fetch document list');
          } else {
            this.$message.error('Failed to fetch document list');
          }
          this.documents = [];
          this.total = 0;
        }
      );
    },

    handleSearch() {
      this.currentPage = 1;
      this.fetchDocuments();
    },

    handlePageChange(page) {
      this.currentPage = page;
      this.fetchDocuments();
    },

    handleSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
      this.fetchDocuments();
    },

    handleUpload() {
      this.$emit('upload');
    },

    handleRetrievalTest() {
      this.$emit('retrieval-test');
    },

    handleDelete(doc) {
      if (!this.kb) return;
      this.$confirm(
        this.$t('knowledgeFileUpload.confirmDelete'),
        this.$t('message.warning'),
        {
          confirmButtonText: this.$t('knowledgeBaseDialog.confirm'),
          cancelButtonText: this.$t('knowledgeBaseDialog.cancel'),
          type: 'warning'
        }
      ).then(() => {
        this.$set(this.deleteLoadingMap, doc.id, true);
        Api.knowledgeBase.deleteDocument(this.kb.datasetId, doc.id, (res) => {
          this.$set(this.deleteLoadingMap, doc.id, false);
          if (res.data && res.data.code === 0) {
            if (this.documents.length === 1 && this.currentPage > 1) {
              this.currentPage--;
            }
            this.fetchDocuments();
            this.$message.success(this.$t('knowledgeFileUpload.deleteSuccess'));
          } else {
            this.$message.error(res.data?.msg || this.$t('knowledgeFileUpload.deleteFailed'));
          }
        }, (err) => {
          this.$set(this.deleteLoadingMap, doc.id, false);
          this.$message.error(err?.data?.msg ||this.$t('knowledgeFileUpload.deleteFailed'));
        });
      }).catch(() => {});
    },

    handleParse(doc) {
      this.$confirm(this.$t('knowledgeFileUpload.confirmParse'), this.$t('message.info'), {
        confirmButtonText: this.$t('knowledgeFileUpload.confirm'),
        cancelButtonText: this.$t('knowledgeFileUpload.cancel'),
        type: 'info'
      }).then(() => {
        this.$set(doc, 'parsing', true);
        doc.parseStatusCode = 1;
        Api.knowledgeBase.parseDocument(
          this.kb.datasetId,
          doc.id,
          ({ data }) => {
            if (data && data.code === 0) {
              this.$message.success(this.$t('knowledgeFileUpload.parsing'));
              this.$set(doc, 'parsing', false);
              this.startParsePolling(doc);
            } else {
              this.$set(doc, 'parsing', false);
              doc.parseStatusCode = 0;
              this.$message.error(data?.msg || this.$t('knowledgeFileUpload.parseFailed'));
            }
          },
          (err) => {
            this.$set(doc, 'parsing', false);
            doc.parseStatusCode = 0;
            this.$message.error(err?.data?.msg || this.$t('knowledgeFileUpload.parseFailed'));
          }
        );
      }).catch(() => {});
    },

    startParsePolling(doc) {
      const poll = () => {
        if (!this.kb) return;
        Api.knowledgeBase.getDocumentList(
          this.kb.datasetId,
          { page: 1, page_size: 100, name: this.searchDocName },
          ({ data }) => {
            if (data && data.code === 0) {
              const list = data.data.list || [];
              const updated = list.find(d => d.id === doc.id);
              if (updated) {
                this.$set(doc, 'parseStatusCode', updated.parseStatusCode);
                if (updated.parseStatusCode !== 1) {
                  this.$set(doc, 'parsing', false);
                  if (updated.parseStatusCode === 3) {
                    this.$message.success(`「${doc.name}」${this.$t('knowledgeFileUpload.parseSuccess')}`);
                    this.fetchSliceCountForSingleDocument(doc);
                  } else if (updated.parseStatusCode === 4) {
                    this.$message.error(`「${doc.name}」${this.$t('knowledgeFileUpload.parseFailed')}`);
                  }
                  return;
                }
              }
              setTimeout(poll, 5000);
            }
          },
          () => {
            setTimeout(poll, 5000);
          }
        );
      };
      setTimeout(poll, 3000);
    },

    fetchSliceCountsForDocuments() {
      if (!this.documents || this.documents.length === 0) return;
      this.documents.forEach(doc => {
        this.fetchSliceCountForSingleDocument(doc);
      });
    },

    fetchSliceCountForSingleDocument(doc) {
      const params = { page: 1, page_size: 1 };
      Api.knowledgeBase.listChunks(
        this.kb.datasetId,
        doc.id,
        params,
        ({ data }) => {
          if (data && data.code === 0) {
            this.$set(doc, 'sliceCount', data.data.total || 0);
          }
        },
        () => {}
      );
    },

    getFileExt(fileName) {
      if (!fileName || !fileName.includes('.')) return 'DOC';
      return fileName.split('.').pop().slice(0, 4);
    },

    getStatusPillClass(doc) {
      const code = doc.parseStatusCode;
      if (code === 3) return 'status-pill--success';
      if (code === 1) return 'status-pill--warning';
      if (code === 4) return 'status-pill--danger';
      return 'status-pill--neutral';
    },

    getProgressClass(doc) {
      const code = doc.parseStatusCode;
      if (code === 3) return 'complete';
      if (code === 1) return 'processing';
      return 'pending';
    },

    getProgressOffset(doc) {
      const code = doc.parseStatusCode;
      const circumference = 94.25;
      if (code === 3) return 0;
      if (code === 1) {
        const progress = doc.chunkNum && doc.totalChunkNum
          ? Math.round((doc.chunkNum / doc.totalChunkNum) * 100)
          : 50;
        return circumference * (1 - progress / 100);
      }
      return circumference;
    },

    getProgressText(doc) {
      const code = doc.parseStatusCode;
      if (code === 3) return '100%';
      if (code === 1) {
        if (doc.chunkNum && doc.totalChunkNum) {
          return Math.round((doc.chunkNum / doc.totalChunkNum) * 100) + '%';
        }
        return '50%';
      }
      return '0%';
    },

    getProgressTextColor(doc) {
      const code = doc.parseStatusCode;
      if (code === 3) return 'var(--x-success-text)';
      if (code === 1) return 'var(--x-text)';
      return 'var(--x-text-muted)';
    },

    getDocStatusText(doc) {
      const code = doc.parseStatusCode;
      if (code === 3) return this.$t('knowledgeFileUpload.statusCompleted');
      if (code === 1) return this.$t('knowledgeFileUpload.statusProcessing');
      if (code === 0) return this.$t('knowledgeFileUpload.statusNotStarted');
      if (code === 4) return this.$t('knowledgeFileUpload.statusFailed');
      if (code === 2) return this.$t('knowledgeFileUpload.statusCancelled');
      return this.$t('knowledgeFileUpload.statusNotStarted');
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    },
  }
};
</script>

<style lang="scss" scoped>
/* ========== Document Section ========== */
.doc-section {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  display: flex;
  flex-direction: column;
  min-width: 0;
  text-align: left;
}

.doc-section-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--x-border);
}

.doc-section-heading {
  min-width: 0;
}

.doc-section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--x-text);
}

.doc-section-meta {
  margin: 2px 0 0;
  font-size: 13px;
  color: var(--x-text-muted);
  display: flex;
  flex-wrap: wrap;
  align-items: center;

  .doc-section-kb {
    color: var(--x-text-secondary);
    font-weight: 500;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }

  .meta-sep {
    padding: 0 6px;
  }
}

.doc-section-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;

  .el-button + .el-button {
    margin-left: 0;
  }
}

.doc-search-input {
  width: 220px;
  max-width: 100%;
}

/* ========== Document Grid ========== */
.doc-grid {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
  min-height: 160px;
}

.doc-card {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--x-border);
  border-radius: 12px;
  background: var(--x-surface);
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: border-color 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }
}

.doc-card-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.doc-file-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--x-divider);
  color: var(--x-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--x-font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.doc-card-info {
  flex: 1;
  min-width: 0;
}

.doc-card-name {
  margin: 0 0 2px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--x-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-card-meta {
  font-size: 12px;
  color: var(--x-text-muted);
}

.doc-card-bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--x-divider);
}

.doc-card-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

/* ========== Progress Ring ========== */
.progress-ring {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  position: relative;

  svg {
    width: 42px;
    height: 42px;
    transform: rotate(-90deg);
  }

  .bg {
    fill: none;
    stroke: var(--x-divider);
    stroke-width: 3;
  }

  .fg {
    fill: none;
    stroke-width: 3;
    stroke-linecap: round;
    transition: stroke-dashoffset 0.5s ease;

    &.complete { stroke: var(--x-success); }
    &.processing { stroke: var(--x-ink); }
    &.pending { stroke: var(--x-divider); }
  }
}

.progress-ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: var(--x-text-muted);
}

.progress-container {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;

  .doc-slice-count {
    font-size: 12px;
    color: var(--x-text-muted);

    > b {
      font-weight: 600;
      color: var(--x-text);
    }
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 1px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;

  &--success {
    background: var(--x-success-bg);
    color: var(--x-success-text);
  }

  &--warning {
    background: var(--x-warning-bg);
    color: var(--x-warning-text);
  }

  &--danger {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }

  &--neutral {
    background: var(--x-divider);
    color: var(--x-text-secondary);
  }
}

.doc-card-actions {
  display: flex;
  align-items: center;
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

  &.is-disabled,
  &.is-disabled:hover {
    color: var(--x-text-disabled);
    background: transparent;
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

.doc-empty {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 10px 16px;
  border-top: 1px solid var(--x-border);
  overflow-x: auto;
}

@media (max-width: 768px) {
  .doc-section-actions,
  .doc-search-input {
    width: 100%;
  }

  .doc-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }

  .pagination-wrapper {
    justify-content: flex-start;
  }
}
</style>