<script setup>
import '@fortawesome/fontawesome-free/css/all.css'
document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm')
    const usernameInput = document.getElementById('username')
    const passwordInput = document.getElementById('password')
    const togglePassword = document.getElementById('togglePassword')
    const errorMessage = document.getElementById('errorMessage')

    togglePassword.addEventListener('click', function () {
        const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password'
        passwordInput.setAttribute('type', type)
        this.classList.toggle('fa-eye')
        this.classList.toggle('fa-eye-slash')
    })

    loginForm.addEventListener('submit', function (event) {
        event.preventDefault()
        errorMessage.textContent = ''
        usernameInput.style.borderColor = 'rgba(255, 255, 255, 0.5)'
        passwordInput.style.borderColor = 'rgba(255, 255, 255, 0.5)'

        const username = usernameInput.value.trim()
        const password = passwordInput.value.trim()

        if (username === '' && password === '') {
            errorMessage.textContent = '请输入用户名和密码'
            usernameInput.style.borderColor = '#ffcccc'
            passwordInput.style.borderColor = '#ffcccc'
            return
        }

        if (username === '') {
            errorMessage.textContent = '请输入用户名'
            usernameInput.style.borderColor = '#ffcccc'
            return
        }

        if (password === '') {
            errorMessage.textContent = '请输入密码'
            passwordInput.style.borderColor = '#ffcccc'
            return
        }

        console.log('正在尝试登录...')
        console.log('用户名:', username)
        console.log('密码:', password)

        errorMessage.style.color = '#cceeff'
        errorMessage.textContent = '登录成功！正在跳转...'

        setTimeout(() => {
            alert(`欢迎您, ${username}!`)
        }, 1500)
    })
})
</script>

<template>
    <div class="login-container">
        <form id="loginForm">
            <h1>MindMate</h1>

            <div class="input-box">
                <input type="text" id="username" placeholder="用户名 / 邮箱" required />
                <i class="fa-solid fa-user icon icon-left"></i>
            </div>

            <div class="input-box">
                <input type="password" id="password" placeholder="密码" required />
                <i class="fa-solid fa-lock icon icon-left"></i>
                <i class="fa-solid fa-eye-slash toggle-password icon icon-right" id="togglePassword"></i>
            </div>

            <div id="errorMessage" class="error-message"></div>

            <div class="options">
                <label><input type="checkbox" /> 记住我</label>
                <a href="#">忘记密码?</a>
            </div>

            <button type="submit" class="btn">登 录</button>

            <div class="register-link">
                <p>还没有账户? <a href="#">立即注册</a></p>
            </div>
        </form>
    </div>
</template>

<style>
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
    font-family:
        'Helvetica Neue', 'Hiragino Sans GB', 'WenQuanYi Micro Hei', 'Microsoft Yahei', sans-serif;
}

body {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background: linear-gradient(135deg, #71b7e6, #9b59b6);
    overflow: hidden;
}

.login-container {
    width: 420px;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.2);
    backdrop-filter: blur(20px);
    box-shadow: 0 15px 25px rgba(0, 0, 0, 0.15);
    border-radius: 15px;
    padding: 40px;
    color: #fff;
    position: relative;
}

.login-container h1 {
    font-size: 36px;
    text-align: center;
    margin-bottom: 30px;
    text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.input-box {
    position: relative;
    width: 100%;
    height: 50px;
    margin-bottom: 25px;
}

.input-box input {
    width: 100%;
    height: 100%;
    background: transparent;
    border: none;
    outline: none;
    border: 2px solid rgba(255, 255, 255, 0.5);
    border-radius: 40px;
    font-size: 16px;
    color: #fff;
    /* 【CSS 修改】: 调整内边距，为左右图标都留出空间 */
    padding: 0 45px 0 45px;
    transition: border-color 0.3s ease;
}

.input-box input::placeholder {
    color: rgba(255, 255, 255, 0.8);
}

.input-box input:focus {
    border-color: #fff;
}

.input-box .icon {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    color: rgba(255, 255, 255, 0.8);
    transition: color 0.3s ease;
}

.input-box .icon-left {
    left: 15px;
}

.input-box .icon-right {
    right: 20px;
}

.input-box input:focus~.icon {
    color: #fff;
}

.input-box .toggle-password {
    cursor: pointer;
}

.options {
    display: flex;
    justify-content: space-between;
    font-size: 14.5px;
    margin-bottom: 30px;
}

.options label {
    color: rgba(255, 255, 255, 0.9);
    cursor: pointer;
}

.options label input {
    accent-color: #fff;
    margin-right: 5px;
}

.options a {
    color: rgba(255, 255, 255, 0.9);
    text-decoration: none;
}

.options a:hover {
    text-decoration: underline;
}

.btn {
    width: 100%;
    height: 45px;
    background: #fff;
    border: none;
    outline: none;
    border-radius: 40px;
    box-shadow: 0 5px 10px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    font-size: 16px;
    color: #333;
    font-weight: 600;
    transition: all 0.3s ease;
}

.btn:hover {
    background: linear-gradient(135deg, #a881d4, #71b7e6);
    color: #fff;
    transform: scale(1.02);
}

.register-link {
    font-size: 14.5px;
    text-align: center;
    margin-top: 20px;
}

.register-link p {
    color: rgba(255, 255, 255, 0.9);
}

.register-link p a {
    color: #fff;
    text-decoration: none;
    font-weight: 600;
}

.register-link p a:hover {
    text-decoration: underline;
}

.error-message {
    color: #ffcccc;
    font-size: 14px;
    text-align: center;
    margin-top: -15px;
    margin-bottom: 15px;
    height: 20px;
}
</style>
