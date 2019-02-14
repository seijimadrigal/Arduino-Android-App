package com.example.madri.bleservice;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.MacAddress;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
public class BluetoothService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    public BluetoothService() {
    }
    public static final String TAG = "SERVICE";
    public boolean commandDelay;
    public boolean startUp = false;
    public boolean reSend = false;
    private boolean canSend;
    String inputString = " ";
    //Bluetooth   VARIABLES
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID DECRIPTOR_UUID = UUID.fromString("ab0bafd2-9115-4c74-9a97-8eda4b7ba073");
   private static final String BLUETOOTH_MAC_ADDRESS  = "98:7B:F3:51:E8:F6";
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothDevice mBluetoothDevice;
    public BluetoothManager manager;
    public static BluetoothGatt gatt;
    public static boolean connected = false;
    public BluetoothGattCharacteristic characteristic;
    public int sendData;
    public int sendno;
    //Spotify VARIABLES
    private static final String CLIENT_ID = "3120573c38c548f4afb2771147c19dfe";
    private static final String REDIRECT_URI = "http://localhost:3000/auth/google/callback";
    public boolean Paused;
    private SpotifyAppRemote mSpotifyAppRemote;
    boolean spotifyIsConnected;
    public String AppName = "";
    public String TRACK_NAME;
    public String TRACK_ARTIST;
    public String previousTrack = "";
    public String previousArtist = "";
    private AudioManager audioManager;
    public static String titleSong = "";
    public static ArrayList list;
    public int tracklength;
    public int artistlength;
    // NOTIFICATIONS VARIABLES
    public int notifTitlelength;
    public int notifBodylength;
    public String notificationTitle = "";
    public String notificationBody= "";
    public String prevnotificationTitle = "";
    public String prevnotificationBody= "";

    @Override
    public void onCreate() {
        super.onCreate();
        gatt = null;
        Log.i(TAG, "onCreate: SERVICE STARTED");
        manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(BLUETOOTH_MAC_ADDRESS);
        gatt = mBluetoothDevice.connectGatt(BluetoothService.this, false, GattCallback);
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationBroadcast, new IntentFilter("Msg"));
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        canSend = true;
        commandDelay = false;
        sendData = 0;
        sendno = 1;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

       // FOR ENABLING SERVICE IN LOCK MODE
        PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyWakeLock");
        wakeLock.acquire(10000);
        Log.i(TAG, "WAKELOCK ACQUIRED" + wakeLock.toString());

        //ESTABLISHES A CONNECTION WITH SPOTIFY APP //
        if (mSpotifyAppRemote == null) {

            ConnectionParams connectionParams =
                    new ConnectionParams.Builder(CLIENT_ID)
                            .setRedirectUri(REDIRECT_URI)
                            .showAuthView(true)
                            .build();

            SpotifyAppRemote.CONNECTOR.connect(this, connectionParams,
                    new Connector.ConnectionListener() {

                        @Override
                        public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                            mSpotifyAppRemote = spotifyAppRemote;
                            spotifyIsConnected = true;
                            Log.d(TAG, "Connected to SPOTIFY");

                            mSpotifyAppRemote.getPlayerApi()
                                    .subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {



                                public void onEvent(PlayerState playerState) {
                                    final Track track = playerState.track;
                                    Paused = playerState.isPaused;

                                        TRACK_NAME = track.name;
                                        tracklength = TRACK_NAME.length();
                                        TRACK_ARTIST = track.artist.name;
                                        artistlength = TRACK_ARTIST.length();
                                        titleSong = TRACK_NAME + " by " + TRACK_ARTIST;
                                        Log.d(TAG, track.name + "  -  " + track.artist.name);

                                }
                            });

                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Log.e(TAG, throwable.getMessage(), throwable);

                        }
                    });

        }

        //STARTS THE LOOP FOR THE BLE SERVICE

        Thread thread = new Thread(new serviceThread(startId));
        thread.start();
        Log.i(TAG, "onStartCommand: THREAD " + startId);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //DISCONNECTS TO SPOTIFY & BLUETOOTH

        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);
       // gatt.disconnect();
        gatt.close();
        gatt = null;
        Log.i(TAG, "onDestroy: Service STOPPED");
        sendData = 0;
        startUp = false;
        Intent intent = new Intent("com.example.madri");
        sendBroadcast(intent);

    }

    private final BluetoothGattCallback GattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt Gatt, int status, int newState) {
            super.onConnectionStateChange(Gatt, status, newState);

            if (BluetoothProfile.STATE_CONNECTED == newState) {
                Log.i(TAG, "onConnectionStateChange: ESTABLISHED CONNECTION WITH " + mBluetoothDevice.getName());
                Gatt.discoverServices();
                sendno = 3;
                connected = true;


            } else if (BluetoothProfile.STATE_DISCONNECTED == newState) {
                Log.i(TAG, "onConnectionStateChange: DISCONNECTED FROM " + mBluetoothDevice.getName());
                connected = false;


            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.i(TAG, "onServicesDiscovered: SERVICES FOUND");

            characteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID);

            gatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DECRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            gatt.writeDescriptor(descriptor);

        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                if (reSend) {

                    canSend = true;
                }
                if (reSend == true) {

                    canSend = false;
                }
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {


                canSend = true;

            }
        }
    };

              //  GETTING NOTIFICATIONS
    private BroadcastReceiver notificationBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //GETS FROM NOTIFICATION LISTENER ALL THE STRING VALUES

            String packageName = intent.getStringExtra("package");
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            if(packageName.startsWith("com.facebook")){
                AppName = "Facebook";
            }
            if(!packageName.startsWith("com.facebook")){
                AppName = packageName;
            }
            if(title != null){
                if(!title.startsWith("Messenger") && packageName.startsWith("com.facebook") &&!title.startsWith("Chat")) {
                    //titleView.setText( AppName + " " +title + " " + text);
                    notificationBody = text;
                    notificationTitle = title;
                    notifTitlelength = notificationTitle.length();
                    notifBodylength = notificationBody.length();
                    sendno = 6;}
                }
            }
    };
