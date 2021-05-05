package com.example.tslrfidbluetooth_demoproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.tslrfidbluetooth_demoproject.Interfaces.TSLReaderAlertNotifier;
import com.example.tslrfidbluetooth_demoproject.Models.InventoryModel;
import com.example.tslrfidbluetooth_demoproject.Utils.GenericHandler;
import com.uk.tsl.rfid.DeviceListActivity;
import com.uk.tsl.rfid.asciiprotocol.AsciiCommander;
import com.uk.tsl.rfid.asciiprotocol.device.ConnectionState;
import com.uk.tsl.rfid.asciiprotocol.device.IAsciiTransport;
import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.asciiprotocol.responders.LoggerResponder;
import com.uk.tsl.utils.Observable;

import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_ACTION;
import static com.uk.tsl.rfid.DeviceListActivity.EXTRA_DEVICE_INDEX;

public class MainActivity extends AppCompatActivity implements TSLReaderAlertNotifier {

    // The Reader currently in use
    private Reader mReader = null;

    // All of the reader inventory tasks are handled by this class
    private InventoryModel mModel;

    // The handler for model messages
    private static GenericHandler mGenericModelHandler;

    private boolean mIsSelectingReader = false;

    Button btnCheckConnection = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();

    }


    //----------------------------------------------------------------------------------------------
    // Pause & Resume life cycle
    //----------------------------------------------------------------------------------------------

    @Override
    public synchronized void onPause() {
        super.onPause();

        mModel.setEnabled(false);

        // Unregister to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommanderMessageReceiver);

        // Disconnect from the reader to allow other Apps to use it
        // unless pausing when USB device attached or using the DeviceListActivity to select a Reader
        if( !mIsSelectingReader && !ReaderManager.sharedInstance().didCauseOnPause() && mReader != null )
        {
            mReader.disconnect();
        }

        ReaderManager.sharedInstance().onPause();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        mModel.setEnabled(true);

        // Register to receive notifications from the AsciiCommander
        LocalBroadcastManager.getInstance(this).registerReceiver(mCommanderMessageReceiver, new IntentFilter(AsciiCommander.STATE_CHANGED_NOTIFICATION));

        // Remember if the pause/resume was caused by ReaderManager - this will be cleared when ReaderManager.onResume() is called
        boolean readerManagerDidCauseOnPause = ReaderManager.sharedInstance().didCauseOnPause();

        // The ReaderManager needs to know about Activity lifecycle changes
        ReaderManager.sharedInstance().onResume();

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList();

        // Locate a Reader to use when necessary
        AutoSelectReader(!readerManagerDidCauseOnPause);

        mIsSelectingReader = false;

        displayReaderState();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DeviceListActivity.SELECT_DEVICE_REQUEST:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    int readerIndex = data.getExtras().getInt(EXTRA_DEVICE_INDEX);
                    Reader chosenReader = ReaderManager.sharedInstance().getReaderList().list().get(readerIndex);

                    int action = data.getExtras().getInt(EXTRA_DEVICE_ACTION);

                    // If already connected to a different reader then disconnect it
                    if (mReader != null) {
                        if (action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_DISCONNECT) {
                            Log.e("Display Name","Disconnecting from: " + mReader.getDisplayName() + "\n");
                            mReader.disconnect();
                            if (action == DeviceListActivity.DEVICE_DISCONNECT) {
                                mReader = null;
                            }
                        }
                    }

                    // Use the Reader found
                    if (action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_CONNECT) {
                        mReader = chosenReader;
                        getCommander().setReader(mReader);
                    }
                }
                displayReaderState();
                break;
        }
    }

    // Helper Methods:

    void initialize(){
        mGenericModelHandler = new GenericHandler(this);
// Ensure the shared instance of AsciiCommander exists
        AsciiCommander.createSharedInstance(getApplicationContext());

        AsciiCommander commander = getCommander();

        // Ensure that all existing responders are removed
        commander.clearResponders();

        // Add the LoggerResponder - this simply echoes all lines received from the reader to the log
        // and passes the line onto the next responder
        // This is added first so that no other responder can consume received lines before they are logged.
        commander.addResponder(new LoggerResponder());

        // Add a synchronous responder to handle synchronous commands
        commander.addSynchronousResponder();

        // Create the single shared instance for this ApplicationContext
        ReaderManager.create(getApplicationContext());
        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);

        ReaderManager.sharedInstance().updateList();
        // Add observers for changes
  /*      ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);
*/        //Create a (custom) model and configure its commander and handler
        mModel = new InventoryModel();
        mModel.setCommander(getCommander());
        mModel.setHandler(mGenericModelHandler);
        setupUI();
    }

    void setupUI(){
        btnCheckConnection = findViewById(R.id.btn_check_connection);

        btnCheckConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ReaderManager.sharedInstance().getReaderList().list().size() > 0) {
                    mReader = ReaderManager.sharedInstance().getReaderList().list().get(0);
                    if (mReader != null) {
                        if (getCommander().getReader() == null) {
                            getCommander().setReader(mReader);
                        }
                        mReader.connect();
                    }
                    displayReaderState();
                }else{
                    openDeviceListActivity();
                }
            }
        });
    }
    void openDeviceListActivity(){
        int index = -1;
        if( mReader != null ) {
            // Determine the index of the current Reader
            index = ReaderManager.sharedInstance().getReaderList().list().indexOf(mReader);
        }
        Intent selectIntent = new Intent(this, DeviceListActivity.class);
        if( index >= 0 ) {
            selectIntent.putExtra(EXTRA_DEVICE_INDEX, index);
        }
        startActivityForResult(selectIntent, DeviceListActivity.SELECT_DEVICE_REQUEST);
    }

    //----------------------------------------------------------------------------------------------
    // AsciiCommander message handling
    //----------------------------------------------------------------------------------------------

    /**
     * @return the current AsciiCommander
     */
    protected AsciiCommander getCommander()
    {
        return AsciiCommander.sharedInstance();
    }

    //
    // Handle the messages broadcast from the AsciiCommander
    //
    private BroadcastReceiver mCommanderMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          //  if (D) { Log.d(getClass().getName(), "AsciiCommander state changed - isConnected: " + getCommander().isConnected()); }

            String connectionStateMsg = intent.getStringExtra(AsciiCommander.REASON_KEY);

            displayReaderState();
            if( getCommander().isConnected() )
            {
                // Update for any change in power limits
                //setPowerBarLimits();
                // This may have changed the current power level setting if the new range is smaller than the old range
                // so update the model's inventory command for the new power value
               // mModel.getCommand().setOutputPower(mPowerLevel);

                mModel.resetDevice();
                mModel.updateConfiguration();
            }

            displayReaderState();
            //UpdateUI();

        }
    };

    private void displayReaderState() {

        String connectionMsg = "Reader: ";
        switch( getCommander().getConnectionState())
        {
            case CONNECTED:
                connectionMsg += getCommander().getConnectedDeviceName();
                break;
            case CONNECTING:
                connectionMsg += "Connecting...";
                break;
            default:
                connectionMsg += "Disconnected";
        }
        Log.e("Talha:", "Reader "+ connectionMsg);
        setTitle(connectionMsg);
    }
    //----------------------------------------------------------------------------------------------
    // TSLReader message handling
    //----------------------------------------------------------------------------------------------

    @Override
    public void barCodeMessageReceived(String Message) {
        Log.e("Talha1", ""+ Message);
    }

    @Override
    public void RFIDCodeMessageReceived(String Message) {
        Log.e("Talha2", ""+ Message);
    }

    @Override
    public void unknownMessageReceived(String Message) {
        Log.e("Talha3", ""+ Message);
    }

    //----------------------------------------------------------------------------------------------
    // ReaderList Observers
    //----------------------------------------------------------------------------------------------
    Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // See if this newly added Reader should be used
            AutoSelectReader(true);
        }
    };

    Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // Was the current Reader disconnected i.e. the connected transport went away or disconnected
            if( reader == mReader && !reader.isConnected() )
            {
                // No longer using this reader
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
            else
            {
                // See if this updated Reader should be used
                // e.g. the Reader's USB transport connected
                AutoSelectReader(true);
            }
        }
    };

    Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            // Was the current Reader removed
            if( reader == mReader)
            {
                mReader = null;

                // Stop using the old Reader
                getCommander().setReader(mReader);
            }
        }
    };


    private void AutoSelectReader(boolean attemptReconnect)
    {
        ObservableReaderList readerList = ReaderManager.sharedInstance().getReaderList();
        Reader usbReader = null;
        if( readerList.list().size() >= 1)
        {
            // Currently only support a single USB connected device so we can safely take the
            // first CONNECTED reader if there is one
            for (Reader reader : readerList.list())
            {
                if (reader.hasTransportOfType(TransportType.USB))
                {
                    usbReader = reader;
                    break;
                }
            }
        }

        if( mReader == null )
        {
            if( usbReader != null)
            {
                // Use the Reader found, if any
                mReader = usbReader;
                getCommander().setReader(mReader);
            }
        }
        else
        {
            // If already connected to a Reader by anything other than USB then
            // switch to the USB Reader
            IAsciiTransport activeTransport = mReader.getActiveTransport();
            if ( activeTransport != null && activeTransport.type() != TransportType.USB && usbReader != null)
            {
                mReader.disconnect();

                mReader = usbReader;

                // Use the Reader found, if any
                getCommander().setReader(mReader);
            }
        }

        // Reconnect to the chosen Reader
        if( mReader != null
                && !mReader.isConnecting()
                && (mReader.getActiveTransport()== null || mReader.getActiveTransport().connectionStatus().value() == ConnectionState.DISCONNECTED))
        {
            // Attempt to reconnect on the last used transport unless the ReaderManager is cause of OnPause (USB device connecting)
            if( attemptReconnect )
            {
                if( mReader.allowMultipleTransports() || mReader.getLastTransportType() == null )
                {
                    // Reader allows multiple transports or has not yet been connected so connect to it over any available transport
                    mReader.connect();
                }
                else
                {
                    // Reader supports only a single active transport so connect to it over the transport that was last in use
                    mReader.connect(mReader.getLastTransportType());
                }
            }
        }
    }

}