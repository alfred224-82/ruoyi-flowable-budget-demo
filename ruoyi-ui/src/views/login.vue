<template>
  <div class="login">
    <!-- 左侧品牌文字叠加层 -->
    <div class="brand-overlay">
      <div class="brand-content">
        <h1 class="brand-title">阿杰小账本</h1>
        <p class="brand-subtitle">多行业门店，一套系统管好日常经营</p>
        <div class="brand-features">
          <span>📋 预约活动</span>
          <span>👥 人员管理</span>
          <span> 客户推广</span>
          <span>📊 报表统计</span>
        </div>
      </div>
    </div>
    <div class="login-container">
      <!-- 登录卡片（背景图右侧留白区） -->
      <div class="login-card">
        <!-- 品牌固定展示 + 租户号输入分层，与小程序/H5 三端一致 -->
        <div class="brand">
          <span class="brand-name">阿杰小账本</span>
          <span class="brand-sub">门店经营 · 活动预约 · 费用管控</span>
        </div>
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
              placeholder="租户ID（admin 可留空，其他用户必填，向平台管理员获取）"
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
    <!-- ★ 备案信息（ICP + 公安备案徽章，网站底部居中展示） -->
    <div class="icp-footer">
      <a v-if="icpNumber" :href="icpLink" target="_blank" rel="noopener noreferrer">{{ icpNumber }}</a>
      <template v-if="icpNumber && policeNumber"> | </template>
      <a v-if="policeNumber" class="police-link" :href="policeLink" target="_blank" rel="noopener noreferrer">
        <img v-if="policeIcon" class="police-badge" :src="policeIcon" alt="公安备案徽章">
        <span>{{ policeNumber }}</span>
      </a>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import { getBeianInfo } from "@/utils/beian";
import Cookies from "js-cookie";

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      // ★ 备案信息（ICP + 公安备案，统一读 .env 配置，见 @/utils/beian）
      ...getBeianInfo(),
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
  background-image: url("../assets/images/login-bg.png");
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  position: relative;
}

/* 左侧品牌文字叠加层 */
.brand-overlay {
  position: absolute;
  left: 0;
  top: 0;
  width: 50%;
  height: 100%;
  display: flex;
  align-items: flex-end;
  padding: 0 8% 12% 8%;
  z-index: 1;
  pointer-events: none;

  .brand-content {
    max-width: 480px;

    .brand-title {
      font-size: 42px;
      font-weight: 800;
      color: #5a3e2b;
      margin: 0 0 12px 0;
      letter-spacing: 4px;
      text-shadow: 0 2px 8px rgba(255, 255, 255, 0.6);
      line-height: 1.2;
    }

    .brand-subtitle {
      font-size: 18px;
      color: #6b5344;
      margin: 0 0 24px 0;
      letter-spacing: 1px;
      text-shadow: 0 1px 4px rgba(255, 255, 255, 0.5);
      line-height: 1.5;
    }

    .brand-features {
      display: flex;
      flex-wrap: wrap;
      gap: 12px 20px;

      span {
        font-size: 14px;
        color: #7a6355;
        background: rgba(255, 255, 255, 0.55);
        backdrop-filter: blur(6px);
        padding: 6px 16px;
        border-radius: 20px;
        letter-spacing: 0.5px;
        white-space: nowrap;
      }
    }
  }
}

.login-container {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  width: 100%;
  padding: 0 10%;
  position: relative;
  z-index: 2;
}

/* 右侧登录卡片 */
.login-card {
  width: 400px;
  flex-shrink: 0;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(30, 105, 242, 0.10), 0 2px 12px rgba(0, 0, 0, 0.04);
  padding: 48px 44px 36px 44px;

  .brand {
    text-align: center;
    margin: 0 0 10px 0;

    .brand-name {
      display: block;
      font-size: 26px;
      font-weight: 800;
      letter-spacing: 3px;
      color: #8B6914;
    }

    .brand-sub {
      display: block;
      margin-top: 6px;
      font-size: 12px;
      letter-spacing: 1px;
      color: #a0896e;
    }
  }

  .card-title {
    margin: 0 0 30px 0;
    text-align: center;
    font-size: 18px;
    font-weight: 600;
    color: #6b5344;
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
          border-bottom-color: #a0714f;
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
      background: linear-gradient(90deg, #c9956b 0%, #a0714f 100%);
      box-shadow: 0 4px 16px rgba(160, 113, 79, 0.35);
      transition: all 0.3s;

      &:hover, &:focus {
        background: linear-gradient(90deg, #d4a57b 0%, #b08160 100%);
        box-shadow: 0 6px 20px rgba(160, 113, 79, 0.45);
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
  .brand-overlay {
    display: none;
  }
  .login-container {
    justify-content: center;
    padding: 40px 24px;
  }
  .login-card {
    width: 100%;
    max-width: 400px;
  }
}

/* ★ 备案信息底部展示 */
.icp-footer {
  position: absolute;
  bottom: 16px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  z-index: 10;
  pointer-events: auto;
}
.icp-footer a {
  color: rgba(255, 255, 255, 0.75);
  text-decoration: none;
  transition: color 0.2s;
}
.icp-footer a:hover {
  color: #fff;
  text-decoration: underline;
}
/* 公安备案：官方徽章 + 备案号同行居中 */
.icp-footer .police-link {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}
.icp-footer .police-badge {
  width: 16px;
  height: 17px;
  margin-right: 4px;
}
</style>
