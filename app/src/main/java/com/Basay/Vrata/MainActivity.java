package com.Basay.Vrata;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


interface StukOtvet {  // интерфейс для передачи ответа от потока запроса обратно в Активити
    void KtoTam(String OtvetArduino);
}

class Stuk extends AsyncTask<String,Void,String>{
    public StukOtvet delegate = null;
    //@SuppressLint("StaticFieldLeak")
    private String myURL;
    Stuk(String url_){
        myURL=url_;
    }
    public static String doGet(String url)
            throws Exception {

        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        //add reuqest header
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0" );
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Content-Type", "application/json");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = bufferedReader.readLine()) != null) {
            response.append(inputLine);
        }
        bufferedReader.close();

//      print result
        Log.i(    "try_send_http","Response string: " + response.toString());
        return response.toString();
    }
    @Override
    protected String doInBackground(String... strings) {
       // String s = Con.getString(R.string.LocalURL);
        //s=s;
        try {
            //private static AsyncTask Potok;
            //String myURL = ;
            strings[0] = doGet(myURL + strings[0] );
        } catch (Exception e) {
            strings[0]="Ошибка запроса. Что-то с соединением";
            e.printStackTrace();
        }
        return strings[0];
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        delegate.KtoTam(s);
        //TextView hw= findViewById(R.id.twHW);
        //HW.setText(s);

    }
}


//===========================================================================================         ACTIVITY =============================================
public class MainActivity extends AppCompatActivity implements StukOtvet{
    Stuk Zapros;//=new Stuk();
    String CurURL;
    String CurCommand;
    Integer CurDim=0;
    private TextView HW,tw_Opened;
    private SwitchMaterial sw_WF;
    private String On, Off, Open;
    private ImageView LampON,LampOFF;
    private static AntiUte4nyiHandler AntiDrebezgH;
    private volatile boolean AntiDrZanyato;
    private volatile Integer AntiDrLastDim=0;










    //==========================================================================================
    static class AntiUte4nyiHandler extends Handler {
        /*слабая ссылка нужна, чтоб сделать хэндлер сатическим(чтоб не утекала память), но чтоб
        * он всё же имел доступ к активити. Если повернуть экран - активити удалится, и хэндлер
        * не будет держать её за яйца, потому что сслыка слабая. Всё похацкается как надо*/
        WeakReference<MainActivity> wrActivity;

        public AntiUte4nyiHandler(MainActivity activity) {
            wrActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            MainActivity activity = wrActivity.get();
            /* яркость устанавливается либо посланная пол секунды назад, либо последняя, которая
            * хотела установиться, когда уже было занято*/
            if (activity != null){
                if(activity.AntiDrLastDim==-1)activity.AntiDrLastDim=msg.what;
                activity.Posyl(activity.AntiDrLastDim);
                activity.AntiDrZanyato=false;
            }
            //activity.someMethod();
        }
    }
    //==========================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        CurURL = getString(R.string.WebURL);
        On = getString(R.string.LightON);
        Off = getString(R.string.LightOFF);
        Open = getString(R.string.Open);

