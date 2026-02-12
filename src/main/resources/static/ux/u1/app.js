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
  clearSessionBtn: document.getElementById("clearSessionBtn"),
  currentUserView: document.getElementById("currentUserView"),
  loginKakaoBtn: document.getElementById("loginKakaoBtn"),
  loginNaverBtn: document.getElementById("loginNaverBtn"),
  meBtn: document.getElementById("meBtn"),
  refreshBtn: document.getElementById("refreshBtn"),
  logoutBtn: document.getElementById("logoutBtn"),
  accessTokenView: document.getElementById("accessTokenView"),
  refreshTokenView: document.getElementById("refreshTokenView"),
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
  currentUser: loadAuthUser()
};

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

function appendLog(tag, payload) {
  const timestamp = new Date().toISOString();
  const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
  const prev = els.consoleView.textContent || "";
  const next = `[${timestamp}] ${tag}\n${body}\n\n${prev}`;
  els.consoleView.textContent = next;
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

function describeToken(token) {
  if (!token) {
    return "-";
  }
  if (token.length <= 36) {
    return token;
  }
  return `${token.slice(0, 24)}...${token.slice(-8)} (len=${token.length})`;
}

function renderAuthSummary() {
  els.accessTokenView.textContent = describeToken(state.accessToken);
  els.refreshTokenView.textContent = describeToken(state.refreshToken);

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
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
  localStorage.removeItem(STORAGE_KEYS.authUser);
  localStorage.removeItem(STORAGE_KEYS.oauthState);
  state.accessToken = "";
  state.refreshToken = "";
  state.currentUser = null;
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

  const response = await fetch(url, {
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
  return `u1|${provider}|${ts}|${nonce}`;
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

async function runAction(name, fn) {
  try {
    await fn();
  } catch (error) {
    appendLog(`${name}_ERROR`, String(error.message || error));
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
    appendLog("CALLBACK_RESULT", {
      provider: params.get("provider"),
      userId: params.get("userId"),
      summary: params.get("summary") || ""
    });
    runAction("AUTH_ME_AFTER_CALLBACK", onMe);
  } else {
    appendLog("CALLBACK_RESULT", window.location.hash.slice(1));
  }

  history.replaceState(null, "", window.location.pathname);
}

function bindEvents() {
  els.useCurrentOriginBtn.addEventListener("click", () => {
    runAction("API_BASE_SET", () => {
      setApiBase(window.location.origin);
    });
  });

  els.apiBaseInput.addEventListener("change", () => {
    runAction("API_BASE_INPUT", () => {
      setApiBase(els.apiBaseInput.value);
    });
  });

  els.clearSessionBtn.addEventListener("click", () => {
    clearSession();
    appendLog("SESSION_CLEARED", "local storage session keys removed");
  });

  els.loginKakaoBtn.addEventListener("click", () => runAction("KAKAO_LOGIN", () => startSocialLogin("kakao")));
  els.loginNaverBtn.addEventListener("click", () => runAction("NAVER_LOGIN", () => startSocialLogin("naver")));

  els.meBtn.addEventListener("click", () => runAction("AUTH_ME", onMe));
  els.refreshBtn.addEventListener("click", () => runAction("AUTH_REFRESH", onRefresh));
  els.logoutBtn.addEventListener("click", () => runAction("AUTH_LOGOUT", onLogout));

  els.createHoldBtn.addEventListener("click", () => runAction("RES_HOLD", onCreateHold));
  els.startPayingBtn.addEventListener("click", () => runAction("RES_PAYING", () => transitionReservation("paying")));
  els.confirmBtn.addEventListener("click", () => runAction("RES_CONFIRM", () => transitionReservation("confirm")));
  els.cancelBtn.addEventListener("click", () => runAction("RES_CANCEL", () => transitionReservation("cancel")));
  els.refundBtn.addEventListener("click", () => runAction("RES_REFUND", () => transitionReservation("refund")));
  els.getReservationBtn.addEventListener("click", () => runAction("RES_GET", onGetReservation));
  els.getMyReservationsBtn.addEventListener("click", () => runAction("RES_ME", onGetMyReservations));
}

function init() {
  setApiBase(state.apiBase, { silent: true });
  renderAuthSummary();
  bindEvents();
  handleCallbackHash();
}

init();
