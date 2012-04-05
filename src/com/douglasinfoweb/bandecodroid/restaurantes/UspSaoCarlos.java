package com.douglasinfoweb.bandecodroid.restaurantes;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

import com.douglasinfoweb.bandecodroid.Cardapio;
import com.douglasinfoweb.bandecodroid.Cardapio.Refeicao;
import com.douglasinfoweb.bandecodroid.Main;
import com.douglasinfoweb.bandecodroid.R;
import com.douglasinfoweb.bandecodroid.Restaurante;
import com.douglasinfoweb.bandecodroid.Util;

@SuppressWarnings("serial")
public class UspSaoCarlos extends Restaurante {
	boolean proximo;
	@Override
	public boolean atualizarCardapios(Main main) {
		Log.v("bandeco","ATUALIZAR");
		ArrayList<Cardapio> cardapios=new ArrayList<Cardapio>();
		try {
			String URL = "http://www.pcasc.usp.br/restaurante.xml";
			String XML = getXmlFromUrl(URL);
			Log.v("usp-saocarlos","XML: "+XML);
			Document doc = getDomElement(XML);
			Element root = doc.getDocumentElement();
			NodeList dias = root.getChildNodes();
			for (int i=0; i < dias.getLength(); i++) {
				Node dia = dias.item(i);
				DateTime ultimaData = new DateTime();
				for (int j=0; j < dia.getChildNodes().getLength(); j++) {
					Node noDoDia = dia.getChildNodes().item(j);
					if (noDoDia.getNodeName().equals("data")) {
						String[] dataSplited = getNodeValue(noDoDia).split("/");
						ultimaData = new DateTime(
								Integer.parseInt(dataSplited[2]),
								Integer.parseInt(dataSplited[1]),
								Integer.parseInt(dataSplited[0]), 
								0, 0 ,0);
					} else if (noDoDia.getNodeName().equals("almoco") || noDoDia.getNodeName().equals("jantar")) {
						Cardapio cardapio = new Cardapio();
						cardapio.setData(ultimaData);
						if (noDoDia.getNodeName().equals("almoco")) {
							cardapio.setRefeicao(Refeicao.ALMOCO);
						} else {
							cardapio.setRefeicao(Refeicao.JANTA);
						}
						for (int k=0; k < noDoDia.getChildNodes().getLength(); k++) {
							Node atributo = noDoDia.getChildNodes().item(k);
							if (atributo.getNodeName().equals("principal")) {
								cardapio.setPratoPrincipal(getNodeValue(atributo));
							} else if (atributo.getNodeName().equals("acompanhamento")) {
								cardapio.setPratoPrincipal(cardapio.getPratoPrincipal()+"\n"+getNodeValue(atributo));
							} else if (atributo.getNodeName().equals("salada")) {
								cardapio.setSalada(getNodeValue(atributo));
							} else if (atributo.getNodeName().equals("sobremesa")) {
								cardapio.setSobremesa(getNodeValue(atributo));
							}
						}
						if (cardapio.getPratoPrincipal() != null 
								&& Util.removerEspacosDuplicados(cardapio.getPratoPrincipal().trim()).length() > 2) {
							cardapios.add(cardapio);
						}
					}
				}
			}
			setCardapios(cardapios);
			removeCardapiosAntigos();
			main.save();
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
	}
	
	
	@Override
	public Boolean temQueAtualizar() {
		DateTime now = new DateTime(new Date());
		ArrayList<Cardapio> cardapios = getCardapios();
		if (cardapios.size() >= 1) {
			Cardapio ultimoCardapio = cardapios.get(cardapios.size() -1);
			//Se o ultimo que esta na memoria ainda eh dessa semana, nao precisa atualizar.
			if (ultimoCardapio.getData().getWeekOfWeekyear() >= now.getWeekOfWeekyear()
					&& ultimoCardapio.getData().getYear() >= now.getYear()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeCardapiosAntigos() {
		DateTime now = DateTime.now();
		for (Cardapio c : new ArrayList<Cardapio>(getCardapios())) {
			//Pra remover, tem que ser no minimo do mesmo dia
			if (c.getData().getDayOfYear() <= now.getDayOfYear() 
					&& c.getData().getYear() <= now.getYear()) {
				//Se for de dias que ja passaram, remove
				if (c.getData().getDayOfYear() < now.getDayOfYear()) { 
					getCardapios().remove(c);
				} else { //Se eh de hoje, ver se ja passou a hora do almo�o/janta
					switch (c.getRefeicao()) {
						case ALMOCO: if (now.getHourOfDay() >= 14) getCardapios().remove(c);
						case JANTA: if (now.getHourOfDay() >= 20) getCardapios().remove(c);
					}
				}
			}
		}
	}
	public final String getNodeValue( Node elem ) {
        Node child;
        if( elem != null){
            if (elem.hasChildNodes()){
                for( child = elem.getFirstChild(); child != null; child = child.getNextSibling() ){
                    if( child.getNodeType() == Node.TEXT_NODE  ){
                        return child.getNodeValue().replace("\n","");
                    }
                }
            }
        }
        return "";
 }    
	public String getXmlFromUrl(String url) throws IOException {
    	
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("User-Agent", "Mozilla");
        httpGet.setHeader("Accept","text/html");
        HttpResponse httpResponse = httpClient.execute(httpGet);
        HttpEntity httpEntity = httpResponse.getEntity();
        return EntityUtils.toString(httpEntity);
    }
	public Document getDomElement(String xml){
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
 
            DocumentBuilder db = dbf.newDocumentBuilder();
 
            InputSource is = new InputSource();
                is.setCharacterStream(new StringReader(xml));
                doc = db.parse(is); 
 
            } catch (ParserConfigurationException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            } catch (SAXException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                return null;
            }
                // return DOM
            return doc;
    }
	@Override
	public int getImagem() {
		return R.drawable.logo_usp_saocarlos;
	}

	@Override
	public String getNome() {
		return "USP S�o Carlos";
	}


	@Override
	public String getSite() {
		return "http://www.pcasc.usp.br/pop_cardapio.php";
	}
	
	  
}
