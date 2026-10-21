package com.example.melodyverse;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SuggestionAdapter extends RecyclerView.Adapter<SuggestionAdapter.ViewHolder> {

    private List<String> names;
    private List<String> usernames;
    private final int[] avatarColors = {
            Color.parseColor("#EF4444"), // Red
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#8B5CF6"), // Purple
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#10B981")  // Emerald
    };

    public SuggestionAdapter(List<String> names, List<String> usernames) {
        this.names = names;
        this.usernames = usernames;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = names.get(position);
        holder.tvName.setText(name);

        // Tạo chữ cái viết tắt nổi bật (VD: Quang Hùng -> QH)
        String initials = getInitials(name);
        if (holder.tvInitials != null) {
            holder.tvInitials.setText(initials);
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(avatarColors[position % avatarColors.length]);
            holder.tvInitials.setBackground(bg);
        }

        holder.itemView.setOnClickListener(v -> {
            // Mở trang cá nhân của nghệ sĩ / bạn bè được gợi ý
            Context ctx = v.getContext();
            Intent intent = new Intent(ctx, ProfileActivity.class);
            intent.putExtra(ProfileActivity.EXTRA_USERNAME, usernames.get(position));
            ctx.startActivity(intent);
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "M";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInitials;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_suggest_name);
            tvInitials = itemView.findViewById(R.id.tv_suggest_initials);
        }
    }
}
