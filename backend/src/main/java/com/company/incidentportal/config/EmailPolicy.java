package com.company.incidentportal.config;

import com.company.incidentportal.exception.InvalidInputException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates user email addresses. When app.email.allowed-domain is set (comma separated for
 * several domains, e.g. "aurum.com"), only addresses on those domains are accepted.
 */
@Component
public class EmailPolicy {

    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    private final List<String> allowedDomains;

    public EmailPolicy(@Value("${app.email.allowed-domain:}") String allowedDomain) {
        this.allowedDomains = Arrays.stream(allowedDomain.split(","))
                .map(d -> d.trim().toLowerCase(Locale.ROOT))
                .map(d -> d.startsWith("@") ? d.substring(1) : d)
                .filter(d -> !d.isEmpty())
                .toList();
    }

    /** Returns the normalised (trimmed, lower-case) email, or null when none was given. */
    public String validate(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(normalized).matches()) {
            throw new InvalidInputException("Invalid email address");
        }
        String domain = normalized.substring(normalized.indexOf('@') + 1);
        if (!allowedDomains.isEmpty() && !allowedDomains.contains(domain)) {
            throw new InvalidInputException(
                    "Email must use the company domain: @" + String.join(", @", allowedDomains));
        }
        return normalized;
    }
}
