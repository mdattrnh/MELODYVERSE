package com.example.melodyverse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình Khám phá (Bài 2 - Activities & Bài 8 - Multimedia), chỉ gồm 2 khu vực:
 * 1. Bảng xếp hạng âm nhạc: tự xếp theo tổng lượt thả tim của từng bài hát, cập nhật khi có người thả / bỏ tim.
 * 2. Vòng xoay đĩa than: lướt chọn bài hát nổi bật một cách trực quan.
 */
public class DiscoverActivity extends BaseActivity {

    private DatabaseHelper dbHelper;

    // Bảng xếp hạng
    private View cardTop1, headerChart;
    private TextView tvBannerTitle, tvBannerArtist, tvChartEmpty;
    private ImageView btnBannerPlay;
    private RecyclerView rvChart;
    private TrackListAdapter chartAdapter;
    private final List<MusicChart.Entry> chartEntries = new ArrayList<>();
    /** Các hàng đang hiển thị (đã lọc theo ô tìm kiếm). */
    private final List<PostModel> chartShown = new ArrayList<>();
    private final List<Integer> chartShownLikes = new ArrayList<>();

    // Vòng xoay đĩa than (cuối trang)
    private DiscWheelView discWheel;
    private View discWheelSection;
    private final List<PostModel> wheelPosts = new ArrayList<>();

    // Tìm kiếm tại chỗ
    private String searchQuery = "";
    private View layoutEmpty;
    private TextView tvEmptyMsg, tvSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Bơm giao diện Khám phá vào FrameLayout của BaseActivity
        setContentLayout(R.layout.activity_discover);
        dbHelper = DatabaseHelper.getInstance(this);

