package net.msembodo.prayertime_nogps;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.msembodo.jprayertime.PrayerTimes;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {

    private TextInputLayout inputLayoutLocation;
    private EditText editLocation;
    private ImageView imgTip;
    private TextView txtLocDetail;
    private CardView cvPT;
    private ImageView imgTime;

    private ProgressDialog pd;

    private boolean hasBeenClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editLocation = (EditText) findViewById(R.id.editLocation);

        pd = new ProgressDialog(this);

        imgTip = (ImageView) findViewById(R.id.imageTip);
        imgTip.setImageResource(R.drawable.tips);

        String tipsMessage = "You can enter as if you wanted to search a location in a map." +
                " Prayer time will be calculated for that specific area.";

        txtLocDetail = (TextView) findViewById(R.id.textLocationDetail);
        txtLocDetail.setText(tipsMessage);

        cvPT = (CardView) findViewById(R.id.cardViewPT);
        cvPT.setVisibility(View.GONE);

        imgTime = (ImageView) findViewById(R.id.imageTime);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasBeenClicked) {
            String strLocation = editLocation.getText().toString();
            buildData(strLocation);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("wasClicked", hasBeenClicked);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        hasBeenClicked = savedInstanceState.getBoolean("wasClicked");
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editLocation.getWindowToken(), 0);
    }

    public void onClickGo(View view) {
        inputLayoutLocation = (TextInputLayout) findViewById(R.id.input_layout_location);
        String strLocation = editLocation.getText().toString();
        if (strLocation.equals("")) {
            inputLayoutLocation.setError(getString(R.string.err_msg_location));
        }
        else {
            buildData(strLocation);
            hideKeyboard();
            hasBeenClicked = true;
        }
    }

    public void buildData(String location) {
        new RetrievePrayerTimes().execute(location);
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    */

    class RetrievePrayerTimes extends AsyncTask<String, String, PrayerTimes> {
        PrayerTimes pt;

        @Override
        protected PrayerTimes doInBackground(String... locations) {
            publishProgress("Getting latitude & longitude...");

            try {
                pt = new PrayerTimes(locations[0]);
                return pt;
            }
            catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... status) {
            pd.setMessage(status[0]);
            pd.setIndeterminate(true);
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected void onPostExecute(PrayerTimes prayerTimes) {
            if (prayerTimes == null) {
                pd.dismiss();
                Toast.makeText(getApplicationContext(), "Location not found.", Toast.LENGTH_LONG).show();
            }
            else {
                pd.dismiss();

                String messageLocDetail = prayerTimes.formattedAddress + " (" + prayerTimes.timeZoneName + ")";

                txtLocDetail.setText(messageLocDetail);
                imgTip.setImageResource(R.drawable.pinmap3);

                cvPT.setVisibility(View.VISIBLE);
                imgTime.setImageResource(R.drawable.time);

                TextView txtFajr = (TextView) findViewById(R.id.textFajr);
                String strFajr = String.format("%1$02d:%2$02d", prayerTimes.fajrTime[0], prayerTimes.fajrTime[1]);
                txtFajr.setText(strFajr);

                TextView txtSunrise = (TextView) findViewById(R.id.textSunrise);
                String strSunrise = String.format("%1$02d:%2$02d", prayerTimes.sunriseTime[0], prayerTimes.sunriseTime[1]);
                txtSunrise.setText(strSunrise);

                TextView txtZuhr = (TextView) findViewById(R.id.textZuhr);
                String strZuhr = String.format("%1$02d:%2$02d", prayerTimes.zuhrTime[0],prayerTimes.zuhrTime[1]);
                txtZuhr.setText(strZuhr);

                TextView txtAsr = (TextView) findViewById(R.id.textAsr);
                String strAsr = String.format("%1$02d:%2$02d", prayerTimes.asrTime[0], prayerTimes.asrTime[1]);
                txtAsr.setText(strAsr);

                TextView txtMaghrib = (TextView) findViewById(R.id.textMaghrib);
                String strMaghrib = String.format("%1$02d:%2$02d", prayerTimes.maghribTime[0], prayerTimes.maghribTime[1]);
                txtMaghrib.setText(strMaghrib);

                TextView txtIsha = (TextView) findViewById(R.id.textIsha);
                String strIsha = String.format("%1$02d:%2$02d", prayerTimes.ishaTime[0], prayerTimes.ishaTime[1]);
                txtIsha.setText(strIsha);
            }

        }
    }
}
