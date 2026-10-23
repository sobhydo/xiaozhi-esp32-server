<template>
  <el-dialog :visible.sync="dialogVisible" width="720px" @close="handleClose" class="compact-dialog" :append-to-body="true">
    <el-form :model="voiceForm" :rules="rules" ref="voiceForm" label-width="auto" label-position="top" class="voice-form">
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-form-item :label="$t('editVoiceDialog.voiceCode')" prop="voiceCode">
            <el-input v-model="voiceForm.voiceCode" :placeholder="$t('editVoiceDialog.voiceCodePlaceholder')" class="compact-input"></el-input>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item :label="$t('editVoiceDialog.voiceName')" prop="voiceName">
            <el-input v-model="voiceForm.voiceName" :placeholder="$t('editVoiceDialog.voiceNamePlaceholder')" class="compact-input"></el-input>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :xs="24" :sm="12">
          <el-form-item :label="$t('editVoiceDialog.languageType')" prop="languageType">
            <el-input v-model="voiceForm.languageType" :placeholder="$t('editVoiceDialog.languageTypePlaceholder')" class="compact-input"></el-input>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item :label="$t('editVoiceDialog.sortNumber')" prop="sortNumber">
            <el-input-number v-model="voiceForm.sortNumber" :min="1" :controls="false" class="compact-number"></el-input-number>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item :label="$t('editVoiceDialog.remark')" prop="remark">
        <el-input v-model="voiceForm.remark" type="textarea" :rows="2" :placeholder="$t('editVoiceDialog.remarkPlaceholder')" class="compact-textarea"
        ></el-input>
        <div class="audio-controls">
           <div class="audio-player">
      <audio
        :src="audioUrl"
        controls
        preload="metadata"
        class="custom-audio"
      ></audio>
    </div>
          <el-button size="small" class="preview-btn">{{ $t('editVoiceDialog.generatePreview') }}</el-button>
        </div>
      </el-form-item>
    </el-form>

    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">{{ $t('button.close') }}</el-button>
      <el-button type="primary" @click="handleSave">{{ $t('button.save') }}</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'EditVoiceDialog',
  props: {
    showDialog: Boolean,
    voiceData: {
      type: Object,
      default: () => ({
        voiceCode: 'wawaxiaohe',
        voiceName: this.$t('editVoiceDialog.defaultVoiceName'),
        languageType: this.$t('editVoiceDialog.defaultLanguageType'),
        sortNumber: 123
      })
    }
  },
  data() {
    return {
      dialogVisible: this.showDialog,
      voiceForm: { ...this.voiceData },
      audioUrl: 'http://music.163.com/song/media/outer/url?id=447925558.mp3',
      generatedAudio: null,
      rules: {
        voiceCode: [{ required: true, message: this.$t('editVoiceDialog.requiredVoiceCode'), trigger: 'blur' }],
        voiceName: [{ required: true, message: this.$t('editVoiceDialog.requiredVoiceName'), trigger: 'blur' }]
      }
    }
  },
  watch: {
    showDialog(newVal) {
      this.dialogVisible = newVal
      if (newVal) this.voiceForm = { ...this.voiceData }
    }
  },
  methods: {
    handleClose() {
      this.dialogVisible = false
      this.$emit('update:showDialog', false)
    },
    handleSave() {
      this.$refs.voiceForm.validate(valid => {
        if (valid) this.$emit('save', this.voiceForm)
      })
    },
  }
}
</script>

<style lang="scss" scoped>
.compact-dialog {
  ::v-deep .el-dialog {
    max-width: calc(100vw - 32px);
  }

  ::v-deep .el-dialog__header {
    padding: 18px 24px 16px;
    border-bottom: 1px solid var(--x-divider);
  }

  ::v-deep .el-dialog__body {
    padding: 20px 24px;
    text-align: left;
  }

  ::v-deep .el-dialog__footer {
    padding: 14px 24px 18px;
    border-top: 1px solid var(--x-divider);
  }

  .voice-form ::v-deep .el-form-item__label {
    padding-bottom: 6px;
    line-height: 20px;
    font-size: 13px;
  }

  .el-form-item {
    margin-bottom: 16px;
  }

  .compact-input,
  .compact-number,
  .compact-textarea {
    width: 100%;
  }

  .compact-number ::v-deep .el-input__inner {
    padding-right: 10px;
    text-align: left;
  }

  .compact-textarea {
    margin-bottom: 8px;
  }

  .audio-controls {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 12px;
    margin-top: 8px;
  }

  .audio-player {
    flex: 1;
    min-width: 0;
  }

  .custom-audio {
    width: 100%;
    max-width: 360px;
    height: 36px;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>
