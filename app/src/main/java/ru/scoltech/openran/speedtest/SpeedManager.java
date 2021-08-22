package ru.scoltech.openran.speedtest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Pair;
import android.view.View;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SpeedManager {
    private List<String> uploadList;
    private List<String> downloadList;


    private static SpeedManager instance;

    private SpeedManager() {
    }

    public static SpeedManager getInstance() {
        if (instance == null) {
            instance = new SpeedManager();
        }
        return instance;
    }

    public void attachList(List<String> list) {
        downloadList = new ArrayList<>();
        uploadList = new ArrayList<>();

        downloadList.addAll(list.subList(0, list.size() / 2));
        uploadList.addAll(list.subList(list.size() / 2, list.size()));
    }


    private Pair<Integer, Integer> convertBitToMbps(String speed) {
        if (speed != null) {

            int speedInt = Integer.parseInt(speed);

            int int_speed = speedInt / 1000000;
            int frac_speed = speedInt % 1000000;

            return new Pair<>(int_speed, frac_speed);
        }
        return null;
    }


    public Pair<Integer, Integer> getSpeedWithPrecision(String strSpeed, int precision) {
        Pair<Integer, Integer> speed = convertBitToMbps(strSpeed);

        if (speed.second > 99) {
            String second = String.valueOf(speed.second).substring(0, precision);
            return new Pair<>(speed.first, Integer.valueOf(second));

        } else {
            return speed;
        }
    }


    private Pair<Integer, Integer> getAverageSpeed(List<String> list) {

        int sum = 0;
        if (!list.isEmpty()) {
            for (String sp : list) {
                sum += Integer.parseInt(sp) / 1000;
            }
            int speed = sum / list.size();

            int int_speed = speed / 1000;
            int frac_speed = speed % 1000;

            if (frac_speed > 99)
                return new Pair<>(int_speed, frac_speed / 10);
            else
                return new Pair<>(int_speed, frac_speed);
        }
        return null;
    }

    public Pair<Integer, Integer> getAverageUploadSpeed() {
        return getAverageSpeed(uploadList);
    }

    public Pair<Integer, Integer> getAverageDownloadSpeed() {
        return getAverageSpeed(downloadList);
    }

    public List<String> getDownloadList() {
        return downloadList;
    }

    public List<String> getUploadList() {
        return uploadList;
    }


    public Bitmap generateImage(Context context) {

        Bitmap bg = BitmapFactory.decodeResource(context.getResources(), R.drawable.generated_result_background);

        Bitmap background = bg.copy(Bitmap.Config.ARGB_8888, true);
        Canvas backgroundCanvas = new Canvas(background);
        backgroundCanvas.drawBitmap(background, 0, 0, null);


        View v = ((Activity) context).findViewById(R.id.result);

        Bitmap foreground = Bitmap.createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas foregroundCanvas = new Canvas(foreground);
        v.draw(foregroundCanvas);


        Bitmap combo = Bitmap.createBitmap(background.getWidth(), background.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas comboCanvas = new Canvas(combo);
        comboCanvas.drawBitmap(background, 0f, 0f, null);
        comboCanvas.drawBitmap(foreground, 10f, 10f, null);


        Typeface futuraPtMedium = ResourcesCompat.getFont(context, R.font.futura_pt_medium);

        Paint textPaint = new Paint();
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(context.getColor(R.color.neutral_100));
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setTypeface(futuraPtMedium);
        textPaint.setAntiAlias(true);


        String timestamp = new SimpleDateFormat("HH:mm\tdd.MM.yyyy", Locale.ROOT).format(new Date());
        comboCanvas.drawText(timestamp, background.getWidth() - 50f, background.getHeight() - 20f, textPaint);
        comboCanvas.drawText("Speedtest 5G", background.getWidth() - 50f, background.getHeight() - 50f, textPaint);

        return combo;

    }
}
