package it.smartmall.config;

import it.smartmall.model.*;
import it.smartmall.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Profile("dev") //
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Se la tabella utenti è vuota, inseriamo i dati di test
        if (userRepository.findByEmail("merchant@test.com").isEmpty()) {

            //Creiamo un Utente (Customer) che ha ID = 1
            User customer = new User();
            customer.setEmail("customer@test.com");
            customer.setPassword(passwordEncoder.encode("password123"));
            customer.setRole(Role.CUSTOMER);
            userRepository.save(customer);

            //Creiamo un Merchant (ID = 2)
            User merchant = new User();
            merchant.setEmail("merchant@test.com");
            merchant.setPassword(passwordEncoder.encode("password123"));
            merchant.setRole(Role.MERCHANT);
            userRepository.save(merchant);

            //Creiamo un Negozio per questo Merchant (ID = 1)
            Store store = new Store();
            store.setName("Apple Store");
            store.setMerchant(merchant);
            storeRepository.save(store);

            //Creiamo la regola di orari per il SABATO (giorno 6)
            AvailabilityRule rule = new AvailabilityRule();
            rule.setStore(store);
            rule.setDayOfWeek(6); // 6 = Sabato
            rule.setStartTime(LocalTime.of(9, 0));  // Apre alle 09:00
            rule.setEndTime(LocalTime.of(18, 0));   // Chiude alle 18:00
            rule.setSlotMinutes(30);                // Slot ogni mezz'ora
            rule.setCapacityPerSlot(2);             // SOLO 2 POSTI! (Così testeremo l'overbooking)
            rule.setActive(true);
            ruleRepository.save(rule);

            System.out.println("Dati di test (Mario Rossi, Apple Store) inseriti con successo!");
        } else {
            System.out.println("Dati di test già presenti");
        }
    }
}