# Research Summarizer Agent

A sophisticated multi-agent system that autonomously gathers information from multiple sources, extracts meaningful insights, and presents them as a coherent research report.

**Status**: Ready for production  
**Language**: Java 21  
**Build Tool**: Gradle 8.x  
**Framework**: Spring Boot 3.3.2  
**Agent Orchestration**: LangChain4j  
**LLM Provider**: HuggingFace Inference API (Mistral-7B or GPT-oss-120b)
**Web Search**: DuckDuckGo (free) or Tavily (with API key)

## Overview

The Research Summarizer Agent implements a three-stage agent pipeline:

1. **Search Agent** - Intelligently routes queries between OpenAI Docs MCP server and web search (DuckDuckGo or Tavily)
2. **Insight Extractor** - Uses HuggingFace LLM to extract structured insights (facts, statistics, definitions) from search results with fallback to regex patterns
3. **Report Generator** - Uses HuggingFace LLM to generate well-formatted summary reports with natural language processing

## Key Features

- ✅ **Intelligent Source Routing** - Automatically detects OpenAI-related queries and routes to the appropriate source
- ✅ **MCP Integration** - Connects to OpenAI's public Model Context Protocol server for documentation search
- ✅ **LLM-Powered Agents** - Agents 2 & 3 use HuggingFace Inference API for intelligent insight extraction and report generation
- ✅ **Flexible Web Search** - Supports DuckDuckGo (free, no API key) or Tavily (more accurate, requires API key)
- ✅ **Fallback Mechanisms** - Graceful degradation to regex-based extraction if LLM unavailable
- ✅ **Structured Output** - Returns well-organized reports with AI-generated executive summary, findings, and sources
- ✅ **REST API** - Single endpoint for easy integration
- ✅ **Comprehensive Logging** - DEBUG logging for request tracking across agent pipeline
- ✅ **Gradle Build** - Modern build system with Spring Boot 3.3.2 and LangChain4j

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              REST API Controller                             │
│         POST /api/research/summarize                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│        Research Summarizer Service (Orchestrator)            │
│        - Manages agent pipeline flow                         │
│        - Maintains pipeline context                          │
│        - Generates trace IDs for tracking                    │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
         ▼             ▼             ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   Agent 1:   │ │   Agent 2:   │ │   Agent 3:   │
│   Search     │ │   Insight    │ │   Report     │
│   Agent      │ │   Extractor  │ │   Generator  │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
   ┌────────────────────────────────────────────┐
   │  Search Results  →  Insights  →  Report    │
   └────────────────────────────────────────────┘
       ▲
       │
   ┌───┴────────────────────────┐
   │                             │
   ▼                             ▼
┌──────────────┐          ┌──────────────┐
│ MCP Server   │          │ Web Search   │
│ (OpenAI Docs)│          │ (Tavily/etc) │
└──────────────┘          └──────────────┘
```

## API Specification

### Endpoint

```
POST /api/research/summarize
```

### Request

```json
{
  "topic": "How to use OpenAI API for chat completions?",
  "maxSources": 5
}
```

### Response

```json
{
  "topic": "How to use OpenAI API for chat completions?",
  "searchSource": "MCP",
  "executiveSummary": "Research Summary: How to use OpenAI API for chat completions?\n\nThis research summary contains 15 key insights including 8 facts, 3 statistics, and 4 definitions...",
  "keyFindings": [
    "The OpenAI API provides endpoints for chat completions using gpt-4 and gpt-3.5-turbo models.",
    "Chat completions require a messages array with roles: system, user, and assistant.",
    "The API supports streaming responses for real-time token generation."
  ],
  "details": "## Key Findings\n\n- The OpenAI API provides endpoints for chat completions...",
  "sources": [
    {
      "title": "Chat Completions API Reference",
      "url": "https://platform.openai.com/docs/api-reference/chat/create",
      "snippet": "Creates a model response for the given chat conversation..."
    }
  ]
}
```

## Query Routing Logic

The Search Agent uses deterministic classification to route queries:

### Classification Rules

```
OPENAI_KEYWORDS = [
  "openai", "gpt", "davinci", "api", "chat completion", "embedding",
  "fine-tuning", "agents sdk", "responses api", "models", "dall-e",
  "assistants", "function calling", "retrieval", "vision"
]

if query_contains_any_openai_keyword():
  return "OpenAI-related" → Use MCP Server
else:
  return "General topic" → Use Web Search
