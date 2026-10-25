<template>
  <div class="x-shell">
    <!-- Top bar, small screens only -->
    <header class="x-topbar">
      <router-link to="/home" class="x-brand x-brand--compact">
        <span class="x-brand__tile"><XMark :size="18" /></span>
        <span class="x-brand__name">{{ $t("console.productName") }}</span>
      </router-link>
      <button
        type="button"
        class="x-icon-btn"
        :aria-label="mobileOpen ? $t('console.closeMenu') : $t('console.openMenu')"
        :aria-expanded="mobileOpen ? 'true' : 'false'"
        @click="mobileOpen = !mobileOpen"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
          <path v-if="mobileOpen" d="M6 6l12 12M18 6 6 18" />
          <path v-else d="M4 7h16M4 12h16M4 17h16" />
        </svg>
      </button>
    </header>

    <div v-if="mobileOpen" class="x-scrim" @click="mobileOpen = false"></div>

    <nav class="x-sidenav" :class="{ 'is-open': mobileOpen }" :aria-label="$t('console.mainNavigation')">
      <router-link to="/home" class="x-brand">
        <span class="x-brand__tile"><XMark :size="20" /></span>
        <span class="x-brand__name">{{ $t("console.productName") }}</span>
      </router-link>

      <div class="x-nav">
        <router-link
          v-for="item in primaryItems"
          :key="item.key"
          :to="item.path"
          class="x-nav__item"
          :class="{ 'is-active': isActive(item) }"
          :aria-current="isActive(item) ? 'page' : null"
        >
          <svg class="x-nav__icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
            stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" v-html="icons[item.icon]"></svg>
          <span>{{ $t(item.label) }}</span>
        </router-link>

        <template v-if="userInfo.superAdmin">
          <div class="x-nav__section">{{ $t("console.administration") }}</div>
          <router-link
            v-for="item in adminItems"
            :key="item.key"
            :to="item.path"
            class="x-nav__item"
            :class="{ 'is-active': isActive(item) }"
            :aria-current="isActive(item) ? 'page' : null"
          >
            <svg class="x-nav__icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" v-html="icons[item.icon]"></svg>
            <span>{{ $t(item.label) }}</span>
          </router-link>

          <button
            type="button"
            class="x-nav__item x-nav__group"
            :class="{ 'is-active': settingsActive && !settingsOpen }"
            :aria-expanded="settingsOpen ? 'true' : 'false'"
            @click="settingsOpen = !settingsOpen"
          >
            <svg class="x-nav__icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" v-html="icons.settings"></svg>
            <span>{{ $t("console.systemSettings") }}</span>
            <svg class="x-nav__chevron" :class="{ 'is-open': settingsOpen }" width="16" height="16" viewBox="0 0 24 24"
              fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <path d="m6 9 6 6 6-6" />
            </svg>
          </button>
          <div v-show="settingsOpen" class="x-nav__children">
            <router-link
              v-for="item in settingsItems"
              :key="item.key"
              :to="item.path"
              class="x-nav__item x-nav__child"
              :class="{ 'is-active': isActive(item) }"
              :aria-current="isActive(item) ? 'page' : null"
            >
              {{ $t(item.label) }}
            </router-link>
          </div>
        </template>
      </div>

      <div class="x-account">
        <span class="x-account__avatar" aria-hidden="true">{{ initials }}</span>
        <span class="x-account__who">
          <span class="x-account__name">{{ userInfo.username || "…" }}</span>
          <span class="x-account__role">{{ userInfo.superAdmin ? $t("console.superAdmin") : $t("console.member") }}</span>
        </span>
        <el-dropdown trigger="click" placement="top-end" @command="handleCommand">
          <button type="button" class="x-icon-btn" :aria-label="$t('console.accountMenu')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" aria-hidden="true">
              <circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" />
            </svg>
          </button>
          <el-dropdown-menu slot="dropdown" class="x-account-menu">
            <li class="x-account-menu__label">{{ $t("console.language") }}</li>
            <el-dropdown-item
              v-for="lang in languages"
              :key="lang.value"
              :command="'lang:' + lang.value"
              :class="{ 'is-current': currentLanguage === lang.value }"
            >
              {{ $t(lang.label) }}
            </el-dropdown-item>
            <el-dropdown-item command="password" divided>{{ $t("header.changePassword") }}</el-dropdown-item>
            <el-dropdown-item command="logout">{{ $t("header.logout") }}</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
    </nav>

    <ChangePasswordDialog v-model="isChangePasswordDialogVisible" />
  </div>
</template>

<script>
import i18n, { changeLanguage } from "@/i18n";
import { mapActions, mapState } from "vuex";
import ChangePasswordDialog from "./ChangePasswordDialog.vue";
import XMark from "./XMark.vue";

