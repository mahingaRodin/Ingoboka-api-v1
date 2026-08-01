# Deploy Ingoboka API on Microsoft Azure ($200 credit)

**Goal:** Replace the down Kamatera host with Azure, keep the same Docker stack (API + Postgres + Redis + MinIO), and give the frontend / Swagger / USSD stable public URLs.

## Recommended architecture (cheapest + fastest)

Use **one Azure Linux VM** and the same `deploy/docker-compose.yml` you already have.

| Component | Where it runs | Notes |
|-----------|---------------|--------|
| Spring API | Docker on the VM (`api`) | Port **8085** → container 8080 |
| PostgreSQL | Docker on the VM (`postgres`) | Data volume on disk |
| Redis | Docker on the VM (`redis`) | OTP + USSD sessions |
| MinIO | Docker on the VM (`minio`) | Documents (ports 9000/9001) |

**Why not App Service / AKS first?** Those need separate Azure Database, Redis Cache, Blob Storage, and more wiring. A VM restores you in ~1 hour and fits well under **$200** credit for months of demo use.

**Suggested VM size**

| SKU | Approx. | Good for |
|-----|---------|----------|
| **Standard_B2s** (2 vCPU, 4 GB) | ~$30–40/mo | Demo / team testing (recommended) |
| Standard_B1s (1 vCPU, 1 GB) | cheaper | Often too small for Java + Postgres |
| Standard_B2ms (2 vCPU, 8 GB) | higher | If builds/OOM happen on B2s |

**Region:** pick one close to you / the team, e.g. `South Africa North`, `West Europe`, or `East US`. Stay in one region for all resources.

Estimated burn on B2s + disk + public IP: **~$35–50/month** → **$200 lasts ~4 months** for this demo stack.

---

## URLs the team will use (after deploy)

Replace `YOUR_IP` with the VM public IP (or later `api.yourdomain.com`).

| Audience | URL |
|----------|-----|
| **Frontend API base URL** | `http://YOUR_IP:8085/api/v1` |
| **Swagger UI** | `http://YOUR_IP:8085/swagger-ui.html` |
| **OpenAPI JSON** | `http://YOUR_IP:8085/api-docs` |
| **Health** | `http://YOUR_IP:8085/actuator/health` |
| **USSD callback (Africa’s Talking)** | `http://YOUR_IP:8085/api/v1/ussd/callback` |
| **USSD simulator** | `http://YOUR_IP:8085/api/v1/ussd/simulate` |
| **MinIO console** (optional) | `http://YOUR_IP:9001` |

Example once live:

```text
Frontend .env:
  NEXT_PUBLIC_API_URL=http://20.x.x.x:8085/api/v1

Swagger:
  http://20.x.x.x:8085/swagger-ui.html
```

> For production later: put **Nginx/Caddy + HTTPS** on 443 and drop the `:8085` from public URLs. Demo can stay on HTTP:8085 like Kamatera.

---

## Step-by-step (do in order)

### 0. Prerequisites on your laptop

