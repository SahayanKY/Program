package simulation.function.nurbs.refiner;

import simulation.function.nurbs.NURBSBasisFunction;
import simulation.function.nurbs.NURBSFunction;
import simulation.function.nurbs.NURBSFunctionGroup;
import simulation.function.nurbs.assertion.NURBSAsserter;

/**
 * NURBS関数の形状を変えずにノットや次数を変化させるクラス
 *
 * @version 2019/02/23 17:59
 * */
public class NURBSRefiner {

	/**
	 * 指定されたNURBS基底関数に対して、ノットを挿入します。
	 * 返されるインスタンスは指定されたものとは全く異なる参照をもちます。
	 * また、返されるNURBSFunctionインスタンスの順番は、指定された順番と対応しています。
	 * また、指定されたNURBS関数の状態は変化しません。
	 *
	 * @param group NURBS基底関数とコントロールポイントの組
	 * @param X 各変数方向のノットベクトルに挿入するノット配列
	 *
	 * @throws IllegalArgumentException
	 * <ul>
	 * 		<li>Xに含まれる値が、基底関数の定義域外であった場合
	 * 		<li>Xを追加することによって関数の連続性が保持されない場合
	 * 		<li>Xが単調増加列の配列の配列で無かった場合
	 * 		<li>Xの要素数がbasisの変数の数に一致しない場合。
	 * </ul>
	 * @version 2019/02/23 17:56
	 * */
	public NURBSFunctionGroup refineKnot(NURBSFunctionGroup group, double[][] X) {
		NURBSAsserter asserter = new NURBSAsserter(true);
		//Xが正常かどうかを調べる
		asserter.assertInsertedKnotVectorIsValid(group.basis,X);

		NURBSBasisFunction basis = group.basis;
		NURBSFunction[] funcs = group.funcs;

		//元のコントロールポイント数
		int n_All = basis.giveNumberOfAllCtrl();
		int[] n = basis.giveNumberArrayOfCtrl();
		//新しいコントロールポイントの数
		int Newn_All = 1;
		for(int varNum=0;varNum<basis.parameterNum;varNum++) {
			//X[varNum].length:ある変数方向のポイントの増加数
			Newn_All *= (n[varNum]+X[varNum].length);
		}

		//---------------------------------------------------------------------

		//ctrl:今のポイントの値をまとめたもの
		//NewCtrl:新しいポイントを保持するもの
		//[0][*][0]:重み
		//[i][1][d]:func[i]の1つ目のポイントのd座標

		//メモリ消費量については、
		//重みは元の配列、ここでctrl用に並び替えたもの、NewWeightで3つ分のメモリが必要
		//ポイントは元の配列、NewCtrlで2つ分のメモリが必要
		double[][][] ctrl = new double[funcs.length+1][][];
		double[][][] NewCtrl = new double[funcs.length+1][Newn_All][];
		double[] weight = basis.giveWeightArray_Shallow();

		ctrl[0] = new double[n_All][1];
		for(int i=0;i<n_All;i++) {
			ctrl[0][i][0] = weight[i];
		}
		for(int i_func=0;i_func<funcs.length;i_func++) {
			double[][] funcCtrls = funcs[i_func].giveCtrlArray_Shallow();
			ctrl[i_func] = funcCtrls;
		}

		for(int i_func=0;i_func<funcs.length;i_func++) {
			for(int i=0;i<Newn_All;i++) {
				NewCtrl[i_func][i] = new double[funcs[i_func].dimension];
			}
		}


		//---------------------------------------------------------------------

		//curentKnot:現在のノット
		//NewKnot:新しいノットを保持するもの
		double[][] currentKnot = basis.giveKnotVector_Shallow();
		double[][] NewKnot = new double[basis.parameterNum][];
		for(int varNum=0;varNum<basis.parameterNum;varNum++) {
			NewKnot[varNum] = new double[currentKnot[varNum].length +X[varNum].length];
		}

		//---------------------------------------------------------------------

		//p:基底関数の次数配列、精細化では次数が変化しないので、
		//後の関数のインスタンス化でも用いる
		int[] p = basis.giveDegreeArray();

		//新しいポイントとノットを計算する
		refineKnot(ctrl, NewCtrl, currentKnot, X, NewKnot, p);

		//---------------------------------------------------------------------

		//計算結果から、新しいNURBSBasisFunctionとNURBSFunctionを生成するために、
		//それらのコンストラクタの引数に必要なものを用意する
		//具体的にはコントロールポイントを適切な構造に組み替える

		//新しい重みの配列
		double[] NewWeight = new double[Newn_All];
		for(int i=0;i<Newn_All;i++) {
			NewWeight[i] = NewCtrl[0][i][0];
		}

		//---------------------------------------------------------------------
		//NURBSBasisFunctionとNURBSFunctionをインスタンス化し、Groupにまとめて返す
		NURBSBasisFunction NewBasis = new NURBSBasisFunction(NewKnot, p, NewWeight);
		NURBSFunction[] NewFuncs = new NURBSFunction[funcs.length];
		for(int i_func=0;i_func<funcs.length;i_func++) {
			NewFuncs[i_func] = new NURBSFunction(NewCtrl[i_func], NewBasis);
		}

		return new NURBSFunctionGroup(NewBasis, NewFuncs);
	}


