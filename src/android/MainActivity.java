package <%PACKAGE_NAME%>;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private Runnable onUserLeaveHintCallback;

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
}