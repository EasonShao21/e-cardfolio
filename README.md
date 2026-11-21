# e-cardfolio 數位名片管理系統

e-cardfolio 是響應式數位名片管理應用，支援名片清單、建立、編輯、刪除、配色與顯示欄位自訂、全螢幕名片展示、QR code 交換、附近系統分享、VCF 聯絡人匯出、名片圖片分享，以及本機名片收藏。

## 專案結構

- `backend/`：Spring Boot 3 REST API
- `frontend/`：Vue 3 + Vite 單頁應用
- chat-records/：AI 協助紀錄

## 交換與收藏

公開名片頁可在目前裝置加入收藏；收藏暫存於瀏覽器，不會跨裝置同步。名片管理預覽提供全螢幕出示模式，含 QR code、Web Share 系統分享、VCF 聯絡人檔與 PNG 名片圖片。系統分享面板會依裝置提供 AirDrop、Quick Share 或其他可用目標；收件者仍需接受。

## 技術架構

- **Backend**：Java 21、Spring Boot 3、Spring Web、Spring Data JPA、Bean Validation
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

開啟 Vite 顯示的網址（預設 `http://localhost:5173`）。若 API 不在 `http://localhost:8080`，設定前端環境變數 `VITE_API_BASE_URL` 為 API 前綴，例如 `https://api.example.com/api`。

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

## GitHub

Repository：<https://github.com/EasonShao21/e-cardfolio>

## AI 使用紀錄

本次使用 AI 協助建立名片領域模型、CRUD API 與 OpenAPI 契約，將前端資料接至 API，並處理手機透過區域網路掃描 QR code 的情境。程式碼標註與對話摘要位於 [`chat-records/009.chat`](chat-records/009.chat)、[`chat-records/010.chat`](chat-records/010.chat) 和 [`chat-records/011.chat`](chat-records/011.chat)。
