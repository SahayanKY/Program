package icg;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DataInputFrame extends JFrame implements ActionListener,FocusListener{
	private CardLayout LayoutOfPanel = new CardLayout();
	private JPanel CardPanel = new JPanel();

	private LinkedHashMap<String, LinkedHashMap<String,JTextField>> dataField = new LinkedHashMap<>();

	private static final String ShowCard = "showCard",
			StartCalculation = "StartCalculation",
			ChangeTextFieldColor = "ChangeTextFieldColor",
			SelectThrustDataFile = "SelectFile",
			SetExistingInputData = "SetInputData";

	DataInputFrame(){
		//フレームの設定
		setTitle("ICGシミュレーション");
		setBounds(250,150,800,500);
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		setComponent();
		setVisible(true);
	}

	/*
	 * このフレームインスタンスに貼り付けるコンポーネントの設定
	 * */
	private void setComponent() {
		ComponentSetter LayoutOfFrame = new ComponentSetter();
		setLayout(LayoutOfFrame);

		Font f = new Font(Font.SERIF, Font.BOLD, 20);

		//カードレイアウトパネルを追加
		CardPanel.setLayout(LayoutOfPanel);
		add(CardPanel);

		//カードレイアウトパネルに追加するパネルを作成
		//同時にtextFieldをメンバ変数のhashmapに登録しておく
		LinkedHashMap<String,LinkedHashMap<String,String>> map = Parameter.getEnumValueMap();
		int row=0;
		int splitNum = 2;
		JPanel card=null;
		ComponentSetter LayoutOfCards = new ComponentSetter();
		for(String Name:map.keySet()) {
			if(row%splitNum==0) {
				card = new JPanel();
				card.setLayout(LayoutOfCards);
				CardPanel.add(card, "Card"+row/splitNum);
			}
			//一番上に来る大まかなネームを貼り付け
			int y=0;
			JLabel label = new JLabel(Name);
			label.setFont(f);
			LayoutOfCards.setComponent(label, row%splitNum*2, y, 2, 1, 0.2d, 0.8d, GridBagConstraints.CENTER);
			card.add(label);

			LinkedHashMap<String,JTextField> TextFieldMap = new LinkedHashMap<>();
			//各パラメータをその下に貼り付けていく
			for(String parameterName:map.get(Name).keySet()) {
				y++;
				JLabel paramLabel = new JLabel(parameterName);
				LayoutOfCards.setComponent(paramLabel, row%splitNum*2, y, 1, 1, 0, 0.1,GridBagConstraints.EAST);
				card.add(paramLabel);

				JTextField text = new JTextField();
				text.setPreferredSize(new Dimension(150,30));
				text.setBackground(Color.RED);
				text.addFocusListener(this);
				text.setActionCommand(ChangeTextFieldColor);
				LayoutOfCards.setComponent(text, row%splitNum*2+1, y, 1, 1, 0, 0.2, GridBagConstraints.WEST);
				card.add(text);
				TextFieldMap.put(parameterName, text);

				if(parameterName.equals(Parameter.燃焼データファイル.childLabel)) {
					y++;
					JButton selectFileButton = new JButton("ファイルを選択");
					selectFileButton.addActionListener(this);
					selectFileButton.setActionCommand(SelectThrustDataFile);
					LayoutOfCards.setComponent(selectFileButton, row%splitNum*2+1, y, 1, 1, 0, 0.1, GridBagConstraints.WEST);
					card.add(selectFileButton);
				}
			}
			row++;

			dataField.put(Name, TextFieldMap);
		}
		//カードの枚数
		int cardNum = row/splitNum+1-(splitNum-row%splitNum)/splitNum;

		//入力値を既存のファイルから取得し、セットする
		JButton setInputDataButton = new JButton("既存のデータファイルからパラメータをセットする");
		setInputDataButton.addActionListener(this);
		setInputDataButton.setActionCommand(SetExistingInputData);
		LayoutOfFrame.setFill(GridBagConstraints.HORIZONTAL);
		LayoutOfFrame.setComponent(setInputDataButton, 0, 0, cardNum, 1 , 0, 0, GridBagConstraints.CENTER);
		add(setInputDataButton);

		//パネル変更用のボタンの作成
		for(int i=0;i<cardNum;i++) {
			JButton button = new JButton("パラメーターその"+(i+1));
			button.addActionListener(this);
			button.setActionCommand(ShowCard+i);
			LayoutOfFrame.setFill(GridBagConstraints.HORIZONTAL);
			LayoutOfFrame.setComponent(button, i,1,1,1,1,0,GridBagConstraints.CENTER);
			add(button);
		}

		JButton calcStartButton = new JButton("計算開始");
		calcStartButton.addActionListener(this);
		calcStartButton.setActionCommand(StartCalculation);
		LayoutOfFrame.setComponent(calcStartButton, 0, 3, cardNum, 1, 0, 0, GridBagConstraints.CENTER);
		add(calcStartButton);

		//ボタン数に応じてカードパネルの幅を調整
		LayoutOfFrame.setComponent(CardPanel, 0, 2, cardNum, 1, 0, 1.0d, GridBagConstraints.CENTER);
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if(cmd.startsWith(ShowCard)) {
			int n=Integer.parseInt(cmd.substring(8));
			LayoutOfPanel.show(CardPanel,"Card"+n);
			return;
		}
		switch(cmd) {
			case StartCalculation:
				try {
					//TextFieldにセットされている文字列をParameterに渡す
					LinkedHashMap<String,LinkedHashMap<String,String>> StringDataMap = new LinkedHashMap<>();
					for(String key:dataField.keySet()) {
						LinkedHashMap<String,String> deepMap = new LinkedHashMap<>();
						for(String deepKey:dataField.get(key).keySet()) {
							deepMap.put(deepKey, dataField.get(key).get(deepKey).getText());
						}
						StringDataMap.put(key, deepMap);
					}
					//データのチェックを行い、結果文字列を得る
					String message = Parameter.checkAllInputDataFormat(StringDataMap);
					//受け取った文字列に対応してダイアログを表示、処理を終了
					if(message != null) {
						if(message.startsWith("エラー")) {
							JOptionPane.showMessageDialog(this, message, "続行不能なエラーを検出", JOptionPane.ERROR_MESSAGE);
							break;
						}else if(message.startsWith("要検証")){
							int ans = JOptionPane.showOptionDialog(this, message, "入力値に不備を検出", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[]{"続行", "取消"}, null);
							if(ans == 1 | ans == -1) {
								//計算取消の場合
								break;
							}
						}
					}
					//保存先のディレクトリを選択、nullの場合は処理を終了
					File choosedDirectory = ChooseFileDialog.choose(this, ChooseFileDialog.ChooseTarget.DirectoryOnly, "D:\\ゆうき", "保存先のフォルダを選択");
					if(choosedDirectory == null) {
						break;
					}
					//指定したディレクトリにパラメータを保存させる
					Parameter.writeProperty_on(choosedDirectory);
					//このフレームの操作を不能にし、次のステージに進める
					this.setEnabled(false);
					new ProgressInformFrame();
				}catch(NullPointerException | IllegalArgumentException | IOException exc) {
					JOptionPane.showMessageDialog(this, exc, "エラー", JOptionPane.ERROR_MESSAGE);
				}
				break;

			case SelectThrustDataFile:
				File choosedFile = ChooseFileDialog.choose(this, ChooseFileDialog.ChooseTarget.ThrustFileOnly, "D:\\ゆうき", "燃焼データファイルを選択");
				//選択に失敗した場合
				if(choosedFile == null) {
					break;
				}
				Parameter parameter = Parameter.燃焼データファイル;
				JTextField thrustTF = dataField.get(parameter.parentLabel).get(parameter.childLabel);
				thrustTF.setText(choosedFile.toString().replace("\\", "/"));
				thrustTF.setBackground(Color.WHITE);

				break;

			case SetExistingInputData:
				choosedFile = ChooseFileDialog.choose(this, ChooseFileDialog.ChooseTarget.PropertiesFileOnly, "D:\\ゆうき\\大学\\プログラム\\Eclipse\\ICG_Simulation", "既存のプロパティファイルを選択");
				//選択に失敗した場合
				if(choosedFile == null) {
					break;
				}
				try {
					//ファイルを渡し、パラメータをセットさせ、その値をTextFieldにセットする
					Parameter.setData_by(choosedFile);
					LinkedHashMap<String,LinkedHashMap<String,String>> paramMap = Parameter.getEnumValueMap();
					for(String key:dataField.keySet()) {
						LinkedHashMap<String,JTextField> deepMap = dataField.get(key);
						for(String deepKey:deepMap.keySet()) {
							String data = paramMap.get(key).get(deepKey);
							JTextField targetTF = deepMap.get(deepKey);
							targetTF.setText(data);
							changeTextFieldColor(targetTF);
						}
					}
				}catch (IOException exc) {
					JOptionPane.showMessageDialog(this, exc, "ファイル操作のエラー", JOptionPane.ERROR_MESSAGE);
				}

				break;

		}
	}

	public void focusGained(FocusEvent e) {
	}
	public void focusLost(FocusEvent e){
		changeTextFieldColor((JTextField) e.getSource());
	}

	/*
	 * 指定されたTextFieldの入力値を取得し、nullまたは空文字なら赤に、
	 * そうでなければ白にJTextFieldの色を変える
	 * */
	private void changeTextFieldColor(JTextField tf) {
		String inputStr = tf.getText();
		if(inputStr == null | inputStr.equals("") | inputStr.matches("[ 　]+")) {
			tf.setBackground(Color.RED);
		}else {
			tf.setBackground(Color.WHITE);
		}
	}
}