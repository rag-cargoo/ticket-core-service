const STORAGE_KEYS = {
  apiBase: "ticketrush_u1_api_base",
  accessToken: "ticketrush_u1_access_token",
  refreshToken: "ticketrush_u1_refresh_token",
  oauthProvider: "ticketrush_u1_oauth_provider",
  oauthState: "ticketrush_u1_oauth_state",
  authUser: "ticketrush_u1_auth_user"
};

const els = {
  apiBaseInput: document.getElementById("apiBaseInput"),
  useCurrentOriginBtn: document.getElementById("useCurrentOriginBtn"),
  loginStatusBadge: document.getElementById("loginStatusBadge"),
  actionStatus: document.getElementById("actionStatus"),
  clearSessionBtn: document.getElementById("clearSessionBtn"),
  currentUserView: document.getElementById("currentUserView"),
  loginKakaoBtn: document.getElementById("loginKakaoBtn"),
  loginNaverBtn: document.getElementById("loginNaverBtn"),
  meBtn: document.getElementById("meBtn"),
  refreshBtn: document.getElementById("refreshBtn"),
  logoutBtn: document.getElementById("logoutBtn"),
  accessTokenView: document.getElementById("accessTokenView"),
  refreshTokenView: document.getElementById("refreshTokenView"),
  tokenPairView: document.getElementById("tokenPairView"),
  concertSearchInput: document.getElementById("concertSearchInput"),
  concertArtistFilter: document.getElementById("concertArtistFilter"),
  concertSortSelect: document.getElementById("concertSortSelect"),
  refreshConcertsBtn: document.getElementById("refreshConcertsBtn"),
  seatAvailableOnlyCheck: document.getElementById("seatAvailableOnlyCheck"),
  concertResultSummary: document.getElementById("concertResultSummary"),
  concertList: document.getElementById("concertList"),
  optionList: document.getElementById("optionList"),
  seatList: document.getElementById("seatList"),
  queueUserIdInput: document.getElementById("queueUserIdInput"),
  queueConcertIdInput: document.getElementById("queueConcertIdInput"),
  queueJoinBtn: document.getElementById("queueJoinBtn"),
  queueStatusBtn: document.getElementById("queueStatusBtn"),
  queueSubscribeBtn: document.getElementById("queueSubscribeBtn"),
  queueUnsubscribeBtn: document.getElementById("queueUnsubscribeBtn"),
  queueStateView: document.getElementById("queueStateView"),
  seatIdInput: document.getElementById("seatIdInput"),
  reservationIdInput: document.getElementById("reservationIdInput"),
  createHoldBtn: document.getElementById("createHoldBtn"),
  startPayingBtn: document.getElementById("startPayingBtn"),
  confirmBtn: document.getElementById("confirmBtn"),
  cancelBtn: document.getElementById("cancelBtn"),
  refundBtn: document.getElementById("refundBtn"),
  getReservationBtn: document.getElementById("getReservationBtn"),
  getMyReservationsBtn: document.getElementById("getMyReservationsBtn"),
  consoleView: document.getElementById("consoleView")
};

const state = {
  apiBase: loadApiBase(),
  accessToken: localStorage.getItem(STORAGE_KEYS.accessToken) || "",
  refreshToken: localStorage.getItem(STORAGE_KEYS.refreshToken) || "",
  currentUser: loadAuthUser(),
  concerts: [],
  filteredConcerts: [],
  options: [],
  seats: [],
  selectedConcertId: null,
  selectedOptionId: null,
  selectedSeatId: null,
  queueEventSource: null,
  queueState: {
    userId: null,
    concertId: null,
    status: "NONE",
    rank: -1,
    activeTtlSeconds: 0,
    lastEvent: "IDLE",
    updatedAt: null
  }
};

const REQUEST_TIMEOUT_MS = 8000;

function loadApiBase() {
  const stored = localStorage.getItem(STORAGE_KEYS.apiBase);
  if (stored && stored.trim() !== "") {
    return stored.trim();
  }
  return window.location.origin;
}

function loadAuthUser() {
  const raw = localStorage.getItem(STORAGE_KEYS.authUser);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw);
  } catch (_) {
    localStorage.removeItem(STORAGE_KEYS.authUser);
    return null;
  }
}

function parseJsonResponse(text) {
  try {
    return text ? JSON.parse(text) : {};
  } catch (_) {
    return text;
  }
}

function truncateMessage(message, maxLength = 180) {
  const text = String(message || "");
  if (text.length <= maxLength) {
    return text;
  }
  return `${text.slice(0, maxLength - 3)}...`;
}

function summarizeTokenForLog(token) {
  if (typeof token !== "string" || !token) {
    return "missing";
  }
  return `stored (len=${token.length})`;
}

function redactTokenFields(payload) {
  if (Array.isArray(payload)) {
    return payload.map((item) => redactTokenFields(item));
  }
  if (!payload || typeof payload !== "object") {
    return payload;
  }

  const next = {};
  for (const [key, value] of Object.entries(payload)) {
    if (key === "accessToken" || key === "refreshToken") {
      next[key] = summarizeTokenForLog(value);
      continue;
    }
    next[key] = redactTokenFields(value);
  }
  return next;
}

