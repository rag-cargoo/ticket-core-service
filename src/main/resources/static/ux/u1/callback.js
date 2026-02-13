const STORAGE_KEYS = {
  apiBase: "ticketrush_u1_api_base",
  accessToken: "ticketrush_u1_access_token",
  refreshToken: "ticketrush_u1_refresh_token",
  oauthProvider: "ticketrush_u1_oauth_provider",
  oauthState: "ticketrush_u1_oauth_state",
  authUser: "ticketrush_u1_auth_user"
};

const messageEl = document.getElementById("callbackMessage");
const consoleEl = document.getElementById("callbackConsole");
const REQUEST_TIMEOUT_MS = 8000;

function apiBase() {
  const stored = localStorage.getItem(STORAGE_KEYS.apiBase);
  if (stored && stored.trim() !== "") {
    return stored.trim().replace(/\/+$/, "");
  }
  return window.location.origin;
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

function log(tag, payload) {
  const safePayload = redactTokenFields(payload);
  const body = typeof safePayload === "string" ? safePayload : JSON.stringify(safePayload, null, 2);
  const prev = consoleEl.textContent || "";
  consoleEl.textContent = `[${new Date().toISOString()}] ${tag}\n${body}\n\n${prev}`;
}

function setMessage(text) {
  messageEl.textContent = text;
}

function parseProvider(stateValue) {
  if (!stateValue) {
    return null;
  }
  // Preferred: u1_<provider>_<ts>_<nonce> (URL-safe)
  const safeParts = stateValue.split("_");
  if (safeParts.length >= 2 && safeParts[0] === "u1") {
    return safeParts[1];
  }

  // Legacy fallback: u1|<provider>|<ts>|<nonce>
  const legacyParts = stateValue.split("|");
  if (legacyParts.length >= 2 && legacyParts[0] === "u1") {
    return legacyParts[1];
  }
  return null;
}

function parseJsonResponse(text) {
  try {
    return text ? JSON.parse(text) : {};
  } catch (_) {
    return text;
  }
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

function clearStoredSession() {
  localStorage.removeItem(STORAGE_KEYS.accessToken);
  localStorage.removeItem(STORAGE_KEYS.refreshToken);
  localStorage.removeItem(STORAGE_KEYS.authUser);
}

async function callExchange(provider, code, stateValue) {
  const response = await fetchWithTimeout(`${apiBase()}/api/auth/social/${provider}/exchange`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      code,
      state: stateValue || null
    })
  });

  const text = await response.text();
  const parsed = parseJsonResponse(text);

  if (!response.ok) {
    throw new Error(`${response.status} ${JSON.stringify(parsed, null, 2)}`);
  }

  return parsed;
}

async function callAuthMe(accessToken) {
  const response = await fetchWithTimeout(`${apiBase()}/api/auth/me`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });

  const text = await response.text();
  const parsed = parseJsonResponse(text);
  if (!response.ok) {
    throw new Error(`auth/me ${response.status} ${JSON.stringify(parsed, null, 2)}`);
  }
  return parsed;
}

function buildUserSnapshot(loginResponse, provider) {
  return {
    userId: loginResponse.userId || null,
    username: loginResponse.username || null,
    provider,
    socialId: loginResponse.socialId || null,
    email: loginResponse.email || null,
    displayName: loginResponse.displayName || null,
    role: loginResponse.role || null
  };
}

function isLikelyU1State(stateValue) {
  return /^u1_[a-z]+_[0-9]{10,}_[a-z0-9]+$/i.test(String(stateValue || ""));
}

function validateState(returnedState) {
  if (!returnedState) {
    throw new Error("missing oauth state from callback");
  }

  const expectedState = localStorage.getItem(STORAGE_KEYS.oauthState);
  if (!expectedState) {
    if (!isLikelyU1State(returnedState)) {
      throw new Error("missing expected oauth state in localStorage and callback state format is invalid");
    }
    log("OAUTH_STATE_WARN", {
      reason: "missing expected oauth state in localStorage; continuing with callback state",
      returnedState
    });
    return;
  }
  if (returnedState !== expectedState) {
    throw new Error("oauth state mismatch");
  }
}

async function run() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get("code");
  const stateValue = params.get("state");
  const error = params.get("error");
  const errorDescription = params.get("error_description");

  if (error) {
    setMessage("Provider returned an error.");
    log("PROVIDER_ERROR", { error, errorDescription });
    return;
  }

  if (!code) {
    setMessage("Missing authorization code.");
    log("CALLBACK_ERROR", { message: "code is required" });
    return;
  }

  const provider =
    parseProvider(stateValue) ||
    params.get("provider") ||
    localStorage.getItem(STORAGE_KEYS.oauthProvider) ||
    "kakao";
  localStorage.setItem(STORAGE_KEYS.oauthProvider, provider);

  try {
    validateState(stateValue);

    setMessage(`Exchanging authorization code for provider=${provider}...`);
    const loginResponse = await callExchange(provider, code, stateValue);

    if (!loginResponse.accessToken || !loginResponse.refreshToken) {
      throw new Error("token pair is missing in exchange response");
    }

    localStorage.setItem(STORAGE_KEYS.accessToken, loginResponse.accessToken);
    localStorage.setItem(STORAGE_KEYS.refreshToken, loginResponse.refreshToken);

    const me = await callAuthMe(loginResponse.accessToken);
    const snapshot = {
      ...buildUserSnapshot(loginResponse, provider),
      ...me
    };
    localStorage.setItem(STORAGE_KEYS.authUser, JSON.stringify(snapshot));
    localStorage.removeItem(STORAGE_KEYS.oauthState);

    log("SOCIAL_EXCHANGE_OK", { exchange: loginResponse, me });
    setMessage("Exchange successful. Redirecting back to console...");

    const summary = encodeURIComponent(`provider=${provider}&userId=${loginResponse.userId || "-"}`);
    window.setTimeout(() => {
      window.location.replace(`./index.html#login=success&provider=${encodeURIComponent(provider)}&userId=${encodeURIComponent(loginResponse.userId || "-")}&summary=${summary}`);
    }, 400);
  } catch (errorObj) {
    const message = String(errorObj.message || errorObj);
    clearStoredSession();
    setMessage("Exchange failed. Check the details below.");
    log("SOCIAL_EXCHANGE_ERROR", message);
  }
}

run();
