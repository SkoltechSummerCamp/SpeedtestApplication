package com.example.wavemockapplication.CustomViews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.wavemockapplication.R;

public class ResultView extends TableLayout {

    TextView downloadSpeedTV;
    TextView uploadSpeedTV;
    TextView pingTV;

    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(context, R.layout.result_layout, this);

        init();
    }

    private void init() {
        downloadSpeedTV = findViewById(R.id.download_value_res);
        uploadSpeedTV = findViewById(R.id.upload_value_res);
        pingTV = findViewById(R.id.ping_value_res);
    }

    public void setDownloadSpeed(String speed) {
        downloadSpeedTV.setText(speed);
    }

    public void setUploadSpeed(String speed) {
        uploadSpeedTV.setText(speed);
    }

    public void setPing(String ping) {
        pingTV.setText(ping);
    }
}
