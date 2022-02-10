package vendor.yfvet.eol;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class EolMediaController {
    private static final String TAG = "EolMediaController";
    private final Context mContext;
    private final MediaBrowser mediaBrowser;
    private MediaController mediaController;
    private OnMetadataChangedListener onMetadataChangedListener;
    private int PLAY_MODE = -1;
    private int STATE_FAST_FORWARD_REWIND = -1;

    public EolMediaController(Context context) {
        this.mContext = context;

        MediaBrowser.ConnectionCallback mediaConnectionCallback = new MediaBrowser.ConnectionCallback() {
            @Override
            public void onConnected() {
                super.onConnected();
                Log.d(TAG, "onConnected() called");

                mediaController = new MediaController(mContext, mediaBrowser.getSessionToken());

                mediaController.registerCallback(new MediaController.Callback() {
                    @Override
                    public void onPlaybackStateChanged(@Nullable PlaybackState state) {
                        super.onPlaybackStateChanged(state);
                        if (state != null) {
                            Log.d(TAG, "onPlaybackStateChanged() called with: state = [" + state + "]");
                        } else {
                            Log.d(TAG, "onPlaybackStateChanged() called with: state = [" + null + "]");
                        }
                    }

                    @Override
                    public void onMetadataChanged(@Nullable MediaMetadata metadata) {
                        super.onMetadataChanged(metadata);
                        if (metadata != null) {
                            Log.d(TAG, "onMetadataChanged() called with: description = [" + metadata.getDescription() + "]");
                        } else {
                            Log.d(TAG, "onMetadataChanged() called with: metadata = [" + null + "]");
                        }
                        if (onMetadataChangedListener != null) {
                            onMetadataChangedListener.onMetadataChanged(metadata != null ? metadata.getDescription().getMediaId() : null);
                        }
                    }

                    @Override
                    public void onQueueChanged(@Nullable List<MediaSession.QueueItem> queue) {
                        super.onQueueChanged(queue);
                        if (queue != null) {
                            Log.d(TAG, "onQueueChanged() called with: queue = [" + queue.size() + "]");
                        } else {
                            Log.d(TAG, "onQueueChanged() called with: queue = [" + null + "]");
                        }
                    }

                    @Override
                    public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
                        super.onAudioInfoChanged(info);
                        Log.d(TAG, "onAudioInfoChanged() called with: info = [" + info + "]");
                    }

                    @Override
                    public void onQueueTitleChanged(@Nullable CharSequence title) {
                        super.onQueueTitleChanged(title);
                        Log.d(TAG, "onQueueTitleChanged() called with: title = [" + title + "]");
                    }

                    @Override
                    public void onSessionEvent(@NonNull String event, @Nullable Bundle extras) {
                        super.onSessionEvent(event, extras);
                        Log.d(TAG, "onSessionEvent() called with: event = [" + event + "]");
                        if ("GET_PLAY_MODE".equals(event)) {
                            if (extras != null) {
                                if (extras.containsKey("MODE")) {
                                    PLAY_MODE = (int) extras.get("MODE");
                                    Log.d(TAG, "PLAY_MODE = " + PLAY_MODE);
                                } else {
                                    Log.d(TAG, "extras keys not contains MODE");
                                }
                            } else {
                                Log.d(TAG, "onSessionEvent() called with: extras = [" + null + "]");
                            }
                        } else if ("GET_REWIND_FORWARD_MODE".equals(event)) {
                            if (extras != null) {
                                if (extras.containsKey("FAST_REWIND_FORWARD")) {
                                    STATE_FAST_FORWARD_REWIND = (int) extras.get("FAST_REWIND_FORWARD");
                                    Log.d(TAG, "STATE_FAST_FORWARD_REWIND = " + STATE_FAST_FORWARD_REWIND);
                                } else {
                                    Log.d(TAG, "extras keys not contains FAST_REWIND_FORWARD");
                                }
                            } else {
                                Log.d(TAG, "onSessionEvent() called with: extras = [" + null + "]");
                            }
                        }
                    }
                });
                mediaController.getTransportControls().sendCustomAction("BOOT_COMPLETE", null);
            }

            @Override
            public void onConnectionFailed() {
                super.onConnectionFailed();
                Log.d(TAG, "onConnectionFailed() called");
            }

            @Override
            public void onConnectionSuspended() {
                super.onConnectionSuspended();
                Log.d(TAG, "onConnectionSuspended() called");
            }
        };

        mediaBrowser = new MediaBrowser(context, new ComponentName("com.ici.media", "com.ici.media.playcontrol.MediaService"),
                mediaConnectionCallback, null);
        mediaBrowser.connect();
    }

    public void setOnMetadataChangedListener(OnMetadataChangedListener onMetadataChangedListener) {
        this.onMetadataChangedListener = onMetadataChangedListener;
    }

    public void disconnect() {
        if (mediaBrowser != null) {
            Log.d(TAG, "disconnect() called");
            mediaBrowser.disconnect();
        } else {
            Log.d(TAG, "mediaBrowser is null");
        }
    }

    public boolean skipToQueueItem(int index, int position) {
        Log.d(TAG, "skipToQueueItem() called with: index = [" + index + "], position = [" + position + "]");
        if (mediaController != null) {
            List<MediaSession.QueueItem> queueItemList = mediaController.getQueue();
            if (queueItemList != null) {
                int size = queueItemList.size();
                Log.d(TAG, "queueItemList size is " + size);
                for (int i = 0; i < size; i++) {
                    Log.d(TAG, "第" + i + "首, mediaId = " + queueItemList.get(i).getDescription().getMediaId()
                            + ", title = " + queueItemList.get(i).getDescription().getTitle());
                }
                if (index < size) {
                    MediaSession.QueueItem queueItem = queueItemList.get(index);
                    String mediaId = queueItem.getDescription().getMediaId();

                    CharSequence desc = queueItem.getDescription().getDescription();
                    long duration = Long.parseLong((String) desc);

                    Log.d(TAG, "skip to mediaId = " + mediaId + ", duration = " + duration
                            + ", title = " + queueItem.getDescription().getTitle());

                    mediaController.getTransportControls().playFromMediaId(mediaId, null);

                    position *= 1000;
                    if (position < duration) {
                        int finalPosition = position;
                        setOnMetadataChangedListener(id -> {
                            Log.d(TAG, "OnMetadataChangedListener() called with: id = [" + id + "]");
                            if (Objects.equals(mediaId, id)) {
                                Log.d(TAG, "start seek to " + finalPosition);
                                mediaController.getTransportControls().seekTo(finalPosition);
                                Log.d(TAG, "unregister metadata change listener ");
                                onMetadataChangedListener = null;
                            }
                        });
                        return true;
                    } else {
                        Log.d(TAG, "skipToQueueItem: position is too larger , can not seek to");
                        return false;
                    }

                } else {
                    Log.d(TAG, "skipToQueueItem: index is too larger , can not skip to");
                    return false;
                }
            } else {
                Log.d(TAG, "skipToQueueItem: queueItemList is null");
                return false;
            }
        } else {
            Log.d(TAG, "skipToQueueItem: mediaController is null");
            return false;
        }
    }

    public int[] getCurrentState() {
        int[] state = new int[2];
        if (mediaController != null) {

            MediaMetadata metadata = mediaController.getMetadata();
            if (metadata != null) {
                MediaDescription mediaDescription = metadata.getDescription();
                if (mediaDescription != null) {
                    state[0] = getIndexByMediaId(mediaDescription.getMediaId(), mediaController);
                } else {
                    Log.d(TAG, "getCurrentState: mediaDescription is null");
                }
            } else {
                Log.d(TAG, "getCurrentState: metadata is null");
            }

            PlaybackState playbackState = mediaController.getPlaybackState();
            if (playbackState != null) {
                long position = playbackState.getPosition();
                Log.d(TAG, "getCurrentState: position is " + position);
                state[1] = (int) position;
                Log.d(TAG, "getCurrentState: play state is " + playbackState.getState());
            } else {
                Log.d(TAG, "getCurrentState: playbackState is null");
            }

        } else {
            Log.d(TAG, "getCurrentState: mediaController is null");
        }
        Log.d(TAG, "getCurrentState: " + Arrays.toString(state));
        return state;
    }

    private int getIndexByMediaId(String mediaId, MediaController mediaController) {
        int index = 0;
        if (mediaController != null) {
            List<MediaSession.QueueItem> queueItemList = mediaController.getQueue();
            if (queueItemList != null) {
                int size = queueItemList.size();
                Log.d(TAG, "getIndexByMediaId: queueItemList size is " + queueItemList.size());
                String id;
                for (int i = 0; i < size; i++) {
                    if (queueItemList.get(i).getDescription() != null) {
                        id = queueItemList.get(i).getDescription().getMediaId();
                        if (Objects.equals(id, mediaId)) {
                            index = i;
                            break;
                        }
                    } else {
                        Log.d(TAG, "getIndexByMediaId: getDescription is null");
                    }
                }
            } else {
                Log.d(TAG, "getIndexByMediaId: queueItemList is null");
            }
        } else {
            Log.d(TAG, "getIndexByMediaId: mediaController is null");
        }
        return index;
    }

    public void play() {
        Log.d(TAG, "play() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().play();
        }
    }

    public void pause() {
        Log.d(TAG, "pause() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().pause();
        }
    }

    public void skipToPrevious() {
        Log.d(TAG, "skipToPrevious() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().skipToPrevious();
        }
    }

    public void skipToNext() {
        Log.d(TAG, "skipToNext() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().skipToNext();
        }
    }

    public void fastForward() {
        Log.d(TAG, "fastForward() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().fastForward();
        }
    }

    public void rewind() {
        Log.d(TAG, "rewind() called");
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().rewind();
        }
    }

    private void setPlayMode(PlayMode playMode) {
        //0 循环 , 1 随机 , 2 单曲
        //1，随机 2，顺序 3，单曲循环
        Bundle bundle = new Bundle();
        int mode = -1;
        if (playMode == PlayMode.ORDER) {
            //mode = 0;
            mode = 2;
        } else if (playMode == PlayMode.SINGLE) {
            //mode = 2;
            mode = 3;
        } else if (playMode == PlayMode.RANDOM) {
            //mode = 1;
            mode = 1;
        }
        Log.d(TAG, "mode = " + mode);
        bundle.putInt("mode", mode);
        if (mediaControllerNotNull()) {
            mediaController.getTransportControls().sendCustomAction("PLAY_MODE", bundle);
        }
    }

    public void playTheOrder() {
        Log.d(TAG, "playTheOrder() called");
        setPlayMode(PlayMode.ORDER);
    }

    public void playTheRandom() {
        Log.d(TAG, "playTheRandom() called");
        setPlayMode(PlayMode.RANDOM);
    }

    public void playTheSingle() {
        Log.d(TAG, "playTheSingle() called");
        setPlayMode(PlayMode.SINGLE);
    }

    public void playTheReserve() {
        Log.d(TAG, "playTheReserve() called , reserve mode now not support");
    }

    public byte getCurrentPlayState() {//01 : Play 02 : Pause
        Log.d(TAG, "getCurrentPlayState() called");
        if (mediaControllerNotNull()) {
            PlaybackState ps = mediaController.getPlaybackState();
            if (ps == null) {
                Log.d(TAG, "current playback state is null");
                return 0x00;
            }
            int state = ps.getState();
            Log.d(TAG, "current playback state = " + state);
            if (PlaybackState.STATE_PLAYING == state) {
                return 0x01;
            } else if (PlaybackState.STATE_PAUSED == state) {
                return 0x02;
            } else {
                Log.d(TAG, "current playback state belong to other state");
                return 0x00;
            }
        }
        return 0x22;
    }

    public byte getCurrentPlayMode() {//00:Normal Mode 01:Reserved    02:Repeat One file Mode   03:Random All Mode
        Log.d(TAG, "getCurrentPlayMode() called with: mode = [" + PLAY_MODE + "]");
        if (PLAY_MODE == 1) {//Random All Mode
            return 0x03;
        } else if (PLAY_MODE == 2) {//Normal Mode
            return 0x00;
        } else if (PLAY_MODE == 3) {//Repeat One file Mode
            return 0x02;
        } else {
            return 0x22;
        }
    }

    public byte getFastForwardRewindState() {
        Log.d(TAG, "getCurrentPlayMode() called with: state_fast_forward_rewind = [" + STATE_FAST_FORWARD_REWIND + "]");
        if (STATE_FAST_FORWARD_REWIND == 1) {//fast forward
            return 0x01;
        } else if (STATE_FAST_FORWARD_REWIND == 2) {//rewind
            return 0x02;
        } else if (STATE_FAST_FORWARD_REWIND == 3) {//normal
            return 0x00;
        } else {
            return 0x22;
        }
    }

    private boolean mediaControllerNotNull() {
        if (mediaController == null) {
            Log.d(TAG, "MediaController is null");
            return false;
        } else {
            return true;
        }
    }
}
