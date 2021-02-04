package com.example.identifyapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {
    //数据库实例
    private final SQLiteDatabase db;

    //带全部参数的构造函数，此构造函数必不可少
    public DatabaseHelper(Context context, String name, CursorFactory factory, int version) {
        super(context, name, factory, version);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //创建数据库sql语句 并 执行
        String sql = "create table if not exists features(" +
                "id varchar(10)," +
                "name varchar(20)," +
                "feature text)";
        String sql1 = "create table if not exists people_info(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "uuid varchar(40)," +     // UUID长度36个字符
                "name varchar(20)," +
                "card_type varchar(10)," +
                "card_num varchar(20)," +
                "sex int," +
                "country varchar(10)," +
                "race varchar(10)," +
                "age int," +
                "height int," +
                "register_date datetime," +
                "register_address text," +
                "register_type text,"+
                "comment text)";
        String sql2 = "create table if not exists voice_features(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT,"+
                "uuid varchar(40)," +     // UUID长度36个字符
                "person_id varchar(40),"+ //关联people_info的id
                "collection_date datetime,"+
                "collection_address text,"+
                "org_data text,"+          // TODO 21.02.02 ZDM:新增音频源文件地址
                "feature text)";

        db.execSQL(sql1);
        db.execSQL(sql2);
        db.execSQL(sql);


        //添加测试数据
        String uuid = UUID.randomUUID().toString();
        sql = "insert into people_info(name,card_num,sex,country,race,age,height,register_type) values('张三','333333333333333333',0,'中国','汉族',20,170,'{\"face\":1,\"gait\":0,\"finger\":0}');";
        db.execSQL(sql);

        uuid = UUID.randomUUID().toString();
        sql = "insert into voice_features(uuid,person_id,feature,collection_date,collection_address) values('"+uuid+"',3,'王五声纹.jpg','2021-01-03 12:00:00','浙江省杭州市');";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    /**
     * 插入
     * @param name
     * @param id
     * @param feature
     */
    public void insert(String id, String name, String feature){
        db.execSQL("insert into features(id,name,feature) values(?,?,?)", new Object[]{id,name,feature});
    }
    public void delete(String name){
        db.delete("features","name=?",new String[]{name});
    }

    public Cursor selectPersonInfoById(String id){
        String sql = "select person_id, feature from gait_features";
        return db.rawQuery(sql, null);
    }

    public void insertVoice(String person_id, String feature, String collect_address){
        String uuid = UUID.randomUUID().toString();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        String collection_date = df.format(new Date());
        db.execSQL(  "insert into voice_features(uuid,person_id,feature,org_data,collection_date,collection_address)values(?,?,?,?,?,?)",
                new Object[]{uuid,person_id,feature,collection_date,collect_address});
    }

}
