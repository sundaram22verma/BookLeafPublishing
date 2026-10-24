package com.bookleaf.support.config;

import com.bookleaf.support.entity.Book;
import com.bookleaf.support.entity.Role;
import com.bookleaf.support.entity.User;
import com.bookleaf.support.repository.BookRepository;
import com.bookleaf.support.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Seeds an admin plus the authors/books from bookleaf_sample_data.json on first boot (only when the
 * users table is empty). The JSON reader is deliberately tolerant: key names are matched ignoring case,
 * underscores and dashes (royalty_paid == royaltyPaid), and books may be a top-level array or nested
 * inside each author.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final BookRepository books;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;

    @Value("${bookleaf.seed.enabled}") private boolean enabled;
    @Value("${bookleaf.seed.admin-email}") private String adminEmail;
    @Value("${bookleaf.seed.admin-password}") private String adminPassword;
    @Value("${bookleaf.seed.author-password}") private String authorPassword;
    @Value("classpath:bookleaf_sample_data.json") private Resource dataFile;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"), DateTimeFormatter.ofPattern("MM/dd/yyyy"));

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled || users.count() > 0) return;
        requireSeedPassword("SEED_ADMIN_PASSWORD", adminPassword);
        if (dataFile.exists()) requireSeedPassword("SEED_AUTHOR_PASSWORD", authorPassword);

        users.save(User.builder().name("BookLeaf Support").email(adminEmail)
                .passwordHash(encoder.encode(adminPassword)).role(Role.ADMIN).build());
        log.info("Seeded admin {}", adminEmail);

        if (!dataFile.exists()) {
            log.warn("bookleaf_sample_data.json not found on classpath; only the admin was seeded");
            return;
        }
        try (InputStream in = dataFile.getInputStream()) {
            JsonNode root = mapper.readTree(in);
            JsonNode authorsNode = root.isArray() ? root : field(root, "authors");
            JsonNode booksNode = root.isArray() ? null : field(root, "books");
            if (authorsNode == null || !authorsNode.isArray()) {
                log.warn("No 'authors' array found in sample data; nothing seeded");
                return;
            }

            Map<String, User> lookup = new HashMap<>();
            int bookCount = 0;
            for (JsonNode a : authorsNode) {
                String name = text(a, "name", "authorname", "fullname");
                String email = text(a, "email", "authoremail");
                if (name == null) name = "Author";
                if (email == null) email = name.toLowerCase().replaceAll("[^a-z0-9]+", ".") + "@example.com";
                User author = users.save(User.builder().name(name).email(email.toLowerCase())
                        .passwordHash(encoder.encode(authorPassword)).role(Role.AUTHOR).build());
                String key = text(a, "id", "authorid", "_id");
                if (key != null) lookup.put(key, author);
                lookup.put(author.getEmail(), author);

                JsonNode nested = field(a, "books");
                if (nested != null && nested.isArray()) {
                    for (JsonNode b : nested) { saveBook(b, author); bookCount++; }
                }
            }
            if (booksNode != null && booksNode.isArray()) {
                for (JsonNode b : booksNode) {
                    String ak = text(b, "authorid", "author");
                    String ae = text(b, "authoremail");
                    User owner = ak != null ? lookup.get(ak) : null;
                    if (owner == null && ae != null) owner = lookup.get(ae.toLowerCase());
                    if (owner == null) { log.warn("Skipping book with unknown author reference: {}", ak != null ? ak : ae); continue; }
                    saveBook(b, owner);
                    bookCount++;
                }
            }
            log.info("Seeded {} authors and {} books (author password: configured via SEED_AUTHOR_PASSWORD)",
                    authorsNode.size(), bookCount);
        }
    }

    private static void requireSeedPassword(String variable, String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalStateException(variable + " must be configured with at least 12 characters when seeding is enabled");
        }
    }

    private void saveBook(JsonNode b, User author) {
        BigDecimal earned = decimal(b, "totalroyaltyearned", "royaltyearned");
        BigDecimal paid = decimal(b, "royaltypaid");
        BigDecimal pending = decimal(b, "royaltypending");
        if (pending == null && earned != null && paid != null) pending = earned.subtract(paid);
        books.save(Book.builder().author(author)
                .title(orDefault(text(b, "title", "booktitle", "name"), "Untitled"))
                .isbn(text(b, "isbn"))
                .genre(text(b, "genre"))
                .publicationDate(date(text(b, "publicationdate", "publisheddate", "publishdate")))
                .status(text(b, "status", "bookstatus"))
                .mrp(decimal(b, "mrp", "price"))
                .totalCopiesSold(intValue(b, "totalcopiessold", "copiessold"))
                .totalRoyaltyEarned(earned).royaltyPaid(paid).royaltyPending(pending)
                .build());
    }

    // ---- tolerant JSON helpers ----
    private static String norm(String k) { return k.toLowerCase().replace("_", "").replace("-", ""); }

    private static JsonNode field(JsonNode node, String... names) {
        if (node == null || !node.isObject()) return null;
        for (String wanted : names) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (norm(e.getKey()).equals(wanted) && !e.getValue().isNull()) return e.getValue();
            }
        }
        return null;
    }

    private static String text(JsonNode node, String... names) {
        JsonNode n = field(node, names);
        return (n == null || n.isContainerNode() || n.asText().isBlank()) ? null : n.asText().trim();
    }

    private static BigDecimal decimal(JsonNode node, String... names) {
        String t = text(node, names);
        if (t == null) return null;
        String cleaned = t.replaceAll("[^0-9.\\-]", "");
        return cleaned.isEmpty() ? null : new BigDecimal(cleaned);
    }

    private static Integer intValue(JsonNode node, String... names) {
        BigDecimal d = decimal(node, names);
        return d == null ? null : d.intValue();
    }

    private static LocalDate date(String s) {
        if (s == null) return null;
        String head = s.length() > 10 && s.charAt(10) == 'T' ? s.substring(0, 10) : s;
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(head, f); } catch (Exception ignored) { /* try next */ }
        }
        return null;
    }

    private static String orDefault(String v, String d) { return v == null ? d : v; }
}
