package io.preboot.securedata;

import static org.assertj.core.api.Assertions.assertThat;

import io.preboot.securedata.annotation.Tenant;
import io.preboot.securedata.config.TestContainersConfig;
import io.preboot.securedata.event.SecureRepositoryEvent;
import io.preboot.securedata.repository.SecureRepository;
import io.preboot.securedata.repository.SecureRepositoryContext;
import io.preboot.securedata.repository.SecureRepositoryImpl;
import java.util.UUID;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import({TestSecurityConfig.class, TestContainersConfig.class, SecureRepositoryEventTest.TestEventConfig.class})
@Transactional
@Sql({"/secure-test-data.sql"})
class SecureRepositoryEventTest {

    @Autowired
    private TestDocumentRepository documentRepository;

    @Autowired
    private TestNoteRepository noteRepository;

    @Autowired
    private TestSecurityContextHolder securityContextHolder;

    @Autowired
    private DocumentEventCollector documentEventCollector;

    @Autowired
    private NoteEventCollector noteEventCollector;

    private static final UUID TENANT_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        securityContextHolder.setCurrentContext(new TestSecurityContext(TENANT_1));
        documentEventCollector.clear();
        noteEventCollector.clear();
    }

    @Test
    void documentEvents_ShouldOnlyTriggerDocumentHandlers() {
        // Arrange
        TestDocument document = new TestDocument();
        document.setTitle("Event Test Document");

        // Act
        TestDocument saved = documentRepository.save(document);

        // Assert
        assertThat(documentEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeCreateEvent.class, SecureRepositoryEvent.AfterCreateEvent.class);

        // Verify note handler wasn't called
        assertThat(noteEventCollector.getEvents()).isEmpty();

        // Verify event content
        var beforeEvent = (SecureRepositoryEvent.BeforeCreateEvent<TestDocument>)
                documentEventCollector.getEvents().get(0);
        var afterEvent = (SecureRepositoryEvent.AfterCreateEvent<TestDocument>)
                documentEventCollector.getEvents().get(1);

        assertThat(beforeEvent.getEntity().getTitle()).isEqualTo("Event Test Document");
        assertThat(afterEvent.getEntity().getId()).isEqualTo(saved.getId());
        assertThat(afterEvent.getEntity().getTenantId()).isEqualTo(TENANT_1);
    }

    @Test
    void noteEvents_ShouldOnlyTriggerNoteHandlers() {
        // Arrange
        TestNote note = new TestNote();
        note.setContent("Test Note Content");

        // Act
        TestNote saved = noteRepository.save(note);

        // Assert
        assertThat(noteEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeCreateEvent.class, SecureRepositoryEvent.AfterCreateEvent.class);

        // Verify document handler wasn't called
        assertThat(documentEventCollector.getEvents()).isEmpty();
    }

    @Test
    void updateEvents_ShouldTriggerCorrectHandlers() {
        // Arrange
        TestDocument document = new TestDocument();
        document.setTitle("Original Title");
        TestDocument savedDoc = documentRepository.save(document);
        documentEventCollector.clear();

        TestNote note = new TestNote();
        note.setContent("Original Content");
        TestNote savedNote = noteRepository.save(note);
        noteEventCollector.clear();

        // Act
        savedDoc.setTitle("Updated Title");
        documentRepository.save(savedDoc);

        savedNote.setContent("Updated Content");
        noteRepository.save(savedNote);

        // Assert
        assertThat(documentEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeUpdateEvent.class, SecureRepositoryEvent.AfterUpdateEvent.class);

        assertThat(noteEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeUpdateEvent.class, SecureRepositoryEvent.AfterUpdateEvent.class);
    }

    @Test
    void deleteEvents_ShouldTriggerCorrectHandlers() {
        // Arrange
        TestDocument document = new TestDocument();
        document.setTitle("To Be Deleted");
        TestDocument savedDoc = documentRepository.save(document);
        documentEventCollector.clear();

        TestNote note = new TestNote();
        note.setContent("To Be Deleted");
        TestNote savedNote = noteRepository.save(note);
        noteEventCollector.clear();

        // Act
        documentRepository.delete(savedDoc);
        noteRepository.delete(savedNote);

        // Assert
        assertThat(documentEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeDeleteEvent.class, SecureRepositoryEvent.AfterDeleteEvent.class);

        assertThat(noteEventCollector.getEvents())
                .hasSize(2)
                .extracting("class")
                .containsExactly(
                        SecureRepositoryEvent.BeforeDeleteEvent.class, SecureRepositoryEvent.AfterDeleteEvent.class);
    }

    @TestConfiguration
    static class TestEventConfig {
        @Bean
        DocumentEventCollector documentEventCollector() {
            return new DocumentEventCollector();
        }

        @Bean
        NoteEventCollector noteEventCollector() {
            return new NoteEventCollector();
        }
    }
}

@Table("secure_notes")
@Data
class TestNote {
    @Id
    private Long id;

    @Tenant
    private UUID tenantId;

    private String content;
}

interface TestNoteRepository extends SecureRepository<TestNote, Long> {}

@Repository
class TestNoteRepositoryImpl extends SecureRepositoryImpl<TestNote, Long> {
    public TestNoteRepositoryImpl(SecureRepositoryContext context) {
        super(context, TestNote.class);
    }
}
