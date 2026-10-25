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
          <el-button v-if="ownModels.length" type="primary" icon="el-icon-plus" class="new-voice-btn" @click="openNewVoice()">
            {{ $t('voiceClone.newVoice') }}
          </el-button>
        </div>
      </header>

      <!-- Self-service cloning needs one of the user's own ElevenLabs models -->
      <div v-if="ownModelsLoaded && !ownModels.length" class="byok-hint" role="note">
        <i class="el-icon-info" aria-hidden="true"></i>
        <span>
          {{ $t('voiceClone.byokHint') }}
          <router-link :to="{ path: '/my-models', query: { type: 'tts' } }" class="byok-hint__link">{{ $t('header.myModels') }}</router-link>
        </span>
      </div>

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
              <el-button v-if="item.selfManaged" size="mini" type="text" class="row-action row-action--danger"
                icon="el-icon-delete" :loading="item._deleting" @click="handleDelete(item)">
                {{ $t('common.delete') }}
              </el-button>
            </div>
          </article>

          <div v-if="!loading && voiceCloneList.length === 0" class="empty-state">
            {{ ownModels.length ? $t('voiceClone.noVoicesYet') : $t('voiceClone.noVoiceCloneAssigned') }}
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

    <CustomDialog
      :visible.sync="newVoiceVisible"
      :title="$t('voiceClone.newVoice')"
      width="480px"
      class="new-voice-dialog"
      :confirm-loading="creating"
      :cancel-text="$t('common.cancel')"
      :confirm-text="$t('voiceClone.createVoice')"
      @confirm="createVoice"
    >
      <p class="dialog-intro">{{ $t('voiceClone.newVoiceIntro') }}</p>
      <el-form ref="newVoiceForm" :model="newVoice" :rules="newVoiceRules" label-position="top" class="new-voice-form"
        @submit.native.prevent>
        <el-form-item :label="$t('voiceClone.name')" prop="name">
          <el-input v-model="newVoice.name" maxlength="64" :placeholder="$t('voiceClone.newVoiceNamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="$t('voiceClone.elevenlabsModel')" prop="modelId">
          <el-select v-model="newVoice.modelId" class="full">
            <el-option v-for="m in ownModels" :key="m.id" :label="m.modelName" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('voiceClone.languages')">
          <el-select v-model="newVoice.languages" class="full" multiple filterable allow-create default-first-option>
            <el-option v-for="lang in languageOptions" :key="lang.value" :label="$t(lang.label)" :value="lang.value" />
          </el-select>
        </el-form-item>
      </el-form>
    </CustomDialog>
  </div>
</template>

<script>
import Api from "@/apis/api";
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import VoiceCloneDialog from "@/components/VoiceCloneDialog.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomPagination from "@/components/CustomPagination.vue";
import CustomDialog from "@/components/CustomDialog.vue";
import { formatDate } from "@/utils/format";

