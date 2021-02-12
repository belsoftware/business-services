package com.tarento.analytics.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tarento.analytics.constant.Constants;
import com.tarento.analytics.constant.ErrorCode;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.RequestDto;
import com.tarento.analytics.dto.RoleDto;
import com.tarento.analytics.dto.UserDto;
import com.tarento.analytics.exception.AINException;
import com.tarento.analytics.org.service.ClientServiceFactory;
import com.tarento.analytics.service.AmazonS3ClientService;
import com.tarento.analytics.service.MetadataService;
import com.tarento.analytics.utils.PathRoutes;
import com.tarento.analytics.utils.ResponseGenerator;

@RestController
@RequestMapping(PathRoutes.DashboardApi.DASHBOARD_ROOT_PATH)
public class DashboardController {

	public static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

	@Autowired
	private MetadataService metadataService;
	@Autowired
	private AmazonS3ClientService amazonS3ClientService;
	
    @Value("#{'${allowed.visualization.codes}'.split(',')}")
    private String[] allowedVisualizationCodes;
    
    @Value("#{'${allowed.visualization.types}'.split(',')}")
    private String[] allowedVisualizationTypes;
    
    private List<String> allowedVisualizationCodeList = new ArrayList<String>();
    private List<String> allowedVisualizationList = new ArrayList<String>();
/*    @Autowired
	private ClientService clientService;*/

	@Autowired
	private ClientServiceFactory clientServiceFactory;
	
    @PostConstruct
    public void loadUrls() {
    	allowedVisualizationCodeList = Arrays.asList(allowedVisualizationCodes);
    	allowedVisualizationList = Arrays.asList(allowedVisualizationTypes);
    }

	@RequestMapping(value = PathRoutes.DashboardApi.FILE_PATH, method = RequestMethod.POST)
	public Map<String, String> uploadFile(@RequestPart(value = "file") MultipartFile file)
	{
		Map<String, String> response = new HashMap<>();
		try{
			String imgUrl = this.amazonS3ClientService.uploadFileToS3Bucket(file, true);
			response.put("message", "file [" + file.getOriginalFilename() + "] uploading request submitted successfully.");
			response.put("url", imgUrl);
		}catch (Exception e){
			logger.error("S3 file upload : "+e.getMessage());
			response.put("message", e.getMessage());
			response.put("url", "");
		}

		return response;
	}

	@DeleteMapping(value = PathRoutes.DashboardApi.FILE_PATH)
	public Map<String, String> deleteFile(@RequestParam("file_name") String fileName)
	{
		Map<String, String> response = new HashMap<>();
		try{
			this.amazonS3ClientService.deleteFileFromS3Bucket(fileName);
			response.put("message", "file [" + fileName + "] removing request submitted successfully.");
		}catch (Exception e ){
			logger.error("S3 file upload : "+e.getMessage());
			response.put("message", e.getMessage());

		}
		return response;

	}

