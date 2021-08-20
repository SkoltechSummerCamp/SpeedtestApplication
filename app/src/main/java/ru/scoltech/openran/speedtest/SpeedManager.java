package ru.scoltech.openran.speedtest;

import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SpeedManager {
    private List<String> uploadList;
    private List<String> downloadList;

    public SpeedManager(List<String> list) {

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
}
