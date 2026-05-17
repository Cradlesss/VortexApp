package com.project_void.vortexapp.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import com.project_void.vortexapp.R;
import com.project_void.vortexapp.core.ui.ThemeUtils;

public final class VortexDialogSingleChoice {
    public interface OnConfirm {
        void onConfirm(int idx);
    }

    public static void show(
      Context ctx,
      String title,
      String[] items,
      int selectedIdx,
      String cancelText,
      String okText,
      OnConfirm cb
    ){
        Context theme = new ContextThemeWrapper(ctx, R.style.Theme_Vortex_Material);
        Dialog d = new Dialog(theme);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View root = LayoutInflater.from(ctx).inflate(R.layout.vortex_dialog_single_choice, null, false);
        d.setContentView(root);

        TextView tvTitle = root.findViewById(R.id.title);
        TextView btnCancel = root.findViewById(R.id.btn_cancel);
        TextView btnOk = root.findViewById(R.id.btn_ok);
        RadioGroup group = root.findViewById(R.id.group);

        tvTitle.setText(title);
        btnCancel.setText(cancelText);
        btnOk.setText(okText);

        int[] ids = new int[items.length];

        for (int i = 0; i < items.length; i++){
            RadioButton rb = new RadioButton(theme);
            rb.setText(items[i]);
            rb.setTextColor(ThemeUtils.resolveColor(theme, R.attr.vortexTextPrimary));
            rb.setPadding(12,14,6,14);
            rb.setId(ids[i] = View.generateViewId());
            group.addView(rb);
        }

        if (selectedIdx >= 0 && selectedIdx < ids.length)
            group.check(ids[selectedIdx]);

        btnCancel.setOnClickListener(v -> d.dismiss());

        btnOk.setOnClickListener(v -> {
           int checkedId = group.getCheckedRadioButtonId();
           int idx = 0;
           for (int i = 0; i < ids.length; i++)
               if (ids[i] == checkedId) {
                   idx = i;
                   break;
               }
           d.dismiss();
           if (cb != null) cb.onConfirm(idx);
        });

        d.show();
    }

    private VortexDialogSingleChoice(){}
}
