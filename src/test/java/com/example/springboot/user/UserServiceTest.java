package com.example.springboot.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.springboot.config.AppProperties;
import com.example.springboot.dto.UserRequest;
import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;
import com.example.springboot.exception.DuplicateResourceException;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    private UserService service;

    @BeforeEach
    void setUp() {
        // AppProperties is an immutable record: just construct it, no mocking needed.
        AppProperties props = new AppProperties("test", false, 5, "s@example.com", "admin");
        service = new UserService(repository, props);
    }

    @Test
    void create_duplicateUsername_throwsAndNeverSaves() {
        given(repository.existsByUsername("admin")).willReturn(true);

        assertThatThrownBy(() -> service.create(new UserRequest("admin", "x@example.com", Role.USER)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("username 'admin'");
        verify(repository, never()).save(any());
    }

    @Test
    void create_checksEmailCaseInsensitively() {
        given(repository.existsByUsername("zed")).willReturn(false);
        given(repository.existsByEmailIgnoreCase("zed@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.create(new UserRequest("zed", "  ZED@Example.com ", Role.USER)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void findAll_clampsPageSizeAndNegativePage() {
        given(repository.findAll(any(Pageable.class))).willReturn(Page.empty());

        service.findAll(null, -3, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void findAll_withRole_usesRoleQuery() {
        given(repository.findByRole(any(), any())).willReturn(Page.<User>empty());

        service.findAll(Role.QA, 0, 2);

        verify(repository).findByRole(any(), any());
        verify(repository, never()).findAll(any(Pageable.class));
    }
}
