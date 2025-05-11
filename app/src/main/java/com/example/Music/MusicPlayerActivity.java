package com.example.Music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MusicPlayerActivity extends AppCompatActivity {

    TextView title, currentTime, totalTime,myName;
    SeekBar seekBar;
    ImageView previousBtn, nextBtn, playPauseBtn, logoMusic;

    ArrayList<MusicModel> musicList,copyList;

    MediaPlayer myMediaPlayer = MyMediaPlayer.getInstance();

    MusicModel currentSong;
    RotateAnimation animation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);
        getSupportActionBar().hide();
        musicList = new ArrayList<>();
        try {
            musicList = (ArrayList<MusicModel>) getIntent().getSerializableExtra("myList");
            copyList = (ArrayList<MusicModel>) getIntent().getSerializableExtra("myCopyList");
        } catch (Exception e) {
            AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle("Exception")
                    .setMessage(e.toString())
                    .setCancelable(true)
                    .show();
        }


        title = findViewById(R.id.song_title);
        title.setSelected(true);
        seekBar = findViewById(R.id.seekBarDuration);
        currentTime = findViewById(R.id.current_time);
        totalTime = findViewById(R.id.total_time);
        previousBtn = findViewById(R.id.pre_button);
        nextBtn = findViewById(R.id.next_button);
        playPauseBtn = findViewById(R.id.play_button);
        logoMusic = findViewById(R.id.music_player_icon);
        animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setDuration(20000);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setInterpolator(new LinearInterpolator());
        myName=findViewById(R.id.myName);


        //setting seek bar from myMediaPlayer
        seekBar.setProgress(0);
        seekBar.setMax(myMediaPlayer.getDuration());
        //setting resources
        setMusicResources();

        MusicPlayerActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (myMediaPlayer != null) {
                    seekBar.setProgress(myMediaPlayer.getCurrentPosition());
                    currentTime.setText(ConvertToMMS(myMediaPlayer.getCurrentPosition() + ""));

                    if (myMediaPlayer.isPlaying()) {
                        playPauseBtn.setImageResource(R.drawable.pause_button);
                    }
                }
                new Handler().postDelayed(this, 100);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (myMediaPlayer != null && b) {
                    myMediaPlayer.seekTo(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        setResAfterReturn();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        seekBar.setProgress(0);
        seekBar.setMax(myMediaPlayer.getDuration());
        setResAfterReturn();
        if(myMediaPlayer.isPlaying()){
            logoMusic.startAnimation(animation);
        }
    }

    void setMusicResources() {
        try {
        if (musicList.size() > MyMediaPlayer.currentIndex && MyMediaPlayer.currentIndex >= 0) {
            currentSong = musicList.get(MyMediaPlayer.currentIndex);
            title.setText(currentSong.getTitle());
            logoMusic.setImageDrawable(getImage(currentSong.getPath()));
            totalTime.setText(ConvertToMMS(currentSong.getDuration()));
            playPauseBtn.setOnClickListener(view -> pausePlayMusic());
            previousBtn.setOnClickListener(view -> playPreviousMusic());
            nextBtn.setOnClickListener(view -> playNextMusic());

            playMusic();
            myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    playNextMusic();
                }
            });

        }
        } catch (Exception e) {
            AlertDialog builder = new AlertDialog.Builder(this)
                    .setTitle("Exception")
                    .setMessage(e.toString())
                    .setCancelable(true)
                    .show();
        }
    }


    private void playMusic() {
        try {
            myMediaPlayer.setDataSource(currentSong.getPath());
            myMediaPlayer.prepare();
            myMediaPlayer.start();
            seekBar.setProgress(0);
            seekBar.setMax(myMediaPlayer.getDuration());
            logoMusic.startAnimation(animation);
        } catch (Exception e) {

        }

    }

    private void playPreviousMusic() {

        if (MyMediaPlayer.currentIndex <=0) {
            myMediaPlayer.reset();
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
            myMediaPlayer.reset();
            MyMediaPlayer.currentIndex=0;
            setMusicResources();
            return;
        }
        MyMediaPlayer.currentIndex = MyMediaPlayer.currentIndex + 1;
        myMediaPlayer.reset();
        setMusicResources();

    }

    public void pausePlayMusic() {
        if (myMediaPlayer.isPlaying()) {
            Drawable pause = getResources().getDrawable(R.drawable.play_button);
            playPauseBtn.setImageDrawable(pause);
            logoMusic.clearAnimation();
            myMediaPlayer.pause();
        } else {
            Drawable pause = getResources().getDrawable(R.drawable.pause_button);
            playPauseBtn.setImageDrawable(pause);
            logoMusic.startAnimation(animation);
            myMediaPlayer.start();
        }
    }


    public String ConvertToMMS(String duration) {
        Long milLies = Long.parseLong(duration);
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milLies) % TimeUnit.HOURS.toMinutes(1)
                , TimeUnit.MILLISECONDS.toSeconds(milLies) % TimeUnit.MINUTES.toSeconds(1));
    }

    void setResAfterReturn()
    {
        if(myMediaPlayer.isPlaying())
        {
            int pos=MyMediaPlayer.currentIndex;
            title.setText(musicList.get(pos).getTitle());
            playPauseBtn.setImageResource(R.drawable.pause_button);
        }
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

    public int matchSongInOriginalList(){
        int pos=0;
        for(int i=0;i<copyList.size();i++)
        {
            if(Objects.equals(copyList.get(i).getTitle(), (musicList.get(MyMediaPlayer.currentIndex)).getTitle()))
            {
                pos=i;
                break;
            }
        }
        return pos;
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
        MyMediaPlayer.currentIndex=matchSongInOriginalList();
        }catch (Exception ignored){}
    }

    void textColorChangeAnimation(){
        Integer colorForm=getResources().getColor(R.color.red);
        Integer colorTo=getResources().getColor(R.color.blue);
        ValueAnimator animator=ValueAnimator.ofObject(new ArgbEvaluator(),colorForm,colorTo);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(@NonNull ValueAnimator valueAnimator) {
                myName.setTextColor((int) valueAnimator.getAnimatedFraction());
            }
        });
        animator.start();
        myName.setSelected(true);
    }
}