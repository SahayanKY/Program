package simulation;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

import simulation.frame.DataInputFrame;
import simulation.param.Parameter;
import simulation.param.ParameterManager;

public abstract class Simulator extends SwingWorker<Object,String>{
	private DataInputFrame inputFrame;
	protected BufferedWriter resultWriter;
	private ProgressMonitor monitor;
	private LocalDateTime simulationStartTime;
	protected final ParameterManager paraMan = new ParameterManager(this);
	protected File resultStoreDirectory;
	private double startTime,currentProgressRate;


	public Simulator(){
		addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				ProgressMonitor monitor = Simulator.this.monitor;
				if(monitor == null) {
					return;
				}
				if(monitor.isCanceled()) {
					return;
				}
				if("progress".equals(e.getPropertyName())) {
					String restTimeStr;
					if(startTime == 0.0) {
						startTime = simulationStartTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
					}
					if(currentProgressRate == 0.0) {
						restTimeStr = "";
					}else {
						double elapsedTime = System.currentTimeMillis() - startTime;
						double restTime = elapsedTime * (1-currentProgressRate)/currentProgressRate;
						int restM = (int)(restTime/60000);
						restTimeStr = "約"+((restM==0)?"":restM+"分")+(int)(restTime/1000-restM*60)+"秒";
					}
					monitor.setNote("現在:"+e.getNewValue()+"%　:完了まで"+restTimeStr);
					monitor.setProgress((Integer)e.getNewValue()+1);
				}
			}
		});
	}

	public final void setSimulationStartTime() {
		this.simulationStartTime = LocalDateTime.now();
	}

	public final LocalDateTime getSimulationStartTime() {
		if(simulationStartTime == null) {
			simulationStartTime = LocalDateTime.now();
		}
		return this.simulationStartTime;
	}

	/*
	 * 計算結果の出力先のディレクトリを指定し、シミュレーションを開始する。
	 * 同名のexecute()では正しく起動しない仕様であるので注意。
	 * */
	public void execute(File resultStoreDirectory) throws IOException{
		//シミュレーション日時の決定
		setSimulationStartTime();
		this.resultStoreDirectory = resultStoreDirectory;
		execute();
	}

	@Override
	/*
	 * シミュレーションの計算実行開始メソッド。
	 * */
	protected Object doInBackground() {
		try {
			//指定したディレクトリにパラメータを保存させる
			paraMan.writeProperty_on(resultStoreDirectory);
			executeSimulation();

		}catch(Exception e) {
			try {
				if(resultWriter != null) {
					resultWriter.close();
				}
			} catch (IOException e1) {
			}
			e.printStackTrace();
		}

		monitor.close();

		return null;
	}

	/*
	 * シミュレーション本体の実装部分。Simulatorの子クラスはこのメソッドをオーバーライドし、
	 * 計算を行うようにすること。また、計算の各段階で適切にupdateProgress(double)とpublish(String)を使うこと。
	 * */
	protected abstract void executeSimulation();

	/*
	 * シミュレーションの計算進捗率を指定する。もし、シミュレーションを中断する
	 * ようユーザーから指示が来ていた場合、falseが返る。
	 * @param
	 * progressRate 計算の進捗を表すdouble型の数値。0から1までの数値が有効。
	 * それ以外の数値は自動的に0または1に変換される。
	 * @return
	 * シミュレーションを中断するようProgressMonitorを介して
	 * 指示されていた場合、false
	 * */
	protected boolean updateProgress(double progressRate) {
		if(monitor.isCanceled()) {
			return false;
		}
		this.currentProgressRate = progressRate;
		int n = (int)(progressRate*100);
		if(n < 0) {
			n = 0;
		}else if(n > 100) {
			n = 100;
		}
		setProgress(n);
		return true;
	}

	/*
	 * シミュレーション実行結果を外部ファイルへ出力する。このメソッドは直接呼ぶものではない。
	 * 必ずpublish(String)を介して呼ぶこと。また、publish(String)で指定する文字列は以下に従うこと。
	 *　・"start"を最初に指定すること。これにより外部ファイルへ出力するWriterが生成される。
	 *　・"restart"を指定することで出力先のファイルを新しく設定することができる。例えば、一つのファイルにシミュレーション実行結果を出力しきれない
	 *	場合などに使うこと。
	 *　・結果を指定する場合はCSVファイルのフォーマットに従うこと。また、一行単位で指定すること。
	 * */
	@Override
	protected void process(List<String> list) {
		for(String strline:list) {
			if(strline.equals("restart") || strline.equals("start")) {
				for(int i=0;i<5;i++) {
					try {
						if(strline.equals("restart")) {
							this.setSimulationStartTime();
						}
						File storeFile = new File(resultStoreDirectory.toString()+"\\"+getSimulationStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH時mm分ss.SSS秒"))+"result.csv");
						if(storeFile.exists()) {
							//同名のファイルが存在する場合
							continue;
						}

						if(this.resultWriter != null) {
							this.resultWriter.flush();
							this.resultWriter.close();
						}
						this.resultWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(storeFile),"UTF-8"));
						break;
					} catch (IOException e) {
					}
					if(i==4) {
						throw new Error("保存ファイル作成に5回失敗しました。");
					}
				}
				continue;
			}
			try {
				this.resultWriter.write(strline);
				this.resultWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * このシミュレーションが使用するParameterをParameterManagerに登録する。
	 * オーバーライドするときはSimulatorクラスのcreateParameter()を呼び出す。
	 * */
	public void createParameters(){
		final Parameter 警告 = new Parameter("シミュレーション", "警告"),
				シミュレーション年月日時分秒 = new Parameter("シミュレーション", "シミュレーション年月日時分秒");

		paraMan.addParameter(警告);
		paraMan.addParameter(シミュレーション年月日時分秒);
		paraMan.addRunnable(()->{
			シミュレーション年月日時分秒.setValue(this.getSimulationStartTime().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss.SSS")));
		});
	}

	/*
	 * このSimulatorインスタンスが保持するParameterManagerを返す。
	 * */
	public ParameterManager getParameterManager() {
		return this.paraMan;
	}

	/*
	 * このSimulatorがシミュレーションを実行するのに必要とするパラメータを入力するためのDataInputFrameを展開する。
	 * @param
	 * width 展開するDataInputFrameの幅
	 * height 展開するDataInputFrameの縦幅
	 * */
	public void openDataInputFrame(int width, int height) {
		inputFrame = new DataInputFrame(this,width,height);
		monitor = new ProgressMonitor(inputFrame, "メッセージ", "ノート", 0, 100);
	}

	/*
	 * このSimulatorがシミュレーション実行終了後に行う処理を規定する。
	 * */
	@Override
	protected void done() {
		inputFrame.dispose();
		try {
			this.resultWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				if(resultWriter != null) {
					this.resultWriter.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}