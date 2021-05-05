package com.example.tslrfidbluetooth_demoproject.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Message;

import com.example.tslrfidbluetooth_demoproject.Interfaces.TSLReaderAlertNotifier;

public class GenericHandler extends WeakHandler<TSLReaderAlertNotifier>
{
    public GenericHandler(TSLReaderAlertNotifier t)
    {
        super(t);
    }

    @Override
    public void handleMessage(Message msg, TSLReaderAlertNotifier receiver)
    {
        try {
            switch (msg.what) {
                case ModelBase.BUSY_STATE_CHANGED_NOTIFICATION:
                    //TODO: process change in model busy state
                    break;

                case ModelBase.MESSAGE_NOTIFICATION:
                    // Examine the message for prefix
                    String message = (String)msg.obj;
                    if( message.startsWith("ER:")) {
                      //RFID CODE  receiver.mResultTextView.setText( message.substring(3));
                        receiver.RFIDCodeMessageReceived(message.substring(3));
                    }
                    else if( message.startsWith("BC:")) {
                     // BARCODE   receiver.mBarcodeResultsArrayAdapter.add(message);
                        receiver.barCodeMessageReceived(message);

                    } else {
                      //UNKNOWN MESSAGGE  receiver.mResultsArrayAdapter.add(message);
                        receiver.unknownMessageReceived(message);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
        }

    }
};
