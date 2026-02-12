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

function apiBase() {
  const stored = localStorage.getItem(STORAGE_KEYS.apiBase);
  if (stored && stored.trim() !== "") {
    return stored.trim().replace(/\/+$/, "");
  }
  return window.location.origin;
}

function log(tag, payload) {
  const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
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
  const parts = stateValue.split("|");
  if (parts.length >= 2 && parts[0] === "u1") {
    return parts[1];
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

async function callExchange(provider, code, stateValue) {
  const response = await fetch(`${apiBase()}/api/auth/social/${provider}/exchange`, {
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

function validateState(returnedState) {
  const expectedState = localStorage.getItem(STORAGE_KEYS.oauthState);
  if (!expectedState) {
    log("OAUTH_STATE_WARN", "missing expected oauth state in localStorage");
    return;
  }
  if (!returnedState) {
    throw new Error("missing oauth state from callback");
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

  const provider = parseProvider(stateValue) || localStorage.getItem(STORAGE_KEYS.oauthProvider) || "kakao";
  localStorage.setItem(STORAGE_KEYS.oauthProvider, provider);

  try {
    validateState(stateValue);

    setMessage(`Exchanging authorization code for provider=${provider}...`);
    const loginResponse = await callExchange(provider, code, stateValue);

    localStorage.setItem(STORAGE_KEYS.accessToken, loginResponse.accessToken || "");
    localStorage.setItem(STORAGE_KEYS.refreshToken, loginResponse.refreshToken || "");

    const snapshot = buildUserSnapshot(loginResponse, provider);
    localStorage.setItem(STORAGE_KEYS.authUser, JSON.stringify(snapshot));
    localStorage.removeItem(STORAGE_KEYS.oauthState);

    log("SOCIAL_EXCHANGE_OK", loginResponse);
    setMessage("Exchange successful. Redirecting back to console...");

    const summary = encodeURIComponent(`provider=${provider}&userId=${loginResponse.userId || "-"}`);
    window.setTimeout(() => {
      window.location.replace(`./index.html#login=success&provider=${encodeURIComponent(provider)}&userId=${encodeURIComponent(loginResponse.userId || "-")}&summary=${summary}`);
    }, 400);
  } catch (errorObj) {
    const message = String(errorObj.message || errorObj);
    setMessage("Exchange failed. Check the details below.");
    log("SOCIAL_EXCHANGE_ERROR", message);
  }
}

run();
