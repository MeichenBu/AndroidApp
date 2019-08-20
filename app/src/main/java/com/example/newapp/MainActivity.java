package com.example.newapp;

import android.database.sqlite.SQLiteDatabase;
import android.text.InputType;
import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class MainActivity extends AppCompatActivity implements SensorEventListener{

    public static DataParas dataparas;
    private SensorManager mSensorManager;

    private TextView tv_gravity;
    private TextView tv_geoX;
    private TextView tv_geoY;
    private TextView tv_geoZ;

    private Button   button_upload;
    private Button   button_geoscan;
    private Button   button_geostop;

    private EditText input_building;
    private EditText input_floor;
    private EditText input_location_x;
    private EditText input_location_y;

    private boolean ACCURATE_VALUE = false;
    private boolean GEO_SCAN_ENABLE = false;

    String resultX = "null";
    String resultY = "null";
    String resultZ = "null";
    double finalG = 0;
    String resultG = "null";

    private final float[] mMagnetometerReading = new float[3];
    private final float[] mGravmeterReading = new float[3];
    private float I[] = new float[9];
    private float[] mRotationMatrix = new float[9];

    MathMethod mathMethod = new MathMethod();

    private static final int PERMISSION_REQUEST_CODE = 0;
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private boolean mHasPermission;
    private Sensor mSensorGeo;
    private Sensor mSensorGrav;

    public DBHelper dbHelper;

    //String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
    //String fileName = "AnalysisData.csv";
    //String filePath = baseDir + File.separator + fileName;
    //File f = new File(filePath);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DBHelper(this);
        // SQLiteStudioService.instance().start(this);    //the live connection between sqlitestudio can be realized
        setContentView(R.layout.activity_main);
        initPermission();
        initGrav();
        initGeoSensor();
        initView();
        initDataParas();
    }

    private void initDataParas(){
        dataparas = new DataParas("","","","");
    }
    private void initPermission(){
        mHasPermission = checkPermission();
        if (!mHasPermission) {
            requestPermission();
        }
    }
    private boolean checkPermission() {
        for (String permission : NEEDED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasAllPermission = true;
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i : grantResults) {
                if (i != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermission = false;   //check whether the user allowed all the permissionst
                    break;
                }
            }
            if (hasAllPermission) {
                mHasPermission = true;
                Toast.makeText(MainActivity.this,"ALL required permissions OK!",Toast.LENGTH_SHORT).show();
                      //execute the first time
            } else {  //user does not allow
                mHasPermission = false;
                Toast.makeText(MainActivity.this,"error,failed to get localization permission, please enable it manually!",Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initGeoSensor(){
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensorGeo = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);  //obtain type_magnetic_field sensor

    }


    private void initGrav(){
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);  //obtain type_gravity sensor
    }

    private void initView(){
        button_upload   =  findViewById(R.id.button_upload_to_server);
        button_geoscan  =  findViewById(R.id.button_geo_start);
        button_geostop  =  findViewById(R.id.button_geo_stop);

        input_building = (EditText) findViewById(R.id.input_building);
        input_building.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        // input_building.clearFocus();
        input_floor = (EditText) findViewById(R.id.input_floor);
        input_floor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input_location_x = (EditText) findViewById(R.id.input_loc_x);
        input_location_x.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input_location_y = (EditText) findViewById(R.id.input_loc_y);
        input_location_y.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);


        button_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkInputs() && GEO_SCAN_ENABLE){
                    Toast.makeText(getApplicationContext(),"Insertion Succeeded", Toast.LENGTH_SHORT).show();
                    ContentValues values = new ContentValues();
                    values.put(Point.BUILDING,dataparas.getStore_building());
                    values.put(Point.FLOOR,dataparas.getStore_floor());
                    values.put(Point.LOC_X,dataparas.getStore_location_x());
                    values.put(Point.LOC_Y,dataparas.getStore_location_y());
                    values.put(Point.MAG_X,resultX);
                    values.put(Point.MAG_Y,resultY);
                    values.put(Point.MAG_Z,resultZ);
                    values.put(Point.G,resultG);
                    dbHelper.insert(values);
                }
            }
        });
        button_geoscan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GEO_SCAN_ENABLE = true;
            }
        });
        button_geostop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GEO_SCAN_ENABLE = false;
            }
        });

        tv_geoX = (TextView) findViewById(R.id.geoX);
        tv_geoY = (TextView) findViewById(R.id.geoY);
        tv_geoZ = (TextView) findViewById(R.id.geoZ);
        tv_gravity =(TextView) findViewById(R.id.id_gravity);
    }

    private boolean checkInputs(){
        dataparas.setStore_building(input_building.getText().toString());
        dataparas.setStore_floor(input_floor.getText().toString());
        dataparas.setStore_location_x(input_location_x.getText().toString());
        dataparas.setStore_location_y(input_location_y.getText().toString());
        if (dataparas.getStore_building().length() == 0
                || dataparas.getStore_floor().length() == 0
                || dataparas.getStore_location_x().length() == 0
                || dataparas.getStore_location_y().length() == 0) {
            Toast.makeText(getApplicationContext(), "error, please recheck the inputs!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
    @Override
    protected void onResume() {
        //SENSOR_DELAY_GAME : 38Hz
        //SENSOR_DELAY_NORMAL: 38Hz
        //SENSOR_DELAY_UI : 15Hz
        //SENSOR_DELAY_FASTEST : 4Hz
        super.onResume();
        mSensorManager.registerListener(this, mSensorGeo, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorGrav, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //暂停后停止地磁和加速度计register，减少电池损耗
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    public float[] calulateMatrix(float[] a, float[] b){
        float[] c = new float[3];
        c[0] = a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
        c[1] = a[0]*b[3] + a[1]*b[4] + a[2]*b[5];
        c[2] = a[0]*b[6] + a[1]*b[7] + a[2]*b[8];
        return c;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean success = false;
        if (GEO_SCAN_ENABLE && ACCURATE_VALUE) {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                System.arraycopy(event.values, 0, mMagnetometerReading,
                        0, mMagnetometerReading.length);
                mathMethod.lowPass(event.values.clone(), mMagnetometerReading);
            } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                System.arraycopy(event.values, 0, mGravmeterReading,
                        0, mGravmeterReading.length);
                mathMethod.lowPass(event.values.clone(), mGravmeterReading);
            }
            if(mMagnetometerReading !=null && mGravmeterReading!=null){
                 success = mSensorManager.getRotationMatrix(mRotationMatrix,I,mGravmeterReading,mMagnetometerReading);
                if(success){

                    float finaldata[] = calulateMatrix(mMagnetometerReading,mRotationMatrix);
                    resultX = String.format("%7.6f",finaldata[0]);
                    resultY = String.format("%7.6f",finaldata[1]);
                    resultZ = String.format("%7.6f", finaldata[2]);
                }
            }

            finalG = Math.sqrt(Math.pow(mGravmeterReading[0],2.0)+ Math.pow(mGravmeterReading[1],2.0)+ Math.pow(mGravmeterReading[2],2.0));
            resultG = String.format("%7.6f",finalG);
            tv_geoX.setText("MagX:"+ resultX);
            tv_geoY.setText("MagY:"+ resultY);
            tv_geoZ.setText("MagZ:"+ resultZ);
            tv_gravity.setText("Gravity:" + resultG);
        }}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(accuracy == 3){
            ACCURATE_VALUE = true;
        }else{
            ACCURATE_VALUE = false;
        }
    }
}