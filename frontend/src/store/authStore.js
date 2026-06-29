import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { authApi } from '../api/auth';

const useAuthStore = create(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      loading: true,

      login: async (email, password) => {
        console.log("=== authStore.login START ===");
        const data = await authApi.login({ email, password });
        console.log("Login response data:", data);

        const token = localStorage.getItem('accessToken');
        console.log("Token from localStorage after login:", token);

        try {
          console.log("Calling getCurrentUser...");
          const user = await authApi.getCurrentUser();
          console.log("getCurrentUser response:", user);

          set({
            user,
            token,
            isAuthenticated: true,
            loading: false,
          });
          console.log("Store updated with token:", token);
          return user;
        } catch (error) {
          console.error("getCurrentUser failed:", error);
          localStorage.removeItem('accessToken');
          set({ user: null, token: null, isAuthenticated: false, loading: false });
          throw error;
        }
      },

      logout: async () => {
        await authApi.logout();
        localStorage.removeItem('accessToken');
        set({
          user: null,
          token: null,
          isAuthenticated: false,
          loading: false,
        });
      },

      loadUser: async () => {
        try {
          const token = localStorage.getItem('accessToken');
          if (token) {
            const user = await authApi.getCurrentUser();
            set({ user, token, isAuthenticated: true, loading: false });
          } else {
            set({ loading: false });
          }
        } catch {
          localStorage.removeItem('accessToken');
          set({ user: null, token: null, isAuthenticated: false, loading: false });
        }
      },
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({ user: state.user, token: state.token, isAuthenticated: state.isAuthenticated }),
    }
  )
);

export default useAuthStore;