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
    private com.codegym.voyai.repository.IRoleRepository roleRepository;

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

    public User processOAuthPostLogin(String email, String name, String avatarUrl) {
        Optional<User> existUser = iUserRepository.findByEmail(email);

        if (existUser.isPresent()) {
            User user = existUser.get();
            // Cập nhật avatar từ Google nếu user chưa có avatar hoặc avatar đã thay đổi
            boolean needUpdate = false;
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank() || !user.getAvatarUrl().equals(avatarUrl)) {
                    user.setAvatarUrl(avatarUrl);
                    needUpdate = true;
                }
            }
            if (name != null && !name.isBlank() && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(name);
                needUpdate = true;
            }
            if (needUpdate) {
                return iUserRepository.save(user);
            }
            return user;
        }

        // Tạo user mới nếu chưa tồn tại
        com.codegym.voyai.model.Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role ROLE_USER chưa có trong DB"));

        User newUser = User.builder()
                .email(email)
                .fullName(name)
                .avatarUrl(avatarUrl)
                .provider("google")
                .passwordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString())) // Mật khẩu ngẫu nhiên
                .roles(java.util.Set.of(userRole))
                .isActive(true)
                .build();

        return iUserRepository.save(newUser);
    }


    public void delete(Long id) {
        iUserRepository.deleteById(id);
    }

    public User updateProfile(Long id, String fullName, String avatarUrl) {
        java.util.Optional<User> userOpt = iUserRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName);
            }
            if (avatarUrl != null) {
                user.setAvatarUrl(avatarUrl);
            }
            return iUserRepository.save(user);
        }
        return null;
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
