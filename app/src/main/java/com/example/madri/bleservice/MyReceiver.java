package com.example.madri.bleservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        Intent startService  = new Intent(context, BluetoothService.class);
        context.startService (startService);

        throw new UnsupportedOperationException("Not yet implemented");

    }
}
