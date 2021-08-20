package ru.scoltech.openran.speedtest.customButtons;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import ru.scoltech.openran.speedtest.R;

public class ShareButton extends androidx.appcompat.widget.AppCompatButton {
    public ShareButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setBackground(getContext().getDrawable(R.drawable.ic_share_btn));

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                shareTask();
            }
        });
    }

    private void shareTask() {
        //TODO
        Log.d("mytag", "shareTask: pressed share");
    }
}
