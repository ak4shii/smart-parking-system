import api from './api';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  role: string;
  enabled: boolean;
  createdAt: string;
}

export interface LoginResponse {
  message: string;
  user: User | null;
  jwtToken: string | null;
}

export interface RegisterErrorResponse {
  email?: string;
  password?: string;
  username?: string;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/api/auth/login', credentials);

    if (response.data.jwtToken && response.data.user) {
      localStorage.setItem('token', response.data.jwtToken);
      localStorage.setItem('user', JSON.stringify(response.data.user));

      try {
        await api.get('/csrf-token');
      } catch (error) {
        console.error('Failed to fetch CSRF token:', error);
      }
    }

    return response.data;
  }

  async register(userData: RegisterRequest): Promise<string> {
    const response = await api.post<string | RegisterErrorResponse>('/api/auth/register', userData);

    if (typeof response.data === 'string') {
      return response.data;
    }

    throw response.data;
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('user');

    const cookiesToClear = ['jwt_token', 'JSESSIONID', 'XSRF-TOKEN', 'session'];
    cookiesToClear.forEach(cookieName => {
      document.cookie = `${cookieName}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;
    });
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}

export default new AuthService();

