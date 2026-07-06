package it.smartmall.config;

import it.smartmall.model.AvailabilityRule;
import it.smartmall.model.Role;
import it.smartmall.model.Store;
import it.smartmall.model.StoreStatus;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.StoreRepository;
import it.smartmall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final AvailabilityRuleRepository ruleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing("customer@test.com", Role.CUSTOMER);
        User merchant = createUserIfMissing("merchant@test.com", Role.MERCHANT);
        createUserIfMissing("admin@test.com", Role.SUPER_ADMIN);

        Store store = createStoreIfMissing("Apple Store", merchant);
        createSaturdayAvailabilityRuleIfMissing(store);

        System.out.println("Dati di test verificati.");
    }

    private User createUserIfMissing(String email, Role role) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        return userRepository.save(user);
    }

    private Store createStoreIfMissing(String name, User merchant) {
        Optional<Store> existingStore = storeRepository.findByName(name);

        if (existingStore.isPresent()) {
            return existingStore.get();
        }

        Store store = new Store();
        store.setName(name);
        store.setMerchant(merchant);
        store.setStatus(StoreStatus.ACTIVE);
        return storeRepository.save(store);
    }

    private void createSaturdayAvailabilityRuleIfMissing(Store store) {
        boolean ruleAlreadyExists = !ruleRepository.findByStoreIdAndDayOfWeek(store.getId(), 6).isEmpty();

        if (ruleAlreadyExists) {
            return;
        }

        AvailabilityRule rule = new AvailabilityRule();
        rule.setStore(store);
        rule.setDayOfWeek(6);
        rule.setStartTime(LocalTime.of(9, 0));
        rule.setEndTime(LocalTime.of(18, 0));
        rule.setMorningStartTime(LocalTime.of(9, 0));
        rule.setMorningEndTime(LocalTime.of(13, 0));
        rule.setAfternoonStartTime(LocalTime.of(14, 0));
        rule.setAfternoonEndTime(LocalTime.of(18, 0));
        rule.setSlotMinutes(30);
        rule.setCapacityPerSlot(2);
        rule.setActive(true);
        ruleRepository.save(rule);
    }
}
