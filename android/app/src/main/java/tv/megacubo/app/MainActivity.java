package tv.megacubo.pip;

import com.getcapacitor.BridgeActivity;
import tv.megacubo.pip.PIPPlugin; // Add import

public class MainActivity extends BridgeActivity {
    private Runnable onUserLeaveHintCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerPlugin(PIPPlugin.class); // Register the plugin
    }

    public void setOnUserLeaveHintCallback(Runnable callback) {
        this.onUserLeaveHintCallback = callback;
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (onUserLeaveHintCallback != null) {
            onUserLeaveHintCallback.run();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (onUserLeaveHintCallback != null && !isChangingConfigurations()) {
            onUserLeaveHintCallback.run();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (!hasFocus && onUserLeaveHintCallback != null) {
            onUserLeaveHintCallback.run();
        }
    }
}