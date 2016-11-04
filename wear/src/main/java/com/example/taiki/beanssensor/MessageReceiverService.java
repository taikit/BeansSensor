//package com.example.taiki.beanssensor;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.util.Log;
//import com.google.android.gms.wearable.*;
//
//public class MessageReceiverService extends WearableListenerService {
//    private static final String TAG = "BeansSensorboard/MessageReceiverService";
//
//    private DeviceClient deviceClient;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//
//        deviceClient = DeviceClient.getInstance(this);
//    }
//
//    @Override
//    public void onDataChanged(DataEventBuffer dataEvents) {
//        super.onDataChanged(dataEvents);
//
//        for (DataEvent dataEvent : dataEvents) {
//            if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
//                DataItem dataItem = dataEvent.getDataItem();
//                Uri uri = dataItem.getUri();
//                String path = uri.getPath();
//
//                if (path.startsWith("/filter")) {
//                    DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
//                    int filterById = dataMap.getInt(DataMapKeys.FILTER);
//                    deviceClient.setSensorFilter(filterById);
//                }
//            }
//        }
//    }
//
//    @Override
//    public void onMessageReceived(MessageEvent messageEvent) {
//        Log.d("BeansSensor", "Received message: " + messageEvent.getPath());
//
//        if (messageEvent.getPath().equals(ClientPaths.START_MEASUREMENT)) {
//            startService(new Intent(this, SensorService.class));
//        }
//
//        if (messageEvent.getPath().equals(ClientPaths.STOP_MEASUREMENT)) {
//            stopService(new Intent(this, SensorService.class));
//        }
//    }
//}
