package com.example.wavemockapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {


    Wave cWave;
    CardView mCard;
    SubResultView mResults;
    HeaderView mHeader;

    //action elem
    Button actionBtn;
    TextView actionTV;

    SpeedManager sm;

    //TODO: replace deprecated handler
    Handler handler;
    Runnable task;

    int MEASURING_DELAY = 200;
    int TASK_DELAY = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        setContentView(R.layout.activity_main);

        init();

        sm = new SpeedManager(readSpeedFromAssetsCSV("iperfClientReal.csv"));
    }

    private void init() {
        mHeader = findViewById(R.id.header);

        actionBtn = findViewById(R.id.action_btn);
        actionTV = findViewById(R.id.action_text);

        cWave = findViewById(R.id.progress_wave);
        cWave.attachColor(getColor(R.color.mint));

        mCard = findViewById(R.id.card);
        mResults = findViewById(R.id.subresult);

    }

    public void onClick(View v) {
        if (v.getId() == R.id.action_btn) {

            if (actionBtn.getContentDescription().toString().equals("start")) {

                mCard.setVisibility(View.VISIBLE);
                mResults.setVisibility(View.VISIBLE);

                actionTV.setVisibility(View.GONE);

                mHeader.setSectionName("Demonstration");
                mHeader.disableButtonGroup();

                setStopToActionBtn();
                measureDownloadSpeed();

            } else if (actionBtn.getContentDescription().toString().equals("stop")) {

                mHeader.enableButtonGroup();
                mHeader.showReturnBtn();

                setPlayToActionBtn();
                stopMeasuring();
            } else if (actionBtn.getContentDescription().toString().equals("play")) {

                mHeader.disableButtonGroup();
                mHeader.hideReturnBtn();

                setStopToActionBtn();
                measureDownloadSpeed();
            }
        }
    }

    // TODO: refactor state
    private void setStopToActionBtn() {
        actionBtn.setContentDescription("stop");
        actionBtn.setBackground(getDrawable(R.drawable.ic_stop_btn));
    }

    // TODO: refactor state
    private void setPlayToActionBtn() { // when continue after stopping
        actionBtn.setContentDescription("play");
        actionBtn.setBackground(getDrawable(R.drawable.ic_play_btn));

        mResults.setDownloadSpeed(getString(R.string.empty));
        mResults.setUploadSpeed(getString(R.string.empty));
    }

    // TODO: refactor state
    private void setStartToActionBtn() { // when start from main menu
        actionBtn.setContentDescription("start");
        actionBtn.setBackground(getDrawable(R.drawable.ic_start_btn));
    }

    private List<String> readSpeedFromAssetsCSV(String filename) {
        List<String> records = new ArrayList<String>();
        try (CSVReader csvReader = new CSVReader(new InputStreamReader(getAssets().open(filename)));) {
            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                records.add(values[8]);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }


    private void measureDownloadSpeed() {

        final int[] i = {0};

        handler = new Handler();
        task = new Runnable() {
            @Override
            public void run() {
                Log.d("mytag", "Doing task download" + i[0]);

                if (i[0] < sm.downloadList.size()) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.downloadList.get(i[0]), 2);
                    setInstantSpeed(instSpeed);

                    i[0]++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                    // if finish counting
                } else {
                    handler.removeCallbacks(this);

                    setDownloadSpeed(sm.getAverageDownloadSpeed());

                    // delay between two tasks: download and upload
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {

                            cWave.attachColor(getColor(R.color.gold));

                            measureUploadSpeed();
                        }
                    }, TASK_DELAY);

                }
            }
        };
        handler.post(task);
    }

    private void measureUploadSpeed() {

        final int[] i = {0};

        handler = new Handler();
        task = new Runnable() {
            @Override
            public void run() {
                Log.d("mytag", "Doing task upload" + i[0]);

                if (i[0] < sm.uploadList.size()) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.uploadList.get(i[0]), 2);
                    setInstantSpeed(instSpeed);

                    i[0]++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                } else {
                    handler.removeCallbacks(this);

                    setUploadSpeed(sm.getAverageUploadSpeed());
                    setPlayToActionBtn();

                    mHeader.enableButtonGroup();
                    mHeader.showReturnBtn();
                }
            }
        };
        handler.post(task);
    }

    private void stopMeasuring() {
        Log.d("mytag", "stopSpeed: mock stopping");

        setPlayToActionBtn();
        handler.removeCallbacks(task);
    }


    private void setInstantSpeed(Pair<Integer, Integer> speed) {
        mCard.setIntegerSpeed(speed.first);
        mCard.setFractionSpeed(speed.second);
    }

    private void setDownloadSpeed(Pair<Integer, Integer> speed) {
        mResults.setDownloadSpeed(String.format("%d.%d", speed.first, speed.second));
    }

    private void setUploadSpeed(Pair<Integer, Integer> speed) {
        mResults.setUploadSpeed(String.format("%d.%d", speed.first, speed.second));
    }

}