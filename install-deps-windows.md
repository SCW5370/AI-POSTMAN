# Windows 部署指南（WSL2）

Windows 原生不支持 `.sh` 脚本，推荐通过 **WSL2（Windows Subsystem for Linux 2）** 在 Windows 上运行一个真实的 Ubuntu 环境，安装完成后直接复用 `install-deps.sh` 一键部署，与 Ubuntu 服务器体验完全一致。

> **WSL2 适用系统**：Windows 10 版本 2004+（内部版本 19041+）或 Windows 11。

---

## 第一步：安装 WSL2 + Ubuntu

以**管理员身份**打开 PowerShell，执行：

```powershell
wsl --install
```

这一条命令会自动启用 WSL2 并安装 Ubuntu（默认版本为 Ubuntu 22.04 LTS）。

安装完成后**重启电脑**，重启后系统会自动打开 Ubuntu 终端，按提示设置用户名和密码（这是 Ubuntu 内部的账号，与 Windows 账号无关）。

> 如果已有旧版 WSL1，执行 `wsl --set-default-version 2` 升级到 WSL2。

---

## 第二步：将项目放入 WSL2 文件系统

打开 Ubuntu 终端（开始菜单搜索"Ubuntu"），将项目克隆到 WSL2 内部：

```bash
cd ~
git clone https://github.com/your-username/ai-postman.git
cd ai-postman
```

> ⚠️ **重要**：项目必须放在 WSL2 的文件系统内（`/home/你的用户名/` 下），**不要放在 `/mnt/c/`（即 Windows 的 C 盘）**。跨文件系统访问会导致 PostgreSQL、文件权限等问题，性能也很差。

---

## 第三步：一键安装依赖

在 Ubuntu 终端中，直接运行 Ubuntu 版安装脚本：

```bash
bash install-deps.sh
```

后续流程与 Ubuntu 服务器完全一致，约需 5-10 分钟。

---

## 第四步：填写配置

```bash
nano .env
```

填入你的 LLM API Key 和 QQ 邮箱 SMTP 配置（详见根目录 `.env` 文件注释）。

---

## 第五步：启动服务

```bash
bash start-all.sh
```

启动成功后，在 **Windows 浏览器**中访问：

- 内置控制台（Onboarding 入口）：`http://localhost:8080`
- React 管理台：`http://localhost:3000`

WSL2 的端口会自动映射到 Windows，直接在浏览器打开即可。

---

## 停止服务

```bash
bash stop-all.sh
```

---

## 常见问题

**Q：关闭 Ubuntu 终端窗口后服务还在跑吗？**
A：还在。WSL2 进程不依赖终端窗口，`nohup` 启动的后台进程会继续运行。但关机或注销 Windows 后服务会停止，下次需要重新运行 `start-all.sh`。

**Q：如何让服务开机自启？**
A：Windows 没有 systemd，开机自启较复杂，不推荐在 Windows 上做持久部署。如果你需要每天准时收信，建议部署到 Ubuntu 服务器（30-60 元/月）。

**Q：PostgreSQL 启动失败，提示权限问题？**
A：确保项目在 WSL2 内部路径（`~/ai-postman/`），而非 `/mnt/c/` 下。

**Q：`wsl --install` 提示"虚拟化未启用"？**
A：需要在 BIOS 中开启 CPU 虚拟化（Intel VT-x 或 AMD-V），不同品牌主板进 BIOS 的方式不同，请搜索"你的电脑品牌 + 开启虚拟化"。
