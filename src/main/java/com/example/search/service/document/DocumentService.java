package com.example.search.service.document;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.search.dto.request.DocumentInputDto;
import com.example.search.model.Document;
import com.example.search.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

  private final DocumentRepository documentRepository;

  public List<Document> getAllDocuments() {
    return documentRepository.findAll();
  }

  public Document getDocumentById(Long id) {
    return documentRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
  }

  @Transactional
  public Document createDocument(DocumentInputDto input) {
    Document document =
        Document.builder()
            .title(input.getTitle())
            .content(input.getContent())
            .author(input.getAuthor())
            .category(input.getCategory())
            .status(input.getStatus())
            .build();

    return documentRepository.save(document);
  }

  @Transactional
  public Document updateDocument(Long id, DocumentInputDto input) {
    Document document = getDocumentById(id);
    document.setTitle(input.getTitle());
    document.setContent(input.getContent());
    document.setAuthor(input.getAuthor());
    if (input.getCategory() != null) {
      document.setCategory(input.getCategory());
    }
    if (input.getStatus() != null) {
      document.setStatus(input.getStatus());
    }

    return documentRepository.save(document);
  }

  @Transactional
  public Boolean deleteDocument(Long id) {
    if (documentRepository.existsById(id)) {
      documentRepository.deleteById(id);
      return true;
    }
    return false;
  }
}
