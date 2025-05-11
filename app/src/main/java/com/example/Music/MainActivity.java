package com.example.Music;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements ServiceConnection {
    private static final String ACTION_PRE ="PREVIOUS" ;
    private static final String ACTION_PLAY ="PLAY" ;
    private static final String ACTION_NEXT ="NEXT" ;
    private static final int CHANNEL_ID_1 =100 ;
    private static final String CHANNEL_ID = "Music";

    RemoteViews small_view,large_view;
    RecyclerView musicRecView;
    ArrayList<MusicModel> musicList,tempList;
    MusicAdapter adapter;
    TextView title_song;
    ImageView nextBtn, preBtn, playBtn, music_icon1;

    MediaPlayer myMediaPlayer;

    MusicModel currentSong;

    RotateAnimation animation;

    ArrayList<MusicModel> songList,copyList;

    RelativeLayout bottomMusicPlayer;

    NotificationManager notificationManager;
    MusicService musicService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        String[] permission={"android.permission.ACCESS_MEDIA_LOCATION"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission,100);
        }
        //Taking permission from the user
        if (!checkPermission()) {
            requestPermission();
        }

        String[] permission1={"android.permission.POST_NOTIFICATIONS"};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permission1,100);
        }



        //Starting
        initViews();


        //Setting adapter
        tempList=new ArrayList<>();
        myMediaPlayer = MyMediaPlayer.getInstance();
        musicRecView.setLayoutManager(new GridLayoutManager(this, 1));
        loadSongs();
        copyList=musicList;

        //adapter ClickListener
        adapter = new MusicAdapter(this, musicList,copyList);
        adapter.setOnItemClickListener(new MusicAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                    createNotificationChannel();
                    animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                    animation.setDuration(20000);
                    animation.setRepeatCount(Animation.INFINITE);
                    animation.setInterpolator(new LinearInterpolator());
                    music_icon1.startAnimation(animation);

                    preBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            playPreviousMusic();
                        }
                    });

                    playBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            pausePlayMusic();
                        }
                    });

                    nextBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            playNextMusic();
                        }
                    });
                }
            });
            musicRecView.setAdapter(adapter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setResAfterReturn();
        if(!myMediaPlayer.isPlaying()){
            music_icon1.clearAnimation();
            playBtn.setImageResource(R.drawable.play_button);
        }
        adapter.notifyDataSetChanged();
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,this,BIND_AUTO_CREATE);
    }

    void initViews()
    {
        musicList = new ArrayList<>();
        musicRecView = findViewById(R.id.music_rec_view);
        title_song=findViewById(R.id.title_song);
        title_song.setSelected(true);
        preBtn=findViewById(R.id.previous);
        nextBtn=findViewById(R.id.next);
        playBtn=findViewById(R.id.play_pause);
        music_icon1=findViewById(R.id.icon);
        bottomMusicPlayer=findViewById(R.id.underRecView);
        small_view=new RemoteViews(getPackageName(), R.layout.layout_one_small_notification);
        large_view=new RemoteViews(getPackageName(), R.layout.layout_two_large_notification);
    }

    //Checking permission
    boolean checkPermission()
    {
        int result= ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_AUDIO);
        if(result== PackageManager.PERMISSION_GRANTED)
        {
            return true;
        }
        else{
            requestPermission();
            return false;
        }
    }



    //Requesting permission
    void requestPermission()
    {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_AUDIO)){
                Toast.makeText(this, "READ PERMISSION IS REQUIRED, PLEASE ALLOW FROM SETTING", Toast.LENGTH_SHORT).show();
            }
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_MEDIA_AUDIO},123);
        }
    }


    void setMusicResources()  {
            currentSong = musicList.get(MyMediaPlayer.currentIndex);
            title_song.setText(currentSong.getTitle());
            music_icon1.setImageDrawable(getImage(currentSong.getPath()));
            playBtn.setOnClickListener(view -> pausePlayMusic());
            preBtn.setOnClickListener(view -> playPreviousMusic());
            nextBtn.setOnClickListener(view -> playNextMusic());

            playMusic();
            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    //playNextMusic();
                }
            });
    }



    //Loading songs
    public void loadSongs()
    {
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = {
                    MediaStore.Audio.Media.BITRATE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DURATION
            };

            String sortOrder=MediaStore.Audio.Media.DATE_ADDED;
            String selection = MediaStore.Audio.Media.IS_MUSIC + " !=0 ";
            Cursor cursor = getContentResolver().query(uri, projection, selection, null, sortOrder);


        songList=new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    MusicModel song = new MusicModel(cursor.getString(1), cursor.getString(2), cursor.getString(3));
                    songList.add(song);
                }
                for(int i=songList.size()-1;i>=0;i--)
                {
                    musicList.add(songList.get(i));
                }
            } else {
                Toast.makeText(this, "No Music exist", Toast.LENGTH_SHORT).show();
            }
    }


    private void playMusic()  {
        try {
            myMediaPlayer.reset();
            myMediaPlayer.setDataSource(currentSong.getPath());
            myMediaPlayer.prepare();
            myMediaPlayer.start();
            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                }
            });
            playBtn.setImageResource(R.drawable.pause_button);
            adapter.notifyDataSetChanged();
        }catch (Exception e)
        {
            AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle("Exception")
                    .setMessage(e.toString())
                    .setCancelable(true)
                    .show();
        }

    }

    private void playPreviousMusic()  {

        if (MyMediaPlayer.currentIndex <=0) {
            MyMediaPlayer.currentIndex = musicList.size()-1;
            setMusicResources();
            return;
        }
        MyMediaPlayer.currentIndex=MyMediaPlayer.currentIndex-1;
        myMediaPlayer.reset();
        setMusicResources();
    }

    private void playNextMusic() {
        if(MyMediaPlayer.currentIndex>=musicList.size()-1)
        {
            MyMediaPlayer.currentIndex=0;
            setMusicResources();
            return;
        }
        MyMediaPlayer.currentIndex = MyMediaPlayer.currentIndex + 1;
        myMediaPlayer.reset();
        setMusicResources();
    }

    public void pausePlayMusic()
    {
        if(myMediaPlayer.isPlaying())
        {
            //builder.setAutoCancel(true);
            Drawable play=getResources().getDrawable(R.drawable.play_button);
            playBtn.setImageDrawable(play);
            myMediaPlayer.pause();
            music_icon1.clearAnimation();
            //builder.setAutoCancel(true);
        }
        else{
            Drawable pause=getResources().getDrawable(R.drawable.pause_button);
            playBtn.setImageDrawable(pause);
            myMediaPlayer.start();
            music_icon1.startAnimation(animation);
        }
        adapter.notifyDataSetChanged();
    }



    void setResAfterReturn()
    {
        if(myMediaPlayer.isPlaying())
        {
            music_icon1.setImageDrawable(getImage(musicList.get(MyMediaPlayer.currentIndex).getPath()));
            int pos=MyMediaPlayer.currentIndex;
            title_song.setText(musicList.get(pos).getTitle());
            playBtn.setImageResource(R.drawable.pause_button);

            playBtn.setOnClickListener(view -> pausePlayMusic());
            preBtn.setOnClickListener(view -> playPreviousMusic());
            nextBtn.setOnClickListener(view -> playNextMusic());
            bottomPlayerClickListener(pos);
        }else{
            if(myMediaPlayer!=null&&MyMediaPlayer.currentIndex!=-1)
            {
                music_icon1.setImageDrawable(getImage(musicList.get(MyMediaPlayer.currentIndex).getPath()));
                int pos=MyMediaPlayer.currentIndex;
                title_song.setText(musicList.get(pos).getTitle());
                playBtn.setImageResource(R.drawable.pause_button);
                bottomPlayerClickListener(pos);
            }
        }
    }

    void bottomPlayerClickListener(int loc)
    {
        bottomMusicPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, MusicPlayerActivity.class);
                intent.putExtra("myList", musicList);
                intent.putExtra("POS", loc);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

            }
        });
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        try {
            int id = item.getItemId();
            if (id == R.id.searchBtn1) {
                return true;
            }
        }catch (Exception ignored)
        {
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try{
            MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.new_search,menu);
        MenuItem menuItem=menu.findItem(R.id.searchBtn1);
        SearchView searchView= (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                String searchStr=newText;
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        }catch (Exception e)
        {
            AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle("Exception")
                    .setMessage(e.toString())
                    .setCancelable(true)
                    .show();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        MusicService.MyBinder binder=(MusicService.MyBinder) iBinder;
        musicService=binder.getService();
        Log.e("Connected",musicService+"");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicService=null;
        Log.e("disconnected",musicService+"");
    }


    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this);
    }

    public RoundedBitmapDrawable getImage(String path)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);

        byte[] albumArt = retriever.getEmbeddedPicture();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.music_icon);
        //
        if (albumArt != null) {
            bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            // use the bitmap as desired
        }
        RoundedBitmapDrawable roundedBitmapDrawable= RoundedBitmapDrawableFactory.create(Resources.getSystem(),bitmap);
        roundedBitmapDrawable.setCircular(true);
        return roundedBitmapDrawable;
    }

    public Bitmap getBitmapImage(String path)
    {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);

        byte[] albumArt = retriever.getEmbeddedPicture();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.music_icon);
        //
        if (albumArt != null) {
            bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length);
            // use the bitmap as desired
        }
        RoundedBitmapDrawable roundedBitmapDrawable= RoundedBitmapDrawableFactory.create(Resources.getSystem(),bitmap);
        roundedBitmapDrawable.setCircular(true);
        return bitmap;
    }

    private void createNotificationChannel() {

        //Activity Intent
        Intent activityIntent=new Intent(getApplicationContext(),MainActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent=PendingIntent.getActivity(this,0,activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);


        //Play pause music
        Intent playIntent=new Intent();


        Bitmap img= BitmapFactory.decodeResource(getResources(),R.drawable.music_icon);

        setSmallView(small_view);
        setLargeView(large_view);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.music_icon)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(small_view)
                .setCustomBigContentView(large_view)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(contentIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            //notificationManager.notify(100,builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        if(myMediaPlayer!=null)
        {
            myMediaPlayer.release();
        }
        super.onDestroy();
    }

    void setSmallView(RemoteViews view)
    {
        if(myMediaPlayer.isPlaying()){
            view.setImageViewResource(R.id.play_pause_small,R.drawable.pause_button);
            view.setTextViewText(R.id.title_song_small,musicList.get(MyMediaPlayer.currentIndex).getTitle());
            Bitmap img=getBitmapImage(musicList.get(MyMediaPlayer.currentIndex).getPath());
            view.setImageViewBitmap(R.id.icon_small,img);
        }
        else{
            view.setImageViewResource(R.id.play_pause_small,R.drawable.play_button);
        }
    }

    void setLargeView(RemoteViews view)
    {
        if(myMediaPlayer.isPlaying()){
            view.setImageViewResource(R.id.play_pause_large,R.drawable.pause_button);
            view.setTextViewText(R.id.title_song_large,musicList.get(MyMediaPlayer.currentIndex).getTitle());
            Bitmap img=getBitmapImage(musicList.get(MyMediaPlayer.currentIndex).getPath());
            view.setImageViewBitmap(R.id.logo_large,img);
        }
        else{
            view.setImageViewResource(R.id.play_pause_large,R.drawable.play_button);
        }
    }
}