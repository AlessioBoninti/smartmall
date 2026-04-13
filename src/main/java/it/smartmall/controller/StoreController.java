package it.smartmall.controller;

import it.smartmall.dto.StoreDTO;
import it.smartmall.model.Store;
import it.smartmall.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreRepository storeRepository;

    @GetMapping
    public ResponseEntity<List<StoreDTO>> getAllStores() {

        List<Store> stores = storeRepository.findAll();

        // lista di Store convertita in una lista di StoreDTO
        List<StoreDTO> storeDTOs = stores.stream().map(store -> {
            StoreDTO dto = new StoreDTO();
            dto.setId(store.getId());
            dto.setName(store.getName());

            // Logica per capire se è sospeso in questo momento
            LocalDateTime now = LocalDateTime.now();
            boolean suspended = store.getSuspendedFrom() != null &&
                    store.getSuspendedTo() != null &&
                    now.isAfter(store.getSuspendedFrom()) &&
                    now.isBefore(store.getSuspendedTo());

            dto.setSuspended(suspended);
            if (suspended) {
                dto.setSuspendedReason(store.getSuspendedReason());
            }

            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(storeDTOs);
    }
}