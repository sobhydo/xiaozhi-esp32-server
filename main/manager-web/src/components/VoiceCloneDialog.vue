<template>
    <CustomDialog
        :title="$t('voiceClone.dialogTitle')"
        :visible.sync="innerVisible"
        width="760px"
        :footer="false"
        @close="handleDialogClose"
    >
        <div class="dialog-content">
            <!-- Step indicator -->
            <ol class="steps-header">
                <li class="step-item" :class="{ 'active': currentStep === 1, 'completed': currentStep > 1 }">
                    <div class="step-number">
                        <i class="el-icon-check" v-if="currentStep > 1"></i>
                        <span v-else>1</span>
                    </div>
                    <div class="step-label">{{ $t('voiceClone.stepUpload') }}</div>
                </li>
                <li class="step-line" aria-hidden="true"></li>
                <li class="step-item" :class="{ 'active': currentStep === 2, 'completed': currentStep > 2 }">
                    <div class="step-number">
                        <i class="el-icon-check" v-if="currentStep > 2"></i>
                        <span v-else>2</span>
                    </div>
                    <div class="step-label">{{ $t('voiceClone.stepEdit') }}</div>
                </li>
            </ol>

            <!-- Step 1: Audio upload -->
            <div v-if="currentStep === 1" class="step-content">
                <div class="upload-area">
                    <el-upload class="audio-uploader" drag :action="uploadAction" :auto-upload="false"
                        :on-change="handleFileChange" :show-file-list="false" accept="audio/*">
                        <i class="el-icon-upload"></i>
                        <div class="el-upload__text">{{ $t('voiceClone.dragOrClick') }}</div>
                        <div class="el-upload__tip">{{ $t('voiceClone.uploadTip') }}</div>
                    </el-upload>
                </div>
            </div>

            <!-- Step 2: Audio editing -->
            <div v-if="currentStep === 2" class="step-content">
                <div class="audio-edit-area">
                    <div class="edit-tips">
                        <p>{{ $t('voiceClone.editTip1') }}</p>
                        <p>{{ $t('voiceClone.editTip2') }}</p>
                    </div>

                    <!-- Waveform display area -->
                    <div class="waveform-container">
                        <canvas ref="waveformCanvas" class="waveform-canvas" @mousedown="handleWaveformMouseDown"
                            @mousemove="handleWaveformMouseMove" @mouseup="handleWaveformMouseUp"></canvas>
                        <div class="selection-overlay" v-if="isSelecting || selectionStart !== null"
                            :style="selectionStyle"></div>
                        <div class="duration-display">
                            {{ $t('voiceClone.selectedDuration', { duration: selectedDuration.toFixed(1) }) }}
                        </div>
                    </div>

                    <!-- Audio control buttons -->
                    <div class="audio-controls">
                        <el-button size="small" :icon="isPlaying ? 'el-icon-video-pause' : 'el-icon-video-play'"
                            @click="togglePlay">
                            {{ isPlaying ? $t('voiceClone.pause') : $t('voiceClone.play') }}
                        </el-button>
                        <el-button size="small" icon="el-icon-scissors" @click="handleTrim"
                            :disabled="selectionStart === null">
                            {{ $t('voiceClone.trim') }}
                        </el-button>
                        <el-button size="small" icon="el-icon-refresh-left" @click="handleReset">
                            {{ $t('voiceClone.reset') }}
                        </el-button>
                    </div>

                    <!-- Audio element -->
                    <audio ref="audioPlayer" @timeupdate="handleTimeUpdate" @ended="handleAudioEnded"
                        style="display: none;"></audio>
                </div>
            </div>
        </div>
        <div class="custom-footer">
            <CustomButton @click="handleCancel">
                {{ currentStep === 1 ? $t('voiceClone.cancel') : $t('voiceClone.prevStep') }}
            </CustomButton>
            <CustomButton type="confirm" :loading="uploading" @click="handleNext">
                {{ currentStep === 1 ? $t('voiceClone.nextStep') : $t('voiceClone.upload') }}
            </CustomButton>
        </div>
    </CustomDialog>
</template>

<script>
import Api from "@/apis/api";
import CustomDialog from './CustomDialog.vue';
import CustomButton from './CustomButton.vue';