        LampON = findViewById(R.id.iv_LampON);
        LampON.setImageAlpha(0);
        LampON.setImageResource(R.drawable.lampon);
        LampON.setVisibility(View.VISIBLE);
        LampOFF = findViewById(R.id.iv_LampOff);
        LampOFF.setVisibility(View.VISIBLE);
        HW = findViewById(R.id.tw_HW);
        tw_Opened = findViewById(R.id.tw_Opened);
        tw_Opened.setVisibility(View.INVISIBLE);
        SeekBar sb_Br = findViewById(R.id.sb_Brightness);
        Button btnOFF = findViewById(R.id.btn_OFF);
        btnOFF.setLongClickable(true);
        sw_WF = findViewById(R.id.sw_WF);
        sw_WF.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    CurURL = getString(R.string.LocalURL);
                else
                    CurURL = getString(R.string.WebURL);
            }
        });


        btnOFF.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                findViewById(R.id.btn_OPEN).setVisibility(View.VISIBLE);
                return false;
            }
        });

        //sw_WF.setChecked(false);


        AntiDrebezgH = new AntiUte4nyiHandler(this);
        AntiDrZanyato = false;
        sb_Br.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /* отправка хэндлу сообщения о текущей яркости, которое он выполнит позже.
                 * Если занято - сохранение самой последней яркости. Она применится, когда хэндл отдуплится */
                if (!AntiDrZanyato) {
                    AntiDrZanyato = true;
                    AntiDrLastDim = -1;
                    AntiDrebezgH.sendEmptyMessageDelayed(progress, 500);
                } else {
                    AntiDrLastDim = progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {


                //HW.setText("StartTracking");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Posyl(seekBar.getProgress());
                // ((TextView)findViewById(R.id.tw_HW3)).setText("StopTracking");
                // HW.setText("dsadas" + seekBar.getProgress());
            }
        });



    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            sw_WF.setChecked(true);//CurURL = getString(R.string.LocalURL);
            //HW.setText("LAND");
        } else {
            sw_WF.setChecked(false);//CurURL = getString(R.string.WebURL);
            //HW.setText("PORT");
        }
        //HW.setText(HW.getText() + " " + sw_WF.isChecked());
        //sw_WF.refreshDrawableState();

    }

    @Override
    protected void onStart() {
        super.onStart();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(AntiDrebezgH!=null) // очистка хэндлера от сообщений, чтоб система могла его спокойно похацкать при повороте
            AntiDrebezgH.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    private void  Posyl (Integer Dim ){
        Posyl("Bright",Dim);
        CurDim=Dim;
    }
    private void  Posyl (String Command ){
        Posyl(Command,-1);
    }
    private void  Posyl (String Command, Integer Dim){
        findViewById(R.id.pb_Sending).setVisibility(View.VISIBLE);
        CurCommand = Command;
        Zapros=new Stuk(CurURL);
        Zapros.delegate=this;
        if (Dim !=-1)
            Zapros.execute(getString(R.string.LightBright)+Dim);
         else
            Zapros.execute(Command);
    }
    @SuppressLint("NewApi") public static void setAlpha(View view, float alpha){
        if (Build.VERSION.SDK_INT < 11) {
            final AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
            animation.setDuration(0);
            animation.setFillAfter(true);
            view.startAnimation(animation);
        }
        else view.setAlpha(alpha);
    }



    public void onClick_btnON(View view) {
        Posyl(On);

    }

    public void onClick_btnOFF(View view) {
        Posyl(Off);
        //LampON.setImageAlpha(0);
    }

    public void onClick_twHW(View view) {
    }

    @Override
    public void KtoTam(String OtvetArduino) {
        findViewById(R.id.pb_Sending).setVisibility(View.INVISIBLE);
        String Rr;//="(?<={\"text\":\").*(?=\"})";
        Pattern RegEx= Pattern.compile("(?<=\\{\"text\":\").*(?=\"\\})");
        Matcher m= RegEx.matcher(OtvetArduino);
        LampON.setImageResource(R.drawable.lampon);
        LampON.setVisibility(View.VISIBLE);
        LampOFF.setVisibility(View.VISIBLE);
        tw_Opened.setVisibility(View.INVISIBLE);
        //Lamp.setImageAlpha(255);
        if (m.find( )) {
            OtvetArduino=m.group();
            if (CurCommand.equals(On) && OtvetArduino.equals("ON_by_WIFI"))
                LampON.setImageAlpha(255); else
                if (CurCommand.equals(Off) && OtvetArduino.equals("OFF"))
                    LampON.setImageAlpha(0); else
                    if (CurCommand.equals("Bright") && OtvetArduino.equals("ON_by_WIFI"))
                        LampON.setImageAlpha(CurDim); else
                        if (CurCommand.equals(Open) && OtvetArduino.equals("OPN_by_WIFI")){
                            tw_Opened.setVisibility(View.VISIBLE);
                            LampON.setVisibility(View.INVISIBLE);
                            LampOFF.setVisibility(View.INVISIBLE);
                            findViewById(R.id.btn_OPEN).setVisibility(View.GONE);
                            final Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    finish();
                                }
                            }, 2000);

                            // анимация открытия
                        }
        }
        else
        {
            //LampON.setImageDrawable(getResources().getDrawable(R.drawable.lamperror));
            LampON.setImageResource(R.drawable.lamperror);
            LampON.setImageAlpha(255);
        }

        //OtvetArduino=

        HW.setText(OtvetArduino);
        Zapros.cancel(true);
    }

    public void onClick_btnOPEN(View view) {
        Posyl(Open);
    }

    public void onClick_btnEXIT(View view) {


        finish();
    }
}