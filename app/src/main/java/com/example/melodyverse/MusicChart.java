package com.example.melodyverse;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bảng xếp hạng âm nhạc: xếp bài hát theo TỔNG số lượt thả tim trên toàn ứng dụng.
 * Nhiều bài viết chia sẻ cùng một bài hát (trùng tên bài + nghệ sĩ) được cộng dồn lượt tim.
 */
public final class MusicChart {

    public static final int TOP_SIZE = 10;

    /** Một hàng trên bảng xếp hạng. */
    public static final class Entry {
        /** Bài viết đại diện (nhiều tim nhất) — dùng để phát nhạc và mở chi tiết. */
        public final PostModel post;
        public final int totalLikes;

        Entry(PostModel post, int totalLikes) {
            this.post = post;
            this.totalLikes = totalLikes;
        }
    }

    private MusicChart() {}

    /**
     * Xếp hạng giảm dần theo tổng lượt tim; bằng tim thì bài được đăng sớm hơn đứng trên (giữ ổn định thứ tự).
     * Bỏ qua bài riêng tư và bài không có tên bài hát.
     */
    public static List<Entry> rank(List<PostModel> posts, int limit) {
        Map<String, PostModel> best = new LinkedHashMap<>();
        Map<String, Integer> likes = new LinkedHashMap<>();
        Map<String, Long> firstShared = new LinkedHashMap<>();
        for (PostModel p : posts) {
            if (p == null || p.isPrivate() || p.getSongTitle() == null || p.getSongTitle().trim().isEmpty()) continue;
            String key = songKey(p);
            int l = Math.max(0, p.getLikeCount());
            likes.put(key, likes.containsKey(key) ? likes.get(key) + l : l);
            PostModel current = best.get(key);
            if (current == null || l > current.getLikeCount()) best.put(key, p);
            Long first = firstShared.get(key);
            if (first == null || p.getCreatedAt() < first) firstShared.put(key, p.getCreatedAt());
        }
        List<String> keys = new ArrayList<>(best.keySet());
        keys.sort((a, b) -> {
            int byLikes = Integer.compare(likes.get(b), likes.get(a));
            if (byLikes != 0) return byLikes;
            int byTime = Long.compare(firstShared.get(a), firstShared.get(b));
            return byTime != 0 ? byTime : a.compareTo(b);
        });
        List<Entry> result = new ArrayList<>();
        for (String key : keys) {
            if (result.size() >= limit) break;
            result.add(new Entry(best.get(key), likes.get(key)));
        }
        return result;
    }

    /** Khóa nhận diện một bài hát: tên bài + nghệ sĩ, bỏ dấu, không phân biệt hoa thường. */
    static String songKey(PostModel p) {
        return normalize(p.getSongTitle()) + "|" + normalize(p.getArtistName());
    }

    static String normalize(String s) {
        if (s == null) return "";
        String nfd = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return nfd.replace('đ', 'd').replace('Đ', 'd').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** "1.024 lượt yêu thích" */
    public static String likesLabel(int likes) {
        return String.format(Locale.GERMANY, "%,d", likes) + " lượt yêu thích";
    }
}
