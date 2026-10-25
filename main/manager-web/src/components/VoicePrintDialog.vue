<template>
  <CustomDialog
    :title="title"
    :visible.sync="innerVisible"
    :confirm-loading="saving"
    width="600px"
    class="voice-print-dialog"
    @confirm="submit"
    @cancel="cancel"
  >
    <el-form :model="form" :rules="rules" ref="form" label-width="auto" label-position="top" class="param-form">
      <el-form-item :label="$t('voicePrintDialog.voicePrintVector')" prop="audioId" class="form-item">
        <el-select v-model="form.audioId" :placeholder="$t('voicePrintDialog.selectVoiceMessage')" class="custom-select" filterable>
          <el-option v-for="item in valueTypeOptions" :key="item.audioId" :label="item.content" :value="item.audioId">
            <div class="audio-option">
              <span class="audio-option-text">{{ item.content }}</span>
              <button type="button" class="audio-icon" :aria-label="$t('voiceClone.play')"
                @click.stop="playAudio(item.audioId)">
                <i :class="getAudioIconClass(item.audioId)" aria-hidden="true"></i>
              </button>
            </div>
          </el-option>
        </el-select>
      </el-form-item>

      <el-form-item :label="$t('voicePrintDialog.name')" prop="sourceName" class="form-item">
        <el-input v-model="form.sourceName" :placeholder="$t('voicePrintDialog.enterName')" class="custom-input"></el-input>
      </el-form-item>

      <el-form-item :label="$t('voicePrintDialog.description')" prop="introduce" class="form-item remark-item">
        <el-input type="textarea" v-model="form.introduce" :placeholder="$t('voicePrintDialog.enterDescription')" :rows="3" class="custom-textarea"
          maxlength="100" show-word-limit></el-input>
      </el-form-item>
    </el-form>
  </CustomDialog>
</template>

<script>
import api from '@/apis/api';
import CustomDialog from './CustomDialog.vue';

export default {
  components: { CustomDialog },
  props: {
    title: {
      type: String,
      default: 'Add speaker'
    },
    visible: {
      type: Boolean,
      default: false
    },
    agentId: {
      type: String
    },
    form: {
      type: Object,
      default: () => ({
        id: null,
        audioId: '',
        sourceName: '',
        introduce: ''
      })
    }
  },
  data() {
    return {
      saving: false,
      playingAudioId: null,
      audioElement: null,
      valueTypeOptions: [
        { audioId: '', content: '' }
      ],
      rules: {
        introduce: [
          { required: true, message: 'Please enter a description', trigger: "blur" }
        ],
        sourceName: [
          { required: true, message: 'Please enter a name', trigger: "blur" }
        ],
        audioId: [
          { required: true, message: 'Please select an audio vector', trigger: "change" }
        ]
      }
    };
  },
  computed: {
    innerVisible: {
      get() {
        return this.visible;
      },
      set(val) {
        this.$emit('update:visible', val);
      }
    }
  },
  methods: {
    getAudioIconClass(audioId) {
      if (this.playingAudioId === audioId) {
        return 'el-icon-loading';
      }
      return 'el-icon-video-play';
    },
    playAudio(audioId) {
      if (this.playingAudioId === audioId) {
        // If this audio is currently playing, stop it
        if (this.audioElement) {
          this.audioElement.pause();
          this.audioElement = null;
        }
        this.playingAudioId = null;
        return;
      }

      // Stop the audio that is currently playing
      if (this.audioElement) {
        this.audioElement.pause();
        this.audioElement = null;
      }

      // First get the audio download ID
      this.playingAudioId = audioId;
      api.agent.getAudioId(audioId, (res) => {
        if (res.data && res.data.data) {
          // Play the audio using the download ID
          this.audioElement = new Audio(api.getServiceUrl() + `/agent/play/${res.data.data}`);

          this.audioElement.onended = () => {
            this.playingAudioId = null;
            this.audioElement = null;
          };

          this.audioElement.play();
        }
      });
    },
    submit() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.saving = true;
          this.$emit('submit', {
            form: this.form,
            done: () => {
              this.saving = false;
            }
          });

          setTimeout(() => {
            this.saving = false;
          }, 3000);
        }
      });
    },
    cancel() {
      this.saving = false;
      this.$emit('cancel');
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.saving = false;
        api.agent.getRecentlyFiftyByAgentId(this.agentId, ((data) => {
          this.valueTypeOptions = data.data.data.map(item => ({
            ...item
          }));
        }))
      }
    },
    'form.audioId'(newVal) {
      if (newVal == null || newVal == "") {
        return
      }
      if (this.valueTypeOptions.some(item => item.audioId === newVal)) {
        return;
      }
      api.agent.getContentByAudioId(newVal, ((data) => {
        this.valueTypeOptions.push({
          audioId: newVal, content: data.data.data
        })
      }))
    }
  }
};
</script>

<style scoped lang="scss">
.voice-print-dialog ::v-deep .el-dialog {
  max-width: calc(100vw - 32px);
}

.audio-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.audio-option-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.audio-icon {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-muted);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);
    color: var(--x-ink);
  }
}

.param-form {
  text-align: start;

  .form-item {
    margin-bottom: 18px;

    :deep(.el-form-item__label) {
      padding-bottom: 6px;
      line-height: 20px;
      font-size: 13px;
    }
  }

  .remark-item {
    margin-bottom: 4px;
  }

  .custom-select {
    width: 100%;
  }
}
</style>
