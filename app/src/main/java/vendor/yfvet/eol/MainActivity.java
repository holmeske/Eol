package vendor.yfvet.eol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    private EolMediaController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService(new Intent(this, EolService.class));

        controller = new EolMediaController(this);
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.play) {
            if (controller != null) controller.play();
        } else if (id == R.id.pause) {
            if (controller != null) controller.pause();
        } else if (id == R.id.skip_to_previous) {
            if (controller != null) controller.skipToPrevious();
        } else if (id == R.id.skip_to_next) {
            if (controller != null) controller.skipToNext();
        } else if (id == R.id.fast_forward) {
            if (controller != null) controller.fastForward();
        } else if (id == R.id.rewind) {
            if (controller != null) controller.rewind();
        } else if (id == R.id.order_play) {
            if (controller != null) controller.playTheOrder();
        } else if (id == R.id.random_play) {
            if (controller != null) controller.playTheRandom();
        } else if (id == R.id.single_play) {
            if (controller != null) controller.playTheSingle();
        } else if (id == R.id.skip_to_queue_item) {
            if (controller != null) controller.skipToQueueItem(1, 100);
        } else if (id == R.id.current_state) {
            if (controller != null) controller.getCurrentState();
        }
    }
}