```

### Routing Table

| Query Type                                                       | Primary Source  | Fallback   |
| ---------------------------------------------------------------- | --------------- | ---------- |
| OpenAI API / Models / SDK / Agents / Fine-tuning / Responses API | OpenAI Docs MCP | Web Search |
| General / Non-OpenAI topics                                      | Web Search      | N/A        |

### Example Routing

- ✅ "How to use GPT-4?" → MCP (contains "gpt")
- ✅ "OpenAI API authentication" → MCP (contains "openai")
- ✅ "Fine-tuning models with OpenAI" → MCP (contains "fine-tuning")
- ✅ "How to grow plants?" → Web Search (no OpenAI keywords)
- ✅ "Best Python frameworks" → Web Search (no OpenAI keywords)

## Setup Instructions

### Prerequisites

- Java 21 or higher
- Gradle 8.x (or use the included Gradle wrapper)
- HuggingFace API Key (for LLM-powered insight extraction and report generation) - Get one free at https://huggingface.co/settings/tokens
- **Optional**: Tavily API Key for more accurate web search (uses free DuckDuckGo by default)

### Installation

1. **Clone the repository**

   ```bash
   git clone <repository-url>
   cd research-summarizer-agent
   ```

2. **Set environment variables**

   **Windows (PowerShell):**
   ```powershell
   $env:HUGGINGFACE_API_KEY="hf_your_token_here"
   ```

   **Linux/Mac (Bash):**
   ```bash
   export HUGGINGFACE_API_KEY="hf_your_token_here"
   ```

   **Get HuggingFace Token:**
   - Go to: https://huggingface.co/settings/tokens
   - Click "New token"
   - Select "Read" access
   - Copy and set as `HUGGINGFACE_API_KEY`

3. **Build the project**

   ```bash
   gradle clean build
   ```

   Or using the Gradle wrapper:

   ```bash
   ./gradlew clean build
   ```

4. **Run the application**

   ```bash
   gradle bootRun
   ```

   Or using the Gradle wrapper:

   ```bash
   ./gradlew bootRun
   ```

   Or with JAR:

   ```bash
   java -jar build/libs/research-summarizer-agent-1.0.0.jar
   ```

   The application starts on `http://localhost:8080`

### Configuration

Edit `src/main/resources/application.properties`:

```properties
# HuggingFace Inference API Configuration (REQUIRED for LLM)
huggingface.api.key=${HUGGINGFACE_API_KEY:demo-key}
huggingface.model=openai/gpt-oss-120b
# Alternative model: mistralai/Mistral-7B-Instruct-v0.1 (faster, lower quality)
huggingface.timeout=30000

# MCP Configuration (for OpenAI documentation search)
mcp.server.url=https://developers.openai.com/mcp
mcp.timeout=30000

# Web Search Configuration
# Option 1: DuckDuckGo (free, no API key needed)
#web.search.provider=duckduckgo

 Option 2: Tavily (more accurate, requires API key)
 web.search.provider=tavily
 tavily.api.key=${TAVILY_API_KEY}
 tavily.max-results=5
 tavily.search-depth=basic

# Server Port
server.port=8080

# Logging - Enable DEBUG to trace agent pipeline
logging.level.com.research=DEBUG
logging.level.com.research.summarizer.service=DEBUG
logging.level.com.research.summarizer.agent=DEBUG
```

**Model Options:**
- `openai/gpt-oss-120b` (Recommended) - Higher quality, ~5-10s per query
- `mistralai/Mistral-7B-Instruct-v0.1` - Faster, ~2-3s per query

## Running the Application

### Using cURL

```bash
# Health check
curl -X POST http://localhost:8080/api/research/health

# OpenAI-related query (uses MCP)
curl -X POST http://localhost:8080/api/research/summarize \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "How to use OpenAI chat completion API?",
    "maxSources": 5
  }'

# General query (uses Web Search)
curl -X POST http://localhost:8080/api/research/summarize \
  -H "Content-Type: application/json" \
  -d '{
    "topic": "Best practices for Python async programming",
    "maxSources": 5
  }'
```

### Docker Setup (Optional)

```bash
# Build Docker image
docker build -t research-summarizer-agent:latest .

# Run container
docker run -e HUGGINGFACE_API_KEY=your-key \
           -e TAVILY_API_KEY=your-key \
           -p 8080:8080 \
           research-summarizer-agent:latest
```

## Project Structure

