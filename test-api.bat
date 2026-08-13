@echo off
REM Test script for Research Summarizer Agent API

setlocal enabledelayedexpansion

echo.
echo ========================================
echo Research Summarizer Agent - API Tests
echo ========================================
echo.

REM Test 1: Health Check
echo Test 1: Health Check
echo Request: POST http://localhost:8080/api/research/health
powershell -Command "$response = (Invoke-WebRequest -Uri 'http://localhost:8080/api/research/health' -Method POST -ContentType 'application/json' -UseBasicParsing); Write-Host $response.Content"
echo.

REM Test 2: OpenAI-related query (should route to MCP)
echo Test 2: OpenAI API Query (Should route to MCP)
echo Request: POST http://localhost:8080/api/research/summarize
echo Body: {"topic": "How to use OpenAI API for chat completions?", "maxSources": 5}
powershell -Command "$body = '{\"topic\":\"How to use OpenAI API for chat completions?\",\"maxSources\":5}'; $response = (Invoke-WebRequest -Uri 'http://localhost:8080/api/research/summarize' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing); Write-Host $response.Content"
echo.

REM Test 3: General query (should route to Web Search)
echo Test 3: General Query (Should route to Web Search)
echo Request: POST http://localhost:8080/api/research/summarize
echo Body: {"topic": "Python async programming best practices", "maxSources": 5}
powershell -Command "$body = '{\"topic\":\"Python async programming best practices\",\"maxSources\":5}'; $response = (Invoke-WebRequest -Uri 'http://localhost:8080/api/research/summarize' -Method POST -ContentType 'application/json' -Body $body -UseBasicParsing); Write-Host $response.Content"
echo.

echo ========================================
echo Tests Complete
echo ========================================
