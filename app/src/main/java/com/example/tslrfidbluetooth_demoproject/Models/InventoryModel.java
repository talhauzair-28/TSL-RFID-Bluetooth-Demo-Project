//----------------------------------------------------------------------------------------------
// Copyright (c) 2013 Technology Solutions UK Ltd. All rights reserved.
//----------------------------------------------------------------------------------------------

package com.example.tslrfidbluetooth_demoproject.Models;

import android.util.Log;

import com.example.tslrfidbluetooth_demoproject.Utils.ModelBase;
import com.uk.tsl.rfid.asciiprotocol.commands.BarcodeCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.FactoryDefaultsCommand;
import com.uk.tsl.rfid.asciiprotocol.commands.InventoryCommand;
import com.uk.tsl.rfid.asciiprotocol.enumerations.TriState;
import com.uk.tsl.rfid.asciiprotocol.responders.IBarcodeReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ICommandResponseLifecycleDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.ITransponderReceivedDelegate;
import com.uk.tsl.rfid.asciiprotocol.responders.TransponderData;
import com.uk.tsl.utils.HexEncoding;

import java.util.Locale;

public class InventoryModel extends ModelBase
{

	// Control 
	private boolean mAnyTagSeen;
	private boolean mEnabled;
	public boolean enabled() { return mEnabled; }

	public void setEnabled(boolean state)
	{
		boolean oldState = mEnabled;
		mEnabled = state;

		// Update the commander for state changes
		if(oldState != state) {
			if( mEnabled ) {
				// Listen for transponders
				getCommander().addResponder(mInventoryResponder);
				// Listen for barcodes
				getCommander().addResponder(mBarcodeResponder);
			} else {
				// Stop listening for transponders
				getCommander().removeResponder(mInventoryResponder);
				// Stop listening for barcodes
				getCommander().removeResponder(mBarcodeResponder);
			}
			
		}
	}

	// The command to use as a responder to capture incoming inventory responses
	private InventoryCommand mInventoryResponder;
	// The command used to issue commands
	private InventoryCommand mInventoryCommand;

	// The command to use as a responder to capture incoming barcode responses
	private BarcodeCommand mBarcodeResponder;
	
	// The inventory command configuration
	public InventoryCommand getCommand() { return mInventoryCommand; }

	public InventoryModel()
	{
		// This is the command that will be used to perform configuration changes and inventories
		mInventoryCommand = new InventoryCommand();
        mInventoryCommand.setResetParameters(TriState.YES);
		// Configure the type of inventory
		mInventoryCommand.setIncludeTransponderRssi(TriState.YES);
		mInventoryCommand.setIncludeChecksum(TriState.YES);
        mInventoryCommand.setIncludePC(TriState.YES);
        mInventoryCommand.setIncludeDateTime(TriState.YES);

		// Use an InventoryCommand as a responder to capture all incoming inventory responses
		mInventoryResponder = new InventoryCommand();

		// Also capture the responses that were not from App commands 
		mInventoryResponder.setCaptureNonLibraryResponses(true);

		// Notify when each transponder is seen
		mInventoryResponder.setTransponderReceivedDelegate(new ITransponderReceivedDelegate() {

			int mTagsSeen = 0;
			@Override
			public void transponderReceived(TransponderData transponder, boolean moreAvailable) {
				mAnyTagSeen = true;

				String tidMessage = transponder.getTidData() == null ? "" : HexEncoding.bytesToString(transponder.getTidData());
				String infoMsg = String.format(Locale.US, "\nRSSI: %d  PC: %04X  CRC: %04X", transponder.getRssi(), transponder.getPc(), transponder.getCrc());
				sendMessageNotification("EPC: " + transponder.getEpc() + infoMsg + "\nTID: " + tidMessage + "\n# " + mTagsSeen );
				mTagsSeen++;
				if( !moreAvailable) {
					sendMessageNotification("");
					Log.d("TagCount",String.format("Tags seen: %s", mTagsSeen));
				}
			}
		});

		mInventoryResponder.setResponseLifecycleDelegate( new ICommandResponseLifecycleDelegate() {
			
			@Override
			public void responseEnded() {
				if( !mAnyTagSeen && mInventoryCommand.getTakeNoAction() != TriState.YES) {
					sendMessageNotification("No transponders seen");
				}
                mInventoryCommand.setTakeNoAction(TriState.NO);
            }
			
			@Override
			public void responseBegan() {
				mAnyTagSeen = false;
			}
		});

		// This command is used to capture barcode responses
		mBarcodeResponder = new BarcodeCommand();
		mBarcodeResponder.setCaptureNonLibraryResponses(true);
		mBarcodeResponder.setUseEscapeCharacter(TriState.YES);
		mBarcodeResponder.setBarcodeReceivedDelegate(new IBarcodeReceivedDelegate() {
			@Override
			public void barcodeReceived(String barcode) {
				sendMessageNotification("BC: " + barcode);
			}
		});

	
	}

	//
	// Reset the reader configuration to default command values
	//
	public void resetDevice()
	{
		if(getCommander().isConnected()) {
            FactoryDefaultsCommand fdCommand = new FactoryDefaultsCommand();
            fdCommand.setResetParameters(TriState.YES);
            getCommander().executeCommand(fdCommand);
		}
	}
	
	//
	// Update the reader configuration from the command
	// Call this after each change to the model's command
	//
	public void updateConfiguration()
	{
		if(getCommander().isConnected()) {
			mInventoryCommand.setTakeNoAction(TriState.YES);
			getCommander().executeCommand(mInventoryCommand);
		}
	}
	
	//
	// Perform an inventory scan with the current command parameters
	//
	public void scan()
	{
		testForAntenna();
		if(getCommander().isConnected()) {
			mInventoryCommand.setTakeNoAction(TriState.NO);
			getCommander().executeCommand(mInventoryCommand);
		}
	}


	//
	// Test for the presence of the antenna
	//
	public void testForAntenna()
	{
		if(getCommander().isConnected()) {
			InventoryCommand testCommand = InventoryCommand.synchronousCommand();
			testCommand.setTakeNoAction(TriState.YES);
			getCommander().executeCommand(testCommand);
			if( !testCommand.isSuccessful() ) {
				sendMessageNotification("ER:Error! Code: " + testCommand.getErrorCode() + " " + testCommand.getMessages().toString());
			}
		}
	}
}
