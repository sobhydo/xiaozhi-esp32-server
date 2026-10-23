<template>
  <div class="auth">
    <main class="auth-main">
      <header class="auth-top">
        <div class="auth-brand">
          <span class="auth-brand-tile"><XMark :size="20" /></span>
          <span class="auth-brand-name">{{ $t('console.productName') }}</span>
        </div>
      </header>

      <form class="auth-form" novalidate @submit.prevent @keyup.enter="register">
        <div class="auth-heading">
          <h1>{{ $t('register.title') }}</h1>
          <p>{{ $t('register.welcome') }}</p>
        </div>

        <!-- Username registration -->
        <div v-if="!enableMobileRegister" class="auth-field">
          <label class="auth-label" for="register-username">{{ $t('login.username') }}</label>
          <el-input id="register-username" v-model="form.username" autocomplete="username"
            :placeholder="$t('register.usernamePlaceholder')" />
        </div>

        <!-- Mobile registration: mobile, captcha, SMS code -->
        <template v-if="enableMobileRegister">
          <div class="auth-field">
            <label class="auth-label" for="register-mobile">{{ $t('retrievePassword.mobile') }}</label>
            <div class="auth-inline">
              <el-select v-model="form.areaCode" class="auth-area-code" filterable>
                <el-option v-for="item in mobileAreaList" :key="item.key" :label="`${item.name} (${item.key})`"
                  :value="item.key" />
              </el-select>
              <el-input id="register-mobile" v-model="form.mobile" autocomplete="tel-national" :placeholder="$t('register.mobilePlaceholder')" />
            </div>
          </div>
          <div class="auth-field">
            <label class="auth-label" for="register-captcha">{{ $t('retrievePassword.captcha') }}</label>
            <div class="auth-inline">
              <el-input id="register-captcha" v-model="form.captcha" autocomplete="off" :placeholder="$t('register.captchaPlaceholder')" />
              <button v-if="captchaUrl" type="button" class="auth-captcha" @click="fetchCaptcha">
                <img :src="captchaUrl" alt="Captcha" />
              </button>
            </div>
          </div>
          <div class="auth-field">
            <label class="auth-label" for="register-sms">{{ $t('retrievePassword.mobileCaptcha') }}</label>
            <div class="auth-inline auth-inline--wrap">
              <el-input id="register-sms" v-model="form.mobileCaptcha" autocomplete="one-time-code" maxlength="6"
                :placeholder="$t('register.mobileCaptchaPlaceholder')" />
              <el-button class="auth-sms-btn" :disabled="!canSendMobileCaptcha" @click="sendMobileCaptcha">
                {{ countdown > 0 ? `${countdown}${$t('register.secondsLater')}` : $t('register.sendCaptcha') }}
              </el-button>
            </div>
          </div>
        </template>

        <div class="auth-field">
          <label class="auth-label" for="register-password">{{ $t('login.password') }}</label>
          <el-input id="register-password" v-model="form.password" autocomplete="new-password"
            :placeholder="$t('register.passwordPlaceholder')" type="password" show-password />
          <label class="auth-sr-only" for="register-confirm">{{ $t('register.confirmPasswordPlaceholder') }}</label>
          <el-input id="register-confirm" v-model="form.confirmPassword" autocomplete="new-password"
            :placeholder="$t('register.confirmPasswordPlaceholder')" type="password" show-password />
        </div>

        <!-- Captcha (username registration) -->
        <template v-if="!enableMobileRegister">
          <div class="auth-field">
            <label class="auth-label" for="register-captcha">{{ $t('retrievePassword.captcha') }}</label>
            <div class="auth-inline">
              <el-input id="register-captcha" v-model="form.captcha" autocomplete="off" :placeholder="$t('register.captchaPlaceholder')" />
              <button v-if="captchaUrl" type="button" class="auth-captcha" @click="fetchCaptcha">
                <img :src="captchaUrl" alt="Captcha" />
              </button>
            </div>
          </div>
        </template>

        <el-button type="primary" class="auth-submit" @click="register">{{ $t('register.registerButton') }}</el-button>

        <p class="auth-alt">
          <button type="button" class="auth-link" @click="goToLogin">{{ $t('register.goToLogin') }}</button>
        </p>

        <p class="auth-legal">
          {{ $t('register.agreeTo') }}
          <button type="button" class="auth-link" @click="openPage('/user-agreement.html')">{{ $t('register.userAgreement') }}</button>
          {{ $t('login.and') }}
          <button type="button" class="auth-link" @click="openPage('/privacy-policy.html')">{{ $t('register.privacyPolicy') }}</button>
        </p>
      </form>

      <footer class="auth-footer">
        <version-footer />
      </footer>
    </main>

    <aside class="auth-night" aria-hidden="true">
      <div class="auth-night-hero">
        <XMark inverse :size="200" />
        <span class="auth-night-name">{{ $t('console.productName') }}</span>
        <p class="auth-night-headline">{{ $t('console.authHeadline') }}</p>
        <p class="auth-night-copy">{{ $t('console.authDescription') }}</p>
      </div>
      <div class="auth-night-chips">
        <span>{{ $t('modelConfig.asr') }}</span>
        <span>{{ $t('home.languageModel') }}</span>
        <span>{{ $t('modelConfig.tts') }}</span>
      </div>
    </aside>
  </div>
