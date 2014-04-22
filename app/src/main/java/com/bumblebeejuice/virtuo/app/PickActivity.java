package com.bumblebeejuice.virtuo.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class PickActivity extends Activity {

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick);

        ArrayList<Pair<String, String>> movies = new ArrayList<Pair<String, String>>();
        movies.add(new Pair<String, String>("Boxing Gym - 22MB", "http://www.mediafire.com/download/7aje0u6uqd4214j/Boxing.mp4"));
        movies.add(new Pair<String, String>("Concert - 223MB", "http://www.mediafire.com/download/1l2cerqxfky6tr6/Disclosure_Latch.mp4"));

        ListView listView = (ListView) findViewById(R.id.listView);

        final ArrayAdapter<Pair<String, String>> listAdapter = new ArrayAdapter<Pair<String, String>>(this, 0, movies) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                Pair<String, String> movie = getItem(position);

                View cell = getLayoutInflater().inflate(R.layout.cell_pick, parent, false);
                TextView title = (TextView) cell.findViewById(R.id.textView);
                title.setText(movie.first);
                return cell;
            }
        };

        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pair<String, String> movie = listAdapter.getItem(position);
                final Uri uri = Uri.parse(movie.second);
                String movieName = uri.getLastPathSegment();
                final File movieFile = new File(getCacheDir(), movieName);

                if (movieFile.exists()) {
                    Intent playerIntent = new Intent(PickActivity.this, PlayerActivity.class);
                    playerIntent.setData(Uri.fromFile(movieFile));
                    startActivity(playerIntent);

                } else {

                    new AsyncTask<String, Integer, Boolean>() {

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();

                            progressDialog = new ProgressDialog(PickActivity.this);
                            progressDialog.setTitle("Downloading");
                            progressDialog.setIndeterminate(false);
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setProgress(0);
                            progressDialog.setProgress(100);
                            progressDialog.setCancelable(false);
                            progressDialog.show();
                        }

                        @Override
                        protected Boolean doInBackground(String... params) {
                            HttpURLConnection connection = null;
                            BufferedInputStream inputStream = null;
                            BufferedOutputStream outputStream = null;

                            boolean success = false;

                            try {
                                URL url = new URL(uri.toString());
                                connection = (HttpURLConnection) url.openConnection();
                                connection.setDoInput(true);

                                int size = connection.getContentLength();
                                int totalBytesRead = 0;

                                byte[] buffer = new byte[8 * 1024];
                                int bytes_read;
                                inputStream = new BufferedInputStream(connection.getInputStream());
                                outputStream = new BufferedOutputStream(new FileOutputStream(movieFile));

                                while ((bytes_read = inputStream.read(buffer, 0, 8 * 1024)) > 0) {
                                    outputStream.write(buffer, 0, bytes_read);
                                    totalBytesRead += bytes_read;
                                    publishProgress((int) (100.0 * totalBytesRead / size));
                                }

                                success = true;

                            } catch (Exception e) {

                                LTErrorHandler.handleException(e);
                                final String message = e.getMessage();
                                getWindow().getDecorView().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PickActivity.this, message, Toast.LENGTH_LONG).show();
                                    }
                                });
                            } finally {
                                if (outputStream != null) {
                                    try {
                                        outputStream.close();
                                    } catch (Exception e) {
                                        LTErrorHandler.handleException(e);
                                    }
                                }

                                if (inputStream != null) {
                                    try {
                                        inputStream.close();
                                    } catch (Exception e) {
                                        LTErrorHandler.handleException(e);
                                    }
                                }

                                if (connection != null)
                                    connection.disconnect();

                                if (!success) movieFile.delete();
                            }

                            return success;
                        }

                        @Override
                        protected void onProgressUpdate(Integer... values) {
                            super.onProgressUpdate(values);
                            if (progressDialog == null) {
                                progressDialog = new ProgressDialog(PickActivity.this);
                                progressDialog.setTitle("Downloading");
                                progressDialog.setIndeterminate(false);
                                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                progressDialog.setProgress(0);
                                progressDialog.setProgress(100);
                                progressDialog.setCancelable(false);
                                progressDialog.show();
                            }
                            progressDialog.setProgress(values[0]);
                        }

                        @Override
                        protected void onPostExecute(Boolean success) {
                            super.onPostExecute(success);

                            if (progressDialog != null) {
                                progressDialog.dismiss();
                                progressDialog = null;
                            }

                            if (success) {
                                Intent playerIntent = new Intent(PickActivity.this, PlayerActivity.class);
                                playerIntent.setData(Uri.fromFile(movieFile));
                                startActivity(playerIntent);
                            }
                        }
                    }.execute();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pick, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

}
