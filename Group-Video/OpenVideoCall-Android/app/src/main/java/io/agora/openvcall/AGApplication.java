package io.agora.openvcall;

import android.app.Application;
import io.agora.openvcall.model.CurrentUserSettings;
import io.agora.openvcall.model.RtcWorker;

public class AGApplication extends Application {
    private RtcWorker mRtcWorker;

    @Override
    public void onCreate() {
        super.onCreate();
        mRtcWorker = new RtcWorker(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mRtcWorker.destroy();
    }

    public synchronized RtcWorker getWorker() {
        return mRtcWorker;
    }

    public static final CurrentUserSettings mVideoSettings = new CurrentUserSettings();
}
