package com.codegym.springbootjwtdemo.config;

import com.codegym.springbootjwtdemo.model.DestinationCost;
import com.codegym.springbootjwtdemo.repository.IDestinationCostRepository;
import com.opencsv.CSVReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final IDestinationCostRepository repository;

    // Một số tên quốc gia trong CSV viết khác Locale — cần normalize thêm
    private static final Map<String, String> CSV_NAME_OVERRIDES = Map.ofEntries(
            Map.entry("South Korea", "KR"),
            Map.entry("Korea, South", "KR"),
            Map.entry("North Korea", "KP"),
            Map.entry("Russia", "RU"),
            Map.entry("Czech Republic", "CZ"),
            Map.entry("Czechia", "CZ"),
            Map.entry("Taiwan", "TW"),
            Map.entry("Hong Kong", "HK"),
            Map.entry("Macau", "MO"),
            Map.entry("Bolivia", "BO"),
            Map.entry("Venezuela", "VE"),
            Map.entry("Moldova", "MD"),
            Map.entry("Macedonia", "MK"),
            Map.entry("North Macedonia", "MK"),
            Map.entry("Bosnia and Herzegovina", "BA"),
            Map.entry("Bosnia And Herzegovina", "BA"),
            Map.entry("Ivory Coast", "CI"),
            Map.entry("Democratic Republic of Congo", "CD"),
            Map.entry("United States", "US"),
            Map.entry("United Kingdom", "GB"),
            Map.entry("United Arab Emirates", "AE"),
            Map.entry("South Africa", "ZA"),
            Map.entry("New Zealand", "NZ"),
            Map.entry("Palestinian Territory", "PS"),
            Map.entry("Sint Maarten", "SX"),
            Map.entry("Curacao", "CW"),
            Map.entry("Kosovo", "XK"),
            Map.entry("Vatican City", "VA")
    );

    /**
     * Tự động build map "France" → "FR" từ tất cả Locale có sẵn trong JVM.
     * Kết quả: ~250 quốc gia không cần hardcode.
     */

    // ✅ Build tự động từ Locale — không cần hardcode
    // Map: "France" → "FR", "Japan" → "JP" ...
    private static final Map<String, String> COUNTRY_TO_CODE = buildCountryCodeMap();

    // Currency map vẫn cần hardcode vì Locale không có currency
    private static final Map<String, String> CODE_TO_CURRENCY = Map.ofEntries(
            Map.entry("VN", "VND"), Map.entry("TH", "THB"), Map.entry("SG", "SGD"),
            Map.entry("JP", "JPY"), Map.entry("MY", "MYR"), Map.entry("ID", "IDR"),
            Map.entry("KR", "KRW"), Map.entry("FR", "EUR"), Map.entry("US", "USD"),
            Map.entry("GB", "GBP"), Map.entry("AU", "AUD"), Map.entry("DE", "EUR"),
            Map.entry("IT", "EUR"), Map.entry("ES", "EUR"), Map.entry("CN", "CNY"),
            Map.entry("IN", "INR"), Map.entry("AE", "AED"), Map.entry("CH", "CHF"),
            Map.entry("CA", "CAD"), Map.entry("PH", "PHP"), Map.entry("KH", "KHR"),
            Map.entry("PT", "EUR"), Map.entry("GR", "EUR"), Map.entry("TR", "TRY"),
            Map.entry("MX", "MXN"), Map.entry("TW", "TWD"), Map.entry("HK", "HKD"),
            Map.entry("CZ", "CZK"), Map.entry("HU", "HUF"), Map.entry("PL", "PLN"),
            Map.entry("NL", "EUR"), Map.entry("BE", "EUR"), Map.entry("SE", "SEK"),
            Map.entry("NO", "NOK"), Map.entry("DK", "DKK"), Map.entry("BR", "BRL"),
            Map.entry("AR", "ARS"), Map.entry("CO", "COP"), Map.entry("ZA", "ZAR"),
            Map.entry("EG", "EGP"), Map.entry("MA", "MAD"), Map.entry("NZ", "NZD"),
            Map.entry("MM", "MMK"), Map.entry("LA", "LAK"), Map.entry("NP", "NPR"),
            Map.entry("LK", "LKR"), Map.entry("RU", "RUB"), Map.entry("UA", "UAH"),
            Map.entry("RO", "RON"), Map.entry("HR", "HRK"), Map.entry("RS", "RSD"),
            Map.entry("MT", "EUR"), Map.entry("LU", "EUR"), Map.entry("IS", "ISK"),
            Map.entry("FI", "EUR"), Map.entry("AT", "EUR"), Map.entry("IE", "EUR"),
            Map.entry("IL", "ILS"), Map.entry("SA", "SAR"), Map.entry("QA", "QAR"),
            Map.entry("KW", "KWD"), Map.entry("BH", "BHD"), Map.entry("OM", "OMR"),
            Map.entry("JO", "JOD"), Map.entry("LB", "LBP"), Map.entry("PK", "PKR"),
            Map.entry("BD", "BDT"), Map.entry("MN", "MNT"), Map.entry("KZ", "KZT"),
            Map.entry("UZ", "UZS"), Map.entry("GE", "GEL"), Map.entry("AM", "AMD"),
            Map.entry("AZ", "AZN"), Map.entry("BY", "BYN"), Map.entry("MK", "MKD"),
            Map.entry("SK", "EUR"), Map.entry("SI", "EUR"), Map.entry("BG", "BGN"),
            Map.entry("LT", "EUR"), Map.entry("LV", "EUR"), Map.entry("EE", "EUR"),
            Map.entry("NG", "NGN"), Map.entry("KE", "KES"), Map.entry("GH", "GHS"),
            Map.entry("TZ", "TZS"), Map.entry("ET", "ETB"), Map.entry("CM", "XAF"),
            Map.entry("CI", "XOF"), Map.entry("SN", "XOF"), Map.entry("TN", "TND"),
            Map.entry("DZ", "DZD"), Map.entry("PE", Map.entry("PE", "PEN").getValue()),
            Map.entry("CL", "CLP"), Map.entry("UY", "UYU"), Map.entry("EC", "USD"),
            Map.entry("BO", "BOB"), Map.entry("PY", "PYG"), Map.entry("VE", "VES"),
            Map.entry("CR", "CRC"), Map.entry("PA", "PAB"), Map.entry("GT", "GTQ"),
            Map.entry("DO", "DOP"), Map.entry("CU", "CUP"), Map.entry("JM", "JMD")
    );

    private List<DestinationCost> buildVietnamData(Set<String> seen) {
        List<DestinationCost> list = new ArrayList<>();

        record VN(String city, String cat, double usd, double vnd) {
        }

        List<VN> data = List.of(
                new VN("Hanoi", "meal_budget", 1.80, 45000),
                new VN("Hanoi", "meal_mid", 5.00, 125000),
                new VN("Hanoi", "meal_fine", 15.00, 375000),
                new VN("Hanoi", "coffee", 1.00, 25000),
                new VN("Hanoi", "hotel_budget", 12.00, 300000),
                new VN("Hanoi", "hotel_mid", 35.00, 875000),
                new VN("Hanoi", "hotel_fine", 90.00, 2250000),
                new VN("Hanoi", "transport_day", 1.20, 30000),
                new VN("Hanoi", "attraction_avg", 3.00, 75000),
                new VN("Hanoi", "motorbike_rent", 6.00, 150000),

                new VN("Ho Chi Minh City", "meal_budget", 2.00, 50000),
                new VN("Ho Chi Minh City", "meal_mid", 6.00, 150000),
                new VN("Ho Chi Minh City", "meal_fine", 18.00, 450000),
                new VN("Ho Chi Minh City", "coffee", 1.20, 30000),
                new VN("Ho Chi Minh City", "hotel_budget", 15.00, 375000),
                new VN("Ho Chi Minh City", "hotel_mid", 45.00, 1125000),
                new VN("Ho Chi Minh City", "hotel_fine", 110.00, 2750000),
                new VN("Ho Chi Minh City", "transport_day", 1.50, 37500),
                new VN("Ho Chi Minh City", "attraction_avg", 4.00, 100000),
                new VN("Ho Chi Minh City", "motorbike_rent", 7.00, 175000),

                new VN("Da Nang", "meal_budget", 1.60, 40000),
                new VN("Da Nang", "meal_mid", 5.00, 125000),
                new VN("Da Nang", "meal_fine", 14.00, 350000),
                new VN("Da Nang", "coffee", 0.90, 22000),
                new VN("Da Nang", "hotel_budget", 14.00, 350000),
                new VN("Da Nang", "hotel_mid", 40.00, 1000000),
                new VN("Da Nang", "hotel_fine", 95.00, 2375000),
                new VN("Da Nang", "transport_day", 1.00, 25000),
                new VN("Da Nang", "attraction_avg", 5.00, 125000),
                new VN("Da Nang", "motorbike_rent", 6.00, 150000),

                new VN("Hoi An", "meal_budget", 1.60, 40000),
                new VN("Hoi An", "meal_mid", 4.50, 112000),
                new VN("Hoi An", "meal_fine", 12.00, 300000),
                new VN("Hoi An", "coffee", 0.80, 20000),
                new VN("Hoi An", "hotel_budget", 12.00, 300000),
                new VN("Hoi An", "hotel_mid", 35.00, 875000),
                new VN("Hoi An", "hotel_fine", 80.00, 2000000),
                new VN("Hoi An", "transport_day", 0.80, 20000),
                new VN("Hoi An", "attraction_avg", 2.50, 62000),
                new VN("Hoi An", "motorbike_rent", 5.00, 125000),

                new VN("Nha Trang", "meal_budget", 1.60, 40000),
                new VN("Nha Trang", "meal_mid", 5.00, 125000),
                new VN("Nha Trang", "meal_fine", 14.00, 350000),
                new VN("Nha Trang", "coffee", 0.90, 22000),
                new VN("Nha Trang", "hotel_budget", 12.00, 300000),
                new VN("Nha Trang", "hotel_mid", 35.00, 875000),
                new VN("Nha Trang", "hotel_fine", 85.00, 2125000),
                new VN("Nha Trang", "transport_day", 1.00, 25000),
                new VN("Nha Trang", "attraction_avg", 6.00, 150000),
                new VN("Nha Trang", "motorbike_rent", 6.00, 150000),

                new VN("Phu Quoc", "meal_budget", 2.00, 50000),
                new VN("Phu Quoc", "meal_mid", 6.00, 150000),
                new VN("Phu Quoc", "meal_fine", 20.00, 500000),
                new VN("Phu Quoc", "coffee", 1.00, 25000),
                new VN("Phu Quoc", "hotel_budget", 18.00, 450000),
                new VN("Phu Quoc", "hotel_mid", 55.00, 1375000),
                new VN("Phu Quoc", "hotel_fine", 150.00, 3750000),
                new VN("Phu Quoc", "transport_day", 2.00, 50000),
                new VN("Phu Quoc", "attraction_avg", 10.00, 250000),
                new VN("Phu Quoc", "motorbike_rent", 8.00, 200000),

                new VN("Da Lat", "meal_budget", 1.60, 40000),
                new VN("Da Lat", "meal_mid", 4.50, 112000),
                new VN("Da Lat", "meal_fine", 12.00, 300000),
                new VN("Da Lat", "coffee", 0.80, 20000),
                new VN("Da Lat", "hotel_budget", 10.00, 250000),
                new VN("Da Lat", "hotel_mid", 30.00, 750000),
                new VN("Da Lat", "hotel_fine", 70.00, 1750000),
                new VN("Da Lat", "transport_day", 0.80, 20000),
                new VN("Da Lat", "attraction_avg", 2.00, 50000),
                new VN("Da Lat", "motorbike_rent", 5.00, 125000),

                new VN("Ha Long", "meal_budget", 1.80, 45000),
                new VN("Ha Long", "meal_mid", 5.00, 125000),
                new VN("Ha Long", "meal_fine", 15.00, 375000),
                new VN("Ha Long", "coffee", 0.90, 22000),
                new VN("Ha Long", "hotel_budget", 14.00, 350000),
                new VN("Ha Long", "hotel_mid", 40.00, 1000000),
                new VN("Ha Long", "hotel_fine", 100.00, 2500000),
                new VN("Ha Long", "transport_day", 1.20, 30000),
                new VN("Ha Long", "attraction_avg", 55.00, 1375000),
                new VN("Ha Long", "motorbike_rent", 6.00, 150000),

                new VN("Hue", "meal_budget", 1.40, 35000),
                new VN("Hue", "meal_mid", 4.00, 100000),
                new VN("Hue", "meal_fine", 10.00, 250000),
                new VN("Hue", "coffee", 0.70, 18000),
                new VN("Hue", "hotel_budget", 9.00, 225000),
                new VN("Hue", "hotel_mid", 28.00, 700000),
                new VN("Hue", "hotel_fine", 65.00, 1625000),
                new VN("Hue", "transport_day", 0.80, 20000),
                new VN("Hue", "attraction_avg", 3.00, 75000),
                new VN("Hue", "motorbike_rent", 5.00, 125000),

                new VN("Vung Tau", "meal_budget", 1.80, 45000),
                new VN("Vung Tau", "meal_mid", 5.00, 125000),
                new VN("Vung Tau", "meal_fine", 14.00, 350000),
                new VN("Vung Tau", "coffee", 0.90, 22000),
                new VN("Vung Tau", "hotel_budget", 12.00, 300000),
                new VN("Vung Tau", "hotel_mid", 35.00, 875000),
                new VN("Vung Tau", "hotel_fine", 80.00, 2000000),
                new VN("Vung Tau", "transport_day", 1.00, 25000),
                new VN("Vung Tau", "attraction_avg", 2.00, 50000),
                new VN("Vung Tau", "motorbike_rent", 6.00, 150000),

                new VN("Sapa", "meal_budget", 1.60, 40000),
                new VN("Sapa", "meal_mid", 4.50, 112000),
                new VN("Sapa", "meal_fine", 12.00, 300000),
                new VN("Sapa", "coffee", 0.80, 20000),
                new VN("Sapa", "hotel_budget", 10.00, 250000),
                new VN("Sapa", "hotel_mid", 32.00, 800000),
                new VN("Sapa", "hotel_fine", 75.00, 1875000),
                new VN("Sapa", "transport_day", 1.50, 37500),
                new VN("Sapa", "attraction_avg", 4.00, 100000),
                new VN("Sapa", "motorbike_rent", 6.00, 150000),

                new VN("Can Tho", "meal_budget", 1.40, 35000),
                new VN("Can Tho", "meal_mid", 4.00, 100000),
                new VN("Can Tho", "meal_fine", 10.00, 250000),
                new VN("Can Tho", "coffee", 0.70, 18000),
                new VN("Can Tho", "hotel_budget", 9.00, 225000),
                new VN("Can Tho", "hotel_mid", 28.00, 700000),
                new VN("Can Tho", "hotel_fine", 60.00, 1500000),
                new VN("Can Tho", "transport_day", 0.80, 20000),
                new VN("Can Tho", "attraction_avg", 2.00, 50000),
                new VN("Can Tho", "motorbike_rent", 5.00, 125000),

                new VN("Mui Ne", "meal_budget", 1.80, 45000),
                new VN("Mui Ne", "meal_mid", 5.00, 125000),
                new VN("Mui Ne", "meal_fine", 14.00, 350000),
                new VN("Mui Ne", "coffee", 0.90, 22000),
                new VN("Mui Ne", "hotel_budget", 12.00, 300000),
                new VN("Mui Ne", "hotel_mid", 35.00, 875000),
                new VN("Mui Ne", "hotel_fine", 80.00, 2000000),
                new VN("Mui Ne", "transport_day", 1.20, 30000),
                new VN("Mui Ne", "attraction_avg", 3.00, 75000),
                new VN("Mui Ne", "motorbike_rent", 6.00, 150000),

                new VN("Sam Son", "meal_budget", 1.40, 35000),
                new VN("Sam Son", "meal_mid", 4.00, 100000),
                new VN("Sam Son", "meal_fine", 10.00, 250000),
                new VN("Sam Son", "coffee", 0.70, 18000),
                new VN("Sam Son", "hotel_budget", 8.00, 200000),
                new VN("Sam Son", "hotel_mid", 25.00, 625000),
                new VN("Sam Son", "hotel_fine", 55.00, 1375000),
                new VN("Sam Son", "transport_day", 0.80, 20000),
                new VN("Sam Son", "attraction_avg", 1.50, 37500),
                new VN("Sam Son", "motorbike_rent", 5.00, 125000)
        );

        for (VN d : data) {
            String key = (d.city() + "|VN|" + d.cat()).toLowerCase();
            if (!seen.add(key)) continue;

            list.add(DestinationCost.builder()
                    .destinationName(d.city())
                    .countryCode("VN")
                    .category(d.cat())
                    .costUsd(BigDecimal.valueOf(d.usd()).setScale(2, RoundingMode.HALF_UP))
                    .costLocal(BigDecimal.valueOf(d.vnd()))
                    .localCurrency("VND")
                    .source("researched_apr2026")
                    .contributionCount(0)
                    .build());
        }

        log.info("Vietnam data: {} bản ghi cho {} thành phố.",
                list.size(),
                data.stream().map(VN::city).distinct().count());
        return list;
    }

    private static Map<String, String> buildCountryCodeMap() {
        Map<String, String> map = new HashMap<>();

        for (String code : Locale.getISOCountries()) {
            Locale locale = Locale.of("", code);

            // Tên tiếng Anh: "France", "Japan", "Vietnam"...
            String displayName = locale.getDisplayCountry(Locale.ENGLISH);
            if (!displayName.isBlank()) {
                map.put(displayName, code);
            }
        }

        // Ghi đè các tên viết khác trong CSV
        map.putAll(CSV_NAME_OVERRIDES);

        return Collections.unmodifiableMap(map);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) {
            log.info("destination_costs đã có {} bản ghi, bỏ qua seed.",
                    repository.count());
            return;
        }

        log.info("Bắt đầu seed...");
        List<DestinationCost> all = new ArrayList<>();

        // ✅ 1 set dùng chung cho cả CSV lẫn Vietnam data
        Set<String> globalSeen = new HashSet<>();

        all.addAll(parseCsv(globalSeen));           // truyền set vào
        all.addAll(buildVietnamData(globalSeen));    // cùng set → tự dedup

        repository.saveAll(all);

        log.info("Seed xong! {} bản ghi, {} thành phố.", all.size(),
                all.stream().map(DestinationCost::getDestinationName).distinct().count());
    }

    private List<DestinationCost> parseCsv(Set<String> seen) throws Exception {
        List<DestinationCost> result = new ArrayList<>();
        Set<String> unknownCountries = new LinkedHashSet<>();

        // 1. Khởi tạo bộ giải quyết đường dẫn của Spring
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 2. Lấy tất cả các file có đuôi .csv trong thư mục resources/data/
        Resource[] resources = resolver.getResources("classpath:data/*.csv");

        log.info("Tìm thấy {} file CSV để xử lý.", resources.length);

        for (Resource resource : resources) {
            log.info("==> Đang nạp dữ liệu từ file: {}", resource.getFilename());

            try (CSVReader reader = new CSVReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                List<String[]> rows = reader.readAll();
                if (rows.size() < 2) continue; // File trống hoặc chỉ có header thì bỏ qua

                String[] header = rows.get(0);
                Map<String, Integer> idx = buildIndex(header);

                int fileSkippedQuality = 0;
                int fileSkippedDuplicate = 0;

                for (int i = 1; i < rows.size(); i++) {
                    String[] row = rows.get(i);
                    try {
                        double quality = parseDouble(row, idx, "data_quality");
                        if (quality > 0 && quality < 0.5) {
                            fileSkippedQuality++;
                            continue;
                        }

                        String country = getString(row, idx, "country");
                        String countryCode = resolveCountryCode(country);

                        if (countryCode == null) {
                            unknownCountries.add(country);
                            continue;
                        }

                        String city = getString(row, idx, "city");
                        if (city.isBlank()) continue;

                        List<DestinationCost> entries = buildEntries(row, idx, city, country, countryCode);

                        for (DestinationCost e : entries) {
                            // Nhờ có Set 'seen' nằm ngoài vòng lặp for (resource),
                            // nên nếu file 2 trùng data với file 1, nó sẽ tự bị loại bỏ ở đây.
                            String key = (e.getDestinationName() + "|" + e.getCountryCode() + "|" + e.getCategory()).toLowerCase();
                            if (seen.add(key)) {
                                result.add(e);
                            } else {
                                fileSkippedDuplicate++;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Lỗi dòng {} trong file {}: {}", i, resource.getFilename(), e.getMessage());
                    }
                }
                log.info("Hoàn tất file {}: Bỏ qua {} quality thấp, {} duplicate.",
                        resource.getFilename(), fileSkippedQuality, fileSkippedDuplicate);
            }
        }

        if (!unknownCountries.isEmpty()) {
            log.warn("Không tìm được country code cho {} quốc gia: {}", unknownCountries.size(), unknownCountries);
        }

        return result;
    }

    private List<DestinationCost> buildEntries(String[] row, Map<String, Integer> idx,
                                               String city, String country,
                                               String countryCode) {
        List<DestinationCost> list = new ArrayList<>();
        String currency = CODE_TO_CURRENCY.getOrDefault(countryCode, "USD");

        // meal_budget = x1
        add(list, city, countryCode, "meal_budget",
                parseDouble(row, idx, "x1"), currency);

        // meal_mid = x2 / 2 (x2 là cho 2 người)
        double x2 = parseDouble(row, idx, "x2");
        add(list, city, countryCode, "meal_mid",
                x2 > 0 ? x2 / 2 : 0, currency);

        // coffee = x5
        add(list, city, countryCode, "coffee",
                parseDouble(row, idx, "x5"), currency);

        // transport_day: ưu tiên monthly pass / 30
        double x12 = parseDouble(row, idx, "x12");
        double x11 = parseDouble(row, idx, "x11");
        double transportDay = x12 > 0 ? x12 / 30 : x11;
        add(list, city, countryCode, "transport_day", transportDay, currency);

        // hotel_budget: thuê 1BR city centre / 30
        double x48 = parseDouble(row, idx, "x48");
        add(list, city, countryCode, "hotel_budget",
                x48 > 0 ? x48 / 30 : 0, currency);

        // hotel_mid: 1BR ngoài trung tâm / 30 × 1.5 (markup nhẹ)
        double x49 = parseDouble(row, idx, "x49");
        add(list, city, countryCode, "hotel_mid",
                x49 > 0 ? (x49 / 30) * 1.5 : 0, currency);

        return list;
    }

    /**
     * Tra country code — Locale trước, override sau
     */
    private String resolveCountryCode(String countryName) {
        if (countryName == null || countryName.isBlank()) return null;

        // 1. Thử map tự động từ Locale
        String code = COUNTRY_TO_CODE.get(countryName);
        if (code != null) return code;

        // 2. Thử normalize: trim + title case
        String normalized = toTitleCase(countryName.trim());
        return COUNTRY_TO_CODE.get(normalized);
    }

    private String toTitleCase(String input) {
        String[] words = input.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void add(List<DestinationCost> list, String city, String countryCode,
                     String category, double costUsd, String currency) {
        if (costUsd <= 0 || costUsd > 50000) return;

        list.add(DestinationCost.builder()
                .destinationName(city)
                .countryCode(countryCode)
                .category(category)
                .costUsd(BigDecimal.valueOf(costUsd).setScale(2, RoundingMode.HALF_UP))
                .costLocal(BigDecimal.ZERO)
                .localCurrency(currency)
                .source("kaggle_numbeo")
                .contributionCount(0)
                .build());
    }

    private Map<String, Integer> buildIndex(String[] header) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            idx.put(header[i].trim().toLowerCase(), i);
        }
        return idx;
    }

    private double parseDouble(String[] row, Map<String, Integer> idx, String col) {
        try {
            Integer i = idx.get(col);
            if (i == null || i >= row.length) return 0;
            String val = row[i].trim();
            if (val.isBlank() || val.equalsIgnoreCase("nan")
                    || val.equals("-") || val.equalsIgnoreCase("null")) return 0;
            return Double.parseDouble(val.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getString(String[] row, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= row.length) return "";
        return row[i].trim();
    }

}