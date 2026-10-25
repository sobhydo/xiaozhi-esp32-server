<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('voiceClone.title') }}</h1>
          <p class="page-description">{{ $t('console.voiceCloneDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-input :placeholder="$t('voiceClone.searchPlaceholder')" :aria-label="$t('voiceClone.searchPlaceholder')"
            v-model="searchName" class="search-input" prefix-icon="el-icon-search"
            @keyup.enter.native="handleSearch" clearable />
          <el-button @click="handleSearch">{{ $t('voiceClone.search') }}</el-button>
        </div>
      </header>

      <section class="list-card">
        <div v-loading="loading" class="voice-clone-grid">
          <article v-for="item in voiceCloneList" :key="item.id" class="voice-clone-card">
            <div class="card-head">
              <div class="voice-icon" aria-hidden="true">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round">
                  <rect x="9" y="3" width="6" height="11" rx="3" />
                  <path d="M5 11a7 7 0 0 0 14 0M12 18v3" />
                </svg>
              </div>
              <div class="info-title">
                <el-input v-show="item.isEdit" v-model="item.name" size="mini" maxlength="64"
                  show-word-limit :aria-label="$t('voiceClone.name')" @keyup.enter.native="onNameEnter(item)"
                  ref="nameInput" />
                <h2 v-show="!item.isEdit" class="name-text" :title="item.name">{{ item.name || '-' }}</h2>
              </div>
              <el-tooltip :content="getTooltipContent(item)" placement="top" effect="light">
                <span class="status-pill" :class="getStatusButtonClass(item)" tabindex="0">
                  <span class="status-pill-dot" aria-hidden="true"></span>{{ getTrainStatusText(item) }}
                </span>
              </el-tooltip>
            </div>

            <dl class="info-grid">
              <dt>{{ $t('voiceClone.voiceId') }}</dt>
              <dd class="mono" :title="item.voiceId">{{ item.voiceId || '-' }}</dd>
              <dt>{{ $t('voiceClone.languages') }}</dt>
              <dd>{{ item.languages || '-' }}</dd>
            </dl>

            <div v-if="item.hasVoice" class="voice-wave" :class="{ 'is-playing': playingRowId === item.id }"
              aria-hidden="true">
              <span v-for="n in 24" :key="n" class="wave-bar"></span>
            </div>

            <div class="card-actions">
              <el-button v-if="item.hasVoice" size="mini" type="text" class="row-action"
                :icon="playingRowId === item.id ? 'el-icon-video-pause' : 'el-icon-video-play'"
                @click="handlePlay(item)">
                {{ playingRowId === item.id ? $t('voiceClone.stop') : $t('voiceClone.play') }}
              </el-button>
              <el-button size="mini" type="text" class="row-action" icon="el-icon-upload2" @click="handleUpload(item)">
                {{ $t('voiceClone.upload') }}
              </el-button>
              <el-button v-if="item.hasVoice" size="mini" type="text" class="row-action" icon="el-icon-copy-document"
                @click="handleClone(item)" :loading="item._cloning">
                {{ $t('voiceClone.clone') }}
              </el-button>
              <el-button size="mini" type="text" class="row-action"
                :icon="item.isEdit ? 'el-icon-check' : 'el-icon-edit'"
                @click="handleEditButtonClick(item)">
                {{ item.isEdit ? $t('button.save') : $t('common.edit') }}
              </el-button>
            </div>
          </article>

          <div v-if="!loading && voiceCloneList.length === 0" class="empty-state">
            {{ $t('voiceClone.noVoiceCloneAssigned') }}
          </div>
        </div>

        <div class="card-footer">
          <CustomPagination
            :total="total"
            :current-page="currentPage"
            :page-size="pageSize"
            :page-size-options="pageSizeOptions"
            @size-change="handlePageSizeChange"
            @page-change="goToPage"
          />
        </div>
      </section>
    </main>

    <el-footer>
      <version-footer />
    </el-footer>

    <VoiceCloneDialog :visible.sync="cloneDialogVisible" :voiceCloneData="currentVoiceClone"
      @success="handleCloneSuccess" />
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import VoiceCloneDialog from "@/components/VoiceCloneDialog.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomPagination from "@/components/CustomPagination.vue";
import { formatDate } from "@/utils/format";

export default {
  components: { HeaderBar, VersionFooter, VoiceCloneDialog, CustomButton, CustomPagination },
  data() {
    return {
      searchName: "",
      loading: false,
      voiceCloneList: [],
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      total: 0,
      cloneDialogVisible: false,
      currentVoiceClone: {},
      voiceCloneForm: {
        modelId: "",
        voiceIds: [],
        userId: null
      },
      currentAudio: null,
      playingRowId: null
    };
  },
  created() {
    this.fetchVoiceCloneList();
  },
  methods: {
    getTooltipContent(row) {
      if (!row.hasVoice) {
        return this.$t('voiceClone.waitingUpload');
      }
      switch (row.trainStatus) {
        case 0:
          return this.$t('voiceClone.waitingTraining');
        case 2:
          return this.$t('voiceClone.trainSuccess');
        case 3:
          if (row.trainError) {
            return this.$t('voiceClone.trainFailedWithError', { error: row.trainError });
          }
          return this.$t('voiceClone.trainFailed');
        default:
          return '';
      }
    },
    handleViewDetails(row) {
      console.log('View details:', row);
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchVoiceCloneList();
    },
    goToPage(page) {
      if (page !== this.currentPage) {
        this.currentPage = page;
        this.fetchVoiceCloneList();
      }
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
      Api.voiceClone.getVoiceCloneList(params, (res) => {
        this.loading = false;
        res = res.data;
        if (res.code === 0) {
          this.voiceCloneList = res.data.list;
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
    formatDate,
    getTrainStatusText(row) {
      if (!row.hasVoice) {
        return this.$t('voiceClone.waitingUpload');
      }
      switch (row.trainStatus) {
        case 0:
          return this.$t('voiceClone.waitingTraining');
        case 2:
          return this.$t('voiceClone.trainSuccess');
        case 3:
          return this.$t('voiceClone.trainFailed');
        default:
          return '';
      }
    },
    getStatusButtonClass(row) {
      if (!row.hasVoice || row.trainStatus === 0) {
        return 'status-waiting';
      } else if (row.trainStatus === 2) {
        return 'status-success';
      } else if (row.trainStatus === 3) {
        return 'status-failed';
      }
      return '';
    },
    handleClone(row) {
      if (row._cloning) {
        return;
      }
      this.$set(row, '_cloning', true);
      const params = { cloneId: row.id };
      try {
        Api.voiceClone.cloneAudio(params, (res) => {
          try {
            res = res.data;
            if (res.code === 0) {
              this.$message.success(this.$t('message.success'));
              this.fetchVoiceCloneList();
            } else {
              this.$message.error(res.msg || this.$t('message.error'));
              this.fetchVoiceCloneList();
            }
          } catch (error) {
            console.error('Error processing response:', error);
            this.$message.error('Error processing response');
            this.fetchVoiceCloneList();
          } finally {
            this.$set(row, '_cloning', false);
          }
        }, (error) => {
          console.error('API call failed:', error);
          this.$message.error(this.$t('voiceClone.cloneErrorTip'));
          this.fetchVoiceCloneList();
          this.$set(row, '_cloning', false);
        });
      } catch (error) {
        console.error('Error calling API:', error);
        this.$message.error(this.$t('voiceClone.apiError'));
        this.fetchVoiceCloneList();
        this.$set(row, '_cloning', false);
      }
    },
    handleCloneSuccess() {
      this.fetchVoiceCloneList();
    },
    handleEditName(row) {
      this.$set(row, 'isEdit', true);
      this.$nextTick(() => {
        const input = this.$refs.nameInput;
        if (input) {
          if (Array.isArray(input)) {
            const idx = this.voiceCloneList.indexOf(row);
            if (input[idx]) {
              input[idx].focus();
            }
          } else {
            input.focus();
          }
        }
      });
    },
    submitName(row) {
      if (row._submitting) {
        return;
      }
      row._submitting = true;
      const params = { id: row.id, name: row.name };
      Api.voiceClone.updateName(params, (res) => {
        res = res.data;
        if (res.code === 0) {
          this.$message.success(this.$t('voiceClone.updateNameSuccess') || 'Name updated successfully');
        } else {
          this.$message.error(res.msg || this.$t('voiceClone.updateNameFailed') || 'Failed to update name');
          this.fetchVoiceCloneList();
        }
        row._submitting = false;
      });
    },
    handleEditButtonClick(row) {
      if (row.isEdit) {
        row.isEdit = false;
        this.submitName(row);
      } else {
        this.handleEditName(row);
      }
    },
    onNameEnter(row) {
      row.isEdit = false;
      this.submitName(row);
    },
    handlePlay(row) {
      if (this.playingRowId === row.id && this.currentAudio) {
        this.stopCurrentAudio();
        return;
      }
      this.stopCurrentAudio();
      Api.voiceClone.getAudioId(row.id, (res) => {
        res = res.data;
        if (res.code === 0) {
          const uuid = res.data;
          const audioUrl = Api.voiceClone.getPlayVoiceUrl(uuid);
          const audio = new Audio(audioUrl);
          this.currentAudio = audio;
          this.playingRowId = row.id;
          audio.addEventListener('ended', () => {
            this.playingRowId = null;
            this.currentAudio = null;
          });
          audio.addEventListener('error', () => {
            this.playingRowId = null;
            this.currentAudio = null;
          });
          audio.play().catch(err => {
            console.error('Playback failed:', err);
            this.$message.error(this.$t('voiceClone.playFailed') || 'Playback failed');
            this.playingRowId = null;
            this.currentAudio = null;
          });
        } else {
          this.$message.error(res.msg || this.$t('voiceClone.audioNotExist') || 'Audio does not exist');
        }
      });
    },
    stopCurrentAudio() {
      if (this.currentAudio) {
        this.currentAudio.pause();
        this.currentAudio.currentTime = 0;
        this.currentAudio = null;
      }
      this.playingRowId = null;
    },
    handleUpload(row) {
      this.currentVoiceClone = row;
      this.cloneDialogVisible = true;
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
  text-align: start;
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

.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.search-input {
  width: 280px;
  max-width: 100%;
}

.list-card {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.voice-clone-grid {
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
  align-content: start;
  min-height: 200px;
}

.voice-clone-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  transition: border-color 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }
}

.card-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px 12px;
}

.voice-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--x-divider);
  color: var(--x-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.info-title {
  flex: 1;
  min-width: 0;

  .name-text {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    line-height: 22px;
    color: var(--x-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  font-size: 12px;
  font-weight: 500;
  line-height: 18px;
  white-space: nowrap;
  background: var(--x-divider);
  color: var(--x-text-secondary);
  outline: none;

  &:focus-visible {
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }

  .status-pill-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--x-text-muted);
  }

  &.status-success {
    background: var(--x-success-bg);
    color: var(--x-success-text);

    .status-pill-dot {
      background: var(--x-success);
    }
  }

  &.status-failed {
    background: var(--x-danger-bg);
    color: var(--x-danger-text);

    .status-pill-dot {
      background: var(--x-danger);
    }
  }
}

.info-grid {
  margin: 0;
  padding: 0 16px 14px;
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  row-gap: 6px;
  column-gap: 16px;
  font-size: 13px;
  line-height: 20px;

  dt {
    color: var(--x-text-muted);
    white-space: nowrap;
  }

  dd {
    margin: 0;
    color: var(--x-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .mono {
    font-family: var(--x-font-mono);
    font-size: 12px;
    color: var(--x-text-secondary);
  }
}

.voice-wave {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 3px;
  height: 32px;
  margin: 0 16px 14px;
  padding: 0 12px;
  border-radius: var(--x-radius-control);
  background: var(--x-surface-raised);

  .wave-bar {
    flex: 1;
    max-width: 3px;
    height: 50%;
    background: var(--x-border-control);
    border-radius: 2px;
    transform-origin: center;
    animation: voice-wave 1.2s ease-in-out infinite paused;

    @for $i from 1 through 24 {
      &:nth-child(#{$i}) {
        animation-delay: #{-0.1s * ($i - 1)};
      }
    }
  }

  &.is-playing .wave-bar {
    background: var(--x-ink);
    animation-play-state: running;
  }
}

@keyframes voice-wave {
  0%, 100% { transform: scaleY(0.3); }
  50% { transform: scaleY(1.5); }
}

.card-actions {
  margin-top: auto;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
  padding: 6px;
  border-top: 1px solid var(--x-divider);

  .el-button + .el-button {
    margin-inline-start: 0;
  }
}

.row-action {
  display: inline-flex;
  align-items: center;
  padding: 6px;
  font-size: 13px;
  color: var(--x-text-secondary);

  ::v-deep [class*="el-icon-"] + span {
    margin-inline-start: 4px;
  }

  &:hover,
  &:focus {
    color: var(--x-text);
    background: var(--x-canvas);
  }
}

.empty-state {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--x-text-muted);
  font-size: 14px;
}

.card-footer {
  padding: 10px 16px;
  border-top: 1px solid var(--x-border);

  overflow-x: auto;

  ::v-deep .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
  }
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

  .page-actions,
  .search-input {
    width: 100%;
  }

  .search-input {
    flex: 1;
    width: auto;
  }

  .voice-clone-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }
}
</style>
