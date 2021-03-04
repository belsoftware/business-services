package com.tarento.analytics.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.dto.AggregateDto;
import com.tarento.analytics.dto.AggregateRequestDto;
import com.tarento.analytics.dto.Data;
import com.tarento.analytics.dto.Plot;
/**
 * This handles ES response for single index, multiple index to represent data as pie figure
 * Creates plots by merging/computing(by summation) index values for same key
 * AGGS_PATH : this defines the path/key to be used to search the tree
 * VALUE_TYPE : defines the data type for the value formed, this could be amount, percentage, number
 *
 */
@Component
public class PieChartResponseHandler implements IResponseHandler {
    public static final Logger logger = LoggerFactory.getLogger(PieChartResponseHandler.class);


    @Override
    public AggregateDto translate(AggregateRequestDto requestDto, ObjectNode aggregations) throws IOException {

        List<Data> dataList = new ArrayList<>();

        JsonNode aggregationNode = aggregations.get(AGGREGATIONS);
        JsonNode chartNode = requestDto.getChartNode();
        String headerKey = chartNode.get(CHART_NAME).asText();
        List<Plot> headerPlotList = new ArrayList<>();
        List<Double> totalValue = new ArrayList<>();

        String symbol = chartNode.get(IResponseHandler.VALUE_TYPE).asText();
        ArrayNode aggrsPaths = (ArrayNode) chartNode.get(IResponseHandler.AGGS_PATH);
        
      //temporary fix. Should be integrated with the localisation data
        if(requestDto.getVisualizationCode().equals("mcTotalCollectionCategoryWise"))
        {
        	Map<String, String> localisationValues = getLocalisationValues();
        	Map<String, Double> createdBuckets = new HashMap<String, Double>();
        	aggrsPaths.forEach(headerPath -> {
	            aggregationNode.findValues(headerPath.asText()).stream().parallel().forEach(valueNode->{
	                if(valueNode.has(BUCKETS)){
	                    JsonNode buckets = valueNode.findValue(BUCKETS);
	                    buckets.forEach(bucket -> {
	                        Double val = valueNode.findValues(VALUE).isEmpty() ? bucket.findValue(DOC_COUNT).asInt() : bucket.findValue(VALUE).asDouble();
	                        totalValue.add(val);
	                        //System.out.println("Check the localisation value: "+bucket.findValue(KEY).asText()+" "+localisationValues.get(bucket.findValue(KEY).asText()));
	                        String key = localisationValues.get(bucket.findValue(KEY).asText().split("\\.")[0].toUpperCase()) == null ? 
	                        		bucket.findValue(KEY).asText().split("\\.")[0].toUpperCase() : 
	                        			localisationValues.get(bucket.findValue(KEY).asText().split("\\.")[0].toUpperCase());
	                        Double cummVal = createdBuckets.get(key) == null? val : (val + createdBuckets.get(key));
	                        //System.out.println("Existing: "+createdBuckets.get(key) +" new value: "+val );
	                        //System.out.println("  OrigKey:"+bucket.findValue(KEY).asText()+" Inserting "+key+ " val: " +val +" cummValue: "+cummVal+" "+(createdBuckets.get(key) == null));;
	                        createdBuckets.put(key, cummVal);
	                        
	                    });
	                    createdBuckets.keySet().forEach(b -> {
	                    	//System.out.println("Created Bucket: "+b);
	                    	Plot plot = new Plot(b, createdBuckets.get(b), symbol);
	                        headerPlotList.add(plot);
	                    });
	                } else {
	                    List<JsonNode> valueNodes = valueNode.findValues(VALUE).isEmpty() ? valueNode.findValues(DOC_COUNT) : valueNode.findValues(VALUE);
	                    double sum = valueNodes.stream().mapToLong(o -> o.asLong()).sum();
	                    totalValue.add(sum);
	                    Plot plot = new Plot(headerPath.asText(), sum, symbol);
	                    headerPlotList.add(plot);
	                }
	                headerPlotList.sort(new Comparator<Plot>() {
					    @Override
					    public int compare(Plot plot1, Plot plot2) {
					    	return plot2.getValue().compareTo(plot1.getValue());
					    }
	                });
	            });
	        });
        }
        else
        //temporary fix. Should be integrated with the localisation data
        if(requestDto.getVisualizationCode().equals("totalCollectionDeptWisev2"))
        {
        	Double[] cummulativeValue = {(double) 0};
	        aggrsPaths.forEach(headerPath -> {
	            aggregationNode.findValues(headerPath.asText()).stream().parallel().forEach(valueNode->{
	                if(valueNode.has(BUCKETS)){
	                    JsonNode buckets = valueNode.findValue(BUCKETS);
	                    buckets.forEach(bucket -> {
	                        Double val = valueNode.findValues(VALUE).isEmpty() ? bucket.findValue(DOC_COUNT).asInt() : bucket.findValue(VALUE).asDouble();
	                        totalValue.add(val);
	                        
	                        if(bucket.findValue(KEY).asText().equals("TL"))
	                        {
	                        	Plot plot = new Plot(bucket.findValue(KEY).asText(), val, symbol);
	                        	headerPlotList.add(plot);
	                        }
	                        else
	                        {
	                        	cummulativeValue[0] += val;
	                        }
	                        //System.out.println("Check the bucket key value: "+bucket.findValue(KEY).asText() +" "+  symbol);
	                    });
	                } else {
	                    List<JsonNode> valueNodes = valueNode.findValues(VALUE).isEmpty() ? valueNode.findValues(DOC_COUNT) : valueNode.findValues(VALUE);
	                    double sum = valueNodes.stream().mapToLong(o -> o.asLong()).sum();
	                    totalValue.add(sum);
	                    Plot plot = new Plot(headerPath.asText(), sum, symbol);
	                    headerPlotList.add(plot);
	                }
	            });
	        });
	        Plot plot = new Plot("MC", cummulativeValue[0], symbol);
        	headerPlotList.add(plot);
        }
        else
        //temporary fix. Should be integrated with the localisation data
        if(requestDto.getVisualizationCode().equals("licenseApplicationByStatus"))
        {
        	Map<String, String> localisationValues = getLocalisationValues();
        	aggrsPaths.forEach(headerPath -> {
	            aggregationNode.findValues(headerPath.asText()).stream().parallel().forEach(valueNode->{
	                if(valueNode.has(BUCKETS)){
	                    JsonNode buckets = valueNode.findValue(BUCKETS);
	                    buckets.forEach(bucket -> {
	                        Double val = valueNode.findValues(VALUE).isEmpty() ? bucket.findValue(DOC_COUNT).asInt() : bucket.findValue(VALUE).asDouble();
	                        totalValue.add(val);
	                        //System.out.println("Check the localisation value: "+bucket.findValue(KEY).asText()+" "+localisationValues.get(bucket.findValue(KEY).asText()));
	                        System.out.println("Inserting "+localisationValues.get(bucket.findValue(KEY).asText().toUpperCase()) + " val: " + val);
	                        Plot plot = new Plot(localisationValues.get(bucket.findValue(KEY).asText().toUpperCase()), val, symbol);
	                        headerPlotList.add(plot);
	                    });
	
	                } else {
	                    List<JsonNode> valueNodes = valueNode.findValues(VALUE).isEmpty() ? valueNode.findValues(DOC_COUNT) : valueNode.findValues(VALUE);
	                    double sum = valueNodes.stream().mapToLong(o -> o.asLong()).sum();
	                    totalValue.add(sum);
	                    Plot plot = new Plot(headerPath.asText(), sum, symbol);
	                    headerPlotList.add(plot);
	                }
	            });
	        });
        }
        else
        //temporary fix. Should be integrated with the localisation data
        if(requestDto.getVisualizationCode().equals("licenseApplicationByTradeType")
        		|| requestDto.getVisualizationCode().equals("tlTotalCollectionByTradeType"))
        {
        	Map<String, Double> createdBuckets = new HashMap<String, Double>();
        	aggrsPaths.forEach(headerPath -> {
	            aggregationNode.findValues(headerPath.asText()).stream().parallel().forEach(valueNode->{
	                if(valueNode.has(BUCKETS)){
	                    JsonNode buckets = valueNode.findValue(BUCKETS);
	                    buckets.forEach(bucket -> {
	                        Double val = valueNode.findValues(VALUE).isEmpty() ? bucket.findValue(DOC_COUNT).asInt() : bucket.findValue(VALUE).asDouble();
	                        totalValue.add(val);
	                        //System.out.println("Check the localisation value: "+bucket.findValue(KEY).asText()+" "+localisationValues.get(bucket.findValue(KEY).asText()));
	                        System.out.println("Inserting "+bucket.findValue(KEY).asText() + " val: " + val);
	                        String key = bucket.findValue(KEY).asText().split("\\.")[1].toUpperCase();
	                        createdBuckets.put(bucket.findValue(KEY).asText().split("\\.")[1].toUpperCase(), createdBuckets.get(key) == null? val : (val + createdBuckets.get(key)));
	                        
	                    });
	                    createdBuckets.keySet().forEach(b -> {
	                    	Plot plot = new Plot(b, createdBuckets.get(b), symbol);
	                        headerPlotList.add(plot);
	                    });
	                } else {
	                    List<JsonNode> valueNodes = valueNode.findValues(VALUE).isEmpty() ? valueNode.findValues(DOC_COUNT) : valueNode.findValues(VALUE);
	                    double sum = valueNodes.stream().mapToLong(o -> o.asLong()).sum();
	                    totalValue.add(sum);
	                    Plot plot = new Plot(headerPath.asText(), sum, symbol);
	                    headerPlotList.add(plot);
	                }
	            });
	        });
        }
        else
        {
        	aggrsPaths.forEach(headerPath -> {
	            aggregationNode.findValues(headerPath.asText()).stream().parallel().forEach(valueNode->{
	                if(valueNode.has(BUCKETS)){
	                    JsonNode buckets = valueNode.findValue(BUCKETS);
	                    buckets.forEach(bucket -> {
	                        Double val = valueNode.findValues(VALUE).isEmpty() ? bucket.findValue(DOC_COUNT).asInt() : bucket.findValue(VALUE).asDouble();
	                        totalValue.add(val);
	                        Plot plot = new Plot(bucket.findValue(KEY).asText(), val, symbol);
	                        headerPlotList.add(plot);
	                    });
	
	                } else {
	                    List<JsonNode> valueNodes = valueNode.findValues(VALUE).isEmpty() ? valueNode.findValues(DOC_COUNT) : valueNode.findValues(VALUE);
	                    double sum = valueNodes.stream().mapToLong(o -> o.asLong()).sum();
	                    totalValue.add(sum);
	                    Plot plot = new Plot(headerPath.asText(), sum, symbol);
	                    headerPlotList.add(plot);
	                }
	            });
	        });
        }

        Data data = new Data(headerKey, totalValue.stream().reduce(0.0, Double::sum), symbol);
        data.setPlots(headerPlotList);
        dataList.add(data);

        return getAggregatedDto(chartNode, dataList, requestDto.getVisualizationCode());

    }
   
