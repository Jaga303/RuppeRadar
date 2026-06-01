package com.example.roomlibrary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class CategoryAdapter
        extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    Context context;
    ArrayList<CategoryModel> list;
    OnCategoryClickListener listener;   // ✅ ADD THIS

    // ✅ UPDATED CONSTRUCTOR
    public CategoryAdapter(Context context,
                           ArrayList<CategoryModel> list,
                           OnCategoryClickListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {

        CategoryModel model = list.get(position);

        h.tvCategory.setText(model.name);
        h.imgCategory.setImageResource(model.icon);
        h.imgCategory.getBackground().setTint(model.color);

        // ✅ CLICK EVENT
        h.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(model);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCategory;
        TextView tvCategory;

        ViewHolder(View itemView) {
            super(itemView);
            imgCategory = itemView.findViewById(R.id.imgCategory);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}
