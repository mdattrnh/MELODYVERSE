package com.example.melodyverse;

import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu mẫu của MelodyVerse. Được nạp lên Firestore MỘT lần duy nhất (document meta/seed)
 * khi có người đăng nhập lần đầu, sau đó mọi máy đều đọc chung từ Firebase.
 */
public final class SeedData {

    public static final String DEMO_USER = "minhthu";
    public static final String DEMO_USER_EMAIL = "minhthu@melodyverse.vn";
    public static final String DEMO_USER_PASSWORD = "12345678";
    public static final String DEMO_ADMIN = "admin";
    public static final String DEMO_ADMIN_EMAIL = "admin@melodyverse.vn";
    public static final String DEMO_ADMIN_PASSWORD = "admin123";

    public static final String[] SAMPLE_POST_IDS = {"p1", "p2", "p3", "p4"};

    /**
     * Hồ sơ nghệ sĩ / thành viên MINH HỌA: chỉ là dữ liệu trình diễn, KHÔNG có email + mật khẩu trên
     * Firebase Authentication nên không ai đăng nhập được bằng các tài khoản này.
     * (Hai tài khoản demo minhthu / admin là tài khoản đăng nhập được thật.)
     */
    public static final String[] SAMPLE_ACCOUNTS = {
            "quanghung_masterd", "hieuthuhai", "wrenevans", "rhyder", "captainboy", "phapkieu", "ngocthanh"
    };

    /** Nghệ sĩ được gợi ý theo dõi cho thành viên mới (người dùng tự chọn, không tự động theo dõi). */
    public static final String[] SUGGESTED_ARTISTS = {
            "quanghung_masterd", "hieuthuhai", "wrenevans", "rhyder", "captainboy", "phapkieu"
    };

    private SeedData() {}

    /** true nếu là hồ sơ minh họa (không có thông tin đăng nhập thực tế). */
    public static boolean isSampleAccount(String username) {
        String u = DatabaseHelper.norm(username);
        for (String s : SAMPLE_ACCOUNTS) {
            if (s.equals(u)) return true;
        }
        return false;
    }

    /** true nếu là bài đăng minh họa do hệ thống nạp sẵn. */
    public static boolean isSamplePost(String postId) {
        for (String id : SAMPLE_POST_IDS) {
            if (id.equals(postId)) return true;
        }
        return false;
    }

    /** Email của tài khoản demo theo username (null nếu không phải tài khoản demo). */
    public static String demoEmail(String username) {
        if (DEMO_USER.equalsIgnoreCase(username)) return DEMO_USER_EMAIL;
        if (DEMO_ADMIN.equalsIgnoreCase(username)) return DEMO_ADMIN_EMAIL;
        return null;
    }

    /** Username của tài khoản demo theo email (null nếu không phải tài khoản demo). */
    public static String demoUsername(String email) {
        if (DEMO_USER_EMAIL.equalsIgnoreCase(email)) return DEMO_USER;
        if (DEMO_ADMIN_EMAIL.equalsIgnoreCase(email)) return DEMO_ADMIN;
        return null;
    }

    /** true nếu đúng email + mật khẩu của một tài khoản demo (được phép tự tạo trên Firebase Auth). */
    public static boolean isDemoCredential(String email, String password) {
        return (DEMO_USER_EMAIL.equalsIgnoreCase(email) && DEMO_USER_PASSWORD.equals(password))
                || (DEMO_ADMIN_EMAIL.equalsIgnoreCase(email) && DEMO_ADMIN_PASSWORD.equals(password));
    }

