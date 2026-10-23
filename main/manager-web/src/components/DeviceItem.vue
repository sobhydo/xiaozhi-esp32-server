<template>
  <article class="device-item">
    <div class="card-head">
      <div class="agent-initial" aria-hidden="true">{{ agentInitial }}</div>
      <div class="agent-meta">
        <el-tooltip :content="device.agentName" placement="top" effect="light">
          <h2 class="device-item-title">{{ device.agentName }}</h2>
        </el-tooltip>
        <div class="agent-devices">
          <span :class="['status-dot', { 'is-active': device.deviceCount > 0 }]" aria-hidden="true"></span>
          {{ $t('home.deviceManagement') }} · {{ device.deviceCount || 0 }}
        </div>
      </div>
      <div class="card-tools">
        <el-tooltip effect="light" :content="device.systemPrompt" placement="top" popper-class="device-item-tooltip">
          <button type="button" class="icon-btn" :aria-label="$t('roleConfig.roleIntroduction')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 11v5M12 8h.01" />
            </svg>
          </button>
        </el-tooltip>
        <button type="button" class="icon-btn icon-btn--danger" :aria-label="$t('common.delete')"
          @click.stop="handleDelete">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
            stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <path d="M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V4h6v3" />
          </svg>
        </button>
      </div>
    </div>

    <dl class="agent-details">
      <dt>{{ $t('home.languageModel') }}</dt>
      <dd :title="device.llmModelName">{{ device.llmModelName || '-' }}</dd>
      <dt>{{ $t('home.voiceModel') }}</dt>
      <dd :title="`${device.ttsModelName || ''} (${device.ttsVoiceName || ''})`">
        {{ device.ttsModelName || '-' }}<template v-if="device.ttsVoiceName"> · {{ device.ttsVoiceName }}</template>
      </dd>
      <dt>{{ $t('home.lastConversation') }}</dt>
      <dd>{{ formattedLastConnectedTime }}</dd>
    </dl>

    <ul v-if="tags.length" class="agent-tags">
      <li v-for="(tag, index) in tags" :key="index" class="agent-tag">{{ tag }}</li>
    </ul>

    <div class="card-actions">
      <button type="button" class="action-btn action-btn--outline" @click="handleConfigure">
        {{ $t('home.configureRole') }}
      </button>
      <button v-if="featureStatus.voiceprintRecognition" type="button" class="action-btn" @click="handleVoicePrint">
        {{ $t('home.voiceprintRecognition') }}
      </button>
      <button type="button" class="action-btn" @click="handleDeviceManage">
        {{ $t('home.deviceManagement') }} ({{ device.deviceCount }})
      </button>
      <el-tooltip v-if="isMemoryDisabled" effect="light" :content="$t('home.enableMemory')" placement="top">
        <button type="button" class="action-btn is-disabled" aria-disabled="true" @click="handleChatHistory">
          {{ $t('home.chatHistory') }}
        </button>
      </el-tooltip>
      <button v-else type="button" class="action-btn" @click="handleChatHistory">
        {{ $t('home.chatHistory') }}
      </button>
    </div>
  </article>
</template>

<script>
import i18n from '@/i18n';

export default {
  name: 'DeviceItem',
  props: {
    device: { type: Object, required: true },
    featureStatus: { 
      type: Object, 
      default: () => ({
        voiceprintRecognition: false,
        voiceClone: false,
        knowledgeBase: false
      })
    }
  },
  data() {
    return { switchValue: false }
  },
  computed: {
    formattedLastConnectedTime() {
      if (!this.device.lastConnectedAt) return this.$t('home.noConversation');

      const lastTime = new Date(this.device.lastConnectedAt);
      const now = new Date();
      const diffMinutes = Math.floor((now - lastTime) / (1000 * 60));

      if (diffMinutes <= 1) {
        return this.$t('home.justNow');
      } else if (diffMinutes < 60) {
        return this.$t('home.minutesAgo', { minutes: diffMinutes });
      } else if (diffMinutes < 24 * 60) {
        const hours = Math.floor(diffMinutes / 60);
        const minutes = diffMinutes % 60;
        return this.$t('home.hoursAgo', { hours, minutes });
      } else {
        return this.device.lastConnectedAt;
      }
    },
    agentInitial() {
      const name = (this.device.agentName || '').trim();
      return name ? Array.from(name)[0].toUpperCase() : '?';
    },
    isMemoryDisabled() {
      return this.device.memModelId === 'Memory_nomem';
    },
    tags() {
      if (!this.device.tags) return [];
      return this.device.tags.map((tag) => tag.tagName);
    }
  },
  methods: {
    handleDelete() {
      this.$emit('delete', this.device)
    },
    handleConfigure() {
      this.$router.push({ path: '/role-config', query: { agentId: this.device.agentId } });
    },
    handleVoicePrint() {
      this.$router.push({ path: '/voice-print', query: { agentId: this.device.agentId } });
    },
    handleDeviceManage() {
      this.$router.push({ path: '/device-management', query: { agentId: this.device.agentId } });
    },
    handleChatHistory() {
      if (this.device.memModelId === 'Memory_nomem') {
        return
      }
      this.$emit('chat-history', { agentId: this.device.agentId, agentName: this.device.agentName })
    }
  },
}
</script>

<style lang="scss" scoped>
.device-item {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  box-sizing: border-box;
  text-align: left;
  transition: border-color 0.15s ease;

  &:hover {
    border-color: var(--x-border-control);
  }
}

.card-head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 12px 12px 16px;
}

.agent-initial {
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

.agent-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.device-item-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 22px;
  color: var(--x-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-devices {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--x-text-muted);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--x-border-control);
  flex-shrink: 0;

  &.is-active {
    background: var(--x-success);
  }
}

.card-tools {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

.icon-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

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

.agent-details {
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
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

.agent-tags {
  list-style: none;
  margin: 0;
  padding: 0 16px 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.agent-tag {
  max-width: 100%;
  font-size: 12px;
  line-height: 18px;
  padding: 2px 8px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  margin-top: auto;
  display: flex;
  flex-wrap: wrap;
  gap: 0;
  padding: 10px 8px;
  border-top: 1px solid var(--x-divider);
}

.action-btn {
  height: 32px;
  padding: 0 6px;
  border: 1px solid transparent;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-secondary);
  font: inherit;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;

  &:hover,
  &:focus-visible {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &--outline {
    border-color: var(--x-border-control);
    color: var(--x-text);
    font-weight: 500;

    &:hover,
    &:focus-visible {
      background: var(--x-surface-raised);
      border-color: var(--x-text-disabled);
    }
  }

  &.is-disabled,
  &.is-disabled:hover {
    background: transparent;
    color: var(--x-text-muted);
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>

<style>
.device-item-tooltip {
  max-height: 60vh !important;
  max-width: min(400px, calc(100vw - 32px)) !important;
  overflow-y: auto !important;
  scrollbar-width: thin;
  word-break: break-word;
}

.device-item-tooltip .popper__arrow {
  display: none !important;
}

.device-item-tooltip[x-placement^="top"] .popper__arrow {
  border-top-color: transparent !important;
}

.device-item-tooltip[x-placement^="bottom"] .popper__arrow {
  border-bottom-color: transparent !important;
}
</style>
