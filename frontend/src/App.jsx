import { useEffect, useState } from "react";
import {
  Building2,
  LockKeyhole,
  LogOut,
} from "lucide-react";
import {
  GoogleAuthProvider,
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
} from "firebase/auth";
import { apiFetch, setAuthTokenProvider } from "./api.js";
import { firebaseAuth } from "./firebase.js";
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

    setAuthTokenProvider(async () => firebaseAuth.currentUser?.getIdToken() ?? null);

    const unsubscribe = onAuthStateChanged(firebaseAuth, async (firebaseUser) => {
      if (ignore) return;

      if (!firebaseUser) {
        setCurrentUser(null);
        setBooting(false);
        return;
      }

      try {
        const user = await apiFetch("/api/me");
        if (!ignore) {
          setCurrentUser(user);
        }
      } catch (error) {
        if (!ignore && ![401, 403].includes(error.status)) {
          setAuthError(error.message);
        }
        if (!ignore) {
          setCurrentUser(null);
        }
      } finally {
        if (!ignore) {
          setBooting(false);
        }
      }
    });

    return () => {
      ignore = true;
      unsubscribe();
    };
  }, []);

  async function handleLogin(credentials) {
    setAuthError("");
    setNotice("");

    try {
      await signInWithEmailAndPassword(firebaseAuth, credentials.email, credentials.password);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function handleGoogleLogin() {
    setAuthError("");
    setNotice("");

    try {
      const provider = new GoogleAuthProvider();
      await signInWithPopup(firebaseAuth, provider);
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function handleRegister(credentials) {
    setAuthError("");
    setNotice("");

    try {
      await createUserWithEmailAndPassword(firebaseAuth, credentials.email, credentials.password);
      setNotice("Account creato con ruolo customer.");
    } catch (error) {
      setAuthError(error.message);
    }
  }

  async function handleLogout() {
    setNotice("");
    setAuthError("");

    try {
      await signOut(firebaseAuth);
      setCurrentUser(null);
    } catch (error) {
      setAuthError(error.message);
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
            onGoogleLogin={handleGoogleLogin}
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
