package com.contificoejemplo.myapplication222;

import android.app.Activity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKManager.FEATURE_TYPE;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.BarcodeManager.ConnectionState;
import com.symbol.emdk.barcode.BarcodeManager.ScannerConnectionListener;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.ScanDataCollection.ScanData;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.Scanner.TriggerType;
import com.symbol.emdk.barcode.StatusData.ScannerStates;
import com.symbol.emdk.barcode.StatusData;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.Context;
import android.content.pm.ActivityInfo;

public class activityEscaner extends AppCompatActivity implements EMDKListener, DataListener, StatusListener, ScannerConnectionListener, OnCheckedChangeListener {

    private EMDKManager emdkManager = null;
    private BarcodeManager barcodeManager = null;
    private Scanner scanner = null;

    private boolean bContinuousMode = true;

    private TextView textViewData = null;
    private TextView textViewStatus = null;

    private CheckBox checkBoxEAN8 = null;
    private CheckBox checkBoxEAN13 = null;
    private CheckBox checkBoxCode39 = null;
    private CheckBox checkBoxCode128 = null;
    private CheckBox checkBoxContinuous = null;

    //private Spinner spinnerScannerDevices = null;
    //private Spinner spinnerTriggers = null;

    private List<ScannerInfo> deviceList = null;

    private int scannerIndex = 0;    // Keep the selected scanner
    //private int defaultIndex = 0; // Keep the default scanner
    //private int triggerIndex = 0;
    private int dataLength = 0;
    private String statusString = "";

    //private String [] triggerStrings = {"HARD", "SOFT"};
    //private String [] triggerStrings = {"HARD"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        deviceList = new ArrayList<ScannerInfo>();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        setContentView(R.layout.activity_escaner);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        textViewData = (TextView)findViewById(R.id.textViewData);     // Aqui se guardan los datos leidos
        textViewStatus = (TextView)findViewById(R.id.textViewStatus); // Aqui se guardan el estado del escaner

        checkBoxEAN8 = (CheckBox)findViewById(R.id.checkBoxEAN8);
        checkBoxEAN13 = (CheckBox)findViewById(R.id.checkBoxEAN13);
        checkBoxCode39 = (CheckBox)findViewById(R.id.checkBoxCode39);
        checkBoxCode128 = (CheckBox)findViewById(R.id.checkBoxCode128);

