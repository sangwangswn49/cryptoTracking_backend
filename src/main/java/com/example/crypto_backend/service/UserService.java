package com.example.crypto_backend.service;

import com.example.crypto_backend.model.User;
import com.example.crypto_backend.repository.UserRepo;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    private boolean isBCryptEncoded(String password) {
        return password != null && password.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    }

    public User createUser(User user) {
        try {
            if (!isBCryptEncoded(user.getPassWord())) {
                user.setPassWord(passwordEncoder.encode(user.getPassWord()));
            }
            return userRepo.save(user);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("User with the same username already exists", e);
        } catch (MongoWriteException e) {
            throw new RuntimeException("Error writing to the database", e);
        }
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    public User getUserByUserName(String userName) {
        return userRepo.getUserByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userName));
    }

    public User getUserByUserNameSimply(String userName) {
        return userRepo.getUserByUserName(userName).orElse(null);
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = getUserByUserName(userName);
        String authority = user.getRole().startsWith("ROLE_") ? user.getRole() : "ROLE_" + user.getRole();
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority);

        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassWord(),
                List.of(grantedAuthority)
        );
    }
}