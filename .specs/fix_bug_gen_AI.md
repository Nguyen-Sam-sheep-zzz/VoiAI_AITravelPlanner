# Refactor: Chuyển itinerary generation từ 1 request lớn sang N requests theo từng ngày

## Bối cảnh hiện tại
Hệ thống đang gọi Gemini API 1 lần duy nhất để sinh toàn bộ lịch trình N ngày, trả về 1 JSON lớn.
Giải pháp tạm đang dùng là rút ngắn độ dài text (description/reason) để JSON không vượt giới hạn
output token của Gemini 2.5 Flash. Đây là workaround tạm, cần refactor sang kiến trúc đúng.

## Mục tiêu
Thay 1 request lớn bằng N request riêng biệt (N = số ngày của chuyến đi), mỗi request chỉ sinh
dữ liệu cho 1 ngày. Loại bỏ hoàn toàn việc giới hạn độ dài text — vì giờ mỗi response nhỏ,
không cần ép ngắn description/reason nữa, có thể trả về nội dung đầy đủ, chất lượng như cũ.

## Yêu cầu kỹ thuật

### 1. Backend (Spring Boot)
- Tạo `DayGenerationContext` (record/DTO) chứa: destination, startDate, dayNumber, totalDays,
  usedPlaceNames (List<String> - tên các địa điểm đã sinh ở ngày trước, không cần full data),
  userPreferences.
- Tạo method `generateSingleDay(tripId, dayNumber, context)` trong service tầng Gemini hiện tại
  (giữ nguyên GeminiRetryHelper/exponential backoff đã có, áp dụng cho từng request ngày).
- Prompt cho mỗi ngày phải:
  - Chỉ yêu cầu JSON cho 1 ngày, schema rõ ràng.
  - Liệt kê usedPlaceNames và yêu cầu AI không lặp lại các địa điểm đó.
  - KHÔNG giới hạn độ dài description/reason — cho phép mô tả đầy đủ tự nhiên như trước khi áp dụng workaround.
- Tạo endpoint SSE: `GET /api/trips/{tripId}/generate-stream` nhận params (destination, startDate,
  totalDays, preferences), chạy bất đồng bộ (CompletableFuture/@Async), generate tuần tự từng ngày,
  sau mỗi ngày xong:
  - Lưu ngay vào DB (TripDay + Activities liên quan), không đợi hết các ngày.
  - Emit SSE event "day-ready" kèm dữ liệu ngày đó.
  - Cộng dồn tên địa điểm vào usedPlaceNames cho ngày kế tiếp (chỉ giữ tối đa 2 ngày gần nhất nếu
    trip > 5 ngày, để tránh input prompt phình to).
  - Nếu 1 ngày lỗi parse/JSON sau khi đã retry theo backoff: emit event "day-error" với dayNumber,
    KHÔNG dừng các ngày còn lại (tiếp tục generate ngày kế tiếp), để frontend có thể retry riêng
    ngày đó sau.
  - Khi xong hết: emit event "complete".
- Thêm endpoint retry riêng: `POST /api/trips/{tripId}/regenerate-day/{dayNumber}` để gọi lại
  1 ngày cụ thể khi nó lỗi hoặc user muốn AI tạo lại riêng ngày đó (tái dùng usedPlaceNames từ
  các ngày khác trừ ngày đang regenerate).

### 2. Frontend (Next.js 14)
- Tạo hook `useTripGeneration` dùng `EventSource` để lắng nghe SSE stream từ endpoint trên.
- State quản lý: `days: TripDay[]` (append dần khi nhận "day-ready"), `loadingDay: number | null`
  (ngày đang chờ AI), `failedDays: number[]` (ngày bị lỗi để hiện nút "Tạo lại"), `isComplete: boolean`.
- UI: render `DayCard` ngay khi ngày đó có trong `days` (không đợi đủ N ngày). Hiện skeleton/loading
  indicator cho `loadingDay`. Nếu ngày nào trong `failedDays`, hiện nút "Tạo lại ngày X" gọi
  endpoint `regenerate-day`.
- Đảm bảo đóng EventSource khi component unmount hoặc khi nhận "complete"/lỗi nghiêm trọng.

## Ràng buộc / không được làm
- KHÔNG còn giới hạn artificial độ dài description/reason trong prompt hay trong code xử lý response —
  bỏ hoàn toàn workaround cũ.
- KHÔNG gọi N request đồng thời (Promise.all) — phải tuần tự, vì:
  (a) ngày sau cần biết địa điểm ngày trước để tránh trùng,
  (b) tránh vượt rate limit RPM của Gemini free tier.
- Giữ nguyên toàn bộ logic GeminiRetryHelper (exponential backoff) đã có, chỉ áp dụng nó cho
  từng request-theo-ngày thay vì request-toàn-bộ.
- Không đổi schema DB hiện tại (TripDay, Activity) trừ khi cần thêm field generationStatus
  (PENDING/SUCCESS/FAILED) cho mỗi TripDay để hỗ trợ retry UI.

## Việc cần làm theo thứ tự
1. Viết DayGenerationContext + sửa prompt builder để nhận context theo ngày.
2. Viết generateSingleDay() trong service, test thử với 1 ngày trước.
3. Viết SSE endpoint generate-stream, đảm bảo lưu DB từng ngày + emit event đúng thứ tự.
4. Viết endpoint regenerate-day cho retry riêng lẻ.
5. Sửa frontend: hook useTripGeneration + cập nhật UI hiển thị progressive.
6. Test case: trip 1 ngày, trip 5 ngày, trip 10 ngày, và case giả lập 1 ngày bị lỗi giữa chừng
   để xác nhận các ngày khác vẫn tiếp tục và retry riêng ngày lỗi hoạt động đúng.