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

export interface RegisterResponse {
  message: string;
  mqttUsername?: string | null;
  mqttPassword?: string | null;
  mqttBrokerUri?: string | null;
}

class AuthService {
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/api/auth/login', credentials);

    if (response.data.jwtToken && response.data.user) {
      localStorage.setItem('token', response.data.jwtToken);
      localStorage.setItem('user', JSON.stringify(response.data.user));
    }

    return response.data;
  }

  async register(userData: RegisterRequest): Promise<string> {
    try {
      // Backend returns RegisterResponseDto object with message field
      const response = await api.post<RegisterResponse>('/api/auth/register', userData);

      // Return success message from response
      return response.data.message || 'Registration successful!';
    } catch (error: any) {
      // Handle validation errors (email/username/password fields)
      if (error.response?.data && typeof error.response.data === 'object') {
        throw error.response.data;
      }
      // Re-throw other errors
      throw error;
    }
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

