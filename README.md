# BJT Niwa – App Android

Bản app Android của web BJT Niwa. App chạy toàn bộ trang web ngay trong máy (không cần mở claude.ai).

## Cách 1: Tạo file APK bằng GitHub (không cần cài gì)

1. Tạo tài khoản GitHub (miễn phí) nếu chưa có, rồi tạo một repository mới, ví dụ `bjt-niwa-android`.
2. Giải nén file zip này và tải **toàn bộ** nội dung lên repository (nút *Add file → Upload files*), giữ nguyên cấu trúc thư mục, kể cả thư mục `.github`.
3. Vào tab **Actions** của repository. Bản dựng *Build APK* sẽ tự chạy (khoảng 3–5 phút). Nếu chưa chạy, chọn *Build APK → Run workflow*.
4. Khi bản dựng có dấu ✓ xanh, bấm vào nó, kéo xuống mục **Artifacts**, tải **BJT-Niwa-apk** (file zip chứa `app-debug.apk`).
5. Chép `app-debug.apk` vào điện thoại hoặc máy tính bảng Android, mở file để cài. Nếu máy hỏi, cho phép *Cài ứng dụng không rõ nguồn gốc* cho trình quản lý file.

## Cách 2: Dùng Android Studio

1. Cài Android Studio (bản mới nhất).
2. *File → Open*, chọn thư mục `bjt-niwa-android`, đợi Gradle tải xong.
3. Cắm điện thoại (bật *USB debugging*) rồi bấm ▶ **Run**, hoặc *Build → Build APK(s)* để lấy file APK trong `app/build/outputs/apk/debug/`.

## App làm được gì

- Đầy đủ chức năng của web: đăng nhập, tạo thẻ tiếng Nhật và tiếng Anh, bộ thẻ có sẵn, học thẻ, kiểm tra, khu vườn trồng cây, tìm kiếm, chọn nhiều để xóa.
- **Nhập Excel chạy được không cần mạng** (bộ đọc Excel có sẵn trong app). Nút chọn file mở trình quản lý file của Android.
- **Nút loa phát âm** dùng giọng đọc (Text-to-Speech) của Android. Nếu máy chưa có giọng tiếng Nhật: *Cài đặt → Quản lý chung → Chuyển văn bản thành giọng nói* (tên mục tùy máy), tải thêm gói tiếng Nhật.
- Nút **Back** của Android đóng cửa sổ đang mở trong app trước, rồi mới thoát app.

## Khác với bản web trên claude.ai

- **Dữ liệu lưu riêng trong app trên máy đó**, không đồng bộ với bản web trên claude.ai. Gỡ app hoặc xóa dữ liệu app sẽ mất thẻ và khu vườn.
- **Không có chức năng quét ảnh bằng AI**, vì chức năng này cần chạy bên trong claude.ai. Nút này tự ẩn trong app.
- Phông chữ đẹp (Google Fonts) cần mạng ở lần mở đầu; không có mạng app vẫn chạy với phông của máy.

## Cập nhật app sau này

Khi web có bản mới: tải file HTML mới, đổi tên thành `index.html`, thay vào `app/src/main/assets/` (giữ đoạn `<script>` nối giọng đọc Android ở đầu file và dòng `s.src = 'xlsx.full.min.js'`), tăng `versionCode` trong `app/build.gradle.kts`, rồi dựng lại APK.
