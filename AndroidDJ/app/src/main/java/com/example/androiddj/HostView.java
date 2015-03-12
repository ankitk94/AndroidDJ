package com.example.androiddj;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.androiddj.database.DatabaseHandler;
import com.example.androiddj.database.Songs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class HostView extends Activity {
    private static String tag = "DJ Debugging";
    ListView list;
    private boolean downloading = false;
    private ListViewAdapterHost adapter;
    int pos = -1;
    private ArrayList<String> song_names;
    public String folder;
    private ArrayList<Songs> songs;
    private DatabaseHandler db;

    public TextView startTimeField, endTimeField;
    private MediaPlayer mediaPlayer;
    private double startTime = 0;
    private double finalTime = 0;
    private Handler myHandler;
    private Handler downloadHandler;
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekbar;
    private ImageButton playButton, pauseButton;
    public static int oneTimeOnly = 0;
    int index = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(tag, "Going to call oncreate");
        Log.i(tag, "calling setcontent view");

        myHandler = new Handler();
        downloadHandler = new Handler();

        folder = Environment.getExternalStorageDirectory() + "/AndroidDJ-Playlist/";
        File dirs = new File(folder);

        if (!dirs.exists())
            dirs.mkdirs();

        Log.i(tag, "Going to create list_file");
        db = new DatabaseHandler(this);
        Log.i(tag, "Going to add song");
        db.deleteAllSongs();
        song_names = updatePlaylist();
        db.addAllSongs(song_names);
        Log.i(tag, "Going to create list: db size - " + String.valueOf(db.getSongsCount()));
        songs = db.getAllSongs();

        startTimeField = (TextView) findViewById(R.id.start_time);
        endTimeField = (TextView) findViewById(R.id.end_time);
        seekbar = (SeekBar) findViewById(R.id.seekBar1);
        playButton = (ImageButton) findViewById(R.id.play);
        pauseButton = (ImageButton) findViewById(R.id.pause);
        seekbar.setClickable(true);
        playButton.setEnabled(true);
        pauseButton.setEnabled(false);

        pauseButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);


        FileObserver plistObserver = new FileObserver(folder) {
            @Override
            public void onEvent(int event, String song_name) {
                if (event == FileObserver.DELETE) {
                    Log.d(tag, "Song deleted: " + song_name);
//                    db.deleteSongByName(song_name);
//                    songs = db.getAllSongs();
//                    plist_size=songs.size();
//                    adapter.notifyDataSetChanged();
                } else if (event == FileObserver.CREATE) {
                    Log.d(tag, "Song created: " + song_name);
                }
            }
        };

        plistObserver.startWatching();

        // Check if folder is empty only play if songs.size()>0

        list = (ListView) findViewById(R.id.listview);
        adapter = new ListViewAdapterHost(songs, HostView.this, pos, db);
        list.setAdapter(adapter);
        Log.i(tag, "Adapter set");
        Log.i(tag, "Defining on click listener");

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (pos == position) {
                    pos = -1;
                } else {
                    pos = position;
                }
                Log.i(tag, "Item at " + Integer.toString(position) + " is clicked");
                adapter.setPosition(pos);
                adapter.notifyDataSetChanged();
            }
        });

        Runnable downloadFile = new Runnable() {
            @Override
            public void run() {
                if (!downloading) {
                    Log.d(tag, "Starting Download..........");
                    try {
                        FileServerAsyncTask file = new FileServerAsyncTask(HostView.this);
                        file.execute();
                    } catch (IOException e) {
                        Log.e(tag, e.getMessage());
                    }
                }

//                Log.d(tag, "handler attached....");
                downloadHandler.postDelayed(this, 5000);
            }
        };

        Thread download = new Thread(downloadFile);
        download.start();

        Log.i(tag, "Going to call create list view");
        Log.i(tag, "Finished create list view");


    }


    public ArrayList<String> updatePlaylist() {
        File song = new File(folder);
        if (!song.isDirectory())
            Log.i(tag, "Not a directory");

        File[] listOfFiles = song.listFiles();

        if (listOfFiles != null)
            Log.i(tag, String.valueOf(listOfFiles.length));

        ArrayList<String> name = new ArrayList();
        Log.d(tag, "Adding songs");

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                name.add(listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                ;//System.out.println("Directory " + listOfFiles[i].getName());
            }
        }

        Log.d(tag, "Complete");

        return name;
    }


    public void play(View view) {
//        Toast.makeText(getApplicationContext(), "Playing sound",
//                Toast.LENGTH_SHORT).show();
        Log.i(tag, "usee");

        if(songs.size()>0)
        {
            if(mediaPlayer==null)
            {
                File file = new File(folder + songs.get(0).getName());
                Uri uri = Uri.fromFile(file);
                mediaPlayer = MediaPlayer.create(this, uri);
            }

            mediaPlayer.start();
            finalTime = mediaPlayer.getDuration();
            startTime = mediaPlayer.getCurrentPosition();
            if (oneTimeOnly == 0) {
                seekbar.setMax((int) finalTime);
                oneTimeOnly = 1;
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(fromUser)
                            mediaPlayer.seekTo(progress);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
            }

            endTimeField.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                            TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                            toMinutes((long) finalTime)))
            );
            startTimeField.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                            TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                            toMinutes((long) startTime)))
            );

            seekbar.setProgress((int) startTime);
            myHandler.postDelayed(UpdateSongTime, 100);
            pauseButton.setEnabled(true);
            playButton.setEnabled(false);

            pauseButton.setVisibility(View.VISIBLE);
            playButton.setVisibility(View.INVISIBLE);
        }
        else
        {
            Toast.makeText(this,"Playlist empty.",Toast.LENGTH_SHORT).show();
        }
    }


    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            if(mediaPlayer!=null)
            {
                startTime = mediaPlayer.getCurrentPosition();
                startTimeField.setText(String.format("%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                toMinutes((long) startTime)))
                );

                seekbar.setProgress((int) startTime);
                //Log.i("Media PLayer:::",startTimeField.getText().toString()+" "+endTimeField.getText().toString());
                if (startTimeField.getText().toString().equals(endTimeField.getText().toString())) {
//                songs=db.getAllSongs();
//                index=index+1;
                    // Delete completed song
                    File file = new File(folder + songs.get(index).getName());
                    file.delete();
                    db.deleteSong(songs.get(index).getID());
                    songs.remove(index);
                    adapter.notifyDataSetChanged();
                    Log.i(tag, "songs size - " + String.valueOf(songs.size()));

                    if (songs.size() > 0) {
                        // next song
                        file = new File(folder + songs.get(index).getName());
                        Uri uri = Uri.fromFile(file);
                        Log.i(tag, "Media PLayer next song: "+file.getName());
                        mediaPlayer = MediaPlayer.create(getApplicationContext(), uri);

                        Toast.makeText(getApplicationContext(), "Playing " + songs.get(index).getName(),
                                Toast.LENGTH_SHORT).show();

                        mediaPlayer.start();

                        finalTime = mediaPlayer.getDuration();
                        startTime = mediaPlayer.getCurrentPosition();
                        if (oneTimeOnly == 0) {
                            seekbar.setMax((int) finalTime);
                            oneTimeOnly = 1;
                        }

                        endTimeField.setText(String.format("%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                                        TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                        toMinutes((long) finalTime)))
                        );
                        startTimeField.setText(String.format("%02d:%02d",
                                        TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                                        TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                                        toMinutes((long) startTime)))
                        );
                        seekbar.setProgress((int) startTime);
                    }
                    else{
                        pause(findViewById(R.id.pause));
                        mediaPlayer = null;
                    }

                }
            }
            if(mediaPlayer != null)
                myHandler.postDelayed(this, 100);

        }
    };

    public void pause(View view) {
//        Toast.makeText(getApplicationContext(), "Pausing sound",
//                Toast.LENGTH_SHORT).show();
        mediaPlayer.pause();
        pauseButton.setEnabled(false);
        playButton.setEnabled(true);

        pauseButton.setVisibility(View.INVISIBLE);
        playButton.setVisibility(View.VISIBLE);
    }

    public void forward(View view) {
        int temp = (int) startTime;
        if ((temp + forwardTime) <= finalTime) {
            startTime = startTime + forwardTime;
            mediaPlayer.seekTo((int) startTime);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Cannot jump forward 5 seconds",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void rewind(View view) {
        int temp = (int) startTime;
        if ((temp - backwardTime) > 0) {
            startTime = startTime - backwardTime;
            mediaPlayer.seekTo((int) startTime);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Cannot jump backward 5 seconds",
                    Toast.LENGTH_SHORT).show();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addSong(String name) {
        int i = songs.size();
        Songs s = new Songs(++i, name);
        db.addSong(s);
        songs.add(s);
        Log.d(tag, "Song added : " + s);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.i(tag, "Activity is restarted");
//        songs = db.getAllSongs();
//        adapter.notifyDataSetChanged();
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.i(tag, "Activity is stopped");
//        songs.clear();
//        db.deleteAllSongs();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        songs.clear();
//        db.deleteAllSongs();
        Log.i(tag, "Activity is destroyed:" + String.valueOf(db.getSongsCount()) + " " + String.valueOf(songs.size()));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:

                moveTaskToBack(true);

                return true;
        }
        return false;
    }

    private class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context context;
        private int port = 8988;
        private ServerSocket serverSocket;

        /**
         * @param context
         */
        public FileServerAsyncTask(Context context) throws IOException {
            this.context = context;
            serverSocket = new ServerSocket(port);
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.d(WiFiDirectActivity.TAG, "Server: Socket opened");

                downloading = true;
                Log.d(tag, "Downloading started: " + downloading);

                Socket client = serverSocket.accept();
                String recieved_fname = "";
                Log.d(WiFiDirectActivity.TAG, "Server: connection done");

                InputStream inputstream = client.getInputStream();
                InputStreamReader isr = new InputStreamReader(inputstream);
                BufferedReader br = new BufferedReader(isr);
                Log.d(WiFiDirectActivity.TAG, "recieving file");
                recieved_fname = br.readLine();

                Log.d(WiFiDirectActivity.TAG, "recieved file name" + " " + recieved_fname);
                String filename = System.currentTimeMillis() + recieved_fname;
                final File f = new File(folder + filename);
                f.createNewFile();

                Log.d(WiFiDirectActivity.TAG, "server: copying files " + f.toString());

                copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                Log.d(WiFiDirectActivity.TAG, "recieved file written " + f.getAbsolutePath());

                return filename;
            } catch (IOException e) {
                Log.e(WiFiDirectActivity.TAG, e.getMessage());
                downloading = false;
                Log.d(tag, "Downloading Error: " + downloading);
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                addSong(result);
                downloading = false;
                Log.d(tag, "Downloading Completed");
            }

        }

    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(WiFiDirectActivity.TAG, "In copy: " + e.toString());
            return false;
        }
        return true;
    }
}