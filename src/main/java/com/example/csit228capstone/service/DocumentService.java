package com.example.csit228capstone.service;

import com.example.csit228capstone.model.*;
import com.example.csit228capstone.model.document.DocumentRequest;
import com.example.csit228capstone.model.document.DocumentStatus;
import com.example.csit228capstone.model.document.DocumentType;
import com.example.csit228capstone.repository.DocumentRequestRepository;
import com.example.csit228capstone.repository.ResidentRepository;
import com.example.csit228capstone.util.AlertEventBus;
import com.example.csit228capstone.util.AlertEventBus.EventType;
import com.example.csit228capstone.util.ReportFactory;

import java.util.List;
import java.util.UUID;

public class DocumentService {

    private final DocumentRequestRepository docRepo = new DocumentRequestRepository();
    private final ResidentRepository residentRepo = new ResidentRepository();

    public List<DocumentRequest> getAll() { return docRepo.findAll(); }
    public List<DocumentRequest> getByStatus(DocumentStatus status) { return docRepo.findByStatus(status); }
    public int getPendingCount() { return docRepo.countByStatus(DocumentStatus.PENDING); }

    public UUID createRequest(UUID residentId, DocumentType type, String purpose) {
        DocumentRequest dr = new DocumentRequest();
        dr.setResidentId(residentId);
        dr.setDocumentType(type);
        dr.setPurpose(purpose);
        return docRepo.insert(dr);
    }

    public void advanceStatus(UUID requestId, DocumentStatus newStatus, UUID processedBy) {
        docRepo.updateStatus(requestId, newStatus, processedBy);
        AlertEventBus.getInstance().publish(EventType.DOCUMENT_STATUS_CHANGED, requestId);
    }

    public byte[] generatePdf(UUID requestId) {
        // Look up the request and resident, then generate
        List<DocumentRequest> all = docRepo.findAll();
        DocumentRequest req = all.stream().filter(d -> d.getId().equals(requestId)).findFirst()
                .orElseThrow(() -> new RuntimeException("Request not found"));
        Resident resident = residentRepo.findById(req.getResidentId())
                .orElseThrow(() -> new RuntimeException("Resident not found"));
        return ReportFactory.generate(req.getDocumentType(), resident, req.getPurpose());
    }
}
