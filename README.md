# 🎵 MelodyVerse — Mạng xã hội chia sẻ âm nhạc

Ứng dụng Android cho phép người dùng chia sẻ bài hát kèm cảm nghĩ, nghe nhạc ngay trên bảng tin, thả tim, bình luận, theo dõi nhau và lưu lại những bản nhạc yêu thích. Quản trị viên có trang riêng để kiểm duyệt bài hát, người dùng và bài viết bị báo cáo vi phạm.

> Đồ án môn **Phát triển ứng dụng di động** — Đại học Kinh tế TP.HCM (UEH).

---

## 1. Tổng quan

| Hạng mục | Nội dung |
|---|---|
| Nền tảng | Android, Java, **100% Activity** (không dùng Fragment) |
| SDK | `compileSdk 34`, `minSdk 30` (Android 11 trở lên) |
| Dữ liệu gốc | **Firebase Authentication** + **Cloud Firestore** (project `melodyverse-c9478`, gói Spark) |
| Bộ nhớ offline | **SQLite** (`melodyverse.db`, v10) — lưu đệm các trang đã xem + hàng chờ đồng bộ |
| Phát nhạc | `MusicService` (Service chạy nền) + `MediaPlayer`, giao tiếp giao diện qua **BroadcastReceiver** |
| Nguồn nhạc | File mẫu trong APK (`res/raw`, nghe được khi offline) hoặc link trực tuyến (hỗ trợ dán link **Audius**) |
| Kiểm thử | JUnit — 23 test (`app/src/test`) |

### Kiến trúc dữ liệu (Hybrid Online – Offline)

```
          Giao diện (Activity kế thừa BaseActivity)
                          │  đọc / ghi
                          ▼
     DatabaseHelper (SQLite)  ◄──── SyncManager ────►  Firebase
     • luôn đọc từ đây → mất mạng      • đẩy thao tác PENDING      • Auth: đăng nhập
       vẫn hiển thị được               • tải trang mới về         • Firestore: users, posts,
     • thao tác offline lưu cờ PENDING • lắng nghe realtime         comments, likes, bookmarks,
                                         (addSnapshotListener)      follows, notifications, reports
```

- **Online:** mở / tải lại trang → đẩy hết thao tác đang chờ → tải dữ liệu mới từ Firestore → ghi đè bộ đệm SQLite.
- **Offline:** màn hình đọc SQLite; thích, bình luận, đăng bài, lưu nhạc, theo dõi… được lưu với 🕒 và tự gửi khi có mạng.
- **Realtime:** bài viết, bình luận, lượt theo dõi, thông báo, khóa tài khoản thay đổi trên Firebase (kể cả sửa tay trên Console) → app cập nhật ngay.

### Cấu trúc mã nguồn chính (`app/src/main/java/com/example/melodyverse`)

| Nhóm | File |
|---|---|
| Màn hình | `SplashActivity`, `LoginActivity`, `RegisterActivity`, `MainActivity` (Bảng tin), `PostDetailActivity`, `CreatePostActivity`, `DiscoverActivity`, `NotificationsActivity`, `ProfileActivity`, `SettingsActivity`, `AdminActivity` — tất cả kế thừa `BaseActivity` (thanh điều hướng + mini player + màn Đang phát) |
| Dữ liệu & đồng bộ | `DatabaseHelper` (SQLite), `SyncManager` (Firestore ↔ SQLite), `AuthManager`, `GoogleSignInHelper`, `SessionManager`, `SeedData` (dữ liệu mẫu) |
| Nhạc | `MusicService`, `NotificationHelper`, `AudiusApi`, `MusicChart` (bảng xếp hạng) |
| Mạng | `NetworkMonitor`, `NetworkChangeReceiver` |
| Adapter / View | `PostAdapter`, `CommentAdapter`, `TrackListAdapter`, `FollowUserAdapter`, `SuggestionAdapter`, `SimilarPostAdapter`, `DiscWheelView`, `WaveformSeekBar` |
| Model | `PostModel`, `UserModel`, `CommentModel`, `NotificationModel`, `SyncStatus` |

---

## 2. Chức năng

