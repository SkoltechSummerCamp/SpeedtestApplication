package com.example.wavemockapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

//TODO: replace there and in xml deprecated RelativeLayout
public class CardView extends RelativeLayout {

    TextView integerSpeedTV;
    TextView fractionSpeedTV;
    TextView pingTV;

    int fractionSpeed;
    int integerSpeed;
    int ping;
    String mStatus;


    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        inflate(getContext(), R.layout.card_layout, this);

        init();
        parseAttrs(context, attrs);
    }

    private void init() {
        integerSpeedTV = findViewById(R.id.integer_speed);
        fractionSpeedTV = findViewById(R.id.fraction_speed);

        pingTV = findViewById(R.id.value_ping);
    }

    private void parseAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CardView);
        int count = typedArray.getIndexCount();

        try {
            for (int i = 0; i < count; ++i) {

                int attr = typedArray.getIndex(i);

                if (attr == R.styleable.CardView_fraction_speed) {
                    fractionSpeed = typedArray.getInteger(attr, 0);

                    setFractionSpeed(fractionSpeed);
                } else if (attr == R.styleable.CardView_integer_speed) {
                    integerSpeed = typedArray.getInteger(attr, 0);

                    setIntegerSpeed(integerSpeed);

                } else if (attr == R.styleable.CardView_ping) {
                    ping = typedArray.getInteger(attr, 0);

                    setPing(ping);
                } else if (attr == R.styleable.CardView_status) {
                    mStatus = typedArray.getString(attr);

                    setStatus(mStatus);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            typedArray.recycle();
        }
    }

    //    status is download or upload for drawable arrow
    private void setStatus(String status) {
        mStatus = status;
    }

    private void setPing(int ping) {
        pingTV.setText(String.valueOf(ping));
    }

    public void setIntegerSpeed(int speed) {
        integerSpeedTV.setText(String.valueOf(speed));
    }

    public void setFractionSpeed(int speed) {
        fractionSpeedTV.setText(String.valueOf(speed));
    }

    private void setDrawableStatus() {
    }

}
