package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {
        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response

        // checking if user associated with given payload already exists in database
        if (userRepository.existsByName(payload.getName()) && userRepository.existsByEmail(payload.getEmail())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Sting.format("User with name: %s and email: %s already exists in database", payload.getName(), payload.getEmail()));
        }
        
        // create a new user entity and update its name and email
        User user = new User();
        user.setName(payload.name);
        user.setEmail(payload.email);

        // save the user to the database
        User savedUser = userRepository.save(user);

        return new ResponseEntity<>(savedUser.getId(), HttpStatus.OK);
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {
        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate

        // delete user with given id
        if (userRepository.existsById(userId)){
            userRepository.delelteById(userId);
            return new ResponseEntity<>("User deleted.", HttpStatus.OK);
        } else {
            return new ResponseEntity<>(String.format("User id: %d does not exist.", userId), HttpStatus.BAD_REQUEST);
        }
    }
}
