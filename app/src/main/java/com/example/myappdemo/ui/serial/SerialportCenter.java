package com.example.myappdemo.ui.serial;

import android.app.smdt.SmdtManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.myappdemo.R;
import com.example.myappdemo.utils.MyUtils;
import com.example.myappdemo.widget.SerialHelper;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class SerialportCenter extends Fragment {
    final String TAG = SerialportCenter.class.getSimpleName();

    Button speakerOpenBtn;
    Button speakerCloseBtn;
    TextView msgView;
    SerialHelper iDcardSerial;
    SerialHelper speakerSerial;
    Boolean IoFlag = false;
    int currIoValue = 1;

    public SerialportCenter() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_serialport_center, container, false);

        bindView(view);
        initIDcardSerial();
        initSpeakerSerial();
        initMicrophone();

        return view;
    }

    private void bindView(View view) {
        speakerOpenBtn = view.findViewById(R.id.loudspeaker_open);
        speakerOpenBtn.setOnClickListener(v -> {
            sendCommand("84", "00");
        });
        speakerCloseBtn = view.findViewById(R.id.loudspeaker_close);
        speakerCloseBtn.setOnClickListener(v -> {
            sendCommand("84", "01");
        });

        Button idcardOpenBtn = view.findViewById(R.id.idcard_open);
        idcardOpenBtn.setOnClickListener(v -> {
            iDcardSerial.open();
        });
        Button idcardCloseBtn = view.findViewById(R.id.idcard_close);
        idcardCloseBtn.setOnClickListener(v -> {
            iDcardSerial.close();
        });
        msgView = view.findViewById(R.id.msg_view);
    }

    private void refreshTextView(String msg) {
        Log.e(TAG, "打印：" + msg);
        SimpleDateFormat sdfTwo = new SimpleDateFormat(" HH:mm:ss", Locale.getDefault());
        long timecurrentTimeMillis = System.currentTimeMillis();
        String time = sdfTwo.format(timecurrentTimeMillis);
        msgView.append(time + "  " + msg + "\n");
        // 滚动到当前
        int offset = msgView.getLineCount() * msgView.getLineHeight();
        if (offset > msgView.getHeight()) {
            msgView.scrollTo(0, offset - msgView.getHeight());
        }
    }

    private void initIDcardSerial() {
        iDcardSerial = new SerialHelper() {
            @Override
            public void onMsgBack(String hexstr, String decstr) {
            }
        };
    }

    // 音响功放串口
    private void initSpeakerSerial() {
        speakerSerial = new SerialHelper("/dev/ttyXRUSB0", 9600) {
            @Override
            public void onMsgBack(String hexstr, String decstr) {
                String helpStr = "A58500005A7A"; // 求助按钮指令
                if (Objects.equals(hexstr, helpStr)) {
                    Log.e(TAG, "按下了求助按钮！");
                }
            }
        };
        speakerSerial.open();
    }

    /**
     * 监听话筒状态
     */
    private void initMicrophone() {
        if (MyUtils.isEmulator()) {
            return;
        }

        SmdtManager smdtManager = SmdtManager.create(getContext());
        if (smdtManager == null) {
            return;
        }
        // 标识话筒拿起状态 1-已拿起，0-未拿起
        currIoValue = smdtManager.smdtReadExtrnalGpioValue(1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int ioValue = smdtManager.smdtReadExtrnalGpioValue(1);
                                if (ioValue == currIoValue) {
                                    // 话筒状态未改变
                                    return;
                                }

                                if (ioValue == 1) {
                                    // 话筒处于拿起状态
                                    refreshTextView("电话机被拿起");
                                    sendCommand("84", "01");
                                } else {
                                    refreshTextView("电话机被挂上");
                                    sendCommand("84", "00");
                                }
                                currIoValue = ioValue; // 记录当前话筒状态

                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 指令组装
     * instruct 命令字 如功放84
     * control 控制 00打开，01关闭
     */
    public void sendCommand(String instruct, String control) {
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append("A5");//起始符
        stringBuffer.append(instruct);//命令
        stringBuffer.append(control);//控制
        stringBuffer.append("00");//数据
        stringBuffer.append("5A");//结束符
        stringBuffer.append(MyUtils.checkXor(stringBuffer.toString()));//异或校验
        speakerSerial.sendHex(stringBuffer.toString());
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy!!");
    }
}