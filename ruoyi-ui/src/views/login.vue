<template>
  <div class="login">
    <div class="login-container">
      <!-- 左侧品牌区 -->
      <div class="login-brand">
        <div class="brand-header">
          <div class="brand-logo">
            <svg viewBox="0 0 40 40" width="34" height="34">
              <rect width="40" height="40" rx="10" fill="#2A7BFF"/>
              <rect x="8" y="20" width="5" height="12" rx="1.5" fill="#fff"/>
              <rect x="17" y="14" width="5" height="18" rx="1.5" fill="#fff"/>
              <rect x="26" y="8" width="5" height="24" rx="1.5" fill="#fff"/>
            </svg>
          </div>
          <span class="brand-title">预算智控</span>
        </div>
        <h1 class="brand-slogan">预算精细化管控 · 全流程智能协同</h1>
        <div class="version-buttons">
          <div class="version-btn version-primary">免费使用基础版</div>
          <div class="version-btn-row">
            <div class="version-btn version-secondary">高级版 <em>付费</em></div>
            <div class="version-btn version-secondary">企业专业版 <em>付费</em></div>
          </div>
        </div>
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#2A7BFF" stroke-width="1.8">
                <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"/>
                <rect x="9" y="3" width="6" height="4" rx="1"/>
                <path d="M9 12h6M9 16h4"/>
              </svg>
            </div>
            <div class="feature-text">
              <h4>预算编制</h4>
              <p>月度预算编制与审批，自动汇总预算数据</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#2A7BFF" stroke-width="1.8">
                <rect x="3" y="5" width="18" height="14" rx="2"/>
                <path d="M3 10h18"/>
                <path d="M7 15h4"/>
              </svg>
            </div>
            <div class="feature-text">
              <h4>报销管理</h4>
              <p>智能报销申请与审批，实时预算余额校验</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#2A7BFF" stroke-width="1.8">
                <path d="M4 8h10M4 8l3-3M4 8l3 3"/>
                <path d="M20 16H10M20 16l-3-3M20 16l-3 3"/>
              </svg>
            </div>
            <div class="feature-text">
              <h4>预算调整</h4>
              <p>灵活调整预算额度，全流程审批留痕</p>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="#2A7BFF" stroke-width="1.8">
                <rect x="2" y="6" width="20" height="12" rx="2"/>
                <circle cx="12" cy="12" r="3"/>
                <path d="M6 12h.01M18 12h.01"/>
              </svg>
            </div>
            <div class="feature-text">
              <h4>资金管控</h4>
              <p>三级资金池分摊，预算余额实时校验</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录卡片 -->
      <div class="login-card">
        <h3 class="card-title">账号登录</h3>
        <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              type="text"
              auto-complete="off"
              placeholder="请输入账号"
            >
              <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              auto-complete="off"
              placeholder="请输入密码"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="请输入验证码"
              style="width: 63%"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
            </el-input>
            <div class="login-code">
              <img :src="codeUrl" @click="getCode" class="login-code-img"/>
            </div>
          </el-form-item>
          <el-checkbox v-model="loginForm.rememberMe" class="remember-me">记住账号</el-checkbox>
          <el-form-item style="width:100%;">
            <el-button
              :loading="loading"
              size="medium"
              type="primary"
              class="login-btn"
              @click.native.prevent="handleLogin"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登 录 中...</span>
            </el-button>
          </el-form-item>
        </el-form>
        <div class="card-links">
          <span class="link-forgot">忘记密码</span>
          <span class="link-admin" @click="handleLogin">管理员入口</span>
        </div>
      </div>
    </div>

    <!--  底部  -->
    <div class="el-login-footer">
      <span>基础版免费使用，高级版与企业专业版为付费增值版本</span>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "123456",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关（默认关闭，由后端配置控制）
      captchaEnabled: false,
      // 注册开关
      register: false,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.data.captchaEnabled === undefined ? true : res.data.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.data.img;
          this.loginForm.uuid = res.data.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        ...this.loginForm,
        username: username === undefined ? this.loginForm.username : username,
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{});
          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background: linear-gradient(135deg, #f0f7ff 0%, #e6f0ff 40%, #ffffff 100%);
  background-image: url("../assets/images/login-bg.png");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  width: 100%;
  max-width: 1200px;
  padding: 0 40px;
}

