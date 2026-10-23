<template>
  <div class="auth">
    <main class="auth-main">
      <header class="auth-top">
        <div class="auth-brand">
          <span class="auth-brand-tile"><XMark :size="20" /></span>
          <span class="auth-brand-name">{{ $t('console.productName') }}</span>
        </div>
      </header>

      <form class="auth-form" novalidate @submit.prevent @keyup.enter="retrievePassword">
        <div class="auth-heading">
          <h1>{{ $t('retrievePassword.title') }}</h1>
          <p>{{ $t('retrievePassword.subtitle') }}</p>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="retrieve-mobile">{{ $t('retrievePassword.mobile') }}</label>
          <div class="auth-inline">
            <el-select v-model="form.areaCode" class="auth-area-code" filterable>
              <el-option v-for="item in mobileAreaList" :key="item.key" :label="`${item.name} (${item.key})`"
                :value="item.key" />
            </el-select>
            <el-input id="retrieve-mobile" v-model="form.mobile" autocomplete="tel-national" :placeholder="$t('retrievePassword.mobilePlaceholder')" />
          </div>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="retrieve-captcha">{{ $t('retrievePassword.captcha') }}</label>
          <div class="auth-inline">
            <el-input id="retrieve-captcha" v-model="form.captcha" autocomplete="off" :placeholder="$t('retrievePassword.captchaPlaceholder')" />
            <button v-if="captchaUrl" type="button" class="auth-captcha" @click="fetchCaptcha">
              <img :src="captchaUrl" alt="Captcha" />
            </button>
          </div>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="retrieve-sms">{{ $t('retrievePassword.mobileCaptcha') }}</label>
          <div class="auth-inline auth-inline--wrap">
            <el-input id="retrieve-sms" v-model="form.mobileCaptcha" autocomplete="one-time-code" maxlength="6"
              :placeholder="$t('retrievePassword.mobileCaptchaPlaceholder')" />
            <el-button class="auth-sms-btn" :disabled="!canSendMobileCaptcha" @click="sendMobileCaptcha">
              {{ countdown > 0 ? `${countdown}${$t('register.secondsLater')}` : $t('retrievePassword.getMobileCaptcha') }}
            </el-button>
          </div>
        </div>

        <div class="auth-field">
          <label class="auth-label" for="retrieve-password">{{ $t('retrievePassword.newPassword') }}</label>
          <el-input id="retrieve-password" v-model="form.newPassword" autocomplete="new-password"
            :placeholder="$t('retrievePassword.newPasswordPlaceholder')" type="password" show-password />
        </div>

        <div class="auth-field">
          <label class="auth-label" for="retrieve-confirm">{{ $t('retrievePassword.confirmNewPassword') }}</label>
          <el-input id="retrieve-confirm" v-model="form.confirmPassword" autocomplete="new-password"
            :placeholder="$t('retrievePassword.confirmNewPasswordPlaceholder')" type="password" show-password />
        </div>

        <el-button type="primary" class="auth-submit" @click="retrievePassword">{{ $t('retrievePassword.resetButton') }}</el-button>

        <p class="auth-alt">
          <button type="button" class="auth-link" @click="goToLogin">{{ $t('retrievePassword.goToLogin') }}</button>
        </p>

        <p class="auth-legal">
          {{ $t('retrievePassword.agreeTo') }}
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
import { getUUID, goToPage, showDanger, showSuccess, validateMobile, sm2Encrypt } from '@/utils';
import { mapState } from 'vuex';
import i18n from '@/i18n';

// Import language switching
import { changeLanguage } from '@/i18n';

export default {
  name: 'retrieve',
  components: {
    VersionFooter,
    XMark
  },
  computed: {
    ...mapState({
      allowUserRegister: state => state.pubConfig.allowUserRegister,
      mobileAreaList: state => state.pubConfig.mobileAreaList,
      sm2PublicKey: state => state.pubConfig.sm2PublicKey
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
        areaCode: '+86',
        mobile: '',
        captcha: '',
        captchaId: '',
        mobileCaptcha: '',
        newPassword: '',
        confirmPassword: ''
      },
      captchaUrl: '',
      countdown: 0,
      timer: null
    }
  },
  mounted() {
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
        showDanger(this.$t('retrievePassword.inputCorrectMobile'));
        return;
      }

      // Validate captcha
      if (!this.validateInput(this.form.captcha, this.$t('retrievePassword.captchaRequired'))) {
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
        showSuccess(this.$t('retrievePassword.captchaSendSuccess'));
      }, (err) => {
        showDanger(err.data.msg || this.$t('register.captchaSendFailed'));
        this.countdown = 0;
        this.fetchCaptcha();
      });
    },

    // Reset logic
    retrievePassword() {
      // Validation logic
      if (!validateMobile(this.form.mobile, this.form.areaCode)) {
        showDanger(this.$t('retrievePassword.inputCorrectMobile'));
        return;
      }
      if (!this.form.captcha) {
        showDanger(this.$t('retrievePassword.captchaRequired'));
        return;
      }
      if (!this.form.mobileCaptcha) {
        showDanger(this.$t('retrievePassword.mobileCaptchaRequired'));
        return;
      }
      if (this.form.newPassword !== this.form.confirmPassword) {
        showDanger(this.$t('retrievePassword.passwordsNotMatch'));
        return;
      }

      // Encrypt password
      let encryptedPassword;
      try {
        // Concatenate captcha and new password for encryption
        const captchaAndPassword = this.form.captcha + this.form.newPassword;
        encryptedPassword = sm2Encrypt(this.sm2PublicKey, captchaAndPassword);
      } catch (error) {
        console.error("Password encryption failed:", error);
        showDanger(this.$t('sm2.encryptionFailed'));
        return;
      }

      Api.user.retrievePassword({
        phone: this.form.areaCode + this.form.mobile,
        password: encryptedPassword,
        code: this.form.mobileCaptcha,
        captchaId: this.form.captchaId
      }, (res) => {
        showSuccess(this.$t('retrievePassword.passwordUpdateSuccess'));
        goToPage('/login');
      }, (err) => {
        showDanger(err.data.msg || this.$t('message.error'));
        if (err.data != null && err.data.msg != null && (err.data.msg.indexOf('图形验证码') > -1 || err.data.msg.indexOf('Captcha') > -1)) {
          this.fetchCaptcha()
        }
      });
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
