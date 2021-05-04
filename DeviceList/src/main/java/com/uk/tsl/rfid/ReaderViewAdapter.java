package com.uk.tsl.rfid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.uk.tsl.rfid.asciiprotocol.device.ObservableReaderList;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.asciiprotocol.device.TransportType;
import com.uk.tsl.rfid.devicelist.R;

public class ReaderViewAdapter extends RecyclerView.Adapter<ReaderViewAdapter.ReaderViewHolder>
{
    private ObservableReaderList mReaders;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ReaderViewHolder extends RecyclerView.ViewHolder
    {
        // each data item is just a string in this case
        ImageView mImageView;
        TextView mNameTextView;
        TextView mDescriptionTextView;
        TextView mBtTextView;
        TextView mBleTextView;
        TextView mUsbTextView;

        ReaderViewHolder(View itemView) {
            super(itemView);

            mImageView = (ImageView) itemView.findViewById(R.id.imageView);

            mNameTextView = (TextView) itemView.findViewById(R.id.nameTextView);
            mDescriptionTextView = (TextView) itemView.findViewById(R.id.descriptionTextView);

            mBtTextView = (TextView) itemView.findViewById(R.id.btTextView);
            mBleTextView = (TextView) itemView.findViewById(R.id.bleTextView);
            mBleTextView.setVisibility(View.GONE);
            mUsbTextView = (TextView) itemView.findViewById(R.id.usbTextView);
            mContext = itemView.getContext();
        }

        private Context mContext;

        void bind(Reader reader, boolean isSelected)
        {
            Resources resources = mContext.getResources();
            boolean isUsbPresent = reader.hasTransportOfType(TransportType.USB);
            boolean isUsbActive = reader.hasConnectedTransportOfType(TransportType.USB);

            boolean isBtPresent = reader.hasTransportOfType(TransportType.BLUETOOTH);
            boolean isBtActive = reader.hasConnectedTransportOfType(TransportType.BLUETOOTH);
            boolean hasSerialNumber = reader.getSerialNumber() != null;

            int rImageId = ReaderMedia.listImageFor(reader);
            mImageView.setImageResource(rImageId);
            mNameTextView.setText(reader.getDisplayName());
            mNameTextView.setTypeface( mNameTextView.getTypeface(), hasSerialNumber ? Typeface.BOLD : Typeface.ITALIC );
            String infoLine = resources.getString(ReaderMedia.descriptionFor(reader));
            infoLine += (infoLine.length() != 0 ? "\n" : "") + reader.getDisplayInfoLine();
            infoLine += (infoLine.length() != 0 ? "\n" : "") + reader.getDisplayTransportLine();
            mDescriptionTextView.setText(infoLine);

            TextView usbTV = mUsbTextView;
            usbTV.setVisibility(isUsbPresent ? View.VISIBLE : View.INVISIBLE);
            usbTV.setTextColor(isUsbActive ? resources.getColor(R.color.dl_transport_on) : resources.getColor(R.color.dl_transport_off));

            TextView btTV = mBtTextView;
            btTV.setVisibility(isBtPresent ? View.VISIBLE : View.INVISIBLE);
            btTV.setTextColor(isBtActive ? resources.getColor(R.color.dl_transport_on) : resources.getColor(R.color.dl_transport_off));

            itemView.setBackgroundColor(isSelected ? resources.getColor(R.color.dl_row_selected) : resources.getColor(R.color.dl_row_normal));
        }
    }


    int getSelectedRowIndex()
    {
        return mSelectedRowIndex;
    }

    void setSelectedRowIndex(int selectedRowIndex)
    {
        if( selectedRowIndex != mSelectedRowIndex)
        {
            int oldRowIndex = mSelectedRowIndex;

            mSelectedRowIndex = selectedRowIndex;

            notifyItemChanged(oldRowIndex);
            notifyItemChanged(mSelectedRowIndex);
        }
    }

    private int mSelectedRowIndex = -1;


    // Provide a suitable constructor (depends on the kind of dataset)
    ReaderViewAdapter(ObservableReaderList readers) {
        mReaders = readers;
    }


    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public ReaderViewAdapter.ReaderViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                     int viewType)
    {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // create a new view
        View readerView = inflater.inflate(R.layout.reader_list_row, parent, false);

        return new ReaderViewHolder(readerView);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ReaderViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Reader reader = mReaders.list().get(position);
        holder.bind(reader, position == mSelectedRowIndex);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mReaders.list().size();
    }
}