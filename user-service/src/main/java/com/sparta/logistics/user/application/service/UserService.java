package com.sparta.logistics.user.application.service;

import com.sparta.logistics.user.domain.repository.UserRepository;
import com.sparta.logistics.user.presentation.dto.response.GetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


}
