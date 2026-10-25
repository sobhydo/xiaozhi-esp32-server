<template>
    <CustomDialog
        :title="$t('chatHistory.with') + agentName + $t('chatHistory.dialogTitle')"
        :visible.sync="dialogVisible"
        width="80%"
        size="large"
        :footer="false"
        :close-on-click-modal="false"
        custom-class="chat-history-dialog">
        <template v-slot:title>
            <span class="chat-title">
                {{ $t('chatHistory.with') + agentName + $t('chatHistory.dialogTitle') }}
                <span v-if="currentMacAddress" class="chat-title-mac">
                    <MacAddressMask :macAddress="currentMacAddress" />
                </span>
            </span>
        </template>
        <div class="chat-container">
            <div class="session-list" @scroll="handleScroll">
                <button v-for="session in sessions" :key="session.sessionId" type="button" class="session-item"
                    :class="{ active: currentSessionId === session.sessionId }"
                    :aria-current="currentSessionId === session.sessionId ? 'true' : null"
                    @click="selectSession(session)">
                    <span class="avatar avatar--user" aria-hidden="true">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                            stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="8" r="4" />
                            <path d="M4 21a8 8 0 0 1 16 0" />
                        </svg>
                    </span>
                    <span class="session-time">{{ session.title || formatTime(session.createdAt) }}</span>
                    <span class="message-count">{{ session.chatCount > 99 ? '99' : session.chatCount }}</span>
                </button>
                <div v-if="loading" class="loading">{{ $t('chatHistory.loading') }}</div>
                <div v-if="!hasMore" class="no-more">{{ $t('chatHistory.noMoreRecords') }}</div>
            </div>
            <div class="chat-pane">
                <div class="chat-content">
                    <div v-if="currentSessionId" class="messages">
                        <div v-for="(message, index) in messagesWithTime" :key="message.id">
                            <div v-if="message.type === 'time'" class="time-divider">
                                <span>{{ message.content }}</span>
                            </div>
                            <div v-else class="message-item" :class="{ 'user-message': message.chatType === 1, 'tool-message': message.chatType === 3 }">
                                <span v-if="message.chatType === 1" class="avatar avatar--user" aria-hidden="true">
                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                                        stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                                        <circle cx="12" cy="8" r="4" />
                                        <path d="M4 21a8 8 0 0 1 16 0" />
                                    </svg>
                                </span>
                                <span v-else class="avatar avatar--agent" aria-hidden="true">
                                    <XMark :size="16" inverse />
                                </span>
                                <div class="message-content">
                                    <template v-if="Array.isArray(extractContentFromString(message.content))">
                                        <div class="content-wrapper">
                                            <div v-for="(item, idx) in extractContentFromString(message.content)" :key="idx">
                                                <div v-if="item.type === 'text'" class="text-content">{{ item.text }}</div>
                                                <div v-else-if="item.type === 'tool'" class="tool-call-text">{{ item.text }}</div>
                                                <div v-else-if="item.type === 'tool_result'" class="tool-call-text">
                                                    <div v-if="item.text && item.text.length > 80" class="tool-result-wrapper">
                                                        <div v-if="isToolResultCollapsed(index, idx)" class="tool-result-collapsed">
                                                            {{ getFirstLineText(item.text) }}
                                                        </div>
                                                        <div v-else class="tool-result-expanded">
                                                            {{ item.text }}
                                                        </div>
                                                        <button type="button" class="tool-toggle-btn"
                                                            :aria-expanded="isToolResultCollapsed(index, idx) ? 'false' : 'true'"
                                                            :aria-label="$t('chatHistory.expandToolResult')"
                                                            @click="toggleToolResult(index, idx)">
                                                            <i :class="isToolResultCollapsed(index, idx) ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" aria-hidden="true"></i>
                                                        </button>
                                                    </div>
                                                    <div v-else>{{ item.text }}</div>
                                                </div>
                                            </div>
                                        </div>
                                    </template>
                                    <template v-else>
                                        <span class="text-content">{{ extractContentFromString(message.content) }}</span>
                                    </template>
                                    <button v-if="message.audioId" type="button" class="audio-btn"
                                        :class="{ 'is-playing': playingAudioId === message.audioId }"
                                        :aria-label="$t('chatHistory.playAudio')"
                                        @click="playAudio(message)">
                                        <i :class="getAudioIconClass(message)" class="audio-icon" aria-hidden="true"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div v-else class="no-session-selected">
                        {{ $t('chatHistory.selectSession') }}
                    </div>
                </div>
                <div v-if="currentSessionId" class="download-buttons">
                    <el-button size="small" icon="el-icon-download" @click="downloadCurrentSessionWithPrevious">
                        {{ $t('chatHistory.downloadCurrentWithPreviousSessions') }}
                    </el-button>
                    <el-button size="small" icon="el-icon-download" @click="downloadCurrentSession">
                        {{ $t('chatHistory.downloadCurrentSession') }}
                    </el-button>
                </div>
            </div>
        </div>
    </CustomDialog>
