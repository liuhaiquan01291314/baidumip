package com.example.baidumip;

import android.Manifest;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends AppCompatActivity {

    private MapView mBmapView;
    private BaiduMap map;
    private LocationClient mLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        requstPermiss();
        startLocation();//进入界面，默认进行定位
    }

    private void startLocation() {
        map.setMyLocationEnabled(true);//开启地图的定位图层
        //定位初始化
        mLocationClient = new LocationClient(this);

//通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);//显示范围
//设置locationClientOption
        mLocationClient.setLocOption(option);

//注册LocationListener监听器,,当定位成功会回调此监听器，把我的位置信息传入onReceiveLocation中，使用
        mLocationClient.registerLocationListener(new BDAbstractLocationListener() {

            private LatLng mMySelfLocation;

            @Override
            public void onReceiveLocation(BDLocation location) {
                //mapView 销毁后不在处理新接收的位置
                if (location == null || mBmapView == null) {
                    return;
                }
                //得到定位后的我的位置
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                map.setMyLocationData(locData);//把我的位置标记在地图上

                //把我的位置拉倒地图的中心
                //得到我的坐标，设置为全局，供POI检索使用
                mMySelfLocation = new LatLng(location.getLatitude(), location.getLongitude());

                MapStatusUpdate status2 = MapStatusUpdateFactory.newLatLng(mMySelfLocation);
                map.setMapStatus(status2);

            }
        });
        //开启地图定位图层
        mLocationClient.start();
    }

    private void requstPermiss() {
        String[] permiss = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA};
        //简单处理  ,直接申请
        ActivityCompat.requestPermissions(this, permiss, 100);
    }

    private void initView() {
        mBmapView = (MapView) findViewById(R.id.bmapView);
        map = mBmapView.getMap();
//        MapStatus.Builder builder = new MapStatus.Builder();
//        builder.zoom(18.0f);
//        map.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mBmapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mBmapView.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mBmapView.onDestroy();
    }
}
