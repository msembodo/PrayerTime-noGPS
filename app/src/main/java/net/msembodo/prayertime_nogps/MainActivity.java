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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.msembodo.extendedwidget.ClearableAutoCompleteTextView;
import net.msembodo.jprayertime.PrayerTimes;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ClearableAutoCompleteTextView editLocation;
    private ImageView imgTip;
    private TextView txtLocDetail;
    private CardView cvPT;
    private ImageView imgTime;

    private ProgressDialog pd;

    private boolean hasBeenClicked;

    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";
    private static String API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editLocation = (ClearableAutoCompleteTextView) findViewById(R.id.editLocation);
        editLocation.setAdapter(new GooglePlacesAutoCompleteAdapter(this, R.layout.list_item));
        editLocation.setOnItemClickListener(this);
        editLocation.showHideClearButton();
        editLocation.hideClearButton();

        API_KEY = this.getString(R.string.api_key);

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

    public void onItemClick(AdapterView adapterView, View view, int position, long id) {
        editLocation.setText((String) adapterView.getItemAtPosition(position));
    }

    public static ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResult = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            //sb.append("&components=country:id");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // load results into StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1)
                jsonResult.append(buff, 0, read);

        } catch (MalformedURLException e) {
            return resultList;
        } catch (IOException e) {
            return resultList;
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        try {
            JSONObject jsonObj = new JSONObject(jsonResult.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                //System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
                //System.out.println("============================================================");
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {}

        return resultList;
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
        TextInputLayout inputLayoutLocation = (TextInputLayout) findViewById(R.id.input_layout_location);
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

    class GooglePlacesAutoCompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return (String) resultList.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // retrieve the autocomplete result
                        resultList = autocomplete(constraint.toString());

                        // assign data to FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }

                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0)
                        notifyDataSetChanged();
                    else
                        notifyDataSetInvalidated();
                }
            };
            return filter;
        }
    }
}