async function fetchWithTimeout(url, options = {}, timeoutMs = REQUEST_TIMEOUT_MS) {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal
    });
  } catch (error) {
    if (error?.name === "AbortError") {
      throw new Error(`request timeout (${timeoutMs}ms). check backend server`);
    }
    throw new Error(`network error. check backend server (${String(error.message || error)})`);
  } finally {
    window.clearTimeout(timer);
  }
}

function isUnauthorizedError(errorObj) {
  const message = String(errorObj?.message || errorObj || "");
  return /->\s*401\b/.test(message) || /"status"\s*:\s*401/.test(message) || /unauthorized/i.test(message);
}

function appendLog(tag, payload) {
  const timestamp = new Date().toISOString();
  const safePayload = redactTokenFields(payload);
  const body = typeof safePayload === "string" ? safePayload : JSON.stringify(safePayload, null, 2);
  const prev = els.consoleView.textContent || "";
  const next = `[${timestamp}] ${tag}\n${body}\n\n${prev}`;
  els.consoleView.textContent = next;
}

function formatStatusTime() {
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, "0");
  const mm = String(now.getMinutes()).padStart(2, "0");
  const ss = String(now.getSeconds()).padStart(2, "0");
  return `${hh}:${mm}:${ss}`;
}

function setActionStatus(message, status = "info") {
  if (!els.actionStatus) {
    return;
  }

  els.actionStatus.textContent = truncateMessage(`[${formatStatusTime()}] ${message}`, 220);
  els.actionStatus.classList.remove("u1-action-info", "u1-action-ok", "u1-action-error");

  if (status === "ok") {
    els.actionStatus.classList.add("u1-action-ok");
    return;
  }
  if (status === "error") {
    els.actionStatus.classList.add("u1-action-error");
    return;
  }
  els.actionStatus.classList.add("u1-action-info");
}

function setApiBase(nextBase, options = {}) {
  const normalized = (nextBase || "").trim().replace(/\/+$/, "");
  if (!normalized) {
    throw new Error("api base is required");
  }
  state.apiBase = normalized;
  localStorage.setItem(STORAGE_KEYS.apiBase, state.apiBase);
  els.apiBaseInput.value = state.apiBase;
  if (!options.silent) {
    appendLog("API_BASE_SET", { apiBase: state.apiBase });
  }
}

function describeTokenState(token) {
  if (!token) {
    return "missing";
  }
  return `stored (len=${token.length})`;
}

function renderTokenPairCheck() {
  if (!els.tokenPairView) {
    return;
  }

  els.tokenPairView.classList.remove("u1-token-neutral", "u1-token-good", "u1-token-bad");

  if (!state.accessToken || !state.refreshToken) {
    els.tokenPairView.textContent = "insufficient data (login first)";
    els.tokenPairView.classList.add("u1-token-neutral");
    return;
  }

  if (state.accessToken === state.refreshToken) {
    els.tokenPairView.textContent = "SAME (invalid)";
    els.tokenPairView.classList.add("u1-token-bad");
    return;
  }

  els.tokenPairView.textContent = "DIFFERENT (normal)";
  els.tokenPairView.classList.add("u1-token-good");
}

function renderAuthSummary() {
  els.accessTokenView.textContent = describeTokenState(state.accessToken);
  els.refreshTokenView.textContent = describeTokenState(state.refreshToken);
  renderTokenPairCheck();

  if (state.currentUser) {
    els.currentUserView.textContent = JSON.stringify(state.currentUser, null, 2);
  } else {
    els.currentUserView.textContent = "-";
  }

  if (state.accessToken) {
    els.loginStatusBadge.textContent = "Logged In";
    els.loginStatusBadge.classList.remove("u1-badge-off", "u1-badge-warn");
    els.loginStatusBadge.classList.add("u1-badge-on");
  } else {
    els.loginStatusBadge.textContent = "Logged Out";
    els.loginStatusBadge.classList.remove("u1-badge-on", "u1-badge-warn");
    els.loginStatusBadge.classList.add("u1-badge-off");
  }
}

function setTokens(accessToken, refreshToken) {
  state.accessToken = accessToken || "";
  state.refreshToken = refreshToken || "";

  if (state.accessToken) {
    localStorage.setItem(STORAGE_KEYS.accessToken, state.accessToken);
  } else {
    localStorage.removeItem(STORAGE_KEYS.accessToken);
  }

  if (state.refreshToken) {
    localStorage.setItem(STORAGE_KEYS.refreshToken, state.refreshToken);
  } else {
    localStorage.removeItem(STORAGE_KEYS.refreshToken);
  }

  renderAuthSummary();
}

