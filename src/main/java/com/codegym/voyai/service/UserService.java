package com.codegym.voyai.service;

import com.codegym.voyai.model.User;
import com.codegym.voyai.model.UserPrinciple;
import com.codegym.voyai.model.dto.UserDTO;
import com.codegym.voyai.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private IUserRepository iUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<UserDTO> findAll() {
        List<UserDTO> userDTOS = new ArrayList<>();
        for (User u : iUserRepository.findAll()) {
            userDTOS.add(toDTO(u));
        }
        return userDTOS;
    }

    public UserDTO findById(Long id) {
        Optional<User> user = iUserRepository.findById(id);
        return user.map(this::toDTO).orElse(null);
    }

    public User findByEmail(String email) {
        return iUserRepository.findByEmail(email).orElse(null);
    }

    public boolean add(User user) {
        if (iUserRepository.findByEmail(user.getEmail()).isPresent()) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        iUserRepository.save(user);
        return true;
    }


    public void delete(Long id) {
        iUserRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = iUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return UserPrinciple.build(user);
    }

    public UserDTO toDTO(User user) {
        return new UserDTO(user.getId(), user.getFullName(), user.getRoles());
    }

    public boolean existsByEmail(String email) {
        return iUserRepository.findByEmail(email).isPresent();
    }
}
