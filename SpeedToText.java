import java.io.File;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;

import org.json.JSONObject;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import com.baidu.aip.speech.AipSpeech;

public class SpeedToText {

	 //设置APPID/AK/SK
    public static final String APP_ID = "11106696";
    public static final String API_KEY = "Zg2KO0RxOXSnrw59mSGA6air";
    public static final String SECRET_KEY = "GW7xtLK936hMYKvf00i7vFk7tNNnGGNw";
    //自己下载ffmpeg
    public static final String ffmpeg="C:\\ffmpeg-20180416-783df2e-win64-static\\bin\\";
    //测试的语音文件，暂时只支持mp3
    public static final String filePath = "d:\\tt.mp3";

	public static void main(String[] args) throws Exception {
		 // 初始化一个AipSpeech
        AipSpeech client = new AipSpeech(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);
        
        //总时长
		int duration = calDurationTime(filePath);
		
		//按照一分钟去切割音频
		int part = duration/60;
		//最后不足1分钟的时间
		int lastSec = duration%60;
		if(duration%60==0) {
			part++;
		}
		
		if(part>0) {
			for(int i=0;i<part;i++) {
				handle(59, i, client);
			}
		}
		if(lastSec>0) {
			handle(lastSec,part, client);
		}
	}
	
	/**
	 * 处理流程(切割,转换格式，转文本)
	 */
	private static void handle(int lastSec,int part,AipSpeech client) {
		String from = "00:"+(part<9?"0":"")+part+":00";
		String end =  "00:00:"+(lastSec<9?"0":"")+lastSec;
		
		String target = filePath.replace(".mp3", "_"+String.valueOf(part)+".mp3");
		split(filePath, from, end, target);
		
		convertToPcm(target);
		
		target = target.replace("mp3", "pcm");
		soundToText(target, client);
	}

	/**
	 * 计算语音的总时长
	 */
	private static int calDurationTime(String filePath) {
		File file = new File(filePath);
		try {
			AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
			if (fileFormat instanceof TAudioFileFormat) {
				Map<String, Object> properties = ((TAudioFileFormat) fileFormat).properties();
				String key = "duration";
				Long microseconds = (Long) properties.get(key);
				int mili = (int) (microseconds / 1000);
				int sec = (mili / 1000);
				return sec;
			} else {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * 将mp3转成pcm
	 */
	private static void convertToPcm(String filePath) {
		String cmd = "/ffmpeg -y  -i %s -acodec pcm_s16le -f s16le -ac 1 -ar 16000 %s";
		
		Runtime run = null;  
        try {  
            run = Runtime.getRuntime();  
            String targetPath = filePath.replace("mp3", "pcm");
            Process p=run.exec(new File(ffmpeg).getAbsolutePath().concat(String.format(cmd, filePath,targetPath)));
            //释放进程  
            p.getOutputStream().close();  
            p.getInputStream().close();  
            p.getErrorStream().close();  
            p.waitFor();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }finally{  
            run.freeMemory();  
        }  
	}
	
	/**
	 * 将语音文件按照时间来分割
	 */
	private static void split(String source,String from,String end,String target) {
		// ffmpeg -i source_mp3.mp3 -ss 00:01:12 -t 00:01:42 -acodec copy output_mp3.mp3
		String cmd = "/ffmpeg -i %s -ss %s -t %s -acodec copy %s";
		
		Runtime run = null;  
        try {  
            run = Runtime.getRuntime();  
            Process p=run.exec(new File(ffmpeg).getAbsolutePath().concat(String.format(cmd, source,from,end,target)));  
            //释放进程  
            p.getOutputStream().close();  
            p.getInputStream().close();  
            p.getErrorStream().close();  
            p.waitFor();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }finally{  
            run.freeMemory();  
        }  
	}
	
	/**
	 * 将语音文件转文字，调用baidu的sdk
	 */
	private static void soundToText(String source,AipSpeech client) {
		 JSONObject asrRes = client.asr(source, "pcm", 16000, null);
	     System.out.println(asrRes);
	     try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
