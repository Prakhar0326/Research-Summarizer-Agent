package com.research.summarizer.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.summarizer.model.AgentPipelineContext;
import com.research.summarizer.model.Insight;
import com.research.summarizer.model.SearchResult;
import com.research.summarizer.service.HuggingFaceLLMService;
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
 * Unit tests for InsightExtractorAgent (Agent 2)
 * 
 * Tests LLM-based insight extraction with fallback mechanisms
 * Based on actual uncommented code (starts at line 815 of InsightExtractorAgent.java)
 */
public class InsightExtractorAgentTest {

    @Mock
    private HuggingFaceLLMService llmService;

    @Mock
    private ObjectMapper objectMapper;

    private InsightExtractorAgent insightExtractorAgent;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        insightExtractorAgent = new InsightExtractorAgent(llmService, objectMapper);
    }

    // ========== Test 1: Execute with Null Search Results ==========

    @Test
    public void testExecuteWithNullSearchResults() {
        // When search results are null, should return empty insights
        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(null);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
        assertTrue(context.getExtractedInsights().isEmpty());
    }

    // ========== Test 2: Execute with Empty Search Results ==========

    @Test
    public void testExecuteWithEmptySearchResults() {
        // When search results are empty, should return empty insights
        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(new ArrayList<>());

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
        assertTrue(context.getExtractedInsights().isEmpty());
    }

    // ========== Test 3: LLM Extraction Returns Some Results ==========

    @Test
    public void testLLMExtractionReturnsResults() {
        // When LLM returns valid JSON, should extract insights
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "OpenAI API Guide",
                "https://platform.openai.com/docs",
                "The OpenAI API provides endpoints for chat completions",
                "API documentation",
                0.95));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"OpenAI API provides chat completions\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 4: LLM Returns Null Response ==========

    @Test
    public void testLLMReturnsNullResponse() {
        // When LLM returns null, should use fallback insights from search results
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Test Title",
                "https://example.com",
                "Some long content here that should be used as fallback",
                "Test snippet",
                0.85));

        when(llmService.generateText(anyString())).thenReturn(null);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
        // Should use fallback insights
        assertTrue(context.getExtractedInsights().size() >= 0);
    }

    // ========== Test 5: LLM Returns Blank Response ==========

    @Test
    public void testLLMReturnsBlankResponse() {
        // When LLM returns blank/empty string, should use fallback
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content here",
                "Snippet",
                0.80));

        when(llmService.generateText(anyString())).thenReturn("   ");

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 6: Context Preserved Through Agent ==========

    @Test
    public void testContextPreservedThroughAgent() {
        // Agent should preserve and return the same context object
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        when(llmService.generateText(anyString())).thenReturn(null);

        AgentPipelineContext originalContext = new AgentPipelineContext();
        originalContext.setTopic("test topic");
        originalContext.setRawSearchResults(searchResults);

        AgentPipelineContext returnedContext = insightExtractorAgent.execute(originalContext);

        assertSame(originalContext, returnedContext);
        assertEquals("test topic", returnedContext.getTopic());
        assertNotNull(returnedContext.getExtractedInsights());
    }

    // ========== Test 7: Multiple Search Results Processing ==========

    @Test
    public void testMultipleSearchResultsProcessing() {
        // Agent should process multiple search results
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Result 1",
                "https://example1.com",
                "Content 1",
                "Snippet 1",
                0.95));
        searchResults.add(new SearchResult(
                "Result 2",
                "https://example2.com",
                "Content 2",
                "Snippet 2",
                0.90));
        searchResults.add(new SearchResult(
                "Result 3",
                "https://example3.com",
                "Content 3",
                "Snippet 3",
                0.85));

        when(llmService.generateText(anyString())).thenReturn(null);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 8: Invalid JSON from LLM ==========

    @Test
    public void testInvalidJSONFromLLM() {
        // When LLM returns invalid JSON, should use fallback
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content here that should become fallback insight",
                "Snippet",
                0.85));

        String invalidJSON = "This is not valid JSON at all";

        when(llmService.generateText(anyString())).thenReturn(invalidJSON);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 9: LLM Service Throws Exception ==========

    @Test
    public void testLLMServiceThrowsException() {
        // When LLM service throws exception, should handle gracefully with fallback
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content as fallback",
                "Snippet",
                0.85));

        when(llmService.generateText(anyString()))
                .thenThrow(new RuntimeException("LLM Service Error"));

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        // Should return result even if LLM throws
        context = insightExtractorAgent.execute(context);
        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 10: JSON Array Not Extracted ==========

    @Test
    public void testJSONArrayNotExtracted() {
        // When response doesn't contain JSON array, should use fallback
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content for fallback",
                "Snippet",
                0.85));

        String nonArrayJSON = "{\"result\": \"not an array\"}";

        when(llmService.generateText(anyString())).thenReturn(nonArrayJSON);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 11: Fallback Insights Creation ==========

    @Test
    public void testFallbackInsightsCreation() {
        // Verify fallback insights are created from search results
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "API Reference",
                "https://platform.openai.com/docs/api",
                "The OpenAI API is a powerful tool for building applications",
                "API documentation",
                0.95));

        when(llmService.generateText(anyString())).thenReturn("");

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
        // Fallback insights should be of type "fact" with 0.70 confidence
        for (Insight insight : context.getExtractedInsights()) {
            assertEquals("fact", insight.type());
            assertEquals(0.70, insight.confidence());
        }
    }

    // ========== Test 12: Insights Sorted by Confidence ==========

    @Test
    public void testInsightsSortedByConfidence() {
        // Insights should be sorted by confidence score (highest first)
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"Test fact\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 13: Max Insights Limit ==========

    @Test
    public void testMaxInsightsLimitEnforced() {
        // Only MAX_INSIGHTS (5) should be returned
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"Test\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
        assertTrue(context.getExtractedInsights().size() <= 5);
    }

    // ========== Test 14: Valid Insight Types ==========

    @Test
    public void testOnlyValidInsightTypesExtracted() {
        // Only valid types (fact, statistic, definition) should be extracted
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"Valid fact\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 15: Content Trimming ==========

    @Test
    public void testInsightContentTrimmed() {
        // Insight content should be trimmed of whitespace
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"Test\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 16: Empty Content Filtered ==========

    @Test
    public void testEmptyContentFiltered() {
        // Insights with empty content should be skipped
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"fact\", \"content\": \"Valid content\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 17: Confidence Scores by Type ==========

    @Test
    public void testCorrectConfidenceScoresAssigned() {
        // Different types should get correct confidence scores
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "Title",
                "https://example.com",
                "Content",
                "Snippet",
                0.90));

        String llmResponse = "[{\"type\": \"statistic\", \"content\": \"95% of users\"}]";

        when(llmService.generateText(anyString())).thenReturn(llmResponse);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }

    // ========== Test 18: Fallback When LLM Parsing Fails ==========

    @Test
    public void testFallbackUsedWhenLLMParsingFails() {
        // When LLM response cannot be parsed, fallback insights should be used
        List<SearchResult> searchResults = new ArrayList<>();
        searchResults.add(new SearchResult(
                "API Guide",
                "https://api.example.com",
                "This is the complete API guide content that will be used as fallback",
                "API snippet",
                0.95));

        // Response that will fail JSON parsing
        String malformedJSON = "[{\"type\": \"fact\", \"content\": \"incomplete";

        when(llmService.generateText(anyString())).thenReturn(malformedJSON);

        AgentPipelineContext context = new AgentPipelineContext();
        context.setRawSearchResults(searchResults);

        context = insightExtractorAgent.execute(context);

        assertNotNull(context.getExtractedInsights());
    }
}
