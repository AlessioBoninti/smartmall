package it.smartmall.service;

import it.smartmall.dto.UpdateUserRoleRequest;
import it.smartmall.exception.RoleChangeNotAllowedException;
import it.smartmall.model.Role;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.RoleChangeRequestRepository;
import it.smartmall.repository.StoreRepository;
import it.smartmall.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoleChangeRequestRepository roleChangeRequestRepository;

    @Mock
    private AvailabilityRuleRepository availabilityRuleRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                userRepository,
                storeRepository,
                bookingRepository,
                roleChangeRequestRepository,
                availabilityRuleRepository
        );
    }

    @Test
    void adminCannotPromoteUserToSuperAdmin() {
        User currentAdmin = user(1L, Role.SUPER_ADMIN);
        User targetUser = user(2L, Role.CUSTOMER);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(Role.SUPER_ADMIN);

        given(userRepository.findById(targetUser.getId())).willReturn(Optional.of(targetUser));

        assertThatThrownBy(() -> adminService.updateUserRole(targetUser.getId(), request, currentAdmin))
                .isInstanceOf(RoleChangeNotAllowedException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    private User user(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@test.com");
        user.setPassword("password");
        user.setRole(role);
        return user;
    }
}
