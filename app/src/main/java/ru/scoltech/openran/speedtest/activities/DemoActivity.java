package ru.scoltech.openran.speedtest.activities;


import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kotlin.Unit;
import kotlin.collections.MapsKt;
import kotlin.collections.SetsKt;
import kotlin.text.StringsKt;
import ru.scoltech.openran.speedtest.ApplicationConstants;
import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;
import ru.scoltech.openran.speedtest.Wave;
import ru.scoltech.openran.speedtest.customButtons.ActionButton;
import ru.scoltech.openran.speedtest.customButtons.SaveButton;
import ru.scoltech.openran.speedtest.customButtons.ShareButton;
import ru.scoltech.openran.speedtest.customViews.CardView;
import ru.scoltech.openran.speedtest.customViews.HeaderView;
import ru.scoltech.openran.speedtest.customViews.ResultView;
import ru.scoltech.openran.speedtest.customViews.SubResultView;
import ru.scoltech.openran.speedtest.manager.DownloadUploadSpeedTestManager;


public class DemoActivity extends AppCompatActivity {

    private String TAG = "DEMO_ACTIVITY";
    private Wave cWave;
    private CardView mCard;
    private SubResultView mSubResults; // in progress result
    private HeaderView mHeader;
    private ResultView mResults; // after finishing

    //TODO global: reorganise view operating

    //action elem
    private ActionButton actionBtn;
    private TextView actionTV;
    private ShareButton shareBtn;
    private SaveButton saveBtn;
    private ConstraintLayout settings;

    private SpeedManager sm;
    private DownloadUploadSpeedTestManager speedTestManager;

    private final static int TASK_DELAY = 2500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // begin block for hand mode switcher
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        @SuppressWarnings("unchecked")
        Map<Integer, String> themeLogMessage = MapsKt.mapOf(
                new kotlin.Pair<>(Configuration.UI_MODE_NIGHT_NO, "onCreate: Light Theme"),
                new kotlin.Pair<>(Configuration.UI_MODE_NIGHT_YES, "onCreate: Dark Theme"),
                new kotlin.Pair<>(Configuration.UI_MODE_NIGHT_UNDEFINED, "onCreate: Undefined Theme")
        );