    //This is a temporary fix. Needs to be integrated with the localisation service
    private Map<String, String> getLocalisationValues()
    {
    	Map<String, String> map = Stream.of(new String[][] {
    		{ "INITIATED", "Initiated" }, 
    		{ "PENDINGPAYMENT", "Pending Payment" },
    		{ "PENDINGAPPROVAL", "Pending Approval" },
    		{ "APPLIED", "Applied" },
    		{ "PENDINGAPPLFEE", "Pending Application Fee" },
    		{ "PENDINGFORAPPFEE", "Pending Application Fee" },
    		{ "PENDING_APPL_FEE_PAYMENT", "Pending Application Fee"},
    		{ "APPROVED", "Approved"},
    		{ "FIELDINSPECTION", "Field Inspection" },
    		{ "REJECTED", "Rejected" },
    		{ "EXPIRED", "Expired" },
    		{ "CANCELLED", "Cancelled" },
    		{ "CITIZENACTIONREQUIRED", "Citizen Review" },
    		
    		{ "ASSREV", "Assigned Revenues and Compensations" },
    		{ "SALE", "Sale and Hire Charges" },
    		{ "OTHFEE", "Other Fee and Fines" },
    		{ "OTHERS", "Others" },
    		{ "GRNTINT", "Grants and Interest Earned" },
    		{ "TX", "Taxes" },
    		{ "CREDDEP", "Creditors Deposits" },
    		{ "SRVC", "Service and Adminstrative Charges" },
    		{ "ENTFEE", "Entry Fee" },
    		{ "RENT", "Rent From Municipal Properties" },
    		{ "UC", "User Charges" }
    	}
    	).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    	return map;
    }
    
}
