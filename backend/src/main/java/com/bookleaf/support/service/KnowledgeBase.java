package com.bookleaf.support.service;

import com.bookleaf.support.entity.TicketCategory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads knowledge-base.md once at startup and splits it by "## KEY" headings. Prompts then include only
 * the always-needed core (overview + tone) and the single section for the ticket's category, which is
 * the main token-cost lever for draft generation.
 */
@Component
public class KnowledgeBase {

    private final Map<String, String> sections = new HashMap<>();

    public KnowledgeBase(@Value("classpath:knowledge-base.md") Resource resource) throws IOException {
        String key = null;
        StringBuilder body = new StringBuilder();
        for (String line : resource.getContentAsString(StandardCharsets.UTF_8).split("\\R")) {
            if (line.startsWith("## ")) {
                if (key != null) sections.put(key, body.toString().trim());
                key = line.substring(3).trim();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        if (key != null) sections.put(key, body.toString().trim());
    }

    public String core() {
        return "Company overview: " + get("OVERVIEW") + "\n\nCommunication tone: " + get("TONE");
    }

    public String forCategory(TicketCategory category) {
        return get(category.name());
    }

    private String get(String key) {
        return sections.getOrDefault(key, "");
    }
}