</template>

<script>
import { debounce } from '@/utils'
import Api from '@/apis/api';
import CustomDialog from '@/components/CustomDialog.vue';
import MacAddressMask from '@/components/MacAddressMask.vue';
import XMark from '@/components/XMark.vue';

export default {
    name: 'ChatHistoryDialog',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        agentId: {
            type: String,
            required: true
        },
        agentName: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            dialogVisible: false,
            sessions: [],
            messages: [],
            currentSessionId: '',
            currentMacAddress: '',
            page: 1,
            limit: 20,
            loading: false,
            hasMore: true,
            scrollTimer: null,
            isFirstLoad: true,
            playingAudioId: null,
            audioElement: null,
            expandedToolResults: {} // Tracks the expanded state of tool results
        };
    },
    components: {
        CustomDialog,
        MacAddressMask,
        XMark,
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
            if (val) {
                this.resetData();
                this.loadSessions();
            } else {
                this.audioElement?.pause();
                this.audioElement = null;
                this.playingAudioId = null;
            }
        },
        dialogVisible(val) {
            if (!val) {
                this.$emit('update:visible', false);
            }
        }
    },
    computed: {
        messagesWithTime() {
            if (!this.messages || this.messages.length === 0) return [];

            const result = [];
            const TIME_INTERVAL = 60 * 1000; // 1-minute interval (milliseconds)

            // Add a timestamp marker for the first message
            if (this.messages[0]) {
                result.push({
                    type: 'time',
                    content: this.formatTime(this.messages[this.messages.length - 1].createdAt),
                    id: `time-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
                });
            }

            // Process the message list
            for (let i = 0; i < this.messages.length; i++) {
                const currentMessage = this.messages[i];
                result.push(currentMessage);

                // Check whether a timestamp marker is needed
                if (i < this.messages.length - 1) {
                    const currentTime = new Date(currentMessage.createdAt).getTime();
                    const nextTime = new Date(this.messages[i + 1].createdAt).getTime();

                    if (nextTime - currentTime > TIME_INTERVAL) {
                        result.push({
                            type: 'time',
                            content: this.formatTime(this.messages[i + 1].createdAt),
                            id: `time-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
                        });
                    }
                }
            }

            return result;
        }
    },
    methods: {
        /**
         * Extract the chat content from the content field
         * If content is JSON (e.g. {"speaker": "Unknown speaker", "content": "What time is it?"}), extract its content field
         * If content is a plain string, return it as-is
         * 
         * @param {string} content Raw content
         * @returns {string} Extracted chat content
         */
        extractContentFromString(content) {
            if (!content || content.trim() === '') {
                return content;
            }

            // Try to parse as JSON
            try {
                const jsonObj = JSON.parse(content);

                // If it is an array (containing text and tool)
                if (Array.isArray(jsonObj)) {
                    return jsonObj;
                }

                // If it is an object with a content field
                if (jsonObj && typeof jsonObj === 'object' && jsonObj.content) {
                    return jsonObj.content;
                }
            } catch (e) {
                // Not valid JSON: return the original content
            }

            // Not JSON or no content field: return the original content
            return content;
        },
        // Toggle expand/collapse of a tool result
        toggleToolResult(messageIndex, itemIndex) {
            const key = `${messageIndex}-${itemIndex}`;
            this.$set(this.expandedToolResults, key, !this.expandedToolResults[key]);
        },
        // Check whether a tool result is collapsed
        isToolResultCollapsed(messageIndex, itemIndex) {
            const key = `${messageIndex}-${itemIndex}`;
            // Collapsed by default (true means collapsed)
            return !this.expandedToolResults[key];
        },
        // Get truncated text (first line only)
        getFirstLineText(text) {
            if (!text) return '';
            const firstLine = text.split('\n')[0];
            return firstLine.length < text.length ? firstLine + '...' : text;
        },
        resetData() {
            this.sessions = [];
            this.messages = [];
            this.currentSessionId = '';
            this.currentMacAddress = '';
            this.page = 1;
            this.loading = false;
            this.hasMore = true;
            this.isFirstLoad = true;
            this.expandedToolResults = {};
        },
        loadSessions() {
            if (this.loading || (!this.isFirstLoad && !this.hasMore)) {
                return;
            }

            this.loading = true;
            const params = {
                page: this.page,
                limit: this.limit
            };

            Api.agent.getAgentSessions(this.agentId, params, (res) => {
                if (res.data && res.data.data && Array.isArray(res.data.data.list)) {
                    const list = res.data.data.list;
                    this.hasMore = list.length === this.limit;

                    this.sessions = [...this.sessions, ...list];
                    this.page++;

                    if (this.sessions.length > 0 && !this.currentSessionId) {
                        this.selectSession(this.sessions[0]);
                    }
                }
                this.loading = false;
                this.isFirstLoad = false;
            });
        },
        selectSession(session) {
            this.currentSessionId = session.sessionId;
            Api.agent.getAgentChatHistory(this.agentId, session.sessionId, (res) => {
                if (res.data && res.data.data) {
                    this.messages = res.data.data;
                    if (this.messages.length > 0 && this.messages[0].macAddress) {
                        this.currentMacAddress = this.messages[0].macAddress;
                    }
                    // Update the chat count in the session list
                    this.sessions = this.sessions.map(item => {
                        if (item.sessionId === session.sessionId) {
                            item.chatCount = this.messages.length;
                        }
                        return item;
                    })
                }
            });
        },
        handleScroll(e) {
            if (this.scrollTimer) {
                clearTimeout(this.scrollTimer);
            }

            this.scrollTimer = setTimeout(() => {
                const { scrollTop, scrollHeight, clientHeight } = e.target;
                // Load more when scrolled to the bottom
                if (scrollHeight - scrollTop <= clientHeight + 50) {
                    this.loadSessions();
                }
            }, 200);
        },
        formatTime(timestamp) {
            const date = new Date(timestamp);
            const now = new Date();
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            const yesterday = new Date(today);
            yesterday.setDate(yesterday.getDate() - 1);

            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');

            if (date >= today) {
                return `${this.$t('chatHistory.today')} ${hours}:${minutes}`;
            } else if (date >= yesterday) {
                return `${this.$t('chatHistory.yesterday')} ${hours}:${minutes}`;
            } else {
                const year = date.getFullYear();
                const month = (date.getMonth() + 1).toString().padStart(2, '0');
                const day = date.getDate().toString().padStart(2, '0');
                return `${year}-${month}-${day} ${hours}:${minutes}`;
            }
        },
        getAudioIconClass(message) {
            if (this.playingAudioId === message.audioId) {
                return 'el-icon-loading';
            }
            return 'el-icon-video-play';
        },
        playAudio: debounce(function(message) {
            if (this.playingAudioId === message.audioId) {
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
            this.playingAudioId = message.audioId;
            Api.agent.getAudioId(message.audioId, (res) => {
                if (res.data && res.data.data) {
                    if (!this.audioElement) {
                        this.audioElement = new Audio();
                    }
                    
                    // Play the audio using the download ID
                    this.audioElement.src = Api.getServiceUrl() + `/agent/play/${res.data.data}`;
                    this.audioElement.onended = () => {
                        this.playingAudioId = null;
                        this.audioElement = null;
                    };

                    this.audioElement.play();
                }
            });
        }, 300),
        // Download this session's chat history
        downloadCurrentSession() {
            Api.agent.getDownloadUrl(this.agentId, this.currentSessionId, (res) => {
                if (res && res.data && res.data.code === 0 && res.data.data) {
                    const uuid = res.data.data;
                    window.open(`${Api.getServiceUrl()}/agent/chat-history/download/${uuid}/current`, '_blank');
                } else {
                    this.$message.error(this.$t('chatHistory.downloadLinkFailed'));
                }
            });
        },

        // Download chat history for this session and the previous 20 sessions
        downloadCurrentSessionWithPrevious() {
            Api.agent.getDownloadUrl(this.agentId, this.currentSessionId, (res) => {
                if (res && res.data && res.data.code === 0 && res.data.data) {
                    const uuid = res.data.data;
                    window.open(`${Api.getServiceUrl()}/agent/chat-history/download/${uuid}/previous`, '_blank');
                } else {
                    this.$message.error(this.$t('chatHistory.downloadLinkFailed'));
                }
            });
        }
    }
};
</script>

