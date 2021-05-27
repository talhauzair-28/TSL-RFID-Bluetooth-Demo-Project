package com.example.tslrfidbluetooth_demoproject.Interfaces;

public interface TSLReaderAlertNotifier {

    void barCodeMessageReceived(String Message);

    void RFIDCodeMessageReceived(String Message);

    void unknownMessageReceived(String Message);
}