// Inner markup of 24x24 stroke icons used by the navigation
const ICONS = {
  agents: '<rect x="3" y="8" width="18" height="12" rx="2"/><path d="M12 8V4M9 14h.01M15 14h.01"/>',
  voice: '<rect x="9" y="3" width="6" height="11" rx="3"/><path d="M5 11a7 7 0 0 0 14 0M12 18v3"/>',
  waveform: '<path d="M4 10v4M8 7v10M12 4v16M16 7v10M20 10v4"/>',
  models: '<path d="M12 3 3 8l9 5 9-5-9-5z"/><path d="m3 13 9 5 9-5"/>',
  knowledge: '<path d="M4 5a2 2 0 0 1 2-2h14v16H6a2 2 0 0 0-2 2z"/><path d="M4 19V5"/>',
  contacts: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
  users: '<circle cx="9" cy="8" r="3.5"/><path d="M2.5 20a6.5 6.5 0 0 1 13 0M16 4.5a3.5 3.5 0 0 1 0 7M21.5 20a6.5 6.5 0 0 0-4-6"/>',
  firmware: '<path d="M12 3v12M7 10l5 5 5-5M5 21h14"/>',
  settings: '<path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12"/><circle cx="16" cy="6" r="2"/><circle cx="10" cy="12" r="2"/><circle cx="18" cy="18" r="2"/>',
};

// Mounted sidebars; the body class stays while at least one is mounted
let mountedCount = 0;

export default {
  name: "HeaderBar",
  components: { ChangePasswordDialog, XMark },
  props: ["devices"], // Kept for compatibility with pages that pass it
  data() {
    return {
      icons: ICONS,
      mobileOpen: false,
      settingsOpen: false,
      isChangePasswordDialogVisible: false,
      languages: [
        { value: "en", label: "language.en" },
        { value: "ar", label: "language.ar" },
        { value: "zh_CN", label: "language.zhCN" },
        { value: "zh_TW", label: "language.zhTW" },
        { value: "de", label: "language.de" },
        { value: "vi", label: "language.vi" },
        { value: "pt_BR", label: "language.ptBR" },
      ],
      settingsItems: [
        { key: "params", path: "/params-management", label: "header.paramManagement" },
        { key: "dict", path: "/dict-management", label: "header.dictManagement" },
        { key: "providers", path: "/provider-management", label: "header.providerManagement" },
        { key: "templates", path: "/agent-template-management", label: "header.agentTemplate", match: ["/template-quick-config"] },
        { key: "words", path: "/replacement-word-management", label: "header.replacementWordManagement" },
        { key: "server", path: "/server-side-management", label: "header.serverSideManagement" },
        { key: "features", path: "/feature-management", label: "header.featureManagement" },
      ],
      adminItems: [
        { key: "users", path: "/user-management", label: "header.userManagement", icon: "users" },
        { key: "ota", path: "/ota-management", label: "header.otaManagement", icon: "firmware" },
      ],
    };
  },
  computed: {
    ...mapState({
      featureStatus: (state) => ({
        voiceClone: state.pubConfig.systemWebMenu?.features?.voiceClone?.enabled,
        knowledgeBase: state.pubConfig.systemWebMenu?.features?.knowledgeBase?.enabled,
        addressBook: state.pubConfig.systemWebMenu?.features?.addressBook?.enabled,
      }),
      userInfo: (state) => state.userInfo,
    }),
    currentLanguage() {
      return i18n.locale || "en";
    },
    primaryItems() {
      const admin = !!this.userInfo.superAdmin;
      const f = this.featureStatus;
      return [
        { key: "agents", path: "/home", label: "header.smartManagement", icon: "agents",
          match: ["/role-config", "/device-management", "/voice-print"] },
        f.voiceClone && { key: "voiceClone", path: "/voice-clone-management", label: "header.voiceCloneManagement", icon: "voice" },
        f.voiceClone && admin && { key: "voiceResources", path: "/voice-resource-management", label: "header.voiceResourceManagement", icon: "waveform" },
        admin && { key: "models", path: "/model-config", label: "header.modelConfig", icon: "models" },
        f.knowledgeBase && { key: "knowledge", path: "/knowledge-base-management", label: "header.knowledgeBase", icon: "knowledge",
          match: ["/knowledge-base"] },
        f.addressBook && { key: "addressBook", path: "/address-book-management", label: "header.addressBook", icon: "contacts" },
      ].filter(Boolean);
    },
    settingsActive() {
      return this.settingsItems.some((item) => this.isActive(item));
    },
    initials() {
      const name = (this.userInfo.username || "").trim();
      return name ? name.slice(0, 2).toUpperCase() : "";
    },
  },
  watch: {
    $route() {
      this.mobileOpen = false;
    },
  },
  created() {
    this.settingsOpen = this.settingsActive;
  },
  mounted() {
    mountedCount += 1;
    document.body.classList.add("x-has-sidenav");
  },
  beforeDestroy() {
    mountedCount -= 1;
    if (mountedCount <= 0) {
      mountedCount = 0;
      document.body.classList.remove("x-has-sidenav");
    }
  },
  methods: {
    isActive(item) {
      const path = this.$route.path;
      if (path === item.path) return true;
      return (item.match || []).some((prefix) => path.startsWith(prefix));
    },
    handleCommand(command) {
      if (command.startsWith("lang:")) {
        this.switchLanguage(command.slice(5));
      } else if (command === "password") {
        this.isChangePasswordDialogVisible = true;
      } else if (command === "logout") {
        this.handleLogout();
      }
    },
    switchLanguage(lang) {
      changeLanguage(lang);
      this.$message.success({
        message: this.$t("message.success"),
        showClose: true,
      });
    },
    async handleLogout() {
      try {
        await this.logout();
        this.$message.success({
          message: this.$t("message.success"),
          showClose: true,
        });
      } catch (error) {
        console.error("Logout failed:", error);
        this.$message.error({
          message: this.$t("message.error"),
          showClose: true,
        });
      }
    },
    ...mapActions(["logout"]),
  },
};
</script>

