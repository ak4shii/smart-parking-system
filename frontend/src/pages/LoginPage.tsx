import React, { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';

import logo from '../assets/logo.png';
import illustration from '../assets/illustration-1.png';
import eyeIcon from '../assets/eye.png';

import { useAuth } from '../context/AuthContext';

const inputBase: React.CSSProperties = {
  width: '100%',
  padding: '12px 44px 12px 14px',
  borderRadius: 12,
  border: '1px solid rgba(255,255,255,0.14)',
  background: 'rgba(255,255,255,0.06)',
  color: '#fff',
  outline: 'none',
};

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const canSubmit = useMemo(() => email.trim() && password.trim(), [email, password]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit || loading) return;

    try {
      setLoading(true);
      await login(email.trim(), password);
      toast.success('Signed in');
      navigate('/', { replace: true });
    } catch (err: any) {
      toast.error(err?.message || 'Failed to sign in');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'radial-gradient(1200px 600px at 70% 40%, rgba(98, 84, 255, 0.35), transparent 55%), linear-gradient(180deg, #0b1020, #070a14 70%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 24,
      }}
    >
      <div
        className="_grid"
        style={{
          width: 'min(1100px, 100%)',
          display: 'grid',
          gridTemplateColumns: '1fr 1fr',
          gap: 36,
          alignItems: 'center',
        }}
      >
        <div style={{ color: '#fff', padding: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18 }}>
            <img src={logo} alt="Smart Parking" style={{ height: 34 }} />
            <span
              style={{
                fontSize: 12,
                letterSpacing: '0.18em',
                fontWeight: 700,
                color: 'rgba(255,255,255,0.9)',
                textTransform: 'uppercase',
              }}
            >
              SMART CAR PARKING SYSTEM
            </span>
          </div>
          <h1 style={{ fontSize: 44, lineHeight: 1.1, margin: 0 }}>Sign In</h1>
          <p style={{ marginTop: 10, color: 'rgba(255,255,255,0.72)', fontSize: 16 }}>
            Enter your credentials to access your account.
          </p>

          <form onSubmit={onSubmit} style={{ marginTop: 26, maxWidth: 420 }}>
            <label style={{ display: 'block', fontSize: 13, color: 'rgba(255,255,255,0.8)', marginBottom: 8 }}>
              Email
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              style={{ ...inputBase, paddingRight: 14 }}
            />

            <div style={{ height: 16 }} />

            <label style={{ display: 'block', fontSize: 13, color: 'rgba(255,255,255,0.8)', marginBottom: 8 }}>
              Password
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                style={inputBase}
              />
              <button
                type="button"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={() => setShowPassword((s) => !s)}
                style={{
                  position: 'absolute',
                  right: 10,
                  top: '50%',
                  transform: 'translateY(-50%)',
                  border: 'none',
                  background: 'transparent',
                  cursor: 'pointer',
                  padding: 6,
                }}
              >
                <img src={eyeIcon} alt="toggle" style={{ width: 18, height: 18, opacity: 0.85 }} />
              </button>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 14 }}>
              <label style={{ display: 'flex', gap: 10, alignItems: 'center', color: 'rgba(255,255,255,0.75)', fontSize: 13 }}>
                <input type="checkbox" style={{ width: 16, height: 16 }} />
                Remember me
              </label>
              <a href="#" style={{ color: 'rgba(255,255,255,0.75)', fontSize: 13, textDecoration: 'none' }}>
                Forgot password?
              </a>
            </div>

            <button
              type="submit"
              disabled={!canSubmit || loading}
              style={{
                width: '100%',
                marginTop: 18,
                padding: '12px 14px',
                borderRadius: 12,
                border: 'none',
                background: loading ? 'rgba(98,84,255,0.55)' : 'linear-gradient(90deg, #6a5cff, #9b7bff)',
                color: '#ffffffff',
                fontWeight: 600,
                cursor: loading ? 'default' : 'pointer',
                opacity: !canSubmit || loading ? 0.85 : 1,
              }}
            >
              {loading ? 'Signing in…' : 'Sign In'}
            </button>

            <p style={{ marginTop: 16, fontSize: 13, color: 'rgba(255,255,255,0.7)' }}>
              Don’t have an account? <Link to="/register" style={{ color: '#b7a6ff' }}>Sign up</Link>
            </p>
          </form>
        </div>

        <div
          style={{
            background: 'linear-gradient(180deg, rgba(255,255,255,0.08), rgba(255,255,255,0.02))',
            border: '1px solid rgba(255,255,255,0.10)',
            borderRadius: 32,
            padding: 26,
            minHeight: 520,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            overflow: 'hidden',
          }}
        >
          <img
            src={illustration}
            alt="Parking illustration"
            style={{
              width: '100%',
              height: '100%',
              objectFit: 'contain',
              maxHeight: 470,
              borderRadius: 24,
            }}
          />
        </div>
      </div>

      <style>
        {`@media (max-width: 900px){
          ._grid{grid-template-columns: 1fr !important;}
        }`}
      </style>
    </div>
  );
}

