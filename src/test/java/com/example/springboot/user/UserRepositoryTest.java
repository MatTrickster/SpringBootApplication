package com.example.springboot.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;
import com.example.springboot.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Bypasses the service's existsBy check entirely: the DB constraint alone must refuse it.
     * saveAndFlush forces the INSERT now; with plain save() the error could surface later at flush.
     */
    @Test
    void uniqueUsernameIsEnforcedByTheDatabase() {
        repository.saveAndFlush(user("dup-user", "first@example.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(user("dup-user", "second@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void uniqueEmailIsEnforcedByTheDatabase() {
        repository.saveAndFlush(user("first-user", "same@example.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(user("second-user", "same@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void roleIndexExists() {
        @SuppressWarnings("unchecked")
        List<String> indexes = entityManager.createNativeQuery(
                        "SELECT index_name FROM information_schema.indexes WHERE table_name = 'USERS'")
                .getResultList();

        assertThat(indexes).contains("IDX_USERS_ROLE");
    }

    private static User user(String username, String email) {
        return User.builder().username(username).email(email).role(Role.USER).build();
    }
}
