package openGL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModelHandler {
	public static ArrayList<Model> loadModelFile(String filePath,String extension) throws SAXException, IOException, ParserConfigurationException{
		ArrayList<Model> modelList;
		switch(extension) {
			case ".amf":
			case ".AMF":
			case "amf":
			case "AMF":
				modelList = createFromAMF(filePath);
				break;
			default:
				throw new IllegalArgumentException("Model.loadModelFile(String,String):指定されたファイルフォーマットは非対応です。");
		}

		return modelList;

		/*
		 * for(Element ele:AMFLoader.loadAMFFile("D:\\アセンブリ.AMF")) {
				System.out.println("materialid:"+ele.getAttribute("materialid"));
				NodeList eleChildList = ele.getChildNodes();
				for(int i=0;i<eleChildList.getLength();i++) {
					Node child1 = eleChildList.item(i);
					if(child1.getNodeType() != Node.ELEMENT_NODE) {
						continue;
					}
					if(child1.getNodeName().equals("mesh")) {

					}
				}
			}
		 * */
	}

	private static ArrayList<Model> createFromAMF(String filePath) throws SAXException, IOException, ParserConfigurationException{
		ArrayList<Model> modelList = new ArrayList<>();

		Element root = AMFLoader.loadAMFFile(filePath);
		NodeList rootChildren = root.getChildNodes();
		ArrayList<Element> objectList = new ArrayList<>();
		HashMap<String,Element> materialMap = new HashMap<>();

		for(int i=0;i<rootChildren.getLength();i++) {
			Node childNode = rootChildren.item(i);
			if(childNode.getNodeType() != Node.ELEMENT_NODE) {
				continue;
			}
			Element childElement = (Element) childNode;
			switch(childElement.getNodeName()) {
				case "object":
					objectList.add(childElement);
					break;
				case "material":
					materialMap.put(childElement.getAttribute("id"), childElement);
					break;
			}
		}

		for(Element objectElement:objectList) {
			//object毎の処理

			//mesh,colorの取得
			NodeList objectChildren = objectElement.getChildNodes();
			Element meshElement=null,colorElement=null;
			for(int i=0;i<objectChildren.getLength();i++) {
				Node childNode = objectChildren.item(i);
				if(childNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				switch(childNode.getNodeName()) {
					case "color":
						colorElement = (Element) childNode;
						break;
					case "mesh":
						meshElement = (Element) childNode;
						break;
				}
			}

			//vertices,volume,...の取得
			NodeList meshChildren = meshElement.getChildNodes();
			int verticesIndex=0;
			for(;verticesIndex<meshChildren.getLength();verticesIndex++) {
				if(meshChildren.item(verticesIndex).getNodeName().equals("vertices")) {
					break;
				}
			}
			//vertexの入ったList
			NodeList vertexList = meshChildren.item(verticesIndex).getChildNodes();
			for(int i=0;i<meshChildren.getLength();i++) {
				if(i == verticesIndex) {
					continue;
				}
				//volume毎に関する処理
				Element volumeElement = (Element) meshChildren.item(i);

				//Modelインスタンスを作っていく
				Model model = new Model();
				NodeList triangleList = volumeElement.getChildNodes();
				for(int j=0;j<triangleList.getLength();j++) {
					//三角形毎の処理
					Element triangleElement = (Element) triangleList.item(j);
					NodeList triangleContentList = triangleElement.getChildNodes();

					float vertexs[][] = new float[3][3];

					for(int vIndex=0;vIndex<3;vIndex++) {
						//三角形の頂点毎の処理
						Node vElement = triangleContentList.item(vIndex);
						Node vertexElement = vertexList.item(Integer.parseInt(vElement.getTextContent()));

						NodeList coordinateList = vertexElement.getFirstChild().getChildNodes();
						for(int xyz = 0; xyz<3;xyz++) {
							Node xyzElement = coordinateList.item(xyz);
							vertexs[vIndex][xyz] = Float.parseFloat(xyzElement.getTextContent());
						}
					}

					Triangle triangle = new Triangle(vertexs);
					//メッシュ情報をmodelに追加する
					model.addTriangle(triangle);
				}

				//material情報に関して取得していく
				String materialId = null;
				if(volumeElement.hasAttribute("materialid")) {
					materialId = volumeElement.getAttribute("materialid");
				}else if(objectElement.hasAttribute("materialid")){
					materialId = objectElement.getAttribute("materialid");
				}
				//materialの取得
				Element materialElement = materialMap.get(materialId);


				Element finalColorElement=null;
				if(colorElement == null) {
					//materialで色の指定があればそれを適用する
					//まずはその指定があるかを検索

					NodeList materialChildren = materialElement.getChildNodes();
					for(int mat=0;mat<materialChildren.getLength();mat++) {
						Node colorN = materialChildren.item(mat);
						if(colorN.getNodeName().equals("color")) {
							finalColorElement = (Element) colorN;
						}
					}
				}else {
					finalColorElement = colorElement;
				}

				if(finalColorElement == null) {
					//色の指定が無かった場合
					//勝手に決める
					model.red = 0.5f;
					model.blue = 0.5f;
					model.green = 0.5f;
				}else {
					for(int colorIndex=0;colorIndex<3;colorIndex++) {
						Node rgbN = finalColorElement.getChildNodes().item(colorIndex);
						float rgbValue = Float.parseFloat(rgbN.getTextContent());
						switch(rgbN.getNodeName()) {
							case "r":
								model.red = rgbValue;
								break;
							case "g":
								model.green = rgbValue;
								break;
							case "b":
								model.blue = rgbValue;
								break;
						}
					}
				}
				modelList.add(model);
			}

		}


		return modelList;
	}
}
