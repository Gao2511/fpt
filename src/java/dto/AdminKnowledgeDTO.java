package dto;

import java.time.Instant;
import java.util.*;

/**
 * Phase 3 DTOs for the Admin Knowledge Base & RAG Management System.
 * All classes expose standard getters for JSP JSTL/EL rendering.
 */
public final class AdminKnowledgeDTO {
    private AdminKnowledgeDTO() {}

    public static final class DashboardMetrics {
        public final int totalDocuments;
        public final int draftDocuments;
        public final int pendingReviewDocuments;
        public final int approvedDocuments;
        public final int publishedDocuments;
        public final int rejectedDocuments;
        public final int archivedDocuments;
        public final int unpublishedDocuments;
        public final int failedDocuments;
        public final int indexedDocuments;
        public final int needingReindexDocuments;
        public final int indexingFailures;
        public final int totalFaqs;
        public final int publishedFaqs;
        public final int totalChunks;
        public final List<AuditLogEntry> recentActivities;

        public DashboardMetrics(int totalDocuments, int draftDocuments, int pendingReviewDocuments,
                int approvedDocuments, int publishedDocuments, int rejectedDocuments, int archivedDocuments,
                int unpublishedDocuments, int failedDocuments, int indexedDocuments,
                int needingReindexDocuments, int indexingFailures, int totalFaqs, int publishedFaqs,
                int totalChunks, List<AuditLogEntry> recentActivities) {
            this.totalDocuments = totalDocuments;
            this.draftDocuments = draftDocuments;
            this.pendingReviewDocuments = pendingReviewDocuments;
            this.approvedDocuments = approvedDocuments;
            this.publishedDocuments = publishedDocuments;
            this.rejectedDocuments = rejectedDocuments;
            this.archivedDocuments = archivedDocuments;
            this.unpublishedDocuments = unpublishedDocuments;
            this.failedDocuments = failedDocuments;
            this.indexedDocuments = indexedDocuments;
            this.needingReindexDocuments = needingReindexDocuments;
            this.indexingFailures = indexingFailures;
            this.totalFaqs = totalFaqs;
            this.publishedFaqs = publishedFaqs;
            this.totalChunks = totalChunks;
            this.recentActivities = recentActivities == null
                    ? Collections.<AuditLogEntry>emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(recentActivities));
        }

