package com.uk.tsl.rfid;

import android.util.Log;

import com.uk.tsl.rfid.asciiprotocol.BuildConfig;
import com.uk.tsl.rfid.asciiprotocol.Constants;
import com.uk.tsl.rfid.asciiprotocol.device.Reader;
import com.uk.tsl.rfid.devicelist.R;
import com.uk.tsl.utils.StringHelper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReaderMedia
{
    /**
     * Get a list view image Id for the given Reader
     * @return
     */
    public static int listImageFor(Reader reader)
    {
        String model = modelFor(reader);


        if( model == null ) return R.drawable.list_unknown_device;
        if( model.contentEquals("1128")) return R.drawable.list_1128_s1;
        if( model.contentEquals("1153")) return R.drawable.list_1153;
        if( model.contentEquals("1166")) return R.drawable.list_1166;
        if( model.contentEquals("2128")) return R.drawable.list_2128_s1;
        if( model.contentEquals("IH21")) return R.drawable.list_ih21_a1;

        if( model.contentEquals("2128P")) return R.drawable.list_2128p;
        if( model.contentEquals("2128L")) return R.drawable.list_2128l;
        if( model.contentEquals("2166")) return R.drawable.list_2166;
        if( model.contentEquals("2173")) return R.drawable.list_2173;
        if( model.contentEquals("IH21L")) return R.drawable.list_ih21l;

        return R.drawable.list_unknown_device;
    }

    public static int descriptionFor(Reader reader)
    {
        String model = modelFor(reader);


        if( model == null ) return R.string.description_unknown_device;
        if( model.contentEquals("2128P")) return R.string.description_2128p;
        if( model.contentEquals("2128L")) return R.string.description_2128l;
        if( model.contentEquals("IH21L")) return R.string.description_ih21l;
        if( model.contentEquals("1128")) return R.string.description_1128;
        if( model.contentEquals("1153")) return R.string.description_1153;
        if( model.contentEquals("1119")) return R.string.description_1119;
        if( model.contentEquals("1126")) return R.string.description_1126;
        if( model.contentEquals("2166")) return R.string.description_2166;
        if( model.contentEquals("1166")) return R.string.description_1166;
        if( model.contentEquals("2128")) return R.string.description_2128;
        if( model.contentEquals("IH21")) return R.string.description_ih21;
        if( model.contentEquals("2173")) return R.string.description_2173;

        return R.string.description_unknown_device;
    }



    /**
     * Return the
     * @param reader
     * @return
     */
    private static String modelFor(Reader reader)
    {
        String serialNumber = reader.getSerialNumber();

        if(D) Log.d(TAG, String.format("SN: %s", serialNumber));

        String model = null;

        if( serialNumber == null )
        {
            // Use the display name
            String displayName = reader.getDisplayName();
            if(D) Log.d(TAG, String.format("DN: %s", displayName));

            for( String regex : sDisplayNamePatterns)
            {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(displayName);
                if( matcher.find() )
                {
                    model = matcher.group(MODEL_GROUP);
                    if (model != null) break;
                }
            }
        }
        else
        {
            // Try all
            for( String regex : sSerialNumberPatterns)
            {
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(serialNumber);
                if( matcher.find() )
                {
                    model = matcher.group(MODEL_GROUP);
                    if (model != null) break;
                }
            }
        }
        return model;
    }

    //
    // Warning
    //
    // Cannot use named capture groups in Android API < 26
    // so patterns must ALWAYS have the same group offsets
    // capture groups are:
    final static int MODEL_GROUP = 1;

    private static String[] sSerialNumberPatterns =
            {
                    //"(?<model>(\\d{2}|IH)\\d{2})-.+"
                    //"((\\d{2}|IH)\\d{2})-.+",
                    "((\\d{2}|IH)\\d{2}(L|P)*)-.+",

                    };

    private static String[] sDisplayNamePatterns =
            {
                    //".+-(?<model>(\\d{2}|IH)\\d{2})"
                    //".+-((\\d{2}|IH)\\d{2})",
                    ".+-((\\d{2}|IH)\\d{2}(L|P)*)",
                    };


    private static final String TAG = "ReaderMedia";
    private static final boolean D = BuildConfig.DEBUG;
}
