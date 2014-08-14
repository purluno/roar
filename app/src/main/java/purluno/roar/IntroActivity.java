package purluno.roar;

import android.app.Activity;
import android.os.Bundle;

public class IntroActivity extends Activity {
    private IntroActivitySupport support;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        support = new IntroActivitySupport(this);
        support.onCreate(savedInstanceState);
    }

}
