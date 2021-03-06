package icg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Properties;

/*
 * 非推奨。コミットId3f3d0d8現在使用無し
 * Propertiesの順番通りに出力してくれない、パス入力の面倒な仕様、既存のコメント削除仕様の改善
 * */
public class ExProperties extends Properties{
	private File file;

	public ExProperties(File file){
		this.file = file;
	}

	/*
	 * コンストラクタで指定したファイルをベースにプロパティを更新する
	 * */
	public void postscript() throws IOException{
		//複製を作り、それを見ながらパラメータをセットしていく
		File copyFile = new File(file.toString() +"copy");
		Files.copy(Paths.get(file.toString()), Paths.get(copyFile.toString()));
		try(
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(copyFile),"UTF-8"));
		){
			String lineStr;
			Hashtable<Object,Object> tab = (Hashtable<Object,Object>)this.clone();
			while((lineStr = reader.readLine())!=null) {
				if(lineStr.matches("[#!]{1}.*") || !lineStr.matches(".+[=]{1}.*")) {
					//プロパティを記述している文で無かった場合
					writer.write(lineStr);
					writer.newLine();
					continue;
				}
				String key,oldValue,newValue;
				String sublineStr[] = lineStr.split("=", 2);
				key = sublineStr[0];
				oldValue = sublineStr[1];
				newValue = (String)tab.get(key);


				if(newValue == null) {
					//パラメータが更新されなかった場合同じ内容を書き込む
					writer.write(lineStr);
				}else {
					//更新された場合はその値を書き込む
					//同時に書き終わったものを消しておく
					writer.write(key+"="+newValue);
					tab.remove(key);
				}

				writer.newLine();
			}
			for(Object key : tab.keySet()) {
				writer.write((String)key + "=" + tab.get(key));
				writer.newLine();
			}
			writer.flush();
		}catch(IOException e) {
			throw e;
		}
		copyFile.delete();
	}
}
