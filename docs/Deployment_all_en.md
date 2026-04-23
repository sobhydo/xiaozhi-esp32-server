# Full-Stack Deployment Guide (English)

This is the English translation of [Deployment_all.md](Deployment_all.md). It covers the **full-stack** setup: WebSocket server + admin web console + MySQL + Redis, all running in Docker.

> For a lighter single-module setup (no web console), see [Deployment.md](Deployment.md).

## Architecture overview

The full stack runs four Docker containers:

| Container | Image | Port | Purpose |
|---|---|---|---|
| `xiaozhi-esp32-server` | `ghcr.io/xinnan-tech/xiaozhi-esp32-server:server_latest` | 8000, 8003 | WebSocket server for ESP32 devices + vision HTTP |
| `xiaozhi-esp32-server-web` | `ghcr.io/xinnan-tech/xiaozhi-esp32-server:web_latest` | 8002 | Admin console (Spring Boot API + Vue frontend) |
| `xiaozhi-esp32-server-db` | `mysql:latest` | (internal) | Database |
| `xiaozhi-esp32-server-redis` | `redis:8.0` | (internal) | Cache / session store |

## Prerequisites

### Hardware
- **CPU**: x86_64 (official images). ARM64 requires local rebuild per [docker-build.md](docker-build.md).
- **RAM**: ≥ 4 GB (8 GB recommended)
- **Disk**: ≥ 10 GB free (speech model ~900 MB + images + MySQL data)

### Software
- Docker Engine + Docker Compose v2 plugin
- `wget` or `curl`

### Network ports to open
| Port | Who uses it |
|---|---|
| **8000** | ESP32 devices (WebSocket) |
| **8002** | You in a browser (admin console) |
| **8003** | Vision / image-analysis HTTP |

MySQL (3306) and Redis (6379) are **not** exposed publicly — they only talk over the Docker internal network.

### Accounts you'll need
At least one LLM API key. The default config points at **Zhipu AI** — you can swap to OpenAI, DeepSeek, Moonshot, etc. from the admin console after first boot. Optionally: TTS provider keys (Volcengine/Doubao, etc.).

---

## Method 1 — Docker (recommended)

### Step 1. Install Docker

Any modern Linux distro works. On Ubuntu:
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER  # log out and back in
```

### Step 2. Create the directory layout

```bash
mkdir -p xiaozhi-server/data
mkdir -p xiaozhi-server/models/SenseVoiceSmall
cd xiaozhi-server
```

Final layout:
```
xiaozhi-server/
├── docker-compose_all.yml
├── data/
│   └── .config.yaml
└── models/
    └── SenseVoiceSmall/
        └── model.pt
```

### Step 3. Download the speech recognition model

The default ASR is `SenseVoiceSmall` (~900 MB). Despite the Chinese origin, it supports Chinese, English, Japanese, Korean, and Cantonese.

```bash
wget -O models/SenseVoiceSmall/model.pt \
  https://modelscope.cn/models/iic/SenseVoiceSmall/resolve/master/model.pt
```

### Step 4. Download config files

```bash
wget https://raw.githubusercontent.com/xinnan-tech/xiaozhi-esp32-server/refs/heads/main/main/xiaozhi-server/docker-compose_all.yml

wget -O data/.config.yaml \
  https://raw.githubusercontent.com/xinnan-tech/xiaozhi-esp32-server/refs/heads/main/main/xiaozhi-server/config_from_api.yaml
```

### Step 5. (Recommended) harden before first boot

Open `docker-compose_all.yml` and change at minimum:

- `MYSQL_ROOT_PASSWORD` — replace `123456` with a strong password
- `SPRING_DATASOURCE_DRUID_PASSWORD` — must match the new MySQL password
- `TZ` — replace `Asia/Shanghai` with your timezone (e.g. `UTC`, `Africa/Cairo`)
- If `ghcr.nju.edu.cn` is slow for you, swap both image references to `ghcr.io/xinnan-tech/xiaozhi-esp32-server:...`

### Step 6. Start the stack

```bash
docker compose -f docker-compose_all.yml up -d
docker logs -f xiaozhi-esp32-server-web
```

Wait for `Started AdminApplication in X seconds`. The admin console is now live at `http://YOUR_SERVER_IP:8002`.

