import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', timeout: 45000, expect: { timeout: 15000 },
  use: { baseURL: 'http://127.0.0.1:3000', browserName: 'chromium', launchOptions: { executablePath: process.env.CHROME_PATH || 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe' } },
  projects: [
    { name: 'desktop', use: { viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { viewport: { width: 390, height: 844 } } },
    { name: 'tablet', use: { viewport: { width: 768, height: 1024 } } }
  ]
});
