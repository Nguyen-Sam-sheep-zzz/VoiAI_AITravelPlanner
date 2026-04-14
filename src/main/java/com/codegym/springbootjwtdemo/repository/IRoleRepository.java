package com.codegym.springbootjwtdemo.repository;

import com.codegym.springbootjwtdemo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IRoleRepository extends JpaRepository<Role, Long> {
    List<Role> findAll();

    Optional<Role> findByName(String name);
}
