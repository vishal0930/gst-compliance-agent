import client from './client';

export const authApi = {
  login: async (credentials) => {
    console.log("=== authApi.login START ===");
    console.log("Credentials:", credentials);

    const data = await client.post('/auth/login', credentials);
    console.log("Login API response:", data);
    console.log("Token in response:", data.token);

    if (data.token) {
      localStorage.setItem('accessToken', data.token);
      console.log("Token saved to localStorage:", data.token);
    } else {
      console.error("No token in login response!");
    }

    return data;
  },

  logout: async () => {
    localStorage.removeItem('accessToken');
  },

  getCurrentUser: async () => {
    console.log("=== authApi.getCurrentUser START ===");
    console.log("Calling /auth/me");
    const response = await client.get('/auth/me');
    console.log("getCurrentUser response:", response);
    return response;
  },
};