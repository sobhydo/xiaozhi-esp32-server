<template>
  <div class="audio-container">
    <audio
      ref="audioRef"
      :src="audioUrl"
      @timeupdate="updateProgress"
      @loadedmetadata="updateDuration"
      style="display: none"
    ></audio>

    <div class="custom-controls">
      <!-- Play/pause button -->
      <button type="button" class="play-btn" :aria-label="isPlaying ? $t('voiceClone.pause') : $t('voiceClone.play')"
        @click="togglePlay">
        <svg width="14" height="14" viewBox="0 0 20 20" aria-hidden="true">
          <path
            fill="currentColor"
            :d="isPlaying
              ? 'M6 3h3v14H6zm5 0h3v14h-3z'
              : 'M5 3l12 7-12 7z'"
          />
        </svg>
      </button>

      <!-- Time display -->
      <span class="time-display">{{ formattedCurrentTime }}/{{ formattedDuration }}</span>

      <!-- Progress bar -->
      <div class="progress-container">
        <div class="progress-bar" @click="handleProgressClick">
          <div
            class="progress-track"
            :style="{ width: progress + '%' }"
          ></div>
          <div
            class="progress-thumb"
            :style="{ left: progress + '%' }"
            @mousedown="startDrag"
          ></div>
        </div>
      </div>

      <!-- Volume control -->
      <div class="volume-control" ref="volumeControl">
        <button
          @click="toggleMute"
          @mouseenter="handleVolumeMouseEnter"
          @mouseleave="startVolumeSliderHideTimer"
          ref="volumeButton"
          type="button"
          class="volume-button"
          :aria-label="$t('roleConfig.ttsVolume')"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" :d="volumeIconPath"/>
          </svg>
        </button>
        <div
          v-show="showVolumeSlider"
          class="volume-slider-container"
          @mouseenter="cancelVolumeSliderHideTimer"
          @mouseleave="startVolumeSliderHideTimer"
          ref="volumeSlider"
        >
          <div class="volume-slider-track">
            <input
              type="range"
              v-model="volume"
              min="0"
              max="1"
              step="0.1"
              class="volume-slider"
              @input="handleVolumeChange"
              orient="vertical"
            >
            <div class="volume-slider-thumb" :style="{ bottom: volume * 100 + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'

const props = defineProps({
  audioUrl: String
})

// Audio element references
const audioRef = ref(null)
const volumeButton = ref(null)
const volumeSlider = ref(null)

// Playback state
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progress = ref(0)

// Volume state
const volume = ref(0.7)
const isMuted = ref(false)
const showVolumeSlider = ref(false)
let volumeSliderHideTimer = null

// Format time as MM:SS
const formatTime = (seconds) => {
  const sec = Math.floor(seconds || 0)
  return `${Math.floor(sec / 60)}:${(sec % 60).toString().padStart(2, '0')}`
}

const formattedCurrentTime = computed(() => formatTime(currentTime.value))
const formattedDuration = computed(() => formatTime(duration.value))

// Return the icon path based on the volume state
const volumeIconPath = computed(() => {
  if (isMuted.value || volume.value === 0) {
    return 'M16.5 12c0-1.77-1.02-3.29-2.5-4.03v2.21l2.45 2.45c.03-.2.05-.41.05-.63zm2.5 0c0 .94-.2 1.82-.54 2.64l1.51 1.51C20.63 14.91 21 13.5 21 12c0-4.28-2.99-7.86-7-8.77v2.06c2.89.86 5 3.54 5 6.71zM4.27 3L3 4.27 7.73 9H3v6h4l5 5v-6.73l4.25 4.25c-.67.52-1.42.93-2.25 1.18v2.06c1.38-.31 2.63-.95 3.69-1.81L19.73 21 21 19.73l-9-9L4.27 3zM12 4L9.91 6.09 12 8.18V4z'
  }
  return 'M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z'
})

// Playback control
const togglePlay = () => {
  if (isPlaying.value) {
    audioRef.value.pause()
  } else {
    audioRef.value.play()
  }
  isPlaying.value = !isPlaying.value
}

const updateDuration = () => {
  if (audioRef.value) {
    duration.value = audioRef.value.duration
  }
}

const updateProgress = () => {
  if (audioRef.value?.duration) {
    currentTime.value = audioRef.value.currentTime
    progress.value = (currentTime.value / duration.value) * 100
  }
}

const handleProgressClick = (e) => {
  const rect = e.currentTarget.getBoundingClientRect()
  const percentage = ((e.clientX - rect.left) / rect.width) * 100
  seekToPercentage(percentage)
}

// Volume control
const toggleMute = () => {
  isMuted.value = !isMuted.value
  audioRef.value.muted = isMuted.value

  // If unmuting and the volume is 0, restore the default volume
  if (!isMuted.value && volume.value === 0) {
    volume.value = 0.5
    audioRef.value.volume = 0.5
  }
}

