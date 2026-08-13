#!/usr/bin/env pwsh

# Test script for Research Summarizer Agent API

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Research Summarizer Agent - API Tests" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Health Check
Write-Host "Test 1: Health Check" -ForegroundColor Green
Write-Host "Request: POST http://localhost:8080/api/research/health"
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/research/health" `
                                 -Method POST `
                                 -ContentType "application/json" `
                                 -UseBasicParsing
    $body = $response.Content | ConvertFrom-Json
    Write-Host "Response Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response Body:`n$($body | ConvertTo-Json)" -ForegroundColor Yellow
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: OpenAI-related query (should route to MCP)
Write-Host "Test 2: OpenAI API Query (Should route to MCP)" -ForegroundColor Green
Write-Host "Request: POST http://localhost:8080/api/research/summarize"
Write-Host "Body: {`"topic`": `"How to use OpenAI API for chat completions?`", `"maxSources`": 5}"
try {
    $body = @{
        topic = "How to use OpenAI API for chat completions?"
        maxSources = 5
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/research/summarize" `
                                 -Method POST `
                                 -ContentType "application/json" `
                                 -Body $body `
                                 -UseBasicParsing
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Response Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Search Source: $($data.searchSource)" -ForegroundColor Yellow
    Write-Host "Topic: $($data.topic)" -ForegroundColor Yellow
    Write-Host "Executive Summary:`n$($data.executiveSummary)" -ForegroundColor Yellow
    if ($data.keyFindings.Count -gt 0) {
        Write-Host "Key Findings:" -ForegroundColor Yellow
        $data.keyFindings | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: General query (should route to Web Search)
Write-Host "Test 3: General Query (Should route to Web Search)" -ForegroundColor Green
Write-Host "Request: POST http://localhost:8080/api/research/summarize"
Write-Host "Body: {`"topic`": `"Python async programming best practices`", `"maxSources`": 5}"
try {
    $body = @{
        topic = "Python async programming best practices"
        maxSources = 5
    } | ConvertTo-Json
    
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/research/summarize" `
                                 -Method POST `
                                 -ContentType "application/json" `
                                 -Body $body `
                                 -UseBasicParsing
    $data = $response.Content | ConvertFrom-Json
    Write-Host "Response Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Search Source: $($data.searchSource)" -ForegroundColor Yellow
    Write-Host "Topic: $($data.topic)" -ForegroundColor Yellow
    Write-Host "Executive Summary:`n$($data.executiveSummary)" -ForegroundColor Yellow
    if ($data.keyFindings.Count -gt 0) {
        Write-Host "Key Findings:" -ForegroundColor Yellow
        $data.keyFindings | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Tests Complete" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan
