package com.xinnan.richtextnote.richtextnote.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.sendtion.xrichtext.RichTextEditor;
import com.xinnan.richtextnote.richtextnote.R;
import com.xinnan.richtextnote.richtextnote.bean.Group;
import com.xinnan.richtextnote.richtextnote.bean.Note;
import com.xinnan.richtextnote.richtextnote.db.GroupDao;
import com.xinnan.richtextnote.richtextnote.db.NoteDao;
import com.xinnan.richtextnote.richtextnote.util.CommonUtil;
import com.xinnan.richtextnote.richtextnote.util.DateUtils;
import com.xinnan.richtextnote.richtextnote.util.ImageUtils;
import com.xinnan.richtextnote.richtextnote.util.SDCardUtil;
import com.xinnan.richtextnote.richtextnote.util.ScreenUtils;
import com.xinnan.richtextnote.richtextnote.util.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import me.iwf.photopicker.PhotoPicker;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * 新建笔记
 */
public class NewActivity extends BaseActivity {

    private EditText et_new_title;
    private RichTextEditor et_new_content;
    private TextView tv_new_time;
    private TextView tv_new_group;
    private TextView tv_new_color;

    private GroupDao groupDao;
    private NoteDao noteDao;
    private Note note;//笔记对象
    private String myTitle;
    private String myContent;
    private String myGroupName;
    private String myNoteTime;
    private int flag;//区分是新建笔记还是编辑笔记

    private static final int cutTitleLength = 20;//截取的标题长度

    private ProgressDialog loadingDialog;
    private ProgressDialog insertDialog;
    private int screenWidth;
    private int screenHeight;
    private Subscription subsLoading;
    private Subscription subsInsert;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    String color = "#ffffff";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_new);
        checkPermission();
        initView();

    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_new);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //toolbar.setNavigationIcon(R.drawable.ic_dialog_info);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dealwithExit();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_new);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        groupDao = new GroupDao(this);
        noteDao = new NoteDao(this);
        note = new Note();

        screenWidth = ScreenUtils.getScreenWidth(this);
        screenHeight = ScreenUtils.getScreenHeight(this);

        insertDialog = new ProgressDialog(this);
        insertDialog.setMessage("正在插入图片...");
        insertDialog.setCanceledOnTouchOutside(false);

        loadingDialog = new ProgressDialog(this);
        loadingDialog.setMessage("图片解析中...");
        loadingDialog.setCanceledOnTouchOutside(false);

        et_new_title = (EditText) findViewById(R.id.et_new_title);
        et_new_content = (RichTextEditor) findViewById(R.id.et_new_content);
        tv_new_time = (TextView) findViewById(R.id.tv_new_time);
        tv_new_group = (TextView) findViewById(R.id.tv_new_group);
        tv_new_color = findViewById(R.id.tv_new_color);

        tv_new_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View v = new View(NewActivity.this);
                PopupMenu pop = new PopupMenu(NewActivity.this, v,100);
                MenuInflater inflater = pop.getMenuInflater();
                inflater.inflate(R.menu.menu_group, pop.getMenu());
                Menu Menu = pop.getMenu();//.addMenu(0,999,9,"更多");
                GroupDao groupDao = new GroupDao(NewActivity.this);
                List<Group> groupList = groupDao.queryGroupAll();
                for (int i = 0; i < groupList.size(); i++) {
                    Menu.add(0,groupList.get(i).getId(),0,groupList.get(i).getName());
                }
//                Menu.add(0,1,0,"111");
//                Menu.add(0,1,0,"222");
                pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        int id = menuItem.getItemId();
//                        Toast.makeText(NewActivity.this, id, Toast.LENGTH_SHORT).show();
                        tv_new_group.setText(menuItem.getTitle());
                        return true;
                    }
                });
                pop.show();
            }
        });

        Intent intent = getIntent();
        flag = intent.getIntExtra("flag", 0);//0新建，1编辑
        if (flag == 1){//编辑
            Bundle bundle = intent.getBundleExtra("data");
            note = (Note) bundle.getSerializable("note");

            myTitle = note.getTitle();
            myContent = note.getContent();
            myNoteTime = note.getCreateTime();
            myGroupName = note.getGroupName();

            setTitle("编辑笔记");
            tv_new_time.setText(note.getCreateTime());
            tv_new_group.setText(myGroupName);
            tv_new_color.setBackgroundColor(Color.parseColor(note.getBgColor()));
            et_new_title.setText(note.getTitle());
            et_new_content.post(new Runnable() {
                @Override
                public void run() {
                    //showEditData(note.getContent());
                    et_new_content.clearAllLayout();
                    showDataSync(note.getContent());
                }
            });
        } else {
            setTitle("新建笔记");
            if (myGroupName == null || "全部笔记".equals(myGroupName)) {
                myGroupName = "默认笔记";
            }
            tv_new_group.setText(myGroupName);
            myNoteTime = DateUtils.date2string(new Date());
            tv_new_time.setText(myNoteTime);
        }

    }

    /**
     * 异步方式显示数据
     * @param html
     */
    private void showDataSync(final String html){
        loadingDialog.show();

        subsLoading = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                showEditData(subscriber, html);
            }
        })
        .onBackpressureBuffer()
        .subscribeOn(Schedulers.io())//生产事件在io
        .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
        .subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                loadingDialog.dismiss();
            }

            @Override
            public void onError(Throwable e) {
                loadingDialog.dismiss();
                showToast("解析错误：图片不存在或已损坏");
            }

            @Override
            public void onNext(String text) {
                if (text.contains(SDCardUtil.getPictureDir())){
                    et_new_content.addImageViewAtIndex(et_new_content.getLastIndex(), text);
                } else {
                    et_new_content.addEditTextAtIndex(et_new_content.getLastIndex(), text);
                }
            }
        });
    }

    /**
     * 显示数据
     */
    protected void showEditData(Subscriber<? super String> subscriber, String html) {
        try{
            List<String> textList = StringUtils.cutStringByImgTag(html);
            for (int i = 0; i < textList.size(); i++) {
                String text = textList.get(i);
                if (text.contains("<img")) {
                    String imagePath = StringUtils.getImgSrc(text);
                    if (new File(imagePath).exists()) {
                        subscriber.onNext(imagePath);
                    } else {
                        showToast("图片"+i+"已丢失，请重新插入！");
                    }
                } else {
                    subscriber.onNext(text);
                }

            }
            subscriber.onCompleted();
        }catch (Exception e){
            e.printStackTrace();
            subscriber.onError(e);
        }
    }

    /**
     * 负责处理编辑数据提交等事宜，请自行实现
     */
    private String getEditData() {
        List<RichTextEditor.EditData> editList = et_new_content.buildEditData();
        StringBuffer content = new StringBuffer();
        for (RichTextEditor.EditData itemData : editList) {
            if (itemData.inputStr != null) {
                content.append(itemData.inputStr);
                //Log.d("RichEditor", "commit inputStr=" + itemData.inputStr);
            } else if (itemData.imagePath != null) {
                content.append("<img src=\"").append(itemData.imagePath).append("\"/>");
                //Log.d("RichEditor", "commit imgePath=" + itemData.imagePath);
                //imageList.add(itemData.imagePath);
            }
        }
        return content.toString();
    }

    /**
     * 保存数据,=0销毁当前界面，=1不销毁界面，为了防止在后台时保存笔记并销毁，应该只保存笔记
     */
    private void saveNoteData(boolean isBackground) {
        String noteTitle = et_new_title.getText().toString();
        String noteContent = getEditData();
        String groupName = tv_new_group.getText().toString();
        String noteTime = tv_new_time.getText().toString();
        View v = new View(this);
                PopupMenu pop = new PopupMenu(this, v,100);
                MenuInflater inflater = pop.getMenuInflater();
                inflater.inflate(R.menu.menu_color, pop.getMenu());
        Group group = groupDao.queryGroupByName(myGroupName);
        if (group != null) {
            if (noteTitle.length() == 0 ){//如果标题为空，则截取内容为标题
                if (noteContent.length() > cutTitleLength){
                    noteTitle = noteContent.substring(0,cutTitleLength);
                } else if (noteContent.length() > 0 && noteContent.length() <= cutTitleLength){
                    noteTitle = noteContent;
                }
            }
            int groupId = group.getId();
            note.setTitle(noteTitle);
            note.setContent(noteContent);
            note.setGroupId(groupId);
            note.setGroupName(groupName);
            note.setType(2);
            note.setBgColor(color);//"#FFF000");
            note.setIsEncrypt(0);
            note.setCreateTime(DateUtils.date2string(new Date()));
            if (flag == 0 ) {//新建笔记
                if (noteTitle.length() == 0 && noteContent.length() == 0) {
                    if (!isBackground){
                        Toast.makeText(NewActivity.this, "请输入内容", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    long noteId = noteDao.insertNote(note);
                    //Log.i("", "noteId: "+noteId);
                    //查询新建笔记id，防止重复插入
                    note.setId((int) noteId);
                    flag = 1;//插入以后只能是编辑
                    if (!isBackground){
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
            }else if (flag == 1) {//编辑笔记
                if (!noteTitle.equals(myTitle) || !noteContent.equals(myContent)
                        || !groupName.equals(myGroupName) || !noteTime.equals(myNoteTime)) {
                    noteDao.updateNote(note);
                }
                if (!isBackground){
                    finish();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_new, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_select_color:
                View v = new View(this);
                PopupMenu pop = new PopupMenu(this, v,100);
                MenuInflater inflater = pop.getMenuInflater();
                inflater.inflate(R.menu.menu_color, pop.getMenu());
                pop.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.color_red:
                                color = "#FF0000";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_orange:
                                color = "#FF7F00";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_yellow:
                                color = "#FFFF00";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_green:
                                color = "#00FF00";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_cyan:
                                color = "#00FFFF";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_blue:
                                color = "#0000FF";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                            case R.id.color_purple:
                                color = "#9900FF";
                                tv_new_color.setBackgroundColor(Color.parseColor(color));
                                break;
                        }
                        return false;
                    }
                });
                pop.show();
                break;
            case R.id.action_insert_image:
                checkPermission();
                callGallery();
                break;
            case R.id.action_new_save:
                saveNoteData(false);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 调用图库选择
     */
    private void callGallery(){
//        //调用系统图库
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");// 相片类型
//        startActivityForResult(intent, 1);

        //调用第三方图库选择
        PhotoPicker.builder()
                .setPhotoCount(5)//可选择图片数量
                .setShowCamera(true)//是否显示拍照按钮
                .setShowGif(true)//是否显示动态图
                .setPreviewEnabled(true)//是否可以预览
                .start(this, PhotoPicker.REQUEST_CODE);
    }

    public void checkPermission() {
//        if(Build.VERSION.SDK_INT >= 23)
        {
            List<String> permissionStrs = new ArrayList<>();
            int hasWriteSdcardPermission =
                    ContextCompat.checkSelfPermission(
                            NewActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if(hasWriteSdcardPermission !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionStrs.add(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                );
            }

            int hasCameraPermission = ContextCompat.checkSelfPermission(
                    NewActivity.this,
                    Manifest.permission.CAMERA);
            if(hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissionStrs.add(Manifest.permission.CAMERA);
            }
            String[] stringArray = permissionStrs.toArray(new String[0]);
            if (permissionStrs.size() > 0) {
                requestPermissions(stringArray,
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                if (requestCode == 1){
                    //处理调用系统图库
                } else if (requestCode == PhotoPicker.REQUEST_CODE){
                    //异步方式插入图片
                    insertImagesSync(data);
                }
            }
        }
    }

    /**
     * 异步方式插入图片
     * @param data
     */
    private void insertImagesSync(final Intent data){
        insertDialog.show();

        subsInsert = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                try{
                    et_new_content.measure(0, 0);
                    int width = ScreenUtils.getScreenWidth(NewActivity.this);
                    int height = ScreenUtils.getScreenHeight(NewActivity.this);
                    ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
                    //可以同时插入多张图片
                    for (String imagePath : photos) {
                        //Log.i("NewActivity", "###path=" + imagePath);
                        Bitmap bitmap = ImageUtils.getSmallBitmap(imagePath, width, height);//压缩图片
                        //bitmap = BitmapFactory.decodeFile(imagePath);
                        imagePath = SDCardUtil.saveToSdCard(bitmap);
                        //Log.i("NewActivity", "###imagePath="+imagePath);
                        subscriber.onNext(imagePath);
                    }
                    subscriber.onCompleted();
                }catch (Exception e){
                    e.printStackTrace();
                    subscriber.onError(e);
                }
            }
        })
        .onBackpressureBuffer()
        .subscribeOn(Schedulers.io())//生产事件在io
        .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
        .subscribe(new Observer<String>() {
            @Override
            public void onCompleted() {
                insertDialog.dismiss();
                et_new_content.addEditTextAtIndex(et_new_content.getLastIndex(), " ");
                showToast("图片插入成功");
            }

            @Override
            public void onError(Throwable e) {
                insertDialog.dismiss();
                showToast("图片插入失败:"+e.getMessage());
            }

            @Override
            public void onNext(String imagePath) {
                et_new_content.insertImage(imagePath, et_new_content.getMeasuredWidth());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //如果APP处于后台，或者手机锁屏，则启用密码锁
        if (CommonUtil.isAppOnBackground(getApplicationContext()) ||
                CommonUtil.isLockScreeen(getApplicationContext())){
            saveNoteData(true);//处于后台时保存数据
        }
    }

    /**
     * 退出处理
     */
    private void dealwithExit(){
        String noteTitle = et_new_title.getText().toString();
        String noteContent = getEditData();
        String groupName = tv_new_group.getText().toString();
        String noteTime = tv_new_time.getText().toString();
        if (flag == 0) {//新建笔记
            if (noteTitle.length() > 0 || noteContent.length() > 0) {
                saveNoteData(false);
            }
        }else if (flag == 1) {//编辑笔记
            if (!noteTitle.equals(myTitle) || !noteContent.equals(myContent)
                    || !groupName.equals(myGroupName) || !noteTime.equals(myNoteTime)) {
                saveNoteData(false);
            }
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        dealwithExit();
    }
}