> The `xiaozhi-esp32-server` container on port 8000 will be erroring at this point — that's expected until Step 7 below.

### Step 7. First-time configuration

Open `http://YOUR_SERVER_IP:8002` and **register the first user** — they become the super-admin. Any subsequent user is a regular user (can bind devices and configure agents only).

You now need to do three things:

#### 7a. Wire the server to the admin API

1. Admin console → top menu **Parameter Management** (`参数管理`) → find the row with code `server.secret` → copy the value.
2. Edit `data/.config.yaml`:
   ```yaml
   manager-api:
     url: http://xiaozhi-esp32-server-web:8002/xiaozhi
     secret: <paste the server.secret value>
   ```
   The URL uses the container name because this is Docker-internal networking.

#### 7b. Add an LLM API key

Admin console → **Model Configuration** (`模型配置`) → sidebar **LLM** (`大语言模型`) → find `Zhipu AI` (or your preferred provider) → **Edit** (`修改`) → paste your API key → Save.

#### 7c. Restart the server and register endpoints

```bash
docker restart xiaozhi-esp32-server
docker logs -f xiaozhi-esp32-server
```
You should see `Server is running at ws://...:8000/xiaozhi/v1/`.

Back in admin console → **Parameter Management**, set:
- `server.websocket` → `ws://YOUR_PUBLIC_IP:8000/xiaozhi/v1/`
- `server.ota` → `http://YOUR_PUBLIC_IP:8002/xiaozhi/ota/`

These two URLs are what your ESP32 firmware points at.

### Step 8. Point your ESP32 at the server

Two options:
1. [Compile your own ESP32 firmware](firmware-build.md)
2. [Configure a custom server on pre-built firmware (v1.6.1+)](firmware-setting.md)

---

## Upgrading

```bash
cd xiaozhi-server
docker compose -f docker-compose_all.yml pull
docker compose -f docker-compose_all.yml up -d
```

Before upgrading, back up anything important from the admin console — the MySQL volume (`./mysql/data`) is persisted between runs, but keep copies of your API keys just in case.

## Tearing down

```bash
docker compose -f docker-compose_all.yml down          # stop + remove containers
docker compose -f docker-compose_all.yml down -v       # also remove volumes (DESTRUCTIVE)
```

---

## Method 2 — Running from source (development)

Only use this if you're actively modifying the code. Otherwise, Docker is simpler. See the Chinese [Deployment_all.md](Deployment_all.md#方式二本地源码运行全模块) starting from "方式二" for the source-run flow; the essentials are:

1. Install MySQL (or run `mysql:latest` in Docker), create database `xiaozhi_esp32_server` with `utf8mb4`.
2. Install Redis (or run `redis` in Docker on 6379).
3. Install JDK 21 + Maven, run `manager-api` from [main/manager-api/src/main/java/xiaozhi/AdminApplication.java](../main/manager-api/src/main/java/xiaozhi/AdminApplication.java).
4. Install Node.js, `cd main/manager-web && npm install && npm run serve`. Admin console → `http://localhost:8001`.
5. Install Conda + Python 3.10, create env, `conda install libopus ffmpeg`, then `pip install -r requirements.txt` in `main/xiaozhi-server`.
6. Download `SenseVoiceSmall/model.pt` (see Step 3 above).
7. Configure `data/.config.yaml` with `server.secret` from the admin console.
8. `python app.py` in `main/xiaozhi-server`.

---

## Deploying via GitHub Actions (self-hosted runner)

This repo ships a [`Deploy to Server` workflow](../.github/workflows/deploy.yml) that you can trigger from the GitHub UI (Actions tab → "Deploy to Server" → Run workflow). It performs `up` / `restart` / `logs` / `status` / `down` actions against the stack running on your server.

It runs on a **self-hosted runner** — a small daemon installed once on your server that receives jobs from GitHub. No SSH keys needed, no inbound ports opened.

### One-time runner setup on your server

