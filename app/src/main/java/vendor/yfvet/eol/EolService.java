package vendor.yfvet.eol;

import android.app.Service;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.eol.CarYFEolManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Arrays;

public class EolService extends Service {
    private static final String TAG = "EolService";
    private EolMediaController mEolMediaController;
    private Car mCar;
    private CarYFEolManager mCarYFEolManager;
    private final CarYFEolManager.CarEolEventCallback mCarEOlCallback = new CarYFEolManager.CarEolEventCallback() {

        public void onChangeEvent(CarPropertyValue value) {
            int propId = value.getPropertyId();
            switch (propId) {
                case CarYFEolManager.DIAG_EOL_SET_USB_IPOD_PLAY_PAUSE:
                    handleDiagnosticPlayPause(value);
                    break;
                case CarYFEolManager.DIAG_EOL_SET_USB_IPOD_PLAY_MODE:
                    handleDiagnosticMediaPlayMode(value);
                    break;
                case CarYFEolManager.DIAG_EOL_SET_USB_IPOD_FORWARD_REWIND:
                    handleDiagnosticForward(value);
                    break;
                case CarYFEolManager.DIAG_EOL_SET_USB_IPOD_SKIP_TRACK:
                    handleDiagnosticSkipTrack(value);
                    break;
                case CarYFEolManager.DIAG_EOL_SET_USB_DESIRED_FILE_DESIRED_TIME:
                    handleDiagnosticDesiredTime(value);
                    break;
                case CarYFEolManager.DIAG_EOL_GET_USB_IPOD_CURRENT_STATE:
                    handleDiagnosticCurrentState(value);
                    break;
                case CarYFEolManager.DIAG_EOL_RDBI_MEDIA_COMMAND_USB_IPOD_PLAY_PAUSE:
                    handleDiagnosticPlayPauseState(value);
                    break;
                case CarYFEolManager.DIAG_EOL_RDBI_MEDIA_COMMAND_USB_IPOD_PLAY_MODE:
                    handleDiagnosticMediaPlayModeState(value);
                    break;
                case CarYFEolManager.DIAG_EOL_RDBI_MEDIA_COMMAND_USB_IPOD_FORWARD_REWIND:
                    handleDiagnosticForwardState(value);
                    break;
                case CarYFEolManager.DIAG_EOL_RDBI_MEDIA_IPOD_CP_CHIP_COMMUNICATION_STATUS:
                    handleDiagnosticChipCommunicationState(value);
                    break;
                default:
                    break;
            }
        }

        public void onErrorEvent(int propertyId, int zone) {
            Log.d(TAG, "onErrorEvent() called with: propertyId = [" + propertyId + "], zone = [" + zone + "]");
        }

    };

