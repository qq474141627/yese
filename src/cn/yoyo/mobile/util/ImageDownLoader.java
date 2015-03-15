package cn.yoyo.mobile.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Video.Thumbnails;
import android.support.v4.util.LruCache;

public class ImageDownLoader {
	/**
	 * 缓存Image的类，当存储Image的大小大于LruCache设定的值，系统自动释放内存
	 */
	private LruCache<String, Bitmap> mMemoryCache;
	/**
	 * 操作文件相关类对象的引用
	 */
	/**
	 * 下载Image的线程池
	 */
	private ExecutorService mImageThreadPool = null;
	
	
	public ImageDownLoader(Context context){
		//获取系统分配给每个应用程序的最大内存，每个应用系统分配32M
		int maxMemory = (int) Runtime.getRuntime().maxMemory();  
        int mCacheSize = maxMemory / 8;
        //给LruCache分配1/8 4M
		mMemoryCache = new LruCache<String, Bitmap>(mCacheSize){

			//必须重写此方法，来测量Bitmap的大小
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getRowBytes() * value.getHeight();
			}
			
		};
		
	}
	
	
	/**
	 * 获取线程池的方法，因为涉及到并发的问题，我们加上同步锁
	 * @return
	 */
	public ExecutorService getThreadPool(){
		if(mImageThreadPool == null){
			synchronized(ExecutorService.class){
				if(mImageThreadPool == null){
					//为了下载图片更加的流畅，我们用了2个线程来下载图片
					mImageThreadPool = Executors.newFixedThreadPool(2);
				}
			}
		}
		
		return mImageThreadPool;
		
	}
	
	/**
	 * 添加Bitmap到内存缓存
	 * @param key
	 * @param bitmap
	 */
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {  
	    if (getBitmapFromMemCache(key) == null && bitmap != null) {  
	        mMemoryCache.put(key, bitmap);  
	    }  
	}  
	 
	/**
	 * 从内存缓存中获取一个Bitmap
	 * @param key
	 * @return
	 */
	public Bitmap getBitmapFromMemCache(String key) {  
	    return mMemoryCache.get(key);  
	} 
	
	/**
	 * 先从内存缓存中获取Bitmap,如果没有就从SD卡或者手机缓存中获取，SD卡或者手机缓存
	 * 没有就去下载
	 * @param url
	 * @param listener
	 * @return
	 */
	public Bitmap downloadImage(final String url, final onImageLoaderListener listener){
		//我们没有创建此目录保存文件就会报错
		final String subUrl = FileUtil.getFileName(url);
		Bitmap bitmap = showCacheBitmap(subUrl);
		if(bitmap != null){
			return bitmap;
		}else{
			
			final Handler handler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					listener.onImageLoader((Bitmap)msg.obj, url);
				}
			};
			
			getThreadPool().execute(new Runnable() {
				
				@Override
				public void run() {
					Bitmap bitmap = getBitmapFormUrl(url);
					Message msg = handler.obtainMessage();
					msg.obj = bitmap;
					handler.sendMessage(msg);
					
					try {
						//保存在SD卡或者手机目录
						ImageUtils.saveBitmap(subUrl, bitmap);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					//将Bitmap 加入内存缓存
					addBitmapToMemoryCache(subUrl, bitmap);
				}
			});
		}
		
		return null;
	}
	@SuppressLint("NewApi")
	public Bitmap getThumbImage(final String subUrl, final onImageThumbListener listener){
		//我们没有创建此目录保存文件就会报错
		Bitmap bitmap = showCacheBitmap(subUrl);
		if(bitmap != null){
			return bitmap;
		}else{
			
			final Handler handler = new Handler(){
				@Override
				public void handleMessage(Message msg) {
					super.handleMessage(msg);
					listener.onImageLoader((Bitmap)msg.obj, subUrl);
				}
			};
			
			getThreadPool().execute(new Runnable() {
				
				@Override
				public void run() {
					Bitmap bitmap=ThumbnailUtils.createVideoThumbnail(subUrl, Thumbnails.MICRO_KIND);
					Message msg = handler.obtainMessage();
					msg.obj = bitmap;
					handler.sendMessage(msg);
					//将Bitmap 加入内存缓存
					addBitmapToMemoryCache(subUrl, bitmap);
				}
			});
		}
		
		return null;
	}
	/**
	 * 获取Bitmap, 内存中没有就去手机或者sd卡中获取，这一步在getView中会调用，比较关键的一步
	 * @param url
	 * @return
	 */
	public Bitmap showCacheBitmap(String url){
		String filename = FileUtil.getFileName(url);
		if(getBitmapFromMemCache(filename) != null){
			return getBitmapFromMemCache(filename);
		}else if(ImageUtils.isFileImgExists(filename)){
			//从SD卡获取手机里面获取Bitmap
			Bitmap bitmap = ImageUtils.getBitmap(filename);
			//将Bitmap 加入内存缓存
			addBitmapToMemoryCache(filename, bitmap);
			return bitmap;
		}
		
		return null;
	}
	
	
	/**
	 * 从Url中获取Bitmap
	 * @param url
	 * @return
	 */
	private Bitmap getBitmapFormUrl(String url) {
		Bitmap bitmap = null;
		HttpURLConnection con = null;
		try {
			URL mImageUrl = new URL(url);
			con = (HttpURLConnection) mImageUrl.openConnection();
			con.setConnectTimeout(10 * 1000);
			con.setReadTimeout(10 * 1000);
			con.setDoInput(true);
			con.setDoOutput(true);
			bitmap = BitmapFactory.decodeStream(con.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
		return bitmap;
	}
	
	/**
	 * 取消正在下载的任务
	 */
	public synchronized void cancelTask() {
		if(mImageThreadPool != null){
			mImageThreadPool.shutdownNow();
			mImageThreadPool = null;
		}
	}
	
	
	/**
	 * 异步下载图片的回调接口
	 * @author len
	 *
	 */
	public interface onImageLoaderListener{
		void onImageLoader(Bitmap bitmap, String url);
	}
	
	/**
	 * 获取视频缩略图片的回调接口
	 *
	 */
	public interface onImageThumbListener{
		void onImageLoader(Bitmap bitmap, String url);
	}
	
}