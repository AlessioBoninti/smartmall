import { useEffect, useState } from "react";
import {
  Building2,
  LockKeyhole,
  LogOut,
} from "lucide-react";
import { apiFetch, ensureCsrfToken } from "./api.js";
import { RoleBadge, LoadingScreen } from "./components/Common.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import CustomerPage from "./pages/CustomerPage.jsx";
import MerchantPage from "./pages/MerchantPage.jsx";
import AdminPage from "./pages/AdminPage.jsx";

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [booting, setBooting] = useState(true);
  const [authError, setAuthError] = useState("");
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let ignore = false;

    apiFetch("/api/me")
      .then((user) => {
        if (!ignore) {
          setCurrentUser(user);
        }
      })
      .catch((error) => {
        if (ignore) return;
        setCurrentUser(null);
        if (![401, 403].includes(error.status)) {
          setAuthError(error.message);
        }
      })
      .finally(() => {
        if (!ignore) {
          setBooting(false);
        }
      });

    return () => {
      ignore = true;
    };
  }, []);

  async function handleLogin(credentials) {
    setAuthError("");
    setNotice("");

    try {
      const user = await loginAndLoadUser(credentials);
      setCurrentUser(user);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function handleRegister(credentials) {
    setAuthError("");
    setNotice("");

    try {
      await apiFetch("/api/auth/register", {
        method: "POST",
        body: credentials,
      });
      const user = await loginAndLoadUser(credentials);
      setCurrentUser(user);
      setNotice("Account creato con ruolo customer.");
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function loginAndLoadUser(credentials) {
    await ensureCsrfToken();
    await apiFetch("/api/auth/login", {
      method: "POST",
      body: credentials,
    });
    await ensureCsrfToken(true);
    return apiFetch("/api/me");
  }

  async function handleLogout() {
    setNotice("");
    setAuthError("");

    try {
      await ensureCsrfToken();
      await apiFetch("/api/auth/logout", { method: "POST" });
    } catch (error) {
      if (![401, 403].includes(error.status)) {
        setAuthError(error.message);
      }
    } finally {
      setCurrentUser(null);
      try {
        await ensureCsrfToken(true);
      } catch {
        // The next mutating request will request a fresh CSRF token again.
      }
    }
  }

  return (
    <div className="app-shell">
      <TopBar user={currentUser} onLogout={handleLogout} />

      <main className="app-main">
        {booting ? (
          <LoadingScreen />
        ) : currentUser ? (
          <AuthenticatedApp user={currentUser} />
        ) : (
          <LoginPage
            onLogin={handleLogin}
            onRegister={handleRegister}
            authError={authError}
            notice={notice}
          />
        )}
      </main>
    </div>
  );
}

function AuthenticatedApp({ user }) {
  if (user.role === "SUPER_ADMIN") {
    return <AdminPage user={user} />;
  }

  if (user.role === "MERCHANT") {
    return <MerchantPage />;
  }

  return <CustomerPage />;
}

function TopBar({ user, onLogout }) {
  return (
    <header className="topbar">
      <div className="brand">
        <span className="brand-mark">
          <Building2 size={22} />
        </span>
        <div>
          <strong>SmartMall</strong>
          <span>Prenotazioni negozi</span>
        </div>
      </div>

      <div className="topbar-actions">
        {user ? (
          <>
            <div className="user-pill">
              <span>{user.email}</span>
              <RoleBadge role={user.role} />
            </div>
            <button className="button ghost" type="button" onClick={onLogout}>
              <LogOut size={18} />
              Esci
            </button>
          </>
        ) : (
          <div className="guest-pill">
            <LockKeyhole size={17} />
            Visitatore
          </div>
        )}
      </div>
    </header>
  );
}

export default App;
