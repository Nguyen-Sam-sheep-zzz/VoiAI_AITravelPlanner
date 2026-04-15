package com.codegym.voyai.repository;

import com.codegym.voyai.model.DestinationCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IDestinationCostRepository extends JpaRepository<DestinationCost, Long> {

    // Lấy toàn bộ chi phí của 1 thành phố — dùng khi AI cần estimate budget
    List<DestinationCost> findByDestinationNameIgnoreCase(String destinationName);

    // Lấy đúng 1 category của 1 thành phố
    Optional<DestinationCost> findByDestinationNameIgnoreCaseAndCategory(
            String destinationName, String category);

    // Tìm kiếm gợi ý khi người dùng gõ tên thành phố
    @Query("SELECT DISTINCT d.destinationName FROM DestinationCost d " +
            "WHERE LOWER(d.destinationName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<String> searchDestinationNames(@Param("keyword") String keyword);

    // Lấy toàn bộ theo country code — ví dụ tất cả thành phố ở Thái Lan
    List<DestinationCost> findByCountryCode(String countryCode);
}