	@GetMapping(value = PathRoutes.DashboardApi.TEST_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTest() throws JsonProcessingException {
		return ResponseGenerator.successResponse("success");

	}

	@RequestMapping(value = PathRoutes.DashboardApi.GET_DASHBOARD_CONFIG + "/{dashboardId}", method = RequestMethod.GET)
	public String getDashboardConfiguration(@PathVariable String dashboardId, @RequestParam(value="catagory", required = false) String catagory, @RequestHeader(value = "x-user-info", required = false) String xUserInfo)
			throws AINException, IOException {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		UserDto user = new UserDto();
		user.setId(new Long("10007"));
		user.setOrgId("1");
		user.setCountryCode("");
		RoleDto role = new RoleDto();
		role.setId(new Long("6"));
		role.setName("HR User");
		List<RoleDto> roles = new ArrayList<>();
		roles.add(role);
		user.setRoles(roles);
		//gson.fromJson(xUserInfo, UserDto.class);

		return ResponseGenerator.successResponse(metadataService.getDashboardConfiguration(dashboardId, catagory, user.getRoles()));
	}

	@RequestMapping(value = PathRoutes.DashboardApi.GET_CHART_V2, method = RequestMethod.POST)
	public String getVisualizationChartV2( @RequestBody RequestDto requestDto, @RequestHeader(value = "x-user-info", required = false) String xUserInfo, ServletWebRequest request)
			throws IOException {
		return getVisualizationChartV2Api( requestDto, xUserInfo, request);
	}
	
	@RequestMapping(value = PathRoutes.DashboardApi.GET_CHART_OPEN, method = RequestMethod.POST)
	public String getVisualizationChartOpen( @RequestBody RequestDto requestDto, @RequestHeader(value = "x-user-info", required = false) String xUserInfo, ServletWebRequest request)
			throws IOException {
		AggregateRequestDto requestInfo = requestDto.getAggregationRequestDto();
		if(allowedVisualizationCodeList.contains(requestInfo.getVisualizationCode()) && allowedVisualizationList.contains(requestInfo.getVisualizationType()))
		{
			return getVisualizationChartV2Api( requestDto, xUserInfo, request);
		}
		else
		{
			return ResponseGenerator.failureResponse("UNAUTHORISED", "UNAUTHORISED");
		}
	}
	
	private String getVisualizationChartV2Api( RequestDto requestDto, String xUserInfo, ServletWebRequest request) throws IOException
	{
		/*logger.info("Request Detail:" + requestDto);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		UserDto user = gson.fromJson(xUserInfo, UserDto.class);*/

		UserDto user = new UserDto();
		logger.info("user"+xUserInfo);

		//Getting the request information only from the Full Request
		AggregateRequestDto requestInfo = requestDto.getAggregationRequestDto();
		Map<String, Object> headers = requestDto.getHeaders();
		//requestInfo.getFilters().putAll(headers);
		String response = "";
		try {
			if (headers.isEmpty()) {
				logger.error("Please provide header details");
				throw new AINException(ErrorCode.ERR320, "header is missing");
			}
			if (headers.get("tenantId") == null) {
				logger.error("Please provide tenant ID details");
				throw new AINException(ErrorCode.ERR320, "tenant is missing");

			}
			
			if(requestDto.getAggregationRequestDto() == null) { 
				logger.error("Please provide requested Visualization Details");
				throw new AINException(ErrorCode.ERR320, "Visualization Request is missing");
			}
			/*if(requestDto.getAggregationRequestDto().getRequestId() == null) { 
				logger.error("Please provide Request ID");
				throw new AINException(ErrorCode.ERR320, "Request ID is missing. Insights will not work");
			}*/


			// To be removed once the development is complete
			if(StringUtils.isBlank(requestInfo.getModuleLevel())) {
				requestInfo.setModuleLevel(Constants.Modules.HOME_REVENUE);
			}

			Object responseData = clientServiceFactory.get(requestInfo.getVisualizationCode()).getAggregatedData(requestInfo, user.getRoles());
			response = ResponseGenerator.successResponse(responseData);

		} catch (AINException e) {
			logger.error("error while executing api getVisualizationChart");
			response = ResponseGenerator.failureResponse(e.getErrorCode(), e.getErrorMessage());
		}
		return response;
	
	}
	
/*
	@RequestMapping(value = PathRoutes.DashboardApi.GET_CHART_V3, method = RequestMethod.POST)
	public String getVisualizationChartV3(@RequestBody RequestDtoV3 requestDtoV3, @RequestHeader(value = "x-user-info", required = false) String xUserInfo, ServletWebRequest request)
			throws IOException {

		*/
/*logger.info("Request Detail:" + requestDto);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		UserDto user = gson.fromJson(xUserInfo, UserDto.class);*//*


		UserDto user = new UserDto();
		logger.info("user"+xUserInfo);

		//Getting the request information only from the Full Request
		AggregateRequestDtoV3 requestInfoV3 = requestDtoV3.getAggregationRequestDto();
		Map<String, Object> headers = requestDtoV3.getHeaders();
		//requestInfo.getFilters().putAll(headers);
		String response = "";
		try {
			if (headers.isEmpty()) {
				logger.error("Please provide header details");
				throw new AINException(ErrorCode.ERR320, "header is missing");
			}
			if (headers.get("tenantId") == null) {
				logger.error("Please provide tenant ID details");
				throw new AINException(ErrorCode.ERR320, "tenant is missing");
			}
			// To be removed once the development is complete
			if(StringUtils.isBlank(requestInfoV3.getModuleLevel())) {
				requestInfoV3.setModuleLevel(Constants.Modules.HOME_REVENUE);
			}

			List<Object> responseDataList = new ArrayList<>(); 
			if(requestInfoV3 !=null && requestInfoV3.getVisualizations() != null && requestInfoV3.getVisualizations().size() > 0) {
				for (int i = 0; i < requestInfoV3.getVisualizations().size(); i++) {
					AggregateRequestDto requestInfo = new AggregateRequestDto(requestInfoV3,
							requestInfoV3.getVisualizations().get(i).getType(), requestInfoV3.getVisualizations().get(i).getCode());
					Object responseData = clientService.getAggregatedData(requestInfo, user.getRoles());
					responseDataList.add(responseData); 
				}
				
			}
			response = ResponseGenerator.successResponse(responseDataList);
		} catch (AINException e) {
			logger.error("error while executing api getVisualizationChart");
			response = ResponseGenerator.failureResponse(e.getErrorCode(), e.getErrorMessage());
		}
		return response;
	}
*/



}
