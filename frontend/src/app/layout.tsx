import type { ReactNode } from 'react';
import './style.css';
export const metadata = { title: 'Election Signal', description: 'Election information demonstration' };
export default function RootLayout({ children }: { children: ReactNode }) {
  return <html lang="en"><body>{children}</body></html>;
}
