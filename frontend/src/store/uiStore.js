import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import dayjs from 'dayjs';

const now = dayjs();

const useUiStore = create(
  persist(
    (set) => ({
      // ── Theme ──────────────────────────────────────────────────────
      theme: 'dark',          // 'dark' | 'light'
      toggleTheme: () =>
        set((s) => ({ theme: s.theme === 'dark' ? 'light' : 'dark' })),

      // ── Sidebar ────────────────────────────────────────────────────
      sidebarCollapsed: false,
      toggleSidebar: () =>
        set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      setSidebarCollapsed: (v) => set({ sidebarCollapsed: v }),

      // ── Global GST Period (shared across all pages) ────────────────
      gstPeriod: {
        month: now.month() + 1,   // 1-12
        year: now.year(),
      },
      setGstPeriod: (month, year) => set({ gstPeriod: { month, year } }),
    }),
    {
      name: 'ui-storage',
      partialize: (s) => ({
        theme: s.theme,
        sidebarCollapsed: s.sidebarCollapsed,
        gstPeriod: s.gstPeriod,
      }),
    }
  )
);

export default useUiStore;
