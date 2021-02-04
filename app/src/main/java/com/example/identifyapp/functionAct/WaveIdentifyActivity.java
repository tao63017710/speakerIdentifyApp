package com.example.identifyapp.functionAct;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.identifyapp.R;
import com.example.identifyapp.utils.DatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class WaveIdentifyActivity extends AppCompatActivity {
    private boolean isRecording = true; //判断是否终止
    private boolean checkRepeat = false; // 控制是否开始或暂停
    private Button startButton, stopButton;
    private Intent intent;
    private String personId;
    private boolean sqlSuccess;
    private String errorText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave_identify);

        startButton = findViewById(R.id.waveIdentifyStartBtn);
        stopButton = findViewById(R.id.waveIdentifyStopBtn);

        intent = getIntent();
        personId = intent.getStringExtra("personId");

        //判断安卓版本
        if (Build.VERSION.SDK_INT >= 23) {
            //需要申请的权限
            String[] permission = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
            for (int i = 0; i < permission.length; i++) {
                //判断是否有权限
                if (this.checkSelfPermission(permission[i]) != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(permission, i);
                }
            }
        }

        startButton.setOnClickListener(v -> {
            if (!checkRepeat) {
                startRecording();
                checkRepeat = true;
            }
        });

        stopButton.setOnClickListener(v -> {
            if (checkRepeat) {
                stopRecording();
                checkRepeat = false;
            }
        });
    }

    private void startRecording() {
        // 耗时操作要开线程
        new Thread() {
            @Override
            public void run() {
                sqlSuccess = false;
                // 音源
                int audioSource = MediaRecorder.AudioSource.MIC;
                // 采样率
                int sampleRate = 16000;
                // 声道数
                int channelConfig = AudioFormat.CHANNEL_IN_STEREO;//双声道
                // 采样位数
                int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                // 获取最小缓存区大小
                int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
                // 创建录音对象
                AudioRecord audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, minBufferSize);

                try {
                    //创建文件夹
                    String rec_path = getFilesDir().getAbsolutePath() + "/voice_features";

                    long nameTime = System.currentTimeMillis();
                    String dataPath = rec_path + "/" + nameTime + ".wav";

                    // 创建随机读写流
                    RandomAccessFile raf = new RandomAccessFile(dataPath, "rw");
                    // 留出文件头的位置
                    raf.seek(44);
                    byte[] buffer = new byte[minBufferSize];

                    // 录音中
                    audioRecord.startRecording();
                    isRecording = true;
                    while (isRecording) {
                        int readSize = audioRecord.read(buffer, 0, minBufferSize);
                        raf.write(buffer, 0, readSize);
                    }

                    // 录音停止
                    audioRecord.stop();
                    audioRecord.release();

                    // 写文件头
                    WriteWaveFileHeader(raf, raf.length(), sampleRate, 2, sampleRate * 16 * 2 / 8);
                    raf.close();

                    //识别
                    initPython();
                    dataPath = rec_path + "/" + "PhilippeRemy_001.wav";
                    String[] recognitionResult = recognitionSpeaker(dataPath, rec_path);

                    //删除识别文件
                    File file = new File(dataPath);
                    file.delete();

                    //异常情况
                    if (recognitionResult[0].equals("dont exist")) {
                        errorText = "此人无法识别";
                    } else if (recognitionResult[0].equals("error")) {
                        errorText = "识别出错";
                    } else if (recognitionResult[0].equals("no wave input")) {
                        errorText = "找不到识别声音文件";
                    }
                    //正常情况
                    else {
                        String recognitionFinishPath = recognitionResult[0];
                        double similarity = Double.parseDouble(recognitionResult[1]);

                        String[] splitPath = recognitionFinishPath.split("/");
                        int personIdFind = Integer.parseInt(splitPath[splitPath.length - 2]);
                        //TODO personIdFind为查找到的人的id, similarity为相似度;
                        DatabaseHelper helper = new DatabaseHelper(WaveIdentifyActivity.this, "feature_identify.db", null, 1);
                        SQLiteDatabase db = helper.getWritableDatabase();
                        // Cursor cursor = db.rawQuery("select * from voice_features where person_id = ?", new String[]{String.valueOf(personIdFind)});
                    }

//                    //音频没问题，插入数据库
//                    if(registerCheck){
//                        try {
//                            DatabaseHelper helper = new DatabaseHelper(WaveIdentifyActivity.this, "feature_identify.db", null, 1);
//                            SQLiteDatabase db = helper.getWritableDatabase();
//                            String uuid = UUID.randomUUID().toString();
//                            // TODO 注册地点暂时写死
//                            String address = "zjut";
//                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
//                            String date = df.format(new Date());
//                            String sql = String.format(
//                                    "insert into voice_features (uuid, person_id, collection_date, collection_address, org_data, feature)" +
//                                            "values ('%s','%s','%s','%s','%s','%s')",
//                                    uuid, personId, date, address, dataPath, numpyPath);
//                            db.execSQL(sql);
//                            sqlSuccess = true;
//                        }
//                        catch (Exception e){
//                            sqlSuccess = false;
//                        }
//                    }
//                    else {
//                        File file = new File(dataPath);
//                        file.delete();
//                    }


                    //显示报错信息
                    if (errorText != null) {
                        Looper.prepare();
                        Toast.makeText(WaveIdentifyActivity.this, errorText, Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 为 wav 文件添加文件头，前提是在头部预留了 44字节空间
     *
     * @param raf        随机读写流
     * @param fileLength 文件总长
     * @param sampleRate 采样率
     * @param channels   声道数量
     * @param byteRate   码率 = 采样率 * 采样位数 * 声道数 / 8
     * @throws IOException
     */
    private void WriteWaveFileHeader(RandomAccessFile raf, long fileLength, long sampleRate, int channels, long byteRate) throws IOException {
        long totalDataLen = fileLength + 36;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (fileLength & 0xff);
        header[41] = (byte) ((fileLength >> 8) & 0xff);
        header[42] = (byte) ((fileLength >> 16) & 0xff);
        header[43] = (byte) ((fileLength >> 24) & 0xff);
        raf.seek(0);
        raf.write(header, 0, 44);
    }

    private void stopRecording() {
        // 停止录音
        isRecording = false;
        System.out.println("stop recording");
    }

    private void initPython() {
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    private String[] recognitionSpeaker(String filePath, String voiceRootPath) {
        Python python = Python.getInstance();
        PyObject out = python.getModule("recognition_android_edition1").callAttr("distinguish", filePath, voiceRootPath);
        List<PyObject> inf = out.asList();

        String[] result= new String[2];
        for (int i = 0; i < inf.size(); i++) {
            result[i] = inf.get(i).toString();
        }
        return result;
//        for (PyObject i:inf){
//            System.out.println(i.toString());
//        }
    }
}