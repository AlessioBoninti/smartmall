package it.smartmall.service;

import it.smartmall.dto.AdminBookingDTO;
import it.smartmall.dto.AdminStoreDTO;
import it.smartmall.dto.AdminUserDTO;
import it.smartmall.dto.RoleChangeRequestDTO;
import it.smartmall.dto.SuspendStoreRequest;
import it.smartmall.dto.UpdateUserRoleRequest;
import it.smartmall.exception.InvalidStoreSuspensionException;
import it.smartmall.exception.RoleChangeNotAllowedException;
import it.smartmall.exception.RoleChangeRequestException;
import it.smartmall.exception.StoreNotFoundException;
import it.smartmall.exception.UserNotFoundException;
import it.smartmall.model.Booking;
import it.smartmall.model.Role;
import it.smartmall.model.RoleChangeRequest;
import it.smartmall.model.RoleChangeRequestStatus;
import it.smartmall.model.Store;
import it.smartmall.model.StoreStatus;
import it.smartmall.model.User;
import it.smartmall.repository.AvailabilityRuleRepository;
import it.smartmall.repository.BookingRepository;
import it.smartmall.repository.RoleChangeRequestRepository;
import it.smartmall.repository.StoreRepository;
import it.smartmall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final BookingRepository bookingRepository;
    private final RoleChangeRequestRepository roleChangeRequestRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;

    public List<AdminUserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toAdminUserDTO)
                .toList();
    }

    @Transactional
    public void deleteUser(Long id, User currentUser) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new RoleChangeNotAllowedException("Non puoi eliminare il tuo stesso account");
        }

        List<Store> ownedStores = storeRepository.findByMerchantId(targetUser.getId());
        for (Store store : ownedStores) {
            bookingRepository.deleteByStoreId(store.getId());
            availabilityRuleRepository.deleteByStoreId(store.getId());
        }

        storeRepository.deleteAll(ownedStores);
        bookingRepository.deleteByCustomerId(targetUser.getId());
        roleChangeRequestRepository.deleteByRequesterId(targetUser.getId());
        roleChangeRequestRepository.clearReviewedById(targetUser.getId());
        userRepository.delete(targetUser);
    }

    public AdminUserDTO updateUserRole(Long id, UpdateUserRoleRequest request, User currentUser) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato"));

        if (targetUser.getId().equals(currentUser.getId())) {
            throw new RoleChangeNotAllowedException("Non puoi cambiare il tuo stesso ruolo");
        }

        if (request.getRole() == Role.SUPER_ADMIN) {
            throw new RoleChangeNotAllowedException("La promozione a super admin non è consentita in questa demo");
        }

        targetUser.setRole(request.getRole());
        return toAdminUserDTO(userRepository.save(targetUser));
    }

    public List<RoleChangeRequestDTO> getRoleChangeRequests() {
        return roleChangeRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toRoleChangeRequestDTO)
                .toList();
    }

    @Transactional
    public RoleChangeRequestDTO approveRoleChangeRequest(Long id, User currentUser) {
        RoleChangeRequest request = findRoleChangeRequest(id);

        if (request.getStatus() != RoleChangeRequestStatus.PENDING) {
            throw new RoleChangeRequestException("Questa richiesta è già stata valutata");
        }

        if (request.getRequestedRole() == Role.SUPER_ADMIN) {
            throw new RoleChangeRequestException("Le richieste non possono assegnare il ruolo super admin");
        }

        User requester = request.getRequester();
        requester.setRole(request.getRequestedRole());
        userRepository.save(requester);

        request.setStatus(RoleChangeRequestStatus.APPROVED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(currentUser);

        return toRoleChangeRequestDTO(roleChangeRequestRepository.save(request));
    }

    public RoleChangeRequestDTO rejectRoleChangeRequest(Long id, User currentUser) {
        RoleChangeRequest request = findRoleChangeRequest(id);

        if (request.getStatus() != RoleChangeRequestStatus.PENDING) {
            throw new RoleChangeRequestException("Questa richiesta è già stata valutata");
        }

        request.setStatus(RoleChangeRequestStatus.REJECTED);
        request.setReviewedAt(LocalDateTime.now());
        request.setReviewedBy(currentUser);

        return toRoleChangeRequestDTO(roleChangeRequestRepository.save(request));
    }

    public List<AdminStoreDTO> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(this::toAdminStoreDTO)
                .toList();
    }

    public AdminStoreDTO suspendStore(Long id, SuspendStoreRequest request) {
        Store store = findStore(id);

        if (request.getTo().isBefore(request.getFrom()) || request.getTo().isEqual(request.getFrom())) {
            throw new InvalidStoreSuspensionException("La data di fine sospensione deve essere successiva alla data di inizio");
        }

        store.setStatus(StoreStatus.SUSPENDED);
        store.setSuspendedFrom(request.getFrom());
        store.setSuspendedTo(request.getTo());
        store.setSuspendedReason(request.getReason());

        return toAdminStoreDTO(storeRepository.save(store));
    }

    public AdminStoreDTO unsuspendStore(Long id) {
        Store store = findStore(id);
        makeStoreActive(store);
        return toAdminStoreDTO(storeRepository.save(store));
    }

    public AdminStoreDTO closeStore(Long id) {
        Store store = findStore(id);
        store.setStatus(StoreStatus.CLOSED);
        clearSuspension(store);
        return toAdminStoreDTO(storeRepository.save(store));
    }

    public AdminStoreDTO activateStore(Long id) {
        Store store = findStore(id);
        makeStoreActive(store);
        return toAdminStoreDTO(storeRepository.save(store));
    }

    public List<AdminBookingDTO> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::toAdminBookingDTO)
                .toList();
    }

    private RoleChangeRequest findRoleChangeRequest(Long id) {
        return roleChangeRequestRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Richiesta cambio ruolo non trovata"));
    }

    private Store findStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new StoreNotFoundException("Store non trovato"));
    }

    private void makeStoreActive(Store store) {
        store.setStatus(StoreStatus.ACTIVE);
        clearSuspension(store);
    }

    private void clearSuspension(Store store) {
        store.setSuspendedFrom(null);
        store.setSuspendedTo(null);
        store.setSuspendedReason(null);
    }

    private AdminUserDTO toAdminUserDTO(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        return dto;
    }

    private AdminStoreDTO toAdminStoreDTO(Store store) {
        AdminStoreDTO dto = new AdminStoreDTO();
        dto.setId(store.getId());
        dto.setName(store.getName());
        dto.setStatus(store.getStatus().name());
        dto.setMerchantId(store.getMerchant().getId());
        dto.setMerchantEmail(store.getMerchant().getEmail());
        dto.setSuspendedFrom(store.getSuspendedFrom());
        dto.setSuspendedTo(store.getSuspendedTo());
        dto.setSuspendedReason(store.getSuspendedReason());
        return dto;
    }

    private AdminBookingDTO toAdminBookingDTO(Booking booking) {
        AdminBookingDTO dto = new AdminBookingDTO();
        dto.setId(booking.getId());
        dto.setStoreId(booking.getStore().getId());
        dto.setStoreName(booking.getStore().getName());
        dto.setCustomerId(booking.getCustomer().getId());
        dto.setCustomerEmail(booking.getCustomer().getEmail());
        dto.setStartDateTime(booking.getStartDateTime());
        dto.setEndDateTime(booking.getEndDateTime());
        dto.setStatus(booking.getStatus().name());
        return dto;
    }

    private RoleChangeRequestDTO toRoleChangeRequestDTO(RoleChangeRequest request) {
        RoleChangeRequestDTO dto = new RoleChangeRequestDTO();
        dto.setId(request.getId());
        dto.setRequesterId(request.getRequester().getId());
        dto.setRequesterEmail(request.getRequester().getEmail());
        dto.setRequestedRole(request.getRequestedRole().name());
        dto.setStatus(request.getStatus().name());
        dto.setReason(request.getReason());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setReviewedAt(request.getReviewedAt());

        if (request.getReviewedBy() != null) {
            dto.setReviewedById(request.getReviewedBy().getId());
            dto.setReviewedByEmail(request.getReviewedBy().getEmail());
        }

        return dto;
    }
}