    public static List<UserModel> users() {
        List<UserModel> list = new ArrayList<>();
        UserModel minhthu = new UserModel(DEMO_USER, "Phạm Minh Thư", "Thành viên tích cực",
                "Đam mê âm nhạc và chia sẻ cảm xúc qua từng giai điệu\nThành viên MelodyVerse", 0, 0, 0, 0);
        minhthu.setEmail(DEMO_USER_EMAIL);
        list.add(minhthu);
        UserModel admin = new UserModel(DEMO_ADMIN, "Quản Trị Viên MelodyVerse", "Quản trị viên (Admin)",
                "Tài khoản điều hành & kiểm duyệt nội dung cộng đồng MelodyVerse", 1, 0, 0, 0);
        admin.setEmail(DEMO_ADMIN_EMAIL);
        list.add(admin);
        list.add(new UserModel("quanghung_masterd", "Quang Hùng MasterD", "Nghệ sĩ V-Pop", "Say Hi Hitmaker • Ca sĩ, Nhạc sĩ, Producer", 0, 0, 0, 0));
        list.add(new UserModel("hieuthuhai", "HIEUTHUHAI", "Rapper V-Pop", "GERDNANG • Say Hi Quán Quân", 1, 0, 0, 0));
        list.add(new UserModel("wrenevans", "Wren Evans", "Nghệ sĩ Indie/R&B", "Album Loi Choi • Từng Quen", 0, 0, 0, 0));
        list.add(new UserModel("rhyder", "Rhyder", "Rapper / Ca sĩ", "Quán quân The Voice Kids • Say Hi All-Star", 0, 0, 0, 0));
        list.add(new UserModel("captainboy", "Captain Boy", "Rapper", "Rapper Gen Z • Say Hi All-Star", 0, 0, 0, 0));
        list.add(new UserModel("phapkieu", "Pháp Kiều", "Rapper", "Rapper tài năng, tràn đầy phong cách và tự tin", 1, 0, 0, 0));
        list.add(new UserModel("ngocthanh", "Ngọc Thành", "Thành viên", "Fan cứng Tinh Hà Say Hi", 0, 0, 0, 0));
        return list;
    }

    /** 4 bài mẫu; số bình luận khớp đúng số bình luận mẫu (sửa lỗi #10). */
    public static List<PostModel> posts(long now) {
        List<PostModel> list = new ArrayList<>();
        list.add(post(new PostModel("p1", "Ngọc Thành", "ngocthanh", "",
                "Chill cùng Tinh Hà nào\nBài này nghe là thấy cả vũ trụ luôn!",
                "50 Cuộc Gọi Nhỡ", "Tinh Hà Say Hi • Tập 8",
                "https://example.com/audio/50_cuoc_goi_nho.mp3",
                "V-Pop", "#TinhHaSayHi #NhacViet #SayHi", 248, 3, 12,
                false, false, R.raw.song_sample_1), now - 7200000));
        list.add(post(new PostModel("p2", "Phạm Minh Thư", DEMO_USER, "",
                "Một buổi chiều ngắm hoàng hôn bên cửa sổ cùng giai điệu nhẹ nhàng",
                "Không Thể Say", "HIEUTHUHAI",
                "https://example.com/audio/khong_the_say.mp3",
                "Rap / Hip-Hop", "#HIEUTHUHAI #ChillVibes #MelodyVerse", 512, 3, 45,
                false, false, R.raw.song_sample_2), now - 14400000));
        list.add(post(new PostModel("p3", "Quang Hùng MasterD", "quanghung_masterd", "",
                "Tập này anh em diễn hết mình luôn, cảm ơn mọi người đã ủng hộ bản phối mới này!",
                "Thủy Triều (Remix)", "Quang Hùng MasterD",
                "https://example.com/audio/thuy_trieu.mp3",
                "V-Pop", "#MasterD #SayHi #RemixTrend", 1024, 3, 89,
                false, false, R.raw.song_sample_1), now - 86400000));
        list.add(post(new PostModel("p4", "Wren Evans", "wrenevans", "",
                "Retro Funk vibe cho ngày cuối tuần, ai cũng từng quen một người như thế...",
                "Từng Quen", "Wren Evans",
                "https://example.com/audio/tung_quen.mp3",
                "R&B", "#WrenEvans #TungQuen #RetroFunk", 890, 2, 70,
                false, false, R.raw.song_sample_2), now - 172800000));
        return list;
    }

    private static PostModel post(PostModel p, long createdAt) {
        p.setCreatedAt(createdAt);
        return p;
    }