```
research-summarizer-agent/
├── src/
│   ├── main/
│   │   ├── java/com/research/summarizer/
│   │   │   ├── ResearchSummarizerApplication.java    # Main entry point
│   │   │   ├── agent/
│   │   │   │   ├── SearchAgent.java                  # Agent 1: Query routing
│   │   │   │   ├── InsightExtractorAgent.java         # Agent 2: LLM-based extraction
│   │   │   │   └── ReportGeneratorAgent.java          # Agent 3: LLM-based report generation
│   │   │   ├── controller/
│   │   │   │   └── ResearchController.java            # REST API endpoints
│   │   │   ├── service/
│   │   │   │   ├── ResearchSummarizerService.java     # Pipeline orchestrator
│   │   │   │   ├── HuggingFaceLLMService.java         # LLM service (NEW)
│   │   │   │   ├── MCPService.java                    # MCP client
│   │   │   │   └── WebSearchService.java              # Web search (Tavily/DuckDuckGo)
│   │   │   ├── dto/
│   │   │   │   ├── ResearchRequest.java               # API request
│   │   │   │   ├── ResearchResponse.java              # API response
│   │   │   │   └── SourceInfo.java                    # Source metadata
│   │   │   ├── model/
│   │   │   │   ├── AgentPipelineContext.java          # Context flow through agents
│   │   │   │   ├── SearchResult.java                  # Raw search result
│   │   │   │   └── Insight.java                       # Extracted insight
│   │   │   ├── tool/
│   │   │   │   └── (Tool implementations)
│   │   │   └── config/
│   │   │       └── ApplicationConfig.java             # Spring configuration
│   │   └── resources/
│   │       └── application.properties                 # App configuration
│   └── test/
│       └── java/com/research/summarizer/
│           └── (Test classes)
├── build.gradle                                       # Gradle build configuration
├── gradle.properties                                  # Gradle properties
├── settings.gradle                                    # Gradle settings
├── README.md                                          # This file
├── LLM_IMPLEMENTATION.md                             # LLM integration details
├── QUICKSTART.md                                     # Quick start guide
└── .gitignore
```

## Testing

### Run Unit Tests

```bash
./gradlew test
```

### Test Coverage

The test suite covers:

- ✅ SearchAgent routing logic (OpenAI keyword detection)
- ✅ MCP server integration
- ✅ Web search fallback (DuckDuckGo/Tavily)
- ✅ LLM-based insight extraction
- ✅ LLM fallback mechanisms

### Example Test Queries

1. **OpenAI API Query** (Routes to MCP, uses LLM extraction)

   ```bash
   curl -X POST http://localhost:8080/api/research/summarize \
     -H "Content-Type: application/json" \
     -d '{"topic": "What is the OpenAI Responses API?", "maxSources": 5}'
   ```

2. **General Query** (Routes to Web Search, uses LLM extraction)
   ```bash
   curl -X POST http://localhost:8080/api/research/summarize \
     -H "Content-Type: application/json" \
     -d '{"topic": "Machine learning best practices", "maxSources": 5}'
   ```

## Design Decisions

### 1. **Query Classification for Routing**

- **Decision**: Keyword-based deterministic classification
- **Rationale**: Fast, lightweight, and predictable without requiring LLM calls for routing
- **Trade-off**: May miss edge cases but provides reliable routing for common patterns

### 2. **LLM-Powered Agents 2 & 3**

- **Decision**: Use HuggingFace Inference API for insight extraction and report generation
- **Rationale**: Free access to powerful models (Mistral-7B, GPT-oss-120b) without OpenAI costs
- **Trade-off**: Rate limits on free tier (~30 req/min); implement fallback to regex-based extraction

### 3. **Agent Pipeline with Context Flow**

- **Decision**: Pass AgentPipelineContext through each agent
- **Rationale**: Maintains all intermediate results for debugging and future enhancements
- **Trade-off**: Slightly higher memory overhead but enables full traceability

### 4. **Graceful Fallback Mechanisms**

- **Decision**: Generate fallback results when LLM or external APIs unavailable
- **Rationale**: Ensures system continues to work even when APIs fail or rate limits hit
- **Trade-off**: Fallback results are less accurate but demonstrate end-to-end flow

### 5. **Multi-provider Web Search Support**

- **Decision**: Support both DuckDuckGo (free) and Tavily (paid, more accurate)
- **Rationale**: Provider flexibility and graceful degradation
- **Trade-off**: More code but better resilience and user choice

### 6. **Structured Logging with DEBUG Mode**

- **Decision**: DEBUG-level logging throughout agent pipeline
- **Rationale**: Enables request tracking and agent monitoring in development
- **Trade-off**: Minimal performance overhead, excellent observability

## MCP Server Integration

### MCP Server Details

- **URL**: https://developers.openai.com/mcp
- **Transport**: Streamable HTTP
- **Authentication**: None (read-only, public server)
- **Response Format**: JSON

### Supported Searches

The OpenAI MCP server provides access to documentation on:

- API Reference (Chat Completions, Embeddings, etc.)
- Agents SDK
- Models and Pricing
- Responses API
- Fine-tuning Guides
- Vision and Multimodal
- Function Calling

### Error Handling