</template>

<script>
import Api from '@/apis/api';
import VersionFooter from '@/components/VersionFooter.vue';
import XMark from '@/components/XMark.vue';
import { getUUID, goToPage, showDanger, showSuccess, sm2Encrypt, validateMobile } from '@/utils';
import { mapState } from 'vuex';
import i18n from '@/i18n';

// Import language switching

export default {
  name: 'register',
  components: {
    VersionFooter,
    XMark
  },
  computed: {
    ...mapState({
      allowUserRegister: state => state.pubConfig.allowUserRegister,
      enableMobileRegister: state => state.pubConfig.enableMobileRegister,
      mobileAreaList: state => state.pubConfig.mobileAreaList,
      sm2PublicKey: state => state.pubConfig.sm2PublicKey,
    }),
    // Get the current language
    currentLanguage() {
      return i18n.locale || "zh_CN";
    },
    canSendMobileCaptcha() {
      return this.countdown === 0 && validateMobile(this.form.mobile, this.form.areaCode);
    }
  },
  data() {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        captcha: '',
        captchaId: '',
        areaCode: '+86',
        mobile: '',
        mobileCaptcha: ''
      },
      captchaUrl: '',
      countdown: 0,
      timer: null,
    }
  },
  mounted() {
    this.$store.dispatch('fetchPubConfig').then(() => {
      if (!this.allowUserRegister) {
        showDanger(this.$t('register.notAllowRegister'));
        setTimeout(() => {
          goToPage('/login');
        }, 1500);
      }
    });
    this.fetchCaptcha();
  },
  methods: {
    openPage(url) {
      const lang = this.$i18n ? this.$i18n.locale : 'zh_CN';
      if (!lang.startsWith('zh')) {
        url = url.replace('.html', '-en.html');
      }
      window.open(url, '_blank');
    },
    // Reuse captcha fetching method
    fetchCaptcha() {
      this.form.captchaId = getUUID();
      Api.user.getCaptcha(this.form.captchaId, (res) => {
        if (res.status === 200) {
          const blob = new Blob([res.data], { type: res.data.type });
          this.captchaUrl = URL.createObjectURL(blob);

        } else {
          console.error('Captcha loading error:', error);
          showDanger(this.$t('register.captchaLoadFailed'));
        }
      });
    },

    // Input validation logic
    validateInput(input, message) {
      if (!input.trim()) {
        showDanger(message);
        return false;
      }
      return true;
    },

    // Send SMS verification code
    sendMobileCaptcha() {
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger(this.$t('register.inputCorrectMobile'));
        return;
      }

      // Validate captcha
      if (!this.validateInput(this.form.captcha, this.$t('register.inputCaptcha'))) {
        this.fetchCaptcha();
        return;
      }

      // Clear any existing timer
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }

      // Start countdown
      this.countdown = 60;
      this.timer = setInterval(() => {
        if (this.countdown > 0) {
          this.countdown--;
        } else {
          clearInterval(this.timer);
          this.timer = null;
        }
      }, 1000);

      // Call the send-verification-code API
      Api.user.sendSmsVerification({
        phone: this.form.areaCode + this.form.mobile,
        captcha: this.form.captcha,
        captchaId: this.form.captchaId
      }, (res) => {
        showSuccess(this.$t('register.captchaSendSuccess'));
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.captchaSendFailed'));
        this.countdown = 0;
        this.fetchCaptcha();
      });
    },

    // Registration logic
    async register() {
      if (this.enableMobileRegister) {
        // Mobile registration validation
        if (!validateMobile(this.form.mobile, this.form.areaCode)) {
          showDanger(this.$t('register.inputCorrectMobile'));
          return;
        }
        if (!this.form.mobileCaptcha) {
          showDanger(this.$t('register.requiredMobileCaptcha'));
          return;
        }
      } else {
        // Username registration validation
        if (!this.validateInput(this.form.username, this.$t('register.requiredUsername'))) {
          return;
        }
      }

      // Validate password
      if (!this.validateInput(this.form.password, this.$t('register.requiredPassword'))) {
        return;
      }
      if (this.form.password !== this.form.confirmPassword) {
        showDanger(this.$t('register.passwordsNotMatch'))
        return
      }
      // Validate captcha
      if (!this.validateInput(this.form.captcha, this.$t('register.requiredCaptcha'))) {
        return;
      }
      // Encrypt
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

      let plainUsername;
      if (this.enableMobileRegister) {
        plainUsername = this.form.areaCode + this.form.mobile;
      } else {
        plainUsername = this.form.username;
      }

      // Prepare registration data
      const registerData = {
        username: plainUsername,
        password: encryptedPassword,
        captchaId: this.form.captchaId,
        mobileCaptcha: this.form.mobileCaptcha
      };

      Api.user.register(registerData, ({ data }) => {
        showSuccess(this.$t('register.registerSuccess'))
        goToPage('/login')
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.registerFailed'))
        if (err.data != null && err.data.msg != null && err.data.msg.indexOf('图形验证码') > -1) {
          this.fetchCaptcha()
        }
      })
    },

    goToLogin() {
      goToPage('/login')
    }
  },
  beforeDestroy() {
    if (this.timer) {
      clearInterval(this.timer);
    }
  }
}
</script>

<style lang="scss" scoped>
@import './auth.scss';
</style>
