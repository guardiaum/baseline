package com.jms5.baseline.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Util {
	
	public static Map<String, Double> calculateFrequency(Map<String, Integer> counted, double denominator) {
		Map<String, Double> frequency = new HashMap<String, Double>();
		
		for (Map.Entry<String, Integer> entry : counted.entrySet()) {
			frequency.put(entry.getKey(), entry.getValue() / denominator);
		}
		
		return frequency;
	}
	
	public static Map<String, Double> sortCategoriesDesc(Map<String, Double> unsorted) {
		List<Map.Entry<String, Double>> list = 
				new LinkedList<Map.Entry<String, Double>>(unsorted.entrySet());
		
		Collections.sort(list, 
				new Comparator<Map.Entry<String,Double>>(){

			@Override
			public int compare(Entry<String, Double> o1, 
					Entry<String, Double> o2) {
				return -((o1.getValue()).compareTo(o2.getValue()));
			}
			
		});
		
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		
		for (Map.Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
	
	public static Map<String, Double> sortCategoriesAsc(Map<String, Double> unsorted) {
		List<Map.Entry<String, Double>> list = 
				new LinkedList<Map.Entry<String, Double>>(unsorted.entrySet());
		
		Collections.sort(list, 
				new Comparator<Map.Entry<String,Double>>(){

			@Override
			public int compare(Entry<String, Double> o1, 
					Entry<String, Double> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
			
		});
		
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		
		for (Map.Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		
		return sortedMap;
	}
}
