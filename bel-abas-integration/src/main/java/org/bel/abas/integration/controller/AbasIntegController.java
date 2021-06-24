package org.bel.abas.integration.controller;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.bel.abas.integration.model.RequestInfoWrapper;
import org.bel.abas.integration.repository.ABASRepository;
import org.bel.abas.integration.utils.AbasIntegUtil;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.jayway.jsonpath.DocumentContext;

@RestController
@RequestMapping("v1/")
public class AbasIntegController {

	@Autowired
    private ABASRepository abasRepository;
	
	@Autowired
    private AbasIntegUtil util;
	
	@RequestMapping(value = { "_transactionReversal"}, method = RequestMethod.POST)
	public ResponseEntity<String>  deleteDeathImport(@RequestParam String json) {
		abasRepository.saveSharedData(json,"ABAS","TRANSACTION_REVERSAL_RECEIVE");
        return new ResponseEntity<>("Saved " , HttpStatus.OK);
    }
	
	/*@RequestMapping(value = { "_taxHeadGLCodeMapList"}, method = RequestMethod.POST)
	public ResponseEntity<String>  taxHeadGLCodeMapList(@RequestParam String tenantId , 
			@RequestBody RequestInfoWrapper requestInfoWrapper) {
		//Map<String, String> taxHeadGLCodeMaps = new HashMap<String, String>();
		JsonArray jsonArray = new JsonArray();
		try {
    		MdmsCriteriaReq mdmsReqTaxHead = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList(util.TAXHEADMASTER),null,
    				requestInfoWrapper.getRequestInfo());
    		DocumentContext mdmsDataTaxHead = util.getAttributeValues(mdmsReqTaxHead);
    		List<String> taxHeadServices = mdmsDataTaxHead.read(util.BS_TAXHEAD_SERVICE_PATH);
    		List<String> taxHeadCodes = mdmsDataTaxHead.read("$.MdmsRes.BillingService.TaxHeadMaster.*.code");
    		System.out.println(taxHeadCodes.size()+" <--code --size-- service--> "+taxHeadServices.size());
    		if(taxHeadServices.size()>0) {
    			for(int i=0; i<(taxHeadServices.size()/700); i++) {
    				System.out.println("i "+i);
		    		MdmsCriteriaReq mdmsReqGLCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList(util.GLCODE), "[?(@.code == '"+taxHeadServices.get(i)+"')]",
		    				requestInfoWrapper.getRequestInfo());
		    		DocumentContext mdmsDataGLCode = util.getAttributeValues(mdmsReqGLCode);
		    		List<String> glCodes = mdmsDataGLCode.read(util.BS_GLCODE_PATH);
		    		if(glCodes.size()>0) {
		    			taxHeadGLCodeMaps.put(taxHeadCodes.get(i), glCodes.get(0));
		    		}else {
		    			taxHeadGLCodeMaps.put(taxHeadCodes.get(i), "NA");
		    		}
    			
		    		MdmsCriteriaReq mdmsReqGLCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList(util.GLCODE), "[?(@.code == '"+taxHeadServices.get(i)+"')]",
		    				requestInfoWrapper.getRequestInfo());
		    		DocumentContext mdmsDataGLCode = util.getAttributeValues(mdmsReqGLCode);
		    		List<String> glCodes = mdmsDataGLCode.read(util.BS_GLCODE_PATH);
		    		JsonObject jsObject = new JsonObject();
		    		jsObject.addProperty("taxHeadCode", taxHeadCodes.get(i));
		    		jsObject.addProperty("taxHeadService", taxHeadServices.get(i));
		    		if(glCodes.size()>0) {
		    			jsObject.addProperty("glCode", glCodes.get(0));
		    		}else {
		    			jsObject.addProperty("glCode", "NA");
		    		}
		    		
		    		MdmsCriteriaReq mdmsReqBSCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList("BusinessService"), "[?(@.code == '"+taxHeadServices.get(i)+"')]",
		    				requestInfoWrapper.getRequestInfo());
		    		DocumentContext mdmsDataBSCode = util.getAttributeValues(mdmsReqBSCode);
		    		List<String> businessServices = mdmsDataBSCode.read("$.MdmsRes.BillingService.BusinessService.*.businessService");
		    		if(businessServices.size()>0) {
		    			jsObject.addProperty("businessService", businessServices.get(0));
		    		}else {
		    			jsObject.addProperty("businessService", "NA");
		    		}
		    		jsonArray.add(jsObject);
    			}
    		}
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
        return new ResponseEntity<>(new Gson().toJson(jsonArray) , HttpStatus.OK);
    }
	
	@RequestMapping(value = { "_taxHeadGLCodeMapListExcel"}, method = RequestMethod.POST)
	public ResponseEntity<String>  _taxHeadGLCodeMapListExcel(@RequestParam String tenantId , 
			@RequestBody RequestInfoWrapper requestInfoWrapper) {
		try {
    		MdmsCriteriaReq mdmsReqTaxHead = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList(util.TAXHEADMASTER),null,
    				requestInfoWrapper.getRequestInfo());
    		DocumentContext mdmsDataTaxHead = util.getAttributeValues(mdmsReqTaxHead);
    		List<String> taxHeadServices = mdmsDataTaxHead.read(util.BS_TAXHEAD_SERVICE_PATH);
    		List<String> taxHeadCodes = mdmsDataTaxHead.read("$.MdmsRes.BillingService.TaxHeadMaster.*.code");
    		System.out.println(taxHeadCodes.size()+" <--code --size-- service--> "+taxHeadServices.size());
    		if(taxHeadServices.size()>0) {
    			XSSFWorkbook workbook = new XSSFWorkbook();
    	        XSSFSheet sheet = workbook.createSheet("finaldata");
    			for(int i=0; i<taxHeadServices.size(); i++) {
    				System.out.println("i "+i);
    				Row row = sheet.createRow(i+1);
    				int columnCount = 0;
		    		
    				MdmsCriteriaReq mdmsReqGLCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList(util.GLCODE), "[?(@.code == '"+taxHeadServices.get(i)+"')]",
		    				requestInfoWrapper.getRequestInfo());
		    		DocumentContext mdmsDataGLCode = util.getAttributeValues(mdmsReqGLCode);
		    		List<String> glCodes = mdmsDataGLCode.read(util.BS_GLCODE_PATH);

		    		Cell cell1 = row.createCell(++columnCount);
		    		cell1.setCellValue(taxHeadCodes.get(i));
		    		
		    		Cell cell2 = row.createCell(++columnCount);
		    		cell2.setCellValue(taxHeadServices.get(i));
		    		
		    		Cell cell3 = row.createCell(++columnCount);
		    		if(glCodes.size()>0) {
		    			cell3.setCellValue(glCodes.get(0));
		    		}else {
		    			cell3.setCellValue("NA");
		    		}
		    		
		    		MdmsCriteriaReq mdmsReqBSCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE, Arrays.asList("BusinessService"), "[?(@.code == '"+taxHeadServices.get(i)+"')]",
		    				requestInfoWrapper.getRequestInfo());
		    		DocumentContext mdmsDataBSCode = util.getAttributeValues(mdmsReqBSCode);
		    		List<String> businessServices = mdmsDataBSCode.read("$.MdmsRes.BillingService.BusinessService.*.businessService");
		    		
		    		Cell cell4 = row.createCell(++columnCount);
		    		Cell cell5 = row.createCell(++columnCount);
		    		if(businessServices.size()>0) {
		    			String[] splitArr = businessServices.get(0).split("\\.");
		    			cell4.setCellValue(splitArr[0]);
		    			if(splitArr.length>1)
		    				cell5.setCellValue(splitArr[1]);
		    		}else {
		    			cell4.setCellValue("NA");
		    		}
    			}
    			try (FileOutputStream outputStream = new FileOutputStream("D:\\project_docs\\ABAS\\work\\"+tenantId+".xlsx")) {
    	            workbook.write(outputStream);
    	        } catch (FileNotFoundException e) {
    				e.printStackTrace();
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
        return new ResponseEntity<>("Success" , HttpStatus.OK);
    }
	
	
	@RequestMapping(value = { "_taxHeadGLCodeMapListJson"}, method = RequestMethod.POST)
	public ResponseEntity<String>  _taxHeadGLCodeMapListJson(
			@RequestBody RequestInfoWrapper requestInfoWrapper) {
		JsonArray jsonArray = new JsonArray();
		String tenantId = "pb";
		try {
			File file = new File("D:\\project_docs\\ABAS\\work\\newGLCode\\CB Agra\\mCollectUpdatedList.xlsx");
			Map<String, String> businessGLCodeMap = new HashMap<String, String>();
			try (Workbook wb = WorkbookFactory.create(file)) {
				Sheet sheet = wb.getSheetAt(0);
				Iterator<Row> itr = sheet.iterator();
				while (itr.hasNext()) {
					Row row = itr.next();
					if (row.getRowNum() >= 0) {
						break;
					}
				}
				while (itr.hasNext()) {
					Row row = itr.next();
					Iterator<Cell> cellIterator = row.cellIterator();
					String key = "";
					String value = "";

					while (cellIterator.hasNext()) {
						Cell cell = cellIterator.next();
						int index = cell.getColumnIndex();
						switch (index) {
						case 1:
							key += cell.getStringCellValue();
							break;
						case 2:
							key += "." + cell.getStringCellValue();
							break;
						case 3:
							value += cell.getStringCellValue();
							break;
						case 4:
							value = value.equalsIgnoreCase("NA") ? "NA" : value+" - " + cell.getStringCellValue();
							break;
						default:
							break;
						}
					}
					if (key.length() > 1 && (value.length() >3 || value.equalsIgnoreCase("NA")))
						businessGLCodeMap.put(key, value);
				}

				MdmsCriteriaReq mdmsReqTaxHead = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE,
						Arrays.asList(util.TAXHEADMASTER), null, requestInfoWrapper.getRequestInfo());
				DocumentContext mdmsDataTaxHead = util.getAttributeValues(mdmsReqTaxHead);
				List<String> taxHeadServices = mdmsDataTaxHead.read(util.BS_TAXHEAD_SERVICE_PATH);
				List<String> taxHeadCodes = mdmsDataTaxHead.read("$.MdmsRes.BillingService.TaxHeadMaster.*.code");
				System.out.println(taxHeadCodes.size() + " <--code --size-- service--> " + taxHeadServices.size());
				if (taxHeadServices.size() > 0) {
					for (int i = 0; i < taxHeadServices.size(); i++) {
						System.out.println("i " + i);
						MdmsCriteriaReq mdmsReqBSCode = util.prepareMdMsRequest(tenantId, util.BILLINGSERVICE,
								Arrays.asList("BusinessService"), "[?(@.code == '" + taxHeadServices.get(i) + "')]",
								requestInfoWrapper.getRequestInfo());
						DocumentContext mdmsDataBSCode = util.getAttributeValues(mdmsReqBSCode);
						List<String> businessServices = mdmsDataBSCode
								.read("$.MdmsRes.BillingService.BusinessService.*.businessService");

						if (businessServices.size() > 0) {
							if (null != businessGLCodeMap.get(businessServices.get(0))) {

								JsonObject jsObject = new JsonObject();
								jsObject.addProperty("code", taxHeadCodes.get(i));
								if (taxHeadCodes.get(i).contains("_FIELD_FEE"))
									jsObject.addProperty("glcode",
											businessGLCodeMap.get("mCollect.Field Fee/Field Verification Fee"));
								else if (taxHeadCodes.get(i).contains("_CGST"))
									jsObject.addProperty("glcode", businessGLCodeMap.get("mCollect.CGST"));
								else if (taxHeadCodes.get(i).contains("_SGST"))
									jsObject.addProperty("glcode", businessGLCodeMap.get("mCollect.SGST"));
								else if (taxHeadCodes.get(i).contains("_SEC_DEP"))
									jsObject.addProperty("glcode", businessGLCodeMap.get("mCollect.Security Deposit"));
								else
									jsObject.addProperty("glcode", businessGLCodeMap.get(businessServices.get(0)));
								jsObject.addProperty("dept", "null");
								jsObject.addProperty("fund", "null");
								jsonArray.add(jsObject);
							}
						}
					}
				}
			}
			
			FileWriter myWriter = new FileWriter(file.getParent() + "\\GLCodes.json");
			myWriter.write("{\"tenantId\": \"pb.agra\",\"moduleName\": \"BillingService\",\"GLCodes\": ");
			myWriter.write(new Gson().toJson(jsonArray));
			myWriter.write("}");
			myWriter.close();

			FileReader fr = new FileReader(file.getParent() + "\\GLCodes.json");
			String s;
			String totalStr = "";
			BufferedReader br = new BufferedReader(fr);

			while ((s = br.readLine()) != null) {
				totalStr += s;
			}
			totalStr = totalStr.replaceAll("\"null\"", "null");
			FileWriter fw = new FileWriter(file.getParent() + "\\GLCodes.json");
			fw.write(totalStr);
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(new Gson().toJson(jsonArray), HttpStatus.OK);
	}*/
	