<style scoped>
.chat-title {
    display: inline-flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    font-size: 16px;
    font-weight: 600;
    line-height: 24px;
    color: var(--x-text);
    padding-inline-end: 32px;
}

.chat-title-mac {
    padding: 1px 8px;
    border-radius: var(--x-radius-pill);
    background: var(--x-divider);
    color: var(--x-text-secondary);
    font-family: var(--x-font-mono);
    font-size: 12px;
    font-weight: 500;
    line-height: 18px;
}

.chat-container {
    display: flex;
    flex: 1;
    min-height: 0;
    text-align: start;
}

.session-list {
    width: 260px;
    min-height: 0;
    flex-shrink: 0;
    box-sizing: border-box;
    border-inline-end: 1px solid var(--x-border);
    background: var(--x-surface-raised);
    overflow-y: auto;
    padding: 8px;
}

.session-item {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px;
    margin-bottom: 2px;
    border: 0;
    border-radius: var(--x-radius-control);
    background: transparent;
    font: inherit;
    color: inherit;
    text-align: start;
    cursor: pointer;
    transition: background-color 0.15s ease;
}

.session-item:hover {
    background: var(--x-canvas);
}

.session-item:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
}

.session-item.active {
    background: var(--x-selected);
}

.avatar {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.avatar--user {
    background: var(--x-divider);
    color: var(--x-text-secondary);
}

.session-item.active .avatar--user {
    background: var(--x-surface);
}

.avatar--agent {
    background: var(--x-ink);
}

.session-time {
    flex: 1;
    min-width: 0;
    font-size: 13px;
    font-weight: 500;
    color: var(--x-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.message-count {
    min-width: 22px;
    padding: 0 6px;
    box-sizing: border-box;
    border-radius: var(--x-radius-pill);
    background: var(--x-surface);
    border: 1px solid var(--x-border);
    color: var(--x-text-secondary);
    font-size: 12px;
    font-weight: 500;
    line-height: 18px;
    text-align: center;
}

.chat-pane {
    flex: 1;
    min-width: 0;
    min-height: 0;
    display: flex;
    flex-direction: column;
}

.chat-content {
    flex: 1;
    min-height: 0;
    padding: 20px 24px;
    overflow-y: auto;
}

.message-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 16px;
}

.message-item.user-message {
    flex-direction: row-reverse;
}

.message-content {
    max-width: 70%;
    padding: 10px 14px;
    border-radius: 14px;
    border-top-left-radius: 4px;
    background: var(--x-canvas);
    color: var(--x-text);
    font-size: 14px;
    line-height: 1.55;
    text-align: start;
    display: flex;
    align-items: center;
    gap: 8px;
    word-break: break-word;
}

.user-message .message-content {
    background: var(--x-ink);
    color: var(--x-on-ink);
    border-top-left-radius: 14px;
    border-top-right-radius: 4px;
    flex-direction: row-reverse;
}

.tool-message .message-content {
    background: var(--x-surface);
    border: 1px solid var(--x-border);
}

.audio-btn {
    width: 28px;
    height: 28px;
    flex-shrink: 0;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: var(--x-surface);
    color: var(--x-ink);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
}

.audio-btn:hover,
.audio-btn:focus-visible {
    box-shadow: 0 0 0 3px var(--x-focus-ring);
    outline: none;
}

.user-message .audio-btn {
    background: rgba(255, 255, 255, 0.16);
    color: var(--x-on-ink);
}

.audio-icon {
    font-size: 18px;
}

.content-wrapper {
    width: 100%;
    min-width: 0;
}

.text-content {
    display: block;
    white-space: pre-wrap;
}

.content-wrapper .text-content {
    margin-bottom: 4px;
}

.tool-call-text {
    display: block;
    margin-top: 4px;
    font-family: var(--x-font-mono);
    font-size: 12px;
    color: var(--x-text-secondary);
}

.user-message .tool-call-text {
    color: var(--x-night-text);
}

.tool-result-wrapper {
    position: relative;
    padding-inline-end: 28px;
}

.tool-result-collapsed {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.tool-result-expanded {
    white-space: pre-wrap;
}

.tool-toggle-btn {
    position: absolute;
    right: 0;
    top: -2px;
    width: 22px;
    height: 22px;
    padding: 0;
    border: 0;
    border-radius: 6px;
    background: transparent;
    color: inherit;
    font-size: 12px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
}

.tool-toggle-btn:hover,
.tool-toggle-btn:focus-visible {
    background: var(--x-divider);
    color: var(--x-text);
    outline: none;
}

.loading,
.no-more {
    text-align: center;
    padding: 10px 10px 24px;
    font-size: 12px;
    color: var(--x-text-muted);
}

.no-session-selected {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    font-size: 14px;
    color: var(--x-text-muted);
}

.time-divider {
    display: flex;
    align-items: center;
    gap: 12px;
    margin: 12px 0 16px;
    font-size: 12px;
    color: var(--x-text-muted);
    white-space: nowrap;
}

.time-divider::before,
.time-divider::after {
    content: '';
    flex: 1;
    height: 1px;
    background: var(--x-divider);
}

.download-buttons {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 8px;
    padding: 12px 24px;
    border-top: 1px solid var(--x-border);
    background: var(--x-surface);
}

.download-buttons .el-button + .el-button {
    margin-inline-start: 0;
}

@media (max-width: 768px) {
    .chat-container {
        flex-direction: column;
    }

    .session-list {
        width: 100%;
        max-height: 168px;
        border-inline-end: 0;
        border-bottom: 1px solid var(--x-border);
    }

    .chat-content {
        padding: 16px;
    }

    .message-content {
        max-width: 82%;
    }

    .download-buttons {
        padding: 12px 16px;
        justify-content: stretch;
    }

    .download-buttons .el-button {
        flex: 1 1 100%;
        white-space: normal;
        word-break: normal;
        overflow-wrap: anywhere;
        height: auto;
        min-height: 32px;
        line-height: 1.4;
    }
}
</style>

<style>
.chat-history-dialog {
    min-width: min(700px, calc(100vw - 32px));
    max-width: min(85vw, calc(100vw - 32px));
    /* Fallback: keep the dialog from shrinking to content height if the size="large" height does not apply */
    height: calc(100vh - 20px) !important;
}

.chat-history-dialog .el-dialog__body {
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    /* Override the size="large" default of 100vh - 130px so the body reaches the dialog bottom; flex: 1 fills the space */
    max-height: calc(100vh - 74px) !important;
}

@media (max-width: 768px) {
    .chat-history-dialog {
        width: calc(100vw - 32px) !important;
        max-width: calc(100vw - 32px);
    }

    /* The title can wrap on narrow screens; let flex size the body instead of a fixed offset */
    .chat-history-dialog .el-dialog__body {
        max-height: none !important;
    }
}
</style>