        setupChart();
        setupSearch();
        setupDiscWheel();
        loadChart();
        refreshChartFromServer();
    }

    // ===================================================================================
    // Bảng xếp hạng
    // ===================================================================================

    private void setupChart() {
        cardTop1 = findViewById(R.id.card_top1_banner);
        headerChart = findViewById(R.id.layout_chart_section_header);
        tvBannerTitle = findViewById(R.id.tv_banner_title);
        tvBannerArtist = findViewById(R.id.tv_banner_artist);
        btnBannerPlay = findViewById(R.id.btn_banner_play);
        tvChartEmpty = findViewById(R.id.tv_discover_chart_empty);
        rvChart = findViewById(R.id.rv_discover_chart);

        rvChart.setLayoutManager(new LinearLayoutManager(this));
        chartAdapter = new TrackListAdapter(this, chartShown, (track, position) -> togglePlay(track));
        chartAdapter.setRankStyle(true);
        chartAdapter.setSubtitleProvider((track, position) ->
                track.getArtistName() + " · " + MusicChart.likesLabel(chartShownLikes.get(position)));
        chartAdapter.setOnTrackLongClickListener((track, position) -> openPost(track));
        rvChart.setAdapter(chartAdapter);

        View.OnClickListener playTop1 = v -> {
            if (!chartEntries.isEmpty()) togglePlay(chartEntries.get(0).post);
        };
        if (btnBannerPlay != null) btnBannerPlay.setOnClickListener(playTop1);
        if (cardTop1 != null) cardTop1.setOnClickListener(playTop1);
    }

    /** Đọc lại bảng xếp hạng từ SQLite (gọi mỗi khi lượt tim thay đổi: mình thả tim, realtime, quay lại màn hình). */
    private void loadChart() {
        chartEntries.clear();
        chartEntries.addAll(MusicChart.rank(dbHelper.getAllPosts(), MusicChart.TOP_SIZE));
        applySearch();
    }

    /** Lấy các bài nhiều tim nhất trên toàn hệ thống từ Firebase (có mạng), rồi xếp hạng lại. */
    private void refreshChartFromServer() {
        SyncManager.get(this).refreshChart((status, more) -> {
            if (isFinishing() || isDestroyed()) return;
            if (status == SyncManager.Status.OK) {
                loadChart();
                loadWheelPosts();
            }
        });
    }

    private void togglePlay(PostModel post) {
        if (post.getId() != null && post.getId().equals(MusicService.currentPostId) && MusicService.isPlaying) {
            MusicService.toggle(this);
        } else {
            MusicService.play(this, post);
            MusicService.announce(this, post);
        }
    }

    private void openPost(PostModel post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_POST_ID, post.getId());
        startActivity(intent);
    }

    // ===================================================================================
    // Tìm kiếm tại chỗ (lọc Bảng xếp hạng và Vòng xoay đĩa than)
    // ===================================================================================

    private void setupSearch() {
        View layoutSearchBar = findViewById(R.id.layout_discover_search_bar);
        android.widget.EditText etSearch = findViewById(R.id.et_discover_search);
        ImageView ivClearSearch = findViewById(R.id.iv_clear_discover_search);
        View btnReset = findViewById(R.id.btn_reset_discover_search);
        tvSummary = findViewById(R.id.tv_discover_search_summary);
        layoutEmpty = findViewById(R.id.layout_discover_search_empty);
        tvEmptyMsg = findViewById(R.id.tv_discover_search_empty_msg);

        findViewById(R.id.iv_discover_search).setOnClickListener(v -> {
            if (layoutSearchBar == null) return;
            if (layoutSearchBar.getVisibility() == View.VISIBLE) {
                layoutSearchBar.setVisibility(View.GONE);
                if (etSearch != null) etSearch.setText("");
            } else {
                layoutSearchBar.setVisibility(View.VISIBLE);
                if (etSearch != null) {
                    etSearch.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        if (ivClearSearch != null && etSearch != null) {
            ivClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }
        if (btnReset != null && etSearch != null) {
            btnReset.setOnClickListener(v -> etSearch.setText(""));
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim();
                    if (ivClearSearch != null) {
                        ivClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    applySearch();
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    /** Áp ô tìm kiếm lên Bảng xếp hạng + Vòng xoay đĩa than (thứ hạng giữ nguyên theo lượt tim). */
    private void applySearch() {
        String q = MusicChart.normalize(searchQuery);
        chartShown.clear();
        chartShownLikes.clear();
        for (MusicChart.Entry e : chartEntries) {
            if (q.isEmpty() || matches(q, e.post)) {
                chartShown.add(e.post);
                chartShownLikes.add(e.totalLikes);
            }
        }
        chartAdapter.notifyDataSetChanged();

        // Banner Top 1: luôn là bài nhiều tim nhất (ẩn khi đang tìm mà Top 1 không khớp)
        MusicChart.Entry top1 = chartEntries.isEmpty() ? null : chartEntries.get(0);
        boolean showTop1 = top1 != null && (q.isEmpty() || matches(q, top1.post));
        if (cardTop1 != null) cardTop1.setVisibility(showTop1 ? View.VISIBLE : View.GONE);
        if (top1 != null) {
            if (tvBannerTitle != null) tvBannerTitle.setText(top1.post.getSongTitle());
            if (tvBannerArtist != null) {
                tvBannerArtist.setText(top1.post.getArtistName() + " · " + MusicChart.likesLabel(top1.totalLikes));
            }
        }

        boolean anyChart = !chartShown.isEmpty();
        if (headerChart != null) headerChart.setVisibility(anyChart || q.isEmpty() ? View.VISIBLE : View.GONE);
        rvChart.setVisibility(anyChart ? View.VISIBLE : View.GONE);
        if (tvChartEmpty != null) tvChartEmpty.setVisibility(q.isEmpty() && chartEntries.isEmpty() ? View.VISIBLE : View.GONE);

        int wheelMatches = applyWheelFilter();
        updatePlayButtons();

        if (q.isEmpty()) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (tvSummary != null) tvSummary.setVisibility(View.GONE);
            return;
        }
        int matchCount = chartShown.size() + wheelMatches;
        if (matchCount == 0) {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
            if (tvEmptyMsg != null) tvEmptyMsg.setText("Không tìm thấy bài hát nào phù hợp cho: \"" + searchQuery + "\".");
            if (tvSummary != null) tvSummary.setVisibility(View.GONE);
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (tvSummary != null) {
                tvSummary.setVisibility(View.VISIBLE);
                tvSummary.setText("Tìm thấy " + matchCount + " kết quả khám phá cho: \"" + searchQuery + "\"");
            }
        }
    }

    private static boolean matches(String queryNorm, PostModel p) {
        for (String src : new String[]{p.getSongTitle(), p.getArtistName(), p.getGenre(), p.getAuthorName()}) {
            if (src != null && MusicChart.normalize(src).contains(queryNorm)) return true;
        }
        return false;
    }

    // ===================================================================================
    // Vòng xoay đĩa than
    // ===================================================================================

    /** Vòng xoay đĩa than: bài viết công khai trong app, hiện dần khi người dùng lướt tới. */
    private void setupDiscWheel() {
        discWheelSection = findViewById(R.id.layout_disc_wheel_section);
        discWheel = findViewById(R.id.rv_disc_wheel);
        if (discWheel == null || discWheelSection == null) return;

        discWheel.setListener(new DiscWheelView.Listener() {
            @Override
            public void onPlayClick(PostModel post) {
                if (post.getId() != null && post.getId().equals(MusicService.currentPostId) && MusicService.hasTrackLoaded) {
                    MusicService.toggle(DiscoverActivity.this);
                } else {
                    MusicService.play(DiscoverActivity.this, post);
                }
            }

            @Override
            public void onOpenPost(PostModel post) {
                openPost(post);
            }
        });

        loadWheelPosts();

        // Phần vòng xoay mờ và nằm thấp hơn một chút, lướt tới đâu hiện ra tới đó rồi vòng cung bung ra
        float density = getResources().getDisplayMetrics().density;
        discWheelSection.setAlpha(0f);
        discWheelSection.setTranslationY(56 * density);
        discWheel.hideForIntro();

        androidx.core.widget.NestedScrollView scroll = findViewById(R.id.scroll_discover);
        if (scroll != null) {
            scroll.setOnScrollChangeListener((androidx.core.widget.NestedScrollView.OnScrollChangeListener)
                    (v, x, y, oldX, oldY) -> revealDiscWheelIfVisible(scroll));
            scroll.post(() -> revealDiscWheelIfVisible(scroll));
        }
    }

    private boolean wheelRevealed = false;

    private void revealDiscWheelIfVisible(androidx.core.widget.NestedScrollView scroll) {
        if (discWheelSection == null) return;
        int[] sectionPos = new int[2];
        int[] scrollPos = new int[2];
        discWheelSection.getLocationOnScreen(sectionPos);
        scroll.getLocationOnScreen(scrollPos);
        int visibleTop = sectionPos[1] - scrollPos[1];
        int viewport = scroll.getHeight();
        if (viewport == 0) return;

        // Hiện dần theo quãng đường lướt: bắt đầu khi đầu phần lọt vào màn hình, rõ hẳn khi đã lên 1/3 màn hình
        float progress = (viewport - (float) visibleTop) / (viewport / 3f);
        progress = Math.max(0f, Math.min(1f, progress));
        float density = getResources().getDisplayMetrics().density;
        discWheelSection.setAlpha(progress);
        discWheelSection.setTranslationY((1f - progress) * 56 * density);

        if (!wheelRevealed && progress >= 0.6f) {
            wheelRevealed = true;
            discWheel.playIntro();
        }
    }

    private void loadWheelPosts() {
        if (discWheel == null) return;
        wheelPosts.clear();
        for (PostModel p : dbHelper.getAllPosts()) {
            if (!p.isPrivate() && p.getSongTitle() != null && !p.getSongTitle().trim().isEmpty()) {
                wheelPosts.add(p);
            }
        }
        applyWheelFilter();
        discWheel.setPlayingPostId(MusicService.isPlaying ? MusicService.currentPostId : null);
    }

    /** Lọc vòng xoay theo ô tìm kiếm; trả về số đĩa khớp (0 khi không tìm kiếm). */
    private int applyWheelFilter() {
        if (discWheel == null) return 0;
        List<PostModel> shown = new ArrayList<>();
        String q = MusicChart.normalize(searchQuery);
        for (PostModel p : wheelPosts) {
            if (q.isEmpty() || matches(q, p)) shown.add(p);
        }
        discWheel.setPosts(shown);
        if (discWheelSection != null) {
            discWheelSection.setVisibility(shown.isEmpty() ? View.GONE : View.VISIBLE);
        }
        return q.isEmpty() ? 0 : shown.size();
    }

    // ===================================================================================
    // Đồng bộ dữ liệu & trạng thái phát nhạc
    // ===================================================================================

    @Override
    protected void onLocalDataChanged() {
        // Lượt tim thay đổi (realtime / vừa đồng bộ): xếp hạng lại ngay
        loadChart();
        loadWheelPosts();
    }

    @Override
    protected void onReconnectedRefresh() {
        refreshChartFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Quay lại sau khi thả / bỏ tim ở Bảng tin hoặc Chi tiết bài viết: thứ hạng cập nhật theo
        if (dbHelper != null) loadChart();
    }

    /** Đồng bộ icon Play/Pause trên BXH ngay khi MusicService đổi trạng thái (Broadcast). */
    @Override
    public void updateMiniPlayerUi() {
        super.updateMiniPlayerUi();
        updatePlayButtons();
        if (chartAdapter != null) chartAdapter.notifyDataSetChanged();
    }

    private void updatePlayButtons() {
        boolean top1Playing = MusicService.isPlaying && !chartEntries.isEmpty()
                && chartEntries.get(0).post.getId().equals(MusicService.currentPostId);
        if (btnBannerPlay != null) {
            btnBannerPlay.setImageResource(top1Playing ? R.drawable.ic_pause : R.drawable.ic_play);
        }
        if (discWheel != null) {
            discWheel.setPlayingPostId(MusicService.isPlaying ? MusicService.currentPostId : null);
        }
    }

    @Override
    protected int getCheckedItem() {
        return R.id.nav_discover;
    }
}
