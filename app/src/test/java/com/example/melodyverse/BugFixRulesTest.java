package com.example.melodyverse;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiểm thử các quy tắc thuần logic của đợt sửa lỗi: username từ tài khoản Google, phân biệt dữ liệu minh họa,
 * thông báo minh họa gắn đúng tài khoản nhận và trỏ tới nội dung có thật trong dữ liệu mẫu.
 */
public class BugFixRulesTest {

    @Test
    public void usernameFromGoogleEmail_isSanitized() {
        assertEquals("nguyen.van_a", AuthManager.usernameBase("Nguyen.Van_A@gmail.com", null));
        // Bỏ dấu tiếng Việt và ký tự không hợp lệ
        assertEquals("tranthiduc", AuthManager.usernameBase("trần+thị-đức@gmail.com", null));
        // Quá ngắn → thêm tiền tố để đủ 3 ký tự
        assertEquals("userab", AuthManager.usernameBase("ab@gmail.com", null));
        // Quá dài → cắt còn 20 ký tự
        assertEquals(20, AuthManager.usernameBase("abcdefghijklmnopqrstuvwxyz@gmail.com", null).length());
        // Không có email → dùng tên hiển thị
        assertEquals("phamminhthu", AuthManager.usernameBase(null, "Phạm Minh Thư"));
    }

    @Test
    public void sampleAccounts_areSeparatedFromLoginAccounts() {
        assertTrue(SeedData.isSampleAccount("hieuthuhai"));
        assertTrue(SeedData.isSampleAccount("@QuangHung_MasterD"));
        // Hai tài khoản demo có thông tin đăng nhập thật, không phải hồ sơ minh họa
        assertFalse(SeedData.isSampleAccount(SeedData.DEMO_USER));
        assertFalse(SeedData.isSampleAccount(SeedData.DEMO_ADMIN));
        // Hồ sơ minh họa không có email đăng nhập
        assertNull(SeedData.demoEmail("hieuthuhai"));
        for (String artist : SeedData.SUGGESTED_ARTISTS) {
            assertTrue("Nghệ sĩ gợi ý phải là hồ sơ mẫu: " + artist, SeedData.isSampleAccount(artist));
        }
    }

    @Test
    public void sampleNotifications_belongToRecipientAndPointToRealSampleContent() {
        List<NotificationModel> list = SeedData.sampleNotifications("minhthu", 1_000_000_000L);
        assertFalse(list.isEmpty());
        Set<String> commentIds = new HashSet<>();
        for (CommentModel c : SeedData.comments(0)) commentIds.add(c.getId());
        Set<String> ids = new HashSet<>();
        for (NotificationModel n : list) {
            assertEquals("minhthu", n.getRecipient());
            assertTrue(n.isSample());
            assertFalse(n.isRead());
            assertTrue("ID thông báo không được trùng", ids.add(n.getId()));
            if (!n.getPostId().isEmpty()) {
                assertTrue("Thông báo phải trỏ tới bài mẫu có thật: " + n.getPostId(), SeedData.isSamplePost(n.getPostId()));
            }
            if (!n.getCommentId().isEmpty()) {
                assertTrue("Thông báo phải trỏ tới bình luận mẫu có thật: " + n.getCommentId(), commentIds.contains(n.getCommentId()));
            }
            if (NotificationModel.TYPE_FOLLOW.equals(n.getType())) {
                assertTrue(SeedData.isSampleAccount(n.getActor()));
            }
        }
    }

    @Test
    public void notificationSummary_matchesType() {
        NotificationModel like = new NotificationModel("x", "a", NotificationModel.TYPE_LIKE, "b", "Bé B",
                "p1", null, "Bài A", null, 0, false, false);
        assertTrue(like.getSummary().contains("Bé B") && like.getSummary().contains("Bài A"));
        NotificationModel comment = new NotificationModel("y", "a", NotificationModel.TYPE_COMMENT, "b", "Bé B",
                "p1", "c1", "Bài A", "Hay quá", 0, true, false);
        assertTrue(comment.getSummary().contains("Hay quá"));
        assertTrue(comment.isRead());
    }
}
