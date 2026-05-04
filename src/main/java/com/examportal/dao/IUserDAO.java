package com.examportal.dao;

import com.examportal.model.User;

import java.util.List;
import java.util.Optional;

/** Contract for all User persistence operations. */
public interface IUserDAO {
    User            insert(User user);
    Optional<User>  findById(Long id);
    Optional<User>  findByEmail(String email);
    Optional<User>  findBySessionToken(String token);
    List<User>      findAllStudents();
    List<User>      findAll();
    void            updateSessionToken(Long userId, String token);
    void            updateLastLogin(Long userId);
    void            updatePassword(Long userId, String passwordHash);
    void            setActive(Long userId, boolean active);
    void            delete(Long userId);
}