export default {
    name: 'VoiceCloneDialog',
    components: {
        CustomDialog,
        CustomButton
    },
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        voiceCloneData: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            currentStep: 1,
            uploadAction: '',
            audioFile: null,
            originalAudioFile: null,
            audioBuffer: null,
            originalAudioBuffer: null,
            isPlaying: false,
            uploading: false,
            // Waveform
            waveformData: [],
            // Selection
            isSelecting: false,
            selectionStart: null,
            selectionEnd: null,
            mouseStartX: 0,
            // Audio context
            audioContext: null,
            audioSource: null,
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
        },
        selectedDuration() {
            if (this.selectionStart !== null && this.selectionEnd !== null && this.audioBuffer) {
                const duration = this.audioBuffer.duration;
                const start = Math.min(this.selectionStart, this.selectionEnd) * duration;
                const end = Math.max(this.selectionStart, this.selectionEnd) * duration;
                return end - start;
            }
            return this.audioBuffer ? this.audioBuffer.duration : 0;
        },
        selectionStyle() {
            if (this.selectionStart === null) return {};
            const canvas = this.$refs.waveformCanvas;
            if (!canvas) return {};

            const start = Math.min(this.selectionStart, this.selectionEnd || this.selectionStart);
            const end = Math.max(this.selectionStart, this.selectionEnd || this.selectionStart);

            return {
                left: `${start * 100}%`,
                width: `${(end - start) * 100}%`
            };
        }
    },
    methods: {
        handleCancel() {
            if (this.currentStep === 2) {
                this.currentStep = 1;
            } else {
                this.$emit('update:visible', false);
            }
        },
        handleDialogClose() {
            this.resetDialog();
        },
        resetDialog() {
            this.currentStep = 1;
            this.audioFile = null;
            this.originalAudioFile = null;
            this.audioBuffer = null;
            this.originalAudioBuffer = null;
            this.isPlaying = false;
            this.uploading = false;
            this.waveformData = [];
            this.selectionStart = null;
            this.selectionEnd = null;
            if (this.audioSource) {
                this.audioSource.stop();
                this.audioSource = null;
            }
        },
        async handleFileChange(file) {
            if (!file || !file.raw) return;

            this.audioFile = file.raw;
            this.originalAudioFile = file.raw;

            // Go to step 2 first so the DOM is rendered
            this.currentStep = 2;

            // Load the audio after the DOM updates
            await this.$nextTick();
            await this.loadAudio(file.raw);
        },
        async loadAudio(file) {
            try {
                const arrayBuffer = await file.arrayBuffer();
                if (!this.audioContext) {
                    this.audioContext = new (window.AudioContext || window.webkitAudioContext)();
                }
                this.audioBuffer = await this.audioContext.decodeAudioData(arrayBuffer.slice(0));
                this.originalAudioBuffer = await this.audioContext.decodeAudioData(await file.arrayBuffer());

                // Set up the audio player
                if (this.$refs.audioPlayer) {
                    const audioUrl = URL.createObjectURL(file);
                    this.$refs.audioPlayer.src = audioUrl;
                    // Load audio metadata
                    this.$refs.audioPlayer.load();
                }

                // Generate waveform data
                await this.generateWaveform();
            } catch (error) {
                console.error('Failed to load audio:', error);
                this.$message.error(this.$t('voiceClone.loadAudioFailed'));
            }
        },
        async generateWaveform() {
            if (!this.audioBuffer) return;

            await this.$nextTick();
            const canvas = this.$refs.waveformCanvas;
            if (!canvas) {
                console.error('Canvas element does not exist');
                return;
            }

            // Set canvas size
            const containerWidth = canvas.parentElement.offsetWidth;
            const containerHeight = canvas.parentElement.offsetHeight;
            canvas.width = containerWidth || 800;
            canvas.height = containerHeight || 200;

            const canvasWidth = canvas.width;
            const channelData = this.audioBuffer.getChannelData(0);
            const step = Math.floor(channelData.length / canvasWidth);
            const waveformData = [];

            for (let i = 0; i < canvasWidth; i++) {
                let sum = 0;
                for (let j = 0; j < step; j++) {
                    sum += Math.abs(channelData[i * step + j] || 0);
                }
                waveformData.push(sum / step);
            }

            this.waveformData = waveformData;
            this.drawWaveform();
        },
        drawWaveform() {
            const canvas = this.$refs.waveformCanvas;
            if (!canvas) {
                console.error('Canvas does not exist while drawing waveform');
                return;
            }

            const ctx = canvas.getContext('2d');
            const width = canvas.width;
            const height = canvas.height;

            // Clear the canvas
            ctx.clearRect(0, 0, width, height);

            // Draw background (colors come from the design tokens)
            const tokens = getComputedStyle(document.documentElement);
            const bg = tokens.getPropertyValue('--x-surface-raised').trim() || '#fafafb';
            const bar = tokens.getPropertyValue('--x-ink').trim() || '#18181b';
            ctx.fillStyle = bg;
            ctx.fillRect(0, 0, width, height);

            if (this.waveformData.length === 0) {
                console.error('Waveform data is empty');
                return;
            }

            // Find the max value for normalization
            const maxValue = Math.max(...this.waveformData);

            // Draw waveform
            ctx.fillStyle = bar;
            ctx.strokeStyle = bar;
            ctx.lineWidth = 1;

            const barWidth = width / this.waveformData.length;

            this.waveformData.forEach((value, index) => {
                // Normalize and scale to 80% of the height
                const normalizedValue = maxValue > 0 ? value / maxValue : 0;
                const barHeight = Math.max(1, normalizedValue * height * 0.8);
                const x = index * barWidth;
                const y = (height - barHeight) / 2;

                ctx.fillRect(x, y, Math.max(1, barWidth - 1), barHeight);
            });
        },
        handleWaveformMouseDown(e) {
            const canvas = this.$refs.waveformCanvas;
            const rect = canvas.getBoundingClientRect();
            this.mouseStartX = e.clientX - rect.left;
            this.selectionStart = this.mouseStartX / rect.width;
            this.selectionEnd = this.selectionStart;
            this.isSelecting = true;
        },
        handleWaveformMouseMove(e) {
            if (!this.isSelecting) return;

            const canvas = this.$refs.waveformCanvas;
            const rect = canvas.getBoundingClientRect();
            const x = e.clientX - rect.left;
            this.selectionEnd = Math.max(0, Math.min(1, x / rect.width));
        },
        handleWaveformMouseUp() {
            this.isSelecting = false;
        },
        async handleTrim() {
            if (this.selectionStart === null || this.selectionEnd === null || !this.audioBuffer) return;

            const start = Math.min(this.selectionStart, this.selectionEnd);
            const end = Math.max(this.selectionStart, this.selectionEnd);

            // Create a new audio buffer
            const duration = this.audioBuffer.duration;
            const startTime = start * duration;
            const endTime = end * duration;
            const startOffset = Math.floor(startTime * this.audioBuffer.sampleRate);
            const endOffset = Math.floor(endTime * this.audioBuffer.sampleRate);
            const newLength = endOffset - startOffset;

            const newBuffer = this.audioContext.createBuffer(
                this.audioBuffer.numberOfChannels,
                newLength,
                this.audioBuffer.sampleRate
            );

            for (let channel = 0; channel < this.audioBuffer.numberOfChannels; channel++) {
                const oldData = this.audioBuffer.getChannelData(channel);
                const newData = newBuffer.getChannelData(channel);
                for (let i = 0; i < newLength; i++) {
                    newData[i] = oldData[startOffset + i];
                }
            }

            this.audioBuffer = newBuffer;

            // Update the audio file
            await this.bufferToFile(newBuffer);

            // Reset selection
            this.selectionStart = null;
            this.selectionEnd = null;

            // Regenerate waveform
            this.generateWaveform();

            this.$message.success(this.$t('voiceClone.trimSuccess'));
        },
        async handleReset() {
            if (!this.originalAudioFile) return;

            this.audioFile = this.originalAudioFile;
            await this.loadAudio(this.originalAudioFile);
            this.selectionStart = null;
            this.selectionEnd = null;

            this.$message.success(this.$t('voiceClone.resetSuccess'));
        },
        togglePlay() {
            const audio = this.$refs.audioPlayer;
            if (this.isPlaying) {
                audio.pause();
                this.isPlaying = false;
            } else {
                audio.play();
                this.isPlaying = true;
            }
        },
        handleTimeUpdate() {
            // Playback progress can be updated here
        },
        handleAudioEnded() {
            this.isPlaying = false;
        },
        async bufferToFile(buffer) {
            // Convert AudioBuffer to a WAV file
            const wav = this.audioBufferToWav(buffer);
            const blob = new Blob([wav], { type: 'audio/wav' });
            this.audioFile = new File([blob], 'audio.wav', { type: 'audio/wav' });

            // Update the player
            await this.$nextTick();
            if (this.$refs.audioPlayer) {
                const audioUrl = URL.createObjectURL(blob);
                this.$refs.audioPlayer.src = audioUrl;
            }
        },
        audioBufferToWav(buffer) {
            const length = buffer.length * buffer.numberOfChannels * 2 + 44;
            const arrayBuffer = new ArrayBuffer(length);
            const view = new DataView(arrayBuffer);
            const channels = [];
            let offset = 0;
            let pos = 0;

            // Write WAV header
            const setUint16 = (data) => {
                view.setUint16(pos, data, true);
                pos += 2;
            };
            const setUint32 = (data) => {
                view.setUint32(pos, data, true);
                pos += 4;
            };

            // "RIFF" chunk descriptor
            setUint32(0x46464952); // "RIFF"
            setUint32(length - 8); // file length - 8
            setUint32(0x45564157); // "WAVE"

            // "fmt " sub-chunk
            setUint32(0x20746d66); // "fmt "
            setUint32(16); // SubChunk1Size = 16
            setUint16(1); // AudioFormat = 1 (PCM)
            setUint16(buffer.numberOfChannels);
            setUint32(buffer.sampleRate);
            setUint32(buffer.sampleRate * 2 * buffer.numberOfChannels); // byte rate
            setUint16(buffer.numberOfChannels * 2); // block align
            setUint16(16); // bits per sample

            // "data" sub-chunk
            setUint32(0x61746164); // "data"
            setUint32(length - pos - 4); // SubChunk2Size

            // Write audio data
            for (let i = 0; i < buffer.numberOfChannels; i++) {
                channels.push(buffer.getChannelData(i));
            }

            while (pos < length) {
                for (let i = 0; i < buffer.numberOfChannels; i++) {
                    let sample = Math.max(-1, Math.min(1, channels[i][offset]));
                    sample = sample < 0 ? sample * 0x8000 : sample * 0x7FFF;
                    view.setInt16(pos, sample, true);
                    pos += 2;
                }
                offset++;
            }

            return arrayBuffer;
        },
        async handleNext() {
            if (this.currentStep === 1) {
                // Check that a file has been selected
                if (!this.audioFile) {
                    this.$message.warning(this.$t('voiceClone.pleaseSelectAudio'));
                    return;
                }
                this.currentStep = 2;
            } else {
                // Upload audio
                await this.uploadAudio();
            }
        },
        async uploadAudio() {
            if (!this.audioFile) {
                this.$message.warning(this.$t('voiceClone.pleaseSelectAudio'));
                return;
            }

            // Validate audio duration (8-60 seconds)
            if (this.audioBuffer) {
                const duration = this.audioBuffer.duration;
                if (duration < 8 || duration > 60) {
                    this.$message.warning(this.$t('voiceClone.durationError'));
                    return;
                }
            }

            this.uploading = true;

            try {
                const formData = new FormData();
                formData.append('voiceFile', this.audioFile);
                formData.append('id', this.voiceCloneData.id);

                await Api.voiceClone.uploadVoice(formData, (res) => {
                    this.uploading = false;
                    res = res.data;
                    if (res.code === 0) {
                        this.$message.success(this.$t('voiceClone.uploadSuccess'));
                        this.resetDialog();
                        this.$emit('update:visible', false);
                        this.$emit('success');
                    } else {
                        this.$message.error(res.msg || this.$t('voiceClone.uploadFailed'));
                    }
                });
            } catch (error) {
                this.uploading = false;
                console.error('Failed to upload audio:', error);
                this.$message.error(this.$t('voiceClone.uploadFailed'));
            }
        }
    },
    mounted() {
        // Set canvas size
        this.$nextTick(() => {
            const canvas = this.$refs.waveformCanvas;
            if (canvas) {
                canvas.width = canvas.offsetWidth;
                canvas.height = canvas.offsetHeight;
            }
        });
    },
    beforeDestroy() {
        if (this.audioContext) {
            this.audioContext.close();
        }
    }
};
</script>

