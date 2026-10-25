<template>
  <div class="welcome">
    <HeaderBar />

    <main class="page">
      <header class="page-header">
        <div class="page-heading">
          <h1 class="page-title">{{ $t('header.featureManagement') }}</h1>
          <p class="page-description">{{ $t('console.featuresDescription') }}</p>
        </div>
        <div class="page-actions">
          <el-button :icon="isAllSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'" @click="!isSaving && toggleSelectAll()" :disabled="isSaving">
            {{ isAllSelected ? $t('featureManagement.deselectAll') : $t('featureManagement.selectAll') }}
          </el-button>
          <el-button icon="el-icon-refresh" @click="handleReset" :disabled="isSaving">
            {{ $t('featureManagement.reset') }}
          </el-button>
          <el-button type="primary" icon="el-icon-check" @click="handleSave" :disabled="isSaving" :loading="isSaving">
            {{ isSaving ? $t('featureManagement.saving') : $t('featureManagement.save') }}
          </el-button>
        </div>
      </header>

      <!-- Feature groups -->
      <div class="feature-groups-container">
        <section v-if="featureManagementFeatures.length > 0" class="feature-group">
          <header class="group-head">
            <div class="group-header">
              <h2 class="group-name">{{ $t('featureManagement.groupName.featureManagement') }}</h2>
              <span class="module-count">{{ $t('featureManagement.moduleCount', { count: featureManagementFeatures.length }) }}</span>
            </div>
            <p class="group-description">{{ $t('featureManagement.groupDescription.featureManagement') }}</p>
          </header>
          <ul class="feature-list">
            <li
              v-for="feature in featureManagementFeatures"
              :key="feature.id"
              class="feature-row"
              :class="{ 'feature-enabled': feature.enabled, 'feature-disabled': isSaving }"
            >
              <span class="feature-icon" aria-hidden="true">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path v-for="(d, n) in featureIconPaths[feature.id] || featureIconPaths.default" :key="n" :d="d" />
                </svg>
              </span>
              <label class="feature-content" :for="'feature-switch-' + feature.id">
                <span class="feature-name">{{ $t(`feature.${feature.id}.name`) }}</span>
                <span class="feature-description">{{ $t(`feature.${feature.id}.description`) }}</span>
              </label>
              <el-switch
                :id="'feature-switch-' + feature.id"
                :value="feature.enabled"
                :disabled="isSaving"
                class="feature-switch"
                @change="!isSaving && toggleFeature(feature)"
              />
            </li>
          </ul>
        </section>

        <section v-if="voiceManagementFeatures.length > 0" class="feature-group">
          <header class="group-head">
            <div class="group-header">
              <h2 class="group-name">{{ $t('featureManagement.groupName.voiceManagement') }}</h2>
              <span class="module-count">{{ $t('featureManagement.moduleCount', { count: voiceManagementFeatures.length }) }}</span>
            </div>
            <p class="group-description">{{ $t('featureManagement.groupDescription.voiceManagement') }}</p>
          </header>
          <ul class="feature-list">
            <li
              v-for="feature in voiceManagementFeatures"
              :key="feature.id"
              class="feature-row"
              :class="{ 'feature-enabled': feature.enabled, 'feature-disabled': isSaving }"
            >
              <span class="feature-icon" aria-hidden="true">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                  stroke-linecap="round" stroke-linejoin="round">
                  <path v-for="(d, n) in featureIconPaths[feature.id] || featureIconPaths.default" :key="n" :d="d" />
                </svg>
              </span>
              <label class="feature-content" :for="'feature-switch-' + feature.id">
                <span class="feature-name">{{ $t(`feature.${feature.id}.name`) }}</span>
                <span class="feature-description">{{ $t(`feature.${feature.id}.description`) }}</span>
              </label>
              <el-switch
                :id="'feature-switch-' + feature.id"
                :value="feature.enabled"
                :disabled="isSaving"
                class="feature-switch"
                @change="!isSaving && toggleFeature(feature)"
              />
            </li>
          </ul>
        </section>
      </div>

      <div v-if="filteredFeatures.length === 0" class="empty-state">
        <el-empty :description="$t('featureManagement.noFeatures')" :image-size="80">
          <p class="empty-tip">{{ $t('featureManagement.contactAdmin') }}</p>
        </el-empty>
      </div>
    </main>

    <el-footer>
      <VersionFooter />
    </el-footer>
  </div>
