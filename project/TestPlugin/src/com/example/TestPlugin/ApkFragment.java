package com.example.TestPlugin;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.storage.StorageManager;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.morgoo.droidplugin.pm.PluginManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApkFragment extends ListFragment implements ServiceConnection {
    private ArrayAdapter<ApkItem> adapter;
    final Handler handler = new Handler();
    private static final String TAG = "ApkFragment";

    public ApkFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        adapter = new ArrayAdapter<ApkItem>(getActivity(), 0) {
            @Override
            public View getView(final int position, View convertView, final ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.apk_item, null);
                }
                ApkItem item = getItem(position);

                ImageView icon = (ImageView) convertView.findViewById(R.id.imageView);
                icon.setImageDrawable(item.icon);

                TextView title = (TextView) convertView.findViewById(R.id.textView1);
                title.setText(item.title);

                TextView version = (TextView) convertView.findViewById(R.id.textView2);
                version.setText(String.format("%s(%s)", item.versionName, item.versionCode));

                TextView btn3 = (TextView) convertView.findViewById(R.id.button3);
                btn3.setText("删除");
                btn3.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        onListItemClick(getListView(), view, position, getItemId(position));
                    }
                });
                TextView btn = (TextView) convertView.findViewById(R.id.button2);
                try {
                    if (item.installing) {
                        btn.setText("安装中ing");
                    } else {
                        if (PluginManager.getInstance().isConnected()) {
                            btn.setText(PluginManager.getInstance().getPackageInfo(item.packageInfo.packageName, 0) != null ? "已经安装" : "安装");
                        } else {
                            btn.setText("等待初始化服务");
                        }
                    }
                } catch (Exception e) {
                    btn.setText("安装1");
                }
                btn.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        onListItemClick(getListView(), view, position, getItemId(position));
                    }
                });


                return convertView;
            }
        };

    }

    private void doUninstall(final ApkItem item) {
        AlertDialog.Builder builder = new Builder(getActivity());
        builder.setTitle("警告，你确定要删除么？");
        builder.setMessage("警告，你确定要删除" + item.title + "么？");
        builder.setNegativeButton("删除", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new File(item.apkfile).delete();
                adapter.remove(item);
                Toast.makeText(getActivity(), "删除成功", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNeutralButton("取消", null);
        builder.show();
    }

    boolean isViewCreated = false;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isViewCreated = true;
        setEmptyText("没有在sdcard找到apk");
        setListAdapter(adapter);
        setListShown(false);
        getListView().setOnItemClickListener(null);
        if (PluginManager.getInstance().isConnected()) {
            startLoad();
        } else {
            PluginManager.getInstance().addServiceConnection(this);
        }
    }

    @Override
    public void onDestroyView() {
        isViewCreated = false;
        super.onDestroyView();
    }

    @Override
    public void setListShown(boolean shown) {
        if (isViewCreated) {
            super.setListShown(shown);
        }
    }

    private void startLoad() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setListShown(true);
            }
        });
        if (!isViewCreated) {
            return;
        }
        new Thread("ApkScanner") {
            @Override
            public void run() {
                File file = Environment.getExternalStorageDirectory();

                //遍历内置储存卡的根目录
                List<File> apks = new ArrayList<File>(10);
                for (File apk : file.listFiles()) {
                    if (apk.exists() && apk.getPath().toLowerCase().endsWith(".apk")) {
                        apks.add(apk);
                    }
                }

                //遍历360下载的根目录
                file = new File(Environment.getExternalStorageDirectory(), "360Download");
                if (file.exists() && file.isDirectory()) {
                    for (File apk : file.listFiles()) {
                        if (apk.exists() && apk.getPath().toLowerCase().endsWith(".apk")) {
                            apks.add(apk);
                        }
                    }
                }
                //遍历外置内存卡目录
                String _StorePath = getStoragePath(ApkFragment.this.getActivity(), true);
//                String _StorePath = getStoragePath(ApkFragment.this.getActivity(), true);
                Log.e(TAG, "外置内存卡的文件路径：" + _StorePath);

                if (!TextUtils.isEmpty(_StorePath)) {
                    file = new File(_StorePath);
                    if (file != null && file.exists() && file.isDirectory()) {
                        File[] files = file.listFiles();
                        if (files != null) {
                            for (File apk : file.listFiles()) {
                                if (apk.exists() && apk.getPath().toLowerCase().endsWith(".apk")) {
                                    apks.add(apk);
                                }
                            }
                        }

                    }
                }


                PackageManager pm = getActivity().getPackageManager();
                for (final File apk : apks) {
                    try {
                        if (apk.exists() && apk.getPath().toLowerCase().endsWith(".apk")) {
                            final PackageInfo info = pm.getPackageArchiveInfo(apk.getPath(), 0);
                            if (info != null && isViewCreated) {
                                try {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            adapter.add(new ApkItem(getActivity(), info, apk.getPath()));
                                        }
                                    });
                                } catch (Exception e) {
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final ApkItem item = adapter.getItem(position);
        if (v.getId() == R.id.button2) {
            if (item.installing) {
                return;
            }
            if (!PluginManager.getInstance().isConnected()) {
                Toast.makeText(getActivity(), "插件服务正在初始化，请稍后再试。。。", Toast.LENGTH_SHORT).show();
            }
            try {
                if (PluginManager.getInstance().getPackageInfo(item.packageInfo.packageName, 0) != null) {
                    Toast.makeText(getActivity(), "已经安装了，不能再安装", Toast.LENGTH_SHORT).show();
                } else {
                    new Thread() {
                        @Override
                        public void run() {
                            doInstall(item);
                        }
                    }.start();

                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    PluginManager.getInstance().installPackage(item.apkfile, 0);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
                adapter.remove(item);
            }
        } else if (v.getId() == R.id.button3) {
            doUninstall(item);
        }
    }

    private synchronized void doInstall(ApkItem item) {
        item.installing = true;

        handler.post(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        try {
            final int re = PluginManager.getInstance().installPackage(item.apkfile, 0);
            item.installing = false;

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity(), re == PluginManager.INSTALL_FAILED_NO_REQUESTEDPERMISSION ? "安装失败，文件请求的权限太多" : "安装完成", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                }
            });
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        startLoad();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public void onDestroy() {
        PluginManager.getInstance().removeServiceConnection(this);
        super.onDestroy();
    }


    /**
     * 通过反射获取内置储存卡和外置存储卡的位置路径
     *
     * @param mContext
     * @param is_removale true为外置，false为内置
     * @return
     */
    private static String getStoragePath(Context mContext, boolean is_removale) {

        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
}
