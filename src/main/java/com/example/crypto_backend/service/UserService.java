package com.example.crypto_backend.service;

import com.example.crypto_backend.dto.request.UserSignupRequest;
import com.example.crypto_backend.dto.response.UserResponse;
import com.example.crypto_backend.entity.User;
import com.example.crypto_backend.repository.UserRepo;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import org.springframework.http.ResponseEntity;
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

    public User updateUser(User user) {
        try {
            User existingUser = getUserByUserNameSimply(user.getUserName());
            if (existingUser == null) {
                throw new UsernameNotFoundException("User not found with username: " + user.getUserName());
            }
            existingUser.setName(user.getName());
            existingUser.setRole(user.getRole());
            existingUser.setCoinList(user.getCoinList());
            existingUser.setAssets(user.getAssets());
            existingUser.setPassWord(user.getPassWord());
            if (!isBCryptEncoded(existingUser.getPassWord())) {
                existingUser.setPassWord(passwordEncoder.encode(user.getPassWord()));
            }
            return userRepo.save(existingUser);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("User with the same username already exists", e);
        } catch (MongoWriteException e) {
            throw new RuntimeException("Error writing to the database", e);
        }
    }

    public User signUpUser(UserSignupRequest user) {
        User newUser = new User();
        newUser.setUserName(user.getUserName());
        newUser.setPassWord(user.getPassWord());
        newUser.setName(user.getName());
        newUser.setRole("USER");
        try {
            if (!isBCryptEncoded(user.getPassWord())) {
                user.setPassWord(passwordEncoder.encode(newUser.getPassWord()));
            }
            return userRepo.save(newUser);
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("User with the same username already exists", e);
        } catch (MongoWriteException e) {
            throw new RuntimeException("Error writing to the database", e);
        }
    }

//    public List<User> getAllUsers() {
//        return userRepo.findAll();
//    }

    public UserResponse getUserResponseByUserName(String userName) {
        User user = getUserByUserNameSimply(userName);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + userName);
        }
        return new UserResponse(
                user.getUserName(),
                user.getName(),
                user.getRole(),
                user.getCoinList(),
                user.getAssets()
        );
    }

    public User getUserByUserName(String userName) {
        return userRepo.getUserByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + userName));
    }

    public User getUserByUserNameSimply(String userName) {
        return userRepo.getUserByUserName(userName).orElse(null);
    }

    public void deleteUserByUserName(String userName) {
        try{
            User existingUser = getUserByUserNameSimply(userName);
            if (existingUser == null) {
                System.out.println("User with username: " + userName + " does not exist.");
            }
            else {
                userRepo.delete(existingUser);
            }
        } catch (Exception e){
            System.out.println("An error occurred while deleting the user: " + e.getMessage());
        }
        System.out.println("User with username: " + userName + " has been deleted successfully.");
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