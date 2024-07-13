package com.eventManagement.EMS.service;

import com.eventManagement.EMS.models.User;
import com.eventManagement.EMS.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {


    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    NotificationService notificationService;

    public User findByUsername(String username){
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username " + username));
    }

    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users = userRepository.findAll();
        if(users.isEmpty()){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else{
            return new ResponseEntity<>(users, HttpStatus.OK);
        }
    }

    public ResponseEntity<String> register(User user){
        if(userRepository.findByUsername(user.getUsername()).isPresent()){
            return new ResponseEntity<>("Username already exists", HttpStatus.CONFLICT);
        }else if(userRepository.findByEmail(user.getEmail()).isPresent()){
            return new ResponseEntity<>("Email already exists", HttpStatus.CONFLICT);
        }else if(userRepository.findBySchoolID(user.getSchoolID()).isPresent()){
            return new ResponseEntity<>("School ID already exists", HttpStatus.CONFLICT);
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);

        user.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, MMM dd yyyy")));
        userRepository.save(user);
        return new ResponseEntity<>("User Registered Successfully", HttpStatus.CREATED);
    }

    public ResponseEntity<User> login(String username, String password, HttpServletRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        User user = userOptional.get();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));

            if (authentication != null) {
                // Store authentication in the session
                SecurityContextHolder.getContext().setAuthentication(authentication);
                HttpSession session = request.getSession(true); // true to create a new session if it doesn't exist
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
                return new ResponseEntity<>(user, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    //Profile Management for users
    public ResponseEntity<String> updateProfile(Long userId, User updatedUser) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();

            if(updatedUser.getUsername() != null && updatedUser.getUsername() != " "){
                //Checks if the updated username is equal to the existing data in the database
                if(updatedUser.getUsername().equals(existingUser.getUsername())){
                    existingUser.setUsername(updatedUser.getUsername());
                }else{
                    existingUser.setUsername(existingUser.getUsername());
                }
            }
            existingUser.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : existingUser.getEmail());
            existingUser.setYear(updatedUser.getYear() != null ? updatedUser.getYear() : existingUser.getYear());
            existingUser.setCourse(updatedUser.getCourse() != null ? updatedUser.getCourse() : existingUser.getCourse());
            existingUser.setDepartment(updatedUser.getDepartment() != null ? updatedUser.getDepartment() : existingUser.getDepartment());
            if (updatedUser.getPassword() != null && updatedUser.getPassword() != "") {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            existingUser.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("E MMM dd yyyy")));
            userRepository.save(existingUser);
            String message = "Your profile has been updated!";
            notificationService.regularNotification(existingUser, message);
            return new ResponseEntity<>("User profile updated successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }
    //Admin can update users' profile
    public ResponseEntity<String> updateUser(Long userId, User updatedUser) {
        Optional<User> userOptional = userRepository.findById(userId);

        if (userOptional.isPresent()) {
            User existingUser = userOptional.get();

            if(updatedUser.getUsername() != null && updatedUser.getUsername() != " "){
                //Checks if the updated username is equal to the existing data in the database
                if(updatedUser.getUsername().equals(existingUser.getUsername())){
                    existingUser.setUsername(updatedUser.getUsername());
                }else{
                    existingUser.setUsername(existingUser.getUsername());
                }
            }
            existingUser.setEmail(updatedUser.getEmail() != null ? updatedUser.getEmail() : existingUser.getEmail());
            existingUser.setFirstName(updatedUser.getFirstName() != null ? updatedUser.getFirstName() : existingUser.getFirstName());
            existingUser.setLastName(updatedUser.getLastName() != null ? updatedUser.getLastName() : existingUser.getLastName());
            existingUser.setUserType(updatedUser.getUserType() != null ? updatedUser.getUserType() : existingUser.getUserType());
            existingUser.setYear(updatedUser.getYear() != null ? updatedUser.getYear() : existingUser.getYear());
            existingUser.setCourse(updatedUser.getCourse() != null ? updatedUser.getCourse() : existingUser.getCourse());
            existingUser.setDepartment(updatedUser.getDepartment() != null ? updatedUser.getDepartment() : existingUser.getDepartment());
            existingUser.setRole(updatedUser.getRole() != null ? updatedUser.getRole() : existingUser.getRole() );
            existingUser.setSchoolID(updatedUser.getSchoolID() != null ? updatedUser.getSchoolID() : existingUser.getSchoolID());

            if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(updatedUser.getPassword()));
            }

            existingUser.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("E, MMM dd yyyy")));
            userRepository.save(existingUser);
            return new ResponseEntity<>("User has been updated successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<User> getCurrentUser(Long userId){
        Optional<User> userOptional = userRepository.findById(userId);

        if(userOptional.isPresent()){
            User user = userOptional.get();
            return new ResponseEntity<>(user, HttpStatus.OK);
        }else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    public ResponseEntity<String> deactivateAccount(User user){
        user.setActive(false);
        userRepository.save(user);
        return new ResponseEntity<>("Account deactivated successfully", HttpStatus.OK);
    }


}