<style lang="scss" scoped>
.x-sidenav {
  position: fixed;
  top: 0;
  bottom: 0;
  inset-inline-start: 0;
  z-index: 1000;
  width: var(--x-sidebar-width);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  padding: 16px 12px 12px;
  background: var(--x-surface);
  border-inline-end: 1px solid var(--x-border);
  text-align: start;
  font-size: 14px;
}

.x-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 10px 20px;
  color: var(--x-text);
  text-decoration: none;

  .x-mark {
    transition: transform 0.6s cubic-bezier(0.7, 0, 0.3, 1);
  }

  &:hover .x-mark {
    transform: rotate(90deg);
  }
}

.x-brand__tile {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--x-border);
  border-radius: 8px;
  background: var(--x-surface);
}

.x-brand__name {
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.x-nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.x-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 40px;
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 0 10px;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-secondary);
  font: inherit;
  font-weight: 500;
  text-align: start;
  text-decoration: none;
  cursor: pointer;

  &:hover {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &.is-active {
    background: var(--x-selected);
    color: var(--x-text);
    font-weight: 600;
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.x-nav__icon {
  flex-shrink: 0;
}

.x-nav__chevron {
  margin-inline-start: auto;
  transition: transform 0.2s ease;

  &.is-open {
    transform: rotate(180deg);
  }
}

.x-nav__section {
  padding: 20px 10px 6px;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--x-text-muted);
}

.x-nav__children {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-inline-start: 19px;
  padding-inline-start: 10px;
  border-inline-start: 1px solid var(--x-border);
}

.x-nav__child {
  height: 34px;
  font-size: 13px;
}

.x-account {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  padding: 12px 4px 0 10px;
  border-top: 1px solid var(--x-border);
}

.x-account__avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--x-border);
  color: var(--x-text-secondary);
  font-size: 12px;
  font-weight: 600;
}

.x-account__who {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.x-account__name {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.x-account__role {
  font-size: 12px;
  color: var(--x-text-muted);
}

.x-icon-btn {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--x-radius-control);
  background: transparent;
  color: var(--x-text-muted);
  cursor: pointer;

  &:hover {
    background: var(--x-canvas);
    color: var(--x-text);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}

.x-topbar,
.x-scrim {
  display: none;
}

@media (max-width: 900px) {
  .x-topbar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 1001;
    height: var(--x-topbar-height);
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 12px 0 6px;
    background: var(--x-surface);
    border-bottom: 1px solid var(--x-border);
  }

  .x-brand--compact {
    padding: 0 10px;
  }

  .x-sidenav {
    top: var(--x-topbar-height);
    transform: translateX(-100%);
    transition: transform 0.2s ease;
    box-shadow: var(--x-shadow-raised);

    [dir="rtl"] & {
      transform: translateX(100%);
    }

    &.is-open,
    [dir="rtl"] &.is-open {
      transform: none;
    }

    > .x-brand {
      display: none;
    }
  }

  .x-scrim {
    display: block;
    position: fixed;
    inset: var(--x-topbar-height) 0 0 0;
    z-index: 999;
    background: rgba(11, 11, 12, 0.4);
  }
}

@media (prefers-reduced-motion: reduce) {
  .x-brand .x-mark,
  .x-sidenav,
  .x-nav__chevron {
    transition: none;
  }
}
</style>

<style lang="scss">
.x-account-menu {
  min-width: 200px;

  .x-account-menu__label {
    padding: 6px 20px 4px;
    font-size: 12px;
    font-weight: 500;
    color: var(--x-text-muted);
    list-style: none;
  }

  .el-dropdown-menu__item.is-current {
    font-weight: 600;
    color: var(--x-ink);
  }
}
</style>