const handleVolumeChange = (e) => {
  const newVolume = parseFloat(e.target.value)
  audioRef.value.volume = newVolume
  isMuted.value = newVolume === 0
}

// Progress bar drag handling
let isDragging = false

const startDrag = (e) => {
  isDragging = true
  document.addEventListener('mousemove', handleDrag)
  document.addEventListener('mouseup', stopDrag)
  e.preventDefault()
}

const handleDrag = (e) => {
  if (!isDragging) return

  const rect = document.querySelector('.progress-bar').getBoundingClientRect()
  const percentage = ((e.clientX - rect.left) / rect.width) * 100
  seekToPercentage(percentage)
}

const stopDrag = () => {
  isDragging = false
  document.removeEventListener('mousemove', handleDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const seekToPercentage = (percentage) => {
  const clampedPercentage = Math.min(Math.max(percentage, 0), 100)
  progress.value = clampedPercentage
  audioRef.value.currentTime = (clampedPercentage / 100) * duration.value
}

// Volume slider show/hide control
const startVolumeSliderHideTimer = () => {
  volumeSliderHideTimer = setTimeout(() => {
    showVolumeSlider.value = false
  }, 300)
}

const cancelVolumeSliderHideTimer = () => {
  clearTimeout(volumeSliderHideTimer)
}

// Volume slider positioning
const updateSliderPosition = () => {
  nextTick(() => {
    if (!volumeButton.value || !volumeSlider.value) return

    const buttonRect = volumeButton.value.getBoundingClientRect()
    const slider = volumeSlider.value

    // Calculate the position relative to the viewport
    const left = buttonRect.left + window.scrollX
    const top = buttonRect.top + window.scrollY

    // Position centered on the button, vertically centered
    slider.style.left = `${left + 5}px`
    slider.style.top = `${top - 85}px`
  })
}

const handleVolumeMouseEnter = () => {
  showVolumeSlider.value = true
  updateSliderPosition()
}

// Initialization
onMounted(() => {
  if (audioRef.value) {
    audioRef.value.volume = volume.value
  }
  window.addEventListener('resize', updateSliderPosition)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateSliderPosition)
})
</script>

<style scoped>
.audio-container {
  width: 100%;
  min-width: 0;
  container-type: inline-size;
  background: var(--x-surface-raised);
  border: 1px solid var(--x-border);
  padding: 4px 10px 4px 4px;
  height: 40px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  border-radius: var(--x-radius-pill);
}

.custom-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.play-btn {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  background: var(--x-ink);
  color: var(--x-on-ink);
  border: none;
  border-radius: 50%;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s ease;
}

.play-btn:hover {
  background: var(--x-ink-hover);
}

.play-btn:focus-visible,
.volume-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px var(--x-focus-ring);
}

.time-display {
  font-size: 12px;
  color: var(--x-text-muted);
  min-width: 40px;
  text-align: center;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.progress-container {
  flex: 1;
  min-width: 24px;
  padding: 8px 6px;
}

.progress-bar {
  height: 3px;
  border-radius: 2px;
  background: var(--x-border-control);
  position: relative;
  cursor: pointer;
}

.progress-track {
  position: absolute;
  height: 100%;
  border-radius: 2px;
  background: var(--x-ink);
}

.progress-thumb {
  position: absolute;
  width: 10px;
  height: 10px;
  background: var(--x-ink);
  border: 2px solid var(--x-surface);
  box-shadow: 0 0 0 1px var(--x-border-control);
  border-radius: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  cursor: pointer;
  z-index: 2;
}

.volume-control {
  position: relative;
  flex-shrink: 0;
}

.volume-button {
  width: 28px;
  height: 28px;
  background: none;
  border: none;
  border-radius: 50%;
  padding: 0;
  color: var(--x-text-muted);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.volume-button:hover {
  color: var(--x-text);
  background: var(--x-canvas);
}

.volume-slider-container {
  position: fixed;
  padding: 10px 4px;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  box-shadow: var(--x-shadow-raised);
  z-index: 1000;
  height: 75px;
  box-sizing: content-box;
}

.volume-slider-track {
  position: relative;
  height: 55px;
  width: 3px;
  border-radius: 2px;
  background: var(--x-border-control);
  margin: 1px;
}

.volume-slider-thumb {
  position: absolute;
  left: 1.5px;
  width: 9px;
  height: 9px;
  background: var(--x-ink);
  border: 2px solid var(--x-surface);
  border-radius: 50%;
  transform: translate(-50%, 50%);
  pointer-events: none;
}

.volume-slider {
  position: absolute;
  left: -14px;
  width: 30px;
  height: 60px;
  writing-mode: vertical-lr;
  direction: rtl;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

/* Narrow cells (e.g. the voice table): drop the time readout first */
@container (max-width: 200px) {
  .time-display {
    display: none;
  }
}

.volume-slider-track::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 3px;
  border-radius: 2px;
  height: calc(100% * v-bind(volume));
  background: var(--x-ink);
}
</style>
