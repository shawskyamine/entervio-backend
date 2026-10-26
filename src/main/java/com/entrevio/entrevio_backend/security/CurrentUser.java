package com.entrevio.entrevio_backend.security;

import com.entrevio.entrevio_backend.exception.ValidationException;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.model.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new ValidationException("Not authenticated");
        }
        return u;
    }

    public static User requireRole(Role role) {
        User u = get();
        if (u.getRole() != role) {
            throw new ValidationException("Requires role " + role);
        }
        return u;
    }
}