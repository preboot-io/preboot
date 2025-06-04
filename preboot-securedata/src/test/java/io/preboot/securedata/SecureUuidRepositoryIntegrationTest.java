package io.preboot.securedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.preboot.query.HasUuid;
import io.preboot.query.SearchParams;
import io.preboot.securedata.annotation.Tenant;
import io.preboot.securedata.config.TestContainersConfig;
import io.preboot.securedata.exception.SecureDataException;
import io.preboot.securedata.repository.SecureRepositoryContext;
import io.preboot.securedata.repository.SecureUuidRepository;
import io.preboot.securedata.repository.SecureUuidRepositoryImpl;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({TestSecurityConfig.class, TestContainersConfig.class})
@Transactional
@Sql("/secure-test-data.sql")
class SecureUuidRepositoryIntegrationTest {

    @Autowired
    private TestUuidDocumentRepository documentRepository;

    @Autowired
    private TestSecurityContextHolder securityContextHolder;

    private static final UUID TENANT_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_1));
    }

    @Test
    void save_ShouldSetTenantIdAndUuid() {
        // Arrange
        TestUuidDocument document = new TestUuidDocument();
        document.setTitle("New UUID Document");

        // Act
        TestUuidDocument saved = documentRepository.save(document);

        // Assert
        assertThat(saved.getTenantId()).isEqualTo(TENANT_1);
        assertThat(saved.getUuid()).isNotNull();
    }

    @Test
    void save_WithExistingTenantId_ShouldNotOverride() {
        // Arrange
        TestUuidDocument document = new TestUuidDocument();
        document.setTitle("New Document");
        document.setTenantId(TENANT_2);

        // Act & Assert
        assertThatExceptionOfType(SecureDataException.class)
                .isThrownBy(() -> documentRepository.save(document))
                .withMessage("Access denied");
    }

    @Test
    void findByUuid_ShouldOnlyReturnDocumentIfBelongsToCurrentTenant() {
        // Arrange
        TestUuidDocument doc = new TestUuidDocument();
        doc.setTitle("Test UUID Document");
        TestUuidDocument saved = documentRepository.save(doc);
        UUID uuid = saved.getUuid();

        // Act & Assert
        // Should find document for current tenant
        Optional<TestUuidDocument> found = documentRepository.findByUuid(uuid);
        assertThat(found).isPresent();
        assertThat(found.get().getUuid()).isEqualTo(uuid);

        // Should not find document for different tenant
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        assertThat(documentRepository.findByUuid(uuid)).isEmpty();
    }

    @Test
    void findAll_ShouldOnlyReturnDocumentsForCurrentTenant() {
        // Act
        List<TestUuidDocument> documents =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Assert
        assertThat(documents).isNotEmpty().allMatch(doc -> TENANT_1.equals(doc.getTenantId()));
    }

    @Test
    void findAll_WhenSwitchingTenants_ShouldReturnDifferentDocuments() {
        // Arrange
        List<TestUuidDocument> tenant1Docs =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Act
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        List<TestUuidDocument> tenant2Docs =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Assert
        assertThat(tenant1Docs)
                .isNotEmpty()
                .extracting(TestUuidDocument::getTenantId)
                .containsOnly(TENANT_1);

        assertThat(tenant2Docs)
                .isNotEmpty()
                .extracting(TestUuidDocument::getTenantId)
                .containsOnly(TENANT_2);

        assertThat(tenant1Docs)
                .extracting(TestUuidDocument::getUuid)
                .doesNotContainAnyElementsOf(
                        tenant2Docs.stream().map(TestUuidDocument::getUuid).toList());
    }

    @Test
    void deleteByUuid_ShouldOnlyDeleteIfBelongsToCurrentTenant() {
        // Arrange
        TestUuidDocument doc = new TestUuidDocument();
        doc.setTitle("To Delete");
        TestUuidDocument saved = documentRepository.save(doc);
        UUID uuid = saved.getUuid();

        // Try to delete from wrong tenant
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        documentRepository.deleteByUuid(uuid);

        // Verify still exists
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_1));
        assertThat(documentRepository.findByUuid(uuid)).isPresent();

        // Delete from correct tenant
        documentRepository.deleteByUuid(uuid);
        assertThat(documentRepository.findByUuid(uuid)).isEmpty();
    }

    @Test
    void existsByUuid_ShouldOnlyReturnTrueForCurrentTenant() {
        // Arrange
        TestUuidDocument doc = new TestUuidDocument();
        doc.setTitle("Exist Test");
        TestUuidDocument saved = documentRepository.save(doc);
        UUID uuid = saved.getUuid();

        // Act & Assert
        assertThat(documentRepository.existsByUuid(uuid)).isTrue();

        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        assertThat(documentRepository.existsByUuid(uuid)).isFalse();
    }
}

@Table("secure_uuid_documents")
@Data
class TestUuidDocument implements HasUuid {
    @Id
    private Long id;

    private UUID uuid;

    @Tenant
    private UUID tenantId;

    private String title;
}

interface TestUuidDocumentRepository extends SecureUuidRepository<TestUuidDocument, Long> {}

@Repository
class TestUuidDocumentRepositoryImpl extends SecureUuidRepositoryImpl<TestUuidDocument, Long> {
    public TestUuidDocumentRepositoryImpl(SecureRepositoryContext context) {
        super(context, TestUuidDocument.class);
    }
}
