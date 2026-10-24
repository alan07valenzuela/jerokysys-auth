package com.jerokysys.auth.repo;

import com.jerokysys.auth.domain.User;
import com.jerokysys.commons.repository.BaseRepository;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User, Long> {

    Optional<User> findByUsernameIgnoreCase(String username);
}
