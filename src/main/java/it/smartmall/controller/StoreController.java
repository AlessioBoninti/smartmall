package it.smartmall.controller;

import it.smartmall.dto.StoreDTO;
import it.smartmall.model.Store;
import it.smartmall.model.StoreStatus;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<List<StoreDTO>> getAllStores() {

        List<Store> stores = storeRepository.findByStatus(StoreStatus.ACTIVE);

        List<StoreDTO> storeDTOs = stores.stream().map(store -> {
            StoreDTO dto = new StoreDTO();
            dto.setId(store.getId());
            dto.setName(store.getName());
            dto.setStatus(store.getStatus().name());
            dto.setSuspendedReason(store.getSuspendedReason());
            return dto;
        }).toList();

        return ResponseEntity.ok(storeDTOs);
    }
}