package it.smartmall.config;

import it.smartmall.model.*;
import it.smartmall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Se la tabella utenti è vuota, inseriamo i dati di test
        if (userRepository.count() == 0) {

           
            User customer = new User();
            customer.setEmail("customer@test.com");
            customer.setPassword(passwordEncoder.encode("password123"));
            customer.setRole(Role.CUSTOMER);
            userRepository.save(customer);

            
            User merchant = new User();
            merchant.setEmail("merchant@test.com");
            merchant.setPassword(passwordEncoder.encode("password123"));
            merchant.setRole(Role.MERCHANT);
            userRepository.save(merchant);

           
            Store store = new Store();
            store.setName("Apple Store");
            store.setMerchant(merchant);
            storeRepository.save(store);

           
            AvailabilityRule rule = new AvailabilityRule();
            rule.setStore(store);
            rule.setDayOfWeek(6); // 6 = Sabato
            rule.setStartTime(LocalTime.of(9, 0));  // Apre alle 09:00
            rule.setEndTime(LocalTime.of(18, 0));   // Chiude alle 18:00
            rule.setSlotMinutes(30);                // Slot ogni mezz'ora
            rule.setCapacityPerSlot(2);             // solo 2 posti
            rule.setActive(true);
            ruleRepository.save(rule);

            System.out.println("Dati di test inseriti con successo nel Database!");
        }
    }
}