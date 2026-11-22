# e-cardfolio 數位名片管理系統

e-cardfolio 是響應式數位名片管理應用，支援帳號註冊與登入、名片清單與 CRUD、配色與顯示欄位自訂、全螢幕名片展示、QR code 交換、附近系統分享、VCF 匯出，以及跨裝置同步收藏。

## 專案結構

- `backend/`：Spring Boot 3 REST API
- `frontend/`：Vue 3 + Vite 單頁應用
- chat-records/：AI 協助紀錄

## 交換與收藏

使用者以 Email 和至少 8 個字元的密碼註冊／登入。密碼以 BCrypt 雜湊後儲存，登入狀態使用伺服器 Session，並以 CSRF Token 保護寫入 API。名片與收藏會儲存在後端資料庫，帳號只能管理自己的名片；公開名片仍可匿名瀏覽。登入同一帳號的手機與桌機會取得相同收藏清單。舊版本機收藏會在該瀏覽器首次登入後嘗試匯入帳號。

第一次使用時開啟網站，選擇「還沒有帳號？免費註冊」；在另一台裝置用相同 Email 和密碼登入，即可同步名片與收藏。登入 Session 分別存在每台裝置的瀏覽器中。

公開名片頁可讓訪客登入或註冊後收藏；名片管理預覽提供全螢幕出示模式，含 QR code、Web Share 系統分享、VCF 聯絡人檔與 PNG 名片圖片。系統分享面板會依裝置提供 AirDrop、Quick Share 或其他可用目標；收件者仍需接受。第一次註冊的帳號會接手資料庫裡尚未綁定帳號的舊名片，之後建立的帳號只會看到自己的名片。

## 技術架構

- **Backend**：Java 21、Spring Boot 3、Spring Web、Spring Data JPA、Spring Security、Bean Validation
- **Frontend**：Vue 3、Vite
- **Database**：H2 持久化資料庫（開箱即用）；可透過環境變數切換至 PostgreSQL
- **API contract**：OpenAPI 3.0

## 本機啟動

需求：Java 21、Node.js 20.19+ 或 22.12+、npm。

### 1. 啟動 API

```bash
cd backend
./mvnw spring-boot:run
```

Windows PowerShell：`cd backend; .\mvnw.cmd spring-boot:run`

- `backend/`：Spring Boot 3 REST API

### 2. 啟動 Vue 前端

```bash
cd frontend
npm install
npm run dev
```

開啟 Vite 顯示的網址（預設 `http://localhost:5173`）。Vite 會將 `/api` 代理至 `http://localhost:8080`，以共享 Session Cookie 和 CSRF Cookie。正式部署需讓前端與 API 同源，或設定 `VITE_API_BASE_URL` 並配置支援 credentials 的 CORS 與 HTTPS Cookie。

## 部署成公開網站

Repository 根目錄的 `Dockerfile` 會先建置 Vue，再把 `frontend/dist` 包進 Spring Boot 靜態資源，讓網站、登入 Session、CSRF Cookie 和 API 都走同一個 HTTPS 網域。`/card/{id}` 公開名片連結支援直接開啟與重新整理。

以 Render 為例：

1. 將 GitHub repository 連到 Render，建立 **Web Service**，選擇 `Docker`，Dockerfile 與 Context 使用 repository 根目錄。
2. 建立 PostgreSQL 資料庫，並在 Web Service 設定：
   - `SPRING_DATASOURCE_URL`：`jdbc:postgresql://<資料庫內部主機>:5432/<資料庫名稱>`
   - `SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`：使用資料庫頁面提供的帳號密碼
   - `CARDFOLIO_FRONTEND_BASE_URL`：`https://<你的服務網址>`（可選）
3. 部署成功後，使用服務的 HTTPS 網址測試註冊、登入、跨裝置收藏與 `/card/{id}` 公開頁，再把該網址和 GitHub repository 網址放到 104 履歷的個人網站／專案成就。

部署環境請使用 PostgreSQL，不要依賴容器本機的 H2 檔案。Render 免費 Web Service 閒置後會休眠；Render 免費 PostgreSQL 有使用期限，若要長期保留帳號、名片與收藏，請選用不會到期的資料庫方案並定期備份。Render 會從連結的 Git repository 建置部署；部署的費用與免費層限制依平台目前方案為準。

### 用手機掃描本機 QR code

1. 確認手機和電腦連上同一個 Wi-Fi。
2. 啟動 Vite 後，從電腦的區域網路網址開啟前端（Vite 會顯示 `Network` 網址）；不要用 `localhost` 網址在手機開啟。
3. 電腦防火牆允許前端 `5173` 與後端 `8080` 連線。

