# Test Scenarios for Research Summarizer Agent

## Scenario 1: MCP Fails → Fallback to Web Search
**Query Type**: OpenAI-related but MCP might not have data  
**Expected Behavior**: Routes to MCP first, if empty/fails → falls back to Web Search

```powershell
$body = @{
    topic = "OpenAI gpt-4-turbo-preview latest features and pricing 2024"
    maxSources = 5
} | ConvertTo-Json

Invoke-WebRequest -Uri 'http://localhost:8080/api/research/summarize' `
  -Method POST `
  -ContentType 'application/json' `
  -Body $body | Select-Object -ExpandProperty Content
```

**Look for in response**:
- `"searchSource": "WEB"` (indicates it fell back from MCP)
- Key findings about GPT-4 pricing/features

---

## Scenario 2: Successful MCP Query (No Fallback)
**Query Type**: Standard OpenAI documentation query  
**Expected Behavior**: MCP server returns results directly

```powershell
$body = @{
    topic = "How to use OpenAI chat completion API"
    maxSources = 5
} | ConvertTo-Json

Invoke-WebRequest -Uri 'http://localhost:8080/api/research/summarize' `
  -Method POST `
  -ContentType 'application/json' `
  -Body $body | Select-Object -ExpandProperty Content
```

**Look for in response**:
- `"searchSource": "MCP"` (MCP had results, no fallback needed)
- Documentation details about chat completions

---

## Scenario 3: Non-OpenAI Query (Direct Web Search)
**Query Type**: General topic, NOT OpenAI-related  
**Expected Behavior**: Skips MCP, goes directly to Web Search

```powershell
$body = @{
    topic = "Python async programming best practices 2024"
    maxSources = 5
} | ConvertTo-Json

Invoke-WebRequest -Uri 'http://localhost:8080/api/research/summarize' `
  -Method POST `
  -ContentType 'application/json' `
  -Body $body | Select-Object -ExpandProperty Content
```

**Look for in response**:
- `"searchSource": "WEB"` (direct web search, no MCP)
- Python async patterns and best practices

---

## Scenario 4: Check Logs to See Routing Decision
**View application logs** to see the decision-making process:

In the application output, watch for these log messages:

```
SearchAgent processing topic: ...
Query classified as OpenAI-related: true/false
Routing query to OpenAI Docs MCP server
MCP server returned X results
MCP server returned no results, falling back to web search
Routing query to general web search
```

---

## How to Run Tests

### Option 1: Using PowerShell (Recommended)
Copy and paste each scenario's PowerShell script into your terminal while the app is running.

### Option 2: Using cURL
```bash
curl -X POST http://localhost:8080/api/research/summarize \
  -H "Content-Type: application/json" \
  -d '{"topic":"How to use OpenAI API","maxSources":5}'
```

### Option 3: Using the batch script
```powershell
.\test-api.bat
```

---

## Expected Response Format

```json
{
    "topic": "...",
    "searchSource": "MCP" or "WEB",
    "executiveSummary": "Research Summary: ...",
    "keyFindings": [
        "Finding 1",
        "Finding 2",
        "..."
    ],
    "details": "## Key Findings\n\n- ...",
    "sources": [
        {
            "title": "Source Title",
            "url": "https://...",
            "snippet": "..."
        }
    ]
}
```

---

## What Each Field Tells You

| Field | Meaning |
|-------|---------|
| `searchSource: "MCP"` | Query was OpenAI-related AND MCP had results |
| `searchSource: "WEB"` | Either (1) Non-OpenAI query, OR (2) OpenAI query but MCP failed/empty |
| `keyFindings` | Structured insights extracted by InsightExtractorAgent |
| `details` | Full report with statistics and definitions (if available) |
| `sources` | List of sources that provided the information |