- Timeout: 30 seconds (configurable)
- Connection errors: Automatic fallback to web search
- Empty results: Automatic fallback to web search
- JSON parsing errors: Logged and skipped

## Dependencies

| Dependency  | Version | Purpose                                  |
| ----------- | ------- | ---------------------------------------- |
| Spring Boot | 3.3.2   | Web framework & REST API                 |
| LangChain4j | 0.32.0  | Agent orchestration & LLM integration    |
| OkHttp      | 4.12.0  | HTTP client for MCP & web search         |
| JSoup       | 1.17.2  | HTML parsing for DuckDuckGo results      |
| Jackson     | Latest  | JSON processing                          |
| GSON        | Latest  | JSON serialization for LLM responses     |
| Lombok      | Latest  | Boilerplate reduction                    |
| JUnit 5     | Latest  | Testing framework                        |

**Build Tool**: Gradle 8.x with Spring Boot plugin

**LLM Provider**: HuggingFace Inference API
- Mistral-7B-Instruct-v0.1 (faster option)
- GPT-oss-120b (higher quality option)

## Future Enhancements

1. **LLM-based Query Classification** - Use Claude/GPT for more sophisticated routing decisions
2. **Advanced Caching Layer** - Redis cache for frequently searched topics and LLM responses
3. **Streaming Responses** - Server-Sent Events for real-time agent processing updates
4. **Multi-language Support** - Internationalization for queries and multilingual reports
5. **Advanced Analytics Dashboard** - Track query patterns, agent performance, and LLM quality metrics
6. **Prompt Optimization** - Dynamic prompt engineering based on topic and query type
7. **Source Quality Scoring** - Rank sources by authority, relevance, and recency
8. **Conversation Memory** - Multi-turn research conversations with context preservation
9. **Batch Processing** - Process multiple queries concurrently with queue management
10. **Custom LLM Providers** - Support for local LLMs (Ollama) and commercial providers (OpenAI, Anthropic)

## Troubleshooting

### Issue: "HuggingFace API key not configured"

**Solution**: 
1. Set `HUGGINGFACE_API_KEY` environment variable
2. Restart the application: `./gradlew bootRun`
3. Check logs for LLM initialization

### Issue: "LLM returned empty response"

**Causes & Solutions:**
1. **Rate limit hit** - Free tier has ~30 requests/minute limit. Wait and retry.
2. **Model overloaded** - Try at a different time or switch to faster model (Mistral-7B)
3. **Invalid API key** - Regenerate token at https://huggingface.co/settings/tokens
4. **Network timeout** - Increase `huggingface.timeout` in application.properties

### Issue: "No web search results found"

**Solution**: 
- If using Tavily: Check that `TAVILY_API_KEY` is valid
- If using DuckDuckGo: No API key needed; check internet connectivity

### Issue: "MCP server timeout"

**Solution**: Increase `mcp.timeout` in application.properties (default 30s)

### Issue: "Port 8080 already in use"

**Solution**: Change `server.port` in application.properties

### Issue: "JSON parsing error"

**Solution**: System falls back to template-based generation. Check logs for detailed error. Retry the query.

## Performance Metrics

- **Average response time**: 5-15 seconds (depends on LLM model, MCP/web search latency)
  - MCP search: 1-3s
  - LLM insight extraction (Agent 2): 2-5s
  - LLM report generation (Agent 3): 2-5s
- **Throughput**: 30+ concurrent requests (limited by HuggingFace free tier rate limits)
- **Memory usage**: ~768MB (Spring Boot + LLM context buffers)
- **Maximum search results**: 10 sources per request
- **LLM Model Performance**:
  - Mistral-7B: ~2-3s per LLM call
  - GPT-oss-120b: ~5-10s per LLM call

## Security Considerations

- ✅ **API keys stored in environment variables**, not in code (use `.gitignore` for sensitive files)
- ✅ **API keys NOT hardcoded** in application.properties (use environment variable substitution)
- ✅ **HTTPS recommended** for production deployment to protect API keys in transit
- ✅ **Input validation** on all API endpoints to prevent injection attacks
- ✅ **Structured error responses** (no sensitive data leaked in error messages)
- ✅ **Rate limiting recommended** to prevent abuse (implement with Spring Cloud Config or API Gateway)
- ⚠️ **HuggingFace API Key** - Regenerate if accidentally exposed
- ⚠️ **Tavily API Key** - Keep private, implement per-user rate limiting in production

## License

This project is provided as-is for educational and commercial use.

## Support

For issues or questions, please create a GitHub issue or contact the development team.

---

**Version**: 2.0.0  
**Last Updated**: 2026-08-14  
**Status**: Production Ready with LLM-Powered Agents
