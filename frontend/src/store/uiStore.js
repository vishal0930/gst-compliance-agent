import { create } from 'zustand';

const useUiStore = create((set) => ({
  sidebarCollapsed: false,
  currentPeriod: { month: 'June', year: 2026 },
  
  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
  setCurrentPeriod: (period) => set({ currentPeriod: period }),
}));

export default useUiStore;