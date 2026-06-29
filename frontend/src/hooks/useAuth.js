import { useEffect } from 'react';
import useAuthStore from '../store/authStore';

export const useAuth = () => {
  const { user, isAuthenticated, loading, login, logout, loadUser } = useAuthStore();

  useEffect(() => {
    loadUser();
  }, []);

  return { user, isAuthenticated, loading, login, logout };
};