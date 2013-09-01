
package com.kimboo.googleimagesearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.kimboo.googleimagesearch.MainActivity.RemoteImageView;
import com.kimboo.googleimagesearch.utils.DiskLruImageCache;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class MainActivity extends Activity {
    
    EditText searchEditText;
    Button searchButton;
    GridView resultGridView;
    ViewSwitcher viewSwitch;
    
    List<GoogleImage> myList;
    
    final static String url = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=";
    final static String urlImages = "http://ajax.googleapis.com/ajax/services/search/images?v=1.0&q=";
    Integer resultsAmount = 12;
    EditText amountEditText;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initWidgets();
        resultsAmount = Integer.valueOf(amountEditText.getText().toString());
        searchButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                try {
                    if (!TextUtils.isEmpty(searchEditText.getText())) {
                        myList = new ArrayList<GoogleImage>();
                        if (viewSwitch.getDisplayedChild() == 1)
                            viewSwitch.showPrevious();
                        new GoogleImageTask().execute(searchEditText.getText().toString());
                    }
                } catch (Exception ex) {
                    Log.v("gsearch", "Error : " + ex.getMessage());
                }

            }
        });

    }

    private void initWidgets() {
        amountEditText = (EditText) findViewById(R.id.amountText);
        searchEditText = (EditText) findViewById(R.id.editText);
        searchButton = (Button) findViewById(R.id.submit);
        viewSwitch = (ViewSwitcher) findViewById(R.id.switcher);
        resultGridView = (GridView) findViewById(R.id.gridview);
    }

    class GoogleImageTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            final String searchString = params[0];
            try {
                
                for (int index = 1; index < resultsAmount; index+=4) {
                    searchRequest(searchString,index);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.v("gsearch", "Updating gridlayout...");
            updateGrid();
        }
        
    }

    public void updateGrid() {
        if (viewSwitch.getDisplayedChild() == 0)
            viewSwitch.showNext();
        resultGridView.setAdapter(new GoogleImagesAdapter());
    }
    
    class GoogleImagesAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public Object getItem(int arg0) {
            return myList.get(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            if (arg1 != null) {
                return arg1;
            } else {
                RemoteImageView view = new RemoteImageView(MainActivity.this, myList.get(arg0));
                view.setScaleType(RemoteImageView.ScaleType.FIT_XY);
                view.setLayoutParams(new GridView.LayoutParams(GridView.LayoutParams.WRAP_CONTENT,
                        100));
                view.setPadding(0, 0, 0, 0);
                return view;
            }
            
        }
        
    }
    
    class RemoteImageView extends ImageView {

        private GoogleImage model;
        private BitmapFactory.Options bitmapOptions;
        
        public GoogleImage getModel() {
            return model;
        }

        public void setModel(GoogleImage model) {
            this.model = model;
        }

        public BitmapFactory.Options getBitmapOptions() {
            return bitmapOptions;
        }

        public void setBitmapOptions(BitmapFactory.Options bitmapOptions) {
            this.bitmapOptions = bitmapOptions;
        }

        public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        public RemoteImageView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public RemoteImageView(Context context) {
            super(context);
        }

        public RemoteImageView(Context context, GoogleImage googleImage) {
            super(context);
            setModel(googleImage);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            setBitmapOptions(options);
            Log.v("gsearch", "Executing DownloadImage task...");
            new DownloadImage().execute(this);
        }
        
        class DownloadImage extends AsyncTask<RemoteImageView, Void, Void> {
            private Bitmap _bitmap;
            private RemoteImageView _current;

            @Override
            protected Void doInBackground(RemoteImageView... params) {
                _current = params[0];
                try {
                    Log.v("gsearch", "Downloading bitmap...");
                    _bitmap = downloadBitmap(_current.getModel().imageUrl, 
                            _current.getBitmapOptions());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                super.onPostExecute(result);
                Log.v("gsearch", "Setting bitmap in the view...");
                _current.setImageBitmap(_bitmap);
            }
            
        }
        
        /**
         * @param strURL: The url of the image to download.
         * @param options: Options to resize the image.
         * @return A bitmap from the result of the search.
         * @throws IOException
         */
        private Bitmap downloadBitmap(String strURL, BitmapFactory.Options options) throws IOException {
            InputStream inputStream = null;
            Bitmap bitmap = null;
            
            URL url = new URL(strURL);
            URLConnection conn = url.openConnection();

            try {
                HttpURLConnection httpConn = (HttpURLConnection) conn;
                httpConn.setRequestMethod("GET");
                httpConn.connect();

                if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    inputStream = httpConn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                    inputStream.close();
                } else {
                    Log.v("gsearch", "The connection has fucked up :D");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return bitmap;
        }
        
    }

    /**
     * Makes the request to google apis and save the results
     * in {@link myList} as {@link GoogleImage} objects.
     * @param searchString Something to search
     * @return a JSON with the google apis result.
     * @throws Exception
     */
    public void searchRequest(String searchString, Integer start) throws Exception {
        String newFeed = urlImages + URLEncoder.encode(searchString) + URLEncoder.encode(""+start);
        StringBuilder sb = new StringBuilder();
        Log.v("gsearch", "Search URL: " + newFeed);
        URL url = new URL(newFeed);
        HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
        if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    httpconn.getInputStream()), 8192);
            String strLine = "";
            while ((strLine = input.readLine()) != null) {
                sb.append(strLine);
            }
            input.close();
        }
        String resp = sb.toString();
        sb = new StringBuilder();
        Log.v("gsearch", "Search Reult: " + resp);
        JSONObject mResponseObj = new JSONObject(resp);
        JSONObject responseObj = mResponseObj.getJSONObject("responseData");
        JSONArray array = responseObj.getJSONArray("results");
        Log.v("gsearch", "Number of Reult: " + array.length());
        for (int i = 0; i < array.length(); i++) {
            String title = array.getJSONObject(i).getString("title").toString();
            String urlLink = array.getJSONObject(i).getString("visibleUrl").toString();
            String imageUri = array.getJSONObject(i).getString("url").toString();
            GoogleImage result = new GoogleImage(imageUri, title);
            Log.v("gsearch", result.toString());
            myList.add(result);
        }
        
    }
    
    class GoogleImage {
        
        public GoogleImage(String imageUri, String title2) {
            imageUrl = imageUri;
            title = title2;
        }
        
        public String imageUrl;
        public String title;
        
        @Override
        public String toString() {
            return "GoogleImage [imageUrl=" + imageUrl + ", title=" + title + "]";
        }
    }

}
