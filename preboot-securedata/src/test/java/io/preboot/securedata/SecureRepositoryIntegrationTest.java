package io.preboot.securedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.preboot.query.SearchParams;
import io.preboot.securedata.annotation.Tenant;
import io.preboot.securedata.config.TestContainersConfig;
import io.preboot.securedata.context.SecurityContext;
import io.preboot.securedata.context.SecurityContextProvider;
import io.preboot.securedata.exception.SecureDataException;
import io.preboot.securedata.repository.SecureRepository;
import io.preboot.securedata.repository.SecureRepositoryContext;
import io.preboot.securedata.repository.SecureRepositoryImpl;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({TestSecurityConfig.class, TestContainersConfig.class})
@Transactional
@Sql("/secure-test-data.sql")
class SecureRepositoryIntegrationTest {

    @Autowired
    private TestDocumentRepository documentRepository;

    @Autowired
    private TestSecurityContextHolder securityContextHolder;

    private static final UUID TENANT_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        // Set default tenant
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_1));
    }

    @Test
    void save_ShouldSetTenantId() {
        // Arrange
        TestDocument document = new TestDocument();
        document.setTitle("New Document");

        // Act
        TestDocument saved = documentRepository.save(document);

        // Assert
        assertThat(saved.getTenantId()).isEqualTo(TENANT_1);
    }

    @Test
    void save_WithExistingTenantId_ShouldNotOverride() {
        // Arrange
        TestDocument document = new TestDocument();
        document.setTitle("New Document");
        document.setTenantId(TENANT_2);

        // Act
        assertThatExceptionOfType(SecureDataException.class)
                .isThrownBy(() -> documentRepository.save(document))
                .withMessage("Access denied");
    }

    @Test
    void findAll_ShouldOnlyReturnDocumentsForCurrentTenant() {
        // Act
        List<TestDocument> documents =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Assert
        assertThat(documents).isNotEmpty().allMatch(doc -> TENANT_1.equals(doc.getTenantId()));
    }

    @Test
    void findAll_WhenSwitchingTenants_ShouldReturnDifferentDocuments() {
        // Arrange
        List<TestDocument> tenant1Docs =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Act
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        List<TestDocument> tenant2Docs =
                documentRepository.findAll(SearchParams.empty()).getContent();

        // Assert
        assertThat(tenant1Docs)
                .isNotEmpty()
                .extracting(TestDocument::getTenantId)
                .containsOnly(TENANT_1);

        assertThat(tenant2Docs)
                .isNotEmpty()
                .extracting(TestDocument::getTenantId)
                .containsOnly(TENANT_2);

        assertThat(tenant1Docs)
                .extracting(TestDocument::getId)
                .doesNotContainAnyElementsOf(
                        tenant2Docs.stream().map(TestDocument::getId).toList());
    }

    @Test
    void findById_ShouldOnlyReturnDocumentIfBelongsToCurrentTenant() {
        // Arrange
        TestDocument doc = new TestDocument();
        doc.setTitle("Test");
        TestDocument saved = documentRepository.save(doc);

        // Act & Assert
        assertThat(documentRepository.findById(saved.getId())).isPresent();

        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        assertThat(documentRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_ShouldOnlyDeleteIfBelongsToCurrentTenant() {
        // Arrange
        TestDocument doc = new TestDocument();
        doc.setTitle("To Delete");
        TestDocument saved = documentRepository.save(doc);
        Long id = saved.getId();

        // Try to delete from wrong tenant
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_2));
        documentRepository.deleteById(id);

        // Verify still exists
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_1));
        assertThat(documentRepository.findById(id)).isPresent();

        // Delete from correct tenant
        documentRepository.deleteById(id);
        assertThat(documentRepository.findById(id)).isEmpty();
    }
}

@Table("secure_documents")
@Data
class TestDocument {
    @Id
    private Long id;

    @Tenant
    private UUID tenantId;

    private String title;
}

interface TestDocumentRepository extends SecureRepository<TestDocument, Long> {}

@Repository
class TestDocumentRepositoryImpl extends SecureRepositoryImpl<TestDocument, Long> {
    public TestDocumentRepositoryImpl(SecureRepositoryContext context) {
        super(context, TestDocument.class);
    }
}

class TestSecurityContext implements SecurityContext {
    private final UUID tenantId;

    TestSecurityContext(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public UUID getUserId() {
        return UUID.randomUUID();
    }

    @Override
    public UUID getTenantId() {
        return tenantId;
    }

    @Override
    public Set<String> getRoles() {
        return new HashSet<>();
    }

    @Override
    public Set<String> getPermissions() {
        return new HashSet<>();
    }
}

@Component
class TestSecurityContextHolder implements SecurityContextProvider {
    private SecurityContext currentContext;

    public void setCurrentContext(SecurityContext context) {
        this.currentContext = context;
    }

    @Override
    public SecurityContext getCurrentContext() {
        return currentContext;
    }
}
