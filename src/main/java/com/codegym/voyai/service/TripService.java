package com.codegym.voyai.service;

import com.codegym.voyai.model.*;
import com.codegym.voyai.model.dto.TripRequest;
import com.codegym.voyai.model.dto.travel.TravelItinerary;
import com.codegym.voyai.model.dto.weather.DailyWeatherDTO;
import com.codegym.voyai.repository.IActivityRepository;
import com.codegym.voyai.repository.IDestinationCostRepository;
import com.codegym.voyai.repository.ITripDayRepository;
import com.codegym.voyai.repository.ITripRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    private final IDestinationCostRepository destinationCostRepository;
    private final GeminiService geminiService;
    private final UserService userService;
    private final WeatherService weatherService;

    // --- 1. DÀNH CHO USER ĐÃ ĐĂNG NHẬP ---
    @Transactional
    public Trip createTrip(TripRequest request, String userEmail) {
        User user = userService.findByEmail(userEmail);
        if (user == null) throw new RuntimeException("User không tồn tại");

        Trip trip = buildBaseTrip(request);
        trip.setUser(user); // Gắn user

        return saveAndProcessItinerary(trip, request);
    }

    // --- 2. DÀNH CHO KHÁCH (GUEST) ---
    @Transactional
    public Trip createGuestTrip(TripRequest request, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new RuntimeException("Session ID không hợp lệ");
        }

        Trip trip = buildBaseTrip(request);
        trip.setSessionId(sessionId); // Gắn sessionId

        return saveAndProcessItinerary(trip, request);
    }

    // --- 3. LOGIC HỖ TRỢ DÙNG CHUNG ---

    // Hàm tạo "Xương" cho Trip (giúp code ngắn gọn hơn)
    private Trip buildBaseTrip(TripRequest request) {
        LocalDate startDate = request.getStartDate() != null
                ? request.getStartDate()
                : LocalDate.now().plusDays(1);

        return Trip.builder()
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
                .originName(request.getOriginName())
                .originLat(request.getOriginLat() != null ? BigDecimal.valueOf(request.getOriginLat()) : null)
                .originLng(request.getOriginLng() != null ? BigDecimal.valueOf(request.getOriginLng()) : null)
                .tripDays(new LinkedHashSet<>()) // Khởi tạo Set để tránh null
                .weatherCaches(new ArrayList<>())
                .build();
    }

    // Hàm xử lý lưu Trip, Gemini và các Day/Activity
    private Trip saveAndProcessItinerary(Trip trip, TripRequest request) {
        // Lưu Trip trước để lấy ID
        Trip savedTrip = tripRepository.save(trip);

        List<DestinationCost> dbCosts = destinationCostRepository.findCostsByDestination(request.getDestination());

        // 3. NHỜ GEMINI SERVICE BUILD CHUỖI TEXT THAM KHẢO
        String priceContext = geminiService.buildPriceReferenceContext(dbCosts);

        // Gọi Gemini lấy lịch trình
        TravelItinerary itinerary = geminiService.generateTravelItinerary(
                request.getOriginName(),
                request.getDestination(),
                request.getNumDays(),
                formatBudget(request.getBudgetTotal(), request.getCurrency()),
                request.getNotes(),
                request.getPlaceId(),
                savedTrip.getStartDate(),
                priceContext
        );

        // Xử lý Lịch trình (Day & Activities)
        if (itinerary != null && itinerary.getItinerary() != null) {
            processItineraryItems(savedTrip, itinerary);
        }
        // Gọi Weather (Optional - giữ nguyên logic cũ của bạn)
        try {
            processWeatherCache(savedTrip, request);
        } catch (Exception e) {
            log.warn("Lỗi lưu weather: {}", e.getMessage());
        }

        // Trả về trip đầy đủ dữ liệu bằng cách load lại từ DB
        return getTripWithDetails(savedTrip.getId());
    }

    private void processItineraryItems(Trip savedTrip, TravelItinerary itinerary) {
        for (TravelItinerary.DayItinerary dayData : itinerary.getItinerary()) {
            TripDay tripDay = TripDay.builder()
                    .trip(savedTrip)
                    .dayNumber(dayData.getDay())
                    .tripDate(savedTrip.getStartDate().plusDays(dayData.getDay() - 1))
                    .activities(new LinkedHashSet<>())
                    .build();

            tripDay = tripDayRepository.save(tripDay);
            savedTrip.getTripDays().add(tripDay);

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
                            .locationLat(actData.getLat() != null ? BigDecimal.valueOf(actData.getLat()) : null)
                            .locationLng(actData.getLng() != null ? BigDecimal.valueOf(actData.getLng()) : null)
                            .estimatedCost(actData.getEstimatedCost() != null ? BigDecimal.valueOf(actData.getEstimatedCost()) : null)
                            .build();

                    activityRepository.save(activity);
                    tripDay.getActivities().add(activity);
                }
            }
        }
    }

    // --- CÁC HÀM GET DỮ LIỆU ---

    public List<Trip> getMyTrips(String userEmail) {
        User user = userService.findByEmail(userEmail);

        return user == null ? List.of() : tripRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Trip> getGuestTrips(String sessionId) {
        return (sessionId == null || sessionId.isBlank()) ? List.of() : tripRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }

    private Trip getTripWithDetails(Long tripId) {
        Trip trip = tripRepository.findByIdWithDays(tripId)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));
        tripDayRepository.findByTripIdWithActivities(tripId);
        return trip;
    }

    private void processWeatherCache(Trip trip, TripRequest request) {
        List<DailyWeatherDTO> forecast = weatherService.getForecast(request.getLat(), request.getLng());
        if (forecast != null && !forecast.isEmpty()) {
            List<WeatherCache> caches = forecast.stream().map(w -> WeatherCache.builder()
                    .trip(trip)
                    .forecastDate(LocalDate.parse(w.getDate()))
                    .temperatureMax(w.getTempMax() != null ? BigDecimal.valueOf(w.getTempMax()) : null)
                    .temperatureMin(w.getTempMin() != null ? BigDecimal.valueOf(w.getTempMin()) : null)
                    .weatherCode(w.getWeatherCode())
                    .precipitationMm(w.getPrecipitation() != null ? BigDecimal.valueOf(w.getPrecipitation()) : null)
                    .build()).toList();
            trip.setWeatherCaches(new ArrayList<>(caches));
        }
    }

    public Trip getTripById(Long id, String userEmail) {
        // Kiểm tra ownership trước bằng query nhẹ
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        if (trip.getUser() == null || !trip.getUser().getEmail().equals(userEmail)) {
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

    @Transactional
    public int claimGuestTrips(String sessionId, String userEmail) {
        if (sessionId == null || sessionId.isBlank()) return 0;

        User user = userService.findByEmail(userEmail);
        if (user == null) throw new RuntimeException("User không tồn tại");

        int count = tripRepository.claimTripsBySession(sessionId, user);
        log.info("Claimed {} trips từ session {} → user {}", count, sessionId, userEmail);
        return count;
    }

    // Lấy trip của guest — kiểm tra sessionId thay vì email
    public Trip getGuestTripById(Long id, String sessionId) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        if (!sessionId.equals(trip.getSessionId())) {
            throw new RuntimeException("Không có quyền truy cập");
        }

        return getTripWithDetails(id);
    }
}