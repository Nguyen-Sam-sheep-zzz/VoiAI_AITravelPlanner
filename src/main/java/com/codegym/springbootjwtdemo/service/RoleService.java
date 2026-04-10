package com.codegym.springbootjwtdemo.service;

import com.codegym.springbootjwtdemo.model.Role;
import com.codegym.springbootjwtdemo.repository.IRoleRepository;
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
