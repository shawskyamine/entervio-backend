package com.entrevio.entrevio_backend.service;

import com.entrevio.entrevio_backend.dto.user.UserDto;
import com.entrevio.entrevio_backend.exception.NotFoundException;
import com.entrevio.entrevio_backend.model.User;
import com.entrevio.entrevio_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserDto.from(u);
    }
}