<style lang="scss" scoped>
.dialog-content {
    text-align: start;
}

.steps-header {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 12px;
}

.step-item {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;

    &.active {
        .step-number {
            background: var(--x-ink);
            border-color: var(--x-ink);
            color: var(--x-on-ink);
        }

        .step-label {
            color: var(--x-text);
            font-weight: 600;
        }
    }

    &.completed {
        .step-number {
            background: var(--x-selected);
            border-color: var(--x-selected);
            color: var(--x-ink);
        }

        .step-label {
            color: var(--x-text-secondary);
        }
    }
}

.step-number {
    width: 28px;
    height: 28px;
    box-sizing: border-box;
    border-radius: 50%;
    border: 1px solid var(--x-border-control);
    background: var(--x-surface);
    color: var(--x-text-muted);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 600;
    font-size: 13px;
}

.step-label {
    color: var(--x-text-muted);
    font-size: 14px;
}

.step-line {
    flex: 0 1 160px;
    min-width: 24px;
    height: 1px;
    background: var(--x-border-control);
}

.step-content {
    padding: 20px 0 4px;
}

.audio-uploader {
    ::v-deep .el-upload {
        width: 100%;
    }

    ::v-deep .el-upload-dragger {
        width: 100%;
        height: 240px;
        padding: 16px;
        box-sizing: border-box;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        border: 1px dashed var(--x-border-control);
        border-radius: var(--x-radius-card);
        background: var(--x-surface-raised);
        transition: border-color 0.15s ease, background 0.15s ease;

        &:hover,
        &.is-dragover {
            border-color: var(--x-ink);
            background: var(--x-surface);
        }

        .el-icon-upload {
            font-size: 40px;
            color: var(--x-text-muted);
            margin: 0 0 12px;
            line-height: 1;
        }
    }

    .el-upload__text {
        color: var(--x-text);
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 4px;
    }

    .el-upload__tip {
        font-size: 12px;
        color: var(--x-text-muted);
        margin-top: 4px;
        line-height: 1.5;
    }
}

