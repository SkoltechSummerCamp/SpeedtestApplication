package ru.scoltech.openran.speedtest.CustomButtons;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import ru.scoltech.openran.speedtest.R;

public class SaveButton extends androidx.appcompat.widget.AppCompatButton {
    public SaveButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setBackground(getContext().getDrawable(R.drawable.ic_save_btn));

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveTask();
            }
        });
    }

    private void saveTask() {
        //TODO
        Log.d("mytag", "saveTask: pressed save");
    }
}
