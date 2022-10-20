package demo.h5.udesk.udeskh5demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        final EditText url = findViewById(R.id.url);
        url.setText("http://udesksdk.udesk.cn/im_client");
        findViewById(R.id.enterChat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,UdeskWebViewActivity.class);
                intent.putExtra("url",url.getText().toString());
                startActivity(intent);
            }
        });
    }
}