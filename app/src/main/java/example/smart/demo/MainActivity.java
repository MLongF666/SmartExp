package example.smart.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    private static final String MYTAG = "MainActivity";

    private static SocketServiceHex myservice;
    private ServiceMsgReceiver myServiceMsg;

    private static final int MAXLEN = 1024;
    private static final int DELAY = 100;
    private Timer timer;
    private Handler handler;
    private boolean bSerialLock = false;
    private int iSerialIn = 0;
    private int iSerialOut = 0;
    private byte[] bytesSerialRecBuff = new byte[MAXLEN];

    private SharedPreferences sharedArg;
    private static final String PARAMETER = "parameter";
    private EditText edit_IpAddr, edit_Port;

    /* 下方添加全局定义代码 */
    private View dialogView;
    private EditText edGatewayIpAddress, edGatewayPort;
    private ViewGroup main;

    private static final int MENU_ENVIRONMENT = 0;
    private static final int MENU_CONTROL = 1;
    private int iCurrentMenu = MENU_ENVIRONMENT;
    private boolean fanStatus = false, socketStatus = false, acStatus = false, lampStatus = false;
    private boolean bGas = false, bSmog = false, bInfrared = false, bButton = false, bDoppler = false;
    //初始化标志
    private boolean bIsEnvironmentInitialize;
    private boolean bIsControlInitialize;
    //运行标志
    private boolean bActivityIsRunning;
    private TextView main_title;
    private String[] titles = {"家居环境", "家居控制"};
    private int[] menuImageDefault = {R.drawable.menu_environment, R.drawable.menu_control};
    private int[] menuImageSelect = {R.drawable.menu_environment_selected, R.drawable.menu_control_selected};
    private ViewPager viewPager;
    private ArrayList<View> pageViews;
    private ImageView[] menuImage;
    private TextView[] menuText;


    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myservice = ((SocketServiceHex.LocalBinder) service).getService();
            SocketServiceHex.strMessageForDemo = MYTAG;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myservice = null;
        }
    };
    public void onClick_Event(View view) {
        switch (view.getId()) {
            case R.id.linEnvironment:
                setMenuCurrentItem(MENU_ENVIRONMENT);
                break;
            case R.id.linControl:
                setMenuCurrentItem(MENU_CONTROL);
                break;
            case R.id.linConnectionSetting:
                showConnectionDialog();
                break;
            case R.id.ctl_lamp_on:
                setDimmingLight((byte) 0x09);
                break;
            case R.id.ctl_lamp_off:
                setDimmingLight((byte) 0x00);
                break;
            case R.id.ctl_infra_on:
                RemoteCtrl((byte) 0x01);
                break;
            case R.id.ctl_infra_off:
                RemoteCtrl((byte) 0x02);
                break;
            case R.id.ctl_curtain_on:
                CurtainOpen();
                break;
            case R.id.ctl_curtain_pause:
                CurtainPause();
                break;
            case R.id.ctl_curtain_off:
                CurtainClose();
                break;
            case R.id.ctl_music_on:
                MusicCtrl((byte) 0x02);
                break;
            case R.id.ctl_music_off:
                MusicCtrl((byte) 0x01);
                break;
            case R.id.ctl_fan_on:
                FanOn();
                break;
            case R.id.ctl_fan_off:
                FanOff();
                break;
            default:
                break;
        }

    }

    private void FanOff() {
        sendControlCmd(DataConstants.DEVTYPE_PWM, DataConstants.SENSOR_PWM_FAN,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_BOOL, (byte) 0x00);
    }

    private void FanOn() {
        sendControlCmd(DataConstants.DEVTYPE_PWM, DataConstants.SENSOR_PWM_FAN,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_BOOL, (byte) 0x01);

    }

    private void MusicCtrl(byte bNo) {
        sendControlCmd(DataConstants.DEVTYPE_485, DataConstants.SENSOR_485_MP3,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_ARRAY, bNo, (byte) 0x00);

    }

    private void CurtainOpen() {
        sendControlCmd(DataConstants.DEVTYPE_485, DataConstants.SENSOR_485_8RELAYS,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_ARRAY, (byte) 0xAA, (byte) 0xA1);
    }

    private void CurtainClose() {
        sendControlCmd(DataConstants.DEVTYPE_485, DataConstants.SENSOR_485_8RELAYS,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_ARRAY, (byte) 0xAA, (byte) 0xA4);
    }

    private void CurtainPause() {
        sendControlCmd(DataConstants.DEVTYPE_485, DataConstants.SENSOR_485_8RELAYS,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_ARRAY, (byte) 0xAA, (byte) 0xA0);

    }

    private void RemoteCtrl(byte bNo) {
        sendControlCmd(DataConstants.DEVTYPE_485, DataConstants.SENSOR_485_REMOTECTRL,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_ARRAY, (byte) 0x01, bNo);
        if (bNo == 0x01) {
            acStatus = true;
        } else if (bNo == 0x02) {
            acStatus = false;
        }
    }

    private void setDimmingLight(byte iLevel) {
        sendControlCmd(DataConstants.DEVTYPE_PWM, DataConstants.SENSOR_PWM_LAMP,
                DataConstants.INDEX_FIRST, DataConstants.DATATYPE_INT, (byte) iLevel);
        if (iLevel == 9) {
            lampStatus = true;
        } else if (iLevel == 0) {
            lampStatus = false;
        }

    }

    private void setMenuCurrentItem(int iwhich) {
        menuImage[iCurrentMenu].setBackgroundResource(menuImageDefault[iCurrentMenu]);
        menuImage[iwhich].setBackgroundResource(menuImageSelect[iwhich]);
        menuText[iCurrentMenu].setTextColor(getResources().getColor(R.color.txtDefault));
        menuText[iwhich].setTextColor(getResources().getColor(R.color.txtSelected));
        iCurrentMenu = iwhich;
        viewPager.setCurrentItem(iwhich);

    }

    // 接收service发送过来的广播，动作为service_msg
    public class ServiceMsgReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MYTAG)) {
                String msg = intent.getStringExtra("msg");
                try {
                    ReceiveData(msg.getBytes("ISO-8859-1"));
                } catch (Exception e) {
                    Log.i("Recv", "Error!");
                }
            }
        }
    }

    private void initEvent() {
        // 初始化SP
        sharedArg = getSharedPreferences(PARAMETER, MODE_PRIVATE);

        bIsEnvironmentInitialize = false;
        bIsControlInitialize = false;
//活动是否处在运行状态设置
        bActivityIsRunning = true;
//初始化界面
        LayoutInflater inflater = getLayoutInflater();
        pageViews = getUIObject(inflater);
        main = (ViewGroup) inflater.inflate(R.layout.activity_main, null);
        setContentView(main);
        viewPager = (ViewPager) main.findViewById(R.id.guidePages);
        menuImage = getMenuImageObject();
        menuText = getMenuTextObject();
        menuImage[0].setBackgroundResource(menuImageSelect[iCurrentMenu]);
        menuText[0].setTextColor(getResources().getColor(R.color.txtSelected));
        main_title = main.findViewById(R.id.txt_main_title);
        main_title.setText(titles[0]);
        viewPager.setCurrentItem(0);
        viewPager.setAdapter(new GuidePageAdapter(pageViews));
        viewPager.setOnPageChangeListener(new GuidePageChangeListener());


        bSerialLock = false;
        iSerialIn = 0;
        iSerialOut = 0;
        // 注册接收服务
        myServiceMsg = new ServiceMsgReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MYTAG);
        registerReceiver(myServiceMsg, filter);
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                handler.sendMessage(message);
            }
        }, 500, DELAY);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    scanData();
                }
                super.handleMessage(msg);
            }
        };
        // bind启动服务
        bindService(new Intent(this, SocketServiceHex.class), mConnection, Context.BIND_AUTO_CREATE);

    }

    /// 接收数据到数据缓冲区内
    private void ReceiveData(byte[] bRecData) {
        int i;
        int iDataLen = bRecData.length;
        if (bSerialLock == false) {
            bSerialLock = true;
            if (iSerialIn + iDataLen <= MAXLEN) {
                for (i = 0; i < iDataLen; i++) {
                    bytesSerialRecBuff[iSerialIn + i] = bRecData[i];
                }
                iSerialIn += iDataLen;
            } else if (iSerialIn + iDataLen == MAXLEN) {
                for (i = 0; i < iDataLen; i++) {
                    bytesSerialRecBuff[iSerialIn + i] = bRecData[i];
                }
                iSerialIn = 0;
            } else {
                for (i = iSerialIn; i < MAXLEN; i++) {
                    bytesSerialRecBuff[i] = bRecData[i - iSerialIn];
                }
                for (i = 0; i < iDataLen - MAXLEN + iSerialIn; i++) {
                    bytesSerialRecBuff[i] = bRecData[i + MAXLEN - iSerialIn];
                }
                iSerialIn = iDataLen - MAXLEN + iSerialIn;
            }
            bSerialLock = false;
        }
    }

    //返回后面第iNum有效数据的位置
    private int dataOutLocation(int iMove) {
        int ret = 0;
        if (iSerialOut + iMove < MAXLEN) {
            ret = iSerialOut + iMove;
        } else if (iSerialOut + iMove > MAXLEN) {
            ret = iSerialOut + iMove - MAXLEN;
        }
        return ret;
    }

    // 从缓冲区内读出有效数据
    private void scanData() {
        if (bSerialLock == false) {
            bSerialLock = true;
            int iValidLen, iPacketLen;
            while (iSerialIn != iSerialOut) {
                if (bytesSerialRecBuff[iSerialOut] == DataConstants.DATA_RET_HEAD) {// 判断是否为包头
                    iValidLen = validReceiveLen();// 包含有效数据长度
                    if (iValidLen < 10) { // 有效长度太短
                        bSerialLock = false;
                        return;
                    }
                    iPacketLen = bytesSerialRecBuff[dataOutLocation(1)] & 0xFF;
                    if (iValidLen < iPacketLen) { // 包不完整
                        bSerialLock = false;
                        return;
                    }
                    if (iPacketLen > 9 && iPacketLen < 50) { // 数据长度正常
                        byte[] buf = new byte[iPacketLen];
                        for (int i = 0; i < iPacketLen; i++) {// 读出包并进行校验和计算
                            buf[i] = bytesSerialRecBuff[dataOutLocation(i)];
                        }
                        if (DataFormat.checkCRC(buf)) {
                            Log.w("bufLen:" + String.valueOf(validReceiveLen()), DataFormat.bytes2HexString(buf));
                            dataDispose(buf);
                            iSerialOut = dataOutLocation(iPacketLen);
                            bSerialLock = false;
                            return;
                        }
                    }
                }
                iSerialOut = dataOutLocation(1);
            }
            bSerialLock = false;
        }
    }

    // 读取当前缓冲区中的数据
    private int validReceiveLen() {
        if (iSerialOut < iSerialIn) {
            return (iSerialIn - iSerialOut);
        } else if (iSerialOut > iSerialIn) {
            return (MAXLEN - iSerialOut + iSerialIn);
        }
        return 0;
    }
    private ArrayList<View> getUIObject(LayoutInflater inflater) {
        ArrayList<View> lv_pageViews = new ArrayList<View>();
        View view1 = inflater.inflate(R.layout.activity_evironment, null);
        View view2 = inflater.inflate(R.layout.activity_control, null);
        lv_pageViews.add(view1);
        lv_pageViews.add(view2);
        return lv_pageViews;
    }

    //配置菜单对应的图片
    private ImageView[] getMenuImageObject() {
        ImageView[] imgMenu = new ImageView[4];
        imgMenu[0] = findViewById(R.id.imgEnvironment);
        imgMenu[1] = findViewById(R.id.imgControl);
        return imgMenu;
    }

    //配置菜单对应的文字
    private TextView[] getMenuTextObject() {
        TextView[] txtMenu = new TextView[4];
        txtMenu[0] = findViewById(R.id.txtEnvironment);
        txtMenu[1] = findViewById(R.id.txtControl);
        return txtMenu;
    }

    @Override
    protected void onDestroy() {
        // TODO 自动生成的方法存根
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        //unbindService(mConnection);
    }
    protected void setStatusBar(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.main_bg));//设置状态栏颜色
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR); //设置字体为暗色
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        setStatusBar();
        initEvent();

        /* 下方添加布局组件操控代码 */
    }
    //连接设置对话框
    private void showConnectionDialog() {
        AlertDialog.Builder customizeDialog = new AlertDialog.Builder(MainActivity.this);
        dialogView = getLayoutInflater().inflate(R.layout.dialog_connection, null);
        edGatewayIpAddress = dialogView.findViewById(R.id.edGatewayIpAddress);
        edGatewayPort = dialogView.findViewById(R.id.edGatewayPort);
        String strIpAddress = sharedArg.getString("gatewayIpAddress", SettingConstants.DemoIp);
        String sPort = sharedArg.getString("gatewayPort", SettingConstants.DemoPort);
        edGatewayIpAddress.setText(strIpAddress);
        edGatewayPort.setText(sPort);
        edGatewayIpAddress.setSelection(edGatewayIpAddress.length());
        customizeDialog.setTitle("连接参数设置");
        customizeDialog.setView(dialogView);
        customizeDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String strIPAddr = edGatewayIpAddress.getText().toString();
                String strPort = edGatewayPort.getText().toString();
                if (!strIPAddr.equals("") && !strPort.equals("")) {
                    SharedPreferences.Editor editor = sharedArg.edit();
                    editor.putString("gatewayIpAddress", strIPAddr);
                    editor.putString("gatewayPort",strPort );
                    editor.commit();
                    myservice.socketConnect();
                }
            }
        });
        customizeDialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        customizeDialog.show();
    }


    // 发送物联网控制命令
    private static void sendControlCmd(byte dev_type, byte addr, byte index, byte data_type, byte arg) {
        byte[] bytesSend = new byte[16];
        bytesSend[0] = DataConstants.DATA_HEAD;
        bytesSend[1] = (byte) bytesSend.length;
        bytesSend[2] = dev_type;
        bytesSend[3] = (byte) 0x00;
        bytesSend[4] = addr;
        bytesSend[5] = index;
        bytesSend[6] = DataConstants.CMD_CTRL;
        bytesSend[7] = data_type;
        bytesSend[13] = arg;
        DataFormat.setCRC(bytesSend);
        myservice.socketSend(bytesSend);
    }

    private static void sendControlCmd(byte dev_type, byte addr, byte index, byte data_type, byte arg1, byte arg2) {
        byte[] bytesSend = new byte[16];
        bytesSend[0] = DataConstants.DATA_HEAD;
        bytesSend[1] = (byte) bytesSend.length;
        bytesSend[2] = dev_type;
        bytesSend[3] = (byte) 0x00;
        bytesSend[4] = addr;
        bytesSend[5] = index;
        bytesSend[6] = DataConstants.CMD_CTRL;
        bytesSend[7] = data_type;
        bytesSend[12] = arg1;
        bytesSend[13] = arg2;
        DataFormat.setCRC(bytesSend);
        myservice.socketSend(bytesSend);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            new AlertDialog.Builder(MainActivity.this).setIcon(R.mipmap.ic_launcher).setTitle("退出提示").setMessage("确认退出吗？").setPositiveButton("确认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialoginterface, int i) {
                    finish();
                }
            }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialoginterface, int i) {
                }
            }).show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // 处理接收的数据包
    private void dataDispose(byte[] Packet) {
        /* 下方添加数据包解析代码 */
        byte[] data = new byte[Packet.length - 10];
        for (int i = 0; i < data.length; i++) {
            data[i] = Packet[8 + i];
        }
        if (Packet[2] == DataConstants.DEVTYPE_IO && Packet[6] == DataConstants.CMD_INTERVAL) {
            switch (Packet[4]) {
                case DataConstants.SENSOR_IO_GAS:
                    showGas(data[5]);
                    break;
            }
        } else if (Packet[2] == DataConstants.DEVTYPE_485 && Packet[6] == DataConstants.CMD_INTERVAL) {
            switch (Packet[4]) {
                case DataConstants.SENSOR_485_TEMP:
                    showTemp(data);
                    break;
                case DataConstants.SENSOR_485_HUMI:
                    showHumi(data);
                    break;
                case DataConstants.SENSOR_485_ILLUMINATION:
                    showIllumination(data);
                    break;
            }
        }

    }

    private void showIllumination(byte[] Packet) {
        int iValue = (Packet[4] & 0xFF) * 256 + (Packet[5] & 0xFF);
        TextView txt_data = (TextView) findViewById(R.id.txt_illuminance);
        txt_data.setText(String.valueOf(iValue) + " Lux");
        CheckBox cb_illu_lamp = (CheckBox)findViewById(R.id.cb_link_illu_lamp);
        EditText edit_illu_lamp = (EditText)findViewById(R.id.edit_link_illu_lamp);
        if (cb_illu_lamp.isChecked()) {
            String strEditIllu = edit_illu_lamp.getText().toString().trim();
            if (!strEditIllu.equals("")) {
                int iEditIllu = Integer.parseInt(strEditIllu);
                if (iValue < iEditIllu) {
                    if (!lampStatus) {
                        setDimmingLight((byte) 0x09);
                    }
                } else {
                    if (lampStatus) {
                        setDimmingLight((byte) 0x00);
                    }
                }
            }
        }

    }

    private void showHumi(byte[] Packet) {
        float fhumi = (((Packet[2] & 0xFF) * 256 + (Packet[3] & 0xFF))
                * 65536 + (Packet[4] & 0xFF) * 256 + (Packet[5] & 0xFF)) / 10000.0f;
        DecimalFormat formater = new DecimalFormat("#0.0");
        TextView txt_data = (TextView) findViewById(R.id.txt_humi);
        txt_data.setText(formater.format(fhumi) + " ％");

    }

    private void showTemp(byte[] Packet) {
        float ftemp = (((Packet[2] & 0xFF) * 256 + (Packet[3] & 0xFF))
                * 65536 + (Packet[4] & 0xFF) * 256 + (Packet[5] & 0xFF)) / 10000.0f;
        if (Packet[0] == 1) {
            ftemp *= -1;
        }
        DecimalFormat formater = new DecimalFormat("#0.0");
        TextView txt_data = (TextView) findViewById(R.id.txt_temp);
        txt_data.setText(formater.format(ftemp) + " ℃");
        CheckBox cb_temp_ac = (CheckBox)findViewById(R.id.cb_link_temp_ac);
        EditText edit_temp_ac = (EditText)findViewById(R.id.edit_link_temp_ac);
        if (cb_temp_ac.isChecked()) {
            String strEditTemp = edit_temp_ac.getText().toString().trim();
            if (!strEditTemp.equals("")) {
                int iEditTemp = Integer.parseInt(strEditTemp);
                if (ftemp > iEditTemp) {
                    if (acStatus == false) {
                        RemoteCtrl((byte) 0x01);
                    }
                } else {
                    if (acStatus == true) {
                        RemoteCtrl((byte) 0x02);
                    }
                }
            }
        }

    }

    private void showGas(byte b) {
        TextView txt_data = (TextView) findViewById(R.id.txt_gas);
        if (b == 0x01) {
            txt_data.setText("警告");
            txt_data.setTextColor(Color.RED);
        } else {
            txt_data.setText("正常");
            txt_data.setTextColor(Color.GREEN);
        }
    }

    private class GuidePageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {
            // 设置底部菜单的颜色
            menuImage[iCurrentMenu].setBackgroundResource(menuImageDefault[iCurrentMenu]);
            menuImage[i].setBackgroundResource(menuImageSelect[i]);
            menuText[iCurrentMenu].setTextColor(getResources().getColor(R.color.txtDefault));
            menuText[i].setTextColor(getResources().getColor(R.color.txtSelected));
            iCurrentMenu = i;
            //设置标题
            main_title.setText(titles[i]);
            //初始化界面
            initLayoutItem();

        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    }

    private void initLayoutItem() {
        switch (iCurrentMenu) {
            case MENU_ENVIRONMENT:
                if (!bIsEnvironmentInitialize) {
                    bIsEnvironmentInitialize = true;
                }
                break;
            case MENU_CONTROL:
                if (!bIsControlInitialize) {
                    bIsControlInitialize = true;

                }
                break;
        }

    }
}
