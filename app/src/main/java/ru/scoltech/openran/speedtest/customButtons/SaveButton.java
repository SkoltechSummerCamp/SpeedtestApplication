package ru.scoltech.openran.speedtest.customButtons;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.scoltech.openran.speedtest.R;
import ru.scoltech.openran.speedtest.SpeedManager;

public class SaveButton extends androidx.appcompat.widget.AppCompatButton {
    private Context mContext;

    public SaveButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        this.setBackground(context.getDrawable(R.drawable.ic_save_btn));

        this.setOnClickListener(view -> {
            SpeedManager sm = SpeedManager.getInstance();
            saveTask(sm.generateImage(mContext));
        });
    }

    private void saveTask(Bitmap bitmap) {
        Log.d("mytag", "saveTask: pressed save");


        if (isStoragePermissionGranted()) {


            try {
                FileOutputStream fileOutputStream = new FileOutputStream(createFile());
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream);

                fileOutputStream.flush();
                fileOutputStream.close();

            } catch (IOException e) {
                e.printStackTrace();

            }
            Toast.makeText(mContext, "Successfully saved image", Toast.LENGTH_SHORT).show();
        }
    }

    private File createFile() {
        File dir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + mContext.getApplicationContext().getPackageName()
                + "/Files");

        if (!dir.exists()) dir.mkdirs();


        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm", Locale.ROOT).format(new Date());
        String imageName = "test_" + timeStamp + ".jpg";

        return new File(dir.getPath() + File.separator + imageName);

    }

    private boolean isStoragePermissionGranted() {
        Activity activity = (Activity) mContext;
        String TAG = "Storage Permission";
        if (Build.VERSION.SDK_INT >= 24) {
            if (activity.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            Log.v(TAG, "Permission is granted by default");
            return true;
        }
    }


}
