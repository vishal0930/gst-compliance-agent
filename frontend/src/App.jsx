import React, { lazy, Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ConfigProvider, theme, Spin } from 'antd';

// Core Layout & Real Authentication State Store
import Layout from './components/common/Layout';
import useAuthStore from './store/authStore'; // ✅ FIXED: Using the real Zustand store

// --- Performance Optimization: Lazy Loading Pages (Aligned to file names) ---
const Login = lazy(() => import('./pages/Login'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Invoices = lazy(() => import('./pages/Invoices'));
const InvoiceDetails = lazy(() => import('./pages/InvoiceDetails')); 
const Gstr2b = lazy(() => import('./pages/Gstr2b'));
const Reconciliation = lazy(() => import('./pages/Reconciliation'));
const ReturnDraft = lazy(() => import('./pages/ReturnDraft')); // ✅ FIXED: Matches ReturnDraft.jsx
const Deadlines = lazy(() => import('./pages/Deadlines'));     // ✅ FIXED: Matches Deadlines.jsx
const Processing = lazy(() => import('./pages/Processing'));   // ✅ FIXED: Matches Processing.jsx
const Analytics = lazy(() => import('./pages/Analytics'));
const Insights = lazy(() => import('./pages/Insights'));
const Profile = lazy(() => import('./pages/Profile'));
const Settings = lazy(() => import('./pages/Settings'));
const NotFound = lazy(() => import('./pages/NotFound'));
// Global App Loader with Branded Copy
const PageLoader = () => (
  <div className="flex flex-col gap-4 justify-center items-center h-screen bg-slate-950">
    <Spin size="large" tip="Loading Compliance Dashboard..." />
    <span className="text-slate-400 text-sm font-sans">Initializing AI Compliance Engines...</span>
  </div>
);

// App Root Level Error Boundary
class ErrorBoundary extends React.Component {
  state = { hasError: false };
  static getDerivedStateFromError() { return { hasError: true }; }
  componentDidCatch(error, errorInfo) { console.error("Root Error Boundary caught runtime crash:", error, errorInfo); }
  render() {
    if (this.state.hasError) {
      return (
        <div className="p-10 text-white bg-slate-950 h-screen flex flex-col justify-center items-center font-sans">
          <h2 className="mb-4">Something went wrong within the platform.</h2>
          <button onClick={() => window.location.reload()} className="bg-amber-400 text-slate-950 font-semibold border-none py-3 px-6 rounded cursor-pointer">Reload System</button>
        </div>
      );
    }
    return this.props.children;
  }
}

// Optimized TanStack Query v5 Client Instance
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 60000, 
      gcTime: 300000, // ✅ FIXED: Changed legacy cacheTime to TanStack Query v5 gcTime
    },
  },
});

// Route Guard Using Centralized Zustand Authentication Tokens
const ProtectedRoute = ({ children }) => {
  const token = useAuthStore((state) => state.token);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return children;
};

const App = () => {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider
          theme={{
            algorithm: theme.darkAlgorithm, 
            token: {
              colorPrimary: '#FACC15',       // Amber Accent
              colorLink: '#FACC15',
              colorLinkHover: '#EAB308',
              borderRadius: 8,
              colorBgContainer: '#1E293B',    // Slate 800 Cards/Tables
              colorBgBase: '#0F172A',         // Slate 900 Canvas
              colorTextBase: '#FFFFFF',
              fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
              
              // ✅ FIXED: Explicit Compliance Mapping Tokens for Status Indicators
              colorSuccess: '#22C55E',       // Clean Green for "Match"
              colorError: '#EF4444',         // Alert Red for "ITC Risk"
              colorWarning: '#F97316',       // Amber/Orange for "Mismatch / Actions Required"
            },
            components: {
              Button: {
                colorPrimary: '#FACC15',
                colorPrimaryHover: '#EAB308',
                colorPrimaryActive: '#CA8A04',
                colorTextLightSolid: '#0F172A',
              },
              Menu: {
                colorItemBgSelected: '#334155', // Slate 700 Selection Background
                colorItemTextSelected: '#FACC15',
                colorItemBg: 'transparent',
              },
              Table: {
                colorBgContainer: '#1E293B',
                headerBg: '#111827',          // Slate 950 Header block
                headerColor: '#FACC15',
              },
              Card: {
                colorBgContainer: '#1E293B',
                colorBorderSecondary: '#334155',
              },
              Tabs: {
                colorPrimary: '#FACC15',
                inkBarColor: '#FACC15',
              },
            },
          }}
        >
          <BrowserRouter>
            <Suspense fallback={<PageLoader />}>
              <Routes>
                {/* Public Route */}
                <Route path="/login" element={<Login />} />

                {/* Protected Enterprise Compliance App Routes */}
                <Route
                  path="/"
                  element = {
                    <ProtectedRoute>
                      <Layout />
                    </ProtectedRoute>
                  }
                >
                  <Route index element={<Navigate to="/dashboard" replace />} />
                  <Route path="dashboard" element={<Dashboard />} />
                  
                  {/* Invoices Group */}
                  <Route path="invoices" element={<Invoices />} />
                  <Route path="invoices/:id" element={<InvoiceDetails />} />
                  
                  {/* GST & Reconciliation Core */}
                  <Route path="gstr2b" element={<Gstr2b />} />
                  <Route path="reconciliation" element={<Reconciliation />} />
                  <Route path="returns" element={<ReturnDraft />} />
                  
                  {/* Operations, Progress & Tracking */}
                  <Route path="processing/:jobId" element={<Processing />} />
                  <Route path="deadlines" element={<Deadlines />} />
                  
                  {/* Data & Intelligence */}
                  <Route path="analytics" element={<Analytics />} />
                  <Route path="insights" element={<Insights />} />
                  
                  {/* System & Profile Configuration */}
                  <Route path="profile" element={<Profile />} />
                  <Route path="settings" element={<Settings />} />
                  
                  {/* Catch-all 404 Route within standard App Shell */}
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