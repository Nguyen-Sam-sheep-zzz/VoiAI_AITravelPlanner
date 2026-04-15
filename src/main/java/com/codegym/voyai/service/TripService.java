package com.codegym.voyai.service;

import com.codegym.voyai.model.Activity;
import com.codegym.voyai.model.Trip;
import com.codegym.voyai.model.TripDay;
import com.codegym.voyai.model.User;
import com.codegym.voyai.model.dto.TripRequest;
import com.codegym.voyai.model.dto.travel.TravelItinerary;
import com.codegym.voyai.repository.IActivityRepository;
import com.codegym.voyai.repository.ITripDayRepository;
import com.codegym.voyai.repository.ITripRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TripService {

    private final ITripRepository tripRepository;
    private final ITripDayRepository tripDayRepository;
    private final IActivityRepository activityRepository;
    private final GeminiService geminiService;
    private final UserService userService;

    @Transactional
    public Trip createTrip(TripRequest request, String userEmail) {

        User user = userService.findByEmail(userEmail);
        if (user == null) throw new RuntimeException("User không tồn tại");

        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDate.now().plusDays(1);

        log.info("Calling Gemini for: {}", request.getDestination());
        TravelItinerary itinerary = geminiService.generateSimpleTravelItinerary(
                request.getDestination(),
                request.getNumDays(),
                formatBudget(request.getBudgetTotal(), request.getCurrency()),
                request.getNotes(),
                request.getLat(),
                request.getLng()
        );

        Trip trip = Trip.builder()
                .user(user)
                .title("Chuyến đi " + request.getDestination())
                .destinationName(request.getDestination())
                .destLat(BigDecimal.valueOf(request.getLat()))
                .destLng(BigDecimal.valueOf(request.getLng()))
                .startDate(startDate)
                .endDate(startDate.plusDays(request.getNumDays() - 1))
                .numDays(request.getNumDays())
                .budgetTotal(request.getBudgetTotal())
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .tripDays(new LinkedHashSet<>())
                .build();

        if (itinerary != null && itinerary.getItinerary() != null) {
            for (TravelItinerary.DayItinerary dayData : itinerary.getItinerary()) {
                TripDay tripDay = TripDay.builder()
                        .trip(trip)
                        .dayNumber(dayData.getDay())
                        .tripDate(startDate.plusDays(dayData.getDay() - 1))
                        .activities(new LinkedHashSet<>())
                        .build();

                trip.getTripDays().add(tripDay);

                if (dayData.getActivities() != null) {
                    int order = 0;
                    for (TravelItinerary.ActivityItem actData : dayData.getActivities()) {
                        Activity activity = Activity.builder()
                                .tripDay(tripDay)
                                .sortOrder(order++)
                                .title(actData.getActivity())
                                .description(actData.getReason())
                                .startTime(parseTime(actData.getTime()))
                                .locationName(actData.getActivity())
                                .locationLat(actData.getLat() != null
                                        ? BigDecimal.valueOf(actData.getLat()) : null)
                                .locationLng(actData.getLng() != null
                                        ? BigDecimal.valueOf(actData.getLng()) : null)
                                .estimatedCost(actData.getEstimatedCost() != null
                                        ? BigDecimal.valueOf(actData.getEstimatedCost()) : null)
                                .build();

                        tripDay.getActivities().add(activity);
                    }
                }
            }
        }

        trip = tripRepository.save(trip);
        return trip;
    }

    public List<Trip> getMyTrips(String userEmail) {
        User user = userService.findByEmail(userEmail);
        if (user == null) return List.of();
        return tripRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public Trip getTripById(Long id, String userEmail) {
        // Kiểm tra ownership trước bằng query nhẹ
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        if (!trip.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        // Load đầy đủ
        return getTripWithDetails(id);
    }

    public Trip getPublicTrip(String shareToken) {
        Trip trip = tripRepository.findByShareToken(shareToken)
                .filter(Trip::getIsPublic)
                .orElseThrow(() -> new RuntimeException(
                        "Trip không tồn tại hoặc chưa public"));
        return getTripWithDetails(trip.getId());
    }

    public void deleteTrip(Long id, String userEmail) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));
        if (!trip.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Không có quyền truy cập");
        }
        tripRepository.delete(trip);
    }

    // ✅ Method dùng chung — tránh MultipleBagFetchException
    // Dùng 2 query riêng thay vì 1 JOIN FETCH lồng nhau
    private Trip getTripWithDetails(Long tripId) {
        // Chỉ cần load Trip kèm Days
        Trip trip = tripRepository.findByIdWithDays(tripId)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        // Sau đó load Days kèm Activities (Hibernate sẽ tự map vào Trip đang có trong Persistence Context)
        tripDayRepository.findByTripIdWithActivities(tripId);

        return trip;
    }

    private String formatBudget(BigDecimal budget, String currency) {
        if (budget == null) return "Không giới hạn";
        return budget.toPlainString() + " " + (currency != null ? currency : "VND");
    }

    private LocalTime parseTime(String timeStr) {
        try {
            if (timeStr == null) return null;
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            return null;
        }
    }
}