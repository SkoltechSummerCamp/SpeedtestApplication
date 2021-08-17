package com.example.wavemockapplication;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SpeedManager {
    public List<String> uploadList;
    public List<String> downloadList;

    public SpeedManager(List<String> list) {

        downloadList = new ArrayList<>();
        uploadList = new ArrayList<>();

        for (int i = 0; i < list.size() / 2; i++) {
            downloadList.add(list.get(i));
        }
        for (int i = list.size() / 2; i < list.size(); i++) {
            uploadList.add(list.get(i));
        }
    }


    private Pair<Integer, Integer> convertBitToMbps(String speed) {
        if (speed != null) {

            int int_speed = Integer.parseInt(speed) / 1000000;
            int frac_speed = Integer.parseInt(speed) % 1000000;

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
                sum += Integer.parseInt(sp.substring(0, sp.length() - 3));
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

}