export default {
  components: { HeaderBar, VersionFooter, VoiceCloneDialog, CustomButton, CustomPagination, CustomDialog },
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
      playingRowId: null,
      // Self-service cloning on the user's own ElevenLabs key
      ownModels: [],
      ownModelsLoaded: false,
      newVoiceVisible: false,
      creating: false,
      newVoice: { name: "", modelId: "", languages: ["English", "Arabic"] },
      languageOptions: [
        { value: "English", label: "myModels.languageEnglish" },
        { value: "Arabic", label: "myModels.languageArabic" }
      ]
    };
  },
  computed: {
    newVoiceRules() {
      return {
        name: [{ required: true, whitespace: true, message: this.$t('voiceClone.nameRequired'), trigger: ["blur", "change"] }],
        modelId: [{ required: true, message: this.$t('voiceClone.modelRequired'), trigger: "change" }]
      };
    }
  },
  created() {
    this.fetchVoiceCloneList();
    this.fetchOwnModels();
  },
  methods: {
    fetchOwnModels() {
      Api.model.getUserModels("TTS", ({ data }) => {
        if (data && data.code === 0 && Array.isArray(data.data)) {
          this.ownModelsLoaded = true;
          this.ownModels = data.data.filter((m) => m.configJson && m.configJson.type === "elevenlabs");
          // Arriving from My models with ?modelId opens the dialog for that model
          const modelId = this.$route.query.modelId;
          if (modelId && this.ownModels.some((m) => m.id === modelId)) {
            this.openNewVoice(modelId);
          }
        }
      });
    },
    openNewVoice(modelId) {
      const preferred = modelId || this.$route.query.modelId;
      const match = this.ownModels.find((m) => m.id === preferred);
      this.newVoice = {
        name: "",
        modelId: match ? match.id : (this.ownModels[0] ? this.ownModels[0].id : ""),
        languages: ["English", "Arabic"]
      };
      this.newVoiceVisible = true;
      this.$nextTick(() => this.$refs.newVoiceForm && this.$refs.newVoiceForm.clearValidate());
    },
    createVoice() {
      if (this.creating) {
        return;
      }
      this.$refs.newVoiceForm.validate((valid) => {
        if (!valid) {
          return;
        }
        const languages = this.newVoice.languages.map((l) => String(l).trim()).filter(Boolean);
        const body = {
          modelId: this.newVoice.modelId,
          name: this.newVoice.name.trim(),
          languages: (languages.length ? languages : ["English", "Arabic"]).join(", ")
        };
        this.creating = true;
        Api.voiceClone.createOwnVoiceClone(body, ({ data }) => {
          this.creating = false;
          if (data && data.code === 0) {
            this.newVoiceVisible = false;
            this.$message.success({ message: this.$t('voiceClone.newVoiceCreated'), showClose: true });
            this.currentPage = 1;
            this.fetchVoiceCloneList();
          } else {
            this.$message.error({ message: (data && data.msg) || this.$t('voiceClone.addFailed'), showClose: true });
          }
        });
      });
    },
    handleDelete(row) {
      this.$confirm(this.$t('voiceClone.deleteVoiceConfirm', { name: row.name || row.id }), this.$t('common.delete'), {
        confirmButtonText: this.$t('common.delete'),
        cancelButtonText: this.$t('common.cancel'),
        confirmButtonClass: "el-button--danger",
        type: "warning"
      }).then(() => {
        if (this.playingRowId === row.id) {
          this.stopCurrentAudio();
        }
        this.$set(row, '_deleting', true);
        Api.voiceClone.deleteVoiceClone(row.id, ({ data }) => {
          this.$set(row, '_deleting', false);
          if (data && data.code === 0) {
            this.$message.success({ message: this.$t('voiceClone.voiceDeleted'), showClose: true });
            if (this.voiceCloneList.length === 1 && this.currentPage > 1) {
              this.currentPage -= 1;
            }
            this.fetchVoiceCloneList();
          } else {
            this.$message.error({ message: (data && data.msg) || this.$t('voiceClone.deleteFailed'), showClose: true });
          }
        });
      }).catch(() => {});
    },
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

.row-action--danger {
  &:hover,
  &:focus {
    color: var(--x-danger-text);
    background: var(--x-danger-bg);
  }
}

.byok-hint {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  background: var(--x-surface);
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-secondary);

  i {
    flex-shrink: 0;
    margin-top: 3px;
    color: var(--x-text-muted);
  }
}

.byok-hint__link {
  margin-inline-start: 4px;
  color: var(--x-text);
  font-weight: 500;
  text-decoration: underline;
  text-underline-offset: 2px;

  &:hover,
  &:focus-visible {
    color: var(--x-ink-hover);
  }
}

.dialog-intro {
  margin: 0 0 16px;
  word-break: normal;
  overflow-wrap: break-word;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-muted);
  text-align: start;
}

.new-voice-form {
  text-align: start;

  // Element positions tag close icons with physical offsets; mirror them for RTL
  ::v-deep .el-select__tags .el-tag {
    margin-block: 2px;
    margin-inline: 6px 0;
  }

  ::v-deep .el-select__tags .el-tag__close.el-icon-close {
    right: auto;
    inset-inline-end: -7px;
  }

  ::v-deep .el-form-item {
    margin-bottom: 16px;
  }

  ::v-deep .el-form-item__label {
    padding-bottom: 4px;
    line-height: 20px;
    float: none;
    display: block;
    text-align: start;
  }

  ::v-deep .el-form-item.is-required:not(.is-no-asterisk) > .el-form-item__label::before {
    display: none;
  }
}

.full {
  width: 100%;
}

.new-voice-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.new-voice-dialog ::v-deep .el-dialog__headerbtn {
  right: auto;
  inset-inline-end: 16px;
}

.new-voice-dialog ::v-deep .el-dialog__header {
  padding-inline-end: 56px;
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

  .new-voice-btn {
    order: -1;
    flex: 1 1 100%;
  }

  .voice-clone-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: 12px;
  }
}
</style>
