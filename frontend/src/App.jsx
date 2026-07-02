import React, { lazy, Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, theme, Spin } from 'antd';
import useAuthStore from './store/authStore';
import useUiStore from './store/uiStore';
import Layout from './components/common/Layout';

const Login = lazy(() => import('./pages/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Invoices = lazy(() => import('./pages/Invoices'));
const InvoiceDetails = lazy(() => import('./pages/InvoiceDetails'));
const Gstr2b = lazy(() => import('./pages/Gstr2b'));
const Reconciliation = lazy(() => import('./pages/Reconciliation'));
const ReturnDraft = lazy(() => import('./pages/ReturnDraft'));
const Deadlines = lazy(() => import('./pages/Deadlines'));
const Processing = lazy(() => import('./pages/Processing'));
const Analytics = lazy(() => import('./pages/Analytics'));
const Insights = lazy(() => import('./pages/Insights'));
const Profile = lazy(() => import('./pages/Profile'));
const Settings = lazy(() => import('./pages/Settings'));
const NotFound = lazy(() => import('./pages/NotFound'));

const PageLoader = () => (
  <div className="flex flex-col gap-4 justify-center items-center h-screen"
    style={{ background: 'var(--bg-canvas)' }}>
    <Spin size="large" />
    <span style={{ color: 'var(--text-muted)', fontSize: 13 }}>
      Initializing AI Compliance Engine…
    </span>
  </div>
);

class ErrorBoundary extends React.Component {
  state = { hasError: false };
  static getDerivedStateFromError() { return { hasError: true }; }
  render() {
    if (this.state.hasError) return (
      <div className="p-10 h-screen flex flex-col justify-center items-center"
        style={{ background: 'var(--bg-canvas)', color: 'var(--text-primary)' }}>
        <h2 className="mb-4">Something went wrong.</h2>
        <button onClick={() => window.location.reload()}
          style={{ background: 'var(--accent)', color: '#fff', padding: '10px 24px', borderRadius: 8, border: 'none', cursor: 'pointer' }}>
          Reload
        </button>
      </div>
    );
    return this.props.children;
  }
}

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 60000, gcTime: 300000 },
  },
});

const ProtectedRoute = ({ children }) => {
  const token = useAuthStore((s) => s.token);
  return token ? children : <Navigate to="/login" replace />;
};

// ─── Ant Design tokens per theme ────────────────────────────────────────────
const darkTokens = {
  algorithm: theme.darkAlgorithm,
  token: {
    colorPrimary: '#8b5cf6',
    colorLink: '#a78bfa',
    borderRadius: 12,
    colorBgContainer: '#0d0d11',
    colorBgBase: '#050507',
    colorTextBase: '#ffffff',
    colorSuccess: '#10b981',
    colorError: '#ef4444',
    colorWarning: '#f59e0b',
    fontFamily: "'Inter', -apple-system, sans-serif",
  },
  components: {
    Button: { colorPrimary: '#8b5cf6', colorPrimaryHover: '#7c3aed' },
    Menu: { colorItemBgSelected: 'rgba(139,92,246,0.15)', colorItemTextSelected: '#c084fc', colorItemBg: 'transparent' },
    Table: { colorBgContainer: '#0d0d11', headerBg: '#131317', headerColor: '#c084fc' },
    Card: { colorBgContainer: '#0d0d11', colorBorderSecondary: '#1b1b22' },
    Tabs: { colorPrimary: '#8b5cf6', inkBarColor: '#8b5cf6' },
    Select: { colorBgContainer: '#131317' },
    Input: { colorBgContainer: '#131317' },
    Modal: { contentBg: '#0d0d11', headerBg: '#0d0d11' },
  },
};

const lightTokens = {
  algorithm: theme.defaultAlgorithm,
  token: {
    colorPrimary: '#7c3aed',
    colorLink: '#6d28d9',
    borderRadius: 12,
    colorBgContainer: '#ffffff',
    colorBgBase: '#f4f4f6',
    colorTextBase: '#09090b',
    colorSuccess: '#10b981',
    colorError: '#ef4444',
    colorWarning: '#f59e0b',
    fontFamily: "'Inter', -apple-system, sans-serif",
  },
  components: {
    Button: { colorPrimary: '#7c3aed', colorPrimaryHover: '#6d28d9' },
    Menu: { colorItemBgSelected: 'rgba(124,58,237,0.08)', colorItemTextSelected: '#7c3aed', colorItemBg: 'transparent' },
    Table: { colorBgContainer: '#ffffff', headerBg: '#f4f4f5', headerColor: '#7c3aed' },
    Card: { colorBgContainer: '#ffffff', colorBorderSecondary: '#e4e4e7' },
    Tabs: { colorPrimary: '#7c3aed', inkBarColor: '#7c3aed' },
  },
};

// Applies data-theme to <html> so CSS vars pick it up
const ThemeApplicator = () => {
  const t = useUiStore((s) => s.theme);
  useEffect(() => {
    document.documentElement.setAttribute('data-theme', t);
  }, [t]);
  return null;
};

const App = () => {
  const appTheme = useUiStore((s) => s.theme);

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider theme={appTheme === 'dark' ? darkTokens : lightTokens}>
          <ThemeApplicator />
          <BrowserRouter>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                <Route path="/login" element={<Login />} />
                <Route path="/" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="dashboard" element={<Dashboard />} />
                  <Route path="invoices" element={<Invoices />} />
                  <Route path="invoices/:id" element={<InvoiceDetails />} />
                  <Route path="gstr2b" element={<Gstr2b />} />
                  <Route path="reconciliation" element={<Reconciliation />} />
                  <Route path="returns" element={<ReturnDraft />} />
                  <Route path="processing/:jobId" element={<Processing />} />
                  <Route path="deadlines" element={<Deadlines />} />
                  <Route path="analytics" element={<Analytics />} />
                  <Route path="insights" element={<Insights />} />
                  <Route path="profile" element={<Profile />} />
                  <Route path="settings" element={<Settings />} />
                  <Route path="*" element={<NotFound />} />
                </Route>
              </Routes>
            </Suspense>
          </BrowserRouter>
        </ConfigProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

export default App;