1. On GitHub: go to your repo → **Settings** → **Actions** → **Runners** → **New self-hosted runner**. Pick Linux / x64.
2. On your server, follow the commands GitHub shows you — they look roughly like:
   ```bash
   mkdir -p ~/actions-runner && cd ~/actions-runner
   curl -o actions-runner-linux-x64.tar.gz -L https://github.com/actions/runner/releases/download/vX.Y.Z/actions-runner-linux-x64-X.Y.Z.tar.gz
   tar xzf actions-runner-linux-x64.tar.gz
   ./config.sh --url https://github.com/<YOU>/<REPO> --token <TOKEN_FROM_GITHUB> --labels xiaozhi
   ```
   The `--labels xiaozhi` is what makes the workflow match this runner.
3. Install and start as a service so it survives reboots:
   ```bash
   sudo ./svc.sh install
   sudo ./svc.sh start
   ```
4. Make sure the runner user can run `docker` without sudo:
   ```bash
   sudo usermod -aG docker $USER  # then re-login or reboot
   ```

### Verify

- `docker ps` as the runner user should work.
- In GitHub → Settings → Actions → Runners, the runner should show as **Idle**.
- Trigger the workflow: Actions tab → "Deploy to Server" → Run workflow → choose `status` → it should report the current container state.

### How it works

- The workflow checks out the repo on the runner.
- It copies the latest `main/xiaozhi-server/docker-compose_all.yml` into `DEPLOY_DIR` (default `/opt/xiaozhi-server`).
- Your persistent data (`data/.config.yaml`, `models/SenseVoiceSmall/model.pt`, `mysql/data/`) lives in `DEPLOY_DIR` and is never touched by the workflow.
- `docker compose pull && up -d` only recreates containers whose images changed.

### Customizing

Edit [.github/workflows/deploy.yml](../.github/workflows/deploy.yml):
- **`DEPLOY_DIR`** — change if your install isn't at `/opt/xiaozhi-server`.
- **Runner labels** — `runs-on: [self-hosted, xiaozhi]` must match what you passed to `./config.sh --labels`.
- **Trigger** — currently manual (`workflow_dispatch`). Add `push: branches: [main]` if you want auto-deploy on push.

---

## Production hardening checklist

- [ ] Change `MYSQL_ROOT_PASSWORD` from `123456`
- [ ] Put Nginx in front with TLS (Let's Encrypt). See [this issue](https://github.com/xinnan-tech/xiaozhi-esp32-server/issues/791) for a reference config
- [ ] Firewall: only expose 8000, 8002, 8003 publicly — keep MySQL/Redis internal
- [ ] Set up regular backups of `./mysql/data` and `./data/.config.yaml`
- [ ] Use a domain name + DNS so your ESP32 endpoints survive IP changes
- [ ] Monitor container health: `docker ps`, `docker logs`

## Common questions

See [FAQ.md](FAQ.md):
- Why does it transcribe my speech as Korean/Japanese/English?
- Why "TTS task failed: file does not exist"?
- TTS frequently fails / times out
- WiFi works but 4G can't connect to my custom server
- How do I speed up responses?
- It interrupts me while I'm still talking

## Further topics

- [Auto-pull and auto-build latest code](dev-ops-integration.md)
- [Deploy MQTT gateway for MQTT+UDP](mqtt-gateway-integration.md)
- [Nginx integration](https://github.com/xinnan-tech/xiaozhi-esp32-server/issues/791)
- [SMS registration for the admin console](ali-sms-integration.md)
- [HomeAssistant integration](homeassistant-integration.md)
- [Vision model for photo recognition](mcp-vision-integration.md)
- [MCP endpoint deploy](mcp-endpoint-enable.md) / [MCP endpoint integration](mcp-endpoint-integration.md)
- [Voiceprint recognition](voiceprint-integration.md)
- [Voice cloning on the admin console](huoshan-streamTTS-voice-cloning.md)
- [Local TTS: index-tts](index-stream-integration.md) / [fish-speech](fish-speech-integration.md) / [PaddleSpeech](paddlespeech-deploy.md)
- [Performance testing guide](performance_tester.md)
