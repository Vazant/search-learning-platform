package com.example.search.service.search;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.search.dto.response.IndexingResultDto;
import com.example.search.dto.response.ReindexResultDto;
import com.example.search.dto.response.SearchComparisonDto;
import com.example.search.dto.response.SearchResultDto;
import com.example.search.model.Document;
import com.example.search.service.document.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

  private final SolrSearchService solrSearchService;
  private final OpenSearchService openSearchService;
  private final TypeSenseService typeSenseService;
  private final DocumentService documentService;

  public List<SearchResultDto> searchWithSolr(String query) {
    return solrSearchService.search(query);
  }

  public List<SearchResultDto> searchWithOpenSearch(String query) {
    return openSearchService.search(query);
  }

  public List<SearchResultDto> searchWithTypeSense(String query) {
    return typeSenseService.search(query);
  }

  public SearchComparisonDto compareSearchEngines(String query) {
    long startTime, endTime;

    // Solr
    startTime = System.currentTimeMillis();
    List<SearchResultDto> solrResults = searchWithSolr(query);
    endTime = System.currentTimeMillis();
    int solrTime = (int) (endTime - startTime);

    // OpenSearch
    startTime = System.currentTimeMillis();
    List<SearchResultDto> openSearchResults = searchWithOpenSearch(query);
    endTime = System.currentTimeMillis();
    int openSearchTime = (int) (endTime - startTime);

    // TypeSense
    startTime = System.currentTimeMillis();
    List<SearchResultDto> typeSenseResults = searchWithTypeSense(query);
    endTime = System.currentTimeMillis();
    int typeSenseTime = (int) (endTime - startTime);

    return SearchComparisonDto.builder()
        .query(query)
        .solrResults(solrResults)
        .openSearchResults(openSearchResults)
        .typeSenseResults(typeSenseResults)
        .solrTime(solrTime)
        .openSearchTime(openSearchTime)
        .typeSenseTime(typeSenseTime)
        .build();
  }

  public IndexingResultDto indexDocument(Long id) {
    Document document = documentService.getDocumentById(id);

    boolean solrSuccess = solrSearchService.indexDocument(document);
    boolean openSearchSuccess = openSearchService.indexDocument(document);
    boolean typeSenseSuccess = typeSenseService.indexDocument(document);

    String message =
        String.format(
            "Indexed in Solr: %s, OpenSearch: %s, TypeSense: %s",
            solrSuccess, openSearchSuccess, typeSenseSuccess);

    return IndexingResultDto.builder()
        .documentId(id)
        .solrSuccess(solrSuccess)
        .openSearchSuccess(openSearchSuccess)
        .typeSenseSuccess(typeSenseSuccess)
        .message(message)
        .build();
  }

  public ReindexResultDto reindexAll() {
    List<Document> documents = documentService.getAllDocuments();
    int successCount = 0;
    int failureCount = 0;

    for (Document document : documents) {
      try {
        indexDocument(document.getId());
        successCount++;
      } catch (Exception e) {
        log.error("Failed to index document {}: {}", document.getId(), e.getMessage());
        failureCount++;
      }
    }

    return ReindexResultDto.builder()
        .totalDocuments(documents.size())
        .successCount(successCount)
        .failureCount(failureCount)
        .message(String.format("Reindexed %d documents, %d failures", successCount, failureCount))
        .build();
  }
}