### 👤 Thành viên
- **Tài khoản:** đăng ký / đăng nhập bằng username hoặc email, đăng nhập Google; tài khoản mới được gợi ý nghệ sĩ để theo dõi (không tự theo dõi sẵn).
- **Bảng tin:** bài hát dạng đĩa than (xoay khi phát), tìm kiếm tiếng Việt không dấu & hashtag, lọc thể loại, vuốt / bấm lại Home để làm mới, ẩn bài.
- **Tương tác:** thả tim, bình luận (sắp xếp Mới nhất / Cũ nhất / Nổi bật), **chia sẻ = sao chép liên kết bài hát**, báo cáo bài vi phạm, bấm tên / ảnh tác giả hoặc người bình luận để mở trang cá nhân.
- **Đăng bài:** chọn bài hát mẫu hoặc dán link nhạc (Audius), chế độ Công khai / Riêng tư, sửa / xóa bài của mình.
- **Khám phá:** **Bảng xếp hạng thịnh hành** (Top 10, tự xếp theo tổng lượt tim, cập nhật khi thả / bỏ tim) và **Vòng xoay đĩa than**.
- **Hồ sơ:** Bài đã chia sẻ, Bài đã thích, **Danh sách nhạc đã lưu** (chỉ lưu bản nhạc; bấm để nghe, *Phát tất cả* — bài tiếp theo chỉ lấy trong danh sách), người theo dõi / đang theo dõi.
- **Trình phát:** mini player, màn Đang phát (đĩa than + sóng âm, tua), Tiếp theo, **Lặp lại / Ngẫu nhiên loại trừ nhau**, thông báo điều khiển nhạc.
- **Thông báo:** theo từng tài khoản (thích, bình luận, theo dõi, bài mới), nút hành động mở đúng bài / bình luận / hồ sơ.
- **Cài đặt:** chế độ tối, thông báo, tự động phát bài tiếp theo, cảnh báo mất mạng.
- **Offline:** xem dữ liệu đã lưu, thao tác chờ đồng bộ (🕒); bấm nghe nhạc trực tuyến khi mất mạng → *"Bạn đang ngoại tuyến. Vui lòng kết nối Internet để nghe bài hát trực tuyến này"*.

### 🛡️ Quản trị viên (tài khoản riêng, không dùng chung với thành viên)
- Thống kê: **Bài đăng, Người dùng, Vi phạm cần xử lý**, kèm trạng thái kết nối Hệ thống.
- 3 khu vực: **Bài hát** (xem bài đăng, nghe thử, ẩn / hiện lại, xóa) · **Người dùng** (tìm theo tên / username, khóa / mở khóa) · **Vi phạm** (bài bị báo cáo kèm số lượt và lý do → Ẩn bài / Xóa / Bỏ qua).
- Nền tảng chỉ chia 2 loại tài khoản: **Thành viên** và **Quản trị viên**. Quyền quản trị trên Firestore = có document `admins/{uid}` (uid lấy ở Authentication → Users).

---

## 3. Cài đặt & chạy

1. Mở thư mục `DEMO MOBILE` bằng **Android Studio** (bản mới, dùng JDK đi kèm) → chờ Gradle Sync.
2. Kiểm tra có file `app/google-services.json`.
3. **Firestore Rules:** Firebase Console → Firestore Database → Rules → dán toàn bộ `firestore.rules` → **Publish** (cần làm lại mỗi khi file này thay đổi).
4. Chọn máy ảo **Android 11+** (khuyên dùng image *Google Play* API 34) → **Run ▶**.

Dòng lệnh: `gradlew :app:assembleDebug` (build APK) · `gradlew :app:testDebugUnitTest` (chạy 23 unit test).

> Đăng nhập Google chỉ chạy trên máy có Google Play services mới và SHA-1 của máy build đã được thêm vào Firebase. Các chức năng khác không bị ảnh hưởng.

### Tài khoản demo

| Tài khoản | Mật khẩu | Vai trò |
|---|---|---|
| `minhthu` | `12345678` | Thành viên (có sẵn bài viết, người theo dõi, thông báo minh họa) |
| `admin` | `admin123` | Quản trị viên — vào thẳng trang Quản trị |

Màn Đăng nhập có nút tự điền nhanh hai tài khoản này. Các hồ sơ nghệ sĩ (`hieuthuhai`, `quanghung_masterd`, `wrenevans`…) là **dữ liệu minh họa** (gắn nhãn *Minh họa*), không đăng nhập được.

---

## 4. Kịch bản demo (~5 phút)

**Chuẩn bị:** đã Publish rules, máy ảo có mạng, đăng xuất sẵn.

