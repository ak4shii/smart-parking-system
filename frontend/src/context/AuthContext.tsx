import { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import authService from '../services/authService';
import type { User, LoginRequest, RegisterRequest } from '../services/authService';
import { initializeCsrfToken } from '../services/csrfService';
import toast from 'react-hot-toast';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginRequest) => Promise<void>;
  register: (userData: RegisterRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const initializeAuth = async () => {
      // Initialize CSRF token on app startup
      await initializeCsrfToken();
      
      const token = authService.getToken();
      if (token) {
        setIsLoading(false);
      } else {
        setIsLoading(false);
      }
    };
    
    initializeAuth();
  }, []);

  const login = async (credentials: LoginRequest) => {
    try {
      const response = await authService.login(credentials);
      if (response.user && response.jwtToken) {
        setUser(response.user);
        toast.success('Login successful!');
        navigate('/');
      } else {
        toast.error(response.message || 'Login failed');
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || 'Login failed';
      toast.error(errorMessage);
      throw error;
    }
  };

  const register = async (userData: RegisterRequest) => {
    try {
      const message = await authService.register(userData);
      toast.success(message || 'Registration successful!');
      navigate('/login');
    } catch (error: any) {
      if (error.email || error.password || error.username) {
        const errorMessages = Object.values(error).filter(Boolean);
        errorMessages.forEach((msg) => toast.error(msg as string));
      } else {
        const errorMessage = error.response?.data?.message || error.message || 'Registration failed';
        toast.error(errorMessage);
      }
      throw error;
    }
  };

  const logout = async () => {
    authService.logout();
    setUser(null);
    // Re-initialize CSRF token after logout
    await initializeCsrfToken();
    toast.success('Logged out successfully');
    navigate('/login');
  };

  const value: AuthContextType = {
    user,
    isLoading,
    isAuthenticated: !!authService.getToken(),
    login,
    register,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

