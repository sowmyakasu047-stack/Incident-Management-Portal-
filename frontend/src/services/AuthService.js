import api from './api';
import { clearAuth, loadAuth, saveAuth } from '../utils/storage';

const AuthService = {
  async login(username, password) {
    const { data } = await api.post('/auth/login', { username, password });
    saveAuth(data);
    return data;
  },

  logout() {
    clearAuth();
  },

  getCurrentUser() {
    return loadAuth();
  },
};

export default AuthService;
