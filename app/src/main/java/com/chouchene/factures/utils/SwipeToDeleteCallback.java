package com.chouchene.factures.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.chouchene.factures.R;

public abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Context context;
    private final ColorDrawable background;
    private final int backgroundColor;
    private final Drawable deleteIcon;
    private final int intrinsicWidth;
    private final int intrinsicHeight;
    private final Paint clearPaint;
    private final Paint bgPaint;

    public SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.LEFT);
        this.context = context;
        this.backgroundColor = Color.parseColor("#EF5350"); // Modern Material Red
        this.background = new ColorDrawable();
        this.deleteIcon = ContextCompat.getDrawable(context, R.drawable.baseline_delete_24);
        this.intrinsicWidth = deleteIcon.getIntrinsicWidth();
        this.intrinsicHeight = deleteIcon.getIntrinsicHeight();
        this.clearPaint = new Paint();
        this.clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        this.bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        this.bgPaint.setColor(backgroundColor);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();
        boolean isCanceled = dX == 0f && !isCurrentlyActive;

        if (isCanceled) {
            clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            return;
        }

        // Draw modern rounded background
        float cornerRadius = 12 * context.getResources().getDisplayMetrics().density;
        int margin = (int) (8 * context.getResources().getDisplayMetrics().density);

        if (dX < 0) {
            c.drawRoundRect(
                    itemView.getRight() + dX + margin,
                    itemView.getTop() + margin,
                    itemView.getRight() - margin,
                    itemView.getBottom() - margin,
                    cornerRadius, cornerRadius,
                    bgPaint
            );

            // Calculate position of delete icon
            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int iconMargin = (int) (16 * context.getResources().getDisplayMetrics().density);
            int deleteIconLeft = itemView.getRight() - iconMargin - intrinsicWidth - margin;
            int deleteIconRight = itemView.getRight() - iconMargin - margin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;

            // Only draw icon if there is enough space
            if (Math.abs(dX) > (iconMargin + intrinsicWidth + margin * 2)) {
                deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                deleteIcon.setTint(Color.WHITE);
                deleteIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
        c.drawRect(left, top, right, bottom, clearPaint);
    }
}
