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
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail("merchant@test.com").isEmpty()) {

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

            User admin = new User();
            admin.setEmail("admin@test.com");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole(Role.SUPER_ADMIN);
            userRepository.save(admin);

            Store store = new Store();
            store.setName("Apple Store");
            store.setMerchant(merchant);
            store.setStatus(StoreStatus.ACTIVE);
            storeRepository.save(store);

            AvailabilityRule rule = new AvailabilityRule();
            rule.setStore(store);
            rule.setDayOfWeek(6);
            rule.setStartTime(LocalTime.of(9, 0));
            rule.setEndTime(LocalTime.of(18, 0));
            rule.setSlotMinutes(30);
            rule.setCapacityPerSlot(2);
            rule.setActive(true);
            ruleRepository.save(rule);

            System.out.println("Dati di test (customer, merchant, admin, store) inseriti con successo!");
        } else {
            System.out.println("Dati di test già presenti");
        }
    }
}