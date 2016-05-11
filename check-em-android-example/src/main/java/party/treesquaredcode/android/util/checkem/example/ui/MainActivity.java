package party.treesquaredcode.android.util.checkem.example.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import party.treesquaredcode.android.util.checkem.CheckEm;
import party.treesquaredcode.android.util.checkem.example.R;
import party.treesquaredcode.android.util.checkem.MicrScanActivity;

public class MainActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity__main_activity);
        Toast.makeText(this, CheckEm.getCheckEmString(), Toast.LENGTH_LONG).show();
		findViewById(R.id.main_activity__button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckEm.getSharedInstance().checkEm(MainActivity.this);
//				Intent intent = new Intent(MainActivity.this, MicrScanActivity.class);
//				startActivity(intent);
            }
        });
	}

    @Override
    protected void onResume() {
        super.onResume();
        if (CheckEm.getSharedInstance().getRoutingNumberResult() != null) {
            ((TextView) findViewById(R.id.main_activity__routing)).setText("Routing: " + CheckEm.getSharedInstance().getRoutingNumberResult());
        } else {
            ((TextView) findViewById(R.id.main_activity__routing)).setText("");
        }
        if (CheckEm.getSharedInstance().getAccountNumberResult() != null) {
            ((TextView) findViewById(R.id.main_activity__account)).setText("Account: " + CheckEm.getSharedInstance().getAccountNumberResult());
        } else {
            ((TextView) findViewById(R.id.main_activity__account)).setText("");
        }
        CheckEm.getSharedInstance().clearResults();

    }
}
