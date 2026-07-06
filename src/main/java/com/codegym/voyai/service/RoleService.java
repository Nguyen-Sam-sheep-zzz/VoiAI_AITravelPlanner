package com.codegym.voyai.service;

import com.codegym.voyai.entity.Role;
import com.codegym.voyai.repository.IRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {
    @Autowired
    private IRoleRepository roleRepository;

    public List<Role> findAll() {
        return roleRepository.findAll();
    }

}
