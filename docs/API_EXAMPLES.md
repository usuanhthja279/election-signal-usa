# Local API Smoke Check Examples

With server running at `http://127.0.0.1:4173`:

```powershell
Invoke-RestMethod http://127.0.0.1:4173/api/v1/health
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/context?mode=history&year=2000&office=house&state=TX'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/demo/races?office=senate&state=TX'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/demo/states/TX/summary'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/geography/manifest'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/polls'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/exit-polls'
Invoke-RestMethod 'http://127.0.0.1:4173/api/v1/models/races/TX-SEN-2026-DEMO'
```

Responses for historical/polls/exit/model return no imported data by design, never fictional live values. Open the API explorer (`/api-explorer.html`) for one-click tests.