    public static List<CommentModel> comments(long now) {
        List<CommentModel> list = new ArrayList<>();
        list.add(comment("p1_c1", "p1", "quanghung_masterd", "Quang Hùng MasterD", "Beat cuốn thực sự! Đoạn drop nghe nổi cả da gà.", now - 7000000));
        list.add(comment("p1_c2", "p1", "hieuthuhai", "HIEUTHUHAI", "Giai điệu này nghe vào đêm khuya thì hết nước chấm luôn bạn ơi!", now - 3600000));
        list.add(comment("p1_c3", "p1", "ngocthanh", "Ngọc Thành", "Cảm ơn anh em đã nhiệt tình ủng hộ bản phối!", now - 900000));
        list.add(comment("p2_c1", "p2", "wrenevans", "Wren Evans", "Vibe này quá chill và hợp tâm trạng luôn.", now - 10800000));
        list.add(comment("p2_c2", "p2", "hieuthuhai", "HIEUTHUHAI", "Bài này nghe khi đi dạo chill hết nấc bạn ơi!", now - 3600000));
        list.add(comment("p2_c3", "p2", DEMO_USER, "Phạm Minh Thư", "Giai điệu yêu thích nhất tuần này của mình <3", now - 1500000));
        list.add(comment("p3_c1", "p3", "rhyder", "Rhyder", "Năng lượng bài này cực kỳ bùng nổ trên sân khấu!", now - 7200000));
        list.add(comment("p3_c2", "p3", "captainboy", "Captain Boy", "Remix đỉnh chóp, đoạn beat nghe bắt tai dã man!", now - 2400000));
        list.add(comment("p3_c3", "p3", "quanghung_masterd", "Quang Hùng MasterD", "Cảm ơn các homies đã cùng cháy hết mình!", now - 600000));
        list.add(comment("p4_c1", "p4", "phapkieu", "Pháp Kiều", "Nghe đoạn điệp khúc là nhớ ngay người cũ luôn á!", now - 90000000));
        list.add(comment("p4_c2", "p4", "wrenevans", "Wren Evans", "Cảm ơn mọi người đã nghe Từng Quen nhé!", now - 3000000));
        return list;
    }

    private static CommentModel comment(String id, String postId, String username, String name, String content, long createdAt) {
        return new CommentModel(id, postId, username, name, content, createdAt, SyncStatus.SYNCED);
    }

    /**
     * Thông báo MINH HỌA chỉ dành cho tài khoản demo {@link #DEMO_USER}: mỗi thông báo trỏ tới đúng bài viết,
     * bình luận hoặc hồ sơ mẫu có thật trên Firestore (p2 là bài của minhthu, p2_c1 là bình luận của Wren Evans...).
     * Tài khoản khác không nhận các thông báo này.
     */
    public static List<NotificationModel> sampleNotifications(String recipient, long now) {
        List<NotificationModel> list = new ArrayList<>();
        list.add(new NotificationModel("sample_like_p2_hieuthuhai", recipient, NotificationModel.TYPE_LIKE,
                "hieuthuhai", "HIEUTHUHAI", "p2", null, "Không Thể Say", null, now - 600_000L, false, true));
        list.add(new NotificationModel("sample_newpost_p3", recipient, NotificationModel.TYPE_NEW_POST,
                "quanghung_masterd", "Quang Hùng MasterD", "p3", null, "Thủy Triều (Remix)", null, now - 3_600_000L, false, true));
        list.add(new NotificationModel("sample_comment_p2_c1", recipient, NotificationModel.TYPE_COMMENT,
                "wrenevans", "Wren Evans", "p2", "p2_c1", "Không Thể Say", "Vibe này quá chill và hợp tâm trạng luôn.",
                now - 18_000_000L, false, true));
        list.add(new NotificationModel("sample_follow_rhyder_" + recipient, recipient, NotificationModel.TYPE_FOLLOW,
                "rhyder", "Rhyder", null, null, null, null, now - 86_400_000L, false, true));
        return list;
    }

    /** Quan hệ theo dõi mẫu: {người theo dõi, người được theo dõi}. */
    public static String[][] follows() {
        return new String[][]{
                {DEMO_USER, "quanghung_masterd"},
                {DEMO_USER, "hieuthuhai"},
                {DEMO_USER, "wrenevans"},
                {"rhyder", DEMO_USER},
                {"phapkieu", DEMO_USER}
                // Tài khoản quản trị chỉ làm nhiệm vụ quản trị, không tham gia theo dõi / đăng bài như thành viên
        };
    }
}
