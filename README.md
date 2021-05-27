# TSL-RFID-Bluetooth-Demo-Project
In this project, we will be implementing the basic TSL SDK v2.0 for our demo project and connect and read RFID tags.

# Steps:
## 1- Downloading Resources:
Downloaded RFID Scanner SDK from website:
- https://www.tsl.com/downloads/tsl-products/1128-downloads/


## 2- Add the SDK Library in your project:
i.      Copy the Rfid.AsciiProtocol-Library folder to your project folder.

ii.     Add this line to the Settings.Gradle file
            
	    include ':Rfid.AsciiProtocol-Library'
	    
iii.    Run a Gradle Sync

iv.     The included Javadoc documentation can be associated with the library using the _Project _view.

v.      Expand the 'External Libraries'

vi.     Right-click the 'Gradle:artifacts:Rfid.AsciiProtocol-Library' item.

vii.    Open the 'Library properties'

viii.   Click the green +

ix.     Navigate to the 'Rfid.AsciiProtocol-Library/doc' folder and click 'OK'

x.      Run a Gradle Sync

## 3- Use the DeviceListActivity
i.      Copy the DeviceList project source to the target project's root folder.

ii.     In Settings.gradle add:
		         
			 include ':DeviceList'            
iii.    Add a dependency for the DeviceListActivity project to the build.gradle file for the project:
		        
            dependencies {
               		implementation project(':DeviceList')
	          }
iv.     Place the following code to call the activity of devicelist:
          
              // Place this at the start of Activity
              private Reader mReader = null;
              
              // Method to call DeviceList Activity
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
v.      Place the following code on return of ActivityResults:
              
              public void onActivityResult(int requestCode, int resultCode, Intent data) {
                  switch (requestCode) {
                      case DeviceListActivity.SELECT_DEVICE_REQUEST:
                          // When DeviceListActivity returns with a device to connect
                          if (resultCode == Activity.RESULT_OK)
                          {
                              int readerIndex = data.getExtras().getInt(EXTRA_DEVICE_INDEX);
                              Reader chosenReader = ReaderManager.sharedInstance().getReaderList().list().get(readerIndex);

                              int action = data.getExtras().getInt(EXTRA_DEVICE_ACTION);

                              // If already connected to a different reader then disconnect it
                              if( mReader != null )
                              {
                                  if( action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_DISCONNECT)
                                  {
                                      appendMessage("Disconnecting from: " + mReader.getDisplayName() + "\n");
                                      mReader.disconnect();
                                      if(action == DeviceListActivity.DEVICE_DISCONNECT)
                                      {
                                          mReader = null;
                                      }
                                  }
                              }

                              // Use the Reader found
                              if( action == DeviceListActivity.DEVICE_CHANGE || action == DeviceListActivity.DEVICE_CONNECT)
                              {
                                  mReader = chosenReader;
                                  getCommander().setReader(mReader);
                              }
                          }
                          break;
                  }
              }
v. Please note, there might be a few changes you need to make in DeviceListActivity e.g.

              // move the following line of code on the start of onCreate:
              
              // Configure the ReaderManager when necessary
              ReaderManager.create(getApplicationContext());
              
## 4- Implementing our Demo Project Activity
#### MainActivity.java
- Connect to TSL RFID Reader
	- Connect with already paired device
	- Pair with new TSL otherwise

- Start receiving values in our callbacks
	- Implementation of _TSLReaderAlertNotifier_
