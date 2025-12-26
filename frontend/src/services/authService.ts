import api from './api';
import Cookies from 'js-cookie';

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
    
    if (response.data.jwtToken) {
      Cookies.set('jwt_token', response.data.jwtToken, { expires: 1 });
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
    Cookies.remove('jwt_token');
  }

  getToken(): string | undefined {
    return Cookies.get('jwt_token');
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
}

export default new AuthService();

