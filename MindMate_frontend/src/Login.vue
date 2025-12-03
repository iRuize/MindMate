<script setup>
import {ref} from 'vue'
import axios from 'axios'

const phoneNumber = ref('')

const sendSmsCode = async () => {
  const phone = phoneNumber.value.trim();
  if (!phone) {
    alert('请输入手机号')
    return
  }

  try {
    // 使用 axios 发 GET 请求
    const response = await axios.get('http://localhost:8080/auth/sendSmsCode', {
      params: {phoneNumber: phone}
    })

    if (response.data.code === 200) {
      alert('验证码发送成功')
    } else {
      alert(response.data.message) // 弹出后端返回的提示
    }

  } catch (error) {
    console.error(error)
    alert('网络错误')
  }
}
</script>

<template>
  <div class="center-container">
    <span>MindMate</span>
    <div class="login-box">
      <!-- 左侧扫码登录 -->
      <div class="login-left">
        <h3>扫码登录</h3>
        <div class="qrcode">
          <img alt="二维码登录" src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=MindMateLogin">
        </div>
      </div>

      <!-- 右侧短信验证码登录 -->
      <div class="login-right">
        <h3>短信验证码登录</h3>
        <input v-model="phoneNumber" placeholder="请输入手机号" type="text">
        <div class="code-input">
          <input placeholder="请输入验证码" type="text">
          <button @click="sendSmsCode">发送验证码</button>
        </div>
        <button class="login-btn">登录</button>
      </div>
    </div>
  </div>
</template>

<style>
@import url('https://fonts.googleapis.com/css2?family=Quicksand&display=swap');

/* 背景部分 */
:root {
  font-size: 15px;
}

body {
  font-family: 'Quicksand', sans-serif;
  margin: 0;
  min-height: 100vh;
  background-color: #e493d0;
  background-image: radial-gradient(closest-side, rgba(235, 105, 78, 1), rgba(235, 105, 78, 0)),
  radial-gradient(closest-side, rgba(243, 11, 164, 1), rgba(243, 11, 164, 0)),
  radial-gradient(closest-side, rgba(254, 234, 131, 1), rgba(254, 234, 131, 0)),
  radial-gradient(closest-side, rgba(170, 142, 245, 1), rgba(170, 142, 245, 0)),
  radial-gradient(closest-side, rgba(248, 192, 147, 1), rgba(248, 192, 147, 0));
  background-size: 130vmax 130vmax,
  80vmax 80vmax,
  90vmax 90vmax,
  110vmax 110vmax,
  90vmax 90vmax;
  background-position: -80vmax -80vmax,
  60vmax -30vmax,
  10vmax 10vmax,
  -30vmax -10vmax,
  50vmax 50vmax;
  background-repeat: no-repeat;
  animation: 8s movement linear infinite;
  display: flex;
  justify-content: center;
  align-items: center;
}

body::after {
  content: '';
  display: block;
  position: fixed;
  z-index: -1;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

span {
  font-size: 5rem;
  color: rgba(255, 255, 255, 0.9);
  text-shadow: 0px 0px 1px rgba(255, 255, 255, .6),
  0px 4px 4px rgba(0, 0, 0, .05);
  letter-spacing: .2rem;
}

@keyframes movement {

  0%,
  100% {
    background-size: 130vmax 130vmax,
    80vmax 80vmax,
    90vmax 90vmax,
    110vmax 110vmax,
    90vmax 90vmax;
    background-position: -80vmax -80vmax,
    60vmax -30vmax,
    10vmax 10vmax,
    -30vmax -10vmax,
    50vmax 50vmax;
  }

  25% {
    background-size: 100vmax 100vmax,
    90vmax 90vmax,
    100vmax 100vmax,
    90vmax 90vmax,
    60vmax 60vmax;
    background-position: -60vmax -90vmax,
    50vmax -40vmax,
    0vmax -20vmax,
    -40vmax -20vmax,
    40vmax 60vmax;
  }

  50% {
    background-size: 80vmax 80vmax,
    110vmax 110vmax,
    80vmax 80vmax,
    60vmax 60vmax,
    80vmax 80vmax;
    background-position: -50vmax -70vmax,
    40vmax -30vmax,
    10vmax 0vmax,
    20vmax 10vmax,
    30vmax 70vmax;
  }

  75% {
    background-size: 90vmax 90vmax,
    90vmax 90vmax,
    100vmax 100vmax,
    90vmax 90vmax,
    70vmax 70vmax;
    background-position: -50vmax -40vmax,
    50vmax -30vmax,
    20vmax 0vmax,
    -10vmax 10vmax,
    40vmax 60vmax;
  }
}

.center-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  text-align: center;
  gap: 50px;
}

/* 登录窗口部分 */
.login-form {
  width: 600px;
  max-width: 90vw;
  margin-top: 30px;
  padding: 30px;
  display: flex;
  gap: 20px;
  justify-content: space-between;
  border-radius: 20px;
  background-color: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.title {
  font-size: 32px;
  color: white;
  margin-bottom: 40px;
  font-weight: bold;
}

.login-box {
  display: flex;
  background-color: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  overflow: hidden;
  width: 900px;
  max-width: 95%;
  box-shadow: 0 0 20px rgba(0, 0, 0, 0.3);
  height: 400px;
}

.login-left,
.login-right {
  flex: 1;
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.login-left {
  border-right: 1px solid rgba(255, 255, 255, 0.2);
}

.login-left h3,
.login-right h3 {
  margin-bottom: 20px;
  color: white;
  font-weight: 500;
}

.qrcode img {
  width: 150px;
  height: 150px;
  border-radius: 15px;
  border: 2px solid rgba(255, 255, 255, 0.5);
}

.login-right input {
  width: 80%;
  padding: 10px;
  margin-bottom: 10px;
  border-radius: 8px;
  border: none;
  outline: none;
}

.code-input {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.code-input input {
  flex: 1;
}

.code-input button,
.login-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  background-color: rgba(255, 255, 255, 0.3);
  color: white;
  cursor: pointer;
}

.code-input button:hover,
.login-btn:hover {
  background-color: rgba(255, 255, 255, 0.5);
}

.login-btn {
  padding: 15px 20px;
  font-size: 1.1rem;
  border-radius: 10px;
  width: 160px;
}
</style>