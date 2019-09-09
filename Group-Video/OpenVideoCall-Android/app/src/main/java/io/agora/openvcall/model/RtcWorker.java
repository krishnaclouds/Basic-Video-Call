package io.agora.openvcall.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.agora.openvcall.R;
import io.agora.propeller.Constant;
import io.agora.rtc.Constants;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class RtcWorker {
    private final static Logger log = LoggerFactory.getLogger(RtcWorker.class);

    private Context mContext;
    private RtcEngine mRtcEngine;

    public final void enablePreProcessor() {
        if (Constant.BEAUTY_EFFECT_ENABLED) {
            mRtcEngine.setBeautyEffectOptions(true, Constant.BEAUTY_OPTIONS);
        }
    }

    public final void setBeautyEffectParameters(float lightness, float smoothness, float redness) {
        Constant.BEAUTY_OPTIONS.lighteningLevel = lightness;
        Constant.BEAUTY_OPTIONS.smoothnessLevel = smoothness;
        Constant.BEAUTY_OPTIONS.rednessLevel = redness;
    }

    public final void disablePreProcessor() {
        // do not support null when setBeautyEffectOptions to false
        mRtcEngine.setBeautyEffectOptions(false, Constant.BEAUTY_OPTIONS);
    }

    public final void joinChannel(final String channel, int uid) {
        ensureRtcEngineReady();

        String accessToken = mContext.getString(R.string.agora_access_token);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "#YOUR ACCESS TOKEN#")) {
            accessToken = null; // default, no token
        }

        mRtcEngine.joinChannel(accessToken, channel, "OpenVCall", uid);

        mEngineConfig.mChannel = channel;

        enablePreProcessor();
        log.debug("joinChannel " + channel + " " + uid);
    }

    public final void leaveChannel(String channel) {
        if (mRtcEngine != null) {
            mRtcEngine.leaveChannel();
            mRtcEngine.enableVideo();
        }

        disablePreProcessor();

        mEngineConfig.reset();
        log.debug("leaveChannel " + channel);
    }

    private EngineConfig mEngineConfig;

    public final EngineConfig getEngineConfig() {
        return mEngineConfig;
    }

    private final MyEngineEventHandler mEngineEventHandler;

    public final void configEngine(VideoEncoderConfiguration.VideoDimensions videoDimension,
                                   VideoEncoderConfiguration.FRAME_RATE fps, String encryptionKey, String encryptionMode) {
        ensureRtcEngineReady();

        if (!TextUtils.isEmpty(encryptionKey)) {
            mRtcEngine.setEncryptionMode(encryptionMode);
            mRtcEngine.setEncryptionSecret(encryptionKey);
        }

        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(videoDimension,
                fps,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT));

        log.debug("configEngine " + videoDimension + " " + fps + " " + encryptionMode);
    }

    public final void preview(boolean start, SurfaceView view, int uid) {
        ensureRtcEngineReady();
        if (start) {
            mRtcEngine.setupLocalVideo(new VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, uid));
            mRtcEngine.startPreview();
        } else {
            mRtcEngine.stopPreview();
        }
    }

    private RtcEngine ensureRtcEngineReady() {
        if (mRtcEngine == null) {
            String appId = mContext.getString(R.string.agora_app_id);
            if (TextUtils.isEmpty(appId)) {
                throw new RuntimeException("NEED TO use your App ID, get your own ID at https://dashboard.agora.io/");
            }
            try {
                mRtcEngine = RtcEngine.create(mContext, appId, mEngineEventHandler.mRtcEventHandler);
            } catch (Exception e) {
                log.error(Log.getStackTraceString(e));
                throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
            }
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
            mRtcEngine.enableVideo();
            mRtcEngine.enableAudioVolumeIndication(200, 3); // 200 ms
        }
        return mRtcEngine;
    }

    public MyEngineEventHandler eventHandler() {
        return mEngineEventHandler;
    }

    public RtcEngine getRtcEngine() {
        return mRtcEngine;
    }

    public RtcWorker(Context context) {
        this.mContext = context;
        this.mEngineConfig = new EngineConfig();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        this.mEngineConfig.mUid = pref.getInt(ConstantApp.PrefManager.PREF_PROPERTY_UID, 0);
        this.mEngineEventHandler = new MyEngineEventHandler(mContext, this.mEngineConfig);
        ensureRtcEngineReady();
    }

    public void destroy() {
        RtcEngine.destroy();
    }
}
