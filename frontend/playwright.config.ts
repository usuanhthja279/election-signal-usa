import { defineConfig } from '@playwright/test';
const previewPort = process.env.ELECTION_E2E_PORT || '3000';
export default defineConfig({
  testDir: './e2e', timeout: 45000, expect: { timeout: 15000 },
  use: { baseURL: `http://127.0.0.1:${previewPort}`, browserName: 'chromium', launchOptions: process.env.CHROME_PATH ? { executablePath: process.env.CHROME_PATH } : {} },
  webServer: {
    command: `npm run dev -- --hostname 127.0.0.1 --port ${previewPort}`,
    url: `http://127.0.0.1:${previewPort}`,
    reuseExistingServer: process.env.ELECTION_E2E_REUSE_SERVER === '1' || !process.env.CI,
    timeout: 120000,
  },
  projects: [
    { name: 'desktop', use: { viewport: { width: 1440, height: 900 } } },
    { name: 'mobile', use: { viewport: { width: 390, height: 844 } } },
    { name: 'tablet', use: { viewport: { width: 768, height: 1024 } } }
  ]
});