// FUNCTION TO WRITE THE STRING PASSED ON THE BLUETOOTH CHARACTERISTIC
    public void writeCharacterstic(String data) {
     if(gatt != null && characteristic!= null && data != null) {
         characteristic.setValue(data);
         gatt.writeCharacteristic(characteristic);
     }
    }

    //   TIME FUNCTION THAT CONVERTS TIME ON ANDROID PHONE TO STRING
    public String getCurrentTime() {
        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm");
        String time = timeFormat.format(currentTime);
        return time;
    }

    class serviceThread extends Thread {
        int service_id;
        serviceThread(int service_id) {
            this.service_id = service_id;
        }

        @Override
        public void run() {

            while (true) {


            // CHECK IF IT SONG & ARTIST ARE THE SAME

                if(previousArtist.equals(TRACK_ARTIST)&& previousTrack.equals(TRACK_NAME)){

                    sendno = 3;

                }
                // THIS IS THE CODE FOR THE BUTTONS PREV/PLAY/NEXT //

                if (MainActivity.buttonNext) {
                    mSpotifyAppRemote.getPlayerApi().skipNext();
                    MainActivity.buttonNext = false;
                }

                if (MainActivity.buttonPlay) {
                    if(Paused = true){
                    mSpotifyAppRemote.getPlayerApi().resume();
                    MainActivity.buttonPlay = false;}

                    else if (Paused=false){
                        mSpotifyAppRemote.getPlayerApi().pause();
                        MainActivity.buttonPlay = false;}
                }
                if (MainActivity.buttonPrevious) {
                    mSpotifyAppRemote.getPlayerApi().skipPrevious();
                    mSpotifyAppRemote.getPlayerApi().skipPrevious();
                    MainActivity.buttonPrevious = false;
                }
                // Log.i(TAG, "RUNNING" + service_id);
                if (connected) {

                    if (canSend) {

                        try {
                        serviceThread.sleep(500);
                       }
                      catch (InterruptedException e) {
                          e.printStackTrace();
                          Log.i(TAG, "DATA ERROR: NOT SENT: "); }




                        switch (sendno){
                            case 1 :
                                if(TRACK_NAME!= null) {

                                    if(tracklength < 19) {
                                        writeCharacterstic("[" + TRACK_NAME + "]");
                                        Log.i(TAG, "Data Sent: " + TRACK_NAME);
                                        sendno = 2;




                                    }
                                    if(tracklength >= 19 && tracklength <= 27) {
                                        reSend = true;
                                        writeCharacterstic("[" + TRACK_NAME.substring(0,tracklength/2));

                                        sendno = 4;

                                    }
                                    if(tracklength > 27){
                                        reSend = true;
                                        writeCharacterstic("[" + TRACK_NAME.substring(0,tracklength/3));
                                        sendno = 5;
                                    }

                                }
                                break;
                            case 2:
                                if(TRACK_ARTIST!= null) {

                                    if(artistlength <= 19){

                                        writeCharacterstic("<" + TRACK_ARTIST + ">");

                                        Log.i(TAG, "Data Sent: " + TRACK_ARTIST);
                                        sendno = 6;
                                    }

                                    if(artistlength > 19){
                                        writeCharacterstic("<" + TRACK_ARTIST.substring(0,artistlength/2));
                                        reSend = true;
                                        sendno = 8;
                                        Log.i(TAG, "SENDING ARTIST...");
                                    }


                                }
                                break;
                            case 3:
                                if(getCurrentTime()!= null) {
                                    if (!previousTrack.equals(TRACK_NAME) || !previousArtist.equals(TRACK_ARTIST)) {
                                        sendno = 1;
                                    }
                                    if (!prevnotificationBody.equals(notificationBody)) {
                                        sendno = 6;
                                    }
                                    writeCharacterstic("{" + getCurrentTime() + "}");

                                }
                                break;
                            case 4:

                                reSend = false;
                                writeCharacterstic(TRACK_NAME.substring(tracklength/2,tracklength) + "]");
                                sendno = 2;
                                break;
                            case 5:
                                reSend = false;
                                writeCharacterstic(TRACK_NAME.substring(tracklength/3,((tracklength/3)*2)) + "]");
                                sendno = 2;
                                break;

                            case 6:
                                writeCharacterstic("%" +notificationTitle + "  ");
                                Log.i(TAG, "Notification Sent: " + notificationTitle);
                                reSend = true;
                                sendno = 7;
                                break;
                            case 7:
                                writeCharacterstic(notificationBody + "@");
                                Log.i(TAG, "Notification Sent 1: "  + notificationBody);
                                prevnotificationBody = notificationBody;
                                prevnotificationTitle = notificationTitle;
                                reSend = false;
                                sendno = 3;
                                break;

                            case 8:
                                reSend = false;
                                writeCharacterstic(TRACK_NAME.substring(tracklength/2,tracklength)+">");
                                sendno = 6;
                                break;
                      }
                        commandDelay = false;
                }
                    if (!canSend) {

                        try {
                                serviceThread.sleep(800);
                        } catch (InterruptedException e) {
                            e.printStackTrace();

                            }
                      if(gatt!= null) {
                          gatt.readCharacteristic(characteristic);
                          byte[] c = characteristic.getValue();
                          if (c != null) {
                              //inputString = String.valueOf(c);
                              inputString = new String(c, StandardCharsets.UTF_8);
                              Log.i(TAG, "READ: " + inputString);
                          }
                      }
                    }
                    if (inputString != null ) {
                        if (inputString.indexOf('ù') >= 0 && commandDelay == false) {
                            inputString = "";
                            audioManager.adjustVolume(+1, AudioManager.FLAG_VIBRATE);
                            commandDelay = true;
                            Log.i(TAG, "Volume Up");

                            // †
                        }
                        if (inputString.indexOf('ú') >= 0 && commandDelay == false) {
                            inputString = "";
                            commandDelay = true;
                            audioManager.adjustVolume(-1, AudioManager.FLAG_VIBRATE);
                            Log.i(TAG, "Volume Down");
                        }

                        if (inputString.indexOf('*') >= 0 && commandDelay == false) {
                           commandDelay = true;
                            Log.i(TAG, "Pause");
                            mSpotifyAppRemote.getPlayerApi().pause();
                            inputString = "";
                        }
                        if (inputString.indexOf('µ') >= 0 && commandDelay == false) {
                            Log.i(TAG, "Play");
                            commandDelay = true;
                            mSpotifyAppRemote.getPlayerApi().resume();
                            inputString = "";
                        }
                        if (inputString.indexOf('¶') >= 0 && commandDelay == false) {
                            Log.i(TAG, "Next");
                            commandDelay = true;
                            mSpotifyAppRemote.getPlayerApi().skipNext();
                            inputString = "";
                        }
                        if (inputString.indexOf('ш') >= 0 && commandDelay == false) {
                            Log.i(TAG, "Previous");
                            commandDelay = true;
                            mSpotifyAppRemote.getPlayerApi().skipPrevious();
                            inputString = "";
                        }
                        if (inputString.indexOf('^') >= 0) {
                            Log.i(TAG, "Ping");

                            previousTrack = TRACK_NAME;
                            previousArtist = TRACK_ARTIST;
                            inputString = "";
                        }
                    }
                }
            if (!connected ) {
              if(gatt != null) {
                  gatt.close();
                  gatt = null;
              }
                try {

                    serviceThread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i(TAG, "DATA ERROR:ENABLE TO RECONNECT");
                }

                Log.i(TAG, "TRYING TO RECONNCECT TO " + mBluetoothDevice);
                gatt = mBluetoothDevice.connectGatt(BluetoothService.this, false, GattCallback);
                try {

                    serviceThread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i(TAG, "DATA ERROR:ENABLE TO RECONNECT");
                }
                }
                }
            }
        }

    }