        public int getTotalDocuments() { return totalDocuments; }
        public int getDraftDocuments() { return draftDocuments; }
        public int getPendingReviewDocuments() { return pendingReviewDocuments; }
        public int getApprovedDocuments() { return approvedDocuments; }
        public int getPublishedDocuments() { return publishedDocuments; }
        public int getRejectedDocuments() { return rejectedDocuments; }
        public int getArchivedDocuments() { return archivedDocuments; }
        public int getUnpublishedDocuments() { return unpublishedDocuments; }
        public int getFailedDocuments() { return failedDocuments; }
        public int getIndexedDocuments() { return indexedDocuments; }
        public int getNeedingReindexDocuments() { return needingReindexDocuments; }
        public int getIndexingFailures() { return indexingFailures; }
        public int getTotalFaqs() { return totalFaqs; }
        public int getPublishedFaqs() { return publishedFaqs; }
        public int getTotalChunks() { return totalChunks; }
        public List<AuditLogEntry> getRecentActivities() { return recentActivities; }
    }

    public static final class ManagedDocument {
        public final long id;
        public final long version;
        public final String title;
        public final String category;
        public final String content;
        public final String source;
        public final String status;
        public final String workflowStatus;
        public final String sourceType;
        public final String tags;
        public final String department;
        public final String contentHash;
        public final String indexingStatus;
        public final Long indexedVersion;
        public final String policyKey;
        public final String sourceAuthority;
        public final Long supersedesDocumentId;
        public final Instant effectiveAt;
        public final Instant expiresAt;
        public final int approvedBy;
        public final Instant approvedAt;
        public final int createdBy;
        public final int updatedBy;
        public final int reviewedBy;
        public final Instant reviewedAt;
        public final String rejectionReason;
        public final int chunkSize;
        public final int chunkOverlap;
        public final Instant createdAt;
        public final Instant updatedAt;
        public String embeddingProvider="MOCK",embeddingError="";
        public Instant lastIndexedAt;
        public String getIndexMode(){return "GEMINI".equals(embeddingProvider)?"GEMINI INDEX":"MOCK INDEX";}
        public String getEmbeddingError(){return embeddingError;}
        public Instant getLastIndexedAt(){return lastIndexedAt;}
        public String getRuntimeIndexStatus(){
            if("ready".equals(indexingStatus))return indexedVersion!=null&&indexedVersion.longValue()==version?"INDEXED":"STALE";
            if(lastIndexedAt!=null&&"pending".equals(indexingStatus))return "STALE";
            return "failed_retryable".equals(indexingStatus)?"FAILED":indexingStatus.toUpperCase(java.util.Locale.ROOT);
        }

        public ManagedDocument(long id, long version, String title, String category, String content,
                String source, String status, String workflowStatus, String sourceType, String tags,
                String department, String contentHash, String indexingStatus, Long indexedVersion,
                String policyKey, String sourceAuthority, Long supersedesDocumentId,
                Instant effectiveAt, Instant expiresAt, int approvedBy, Instant approvedAt,
                int createdBy, int updatedBy, int reviewedBy, Instant reviewedAt,
                String rejectionReason, int chunkSize, int chunkOverlap,
                Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.version = version;
            this.title = title;
            this.category = category;
            this.content = content;
            this.source = source;
            this.status = status;
            this.workflowStatus = workflowStatus;
            this.sourceType = sourceType;
            this.tags = tags == null ? "" : tags;
            this.department = department == null ? "" : department;
            this.contentHash = contentHash;
            this.indexingStatus = indexingStatus;
            this.indexedVersion = indexedVersion;
            this.policyKey = policyKey == null ? "" : policyKey;
            this.sourceAuthority = sourceAuthority;
            this.supersedesDocumentId = supersedesDocumentId;
            this.effectiveAt = effectiveAt;
            this.expiresAt = expiresAt;
            this.approvedBy = approvedBy;
            this.approvedAt = approvedAt;
            this.createdBy = createdBy;
            this.updatedBy = updatedBy;
            this.reviewedBy = reviewedBy;
            this.reviewedAt = reviewedAt;
            this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
            this.chunkSize = chunkSize;
            this.chunkOverlap = chunkOverlap;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public boolean isNeedsReindex() {
            return "PUBLISHED".equals(workflowStatus)
                    && (!"ready".equals(indexingStatus) || indexedVersion == null || indexedVersion.longValue() != version);
        }

        public long getId() { return id; }
        public long getVersion() { return version; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getContent() { return content; }
        public String getSource() { return source; }
        public String getStatus() { return status; }
        public String getWorkflowStatus() { return workflowStatus; }
        public String getSourceType() { return sourceType; }
        public String getTags() { return tags; }
        public String getDepartment() { return department; }
        public String getContentHash() { return contentHash; }
        public String getIndexingStatus() { return indexingStatus; }
        public Long getIndexedVersion() { return indexedVersion; }
        public String getPolicyKey() { return policyKey; }
        public String getSourceAuthority() { return sourceAuthority; }
        public Long getSupersedesDocumentId() { return supersedesDocumentId; }
        public Instant getEffectiveAt() { return effectiveAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public int getApprovedBy() { return approvedBy; }
        public Instant getApprovedAt() { return approvedAt; }
        public int getCreatedBy() { return createdBy; }
        public int getUpdatedBy() { return updatedBy; }
        public int getReviewedBy() { return reviewedBy; }
        public Instant getReviewedAt() { return reviewedAt; }
        public String getRejectionReason() { return rejectionReason; }
        public int getChunkSize() { return chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
    }

    public static final class DocumentRevision {
        public final long id;
        public final long documentId;
        public final long versionNumber;
        public final String title;
        public final String category;
        public final String tags;
        public final String department;
        public final String source;
        public final String sourceType;
        public final String sourceAuthority;
        public final String policyKey;
        public final String content;
        public final String contentHash;
        public final String workflowStatus;
        public final Instant effectiveAt;
        public final Instant expiresAt;
        public final Long supersedesDocumentId;
        public final String changeNote;
        public final Long rollbackFromVersion;
        public final int createdBy;
        public final Instant createdAt;

        public DocumentRevision(long id, long documentId, long versionNumber, String title,
                String category, String tags, String department, String source, String sourceType,
                String sourceAuthority, String policyKey, String content, String contentHash,
                String workflowStatus, Instant effectiveAt, Instant expiresAt,
                Long supersedesDocumentId, String changeNote, Long rollbackFromVersion,
                int createdBy, Instant createdAt) {
            this.id = id;
            this.documentId = documentId;
            this.versionNumber = versionNumber;
            this.title = title;
            this.category = category;
            this.tags = tags == null ? "" : tags;
            this.department = department == null ? "" : department;
            this.source = source;
            this.sourceType = sourceType;
            this.sourceAuthority = sourceAuthority;
            this.policyKey = policyKey == null ? "" : policyKey;
            this.content = content;
            this.contentHash = contentHash;
            this.workflowStatus = workflowStatus;
            this.effectiveAt = effectiveAt;
            this.expiresAt = expiresAt;
            this.supersedesDocumentId = supersedesDocumentId;
            this.changeNote = changeNote == null ? "" : changeNote;
            this.rollbackFromVersion = rollbackFromVersion;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public long getId() { return id; }
        public long getDocumentId() { return documentId; }
        public long getVersionNumber() { return versionNumber; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getTags() { return tags; }
        public String getDepartment() { return department; }
        public String getSource() { return source; }
        public String getSourceType() { return sourceType; }
        public String getSourceAuthority() { return sourceAuthority; }
        public String getPolicyKey() { return policyKey; }
        public String getContent() { return content; }
        public String getContentHash() { return contentHash; }
        public String getWorkflowStatus() { return workflowStatus; }
        public Instant getEffectiveAt() { return effectiveAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public Long getSupersedesDocumentId() { return supersedesDocumentId; }
        public String getChangeNote() { return changeNote; }
        public Long getRollbackFromVersion() { return rollbackFromVersion; }
        public int getCreatedBy() { return createdBy; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static final class RevisionDiff {
        public final long documentId;
        public final long fromVersion;
        public final long toVersion;
        public final boolean contentChanged;
        public final List<String> metadataChanges;
        public final List<String> diffLines;
        public final int addedCount;
        public final int removedCount;

        public RevisionDiff(long documentId, long fromVersion, long toVersion, boolean contentChanged,
                List<String> metadataChanges, List<String> diffLines, int addedCount, int removedCount) {
            this.documentId = documentId;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.contentChanged = contentChanged;
            this.metadataChanges = Collections.unmodifiableList(new ArrayList<>(metadataChanges));
            this.diffLines = Collections.unmodifiableList(new ArrayList<>(diffLines));
            this.addedCount = addedCount;
            this.removedCount = removedCount;
        }

        public long getDocumentId() { return documentId; }
        public long getFromVersion() { return fromVersion; }
        public long getToVersion() { return toVersion; }
        public boolean isContentChanged() { return contentChanged; }
        public List<String> getMetadataChanges() { return metadataChanges; }
        public List<String> getDiffLines() { return diffLines; }
        public int getAddedCount() { return addedCount; }
        public int getRemovedCount() { return removedCount; }
    }

    public static final class DocumentReview {
        public final long id;
        public final long documentId;
        public final long versionNumber;
        public final String action;
        public final String previousStatus;
        public final String newStatus;
        public final int reviewerId;
        public final String comments;
        public final String rejectionReason;
        public final Instant createdAt;

        public DocumentReview(long id, long documentId, long versionNumber, String action,
                String previousStatus, String newStatus, int reviewerId, String comments,
                String rejectionReason, Instant createdAt) {
            this.id = id;
            this.documentId = documentId;
            this.versionNumber = versionNumber;
            this.action = action;
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.reviewerId = reviewerId;
            this.comments = comments == null ? "" : comments;
            this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
            this.createdAt = createdAt;
        }

        public long getId() { return id; }
        public long getDocumentId() { return documentId; }
        public long getVersionNumber() { return versionNumber; }
        public String getAction() { return action; }
        public String getPreviousStatus() { return previousStatus; }
        public String getNewStatus() { return newStatus; }
        public int getReviewerId() { return reviewerId; }
        public String getComments() { return comments; }
        public String getRejectionReason() { return rejectionReason; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static final class UploadedFileRecord {
        public final String id;
        public final Long documentId;
        public final long versionNumber;
        public final String originalFilename;
        public final String storedFilename;
        public final String storageRelPath;
        public final String detectedMimeType;
        public final String fileExtension;
        public final long fileSizeBytes;
        public final String sha256Hash;
        public final String extractionStatus;
        public final int extractedCharCount;
        public final String extractionWarning;
        public final int uploadedBy;
        public final Instant uploadedAt;

        public UploadedFileRecord(String id, Long documentId, long versionNumber, String originalFilename,
                String storedFilename, String storageRelPath, String detectedMimeType, String fileExtension,
                long fileSizeBytes, String sha256Hash, String extractionStatus, int extractedCharCount,
                String extractionWarning, int uploadedBy, Instant uploadedAt) {
            this.id = id;
            this.documentId = documentId;
            this.versionNumber = versionNumber;
            this.originalFilename = originalFilename;
            this.storedFilename = storedFilename;
            this.storageRelPath = storageRelPath;
            this.detectedMimeType = detectedMimeType;
            this.fileExtension = fileExtension;
            this.fileSizeBytes = fileSizeBytes;
            this.sha256Hash = sha256Hash;
            this.extractionStatus = extractionStatus;
            this.extractedCharCount = extractedCharCount;
            this.extractionWarning = extractionWarning == null ? "" : extractionWarning;
            this.uploadedBy = uploadedBy;
            this.uploadedAt = uploadedAt;
        }

        public String getId() { return id; }
        public Long getDocumentId() { return documentId; }
        public long getVersionNumber() { return versionNumber; }
        public String getOriginalFilename() { return originalFilename; }
        public String getStoredFilename() { return storedFilename; }
        public String getStorageRelPath() { return storageRelPath; }
        public String getDetectedMimeType() { return detectedMimeType; }
        public String getFileExtension() { return fileExtension; }
        public long getFileSizeBytes() { return fileSizeBytes; }
        public String getSha256Hash() { return sha256Hash; }
        public String getExtractionStatus() { return extractionStatus; }
        public int getExtractedCharCount() { return extractedCharCount; }
        public String getExtractionWarning() { return extractionWarning; }
        public int getUploadedBy() { return uploadedBy; }
        public Instant getUploadedAt() { return uploadedAt; }
    }

    public static final class FaqItem {
        public final long id;
        public final String question;
        public final String answer;
        public final String category;
        public final String tags;
        public final int priority;
        public final String workflowStatus;
        public final long versionNumber;
        public final Instant effectiveAt;
        public final Instant expiresAt;
        public final String sourceAuthority;
        public final String policyKey;
        public final Long linkedDocumentId;
        public final int createdBy;
        public final int updatedBy;
        public final int reviewedBy;
        public final Instant reviewedAt;
        public final String rejectionReason;
        public final Instant createdAt;
        public final Instant updatedAt;

        public FaqItem(long id, String question, String answer, String category, String tags,
                int priority, String workflowStatus, long versionNumber, Instant effectiveAt,
                Instant expiresAt, String sourceAuthority, String policyKey, Long linkedDocumentId,
                int createdBy, int updatedBy, int reviewedBy, Instant reviewedAt,
                String rejectionReason, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.category = category;
            this.tags = tags == null ? "" : tags;
            this.priority = priority;
            this.workflowStatus = workflowStatus;
            this.versionNumber = versionNumber;
            this.effectiveAt = effectiveAt;
            this.expiresAt = expiresAt;
            this.sourceAuthority = sourceAuthority;
            this.policyKey = policyKey == null ? "" : policyKey;
            this.linkedDocumentId = linkedDocumentId;
            this.createdBy = createdBy;
            this.updatedBy = updatedBy;
            this.reviewedBy = reviewedBy;
            this.reviewedAt = reviewedAt;
            this.rejectionReason = rejectionReason == null ? "" : rejectionReason;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        public String getSourceId() { return "faq:" + id; }
        public long getId() { return id; }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getCategory() { return category; }
        public String getTags() { return tags; }
        public int getPriority() { return priority; }
        public String getWorkflowStatus() { return workflowStatus; }
        public long getVersionNumber() { return versionNumber; }
        public Instant getEffectiveAt() { return effectiveAt; }
        public Instant getExpiresAt() { return expiresAt; }
        public String getSourceAuthority() { return sourceAuthority; }
        public String getPolicyKey() { return policyKey; }
        public Long getLinkedDocumentId() { return linkedDocumentId; }
        public int getCreatedBy() { return createdBy; }
        public int getUpdatedBy() { return updatedBy; }
        public int getReviewedBy() { return reviewedBy; }
        public Instant getReviewedAt() { return reviewedAt; }
        public String getRejectionReason() { return rejectionReason; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
    }

    public static final class FaqRevision {
        public final long id;
        public final long faqId;
        public final long versionNumber;
        public final String question;
        public final String answer;
        public final String category;
        public final String tags;
        public final int priority;
        public final String workflowStatus;
        public final String changeNote;
        public final int createdBy;
        public final Instant createdAt;

        public FaqRevision(long id, long faqId, long versionNumber, String question, String answer,
                String category, String tags, int priority, String workflowStatus, String changeNote,
                int createdBy, Instant createdAt) {
            this.id = id;
            this.faqId = faqId;
            this.versionNumber = versionNumber;
            this.question = question;
            this.answer = answer;
            this.category = category;
            this.tags = tags == null ? "" : tags;
            this.priority = priority;
            this.workflowStatus = workflowStatus;
            this.changeNote = changeNote == null ? "" : changeNote;
            this.createdBy = createdBy;
            this.createdAt = createdAt;
        }

        public long getId() { return id; }
        public long getFaqId() { return faqId; }
        public long getVersionNumber() { return versionNumber; }
        public String getQuestion() { return question; }
        public String getAnswer() { return answer; }
        public String getCategory() { return category; }
        public String getTags() { return tags; }
        public int getPriority() { return priority; }
        public String getWorkflowStatus() { return workflowStatus; }
        public String getChangeNote() { return changeNote; }
        public int getCreatedBy() { return createdBy; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static final class AuditLogEntry {
        public final long id;
        public final String correlationId;
        public final int actorId;
        public final String actorUsername;
        public final String action;
        public final String resourceType;
        public final String resourceId;
        public final String previousState;
        public final String newState;
        public final String summary;
        public final Instant createdAt;

        public AuditLogEntry(long id, String correlationId, int actorId, String actorUsername,
                String action, String resourceType, String resourceId, String previousState,
                String newState, String summary, Instant createdAt) {
            this.id = id;
            this.correlationId = correlationId;
            this.actorId = actorId;
            this.actorUsername = actorUsername == null ? "" : actorUsername;
            this.action = action;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.previousState = previousState == null ? "{}" : previousState;
            this.newState = newState == null ? "{}" : newState;
            this.summary = summary == null ? "" : summary;
            this.createdAt = createdAt;
        }

        public long getId() { return id; }
        public String getCorrelationId() { return correlationId; }
        public int getActorId() { return actorId; }
        public String getActorUsername() { return actorUsername; }
        public String getAction() { return action; }
        public String getResourceType() { return resourceType; }
        public String getResourceId() { return resourceId; }
        public String getPreviousState() { return previousState; }
        public String getNewState() { return newState; }
        public String getSummary() { return summary; }
        public Instant getCreatedAt() { return createdAt; }
    }

    public static final class RagAdminSettings {
        public final boolean localIndexingEnabled;
        public final boolean stagingRagEnabled;
        public final boolean productionRagEnabled; // Always false in Phase 3
        public final int topK;
        public final double similarityThreshold;
        public final int maxContextTokens;
        public final int chunkSize;
        public final int chunkOverlap;
        public final String primaryChatModel;
        public final String primaryThinkingLevel;
        public final String backupChatModel;
        public final boolean backupEnabled;
        public final String embeddingModel;
        public final int vectorDimensions;

        public RagAdminSettings(boolean localIndexingEnabled, boolean stagingRagEnabled,
                boolean productionRagEnabled, int topK, double similarityThreshold,
                int maxContextTokens, int chunkSize, int chunkOverlap,
                String primaryChatModel, String primaryThinkingLevel,
                String backupChatModel, boolean backupEnabled,
                String embeddingModel, int vectorDimensions) {
            this.localIndexingEnabled = localIndexingEnabled;
            this.stagingRagEnabled = stagingRagEnabled;
            this.productionRagEnabled = false; // Hard-locked false server-side
            this.topK = topK;
            this.similarityThreshold = similarityThreshold;
            this.maxContextTokens = maxContextTokens;
            this.chunkSize = chunkSize;
            this.chunkOverlap = chunkOverlap;
            this.primaryChatModel = primaryChatModel;
            this.primaryThinkingLevel = primaryThinkingLevel;
            this.backupChatModel = backupChatModel;
            this.backupEnabled = backupEnabled;
            this.embeddingModel = embeddingModel;
            this.vectorDimensions = vectorDimensions;
        }

        public boolean isLocalIndexingEnabled() { return localIndexingEnabled; }
        public boolean isStagingRagEnabled() { return stagingRagEnabled; }
        public boolean isProductionRagEnabled() { return productionRagEnabled; }
        public int getTopK() { return topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public int getMaxContextTokens() { return maxContextTokens; }
        public int getChunkSize() { return chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public String getPrimaryChatModel() { return primaryChatModel; }
        public String getPrimaryThinkingLevel() { return primaryThinkingLevel; }
        public String getBackupChatModel() { return backupChatModel; }
        public boolean isBackupEnabled() { return backupEnabled; }
        public String getEmbeddingModel() { return embeddingModel; }
        public int getVectorDimensions() { return vectorDimensions; }
    }

    public static final class PlaygroundChunkView {
        public final long chunkId;
        public final long documentId;
        public final long documentVersion;
        public final String title;
        public final String content;
        public final String source;
        public final double score;
        public final String status;
        public final String category;
        public final String authority;
        public final String policyKey;
        public final String policyDecision;
        public final boolean selectedForContext;

        public PlaygroundChunkView(long chunkId, long documentId, long documentVersion, String title,
                String content, String source, double score, String status, String category,
                String authority, String policyKey, String policyDecision, boolean selectedForContext) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.documentVersion = documentVersion;
            this.title = title;
            this.content = content;
            this.source = source;
            this.score = score;
            this.status = status;
            this.category = category;
            this.authority = authority;
            this.policyKey = policyKey;
            this.policyDecision = policyDecision;
            this.selectedForContext = selectedForContext;
        }

        public long getChunkId() { return chunkId; }
        public long getDocumentId() { return documentId; }
        public long getDocumentVersion() { return documentVersion; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getSource() { return source; }
        public double getScore() { return score; }
        public String getStatus() { return status; }
        public String getCategory() { return category; }
        public String getAuthority() { return authority; }
        public String getPolicyKey() { return policyKey; }
        public String getPolicyDecision() { return policyDecision; }
        public boolean isSelectedForContext() { return selectedForContext; }
    }

    public static final class PlaygroundResult {
        public String banner;
        public final String query;
        public final List<PlaygroundChunkView> candidateChunks;
        public final List<PlaygroundChunkView> selectedChunks;
        public final String selectionIssue;
        public final Map<String, Integer> rejectedReasons;
        public final String constructedContextJson;
        public final String deterministicMockAnswer;
        public final boolean abstained;

        public PlaygroundResult(String query, List<PlaygroundChunkView> candidateChunks,
                List<PlaygroundChunkView> selectedChunks, String selectionIssue,
                Map<String, Integer> rejectedReasons, String constructedContextJson,
                String deterministicMockAnswer, boolean abstained) {
            this.banner = "OFFLINE TEST MODE — NO GEMINI API REQUESTS";
            this.query = query;
            this.candidateChunks = Collections.unmodifiableList(new ArrayList<>(candidateChunks));
            this.selectedChunks = Collections.unmodifiableList(new ArrayList<>(selectedChunks));
            this.selectionIssue = selectionIssue;
            this.rejectedReasons = Collections.unmodifiableMap(new LinkedHashMap<>(rejectedReasons));
            this.constructedContextJson = constructedContextJson;
            this.deterministicMockAnswer = deterministicMockAnswer;
            this.abstained = abstained;
        }

        public String getBanner() { return banner; }
        public String getQuery() { return query; }
        public List<PlaygroundChunkView> getCandidateChunks() { return candidateChunks; }
        public List<PlaygroundChunkView> getSelectedChunks() { return selectedChunks; }
        public String getSelectionIssue() { return selectionIssue; }
        public Map<String, Integer> getRejectedReasons() { return rejectedReasons; }
        public String getConstructedContextJson() { return constructedContextJson; }
        public String getDeterministicMockAnswer() { return deterministicMockAnswer; }
        public boolean isAbstained() { return abstained; }
    }

    public static final class ComponentStatus {
        public final String name;
        public final String code;
        public final String state;
        public final String badgeVariant;
        public final String summary;
        public final String detail;

        public ComponentStatus(String name, String state, String badgeVariant, String summary, String detail) {
            this(name, state == null ? "UNKNOWN" : state.toUpperCase(java.util.Locale.ROOT).replace(' ', '_'),
                    state, badgeVariant, summary, detail);
        }

        public ComponentStatus(String name, String code, String state, String badgeVariant, String summary, String detail) {
            this.name = name;
            this.code = code == null ? "UNKNOWN" : code;
            this.state = state;
            this.badgeVariant = badgeVariant;
            this.summary = summary == null ? "" : summary;
            this.detail = detail == null ? "" : detail;
        }

        public String getName() { return name; }
        public String getCode() { return code; }
        public String getState() { return state; }
        public String getBadgeVariant() { return badgeVariant; }
        public String getSummary() { return summary; }
        public String getDetail() { return detail; }
    }

    public static final class SystemReadinessStatus {
        public final ComponentStatus geminiChat;
        public final ComponentStatus geminiEmbedding;
        public final ComponentStatus database;
        public final ComponentStatus knowledgeBase;
        public final ComponentStatus rag;
        public final ComponentStatus indexing;
        public final ComponentStatus backupModel;
        public final boolean aiChatbotEnabled;
        public final boolean ragEnabled;
        public final boolean geminiIndexingEnabled;
        public final boolean kbDatabaseConnected;
        public final boolean productionReleaseAuthorized;
        public final String runtimeMode;
        public final String environment;
        public final String buildVersion;
        public final String buildCommit;
        public final String buildTimestamp;
        public final String storageBackend;

        public SystemReadinessStatus(ComponentStatus geminiChat, ComponentStatus geminiEmbedding,
                ComponentStatus knowledgeBase, ComponentStatus rag, ComponentStatus indexing,
                ComponentStatus backupModel, boolean aiChatbotEnabled, boolean ragEnabled,
                boolean geminiIndexingEnabled, boolean kbDatabaseConnected,
                boolean productionReleaseAuthorized, String runtimeMode) {
            this(geminiChat, geminiEmbedding,
                    new ComponentStatus("Database", kbDatabaseConnected ? "CONNECTED" : "DISCONNECTED",
                            kbDatabaseConnected ? "ready" : "offline",
                            kbDatabaseConnected ? "PostgreSQL Connected" : "Database Disconnected", ""),
                    knowledgeBase, rag, indexing, backupModel,
                    aiChatbotEnabled, ragEnabled, geminiIndexingEnabled, kbDatabaseConnected,
                    productionReleaseAuthorized, runtimeMode,
                    utils.BuildInfo.getEnvironment(),
                    utils.BuildInfo.getVersion(),
                    utils.BuildInfo.getShortCommitSha(),
                    utils.BuildInfo.getBuildTimestamp(),
                    "DISK");
        }

        public SystemReadinessStatus(ComponentStatus geminiChat, ComponentStatus geminiEmbedding,
                ComponentStatus database, ComponentStatus knowledgeBase, ComponentStatus rag,
                ComponentStatus indexing, ComponentStatus backupModel, boolean aiChatbotEnabled,
                boolean ragEnabled, boolean geminiIndexingEnabled, boolean kbDatabaseConnected,
                boolean productionReleaseAuthorized, String runtimeMode, String environment,
                String buildVersion, String buildCommit, String buildTimestamp, String storageBackend) {
            this.geminiChat = geminiChat;
            this.geminiEmbedding = geminiEmbedding;
            this.database = database;
            this.knowledgeBase = knowledgeBase;
            this.rag = rag;
            this.indexing = indexing;
            this.backupModel = backupModel;
            this.aiChatbotEnabled = aiChatbotEnabled;
            this.ragEnabled = ragEnabled;
            this.geminiIndexingEnabled = geminiIndexingEnabled;
            this.kbDatabaseConnected = kbDatabaseConnected;
            this.productionReleaseAuthorized = productionReleaseAuthorized;
            this.runtimeMode = runtimeMode == null ? "OFFLINE_MOCK" : runtimeMode;
            this.environment = environment == null ? "LOCAL" : environment;
            this.buildVersion = buildVersion == null ? utils.BuildInfo.getVersion() : buildVersion;
            this.buildCommit = buildCommit == null ? utils.BuildInfo.getShortCommitSha() : buildCommit;
            this.buildTimestamp = buildTimestamp == null ? utils.BuildInfo.getBuildTimestamp() : buildTimestamp;
            this.storageBackend = storageBackend == null ? "DISK" : storageBackend;
        }

        public ComponentStatus getGeminiChat() { return geminiChat; }
        public ComponentStatus getGeminiEmbedding() { return geminiEmbedding; }
        public ComponentStatus getDatabase() { return database; }
        public ComponentStatus getKnowledgeBase() { return knowledgeBase; }
        public ComponentStatus getRag() { return rag; }
        public ComponentStatus getIndexing() { return indexing; }
        public ComponentStatus getBackupModel() { return backupModel; }
        public boolean isAiChatbotEnabled() { return aiChatbotEnabled; }
        public boolean isRagEnabled() { return ragEnabled; }
        public boolean isGeminiIndexingEnabled() { return geminiIndexingEnabled; }
        public boolean isKbDatabaseConnected() { return kbDatabaseConnected; }
        public boolean isProductionReleaseAuthorized() { return productionReleaseAuthorized; }
        public String getRuntimeMode() { return runtimeMode; }
        public String getEnvironment() { return environment; }
        public String getBuildVersion() { return buildVersion; }
        public String getBuildCommit() { return buildCommit; }
        public String getBuildTimestamp() { return buildTimestamp; }
        public String getStorageBackend() { return storageBackend; }
    }
}

