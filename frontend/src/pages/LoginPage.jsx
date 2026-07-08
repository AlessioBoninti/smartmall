import { useState } from "react";
import {
  KeyRound,
  LoaderCircle,
  LogIn,
  Mail,
  UserPlus,
} from "lucide-react";
import { Message, SectionHeader } from "../components/Common.jsx";
import { MarketplacePanel } from "./CustomerPage.jsx";

export default function LoginPage({ onLogin, onRegister, authError, notice }) {
  return (
    <div className="dashboard visitor-dashboard">
      <section className="panel auth-panel">
        <SectionHeader
          icon={<LogIn size={20} />}
          title="Accesso"
          subtitle="Consulta gli slot anche senza account."
        />
        <AuthPanel
          onLogin={onLogin}
          onRegister={onRegister}
          error={authError}
          notice={notice}
        />
      </section>

      <MarketplacePanel role="GUEST" />
    </div>
  );
}

function AuthPanel({ onLogin, onRegister, error, notice }) {
  const [mode, setMode] = useState("login");
  const [email, setEmail] = useState("customer@test.com");
  const [password, setPassword] = useState("password123");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);

    const payload = { email, password };
    try {
      if (mode === "login") {
        await onLogin(payload);
      } else {
        await onRegister(payload);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="auth-form" onSubmit={handleSubmit}>
      <div className="segmented">
        <button
          type="button"
          className={mode === "login" ? "active" : ""}
          onClick={() => setMode("login")}
        >
          <LogIn size={16} />
          Login
        </button>
        <button
          type="button"
          className={mode === "register" ? "active" : ""}
          onClick={() => setMode("register")}
        >
          <UserPlus size={16} />
          Registrati
        </button>
      </div>

      {error && <Message type="error" text={error} />}
      {notice && <Message type="success" text={notice} />}

      <label className="field">
        <span>Email</span>
        <div className="input-with-icon">
          <Mail size={17} />
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="nome@email.it"
            required
          />
        </div>
      </label>

      <label className="field">
        <span>Password</span>
        <div className="input-with-icon">
          <KeyRound size={17} />
          <input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder="Password"
            minLength={mode === "register" ? 6 : undefined}
            required
          />
        </div>
      </label>

      <button className="button primary wide" type="submit" disabled={submitting}>
        {submitting ? <LoaderCircle className="spin" size={18} /> : mode === "login" ? <LogIn size={18} /> : <UserPlus size={18} />}
        {mode === "login" ? "Entra" : "Crea account"}
      </button>
    </form>
  );
}