</template>

<script>
import HeaderBar from "@/components/HeaderBar.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import featureManager from "@/utils/featureManager.js";

export default {
  name: "FeatureManagement",
  components: {
    HeaderBar,
    VersionFooter
  },
  data() {
    return {
      pendingChanges: false,
      featureManagementFeatures: [],
      voiceManagementFeatures: [],
      isSaving: false, // Saving-state lock
      // Stroke icon paths (24px grid) for each feature module
      featureIconPaths: {
        voiceprintRecognition: ['M2 12h2', 'M6 8v8', 'M10 5v14', 'M14 9v6', 'M18 7v10', 'M22 12h-2'],
        voiceClone: ['M9 3a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z', 'M3 10v1a6 6 0 0 0 12 0v-1', 'M9 17v4', 'M18 8h3', 'M18 12h3', 'M18 16h3'],
        knowledgeBase: ['M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2V5z', 'M4 19a2 2 0 0 1 2-2h13', 'M9 7h6'],
        mcpAccessPoint: ['M9 2v5', 'M15 2v5', 'M6 7h12v4a6 6 0 0 1-12 0V7z', 'M12 17v5'],
        addressBook: ['M6 3h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6V3z', 'M3 8h3', 'M3 16h3', 'M13 11a2 2 0 1 0 0-4 2 2 0 0 0 0 4z', 'M9.5 16a3.5 3.5 0 0 1 7 0'],
        vad: ['M3 12h4l3-8 4 16 3-8h4'],
        asr: ['M12 3a3 3 0 0 0-3 3v5a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3z', 'M5 10v1a7 7 0 0 0 14 0v-1', 'M12 18v3'],
        default: ['M12 3l8 4.5v9L12 21l-8-4.5v-9L12 3z']
      },
    }
  },
  computed: {
    // List of all features
    filteredFeatures() {
      return [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
    },
    
    // Whether all features are selected
    isAllSelected() {
      const allFeatures = [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
      return allFeatures.length > 0 && allFeatures.every(feature => feature.enabled)
    }
  },
  async created() {
    // Wait for the feature config manager to finish initializing
    try {
      await featureManager.waitForInitialization()
      await this.loadFeatures()
      this.setupConfigChangeListener()
    } catch (error) {
      console.error('Failed waiting for feature config manager initialization:', error)
      await this.loadFeatures()
      this.setupConfigChangeListener()
    }
  },
  
  beforeDestroy() {
    this.removeConfigChangeListener()
  },
  
  methods: {
    // Get features by ID list
    async getFeaturesByIds(featureIds) {
      try {
        const featureConfig = await featureManager.getAllFeatures()
        const result = featureIds.map(id => {
          const feature = featureConfig[id]
          return {
            id: id,
            name: this.$t(`feature.${id}.name`),
            description: this.$t(`feature.${id}.description`),
            enabled: feature?.enabled || false
          }
        })
        
        return result
      } catch (error) {
        console.error('Failed to get feature config:', error)
        // If fetching fails, return the default config
        return featureIds.map(id => ({
          id: id,
          name: this.$t(`feature.${id}.name`),
          description: this.$t(`feature.${id}.description`),
          enabled: false
        }))
      }
    },
    
    // Load feature config
    async loadFeatures() {
      // Save the user's current selection state
      const currentFeatureStates = {}
      const allCurrentFeatures = [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
      allCurrentFeatures.forEach(feature => {
        currentFeatureStates[feature.id] = feature.enabled
      })
      
      // Reload config
      this.featureManagementFeatures = await this.getFeaturesByIds(['voiceprintRecognition', 'voiceClone', 'knowledgeBase', 'mcpAccessPoint', 'addressBook'])
      this.voiceManagementFeatures = await this.getFeaturesByIds(['vad', 'asr'])
      
      // Restore the user's selection state (if any)
      const allFeatures = [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
      allFeatures.forEach(feature => {
        if (currentFeatureStates.hasOwnProperty(feature.id)) {
          feature.enabled = currentFeatureStates[feature.id]
        }
      })
    },
    // Toggle feature state
    async toggleFeature(feature) {
      // If saving is in progress, block the action
      if (this.isSaving) {
        return
      }
      
      feature.enabled = !feature.enabled
      this.pendingChanges = true
      
      // No longer pushed to the config manager immediately; updated together on save
    },
    // Save config
    async handleSave() {
      if (!this.pendingChanges) {
        this.$message.info({
          message: this.$t('featureManagement.noChanges'),
          showClose: true
        })
        return
      }
      
      // Set saving state and lock the UI
      this.isSaving = true
      
      try {
        // Collect the current state of all features and save
        const featureUpdates = {}
        const allFeatures = [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
        allFeatures.forEach(feature => {
          featureUpdates[feature.id] = feature.enabled
        })
        await featureManager.updateFeatures(featureUpdates)
        
        this.pendingChanges = false
        this.$message.success({
          message: this.$t('featureManagement.saveSuccess'),
          showClose: true
        })

        setTimeout(() => {
          this.loadFeatures()
        }, 1000)
      } catch (error) {
        console.error('Failed to save config:', error)
        this.$message.error({
          message: this.$t('featureManagement.saveError'),
          showClose: true
        })
      } finally {
        // Release the saving lock regardless of outcome
        this.isSaving = false
      }
    },
    // Set up the config change listener
    setupConfigChangeListener() {
      this.configChangeHandler = () => {
        this.loadFeatures()
      }
      window.addEventListener('featureConfigReloaded', this.configChangeHandler)
    },
    
    // Remove the config change listener
    removeConfigChangeListener() {
      if (this.configChangeHandler) {
        window.removeEventListener('featureConfigReloaded', this.configChangeHandler)
      }
    },
    
    // Reset config
    async handleReset() {
      try {
        await this.$confirm(
          this.$t('featureManagement.resetConfirm'),
          this.$t('featureManagement.reset'),
          {
            confirmButtonText: this.$t('featureManagement.confirm'),
            cancelButtonText: this.$t('featureManagement.cancel'),
            type: 'warning'
          }
        )
        
        featureManager.resetToDefault()
        this.loadFeatures()
        this.pendingChanges = false
        
        this.$message.success({
          message: this.$t('featureManagement.resetSuccess'),
          showClose: true
        })
        
        setTimeout(() => {
          this.loadFeatures()
          this.$router.go(0)
        }, 1000)
      } catch (error) {
        // User cancelled
      }
    },
    // Search (reserved hook)
    handleSearch() {
      // Search not yet implemented
    },
    // Select all / deselect all
    toggleSelectAll() {
      // If saving is in progress, block the action
      if (this.isSaving) {
        return
      }
      
      const allFeatures = [...this.featureManagementFeatures, ...this.voiceManagementFeatures]
      const newStatus = !this.isAllSelected
      
      allFeatures.forEach(feature => {
        feature.enabled = newStatus
      })
      
      this.pendingChanges = true
    }
  }
}
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

.feature-groups-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 20px;
  align-items: start;
}

.feature-group {
  min-width: 0;
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
  overflow: hidden;
}

.group-head {
  padding: 16px 20px 14px;
  border-bottom: 1px solid var(--x-border);
}

.group-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
}

.group-name {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--x-text);
}

.module-count {
  height: 20px;
  padding: 0 8px;
  border-radius: var(--x-radius-pill);
  background: var(--x-divider);
  color: var(--x-text-muted);
  font-size: 12px;
  font-weight: 500;
  line-height: 20px;
}

.group-description {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

.feature-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.feature-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--x-divider);
  transition: background 0.15s;

  &:last-child {
    border-bottom: 0;
  }

  &:hover {
    background: var(--x-surface-raised);
  }

  &.feature-disabled {
    opacity: 0.7;
  }
}

.feature-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: var(--x-canvas);
  color: var(--x-text-muted);
  transition: background 0.15s, color 0.15s;

  .feature-enabled & {
    background: var(--x-ink);
    color: var(--x-on-ink);
  }
}

.feature-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;

  .feature-disabled & {
    cursor: not-allowed;
  }
}

.feature-name {
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  color: var(--x-text);
  overflow-wrap: break-word;
}

.feature-description {
  font-size: 13px;
  line-height: 1.5;
  color: var(--x-text-muted);
}

.feature-switch {
  flex-shrink: 0;
}

.empty-state {
  background: var(--x-surface);
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-card);
}

.empty-tip {
  margin: 0;
  font-size: 13px;
  color: var(--x-text-muted);
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

  .feature-groups-container {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .group-head,
  .feature-row {
    padding-inline-start: 16px;
    padding-inline-end: 16px;
  }
}
</style>