        EMDKResults results = EMDKManager.getEMDKManager(getApplicationContext(), this);

        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            textViewStatus.setText("Estado: " + "Fallo al solicitar Objeto EMDKManager!");
            return;
        }

        checkBoxEAN8.setOnCheckedChangeListener(this);
        checkBoxEAN13.setOnCheckedChangeListener(this);
        checkBoxCode39.setOnCheckedChangeListener(this);
        checkBoxCode128.setOnCheckedChangeListener(this);

        //addSpinnerScannerDevicesListener();                            //Agrega listener
        //populateTriggers();                                            // Llena triggers hard o soft
        //addSpinnerTriggersListener();                                  // agrega listener al combo anterior

        //addStartScanButtonListener();
        //addStopScanButtonListener();
        //addCheckBoxListener();

        textViewData.setSelected(true);
        textViewData.setMovementMethod(new ScrollingMovementMethod());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
        }

        // Release all the resources
        if (emdkManager != null) {
            emdkManager.release();
            emdkManager = null;

        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // The application is in background

        // De-initialize scanner
        deInitScanner();

        // Remove connection listener
        if (barcodeManager != null) {
            barcodeManager.removeConnectionListener(this);
            barcodeManager = null;
            deviceList = null;
        }

        // Release the barcode manager resources
        if (emdkManager != null) {
            emdkManager.release(FEATURE_TYPE.BARCODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The application is in foreground

        // Acquire the barcode manager resources
        if (emdkManager != null) {
            barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

            // Add connection listener
            if (barcodeManager != null) {
                barcodeManager.addConnectionListener(this);
            }

            // Enumerate scanner devices
            enumerateScannerDevices();

            // Set selected scanner
            //spinnerScannerDevices.setSelection(scannerIndex);

            // Initialize scanner
            initScanner();
            setTrigger();  // Setea hard o soft
            setDecoders();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {

        textViewStatus.setText("Estado: " + "Apertura de EMDK exitosa!");

        this.emdkManager = emdkManager;

        // Acquire the barcode manager resources
        barcodeManager = (BarcodeManager) emdkManager.getInstance(FEATURE_TYPE.BARCODE);

        // Add connection listener
        if (barcodeManager != null) {
            barcodeManager.addConnectionListener(this);
        }

        // Enumerate scanner devices
        enumerateScannerDevices();

        startScan();
        // Set default scanner
        //spinnerScannerDevices.setSelection(defaultIndex);
    }

    @Override
    public void onClosed() {

        if (emdkManager != null) {

            // Remove connection listener
            if (barcodeManager != null){
                barcodeManager.removeConnectionListener(this);
                barcodeManager = null;
            }

            // Release all the resources
            emdkManager.release();
            emdkManager = null;
        }
        textViewStatus.setText("Estado: " + "EMDK se cerró inesperadamente! Por favor, cierre y reinicie la aplicación.");
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {

        if ((scanDataCollection != null) && (scanDataCollection.getResult() == ScannerResults.SUCCESS)) {
            ArrayList <ScanData> scanData = scanDataCollection.getScanData();
            for(ScanData data : scanData) {

                String dataString =  data.getData();

                new AsyncDataUpdate().execute(dataString);
            }
        }
    }

    @Override
    public void onStatus(StatusData statusData) {

        ScannerStates state = statusData.getState();
        switch(state) {
            case IDLE:
                statusString = statusData.getFriendlyName()+" está disponible y desocupado...";
                new AsyncStatusUpdate().execute(statusString);
                if (bContinuousMode) {
                    try {
                        // An attempt to use the scanner continuously and rapidly (with a delay < 100 ms between scans)
                        // may cause the scanner to pause momentarily before resuming the scanning.
                        // Hence add some delay (>= 100ms) before submitting the next read.
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        scanner.read();
                    } catch (ScannerException e) {
                        statusString = e.getMessage();
                        new AsyncStatusUpdate().execute(statusString);
                    }
                }
                new AsyncUiControlUpdate().execute(true);
                break;
            case WAITING:
                statusString = "El escaner está esperando.. ";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case SCANNING:
                statusString = "Escaneando...";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(false);
                break;
            case DISABLED:
                statusString = statusData.getFriendlyName()+" está inabilitado.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            case ERROR:
                statusString = "Ha ocurrido un error.";
                new AsyncStatusUpdate().execute(statusString);
                new AsyncUiControlUpdate().execute(true);
                break;
            default:
                break;
        }
    }

    private void addStartScanButtonListener() {

        /*
        Button btnStartScan = (Button)findViewById(R.id.buttonStartScan);

        btnStartScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {

                startScan();
            }
        });
        */
    }

    private void addStopScanButtonListener() {

        /*Button btnStopScan = (Button)findViewById(R.id.buttonStopScan);

        btnStopScan.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                stopScan();
            }
        });*/
    }

    private void enumerateScannerDevices() {

        if (barcodeManager != null) {

            //List<String> friendlyNameList = new ArrayList<String>();
            //int spinnerIndex = 0;

            deviceList = barcodeManager.getSupportedDevicesInfo();

            /*
            if ((deviceList != null) && (deviceList.size() != 0)) {

                Iterator<ScannerInfo> it = deviceList.iterator();

                while(it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if(scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            }
            else {
                textViewStatus.setText("Status: " + "Failed to get the list of supported scanner devices! Please close and restart the application.");
            }
            */

            if ((deviceList == null) || (deviceList.size() == 0)) {
                //textViewStatus.setText("Status: " + "Failed to get the list of supported scanner devices! Please close and restart the application.");
                textViewStatus.setText("Estado: " + "Fallo al obtener dispositivos de escáner soportados. Por favor, cierre y reinicie la aplicación.");
            }

            //ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, friendlyNameList);
            //spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            //spinnerScannerDevices.setAdapter(spinnerAdapter);
        }
    }

    private void setTrigger() {  // Setea hard o soft

        if (scanner == null) {
            initScanner();
        }

        /*
        if (scanner != null) {
            switch (triggerIndex) {
                case 0: // Selected "HARD"
                    scanner.triggerType = TriggerType.HARD;   // Hay que presionar el boton de escanear del dispositivo
                    break;
                case 1: // Selected "SOFT"
                    scanner.triggerType = TriggerType.SOFT_ALWAYS;  // No hay que presionar ningun boton, escanea cualquier cosa que le ponen
                    break;
                    // SOFT_ONCE es lo mismo que SOFT_ALWAYS pero solo una vez
            }
        }*/

        if(scanner != null){
            scanner.triggerType = TriggerType.HARD;
        }
    }

    private void setDecoders() {

        if (scanner == null) {
            initScanner();
        }

        if ((scanner != null) && (scanner.isEnabled())) {
            try {

                ScannerConfig config = scanner.getConfig();

                // Set EAN8
                if(checkBoxEAN8.isChecked())
                    config.decoderParams.ean8.enabled = true;
                else
                    config.decoderParams.ean8.enabled = false;

                // Set EAN13
                if(checkBoxEAN13.isChecked())
                    config.decoderParams.ean13.enabled = true;
                else
                    config.decoderParams.ean13.enabled = false;

                // Set Code39
                if(checkBoxCode39.isChecked())
                    config.decoderParams.code39.enabled = true;
                else
                    config.decoderParams.code39.enabled = false;

                //Set Code128
                if(checkBoxCode128.isChecked())
                    config.decoderParams.code128.enabled = true;
                else
                    config.decoderParams.code128.enabled = false;

                scanner.setConfig(config);

            } catch (ScannerException e) {

                textViewStatus.setText("Estado " + e.getMessage());
            }
        }
    }

    private void startScan() {

        if(scanner == null) {
            initScanner();
        }

        if (scanner != null) {
            try {

                if(scanner.isEnabled())
                {
                    // Submit a new read.
                    scanner.read();

					/*
					if (checkBoxContinuous.isChecked())
						bContinuousMode = true;
					else
						bContinuousMode = false;
					*/

                    new AsyncUiControlUpdate().execute(false);
                }
                else
                {
                    textViewStatus.setText("Estado: Escáner no disponible.");
                }

            } catch (ScannerException e) {

                textViewStatus.setText("Estado: " + e.getMessage());
            }
        }

    }

    private void stopScan() {

        if (scanner != null) {

            try {

                // Reset continuous flag
                //bContinuousMode = false;
                bContinuousMode = true;

                // Cancel the pending read.
                scanner.cancelRead();

                new AsyncUiControlUpdate().execute(true);

            } catch (ScannerException e) {

                textViewStatus.setText("Estado: " + e.getMessage());
            }
        }
    }

    private void initScanner() {

        if (scanner == null) {

            if ((deviceList != null) && (deviceList.size() != 0)) {
                scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
            }
            else {
                // textViewStatus.setText("Estado: " + "Failed to get the specified scanner device! Please close and restart the application.");
                textViewStatus.setText("Estado: " + "Fallo al obtener dispositivos de escáner soportados. Por favor, cierre y reinicie la aplicación.");
                return;
            }

            if (scanner != null) {

                scanner.addDataListener(this);
                scanner.addStatusListener(this);

                try {
                    scanner.enable();
                } catch (ScannerException e) {

                    textViewStatus.setText("Estado: " + e.getMessage());
                }
            }else{
                //textViewStatus.setText("Status: " + "Failed to initialize the scanner device.");
                textViewStatus.setText("Estado: " + "Fallo en la inicialización del dispositivo escáner.");
            }
        }
    }

    private void deInitScanner() {

        if (scanner != null) {

            try {

                scanner.cancelRead();
                scanner.disable();

            } catch (Exception e) {

                textViewStatus.setText("Estado: " + e.getMessage());
            }

            try {
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);

            } catch (Exception e) {

                textViewStatus.setText("Estado: " + e.getMessage());
            }

            try{
                scanner.release();
            } catch (Exception e) {

                textViewStatus.setText("Estado: " + e.getMessage());
            }

            scanner = null;
        }
    }

    private class AsyncDataUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        protected void onPostExecute(String result) {

            if (result != null) {
                if(dataLength ++ > 100) { //Clear the cache after 100 scans
                    textViewData.setText("");
                    dataLength = 0;
                }

                textViewData.append(result+"\n");


                ((View) findViewById(R.id.scrollView1)).post(new Runnable()
                {
                    public void run()
                    {
                        ((ScrollView) findViewById(R.id.scrollView1)).fullScroll(View.FOCUS_DOWN);
                    }
                });

            }
        }
    }

    private class AsyncStatusUpdate extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            return params[0];
        }

        @Override
        protected void onPostExecute(String result) {

            textViewStatus.setText("Estado: " + result);
        }
    }

    private class AsyncUiControlUpdate extends AsyncTask<Boolean, Void, Boolean> {


        @Override
        protected void onPostExecute(Boolean bEnable) {

            checkBoxEAN8.setEnabled(bEnable);
            checkBoxEAN13.setEnabled(bEnable);
            checkBoxCode39.setEnabled(bEnable);
            checkBoxCode128.setEnabled(bEnable);
            //spinnerScannerDevices.setEnabled(bEnable);
            //spinnerTriggers.setEnabled(bEnable);
        }

        @Override
        protected Boolean doInBackground(Boolean... arg0) {

            return arg0[0];
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {

        setDecoders();
    }

    @Override
    public void onConnectionChange(ScannerInfo scannerInfo, ConnectionState connectionState) {

        String status;
        String scannerName = "";

        String statusExtScanner = connectionState.toString();
        String scannerNameExtScanner = scannerInfo.getFriendlyName();

        if (deviceList.size() != 0) {
            scannerName = deviceList.get(scannerIndex).getFriendlyName();
        }

        if (scannerName.equalsIgnoreCase(scannerNameExtScanner)) {

            switch(connectionState) {
                case CONNECTED:
                    deInitScanner();
                    initScanner();
                    setTrigger();   // setea hard o soft
                    setDecoders();
                    break;
                case DISCONNECTED:
                    deInitScanner();
                    new AsyncUiControlUpdate().execute(true);
                    break;
            }

            status = scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        }
        else {
            status =  statusString + " " + scannerNameExtScanner + ":" + statusExtScanner;
            new AsyncStatusUpdate().execute(status);
        }
    }

}
