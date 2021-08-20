package ru.scoltech.openran.speedtest.Activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.TextView;
import android.os.Handler;

import ru.scoltech.openran.speedtest.CustomButtons.ActionButton;
import ru.scoltech.openran.speedtest.CustomButtons.SaveButton;
import ru.scoltech.openran.speedtest.CustomButtons.ShareButton;
import ru.scoltech.openran.speedtest.CustomViews.CardView;
import ru.scoltech.openran.speedtest.CustomViews.HeaderView;
import ru.scoltech.openran.speedtest.CustomViews.ResultView;
import ru.scoltech.openran.speedtest.CustomViews.SubResultView;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.Wave;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    Wave cWave;
    CardView mCard;
    SubResultView mSubResults; // in progress result
    HeaderView mHeader;
    ResultView mResults; // after finishing

    //TODO global: merge SubResultView and ResultView and load only different xml
    //TODO global: replace visibility changing with adding and removing components from the view

    //action elem
    ActionButton actionBtn;
    TextView actionTV;
    ShareButton shareBtn;
    SaveButton saveBtn;

    SpeedManager sm;

    Handler handler;
    Runnable task;

    final static int MEASURING_DELAY = 200;
    final static int TASK_DELAY = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null)
        {
            getSupportActionBar().hide();
        }
        setContentView(R.layout.activity_main);

        init();

        sm = new SpeedManager(readSpeedFromAssetsCSV("iperfClientReal.csv"));
    }

    private void init() {
        mHeader = findViewById(R.id.header);

        actionBtn = findViewById(R.id.action_btn);
        actionTV = findViewById(R.id.action_text);

        mCard = findViewById(R.id.card);
        cWave = mCard.getWave();

        mSubResults = findViewById(R.id.subresult);
        mResults = findViewById(R.id.result);

        shareBtn = findViewById(R.id.share_btn);
        saveBtn = findViewById(R.id.save_btn);

    }

    public void onClick(View v) {
        if (v.getId() == R.id.action_btn) {

            if (actionBtn.getContentDescription().toString().equals("start")) {

                onPlayUI();
                measureDownloadSpeed();

            } else if (actionBtn.getContentDescription().toString().equals("stop")) {

                onStopUI();
                stopMeasuring();

            } else if (actionBtn.getContentDescription().toString().equals("play")) {

                onPlayUI();
                measureDownloadSpeed();
            }
        }
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

                if (i[0] < sm.getDownloadList().size()) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.getDownloadList().get(i[0]), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    i[0]++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                    // if finish counting
                } else {
                    handler.removeCallbacks(this);

                    mSubResults.setDownloadSpeed(getSpeedString(sm.getAverageDownloadSpeed()));

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

                if (i[0] < sm.getUploadList().size()) {

                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(sm.getUploadList().get(i[0]), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    i[0]++;
                    handler.postDelayed(this, MEASURING_DELAY);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                    cWave.invalidate();

                } else {
                    handler.removeCallbacks(this);

                    mSubResults.setUploadSpeed(getSpeedString(sm.getAverageUploadSpeed()));
                    actionBtn.setPlay();

                    String downloadSpeed = mSubResults.getDownloadSpeed();
                    String uploadSpeed = mSubResults.getUploadSpeed();
                    String ping = mCard.getPing();
                    onResultUI(downloadSpeed, uploadSpeed, ping);
                }
            }
        };
        handler.post(task);
    }

    private void stopMeasuring() {
        Log.d("mytag", "stopSpeed: mock stopping");

        actionBtn.setPlay();
        mSubResults.setEmpty();

        handler.removeCallbacks(task);
    }

    private String getSpeedString(Pair<Integer, Integer> speed) {
        return String.format("%d.%d", speed.first, speed.second);
    }

    private void onResultUI(String downloadSpeed, String uploadSpeed, String ping) {

        mSubResults.setVisibility(View.GONE);

        mResults.setVisibility(View.VISIBLE);

        mCard.setEmptyCaptions();
        mCard.setMessage("Done");

        mResults.setDownloadSpeed(downloadSpeed);
        mResults.setUploadSpeed(uploadSpeed);
        mResults.setPing(ping);

        actionBtn.setRestart();

        mHeader.showReturnBtn();

        shareBtn.setVisibility(View.VISIBLE);
        saveBtn.setVisibility(View.VISIBLE);

    }

    public void onPlayUI() {
        mCard.setVisibility(View.VISIBLE);
        mCard.setDefaultCaptions();

        cWave.attachColor(getColor(R.color.mint));

        mSubResults.setVisibility(View.VISIBLE);
        mSubResults.setEmpty();
        mResults.setVisibility(View.GONE);

        mHeader.setSectionName("Demonstration");
        mHeader.disableButtonGroup();
        mHeader.hideReturnBtn();

        actionTV.setVisibility(View.GONE);
        actionBtn.setStop();

        shareBtn.setVisibility(View.GONE);
        saveBtn.setVisibility(View.GONE);
    }

    public void onStopUI() {
        mHeader.enableButtonGroup();
        mHeader.showReturnBtn();

        actionBtn.setPlay();

        mSubResults.setEmpty();
    }

}