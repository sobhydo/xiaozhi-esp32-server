<template>
  <div class="auth">
    <main class="auth-main">
      <header class="auth-top">
        <div class="auth-brand">
          <span class="auth-brand-tile"><XMark :size="20" /></span>
          <span class="auth-brand-name">{{ $t("console.productName") }}</span>
        </div>

        <!-- Language switch dropdown -->
        <el-dropdown trigger="click" class="auth-lang" @visible-change="handleLanguageDropdownVisibleChange">
          <button type="button" class="auth-lang-btn" :aria-label="$t('console.language')">
            <span>{{ currentLanguageText }}</span>
            <i class="el-icon-arrow-down" :class="{ 'rotate-down': languageDropdownVisible }" aria-hidden="true"></i>
          </button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item @click.native="changeLanguage('en')">
              {{ $t("language.en") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('ar')">
              {{ $t("language.ar") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('zh_CN')">
              {{ $t("language.zhCN") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('zh_TW')">
              {{ $t("language.zhTW") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('de')">
              {{ $t("language.de") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('vi')">
              {{ $t("language.vi") }}
            </el-dropdown-item>
            <el-dropdown-item @click.native="changeLanguage('pt_BR')">
              {{ $t("language.ptBR") }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </header>

      <form class="auth-form" novalidate @submit.prevent @keyup.enter="login">
        <div class="auth-heading">
          <h1>{{ $t("login.title") }}</h1>
          <p>{{ $t("login.subtitle") }}</p>
        </div>

        <!-- Login method switch -->
        <div v-if="enableMobileRegister" class="auth-switch" role="group">
          <button type="button" class="auth-switch-btn" :class="{ 'is-active': !isMobileLogin }"
            :aria-pressed="String(!isMobileLogin)" @click="switchLoginType('username')">
            <i class="el-icon-user" aria-hidden="true"></i>
            {{ $t("login.usernameLogin") }}
          </button>
          <button type="button" class="auth-switch-btn" :class="{ 'is-active': isMobileLogin }"
            :aria-pressed="String(isMobileLogin)" @click="switchLoginType('mobile')">
            <i class="el-icon-mobile" aria-hidden="true"></i>
            {{ $t("login.mobileLogin") }}
          </button>
        </div>

        <!-- Username login -->
        <div v-if="!isMobileLogin" class="auth-field">
          <label class="auth-label" for="login-username">{{ $t("login.username") }}</label>
          <el-input id="login-username" v-model="form.username" autocomplete="username"
            :placeholder="$t('login.usernamePlaceholder')" />
        </div>

        <!-- Mobile number login -->
        <div v-else class="auth-field">
          <label class="auth-label" for="login-mobile">{{ $t("retrievePassword.mobile") }}</label>
          <div class="auth-inline">
            <el-select v-model="form.areaCode" class="auth-area-code" filterable>
              <el-option v-for="item in mobileAreaList" :key="item.key" :label="`${item.name} (${item.key})`"
                :value="item.key" />
            </el-select>
            <el-input id="login-mobile" v-model="form.mobile" autocomplete="tel-national"
              :placeholder="$t('login.mobilePlaceholder')" />
          </div>
        </div>

        <div class="auth-field">
          <div class="auth-label-row">
            <label class="auth-label" for="login-password">{{ $t("login.password") }}</label>
            <button v-if="enableMobileRegister" type="button" class="auth-link" @click="goToForgetPassword">
              {{ $t("login.forgetPassword") }}
            </button>
          </div>
          <el-input id="login-password" v-model="form.password" autocomplete="current-password"
            :placeholder="$t('login.passwordPlaceholder')" type="password" show-password />
        </div>

        <div class="auth-field">
          <label class="auth-label" for="login-captcha">{{ $t("retrievePassword.captcha") }}</label>
          <div class="auth-inline">
            <el-input id="login-captcha" v-model="form.captcha" autocomplete="off"
              :placeholder="$t('login.captchaPlaceholder')" />
            <button v-if="captchaUrl" type="button" class="auth-captcha" @click="fetchCaptcha">
              <img :src="captchaUrl" alt="Captcha" />
            </button>
          </div>
        </div>

        <el-button type="primary" class="auth-submit" @click="login">{{ $t("login.login") }}</el-button>

        <p v-if="allowUserRegister" class="auth-alt">
          <button type="button" class="auth-link" @click="goToRegister">{{ $t("login.register") }}</button>
        </p>

        <p class="auth-legal">
          {{ $t("login.agreeTo") }}
          <button type="button" class="auth-link" @click="openPage('/user-agreement.html')">
            {{ $t("login.userAgreement") }}
          </button>
          {{ $t("login.and") }}
          <button type="button" class="auth-link" @click="openPage('/privacy-policy.html')">
            {{ $t("login.privacyPolicy") }}
          </button>
        </p>
      </form>

      <footer class="auth-footer">
        <version-footer />
      </footer>
    </main>

    <aside class="auth-night" aria-hidden="true">
      <div class="auth-night-hero">
        <XMark inverse :size="200" />
        <span class="auth-night-name">{{ $t("console.productName") }}</span>
        <p class="auth-night-headline">{{ $t("console.authHeadline") }}</p>
        <p class="auth-night-copy">{{ $t("console.authDescription") }}</p>
      </div>
      <div class="auth-night-chips">
        <span>{{ $t("modelConfig.asr") }}</span>
        <span>{{ $t("home.languageModel") }}</span>
        <span>{{ $t("modelConfig.tts") }}</span>
      </div>
    </aside>
  </div>
</template>

<script>
import Api from "@/apis/api";
import VersionFooter from "@/components/VersionFooter.vue";
import XMark from "@/components/XMark.vue";
import i18n, { changeLanguage } from "@/i18n";
import { getUUID, goToPage, showDanger, showSuccess, sm2Encrypt, validateMobile } from "@/utils";
import { mapState } from "vuex";
import featureManager from "@/utils/featureManager";

export default {
  name: "login",
  components: {
    VersionFooter,
    XMark,
  },
  computed: {
    ...mapState({
      allowUserRegister: (state) => state.pubConfig.allowUserRegister,
      enableMobileRegister: (state) => state.pubConfig.enableMobileRegister,
      mobileAreaList: (state) => state.pubConfig.mobileAreaList,
      sm2PublicKey: (state) => state.pubConfig.sm2PublicKey,
    }),
    // Get the current language
    currentLanguage() {
      return i18n.locale || "en";
    },
    // Get display text for the current language
    currentLanguageText() {
      const currentLang = this.currentLanguage;
      switch (currentLang) {
        case "zh_CN":
          return this.$t("language.zhCN");
        case "zh_TW":
          return this.$t("language.zhTW");
        case "en":
          return this.$t("language.en");
        case "de":
          return this.$t("language.de");
        case "vi":
          return this.$t("language.vi");
        case "pt_BR":
          return this.$t("language.ptBR");
        case "ar":
          return this.$t("language.ar");
        default:
          return this.$t("language.en");
      }
    },
  },
  data() {
    return {
      activeName: "username",
      form: {
        username: "",
        password: "",
        captcha: "",
        captchaId: "",
        areaCode: "+86",
        mobile: "",
      },
      captchaUuid: "",
      captchaUrl: "",
      isMobileLogin: false,
      languageDropdownVisible: false,
    };
  },
  mounted() {
    this.fetchCaptcha();
    this.$store.dispatch("fetchPubConfig").then(() => {
      // Determine default login method from config
      this.isMobileLogin = this.enableMobileRegister;
    });
  },
  methods: {
    openPage(url) {
      const lang = this.$i18n ? this.$i18n.locale : 'zh_CN';
      if (!lang.startsWith('zh')) {
        url = url.replace('.html', '-en.html');
      }
      window.open(url, '_blank');
    },
    fetchCaptcha() {
      // Handle captcha fetch failure caused by manually cleared localStorage
      const token = localStorage.getItem('token')
      if (token) {
        if (this.$route.path !== "/home") {
          this.$router.push("/home");
        }
      } else {
        this.captchaUuid = getUUID();

        Api.user.getCaptcha(this.captchaUuid, (res) => {
          if (res.status === 200) {
            const blob = new Blob([res.data], { type: res.data.type });
            this.captchaUrl = URL.createObjectURL(blob);
          } else {
            showDanger("Failed to load captcha, click to refresh");
          }
        });
      }
    },

    // Language dropdown visibility change
    handleLanguageDropdownVisibleChange(visible) {
      this.languageDropdownVisible = visible;
    },

    // Switch language
    changeLanguage(lang) {
      changeLanguage(lang);
      this.languageDropdownVisible = false;
      this.$message.success({
        message: this.$t("message.success"),
        showClose: true,
      });
    },

    // Switch login method
    switchLoginType(type) {
      this.isMobileLogin = type === "mobile";
      // Clear form
      this.form.username = "";
      this.form.mobile = "";
      this.form.password = "";
      this.form.captcha = "";
      this.fetchCaptcha();
    },

    // Input validation logic
    validateInput(input, messageKey) {
      if (!input.trim()) {
        showDanger(this.$t(messageKey));
        return false;
      }
      return true;
    },
    
    getUserInfo() {
      Api.user.getUserInfo(({ data }) => {
        if (data.code === 0) {
          this.$store.commit("setUserInfo", data.data);
          goToPage("/home");
        } else {
          showDanger("Failed to get user info");
        }
      });
    },

    async login() {
      if (this.isMobileLogin) {
        // Mobile login validation
        if (!validateMobile(this.form.mobile, this.form.areaCode)) {
          showDanger(this.$t('login.requiredMobile'));
          return;
        }
        // Concatenate mobile number as username
        this.form.username = this.form.areaCode + this.form.mobile;
      } else {
        // Username login validation
        if (!this.validateInput(this.form.username, 'login.requiredUsername')) {
          return;
        }
      }

      // Validate password
      if (!this.validateInput(this.form.password, 'login.requiredPassword')) {
        return;
      }
      // Validate captcha
      if (!this.validateInput(this.form.captcha, 'login.requiredCaptcha')) {
        return;
      }
      // Encrypt password
      let encryptedPassword;
      try {
        // Concatenate captcha and password
        const captchaAndPassword = this.form.captcha + this.form.password;
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, captchaAndPassword);
      } catch (error) {
        console.error("Password encryption failed:", error);
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      const plainUsername = this.form.username;

      this.form.captchaId = this.captchaUuid;

      // Encrypt
      const loginData = {
        username: plainUsername,
        password: encryptedPassword,
        captchaId: this.form.captchaId
      };

      Api.user.login(
        loginData,
        ({ data }) => {
          showSuccess(this.$t('login.loginSuccess'));
          this.$store.commit("setToken", JSON.stringify(data.data));
          this.getUserInfo();
        },
        (err) => {
          // Use the localized message returned by the backend
          let errorMessage = err.data.msg || "Login failed";

          showDanger(errorMessage);
        }
      );

      // Refresh captcha
      setTimeout(() => {
        this.fetchCaptcha();
      }, 1000);
    },

    goToRegister() {
      goToPage("/register");
    },
    goToForgetPassword() {
      goToPage("/retrieve-password");
    }
  },
};
</script>
<style lang="scss" scoped>
@import "./auth.scss";

.auth-lang-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 10px 0 12px;
  border: 1px solid var(--x-border);
  border-radius: var(--x-radius-control);
  background: var(--x-surface);
  font: inherit;
  font-size: 13px;
  color: var(--x-text-secondary);
  cursor: pointer;

  &:hover {
    border-color: var(--x-border-control);
    color: var(--x-text);
  }

  &:focus-visible {
    outline: none;
    border-color: var(--x-ink);
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }

  i {
    font-size: 12px;
    color: var(--x-text-muted);
    transition: transform 0.3s ease;
  }
}

.rotate-down {
  transform: rotate(180deg);
}

// Username / mobile segmented switch
.auth-switch {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: var(--x-radius-pill);
  background: var(--x-canvas);
}

.auth-switch-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  height: 32px;
  padding: 0 12px;
  border: 0;
  border-radius: var(--x-radius-pill);
  background: transparent;
  font: inherit;
  font-size: 13px;
  font-weight: 500;
  color: var(--x-text-muted);
  cursor: pointer;
  white-space: nowrap;

  &:hover {
    color: var(--x-text);
  }

  &.is-active {
    background: var(--x-surface);
    color: var(--x-text);
    box-shadow: 0 0 0 1px var(--x-border);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px var(--x-focus-ring);
  }
}
</style>
