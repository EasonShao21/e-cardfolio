<script setup>
import { computed, onMounted, ref } from 'vue'

// [AI assisted chat-009] AI 協助將介面由本機暫存改為呼叫 Spring Boot 名片 API。
// [AI assisted chat-010] 手機經區網開啟時，API 網址沿用電腦的區網主機名稱。
// [AI assisted chat-013] 修正展示彈窗狀態未宣告，並讓正式網址與區網網址各自使用正確來源。
// [AI assisted chat-015] 登入 session 與收藏清單由後端管理，避免裝置間資料分叉。
const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
const cards = ref([])
const pageMode = ref('cards')
const favoriteCards = ref([])
const currentUser = ref(null)
const authMode = ref('login')
const authError = ref('')
const authBusy = ref(false)
const authOpen = ref(false)
const authForm = ref({ email: '', password: '', displayName: '' })
const shareOrigin = ref(globalThis.location.origin)
const presentationOpen = ref(false)
const presentationCard = ref(null)
const presentationImage = ref(null)
const selectedId = ref(null)
const loading = ref(true)
const mobileMenuOpen = ref(false)
const search = ref('')
const modalOpen = ref(false)
const editingId = ref(null)
const toast = ref('')
const emptyForm = () => ({ name: '', role: '', company: '', email: '', phone: '', website: '', location: '', color: 'violet', displayFields: ['email', 'phone', 'website'], published: true })
const form = ref(emptyForm())
const visibleCards = computed(() => pageMode.value === 'favorites' ? favoriteCards.value : cards.value)
const selected = computed(() => visibleCards.value.find(card => card.id === selectedId.value) || visibleCards.value[0])
const filteredCards = computed(() => visibleCards.value.filter(card => `${card.name} ${card.role} ${card.company}`.toLowerCase().includes(search.value.toLowerCase())))
const publishedCount = computed(() => cards.value.filter(card => card.published).length)
function initials(name) { return name.trim().split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase() }
function readLegacyFavorites() {
  try { return JSON.parse(localStorage.getItem('e-cardfolio-favorites') || '[]') }
  catch { return [] }
}
function csrfCookie() {
  return document.cookie.split('; ').find(cookie => cookie.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('=') || ''
}
async function refreshCsrf() {
  const response = await fetch(`${apiBase}/auth/csrf`, { credentials: 'include' })
  if (!response.ok) throw new Error('無法初始化安全連線')
  const data = await response.json()
  return data.token
}
async function apiRequest(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase()
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    let csrf = csrfCookie()
    if (!csrf) csrf = await refreshCsrf()
    headers['X-XSRF-TOKEN'] = decodeURIComponent(csrf)
  }
  const response = await fetch(`${apiBase}${path}`, { credentials: 'include', ...options, headers })
  if (!response.ok) {
    let detail = ''
    try { detail = (await response.json()).error || '' } catch { }
    if (response.status === 401) throw new Error('登入狀態已失效，請重新登入。')
    if (response.status === 403) throw new Error('安全驗證失效，請重新操作。')
    if (response.status === 404) throw new Error('找不到這張名片')
    throw new Error(detail || '無法完成這項操作')
  }
  return response.status === 204 ? null : response.json()
}
async function request(path = '', options = {}) { return apiRequest(`/cards${path}`, options) }
function withInitials(card) { return { ...card, initials: initials(card.name) } }
function isFavorite(card) { return favoriteCards.value.some(item => String(item.id) === String(card?.id)) }
async function refreshFavorites() { favoriteCards.value = (await apiRequest('/favorites')).map(withInitials) }
async function toggleFavorite(card) {
  if (!card) return
  if (!currentUser.value) {
    authMode.value = 'login'; authOpen.value = true; authError.value = '登入或建立帳號後，收藏會同步到你的其他裝置。'
    return
  }
  try {
    if (isFavorite(card)) { await apiRequest(`/favorites/${card.id}`, { method: 'DELETE' }); notify('已從帳號收藏移除') }
    else { await apiRequest(`/favorites/${card.id}`, { method: 'POST' }); notify('已加入帳號收藏') }
    await refreshFavorites()
  } catch (error) { notify(error.message) }
}
function setPageMode(mode) {
  pageMode.value = mode
  mobileMenuOpen.value = false
  selectedId.value = mode === 'favorites' ? favoriteCards.value[0]?.id ?? null : cards.value[0]?.id ?? null
}
function xmlText(value) { return String(value || '').replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&apos;' })[char]) }
function vCardEscape(value) { return String(value || '').replace(/\\/g, '\\\\').replace(/\n/g, '\\n').replace(/,/g, '\\,').replace(/;/g, '\\;') }
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url; anchor.download = filename; anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
function downloadContact(card) {
  const rows = ['BEGIN:VCARD', 'VERSION:3.0', 'FN:' + vCardEscape(card.name), 'N:' + vCardEscape(card.name) + ';;;;']
  if (card.role) rows.push('TITLE:' + vCardEscape(card.role))
  if (card.company) rows.push('ORG:' + vCardEscape(card.company))
  if (card.email && card.displayFields.includes('email')) rows.push('EMAIL;TYPE=INTERNET:' + vCardEscape(card.email))
  if (card.phone && card.displayFields.includes('phone')) rows.push('TEL;TYPE=CELL:' + vCardEscape(card.phone))
  if (card.website && card.displayFields.includes('website')) rows.push('URL:' + vCardEscape(/^https?:\/\//i.test(card.website) ? card.website : 'https://' + card.website))
  if (card.location && card.displayFields.includes('location')) rows.push('ADR;TYPE=WORK:;;' + vCardEscape(card.location) + ';;;;')
  rows.push('END:VCARD')
  downloadBlob(new Blob([rows.join('\r\n')], { type: 'text/vcard;charset=utf-8' }), 'e-cardfolio-' + card.id + '.vcf')
  notify('聯絡人檔已下載')
}
async function createCardImage(card) {
  const canvas = document.createElement('canvas'); canvas.width = 1200; canvas.height = 750
  const context = canvas.getContext('2d')
  const colors = { violet: ['#f7f0fa', '#e5d8ed', '#614779'], mint: ['#f0f8f1', '#d9eadc', '#405d4b'], sand: ['#fbf5e9', '#ecdfc8', '#67563d'] }
  const palette = colors[card.color] || colors.violet
  const gradient = context.createLinearGradient(0, 0, 1200, 750); gradient.addColorStop(0, palette[0]); gradient.addColorStop(1, palette[1])
  context.fillStyle = gradient; context.fillRect(0, 0, 1200, 750)
  context.strokeStyle = '#ffffff99'; context.lineWidth = 3; context.beginPath(); context.arc(1050, 620, 280, 0, Math.PI * 2); context.stroke()
  context.beginPath(); context.arc(1040, 610, 190, 0, Math.PI * 2); context.stroke()
  context.fillStyle = palette[2]; context.font = '700 34px system-ui, sans-serif'; context.fillText('e-cardfolio', 82, 92)
  context.fillStyle = '#ffffffb8'; context.beginPath(); context.arc(1060, 110, 54, 0, Math.PI * 2); context.fill()
  context.fillStyle = palette[2]; context.font = '700 25px system-ui, sans-serif'; context.textAlign = 'center'; context.fillText(initials(card.name), 1060, 119); context.textAlign = 'left'
  context.fillStyle = palette[2]; context.font = '600 58px system-ui, "Noto Sans TC", sans-serif'; context.fillText(card.name, 82, 430)
  context.fillStyle = '#716778'; context.font = '28px system-ui, sans-serif'; context.fillText(card.role + (card.company ? ' · ' + card.company : ''), 84, 480)
  context.fillStyle = '#b4a1bd'; context.fillRect(84, 515, 74, 3)
  const fieldValues = { email: card.email, phone: card.phone, website: card.website, location: card.location }
  const icons = { email: '✉', phone: '↗', website: '↗', location: '⌖' }
  context.fillStyle = '#645a6d'; context.font = '22px system-ui, "Noto Sans TC", sans-serif'
  card.displayFields.filter(field => fieldValues[field]).slice(0, 4).forEach((field, index) => context.fillText(icons[field] + '  ' + fieldValues[field], 84, 565 + index * 38))
  return new Promise((resolve, reject) => canvas.toBlob(blob => blob ? resolve(blob) : reject(new Error('無法產生名片圖片')), 'image/png'))
}
async function openPresentation(card) {
  if (!card?.published) { notify('請先公開這張名片，才能出示給對方'); return }
  presentationCard.value = card; presentationImage.value = null; presentationOpen.value = true
  try { presentationImage.value = await createCardImage(card) } catch { notify('名片圖片產生失敗，仍可使用 QR code 交換') }
}
function closePresentation() { presentationOpen.value = false; presentationCard.value = null; presentationImage.value = null }
async function shareNearby(card) {
  const url = cardUrl(card)
  if (typeof navigator.share === 'function') {
    try { await navigator.share({ title: card.name + ' 的數位名片', text: '透過 e-cardfolio 與我交換名片', url }); return }
    catch (error) { if (error?.name === 'AbortError') return }
  }
  try { await navigator.clipboard.writeText(url); notify('此瀏覽器無法開啟附近分享，名片連結已複製') }
  catch { notify('請讓對方掃描畫面上的 QR code') }
}
async function shareCardImage(card) {
  if (!presentationImage.value) { notify('名片圖片正在準備'); return }
  const file = new File([presentationImage.value], 'e-cardfolio-' + card.id + '.png', { type: 'image/png' })
  if (typeof navigator.share === 'function' && navigator.canShare?.({ files: [file] })) {
    try { await navigator.share({ title: card.name + ' 的名片圖片', files: [file] }); return }
    catch (error) { if (error?.name === 'AbortError') return }
  }
  downloadBlob(presentationImage.value, 'e-cardfolio-' + card.id + '.png')
  notify('名片圖片已下載，可從相簿或檔案分享')
}
async function loadShareOrigin() {
  // 正式網站直接使用目前網址；只有本機 localhost 開發時才改用後端偵測的區網 IP。
  if (!['localhost', '127.0.0.1'].includes(globalThis.location.hostname)) return
  try {
    const response = await fetch(`${apiBase}/share-config`)
    if (response.ok) shareOrigin.value = (await response.json()).frontendBaseUrl
  } catch { /* 本機 API 未提供分享網址時，沿用目前瀏覽器網址。 */ }
}
async function loadCards() {
  if (!currentUser.value) { cards.value = []; loading.value = false; return }
  await loadShareOrigin()
  try {
    let result = await request()
    // [AI assisted chat-009] 首次啟動時將舊版瀏覽器範例資料匯入後端，避免使用者資料消失。
    const legacy = JSON.parse(localStorage.getItem('cardfolio-cards') || 'null')
    if (!result.length && legacy?.length) {
      result = await Promise.all(legacy.map(({ id, initials: _initials, ...card }) => request('', { method: 'POST', body: JSON.stringify({ ...card, displayFields: card.displayFields || ['email', 'phone', 'website'] }) })))
      localStorage.removeItem('cardfolio-cards')
    }
    cards.value = result.map(withInitials)
    selectedId.value = cards.value[0]?.id ?? null
  } catch (error) { notify(error.message) } finally { loading.value = false }
}
async function loadCurrentUser() {
  try {
    currentUser.value = await apiRequest('/auth/me')
    await refreshFavorites()
    await loadCards()
  } catch { currentUser.value = null; favoriteCards.value = []; cards.value = []; loading.value = false }
}
async function submitAuth() {
  authBusy.value = true; authError.value = ''
  try {
    await refreshCsrf()
    currentUser.value = await apiRequest(`/auth/${authMode.value}`, {
      method: 'POST',
      body: JSON.stringify({ email: authForm.value.email, password: authForm.value.password, displayName: authForm.value.displayName || null }),
    })
    authOpen.value = false
    const oldFavorites = readLegacyFavorites()
    if (oldFavorites.length) {
      await Promise.all(oldFavorites.map(card => apiRequest(`/favorites/${card.id}`, { method: 'POST' }).catch(() => null)))
      localStorage.removeItem('e-cardfolio-favorites')
    }
    await refreshFavorites()
    await loadCards()
    authForm.value.password = ''
    if (isPublicRoute && publicCard.value && !isFavorite(publicCard.value)) await toggleFavorite(publicCard.value)
    else notify(authMode.value === 'register' ? '帳號建立完成，收藏已同步至帳號' : '登入成功，收藏已同步')
  } catch (error) { authError.value = error.message }
  finally { authBusy.value = false }
}
async function logout() {
  try { await apiRequest('/auth/logout', { method: 'POST' }) } catch { }
  currentUser.value = null; favoriteCards.value = []; cards.value = []; pageMode.value = 'cards'; loading.value = false
  notify('已登出')
}
onMounted(async () => {
  try { await refreshCsrf() } catch { }
  await loadCurrentUser()
})
function openCreate() { editingId.value = null; form.value = emptyForm(); modalOpen.value = true }
function openEdit(card) { editingId.value = card.id; form.value = { ...card }; modalOpen.value = true }
async function saveCard() {
  if (!form.value.name.trim() || !form.value.role.trim()) return
  try {
    const editing = editingId.value
    const card = withInitials(await request(editing ? `/${editing}` : '', { method: editing ? 'PUT' : 'POST', body: JSON.stringify(form.value) }))
    if (editing) cards.value = cards.value.map(item => item.id === card.id ? card : item)
    else { cards.value.unshift(card); selectedId.value = card.id }
    modalOpen.value = false
    notify(editing ? '名片已更新' : '新名片已建立')
  } catch (error) { notify(error.message) }
}
async function removeCard(card) {
  if (pageMode.value === 'favorites') { toggleFavorite(card); return }
  if (!confirm(`確定要刪除「${card.name}」這張名片嗎？`)) return
  try {
    await request(`/${card.id}`, { method: 'DELETE' })
    cards.value = cards.value.filter(item => item.id !== card.id)
    if (selectedId.value === card.id) selectedId.value = cards.value[0]?.id ?? null
    notify('名片已刪除')
  } catch (error) { notify(error.message) }
}
async function togglePublished(card) {
  try {
    const updated = withInitials(await request(`/${card.id}`, { method: 'PUT', body: JSON.stringify({ ...card, published: !card.published }) }))
    cards.value = cards.value.map(item => item.id === updated.id ? updated : item)
    notify(updated.published ? '名片已公開' : '名片已設為私人')
  } catch (error) { notify(error.message) }
}
function notify(message) { toast.value = message; setTimeout(() => toast.value = '', 2400) }
function cardUrl(card) { return `${shareOrigin.value}/card/${card.id}` }
function copyLink(card) { navigator.clipboard?.writeText(cardUrl(card)); notify('名片連結已複製') }
function qrUrl(card) { return `https://api.qrserver.com/v1/create-qr-code/?size=180x180&margin=4&data=${encodeURIComponent(cardUrl(card))}` }
const isPublicRoute = globalThis.location.pathname.startsWith('/card/')
const publicCardId = isPublicRoute ? globalThis.location.pathname.split('/').pop() : null
const publicCard = ref(null)
const publicCardIsFavorite = computed(() => isFavorite(publicCard.value))
if (publicCardId) request(`/${publicCardId}`).then(card => { if (card.published) publicCard.value = withInitials(card) }).catch(() => {})
</script>

<template>
  <main v-if="isPublicRoute" class="public-card-page">
      <header class="public-card-header"><div class="public-card-brand"><span class="brand-mark">c.</span><span>e-cardfolio</span></div><a class="public-library-link" href="/">← <span>回到名片庫</span></a></header>
    <article v-if="publicCard" class="profile-card public-profile" :class="`theme-${publicCard.color}`">
      <span class="profile-brand">e-cardfolio<span>.</span></span><span class="profile-monogram">{{ publicCard.initials }}</span>
      <div class="profile-details"><div class="profile-name">{{ publicCard.name }}</div><div class="profile-role">{{ publicCard.role }}<template v-if="publicCard.company"> · {{ publicCard.company }}</template></div><div class="profile-rule"></div>
        <div class="profile-contact"><span v-for="field in publicCard.displayFields" :key="field" v-show="publicCard[field]">{{ ({ email: '✉', phone: '↗', website: '↗', location: '⌖' })[field] }} <span>{{ publicCard[field] }}</span></span></div>
      </div><span class="profile-decoration deco-a"></span><span class="profile-decoration deco-b"></span>
    </article>
    <section v-if="publicCard" class="public-card-actions">
      <button class="public-favorite-button" :class="{ favorited: publicCardIsFavorite }" @click="toggleFavorite(publicCard)"><span>{{ publicCardIsFavorite ? '✓' : '＋' }}</span>{{ publicCardIsFavorite ? '已收藏名片' : '收藏這張名片' }}</button>
      <button class="public-share-button" @click="shareNearby(publicCard)">附近分享 <span>↗</span></button>
      <button class="public-contact-button" @click="downloadContact(publicCard)">存入聯絡人 <span>↓</span></button>
      <p>{{ currentUser ? `已登入 ${currentUser.email}，收藏會同步到你的其他裝置。` : '登入帳號後收藏，即可在其他裝置同步查看。' }}</p>
    </section>
    <p v-if="!publicCard">找不到這張名片，或名片尚未公開。</p>
    <div v-if="authOpen" class="auth-modal-backdrop" @click.self="authOpen = false"><form class="auth-card auth-card-modal" @submit.prevent="submitAuth"><button class="auth-close" type="button" @click="authOpen = false">×</button><span class="brand-mark">c.</span><h2>{{ authMode === 'register' ? '建立帳號' : '登入 e-cardfolio' }}</h2><p>登入後，這張名片會收藏到你的帳號。</p><label v-if="authMode === 'register'">顯示名稱<input v-model="authForm.displayName" maxlength="100" autocomplete="name" placeholder="你的名字"></label><label>Email<input v-model="authForm.email" type="email" required maxlength="254" autocomplete="email" placeholder="name@example.com"></label><label>密碼<input v-model="authForm.password" type="password" required minlength="8" maxlength="72" :autocomplete="authMode === 'register' ? 'new-password' : 'current-password'" placeholder="至少 8 個字元"></label><p v-if="authError" class="auth-error">{{ authError }}</p><button class="primary-button auth-submit" :disabled="authBusy">{{ authBusy ? '處理中…' : authMode === 'register' ? '建立帳號並收藏' : '登入並收藏' }}</button><button class="auth-switch" type="button" @click="authMode = authMode === 'register' ? 'login' : 'register'; authError = ''">{{ authMode === 'register' ? '已有帳號？登入' : '還沒有帳號？免費註冊' }}</button></form></div>
  </main>
  <section v-else-if="!currentUser" class="auth-gate"><form class="auth-card" @submit.prevent="submitAuth"><span class="brand-mark">c.</span><div class="auth-eyebrow">YOUR DIGITAL IDENTITY</div><h1>{{ authMode === 'register' ? '建立你的帳號' : '歡迎回來' }}<span>.</span></h1><p>登入後管理你的名片，收藏也會在手機與桌機同步。</p><label v-if="authMode === 'register'">顯示名稱<input v-model="authForm.displayName" maxlength="100" autocomplete="name" placeholder="你的名字"></label><label>Email<input v-model="authForm.email" type="email" required maxlength="254" autocomplete="email" placeholder="name@example.com"></label><label>密碼<input v-model="authForm.password" type="password" required minlength="8" maxlength="72" :autocomplete="authMode === 'register' ? 'new-password' : 'current-password'" placeholder="至少 8 個字元"></label><p v-if="authError" class="auth-error">{{ authError }}</p><button class="primary-button auth-submit" :disabled="authBusy">{{ authBusy ? '處理中…' : authMode === 'register' ? '建立帳號' : '登入' }} <span>→</span></button><button class="auth-switch" type="button" @click="authMode = authMode === 'register' ? 'login' : 'register'; authError = ''">{{ authMode === 'register' ? '已有帳號？登入' : '還沒有帳號？免費註冊' }}</button></form></section>
  <div v-else class="app-shell">
    <aside class="sidebar" :class="{ 'menu-open': mobileMenuOpen }">
      <a class="brand" href="#"><span class="brand-mark">c.</span><span>cardfolio<span class="brand-dot">.</span></span></a>
      <button class="mobile-menu-toggle" type="button" :aria-expanded="mobileMenuOpen" aria-label="切換側邊選單" @click="mobileMenuOpen = !mobileMenuOpen"><span>{{ mobileMenuOpen ? '×' : '☰' }}</span><span class="mobile-menu-label">{{ mobileMenuOpen ? '關閉選單' : '選單' }}</span></button>
      <div class="workspace-label">WORKSPACE</div>
      <button class="nav-item" :class="{ active: pageMode === 'cards' }" @click="setPageMode('cards')"><span class="nav-icon">▦</span> 我的名片 <span class="nav-count">{{ cards.length }}</span></button>
      <button class="nav-item" :class="{ active: pageMode === 'favorites' }" @click="setPageMode('favorites')"><span class="nav-icon">♡</span> 收藏名片 <span class="nav-count">{{ favoriteCards.length }}</span></button>
      <!-- [AI assisted chat-018] 暫時隱藏尚未實作的公開頁面頁籤。 -->
      <!-- <button class="nav-item" @click="notify('公開頁面即將推出')"><span class="nav-icon">◎</span> 公開頁面</button> -->
      <div class="side-bottom">
        <div class="side-tip"><span class="tip-spark">✳</span><strong>讓每一次交換<br>都留下好印象。</strong><p>一張名片，連結你的<br>所有專業可能。</p><span class="tip-orb orb-one"></span><span class="tip-orb orb-two"></span></div>
        <div class="profile-row"><div class="avatar-small">{{ initials(currentUser.displayName) }}</div><div><strong>{{ currentUser.displayName }}</strong><small>{{ currentUser.email }}</small></div><button class="more logout-button" title="登出" @click="logout">↗</button></div>
      </div>
    </aside>

    <main class="main-content">
      <header class="topbar"><div class="breadcrumb">工作區 <span>/</span> <strong>{{ pageMode === 'favorites' ? '收藏名片' : '我的名片' }}</strong></div><div class="top-actions"><span class="saved-status"><i></i> 已自動儲存</span><button class="help-button" @click="notify('歡迎使用 Cardfolio')">?</button><div class="avatar-small" :title="currentUser.email">{{ initials(currentUser.displayName) }}</div><button class="logout-top" @click="logout">登出</button></div></header>
      <section class="page-heading"><div><div class="eyebrow"><span class="eyebrow-line"></span> YOUR DIGITAL IDENTITY</div><h1>{{ pageMode === 'favorites' ? '收藏名片' : '名片管理' }}<span class="heading-period">.</span></h1><p class="heading-copy">{{ pageMode === 'favorites' ? '把交換過的名片收藏在這裡。' : '把你的每個專業面向，整理成恰到好處的第一印象。' }}</p></div><button class="primary-button" @click="openCreate"><span>＋</span> 建立新名片</button></section>
      <section class="stats-row"><div class="stat-card"><div class="stat-icon lavender">▤</div><div><span>名片總數</span><strong>{{ cards.length }}<small> 張</small></strong></div><span class="stat-note">持續累積中</span></div><div class="stat-card"><div class="stat-icon green">↗</div><div><span>公開名片</span><strong>{{ publishedCount }}<small> 張</small></strong></div><span class="stat-note">隨時分享</span></div><div class="stat-card"><div class="stat-icon amber">⌁</div><div><span>數位名片</span><strong>100<small>%</small></strong></div><span class="stat-note">環保又便利</span></div></section>

      <div class="content-grid">
        <section class="library-panel"><div class="section-head"><div><h2>{{ pageMode === 'favorites' ? '你的收藏' : '你的名片庫' }}</h2><p>{{ pageMode === 'favorites' ? '收藏同步至你的帳號' : '管理並打造不同場合的自己' }}</p></div><label class="search-box"><span>⌕</span><input v-model="search" placeholder="搜尋名片..."></label></div>
          <div v-if="filteredCards.length" class="card-list"><button v-for="card in filteredCards" :key="card.id" class="business-card" :class="[`theme-${card.color}`, { chosen: selectedId === card.id }]" @click="selectedId = card.id"><span class="card-topline"><span class="mini-brand">c.</span><span class="card-type">DIGITAL CARD <i>✳</i></span></span><span class="card-identity"><span class="list-avatar">{{ card.initials }}</span><span><strong>{{ card.name }}</strong><small>{{ card.role }}<template v-if="card.company"> · {{ card.company }}</template></small></span></span><span class="card-bottom"><span class="visibility"><i :class="{ private: !card.published }"></i>{{ card.published ? '公開中' : '私人' }}</span><span class="card-date">{{ card.website || '尚未設定連結' }}</span></span><span v-if="selectedId === card.id" class="selected-check">✓</span></button></div>
          <div v-else class="empty-state"><span>{{ pageMode === 'favorites' ? '♡' : '⌕' }}</span><strong>{{ pageMode === 'favorites' ? '還沒有收藏名片' : '找不到符合的名片' }}</strong><p>{{ pageMode === 'favorites' ? '在公開名片頁按「收藏這張名片」，就會出現在這裡。' : '試試其他關鍵字，或建立一張新名片。' }}</p></div>
          <button class="add-card-dashed" @click="openCreate"><span>＋</span><strong>新增一張名片</strong><small>為不同身份或場合建立專屬版本</small></button>
        </section>

        <section v-if="selected" class="preview-panel"><div class="preview-heading"><div><h2>即時預覽</h2><p>名片的數位樣貌</p></div><button class="icon-button" title="編輯名片" @click="openEdit(selected)">✎</button></div><div class="preview-stage"><div class="profile-card" :class="`theme-${selected.color}`"><span class="profile-brand">cardfolio<span>.</span></span><span class="profile-monogram">{{ selected.initials }}</span><div class="profile-details"><div class="profile-name">{{ selected.name }}</div><div class="profile-role">{{ selected.role }}<template v-if="selected.company"> · {{ selected.company }}</template></div><div class="profile-rule"></div><div class="profile-contact"><span v-if="selected.displayFields.includes('email') && selected.email">✉ <span>{{ selected.email }}</span></span><span v-if="selected.displayFields.includes('phone') && selected.phone">↗ <span>{{ selected.phone }}</span></span><span v-if="selected.displayFields.includes('website') && selected.website">↗ <span>{{ selected.website }}</span></span><span v-if="selected.displayFields.includes('location') && selected.location">⌖ <span>{{ selected.location }}</span></span></div></div><span class="profile-decoration deco-a"></span><span class="profile-decoration deco-b"></span></div><div class="stage-caption"><span class="live-dot"></span> 正面預覽 <span class="caption-sep">·</span> {{ selected.color === 'violet' ? '暮紫' : selected.color === 'mint' ? '薄荷' : '暖沙' }}配色</div></div>
          <div class="exchange-entry"><span class="exchange-entry-icon">↗</span><div class="exchange-entry-copy"><strong>出示／交換名片</strong><p>展示你的名片，讓對方掃描收藏或使用附近分享</p></div><button class="exchange-entry-button" @click="openPresentation(selected)" :disabled="!selected.published" :title="selected.published ? '進入名片展示模式' : '請先公開名片'">出示名片 <span>→</span></button></div>
          <div class="quick-actions"><button v-if="pageMode === 'cards'" @click="openEdit(selected)"><span>✎</span> 編輯名片</button><button v-else @click="removeCard(selected)"><span>♡</span> 移除收藏</button><button v-if="pageMode === 'cards'" class="delete-action" @click="removeCard(selected)"><span>⌫</span> 刪除</button></div>
        </section>
        <section v-else class="preview-panel blank-preview"><div class="blank-icon">✳</div><h2>先建立你的第一張名片</h2><p>完成個人資訊後，就能即時預覽並分享。</p><button class="primary-button" @click="openCreate">＋ 建立新名片</button></section>
      </div>
      <footer class="footer"><span>© 2025 CARDFOLIO</span><span>你的專業，值得被好好介紹。 <b>✳</b></span></footer>
    </main>

    <div v-if="presentationOpen && presentationCard" class="presentation-backdrop" role="dialog" aria-modal="true" aria-label="出示與交換名片" @click.self="closePresentation" @keydown.esc="closePresentation" tabindex="-1">
      <header class="presentation-topbar"><button class="presentation-close" @click="closePresentation">← <span>返回名片庫</span></button><div class="presentation-wordmark"><span class="brand-mark">c.</span> e-cardfolio</div><span class="presentation-status"><i></i> 名片展示中</span></header>
      <div class="presentation-body">
        <section class="presentation-card-stage"><div class="presentation-hint"><span>01</span> 把這張名片出示給對方</div><article class="profile-card presentation-profile" :class="`theme-${presentationCard.color}`"><span class="profile-brand">e-cardfolio<span>.</span></span><span class="profile-monogram">{{ presentationCard.initials }}</span><div class="profile-details"><div class="profile-name">{{ presentationCard.name }}</div><div class="profile-role">{{ presentationCard.role }}<template v-if="presentationCard.company"> · {{ presentationCard.company }}</template></div><div class="profile-rule"></div><div class="profile-contact"><span v-for="field in presentationCard.displayFields" :key="field" v-show="presentationCard[field]">{{ ({ email: '✉', phone: '↗', website: '↗', location: '⌖' })[field] }} <span>{{ presentationCard[field] }}</span></span></div></div><span class="profile-decoration deco-a"></span><span class="profile-decoration deco-b"></span></article><p class="presentation-name-caption">{{ presentationCard.name }}<span>{{ presentationCard.role }}<template v-if="presentationCard.company"> · {{ presentationCard.company }}</template></span></p></section>
        <section class="presentation-exchange"><div class="presentation-eyebrow">MEET & CONNECT</div><h1>交換名片<span>.</span></h1><p class="presentation-description">請對方掃描 QR code 開啟名片並收藏，或使用手機的附近分享。</p><div class="presentation-qr"><img :src="qrUrl(presentationCard)" :alt="presentationCard.name + ' 的名片交換 QR code'"><span class="qr-scan-corner corner-tl"></span><span class="qr-scan-corner corner-tr"></span><span class="qr-scan-corner corner-bl"></span></div><div class="presentation-qr-caption"><strong>讓對方掃描此碼</strong><span>開啟名片後可登入收藏或存入聯絡人</span></div><div class="presentation-divider"><span>或使用附近分享</span></div><button class="nearby-share-button" @click="shareNearby(presentationCard)"><span>✦</span><span><strong>AirDrop／Quick Share</strong><small>開啟手機系統分享面板</small></span><b>→</b></button><div class="presentation-file-actions"><button @click="downloadContact(presentationCard)">下載聯絡人檔 <span>.vcf</span></button><button @click="shareCardImage(presentationCard)" :disabled="!presentationImage">{{ presentationImage ? '分享／下載名片圖片' : '正在準備圖片…' }} <span>↓</span></button></div><p class="presentation-footnote">附近分享需由接收者接受；登入後收藏會同步至此帳號的其他裝置。</p></section>
      </div>
    </div>    <div v-if="modalOpen" class="modal-backdrop" @click.self="modalOpen = false"><form class="edit-modal" @submit.prevent="saveCard"><div class="modal-top"><div><span class="eyebrow">MAKE IT YOURS</span><h2>{{ editingId ? '編輯名片' : '建立新名片' }}</h2><p>填入你的資訊，打造專屬數位名片。</p></div><button type="button" class="close-button" @click="modalOpen = false">×</button></div><div class="form-grid"><label class="full-field">姓名 <span>*</span><input v-model="form.name" required maxlength="60" placeholder="例如：林予安"></label><label>職稱 <span>*</span><input v-model="form.role" required maxlength="80" placeholder="例如：Product Designer"></label><label>公司 / 品牌<input v-model="form.company" maxlength="80" placeholder="例如：Studio North"></label><label>Email<input v-model="form.email" type="email" placeholder="hello@example.com"></label><label>電話<input v-model="form.phone" placeholder="+886 912 345 678"></label><label class="full-field">個人網站 / 社群<input v-model="form.website" placeholder="yourwebsite.com"></label><label class="full-field">所在地<input v-model="form.location" placeholder="Taipei, Taiwan"></label></div><div class="color-picker"><span>名片配色</span><button v-for="color in ['violet', 'mint', 'sand']" :key="color" type="button" :class="[`swatch-${color}`, { picked: form.color === color }]" @click="form.color = color" :aria-label="color"></button></div><fieldset class="display-fields"><legend>名片顯示欄位</legend><label v-for="item in [{ key: 'email', label: 'Email' }, { key: 'phone', label: '電話' }, { key: 'website', label: '網站' }, { key: 'location', label: '所在地' }]" :key="item.key"><input v-model="form.displayFields" type="checkbox" :value="item.key"> {{ item.label }}</label></fieldset><label class="publish-toggle"><input v-model="form.published" type="checkbox"><span class="toggle-ui"></span><span><strong>公開這張名片</strong><small>開啟後可透過連結和 QR code 分享</small></span></label><div class="modal-actions"><button type="button" class="cancel-button" @click="modalOpen = false">取消</button><button class="primary-button" type="submit">{{ editingId ? '儲存變更' : '建立名片' }} <span>→</span></button></div></form></div>
    <Transition name="toast"><div v-if="toast" class="toast-message"><span>✓</span>{{ toast }}</div></Transition>
  </div>
</template>
