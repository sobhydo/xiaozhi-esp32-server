<template>
  <div class="welcome">
    <HeaderBar />
    <main class="kb-page" v-loading="loading" :element-loading-text="$t('knowledgeBaseManagement.loading')">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('knowledgeBaseManagement.title') }}</h1>
          <p class="page-description">{{ $t('console.knowledgeDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-input
            :placeholder="$t('knowledgeBaseManagement.searchPlaceholder')"
            :aria-label="$t('knowledgeBaseManagement.searchPlaceholder')"
            v-model="searchKbName"
            class="search-input"
            @keyup.enter.native="handleSearchKb"
            clearable
          >
            <button slot="prefix" type="button" class="search-icon-btn" :aria-label="$t('knowledgeBaseManagement.search')"
              @click="handleSearchKb">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
                stroke-linecap="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
            </button>
          </el-input>
          <el-button @click="handleSearchKb">{{ $t('knowledgeBaseManagement.search') }}</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">
            {{ $t('knowledgeBaseManagement.addKnowledgeBase') }}
          </el-button>
        </div>
      </header>

      <!-- Knowledge base cards -->
      <section v-if="filteredKnowledgeBases.length > 0" class="kb-strip" :aria-label="$t('knowledgeBaseManagement.title')">
        <button type="button" class="kb-arrow" :aria-label="$t('knowledgeBaseManagement.prevPage')" @click="scrollCards(-1)">
          <i class="el-icon-arrow-left" aria-hidden="true"></i>
        </button>
        <div class="kb-cards" ref="kbCards">
          <article
            v-for="kb in filteredKnowledgeBases"
            :key="kb.datasetId"
            class="kb-card"
            :class="{ active: selectedKb && selectedKb.datasetId === kb.datasetId, error: !!kb.errorMessage }"
            role="button"
            tabindex="0"
            :aria-pressed="selectedKb && selectedKb.datasetId === kb.datasetId ? 'true' : 'false'"
            @click="selectKnowledgeBase(kb)"
            @keydown.enter.self="selectKnowledgeBase(kb)"
          >
            <div class="kb-card-head">
              <div class="kb-initial" aria-hidden="true">{{ getKbInitial(kb) }}</div>
              <div class="kb-card-title">
                <h2 class="kb-card-name" :title="kb.name">{{ kb.name }}</h2>
                <div class="kb-card-meta">
                  <span>{{ $t('knowledgeBaseManagement.documentCount') }} {{ kb.documentCount || 0 }}</span>
                  <span v-if="kb.createdAt" class="meta-sep" aria-hidden="true">·</span>
                  <span>{{ formatDate(kb.createdAt) }}</span>
                </div>
              </div>
              <div class="kb-card-tools">
                <button type="button" class="icon-btn" :aria-label="$t('knowledgeBaseManagement.edit')"
                  :title="$t('knowledgeBaseManagement.edit')" @click.stop="editKnowledgeBase(kb)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                    stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4" />
                  </svg>
                </button>
                <button type="button" class="icon-btn icon-btn--danger" :aria-label="$t('knowledgeBaseManagement.delete')"
                  :title="$t('knowledgeBaseManagement.delete')" @click.stop="deleteKnowledgeBase(kb)">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                    stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />
                  </svg>
                </button>
              </div>
            </div>
            <p class="kb-card-desc">{{ kb.description || '-' }}</p>
            <div class="kb-card-foot">
              <div class="kb-status" @click.stop>
                <el-switch
                  v-model="kb.status"
                  :active-value="1"
                  :inactive-value="0"
                  @change="handleStatusChange(kb)"
                ></el-switch>
                <span class="kb-status-text">{{ getStatusText(kb) }}</span>
              </div>
              <el-tooltip v-if="kb.errorMessage" :content="kb.errorMessage" placement="top-end" effect="dark">
                <span class="status-pill status-pill--warning kb-warning" tabindex="0" @click.stop>
                  <i class="el-icon-warning" aria-hidden="true"></i>
                  <span class="kb-warning-text">{{ kb.errorMessage }}</span>
                </span>
              </el-tooltip>
            </div>
          </article>
        </div>
        <button type="button" class="kb-arrow" :aria-label="$t('knowledgeBaseManagement.nextPage')" @click="scrollCards(1)">
          <i class="el-icon-arrow-right" aria-hidden="true"></i>
        </button>
      </section>
      <section v-else-if="!loading" class="kb-empty">
        <el-empty :description="$t('knowledgeBaseManagement.noData')"></el-empty>
      </section>

      <!-- Document section -->
      <KnowledgeBaseItem
        ref="knowledgeBaseItem"
        v-if="selectedKb"
        :kb="selectedKb"
        @upload="showUploadDialog"
        @retrieval-test="showRetrievalTestDialog"
        @view-slices="handleViewSlices"
        @refresh="refreshDocuments"
      />
    </main>

    <!-- Knowledge Base Dialog -->
    <KnowledgeBaseDialog
      ref="knowledgeBaseDialog"
      :title="dialogTitle"
      :visible.sync="dialogVisible"
      :form="knowledgeBaseForm"
      @submit="handleSubmit"
      @cancel="dialogVisible = false"
    />

    <!-- Slice Dialog -->
    <CustomDialog
      class="kb-dialog"
      :title="`${$t('knowledgeFileUpload.viewSlices')} - ${currentDocumentName}`"
      :visible.sync="sliceDialogVisible"
      width="1200px"
      size="large"
      :footer="false"
    >
      <div class="slice-management">
        <div v-loading="sliceLoading" class="slice-content-container">
          <ol v-if="sliceList.length > 0" class="slice-list">
            <li v-for="(slice, index) in sliceList" :key="index" class="slice-card">
              <span class="slice-index">{{ $t('knowledgeFileUpload.slice') }} {{ (sliceCurrentPage - 1) * slicePageSize + index + 1 }}</span>
              <div class="content-text">{{ slice.content }}</div>
            </li>
          </ol>
          <div v-else class="no-slice-data">
            <el-empty :description="$t('knowledgeFileUpload.noSliceData')"></el-empty>
          </div>
        </div>
        <div class="slice-pagination">
          <CustomPagination
            :total="parseInt(sliceTotal)"
            :current-page="sliceCurrentPage"
            :page-size="slicePageSize"
            :page-size-options="[10, 20, 50]"
            @size-change="handleSliceSizeChange"
            @page-change="handleSlicePageChange"
          />
        </div>
      </div>
    </CustomDialog>

    <!-- Upload Dialog -->
    <CustomDialog
      class="kb-dialog"
      :title="$t('knowledgeFileUpload.uploadDocument')"
      :visible.sync="uploadDialogVisible"
      width="800px"
      :close-on-click-modal="false"
      :confirmLoading="uploading"
      @close="handleUploadDialogClose"
      @confirm="handleBatchUploadSubmit"
    >
      <el-upload
        ref="uploadRef"
        class="kb-upload"
        action="#"
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
        multiple
        accept=".doc,.docx,.pdf,.txt,.md,.mdx,.csv,.xls,.xlsx,.ppt,.pptx"
        drag
      >
        <span class="upload-icon" aria-hidden="true">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 16V4M7 9l5-5 5 5M4 16v2a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-2" />
          </svg>
        </span>
        <div class="el-upload__text">{{ $t('knowledgeFileUpload.dragOrClick') }}</div>
        <div class="el-upload__tip" slot="tip">{{ $t('knowledgeFileUpload.uploadTip') }}</div>
      </el-upload>
      <!-- Selected file list -->
      <div class="selected-files-section" v-if="selectedFilesList.length > 0">
        <h4>{{ $t('knowledgeFileUpload.selectedFiles') }} ({{ selectedFilesList.length }})</h4>
        <ul class="selected-files-list">
          <li v-for="(file, index) in selectedFilesList" :key="index" class="selected-file-item">
            <div class="file-info">
              <i class="el-icon-document" aria-hidden="true"></i>
              <span class="file-name" :title="file.name">{{ file.name }}</span>
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
            </div>
            <button type="button" class="icon-btn icon-btn--danger" :aria-label="$t('knowledgeFileUpload.delete')"
              @click="removeSelectedFile(index)">
              <i class="el-icon-close" aria-hidden="true"></i>
            </button>
          </li>
        </ul>
      </div>
    </CustomDialog>

    <!-- Retrieval Test Dialog -->
    <CustomDialog
      class="kb-dialog"
      :title="$t('knowledgeFileUpload.retrievalTest')"
      :visible.sync="retrievalTestDialogVisible"
      width="1200px"
      size="large"
      :close-on-click-modal="false"
      :confirm-text="$t('knowledgeFileUpload.executeTest')"
      :confirmLoading="retrievalTestLoading"
      @confirm="runRetrievalTest"
    >
      <div class="retrieval-test-form">
        <el-form :model="retrievalTestForm" label-position="top" @submit.native.prevent>
          <el-form-item :label="$t('knowledgeFileUpload.testQuestion')" required>
            <el-input
              v-model="retrievalTestForm.question"
              @keyup.enter.native="runRetrievalTest"
              clearable
              :placeholder="$t('knowledgeFileUpload.testQuestionPlaceholder')"
            />
          </el-form-item>
        </el-form>
        <div v-if="retrievalTestResult" class="retrieval-test-result">
          <ol class="result-chunks" v-if="retrievalTestResult.chunks.length">
            <li v-for="(chunk, index) in retrievalTestResult.chunks" :key="index" class="result-chunk">
              <div class="chunk-similarity">
                <p class="similarity-label">{{ $t('knowledgeFileUpload.comprehensiveSimilarity') }}</p>
                <p class="similarity-value">{{ (chunk.similarity || 0).toFixed(4) }}</p>
                <el-progress
                  :percentage="Math.round((chunk.similarity || 0) * 100)"
                  :stroke-width="6"
                  :show-text="false"
                  class="similarity-progress"
                />
              </div>
              <div class="chunk-content">
                <div class="chunk-header">
                  <span class="chunk-source">{{ $t('knowledgeFileUpload.sourceDocument') }}: {{ chunk.document_keyword || '-' }}</span>
                  <span class="slice-index">{{ $t('knowledgeFileUpload.slice') }} {{ index + 1 }}</span>
                </div>
                <p>{{ chunk.content }}</p>
              </div>
            </li>
          </ol>
          <el-empty v-else :description="$t('knowledgeFileUpload.noSliceData')"></el-empty>
        </div>
      </div>
    </CustomDialog>

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import KnowledgeBaseDialog from "@/components/KnowledgeBaseDialog.vue";
import KnowledgeBaseItem from "./KnowledgeBaseItem.vue";
import CustomDialog from "@/components/CustomDialog.vue";
import CustomPagination from "@/components/CustomPagination.vue";

export default {
  components: { HeaderBar, VersionFooter, KnowledgeBaseDialog, KnowledgeBaseItem, CustomDialog, CustomPagination },
  data() {
    return {
      knowledgeBases: [],
      selectedKb: null,
      searchKbName: '',
      loading: false,
      dialogVisible: false,
      dialogTitle: '',
      knowledgeBaseForm: {
        id: null,
        datasetId: null,
        name: '',
        description: '',
        status: 1
      },
      uploadDialogVisible: false,
      sliceDialogVisible: false,
      retrievalTestDialogVisible: false,
      retrievalTestForm: {
        question: ''
      },
      retrievalTestResult: null,
      retrievalTestLoading: false,
      selectedFilesList: [],
      uploading: false,
      currentDocumentId: '',
      currentDocumentName: '',
      sliceList: [],
      sliceLoading: false,
      sliceCurrentPage: 1,
      slicePageSize: 10,
      sliceTotal: 0,
    };
  },
  computed: {
    filteredKnowledgeBases() {
      return this.knowledgeBases;
    },
  },
  created() {
    this.fetchKnowledgeBases();
  },
  methods: {
    fetchKnowledgeBases() {
      this.loading = true;
      Api.knowledgeBase.getKnowledgeBaseList(
        { page: 1, page_size: 100, name: this.searchKbName },
        (res) => {
          this.loading = false;
          if (res.data && res.data.code === 0) {
            const pageData = res.data.data || {};
            this.knowledgeBases = pageData.list || [];
            if (this.knowledgeBases.length > 0 && !this.selectedKb) {
              this.selectKnowledgeBase(this.knowledgeBases[0]);
            } else if (this.selectedKb) {
              const updated = this.knowledgeBases.find(kb => kb.datasetId === this.selectedKb.datasetId);
              if (updated) this.selectedKb = updated;
            }
          } else {
            this.$message.error(res.data?.msg || this.$t('knowledgeBaseManagement.getListFailed'));
          }
        },
        () => {
          this.loading = false;
          this.$message.error(this.$t('knowledgeBaseManagement.getListFailed'));
        }
      );
    },

    selectKnowledgeBase(kb) {
      this.selectedKb = kb;
    },

    refreshDocuments() {
      this.$refs.knowledgeBaseItem?.fetchDocuments();
    },

    handleSearchKb() {
      this.fetchKnowledgeBases();
    },

    scrollCards(direction) {
      const container = this.$refs.kbCards;
      if (container) {
        container.scrollBy({ left: direction * 364, behavior: 'smooth' });
      }
    },

    showAddDialog() {
      this.dialogTitle = this.$t('knowledgeBaseManagement.addKnowledgeBase');
      this.knowledgeBaseForm = {
        id: null,
        datasetId: null,
        name: '',
        description: '',
        status: 1
      };
      this.dialogVisible = true;
    },

    editKnowledgeBase(kb) {
      this.dialogTitle = this.$t('knowledgeBaseManagement.editKnowledgeBase');
      this.knowledgeBaseForm = {
        id: kb.id,
        datasetId: kb.datasetId,
        name: kb.name,
        description: kb.description || '',
        status: kb.status,
        ragModelId: kb.ragModelId
      };
      this.dialogVisible = true;
    },

    handleStatusChange(kb) {
      const updateForm = {
        id: kb.id,
        datasetId: kb.datasetId,
        name: kb.name,
        description: kb.description,
        status: kb.status
      };
      Api.knowledgeBase.updateKnowledgeBase(kb.datasetId, updateForm, (res) => {
        if (res.data && res.data.code !== 0) {
          this.fetchKnowledgeBases();
          this.$message.error(res.data?.msg || this.$t('knowledgeBaseManagement.updateFailed'));
        } else {
          this.$message.success(kb.status === 1 ? this.$t('knowledgeBaseManagement.enabled') : this.$t('knowledgeBaseManagement.disabled'));
          if (this.selectedKb && this.selectedKb.datasetId === kb.datasetId) {
            this.selectedKb = { ...kb };
          }
        }
      }, () => {
        this.fetchKnowledgeBases();
        this.$message.error(this.$t('knowledgeBaseManagement.updateFailed'));
      });
    },

    deleteKnowledgeBase(kb) {
      this.$confirm(
        this.$t("knowledgeBaseManagement.confirmBatchDelete", {count: 1}),
        this.$t('message.warning'),
        {
          confirmButtonText: this.$t('knowledgeBaseDialog.confirm'),
          cancelButtonText: this.$t('knowledgeBaseDialog.cancel'),
          type: 'warning'
        }
      ).then(() => {
        Api.knowledgeBase.deleteKnowledgeBase(kb.datasetId, (res) => {
          if (res.data && res.data.code === 0) {
            this.$message.success(this.$t('knowledgeBaseManagement.batchDeleteSuccess', { count: 1 }));
            if (this.selectedKb && this.selectedKb.datasetId === kb.datasetId) {
              this.selectedKb = null;
            }
            this.fetchKnowledgeBases();
          } else {
            this.$message.error(res.data?.msg || this.$t('knowledgeBaseManagement.deleteFailed'));
          }
        }, (err) => {
          this.$message.error(err?.data?.msg || this.$t('knowledgeBaseManagement.deleteFailed'));
        });
      }).catch(() => {});
    },

    showUploadDialog() {
      this.uploadDialogVisible = true;
    },

    handleSubmit(form) {
      if (form.id) {
        Api.knowledgeBase.updateKnowledgeBase(form.datasetId, form, (res) => {
          if (res.data && res.data.code === 0) {
            this.dialogVisible = false;
            this.fetchKnowledgeBases();
            this.$message.success(this.$t('knowledgeBaseManagement.updateSuccess'));
          } else {
            this.$message.error(res.data?.msg || this.$t('knowledgeBaseManagement.updateFailed'));
          }
        }, (err) => {
          this.$message.error(err?.data?.msg || this.$t('knowledgeBaseManagement.updateFailed'));
        });
      } else {
        const createData = {
          name: form.name,
          description: form.description,
          status: form.status,
          ragModelId: form.ragModelId
        };
        Api.knowledgeBase.createKnowledgeBase(createData, (res) => {
          if (res.data && res.data.code === 0) {
            this.dialogVisible = false;
            this.fetchKnowledgeBases();
            this.$message.success(this.$t('knowledgeBaseManagement.addSuccess'));
          } else {
            this.$message.error(res.data?.msg || this.$t('knowledgeBaseManagement.addFailed'));
          }
        }, (err) => {
          this.$message.error(err?.data?.msg || this.$t('knowledgeBaseManagement.addFailed'));
        });
      }
    },

    handleUploadDialogClose() {
      if (this.$refs.uploadRef) {
        this.$refs.uploadRef.clearFiles();
      }
      this.selectedFilesList = [];
    },

    handleFileChange(file) {
      if (!file || !file.raw) return;
       // Validation before file upload
      const isLt10M = file.size / 1024 / 1024 < 10;
      if (!isLt10M) {
        this.$message.error(this.$t('knowledgeFileUpload.fileSizeExceeded'));
        return;
      }
      this.selectedFilesList.push({
        name: file.name,
        size: file.size,
        raw: file.raw
      });
    },

    removeSelectedFile(index) {
      this.selectedFilesList.splice(index, 1);
    },

    formatFileSize(bytes) {
      if (bytes === 0) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    handleBatchUploadSubmit() {
      if (this.selectedFilesList.length === 0) {
        this.$message.error(this.$t('knowledgeFileUpload.fileRequired'));
        return;
      }

      this.uploading = true;

      const uploadPromises = this.selectedFilesList.map(file => {
        return new Promise((resolve, reject) => {
          const formData = new FormData();
          formData.append('file', file.raw);

          Api.knowledgeBase.uploadDocument(this.selectedKb.datasetId, formData,
            ({ data }) => {
              if (data && data.code === 0) {
                resolve({ success: true, fileName: file.name });
              } else {
                reject({ success: false, fileName: file.name, error: data?.msg || this.$t('knowledgeFileUpload.uploadFailed') });
              }
            },
            (err) => {
              if (err && err.data) {
                reject({ success: false, fileName: file.name, error: err.data.msg || err.msg || this.$t('knowledgeFileUpload.uploadFailed') });
              } else {
                reject({ success: false, fileName: file.name, error: this.$t('knowledgeFileUpload.uploadFailed') });
              }
              console.error('Failed to upload document:', err);
            }
          );
        });
      });

      Promise.all(uploadPromises.map(p => p.catch(e => e)))
        .then(results => {
          this.uploading = false;

          const successCount = results.filter(r => r.success).length;
          const failedCount = results.filter(r => !r.success).length;

          if (successCount > 0) {
            this.$message.success(this.$t('knowledgeFileUpload.uploadSuccess'));
          }

          if (failedCount > 0) {
            this.$message.error(this.$t('knowledgeFileUpload.uploadFailed'));
          }

          if (successCount > 0) {
            this.uploadDialogVisible = false;
            this.refreshDocuments();
          }
        })
        .catch(error => {
          this.uploading = false;
          this.$message.error(this.$t('knowledgeFileUpload.uploadFailed'));
          console.error(error);
        });
    },

    showRetrievalTestDialog() {
      this.retrievalTestForm.question = '';
      this.retrievalTestResult = null;
      this.retrievalTestDialogVisible = true;
    },

    runRetrievalTest() {
      if (this.retrievalTestLoading) return;
      if (!this.retrievalTestForm.question.trim()) {
        this.$message.error(this.$t('knowledgeFileUpload.testQuestionRequired'));
        return;
      }
      this.retrievalTestLoading = true;
      Api.knowledgeBase.retrievalTest(
        this.selectedKb.datasetId,
        { question: this.retrievalTestForm.question.trim() },
        ({ data }) => {
          this.retrievalTestLoading = false;
          if (data && data.code === 0) {
            this.retrievalTestResult = data.data || data;
          } else {
            this.$message.error(data?.msg || 'Retrieval test failed');
          }
        },
        (err) => {
          this.retrievalTestLoading = false;
          this.$message.error(err?.data?.msg || 'Retrieval test failed');
        }
      );
    },

    handleViewSlices(doc) {
      this.currentDocumentId = doc.id;
      this.currentDocumentName = doc.name;
      this.sliceDialogVisible = true;
      this.sliceCurrentPage = 1;
      this.fetchSlices();
    },

    fetchSlices() {
      if (!this.selectedKb || !this.currentDocumentId) return;
      this.sliceLoading = true;
      const params = {
        page: this.sliceCurrentPage,
        page_size: this.slicePageSize
      };
      Api.knowledgeBase.listChunks(
        this.selectedKb.datasetId,
        this.currentDocumentId,
        params,
        ({ data }) => {
          this.sliceLoading = false;
          if (data && data.code === 0) {
            const responseData = data.data;
            if (responseData && responseData.list) {
              this.sliceList = responseData.list;
              this.sliceTotal = responseData.total || responseData.list.length;
            } else if (responseData && responseData.chunks && Array.isArray(responseData.chunks)) {
              this.sliceList = responseData.chunks;
              this.sliceTotal = responseData.total || responseData.chunks.length;
            } else if (Array.isArray(responseData)) {
              this.sliceList = responseData;
              this.sliceTotal = responseData.length;
            } else {
              this.sliceList = [];
              this.sliceTotal = 0;
            }
          } else {
            this.$message.error(data?.msg || this.$t('knowledgeBaseManagement.getListFailed'));
            this.sliceList = [];
            this.sliceTotal = 0;
          }
        },
        (err) => {
          this.sliceLoading = false;
          if (err && err.data) {
            this.$message.error(err.data.msg || err.msg || this.$t('knowledgeBaseManagement.getListFailed'));
          } else {
            this.$message.error(this.$t('knowledgeBaseManagement.getListFailed'));
          }
          this.sliceList = [];
          this.sliceTotal = 0;
        }
      );
    },

    handleSliceSizeChange(val) {
      this.slicePageSize = val;
      this.sliceCurrentPage = 1;
      this.fetchSlices();
    },

    handleSlicePageChange(page) {
      this.sliceCurrentPage = page;
      this.fetchSlices();
    },

    getKbInitial(kb) {
      const name = (kb.name || '').trim();
      return name ? Array.from(name)[0].toUpperCase() : '?';
    },

    getStatusClass(kb) {
      return kb.status === 1 ? 'active' : 'inactive';
    },

    getStatusText(kb) {
      return kb.status === 1 ? this.$t('knowledgeBaseManagement.enabled') : this.$t('knowledgeBaseManagement.disabled');
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
    },

    formatFileSize(bytes) {
      if (!bytes) return '0 B';
      const units = ['B', 'KB', 'MB', 'GB'];
      let i = 0;
      let size = bytes;
      while (size >= 1024 && i < units.length - 1) {
        size /= 1024;
        i++;
      }
      return size.toFixed(1) + ' ' + units[i];
    },
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--x-canvas);
  text-align: start;
}

.kb-page {
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

.page-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.search-input {
  width: 280px;
  max-width: 100%;

  ::v-deep .el-input__inner {
    padding-inline-start: 34px;
  }

  ::v-deep .el-input__prefix {
    left: 0;
    display: flex;
    align-items: center;
  }
}

.search-icon-btn {
  width: 34px;
  height: 100%;
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

/* ---------- Knowledge base cards ---------- */
.kb-strip {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.kb-cards {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 2px;
  scroll-behavior: smooth;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.kb-arrow {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  padding: 0;
  border-radius: 50%;
  background: var(--x-surface);
  border: 1px solid var(--x-border-control);
  color: var(--x-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.15s ease, color 0.15s ease;

  &:hover,
  &:focus-visible {
    background: var(--x-surface-raised);
    color: var(--x-ink);
  }
}

.kb-card {
  flex: 0 0 300px;
  width: 300px;
  min-width: 0;
  box-sizing: border-box;
  padding: 16px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }

  &:focus-visible {
    box-shadow: 0 0 0 4px var(--x-focus-ring);
  }

  &.active {
    border-color: var(--x-ink);
    box-shadow: inset 0 0 0 1px var(--x-ink);
  }

  &.error {
    border-color: var(--x-warning);
  }

  &.error.active {
    box-shadow: inset 0 0 0 1px var(--x-warning);
  }
}

.kb-card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.kb-initial {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--x-divider);
  color: var(--x-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 15px;
}

.kb-card.active .kb-initial {
  background: var(--x-ink);
  color: var(--x-on-ink);
}

.kb-card-title {
  flex: 1;
  min-width: 0;
}

.kb-card-name {
  margin: 0 0 2px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--x-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-card-meta {
  display: flex;
  flex-wrap: wrap;
  font-size: 12px;
  line-height: 18px;
  color: var(--x-text-muted);

  .meta-sep {
    padding: 0 6px;
  }
}

.kb-card-tools {
  display: flex;
  gap: 2px;
  margin: -4px -6px 0 0;
}

.icon-btn {
  width: 30px;
  height: 30px;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--x-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &--danger:hover,
  &--danger:focus-visible {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);
  }
}

.kb-card-desc {
  margin: 0;
  min-height: 39px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  word-break: break-word;
}

.kb-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--x-divider);
  min-height: 24px;
}

.kb-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: default;
}

.kb-status-text {
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

  &--warning {
    background: var(--x-warning-bg);
    color: var(--x-warning-text);

    i {
      color: var(--x-warning);
    }
  }
}

.kb-warning {
  min-width: 0;
  max-width: 150px;
  cursor: help;
  outline: none;

  &:focus-visible {
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.kb-warning-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-empty {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  padding: 24px;
}

/* ---------- Dialogs ---------- */
.kb-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.slice-management {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.slice-content-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.slice-list,
.result-chunks,
.selected-files-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.slice-card {
  padding: 14px 16px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: 12px;
  text-align: start;

  & + & {
    margin-top: 10px;
  }
}

.slice-index {
  display: inline-block;
  margin-bottom: 8px;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;
}

.content-text {
  font-size: 14px;
  line-height: 1.6;
  color: var(--x-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.no-slice-data {
  flex: 1;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.slice-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* Upload */
.kb-upload {
  ::v-deep .el-upload,
  ::v-deep .el-upload-dragger {
    width: 100%;
  }

  ::v-deep .el-upload-dragger {
    height: auto;
    padding: 28px 16px;
    box-sizing: border-box;
    border: 1px dashed var(--x-border-control);
    border-radius: var(--x-radius-card);
    background: var(--x-surface-raised);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    transition: border-color 0.15s ease, background-color 0.15s ease;

    &:hover,
    &.is-dragover {
      border-color: var(--x-ink);
      background: var(--x-surface);
    }
  }

  ::v-deep .el-upload__text {
    font-size: 14px;
    color: var(--x-text);
  }

  ::v-deep .el-upload__tip {
    margin-top: 8px;
    font-size: 12px;
    line-height: 1.5;
    color: var(--x-text-muted);
    text-align: start;
  }
}

.upload-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  color: var(--x-text);
  display: flex;
  align-items: center;
  justify-content: center;
}

.selected-files-section {
  margin-top: 16px;
  text-align: start;

  h4 {
    margin: 0 0 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--x-text);
  }
}

.selected-files-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid var(--x-border);
  border-radius: 12px;
}

.selected-file-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px 6px 12px;

  & + & {
    border-top: 1px solid var(--x-divider);
  }

  .file-info {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;

    .el-icon-document {
      color: var(--x-text-muted);
      font-size: 16px;
    }

    .file-name {
      font-size: 14px;
      color: var(--x-text);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      min-width: 0;
    }

    .file-size {
      flex-shrink: 0;
      font-size: 12px;
      color: var(--x-text-muted);
      font-family: var(--x-font-mono);
    }
  }
}

/* Retrieval test */
.retrieval-test-form {
  display: flex;
  flex-direction: column;
  text-align: start;

  ::v-deep .el-form-item {
    margin-bottom: 8px;
  }

  ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    line-height: 20px;
    font-size: 13px;
  }
}

.retrieval-test-result {
  margin-top: 12px;
}

.result-chunk {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 16px;
  border: 1px solid var(--x-border);
  border-radius: 12px;

  & + & {
    margin-top: 10px;
  }
}

.chunk-similarity {
  flex: 0 0 160px;
  padding-inline-end: 20px;
  border-inline-end: 1px solid var(--x-divider);

  .similarity-label {
    margin: 0;
    font-size: 12px;
    color: var(--x-text-muted);
  }

  .similarity-value {
    margin: 4px 0 10px;
    font-family: var(--x-font-mono);
    font-size: 18px;
    font-weight: 600;
    color: var(--x-text);
  }

  .similarity-progress {
    ::v-deep .el-progress-bar__outer {
      background-color: var(--x-divider);
    }

    ::v-deep .el-progress-bar__inner {
      background: var(--x-ink);
    }
  }
}

.chunk-content {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  line-height: 1.6;
  color: var(--x-text);

  > p {
    margin: 0;
    word-break: break-word;
  }
}

.chunk-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .slice-index {
    margin-bottom: 0;
  }
}

.chunk-source {
  font-size: 13px;
  font-weight: 600;
  color: var(--x-text);
  min-width: 0;
  word-break: break-all;
}

@media (max-width: 768px) {
  .kb-page {
    padding: 16px 16px 0;
    gap: 16px;
  }

  .page-actions,
  .search-input {
    width: 100%;
  }

  .kb-arrow {
    display: none;
  }

  .kb-card {
    flex-basis: 280px;
    width: 280px;
  }

  .result-chunk {
    flex-direction: column;
    gap: 12px;
  }

  .chunk-similarity {
    flex: none;
    width: 100%;
    padding-inline-end: 0;
    border-inline-end: 0;
  }
}
</style>
