import { useEffect, useMemo, useState } from 'react';

type Theme = 'light' | 'dark';

const STORAGE_KEY = 'theme';

function getInitialTheme(): Theme {
  // Force Light theme by default (we still keep dark classes in code for future enablement).
  return 'light';
}

export default function useTheme() {
  const [theme, setTheme] = useState<Theme>(() => getInitialTheme());

  useEffect(() => {
    // Force Light: ensure "dark" class is never applied.
    const root = document.documentElement;
    root.classList.remove('dark');
    localStorage.setItem(STORAGE_KEY, 'light');
  }, [theme]);

  const toggle = useMemo(() => {
    return () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  }, []);

  return { theme, setTheme, toggle };
}

