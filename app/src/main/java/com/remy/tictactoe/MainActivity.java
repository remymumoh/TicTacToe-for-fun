package com.remy.tictactoe;

import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sp;
    boolean player1;
    int num_pressed;
    boolean isForeground = true;
    int[] cells_values = new int[15];
    List<ImageView> cells = new ArrayList<ImageView>();
    AnimatorSet anim;
    Handler h = new Handler();
    boolean single_game;
    boolean comp_first;
    SoundPool sndpool;
    int snd_click;
    int snd_win;
    int screen_width;
    int screen_height;
    MediaPlayer mp = new MediaPlayer();
    int num_games = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        // AdMob smart banner

        // bg sound
        try {
            mp = new MediaPlayer();
            AssetFileDescriptor descriptor = getAssets().openFd("snd_bg.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setVolume(0, 0);
            mp.setLooping(true);
            mp.prepare();
            mp.start();
        } catch (Exception e) {
        }

        // if mute
        if (sp.getBoolean("mute", false))
            ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
        else
            mp.setVolume(0.5f, 0.5f);

        // SoundPool
        sndpool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_click = sndpool.load(getAssets().openFd("snd_click.mp3"), 1);
            snd_win = sndpool.load(getAssets().openFd("snd_win.mp3"), 1);
        } catch (IOException e) {
        }

        // hide navigation bar listener
        findViewById(R.id.all).setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                hide_navigation_bar();
            }
        });

        // add cells
        for (int i = 0; i < 9; i++) {
            ImageView cell = new ImageView(this);
            cell.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            cell.setTag(i);
            cell.setClickable(true);
            ((ViewGroup) findViewById(R.id.frame)).addView(cell);
            cells.add(cell);

            // touch listener
            cell.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // cell click
                    if (event.getAction() == MotionEvent.ACTION_DOWN && v.isEnabled()
                            && ((single_game && !player1) || !single_game))
                        CELL_CLICK(v);
                    return false;
                }
            });
        }

        // custom font
        Typeface font = Typeface.createFromAsset(getAssets(), "CooperBlack.otf");
        ((TextView) findViewById(R.id.mess)).setTypeface(font);

        SCALE();
    }
    // SCALE
    void SCALE() {
        // text
        ((TextView) findViewById(R.id.mess)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.txt_score)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));

        // buttons text
        ((TextView) findViewById(R.id.btn_clear)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_sound)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_start1)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.btn_start2)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(26));
        ((TextView) findViewById(R.id.btn_exit)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(40));
    }
    // START
    void START() {
        player1 = true;
        num_pressed = 0;
        findViewById(R.id.game).setVisibility(View.VISIBLE);
        findViewById(R.id.main).setVisibility(View.GONE);
        findViewById(R.id.mess).setVisibility(View.GONE);

        // screen size
        screen_width = findViewById(R.id.all).getWidth();
        screen_height = findViewById(R.id.all).getHeight();

        // cell size
        int cell_size = (int) ((screen_width - DpToPx(20)) / 3);

        // frame size and position
        findViewById(R.id.frame).getLayoutParams().width = cell_size * 3;
        findViewById(R.id.frame).getLayoutParams().height = cell_size * 3;

        // cells at start
        int x_pos = 0;
        int y_pos = 0;
        for (int i = 0; i < cells.size(); i++) {
            cells_values[i] = 0;
            cells.get(i).setEnabled(true);
            cells.get(i).setImageResource(0);
            cells.get(i).getLayoutParams().width = cell_size;
            cells.get(i).getLayoutParams().height = cell_size;
            cells.get(i).setX(x_pos * cell_size);
            cells.get(i).setY(y_pos * cell_size);
            cells.get(i).setScaleX(1);
            cells.get(i).setScaleY(1);

            x_pos++;
            if (x_pos == 3) {
                x_pos = 0;
                y_pos++;
            }
        }
        // computer go
        if (single_game)
            h.postDelayed(computer_go, 500);
    }
    // onClick
    public void onClick(View v) {
        SharedPreferences.Editor ed = sp.edit();

        switch (v.getId()) {
            case R.id.btn_start1:
                // single player game
                single_game = true;
                comp_first = true;
                START();
                break;
            case R.id.btn_start2:
                // multiple player game
                single_game = false;
                START();
                break;
            case R.id.btn_clear:
                // clear scores
                ed.remove("player1");
                ed.remove("player2");
                ed.commit();
                show_main.run();
                break;
            case R.id.btn_exit:
                finish();
                break;
            case R.id.btn_sound:
                // sound
                if (sp.getBoolean("mute", false)) {
                    ed.putBoolean("mute", false);
                    mp.setVolume(0.5f, 0.5f);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_mute));
                } else {
                    ed.putBoolean("mute", true);
                    mp.setVolume(0, 0);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
                }
                ed.commit();
                break;
        }
    }


}
}