在電腦以 `localhost:5173` 開發時，QR code 會使用後端偵測到的電腦區網 IP；手機與電腦須連同一個 Wi-Fi，並且 Vite 的 `5173`、Spring Boot 的 `8080` 埠都能從區網連線。若掃碼仍無法開啟，請以 Vite 顯示的 `Network` 網址在手機瀏覽器直接測試；公司／公共 Wi-Fi 的用戶隔離或電腦防火牆可能會阻擋裝置互連。

正式部署時，QR code 改用目前網站網址，不會指向開發者電腦的區網 IP。前端需設定 `VITE_API_BASE_URL` 指向可公開連線的 API，例如 `https://api.example.com/api`；若要固定分享網址，可設定後端 `CARDFOLIO_FRONTEND_BASE_URL`。

## OpenAPI 規格與 API

- `backend/`：Spring Boot 3 REST API
- 執行中 Swagger UI：<http://localhost:8080/swagger-ui.html>
- 執行中 OpenAPI JSON：<http://localhost:8080/v3/api-docs>
- 執行中 OpenAPI YAML：<http://localhost:8080/v3/api-docs.yaml>

| 方法 | 路徑 | 說明 |
| --- | --- | --- |
| `GET` | `/api/cards` | 列出名片 |
| `POST` | `/api/auth/register` | 建立帳號並登入 |
| `POST` | `/api/auth/login` | 登入 |
| `GET` | `/api/auth/me` | 取得目前使用者 |
| `POST` | `/api/auth/logout` | 登出 |
| `GET` | `/api/auth/csrf` | 初始化 CSRF Cookie 與 Token |
| `GET` | `/api/favorites` | 取得帳號收藏清單 |
| `POST` | `/api/favorites/{cardId}` | 收藏公開名片 |
| `DELETE` | `/api/favorites/{cardId}` | 移除收藏 |
| `GET` | `/api/share-config` | 取得手機掃碼用的前端網址 |
| `POST` | `/api/cards` | 建立名片 |
| `GET` | `/api/cards/{id}` | 取得公開名片（私人或不存在的名片回傳 404） |
| `PUT` | `/api/cards/{id}` | 更新名片資料與外觀設定 |
| `DELETE` | `/api/cards/{id}` | 刪除名片 |

成功建立回傳 `201`，成功刪除回傳 `204`。名片輸入欄位會在後端驗證必填姓名、職稱、Email 格式與欄位長度。

## 領域模型

- `backend/`：Spring Boot 3 REST API

| 欄位 | 型別 | 說明 |
| --- | --- | --- |
| `id` | Long | 名片識別碼 |
| `name` | String | 姓名，必填，最多 60 字元 |
| `role` | String | 職稱，必填，最多 80 字元 |
| `company` | String | 公司或品牌 |
| `email` | String | 電子郵件 |
| `phone` | String | 電話 |
| `website` | String | 個人網站或社群連結 |
| `location` | String | 所在地 |
| `color` | String | 背景主題：`violet`、`mint`、`sand` |
| `displayFields` | String[] | 名片正面要顯示的欄位：`email`、`phone`、`website`、`location` |
| `published` | Boolean | 是否公開；公開連結與 QR code 只顯示公開名片 |
| `createdAt` | Instant | 建立時間 |
| `updatedAt` | Instant | 最後更新時間 |

其他持久化模型：

| 模型 | 欄位 | 說明 |
| --- | --- | --- |
| `UserAccount` | `id`, `email`, `displayName`, `passwordHash`, `createdAt` | 使用者帳號；`passwordHash` 僅存 BCrypt 雜湊，不回傳至前端 |
| `FavoriteCard` | `id`, `user`, `card`, `createdAt` | 使用者與公開名片的關聯；同一帳號不能重複收藏同一名片 |
| `BusinessCard` | `owner` | 名片所屬帳號；匿名公開頁只讀取已公開名片 |

收藏、帳號與名片歸屬資料由 Spring Data JPA 儲存於目前設定的資料庫。

## GitHub

Repository：<https://github.com/EasonShao21/e-cardfolio>

## AI 使用紀錄

本次使用 AI 協助建立名片領域模型、CRUD API 與 OpenAPI 契約，將前端資料接至 API，處理手機掃描 QR code、RWD 選單與帳號登入／跨裝置收藏。程式碼標註與對話摘要位於 `chat-records/009.chat` 至 `chat-records/015.chat`。