	@RequestMapping(value = { "_taxHeadGLCodeMapListJsonFinal"}, method = RequestMethod.POST)
	public ResponseEntity<String>  _taxHeadGLCodeMapListJsonFinal(
			@RequestBody RequestInfoWrapper requestInfoWrapper) {

		ArrayList<String> tenantIds = new ArrayList<String>(Arrays.asList("pb.testing", "pb.agra", "pb.ahmedabad",
				"pb.ahmednagar", "pb.ajmer", "pb.allahabad", "pb.almora", "pb.ambala", "pb.amritsar", "pb.aurangabad",
				"pb.babina", "pb.badamibagh", "pb.bakloh", "pb.bareilly", "pb.barrackpore", "pb.belgaum",
				"pb.cannanore", "pb.chakrata", "pb.clementtown", "pb.dagshai", "pb.dalhousie", "pb.danapur",
				"pb.dehradun", "pb.dehuroad", "pb.delhi", "pb.deolali", "pb.faizabad", "pb.fatehgarh", "pb.ferozepur",
				"pb.jabalpur", "pb.jalandhar", "pb.jalapahar", "pb.jammu", "pb.jhansi", "pb.jutogh", "pb.kamptee",
				"pb.kanpur", "pb.kasauli", "pb.khasyol", "pb.kirkee", "pb.landour", "pb.lansdowne", "pb.lebong",
				"pb.lucknow", "pb.mathura", "pb.meerut", "pb.mhow", "pb.morar", "pb.nainital", "pb.nasirabad",
				"pb.pachmarhi", "pb.pune", "pb.ramgarh", "pb.ranikhet", "pb.roorkee", "pb.saugor", "pb.secunderabad",
				"pb.shahjahanpur", "pb.shillong", "pb.stm", "pb.subathu", "pb.varanasi", "pb.wellington"));

		try {
			MdmsCriteriaReq mdmsReqTaxHead = util.prepareMdMsRequest("pb", util.BILLINGSERVICE,
					Arrays.asList(util.TAXHEADMASTER), null, requestInfoWrapper.getRequestInfo());
			DocumentContext mdmsDataTaxHead = util.getAttributeValues(mdmsReqTaxHead);
			List<String> taxHeadServices = mdmsDataTaxHead.read(util.BS_TAXHEAD_SERVICE_PATH);
			Map<String, String> taxHeadBusinessServiceMap = new HashMap<String, String>();
			List<String> taxHeadCodes = mdmsDataTaxHead.read("$.MdmsRes.BillingService.TaxHeadMaster.*.code");
			if (taxHeadServices.size() > 0) {
				for (int i = 0; i < taxHeadServices.size(); i++) {
					System.out.println("i " + i);
					MdmsCriteriaReq mdmsReqBSCode = util.prepareMdMsRequest("pb", util.BILLINGSERVICE,
							Arrays.asList("BusinessService"), "[?(@.code == '" + taxHeadServices.get(i) + "')]",
							requestInfoWrapper.getRequestInfo());
					DocumentContext mdmsDataBSCode = util.getAttributeValues(mdmsReqBSCode);
					List<String> businessServices = mdmsDataBSCode
							.read("$.MdmsRes.BillingService.BusinessService.*.businessService");

					if (businessServices.size() > 0) {
						taxHeadBusinessServiceMap.put(taxHeadServices.get(i), businessServices.get(0));
					}
				}
			}
			System.out.println("taxHeadBusinessServiceMap " + taxHeadBusinessServiceMap.size());
			String rootDir = "D:\\project_docs\\ABAS\\work\\newGLCode\\Inputs";
			Path start = Paths.get(rootDir);
			try (Stream<Path> stream = Files.walk(start, 3)) {
				List<String> collect = stream.filter(Files::isRegularFile).map(String::valueOf).sorted()
						.collect(Collectors.toList());
				for (String filepath : collect) {
					String ext = FilenameUtils.getExtension(filepath);
					if (ext.equalsIgnoreCase("xlsx")) {
						File file = new File(filepath);
						String tenantId = file.getParent().split("\\\\")[file.getParent().split("\\\\").length - 1]
								.toLowerCase().replace("cb ", "pb.");
						if (tenantIds.contains(tenantId)) {
							System.out.println("tenant " + tenantId);
							JsonArray jsonArray = new JsonArray();
							Map<String, String> businessGLCodeMap = new HashMap<String, String>();
							try (Workbook wb = WorkbookFactory.create(file)) {
								Sheet sheet = wb.getSheetAt(0);
								Iterator<Row> itr = sheet.iterator();
								while (itr.hasNext()) {
									Row row = itr.next();
									if (row.getRowNum() >= 0) {
										break;
									}
								}
								while (itr.hasNext()) {
									Row row = itr.next();
									Iterator<Cell> cellIterator = row.cellIterator();
									String key = "";
									String value = "";

									while (cellIterator.hasNext()) {
										Cell cell = cellIterator.next();
										int index = cell.getColumnIndex();
										switch (index) {
										case 1:
											key += getStringVal(cell);
											break;
										case 2:
											key += "." + getStringVal(cell);
											break;
										case 3:
											value += getStringVal(cell);
											break;
										case 4:
											value = value.equalsIgnoreCase("NA") ? "NA"
													: value + " - " + getStringVal(cell);
											break;
										default:
											break;
										}
									}
									if (key.length() > 1 && (value.length() > 3 || value.equalsIgnoreCase("NA")))
										businessGLCodeMap.put(key, value);
								}

								for (int i = 0; i < taxHeadServices.size(); i++) {
									if (null != businessGLCodeMap
											.get(taxHeadBusinessServiceMap.get(taxHeadServices.get(i)))) {
										JsonObject jsObject = new JsonObject();
										jsObject.addProperty("code", taxHeadCodes.get(i));
										if (taxHeadCodes.get(i).contains("_FIELD_FEE"))
											jsObject.addProperty("glcode", businessGLCodeMap
													.get("mCollect.Field Fee/Field Verification Fee") == null ? "null"
															: businessGLCodeMap
																	.get("mCollect.Field Fee/Field Verification Fee"));
										else if (taxHeadCodes.get(i).contains("_CGST"))
											jsObject.addProperty("glcode",
													businessGLCodeMap.get("mCollect.CGST") == null ? "null"
															: businessGLCodeMap.get("mCollect.CGST"));
										else if (taxHeadCodes.get(i).contains("_SGST"))
											jsObject.addProperty("glcode",
													businessGLCodeMap.get("mCollect.SGST") == null ? "null"
															: businessGLCodeMap.get("mCollect.SGST"));
										else if (taxHeadCodes.get(i).contains("_SEC_DEP"))
											jsObject.addProperty("glcode",
													businessGLCodeMap.get("mCollect.Security Deposit") == null ? "null"
															: businessGLCodeMap.get("mCollect.Security Deposit"));
										else
											jsObject.addProperty("glcode", businessGLCodeMap
													.get(taxHeadBusinessServiceMap.get(taxHeadServices.get(i))));
										jsObject.addProperty("dept", "null");
										jsObject.addProperty("fund", "null");
										jsonArray.add(jsObject);
									}
								}
							}

							String outPutFilePath = "E:\\repository\\e-gov\\egov-mdms-data\\data\\pb\\"
									+ tenantId.split("\\.")[1] + "\\BillingService\\GLCodes.json";

							String finalStr = "{\"tenantId\": \"" + tenantId
									+ "\",\"moduleName\": \"BillingService\",\"GLCodes\": ";
							finalStr += new Gson().toJson(jsonArray).replaceAll("\"null\"", "null");
							finalStr += "}";

							ObjectMapper mapper = new ObjectMapper();
							Object json = mapper.readValue(finalStr, Object.class);
							String indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);

							FileWriter myWriter = new FileWriter(outPutFilePath);
							myWriter.write(indented);
							myWriter.close();

							try {
								JsonParser parser = new JsonParser();
								parser.parse(indented);
								// System.out.println("Valid json : "+tenantId);
							} catch (JsonSyntaxException jse) {
								System.out.println("Not a valid Json String: " + tenantId);
							}

						} else {
							System.out.println("Invalid tenantId " + tenantId);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>("Success", HttpStatus.OK);
	}
	
	private static String getStringVal(Cell cell)
    {
    	return cell.getCellType() == CellType.NUMERIC ? String.valueOf(cell.getNumericCellValue()) : cell.getStringCellValue();
    }
}
