package com.codegym.voyai.service;

import com.codegym.voyai.model.DestinationCost;
import com.codegym.voyai.model.dto.DestinationCostDTO;
import com.codegym.voyai.model.dto.UserContributionRequest;
import com.codegym.voyai.repository.IDestinationCostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DestinationCostService {

    private final IDestinationCostRepository repository;

    // Trả về map category → costUsd để Gemini dùng estimate
    public Map<String, BigDecimal> getCostMapForDestination(String destinationName) {
        List<DestinationCost> costs = repository
                .findByDestinationNameIgnoreCase(destinationName);

        return costs.stream()
                .collect(Collectors.toMap(
                        DestinationCost::getCategory,
                        DestinationCost::getCostUsd
                ));
    }

    // Lấy toàn bộ chi phí 1 thành phố dạng list DTO — trả về frontend
    public List<DestinationCostDTO> getByDestination(String destinationName) {
        return repository.findByDestinationNameIgnoreCase(destinationName)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Gợi ý tên thành phố khi user gõ vào form
    public List<String> suggest(String keyword) {
        return repository.searchDestinationNames(keyword);
    }

    // Người dùng đóng góp chi phí thực tế sau chuyến đi
    @Transactional
    public String contribute(UserContributionRequest request) {
        Optional<DestinationCost> found = repository
                .findByDestinationNameIgnoreCaseAndCategory(
                        request.getDestinationName(),
                        request.getCategory());

        if (found.isEmpty()) {
            // Trả về thông báo rõ ràng thay vì âm thầm bỏ qua
            return "Không tìm thấy destination: "
                    + request.getDestinationName()
                    + " / category: " + request.getCategory();
        }

        DestinationCost existing = found.get();
        int count = existing.getContributionCount();
        BigDecimal currentAvg = existing.getCostUsd();

        BigDecimal newAvg = currentAvg
                .multiply(BigDecimal.valueOf(count))
                .add(request.getCostUsd())
                .divide(BigDecimal.valueOf(count + 1), 2, RoundingMode.HALF_UP);

        existing.setCostUsd(newAvg);
        existing.setContributionCount(count + 1);
        existing.setSource("user_contributed");
        repository.save(existing);

        return "OK";
    }

    private DestinationCostDTO toDTO(DestinationCost d) {
        return DestinationCostDTO.builder()
                .destinationName(d.getDestinationName())
                .category(d.getCategory())
                .costUsd(d.getCostUsd())
                .costLocal(d.getCostLocal())
                .localCurrency(d.getLocalCurrency())
                .contributionCount(d.getContributionCount())
                .source(d.getSource())
                .build();
    }
}