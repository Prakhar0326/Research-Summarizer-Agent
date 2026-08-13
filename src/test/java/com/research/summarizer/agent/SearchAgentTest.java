package com.research.summarizer.agent;

import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.SearchResult;
import com.research.summarizer.service.MCPService;
import com.research.summarizer.service.WebSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SearchAgent routing logic
 */
public class SearchAgentTest {

    @Mock
    private MCPService mcpService;

    @Mock
    private WebSearchService webSearchService;

    private SearchAgent searchAgent;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        searchAgent = new SearchAgent(mcpService, webSearchService);
    }

    @Test
    public void testOpenAIRelatedQueryRoutsToMCP() {
        // Test: Query with OpenAI keyword should route to MCP
        String topic = "How do I use the OpenAI API?";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "OpenAI API Guide",
                "https://platform.openai.com/docs",
                "OpenAI API documentation",
                "OpenAI API documentation",
                0.95));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
        assertEquals(1, context.getRawSearchResults().size());
    }

    @Test
    public void testOpenAIRelatedQueryFallsBackToWebSearch() {
        // Test: MCP returns empty, should fall back to web search
        String topic = "Latest GPT-4 capabilities";

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(new ArrayList<>());

        List<SearchResult> webResults = new ArrayList<>();
        webResults.add(new SearchResult(
                "GPT-4 Features",
                "https://example.com",
                "GPT-4 capabilities",
                "GPT-4 capabilities",
                0.9));

        when(webSearchService.search(topic)).thenReturn(webResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("WEB", context.getSearchSource());
        assertEquals(1, context.getRawSearchResults().size());
    }

    @Test
    public void testGeneralTopicRoutesDirectlyToWebSearch() {
        // Test: Non-OpenAI topic should route to web search directly
        String topic = "How to grow tomatoes";

        List<SearchResult> webResults = new ArrayList<>();
        webResults.add(new SearchResult(
                "Tomato Growing Guide",
                "https://example.com",
                "How to grow tomatoes",
                "How to grow tomatoes",
                0.85));

        when(webSearchService.search(topic)).thenReturn(webResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("WEB", context.getSearchSource());
    }

    @Test
    public void testGPTKeywordIsOpenAIRelated() {
        // Test: GPT keyword should be recognized as OpenAI-related
        String topic = "GPT-4 vs GPT-3.5 differences";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "Model Comparison",
                "https://platform.openai.com",
                "Model comparison",
                "Model comparison",
                0.9));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }

    @Test
    public void testFineTuningKeywordIsOpenAIRelated() {
        // Test: Fine-tuning keyword should be recognized as OpenAI-related
        String topic = "How to fine-tune models";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "Fine-tuning Guide",
                "https://platform.openai.com/docs/guides/fine-tuning",
                "Fine-tuning documentation",
                "Fine-tuning documentation",
                0.92));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }

    @Test
    public void testFineTuningWithSpaceIsOpenAIRelated() {
        // Test: "fine tuning" (with space) should be recognized as OpenAI-related
        String topic = "Can you tell me about fine tuning?";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "Fine-tuning Guide",
                "https://platform.openai.com/docs/guides/fine-tuning",
                "Fine-tuning documentation",
                "Fine-tuning documentation",
                0.92));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }

    @Test
    public void testAgentsKeywordIsOpenAIRelated() {
        // Test: "agents" keyword should be recognized as OpenAI-related
        String topic = "How do I use OpenAI Agents?";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "Agents Guide",
                "https://platform.openai.com/docs/guides/agents",
                "Agents documentation",
                "Agents documentation",
                0.90));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }

    @Test
    public void testResponsesKeywordIsOpenAIRelated() {
        // Test: "responses" keyword should be recognized as OpenAI-related
        String topic = "How to use OpenAI Responses API?";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "Responses API Guide",
                "https://platform.openai.com/docs/guides/responses",
                "Responses API documentation",
                "Responses API documentation",
                0.90));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }

    @Test
    public void testSDKKeywordIsOpenAIRelated() {
        // Test: "sdk" keyword should be recognized as OpenAI-related
        String topic = "What is the OpenAI SDK?";

        List<SearchResult> mcpResults = new ArrayList<>();
        mcpResults.add(new SearchResult(
                "SDK Guide",
                "https://platform.openai.com/docs/guides/sdk",
                "SDK documentation",
                "SDK documentation",
                0.90));

        when(mcpService.searchOpenAIDocs(topic)).thenReturn(mcpResults);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setTopic(topic);

        context = searchAgent.execute(context);

        assertEquals("MCP", context.getSearchSource());
    }
}
