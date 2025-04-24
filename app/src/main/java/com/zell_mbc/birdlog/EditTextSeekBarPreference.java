package com.zell_mbc.birdlog;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.PreferenceViewHolder;
import androidx.preference.SeekBarPreference;
import android.widget.EditText;
import android.app.AlertDialog;
import android.text.InputType;
import android.widget.TextView;

public class EditTextSeekBarPreference extends SeekBarPreference {
    Context mContext;
    public EditTextSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView seekBarValueTextView = (TextView) holder.findViewById(androidx.preference.R.id.seekbar_value);
        if (seekBarValueTextView != null) {
            seekBarValueTextView.setOnClickListener(v -> showInputDialog(seekBarValueTextView));
        }
    }

    private void showInputDialog(final TextView seekBarValueTextView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getTitle());

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton(mContext.getString(android.R.string.ok), (dialog, which) -> {
            String value = input.getText().toString();
            try {
                int intValue = Integer.parseInt(value);
                if (intValue >= getMin() && intValue <= getMax()) {
                    setValue(intValue);
                    seekBarValueTextView.setText(value);
                }
            } catch (NumberFormatException ignored) {}
        });

        builder.setNegativeButton(mContext.getString(android.R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }
}