    private final ServiceConnection mPowerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected() called");
            initEolManager();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected() called");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called 2022-09-08");

        initCar();

        mEolMediaController = new EolMediaController(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        if (mCar != null) {
            mCar.disconnect();
        }
        if (mEolMediaController != null) {
            mEolMediaController.disconnect();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called with: action = [" + intent.getAction() + "]");
        return null;
    }

    private void initCar() {
        Log.d(TAG, "initCar() called");
        mCar = Car.createCar(this, mPowerConnection);
        if (mCar != null) {
            mCar.connect();
        }
    }

    private void initEolManager() {
        Log.d(TAG, "initEolManager() called");
        if (mCar != null) {
            try {
                mCarYFEolManager = (CarYFEolManager) mCar.getCarManager(Car.EOL_SERVICE);
                if (mCarYFEolManager != null) {
                    Log.i(TAG, "register EOl Callback ");
                    mCarYFEolManager.registerCallback(mCarEOlCallback);
                } else {
                    Log.i(TAG, "mCarYFEolManager is null");
                }
            } catch (CarNotConnectedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleDiagnosticPlayPause(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticPlayPause() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (bsValue != null && bsValue.length > 0) {
            int cmd = bsValue[0];
            if (EolMediaControllerNotNull()) {
                if (cmd == 0x01) {
                    mEolMediaController.play();
                } else if (cmd == 0x02) {
                    mEolMediaController.pause();
                } else {
                    valueResult[0] = 0x31;
                }
            }
        }
        setProperty(value, valueResult);
    }

    private void handleDiagnosticMediaPlayMode(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticMediaPlayMode() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (bsValue != null && bsValue.length > 0) {
            int cmd = bsValue[0];
            if (EolMediaControllerNotNull()) {
                //00:Normal Mode 01:Reserved 02:Repeat One file Mode 03:Random All Mode
                if (cmd == 0x00) {
                    mEolMediaController.playTheOrder();
                } else if (cmd == 0x01) {
                    mEolMediaController.playTheReserve();
                } else if (cmd == 0x02) {
                    mEolMediaController.playTheSingle();
                } else if (cmd == 0x03) {
                    mEolMediaController.playTheRandom();
                } else {
                    valueResult[0] = 0x31;
                }
            }
        }
        setProperty(value, valueResult);
    }

    private void handleDiagnosticForward(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticForward() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));
        if ((bsValue != null) && (bsValue.length > 0)) {
            int cmd = bsValue[0];
            //01 : fast forward  02 : fast rewind
            if (EolMediaControllerNotNull()) {
                if (cmd == 0x01) {
                    mEolMediaController.fastForward();
                } else if (cmd == 0x02) {
                    mEolMediaController.rewind();
                } else {
                    valueResult[0] = 0x31;
                }
            }
        }
        setProperty(value, valueResult);
    }

    private void handleDiagnosticSkipTrack(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticSkipTrack() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (bsValue != null && bsValue.length > 0) {
            int cmd = bsValue[0];
            //01 : skip to next track 02 : skip to previous track
            if (EolMediaControllerNotNull()) {
                if (cmd == 0x01) {
                    mEolMediaController.skipToNext();
                } else if (cmd == 0x02) {
                    mEolMediaController.skipToPrevious();
                } else {
                    valueResult[0] = 0x31;
                }
            }
        }
        setProperty(value, valueResult);
    }

    private void handleDiagnosticDesiredTime(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticDesiredTime() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (bsValue != null && bsValue.length > 4) {
            /*
            d1: MSB of absolutely track number
            d2: LSB of absolutely track number
               Note: track number start from 1. Zero is invalid value.
            t1: MSB of minute
            t2: LSB of minute
            t3: second
            */
            int track = bsValue[0];
            track = (track << 8) + bsValue[1];

            int time = bsValue[2];
            time = (time << 8) + bsValue[3];
            time = time * 60 + bsValue[4];

            Log.d(TAG, "item: " + track + "   time: " + time);
            if (EolMediaControllerNotNull()) {
                if (track < 0 || time < 0) {
                    valueResult[0] = 0x31;
                } else {
                    if (!mEolMediaController.skipToQueueItem(track, time)) {
                        valueResult[0] = 0x22;
                    }
                }
            }
            setProperty(value, valueResult);
        }
    }

    private void handleDiagnosticCurrentState(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticCurrentState() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00, 0x00, 0x00, 0x00, 0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (EolMediaControllerNotNull()) {
            int[] state = mEolMediaController.getCurrentState();
            int number = state[0];
            int millisecond = state[1];

            int minute = millisecond / 1000 / 60;
            int second = (millisecond / 1000) % 60;
            Log.d(TAG, "minute: " + minute + ", second: " + second);

            valueResult[0] = (byte) ((number >> 8) & 0xFF);
            valueResult[1] = (byte) ((number) & 0xFF);
            valueResult[2] = (byte) ((minute >> 8) & 0xFF);
            valueResult[3] = (byte) ((minute) & 0xFF);
            valueResult[4] = (byte) second;
        }
        setProperty(value, valueResult);
    }

    private void handleDiagnosticPlayPauseState(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticPlayPauseState() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00, 0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (EolMediaControllerNotNull()) {
            valueResult[1] = mEolMediaController.getCurrentPlayState();
        }

        Log.d(TAG, "valueResult: " + Arrays.toString(valueResult));
        setProperty(value, valueResult);
    }

    private void handleDiagnosticMediaPlayModeState(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticMediaPlayModeState() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (EolMediaControllerNotNull()) {
            valueResult[0] = mEolMediaController.getCurrentPlayMode();
        }

        setProperty(value, valueResult);
    }

    private void handleDiagnosticForwardState(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticForwardState() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));

        if (EolMediaControllerNotNull()) {
            valueResult[0] = mEolMediaController.getFastForwardRewindState();
        }

        setProperty(value, valueResult);
    }

    private void handleDiagnosticChipCommunicationState(CarPropertyValue<?> value) {
        Log.d(TAG, "handleDiagnosticChipCommunicationState() called with: PropertyId = [" + value.getPropertyId() + "]");

        byte[] valueResult = {0x00, 0x01};

        byte[] bsValue = (byte[]) value.getValue();
        Log.d(TAG, "bsValue: " + Arrays.toString(bsValue));
        //TODO: uxuxy270: need to finish the ChipCommunication State value
        //1. valueResult[0] = 0x00; <positive response value> valueResult[1] = 0x01; <Chip status OK>
        //                                                    valueResult[1] = 0x00; <Chip status NG>
        //2. valueResult[0] = 0x22; <negative response value>
        //Currently only response {0x00, 0x01}, need to finish logic to monitor MFi Auth IC states

        setProperty(value, valueResult);
    }


    /**
     * Returns true if the EolMediaController is not null.
     */
    private boolean EolMediaControllerNotNull() {
        if (mEolMediaController != null) {
            return true;
        } else {
            Log.d(TAG, "EolMediaController is null");
            return false;
        }
    }

    /**
     * Callback to CarService
     *
     * @param value       CarPropertyValue
     * @param valueResult The value of the callback
     */
    private void setProperty(CarPropertyValue<?> value, byte[] valueResult) {
        try {
            mCarYFEolManager.setProperty(byte[].class, value.getPropertyId(), 0, valueResult);
        } catch (CarNotConnectedException e) {
            e.printStackTrace();
        }
    }
}
