<template>
  <div class="login">
    <div class="login-container">
      <!-- 登录卡片（背景图右侧留白区） -->
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
            >
              <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
            </el-input>
          </el-form-item>
          <el-form-item prop="tenantId">
            <el-input
              v-model="loginForm.tenantId"
              type="text"
              auto-complete="off"
              placeholder="租户ID（向平台管理员获取）"
              @keyup.enter.native="handleLogin"
            >
              <svg-icon slot="prefix" icon-class="component" class="el-input__icon input-icon" />
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
      </div>
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
        username: "",
        password: "",
        rememberMe: false,
        code: "",
        uuid: "",
        tenantId: ""
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
      const tenantId = Cookies.get('tenantId')
      this.loginForm = {
        ...this.loginForm,
        username: username === undefined ? this.loginForm.username : username,
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe),
        tenantId: tenantId === undefined ? '' : tenantId
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
            if (this.loginForm.tenantId) {
              Cookies.set('tenantId', this.loginForm.tenantId, { expires: 30 });
            } else {
              Cookies.remove('tenantId');
            }
          } else {
            Cookies.remove("username");
            Cookies.remove('rememberMe');
            Cookies.remove('tenantId');
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
  justify-content: flex-end;
  width: 100%;
  padding: 0 10%;
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

/* 响应式适配 */
@media (max-width: 1080px) {
  .login-container {
    justify-content: center;
    padding: 40px 24px;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
