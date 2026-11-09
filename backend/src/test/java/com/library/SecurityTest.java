
package com.library;

import com.library.controller.AuthController;
import com.library.entity.LibraryUser;
import com.library.repository.UserRepository;
import com.library.security.SecurityConfig;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository users;

    @Test
    void csrfTokenIsPublicButSessionIsNot() throws Exception {

        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mutationsRequireCsrf() throws Exception {

        mvc.perform(post("/api/auth/login")
                        .param("username", "admin")
                        .param("password", "invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotManageBooks() throws Exception {

        LibraryUser user = new LibraryUser();
        user.setUsername("reader");
        user.setRole("CUSTOMER");

        when(users.findByUsername("reader"))
                .thenReturn(Optional.of(user));

        mvc.perform(post("/api/admin/books")
                        .with(user("reader").roles("CUSTOMER"))
                        .with(csrf())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatedAccountCannotUseExistingSession()
            throws Exception {

        LibraryUser user = new LibraryUser();
        user.setActive(false);

        when(users.findByUsername("reader"))
                .thenReturn(Optional.of(user));

        mvc.perform(get("/api/auth/me")
                        .with(user("reader").roles("CUSTOMER")))
                .andExpect(status().isUnauthorized());
    }
}