	/**
	 * <p>TODO ポイント計算の実装を完了させる</p>
	 * <p>TODO 処理のコンテクスト毎にメソッドを分割する</p>
	 * <p>TODO 並列実装と逐次実装の両方を作る</p>
	 *
	 * ノットを精細化します。
	 * ctrlとknotとX、そしてpの要素の値は変えません。
	 * また、NewCtrlとNewKnotは全て初期化しておいてください。
	 *
	 * @param ctrl 基底関数の重み、各NURBS関数のコントロールポイントをまとめたもの
	 * @param NewCtrl 新しいポイントを保存する配列
	 * @param knot 基底関数のノットベクトル
	 * @param X 挿入するノット
	 * @param NewKnot 新しいノットベクトルを保存する配列
	 * @param p 基底関数の次数
	 *
	 * @throws IllegalArgumentException ノットを挿入した事により関数の不連続化が起こる場合
	 * @throws NullPointerException NewCtrl[*]やNewKnot[*]がnullの場合
	 * @version 2019/02/22 21:57
	 * */
	private void refineKnot(double[][][] ctrl, double[][][] NewCtrl, double[][] knot, double[][] X, double[][] NewKnot, int[] p) {
		//変数の数
		final int parameterNum = knot.length;

		for(int l=0;l<parameterNum;l++) {

			double[] Ul = knot[l];
			double[] Xl = X[l];
			double[] bUl = NewKnot[l];

			//X[l]をknot[l]に挿入した結果をNewKnot[l]に代入する
			//NewKnot[l]に挿入結果を作りながら、順次新しいポイントを計算していく
			int k_bef = -1,k_now;
			int i_Ul = 0, i_bUl = 0;
			int n_Xl = Xl.length, n_Ul = Ul.length;
			for(int i_Xl = 0;i_Xl < n_Xl ; i_Xl++) {
				//x_iXまでbUlを完成させる
				for(; Ul[i_Ul] <= Xl[i_Xl] ; i_Ul++,i_bUl++) {
					bUl[i_bUl] = Ul[i_Ul];
				}
				bUl[i_bUl] = Xl[i_Xl];
				k_now = i_bUl -1;
				i_bUl++;

				//---------------------------------------------------------------------

				//x_iXを挿入した事による新しいポイントを計算するのに必要な分だけ、
				//QiにPiを代入する

				for(int j = k_bef+1 ; j <= k_now ;j++) {
					//変数l以外の方向についてループさせる
					//代入
					//TODO 未実装
				}

				//---------------------------------------------------------------------

				//新しいポイントを内分計算

				for(int j = k_now ; j >= k_now -p[l]+1 ; j--) {
					double α = (Xl[i_Xl] -bUl[j])/(Ul[j-k_now+i_Ul+p[l]-1] -bUl[j]);

					//変数l以外の方向についてループさせる
					//内分計算
					//TODO 未実装

				}

				//---------------------------------------------------------------------

				k_bef = k_now;

			}

			//X[l]を全て代入し終わった

			//-------------------------------------------------------------------------

			//後方の変化しなかったポイントを代入する
			for(int i=k_bef+1 ; i < n_Ul+n_Xl ; i++) {
				//TODO 未実装
			}

			//-------------------------------------------------------------------------

			//残った元のノットを追加する
			for(;i_Ul < n_Ul ; i_Ul++, i_bUl++) {
				bUl[i_bUl] = Ul[i_Ul];
			}

		}

		//全てのノットを挿入し終わり、ポイントも計算し終わった
	}

}