.edit-tips {
    margin-bottom: 16px;
    padding: 12px 14px;
    background: var(--x-surface-raised);
    border: 1px solid var(--x-border);
    border-radius: var(--x-radius-control);

    p {
        margin: 2px 0;
        font-size: 13px;
        line-height: 1.5;
        color: var(--x-text-secondary);

        &:first-child {
            font-weight: 500;
            color: var(--x-text);
        }
    }
}

.waveform-container {
    position: relative;
    width: 100%;
    height: 200px;
    border: 1px solid var(--x-border);
    border-radius: var(--x-radius-control);
    overflow: hidden;
    cursor: crosshair;
    background: var(--x-surface-raised);
}

.waveform-canvas {
    display: block;
    width: 100%;
    height: 100%;
}

.selection-overlay {
    position: absolute;
    top: 0;
    bottom: 0;
    background: var(--x-focus-ring);
    border-inline-start: 1px solid var(--x-ink);
    border-inline-end: 1px solid var(--x-ink);
    pointer-events: none;
}

.duration-display {
    position: absolute;
    top: 8px;
    right: 8px;
    background: var(--x-ink);
    color: var(--x-on-ink);
    padding: 3px 8px;
    border-radius: var(--x-radius-pill);
    font-size: 12px;
    font-variant-numeric: tabular-nums;
}

.audio-controls {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
    margin-top: 16px;

    .el-button + .el-button {
        margin-inline-start: 0;
    }
}

.custom-footer {
    display: flex;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 20px;
    padding-top: 16px;
    border-top: 1px solid var(--x-divider);

    .el-button + .el-button {
        margin-inline-start: 0;
    }
}

::v-deep .el-dialog {
    max-width: calc(100vw - 32px);
}

@media (max-width: 600px) {
    .step-label {
        font-size: 13px;
    }
}
</style>