1. **Đăng nhập thành viên** — bấm điền nhanh `minhthu` → *Đăng nhập*. (Có thể demo nhập sai mật khẩu bị chặn.)
2. **Bảng tin & phát nhạc**
   - Bấm ▶ trên một bài → đĩa than xoay, sóng nhạc chạy, mini player hiện ở dưới.
   - Chạm mini player → màn **Đang phát**: tua nhạc; bật **Lặp lại** rồi bật **Ngẫu nhiên** → Lặp lại tự tắt.
   - Tìm "say hi" hoặc bấm một hashtag để lọc bài.
3. **Tương tác**
   - Thả tim một bài; mở bài → viết bình luận.
   - Bấm tên một người bình luận → mở trang cá nhân của họ → *+ Theo dõi* → Back.
   - Bấm **Chia sẻ** → *Sao chép liên kết* → dán vào ô tìm kiếm / app khác để thấy đúng link.
4. **Khám phá** — tab Khám phá: Bảng xếp hạng theo lượt tim. Thả tim cho một bài ở hạng thấp → quay lại Khám phá thấy bài đó lên hạng. Lướt xuống **Vòng xoay đĩa than**, chọn đĩa và phát.
5. **Danh sách nhạc đã lưu**
   - Ở Bảng tin bấm **Lưu 📑** trên 2–3 bài.
   - Hồ sơ → tab *Danh sách nhạc đã lưu* → **Phát tất cả**: hết bài 1 tự chuyển đúng bài 2 trong danh sách.
   - (Mẹo: bài nhạc mẫu chỉ dài vài giây nên thấy chuyển bài rất nhanh.)
6. **Đăng bài** — bấm `+` → chọn bài hát mẫu, viết cảm nghĩ → *Đăng* → bài mới hiện đầu Bảng tin.
7. **Offline**
   - Tắt Wi-Fi + dữ liệu di động: Bảng tin vẫn hiển thị.
   - Thả tim / bình luận → hiện 🕒.
   - Bấm nghe bài có link nhạc trực tuyến → hiện thông báo ngoại tuyến, nút về Play.
   - Bật mạng lại → 🕒 tự biến mất.
8. **Báo cáo vi phạm** — ⋮ trên bài của người khác → *Báo cáo vi phạm* → chọn lý do.
9. **Quản trị viên**
   - Đăng xuất → đăng nhập `admin` → trang Quản trị riêng, không có thanh điều hướng thành viên.
   - Xem thống kê → tab **Vi phạm** thấy bài vừa báo cáo → **Ẩn bài** (bài biến mất khỏi Bảng tin của thành viên).
   - Tab **Người dùng**: khóa / mở khóa một tài khoản.
10. **Realtime (nếu còn thời gian)**
    - Đăng nhập lại `minhthu`, mở Firebase Console song song.
    - Thêm một document mới trong `posts` (`songTitle`, `artistName`, `authorName`, `authorUsername`, `caption`, `createdAt` kiểu number) → bài hiện ngay trên Bảng tin.
    - Xóa một document trong `follows` → nút trên hồ sơ tương ứng tự về *+ Theo dõi*.

---

## 5. Sự cố thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Đăng nhập báo không kết nối được Firebase | Kiểm tra mạng của máy ảo (mở Chrome thử) |
| Báo cáo / thông báo không hoạt động | Chưa Publish `firestore.rules` mới |
| Trang Quản trị báo *Lỗi máy chủ* / Vi phạm luôn = 0 | Thiếu document `admins/{uid}` của tài khoản admin: Firestore → collection `admins` → Document ID = User UID của `admin@melodyverse.vn` (Authentication → Users), field `username` = `admin` |
| Gõ bàn phím máy tính vào máy ảo bị mất chữ | Tắt Unikey / EVKey (chuyển sang chế độ E) khi gõ vào máy ảo |
| Máy ảo màn hình đen | Device Manager → Edit → *Graphics: Software* |
| Bấm tab dưới không ăn | Bàn phím đang che thanh điều hướng → bấm Back ẩn bàn phím |
| "Thiết bị chưa hỗ trợ đăng nhập Google" | Máy thiếu / cũ Google Play services |
| `minhthu` lại vào trang Quản trị | Trên Firestore sửa `users/minhthu.role` về `Thành viên tích cực`, xóa `admins/<uid của minhthu>` nếu có |
| Gradle Sync lỗi tải thư viện | Kiểm tra Internet; *File → Invalidate Caches → Restart* |
