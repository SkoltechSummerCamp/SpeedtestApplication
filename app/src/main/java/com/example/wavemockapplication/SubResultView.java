package com.example.wavemockapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

//TODO: replace there and in xml deprecated RelativeLayout
public class SubResultView extends RelativeLayout {

    String downloadSpeed;
    String uploadSpeed;

    TextView downloadSpeedTV;
    TextView uploadSpeedTV;

    public SubResultView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.subresult_layout, this);

        init();
        parseAttrs(context, attrs);
    }

    private void parseAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SubResultView);
        int count = typedArray.getIndexCount();

        try {
            for (int i = 0; i < count; ++i) {

                int attr = typedArray.getIndex(i);

                if (attr == R.styleable.SubResultView_download_speed) {
                    downloadSpeed = typedArray.getString(attr);

                    setDownloadSpeed(downloadSpeed);

                } else if (attr == R.styleable.SubResultView_upload_speed) {
                    uploadSpeed = typedArray.getString(attr);

                    setUploadSpeed(uploadSpeed);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            typedArray.recycle();
        }
    }

    public void setUploadSpeed(String speed) {
        uploadSpeedTV.setText(speed);
    }

    public void setDownloadSpeed(String speed) {
        downloadSpeedTV.setText(speed);
    }

    private void init() {
        downloadSpeedTV = findViewById(R.id.value_download);
        uploadSpeedTV = findViewById(R.id.value_upload);
    }


}
