package com.uploadfiles.uploadingfiles.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uploadfiles.uploadingfiles.entity.User;


public interface UserRepository extends JpaRepository<User, Integer> {

}
