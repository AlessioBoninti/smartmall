package it.smartmall.repository;

import it.smartmall.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Boot scriverà la query SQL per noi solo leggendo il nome di questo metodo!
    Optional<User> findByEmail(String email);
}