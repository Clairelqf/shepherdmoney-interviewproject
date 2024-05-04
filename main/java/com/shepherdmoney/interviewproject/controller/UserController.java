package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.UserRepository;

import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    @Autowired
    private UserRepository userRepository;


    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response
        User user = new User();
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());
        userRepository.save(user);
        return ResponseEntity.ok(user.getId());
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return ResponseEntity.ok("User deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("User with ID " + userId + " does not exist");
        }
    }
}
