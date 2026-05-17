package com.project_void.vortexapp.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;
import com.project_void.vortexapp.R;
import java.util.Objects;

public final class VortexDialogInfo {
    public interface CallBack {
        void onConfirm();

        default void onCancel() {}
    }

    private VortexDialogInfo() {
    }

    public static void show(
            @NonNull Context ctx,
            @NonNull String title,
            @NonNull String message,
            @NonNull String primaryText,
            @Nullable String cancelText,
            @NonNull CallBack cb
    ) {
        Context theme = new ContextThemeWrapper(ctx, R.style.Theme_Vortex_Material);
        Dialog d = new Dialog(theme);
        d.setContentView(LayoutInflater.from(theme).inflate(R.layout.vortex_dialog_info, null));

        Window w = d.getWindow();
        if (w != null)
            w.setBackgroundDrawableResource(android.R.color.transparent);

        ((TextView) d.findViewById(R.id.title)).setText(title);
        ((TextView) d.findViewById(R.id.message)).setText(message);

        MaterialButton btnConfirm = d.findViewById(R.id.btn_primary);
        btnConfirm.setText(primaryText);
        btnConfirm.setOnClickListener(v -> {
            cb.onConfirm();
            d.dismiss();
        });

        MaterialButton btnCancel = d.findViewById(R.id.btn_cancel);
        btnCancel.setText(Objects.requireNonNullElse(cancelText, ctx.getString(R.string.cancel)));

        btnCancel.setOnClickListener(v -> {
            cb.onCancel();
            d.dismiss();
        });
        d.show();
    }

    public static void show(
            @NonNull Context ctx,
            @NonNull String title,
            @NonNull String message,
            @NonNull String primaryText,
            @NonNull CallBack cb
    ) {
        show(ctx, title, message, primaryText, null, cb);
    }

    public static void show(
            @NonNull Context ctx,
            @NonNull String title,
            @NonNull String message,
            @NonNull CallBack cb
    ) {
        show(ctx, title, message, ctx.getString(R.string.ok), cb);
    }
}
