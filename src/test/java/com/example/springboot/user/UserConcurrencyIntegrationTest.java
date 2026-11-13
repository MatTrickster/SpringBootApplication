package com.example.springboot.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.springboot.dto.UserRequest;
import com.example.springboot.entity.Role;
import com.example.springboot.exception.DuplicateResourceException;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;

/**
 * Many threads create the SAME username at the same moment. Whatever the interleaving,
 * exactly one must win and exactly one row must exist. Losers fail either at the
 * service-level existsBy check (DuplicateResourceException) or, when they raced past it,
 * at the DB unique constraint (a DataAccessException such as DataIntegrityViolationException).
 *
 * No @Transactional on the test: each thread needs its own real, committed transaction.
 * Because the data really commits, @AfterEach removes it so other tests keep their seed data.
 */
@SpringBootTest
class UserConcurrencyIntegrationTest {

    private static final String USERNAME = "racer";
    private static final int THREADS = 8;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
    }

    @RepeatedTest(3)
    void concurrentCreatesOfSameUsername_exactlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                String email = USERNAME + i + "@example.com"; // only the username clashes
                futures.add(pool.submit(() -> {
                    start.await();
                    return userService.create(new UserRequest(USERNAME, email, Role.USER));
                }));
            }
            start.countDown();

            int successes = 0;
            List<Throwable> failures = new ArrayList<>();
            for (Future<?> f : futures) {
                try {
                    f.get(10, TimeUnit.SECONDS);
                    successes++;
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }

            assertThat(successes).isEqualTo(1);
            assertThat(failures).hasSize(THREADS - 1)
                    .allSatisfy(t -> assertThat(t).isInstanceOfAny(
                            DuplicateResourceException.class, DataAccessException.class));
            assertThat(userRepository.findAll()).filteredOn(u -> USERNAME.equals(u.getUsername())).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
