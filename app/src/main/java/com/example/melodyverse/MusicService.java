package com.example.melodyverse;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import java.util.List;

/**
 * Dịch vụ phát nhạc nền (Bài 6 - 07.4 Services & 07.3 Broadcasts).
 * Cho phép duy trì âm thanh khi người dùng chuyển qua lại các màn hình.
 */
public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicService";

    public static final String ACTION_PLAY = "com.example.melodyverse.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.melodyverse.ACTION_PAUSE";
    public static final String ACTION_RESUME = "com.example.melodyverse.ACTION_RESUME";
    public static final String ACTION_TOGGLE = "com.example.melodyverse.ACTION_TOGGLE";
    public static final String ACTION_STOP = "com.example.melodyverse.ACTION_STOP";
    public static final String ACTION_NEXT = "com.example.melodyverse.ACTION_NEXT";
    public static final String ACTION_SEEK = "com.example.melodyverse.ACTION_SEEK";

    public static final String ACTION_MUSIC_STATUS_CHANGED = "com.example.melodyverse.MUSIC_STATUS_CHANGED";
    public static final String EXTRA_IS_PLAYING = "is_playing";
    public static final String EXTRA_SONG_TITLE = "song_title";
    public static final String EXTRA_ARTIST_NAME = "artist_name";
    public static final String EXTRA_POST_ID = "post_id";
    public static final String EXTRA_DURATION = "duration";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_SEEK_POSITION = "seek_position";

    public static final String EXTRA_MUSIC_URL = "music_url";
    public static final String EXTRA_RAW_RES = "raw_res";
    public static final String EXTRA_GENRE = "genre";
    /** Màn hình gọi phát đã tự hiện thông báo (ví dụ báo ngoại tuyến) nên Service không báo lại. */
    public static final String EXTRA_ANNOUNCED = "announced";
    /** Thông báo cần hiện cho người dùng kèm Broadcast trạng thái (BaseActivity hiện Toast). */
    public static final String EXTRA_MESSAGE = "message";

    /** Thông báo khi bấm nghe bài hát trực tuyến lúc điện thoại không có Internet. */
    public static final String OFFLINE_STREAM_MESSAGE =
            "Bạn đang ngoại tuyến. Vui lòng kết nối Internet để nghe bài hát trực tuyến này";

    // Trạng thái dùng chung
    public static boolean isPlaying = false;
    public static boolean hasTrackLoaded = false;
    public static String currentSongTitle = "50 Cuộc Gọi Nhỡ";
    public static String currentArtistName = "Tinh Hà Say Hi • Tập 8";
    public static String currentPostId = "p1";
    public static String currentGenre = "V-Pop";
    public static int currentRawRes = R.raw.song_sample_1;
    public static String currentMusicUrl = null;
    public static int currentDuration = 0;
    public static int currentPosition = 0;
    /** Đang tải nhạc trực tuyến (chưa phát được) — giao diện hiện "Đang tải", KHÔNG hiện "Đang phát". */
    public static boolean isBuffering = false;

    // Chế độ phát (điều khiển từ Now Playing Dialog). Hai chế độ loại trừ nhau, cả hai tắt = phát tuần tự.
    public static boolean isShuffleEnabled = false;
    public static boolean isRepeatEnabled = false;

    /**
     * Danh sách phát đang dùng (ví dụ Danh sách nhạc đã lưu): khi có, bài tiếp theo chỉ lấy trong danh sách này
     * cho tới khi hết danh sách. null = phát theo Bảng tin như bình thường.
     */
    private static List<PostModel> playQueue = null;
    private static String playQueueName = null;

    public static void setShuffleEnabled(boolean enabled) {
        isShuffleEnabled = enabled;
        if (enabled) isRepeatEnabled = false;
    }

    public static void setRepeatEnabled(boolean enabled) {
        isRepeatEnabled = enabled;
        if (enabled) isShuffleEnabled = false;
    }

    /** true nếu đang phát trong danh sách {@code name}. */
    public static boolean isPlayingQueue(String name) {
        return playQueue != null && name != null && name.equals(playQueueName);
    }

    // MediaPlayer đang phát — dùng để đọc vị trí & thời lượng thật của bài hát
    private static MediaPlayer activePlayer;
    private static final java.util.Random random = new java.util.Random();

    private MediaPlayer mediaPlayer;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("melodyverse_settings", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }

        String action = intent.getAction();

        switch (action) {
            case ACTION_PLAY:
                String title = intent.getStringExtra(EXTRA_SONG_TITLE);
                String artist = intent.getStringExtra(EXTRA_ARTIST_NAME);
                String postId = intent.getStringExtra(EXTRA_POST_ID);
                String genre = intent.getStringExtra(EXTRA_GENRE);
                int rawRes = intent.getIntExtra(EXTRA_RAW_RES, 0);
                String url = intent.getStringExtra(EXTRA_MUSIC_URL);

                if (title != null) currentSongTitle = title;
                if (artist != null) currentArtistName = artist;
                if (postId != null) currentPostId = postId;
                if (genre != null) currentGenre = genre;
                if (rawRes != 0) currentRawRes = rawRes;

                playTrack(currentRawRes, url, intent.getBooleanExtra(EXTRA_ANNOUNCED, false));
                break;

            case ACTION_PAUSE:
                pauseTrack();
                break;

            case ACTION_RESUME:
                resumeTrack();
                break;

            case ACTION_TOGGLE:
                if (isPlaying) {
                    pauseTrack();
                } else {
                    if (mediaPlayer != null) {
                        resumeTrack();
                    } else {
                        playTrack(currentRawRes, currentMusicUrl, false);
                    }
                }
                break;

            case ACTION_NEXT:
                playNextTrack(true);
                break;

            case ACTION_SEEK:
                int seekPos = intent.getIntExtra(EXTRA_SEEK_POSITION, 0);
                if (mediaPlayer != null) {
                    try {
                        int realDur = mediaPlayer.getDuration();
                        if (realDur > 0) {
                            mediaPlayer.seekTo(Math.max(0, Math.min(seekPos, realDur - 1)));
                        }
                    } catch (Exception ignored) {}
                }
                broadcastStatus();
                break;

            case ACTION_STOP:
                stopTrack();
                stopSelf();
                break;
        }

        return START_STICKY;
    }

    /** Link nhạc thật (http/https, không phải link mẫu example.com) thì phát trực tuyến. */
    static boolean isStreamableUrl(String url) {
        if (url == null) return false;
        String u = url.trim().toLowerCase();
        return (u.startsWith("http://") || u.startsWith("https://")) && !u.contains("example.com");
    }

    /**
     * @param announced true nếu màn hình gọi đã tự báo cho người dùng (không báo lại khi ngoại tuyến)
     */
    private void playTrack(int rawRes, String url, boolean announced) {
        currentMusicUrl = url;
        if (isStreamableUrl(url)) {
            if (!SyncManager.get(this).isOnline()) {
                // Không tự chuyển sang bài mẫu khác: dừng ở trạng thái tạm dừng (nút phát về Play) và báo ngoại tuyến.
                // Thông báo đi kèm Broadcast để màn hình đang mở hiện Toast (Toast từ Service có thể bị hệ thống bỏ qua)
                streamFailed(announced ? null : OFFLINE_STREAM_MESSAGE);
                return;
            }
            playStream(url.trim());
            return;
        }
        // File âm thanh mẫu đóng gói trong APK: phát được cả khi offline
        playRaw(rawRes);
    }

    /** true nếu bài là nhạc trực tuyến mà điện thoại đang không có Internet (không thể phát). */
    public static boolean isOfflineStream(Context context, PostModel post) {
        return isStreamableUrl(post.getMusicUrl()) && !SyncManager.get(context).isOnline();
    }

    /** Thông báo đúng trạng thái khi người dùng bấm phát (không báo "Đang phát" khi nhạc trực tuyến chưa thể phát). */
    public static String startMessage(Context context, PostModel post) {
        if (isOfflineStream(context, post)) return OFFLINE_STREAM_MESSAGE;
        String song = post.getSongTitle() + " - " + post.getArtistName();
        if (isStreamableUrl(post.getMusicUrl())) {
            return "⏳ Đang tải nhạc trực tuyến: " + song;
        }
        return "Đang phát: " + song;
    }

    /** true nếu bài hiện tại phát trực tuyến (cần mạng). */
    public static boolean isStreamingCurrent() {
        return isStreamableUrl(currentMusicUrl);
    }

    /** Phát trực tuyến từ link người dùng nhập. Lỗi / mất mạng: dừng và thông báo (không phát bài khác thay thế). */
    private void playStream(String url) {
        releaseMediaPlayer();
        isPlaying = false;
        isBuffering = true;
        hasTrackLoaded = true;
        broadcastStatus();
        updateNotification();
        try {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mp.setDataSource(url);
            mp.setOnPreparedListener(p -> {
                if (p != mediaPlayer) return; // đã chuyển sang bài khác trong lúc đang tải
                p.setLooping(false);
                p.setOnCompletionListener(this);
                p.start();
                activePlayer = p;
                isBuffering = false;
                isPlaying = true;
                hasTrackLoaded = true;
                broadcastStatus();
                updateNotification();
            });
            mp.setOnErrorListener((p, what, extra) -> {
                Log.e(TAG, "Không phát được link " + url + " (what=" + what + ", extra=" + extra + ")");
                if (p == mediaPlayer) {
                    streamFailed(SyncManager.get(this).isOnline()
                            ? "Không phát được nhạc trực tuyến (link lỗi hoặc máy chủ không phản hồi)."
                            : "📴 Mất kết nối mạng — đã dừng phát nhạc trực tuyến. Hãy kết nối lại rồi bấm phát.");
                }
                return true;
            });
            mediaPlayer = mp;
            mp.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Link nhạc không hợp lệ: " + e.getMessage());
            streamFailed("Link nhạc trực tuyến không hợp lệ, không thể phát.");
        }
    }

    /** Nhạc trực tuyến không tải / không phát tiếp được: về trạng thái tạm dừng, đồng bộ mini player + notification. */
    private void streamFailed(String message) {
        releaseMediaPlayer();
        isPlaying = false;
        isBuffering = false;
        hasTrackLoaded = true;
        currentPosition = 0;
        currentDuration = 0;
        broadcastStatus(message);
        updateNotification();
    }

    private void playRaw(int rawRes) {
        releaseMediaPlayer();
        isBuffering = false;

        try {
            // Nạp từ res/raw để nghe mượt mà không delay
            if (rawRes != 0) {
                mediaPlayer = MediaPlayer.create(this, rawRes);
            } else {
                mediaPlayer = MediaPlayer.create(this, R.raw.song_sample_1);
            }

            if (mediaPlayer != null) {
                // Không lặp vô hạn: khi hết bài, onCompletion() xử lý Lặp lại / Tự động chuyển bài
                mediaPlayer.setLooping(false);
                mediaPlayer.setOnCompletionListener(this);
                mediaPlayer.setOnErrorListener(this);
                mediaPlayer.start();
                activePlayer = mediaPlayer;
                isPlaying = true;
                hasTrackLoaded = true;

                broadcastStatus();
                updateNotification();
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi phát bài hát: " + e.getMessage());
            isPlaying = false;
            broadcastStatus();
        }
    }

    private void pauseTrack() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        isPlaying = false;
        broadcastStatus();
        updateNotification();
    }

    private void resumeTrack() {
        if (mediaPlayer != null && mediaPlayer != activePlayer) {
            return; // link nhạc còn đang tải, onPrepared sẽ tự phát
        }
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
            hasTrackLoaded = true;
            broadcastStatus();
            updateNotification();
        } else {
            playTrack(currentRawRes, currentMusicUrl, false);
        }
    }

    private void stopTrack() {
        isBuffering = false;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            releaseMediaPlayer();
        }
        isPlaying = false;
        broadcastStatus();
        NotificationHelper.cancelMusicNotification(this);
    }

    /**
     * Chuyển sang bài tiếp theo.
     * Đang phát một danh sách (Danh sách nhạc đã lưu): chỉ lấy bài trong danh sách đó, hết danh sách thì dừng.
     * Không có danh sách: lấy bài tiếp theo trong Bảng tin (SQLite), bỏ qua bài riêng tư của người khác.
     *
     * @param manual true nếu người dùng bấm nút Tiếp theo, false nếu tự chuyển khi hết bài
     */
    private void playNextTrack(boolean manual) {
        List<PostModel> posts;
        if (playQueue != null) {
            posts = playQueue;
        } else {
            posts = new java.util.ArrayList<>();
            for (PostModel p : DatabaseHelper.getInstance(this).getAllPosts()) {
                if (!p.isPrivate() || SessionManager.isCurrentUser(this, p.getAuthorUsername())) {
                    posts.add(p);
                }
            }
        }
        if (posts.isEmpty()) {
            broadcastStatus();
            updateNotification();
            return;
        }
        int currentIndex = indexOfPost(posts, currentPostId);
        int nextIndex;
        if (isShuffleEnabled && posts.size() > 1) {
            // Phát ngẫu nhiên: chọn một bài khác bài đang phát (trong đúng phạm vi đang phát)
            do {
                nextIndex = random.nextInt(posts.size());
            } while (nextIndex == currentIndex);
        } else if (playQueue != null) {
            nextIndex = currentIndex + 1;
            if (nextIndex >= posts.size()) {
                // Đã phát hết danh sách: dừng lại, không nhảy ra bài ngoài danh sách
                finishQueue(manual ? "Đây là bài cuối cùng trong " + playQueueName + "."
                        : "Đã phát hết " + playQueueName + ".");
                return;
            }
        } else {
            nextIndex = (currentIndex + 1) % posts.size();
        }
        playPost(posts.get(nextIndex));
    }

    private static int indexOfPost(List<PostModel> posts, String postId) {
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() != null && posts.get(i).getId().equals(postId)) return i;
        }
        return -1;
    }

    private void playPost(PostModel next) {
        currentSongTitle = next.getSongTitle();
        currentArtistName = next.getArtistName();
        currentPostId = next.getId();
        currentGenre = next.getGenre();
        currentRawRes = next.getRawAudioRes() != 0 ? next.getRawAudioRes() : R.raw.song_sample_1;
        playTrack(currentRawRes, next.getMusicUrl(), false);
    }

    /**
     * Hết danh sách phát. Bấm Tiếp ở bài cuối: giữ nguyên bài đang nghe.
     * Tự hết bài cuối: dừng và quay về đầu bài (bấm Play sẽ nghe lại).
     */
    private void finishQueue(String message) {
        if (!isPlaying && mediaPlayer != null) {
            try {
                mediaPlayer.seekTo(0);
            } catch (Exception ignored) {}
        }
        broadcastStatus(message);
        updateNotification();
    }

    private void broadcastStatus() {
        broadcastStatus(null);
    }

    /** @param message thông báo cần hiện cho người dùng (null = không có) */
    private void broadcastStatus(String message) {
        // Thời lượng & vị trí thật của file âm thanh đang phát
        currentDuration = getDuration();
        currentPosition = getCurrentPosition();
        Intent intent = new Intent(ACTION_MUSIC_STATUS_CHANGED);
        intent.setPackage(getPackageName());
        intent.putExtra(EXTRA_IS_PLAYING, isPlaying);
        intent.putExtra(EXTRA_SONG_TITLE, currentSongTitle);
        intent.putExtra(EXTRA_ARTIST_NAME, currentArtistName);
        intent.putExtra(EXTRA_POST_ID, currentPostId);
        intent.putExtra(EXTRA_DURATION, currentDuration);
        intent.putExtra(EXTRA_POSITION, currentPosition);
        if (message != null) intent.putExtra(EXTRA_MESSAGE, message);
        sendBroadcast(intent);
    }

    private void updateNotification() {
        boolean allowNotif = prefs.getBoolean("notifications", true);
        if (allowNotif) {
            NotificationHelper.showMusicNotification(this, currentSongTitle, currentArtistName, isPlaying, isBuffering);
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // Đã tạm dừng (bởi người dùng / mất mạng) ngay sát cuối bài thì không tự phát tiếp
        if (!isPlaying) {
            return;
        }
        // Nhạc trực tuyến "kết thúc" sớm do mất mạng (hết bộ đệm): không tự chuyển sang bài khác
        if (isStreamingCurrent() && !SyncManager.get(this).isOnline()) {
            int dur = getDuration();
            int pos = getCurrentPosition();
            if (dur <= 0 || pos < dur - 2000) {
                streamFailed("📴 Mất kết nối mạng — đã dừng phát nhạc trực tuyến. Hãy kết nối lại rồi bấm phát.");
                return;
            }
        }
        if (isRepeatEnabled) {
            // Lặp lại bài hát hiện tại
            mp.seekTo(0);
            mp.start();
            isPlaying = true;
            broadcastStatus();
            return;
        }
        boolean autoNext = prefs.getBoolean("auto_next", true);
        // Đang nghe một danh sách (Danh sách nhạc đã lưu) thì luôn phát tiếp bài kế trong danh sách
        if (autoNext || playQueue != null) {
            isPlaying = false; // bài vừa kết thúc; bài kế tiếp sẽ bật lại trạng thái đang phát
            playNextTrack(false);
        } else {
            // Dừng ở cuối bài, quay về đầu để bấm Play nghe lại
            isPlaying = false;
            mp.seekTo(0);
            broadcastStatus();
            updateNotification();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
        releaseMediaPlayer();
        isPlaying = false;
        broadcastStatus();
        return true;
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            if (activePlayer == mediaPlayer) activePlayer = null;
            mediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        stopTrack();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Helper static methods để Activity gọi thuận tiện

    /** Phát một bài lẻ (Bảng tin, Khám phá, Chi tiết...): thoát khỏi danh sách phát đang dùng. */
    public static void play(Context context, PostModel post) {
        playQueue = null;
        playQueueName = null;
        startPlay(context, post);
    }

    /**
     * Phát một danh sách bài hát, bắt đầu từ vị trí {@code startIndex}.
     * Bài tiếp theo (tự chuyển hoặc bấm Tiếp) chỉ nằm trong danh sách này cho tới khi hết danh sách.
     */
    public static void playQueue(Context context, String name, List<PostModel> tracks, int startIndex) {
        if (tracks == null || tracks.isEmpty()) return;
        int index = Math.max(0, Math.min(startIndex, tracks.size() - 1));
        playQueue = new java.util.ArrayList<>(tracks);
        playQueueName = name;
        startPlay(context, playQueue.get(index));
    }

    private static void startPlay(Context context, PostModel post) {
        // Báo ngoại tuyến NGAY tại màn hình đang bấm (Toast từ Activity luôn hiển thị)
        boolean offline = isOfflineStream(context, post);
        if (offline) {
            android.widget.Toast.makeText(context, OFFLINE_STREAM_MESSAGE, android.widget.Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_SONG_TITLE, post.getSongTitle());
        intent.putExtra(EXTRA_ARTIST_NAME, post.getArtistName());
        intent.putExtra(EXTRA_POST_ID, post.getId());
        intent.putExtra(EXTRA_RAW_RES, post.getRawAudioRes());
        intent.putExtra(EXTRA_GENRE, post.getGenre());
        intent.putExtra(EXTRA_MUSIC_URL, post.getMusicUrl());
        intent.putExtra(EXTRA_ANNOUNCED, offline);
        context.startService(intent);
    }

    /** Thông báo "Đang phát / Đang tải" sau khi bấm phát (trường hợp ngoại tuyến đã được báo khi gọi play). */
    public static void announce(Context context, PostModel post) {
        if (isOfflineStream(context, post)) return;
        android.widget.Toast.makeText(context, startMessage(context, post), android.widget.Toast.LENGTH_SHORT).show();
    }

    public static void toggle(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_TOGGLE);
        context.startService(intent);
    }

    public static void pause(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_PAUSE);
        context.startService(intent);
    }

    public static void next(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_NEXT);
        context.startService(intent);
    }

    public static void seekTo(Context context, int msec) {
        currentPosition = msec;
        Intent intent = new Intent(context, MusicService.class);
        intent.setAction(ACTION_SEEK);
        intent.putExtra(EXTRA_SEEK_POSITION, msec);
        context.startService(intent);
    }

    /** Vị trí phát hiện tại (ms) đọc trực tiếp từ MediaPlayer. */
    public static int getCurrentPosition() {
        if (activePlayer != null) {
            try {
                currentPosition = activePlayer.getCurrentPosition();
            } catch (Exception ignored) {}
        }
        return currentPosition;
    }

    /** Thời lượng thật của bài hát (ms) đọc từ MediaPlayer. */
    public static int getDuration() {
        if (activePlayer != null) {
            try {
                int dur = activePlayer.getDuration();
                if (dur > 0) currentDuration = dur;
            } catch (Exception ignored) {}
        }
        return currentDuration;
    }
}