function setCurrentUser(user) {
  state.currentUser = user || null;
  if (state.currentUser) {
    localStorage.setItem(STORAGE_KEYS.authUser, JSON.stringify(state.currentUser));
  } else {
    localStorage.removeItem(STORAGE_KEYS.authUser);
  }
  renderAuthSummary();
  syncQueueUserIdInput();
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
  localStorage.removeItem(STORAGE_KEYS.authUser);
  localStorage.removeItem(STORAGE_KEYS.oauthState);
  state.accessToken = "";
  state.refreshToken = "";
  state.currentUser = null;
  closeQueueSubscription("SESSION_CLEARED", { silent: true });
  els.queueUserIdInput.value = "";
  setQueueState({
    userId: null,
    status: "NONE",
    rank: -1,
    activeTtlSeconds: 0,
    lastEvent: "SESSION_CLEARED"
  });
  renderAuthSummary();
}

function requireSeatId() {
  const seatId = Number(els.seatIdInput.value.trim());
  if (!Number.isInteger(seatId) || seatId <= 0) {
    throw new Error("valid seatId is required");
  }
  return seatId;
}

function requireReservationId() {
  const reservationId = Number(els.reservationIdInput.value.trim());
  if (!Number.isInteger(reservationId) || reservationId <= 0) {
    throw new Error("valid reservationId is required");
  }
  return reservationId;
}

function ensureAccessToken() {
  if (!state.accessToken) {
    throw new Error("access token is empty. run social login first.");
  }
}

function randomFingerprint(prefix) {
  const token = Math.random().toString(36).slice(2, 10);
  return `${prefix}-${token}`;
}