/* 左侧品牌区 */
.login-brand {
  flex: 1;
  min-width: 0;

  .brand-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 28px;

    .brand-logo svg {
      display: block;
      filter: drop-shadow(0 3px 6px rgba(42, 123, 255, 0.3));
    }

    .brand-title {
      font-size: 20px;
      font-weight: 700;
      color: #1E69F2;
      letter-spacing: 1px;
    }
  }

  .brand-slogan {
    margin: 0 0 32px 0;
    font-size: 30px;
    font-weight: 800;
    color: #1a1a1a;
    letter-spacing: 1px;
    line-height: 1.4;
  }

  .version-buttons {
    margin-bottom: 36px;

    .version-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.25s;
    }

    .version-primary {
      width: 220px;
      height: 44px;
      background: #ffffff;
      border: 2px solid #1E69F2;
      color: #1a1a1a;
      font-size: 15px;
      margin-bottom: 12px;
      box-shadow: 0 2px 10px rgba(30, 105, 242, 0.10);

      &:hover {
        background: #f0f6ff;
        box-shadow: 0 4px 14px rgba(30, 105, 242, 0.18);
      }
    }

    .version-btn-row {
      display: flex;
      gap: 12px;
    }

    .version-secondary {
      width: 130px;
      height: 36px;
      background: #ffffff;
      border: 1px solid rgba(30, 105, 242, 0.35);
      color: #1E69F2;
      font-size: 13px;
      box-shadow: 0 2px 8px rgba(30, 105, 242, 0.08);

      em {
        font-style: normal;
        font-size: 11px;
        margin-left: 4px;
        opacity: 0.7;
      }

      &:hover {
        background: #f0f6ff;
        border-color: #1E69F2;
        box-shadow: 0 3px 12px rgba(30, 105, 242, 0.15);
      }
    }
  }

  .brand-features {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 14px;

    .feature-item {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 10px;
      background: rgba(255, 255, 255, 0.85);
      border-radius: 12px;
      padding: 16px 14px;
      box-shadow: 0 2px 10px rgba(30, 105, 242, 0.06);
      transition: all 0.25s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(30, 105, 242, 0.12);
      }

      .feature-icon {
        width: 44px;
        height: 44px;
        border-radius: 10px;
        background: rgba(42, 123, 255, 0.08);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      .feature-text {
        h4 {
          margin: 0 0 4px 0;
          font-size: 14px;
          font-weight: 600;
          color: #1a1a1a;
        }
        p {
          margin: 0;
          font-size: 11px;
          color: #888888;
          line-height: 1.5;
        }
      }
    }
  }
}

/* 右侧登录卡片 */
.login-card {
  width: 400px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(30, 105, 242, 0.10), 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 48px 44px 36px 44px;

  .card-title {
    margin: 0 0 36px 0;
    text-align: center;
    font-size: 22px;
    font-weight: 700;
    color: #1a1a1a;
  }

  .login-form {
    .el-form-item {
      margin-bottom: 24px;
    }

    .el-input {
      height: 44px;
      ::v-deep input {
        height: 44px;
        line-height: 44px;
        border: none;
        border-bottom: 1px solid #e4e7ed;
        border-radius: 0;
        background: transparent;
        font-size: 14px;
        padding-left: 34px;

        &::placeholder {
          color: #999999;
        }

        &:focus {
          border-bottom-color: #2A7BFF;
        }
      }
      ::v-deep .el-input__prefix {
        left: 4px;
        display: flex;
        align-items: center;
      }
    }

    .input-icon {
      height: 44px;
      width: 16px;
      color: #999999;
    }

    .remember-me {
      margin: 0 0 28px 0;
      ::v-deep .el-checkbox__label {
        color: #333333;
        font-size: 14px;
      }
    }

    .login-btn {
      width: 80%;
      display: block;
      margin: 0 auto;
      height: 44px;
      font-size: 16px;
      font-weight: 500;
      letter-spacing: 4px;
      border: none;
      border-radius: 22px;
      background: linear-gradient(90deg, #3d8bfd 0%, #1E69F2 100%);
      box-shadow: 0 4px 16px rgba(30, 105, 242, 0.35);
      transition: all 0.3s;

      &:hover, &:focus {
        background: linear-gradient(90deg, #5a9cfe 0%, #3d7bf5 100%);
        box-shadow: 0 6px 20px rgba(30, 105, 242, 0.45);
      }
    }
  }

  .card-links {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 4px;

    .link-forgot {
      font-size: 13px;
      color: #666666;
      cursor: pointer;
      &:hover {
        color: #2A7BFF;
      }
    }

    .link-admin {
      font-size: 13px;
      color: #2A7BFF;
      font-weight: 600;
      cursor: pointer;
      &:hover {
        opacity: 0.8;
      }
    }
  }
}

.login-code {
  width: 33%;
  height: 44px;
  float: right;
  display: flex;
  align-items: center;
  justify-content: flex-end;

  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 4px;
  }
}

.login-code-img {
  height: 38px;
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #8fa8cc;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}

/* 响应式适配 */
@media (max-width: 1080px) {
  .login-container {
    flex-direction: column;
    gap: 36px;
    padding: 40px 24px;
  }
  .login-brand {
    text-align: center;
    .brand-header {
      justify-content: center;
    }
    .version-buttons {
      display: flex;
      flex-direction: column;
      align-items: center;
      .version-btn-row {
        justify-content: center;
      }
    }
    .brand-features {
      grid-template-columns: repeat(2, 1fr);
    }
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
