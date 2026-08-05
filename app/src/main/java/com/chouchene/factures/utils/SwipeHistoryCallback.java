package com.chouchene.factures.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;

public abstract class SwipeHistoryCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final Drawable checkIcon;
    private final Drawable shareIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final Paint paint;

    public SwipeHistoryCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.context = context;
        this.checkIcon = ContextCompat.getDrawable(context, R.drawable.rounded_check_24);
        this.shareIcon = ContextCompat.getDrawable(context, R.drawable.rounded_share_24);
        this.intrinsicWidth = checkIcon != null ? checkIcon.getIntrinsicWidth() : 0;
        this.intrinsicHeight = checkIcon != null ? checkIcon.getIntrinsicHeight() : 0;
        this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        float cornerRadius = 12 * context.getResources().getDisplayMetrics().density;
        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);
        int iconMargin = (int) (16 * context.getResources().getDisplayMetrics().density);

        if (dX > 0) { // Swiping Right -> Mark as Paid (Green)
            paint.setColor(Color.parseColor("#4CAF50"));
            c.drawRoundRect(
                    itemView.getLeft() + margin,
                    itemView.getTop() + margin,
                    itemView.getLeft() + dX + margin,
                    itemView.getBottom() - margin,
                    cornerRadius, cornerRadius,
                    paint
            );

            int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int iconLeft = itemView.getLeft() + iconMargin + margin;
            int iconRight = itemView.getLeft() + iconMargin + intrinsicWidth + margin;
            int iconBottom = iconTop + intrinsicHeight;

            if (dX > (iconMargin + intrinsicWidth + margin * 2) && checkIcon != null) {
                checkIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                checkIcon.setTint(Color.WHITE);
                checkIcon.draw(c);
            }
        } else if (dX < 0) { // Swiping Left -> Share (Blue)
            paint.setColor(Color.parseColor("#2196F3"));
            c.drawRoundRect(
                    itemView.getRight() + dX - margin,
                    itemView.getTop() + margin,
                    itemView.getRight() - margin,
                    itemView.getBottom() - margin,
                    cornerRadius, cornerRadius,
                    paint
            );

            int iconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int iconRight = itemView.getRight() - iconMargin - margin;
            int iconLeft = itemView.getRight() - iconMargin - intrinsicWidth - margin;
            int iconBottom = iconTop + intrinsicHeight;

            if (Math.abs(dX) > (iconMargin + intrinsicWidth + margin * 2) && shareIcon != null) {
                shareIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                shareIcon.setTint(Color.WHITE);
                shareIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