async function callApi(path, options = {}) {
  const method = options.method || "GET";
  const url = `${state.apiBase}${path}`;
  const headers = {
    "Content-Type": "application/json"
  };

  if (options.auth) {
    ensureAccessToken();
    headers.Authorization = `Bearer ${state.accessToken}`;
  }

  const response = await fetchWithTimeout(url, {
    method,
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const text = await response.text();
  const parsed = parseJsonResponse(text);

  if (!response.ok) {
    throw new Error(`${method} ${path} -> ${response.status}\n${JSON.stringify(parsed, null, 2)}`);
  }

  return parsed;
}

function buildOauthState(provider) {
  const nonce = Math.random().toString(36).slice(2, 12);
  const ts = Date.now();
  // Naver can return `state` without URL-encoding on callback.
  // Keep a URL-safe delimiter to avoid Tomcat 400 on reserved chars (e.g. `|`).
  return `u1_${provider}_${ts}_${nonce}`;
}

async function startSocialLogin(provider) {
  const oauthState = buildOauthState(provider);
  localStorage.setItem(STORAGE_KEYS.oauthProvider, provider);
  localStorage.setItem(STORAGE_KEYS.oauthState, oauthState);

  const response = await callApi(`/api/auth/social/${provider}/authorize-url?state=${encodeURIComponent(oauthState)}`);
  appendLog("SOCIAL_AUTHORIZE_URL", response);

  if (response.state && response.state !== oauthState) {
    throw new Error("oauth state mismatch before redirect");
  }
  if (!response.authorizeUrl) {
    throw new Error("authorizeUrl is missing in response");
  }

  window.location.href = response.authorizeUrl;
}

async function onMe() {
  const response = await callApi("/api/auth/me", { auth: true });
  setCurrentUser(response);
  appendLog("AUTH_ME", response);
  return response;
}

async function onRefresh() {
  if (!state.refreshToken) {
    throw new Error("refresh token is empty");
  }

  const response = await callApi("/api/auth/token/refresh", {
    method: "POST",
    body: { refreshToken: state.refreshToken }
  });

  setTokens(response.accessToken, response.refreshToken);
  appendLog("AUTH_REFRESH", response);
  await onMe();
}

async function bootstrapAuthSession() {
  if (!state.accessToken) {
    appendLog("AUTH_BOOTSTRAP_SKIP", "no access token in localStorage");
    return;
  }

  try {
    await onMe();
    appendLog("AUTH_BOOTSTRAP_OK", "access token is valid");
    return;
  } catch (meError) {
    appendLog("AUTH_BOOTSTRAP_ME_FAILED", String(meError.message || meError));
    if (!isUnauthorizedError(meError) || !state.refreshToken) {
      clearSession();
      appendLog("AUTH_BOOTSTRAP_RESET", "invalid session cleared");
      return;
    }
  }

  try {
    await onRefresh();
    appendLog("AUTH_BOOTSTRAP_REFRESH_OK", "access token refreshed");
  } catch (refreshError) {
    appendLog("AUTH_BOOTSTRAP_REFRESH_FAILED", String(refreshError.message || refreshError));
    clearSession();
    appendLog("AUTH_BOOTSTRAP_RESET", "refresh failed, session cleared");
  }
}

async function onLogout() {
  if (!state.refreshToken) {
    throw new Error("refresh token is empty");
  }

  const response = await callApi("/api/auth/logout", {
    method: "POST",
    auth: true,
    body: { refreshToken: state.refreshToken }
  });

  clearSession();
  appendLog("AUTH_LOGOUT", response);
}

async function onCreateHold() {
  const seatId = requireSeatId();
  const response = await callApi("/api/reservations/v7/holds", {
    method: "POST",
    auth: true,
    body: {
      seatId,
      requestFingerprint: randomFingerprint("u1-request"),
      deviceFingerprint: randomFingerprint("u1-device")
    }
  });

  if (response.id) {
    els.reservationIdInput.value = String(response.id);
  }

  appendLog("RESERVATION_HOLD_V7", response);
}

async function transitionReservation(action) {
  const reservationId = requireReservationId();
  const response = await callApi(`/api/reservations/v7/${reservationId}/${action}`, {
    method: "POST",
    auth: true
  });

  appendLog(`RESERVATION_${action.toUpperCase()}_V7`, response);
}

async function onGetReservation() {
  const reservationId = requireReservationId();
  const response = await callApi(`/api/reservations/v7/${reservationId}`, { auth: true });
  appendLog("RESERVATION_GET_V7", response);
}

async function onGetMyReservations() {
  const response = await callApi("/api/reservations/v7/me", { auth: true });
  appendLog("RESERVATION_ME_V7", response);
}

function parsePositiveInteger(rawValue) {
  const parsed = Number(String(rawValue || "").trim());
  if (!Number.isInteger(parsed) || parsed <= 0) {
    return null;
  }
  return parsed;
}

function currentUserId() {
  if (!state.currentUser || typeof state.currentUser !== "object") {
    return null;
  }
  return parsePositiveInteger(state.currentUser.userId);
}

function syncQueueUserIdInput(options = {}) {
  const userId = currentUserId();
  if (!userId) {
    return;
  }
  const existing = parsePositiveInteger(els.queueUserIdInput.value);
  if (options.force || !existing) {
    els.queueUserIdInput.value = String(userId);
  }
}

function syncQueueConcertIdInput(options = {}) {
  if (!state.selectedConcertId) {
    if (options.clearWhenMissing) {
      els.queueConcertIdInput.value = "";
    }
    return;
  }
  const existing = parsePositiveInteger(els.queueConcertIdInput.value);
  if (options.force || !existing) {
    els.queueConcertIdInput.value = String(state.selectedConcertId);
  }
}

function requireQueueUserId() {
  const fromInput = parsePositiveInteger(els.queueUserIdInput.value);
  if (fromInput) {
    return fromInput;
  }

  const fromSession = currentUserId();
  if (fromSession) {
    els.queueUserIdInput.value = String(fromSession);
    return fromSession;
  }

  throw new Error("queue userId is required (login or input manually)");
}

function requireQueueConcertId() {
  const fromInput = parsePositiveInteger(els.queueConcertIdInput.value);
  if (fromInput) {
    return fromInput;
  }

  if (state.selectedConcertId) {
    els.queueConcertIdInput.value = String(state.selectedConcertId);
    return state.selectedConcertId;
  }

  throw new Error("queue concertId is required (select concert or input manually)");
}

async function callQueueApi(path, options = {}) {
  const method = options.method || "GET";
  const url = `${state.apiBase}/api/v1/waiting-queue${path}`;

  const response = await fetchWithTimeout(url, {
    method,
    headers: { "Content-Type": "application/json" },
    body: options.body ? JSON.stringify(options.body) : undefined
  });

  const text = await response.text();
  const parsed = parseJsonResponse(text);

  if (!response.ok) {
    throw new Error(`${method} ${path} -> ${response.status}\n${JSON.stringify(parsed, null, 2)}`);
  }

  return parsed;
}

function setQueueState(nextState) {
  state.queueState = {
    ...state.queueState,
    ...nextState,
    updatedAt: new Date().toISOString()
  };
  renderQueueState();
}

function renderQueueState() {
  const snapshot = {
    ...state.queueState,
    sse: state.queueEventSource ? "OPEN" : "CLOSED"
  };
  els.queueStateView.textContent = JSON.stringify(snapshot, null, 2);
}

function parseSsePayload(raw) {
  if (typeof raw !== "string") {
    return raw;
  }

  const trimmed = raw.trim();
  if (!trimmed) {
    return {};
  }

  if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
      (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
    try {
      return JSON.parse(trimmed);
    } catch (_) {
      return { message: trimmed };
    }
  }

  return { message: trimmed };
}

function applyQueueEvent(eventName, payload, fallbackUserId, fallbackConcertId) {
  const normalized = payload && typeof payload === "object" ? payload : { message: String(payload || "") };
  const nextUserId = parsePositiveInteger(normalized.userId) || fallbackUserId || state.queueState.userId;
  const nextConcertId = parsePositiveInteger(normalized.concertId) || fallbackConcertId || state.queueState.concertId;
  const nextRank = Number.isFinite(Number(normalized.rank)) ? Number(normalized.rank) : state.queueState.rank;
  const nextTtl = Number.isFinite(Number(normalized.activeTtlSeconds))
    ? Number(normalized.activeTtlSeconds)
    : state.queueState.activeTtlSeconds;

  setQueueState({
    userId: nextUserId,
    concertId: nextConcertId,
    status: normalized.status || state.queueState.status,
    rank: nextRank,
    activeTtlSeconds: nextTtl,
    lastEvent: eventName
  });

  appendLog(`QUEUE_SSE_${eventName}`, normalized);
}

function closeQueueSubscription(reason = "MANUAL_CLOSE", options = {}) {
  if (state.queueEventSource) {
    state.queueEventSource.close();
    state.queueEventSource = null;
  }

  setQueueState({ lastEvent: reason });
  if (!options.silent) {
    appendLog("QUEUE_SSE_CLOSED", { reason });
  }
}

async function onQueueJoin() {
  const userId = requireQueueUserId();
  const concertId = requireQueueConcertId();
  const response = await callQueueApi("/join", {
    method: "POST",
    body: { userId, concertId }
  });

  setQueueState({
    userId,
    concertId,
    status: response.status || "NONE",
    rank: Number(response.rank ?? -1),
    activeTtlSeconds: response.status === "ACTIVE" ? state.queueState.activeTtlSeconds : 0,
    lastEvent: "JOIN"
  });
  appendLog("QUEUE_JOIN", response);
}

async function onQueueStatus() {
  const userId = requireQueueUserId();
  const concertId = requireQueueConcertId();
  const response = await callQueueApi(`/status?userId=${encodeURIComponent(userId)}&concertId=${encodeURIComponent(concertId)}`);

  setQueueState({
    userId,
    concertId,
    status: response.status || "NONE",
    rank: Number(response.rank ?? -1),
    activeTtlSeconds: response.status === "ACTIVE" ? state.queueState.activeTtlSeconds : 0,
    lastEvent: "STATUS"
  });
  appendLog("QUEUE_STATUS", response);
}

function onQueueSubscribe() {
  const userId = requireQueueUserId();
  const concertId = requireQueueConcertId();
  closeQueueSubscription("RECONNECT", { silent: true });

  const url = `${state.apiBase}/api/v1/waiting-queue/subscribe?userId=${encodeURIComponent(userId)}&concertId=${encodeURIComponent(concertId)}`;
  const source = new EventSource(url);
  state.queueEventSource = source;

  setQueueState({
    userId,
    concertId,
    status: state.queueState.status || "NONE",
    rank: state.queueState.rank,
    lastEvent: "SUBSCRIBING"
  });

  appendLog("QUEUE_SSE_CONNECT", { url });

  source.onopen = () => {
    setQueueState({ lastEvent: "OPEN" });
    appendLog("QUEUE_SSE_OPEN", { userId, concertId });
  };

  source.onerror = () => {
    setQueueState({ lastEvent: "ERROR" });
    appendLog("QUEUE_SSE_ERROR", "connection dropped or blocked");
  };

  source.onmessage = (event) => {
    const payload = parseSsePayload(event.data);
    applyQueueEvent("MESSAGE", payload, userId, concertId);
  };

  const names = ["INIT", "RANK_UPDATE", "ACTIVE", "KEEPALIVE"];
  for (const name of names) {
    source.addEventListener(name, (event) => {
      const payload = parseSsePayload(event.data);
      applyQueueEvent(name, payload, userId, concertId);
    });
  }
}

function onQueueUnsubscribe() {
  closeQueueSubscription("MANUAL_CLOSE");
}

function normalizeText(value) {
  return String(value || "").trim().toLowerCase();
}

function clearChildren(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

function createEmptyRow(text) {
  const div = document.createElement("div");
  div.className = "u1-empty";
  div.textContent = text;
  return div;
}

function seatIsAvailable(seat) {
  return String(seat.status || "").toUpperCase() === "AVAILABLE";
}

function sortConcerts(list, sortValue) {
  const next = [...list];
  switch (sortValue) {
    case "title_desc":
      next.sort((a, b) => String(b.title || "").localeCompare(String(a.title || "")));
      break;
    case "artist_asc":
      next.sort((a, b) => String(a.artistName || "").localeCompare(String(b.artistName || "")));
      break;
    case "artist_desc":
      next.sort((a, b) => String(b.artistName || "").localeCompare(String(a.artistName || "")));
      break;
    case "id_desc":
      next.sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
      break;
    case "id_asc":
      next.sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
      break;
    case "title_asc":
    default:
      next.sort((a, b) => String(a.title || "").localeCompare(String(b.title || "")));
      break;
  }
  return next;
}

function formatDateTime(value) {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleString();
}

function renderExplorerSummary() {
  const allConcerts = state.concerts.length;
  const filteredConcerts = state.filteredConcerts.length;
  const visibleSeats = els.seatAvailableOnlyCheck.checked
    ? state.seats.filter(seatIsAvailable).length
    : state.seats.length;

  const summary = [
    `concerts=${filteredConcerts}/${allConcerts}`,
    `options=${state.options.length}`,
    `seats=${visibleSeats}/${state.seats.length}`,
    `selected(concert=${state.selectedConcertId || "-"}, option=${state.selectedOptionId || "-"}, seat=${state.selectedSeatId || "-"})`
  ];

  els.concertResultSummary.textContent = summary.join(" | ");
}

function populateArtistFilter() {
  const prevValue = els.concertArtistFilter.value || "all";
  const artistSet = new Set();

  for (const concert of state.concerts) {
    const artistName = String(concert.artistName || "").trim();
    if (artistName) {
      artistSet.add(artistName);
    }
  }

  const artists = [...artistSet].sort((a, b) => a.localeCompare(b));

  clearChildren(els.concertArtistFilter);
  const allOption = document.createElement("option");
  allOption.value = "all";
  allOption.textContent = "All Artists";
  els.concertArtistFilter.appendChild(allOption);

  for (const artist of artists) {
    const option = document.createElement("option");
    option.value = artist;
    option.textContent = artist;
    els.concertArtistFilter.appendChild(option);
  }

  if (artists.includes(prevValue)) {
    els.concertArtistFilter.value = prevValue;
  } else {
    els.concertArtistFilter.value = "all";
  }
}

function applyConcertFilters() {
  const query = normalizeText(els.concertSearchInput.value);
  const artistFilter = els.concertArtistFilter.value || "all";
  const sortValue = els.concertSortSelect.value || "title_asc";

  const filtered = state.concerts.filter((concert) => {
    if (artistFilter !== "all" && String(concert.artistName || "") !== artistFilter) {
      return false;
    }

    if (!query) {
      return true;
    }

    const idText = String(concert.id || "");
    const titleText = normalizeText(concert.title);
    const artistText = normalizeText(concert.artistName);

    return idText.includes(query) || titleText.includes(query) || artistText.includes(query);
  });

  state.filteredConcerts = sortConcerts(filtered, sortValue);

  if (!state.filteredConcerts.some((concert) => concert.id === state.selectedConcertId)) {
    state.selectedConcertId = null;
    state.selectedOptionId = null;
    state.selectedSeatId = null;
    state.options = [];
    state.seats = [];
  }

  renderConcertList();
  renderOptionList();
  renderSeatList();
  renderExplorerSummary();
}

function renderConcertList() {
  clearChildren(els.concertList);

  if (state.concerts.length === 0) {
    els.concertList.appendChild(createEmptyRow("No concert data loaded yet."));
    return;
  }

  if (state.filteredConcerts.length === 0) {
    els.concertList.appendChild(createEmptyRow("No concerts matched current search/filter."));
    return;
  }

  for (const concert of state.filteredConcerts) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "u1-item-btn";
    if (concert.id === state.selectedConcertId) {
      button.classList.add("u1-item-selected");
    }

    const title = document.createElement("div");
    title.className = "u1-item-title";
    title.textContent = concert.title || "(untitled)";

    const meta = document.createElement("div");
    meta.className = "u1-item-meta";
    meta.textContent = `#${concert.id || "-"} · ${concert.artistName || "-"}`;

    button.appendChild(title);
    button.appendChild(meta);
    button.addEventListener("click", () => runAction("CONCERT_SELECT", () => onSelectConcert(concert.id)));

    els.concertList.appendChild(button);
  }
}

function renderOptionList() {
  clearChildren(els.optionList);

  if (!state.selectedConcertId) {
    els.optionList.appendChild(createEmptyRow("Select a concert to load options."));
    return;
  }

  if (state.options.length === 0) {
    els.optionList.appendChild(createEmptyRow("No options returned for this concert."));
    return;
  }

  for (const option of state.options) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "u1-item-btn";
    if (option.id === state.selectedOptionId) {
      button.classList.add("u1-item-selected");
    }

    const title = document.createElement("div");
    title.className = "u1-item-title";
    title.textContent = formatDateTime(option.concertDate);

    const meta = document.createElement("div");
    meta.className = "u1-item-meta";
    meta.textContent = `optionId=${option.id || "-"}`;

    button.appendChild(title);
    button.appendChild(meta);
    button.addEventListener("click", () => runAction("OPTION_SELECT", () => onSelectOption(option.id)));

    els.optionList.appendChild(button);
  }
}

function renderSeatList() {
  clearChildren(els.seatList);

  if (!state.selectedOptionId) {
    els.seatList.appendChild(createEmptyRow("Select an option to load seats."));
    return;
  }

  if (state.seats.length === 0) {
    els.seatList.appendChild(createEmptyRow("No seats returned for this option."));
    return;
  }

  const availableOnly = els.seatAvailableOnlyCheck.checked;
  const visibleSeats = availableOnly ? state.seats.filter(seatIsAvailable) : state.seats;

  if (visibleSeats.length === 0) {
    els.seatList.appendChild(createEmptyRow("No AVAILABLE seats under current filter."));
    return;
  }

  for (const seat of visibleSeats) {
    const button = document.createElement("button");
    const available = seatIsAvailable(seat);

    button.type = "button";
    button.className = "u1-item-btn";
    if (available) {
      button.classList.add("u1-seat-available");
    } else {
      button.classList.add("u1-seat-unavailable");
      button.disabled = true;
    }

    if (seat.id === state.selectedSeatId) {
      button.classList.add("u1-item-selected", "u1-seat-selected");
    }

    const title = document.createElement("div");
    title.className = "u1-item-title";
    title.textContent = `${seat.seatNumber || "Seat"} (#${seat.id || "-"})`;

    const meta = document.createElement("div");
    meta.className = "u1-item-meta";
    meta.textContent = `status=${seat.status || "-"}`;

    button.appendChild(title);
    button.appendChild(meta);

    if (available) {
      button.addEventListener("click", () => {
        runAction("SEAT_SELECT", () => {
          onSelectSeat(seat);
        });
      });
    }

    els.seatList.appendChild(button);
  }
}

async function onRefreshConcerts() {
  const response = await callApi("/api/concerts");
  const concerts = Array.isArray(response) ? response : [];

  state.concerts = concerts;

  const selectedConcertExists = state.concerts.some((concert) => concert.id === state.selectedConcertId);
  if (!selectedConcertExists) {
    state.selectedConcertId = null;
    state.selectedOptionId = null;
    state.selectedSeatId = null;
    state.options = [];
    state.seats = [];
  }

  populateArtistFilter();
  applyConcertFilters();
  syncQueueConcertIdInput();

  appendLog("CONCERTS_LOADED", { count: state.concerts.length });

  if (state.selectedConcertId) {
    await loadConcertOptions(state.selectedConcertId, { autoSelectFirst: false });
  }
}

async function onSelectConcert(concertId) {
  if (!concertId) {
    throw new Error("concertId is required");
  }

  state.selectedConcertId = concertId;
  state.selectedOptionId = null;
  state.selectedSeatId = null;
  state.options = [];
  state.seats = [];
  syncQueueConcertIdInput({ force: true });

  renderConcertList();
  renderOptionList();
  renderSeatList();
  renderExplorerSummary();

  await loadConcertOptions(concertId, { autoSelectFirst: true });
}

async function loadConcertOptions(concertId, options = {}) {
  const response = await callApi(`/api/concerts/${concertId}/options`);
  const nextOptions = Array.isArray(response) ? response : [];

  nextOptions.sort((a, b) => String(a.concertDate || "").localeCompare(String(b.concertDate || "")));
  state.options = nextOptions;

  appendLog("CONCERT_OPTIONS_LOADED", { concertId, count: state.options.length });

  if (state.options.length === 0) {
    state.selectedOptionId = null;
    state.selectedSeatId = null;
    state.seats = [];
    renderOptionList();
    renderSeatList();
    renderExplorerSummary();
    return;
  }

  if (options.autoSelectFirst) {
    await onSelectOption(state.options[0].id, { skipLog: true });
    return;
  }

  const selectedStillExists = state.options.some((option) => option.id === state.selectedOptionId);
  if (!selectedStillExists) {
    state.selectedOptionId = state.options[0].id;
  }

  renderOptionList();
  await loadOptionSeats(state.selectedOptionId, { skipLog: true });
}

async function onSelectOption(optionId, options = {}) {
  if (!optionId) {
    throw new Error("optionId is required");
  }

  state.selectedOptionId = optionId;
  state.selectedSeatId = null;
  state.seats = [];

  renderOptionList();
  renderSeatList();
  renderExplorerSummary();

  await loadOptionSeats(optionId, options);
}

async function loadOptionSeats(optionId, options = {}) {
  const response = await callApi(`/api/concerts/options/${optionId}/seats`);
  const seats = Array.isArray(response) ? response : [];

  seats.sort((a, b) => String(a.seatNumber || "").localeCompare(String(b.seatNumber || "")));
  state.seats = seats;

  const selectedStillExists = state.seats.some((seat) => seat.id === state.selectedSeatId);
  if (!selectedStillExists) {
    state.selectedSeatId = null;
  }

  renderSeatList();
  renderExplorerSummary();

  if (!options.skipLog) {
    appendLog("SEATS_LOADED", { optionId, count: state.seats.length });
  }
}

function onSelectSeat(seat) {
  if (!seat || !seat.id) {
    throw new Error("seat id is required");
  }
  if (!seatIsAvailable(seat)) {
    throw new Error(`seat ${seat.id} is not AVAILABLE`);
  }

  state.selectedSeatId = seat.id;
  els.seatIdInput.value = String(seat.id);

  renderSeatList();
  renderExplorerSummary();

  appendLog("SEAT_SELECTED", {
    seatId: seat.id,
    seatNumber: seat.seatNumber,
    status: seat.status
  });
}

async function runAction(name, fn, options = {}) {
  if (!options.silentStatus) {
    setActionStatus(`${name} running...`, "info");
  }

  try {
    await fn();
    if (!options.silentStatus) {
      setActionStatus(`${name} OK`, "ok");
    }
  } catch (error) {
    const message = String(error.message || error);
    appendLog(`${name}_ERROR`, message);
    if (!options.silentStatus) {
      setActionStatus(`${name} ERROR: ${truncateMessage(message, 160)}`, "error");
    }
  }
}

function parseHashParams() {
  if (!window.location.hash) {
    return null;
  }
  return new URLSearchParams(window.location.hash.slice(1));
}

function handleCallbackHash() {
  const params = parseHashParams();
  if (!params) {
    return;
  }

  const login = params.get("login");
  if (login === "success") {
    const provider = params.get("provider") || "-";
    const userId = params.get("userId") || "-";
    appendLog("CALLBACK_RESULT", {
      provider,
      userId,
      summary: params.get("summary") || ""
    });
    setActionStatus(`LOGIN SUCCESS (${provider}) userId=${userId}`, "ok");
    runAction("AUTH_ME_AFTER_CALLBACK", onMe);
  } else {
    appendLog("CALLBACK_RESULT", window.location.hash.slice(1));
    setActionStatus(`LOGIN CALLBACK RESULT: ${window.location.hash.slice(1)}`, "info");
  }

  history.replaceState(null, "", window.location.pathname);
}

function bindEvents() {
  els.useCurrentOriginBtn.addEventListener("click", () => {
    runAction("API_BASE_SET", async () => {
      setApiBase(window.location.origin);
      closeQueueSubscription("API_BASE_CHANGED", { silent: true });
      await onRefreshConcerts();
    });
  });

  els.apiBaseInput.addEventListener("change", () => {
    runAction("API_BASE_INPUT", async () => {
      setApiBase(els.apiBaseInput.value);
      closeQueueSubscription("API_BASE_CHANGED", { silent: true });
      await onRefreshConcerts();
    });
  });

  els.clearSessionBtn.addEventListener("click", () => {
    clearSession();
    appendLog("SESSION_CLEARED", "local storage session keys removed");
    setActionStatus("SESSION_CLEARED OK", "ok");
  });

  els.loginKakaoBtn.addEventListener("click", () => runAction("KAKAO_LOGIN", () => startSocialLogin("kakao")));
  els.loginNaverBtn.addEventListener("click", () => runAction("NAVER_LOGIN", () => startSocialLogin("naver")));

  els.meBtn.addEventListener("click", () => runAction("AUTH_ME", onMe));
  els.refreshBtn.addEventListener("click", () => runAction("AUTH_REFRESH", onRefresh));
  els.logoutBtn.addEventListener("click", () => runAction("AUTH_LOGOUT", onLogout));

  els.refreshConcertsBtn.addEventListener("click", () => runAction("CONCERTS_REFRESH", onRefreshConcerts));
  els.concertSearchInput.addEventListener("input", applyConcertFilters);
  els.concertArtistFilter.addEventListener("change", applyConcertFilters);
  els.concertSortSelect.addEventListener("change", applyConcertFilters);
  els.seatAvailableOnlyCheck.addEventListener("change", () => {
    renderSeatList();
    renderExplorerSummary();
  });

  els.queueJoinBtn.addEventListener("click", () => runAction("QUEUE_JOIN", onQueueJoin));
  els.queueStatusBtn.addEventListener("click", () => runAction("QUEUE_STATUS", onQueueStatus));
  els.queueSubscribeBtn.addEventListener("click", () => runAction("QUEUE_SSE_SUBSCRIBE", onQueueSubscribe));
  els.queueUnsubscribeBtn.addEventListener("click", () => runAction("QUEUE_SSE_UNSUBSCRIBE", onQueueUnsubscribe));

  els.createHoldBtn.addEventListener("click", () => runAction("RES_HOLD", onCreateHold));
  els.startPayingBtn.addEventListener("click", () => runAction("RES_PAYING", () => transitionReservation("paying")));
  els.confirmBtn.addEventListener("click", () => runAction("RES_CONFIRM", () => transitionReservation("confirm")));
  els.cancelBtn.addEventListener("click", () => runAction("RES_CANCEL", () => transitionReservation("cancel")));
  els.refundBtn.addEventListener("click", () => runAction("RES_REFUND", () => transitionReservation("refund")));
  els.getReservationBtn.addEventListener("click", () => runAction("RES_GET", onGetReservation));
  els.getMyReservationsBtn.addEventListener("click", () => runAction("RES_ME", onGetMyReservations));

  window.addEventListener("beforeunload", () => {
    closeQueueSubscription("WINDOW_UNLOAD", { silent: true });
  });
}

function init() {
  setApiBase(state.apiBase, { silent: true });
  setActionStatus("Ready", "info");
  renderAuthSummary();
  syncQueueUserIdInput();
  syncQueueConcertIdInput();
  renderConcertList();
  renderOptionList();
  renderSeatList();
  renderExplorerSummary();
  renderQueueState();
  bindEvents();
  handleCallbackHash();
  runAction("AUTH_BOOTSTRAP", bootstrapAuthSession, { silentStatus: true });
  runAction("CONCERTS_BOOTSTRAP", onRefreshConcerts, { silentStatus: true });
}

init();
