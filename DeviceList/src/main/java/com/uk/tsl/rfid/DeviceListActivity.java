
package com.uk.tsl.rfid;

import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.ReaderManager;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.devicelist.R;
import com.uk.tsl.utils.Observable;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 */
public class DeviceListActivity extends Activity
{
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;


    // Intent request codes
    public static final int SELECT_DEVICE_REQUEST = 0x5344;


    /// Return Intent extra
    public static String EXTRA_DEVICE_INDEX = "tsl_device_index";
    public static String EXTRA_DEVICE_ACTION = "tsl_device_action";

    /// Actions requested for the chosen device
    public static int DEVICE_CONNECT = 1;
    public static int DEVICE_CHANGE = 2;
    public static int DEVICE_DISCONNECT = 3;


    // Member fields
    private RecyclerView mRecyclerView;
    private ReaderViewAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ObservableReaderList mReaders;

    private Reader mSelectedReader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.reader_list);

        // Configure the ReaderManager when necessary
        ReaderManager.create(getApplicationContext());
        mRecyclerView = (RecyclerView) findViewById(R.id.reader_recycler_view);

        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(itemDecoration);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        //mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mReaders = ReaderManager.sharedInstance().getReaderList();
        mAdapter = new ReaderViewAdapter(mReaders);
        mRecyclerView.setAdapter( mAdapter );

        ItemClickSupport.addTo(mRecyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v)
            {
                int oldIndex = mAdapter.getSelectedRowIndex();
                mAdapter.setSelectedRowIndex(position);
                if( oldIndex == position )
                {
                    // Offer disconnect
                    offerDisconnect(mReaders.list().get(position), position);
                }
                else
                {
                    // Warn about disconnection of other reader
                    if( oldIndex >= 0 )
                    {
                        offerChange(mReaders.list().get(oldIndex), oldIndex, mReaders.list().get(position), position);
                    }
                    else
                    {
                        returnSelectedReader(position, DEVICE_CONNECT);
                    }
                }
            }
        });

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Add observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().addObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().addObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().addObserver(mRemovedObserver);


        // See if there is a reader currently in use
        Intent intent = getIntent();
        int startIndex = intent.getIntExtra(EXTRA_DEVICE_INDEX, -1);
        if( startIndex >= 0 )
        {
            mSelectedReader = ReaderManager.sharedInstance().getReaderList().list().get(startIndex);
            mRecyclerView.scrollToPosition(startIndex);
        }
    }

    void offerDisconnect(Reader reader,int index)
    {
        final int confirmedIndex = index;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("From:  " + reader.getDisplayName() )
               .setTitle("Disconnect?");

        builder.setPositiveButton("Disconnect", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                returnSelectedReader(confirmedIndex, DEVICE_DISCONNECT);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled the dialog
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    void offerChange(Reader oldReader, int oldIndex, Reader newReader, int newIndex)
    {
        final int restoreIndex = oldIndex;
        final int confirmedIndex = newIndex;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(String.format( "From:  %s\n\nTo:  %s", oldReader.getDisplayName(), newReader.getDisplayName() ) )
               .setTitle("Change Reader?");

        builder.setPositiveButton("Change", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                returnSelectedReader(confirmedIndex, DEVICE_CHANGE);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id)
            {
                // User cancelled - revert to previous
                mAdapter.setSelectedRowIndex(restoreIndex);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    void returnSelectedReader(int readerIndex, int action)
    {
        // Create the result Intent
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_INDEX, readerIndex);
        intent.putExtra(EXTRA_DEVICE_ACTION, action);

        // Set result and finish this Activity
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove observers for changes
        ReaderManager.sharedInstance().getReaderList().readerAddedEvent().removeObserver(mAddedObserver);
        ReaderManager.sharedInstance().getReaderList().readerUpdatedEvent().removeObserver(mUpdatedObserver);
        ReaderManager.sharedInstance().getReaderList().readerRemovedEvent().removeObserver(mRemovedObserver);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_add_new)
        {
            startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    @Override
    protected void onPause()
    {
        super.onPause();
        ReaderManager.sharedInstance().onPause();

    }

    @Override
    protected void onResume()
    {
    	super.onResume();
        ReaderManager.sharedInstance().onResume();

        // The Activity may start with a reader already connected (perhaps by another App)
        // Update the ReaderList which will add any unknown reader, firing events appropriately
        ReaderManager.sharedInstance().updateList();
        if(mAdapter!=null)
        {
            // Reapply the selected Reader in case the Reader list has been changed while paused
            mAdapter.setSelectedRowIndex(-1);
            mAdapter.notifyDataSetChanged();
            int readerIndex = ReaderManager.sharedInstance().getReaderList().list().indexOf(mSelectedReader);
            mAdapter.setSelectedRowIndex(readerIndex);
        }
    }

    Observable.Observer<Reader> mAddedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            if (D) { Log.d(TAG, "Reader arrived"); }
            int readerIndex = ReaderManager.sharedInstance().getReaderList().list().indexOf(reader);
            mAdapter.notifyItemInserted(readerIndex);

            // If the new Reader is connected over USB then this will be auto selected and
            if( reader.hasTransportOfType(TransportType.USB))
            {
                returnSelectedReader(readerIndex, mSelectedReader != null ? DEVICE_CHANGE : DEVICE_CONNECT);
            }
        }
    };

    Observable.Observer<Reader> mUpdatedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            if (D) { Log.d(TAG, "Reader updated"); }
            int readerIndex = ReaderManager.sharedInstance().getReaderList().list().indexOf(reader);
            // A Reader has changed - check to see if it is the currently selected Reader and no longer connected
            if( !reader.isConnected() && mAdapter.getSelectedRowIndex() == readerIndex)
            {
                mAdapter.setSelectedRowIndex(-1);
            }
            mAdapter.notifyItemChanged(readerIndex);
        }
    };

    Observable.Observer<Reader> mRemovedObserver = new Observable.Observer<Reader>()
    {
        @Override
        public void update(Observable<? extends Reader> observable, Reader reader)
        {
            if (D) { Log.d(TAG, "Reader Removed"); }
            int readerIndex = ReaderManager.sharedInstance().getReaderList().list().indexOf(reader);
            if(mAdapter.getSelectedRowIndex() == readerIndex)
            {
                mAdapter.setSelectedRowIndex(-1);
            }
            mAdapter.notifyItemRemoved(readerIndex);
        }
    };

}
