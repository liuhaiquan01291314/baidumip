package com.example.baidumip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.walknavi.WalkNavigateHelper;
import com.baidu.mapapi.walknavi.adapter.IWEngineInitListener;
import com.baidu.mapapi.walknavi.adapter.IWRoutePlanListener;
import com.baidu.mapapi.walknavi.model.WalkRoutePlanError;
import com.baidu.mapapi.walknavi.params.WalkNaviLaunchParam;

import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends AppCompatActivity  implements View.OnClickListener{

    private MapView mBmapView;
    private BaiduMap mBaiduMap;
    private EditText mEtKey;
    /**
     * 搜索
     */
    private Button mBtnSearch;
    private PoiSearch mPoiSearch;
    private LocationClient mLocationClient;

    String[] permissions = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    List<String> mPermissionList = new ArrayList<>();
    private BDLocation mLocation;//我的位置：导航开始位置
    private String TAG = "Main2Activity";
    private EditText mEtGoto;
    private Button mBtnNavi;
    private LatLng end; //导航目的地
    private LatLng start;
    private WalkNaviLaunchParam mParam;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        initView();
        initPermission();
    }
    private void initPermission() {
        mPermissionList.clear();//清空没有通过的权限
        //逐个判断你要的权限是否已经通过
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permissions[i]);//添加还未授予的权限
            }
        }
        //申请权限
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            String[] permiss = mPermissionList.toArray(new String[mPermissionList.size()]);
            ActivityCompat.requestPermissions(this, permiss, 100);
        } else {
            startLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean hasPermissionDismiss = false;//有权限没有通过
        if (requestCode == 100) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                }
            }
            //如果有权限没有被允许
            if (hasPermissionDismiss) {
                //跳转到系统设置权限页面，或者直接关闭页面，不让他继续访问
            } else {
                //全部权限通过，可以进行下一步操作。。。
                startLocation();
            }
        }
    }

    private void initView() {
        mBmapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mBmapView.getMap();
        //切换地图类型
//        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);//卫星图
//开启交通图
//        mBaiduMap.setTrafficEnabled(true);
        mEtKey = (EditText) findViewById(R.id.et_key);
        mBtnSearch = (Button) findViewById(R.id.btn_search);
        mBtnSearch.setOnClickListener(this);

        //点击位置生成一个marker
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                end = latLng;
                mEtGoto.setText(latLng.latitude+","+latLng.longitude);
                addMarker(latLng);
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        });

        //没有使用
        mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                marker.getPosition();
                return false;
            }
        });
        mEtGoto = (EditText) findViewById(R.id.et_goto);
        mBtnNavi = (Button) findViewById(R.id.btn_navi);
        mBtnNavi.setOnClickListener(this);
    }

    private void addMarker(LatLng target) {
        //获取当前地图屏幕中心点的坐标
        //定义Maker坐标点:由经纬度定位的一个位置坐标
        LatLng point = new LatLng(target.latitude, target.longitude);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_marka);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .draggable(true)//可拖拽，默认是false不可拖拽
                .position(point)//坐标位置
                .icon(bitmap);//设置图标
        //在地图上添加Marker，并显
        mBaiduMap.addOverlay(option);
    }

    private void startLocation() {
        mBaiduMap.setMyLocationEnabled(true);
        //定位初始化
        mLocationClient = new LocationClient(this);
        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        mLocationClient.registerLocationListener(new BDAbstractLocationListener() {
            @Override
            public void onReceiveLocation(BDLocation location) {
                mLocation = location;
                //mapView 销毁后不在处理新接收的位置
                if (location == null || mBmapView == null) {
                    return;
                }
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);

                //改变地图手势的中心点
                start = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(start));
            }
        });
        //开启地图定位图层
        mLocationClient.start();
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
        mLocationClient.stop();
        mPoiSearch.destroy();

        mBaiduMap.setMyLocationEnabled(false);
        mBmapView.onDestroy();
        mBmapView = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_search:
                search();
                break;
            case R.id.btn_navi:
                startWalkNavi();
                break;
        }
    }

    private void startWalkNavi() {
        //发起算路
        try {
            WalkNavigateHelper.getInstance().initNaviEngine(this, new IWEngineInitListener() {
                @Override
                public void engineInitSuccess() {
                    Log.d(TAG, "WalkNavi engineInitSuccess");
                    routePlanWithWalkParam();
                }

                @Override
                public void engineInitFail() {
                    Log.d(TAG, "WalkNavi engineInitFail");
                }
            });
        } catch (Exception e) {
            Log.d(TAG, "startBikeNavi Exception");
            e.printStackTrace();
        }
    }

    private void routePlanWithWalkParam() {
        mParam = new WalkNaviLaunchParam().stPt(start).endPt(end);;
        WalkNavigateHelper.getInstance().routePlanWithParams(mParam, new IWRoutePlanListener() {
            @Override
            public void onRoutePlanStart() {
                Log.d(TAG, "WalkNavi onRoutePlanStart");
            }

            @Override
            public void onRoutePlanSuccess() {
                Log.d("View", "onRoutePlanSuccess");
                Intent intent = new Intent();
                intent.setClass(MainActivity2.this, WNaviGuideActivity.class);
                startActivity(intent);
            }

            @Override
            public void onRoutePlanFail(WalkRoutePlanError error) {
                Log.d(TAG, "WalkNavi onRoutePlanFail");
            }

        });
    }

    private void search() {
        String trim = mEtKey.getText().toString().trim();
        if (TextUtils.isEmpty(trim)) {
            Toast.makeText(this, "输入框不能为空", Toast.LENGTH_SHORT).show();
            ;
            return;
        }
        //创建POI检索实例
        mPoiSearch = PoiSearch.newInstance();
        //设置检索监听器
        mPoiSearch.setOnGetPoiSearchResultListener(listener);
        /**
         *  PoiCiySearchOption 设置检索属性
         *  city 检索城市
         *  keyword 检索内容关键字
         *  pageNum 分页页码
         *  radius搜索半径100米以内的餐厅
         */
        mPoiSearch.searchNearby(new PoiNearbySearchOption().location(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()))
                .radius(5000)
                .keyword(trim)
                .pageNum(2));
    }

    //创建POI检索监听器
    OnGetPoiSearchResultListener listener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            //将搜索结果显示
            if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {
                mBaiduMap.clear();
                //创建PoiOverlay对象
                PoiOverlay poiOverlay = new PoiOverlay(mBaiduMap);
                //设置Poi检索数据
                poiOverlay.setData(poiResult);
                //将poiOverlay添加至地图并缩放至合适级别
                poiOverlay.addToMap();
                poiOverlay.zoomToSpan();

                List<PoiInfo> allPoi = poiResult.getAllPoi();
                for (int i = 0; i < allPoi.size(); i++) {
                    PoiInfo poiInfo = allPoi.get(i);
                    String address = poiInfo.address;
                    String name = poiInfo.name;
                    Log.i(TAG, name + "-------" + address);

                }
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailSearchResult poiResult) {

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

        }

        //废弃
        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiResult) {

        }
    };
}