- Azure account with the **$200 credit** active ([portal.azure.com](https://portal.azure.com))
- Azure CLI optional: `az login`
- GitHub repo access (to update deploy secrets)
- Same secrets you used before (JWT, mail, AT key, admin password) — copy from old `/opt/ingoboka/deploy/.env` if you still have a backup

### 1. Create a resource group

Portal: **Resource groups → Create**

- Name: `rg-ingoboka-demo`
- Region: your chosen region

Or CLI:

```bash
az group create --name rg-ingoboka-demo --location westeurope
```

### 2. Create the VM

Portal: **Virtual machines → Create → Azure virtual machine**

| Field | Value |
|-------|--------|
| Resource group | `rg-ingoboka-demo` |
| Name | `vm-ingoboka-api` |
| Image | **Ubuntu Server 22.04 LTS** |
| Size | **Standard_B2s** |
| Authentication | SSH public key (recommended) |
| Username | `azureuser` |
| Public inbound ports | Allow **SSH (22)** for now; we open 8085 next |
| Disks | Premium SSD 64 GB (or 30 GB OK for demo) |

Create → wait until **Running**.

Copy the **Public IP address** → this is `YOUR_IP`.

### 3. Open firewall ports (NSG)

On the VM → **Networking** → inbound rules → **Add**:

| Priority | Name | Port | Protocol | Source |
|----------|------|------|----------|--------|
| 1000 | ssh | 22 | TCP | Your IP (best) or Any |
| 1010 | api | **8085** | TCP | Any |
| 1020 | minio-api | 9000 | TCP | Your IP only (optional) |
| 1030 | minio-console | 9001 | TCP | Your IP only (optional) |

Do **not** expose Postgres (5432) or Redis (6379) publicly.

Also allow on the VM OS firewall if enabled (Ubuntu often uses NSG only).

### 4. SSH in and install Docker

```bash
ssh azureuser@YOUR_IP
```

Then run the bootstrap script from this repo (or paste the commands in `deploy/azure/bootstrap-vm.sh`):

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker azureuser
# log out and back in so docker group applies
exit
ssh azureuser@YOUR_IP
docker --version
docker compose version
```

### 5. First deploy of the app on the VM

```bash
sudo mkdir -p /opt/ingoboka/deploy
sudo chown -R azureuser:azureuser /opt/ingoboka
cd /opt/ingoboka
```

Either:

**A) Let GitHub Actions deploy** (after step 7), or  
**B) Manual first bring-up:**

```bash
# from your laptop (in the repo root)
scp -r deploy Dockerfile pom.xml mvnw .mvn src azureuser@YOUR_IP:/opt/ingoboka/
```

On the VM:

```bash
cd /opt/ingoboka/deploy
cp .env.example .env   # if you copied .env.example; else create .env
nano .env              # fill JWT, mail, passwords, CORS, AT keys
docker compose up -d --build
docker compose ps
docker compose logs -f api
```

Wait until you see `Started V1Application`.

### 6. Verify URLs

From your laptop:

```bash
curl -s http://YOUR_IP:8085/actuator/health
curl -s -o /dev/null -w "%{http_code}\n" http://YOUR_IP:8085/swagger-ui.html
```

Open Swagger in the browser: `http://YOUR_IP:8085/swagger-ui.html`

### 7. Point CI/CD at the new VM

GitHub repo → **Settings → Secrets and variables → Actions** → update:

| Secret | New value |
|--------|-----------|
| `SERVER_HOST` | `4.168.192.169` (Azure public IP — update if IP changes) |
| `SERVER_USERNAME` | `azureuser` |
| `SERVER_SSH_KEY` | Private key that matches the VM’s authorized key |
| `SERVER_SSH_PASSPHRASE` | Optional — only if the private key is encrypted; omit otherwise |

Push to `main` → existing workflow copies the bundle to `/opt/ingoboka` and runs `docker compose up -d --build`.

**Important:** `.env` is **not** in git. Keep `/opt/ingoboka/deploy/.env` on the VM; Actions must not overwrite it. Current script unpacks into `/opt/ingoboka` and runs compose from `deploy/` — ensure `.env` stays on the server (do not delete it during unpack).

### 8. Give the frontend team

```text
API base URL:     http://YOUR_IP:8085/api/v1
Swagger:          http://YOUR_IP:8085/swagger-ui.html
Health:           http://YOUR_IP:8085/actuator/health
```

Also update **CORS** in VM `.env`:

```env
CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app,http://localhost:3000
FRONTEND_VERIFY_EMAIL_URL=https://ingoboka-platform.vercel.app/verify-email
```

Then:

```bash
cd /opt/ingoboka/deploy && docker compose up -d api
```

### 9. Africa’s Talking (USSD)

In AT dashboard → USSD channel → set callback to:

```text
http://YOUR_IP:8085/api/v1/ussd/callback
```

(Use HTTPS later when you add a reverse proxy.)

### 10. Optional: static public IP + DNS

- Portal → Public IP of the VM → change assignment to **Static** (so IP does not change on stop/start).
- Optional: DNS `A` record `api.yourdomain.com` → that IP, then put Nginx + Let’s Encrypt in front.

---

## Required `.env` checklist on the VM

Minimum to boot:

```env
POSTGRES_DB=ingoboka
POSTGRES_USER=ingoboka
POSTGRES_PASSWORD=<strong>
JWT_SECRET=<openssl rand -base64 32>
PLATFORM_ADMIN_EMAIL=...
PLATFORM_ADMIN_PASSWORD=...
PLATFORM_ADMIN_PHONE=+250...
CORS_ALLOWED_ORIGINS=https://ingoboka-platform.vercel.app,http://localhost:3000
MAIL_USERNAME=...
MAIL_PASSWORD=...   # Gmail app password
SEED_DEMO_DATA=true
USSD_ENABLED=true
```

Copy the rest from `deploy/.env.example` (SMS / AT flags as needed).

---

## Cost control tips (stretch the $200)

1. **Stop the VM** when nobody is testing (Portal → Stop). B-series billing drops a lot when deallocated (public IP may still cost a little).
2. Do **not** create AKS, Application Gateway, or large Postgres Flexible Server yet.
3. Keep **one** region / one resource group so you can delete everything with one click if needed.
4. Watch **Cost Management + Billing → Credits**.

---

## Later upgrade path (when credit / product needs it)

| Now (VM) | Later (Azure native) |
|----------|----------------------|
| Postgres in Docker | Azure Database for PostgreSQL Flexible Server |
| Redis in Docker | Azure Cache for Redis |
| MinIO | Azure Blob Storage |
| Manual/SSH deploy | Azure Container Apps + ACR + GitHub Actions |

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Swagger timeout | NSG rule for **8085**, `docker compose ps`, `curl localhost:8085/actuator/health` on VM |
| Frontend CORS errors | `CORS_ALLOWED_ORIGINS` includes the Vercel origin exactly |
| USSD “technical problems” | AT callback URL = new IP; API logs `USSD response ... CON` |
| CI deploy fails SSH | `SERVER_HOST` / key / username; VM running |
| OOM / API crash | Resize VM to **B2ms** |

---

## What you do next (this hour)

1. Create `rg-ingoboka-demo` + Ubuntu **B2s** VM.  
2. Open port **8085**.  
3. Install Docker; create `/opt/ingoboka/deploy/.env`.  
4. `docker compose up -d --build`.  
5. Send frontend: `http://YOUR_IP:8085/api/v1` + Swagger link.  
6. Update GitHub `SERVER_*` secrets + AT USSD callback.

When you have the **public IP**, paste it here and we can double-check health/Swagger/CORS/USSD wording for the frontend message.