        Log.d(TAG, themeLogMessage.get(currentNightMode));
        // end block

        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.activity_demo);

        init();

        sm = SpeedManager.getInstance();
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

        settings = findViewById(R.id.start_screen_settings);
        final EditText mainAddress = findViewById(R.id.main_address);
        mainAddress.setText(
                getPreferences(MODE_PRIVATE).getString(
                        ApplicationConstants.MAIN_ADDRESS_KEY,
                        getString(R.string.default_main_address)
                )
        );
        mainAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no operations
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no operations
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final CharSequence newMainAddress = StringsKt.isBlank(s)
                        ? getString(R.string.default_main_address) : s;
                SharedPreferences.Editor preferences = getPreferences(MODE_PRIVATE).edit();
                preferences.putString(
                        ApplicationConstants.MAIN_ADDRESS_KEY,
                        newMainAddress.toString()
                );
                preferences.apply();
            }
        });

        final RadioGroup modeRadioGroup = findViewById(R.id.mode_radio_group);
        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor preferences = getPreferences(MODE_PRIVATE).edit();
            preferences.putBoolean(
                    ApplicationConstants.USE_BALANCER_KEY,
                    checkedId == R.id.balancer_mode
            );
            preferences.apply();
        });

        final boolean useBalancer = getPreferences(MODE_PRIVATE)
                .getBoolean(ApplicationConstants.USE_BALANCER_KEY, true);
        if (useBalancer) {
            this.<RadioButton>findViewById(R.id.balancer_mode).setChecked(true);
        } else {
            this.<RadioButton>findViewById(R.id.direct_mode).setChecked(true);
        }

        if (!getPreferences(MODE_PRIVATE).getBoolean(ApplicationConstants.PRIVACY_SHOWN, false)) {
            SharedPreferences.Editor preferencesEditor = getPreferences(MODE_PRIVATE).edit();
            preferencesEditor.putBoolean(ApplicationConstants.PRIVACY_SHOWN, true);
            preferencesEditor.apply();
            findViewById(R.id.main_layout).post(this::showPrivacyPopUp);
        }

        // TODO split on methods
        speedTestManager = new DownloadUploadSpeedTestManager.Builder(this)
                .onPingUpdate((ping) -> runOnUiThread(() -> mCard.setPing((int) ping)))
                .onDownloadStart(() -> runOnUiThread(() -> {
                    mCard.setInstantSpeed(0, 0);

                    cWave.start();
                    cWave.attachSpeed(0);
                }))
                .onDownloadSpeedUpdate((statistics, speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(speedBitsPS.intValue(), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                }))
                .onDownloadFinish((statistics) -> runOnUiThread(() -> {
                    mSubResults.setDownloadSpeed(getSpeedString(sm.getAverageSpeed(statistics)));
                    cWave.stop();
                }))
                .onUploadStart(() -> runOnUiThread(() -> {
                    mCard.setInstantSpeed(0, 0);

                    cWave.start();
                    cWave.attachColor(getColor(R.color.gold));
                    cWave.attachSpeed(0);
                }))
                .onUploadSpeedUpdate((statistics, speedBitsPS) -> runOnUiThread(() -> {
                    Pair<Integer, Integer> instSpeed = sm.getSpeedWithPrecision(speedBitsPS.intValue(), 2);
                    mCard.setInstantSpeed(instSpeed.first, instSpeed.second);

                    //animation
                    cWave.attachSpeed(instSpeed.first);
                }))
                .onUploadFinish((statistics) -> runOnUiThread(() -> {
                    cWave.stop();
                    mSubResults.setUploadSpeed(getSpeedString(sm.getAverageSpeed(statistics)));
                }))
                .onFinish(() -> runOnUiThread(() -> {
                    actionBtn.setPlay();

                    String downloadSpeed = mSubResults.getDownloadSpeed();
                    String uploadSpeed = mSubResults.getUploadSpeed();
                    String ping = mCard.getPing();
                    onResultUI(downloadSpeed, uploadSpeed, ping);
                }))
                .onStop(() -> runOnUiThread(() -> {
                    onStopUI();
                    actionBtn.setPlay();
                    mSubResults.setEmpty();
                }))
                .onFatalError((s, exception) -> runOnUiThread(() -> {
                    Log.e("SpeedtestFatal", s, exception);

                    onStopUI();
                    actionBtn.setPlay();
                    mSubResults.setEmpty();
                }))
                .build();
    }

    private void showPrivacyPopUp() {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.policy_title))
                .setMessage(R.string.policy_content)
                .setPositiveButton(android.R.string.ok, null)
                .create();
        alert.show();

        ((TextView) Objects.requireNonNull(alert.findViewById(android.R.id.message)))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onClick(View v) {
        if (v.getId() == R.id.action_btn) {

            if (SetsKt.setOf("start", "play").contains(actionBtn.getContentDescription().toString())) {

                onPlayUI();
                speedTestManager.start(
                        getPreferences(MODE_PRIVATE).getBoolean(
                                ApplicationConstants.USE_BALANCER_KEY,
                                true
                        ),
                        getPreferences(MODE_PRIVATE).getString(
                                ApplicationConstants.MAIN_ADDRESS_KEY,
                                getString(R.string.default_main_address)
                        ),
                        TASK_DELAY
                );

            } else if (actionBtn.getContentDescription().toString().equals("stop")) {

                onStopUI();
                speedTestManager.stop();

            }
        }
    }


    private String getSpeedString(Pair<Integer, Integer> speed) {
        return String.format(Locale.ENGLISH, "%d.%d", speed.first, speed.second);
    }

    private void onResultUI(String downloadSpeed, String uploadSpeed, String ping) {

        mSubResults.setVisibility(View.GONE);

        mResults.setVisibility(View.VISIBLE);

        mCard.setEmptyCaptions();
        mCard.setMessage("Done");

        mResults.setDownloadSpeed(downloadSpeed);
        mResults.setUploadSpeed(uploadSpeed);
        mResults.setPing(ping);
        mHeader.setSectionName("Results");

        actionBtn.setRestart();

        mHeader.showReturnBtn();

        shareBtn.setVisibility(View.VISIBLE);
        saveBtn.setVisibility(View.VISIBLE);

    }

    public void onPlayUI() {
        settings.setVisibility(View.GONE);

        mCard.setVisibility(View.VISIBLE);
        mCard.setDefaultCaptions();

        cWave.attachColor(getColor(R.color.mint));

        mSubResults.setVisibility(View.VISIBLE);
        mSubResults.setEmpty();
        mResults.setVisibility(View.GONE);

        mHeader.setSectionName("Measuring");
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

        cWave.stop();
        actionBtn.setPlay();

        mSubResults.setEmpty();
    }

}