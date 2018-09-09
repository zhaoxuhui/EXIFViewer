package com.xuhui.exifviewer;

import android.content.Intent;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.*;
import com.baidu.mapapi.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView baiduMapView;
    private BaiduMap baiduMap;
    private List<Marker> markerList = new ArrayList<>();
    private InfoWindow infoWindow;
    private List<String> img_paths = new ArrayList<>();
    private List<LatLng> img_locs = new ArrayList<>();

    private Marker addMarker(BaiduMap baiduMap, LatLng loc, @DrawableRes int ID) {
        // 创建图像资源
        BitmapDescriptor bd = BitmapDescriptorFactory.fromResource(ID);
        // 设置marker属性
        MarkerOptions markerOptions = new MarkerOptions().position(loc).icon(bd).zIndex(9);
        // 向地图添加marker
        Marker marker = (Marker) baiduMap.addOverlay(markerOptions);
        return marker;
    }

    private int findIndex(List<LatLng> list, LatLng loc) {
        double curLat = loc.latitude;
        double curLon = loc.longitude;
        double tempLat;
        double tempLon;
        double minDis = 100;
        double deltaDis;
        int minIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            tempLat = list.get(i).latitude;
            tempLon = list.get(i).longitude;
            deltaDis = Math.abs(curLat - tempLat) + Math.abs(curLon - tempLon);
            if (deltaDis < minDis) {
                minDis = deltaDis;
                minIndex = i;
            }
        }
        return minIndex;
    }

    public double cvtLocTag(String locTag, String locRef) {
        // 直接获取到的位置信息需要解析一下才能使用
        // 33/1,26/1,465/10000
        // 度分秒以逗号隔开，且以除号表示，这样做的好处是以整形存储了浮点型数据
        String loc_deg = locTag.split(",")[0];
        String loc_min = locTag.split(",")[1];
        String loc_sec = locTag.split(",")[2];
        double loc_Deg = Double.parseDouble(loc_deg.split("/")[0]) / 1.0;
        double loc_Min = Double.parseDouble(loc_min.split("/")[0]) / 1.0;
        double loc_Sec = Double.parseDouble(loc_sec.split("/")[0]) / 10000.0;
        double loc = loc_Deg + loc_Min / 60.0 + loc_Sec / 3600.0;
        // 在计算中南纬和西经都为负数，所以增加符号
        if (locRef.contains("S") || locRef.contains("W")) {
            loc = -1 * loc;
        }
        return loc;
    }

    public LatLng getLocInfo(String img_path) {
        try {
            // Step1 新建ExifInterface对象用于获取EXIF信息，传入的参数是图片路径
            ExifInterface exifInterface = new ExifInterface(img_path);
            // Step2 调用获取属性函数获取属性值
            // TAG_GPS_LATITUDE:纬度  TAG_GPS_LONGITUDE:经度
            // TAG_GPS_LATITUDE_REF:南或北半球(S or N) TAG_GPS_LONGITUDE_REF:东或西半球(E or W)
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            // 有些照片没有位置信息，因此需要做个判断
            if (latitude == null || longitude == null) {
                return null;
            } else {
                // 对于有位置信息的照片，调用函数解析经纬度并返回
                double lat = cvtLocTag(latitude, latitudeRef);
                double lon = cvtLocTag(longitude, longitudeRef);
                LatLng point = new LatLng(lat, lon);
                return point;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void passIMG2Sys(String filepath) {
        File img = new File(filepath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //判断是否是AndroidN以及更高的版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(
                    getApplicationContext(),
                    BuildConfig.APPLICATION_ID + ".fileProvider", img);
            intent.setDataAndType(contentUri, "image/*");
        } else {
            intent.setDataAndType(Uri.fromFile(img), "image/*");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        SDKInitializer.setCoordType(CoordType.BD09LL);

        setContentView(R.layout.activity_main);
        baiduMapView = (MapView) findViewById(R.id.bmapView);
        baiduMap = baiduMapView.getMap();

        // 向Marker添加点击事件，如果不同Marker需要不同点击事件，可用If语句判断不同Marker
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                // 创建InfoWindow的内容
                Button button = new Button(getApplicationContext());
                button.setBackgroundResource(R.drawable.popup);
                // 新建InfoWindow点击事件监听器
                InfoWindow.OnInfoWindowClickListener listener = null;

                LatLng ll = marker.getPosition();
                final int index = findIndex(img_locs, ll);

                button.setText("查看照片");
                button.setTextColor(Color.BLACK);
                button.setWidth(300);

                Toast.makeText(getApplicationContext(),
                        String.valueOf(ll.latitude) + ","
                                + String.valueOf(ll.longitude),
                        Toast.LENGTH_LONG).show();


                // 点击InfoWindow后触发的操作
                listener = new InfoWindow.OnInfoWindowClickListener() {
                    @Override
                    public void onInfoWindowClick() {
                        passIMG2Sys(img_paths.get(index));
                        baiduMap.hideInfoWindow();
                    }
                };

                // 配置InfoWindow
                infoWindow = new InfoWindow(BitmapDescriptorFactory.fromView(button), ll, -47, listener);
                // 显示InfoWindow
                baiduMap.showInfoWindow(infoWindow);
                return false;
            }
        });

        // 向地图添加点击事件，当用户点击地图上任意空白区域后会自动清除正在显示的InfoWindow
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                baiduMap.hideInfoWindow();
            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        baiduMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        baiduMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        baiduMapView.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_open:
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("*/*");
                intent.addCategory("android.intent.category.OPENABLE");
                startActivityForResult(intent, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String filepath = data.getData().getPath();

        if (filepath.contains("external_files")) {
            filepath = Environment.getExternalStorageDirectory().toString() + filepath.substring(15);
        }

        // 由于这里我们需要的并不是某一个文件，而是文件夹
        // 所以可以让用户选择某个文件，我们识别出当前文件夹，并遍历图像
        String dirPath = filepath.substring(0, filepath.lastIndexOf('/'));
        File file = new File(dirPath);
        File[] subFile = file.listFiles();
        for (File temp : subFile) {
            LatLng point = getLocInfo(temp.getPath());
            if (point != null) {
                img_paths.add(temp.getPath());
                img_locs.add(point);
            } else {
                Log.e("error", "no location info");
            }
        }
        for (LatLng item : img_locs) {
            markerList.add(addMarker(baiduMap, item, R.drawable.marker_01));
        }

    }
}
